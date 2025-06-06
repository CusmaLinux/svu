package co.edu.itp.svu.domain.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the different types of PQRSD submissions.
 *
 * Each enum constant serves as a stable, internal identifier (e.g.,
 * {@code REQUEST}),
 * while its associated string value is used for API serialization and
 * user-facing display.
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

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PqrsType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (PqrsType type : PqrsType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        try {
            return PqrsType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown PqrsType: '" + value + "'");
        }
    }

    @Override
    public String toString() {
        return this.value;
    }
}
