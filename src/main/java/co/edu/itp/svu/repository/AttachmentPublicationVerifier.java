package co.edu.itp.svu.repository;

import co.edu.itp.svu.domain.attachment.AttachmentIntegrityVerifier;
import co.edu.itp.svu.domain.attachment.AttachmentOperation;
import co.edu.itp.svu.domain.attachment.AttachmentOperation.GenerationState;
import co.edu.itp.svu.domain.attachment.AttachmentOperation.LogicalFile;
import co.edu.itp.svu.domain.attachment.AttachmentOperation.PhysicalEntry;
import co.edu.itp.svu.domain.attachment.AttachmentOperation.PhysicalGeneration;
import co.edu.itp.svu.domain.attachment.AttachmentOperationState;
import co.edu.itp.svu.domain.attachment.AttachmentOperationType;
import co.edu.itp.svu.repository.AttachmentOperationRepository.Lease;
import com.mongodb.DBRef;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/** Issues publication evidence only after rechecking manifest fencing, metadata ownership and physical bytes. */
@Component
public class AttachmentPublicationVerifier {

    private final AttachmentOperationRepository operations;
    private final AttachmentPublicationRepository publications;
    private final AttachmentIntegrityVerifier integrityVerifier;
    private final MongoTemplate mongo;

    public AttachmentPublicationVerifier(
        AttachmentOperationRepository operations,
        AttachmentPublicationRepository publications,
        AttachmentIntegrityVerifier integrityVerifier,
        MongoTemplate mongo
    ) {
        this.operations = operations;
        this.publications = publications;
        this.integrityVerifier = integrityVerifier;
        this.mongo = mongo;
    }

    public AttachmentPublicationRepository.Result publishIfVerified(Lease lease) {
        AttachmentOperation operation = operations.findCurrent(lease).orElse(null);
        if (operation == null) return AttachmentPublicationRepository.Result.NOT_READY;
        Root existing = root(operation);
        if (existing != null && committedForLease(existing.document(), operation, lease)) {
            return AttachmentPublicationRepository.Result.ALREADY_PUBLISHED;
        }
        Optional<VerifiedAttachmentPublication> proof = verifyAndBind(lease);
        return proof.map(publications::publish).orElse(AttachmentPublicationRepository.Result.NOT_READY);
    }

    private static boolean committedForLease(Document root, AttachmentOperation operation, Lease lease) {
        Document marker = root.get("publication", Document.class);
        return (
            marker != null &&
            "COMMITTED".equals(marker.getString("state")) &&
            operation.getId().equals(marker.getString("operation_id")) &&
            Objects.equals(operation.getPublicationId(), marker.getString("publication_id")) &&
            Objects.equals(operation.getRequestDigest(), marker.getString("digest")) &&
            number(marker.get("manifest_revision"), -1) == lease.version().revision() &&
            number(marker.get("fencing_token"), -1) == lease.fencing() &&
            Objects.equals(number(marker.get("selected_generation"), -1), operation.getSelectedPhysicalGeneration())
        );
    }

    private Optional<VerifiedAttachmentPublication> verifyAndBind(Lease lease) {
        AttachmentOperation operation = operations.findCurrent(lease).orElse(null);
        if (
            operation == null ||
            operation.getFormatVersion() != 2 ||
            operation.getState() != AttachmentOperationState.PERSISTING ||
            operation.getSelectedPhysicalGeneration() == null ||
            operation.getPublicationId() == null ||
            operation.getClaim() == null ||
            operation.getClaim().getFencingToken() != lease.fencing()
        ) return Optional.empty();

        Root root = root(operation);
        if (root == null || !publicationMatches(root.document(), operation)) return Optional.empty();
        Document publication = root.document().get("publication", Document.class);
        long publicationRevision = number(publication.get("revision"), -1);
        if (publicationRevision < 0 || !verifyParentOwnership(operation, root)) return Optional.empty();

        PhysicalGeneration selected = operation
            .getPhysicalGenerations()
            .stream()
            .filter(generation -> generation.generation() == operation.getSelectedPhysicalGeneration())
            .filter(generation -> generation.state() == GenerationState.VERIFIED)
            .findFirst()
            .orElse(null);
        if (selected == null || selected.entries().size() != operation.getLogicalFiles().size()) return Optional.empty();
        if (!verifyFiles(operation, root, selected.entries())) return Optional.empty();

        if (!publications.bindLease(lease, root.collection(), root.id())) return Optional.empty();
        AttachmentOperation stillCurrent = operations.findCurrent(lease).orElse(null);
        if (
            stillCurrent == null || !Objects.equals(stillCurrent.getSelectedPhysicalGeneration(), operation.getSelectedPhysicalGeneration())
        ) {
            return Optional.empty();
        }
        Document latestRoot = mongo.getCollection(root.collection()).find(new Document("_id", root.id())).first();
        Document latestPublication = latestRoot == null ? null : latestRoot.get("publication", Document.class);
        if (latestPublication == null || !publicationMatches(latestRoot, stillCurrent)) return Optional.empty();
        Date leaseUntil = latestPublication.getDate("lease_until");
        if (leaseUntil == null || !sameBinding(latestPublication, stillCurrent, lease)) return Optional.empty();
        long rootRevision = number(latestPublication.get("revision"), -1);
        if (rootRevision < 0) return Optional.empty();
        return Optional.of(
            new VerifiedAttachmentPublication(
                root.collection(),
                root.id(),
                stillCurrent.getId(),
                stillCurrent.getPublicationId(),
                stillCurrent.getRequestDigest(),
                rootRevision,
                lease.version().revision(),
                lease.fencing(),
                stillCurrent.getSelectedPhysicalGeneration(),
                leaseUntil
            )
        );
    }

