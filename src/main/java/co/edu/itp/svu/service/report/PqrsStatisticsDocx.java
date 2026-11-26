package co.edu.itp.svu.service.report;

import co.edu.itp.svu.service.report.PqrsStatisticsDocument.*;
import java.io.*;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PqrsStatisticsDocx {

    private static final Locale SPANISH = Locale.forLanguageTag("es-CO");
    private static final String DARK = "173742";
    private static final String TEAL = "17655E";
    private static final int WIDTH = 9466;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu", SPANISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss.SSS XXX", SPANISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM uuuu", SPANISH);

    private final int maximumCells;
    private final int maximumBytes;

    PqrsStatisticsDocx(
        @Value("${svu.pqrs-statistics.export.docx.max-cells:12000}") int maximumCells,
        @Value("${svu.pqrs-statistics.export.docx.max-bytes:8388608}") int maximumBytes
    ) {
        if (maximumCells < 1 || maximumCells > 100_000 || maximumBytes < 1 || maximumBytes > 33_554_432) {
            throw new IllegalArgumentException("Invalid DOCX limits");
        }
        this.maximumCells = maximumCells;
        this.maximumBytes = maximumBytes;
    }

    public byte[] generate(PqrsStatisticsDocument source) throws IOException {
        checkCapacity(source);
        try (var document = new XWPFDocument()) {
            layout(document, source.metadata());
            for (var section : source.sections()) {
                heading(document, section.number() + ". " + section.title(), 1);
                for (var field : section.narrative()) {
                    heading(document, field.field().label(), 2);
                    text(document.createParagraph(), field.displayText(), 11, false, DARK);
                }
                for (var indicator : section.indicators()) {
                    text(
                        document.createParagraph(),
                        indicator.code() +
                        " · " +
                        indicator.name() +
                        " — " +
                        indicator.availability().label() +
                        ". " +
                        indicator.explanation(),
                        10,
                        false,
                        DARK
                    );
                }
                for (var table : section.tables()) {
                    heading(document, table.title(), 2);
                    renderTable(document, table);
                    for (var note : table.notes()) text(document.createParagraph(), note, 9, false, DARK);
                    for (var figure : section.charts()) if (figure.table() == table.id()) renderChart(document, figure);
                }
            }
            var output = new BoundedOutput(maximumBytes);
            document.write(output);
            return output.buffer.toByteArray();
        }
    }

    private void checkCapacity(PqrsStatisticsDocument source) {
        long cells = 0;
        long characters = 0;
        for (var section : source.sections()) for (var table : section.tables()) {
            int columns = table.columns().size();
            cells += (table.rows().size() + 2L) * columns;
            if (cells > maximumCells) throw capacity();
            for (var row : table.rows()) for (var cell : row.cells()) {
                if (cell instanceof Category value) characters += value.label().length();
                else if (cell instanceof Text value) characters += value.value().length();
                if (characters > 1_000_000) throw capacity();
            }
        }
    }

    private static void layout(XWPFDocument document, Metadata report) {
        var section = document.getDocument().getBody().addNewSectPr();
        var paper = section.addNewPgSz();
        paper.setW(BigInteger.valueOf(11906));
        paper.setH(BigInteger.valueOf(16838));
        var margins = section.addNewPgMar();
        margins.setLeft(BigInteger.valueOf(1220));
        margins.setRight(BigInteger.valueOf(1220));
        margins.setTop(BigInteger.valueOf(1120));
        margins.setBottom(BigInteger.valueOf(1120));
        margins.setHeader(BigInteger.valueOf(540));
        margins.setFooter(BigInteger.valueOf(540));

        var metadata = document.getProperties().getCoreProperties();
        metadata.setTitle(report.title());
        metadata.setCreator(report.institution());
        metadata.setCreated(java.util.Optional.of(java.util.Date.from(report.generatedAt())));

        var header = document.createHeader(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT).createParagraph();
        text(header, report.institution(), 9, true, TEAL);
        var footer = document.createFooter(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT).createParagraph();
        footer.setAlignment(ParagraphAlignment.RIGHT);
        text(footer, "Informe preliminar  •  Página ", 9, false, DARK);
        footer.getCTP().addNewFldSimple().setInstr(" PAGE ");

        text(document.createParagraph(), report.institution(), 12, true, TEAL);
        heading(document, report.title(), 0);
        text(document.createParagraph(), "INFORME PRELIMINAR", 12, true, TEAL);
        detail(document, "Alcance", report.office() == null ? "Institucional" : "Por dependencia");
        if (report.office() != null) detail(
            document,
            "Dependencia",
            report.office().name() == null ? report.office().id() : report.office().name()
        );
        detail(document, "Periodo", DATE.format(report.filter().start()) + " a " + DATE.format(report.filter().end()));
        var time = TIME.withZone(ZoneId.of(report.period().zone()));
        detail(document, "Zona horaria", report.period().zone());
        detail(document, "Corte efectivo de recepción", time.format(report.period().effectiveCutoff()));
        detail(document, "Observación original", time.format(report.period().observedAt()));
        detail(document, "Comprobación", time.format(report.checkedAt()));
        detail(document, "Generación", time.format(report.generatedAt()));
        detail(document, "Cobertura", report.coverage());
        text(
            document.createParagraph(),
            "Las cifras corresponden al resultado agregado comprobado. La observación no es transaccional.",
            10,
            false,
            DARK
        );
    }

    private static void detail(XWPFDocument document, String label, String value) {
        var paragraph = document.createParagraph();
        paragraph.setSpacingAfter(40);
        text(paragraph, label + " · ", 10, true, DARK);
        text(paragraph, value, 10, false, DARK);
    }

    private static void heading(XWPFDocument document, String title, int level) {
        var p = document.createParagraph();
        p.setSpacingBefore(level == 1 ? 310 : 135);
        p.setKeepNext(true);
        p.setSpacingAfter(95);
        if (level > 0) p.getCTP().getPPr().addNewOutlineLvl().setVal(BigInteger.valueOf(level - 1L));
        text(p, title, level == 0 ? 26 : level == 1 ? 16 : 12, true, level == 2 ? TEAL : DARK);
    }

    private static void renderTable(XWPFDocument document, Table model) {
        int n = model.columns().size();
        XWPFTable table = document.createTable(1, n);
        table.setWidth(WIDTH);
        table.setCellMargins(70, 80, 70, 80);
        var grid = table.getCTTbl().getTblGrid();
        if (grid == null) grid = table.getCTTbl().addNewTblGrid();
        var header = table.getRow(0);
        header.setRepeatHeader(true);
        header.setCantSplitRow(true);
        int totalWeight = model.columns().stream().mapToInt(PqrsStatisticsDocx::weight).sum();
        int[] widths = new int[n];
        for (int index = 0; index < n; index++) {
            var column = model.columns().get(index);
            int width = (WIDTH * weight(column)) / totalWeight;
            widths[index] = width;
            grid.addNewGridCol().setW(BigInteger.valueOf(width));
            formatCell(header.getCell(index), column.name(), width, DARK, true, false);
        }
        var format = NumberFormat.getNumberInstance(SPANISH);
        format.setMaximumFractionDigits(8);
        int index = 0;
        for (var row : model.rows()) renderRow(table, row, widths, format, index++ % 2 == 0 ? "F2F7F7" : null, false);
        if (model.totals() != null) renderRow(table, model.totals(), widths, format, "D4E9E6", true);
    }

    private static int weight(Column column) {
        return switch (column.type()) {
            case TEXT, CATEGORY, STATUS -> 3;
            case MONTH -> 2;
            default -> 1;
        };
    }

    private static void renderRow(XWPFTable table, Row model, int[] widths, NumberFormat format, String fill, boolean strong) {
        var row = table.createRow();
        for (int index = 0; index < widths.length; index++) {
            var cell = model.cells().get(index);
            formatCell(
                row.getCell(index),
                display(cell, format),
                widths[index],
                fill,
                strong,
                cell instanceof Quantity || cell instanceof Percentage
            );
        }
    }

    private static String display(Cell cell, NumberFormat format) {
        if (cell instanceof Quantity value) return format.format(value.value());
        if (cell instanceof Percentage value) return format.format(value.value()) + " %";
        if (cell instanceof Month value) return MONTH.format(value.value()) + (value.partial() ? " *" : "");
        if (cell instanceof Category value) return value.label();
        if (cell instanceof State value) return value.label();
        if (cell instanceof Text value) return value.value();
        return ((Missing) cell).label();
    }

    private static void formatCell(XWPFTableCell cell, String value, int width, String fill, boolean strong, boolean numeric) {
        cell.setWidth(Integer.toString(width));
        if (fill != null) cell.setColor(fill);
        var p = cell.getParagraphs().get(0);
        p.setSpacingAfter(25);
        if (numeric) p.setAlignment(ParagraphAlignment.RIGHT);
        boolean header = DARK.equals(fill);
        if (header) p.setKeepNext(true);
        text(p, value, 9, strong, header ? "FFFFFF" : DARK);
    }

    private static void renderChart(XWPFDocument document, Chart chart) throws IOException {
        heading(document, chart.title(), 2);
        var p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        try {
            var run = p.createRun();
            run
                .addPicture(
                    new ByteArrayInputStream(PqrsStatisticsChartImage.png(chart)),
                    Document.PICTURE_TYPE_PNG,
                    "figura.png",
                    Units.toEMU(456),
                    Units.toEMU(273.6)
                )
                .getCTPicture()
                .getNvPicPr()
                .getCNvPr()
                .setDescr(chart.title());
        } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException failure) {
            throw PqrsStatisticsReceiptException.unavailable();
        }
        for (var note : chart.notes()) text(document.createParagraph(), note, 9, false, DARK);
    }

    private static void text(XWPFParagraph p, String value, int size, boolean bold, String color) {
        var run = p.createRun();
        run.setFontFamily("Arial");
        run.setFontSize(size);
        run.setBold(bold);
        run.setColor(color);
        // Process literal segments with Word break and tab elements, never with XML or field instructions.
        int start = 0;
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            if (c == '\n' || c == '\t') {
                if (index > start) run.setText(value.substring(start, index));
                if (c == '\n') run.addBreak();
                else run.addTab();
                start = index + 1;
            }
        }
        if (start < value.length()) run.setText(value.substring(start));
    }

    private static PqrsStatisticsReceiptException capacity() {
        return new PqrsStatisticsReceiptException(503, "exportDocumentCapacity");
    }

    private static final class BoundedOutput extends OutputStream {

        private final int limit;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        BoundedOutput(int limit) {
            this.limit = limit;
        }

        @Override
        public void write(int value) {
            if (buffer.size() >= limit) throw capacity();
            buffer.write(value);
        }

        @Override
        public void write(byte[] bytes, int offset, int count) {
            if (count > limit - buffer.size()) throw capacity();
            buffer.write(bytes, offset, count);
        }
    }
}
