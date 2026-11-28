package co.edu.itp.svu.service.storage;

import co.edu.itp.svu.domain.attachment.AttachmentIntegrityVerifier;
import co.edu.itp.svu.domain.attachment.AttachmentOperation;
import co.edu.itp.svu.domain.attachment.AttachmentOperationState;
import co.edu.itp.svu.domain.attachment.AttachmentOperationType;
import co.edu.itp.svu.repository.AttachmentOperationRepository;
import co.edu.itp.svu.repository.AttachmentOperationRepository.Lease;
import co.edu.itp.svu.repository.AttachmentOperationRepository.PublishedRoot;
import co.edu.itp.svu.repository.AttachmentOperationRepository.Version;
import co.edu.itp.svu.repository.AttachmentPublicationRepository;
import co.edu.itp.svu.repository.AttachmentPublicationVerifier;
import co.edu.itp.svu.service.ResponsePublicationGate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final AttachmentPublicationVerifier publicationVerifier;
    private final AttachmentIntegrityVerifier integrityVerifier;
    private final ResponsePublicationGate responseGate;

    public AttachmentRecoveryService(
        AttachmentOperationRepository operations,
        MongoTemplate mongo,
        AttachmentPublicationVerifier publicationVerifier,
        AttachmentIntegrityVerifier integrityVerifier,
        ResponsePublicationGate responseGate
    ) {
        this.operations = operations;
        this.mongo = mongo;
        this.publicationVerifier = publicationVerifier;
        this.integrityVerifier = integrityVerifier;
        this.responseGate = responseGate;
    }

    /** Read-only physical reconciliation. Orphans and uncertain references are counted, never deleted. */
    public PhysicalInventory inspectPhysicalInventory() {
        Set<String> physical = new HashSet<>(integrityVerifier.listStoredKeys());
        Set<String> metadata = new HashSet<>();
        mongo
            .getCollection("archivo_adjunto")
            .find()
            .projection(new Document("url_archivo", 1))
            .forEach(document -> {
                if (document.get("url_archivo") instanceof String key) metadata.add(key);
            });
        Set<String> manifested = new HashSet<>();
        mongo
            .getCollection(AttachmentOperationRepository.COLLECTION)
            .find()
            .projection(new Document("physical_generations.entries.storage_key", 1))
            .forEach(document -> {
                Object rawGenerations = document.get("physical_generations");
                if (rawGenerations instanceof List<?> generations) {
                    for (Object rawGeneration : generations) {
                        if (rawGeneration instanceof Document generation && generation.get("entries") instanceof List<?> entries) {
                            for (Object rawEntry : entries) {
                                if (rawEntry instanceof Document entry && entry.get("storage_key") instanceof String key) manifested.add(
                                    key
                                );
                            }
                        }
                    }
                }
            });
        Set<String> referenced = new HashSet<>(metadata);
        referenced.addAll(manifested);
        long missing = referenced.stream().filter(key -> !physical.contains(key)).count();
        long unclassified = physical.stream().filter(key -> !referenced.contains(key)).count();
        return new PhysicalInventory(physical.size(), metadata.size(), manifested.size(), missing, unclassified);
    }

    public record PhysicalInventory(
        long physicalFiles,
        long metadataKeys,
        long manifestKeys,
        long missingReferences,
        long unclassifiedFiles
    ) {}

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
            if (finalizePublishedRoot(candidate, lease)) return;
            AttachmentPublicationRepository.Result publication = publicationVerifier.publishIfVerified(lease);
            if (
                publication == AttachmentPublicationRepository.Result.PUBLISHED ||
                publication == AttachmentPublicationRepository.Result.ALREADY_PUBLISHED
            ) {
                if (finalizePublishedRoot(candidate, lease)) return;
            }
            if (publication != AttachmentPublicationRepository.Result.UNCERTAIN) operations.requireReconciliation(lease);
            return;
        }

        String collection = candidate.getOperationType().name().equals("PUBLIC_CASE_CREATE") ? "pqrs" : "respuesta";
        Object rootId = "pqrs".equals(collection) ? candidate.getCaseId() : candidate.getResponseId();
        Document root = mongo.getCollection(collection).find(new Document("_id", rootId)).first();
        if (root == null && operations.abortBeforePersistence(lease).isPresent()) {
            releaseResponseGate(candidate);
            return;
        }
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
        boolean confirmed = operations
            .confirmPublished(
                lease,
                new PublishedRoot(
                    collection,
                    id,
                    operation.getId(),
                    operation.getPublicationId(),
                    publication.getString("digest"),
                    revision.longValue(),
                    number(publication.get("manifest_revision"), -1),
                    number(publication.get("fencing_token"), -1),
                    number(publication.get("selected_generation"), -1)
                )
            )
            .isPresent();
        if (confirmed) releaseResponseGate(operation);
        return confirmed;
    }

    private void releaseResponseGate(AttachmentOperation operation) {
        if (operation.getOperationType() == AttachmentOperationType.INSTITUTIONAL_RESPONSE_CREATE && operation.getCaseId() != null) {
            responseGate.release(operation.getCaseId().toString(), operation.getId());
        }
    }

    private static long number(Object value, long fallback) {
        return value instanceof Number number ? number.longValue() : fallback;
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
