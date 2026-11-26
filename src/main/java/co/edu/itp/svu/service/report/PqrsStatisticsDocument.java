package co.edu.itp.svu.service.report;

import co.edu.itp.svu.repository.PqrsStatisticsRepository.ResolvedOffice;
import co.edu.itp.svu.service.dto.PqrsStatistics.Period;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** In-memory, format-independent content for a preliminary management report. */
public record PqrsStatisticsDocument(Metadata metadata, List<Section> sections) {
    public PqrsStatisticsDocument {
        sections = List.copyOf(sections);
    }

    public enum Status {
        PRELIMINARY,
    }

    public record Metadata(
        UUID id,
        String institution,
        String title,
        Status status,
        PqrsStatisticsFilter filter,
        ResolvedOffice office,
        Period period,
        Instant checkedAt,
        Instant generatedAt,
        int modelVersion,
        int methodologyVersion,
        int catalogVersion,
        String coverage,
        List<Notice> warnings,
        List<Notice> limitations
    ) {
        public Metadata {
            warnings = List.copyOf(warnings);
            limitations = List.copyOf(limitations);
        }
        @Override
        public String toString() {
            return "Report metadata";
        }
    }

    public record Notice(String code, String explanation) {
        @Override
        public String toString() {
            return "Report notice";
        }
    }

    public record Section(
        int number,
        String title,
        List<PqrsStatisticsNarrativeText.Paragraph> narrative,
        List<Indicator> indicators,
        List<Table> tables,
        List<Chart> charts
    ) {
        public Section {
            narrative = List.copyOf(narrative);
            indicators = List.copyOf(indicators);
            tables = List.copyOf(tables);
            charts = List.copyOf(charts);
        }
        @Override
        public String toString() {
            return "Report section " + number;
        }
    }

    public enum Availability {
        AVAILABLE("Disponible"),
        PARTIAL_SOURCE("Información parcialmente disponible"),
        SOURCE_UNAVAILABLE("Información no disponible"),
        DEFINITION_PENDING("Indicador pendiente de definición institucional");

        private final String label;

        Availability(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record Indicator(String code, String name, Availability availability, String explanation, TableId evidence) {
        @Override
        public String toString() {
            return "Report indicator " + code;
        }
    }

    public enum TableId {
        SUMMARY,
        EVOLUTION,
        TYPOLOGIES,
        STATES,
        OFFICES,
        MONTHLY_TYPES,
        MONTHLY_OFFICES,
        VARIATIONS,
        INDICATORS,
        METHODOLOGY,
    }

    public enum ColumnType {
        TEXT,
        COUNT,
        PERCENT,
        MONTH,
        CATEGORY,
        STATUS,
    }
    public record Column(String name, ColumnType type) {}

    public sealed interface Cell permits Text, Quantity, Percentage, Month, Category, State, Missing {}
    public record Text(String value) implements Cell {
        @Override
        public String toString() {
            return "Text cell";
        }
    }
    public record Quantity(long value) implements Cell {
        @Override
        public String toString() {
            return "Quantity cell";
        }
    }
    /** Percentage points as returned by the engine (e.g. -50 for -50%). */
    public record Percentage(BigDecimal value) implements Cell {
        @Override
        public String toString() {
            return "Percentage cell";
        }
    }
    public record Month(YearMonth value, boolean partial) implements Cell {
        @Override
        public String toString() {
            return "Month cell";
        }
    }
    /** Category identity includes the original BSON type where it exists. */
    public record Category(String code, String idType, String id, String label) implements Cell {
        @Override
        public String toString() {
            return "Category cell";
        }
    }
    public record State(String code, String label) implements Cell {
        @Override
        public String toString() {
            return "State cell";
        }
    }
    public enum Missing implements Cell {
        NULL_VALUE("Información no disponible"),
        NO_CALCULABLE("No calculable"),
        PARTIAL_MONTH("Mes parcial"),
        NO_COMPARABLE_BASE("Sin base comparable");

        private final String label;

        Missing(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record Row(List<Cell> cells) {
        public Row {
            cells = List.copyOf(cells);
        }
        @Override
        public String toString() {
            return "Report row";
        }
    }

    public record Table(TableId id, String title, List<Column> columns, List<Row> rows, Row totals, List<String> notes) {
        public Table {
            columns = List.copyOf(columns);
            rows = List.copyOf(rows);
            notes = List.copyOf(notes);
        }
        @Override
        public String toString() {
            return "Report table " + id;
        }
    }

    public enum ChartKind {
        COLUMNS,
        BARS,
        LINE,
        STACKED_COLUMNS,
    }
    public enum Unit {
        REQUESTS,
        PERCENT,
    }

    public record ChartCategory(String label, boolean partial, int rgb) {
        @Override
        public String toString() {
            return "Chart category";
        }
    }

    /** A null value is a gap, even in a stacked chart. */
    public record Point(BigDecimal value) {
        @Override
        public String toString() {
            return "Chart point";
        }
    }

    public record Series(String label, int rgb, List<Point> points) {
        public Series {
            points = List.copyOf(points);
        }
        @Override
        public String toString() {
            return "Chart series";
        }
    }

    public record Chart(
        TableId table,
        ChartKind kind,
        Unit unit,
        String title,
        List<ChartCategory> categories,
        List<Series> series,
        List<String> notes
    ) {
        public Chart {
            categories = List.copyOf(categories);
            series = List.copyOf(series);
            notes = List.copyOf(notes);
        }
        @Override
        public String toString() {
            return "Report chart " + table;
        }
    }

    @Override
    public String toString() {
        return "Preliminary PQRSD report";
    }
}
