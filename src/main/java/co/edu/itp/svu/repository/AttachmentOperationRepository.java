package co.edu.itp.svu.repository;

import static co.edu.itp.svu.domain.attachment.AttachmentOperationState.*;
import static com.mongodb.client.model.Filters.*;

import co.edu.itp.svu.domain.attachment.AttachmentOperation;
import co.edu.itp.svu.domain.attachment.AttachmentOperation.LogicalFile;
import co.edu.itp.svu.domain.attachment.AttachmentOperation.PhysicalGeneration;
import co.edu.itp.svu.domain.attachment.AttachmentOperationState;
import co.edu.itp.svu.domain.attachment.AttachmentOperationType;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Sorts;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.bson.BsonBinaryWriter;
import org.bson.BsonType;
import org.bson.Document;
import org.bson.codecs.EncoderContext;
import org.bson.conversions.Bson;
import org.bson.io.BasicOutputBuffer;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Repository;

/** Persistence primitives only. No background recovery or publication is started by this repository. */
@Repository
public class AttachmentOperationRepository {

    public static final String COLLECTION = "attachment_operation";

    static final int MAX_MANIFEST_BYTES = 1_048_576;
    private static final int MAX_ATTEMPTS = 5;
    private static final String GENERATIONS = "physical_generations";
    private static final String SELECTED = "selected_physical_generation";
    private static final String FORMAT = "format_version";
    private static final String BSON_BUDGET = "PHYSICAL_MANIFEST_BUDGET";
    private static final String ATTEMPTS_EXHAUSTED = "PHYSICAL_ATTEMPTS_EXHAUSTED";
    private static final int MAX_PAGE = 100;
    private static final String OWNER = "claim.owner_id";
    private static final String LEASE = "claim.lease_until";
    private static final String FENCE = "claim.fencing_token";
    private static final String IDEMPOTENCY_INDEX = "uq_attachment_operation_idempotency";
    private static final List<String> ACTIVE = RECOVERABLE.stream().map(Enum::name).toList();
    private static final WriteConcern JOURNALED = WriteConcern.W1.withJournal(true).withWTimeout(10, TimeUnit.SECONDS);

    private final MongoTemplate mongo;

    public AttachmentOperationRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** Explicit and idempotent. Call only from an authorized migration or isolated test. */
    public void ensureIndexes() {
        IndexOperations indexes = mongo.indexOps(COLLECTION);
        indexes.ensureIndex(
            new Index()
                .on("actor_scope_hash", Sort.Direction.ASC)
                .on("operation_type", Sort.Direction.ASC)
                .on("request_key_hash", Sort.Direction.ASC)
                .unique()
                .named(IDEMPOTENCY_INDEX)
        );
        indexes.ensureIndex(
            new Index().on("state", Sort.Direction.ASC).on("updated_at", Sort.Direction.ASC).named("ix_attachment_operation_pending")
        );
        indexes.ensureIndex(
            new Index()
                .on("claim.lease_until", Sort.Direction.ASC)
                .on("state", Sort.Direction.ASC)
                .named("ix_attachment_operation_claim_expiry")
        );
    }

    private MongoCollection<Document> collection() {
        return mongo.getCollection(COLLECTION).withReadPreference(ReadPreference.primary()).withWriteConcern(JOURNALED);
    }

    public static class IdempotencyConflict extends IllegalStateException {

        private IdempotencyConflict() {
            super("Request key already reserved for different input");
        }
    }

    public static class UnsupportedManifestFormat extends IllegalStateException {

        private UnsupportedManifestFormat() {
            super("Unsupported attachment manifest format");
        }
    }

