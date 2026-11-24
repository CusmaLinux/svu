package co.edu.itp.svu.repository;

import static org.assertj.core.api.Assertions.*;

import co.edu.itp.svu.domain.attachment.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.time.Instant;
import java.util.List;
import org.bson.Document;
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

    @BeforeAll
    static void startStandaloneMongo() {
        MONGO.start();
        String uri = "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/" + DATABASE;
        client = MongoClients.create(uri);
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
    }

    @Test
    void provesStandaloneWritesReadsUniqueIndexesAndConditionalUpdate() {
        var operation = operation("actor-a", "key-a", "digest-a");
        repository.insert(operation);
        var loaded = repository.findById(operation.getId()).orElseThrow();
        assertThat(loaded.getOperationType()).isEqualTo(AttachmentOperationType.PUBLIC_CASE_CREATE);
        assertThat(loaded.getState()).isEqualTo(AttachmentOperationState.PREPARING);
        assertThat(
            repository.compareAndSetState(loaded.getId(), loaded.getState(), 0, AttachmentOperationState.FILES_READY, Instant.now())
        ).isTrue();
        assertThat(
            repository.compareAndSetState(loaded.getId(), loaded.getState(), 0, AttachmentOperationState.PERSISTING, Instant.now())
        ).isFalse();
        assertThatThrownBy(() -> repository.insert(operation("actor-a", "key-a", "different"))).isInstanceOf(DuplicateKeyException.class);
        assertThat(repository.insert(operation("actor-b", "key-a", "digest-a")).getId()).isNotEqualTo(loaded.getId());
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
        file.setStorageKey("opaque-key");
        file.setSizeBytes(7L);
        file.setSha256("a".repeat(64));
        file.setState("RESERVED");
        op.setFiles(List.of(file));
        repository.insert(op);
        Document raw = template.getCollection(AttachmentOperationRepository.COLLECTION).find(new Document("_id", op.getId())).first();
        assertThat(raw.get("case_id")).isInstanceOf(org.bson.types.ObjectId.class);
        assertThat(((Document) ((List<?>) raw.get("files")).get(0)).get("metadata_id")).isInstanceOf(org.bson.types.ObjectId.class);
        assertThat(raw.containsKey("token")).isFalse();
        assertThat(raw.containsKey("contenido")).isFalse();
        assertThat(repository.findPending(Instant.now().plusSeconds(2), 10)).hasSize(1);
        assertThat(repository.findExpiredClaims(Instant.now(), 10)).isEmpty();
        op.setClaim(new AttachmentOperation.Claim());
        op.getClaim().setOwnerId("worker-synthetic");
        op.getClaim().setLeaseUntil(Instant.now().minusSeconds(5));
        op.getClaim().setFencingToken(3);
        template.save(op, AttachmentOperationRepository.COLLECTION);
        assertThat(repository.findExpiredClaims(Instant.now(), 10)).extracting(AttachmentOperation::getId).containsExactly(op.getId());
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
        op.setActorScopeHash(actor);
        op.setRequestKeyHash(key);
        op.setRequestDigest(digest);
        return op;
    }
}
