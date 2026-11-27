package co.edu.itp.svu.service.report;

import static co.edu.itp.svu.service.report.PqrsStatisticsDocumentCatalog.*;

import co.edu.itp.svu.service.dto.PqrsStatistics;
import co.edu.itp.svu.service.dto.PqrsStatistics.OfficeCount;
import co.edu.itp.svu.service.report.PqrsStatisticsDocument.*;
import co.edu.itp.svu.service.report.PqrsStatisticsNarrativeService.Checked;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

final class PqrsStatisticsDocumentAssembler {

    private static final List<String> SECTION_TITLES = List.of(
        "Introducción y marco normativo",
        "Consolidado de PQRSD por tipología y periodo",
        "Caracterización de usuarios y canales",
        "Direccionamiento y traslados",
        "Distribución por dependencia y mes",
        "Indicadores de gestión, oportunidad de respuesta y satisfacción",
        "Conclusiones y recomendaciones"
    );
    private static final List<TableId> EVIDENCE = List.of(
        TableId.SUMMARY,
        TableId.EVOLUTION,
        TableId.TYPOLOGIES,
        TableId.TYPOLOGIES,
        TableId.VARIATIONS,
        TableId.OFFICES,
        TableId.STATES
    );
    private static final String[] PALETTE = { "16746D", "355A9C", "A36123", "6456A0", "A13760", "426535", "555B65", "926528" };
    private static final DateTimeFormatter SHORT_MONTH = DateTimeFormatter.ofPattern("MM/uuuu", Locale.ROOT);
    static final int OFFICE_BARS = 16;
    static final int OFFICE_SERIES = 8;

    private PqrsStatisticsDocumentAssembler() {}

    static PqrsStatisticsDocument assemble(Checked checked, PqrsStatisticsNarrativeText narrative, Instant generatedAt) {
        var report = checked.statistics();
        var metadata = new Metadata(
            UUID.randomUUID(),
            INSTITUTION,
            "Informe de gestión PQRSD",
            Status.PRELIMINARY,
            report.filter(),
            checked.resolvedOffice(),
            report.period(),
            checked.checkedAt(),
            generatedAt,
            checked.modelVersion(),
            checked.methodologyVersion(),
            checked.catalogVersion(),
            label(report.coverage().status()),
            notices(report.coverage().warnings()),
            notices(report.coverage().limitations())
        );
        var indicators = indicators(checked);
        var tables = tables(report, metadata, indicators);
        var charts = charts(report, tables);
        return new PqrsStatisticsDocument(
            metadata,
            List.of(
                section(1, narrative, List.of(), tables, charts, TableId.SUMMARY),
                section(
                    2,
                    narrative,
                    indicators.subList(0, 5),
                    tables,
                    charts,
                    TableId.EVOLUTION,
                    TableId.TYPOLOGIES,
                    TableId.STATES,
                    TableId.MONTHLY_TYPES,
                    TableId.VARIATIONS
                ),
                section(3, narrative, indicators.subList(16, 19), tables, charts),
                section(4, narrative, indicators.subList(15, 16), tables, charts),
                section(5, narrative, indicators.subList(5, 6), tables, charts, TableId.OFFICES, TableId.MONTHLY_OFFICES),
                section(6, narrative, indicators.subList(6, 15), tables, charts, TableId.INDICATORS, TableId.METHODOLOGY),
                section(7, narrative, List.of(), tables, charts)
            )
        );
    }

    private static Section section(
        int number,
        PqrsStatisticsNarrativeText narrative,
        List<Indicator> indicators,
        Map<TableId, Table> tables,
        Map<TableId, Chart> charts,
        TableId... ids
    ) {
        return new Section(
            number,
            SECTION_TITLES.get(number - 1),
            narrative.section(number),
            indicators,
            Arrays.stream(ids).map(tables::get).toList(),
            Arrays.stream(ids).map(charts::get).filter(Objects::nonNull).toList()
        );
    }

    private static List<Notice> notices(List<String> codes) {
        return codes.stream().sorted().map(code -> new Notice(code, label(code))).toList();
    }

