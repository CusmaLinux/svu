package co.edu.itp.svu.domain.enumeration;

public enum PqrsStatus {
    PENDING("PENDIENTE"),
    IN_PROGRESS("EN PROGRESO"),
    RESOLVED("RESUELTA"),
    CLOSED("CERRADA"),
    REJECTED("RECHAZADA");

    private final String displayName;

    PqrsStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static PqrsStatus fromDisplayName(String displayName) {
        for (PqrsStatus status : PqrsStatus.values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        try {
            return PqrsStatus.valueOf(displayName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("No PQRS status has been found with name: " + displayName);
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}
