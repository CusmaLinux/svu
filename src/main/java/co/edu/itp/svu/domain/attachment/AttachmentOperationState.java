package co.edu.itp.svu.domain.attachment;

public enum AttachmentOperationState {
    PREPARING,
    FILES_READY,
    PERSISTING,
    COMMITTED,
    ABORTED,
    RECONCILIATION_REQUIRED,
}
