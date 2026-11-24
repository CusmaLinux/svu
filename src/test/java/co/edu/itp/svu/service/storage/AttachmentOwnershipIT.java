package co.edu.itp.svu.service.storage;

import static co.edu.itp.svu.service.AttachmentOwnershipResolver.Status.*;
import static org.assertj.core.api.Assertions.assertThat;

import co.edu.itp.svu.IntegrationTest;
import co.edu.itp.svu.repository.AttachmentOwnershipRepository;
import co.edu.itp.svu.service.AttachmentOwnershipResolver;
import com.mongodb.DBRef;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

@IntegrationTest
class AttachmentOwnershipIT {

    @TempDir
    Path root;

    @Autowired
    MongoTemplate mongo;

    @Autowired
    AttachmentOwnershipRepository ownershipRepository;

    private final List<DBRef> fixtures = new ArrayList<>();
    private AttachmentOwnershipResolver resolver;
    private DBRef caseRef;
    private DBRef responseRef;
    private String key;

    @BeforeEach
    void setup() throws IOException {
        resolver = new AttachmentOwnershipResolver(ownershipRepository, new AttachmentStorage(root, UUID::randomUUID));
        caseRef = insert("pqrs", new ObjectId(), new Document());
        responseRef = insert("respuesta", UUID.randomUUID().toString(), new Document("pqrs", caseRef));
        key = "Histórico á " + UUID.randomUUID();
        Files.writeString(root.resolve(key), "documento sintético");
    }

    @AfterEach
    void cleanup() {
        for (DBRef fixture : fixtures) {
            mongo.getCollection(fixture.getCollectionName()).deleteOne(new Document("_id", fixture.getId()));
        }
    }

    @Test
    void resolvesDirectParentWithExactBsonId() {
        assertThat(resolver.resolve(file(caseRef, null, key))).isEqualTo(
            new AttachmentOwnershipResolver.Ownership(DIRECT, caseRef.getId(), null)
        );
    }

    @Test
    void resolvesResponseParent() {
        assertThat(resolver.resolve(file(null, responseRef, key))).isEqualTo(
            new AttachmentOwnershipResolver.Ownership(RESPONSE, caseRef.getId(), responseRef.getId())
        );
    }

    @Test
    void resolvesBothParentsInSameCase() {
        assertThat(resolver.resolve(file(caseRef, responseRef, key))).isEqualTo(
            new AttachmentOwnershipResolver.Ownership(BOTH, caseRef.getId(), responseRef.getId())
        );
    }

    @Test
    void recognizesUniqueHistoricalInverseAssociation() {
        String id = file(null, null, key);
        DBRef owner = inverse(id);
        assertThat(resolver.resolve(id)).isEqualTo(new AttachmentOwnershipResolver.Ownership(HISTORICAL_INVERSE, owner.getId(), null));
    }

    @Test
    void rejectsConflictingForwardAndInverseParents() {
        String id = file(caseRef, null, key);
        inverse(id);
        rejected(id, INCONSISTENT);
    }

    @Test
    void rejectsMultipleInverseParents() {
        String id = file(null, null, key);
        inverse(id);
        inverse(id);
        rejected(id, INCONSISTENT);
    }

    @Test
    void rejectsContradictoryForwardParents() {
        DBRef another = insert("pqrs", new ObjectId(), new Document());
        rejected(file(another, responseRef, key), INCONSISTENT);
    }

    @Test
    void rejectsBrokenCaseAndResponseReferences() {
        var missingCase = new DBRef("pqrs", new ObjectId());
        var missingResponse = new DBRef("respuesta", new ObjectId());
        var brokenResponse = insert("respuesta", new ObjectId(), new Document("pqrs", missingCase));
        for (String id : List.of(file(missingCase, null, key), file(caseRef, missingResponse, key), file(caseRef, brokenResponse, key))) {
            rejected(id, INCONSISTENT);
        }
    }

    @Test
    void rejectsUnassociatedMissingAndUnsafeDocuments() {
        rejected(file(null, null, key), UNASSOCIATED);
        rejected(file(caseRef, null, "inexistente"), MISSING_FILE);
        rejected(file(caseRef, null, "../synthetic"), UNSAFE_FILE);
        rejected(new ObjectId().toHexString(), NOT_FOUND);
    }

    @Test
    void rejectsSharedKeyWithoutRemovingData() {
        String id = file(caseRef, null, key);
        String duplicate = file(null, null, key);
        rejected(id, SHARED_KEY);
        assertThat(root.resolve(key)).hasContent("documento sintético");
        assertThat(ownershipRepository.findCandidates(duplicate)).hasSize(1);
    }

    @Test
    void rejectsAmbiguousPublicIdAcrossBsonTypes() {
        var binaryId = new ObjectId();
        insert("archivo_adjunto", binaryId, new Document("url_archivo", key));
        insert("archivo_adjunto", binaryId.toHexString(), new Document("url_archivo", key));
        rejected(binaryId.toHexString(), INCONSISTENT);
    }

    private void rejected(String id, AttachmentOwnershipResolver.Status status) {
        assertThat(resolver.resolve(id)).isEqualTo(new AttachmentOwnershipResolver.Ownership(status, null, null));
    }

    private DBRef inverse(String id) {
        var explicitRef = new DBRef(mongo.getDb().getName(), "archivo_adjunto", new ObjectId(id));
        return insert("pqrs", UUID.randomUUID().toString(), new Document("archivosAdjuntos", List.of(explicitRef)));
    }

    private String file(DBRef direct, DBRef response, String storageKey) {
        var id = new ObjectId();
        insert(
            "archivo_adjunto",
            id,
            new Document("url_archivo", storageKey).append("pqrs_attachment", direct).append("response_attachment", response)
        );
        return id.toHexString();
    }

    private DBRef insert(String collection, Object id, Document document) {
        mongo.getCollection(collection).insertOne(document.append("_id", id));
        var reference = new DBRef(collection, id);
        fixtures.add(reference);
        return reference;
    }
}
