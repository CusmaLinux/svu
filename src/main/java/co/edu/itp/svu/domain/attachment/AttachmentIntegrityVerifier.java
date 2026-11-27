package co.edu.itp.svu.domain.attachment;

import java.util.Set;

/** Answers whether a selected physical object still matches its persisted size and digest. */
public interface AttachmentIntegrityVerifier {
    boolean isPersistedVerified(String logicalId, String storageKey, long expectedSize, String expectedSha256);

    Set<String> listStoredKeys();
}