    private static List<Indicator> indicators(Checked checked) {
        var report = checked.statistics();
        return IntStream.range(0, INDICATORS.size())
            .mapToObj(index -> {
                String code = String.format(Locale.ROOT, "IND-%02d", index + 1);
                boolean available = checked.availableIndicators().contains(code);
                var evidence = available && index < EVIDENCE.size() ? EVIDENCE.get(index) : null;
                var availability = available
                    ? Availability.AVAILABLE
                    : Availability.valueOf(report.deferredIndicators().getOrDefault(code, "SOURCE_UNAVAILABLE"));
                if (
                    available &&
                    (index == 2 || index == 3) &&
                    report.coverage().warnings().stream().anyMatch(key -> key.startsWith("TYPE_"))
                ) {
                    availability = Availability.PARTIAL_SOURCE;
                }
                return new Indicator(code, INDICATORS.get(index), availability, INDICATOR_NOTES.get(index), evidence);
            })
            .toList();
    }

    private static Map<TableId, Table> tables(PqrsStatistics r, Metadata metadata, List<Indicator> indicators) {
        var tables = new EnumMap<TableId, Table>(TableId.class);
        var total = new Quantity(r.received());
        var countsTotal = row(new Text(TOTAL), total);
        var states = new TreeMap<>(r.currentStates());
        var types = r.types().stream().sorted(Comparator.comparing(PqrsStatistics.TypeShare::code)).toList();
        var months = r.months().stream().sorted(Comparator.comparing(PqrsStatistics.Month::month)).toList();
        var offices = new OfficeCategories(r.currentOffices());
        tables.put(
            TableId.SUMMARY,
            table(
                TableId.SUMMARY,
                "Resumen general",
                columns("Concepto", ColumnType.TEXT, "Solicitudes", ColumnType.COUNT),
                List.of(row(new Text("Solicitudes recibidas"), total)),
                null,
                "Informe preliminar."
            )
        );
        tables.put(
            TableId.EVOLUTION,
            table(
                TableId.EVOLUTION,
                "Evolución mensual",
                columns("Mes", ColumnType.MONTH, "Recibidas", ColumnType.COUNT),
                months.stream().map(m -> row(month(m), new Quantity(m.received()))).toList(),
                countsTotal,
                "El asterisco indica un mes parcial."
            )
        );
        tables.put(
            TableId.TYPOLOGIES,
            table(
                TableId.TYPOLOGIES,
                "Distribución por tipología",
                columns(
                    "Tipología",
                    ColumnType.CATEGORY,
                    "Cantidad",
                    ColumnType.COUNT,
                    "Denominador",
                    ColumnType.COUNT,
                    "Porcentaje",
                    ColumnType.PERCENT
                ),
                types
                    .stream()
                    .map(t ->
                        row(
                            category(t.code()),
                            new Quantity(t.count()),
                            new Quantity(t.denominator()),
                            t.percent() == null ? Missing.NO_CALCULABLE : new Percentage(t.percent())
                        )
                    )
                    .toList(),
                row(new Text(TOTAL), total, total, totalTypePercentage(types, r.received())),
                "El denominador es el total recibido. Los porcentajes redondeados se muestran por separado."
            )
        );
        tables.put(
            TableId.STATES,
            table(
                TableId.STATES,
                "Estados actuales",
                columns("Estado actual", ColumnType.CATEGORY, "Solicitudes", ColumnType.COUNT),
                states.entrySet().stream().map(e -> row(category(e.getKey()), new Quantity(e.getValue()))).toList(),
                countsTotal,
                "El estado es el observado al consultar."
            )
        );
        tables.put(
            TableId.OFFICES,
            table(
                TableId.OFFICES,
                "Distribución por dependencias",
                columns("Dependencia actual", ColumnType.CATEGORY, "Solicitudes", ColumnType.COUNT),
                offices.sorted.stream().map(o -> row(offices.category(o), new Quantity(o.count()))).toList(),
                countsTotal,
                "La dependencia es la observada al consultar."
            )
        );
        tables.put(TableId.MONTHLY_TYPES, monthlyTypes(months, types, total));
        tables.put(
            TableId.MONTHLY_OFFICES,
            table(
                TableId.MONTHLY_OFFICES,
                "Dependencias por mes",
                columns("Mes", ColumnType.MONTH, "Dependencia actual", ColumnType.CATEGORY, "Solicitudes", ColumnType.COUNT),
                months
                    .stream()
                    .flatMap(m ->
                        m
                            .currentOffices()
                            .stream()
                            .sorted(OfficeCategories.ORDER)
                            .map(o -> row(month(m), offices.category(o), new Quantity(o.count())))
                    )
                    .toList(),
                row(new Text(TOTAL), Missing.NULL_VALUE, total),
                "El mes corresponde a la fecha de recepción. El asterisco indica un mes parcial."
            )
        );
        tables.put(
            TableId.VARIATIONS,
            table(
                TableId.VARIATIONS,
                "Variaciones mensuales",
                columns(
                    "Mes",
                    ColumnType.MONTH,
                    "Recibidas",
                    ColumnType.COUNT,
                    "Mes previo",
                    ColumnType.COUNT,
                    "Variación",
                    ColumnType.PERCENT,
                    "Comparabilidad",
                    ColumnType.STATUS
                ),
                months
                    .stream()
                    .map(m ->
                        row(
                            month(m),
                            new Quantity(m.received()),
                            m.previousCount() == null ? Missing.NULL_VALUE : new Quantity(m.previousCount()),
                            m.variationPercent() == null ? Missing.valueOf(m.comparison()) : new Percentage(m.variationPercent()),
                            state(m.comparison())
                        )
                    )
                    .toList(),
                null,
                "Cada variación utiliza la base comparable del motor estadístico. El asterisco indica un mes parcial."
            )
        );
        tables.put(
            TableId.INDICATORS,
            table(
                TableId.INDICATORS,
                "Disponibilidad de indicadores",
                columns(
                    "Código",
                    ColumnType.TEXT,
                    "Indicador",
                    ColumnType.TEXT,
                    "Disponibilidad",
                    ColumnType.STATUS,
                    "Explicación",
                    ColumnType.TEXT
                ),
                indicators
                    .stream()
                    .map(i ->
                        row(
                            new Text(i.code()),
                            new Text(i.name()),
                            new State(i.availability().name(), i.availability().label()),
                            new Text(i.explanation())
                        )
                    )
                    .toList(),
                null
            )
        );
        var methodology = new ArrayList<Row>();
        methodology.add(row(new Text("Periodo de recepción"), new Text("El inicio es inclusivo y el corte efectivo es exclusivo.")));
        methodology.add(row(new Text("Zona horaria"), new Text(r.period().zone())));
        methodology.add(row(new Text("Cobertura"), new Text(metadata.coverage())));
        metadata.warnings().forEach(n -> methodology.add(row(new Text("Advertencia"), new Text(n.explanation()))));
        metadata.limitations().forEach(n -> methodology.add(row(new Text("Limitación"), new Text(n.explanation()))));
        tables.put(
            TableId.METHODOLOGY,
            table(
                TableId.METHODOLOGY,
                "Advertencias y metodología",
                columns("Concepto", ColumnType.TEXT, "Detalle", ColumnType.TEXT),
                methodology,
                null
            )
        );
        return tables;
    }

