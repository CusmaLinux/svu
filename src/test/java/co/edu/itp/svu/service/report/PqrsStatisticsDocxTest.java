package co.edu.itp.svu.service.report;

import static org.assertj.core.api.Assertions.*;

import co.edu.itp.svu.service.dto.PqrsStatistics;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import co.edu.itp.svu.service.report.PqrsStatisticsDocument.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.util.ZipFileZipEntrySource;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

class PqrsStatisticsDocxTest {

    private final java.time.Instant now = PqrsStatisticsReceiptTest.NOW;

    private PqrsStatisticsDocument document() {
        var filter = new PqrsStatisticsFilter(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 2, 1),
            PqrsStatisticsFilter.Scope.INSTITUTIONAL,
            null
        );
        var metadata = new Metadata(
            UUID.randomUUID(),
            "Institución Universitaria del Putumayo",
            "Gestión PQRSD",
            Status.PRELIMINARY,
            filter,
            null,
            new PqrsStatistics.Period("America/Bogota", now, now, now, now),
            now,
            now,
            1,
            1,
            1,
            "Observada",
            List.of(),
            List.of()
        );
        var chart = new Chart(
            TableId.TYPOLOGIES,
            ChartKind.BARS,
            Unit.REQUESTS,
            "Conteos",
            List.of(new ChartCategory("Tipología", false, 0x16746d)),
            List.of(new Series("Solicitudes", 0x16746d, List.of(new Point(BigDecimal.ZERO)))),
            List.of()
        );
        var table = new Table(
            TableId.TYPOLOGIES,
            "Tipología",
            List.of(new Column("Solicitudes", ColumnType.COUNT)),
            List.of(new Row(List.of(new Quantity(0)))),
            null,
            List.of()
        );
        return new PqrsStatisticsDocument(
            metadata,
            List.of(new Section(1, "Introducción", List.of(), List.of(), List.of(table), List.of(chart)))
        );
    }

    @Test
    void createsOoxmlWithEditableTableAndEmbeddedPng() throws Exception {
        byte[] bytes = new PqrsStatisticsDocx(100, 1_000_000).generate(document());
        var file = java.nio.file.Files.createTempFile("synthetic-pqrs-docx", ".docx");
        try {
            java.nio.file.Files.write(file, bytes);
            try (var pkg = OPCPackage.open(new ZipFileZipEntrySource(new ZipFile(file.toFile()))); var word = new XWPFDocument(pkg)) {
                assertThat(word.getTables()).hasSize(1);
                assertThat(word.getAllPictures()).hasSize(1);
                assertThat(word.getTables().get(0).getRow(1).getCell(0).getText()).isEqualTo("0");
            }
        } finally {
            java.nio.file.Files.deleteIfExists(file);
        }
    }

    @Test
    void rejectsLimitOverflowInsteadOfReturningPartialArchive() {
        assertThatThrownBy(() -> new PqrsStatisticsDocx(1, 1_000_000).generate(document())).hasMessage("exportDocumentCapacity");
        assertThatThrownBy(() -> new PqrsStatisticsDocx(100, 16).generate(document())).isInstanceOf(RuntimeException.class);
    }

    @Test
    void buildsExecutiveSummaryOnlyFromModelAndPreservesNarrative() throws Exception {
        var source = document();
        var base = source.metadata();
        var metadata = new Metadata(
            base.id(),
            base.institution(),
            base.title(),
            base.status(),
            base.filter(),
            base.office(),
            base.period(),
            base.checkedAt(),
            base.generatedAt(),
            base.modelVersion(),
            base.methodologyVersion(),
            base.catalogVersion(),
            base.coverage(),
            List.of(new Notice("PARTIAL", "Información parcialmente disponible.")),
            List.of()
        );
        var types = new Table(
            TableId.TYPOLOGIES,
            "Tipologías",
            List.of(new Column("Tipología", ColumnType.CATEGORY), new Column("Cantidad", ColumnType.COUNT)),
            List.of(new Row(List.of(new Category("REQUEST", null, null, "Petición"), new Quantity(7)))),
            null,
            List.of()
        );
        var states = new Table(
            TableId.STATES,
            "Estados",
            List.of(new Column("Estado", ColumnType.CATEGORY), new Column("Cantidad", ColumnType.COUNT)),
            List.of(new Row(List.of(new Category("RESOLVED", null, null, "Resueltas"), new Quantity(7)))),
            null,
            List.of()
        );
        var offices = new Table(
            TableId.OFFICES,
            "Dependencias",
            List.of(new Column("Dependencia", ColumnType.CATEGORY), new Column("Cantidad", ColumnType.COUNT)),
            List.of(new Row(List.of(new Category("ASSIGNED", "office", "1", "Atención"), new Quantity(7)))),
            null,
            List.of()
        );
        var summary = new Table(
            TableId.SUMMARY,
            "Resumen",
            List.of(new Column("Concepto", ColumnType.TEXT), new Column("Solicitudes", ColumnType.COUNT)),
            List.of(new Row(List.of(new Text("Solicitudes recibidas"), new Quantity(7)))),
            null,
            List.of()
        );
        var variations = new Table(
            TableId.VARIATIONS,
            "Variaciones",
            List.of(
                new Column("Mes", ColumnType.MONTH),
                new Column("Recibidas", ColumnType.COUNT),
                new Column("Anterior", ColumnType.COUNT),
                new Column("Variación", ColumnType.PERCENT),
                new Column("Comparabilidad", ColumnType.STATUS)
            ),
            List.of(
                new Row(
                    List.of(
                        new Month(java.time.YearMonth.of(2024, 1), false),
                        new Quantity(3),
                        new Quantity(2),
                        new Percentage(new BigDecimal("50.0")),
                        new State("CALCULATED", "Calculada")
                    )
                )
            ),
            null,
            List.of()
        );
        var narrative = PqrsStatisticsNarrativeText.validate(
            com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("objective", "Narrativa validada literal")
        );
        var content = new PqrsStatisticsDocument(
            metadata,
            List.of(
                new Section(
                    1,
                    "Introducción",
                    narrative.section(1),
                    List.of(new Indicator("IND-TEST", "Satisfacción", Availability.DEFINITION_PENDING, "Umbral no definido.", null)),
                    List.of(summary, types, states, offices, variations),
                    List.of()
                )
            )
        );
        byte[] bytes = new PqrsStatisticsDocx(100, 1_000_000).generate(content);
        var text = readWordText(bytes);
        assertThat(text).contains(
            "Resumen ejecutivo",
            "Solicitudes recibidas: 7",
            "Petición (7)",
            "Resueltas (7)",
            "Atención (7)",
            "enero 2024: 50",
            "Satisfacción: Indicador pendiente de definición institucional.",
            "Advertencia: Información parcialmente disponible.",
            "Narrativa validada literal"
        );
        assertThat(text.toLowerCase(java.util.Locale.ROOT)).doesNotContain("cumplimiento de términos legales");
    }

    @Test
    void movesLongTableToReferencedAnnexAndRetainsEveryRow() throws Exception {
        var base = document();
        var longRows = java.util.stream.IntStream.range(0, 31).mapToObj(index -> new Row(List.of(new Text("Registro " + index)))).toList();
        var longTable = new Table(
            TableId.MONTHLY_TYPES,
            "Detalle mensual extenso",
            List.of(new Column("Detalle", ColumnType.TEXT)),
            longRows,
            null,
            List.of()
        );
        var source = new PqrsStatisticsDocument(
            base.metadata(),
            List.of(new Section(1, "Introducción", List.of(), List.of(), List.of(longTable), List.of()))
        );
        byte[] bytes = new PqrsStatisticsDocx(100, 1_000_000).generate(source);
        var file = java.nio.file.Files.createTempFile("synthetic-pqrs-annex", ".docx");
        try {
            java.nio.file.Files.write(file, bytes);
            try (var pkg = OPCPackage.open(new ZipFileZipEntrySource(new ZipFile(file.toFile()))); var word = new XWPFDocument(pkg)) {
                assertThat(word.getTables()).hasSize(1);
                assertThat(word.getTables().get(0).getRows()).hasSize(32);
                assertThat(word.getTables().get(0).getText()).contains("Registro 0", "Registro 30");
                assertThat(word.getParagraphs().stream().map(XWPFParagraph::getText).toList()).anyMatch(
                    text -> text.contains("Anexo A") && text.contains("Tabla trasladada")
                );
                assertThat(word.getParagraphs().stream().map(XWPFParagraph::getText).toList()).anyMatch(text ->
                    text.contains("Anexo A · Detalle mensual extenso")
                );
            }
        } finally {
            java.nio.file.Files.deleteIfExists(file);
        }
    }

    private static String readWordText(byte[] bytes) throws Exception {
        var file = java.nio.file.Files.createTempFile("synthetic-pqrs-summary", ".docx");
        try {
            java.nio.file.Files.write(file, bytes);
            try (var pkg = OPCPackage.open(new ZipFileZipEntrySource(new ZipFile(file.toFile()))); var word = new XWPFDocument(pkg)) {
                return (
                    word.getParagraphs().stream().map(XWPFParagraph::getText).reduce("", (left, right) -> left + "\n" + right) +
                    word
                        .getTables()
                        .stream()
                        .map(org.apache.poi.xwpf.usermodel.XWPFTable::getText)
                        .reduce("", (left, right) -> left + "\n" + right)
                );
            }
        } finally {
            java.nio.file.Files.deleteIfExists(file);
        }
    }
}
