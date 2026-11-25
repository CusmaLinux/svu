package co.edu.itp.svu.service.report;

import static org.assertj.core.api.Assertions.*;

import co.edu.itp.svu.service.dto.PqrsStatistics;
import co.edu.itp.svu.service.dto.PqrsStatistics.*;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter.Scope;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.IntStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.util.ZipFileZipEntrySource;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PqrsStatisticsWorkbookTest {

    @TempDir
    Path directory;

    private final PqrsStatisticsWorkbook generator = new PqrsStatisticsWorkbook();

    @Test
    void generatesOrderedTypedSheetsWithNativeChartsAndLiteralLabels() throws Exception {
        String officeName = "=1+1";
        var offices = List.of(
            new OfficeCount("ASSIGNED", "string", "a", officeName, 2),
            new OfficeCount("ASSIGNED", "objectId", "b", officeName, 1),
            new OfficeCount("UNASSIGNED", null, null, null, 1),
            new OfficeCount("UNVERIFIABLE", null, null, null, 1)
        );
        var months = List.of(
            new Month(
                YearMonth.of(2024, 1),
                2,
                true,
                4L,
                "CALCULATED",
                new BigDecimal("-50"),
                Map.of("DEMAND", 2L),
                List.of(offices.get(0))
            ),
            new Month(YearMonth.of(2024, 2), 0, true, 2L, "NO_COMPARABLE_BASE", null, Map.of(), List.of()),
            new Month(
                YearMonth.of(2024, 3),
                3,
                false,
                0L,
                "PARTIAL_MONTH",
                null,
                Map.of("HISTORICALLY_AMBIGUOUS", 2L, "UNKNOWN", 1L),
                offices.subList(1, 4)
            )
        );
        var types = List.of(
            new TypeShare("DEMAND", 2, 5, new BigDecimal("40")),
            new TypeShare("HISTORICALLY_AMBIGUOUS", 2, 5, new BigDecimal("40")),
            new TypeShare("UNKNOWN", 1, 5, new BigDecimal("20"))
        );
        try (var book = open(report(5, offices, months, types))) {
            assertThat(IntStream.range(0, book.getNumberOfSheets()).mapToObj(book::getSheetName)).containsExactly(
                "01 Resumen",
                "02 Evolución mensual",
                "03 Tipologías",
                "04 Estados actuales",
                "05 Dependencias",
                "06 Tipologías por mes",
                "07 Dependencias por mes",
                "08 Variaciones",
                "09 Indicadores",
                "10 Metodología"
            );
            assertThat(book.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).contains("No aprobado");
            assertThat(book.getSheetAt(1).getRow(6).getCell(2).getStringCellValue()).contains("Parcial");
            assertThat(book.getSheetAt(1).getRow(7).getCell(1).getNumericCellValue()).isEqualTo(5);
            var type = book.getSheetAt(2).getRow(4);
            assertThat(type.getCell(0).getStringCellValue()).isEqualTo("Demanda");
            assertThat(type.getCell(3).getNumericCellValue()).isEqualTo(0.4);
            assertThat(type.getCell(3).getCellStyle().getDataFormatString()).isEqualTo("0.0%");
            assertThat(book.getSheetAt(2).getRow(5).getCell(0).getStringCellValue()).contains("ambigua");
            assertThat(book.getSheetAt(3).getRow(5).getCell(1).getNumericCellValue()).isZero();
            assertThat(book.getSheetAt(3).getRow(9).getCell(0).getStringCellValue()).contains("no reconocido");
            var sheet = book.getSheetAt(4);
            assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("=1+1 · a (string)");
            assertThat(sheet.getRow(5).getCell(0).getStringCellValue()).isEqualTo("=1+1 · b (objectId)");
            assertThat(sheet.getRow(6).getCell(0).getStringCellValue()).isEqualTo("Sin dependencia asignada");
            assertThat(sheet.getRow(7).getCell(0).getStringCellValue()).isEqualTo("Referencia no verificable");
            assertThat(book.getSheetAt(7).getRow(4).getCell(3).getNumericCellValue()).isEqualTo(-0.5);
            assertThat(book.getSheetAt(7).getRow(5).getCell(3).getCellType()).isEqualTo(CellType.BLANK);
            assertThat(book.getSheetAt(7).getRow(6).getCell(2).getCellType()).isEqualTo(CellType.BLANK);
            assertThat(book.getSheetAt(7).getRow(6).getCell(4).getStringCellValue()).contains("mes parcial");
            assertThat(book.getSheetAt(8).getLastRowNum()).isEqualTo(22);
            assertThat(book.getExternalLinksTable()).isEmpty();
            assertThat(book.isMacroEnabled()).isFalse();
            for (var tab : book) for (var row : tab) for (var cell : row) assertThat(cell.getCellType()).isNotEqualTo(CellType.FORMULA);
            for (int index : new int[] { 0, 1, 2, 3, 4, 5, 7 }) {
                var tab = book.getSheetAt(index);
                var charts = tab.getDrawingPatriarch().getCharts();
                assertThat(charts).hasSize(1);
                var series = charts.get(0).getChartSeries().get(0).getSeries(0);
                assertThat(series.getCategoryData().getDataRangeReference()).contains(tab.getSheetName());
                assertThat(series.getValuesData().getDataRangeReference()).contains(tab.getSheetName());
            }
            assertThat(book.getSheetAt(5).getDrawingPatriarch().getCharts().get(0).getCTChart().xmlText()).contains("stacked");
            assertThat(
                book.getSheetAt(7).getDrawingPatriarch().getCharts().get(0).getCTChart().getDispBlanksAs().getVal().toString()
            ).isEqualTo("gap");
        }
    }

    @Test
    void emptyPeriodKeepsZerosAndBlankPercentages() throws Exception {
        var january = new Month(YearMonth.of(2024, 1), 0, true, 0L, "NO_CALCULABLE", null, Map.of("REQUEST", 0L), List.of());
        try (var book = open(report(0, List.of(), List.of(january), List.of(new TypeShare("REQUEST", 0, 0, null))))) {
            var row = book.getSheetAt(2).getRow(4);
            assertThat(row.getCell(1).getNumericCellValue()).isZero();
            assertThat(row.getCell(3).getCellType()).isEqualTo(CellType.BLANK);
            assertThat(book.getSheetAt(7).getDrawingPatriarch()).isNull();
            assertThat(book.getSheetAt(7).getRow(4).getCell(4).getStringCellValue()).contains("base cero");
        }
    }

    @Test
    void workbookRetainsAllRowsWithManyOffices() throws Exception {
        var offices = IntStream.range(0, 1000)
            .mapToObj(i -> new OfficeCount("ASSIGNED", "string", "o" + i, "Dependencia sintética " + i, 12))
            .toList();
        var months = IntStream.rangeClosed(1, 12)
            .mapToObj(i ->
                new Month(
                    YearMonth.of(2024, i),
                    1000,
                    true,
                    1000L,
                    "CALCULATED",
                    BigDecimal.ZERO,
                    Map.of("REQUEST", 1000L),
                    offices
                        .stream()
                        .map(office -> new OfficeCount(office.category(), office.idType(), office.id(), office.name(), 1))
                        .toList()
                )
            )
            .toList();
        var memory = ManagementFactory.getMemoryMXBean();
        long before = memory.getHeapMemoryUsage().getUsed();
        long start = System.nanoTime();
        byte[] file = generator.generate(
            report(12000, offices, months, List.of(new TypeShare("REQUEST", 12000, 12000, BigDecimal.valueOf(100))))
        );
        System.out.printf(
            "XLSX benchmark: %d bytes; %d ms; heap delta %d bytes before reopening%n",
            file.length,
            (System.nanoTime() - start) / 1_000_000,
            memory.getHeapMemoryUsage().getUsed() - before
        );
        try (var book = open(file)) {
            assertThat(book.getSheetAt(4).getLastRowNum()).isEqualTo(1004);
            assertThat(book.getSheetAt(6).getLastRowNum()).isEqualTo(12004);
            assertThat(book.getSheetAt(4).getDrawingPatriarch().getCharts().get(0).getCTChart().xmlText()).contains("20 mayores conteos");
        }
    }

    private XSSFWorkbook open(PqrsStatistics report) throws Exception {
        return open(generator.generate(report));
    }

    private XSSFWorkbook open(byte[] file) throws Exception {
        Path path = directory.resolve("report.xlsx");
        Files.write(path, file);
        return new XSSFWorkbook(OPCPackage.open(new ZipFileZipEntrySource(new ZipFile(path.toFile()))));
    }

    private static PqrsStatistics report(long count, List<OfficeCount> offices, List<Month> months, List<TypeShare> types) {
        var filter = new PqrsStatisticsFilter(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), Scope.INSTITUTIONAL, null);
        var now = Instant.parse("2025-01-01T12:00:00Z");
        return new PqrsStatistics(
            filter,
            new PqrsStatistics.Period("America/Bogota", now, now, now, now),
            new Coverage("PARTIAL", List.of("TYPE_HISTORICALLY_AMBIGUOUS"), List.of("HISTORICAL_COMPLETENESS_UNMEASURED")),
            count,
            months,
            types,
            Map.of("PENDING", count, "IN_PROGRESS", 0L, "RESOLVED", 0L, "CLOSED", 0L, "REJECTED", 0L, "UNKNOWN", 1L),
            offices,
            Map.of("IND-09", "SOURCE_UNAVAILABLE")
        );
    }
}