    private static Table monthlyTypes(List<PqrsStatistics.Month> months, List<PqrsStatistics.TypeShare> types, Quantity total) {
        var columns = new ArrayList<Column>();
        columns.add(new Column("Mes", ColumnType.MONTH));
        types.forEach(t -> columns.add(new Column(label(t.code()), ColumnType.COUNT)));
        columns.add(new Column("Total mensual", ColumnType.COUNT));
        var rows = months
            .stream()
            .map(m -> {
                var cells = new ArrayList<Cell>();
                cells.add(month(m));
                types.forEach(t -> cells.add(monthlyTypeCount(m, t.code())));
                cells.add(new Quantity(m.received()));
                return new Row(cells);
            })
            .toList();
        var totals = new ArrayList<Cell>();
        totals.add(new Text(TOTAL));
        types.forEach(t -> totals.add(new Quantity(t.count())));
        totals.add(total);
        return table(TableId.MONTHLY_TYPES, "Tipologías por mes", columns, rows, new Row(totals), "El asterisco indica un mes parcial.");
    }

    private static Map<TableId, Chart> charts(PqrsStatistics report, Map<TableId, Table> tables) {
        var charts = new EnumMap<TableId, Chart>(TableId.class);
        for (var id : List.of(TableId.EVOLUTION, TableId.TYPOLOGIES, TableId.STATES, TableId.OFFICES, TableId.VARIATIONS)) {
            var table = tables.get(id);
            boolean monthly = id == TableId.EVOLUTION || id == TableId.VARIATIONS;
            var rows = id == TableId.OFFICES ? table.rows().stream().limit(OFFICE_BARS).toList() : table.rows();
            var notes = new ArrayList<>(table.notes());
            if (rows.size() < table.rows().size()) notes.add(selectionNote(rows.size(), table.rows().size()));
            boolean variation = id == TableId.VARIATIONS;
            var points = rows.stream().map(r -> point(r.cells().get(variation ? 3 : 1))).toList();
            if (points.stream().noneMatch(p -> p.value() != null)) continue;
            var categories = IntStream.range(0, rows.size())
                .mapToObj(index ->
                    chartCategory(rows.get(index).cells().get(0), id == TableId.OFFICES ? PALETTE[index % PALETTE.length] : null)
                )
                .toList();
            charts.put(
                id,
                new Chart(
                    id,
                    variation ? ChartKind.LINE : monthly ? ChartKind.COLUMNS : ChartKind.BARS,
                    variation ? Unit.PERCENT : Unit.REQUESTS,
                    table.title(),
                    categories,
                    List.of(new Series(variation ? "Variación" : "Solicitudes", rgb("16746D"), points)),
                    notes
                )
            );
        }
        var monthTypes = tables.get(TableId.MONTHLY_TYPES);
        var codes = report.types().stream().map(PqrsStatistics.TypeShare::code).sorted().toList();
        var months = monthTypes.rows().stream().map(r -> chartCategory(r.cells().get(0), null)).toList();
        if (!codes.isEmpty() && !months.isEmpty()) {
            var series = IntStream.range(0, codes.size())
                .mapToObj(index ->
                    new Series(
                        label(codes.get(index)),
                        rgb(COLORS.getOrDefault(codes.get(index), PALETTE[index % PALETTE.length])),
                        monthTypes.rows().stream().map(r -> point(r.cells().get(index + 1))).toList()
                    )
                )
                .toList();
            charts.put(
                TableId.MONTHLY_TYPES,
                new Chart(
                    TableId.MONTHLY_TYPES,
                    ChartKind.STACKED_COLUMNS,
                    Unit.REQUESTS,
                    monthTypes.title(),
                    months,
                    series,
                    monthTypes.notes()
                )
            );
        }
        var officeTable = tables.get(TableId.OFFICES);
        if (!officeTable.rows().isEmpty() && !months.isEmpty()) {
            var identities = officeTable.rows().stream().limit(OFFICE_SERIES).map(r -> (Category) r.cells().get(0)).toList();
            var series = IntStream.range(0, identities.size())
                .mapToObj(index -> {
                    var identity = identities.get(index);
                    return new Series(
                        identity.label(),
                        rgb(PALETTE[index]),
                        report
                            .months()
                            .stream()
                            .sorted(Comparator.comparing(PqrsStatistics.Month::month))
                            .map(m -> monthlyOfficePoint(m, identity))
                            .toList()
                    );
                })
                .toList();
            var monthlyOffices = tables.get(TableId.MONTHLY_OFFICES);
            var notes = new ArrayList<>(monthlyOffices.notes());
            if (identities.size() < officeTable.rows().size()) notes.add(selectionNote(identities.size(), officeTable.rows().size()));
            charts.put(
                TableId.MONTHLY_OFFICES,
                new Chart(TableId.MONTHLY_OFFICES, ChartKind.STACKED_COLUMNS, Unit.REQUESTS, monthlyOffices.title(), months, series, notes)
            );
        }
        return charts;
    }

