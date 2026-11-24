package co.edu.itp.svu.repository;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;

import com.mongodb.DBRef;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/** Reads raw DBRefs so missing targets cannot be mistaken for absent relations. */
@Repository
public class AttachmentOwnershipRepository {

    private static final String ATTACHMENTS = "archivo_adjunto";
    private static final String CASES = "pqrs";
    private static final String RESPONSES = "respuesta";

    public record Attachment(Object id, String key, Object directReference, Object responseReference) {}

    private final MongoTemplate mongo;

    public AttachmentOwnershipRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public List<Attachment> findCandidates(String id) {
        var filter = ObjectId.isValid(id) ? in("_id", id, new ObjectId(id)) : eq("_id", id);
        return mongo
            .getCollection(ATTACHMENTS)
            .find(filter)
            .projection(include("url_archivo", "pqrs_attachment", "response_attachment"))
            .limit(2)
            .map(doc ->
                new Attachment(
                    doc.get("_id"),
                    doc.get("url_archivo") instanceof String key ? key : null,
                    doc.get("pqrs_attachment"),
                    doc.get("response_attachment")
                )
            )
            .into(new java.util.ArrayList<>());
    }

    public boolean hasSharedKey(Attachment attachment) {
        return (
            mongo
                .getCollection(ATTACHMENTS)
                .find(and(eq("url_archivo", attachment.key()), ne("_id", attachment.id())))
                .projection(include("_id"))
                .first() !=
            null
        );
    }

    public List<Object> inverseCases(Object attachmentId) {
        var local = new DBRef(ATTACHMENTS, attachmentId);
        var explicit = new DBRef(mongo.getDb().getName(), ATTACHMENTS, attachmentId);
        return mongo
            .getCollection(CASES)
            .find(in("archivosAdjuntos", local, explicit))
            .projection(include("_id"))
            .limit(2)
            .map(doc -> doc.get("_id"))
            .into(new java.util.ArrayList<>());
    }

    public Object existingCase(Object reference) {
        var doc = referencedDocument(CASES, reference, "_id");
        return doc == null ? null : doc.get("_id");
    }

    public Object responseCaseReference(Object reference) {
        var doc = referencedDocument(RESPONSES, reference, "pqrs");
        return doc == null ? null : doc.get("pqrs");
    }

    private Document referencedDocument(String collection, Object reference, String field) {
        if (
            !(reference instanceof DBRef ref) ||
            !collection.equals(ref.getCollectionName()) ||
            (ref.getDatabaseName() != null && !mongo.getDb().getName().equals(ref.getDatabaseName())) ||
            !(ref.getId() instanceof String || ref.getId() instanceof ObjectId)
        ) {
            return null;
        }
        return mongo.getCollection(collection).find(eq("_id", ref.getId())).projection(include(field)).first();
    }
}