    /** No upsert. An ambiguous write is propagated; retry only with the same protected identity. */
    public AttachmentOperation reserve(AttachmentOperation candidate) {
        validateReservation(candidate);
        requireUniqueIndex();
        // Search across formats, including retries whose new candidate would exceed the v2 budget.
        var prior = existingReservation(candidate);
        if (prior.isPresent()) return prior.orElseThrow();
        Document raw = new Document();
        mongo.getConverter().write(candidate, raw);
        Date serverTime = mongo.getDb().runCommand(new Document("hello", 1), ReadPreference.primary()).getDate("localTime");
        raw.put("created_at", serverTime);
        raw.put("updated_at", serverTime);
        if (candidate.getFormatVersion() == 2) {
            raw.put("files", candidate.getLogicalFiles().stream().map(AttachmentOperationRepository::logicalFile).toList());
            raw.put(GENERATIONS, List.of());
            if (bsonSize(completionEnvelope(raw)) > MAX_MANIFEST_BYTES) {
                throw new IllegalArgumentException("Attachment manifest exceeds BSON budget");
            }
        }
        try {
            collection().insertOne(raw);
            return decode(raw);
        } catch (MongoWriteException e) {
            if (e.getError().getCode() != 11000) throw e;
            String message = e.getError().getMessage();
            if (!message.contains("index: " + IDEMPOTENCY_INDEX + " dup key:") && !message.contains("index: _id_ dup key:")) {
                throw new DuplicateKeyException("Unrecognized unique constraint");
            }
            return existingReservation(candidate).orElseThrow(() -> new DuplicateKeyException("Manifest identifier collision"));
        }
    }

    private Optional<AttachmentOperation> existingReservation(AttachmentOperation candidate) {
        var existing = findByIdentity(candidate.getActorScopeHash(), candidate.getOperationType(), candidate.getRequestKeyHash());
        if (existing.isEmpty()) return existing;
        var prior = existing.orElseThrow();
        if (
            !candidate.getId().equals(prior.getId()) &&
            collection().find(eq("_id", candidate.getId())).projection(new Document("_id", 1)).first() != null
        ) {
            throw new DuplicateKeyException("Manifest identifier collision");
        }
        boolean sameTarget =
            candidate.getOperationType() == AttachmentOperationType.PUBLIC_CASE_CREATE ||
            Objects.equals(candidate.getCaseId(), prior.getCaseId());
        if (
            !sameTarget ||
            !candidate.getRequestDigest().equals(prior.getRequestDigest()) ||
            !fingerprints(candidate).equals(fingerprints(prior))
        ) throw new IdempotencyConflict();
        return existing;
    }

    private record Fingerprint(Long size, String sha256) {}

    private static List<Fingerprint> fingerprints(AttachmentOperation operation) {
        if (operation.getFormatVersion() == 2) {
            return operation
                .getLogicalFiles()
                .stream()
                .map(file -> new Fingerprint(file.expectedSizeBytes(), file.expectedSha256()))
                .toList();
        }
        return operation.getFiles().stream().map(file -> new Fingerprint(file.getSizeBytes(), file.getSha256())).toList();
    }

    private static Document logicalFile(LogicalFile file) {
        return new Document("logical_id", file.logicalId())
            .append("metadata_id", file.metadataId())
            .append("expected_size_bytes", file.expectedSizeBytes())
            .append("expected_sha256", file.expectedSha256());
    }

    /** Collection/index provisioning is explicit. A missing unique index must never turn reserve into an ordinary insert. */
    private void requireUniqueIndex() {
        Document keys = new Document("actor_scope_hash", 1).append("operation_type", 1).append("request_key_hash", 1);
        for (Document index : collection().listIndexes()) {
            if (
                IDEMPOTENCY_INDEX.equals(index.getString("name")) &&
                index.getBoolean("unique", false) &&
                keys.equals(index.get("key")) &&
                !index.containsKey("partialFilterExpression") &&
                !index.getBoolean("sparse", false) &&
                !index.containsKey("collation")
            ) return;
        }
        throw new IllegalStateException("Required manifest index unavailable");
    }