    static Point monthlyOfficePoint(PqrsStatistics.Month month, Category identity) {
        var observed = month
            .currentOffices()
            .stream()
            .filter(o -> OfficeCategories.key(o).equals(new OfficeKey(identity.code(), identity.idType(), identity.id())))
            .findFirst()
            .map(o -> new Point(BigDecimal.valueOf(o.count())));
        return observed.orElseGet(() -> officeCountsAreExhaustive(month) ? new Point(BigDecimal.ZERO) : new Point(null));
    }

    static Cell totalTypePercentage(List<PqrsStatistics.TypeShare> types, long denominator) {
        if (denominator == 0) return Missing.NO_CALCULABLE;
        long categorized = types.stream().mapToLong(PqrsStatistics.TypeShare::count).sum();
        return categorized == denominator ? new Percentage(BigDecimal.valueOf(100)) : Missing.PARTIAL_INFORMATION;
    }

    static Cell monthlyTypeCount(PqrsStatistics.Month month, String code) {
        if (month.types() == null) return Missing.NULL_VALUE;
        Long count = month.types().get(code);
        if (count != null) return new Quantity(count);
        return typeCountsAreExhaustive(month) ? new Quantity(0) : Missing.PARTIAL_INFORMATION;
    }

    private static boolean typeCountsAreExhaustive(PqrsStatistics.Month month) {
        return month.types() != null && month.types().values().stream().mapToLong(Long::longValue).sum() == month.received();
    }

