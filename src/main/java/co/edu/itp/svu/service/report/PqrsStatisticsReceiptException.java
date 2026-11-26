package co.edu.itp.svu.service.report;

/** Stable safe error without request data or parser/cryptography causes. */
public final class PqrsStatisticsReceiptException extends RuntimeException {

    private final int status;

    public PqrsStatisticsReceiptException(int status, String code) {
        super(code);
        this.status = status;
    }

    public int status() {
        return status;
    }

    static PqrsStatisticsReceiptException missing() {
        return new PqrsStatisticsReceiptException(404, "previewNotFound");
    }

    static PqrsStatisticsReceiptException unavailable() {
        return new PqrsStatisticsReceiptException(503, "exportUnavailable");
    }
}