    private static void validateReservation(AttachmentOperation op) {
        if (
            !canonicalUuid(op.getId()) ||
            (op.getFormatVersion() != 1 && op.getFormatVersion() != 2) ||
            op.getState() != PREPARING ||
            op.getRevision() != 0 ||
            op.getOperationType() == null ||
            op.getClaim() != null ||
            op.getEvidence() != null ||
            op.getLastFailure() != null ||
            op.getFiles() == null ||
            op.getSelectedPhysicalGeneration() != null ||
            op.getPhysicalGenerations() != null ||
            !digest(op.getActorScopeHash()) ||
            !digest(op.getRequestKeyHash()) ||
            !digest(op.getRequestDigest()) ||
            !validCaseReference(op) ||
            (op.getResponseId() != null && !validReference(op.getResponseId())) ||
            (op.getPublicationId() != null && !canonicalUuid(op.getPublicationId())) ||
            (op.getOperationType() != AttachmentOperationType.PUBLIC_CASE_CREATE && op.getResponseId() == null)
        ) {
            throw new IllegalArgumentException("Invalid manifest reservation");
        }
        Set<ObjectId> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        if (op.getFormatVersion() == 2) {
            if (!op.getFiles().isEmpty()) throw new IllegalArgumentException("Legacy file data in v2 reservation");
            for (var file : op.getLogicalFiles()) {
                if (
                    !canonicalUuid(file.logicalId()) ||
                    !keys.add(file.logicalId()) ||
                    file.metadataId() == null ||
                    !ids.add(file.metadataId()) ||
                    file.expectedSizeBytes() < 0 ||
                    !digest(file.expectedSha256())
                ) {
                    throw new IllegalArgumentException("Invalid logical file reservation");
                }
            }
            return;
        }
        if (!op.getLogicalFiles().isEmpty()) throw new IllegalArgumentException("Logical file data in v1 reservation");
        for (var file : op.getFiles()) {
            if (
                file.getMetadataId() == null ||
                !ids.add(file.getMetadataId()) ||
                !canonicalUuid(file.getStorageKey()) ||
                !keys.add(file.getStorageKey()) ||
                !"RESERVED".equals(file.getState()) ||
                file.getSizeBytes() == null ||
                file.getSizeBytes() < 0 ||
                !digest(file.getSha256())
            ) {
                throw new IllegalArgumentException("Invalid file reservation");
            }
        }
    }

