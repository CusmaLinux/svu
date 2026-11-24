package co.edu.itp.svu.repository;

import static co.edu.itp.svu.domain.attachment.AttachmentOperationState.*;
import static com.mongodb.client.model.Filters.*;

import co.edu.itp.svu.domain.attachment.AttachmentOperation;
import co.edu.itp.svu.domain.attachment.AttachmentOperationState;
import co.edu.itp.svu.domain.attachment.AttachmentOperationType;
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
import org.bson.BsonType;
import org.bson.Document;
import org.bson.conversions.Bson;
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

    /** No upsert. An ambiguous write is propagated; retry only with the same protected identity. */
    public AttachmentOperation reserve(AttachmentOperation candidate) {
        validateReservation(candidate);
        requireUniqueIndex();
        Document raw = new Document();
        mongo.getConverter().write(candidate, raw);
        Date serverTime = mongo.getDb().runCommand(new Document("hello", 1), ReadPreference.primary()).getDate("localTime");
        raw.put("created_at", serverTime);
        raw.put("updated_at", serverTime);
        try {
            collection().insertOne(raw);
            return decode(raw);
        } catch (MongoWriteException e) {
            if (e.getError().getCode() != 11000) throw e;
            String message = e.getError().getMessage();
            if (!message.contains("index: " + IDEMPOTENCY_INDEX + " dup key:") && !message.contains("index: _id_ dup key:")) {
                throw new DuplicateKeyException("Unrecognized unique constraint");
            }
            // Both unique constraints can collide. Never disclose the record occupying an unrelated _id.
            var byId = findById(candidate.getId());
            var existing = findByIdentity(candidate.getActorScopeHash(), candidate.getOperationType(), candidate.getRequestKeyHash());
            var prior = existing.orElseThrow(() -> new DuplicateKeyException("Manifest identifier collision"));
            if (byId.isPresent() && !byId.orElseThrow().getId().equals(prior.getId())) throw new DuplicateKeyException(
                "Manifest identifier collision"
            );
            boolean sameTarget =
                candidate.getOperationType() == AttachmentOperationType.PUBLIC_CASE_CREATE ||
                Objects.equals(candidate.getCaseId(), prior.getCaseId());
            if (
                prior.getFormatVersion() != 1 ||
                !sameTarget ||
                !candidate.getRequestDigest().equals(prior.getRequestDigest()) ||
                !sameFileFingerprints(candidate.getFiles(), prior.getFiles())
            ) throw new IdempotencyConflict();
            return prior;
        }
    }

    private static boolean sameFileFingerprints(
        List<AttachmentOperation.FileEntry> requested,
        List<AttachmentOperation.FileEntry> existing
    ) {
        if (existing == null || requested.size() != existing.size()) return false;
        for (int i = 0; i < requested.size(); i++) {
            var file = requested.get(i);
            var prior = existing.get(i);
            if (!Objects.equals(file.getSizeBytes(), prior.getSizeBytes()) || !file.getSha256().equals(prior.getSha256())) return false;
        }
        return true;
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
            op.getFormatVersion() != 1 ||
            op.getState() != PREPARING ||
            op.getRevision() != 0 ||
            op.getOperationType() == null ||
            op.getClaim() != null ||
            op.getEvidence() != null ||
            op.getLastFailure() != null ||
            op.getFiles() == null ||
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
        return mongo.getConverter().read(AttachmentOperation.class, value);
    }

    private Optional<AttachmentOperation> first(Bson filter) {
        return Optional.ofNullable(collection().find(filter).first()).map(this::decode);
    }

    public List<AttachmentOperation> findPending(Instant before, int limit) {
        return page(and(eq("format_version", 1), in("state", ACTIVE), lte("updated_at", Date.from(before))), "updated_at", limit);
    }

    public List<AttachmentOperation> findExpiredClaims(int limit) {
        return page(and(eq("format_version", 1), in("state", ACTIVE), expired()), LEASE, limit);
    }

    public List<AttachmentOperation> findForReconciliation(int limit) {
        return page(eq("state", RECONCILIATION_REQUIRED.name()), "updated_at", limit);
    }

    private List<AttachmentOperation> page(Bson filter, String sortField, int limit) {
        if (limit < 1 || limit > MAX_PAGE) throw new IllegalArgumentException("Limit must be between 1 and 100");
        return collection().find(filter).sort(Sorts.ascending(sortField, "_id")).limit(limit).map(this::decode).into(new ArrayList<>());
    }

    /** A snapshot is only a CAS precondition. MongoDB checks it again during every write. */
    public record Version(String id, AttachmentOperationState state, long revision) {
        Bson filter() {
            if (revision < 0 || revision == Long.MAX_VALUE || !RECOVERABLE.contains(state)) throw new IllegalArgumentException(
                "Manifest requires manual handling or is terminal"
            );
            return and(eq("_id", id), eq("format_version", 1), eq("state", state.name()), eq("revision", revision));
        }
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
        return change(and(version.filter(), or(firstClaim, laterClaim)), changes);
    }

    public Optional<AttachmentOperation> renew(Lease lease, Duration ttl) {
        return change(lease.filter(), new Document(LEASE, deadline(ttl)));
    }

    public Optional<AttachmentOperation> release(Lease lease) {
        return change(lease.filter(), clearLease());
    }

    private static Document clearLease() {
        return new Document(OWNER, "$$REMOVE").append(LEASE, "$$REMOVE");
    }

    public record VerifiedFile(String storageKey, long size, String sha256) {}

    /** Observations come from a trusted reader of the finished bytes, never from client-provided metadata. */
    public Optional<AttachmentOperation> filesReady(Lease lease, List<VerifiedFile> observed) {
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
