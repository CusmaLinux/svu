package co.edu.itp.svu.repository;

import static org.assertj.core.api.Assertions.*;

import co.edu.itp.svu.domain.attachment.*;
import co.edu.itp.svu.repository.AttachmentOperationRepository.*;
import co.edu.itp.svu.service.storage.AttachmentRequestIdentity;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
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
        return new Version(op.getId(), op.getState(), op.getRevision());
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
}