    private static boolean canonicalUuid(String value) {
        if (value == null || value.length() != 36) return false;
        try {
            return UUID.fromString(value).toString().equals(value);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean validCaseReference(AttachmentOperation op) {
        // For a response, the authorized existing case may have a legacy BSON string identifier.
        if (op.getOperationType() != AttachmentOperationType.PUBLIC_CASE_CREATE && op.getCaseId() instanceof String persistedId) {
            return !persistedId.isBlank();
        }
        return validReference(op.getCaseId());
    }

    private static boolean validReference(Object value) {
        return value instanceof ObjectId || (value instanceof String id && (ObjectId.isValid(id) || canonicalUuid(id)));
    }

    private static boolean digest(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    public Optional<AttachmentOperation> findById(String id) {
        return first(eq("_id", id));
    }

    public Optional<AttachmentOperation> findByIdentity(String actorScopeHash, AttachmentOperationType type, String requestKeyHash) {
        return first(
            and(eq("actor_scope_hash", actorScopeHash), eq("operation_type", type.name()), eq("request_key_hash", requestKeyHash))
        );
    }

    private AttachmentOperation decode(Document value) {
        if (Integer.valueOf(1).equals(value.get(FORMAT))) return mongo.getConverter().read(AttachmentOperation.class, value);
        if (!Integer.valueOf(2).equals(value.get(FORMAT))) throw new UnsupportedManifestFormat();
        Document common = new Document(value);
        common.remove("files");
        var operation = mongo.getConverter().read(AttachmentOperation.class, common);
        operation.setLogicalFiles(
            value
                .getList("files", Document.class)
                .stream()
                .map(file ->
                    new LogicalFile(
                        file.getString("logical_id"),
                        file.getObjectId("metadata_id"),
                        file.getLong("expected_size_bytes"),
                        file.getString("expected_sha256")
                    )
                )
                .toList()
        );
        return operation;
    }

    private Optional<AttachmentOperation> first(Bson filter) {
        return Optional.ofNullable(collection().find(filter).first()).map(this::decode);
    }

    public List<AttachmentOperation> findPending(Instant before, int limit) {
        return page(and(knownFormats(), in("state", ACTIVE), lte("updated_at", Date.from(before))), "updated_at", limit);
    }

    public List<AttachmentOperation> findExpiredClaims(int limit) {
        return page(and(knownFormats(), in("state", ACTIVE), expired()), LEASE, limit);
    }

    public List<AttachmentOperation> findForReconciliation(int limit) {
        return page(and(knownFormats(), eq("state", RECONCILIATION_REQUIRED.name())), "updated_at", limit);
    }

    /** Minimal diagnostic view. Unknown structures never enter the typed mapper or worker queues. */
    public List<Document> findUnsupportedFormats(int limit) {
        return collection()
            .find(nor(knownFormats()))
            .projection(new Document(FORMAT, 1))
            .sort(Sorts.ascending("_id"))
            .limit(checkedLimit(limit))
            .into(new ArrayList<>());
    }

    private List<AttachmentOperation> page(Bson filter, String sortField, int limit) {
        return collection()
            .find(filter)
            .sort(Sorts.ascending(sortField, "_id"))
            .limit(checkedLimit(limit))
            .map(this::decode)
            .into(new ArrayList<>());
    }

    private static int checkedLimit(int limit) {
        if (limit < 1 || limit > MAX_PAGE) throw new IllegalArgumentException("Limit must be between 1 and 100");
        return limit;
    }

    /** A snapshot is only a CAS precondition. MongoDB checks it again during every write. */
    public record Version(String id, AttachmentOperationState state, long revision, int formatVersion) {
        public Version(String id, AttachmentOperationState state, long revision) {
            this(id, state, revision, 1);
        }
        Bson filter() {
            if (revision < 0 || revision == Long.MAX_VALUE || !RECOVERABLE.contains(state)) throw new IllegalArgumentException(
                "Manifest requires manual handling or is terminal"
            );
            return and(
                eq("_id", id),
                and(eq(FORMAT, checkedFormat(formatVersion)), formatIsInt()),
                eq("state", state.name()),
                eq("revision", revision)
            );
        }
    }

    private static Bson knownFormats() {
        return and(in(FORMAT, 1, 2), formatIsInt());
    }

    private static Bson formatIsInt() {
        return expr(new Document("$eq", List.of(new Document("$type", "$" + FORMAT), "int")));
    }

    private static int checkedFormat(int format) {
        if (format != 1 && format != 2) throw new UnsupportedManifestFormat();
        return format;
    }

    public record Lease(Version version, UUID owner, long fencing) {
        Bson filter() {
            if (fencing < 1) throw new IllegalArgumentException("Invalid fencing generation");
            return and(
                version.filter(),
                eq(OWNER, owner.toString()),
                eq(FENCE, fencing),
                type(LEASE, BsonType.DATE_TIME),
                expr(new Document("$gt", List.of("$" + LEASE, "$$NOW")))
            );
        }
    }

    private static Bson expired() {
        return and(type(LEASE, BsonType.DATE_TIME), expr(new Document("$lte", List.of("$" + LEASE, "$$NOW"))));
    }

    private static Document deadline(Duration ttl) {
        if (ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofHours(1)) > 0 || ttl.toMillis() == 0) {
            throw new IllegalArgumentException("Lease duration must be between one millisecond and one hour");
        }
        return new Document("$add", List.of("$$NOW", ttl.toMillis()));
    }

    public Optional<AttachmentOperation> acquire(Version version, UUID owner, Duration ttl) {
        Bson firstClaim = and(exists("claim", false), eq("revision", 0L), eq("state", PREPARING.name()));
        Bson released = and(exists(OWNER, false), exists(LEASE, false));
        Bson takeover = and(type(OWNER, BsonType.STRING), expired());
        Bson laterClaim = and(type(FENCE, BsonType.INT64), gte(FENCE, 1L), lt(FENCE, Long.MAX_VALUE), or(released, takeover));
        Document priorFence = new Document("$ifNull", List.of("$" + FENCE, 0L));
        Document changes = new Document(OWNER, owner.toString())
            .append(LEASE, deadline(ttl))
            .append(FENCE, new Document("$add", List.of(priorFence, 1L)));
        Bson filter = and(version.filter(), or(firstClaim, laterClaim));
        if (version.formatVersion() == 2) {
            filter = and(filter, lt("revision", Long.MAX_VALUE - 1), or(exists(FENCE, false), lt(FENCE, Long.MAX_VALUE - 1)));
        }
        return change(filter, changes);
    }

    public Optional<AttachmentOperation> renew(Lease lease, Duration ttl) {
        return change(ordinaryMutation(lease), new Document(LEASE, deadline(ttl)));
    }

    public Optional<AttachmentOperation> release(Lease lease) {
        return change(ordinaryMutation(lease), clearLease());
    }

    private static Bson ordinaryMutation(Lease lease) {
        var filter = lease.filter();
        return lease.version().formatVersion() == 2 ? and(filter, lt("revision", Long.MAX_VALUE - 1)) : filter;
    }

    private static Document clearLease() {
        return new Document(OWNER, "$$REMOVE").append(LEASE, "$$REMOVE");
    }

    public record VerifiedFile(String storageKey, long size, String sha256) {}

    /** Observations come from a trusted reader of the finished bytes, never from client-provided metadata. */
    public Optional<AttachmentOperation> filesReady(Lease lease, List<VerifiedFile> observed) {
        if (lease.version().formatVersion() != 1) return Optional.empty();
        requireTransition(lease, FILES_READY);
        var current = first(lease.filter());
        if (current.isEmpty()) return Optional.empty();
        var files = current.orElseThrow().getFiles();
        if (files.size() != observed.size()) throw new IllegalArgumentException("Incomplete file verification");
        for (int i = 0; i < files.size(); i++) {
            var file = files.get(i);
            var observation = observed.get(i);
            if (
                !Objects.equals(file.getStorageKey(), observation.storageKey()) ||
                !Objects.equals(file.getSizeBytes(), observation.size()) ||
                !digest(observation.sha256()) ||
                !observation.sha256().equals(file.getSha256())
            ) {
                throw new IllegalArgumentException("File verification mismatch");
            }
        }
        Document changes = new Document("state", FILES_READY.name());
        Document verified = new Document("$mergeObjects", List.of("$$file", new Document("state", "VERIFIED")));
        changes.append("files", new Document("$map", new Document("input", "$files").append("as", "file").append("in", verified)));
        return change(lease.filter(), changes);
    }

    public Optional<AttachmentOperation> beginPersistence(Lease lease) {
        if (lease.version().formatVersion() != 1) return Optional.empty();
        requireTransition(lease, PERSISTING);
        return change(lease.filter(), new Document("state", PERSISTING.name()));
    }

    public Optional<AttachmentOperation> abortBeforePersistence(Lease lease) {
        requireTransition(lease, ABORTED);
        var current = first(lease.filter());
        if (current.isEmpty()) return Optional.empty();
        var op = current.orElseThrow();
        boolean isCase = op.getOperationType() == AttachmentOperationType.PUBLIC_CASE_CREATE;
        Object id = isCase ? op.getCaseId() : op.getResponseId();
        // Legacy string identifiers may also be represented by a BSON ObjectId.
        Bson root = eq("_id", id);
        if (id instanceof ObjectId oid) root = in("_id", oid, oid.toHexString());
        if (id instanceof String s && ObjectId.isValid(s)) root = in("_id", s, new ObjectId(s));
        if (!validReference(id)) return Optional.empty();
        var roots = mongo.getCollection(isCase ? "pqrs" : "respuesta").withReadPreference(ReadPreference.primary());
        if (roots.find(root).projection(new Document("_id", 1)).first() != null) return Optional.empty();
        // Publication can only start after PERSISTING; that competing CAS invalidates this revision.
        return change(lease.filter(), clearLease().append("state", ABORTED.name()));
    }

    public Optional<AttachmentOperation> requireReconciliation(Lease lease) {
        requireTransition(lease, RECONCILIATION_REQUIRED);
        return change(lease.filter(), clearLease().append("state", RECONCILIATION_REQUIRED.name()));
    }

    /** Allocates one journaled batch. An uncertain write is resolved with the original capability. */
    public Optional<AttachmentOperation> reservePhysicalGeneration(Lease lease, AttachmentWriterCapability writer) {
        Bson filter = physicalLease(lease);
        String proof = writer.proof(lease.version().id(), lease.fencing());
        Document current = collection().find(filter).first();
        if (current == null) return Optional.empty();

        List<Document> history = new ArrayList<>(current.getList(GENERATIONS, Document.class));
        if (history.stream().anyMatch(generation -> generation.getLong("generation") == lease.fencing())) {
            return confirmPhysicalReservation(lease, writer);
        }
        if (history.size() >= MAX_ATTEMPTS) return reconcilePhysical(filter, ATTEMPTS_EXHAUSTED);
        if (lease.version().revision() >= Long.MAX_VALUE - 2) return reconcilePhysical(filter, "PHYSICAL_REVISION_EXHAUSTED");

        List<Document> files = current.getList("files", Document.class);
        List<UUID> keys = writer.prepareKeys(files.size());
        Set<String> occupied = new HashSet<>();
        history.forEach(generation ->
            generation.getList("entries", Document.class).forEach(entry -> occupied.add(entry.getString("storage_key")))
        );
        List<Document> entries = new ArrayList<>(files.size());
        for (int i = 0; i < files.size(); i++) {
            String key = keys.get(i).toString();
            if (!occupied.add(key)) throw new IllegalStateException("Physical storage key collision");
            entries.add(new Document("logical_id", files.get(i).getString("logical_id")).append("storage_key", key));
        }

        // Date(0) is a size placeholder. The Mongo expression writes its own clock into the new entry.
        Document batch = new Document("generation", lease.fencing())
            .append("reserved_at", new Date(0))
            .append("writer_proof_hash", proof)
            .append("state", "RESERVED")
            .append("entries", entries);
        history.add(batch);
        Document candidate = new Document(current).append(GENERATIONS, history);
        int completedBytes = bsonSize(completionEnvelope(candidate));
        if (completedBytes > MAX_MANIFEST_BYTES) return reconcilePhysical(filter, BSON_BUDGET);

        Bson guard = and(filter, ne(GENERATIONS + ".generation", lease.fencing()), sizeGuard(current, completedBytes));
        batch.put("reserved_at", "$$NOW");
        Document append = new Document("$concatArrays", List.of("$" + GENERATIONS, List.of(batch)));
        return change(guard, new Document(GENERATIONS, append));
    }

    public Optional<PhysicalGeneration> findPhysicalGeneration(String id, long generation) {
        return findById(id)
            .filter(operation -> operation.getFormatVersion() == 2)
            .flatMap(operation -> operation.getPhysicalGenerations().stream().filter(batch -> batch.generation() == generation).findFirst()
            );
    }

    /** A primary read identifies a matching batch. A fresh journaled revision CAS establishes durability. */
    public Optional<AttachmentOperation> confirmPhysicalReservation(Lease lease, AttachmentWriterCapability writer) {
        Bson filter = and(physicalLease(lease), lt("revision", Long.MAX_VALUE - 1));
        String proof = writer.proof(lease.version().id(), lease.fencing());
        Document current = collection().find(filter).first();
        if (current == null) return Optional.empty();
        var persisted = current
            .getList(GENERATIONS, Document.class)
            .stream()
            .filter(
                batch ->
                    batch.getLong("generation") == lease.fencing() &&
                    proof.equals(batch.getString("writer_proof_hash")) &&
                    "RESERVED".equals(batch.getString("state"))
            )
            .findFirst();
        if (persisted.isEmpty()) return Optional.empty();
        Document batch = persisted.orElseThrow();
        var entries = batch.getList("entries", Document.class);
        var files = current.getList("files", Document.class);
        if (
            !entries
                .stream()
                .map(entry -> entry.getString("logical_id"))
                .toList()
                .equals(files.stream().map(file -> file.getString("logical_id")).toList()) ||
            !writer.matchesKeys(entries.stream().map(entry -> UUID.fromString(entry.getString("storage_key"))).toList())
        ) {
            return Optional.empty();
        }
        return change(and(filter, eq(GENERATIONS, batch)), new Document());
    }

    /** Issues one process-local context only after a successful journaled confirmation. */
    public Optional<PhysicalWriteContext> authorizePhysicalWrite(Lease lease, AttachmentWriterCapability writer) {
        synchronized (writer) {
            if (writer.writeAuthorized()) return Optional.empty();
            Optional<AttachmentOperation> confirmed;
            try {
                confirmed = confirmPhysicalReservation(lease, writer);
            } catch (MongoException ex) {
                throw new IllegalStateException("Physical writer authorization unavailable");
            }
            if (confirmed.isEmpty()) return Optional.empty();
            AttachmentOperation operation = confirmed.orElseThrow();
            var batch = operation
                .getPhysicalGenerations()
                .stream()
                .filter(generation -> generation.generation() == lease.fencing())
                .findFirst()
                .orElseThrow();
            List<ReservedFile> files = new ArrayList<>();
            for (int i = 0; i < operation.getLogicalFiles().size(); i++) {
                var logical = operation.getLogicalFiles().get(i);
                files.add(
                    new ReservedFile(
                        logical.logicalId(),
                        batch.entries().get(i).storageKey(),
                        logical.expectedSizeBytes(),
                        logical.expectedSha256()
                    )
                );
            }
            var currentLease = new Lease(
                new Version(operation.getId(), PREPARING, operation.getRevision(), 2),
                lease.owner(),
                lease.fencing()
            );
            var filter = physicalLease(currentLease);
            writer.authorizeWrite();
            return Optional.of(
                new PhysicalWriteContext(
                    currentLease,
                    files,
                    () -> collection().find(filter).projection(new Document("_id", 1)).first() != null
                )
            );
        }
    }

    public record ReservedFile(String logicalId, String storageKey, long expectedSize, String expectedSha256) {}

    @com.fasterxml.jackson.annotation.JsonIgnoreType
    public static final class PhysicalWriteContext {

        private final Lease lease;
        private final List<ReservedFile> files;
        private final BooleanSupplier current;
        private final AtomicBoolean started = new AtomicBoolean();

        private PhysicalWriteContext(Lease lease, List<ReservedFile> files, BooleanSupplier current) {
            this.lease = lease;
            this.files = List.copyOf(files);
            this.current = current;
        }

        public boolean start() {
            return started.compareAndSet(false, true);
        }

        public Lease lease() {
            return lease;
        }

        public List<ReservedFile> files() {
            return files;
        }

        public boolean current() {
            try {
                return current.getAsBoolean();
            } catch (MongoException ex) {
                throw new IllegalStateException("Physical writer claim check unavailable");
            }
        }

        @Override
        public String toString() {
            return "PhysicalWriteContext[redacted]";
        }
    }

    private static Bson physicalLease(Lease lease) {
        if (lease.version().formatVersion() != 2 || lease.version().state() != PREPARING) {
            throw new IllegalArgumentException("Physical reservations require v2 PREPARING");
        }
        return and(lease.filter(), exists(SELECTED, false));
    }

    private Optional<AttachmentOperation> reconcilePhysical(Bson filter, String reason) {
        return change(filter, clearLease().append("state", RECONCILIATION_REQUIRED.name()).append("last_failure", reason));
    }

    private Bson sizeGuard(Document current, int completedBytes) {
        int growth = completedBytes - bsonSize(current);
        return expr(
            new Document("$lte", List.of(new Document("$add", List.of(new Document("$bsonSize", "$$ROOT"), growth)), MAX_MANIFEST_BYTES))
        );
    }

    /** Exact upper envelope for the already reserved history plus the fields needed to finish or reconcile. */
    static Document completionEnvelope(Document candidate) {
        Document envelope = new Document(candidate);
        envelope.put("claim", new Document("owner_id", "0".repeat(36)).append("lease_until", new Date(0)).append("fencing_token", 0L));
        envelope.put("state", RECONCILIATION_REQUIRED.name());
        envelope.put("last_failure", "PHYSICAL_REVISION_EXHAUSTED");
        envelope.put(SELECTED, 0L);
        List<Document> history = new ArrayList<>(candidate.getList(GENERATIONS, Document.class));
        if (!history.isEmpty()) {
            int last = history.size() - 1;
            Document batch = new Document(history.get(last));
            batch.put("state", "VERIFIED");
            batch.put(
                "entries",
                batch
                    .getList("entries", Document.class)
                    .stream()
                    .map(entry ->
                        new Document(entry)
                            .append("observed_size_bytes", 0L)
                            .append("observed_sha256", "0".repeat(64))
                            .append("verified_at", new Date(0))
                    )
                    .toList()
            );
            history.set(last, batch);
        }
        envelope.put(GENERATIONS, history);
        return envelope;
    }

    int bsonSize(Document value) {
        try (var buffer = new BasicOutputBuffer(); var writer = new BsonBinaryWriter(buffer)) {
            collection().getCodecRegistry().get(Document.class).encode(writer, value, EncoderContext.builder().build());
            return buffer.getSize();
        }
    }

    private static void requireTransition(Lease lease, AttachmentOperationState next) {
        if (!lease.version().state().permits(next)) throw new IllegalArgumentException("Unsupported manifest transition");
    }

    private Optional<AttachmentOperation> change(Bson filter, Document changes) {
        changes.append("updated_at", "$$NOW").append("revision", new Document("$add", List.of("$revision", 1L)));
        Document updated = collection()
            .findOneAndUpdate(
                filter,
                List.of(new Document("$set", changes)),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            );
        return Optional.ofNullable(updated).map(this::decode);
    }
}
