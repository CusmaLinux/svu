package co.edu.itp.svu.repository;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

import co.edu.itp.svu.domain.attachment.AttachmentOperation;
import com.mongodb.MongoException;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.bson.BsonType;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/** Atomic visibility gate for roots. Only package-internal verified evidence can reach the CAS. */
@Repository
public class AttachmentPublicationRepository {

    public enum Result {
        PUBLISHED,
        ALREADY_PUBLISHED,
        NOT_READY,
        UNCERTAIN,
    }

    private final MongoTemplate mongo;
    private final AttachmentOperationRepository operations;

    public AttachmentPublicationRepository(MongoTemplate mongo, AttachmentOperationRepository operations) {
        this.mongo = mongo;
        this.operations = operations;
    }

    /** Binds the hidden root to the exact active manifest revision/fence before physical proof is issued. */
    boolean bindLease(AttachmentOperationRepository.Lease lease, String collectionName, Object rootId) {
        if (!supported(collectionName) || rootId == null) return false;
        AttachmentOperation current = operations.findCurrent(lease).orElse(null);
        if (current == null || current.getState() != co.edu.itp.svu.domain.attachment.AttachmentOperationState.PERSISTING) return false;
        Date leaseUntil = Date.from(current.getClaim().getLeaseUntil());
        String publicationId = current.getPublicationId();
        if (publicationId == null || !publicationId.equals(current.getPublicationId())) return false;
        MongoCollection<Document> roots = roots(collectionName);
        Document root = roots.find(eq("_id", rootId)).first();
        if (root == null) return false;
        Document publication = root.get("publication", Document.class);
        if (
            publication == null ||
            !"PREPARING".equals(publication.getString("state")) ||
            !current.getId().equals(publication.getString("operation_id")) ||
            !publicationId.equals(publication.getString("publication_id")) ||
            !current.getRequestDigest().equals(publication.getString("digest"))
        ) return false;

        Number priorRevision = publication.get("manifest_revision", Number.class);
        Number priorFence = publication.get("fencing_token", Number.class);
        Number selected = current.getSelectedPhysicalGeneration();
        if (selected == null) return false;
        long markerRevision = number(publication.get("revision"), -1);
        if (markerRevision < 0) return false;
        long oldManifestRevision = priorRevision == null ? -1 : priorRevision.longValue();
        long oldFence = priorFence == null ? 0 : priorFence.longValue();
        if (oldFence > lease.fencing() || (oldFence == lease.fencing() && oldManifestRevision > lease.version().revision())) return false;

        var guard = and(
            eq("_id", rootId),
            eq("publication.state", "PREPARING"),
            eq("publication.operation_id", current.getId()),
            eq("publication.publication_id", publicationId),
            eq("publication.digest", current.getRequestDigest()),
            eq("publication.revision", markerRevision),
            oldManifestRevision < 0
                ? exists("publication.manifest_revision", false)
                : eq("publication.manifest_revision", oldManifestRevision),
            priorFence == null ? exists("publication.fencing_token", false) : eq("publication.fencing_token", oldFence)
        );
        Document changed = roots.findOneAndUpdate(
            guard,
            combine(
                set("publication.manifest_revision", lease.version().revision()),
                set("publication.fencing_token", lease.fencing()),
                set("publication.selected_generation", selected.longValue()),
                set("publication.lease_until", leaseUntil)
            ),
            new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
        );
        if (changed != null) return true;
        Document observed = roots.find(eq("_id", rootId)).first();
        Document bound = observed == null ? null : observed.get("publication", Document.class);
        return bound != null && sameBinding(bound, current, lease, selected.longValue(), leaseUntil);
    }

    Result publish(VerifiedAttachmentPublication proof) {
        MongoCollection<Document> roots = roots(proof.collection);
        var guard = and(
            eq("_id", proof.rootId),
            eq("publication.state", "PREPARING"),
            eq("publication.operation_id", proof.operationId),
            eq("publication.publication_id", proof.publicationId),
            eq("publication.digest", proof.digest),
            eq("publication.revision", proof.publicationRevision),
            eq("publication.manifest_revision", proof.manifestRevision),
            eq("publication.fencing_token", proof.fencingToken),
            eq("publication.selected_generation", proof.selectedGeneration),
            type("publication.lease_until", BsonType.DATE_TIME),
            expr(new Document("$gt", List.of("$publication.lease_until", "$$NOW")))
        );
        try {
            Document changed = roots.findOneAndUpdate(
                guard,
                combine(
                    set("publication.state", "COMMITTED"),
                    set("publication.revision", proof.publicationRevision + 1),
                    currentDate("publication.committed_at")
                ),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
            );
            if (changed != null) return Result.PUBLISHED;
        } catch (MongoException uncertain) {
            return classify(roots, proof);
        }
        return classify(roots, proof);
    }

    private Result classify(MongoCollection<Document> roots, VerifiedAttachmentPublication proof) {
        try {
            Document observed = roots.find(eq("_id", proof.rootId)).first();
            if (observed == null) return Result.NOT_READY;
            Document publication = observed.get("publication", Document.class);
            if (publication == null) return Result.NOT_READY;
            boolean same =
                Objects.equals(proof.operationId, publication.getString("operation_id")) &&
                Objects.equals(proof.publicationId, publication.getString("publication_id")) &&
                Objects.equals(proof.digest, publication.getString("digest")) &&
                Objects.equals(proof.fencingToken, number(publication.get("fencing_token"), -1)) &&
                Objects.equals(proof.manifestRevision, number(publication.get("manifest_revision"), -1)) &&
                Objects.equals(proof.selectedGeneration, number(publication.get("selected_generation"), -1));
            if (
                same &&
                "COMMITTED".equals(publication.getString("state")) &&
                Objects.equals(proof.publicationRevision + 1, number(publication.get("revision"), -1))
            ) {
                return Result.ALREADY_PUBLISHED;
            }
            if (same && "PREPARING".equals(publication.getString("state"))) return Result.NOT_READY;
            return Result.UNCERTAIN;
        } catch (MongoException unavailable) {
            return Result.UNCERTAIN;
        }
    }

    private MongoCollection<Document> roots(String collection) {
        if (!supported(collection)) throw new IllegalArgumentException("Unsupported publication root");
        return mongo
            .getCollection(collection)
            .withReadPreference(ReadPreference.primary())
            .withWriteConcern(WriteConcern.W1.withJournal(true).withWTimeout(10, TimeUnit.SECONDS));
    }

    private static boolean supported(String collection) {
        return "pqrs".equals(collection) || "respuesta".equals(collection);
    }

    private static long number(Object value, long fallback) {
        return value instanceof Number number ? number.longValue() : fallback;
    }

    private static boolean sameBinding(
        Document publication,
        AttachmentOperation operation,
        AttachmentOperationRepository.Lease lease,
        long selected,
        Date deadline
    ) {
        Instant observedDeadline = publication.getDate("lease_until") == null ? null : publication.getDate("lease_until").toInstant();
        return (
            "PREPARING".equals(publication.getString("state")) &&
            operation.getId().equals(publication.getString("operation_id")) &&
            Objects.equals(operation.getPublicationId(), publication.getString("publication_id")) &&
            Objects.equals(operation.getRequestDigest(), publication.getString("digest")) &&
            Objects.equals(number(publication.get("manifest_revision"), -1), lease.version().revision()) &&
            Objects.equals(number(publication.get("fencing_token"), -1), lease.fencing()) &&
            Objects.equals(number(publication.get("selected_generation"), -1), selected) &&
            Objects.equals(observedDeadline, deadline.toInstant())
        );
    }
}
