package co.edu.itp.svu.domain.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the different types of PQRSD submissions.
 */
public enum PqrsType {
    REQUEST("Petición"),
    COMPLAINT("Queja"),
    CLAIM("Reclamo"),
    SUGGESTION("Sugerencia"),
    DEMAND("Demanda");

    private final String value;

    PqrsType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonValue
    public String toErrorCode() {
        return this.name();
    }

    @JsonCreator
    public static PqrsType fromValue(String value) {
        if (value == null) {
            return null;
        }

        try {
            return PqrsType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {}

        for (PqrsType type : PqrsType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown PqrsType: '" + value + "'");
    }

    @Override
    public String toString() {
        return this.value;
    }
}
