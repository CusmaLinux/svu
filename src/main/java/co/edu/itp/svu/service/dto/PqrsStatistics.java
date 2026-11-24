package co.edu.itp.svu.service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record PqrsStatistics(
    PqrsStatisticsFilter filter,
    Period period,
    Coverage coverage,
    long received,
    List<Month> months,
    List<TypeShare> types,
    Map<String, Long> currentStates,
    List<OfficeCount> currentOffices,
    Map<String, String> deferredIndicators
) {
    public record Period(String zone, Instant startInclusive, Instant endExclusive, Instant effectiveCutoff, Instant observedAt) {}
    public record Coverage(String status, List<String> warnings, List<String> limitations) {}
    public record TypeShare(String code, long count, long denominator, BigDecimal percent) {}
    public record OfficeCount(String category, String idType, String id, String name, long count) {}
    public record Month(
        YearMonth month,
        long received,
        boolean complete,
        Long previousCount,
        String comparison,
        BigDecimal variationPercent,
        Map<String, Long> types,
        List<OfficeCount> currentOffices
    ) {}
}
