package co.edu.itp.svu.service.report;

import co.edu.itp.svu.repository.PqrsStatisticsRepository.ResolvedOffice;
import co.edu.itp.svu.service.dto.PqrsStatistics.*;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.function.Function;

/** Closed, transport-independent projection for narrative comparison. */
final class PqrsStatisticsProjection {

    static final int MODEL = 1;
    static final int METHODOLOGY = 1;
    static final int CATALOG = 1;
    private static final DateTimeFormatter INSTANT = new DateTimeFormatterBuilder().appendInstant(3).toFormatter(Locale.ROOT);

    private PqrsStatisticsProjection() {}

    static Map<String, Object> filter(PqrsStatisticsFilter filter) {
        return fields(
            "start",
            filter.start().toString(),
            "end",
            filter.end().toString(),
            "scope",
            filter.scope().name(),
            "officeId",
            filter.officeId()
        );
    }

    static byte[] content(PqrsStatisticsService.Observation observation, List<String> available) {
        var s = observation.statistics();
        var p = s.period();
        ResolvedOffice office = observation.office();
        var root = fields(
            "schema",
            "svu.pqrs.stats/1",
            "modelVersion",
            MODEL,
            "methodologyVersion",
            METHODOLOGY,
            "catalogVersion",
            CATALOG,
            "filter",
            filter(s.filter()),
            "resolvedOffice",
            office == null ? null : fields("idType", office.idType(), "id", office.id(), "name", office.name()),
            "period",
            fields(
                "zone",
                p.zone(),
                "startInclusive",
                INSTANT.format(p.startInclusive()),
                "endExclusive",
                INSTANT.format(p.endExclusive()),
                "effectiveCutoff",
                INSTANT.format(p.effectiveCutoff())
            ),
            "received",
            s.received(),
            "types",
            sorted(s.types(), Comparator.comparing(TypeShare::code), t ->
                fields("code", t.code(), "count", t.count(), "denominator", t.denominator(), "percent", t.percent())
            ),
            "currentStates",
            s.currentStates(),
            "currentOffices",
            offices(s.currentOffices()),
            "months",
            sorted(s.months(), Comparator.comparing(Month::month), m ->
                fields(
                    "month",
                    m.month().toString(),
                    "received",
                    m.received(),
                    "complete",
                    m.complete(),
                    "previousCount",
                    m.previousCount(),
                    "comparison",
                    m.comparison(),
                    "variationPercent",
                    m.variationPercent(),
                    "types",
                    m.types(),
                    "currentOffices",
                    offices(m.currentOffices())
                )
            ),
            "availableIndicators",
            codes(available),
            "deferredIndicators",
            s.deferredIndicators(),
            "coverage",
            fields(
                "status",
                s.coverage().status(),
                "warnings",
                codes(s.coverage().warnings()),
                "limitations",
                codes(s.coverage().limitations())
            )
        );
        return bytes(root);
    }

    private static List<?> offices(List<OfficeCount> rows) {
        Comparator<String> nullable = Comparator.nullsFirst(Comparator.naturalOrder());
        return sorted(
            rows,
            Comparator.comparing(OfficeCount::category)
                .thenComparing(OfficeCount::idType, nullable)
                .thenComparing(OfficeCount::id, nullable),
            o -> fields("category", o.category(), "idType", o.idType(), "id", o.id(), "name", o.name(), "count", o.count())
        );
    }

    private static List<String> codes(List<String> codes) {
        return sorted(codes, Comparator.naturalOrder(), Function.identity());
    }

    private static <T, R> List<R> sorted(List<T> values, Comparator<? super T> order, Function<T, R> project) {
        var keys = new TreeSet<T>(order);
        keys.addAll(values);
        if (keys.size() != values.size()) throw new IllegalStateException("Duplicate statistical key");
        return keys.stream().map(project).toList();
    }

    private static Map<String, Object> fields(Object... pairs) {
        Map<String, Object> result = new TreeMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    static byte[] digest(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Digest unavailable");
        }
    }

    static byte[] bytes(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null || value instanceof Boolean) out.append(value);
        else if (value instanceof String text) quote(text, out);
        else if (value instanceof Integer || value instanceof Long) quote(value.toString(), out);
        else if (value instanceof BigDecimal decimal) quote(decimal.stripTrailingZeros().toPlainString(), out);
        else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (var entry : new TreeMap<>(map).entrySet()) {
                if (!first) out.append(',');
                quote((String) entry.getKey(), out);
                out.append(':');
                write(entry.getValue(), out);
                first = false;
            }
            out.append('}');
        } else if (value instanceof List<?> list) {
            out.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) out.append(',');
                write(list.get(i), out);
            }
            out.append(']');
        } else throw new IllegalStateException("Unknown canonical type");
    }

    private static void quote(String text, StringBuilder out) {
        out.append('"');
        text
            .codePoints()
            .forEach(c -> {
                if (c >= Character.MIN_SURROGATE && c <= Character.MAX_SURROGATE) throw new IllegalStateException("Invalid Unicode scalar");
                if (c < 32) out.append(String.format(Locale.ROOT, "\\u%04x", c));
                else {
                    if (c == '"' || c == '\\') out.append('\\');
                    out.appendCodePoint(c);
                }
            });
        out.append('"');
    }
}
