package co.edu.itp.svu.domain.attachment;

import java.util.Set;

public enum AttachmentOperationState {
    PREPARING,
    FILES_READY,
    PERSISTING,
    COMMITTED,
    ABORTED,
    RECONCILIATION_REQUIRED;

    public static final Set<AttachmentOperationState> RECOVERABLE = Set.of(PREPARING, FILES_READY, PERSISTING);

    /** Publication evidence cannot yet be obtained, so COMMITTED has no incoming transition. */
    public boolean permits(AttachmentOperationState next) {
        return switch (this) {
            case PREPARING -> next == FILES_READY || next == ABORTED || next == RECONCILIATION_REQUIRED;
            case FILES_READY -> next == PERSISTING || next == ABORTED || next == RECONCILIATION_REQUIRED;
            case PERSISTING -> next == RECONCILIATION_REQUIRED;
            default -> false;
        };
    }
}
