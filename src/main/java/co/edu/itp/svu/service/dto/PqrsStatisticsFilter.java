package co.edu.itp.svu.service.dto;

import java.time.LocalDate;
import java.util.Objects;

public record PqrsStatisticsFilter(LocalDate start, LocalDate end, Scope scope, String officeId) {
    public enum Scope {
        INSTITUTIONAL,
        OFFICE,
    }

    public PqrsStatisticsFilter {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(scope);
        if (end.isBefore(start)) throw new IllegalArgumentException("Invalid reporting period");
        if (scope == Scope.OFFICE && (officeId == null || officeId.isBlank())) throw new IllegalArgumentException("Office required");
        if (scope == Scope.INSTITUTIONAL && officeId != null) throw new IllegalArgumentException("Unexpected office");
    }
}
