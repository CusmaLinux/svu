package co.edu.itp.svu.repository;

import static co.edu.itp.svu.repository.AttachmentOperationRepository.PhysicalSelection.*;
import static org.assertj.core.api.Assertions.*;

import co.edu.itp.svu.config.ApplicationProperties;
import co.edu.itp.svu.domain.attachment.*;
import co.edu.itp.svu.repository.AttachmentOperationRepository.*;
import co.edu.itp.svu.service.storage.AttachmentRequestIdentity;
import co.edu.itp.svu.service.storage.AttachmentStorage;
import co.edu.itp.svu.service.storage.AttachmentStorage.Failure;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

class AttachmentOperationStandaloneIT {

    private static final String DATABASE = "svu_attachment_manifest_test";
    private static final GenericContainer<?> MONGO = new GenericContainer<>(DockerImageName.parse("mongo:8.0.0"))
        .withExposedPorts(27017)
        .withCommand("mongod", "--bind_ip_all", "--port", "27017")
        .waitingFor(Wait.forLogMessage(".*Waiting for connections.*\\n", 1));

    private static MongoClient client;
    private static MongoTemplate template;
    private static AttachmentOperationRepository repository;

    private static final List<BsonDocument> writeConcerns = new CopyOnWriteArrayList<>();
    private static AttachmentRequestIdentity identity;