    private boolean verifyFiles(AttachmentOperation operation, Root root, List<PhysicalEntry> entries) {
        List<LogicalFile> logicalFiles = operation.getLogicalFiles();
        Set<ObjectId> expectedIds = new HashSet<>();
        Set<ObjectId> rootIds = root.collection().equals("pqrs") ? rootAttachmentIds(root.document()) : null;
        if (root.collection().equals("pqrs") && rootIds == null) return false;
        for (int i = 0; i < logicalFiles.size(); i++) {
            LogicalFile logical = logicalFiles.get(i);
            PhysicalEntry physical = entries.get(i);
            if (
                logical.metadataId() == null ||
                !expectedIds.add(logical.metadataId()) ||
                !logical.logicalId().equals(physical.logicalId()) ||
                physical.verifiedAt() == null ||
                physical.observedSizeBytes() == null ||
                physical.observedSizeBytes() != logical.expectedSizeBytes() ||
                !Objects.equals(physical.observedSha256(), logical.expectedSha256()) ||
                !integrityVerifier.isPersistedVerified(
                    logical.logicalId(),
                    physical.storageKey(),
                    logical.expectedSizeBytes(),
                    logical.expectedSha256()
                )
            ) return false;
            Document metadata = mongo.getCollection("archivo_adjunto").find(new Document("_id", logical.metadataId())).first();
            if (metadata == null || !physical.storageKey().equals(metadata.getString("url_archivo"))) return false;
            if (root.collection().equals("pqrs")) {
                if (!rootIds.contains(logical.metadataId()) || !refersTo(metadata.get("pqrs_attachment"), "pqrs", root.id())) return false;
            } else {
                if (!refersTo(metadata.get("response_attachment"), "respuesta", root.id())) return false;
                Object caseReference = metadata.get("pqrs_attachment");
                if (caseReference != null && !refersTo(caseReference, "pqrs", operation.getCaseId())) return false;
            }
        }
        if (root.collection().equals("respuesta")) {
            Set<ObjectId> responseIds = responseAttachmentIds(root.id());
            if (responseIds == null || !responseIds.equals(expectedIds)) return false;
        }
        return root.collection().equals("pqrs") ? rootIds.equals(expectedIds) : true;
    }

    private boolean verifyParentOwnership(AttachmentOperation operation, Root root) {
        if (root.collection().equals("pqrs")) return Objects.equals(root.id(), operation.getCaseId());
        if (!Objects.equals(root.id(), operation.getResponseId())) return false;
        Document parent = mongo.getCollection("pqrs").find(new Document("_id", operation.getCaseId())).first();
        if (parent == null || !isPublished(parent)) return false;
        return refersTo(root.document().get("pqrs"), "pqrs", operation.getCaseId());
    }

    private Root root(AttachmentOperation operation) {
        String collection = operation.getOperationType() == AttachmentOperationType.PUBLIC_CASE_CREATE ? "pqrs" : "respuesta";
        Object id = collection.equals("pqrs") ? operation.getCaseId() : operation.getResponseId();
        if (id == null) return null;
        Document document = mongo.getCollection(collection).find(new Document("_id", id)).first();
        return document == null ? null : new Root(collection, id, document);
    }

    private boolean publicationMatches(Document root, AttachmentOperation operation) {
        Document marker = root.get("publication", Document.class);
        return (
            marker != null &&
            "PREPARING".equals(marker.getString("state")) &&
            operation.getId().equals(marker.getString("operation_id")) &&
            operation.getPublicationId().equals(marker.getString("publication_id")) &&
            operation.getRequestDigest().equals(marker.getString("digest"))
        );
    }

    private static boolean sameBinding(Document marker, AttachmentOperation operation, Lease lease) {
        return (
            number(marker.get("manifest_revision"), -1) == lease.version().revision() &&
            number(marker.get("fencing_token"), -1) == lease.fencing() &&
            number(marker.get("selected_generation"), -1) == operation.getSelectedPhysicalGeneration()
        );
    }

    private static boolean isPublished(Document root) {
        Document marker = root.get("publication", Document.class);
        return marker == null || "COMMITTED".equals(marker.getString("state"));
    }

    private Set<ObjectId> rootAttachmentIds(Document root) {
        Object raw = root.get("archivosAdjuntos");
        if (raw == null) return Set.of();
        if (!(raw instanceof List<?> refs)) return null;
        Set<ObjectId> ids = new HashSet<>();
        for (Object ref : refs) {
            Object id = refId(ref, "archivo_adjunto");
            if (!(id instanceof ObjectId objectId) || !ids.add(objectId)) return null;
        }
        return ids;
    }

    private Set<ObjectId> responseAttachmentIds(Object responseId) {
        Set<ObjectId> ids = new HashSet<>();
        List<Document> files = mongo
            .getCollection("archivo_adjunto")
            .find(new Document("response_attachment.$id", responseId))
            .into(new ArrayList<>());
        for (Document file : files) {
            if (!(file.get("_id") instanceof ObjectId id) || !ids.add(id)) return null;
        }
        return ids;
    }

    private static boolean refersTo(Object value, String collection, Object id) {
        return Objects.equals(refId(value, collection), id);
    }

    private static Object refId(Object value, String collection) {
        if (value instanceof DBRef ref) return collection.equals(ref.getCollectionName()) ? ref.getId() : null;
        if (value instanceof Document document) {
            return collection.equals(document.getString("$ref")) ? document.get("$id") : null;
        }
        return null;
    }

    private static long number(Object value, long fallback) {
        return value instanceof Number number ? number.longValue() : fallback;
    }

    private record Root(String collection, Object id, Document document) {}
}
