package co.edu.itp.svu.service.dto;

import co.edu.itp.svu.domain.InvalidPqrsStatisticsFilter;
import java.time.LocalDate;
import java.util.Objects;

public record PqrsStatisticsFilter(LocalDate start, LocalDate end, Scope scope, String officeId) {
    public static final int MAX_OFFICE_ID_LENGTH = 128;

    public enum Scope {
        INSTITUTIONAL,
        OFFICE,
    }

    public PqrsStatisticsFilter {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(scope);
        if (start.getYear() < 1 || end.getYear() > 9999 || end.isBefore(start)) {
            throw new InvalidPqrsStatisticsFilter("invalidPeriod", "Invalid reporting period");
        }
        if (scope == Scope.OFFICE && (officeId == null || officeId.isBlank())) throw new InvalidPqrsStatisticsFilter(
            "officeRequired",
            "Office required"
        );
        if (officeId != null && officeId.length() > MAX_OFFICE_ID_LENGTH) {
            throw new InvalidPqrsStatisticsFilter("invalidOffice", "Invalid office identifier");
        }
        if (scope == Scope.INSTITUTIONAL && officeId != null) throw new InvalidPqrsStatisticsFilter(
            "unexpectedOffice",
            "Unexpected office"
        );
    }
}
