package co.edu.itp.svu.service.report;

import static org.assertj.core.api.Assertions.*;

import co.edu.itp.svu.service.dto.PqrsStatistics;
import co.edu.itp.svu.service.report.PqrsStatisticsDocument.*;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PqrsStatisticsDocumentTest {

    @Test
    void totalTypePercentageUsesExhaustiveCountsAndDoesNotAddRoundedPercentages() {
        var types = List.of(
            new PqrsStatistics.TypeShare("A", 1, 3, new BigDecimal("33.33")),
            new PqrsStatistics.TypeShare("B", 1, 3, new BigDecimal("33.33")),
            new PqrsStatistics.TypeShare("C", 1, 3, new BigDecimal("33.33"))
        );
        assertThat(PqrsStatisticsDocumentAssembler.totalTypePercentage(types, 3)).isEqualTo(new Percentage(BigDecimal.valueOf(100)));
        assertThat(PqrsStatisticsDocumentAssembler.totalTypePercentage(types, 0)).isEqualTo(Missing.NO_CALCULABLE);
        assertThat(PqrsStatisticsDocumentAssembler.totalTypePercentage(types.subList(0, 2), 3)).isEqualTo(Missing.PARTIAL_INFORMATION);
    }

    @Test
    void missingMonthlyCategoriesAreZeroOnlyForExhaustiveObservedCounts() {
        var exhaustive = month(
            4,
            java.util.Map.of("A", 4L),
            List.of(new PqrsStatistics.OfficeCount("ASSIGNED", "office", "1", "Oficina", 4))
        );
        var partial = month(4, java.util.Map.of("A", 3L), List.of(new PqrsStatistics.OfficeCount("ASSIGNED", "office", "1", "Oficina", 3)));

        assertThat(PqrsStatisticsDocumentAssembler.monthlyTypeCount(exhaustive, "B")).isEqualTo(new Quantity(0));
        assertThat(PqrsStatisticsDocumentAssembler.monthlyTypeCount(partial, "B")).isEqualTo(Missing.PARTIAL_INFORMATION);
        var office = new Category("ASSIGNED", "office", "2", "Otra oficina");
        assertThat(PqrsStatisticsDocumentAssembler.monthlyOfficePoint(exhaustive, office).value()).isEqualTo(BigDecimal.ZERO);
        assertThat(PqrsStatisticsDocumentAssembler.monthlyOfficePoint(partial, office).value()).isNull();
    }

    @Test
    void missingValueLabelsKeepDistinctReportingStates() {
        assertThat(Missing.NO_CALCULABLE.label()).isEqualTo("No calculable");
        assertThat(Missing.NULL_VALUE.label()).isEqualTo("Información no disponible");
        assertThat(Missing.NOT_APPLICABLE.label()).isEqualTo("No aplica");
        assertThat(Missing.PARTIAL_INFORMATION.label()).isEqualTo("Información parcial");
        assertThat(Missing.DEFINITION_PENDING.label()).isEqualTo("Pendiente de definición");
        assertThat(Availability.DEFINITION_PENDING.label()).contains("pendiente de definición");
    }

    private static PqrsStatistics.Month month(long received, java.util.Map<String, Long> types, List<PqrsStatistics.OfficeCount> offices) {
        return new PqrsStatistics.Month(YearMonth.of(2024, 1), received, true, null, "NO_COMPARABLE_BASE", null, types, offices);
    }

    @Test
    void copiesSourceCollectionsAtEachLevel() {
        var cells = new ArrayList<Cell>(List.of(new Quantity(0)));
        var row = new Row(cells);
        var rows = new ArrayList<>(List.of(row));
        var table = new Table(TableId.SUMMARY, "Resumen", List.of(new Column("Solicitudes", ColumnType.COUNT)), rows, null, List.of());
        var tables = new ArrayList<>(List.of(table));
        var points = new ArrayList<>(List.of(new Point(null)));
        var series = new Series("Variación", 0x16746d, points);
        var categories = new ArrayList<>(List.of(new ChartCategory("01/2024", true, 0x16746d)));
        var chart = new Chart(TableId.VARIATIONS, ChartKind.LINE, Unit.PERCENT, "Variaciones", categories, List.of(series), List.of());
        var sections = new ArrayList<>(List.of(new Section(1, "Introducción", List.of(), List.of(), tables, List.of(chart))));
        var document = new PqrsStatisticsDocument(null, sections);
        for (var source : List.of(cells, rows, tables, points, categories, sections)) source.clear();
        assertThat(document.sections().get(0).tables().get(0).rows().get(0).cells()).containsExactly(new Quantity(0));
        assertThat(chart.categories()).hasSize(1);
        assertThat(chart.series().get(0).points().get(0).value()).isNull();
        assertThatThrownBy(() -> document.sections().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> table.rows().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> series.points().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rendersLineGapAndNegativeValueInBoundedPng() throws Exception {
        int green = 0x16746d;
        var chart = new Chart(
            TableId.VARIATIONS,
            ChartKind.LINE,
            Unit.PERCENT,
            "Variación mensual",
            List.of(
                new ChartCategory("01/2024", false, green),
                new ChartCategory("02/2024", true, green),
                new ChartCategory("03/2024", false, green)
            ),
            List.of(
                new Series("Variación", green, List.of(new Point(BigDecimal.valueOf(-50)), new Point(null), new Point(BigDecimal.TEN)))
            ),
            List.of()
        );
        var image = ImageIO.read(new ByteArrayInputStream(PqrsStatisticsChartImage.png(chart)));
        assertThat(image.getWidth()).isEqualTo(1200);
        assertThat(image.getHeight()).isEqualTo(720);
        assertThat(image.getRGB(285, 620)).isEqualTo(new Color(green).getRGB());
        assertThat(image.getRGB(985, 85)).isEqualTo(new Color(green).getRGB());
        for (int y = 85; y <= 620; y++) assertThat(image.getRGB(635, y)).isNotEqualTo(new Color(green).getRGB());
    }

    @Test
    void rendersBarsAndStackedSeriesWithNumericZeroAndMissingPoints() throws Exception {
        var categories = List.of(
            new ChartCategory("Tipología histórica ambigua", true, 0xA36123),
            new ChartCategory("Sin asignación", false, 0x355A9C)
        );
        var counts = new Series("Conteo", 0x16746d, List.of(new Point(BigDecimal.TEN), new Point(BigDecimal.ZERO)));
        var bars = new Chart(TableId.TYPOLOGIES, ChartKind.BARS, Unit.REQUESTS, "Tipologías", categories, List.of(counts), List.of());
        var stacked = new Chart(
            TableId.MONTHLY_TYPES,
            ChartKind.STACKED_COLUMNS,
            Unit.REQUESTS,
            "Tipos por mes",
            categories,
            List.of(counts, new Series("Otra", 0x355A9C, List.of(new Point(BigDecimal.ONE), new Point(null)))),
            List.of()
        );
        assertThat(ImageIO.read(new ByteArrayInputStream(PqrsStatisticsChartImage.png(bars))).getRGB(400, 102)).isEqualTo(
            new Color(0xA36123).getRGB()
        );
        assertThat(ImageIO.read(new ByteArrayInputStream(PqrsStatisticsChartImage.png(stacked))).getRGB(372, 90)).isEqualTo(
            new Color(0x355A9C).getRGB()
        );
    }

    @Test
    void rejectsChartOverCapacityBeforeAllocatingImage() {
        var categories = Collections.nCopies(25, new ChartCategory("Enero", false, 0));
        var series = new Series("Solicitudes", 0, Collections.nCopies(25, new Point(BigDecimal.ZERO)));
        var chart = new Chart(TableId.EVOLUTION, ChartKind.COLUMNS, Unit.REQUESTS, "Evolución", categories, List.of(series), List.of());
        assertThatThrownBy(() -> PqrsStatisticsChartImage.png(chart)).hasMessage("exportChartCapacity").hasNoCause();
        var category = new ChartCategory("SENSITIVE".repeat(65), false, 0);
        var tooLong = new Chart(
            TableId.STATES,
            ChartKind.BARS,
            Unit.REQUESTS,
            "Estados",
            List.of(category),
            List.of(new Series("Estados", 0, List.of(new Point(BigDecimal.ONE)))),
            List.of()
        );
        assertThatThrownBy(() -> PqrsStatisticsChartImage.png(tooLong)).hasMessage("exportChartCapacity").hasNoCause();
    }
}
