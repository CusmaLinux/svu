package co.edu.itp.svu.service;

import co.edu.itp.svu.repository.AttachmentOwnershipRepository;
import co.edu.itp.svu.service.storage.AttachmentStorage;
import org.springframework.stereotype.Service;

/** Resolves evidence of ownership. A resolved document still requires authorization for its case and operation. */
@Service
public class AttachmentOwnershipResolver {

    public enum Status {
        DIRECT,
        RESPONSE,
        BOTH,
        HISTORICAL_INVERSE,
        UNASSOCIATED,
        INCONSISTENT,
        NOT_FOUND,
        SHARED_KEY,
        MISSING_FILE,
        UNSAFE_FILE,
        STORAGE_UNAVAILABLE,
    }

    public record Ownership(Status status, Object caseId, Object responseId) {}

    private final AttachmentOwnershipRepository repository;
    private final AttachmentStorage storage;

    public AttachmentOwnershipResolver(AttachmentOwnershipRepository repository, AttachmentStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    public Ownership resolve(String attachmentId) {
        if (attachmentId == null || attachmentId.isBlank()) {
            return rejected(Status.NOT_FOUND);
        }
        var candidates = repository.findCandidates(attachmentId);
        if (candidates.size() != 1) {
            return rejected(candidates.isEmpty() ? Status.NOT_FOUND : Status.INCONSISTENT);
        }
        var attachment = candidates.get(0);
        var direct = repository.existingCase(attachment.directReference());
        var indirect = repository.existingCase(repository.responseCaseReference(attachment.responseReference()));
        if (
            (attachment.directReference() != null && direct == null) ||
            (attachment.responseReference() != null && indirect == null) ||
            (direct != null && indirect != null && !direct.equals(indirect))
        ) {
            return rejected(Status.INCONSISTENT);
        }
        var inverse = repository.inverseCases(attachment.id());
        var caseId = direct != null ? direct : indirect;
        if (inverse.size() > 1 || (caseId != null && !inverse.isEmpty() && !caseId.equals(inverse.get(0)))) {
            return rejected(Status.INCONSISTENT);
        }
        if (caseId == null && inverse.isEmpty()) {
            return rejected(Status.UNASSOCIATED);
        }
        if (attachment.key() != null && repository.hasSharedKey(attachment)) {
            return rejected(Status.SHARED_KEY);
        }
        Status failure = checkFile(attachment.key());
        if (failure != null) {
            return rejected(failure);
        }
        if (caseId == null) {
            return new Ownership(Status.HISTORICAL_INVERSE, inverse.get(0), null);
        }
        var status = indirect == null ? Status.DIRECT : direct == null ? Status.RESPONSE : Status.BOTH;
        var responseId = attachment.responseReference() instanceof com.mongodb.DBRef ref ? ref.getId() : null;
        return new Ownership(status, caseId, responseId);
    }

    private Status checkFile(String key) {
        try (var stream = storage.open(key)) {
            return null;
        } catch (AttachmentStorage.StorageException e) {
            return switch (e.failure()) {
                case MISSING -> Status.MISSING_FILE;
                case INVALID_KEY, UNSAFE_ENTRY -> Status.UNSAFE_FILE;
                default -> Status.STORAGE_UNAVAILABLE;
            };
        } catch (java.io.IOException e) {
            return Status.STORAGE_UNAVAILABLE;
        }
    }

    private Ownership rejected(Status status) {
        return new Ownership(status, null, null);
    }
}
