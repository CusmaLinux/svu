package co.edu.itp.svu.repository;

import co.edu.itp.svu.repository.AttachmentOperationRepository.PhysicalWriteContext;
import java.util.List;
import java.util.Optional;

/** Persistence port implemented by the configured attachment store. */
public interface PhysicalPreparationVerifier {
    interface Evidence {}

    record Observation(String logicalId, String storageKey, long size, String sha256) {}

    /** Data extracted after authenticating the opaque evidence at the storage boundary. */
    record VerifiedPreparation(PhysicalWriteContext context, List<Observation> observations) {
        public VerifiedPreparation {
            observations = List.copyOf(observations);
        }
    }

    Optional<VerifiedPreparation> verifyPreparation(Evidence evidence);
}