    @BeforeAll
    static void startStandaloneMongo() {
        MONGO.start();
        String uri = "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/" + DATABASE;
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        identity = new AttachmentRequestIdentity(key);
        client = MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .addCommandListener(
                    new CommandListener() {
                        @Override
                        public void commandStarted(CommandStartedEvent event) {
                            if (Set.of("findAndModify", "insert").contains(event.getCommandName())) {
                                var concern = event.getCommand().getDocument("writeConcern", new BsonDocument());
                                writeConcerns.add(BsonDocument.parse(concern.toJson()));
                            }
                        }
                    }
                )
                .build()
        );
        template = new MongoTemplate(new SimpleMongoClientDatabaseFactory(client, DATABASE));
        repository = new AttachmentOperationRepository(template);
        Document hello = client.getDatabase(DATABASE).runCommand(new Document("hello", 1));
        assertThat(hello.getString("setName")).isNull();
        assertThat(hello.getBoolean("isreplicaset", false)).isFalse();
        assertThat("isdbgrid".equals(hello.getString("msg"))).isFalse();
        assertThat(hello.getBoolean("isWritablePrimary", false)).isTrue();
        assertThat(hello.containsKey("hosts")).isFalse();
        client.getDatabase(DATABASE).runCommand(new Document("ping", 1));
    }

    @AfterAll
    static void stopStandaloneMongo() {
        if (client != null) client.close();
        MONGO.stop();
    }

    @BeforeEach
    void resetCollection() {
        template.dropCollection(AttachmentOperationRepository.COLLECTION);
        repository.ensureIndexes();
        writeConcerns.clear();
    }

    @Test
    void provesStandaloneWritesReadsUniqueIndexesAndConditionalUpdate() {
        var operation = operation("actor-a", "key-a", "digest-a");
        repository.reserve(operation);
        var loaded = repository.findById(operation.getId()).orElseThrow();
        assertThat(loaded.getOperationType()).isEqualTo(AttachmentOperationType.PUBLIC_CASE_CREATE);
        assertThat(loaded.getState()).isEqualTo(AttachmentOperationState.PREPARING);
        var acquired = acquire(loaded);
        assertThat(repository.filesReady(lease(acquired), List.of())).isPresent();
        assertThat(repository.filesReady(lease(acquired), List.of())).isEmpty();
        assertThatThrownBy(() -> repository.reserve(operation("actor-a", "key-a", "different"))).isInstanceOf(IdempotencyConflict.class);
        assertThat(repository.reserve(operation("actor-b", "key-a", "digest-a")).getId()).isNotEqualTo(loaded.getId());
        var indexes = template.indexOps(AttachmentOperationRepository.COLLECTION).getIndexInfo();
        assertThat(indexes)
            .extracting("name")
            .contains(
                "_id_",
                "uq_attachment_operation_idempotency",
                "ix_attachment_operation_pending",
                "ix_attachment_operation_claim_expiry"
            );
    }

    @Test
    void persistsReservedBsonIdsAndMinimalFileMetadata() {
        var op = operation("actor", "key", "digest");
        op.setCaseId(new org.bson.types.ObjectId());
        var file = new AttachmentOperation.FileEntry();
        file.setMetadataId(new org.bson.types.ObjectId());
        file.setStorageKey(UUID.randomUUID().toString());
        file.setSizeBytes(7L);
        file.setSha256("a".repeat(64));
        file.setState("RESERVED");
        op.setFiles(List.of(file));
        repository.reserve(op);
        Document raw = template.getCollection(AttachmentOperationRepository.COLLECTION).find(new Document("_id", op.getId())).first();
        assertThat(raw.get("case_id")).isInstanceOf(org.bson.types.ObjectId.class);
        assertThat(((Document) ((List<?>) raw.get("files")).get(0)).get("metadata_id")).isInstanceOf(org.bson.types.ObjectId.class);
        assertThat(raw.containsKey("token")).isFalse();
        assertThat(raw.containsKey("contenido")).isFalse();
        assertThat(repository.findPending(Instant.now().plusSeconds(2), 10)).hasSize(1);
        assertThat(repository.findExpiredClaims(10)).isEmpty();
        op.setClaim(new AttachmentOperation.Claim());
        op.getClaim().setOwnerId("worker-synthetic");
        op.getClaim().setLeaseUntil(Instant.now().minusSeconds(5));
        op.getClaim().setFencingToken(3);
        template.save(op, AttachmentOperationRepository.COLLECTION);
        assertThat(repository.findExpiredClaims(10)).extracting(AttachmentOperation::getId).containsExactly(op.getId());
        assertThat(repository.findById(op.getId()).orElseThrow().getFormatVersion()).isEqualTo(1);
    }

    private AttachmentOperation operation(String actor, String key, String digest) {
        var op = new AttachmentOperation();
        op.setId(java.util.UUID.randomUUID().toString());
        op.setFormatVersion(1);
        op.setOperationType(AttachmentOperationType.PUBLIC_CASE_CREATE);
        op.setState(AttachmentOperationState.PREPARING);
        op.setCreatedAt(Instant.now());
        op.setUpdatedAt(Instant.now());
        op.setRevision(0);
        var hashes = identity.derive(List.of(actor), op.getOperationType(), null, key, Map.of("payload", digest), List.of());
        op.setActorScopeHash(hashes.actorScopeHash());
        op.setRequestKeyHash(hashes.requestKeyHash());
        op.setRequestDigest(hashes.requestDigest());
        op.setCaseId(new ObjectId());
        return op;
    }

    private Version version(AttachmentOperation op) {
        return new Version(op.getId(), op.getState(), op.getRevision(), op.getFormatVersion());
    }

    private Lease lease(AttachmentOperation op) {
        return new Lease(version(op), UUID.fromString(op.getClaim().getOwnerId()), op.getClaim().getFencingToken());
    }

    private AttachmentOperation acquire(AttachmentOperation op) {
        return repository.acquire(version(op), UUID.randomUUID(), Duration.ofMinutes(1)).orElseThrow();
    }

    private AttachmentOperation reserve() {
        return repository.reserve(operation("synthetic", "request", "payload"));
    }

    private <T> List<T> race(Supplier<T> left, Supplier<T> right) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CyclicBarrier start = new CyclicBarrier(2);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (var action : List.of(left, right)) {
                futures.add(
                    workers.submit(() -> {
                        start.await(10, TimeUnit.SECONDS);
                        return action.get();
                    })
                );
            }
            List<T> results = new ArrayList<>();
            for (var future : futures) results.add(future.get(20, TimeUnit.SECONDS));
            return results;
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void racesReservationsAndRejectsUnrelatedIdentifierCollisions() throws Exception {
        var other = new AttachmentOperationRepository(template);
        var first = operation("synthetic", "request", "payload");
        var second = operation("synthetic", "request", "payload");
        var results = race(() -> repository.reserve(first), () -> other.reserve(second));
        var winner = results.get(0);
        assertThat(results).extracting(AttachmentOperation::getId).containsOnly(winner.getId());
        assertThat(repository.reserve(first).getId()).isEqualTo(winner.getId());
        assertThat(repository.findByIdentity(first.getActorScopeHash(), first.getOperationType(), first.getRequestKeyHash())).isPresent();
        assertThat(template.getCollection(AttachmentOperationRepository.COLLECTION).countDocuments()).isEqualTo(1);
        var conflict = operation("different", "request", "payload");
        conflict.setId(winner.getId());
        assertThatThrownBy(() -> repository.reserve(conflict)).isInstanceOf(DuplicateKeyException.class);
        var raw = template.getCollection(AttachmentOperationRepository.COLLECTION).find().first();
        assertThat(raw.keySet()).containsExactlyInAnyOrder(
            "_id",
            "format_version",
            "operation_type",
            "state",
            "created_at",
            "updated_at",
            "revision",
            "actor_scope_hash",
            "request_key_hash",
            "request_digest",
            "case_id",
            "files",
            "_class"
        );
        assertThat(raw.getString("request_digest")).matches("[0-9a-f]{64}");
        assertThat(raw.values()).doesNotContain("request", "synthetic", "payload");
    }

    @Test
    void refusesReservationWithoutTheUniquenessGuarantee() {
        template.getCollection(AttachmentOperationRepository.COLLECTION).dropIndex("uq_attachment_operation_idempotency");
        assertThatThrownBy(this::reserve).isInstanceOf(IllegalStateException.class);
        assertThat(template.getCollection(AttachmentOperationRepository.COLLECTION).countDocuments()).isZero();
    }

    @Test
    void concurrentClaimRenewReleaseAndTakeoverFenceOldOwners() throws Exception {
        var root = reserve();
        var other = new AttachmentOperationRepository(template);
        Duration ttl = Duration.ofMinutes(2);
        var results = race(
            () -> repository.acquire(version(root), UUID.randomUUID(), ttl),
            () -> other.acquire(version(root), UUID.randomUUID(), ttl)
        );
        assertThat(results.stream().flatMap(Optional::stream)).hasSize(1);
        var winner = results.stream().flatMap(Optional::stream).findFirst().orElseThrow();
        assertThat(winner.getClaim().getFencingToken()).isEqualTo(1);
        var renewed = repository.renew(lease(winner), Duration.ofMinutes(3)).orElseThrow();
        assertThat(renewed.getClaim().getLeaseUntil()).isAfter(winner.getClaim().getLeaseUntil());
        assertThat(repository.renew(lease(winner), ttl)).isEmpty();
        assertThat(repository.acquire(version(renewed), UUID.randomUUID(), ttl)).isEmpty();
        assertThat(repository.release(new Lease(version(renewed), UUID.randomUUID(), 1))).isEmpty();
        var collection = template.getCollection(AttachmentOperationRepository.COLLECTION);
        collection.updateOne(new Document("_id", root.getId()), new Document("$set", new Document("claim.lease_until", new Date(0))));
        assertThat(repository.findExpiredClaims(10)).hasSize(1);
        assertThat(repository.filesReady(lease(renewed), List.of())).isEmpty();
        assertThat(repository.renew(lease(renewed), ttl)).isEmpty();
        assertThat(repository.release(lease(renewed))).isEmpty();
        var takeover = acquire(renewed);
        assertThat(takeover.getClaim().getFencingToken()).isEqualTo(2);
        assertThat(repository.release(lease(renewed))).isEmpty();
        var forged = new Lease(version(takeover), lease(takeover).owner(), 1);
        assertThat(repository.release(forged)).isEmpty();
        var released = repository.release(lease(takeover)).orElseThrow();
        assertThat(released.getClaim().getOwnerId()).isNull();
        assertThat(acquire(released).getClaim().getFencingToken()).isEqualTo(3);
        assertThat(repository.findExpiredClaims(10)).isEmpty();
    }

    @Test
    void requiresVerifiedFilesAndGuardsTerminalAndManualStates() {
        var op = operation("case", "key", "digest");
        var file = new AttachmentOperation.FileEntry();
        file.setMetadataId(new ObjectId());
        file.setState("RESERVED");
        file.setStorageKey(UUID.randomUUID().toString());
        file.setSha256("a".repeat(64));
        file.setSizeBytes(5L);
        op.setFiles(List.of(file));
        var held = lease(acquire(repository.reserve(op)));
        assertThatThrownBy(() -> repository.beginPersistence(held)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.filesReady(held, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.filesReady(held, List.of(new VerifiedFile(file.getStorageKey(), 5L, "b".repeat(64))))
        ).isInstanceOf(IllegalArgumentException.class);
        var ready = repository.filesReady(held, List.of(new VerifiedFile(file.getStorageKey(), 5L, file.getSha256()))).orElseThrow();
        assertThat(ready.getFiles()).extracting(AttachmentOperation.FileEntry::getState).containsExactly("VERIFIED");
        var persisting = repository.beginPersistence(lease(ready)).orElseThrow();
        assertThatThrownBy(() -> repository.abortBeforePersistence(lease(persisting))).isInstanceOf(IllegalArgumentException.class);
        var manual = repository.requireReconciliation(lease(persisting)).orElseThrow();
        assertThat(repository.findForReconciliation(1)).extracting(AttachmentOperation::getId).containsExactly(manual.getId());
        assertThat(repository.findPending(Instant.now().plusSeconds(60), 10)).isEmpty();
        assertThatThrownBy(() -> acquire(manual)).isInstanceOf(IllegalArgumentException.class);
        for (var blocked : List.of("COMMITTED", "ABORTED", "RECONCILIATION_REQUIRED")) {
            var col = template.getCollection(AttachmentOperationRepository.COLLECTION);
            col.updateOne(
                new Document("_id", op.getId()),
                new Document("$set", new Document("state", blocked).append("claim.lease_until", new Date(0)))
            );
            assertThat(repository.findExpiredClaims(10)).isEmpty();
            assertThat(
                repository.acquire(
                    new Version(op.getId(), AttachmentOperationState.PREPARING, manual.getRevision()),
                    UUID.randomUUID(),
                    Duration.ofMinutes(1)
                )
            ).isEmpty();
        }
    }

    @Test
    void abortRequiresAbsentReservedRootAndLivePrePersistenceRevision() {
        var held = acquire(reserve());
        var roots = template.getCollection("pqrs");
        roots.insertOne(new Document("_id", held.getCaseId()));
        assertThat(repository.abortBeforePersistence(lease(held))).isEmpty();
        var ready = repository.filesReady(lease(held), List.of()).orElseThrow();
        roots.deleteOne(new Document("_id", held.getCaseId()));
        assertThat(repository.abortBeforePersistence(lease(held))).isEmpty();
        var aborted = repository.abortBeforePersistence(lease(ready)).orElseThrow();
        assertThat(aborted.getState()).isEqualTo(AttachmentOperationState.ABORTED);
        assertThatThrownBy(() -> acquire(aborted)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void boundsQueriesAndBreaksDateTiesByIdentifier() {
        Instant date = Instant.EPOCH;
        for (String key : List.of("first", "second", "third")) {
            var op = operation("actor", key, "digest");
            op.setUpdatedAt(date);
            repository.reserve(op);
        }
        template
            .getCollection(AttachmentOperationRepository.COLLECTION)
            .updateMany(new Document(), new Document("$set", new Document("updated_at", Date.from(date))));
        var ids = repository.findPending(date, 100).stream().map(AttachmentOperation::getId).toList();
        assertThat(ids).hasSize(3).isSorted();
        assertThat(repository.findPending(date, 2)).extracting(AttachmentOperation::getId).containsExactlyElementsOf(ids.subList(0, 2));
        for (int limit : List.of(-1, 0, 101)) {
            assertThatThrownBy(() -> repository.findPending(date, limit)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository.findExpiredClaims(limit)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> repository.findForReconciliation(limit)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void lostAcknowledgmentsCanBeInspectedWithoutRepeatingTheMutation() {
        var candidate = operation("synthetic", "request", "payload");
        repository.reserve(candidate); // discard acknowledged insert as if its response had been lost
        var readBack = repository.reserve(operation("synthetic", "request", "payload"));
        UUID owner = UUID.randomUUID();
        repository.acquire(version(readBack), owner, Duration.ofMinutes(1));
        assertThat(repository.acquire(version(readBack), owner, Duration.ofMinutes(1))).isEmpty();
        var claimed = repository.findById(candidate.getId()).orElseThrow();
        assertThat(claimed.getClaim().getOwnerId()).isEqualTo(owner.toString());
        repository.filesReady(lease(claimed), List.of());
        assertThat(repository.filesReady(lease(claimed), List.of())).isEmpty();
        var observed = repository.findById(candidate.getId()).orElseThrow();
        assertThat(observed.getState()).isEqualTo(AttachmentOperationState.FILES_READY);
        assertThat(observed.getRevision()).isEqualTo(2);
        assertThat(observed.getEvidence()).isNull();
    }

    @Test
    void differentDigestsRacingForOneKeyProduceOneConflict() throws Exception {
        var other = new AttachmentOperationRepository(template);
        var results = race(() -> tryReserve(repository, "first"), () -> tryReserve(other, "second"));
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(template.getCollection(AttachmentOperationRepository.COLLECTION).countDocuments()).isOne();
    }

    private boolean tryReserve(AttachmentOperationRepository worker, String payload) {
        try {
            worker.reserve(operation("synthetic", "request", payload));
            return true;
        } catch (IdempotencyConflict conflict) {
            return false;
        }
    }

    @Test
    void responseTargetMustMatchEvenIfTheSuppliedIdentityIsReused() {
        ObjectId target = new ObjectId();
        var original = response(target);
        repository.reserve(original);
        assertThat(repository.reserve(response(target)).getId()).isEqualTo(original.getId());
        var separate = response(new ObjectId());
        assertThat(repository.reserve(separate).getId()).isNotEqualTo(original.getId());
        var misbound = response(target);
        misbound.setCaseId(separate.getCaseId());
        assertThatThrownBy(() -> repository.reserve(misbound)).isInstanceOf(IdempotencyConflict.class);
        var held = acquire(original);
        template.getCollection("respuesta").insertOne(new Document("_id", original.getResponseId()));
        assertThat(repository.abortBeforePersistence(lease(held))).isEmpty();
    }

    private AttachmentOperation response(ObjectId target) {
        var candidate = operation("synthetic", "request", "payload");
        candidate.setOperationType(AttachmentOperationType.CITIZEN_RESPONSE_CREATE);
        candidate.setResponseId(new ObjectId());
        candidate.setCaseId(target);
        var protectedIdentity = identity.derive(
            List.of("synthetic"),
            candidate.getOperationType(),
            target,
            "request",
            Map.of("field", "payload"),
            List.of()
        );
        candidate.setActorScopeHash(protectedIdentity.actorScopeHash());
        candidate.setRequestKeyHash(protectedIdentity.requestKeyHash());
        candidate.setRequestDigest(protectedIdentity.requestDigest());
        return candidate;
    }

    @Test
    void damagedOrExhaustedClaimCannotRestartItsGeneration() {
        var held = acquire(reserve());
        var collection = template.getCollection(AttachmentOperationRepository.COLLECTION);
        collection.updateOne(
            new Document("_id", held.getId()),
            new Document("$set", new Document("claim.lease_until", new Date(0)).append("claim.fencing_token", Long.MAX_VALUE))
        );
        assertThat(repository.acquire(version(held), UUID.randomUUID(), Duration.ofMinutes(1))).isEmpty();
        collection.updateOne(new Document("_id", held.getId()), new Document("$unset", new Document("claim", 1)));
        assertThat(repository.acquire(version(held), UUID.randomUUID(), Duration.ofMinutes(1))).isEmpty();
    }

    @Test
    void standaloneAcknowledgesJournaledManifestCommands() {
        Document status = template.executeCommand(new Document("serverStatus", 1));
        assertThat(status.get("storageEngine", Document.class).getString("name")).isEqualTo("wiredTiger");
        var held = acquire(reserve());
        assertThat(repository.release(lease(held))).isPresent();
        assertThat(writeConcerns)
            .hasSize(3)
            .allSatisfy(wc -> {
                assertThat(wc.getNumber("w").intValue()).isOne();
                assertThat(wc.getBoolean("j").getValue()).isTrue();
                assertThat(wc.getNumber("wtimeout").longValue()).isEqualTo(10_000L);
            });
    }

    private AttachmentOperation v2(String key, int count) {
        var operation = operation("synthetic", key, "payload");
        operation.setFormatVersion(2);
        operation.setLogicalFiles(
            java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new AttachmentOperation.LogicalFile(UUID.randomUUID().toString(), new ObjectId(), i, "a".repeat(64)))
                .toList()
        );
        return operation;
    }

    private AttachmentWriterCapability writer(AttachmentOperation op) {
        return new AttachmentWriterCapability(UUID.fromString(op.getId()), op.getClaim().getFencingToken());
    }

    private com.mongodb.client.MongoCollection<Document> manifests() {
        return template.getCollection(AttachmentOperationRepository.COLLECTION);
    }

    private Document raw(AttachmentOperation op) {
        return manifests().find(new Document("_id", op.getId())).first();
    }

    @Test
    void persistsV2BsonAndKeepsLogicalFilesStableAcrossGenerations() {
        var held = acquire(repository.reserve(v2("bson", 2)));
        var reserved = repository.reservePhysicalGeneration(lease(held), writer(held)).orElseThrow();
        var raw = raw(reserved);
        var logical = raw.getList("files", Document.class).get(0);
        var batch = raw.getList("physical_generations", Document.class).get(0);
        var entry = batch.getList("entries", Document.class).get(0);
        assertThat(raw.get("format_version")).isInstanceOf(Integer.class).isEqualTo(2);
        assertThat(raw.get("revision")).isInstanceOf(Long.class);
        assertThat(logical.keySet()).containsExactly("logical_id", "metadata_id", "expected_size_bytes", "expected_sha256");
        assertThat(logical.get("metadata_id")).isInstanceOf(ObjectId.class);
        assertThat(logical.get("expected_size_bytes")).isInstanceOf(Long.class);
        assertThat(batch.keySet()).containsExactly("generation", "reserved_at", "writer_proof_hash", "state", "entries");
        assertThat(batch.get("generation")).isInstanceOf(Long.class);
        assertThat(batch.get("reserved_at")).isInstanceOf(Date.class);
        assertThat(batch.getString("writer_proof_hash")).matches("[0-9a-f]{64}");
        assertThat(entry.keySet()).containsExactly("logical_id", "storage_key");
        assertThat(reserved.getPhysicalGenerations())
            .singleElement()
            .satisfies(generation -> {
                assertThat(generation.reservedAt()).isNotNull();
                assertThat(generation.state()).isEqualTo(AttachmentOperation.GenerationState.RESERVED);
                assertThat(generation.entries())
                    .extracting(AttachmentOperation.PhysicalEntry::logicalId)
                    .containsExactlyElementsOf(held.getLogicalFiles().stream().map(AttachmentOperation.LogicalFile::logicalId).toList());
            });
        assertThat(reserved.getSelectedPhysicalGeneration()).isNull();
        var next = acquire(repository.release(lease(reserved)).orElseThrow());
        var two = repository.reservePhysicalGeneration(lease(next), writer(next)).orElseThrow();
        assertThat(two.getLogicalFiles()).isEqualTo(held.getLogicalFiles());
        assertThat(
            two
                .getPhysicalGenerations()
                .stream()
                .flatMap(generation -> generation.entries().stream())
                .map(AttachmentOperation.PhysicalEntry::storageKey)
        )
            .hasSize(4)
            .doesNotHaveDuplicates();
    }

    @Test
    void racesAcrossFormatsAndRetainsTheWinnerAndItsOriginalShape() throws Exception {
        var legacy = operation("synthetic", "shared", "payload");
        var modern = v2("shared", 0);
        var results = race(() -> repository.reserve(legacy), () -> new AttachmentOperationRepository(template).reserve(modern));
        var winner = results.get(0);
        assertThat(results).extracting(AttachmentOperation::getId).containsOnly(winner.getId());
        assertThat(repository.reserve(legacy).getFormatVersion()).isEqualTo(winner.getFormatVersion());
        assertThat(repository.reserve(modern).getFormatVersion()).isEqualTo(winner.getFormatVersion());
        assertThat(manifests().countDocuments()).isOne();
        assertThatThrownBy(() -> repository.reserve(operation("synthetic", "shared", "changed"))).isInstanceOf(IdempotencyConflict.class);
        assertThat(raw(winner).containsKey("physical_generations")).isEqualTo(winner.getFormatVersion() == 2);
    }

    @Test
    void unknownFormatsBlockDuplicatesAndStayOutOfTypedQueues() {
        var candidate = v2("unknown", 0);
        var held = acquire(repository.reserve(candidate));
        manifests()
            .updateOne(
                new Document("_id", held.getId()),
                new Document(
                    "$set",
                    new Document("format_version", 2L).append("claim.lease_until", new Date(0)).append("files", "unknown shape")
                )
            );
        assertThat(repository.findUnsupportedFormats(10)).containsExactly(new Document("_id", held.getId()).append("format_version", 2L));
        assertThat(repository.findPending(Instant.now().plusSeconds(60), 10)).isEmpty();
        assertThat(repository.findExpiredClaims(10)).isEmpty();
        assertThatThrownBy(() -> repository.findById(held.getId())).isInstanceOf(UnsupportedManifestFormat.class);
        assertThatThrownBy(() -> repository.reserve(candidate)).isInstanceOf(UnsupportedManifestFormat.class);
        assertThat(repository.reservePhysicalGeneration(lease(held), writer(held))).isEmpty();
        assertThat(manifests().countDocuments()).isOne();
    }

    @Test
    void racesIndependentWritersAndConfirmsLostSuccessfulResponseWithOriginalCapability() throws Exception {
        var held = acquire(repository.reserve(v2("race", 2)));
        var first = writer(held);
        var second = writer(held);
        var other = new AttachmentOperationRepository(template);
        var results = race(
            () -> repository.reservePhysicalGeneration(lease(held), first),
            () -> other.reservePhysicalGeneration(lease(held), second)
        );
        assertThat(results.stream().flatMap(Optional::stream)).hasSize(1);
        // Read after intentionally discarding the winner's acknowledged response. No network fault is injected.
        var observed = repository.findById(held.getId()).orElseThrow();
        var winning = results.get(0).isPresent() ? first : second;
        var losing = results.get(0).isPresent() ? second : first;
        assertThat(repository.confirmPhysicalReservation(lease(observed), losing)).isEmpty();
        assertThat(other.confirmPhysicalReservation(lease(observed), writer(observed))).isEmpty();
        var confirmed = other.confirmPhysicalReservation(lease(observed), winning).orElseThrow();
        assertThat(confirmed.getRevision()).isEqualTo(observed.getRevision() + 1);
        assertThat(confirmed.getPhysicalGenerations()).isEqualTo(observed.getPhysicalGenerations());
        assertThat(repository.findPhysicalGeneration(held.getId(), lease(held).fencing())).contains(
            confirmed.getPhysicalGenerations().get(0)
        );
        var retried = repository.reservePhysicalGeneration(lease(confirmed), winning).orElseThrow();
        assertThat(retried.getPhysicalGenerations()).isEqualTo(confirmed.getPhysicalGenerations());
        assertThat(writeConcerns.get(writeConcerns.size() - 1).getBoolean("j").getValue()).isTrue();
    }

    @Test
    void rejectsWrongOwnerStaleRevisionStaleFenceAndExpiredLease() {
        var held = acquire(repository.reserve(v2("stale", 0)));
        var original = lease(held);
        var capability = writer(held);
        assertThat(
            repository.reservePhysicalGeneration(new Lease(version(held), UUID.randomUUID(), original.fencing()), capability)
        ).isEmpty();
        var renewed = repository.renew(original, Duration.ofMinutes(1)).orElseThrow();
        assertThat(repository.reservePhysicalGeneration(original, capability)).isEmpty();
        assertThat(renewed.getClaim().getFencingToken()).isEqualTo(original.fencing());
        manifests().updateOne(new Document("_id", held.getId()), new Document("$set", new Document("claim.lease_until", new Date(0))));
        assertThat(repository.reservePhysicalGeneration(lease(renewed), capability)).isEmpty();
        var next = acquire(renewed);
        assertThat(
            repository.reservePhysicalGeneration(new Lease(version(next), lease(next).owner(), original.fencing()), capability)
        ).isEmpty();
        assertThat(next.getPhysicalGenerations()).isEmpty();
        assertThat(repository.filesReady(lease(next), List.of())).isEmpty();
    }

    @Test
    void sixthReservationReconcilesWithoutDiscardingEvidence() {
        var held = acquire(repository.reserve(v2("five", 1)));
        List<AttachmentOperation.PhysicalGeneration> saved = List.of();
        for (int i = 1; i <= 5; i++) {
            var reserved = repository.reservePhysicalGeneration(lease(held), writer(held)).orElseThrow();
            saved = reserved.getPhysicalGenerations();
            assertThat(saved).hasSize(i);
            assertThat(reserved.getState()).isEqualTo(AttachmentOperationState.PREPARING);
            held = acquire(repository.release(lease(reserved)).orElseThrow());
        }
        var manual = repository.reservePhysicalGeneration(lease(held), writer(held)).orElseThrow();
        assertThat(manual.getState()).isEqualTo(AttachmentOperationState.RECONCILIATION_REQUIRED);
        assertThat(manual.getPhysicalGenerations()).isEqualTo(saved);
        assertThat(repository.findForReconciliation(10)).extracting(AttachmentOperation::getId).containsExactly(manual.getId());
    }

    private AttachmentOperation responseWithPaddedId(String key, int bytes) {
        var candidate = v2(key, 1);
        candidate.setOperationType(AttachmentOperationType.CITIZEN_RESPONSE_CREATE);
        candidate.setResponseId(new ObjectId());
        candidate.setCaseId("x".repeat(bytes));
        return candidate;
    }

    @Test
    void enforcesExactBsonBudgetAndLeavesRoomForReconciliation() {
        var probe = acquire(repository.reserve(responseWithPaddedId("probe", 1)));
        var reserved = repository.reservePhysicalGeneration(lease(probe), writer(probe)).orElseThrow();
        int paddedSize =
            AttachmentOperationRepository.MAX_MANIFEST_BYTES -
            repository.bsonSize(AttachmentOperationRepository.completionEnvelope(raw(reserved))) +
            1;
        var boundary = acquire(repository.reserve(responseWithPaddedId("exact", paddedSize)));
        var exact = repository.reservePhysicalGeneration(lease(boundary), writer(boundary)).orElseThrow();
        assertThat(repository.bsonSize(AttachmentOperationRepository.completionEnvelope(raw(exact)))).isEqualTo(1_048_576);
        var second = acquire(repository.release(lease(exact)).orElseThrow());
        var manual = repository.reservePhysicalGeneration(lease(second), writer(second)).orElseThrow();
        assertThat(manual.getState()).isEqualTo(AttachmentOperationState.RECONCILIATION_REQUIRED);
        assertThat(manual.getPhysicalGenerations()).isEqualTo(exact.getPhysicalGenerations());
        assertThat(repository.bsonSize(raw(manual))).isLessThanOrEqualTo(1_048_576);
        var overflow = acquire(repository.reserve(responseWithPaddedId("overflow", paddedSize + 1)));
        var rejected = repository.reservePhysicalGeneration(lease(overflow), writer(overflow)).orElseThrow();
        assertThat(rejected.getState()).isEqualTo(AttachmentOperationState.RECONCILIATION_REQUIRED);
        assertThat(rejected.getPhysicalGenerations()).isEmpty();
        assertThat(rejected.getLastFailure()).isEqualTo("PHYSICAL_MANIFEST_BUDGET");
    }

    @Test
    void renewedClaimsPreserveAlreadySelectedGenerationFixtures() {
        for (var state : List.of(AttachmentOperationState.FILES_READY, AttachmentOperationState.PERSISTING)) {
            var held = acquire(repository.reserve(v2(state.name(), 0)));
            var reserved = repository.reservePhysicalGeneration(lease(held), writer(held)).orElseThrow();
            // Synthetic BSON fixture for future states. No physical verification or selection API is called.
            manifests()
                .updateOne(
                    new Document("_id", held.getId()),
                    new Document(
                        "$set",
                        new Document("state", state.name())
                            .append("physical_generations.0.state", "VERIFIED")
                            .append("selected_physical_generation", 1L)
                            .append("claim.lease_until", new Date(0))
                    )
                );
            var next = acquire(repository.findById(reserved.getId()).orElseThrow());
            assertThat(next.getClaim().getFencingToken()).isEqualTo(2L);
            assertThat(next.getSelectedPhysicalGeneration()).isEqualTo(1L);
            assertThat(next.getPhysicalGenerations()).hasSize(1);
            assertThat(repository.beginPersistence(lease(next))).isEmpty();
        }
    }

    @Test
    void aPersistedProofCannotBeReplayedAsTheSecretAndLoggingRedactsPrivateBytes() throws Exception {
        var held = acquire(repository.reserve(v2("redacted", 1)));
        var winner = writer(held);
        var secret = AttachmentWriterCapability.class.getDeclaredField("secret");
        secret.setAccessible(true);
        byte[] bytes = (byte[]) secret.get(winner);
        String hex = HexFormat.of().formatHex(bytes);
        String base64 = Base64.getEncoder().encodeToString(bytes);

        var env = new org.springframework.mock.env.MockEnvironment();
        env.setActiveProfiles("dev");
        var proxyFactory = new org.springframework.aop.aspectj.annotation.AspectJProxyFactory(repository);
        proxyFactory.addAspect(new co.edu.itp.svu.aop.logging.LoggingAspect(env));
        AttachmentOperationRepository proxy = proxyFactory.getProxy();
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(AttachmentOperationRepository.class);
        var level = logger.getLevel();
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        try {
            var stored = proxy.reservePhysicalGeneration(lease(held), winner).orElseThrow();
            var proof = stored.getPhysicalGenerations().get(0).writerProofHash();
            var replay = writer(held);
            replay.prepareKeys(1);
            System.arraycopy(HexFormat.of().parseHex(proof), 0, secret.get(replay), 0, 32);
            assertThat(proxy.confirmPhysicalReservation(lease(stored), replay)).isEmpty();
            assertThatThrownBy(() ->
                proxy.confirmPhysicalReservation(lease(stored), new AttachmentWriterCapability(UUID.randomUUID(), lease(stored).fencing()))
            ).hasMessage("Writer capability belongs to another reservation");
            assertThat(raw(stored).toJson()).doesNotContain(hex, base64, Arrays.toString(bytes));
            assertThat(appender.list.stream().map(event -> event.getFormattedMessage()).toList())
                .anySatisfy(message -> assertThat(message).contains("AttachmentWriterCapability[redacted]"))
                .allSatisfy(message -> assertThat(message).doesNotContain(hex, base64, Arrays.toString(bytes)));
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(level);
            appender.stop();
        }
    }

    @Test
    void v2CannotBypassSelectionThroughLegacyEntryPoints() {
        var held = acquire(repository.reserve(v2("no-selection", 0)));
        var reserved = repository.reservePhysicalGeneration(lease(held), writer(held)).orElseThrow();
        assertThat(repository.filesReady(lease(reserved), List.of())).isEmpty();
        assertThat(
            repository.filesReady(
                new Lease(
                    new Version(reserved.getId(), reserved.getState(), reserved.getRevision()),
                    lease(reserved).owner(),
                    lease(reserved).fencing()
                ),
                List.of()
            )
        ).isEmpty();
        assertThat(raw(reserved)).doesNotContainKey("selected_physical_generation");
        // Invalid future-shaped record tests the persistence guard without selecting a physical batch.
        manifests().updateOne(new Document("_id", reserved.getId()), new Document("$set", new Document("state", "FILES_READY")));
        var invalid = repository.findById(reserved.getId()).orElseThrow();
        assertThat(repository.beginPersistence(lease(invalid))).isEmpty();
        assertThat(raw(invalid).getString("state")).isEqualTo("FILES_READY");
    }

    private static final byte[] PHYSICAL_BYTES = { 7, 2, 4 };

    private AttachmentStorage physicalStorage(Path root) {
        var properties = new ApplicationProperties();
        properties.getFile().setUploadDir(root.toString());
        return new AttachmentStorage(properties, "50MB", "50MB");
    }

    private AttachmentOperation logicalBatch(String key, byte[]... files) throws Exception {
        var op = v2(key, 0);
        var logical = new ArrayList<AttachmentOperation.LogicalFile>();
        var digest = MessageDigest.getInstance("SHA-256");
        for (byte[] bytes : files) {
            logical.add(
                new AttachmentOperation.LogicalFile(
                    UUID.randomUUID().toString(),
                    new ObjectId(),
                    bytes.length,
                    HexFormat.of().formatHex(digest.digest(bytes))
                )
            );
        }
        op.setLogicalFiles(logical);
        return op;
    }

    private void expire(String id) {
        manifests().updateOne(new Document("_id", id), new Document("$set", new Document("claim.lease_until", new Date(0))));
    }

    private void assertStillPreparing(String id) {
        var operation = repository.findById(id).orElseThrow();
        assertThat(operation.getState()).isEqualTo(AttachmentOperationState.PREPARING);
        assertThat(operation.getSelectedPhysicalGeneration()).isNull();
        assertThat(operation.getPhysicalGenerations()).allSatisfy(batch -> {
            assertThat(batch.state()).isEqualTo(AttachmentOperation.GenerationState.RESERVED);
            assertThat(batch.entries()).allSatisfy(entry -> {
                assertThat(entry.observedSizeBytes()).isNull();
                assertThat(entry.observedSha256()).isNull();
                assertThat(entry.verifiedAt()).isNull();
            });
        });
    }

    private Optional<AttachmentStorage.PreparedGeneration> prepareOnce(AttachmentStorage storage, PhysicalWriteContext context) {
        try {
            return Optional.of(
                storage.prepare(context, Map.of(context.files().get(0).logicalId(), new ByteArrayInputStream(PHYSICAL_BYTES)))
            );
        } catch (AttachmentStorage.StorageException ex) {
            assertThat(ex.failure()).isEqualTo(Failure.CONTEXT_CONSUMED);
            return Optional.empty();
        }
    }

    @Test
    void journaledReservationAndWinningCapabilityAreRequiredBeforeOnePhysicalContext(@TempDir Path root) throws Exception {
        var held = acquire(repository.reserve(logicalBatch("write-once", PHYSICAL_BYTES)));
        var winning = writer(held);
        assertThat(repository.authorizePhysicalWrite(lease(held), winning)).isEmpty();
        try (var files = Files.list(root)) {
            assertThat(files).isEmpty();
        }
        var reserved = repository.reservePhysicalGeneration(lease(held), winning).orElseThrow();
        var other = new AttachmentOperationRepository(template);
        assertThat(other.authorizePhysicalWrite(lease(reserved), writer(held))).isEmpty();
        int previousCommands = writeConcerns.size();
        var authorizations = race(
            () -> repository.authorizePhysicalWrite(lease(reserved), winning),
            () -> other.authorizePhysicalWrite(lease(reserved), winning)
        );
        assertThat(authorizations.stream().flatMap(Optional::stream)).hasSize(1);
        var context = authorizations.stream().flatMap(Optional::stream).findFirst().orElseThrow();
        assertThat(context.lease().version().revision()).isEqualTo(reserved.getRevision() + 1);
        assertThat(writeConcerns.subList(previousCommands, writeConcerns.size()))
            .singleElement()
            .satisfies(concern -> {
                assertThat(concern.getNumber("w").intValue()).isEqualTo(1);
                assertThat(concern.getBoolean("j").getValue()).isTrue();
                assertThat(concern.getNumber("wtimeout").longValue()).isEqualTo(10_000);
            });
        assertThat(context.toString()).isEqualTo("PhysicalWriteContext[redacted]");
        assertThat(other.authorizePhysicalWrite(context.lease(), winning)).isEmpty();

        var storage = physicalStorage(root);
        var results = race(() -> prepareOnce(storage, context), () -> prepareOnce(storage, context));
        assertThat(results.stream().flatMap(Optional::stream)).hasSize(1);
        var evidence = results.stream().flatMap(Optional::stream).findFirst().orElseThrow();
        assertThat(evidence.lease()).isEqualTo(context.lease());
        assertThat(evidence.files()).hasSize(1);
        assertThat(Files.readAllBytes(root.resolve(evidence.files().get(0).storageKey()))).containsExactly(PHYSICAL_BYTES);
        assertStillPreparing(held.getId());

        var legacy = acquire(repository.reserve(operation("legacy", "no-physical", "example")));
        assertThatThrownBy(() -> repository.authorizePhysicalWrite(lease(legacy), writer(legacy))).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    void expiredContextCannotOpenDestination(@TempDir Path root) throws Exception {
        var held = acquire(repository.reserve(logicalBatch("expired-before-write", PHYSICAL_BYTES)));
        var capability = writer(held);
        var reserved = repository.reservePhysicalGeneration(lease(held), capability).orElseThrow();
        var context = repository.authorizePhysicalWrite(lease(reserved), capability).orElseThrow();
        expire(held.getId());
        assertThatThrownBy(() ->
            physicalStorage(root).prepare(context, Map.of(context.files().get(0).logicalId(), new ByteArrayInputStream(PHYSICAL_BYTES)))
        ).isInstanceOfSatisfying(AttachmentStorage.StorageException.class, ex -> assertThat(ex.failure()).isEqualTo(Failure.CLAIM_LOST));
        try (var entries = Files.list(root)) {
            assertThat(entries).isEmpty();
        }
        assertStillPreparing(held.getId());
    }

    @Test
    void replacementGenerationWritesWhileOldDescriptorRemainsOpenAndOldEvidenceIsRejected(@TempDir Path root) throws Exception {
        var held = acquire(repository.reserve(logicalBatch("parallel-generations", PHYSICAL_BYTES)));
        var capability = writer(held);
        var reserved = repository.reservePhysicalGeneration(lease(held), capability).orElseThrow();
        var stale = repository.authorizePhysicalWrite(lease(reserved), capability).orElseThrow();
        var opened = new CountDownLatch(1);
        var resume = new CountDownLatch(1);
        var blocking = new ByteArrayInputStream(PHYSICAL_BYTES) {
            @Override
            public int read(byte[] buffer, int offset, int length) {
                opened.countDown();
                try {
                    if (!resume.await(15, TimeUnit.SECONDS)) throw new AssertionError("Writer was not released");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(ex);
                }
                return super.read(buffer, offset, length);
            }
        };
        var storage = physicalStorage(root);
        var thread = Executors.newSingleThreadExecutor();
        try {
            var pending = thread.submit(() -> storage.prepare(stale, Map.of(stale.files().get(0).logicalId(), blocking)));
            assertThat(opened.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(pending.isDone()).isFalse();
            expire(held.getId());
            var takeover = acquire(repository.findById(held.getId()).orElseThrow());
            var replacement = writer(takeover);
            var next = repository.reservePhysicalGeneration(lease(takeover), replacement).orElseThrow();
            var context = repository.authorizePhysicalWrite(lease(next), replacement).orElseThrow();
            var evidence = storage.prepare(context, Map.of(context.files().get(0).logicalId(), new ByteArrayInputStream(PHYSICAL_BYTES)));
            String oldKey = stale.files().get(0).storageKey();
            String newKey = evidence.files().get(0).storageKey();
            assertThat(newKey).isNotEqualTo(oldKey);
            assertThat(pending.isDone()).isFalse();
            assertThat(Files.size(root.resolve(oldKey))).isZero();
            resume.countDown();
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(AttachmentStorage.StorageException.class)
                .hasRootCauseMessage("CLAIM_LOST");
            assertThat(Files.readAllBytes(root.resolve(oldKey))).containsExactly(PHYSICAL_BYTES);
            assertThat(Files.readAllBytes(root.resolve(newKey))).containsExactly(PHYSICAL_BYTES);
            assertStillPreparing(held.getId());
        } finally {
            resume.countDown();
            thread.shutdownNow();
        }
    }

    @Test
    void collisionAcrossOperationsDoesNotAdoptExistingIdenticalBytes(@TempDir Path root) throws Exception {
        var first = acquire(repository.reserve(logicalBatch("collision-first", PHYSICAL_BYTES)));
        var firstCap = writer(first);
        var firstReserved = repository.reservePhysicalGeneration(lease(first), firstCap).orElseThrow();
        var firstContext = repository.authorizePhysicalWrite(lease(firstReserved), firstCap).orElseThrow();
        var storage = physicalStorage(root);
        var prior = prepareOnce(storage, firstContext).orElseThrow();

        var second = acquire(repository.reserve(logicalBatch("collision-second", PHYSICAL_BYTES)));
        var secondCap = writer(second);
        // Force a UUID collision for the test. Both manifests still reserve and confirm through MongoDB.
        var keys = AttachmentWriterCapability.class.getDeclaredField("keys");
        keys.setAccessible(true);
        UUID collision = UUID.fromString(prior.files().get(0).storageKey());
        keys.set(secondCap, List.of(collision));
        var secondReserved = repository.reservePhysicalGeneration(lease(second), secondCap).orElseThrow();
        var secondContext = repository.authorizePhysicalWrite(lease(secondReserved), secondCap).orElseThrow();
        assertThatThrownBy(() ->
            storage.prepare(secondContext, Map.of(secondContext.files().get(0).logicalId(), new ByteArrayInputStream(PHYSICAL_BYTES)))
        ).isInstanceOfSatisfying(AttachmentStorage.StorageException.class, ex -> assertThat(ex.failure()).isEqualTo(Failure.COLLISION));
        assertThat(Files.readAllBytes(root.resolve(collision.toString()))).containsExactly(PHYSICAL_BYTES);
        assertStillPreparing(first.getId());
        assertStillPreparing(second.getId());
    }

    @Test
    void idempotencyAcrossVersionsRequiresIdenticalOrderedFileFingerprints() throws Exception {
        var v2 = logicalBatch("cross-format-files", new byte[] { 1 }, new byte[] { 2 });
        var v1 = operation("synthetic", "cross-format-files", "payload");
        v1.setFiles(
            v2
                .getLogicalFiles()
                .stream()
                .map(logical -> {
                    var entry = new AttachmentOperation.FileEntry();
                    entry.setMetadataId(new ObjectId());
                    entry.setStorageKey(UUID.randomUUID().toString());
                    entry.setSizeBytes(logical.expectedSizeBytes());
                    entry.setSha256(logical.expectedSha256());
                    entry.setState("RESERVED");
                    return entry;
                })
                .toList()
        );
        var stored = repository.reserve(v1);
        assertThat(repository.reserve(v2).getId()).isEqualTo(stored.getId());
        var files = v2.getLogicalFiles();
        v2.setLogicalFiles(List.of(files.get(1), files.get(0)));
        assertThatThrownBy(() -> repository.reserve(v2)).isInstanceOf(IdempotencyConflict.class);
        var original = files.get(0);
        v2.setLogicalFiles(
            List.of(
                new AttachmentOperation.LogicalFile(
                    original.logicalId(),
                    original.metadataId(),
                    original.expectedSizeBytes(),
                    "0".repeat(64)
                ),
                files.get(1)
            )
        );
        assertThatThrownBy(() -> repository.reserve(v2)).isInstanceOf(IdempotencyConflict.class);
        assertThat(manifests().countDocuments()).isOne();
    }

    private record Prepared(AttachmentStorage.PreparedGeneration evidence, AttachmentWriterCapability writer) {}

    private Prepared prepareForSelection(AttachmentStorage storage, String key, byte[]... content) throws Exception {
        var acquired = acquire(repository.reserve(logicalBatch(key, content)));
        return prepareForSelection(storage, acquired, content);
    }

    private Prepared prepareForSelection(AttachmentStorage storage, AttachmentOperation acquired, byte[]... content) throws Exception {
        var writer = writer(acquired);
        var reserved = repository.reservePhysicalGeneration(lease(acquired), writer).orElseThrow();
        var context = repository.authorizePhysicalWrite(lease(reserved), writer).orElseThrow();
        var sources = new HashMap<String, ByteArrayInputStream>();
        for (int i = 0; i < content.length; i++) sources.put(context.files().get(i).logicalId(), new ByteArrayInputStream(content[i]));
        return new Prepared(storage.prepare(context, sources), writer);
    }

    @Test
    void selectsSingleMultipleAndEmptyPhysicalBatchesWithExactBsonTypes(@TempDir Path root) throws Exception {
        var storage = physicalStorage(root);
        var selector = new AttachmentOperationRepository(template, storage);
        for (byte[][] bytes : List.of(new byte[][] { PHYSICAL_BYTES }, new byte[][] { PHYSICAL_BYTES, {}, { 1 } }, new byte[][] {})) {
            var prepared = prepareForSelection(storage, "selection-" + bytes.length, bytes);
            var claim = prepared.evidence().lease();
            int writes = writeConcerns.size();
            assertThat(selector.selectPhysicalGeneration(prepared.evidence(), prepared.writer())).isEqualTo(SELECTED);
            assertThat(writeConcerns.subList(writes, writeConcerns.size()))
                .singleElement()
                .satisfies(concern -> {
                    assertThat(concern.getNumber("w").intValue()).isOne();
                    assertThat(concern.getBoolean("j").getValue()).isTrue();
                    assertThat(concern.getNumber("wtimeout").longValue()).isEqualTo(10_000L);
                });
            var selected = repository.findById(claim.version().id()).orElseThrow();
            var raw = raw(selected);
            var batch = raw.getList("physical_generations", Document.class).get(0);
            assertThat(raw.get("selected_physical_generation")).isEqualTo(1L);
            assertThat(raw.get("revision")).isEqualTo(claim.version().revision() + 1);
            assertThat(raw.get("state")).isEqualTo("FILES_READY");
            assertThat(batch.get("state")).isEqualTo("VERIFIED");
            var entries = batch.getList("entries", Document.class);
            assertThat(entries).hasSize(bytes.length);
            for (int i = 0; i < bytes.length; i++) {
                var actual = prepared.evidence().files().get(i);
                var entry = entries.get(i);
                assertThat(entry.get("observed_size_bytes")).isEqualTo(actual.size());
                assertThat(entry.get("observed_sha256")).isEqualTo(actual.sha256());
                assertThat(entry.get("verified_at")).isInstanceOf(Date.class).isEqualTo(raw.get("updated_at"));
                assertThat(entry.get("logical_id")).isEqualTo(actual.logicalId());
                assertThat(entry.get("storage_key")).isEqualTo(actual.storageKey());
                assertThat(Files.readAllBytes(root.resolve(actual.storageKey()))).containsExactly(bytes[i]);
            }
            assertThat(repository.beginPersistence(lease(selected))).isEmpty();
            assertThatThrownBy(() -> repository.reservePhysicalGeneration(lease(selected), prepared.writer())).isInstanceOf(
                IllegalArgumentException.class
            );
        }
    }

    @Test
    void rejectsOtherStorageOtherOperationRawEvidenceAndWrongCapability(@TempDir Path root) throws Exception {
        var storage = physicalStorage(root.resolve("configured"));
        var foreign = physicalStorage(Files.createDirectory(root.resolve("foreign")));
        Files.createDirectory(root.resolve("configured"));
        var foreignPrepared = prepareForSelection(foreign, "foreign-volume", PHYSICAL_BYTES);
        var selector = new AttachmentOperationRepository(template, storage);
        assertThat(selector.selectPhysicalGeneration(foreignPrepared.evidence(), foreignPrepared.writer())).isEqualTo(CONFLICT);

        var prepared = prepareForSelection(storage, "own-volume", PHYSICAL_BYTES);
        var claim = prepared.evidence().lease();
        var unauthorized = new AttachmentWriterCapability(UUID.fromString(claim.version().id()), claim.fencing());
        assertThat(selector.selectPhysicalGeneration(prepared.evidence(), unauthorized)).isEqualTo(CONFLICT);
        assertThat(selector.selectPhysicalGeneration(prepared.evidence(), foreignPrepared.writer())).isEqualTo(CONFLICT);
        assertThat(selector.selectPhysicalGeneration(new PhysicalPreparationVerifier.Evidence() {}, prepared.writer())).isEqualTo(CONFLICT);
        var reservedOnly = acquire(repository.reserve(logicalBatch("confirmed-only", PHYSICAL_BYTES)));
        var reservedWriter = writer(reservedOnly);
        var reserved = repository.reservePhysicalGeneration(lease(reservedOnly), reservedWriter).orElseThrow();
        assertThat(repository.confirmPhysicalReservation(lease(reserved), reservedWriter)).isPresent();
        assertThat(selector.selectPhysicalGeneration(null, reservedWriter)).isEqualTo(CONFLICT);

        String proof = repository.findPhysicalGeneration(claim.version().id(), claim.fencing()).orElseThrow().writerProofHash();
        var field = AttachmentWriterCapability.class.getDeclaredField("secret");
        field.setAccessible(true);
        System.arraycopy(HexFormat.of().parseHex(proof), 0, field.get(unauthorized), 0, 32);
        assertThat(selector.selectPhysicalGeneration(prepared.evidence(), unauthorized)).isEqualTo(CONFLICT);
        assertStillPreparing(claim.version().id());
        assertStillPreparing(foreignPrepared.evidence().lease().version().id());
    }

    @Test
    void rejectsRenewedReleasedExpiredReassignedAndWrongOwnerClaims(@TempDir Path root) throws Exception {
        var storage = physicalStorage(root);
        var selector = new AttachmentOperationRepository(template, storage);
        for (String change : List.of("renew", "confirm", "release", "expire", "reassign", "wrong-owner")) {
            var prepared = prepareForSelection(storage, change, PHYSICAL_BYTES);
            var claim = prepared.evidence().lease();
            String id = claim.version().id();
            switch (change) {
                case "renew" -> assertThat(repository.renew(claim, Duration.ofMinutes(2))).isPresent();
                case "confirm" -> assertThat(repository.confirmPhysicalReservation(claim, prepared.writer())).isPresent();
                case "release" -> assertThat(repository.release(claim)).isPresent();
                case "expire" -> expire(id);
                case "reassign" -> {
                    expire(id);
                    assertThat(acquire(repository.findById(id).orElseThrow()).getClaim().getFencingToken()).isEqualTo(claim.fencing() + 1);
                }
                case "wrong-owner" -> manifests()
                    .updateOne(new Document("_id", id), new Document("$set", new Document("claim.owner_id", UUID.randomUUID().toString())));
                default -> throw new AssertionError(change);
            }
            assertThat(selector.selectPhysicalGeneration(prepared.evidence(), prepared.writer())).isEqualTo(CLAIM_LOST);
            assertStillPreparing(id);
            assertThat(Files.readAllBytes(root.resolve(prepared.evidence().files().get(0).storageKey()))).containsExactly(PHYSICAL_BYTES);
        }
    }

    @Test
    void preservesEarlierAttemptsAndRecoversLostAcknowledgmentWithoutReplacingSelection(@TempDir Path root) throws Exception {
        var storage = physicalStorage(root);
        var selector = new AttachmentOperationRepository(template, storage);
        var acquired = acquire(repository.reserve(logicalBatch("race-selection", PHYSICAL_BYTES)));
        var abandoned = repository.reservePhysicalGeneration(lease(acquired), writer(acquired)).orElseThrow();
        var previous = raw(abandoned).getList("physical_generations", Document.class).get(0);
        expire(acquired.getId());
        var next = acquire(repository.findById(acquired.getId()).orElseThrow());
        var prepared = prepareForSelection(storage, next, PHYSICAL_BYTES);
        var other = new AttachmentOperationRepository(template, storage);
        var results = race(
            () -> selector.selectPhysicalGeneration(prepared.evidence(), prepared.writer()),
            () -> other.selectPhysicalGeneration(prepared.evidence(), prepared.writer())
        );
        assertThat(results).containsExactlyInAnyOrder(SELECTED, ALREADY_SELECTED);
        var selected = repository.findById(acquired.getId()).orElseThrow();
        var originalHistory = raw(selected).getList("physical_generations", Document.class);
        assertThat(originalHistory.get(0)).isEqualTo(previous);

        // Discard the acknowledged return values. Re-identification performs a new journaled revision CAS.
        int writes = writeConcerns.size();
        assertThat(other.selectPhysicalGeneration(prepared.evidence(), prepared.writer())).isEqualTo(ALREADY_SELECTED);
        assertThat(writeConcerns.subList(writes, writeConcerns.size()))
            .singleElement()
            .satisfies(concern -> assertThat(concern.getBoolean("j").getValue()).isTrue());
        var confirmed = repository.findById(acquired.getId()).orElseThrow();
        assertThat(confirmed.getRevision()).isEqualTo(selected.getRevision() + 1);
        expire(acquired.getId());
        var taken = acquire(confirmed);
        assertThat(taken.getClaim().getFencingToken()).isEqualTo(next.getClaim().getFencingToken() + 1);
        assertThat(taken.getSelectedPhysicalGeneration()).isEqualTo(next.getClaim().getFencingToken());
        assertThat(raw(taken).getList("physical_generations", Document.class)).isEqualTo(originalHistory);
        assertThat(other.selectPhysicalGeneration(prepared.evidence(), prepared.writer())).isEqualTo(UNCERTAIN);
        assertThat(repository.beginPersistence(lease(taken))).isEmpty();
        assertThat(Files.readAllBytes(root.resolve(prepared.evidence().files().get(0).storageKey()))).containsExactly(PHYSICAL_BYTES);
    }

    @Test
    void rejectsMismatchedPersistedEvidenceWithNoPartialUpdates(@TempDir Path root) throws Exception {
        var storage = physicalStorage(root);
        var selector = new AttachmentOperationRepository(template, storage);
        var changedFields = Map.of(
            "missing-entries",
            new Document("physical_generations.0.entries", List.of()),
            "wrong-logical",
            new Document("physical_generations.0.entries.0.logical_id", UUID.randomUUID().toString()),
            "wrong-key",
            new Document("physical_generations.0.entries.0.storage_key", UUID.randomUUID().toString()),
            "wrong-size",
            new Document("files.0.expected_size_bytes", 2L),
            "wrong-digest",
            new Document("files.0.expected_sha256", "0".repeat(64))
        );
        for (var change : changedFields.entrySet()) {
            var prepared = prepareForSelection(storage, change.getKey(), PHYSICAL_BYTES, new byte[] { 1 });
            String id = prepared.evidence().lease().version().id();
            var filter = new Document("_id", id);
            manifests().updateOne(filter, new Document("$set", change.getValue()));
            Document before = manifests().find(filter).first();
            assertThat(selector.selectPhysicalGeneration(prepared.evidence(), prepared.writer())).isEqualTo(CONFLICT);
            assertThat(manifests().find(filter).first()).isEqualTo(before);
        }

        for (boolean duplicate : List.of(false, true)) {
            var prepared = prepareForSelection(storage, "order-" + duplicate, PHYSICAL_BYTES, new byte[] { 1 });
            var filter = new Document("_id", prepared.evidence().lease().version().id());
            var entries = manifests()
                .find(filter)
                .first()
                .getList("physical_generations", Document.class)
                .get(0)
                .getList("entries", Document.class);
            var wrong = List.of(entries.get(1), entries.get(duplicate ? 1 : 0));
            manifests().updateOne(filter, new Document("$set", new Document("physical_generations.0.entries", wrong)));
            var before = manifests().find(filter).first();
            assertThat(selector.selectPhysicalGeneration(prepared.evidence(), prepared.writer())).isEqualTo(CONFLICT);
            assertThat(manifests().find(filter).first()).isEqualTo(before);
        }

        for (int version : List.of(1, 3)) {
            var prepared = prepareForSelection(storage, "format-" + version, PHYSICAL_BYTES);
            var filter = new Document("_id", prepared.evidence().lease().version().id());
            manifests().updateOne(filter, new Document("$set", new Document("format_version", version)));
            assertThat(selector.selectPhysicalGeneration(prepared.evidence(), prepared.writer())).isEqualTo(CONFLICT);
            assertThat(manifests().find(filter).first()).doesNotContainKey("selected_physical_generation");
        }
    }

    @Test
    void checksExactBsonBudgetWithoutDiscardingPreparedFiles(@TempDir Path root) throws Exception {
        var storage = physicalStorage(root);
        var selector = new AttachmentOperationRepository(template, storage);
        var probe = prepareForSelection(storage, "budget-probe", PHYSICAL_BYTES);
        assertThat(selector.selectPhysicalGeneration(probe.evidence(), probe.writer())).isEqualTo(SELECTED);
        var probeRaw = manifests().find(new Document("_id", probe.evidence().lease().version().id())).first();
        int paddedLength =
            AttachmentOperationRepository.MAX_MANIFEST_BYTES - repository.bsonSize(new Document(probeRaw).append("synthetic_padding", ""));
        for (int extra : List.of(0, 1)) {
            var tested = prepareForSelection(storage, "budget-" + extra, PHYSICAL_BYTES);
            var filter = new Document("_id", tested.evidence().lease().version().id());
            // Deliberately perturb the manifest after preparation to exercise the selector's independent budget check.
            manifests().updateOne(filter, new Document("$set", new Document("synthetic_padding", "x".repeat(paddedLength + extra))));
            var before = manifests().find(filter).first();
            assertThat(selector.selectPhysicalGeneration(tested.evidence(), tested.writer())).isEqualTo(
                extra == 0 ? SELECTED : RECONCILIATION_REQUIRED
            );
            var after = manifests().find(filter).first();
            if (extra == 0) assertThat(repository.bsonSize(after)).isEqualTo(1_048_576);
            else assertThat(after).isEqualTo(before);
            assertThat(Files.readAllBytes(root.resolve(tested.evidence().files().get(0).storageKey()))).containsExactly(PHYSICAL_BYTES);
        }
    }

    @Test
    void selectionLoggingRedactsEvidenceAndWriterAndDoesNotDiscloseTheSecret(@TempDir Path root) throws Exception {
        var storage = physicalStorage(root);
        var selector = new AttachmentOperationRepository(template, storage);
        var prepared = prepareForSelection(storage, "selection-logs", PHYSICAL_BYTES);
        var factory = new org.springframework.aop.aspectj.annotation.AspectJProxyFactory(selector);
        var env = new org.springframework.mock.env.MockEnvironment();
        env.setActiveProfiles("dev");
        factory.addAspect(new co.edu.itp.svu.aop.logging.LoggingAspect(env));
        AttachmentOperationRepository proxy = factory.getProxy();
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(AttachmentOperationRepository.class);
        var oldLevel = logger.getLevel();
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        try {
            var field = AttachmentWriterCapability.class.getDeclaredField("secret");
            field.setAccessible(true);
            var secret = (byte[]) field.get(prepared.writer());
            var claim = prepared.evidence().lease();
            var foreignWriter = new AttachmentWriterCapability(UUID.fromString(claim.version().id()), claim.fencing());
            assertThat(proxy.selectPhysicalGeneration(prepared.evidence(), foreignWriter)).isEqualTo(CONFLICT);
            assertThat(proxy.selectPhysicalGeneration(prepared.evidence(), prepared.writer())).isEqualTo(SELECTED);
            assertThat(appender.list.stream().map(event -> event.getFormattedMessage()).toList())
                .anySatisfy(message -> assertThat(message).contains("PreparedGeneration[redacted]", "AttachmentWriterCapability[redacted]"))
                .allSatisfy(message ->
                    assertThat(message).doesNotContain(
                        HexFormat.of().formatHex(secret),
                        Base64.getEncoder().encodeToString(secret),
                        Arrays.toString(secret),
                        prepared.evidence().files().get(0).storageKey()
                    )
                );
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(oldLevel);
            appender.stop();
        }
    }
}
