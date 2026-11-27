package co.edu.itp.svu.service.storage;

import co.edu.itp.svu.domain.attachment.AttachmentOperation;
import co.edu.itp.svu.domain.attachment.AttachmentOperationState;
import co.edu.itp.svu.repository.AttachmentOperationRepository;
import co.edu.itp.svu.repository.AttachmentOperationRepository.Lease;
import co.edu.itp.svu.repository.AttachmentOperationRepository.PublishedRoot;
import co.edu.itp.svu.repository.AttachmentOperationRepository.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Conservative, repeatable recovery. It never removes physical or historical files. */
@Component
public class AttachmentRecoveryService {

    private static final int BATCH_SIZE = 50;
    private static final Duration LEASE = Duration.ofMinutes(2);

    private final AttachmentOperationRepository operations;
    private final MongoTemplate mongo;

    public AttachmentRecoveryService(AttachmentOperationRepository operations, MongoTemplate mongo) {
        this.operations = operations;
        this.mongo = mongo;
    }

    @Scheduled(fixedDelayString = "${svu.attachments.recovery-delay-ms:60000}")
    public void recoverPendingBatch() {
        for (AttachmentOperation candidate : operations.findPending(Instant.now(), BATCH_SIZE)) {
            recover(candidate);
        }
    }

    private void recover(AttachmentOperation candidate) {
        Version version = new Version(candidate.getId(), candidate.getState(), candidate.getRevision(), candidate.getFormatVersion());
        Lease lease = operations.acquire(version, UUID.randomUUID(), LEASE).map(this::lease).orElse(null);
        if (lease == null) return; // Another live worker owns it, or its version changed.

        AttachmentOperationState state = candidate.getState();
        if (state == AttachmentOperationState.PERSISTING) {
            if (!finalizePublishedRoot(candidate, lease)) operations.requireReconciliation(lease);
            return;
        }

        String collection = candidate.getOperationType().name().equals("PUBLIC_CASE_CREATE") ? "pqrs" : "respuesta";
        Object rootId = "pqrs".equals(collection) ? candidate.getCaseId() : candidate.getResponseId();
        Document root = mongo.getCollection(collection).find(new Document("_id", rootId)).first();
        if (root == null && operations.abortBeforePersistence(lease).isPresent()) return;
        operations.requireReconciliation(lease);
    }

    private boolean finalizePublishedRoot(AttachmentOperation operation, Lease lease) {
        String collection = operation.getOperationType().name().equals("PUBLIC_CASE_CREATE") ? "pqrs" : "respuesta";
        Object id = "pqrs".equals(collection) ? operation.getCaseId() : operation.getResponseId();
        Document root = mongo.getCollection(collection).find(new Document("_id", id)).first();
        if (root == null) return false;
        Document publication = root.get("publication", Document.class);
        if (publication == null || !"COMMITTED".equals(publication.getString("state"))) return false;
        Number revision = publication.get("revision", Number.class);
        Date committedAt = publication.get("committed_at", Date.class);
        if (
            revision == null ||
            committedAt == null ||
            !operation.getId().equals(publication.getString("operation_id")) ||
            !java.util.Objects.equals(operation.getPublicationId(), publication.getString("publication_id"))
        ) return false;
        return operations
            .confirmPublished(
                lease,
                new PublishedRoot(
                    collection,
                    id,
                    operation.getId(),
                    operation.getPublicationId(),
                    publication.getString("digest"),
                    revision.longValue()
                )
            )
            .isPresent();
    }

    private Lease lease(AttachmentOperation operation) {
        var claim = operation.getClaim();
        return new Lease(
            new Version(operation.getId(), operation.getState(), operation.getRevision(), operation.getFormatVersion()),
            UUID.fromString(claim.getOwnerId()),
            claim.getFencingToken()
        );
    }
}
