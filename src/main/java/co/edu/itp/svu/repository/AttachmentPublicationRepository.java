package co.edu.itp.svu.repository;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/** Atomic visibility gate for newly prepared PQRSD roots. Legacy roots have no publication marker. */
@Repository
public class AttachmentPublicationRepository {

    public enum Result {
        PUBLISHED,
        ALREADY_PUBLISHED,
        NOT_READY,
        UNCERTAIN,
    }

    private final MongoTemplate mongo;

    public AttachmentPublicationRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /**
     * Performs the single-document linearization point. Callers must verify all metadata and physical
     * objects before invoking this CAS. A lost acknowledgement is resolved by rereading the root.
     */
    public Result publish(String collectionName, Object rootId, String operationId, String publicationId, String digest, long revision) {
        if (!"pqrs".equals(collectionName) && !"respuesta".equals(collectionName)) throw new IllegalArgumentException("Unsupported root");
        if (rootId == null || operationId == null || publicationId == null || digest == null || revision < 0) {
            throw new IllegalArgumentException("Invalid publication evidence");
        }
        MongoCollection<Document> roots = mongo
            .getCollection(collectionName)
            .withReadPreference(ReadPreference.primary())
            .withWriteConcern(WriteConcern.W1.withJournal(true).withWTimeout(10, TimeUnit.SECONDS));
        var identity = and(
            eq("_id", rootId),
            eq("publication.state", "PREPARING"),
            eq("publication.operation_id", operationId),
            eq("publication.publication_id", publicationId),
            eq("publication.digest", digest),
            eq("publication.revision", revision)
        );
        try {
            Document changed = roots.findOneAndUpdate(
                identity,
                combine(
                    set("publication.state", "COMMITTED"),
                    set("publication.revision", revision + 1),
                    currentDate("publication.committed_at")
                ),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            );
            if (changed != null) return Result.PUBLISHED;
        } catch (MongoException uncertain) {
            return classify(roots, rootId, operationId, publicationId, digest, revision);
        }
        return classify(roots, rootId, operationId, publicationId, digest, revision);
    }

    private Result classify(
        MongoCollection<Document> roots,
        Object id,
        String operationId,
        String publicationId,
        String digest,
        long revision
    ) {
        try {
            Document observed = roots.find(eq("_id", id)).first();
            if (observed == null) return Result.NOT_READY;
            Document publication = observed.get("publication", Document.class);
            if (publication == null) return Result.NOT_READY;
            boolean same =
                Objects.equals(operationId, publication.getString("operation_id")) &&
                Objects.equals(publicationId, publication.getString("publication_id")) &&
                Objects.equals(digest, publication.getString("digest"));
            if (same && "COMMITTED".equals(publication.getString("state"))) return Result.ALREADY_PUBLISHED;
            if (
                same &&
                "PREPARING".equals(publication.getString("state")) &&
                Objects.equals(revision, publication.get("revision", Number.class).longValue())
            ) {
                return Result.NOT_READY;
            }
            return Result.UNCERTAIN;
        } catch (MongoException unavailable) {
            return Result.UNCERTAIN;
        }
    }
}
