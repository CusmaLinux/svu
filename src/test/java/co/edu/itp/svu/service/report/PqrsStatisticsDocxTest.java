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
}
