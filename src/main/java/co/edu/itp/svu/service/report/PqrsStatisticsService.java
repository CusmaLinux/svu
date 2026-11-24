package co.edu.itp.svu.service.report;

import co.edu.itp.svu.domain.InvalidPqrsStatisticsFilter;
import co.edu.itp.svu.domain.enumeration.PqrsStatus;
import co.edu.itp.svu.domain.enumeration.PqrsType;
import co.edu.itp.svu.repository.PqrsStatisticsRepository;
import co.edu.itp.svu.repository.PqrsStatisticsRepository.*;
import co.edu.itp.svu.security.PqrsAuthorizationService;
import co.edu.itp.svu.service.dto.PqrsStatistics;
import co.edu.itp.svu.service.dto.PqrsStatistics.*;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PqrsStatisticsService {

    private static final int DEFAULT_MAXIMUM_DAYS = 366;
    private static final ZoneId ZONE = ZoneId.of("America/Bogota");
    private static final Map<String, String> DEFERRED = Map.ofEntries(
        Map.entry("IND-08", "PARTIAL_SOURCE"),
        Map.entry("IND-09", "SOURCE_UNAVAILABLE"),
        Map.entry("IND-10", "PARTIAL_SOURCE"),
        Map.entry("IND-11", "SOURCE_UNAVAILABLE"),
        Map.entry("IND-12", "SOURCE_UNAVAILABLE"),
        Map.entry("IND-13", "PARTIAL_SOURCE"),
        Map.entry("IND-14", "DEFINITION_PENDING"),
        Map.entry("IND-15", "DEFINITION_PENDING"),
        Map.entry("IND-16", "SOURCE_UNAVAILABLE"),
        Map.entry("IND-17", "SOURCE_UNAVAILABLE"),
        Map.entry("IND-18", "SOURCE_UNAVAILABLE"),
        Map.entry("IND-19", "SOURCE_UNAVAILABLE")
    );

    private final PqrsStatisticsRepository repository;
    private final PqrsAuthorizationService authorization;
    private final Clock clock;
    private final int maximumDays;

    @Autowired
    public PqrsStatisticsService(
        PqrsStatisticsRepository repository,
        PqrsAuthorizationService authorization,
        @Value("${svu.pqrs-statistics.max-period-days:366}") int maximumDays
    ) {
        this(repository, authorization, Clock.systemUTC(), maximumDays);
    }

    public PqrsStatisticsService(PqrsStatisticsRepository repository, PqrsAuthorizationService authorization) {
        this(repository, authorization, DEFAULT_MAXIMUM_DAYS);
    }

    PqrsStatisticsService(PqrsStatisticsRepository repository, PqrsAuthorizationService authorization, Clock clock) {
        this(repository, authorization, clock, DEFAULT_MAXIMUM_DAYS);
    }

    PqrsStatisticsService(PqrsStatisticsRepository repository, PqrsAuthorizationService authorization, Clock clock, int maximumDays) {
        if (maximumDays < 1) throw new IllegalArgumentException("Positive reporting period limit required");
        this.repository = repository;
        this.authorization = authorization;
        this.clock = clock;
        this.maximumDays = maximumDays;
    }

    public record Configuration(
        List<PqrsStatisticsFilter.Scope> scopes,
        String zone,
        int maxPeriodDays,
        int maxOfficeIdLength,
        List<String> availableIndicators,
        Map<String, String> deferredIndicators,
        List<String> limitations
    ) {}

    private static final List<String> LIMITATIONS = List.of(
        "ONLY_DATED_ROOTS",
        "OFFICE_AND_STATE_AT_OBSERVATION",
        "HISTORICAL_COMPLETENESS_UNMEASURED",
        "OBSERVATION_NOT_TRANSACTIONAL"
    );

    public Configuration configuration() {
        authorization.requireStatisticalAccess();
        return new Configuration(
            List.of(PqrsStatisticsFilter.Scope.values()),
            ZONE.getId(),
            maximumDays,
            PqrsStatisticsFilter.MAX_OFFICE_ID_LENGTH,
            List.of("IND-01", "IND-02", "IND-03", "IND-04", "IND-05", "IND-06", "IND-07"),
            DEFERRED,
            LIMITATIONS
        );
    }

    public PqrsStatistics calculate(PqrsStatisticsFilter filter) {
        authorization.requireStatisticalAccess();
        Instant observed = clock.instant().truncatedTo(ChronoUnit.MILLIS);
        if (filter.end().isAfter(observed.atZone(ZONE).toLocalDate())) {
            throw new InvalidPqrsStatisticsFilter("futurePeriod", "Future reporting date");
        }
        if (ChronoUnit.DAYS.between(filter.start(), filter.end()) + 1 > maximumDays) {
            throw new InvalidPqrsStatisticsFilter("periodTooLong", "Reporting period exceeds the maximum number of days");
        }
        YearMonth first = YearMonth.from(filter.start());
        Instant end = startOfDay(filter.end().plusDays(1));
        Instant cutoff = end.isAfter(observed) ? observed : end;
        Window window = new Window(
            startOfDay(first.minusMonths(1).atDay(1)),
            startOfDay(first.atDay(1)),
            startOfDay(filter.start()),
            cutoff,
            ZONE
        );
        var buckets =
            switch (filter.scope()) {
                case INSTITUTIONAL -> repository.institutional(window);
                case OFFICE -> repository.office(window, filter.officeId());
            };
        var totals = new Counts();
        var byMonth = new HashMap<YearMonth, Counts>();
        long baseline = 0;
        for (Bucket bucket : buckets) {
            if (bucket.month().isBefore(first)) {
                baseline += bucket.count();
            } else {
                totals.add(bucket);
                byMonth.computeIfAbsent(bucket.month(), ignored -> new Counts()).add(bucket);
            }
        }

        List<PqrsStatistics.Month> series = new ArrayList<>();
        Long previous = baseline;
        for (YearMonth month = first; !month.isAfter(YearMonth.from(filter.end())); month = month.plusMonths(1)) {
            Counts counts = byMonth.getOrDefault(month, new Counts());
            boolean complete = !month.atDay(1).isBefore(filter.start()) && !startOfDay(month.plusMonths(1).atDay(1)).isAfter(cutoff);
            String comparison;
            BigDecimal variation = null;
            if (!complete) comparison = "PARTIAL_MONTH";
            else if (previous == null) comparison = "NO_COMPARABLE_BASE";
            else if (previous == 0) comparison = "NO_CALCULABLE";
            else {
                comparison = "CALCULATED";
                variation = percent(counts.total - previous, previous);
            }
            series.add(
                new PqrsStatistics.Month(
                    month,
                    counts.total,
                    complete,
                    previous,
                    comparison,
                    variation,
                    Map.copyOf(counts.types),
                    counts.officeRows()
                )
            );
            previous = complete ? Long.valueOf(counts.total) : null;
        }
        List<TypeShare> shares = totals.types
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new TypeShare(e.getKey(), e.getValue(), totals.total, totals.total == 0 ? null : percent(e.getValue(), totals.total)))
            .toList();
        List<String> warnings = new ArrayList<>();
        for (String code : List.of("HISTORICALLY_AMBIGUOUS", "MISSING", "UNKNOWN")) {
            if (totals.types.getOrDefault(code, 0L) > 0) warnings.add("TYPE_" + code);
        }
        if (totals.states.containsKey("UNKNOWN")) warnings.add("STATE_UNKNOWN");
        totals
            .officeRows()
            .stream()
            .map(OfficeCount::category)
            .distinct()
            .filter(c -> !c.equals("ASSIGNED"))
            .forEach(c -> warnings.add("OFFICE_" + c));
        if (cutoff.isBefore(end)) warnings.add("PROVISIONAL_PERIOD");
        var coverage = new Coverage(warnings.isEmpty() ? "OBSERVED" : "PARTIAL", List.copyOf(warnings), LIMITATIONS);
        return new PqrsStatistics(
            filter,
            new PqrsStatistics.Period(ZONE.getId(), window.start(), end, cutoff, observed),
            coverage,
            totals.total,
            List.copyOf(series),
            shares,
            Map.copyOf(totals.states),
            totals.officeRows(),
            DEFERRED
        );
    }

    private static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant();
    }

    private static BigDecimal percent(long numerator, long denominator) {
        return BigDecimal.valueOf(numerator).movePointRight(2).divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private static final class Counts {

        long total;
        final Map<String, Long> types = new LinkedHashMap<>();
        final Map<String, Long> states = new LinkedHashMap<>();
        final Map<Office, Long> offices = new HashMap<>();

        Counts() {
            for (var type : PqrsType.values()) types.put(type.name(), 0L);
            for (var state : PqrsStatus.values()) states.put(state.name(), 0L);
        }

        void add(Bucket bucket) {
            total += bucket.count();
            types.merge(bucket.type(), bucket.count(), Long::sum);
            states.merge(bucket.state(), bucket.count(), Long::sum);
            offices.merge(bucket.office(), bucket.count(), Long::sum);
        }

        List<OfficeCount> officeRows() {
            return offices
                .entrySet()
                .stream()
                .map(entry -> {
                    Office office = entry.getKey();
                    return new OfficeCount(office.category(), office.idType(), office.id(), office.name(), entry.getValue());
                })
                .sorted(
                    Comparator.comparing(OfficeCount::category)
                        .thenComparing(OfficeCount::idType, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(OfficeCount::id, Comparator.nullsFirst(Comparator.naturalOrder()))
                )
                .toList();
        }
    }
}
