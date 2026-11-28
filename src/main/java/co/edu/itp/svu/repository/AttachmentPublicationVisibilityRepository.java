package co.edu.itp.svu.repository;

import com.mongodb.DBRef;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/** Resolves visibility for attachment readers while retaining legacy objects without publication markers. */
@Repository
public class AttachmentPublicationVisibilityRepository {

    private final MongoTemplate mongo;

    public AttachmentPublicationVisibilityRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public boolean isVisible(String attachmentId) {
        Document attachment = mongo.getCollection("archivo_adjunto").find(new Document("_id", storedId(attachmentId))).first();
        return attachment != null && isVisible(attachment);
    }

    public boolean isVisibleKey(String storageKey) {
        List<Document> candidates = mongo
            .getCollection("archivo_adjunto")
            .find(new Document("url_archivo", storageKey))
            .limit(2)
            .into(new java.util.ArrayList<>());
        // Unindexed historical files may have no metadata record. Multiple owners are ambiguous.
        return candidates.isEmpty() || (candidates.size() == 1 && isVisible(candidates.get(0)));
    }

    public boolean isManaged(String attachmentId) {
        Document attachment = mongo.getCollection("archivo_adjunto").find(new Document("_id", storedId(attachmentId))).first();
        if (attachment == null) return false;
        return (
            mongo
                .getCollection(AttachmentOperationRepository.COLLECTION)
                .find(new Document("files.metadata_id", attachment.get("_id")))
                .limit(1)
                .first() !=
            null
        );
    }

    public boolean isManagedKey(String storageKey) {
        Document attachment = mongo.getCollection("archivo_adjunto").find(new Document("url_archivo", storageKey)).first();
        return attachment != null && isManaged(attachment.get("_id").toString());
    }

    public boolean isVisible(Document attachment) {
        Object id = attachment.get("_id");
        List<Document> manifests = mongo
            .getCollection(AttachmentOperationRepository.COLLECTION)
            .find(new Document("files.metadata_id", id))
            .limit(2)
            .into(new java.util.ArrayList<>());
        if (manifests.size() > 1) return false;
        boolean managed = !manifests.isEmpty();
        if (managed && !"COMMITTED".equals(manifests.get(0).getString("state"))) return false;
        Object direct = attachment.get("pqrs_attachment");
        Object response = attachment.get("response_attachment");
        if (direct == null && response == null) return !managed;
        if (direct != null && !visibleReference(direct, "pqrs")) return false;
        if (response != null) {
            Document responseRoot = referenced(response, "respuesta");
            if (responseRoot == null || !visibleRoot(responseRoot)) return false;
            Object caseReference = responseRoot.get("pqrs");
            if (caseReference == null || !visibleReference(caseReference, "pqrs")) return false;
        }
        return true;
    }

    private boolean visibleReference(Object raw, String expectedCollection) {
        if (
            !(raw instanceof DBRef reference) || !expectedCollection.equals(reference.getCollectionName()) || foreignDatabase(reference)
        ) return false;
        Document root = mongo.getCollection(expectedCollection).find(new Document("_id", reference.getId())).first();
        return root != null && visibleRoot(root);
    }

    private Document referenced(Object raw, String expectedCollection) {
        if (
            !(raw instanceof DBRef reference) || !expectedCollection.equals(reference.getCollectionName()) || foreignDatabase(reference)
        ) return null;
        return mongo.getCollection(expectedCollection).find(new Document("_id", reference.getId())).first();
    }

    private boolean foreignDatabase(DBRef reference) {
        return reference.getDatabaseName() != null && !mongo.getDb().getName().equals(reference.getDatabaseName());
    }

    private static boolean visibleRoot(Document root) {
        if (!root.containsKey("publication")) return true;
        Document marker = root.get("publication", Document.class);
        return marker != null && "COMMITTED".equals(marker.getString("state"));
    }

    private static Object storedId(String id) {
        if (id == null) return null;
        if (org.bson.types.ObjectId.isValid(id)) return new org.bson.types.ObjectId(id);
        return id;
    }
}