    private static boolean officeCountsAreExhaustive(PqrsStatistics.Month month) {
        return month.currentOffices() != null && month.currentOffices().stream().mapToLong(OfficeCount::count).sum() == month.received();
    }

    private static String selectionNote(int shown, int total) {
        return "Se muestran los " + shown + " mayores conteos del periodo de " + total + " categorías. La tabla conserva todas.";
    }

    private static ChartCategory chartCategory(Cell cell, String colorOverride) {
        if (cell instanceof Month month) return new ChartCategory(SHORT_MONTH.format(month.value()), month.partial(), rgb("16746D"));
        var category = (Category) cell;
        return new ChartCategory(
            category.label(),
            false,
            rgb(colorOverride == null ? COLORS.getOrDefault(category.code(), "16746D") : colorOverride)
        );
    }

    private static Point point(Cell cell) {
        if (cell instanceof Quantity count) return new Point(BigDecimal.valueOf(count.value()));
        if (cell instanceof Percentage percent) return new Point(percent.value());
        return new Point(null);
    }

    private static int rgb(String hex) {
        return Integer.parseInt(hex, 16);
    }

    private static Cell quantity(Long count) {
        return count == null ? Missing.NULL_VALUE : new Quantity(count);
    }

    private static Month month(PqrsStatistics.Month month) {
        return new Month(month.month(), !month.complete());
    }

    private static State state(String code) {
        return new State(code, label(code));
    }

    private static Category category(String code) {
        return new Category(code, null, null, label(code));
    }

    private static Row row(Cell... cells) {
        return new Row(List.of(cells));
    }

    private static List<Column> columns(Object... nameAndType) {
        var columns = new ArrayList<Column>();
        for (int i = 0; i < nameAndType.length; i += 2) columns.add(new Column((String) nameAndType[i], (ColumnType) nameAndType[i + 1]));
        return columns;
    }

    private static Table table(TableId id, String title, List<Column> columns, List<Row> rows, Row total, String... notes) {
        return new Table(id, title, columns, rows, total, List.of(notes));
    }

    private static String label(String code) {
        return switch (code) {
            case "REQUEST" -> "Petición";
            case "COMPLAINT" -> "Queja";
            case "CLAIM" -> "Reclamo";
            case "SUGGESTION" -> "Sugerencia";
            case "DEMAND" -> "Demanda";
            case "PENDING" -> "Pendientes";
            case "IN_PROGRESS" -> "En progreso";
            case "RESOLVED" -> "Resueltas";
            case "CLOSED" -> "Cerradas";
            case "REJECTED" -> "Rechazadas";
            default -> LABELS.getOrDefault(code, code);
        };
    }

    private record OfficeKey(String category, String type, String id) {}

    private static final class OfficeCategories {

        static final Comparator<OfficeCount> ORDER = Comparator.comparingLong(OfficeCount::count)
            .reversed()
            .thenComparing(OfficeCount::category)
            .thenComparing(o -> Objects.toString(o.idType(), ""))
            .thenComparing(o -> Objects.toString(o.id(), ""));
        final List<OfficeCount> sorted;
        final Map<String, Long> nameCounts;

        OfficeCategories(List<OfficeCount> offices) {
            sorted = offices.stream().sorted(ORDER).toList();
            nameCounts = sorted
                .stream()
                .map(OfficeCategories::name)
                .collect(java.util.stream.Collectors.groupingBy(Function.identity(), java.util.stream.Collectors.counting()));
        }

        static OfficeKey key(OfficeCount office) {
            return new OfficeKey(office.category(), office.idType(), office.id());
        }

        static String name(OfficeCount office) {
            return "ASSIGNED".equals(office.category())
                ? (office.name() == null || office.name().isBlank() ? "Dependencia sin nombre" : office.name())
                : label(office.category());
        }

        Category category(OfficeCount office) {
            String name = name(office);
            if (office.id() != null && nameCounts.getOrDefault(name, 0L) > 1) name += " · " + office.id() + " (" + office.idType() + ")";
            return new Category(office.category(), office.idType(), office.id(), name);
        }
    }
}
