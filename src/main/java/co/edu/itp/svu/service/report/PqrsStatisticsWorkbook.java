package co.edu.itp.svu.service.report;

import static co.edu.itp.svu.service.report.PqrsStatisticsDocumentCatalog.*;

import co.edu.itp.svu.domain.enumeration.PqrsStatus;
import co.edu.itp.svu.domain.enumeration.PqrsType;
import co.edu.itp.svu.service.dto.PqrsStatistics;
import co.edu.itp.svu.service.dto.PqrsStatistics.OfficeCount;
import co.edu.itp.svu.service.dto.PqrsStatisticsFilter.Scope;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.xml.namespace.QName;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.*;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;

@Component
public class PqrsStatisticsWorkbook {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CO"));

    public byte[] generate(PqrsStatistics report) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var styles = new Styles(workbook);
            var offices = new OfficeLabels(report.currentOffices());
            var states = new LinkedHashMap<String, Long>();
            for (var state : PqrsStatus.values()) states.put(state.name(), report.currentStates().get(state.name()));
            report.currentStates().forEach(states::putIfAbsent);
            summary(workbook, styles, report, states, offices);
            monthly(workbook, styles, report);
            types(workbook, styles, report);
            states(workbook, styles, report, states);
            offices(workbook, styles, report, offices);
            monthlyTypes(workbook, styles, report);
            monthlyOffices(workbook, styles, report, offices);
            variations(workbook, styles, report);
            indicators(workbook, styles, report);
            methodology(workbook, styles, report);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static void summary(XSSFWorkbook wb, Styles styles, PqrsStatistics r, Map<String, Long> states, OfficeLabels offices) {
        var table = new Table(wb, styles, "01 Resumen", "Informe de Gestión PQRSD", new int[] { 34, 72 }, "Concepto", "Información");
        table.row("Alcance", r.filter().scope() == Scope.INSTITUTIONAL ? "Institucional" : "Por dependencia");
        if (r.filter().scope() == Scope.OFFICE) {
            table.row("Dependencia", r.currentOffices().stream().findFirst().map(offices::label).orElse("ID " + r.filter().officeId()));
        }
        table.row("Fecha inicial", r.filter().start());
        table.row("Fecha final", r.filter().end());
        var zone = ZoneId.of(r.period().zone());
        table.row("Fecha y hora de consulta", LocalDateTime.ofInstant(r.period().observedAt(), zone));
        table.row("Corte de la información", LocalDateTime.ofInstant(r.period().effectiveCutoff(), zone));
        table.row("Zona horaria", r.period().zone());
        table.total("Solicitudes recibidas", r.received());
        table.row("Cobertura", label(r.coverage().status()));
        table.row("Revisión", "Las cifras pueden variar respecto de una vista previa anterior si se actualizan las solicitudes.");
        for (String warning : r.coverage().warnings()) table.row("Advertencia", label(warning));
        int header = table.row("Estado actual", "Solicitudes");
        states.forEach((code, count) -> table.row(stateLabel(code), count));
        chart(table, header, states.size(), 1, "Estados actuales", BarDirection.BAR, false, new ArrayList<>(states.keySet()));
    }

    private static void monthly(XSSFWorkbook wb, Styles styles, PqrsStatistics r) {
        var t = new Table(
            wb,
            styles,
            "02 Evolución mensual",
            "Evolución mensual",
            new int[] { 23, 18, 23 },
            "Mes",
            "Recibidas",
            "Cobertura del mes"
        );
        for (var m : r.months()) t.row(month(m), m.received(), completeness(m));
        t.total(TOTAL, r.received());
        chart(t, Table.HEADER, r.months().size(), 1, "Solicitudes recibidas por mes", BarDirection.COL, false, List.of());
    }

    private static void types(XSSFWorkbook wb, Styles styles, PqrsStatistics r) {
        var t = new Table(
            wb,
            styles,
            "03 Tipologías",
            "Distribución por tipología",
            new int[] { 34, 16, 16, 16, 32 },
            "Tipología",
            "Solicitudes",
            "Denominador",
            "Porcentaje",
            "Disponibilidad"
        );
        for (var type : r.types()) t.row(
            typeLabel(type.code()),
            type.count(),
            type.denominator(),
            type.percent(),
            type.percent() == null ? "Porcentaje no calculable" : "Disponible"
        );
        t.total(TOTAL, r.received());
        chart(
            t,
            Table.HEADER,
            r.types().size(),
            1,
            "Solicitudes por tipología",
            BarDirection.BAR,
            false,
            r.types().stream().map(PqrsStatistics.TypeShare::code).toList()
        );
    }

    private static void states(XSSFWorkbook wb, Styles styles, PqrsStatistics r, Map<String, Long> states) {
        var t = new Table(
            wb,
            styles,
            "04 Estados actuales",
            "Estados actuales de las solicitudes",
            new int[] { 32, 18 },
            "Estado actual",
            "Solicitudes"
        );
        states.forEach((code, count) -> t.row(stateLabel(code), count));
        t.total(TOTAL, r.received());
        chart(
            t,
            Table.HEADER,
            states.size(),
            1,
            "Solicitudes por estado actual",
            BarDirection.BAR,
            false,
            new ArrayList<>(states.keySet())
        );
    }

    private static void offices(XSSFWorkbook wb, Styles styles, PqrsStatistics r, OfficeLabels offices) {
        var t = new Table(
            wb,
            styles,
            "05 Dependencias",
            "Dependencia responsable actual",
            new int[] { 52, 18 },
            "Dependencia responsable actual",
            "Solicitudes"
        );
        var sorted = r.currentOffices().stream().sorted(Comparator.comparingLong(OfficeCount::count).reversed()).toList();
        for (var o : sorted) t.row(offices.label(o), o.count());
        t.filter();
        t.total(TOTAL, r.received());
        int visible = Math.min(sorted.size(), 20);
        String title = visible < sorted.size()
            ? "20 mayores conteos de " + sorted.size() + " categorías"
            : "Solicitudes por dependencia responsable actual";
        chart(
            t,
            Table.HEADER,
            visible,
            1,
            title,
            BarDirection.BAR,
            false,
            sorted.stream().limit(visible).map(OfficeCount::category).toList()
        );
    }

    private static void monthlyTypes(XSSFWorkbook wb, Styles styles, PqrsStatistics r) {
        var codes = r.types().stream().map(PqrsStatistics.TypeShare::code).toList();
        var headers = new ArrayList<String>();
        headers.add("Mes");
        codes.forEach(code -> headers.add(typeLabel(code)));
        headers.addAll(List.of("Total mensual", "Cobertura del mes"));
        int[] widths = new int[headers.size()];
        Arrays.fill(widths, 24);
        var t = new Table(wb, styles, "06 Tipologías por mes", "Tipologías por mes de recepción", widths, headers.toArray(String[]::new));
        for (var m : r.months()) {
            var cells = new ArrayList<>();
            cells.add(month(m));
            codes.forEach(code -> cells.add(m.types().get(code)));
            cells.add(m.received());
            cells.add(completeness(m));
            t.row(cells.toArray());
        }
        var totals = new ArrayList<>();
        totals.add(TOTAL);
        r.types().forEach(type -> totals.add(type.count()));
        totals.add(r.received());
        t.total(totals.toArray());
        if (!codes.isEmpty()) chart(t, Table.HEADER, r.months().size(), codes.size(), "Tipologías por mes", BarDirection.COL, true, codes);
    }

    private static void monthlyOffices(XSSFWorkbook wb, Styles styles, PqrsStatistics r, OfficeLabels offices) {
        var t = new Table(
            wb,
            styles,
            "07 Dependencias por mes",
            "Dependencias por mes de recepción",
            new int[] { 23, 54, 18, 22 },
            "Mes",
            "Dependencia responsable actual",
            "Solicitudes",
            "Cobertura del mes"
        );
        for (var m : r.months()) for (var office : m.currentOffices()) t.row(
            month(m),
            offices.label(office),
            office.count(),
            completeness(m)
        );
        t.filter();
        t.total(TOTAL, null, r.received());
    }

    private static void variations(XSSFWorkbook wb, Styles styles, PqrsStatistics r) {
        var t = new Table(
            wb,
            styles,
            "08 Variaciones",
            "Variación mensual",
            new int[] { 23, 18, 22, 18, 44 },
            "Mes",
            "Recibidas",
            "Mes previo",
            "Variación",
            "Comparabilidad"
        );
        for (var m : r.months()) t.row(
            month(m),
            m.received(),
            m.complete() ? m.previousCount() : null,
            m.variationPercent(),
            label(m.comparison())
        );
        if (r.months().stream().anyMatch(m -> m.variationPercent() != null)) {
            chart(t, Table.HEADER, r.months().size(), 3, "Variación mensual porcentual", null, false, List.of());
        }
    }

    private static void indicators(XSSFWorkbook wb, Styles styles, PqrsStatistics r) {
        var t = new Table(
            wb,
            styles,
            "09 Indicadores",
            "Disponibilidad de indicadores",
            new int[] { 14, 34, 44, 76 },
            "Código",
            "Indicador",
            "Disponibilidad",
            "Explicación"
        );
        for (int i = 0; i < INDICATORS.size(); i++) {
            String code = String.format(Locale.ROOT, "IND-%02d", i + 1);
            String availability = "Disponible";
            if (i >= 7) availability = label(r.deferredIndicators().getOrDefault(code, "SOURCE_UNAVAILABLE"));
            if ((i == 2 || i == 3) && r.coverage().warnings().stream().anyMatch(key -> key.startsWith("TYPE_"))) {
                availability = label("PARTIAL_SOURCE");
            }
            if (i == 3 && r.received() == 0) availability = "No calculable · base cero";
            if (i == 4 && r.months().stream().noneMatch(m -> m.variationPercent() != null)) availability = "No calculable en el periodo";
            t.row(code, INDICATORS.get(i), availability, INDICATOR_NOTES.get(i));
        }
        t.filter();
    }

    private static void methodology(XSSFWorkbook wb, Styles styles, PqrsStatistics r) {
        var t = new Table(wb, styles, "10 Metodología", "Metodología y limitaciones", new int[] { 33, 94 }, "Tema", "Criterio");
        t.row("Fuente", "Motor estadístico PQRSD de SVU sobre MongoDB. Una raíz por solicitud recibida.");
        t.row("Periodo", "Fechas civiles inclusivas en Colombia. Se incluye el inicio del primer día.");
        t.row("Corte exclusivo", "Se excluyen las solicitudes registradas a partir del corte indicado en el Resumen.");
        t.row("Estados y dependencias", "Estado y dependencia actuales de la cohorte recibida, observados al consultar.");
        t.row("Tipologías históricas", "Las combinaciones ambiguas se conservan como categoría independiente.");
        t.row("Cero y ausencia", "Cero es un valor observado. Una celda vacía indica ausencia o imposibilidad de cálculo.");
        t.row("Comparabilidad", "La variación requiere meses completos comparables y base anterior positiva.");
        t.row(
            "Oportunidad y satisfacción",
            "La respuesta final, los plazos históricos y las encuestas requieren evidencia y definiciones adicionales."
        );
        t.row(
            "Actualizaciones concurrentes",
            "Las cifras pueden variar respecto de una vista previa anterior si se actualizan los registros."
        );
        t.row("Estado del informe", PRELIMINARY);
        for (var limitation : r.coverage().limitations()) t.row("Limitación", label(limitation));
        for (var warning : r.coverage().warnings()) t.row("Advertencia", label(warning));
    }

    private static void chart(
        Table t,
        int header,
        int rows,
        int value,
        String title,
        BarDirection direction,
        boolean stacked,
        List<String> codes
    ) {
        if (rows == 0) return;
        int left = t.columns + 1;
        int height = direction == BarDirection.BAR ? Math.max(16, rows + 5) : 17;
        var drawing = t.sheet.createDrawingPatriarch();
        var chart = drawing.createChart(drawing.createAnchor(0, 0, 0, 0, left, 2, left + 10, 2 + height));
        chart.setTitleText(title);
        chart.setTitleOverlay(false);
        boolean horizontal = direction == BarDirection.BAR;
        var categories = chart.createCategoryAxis(horizontal ? AxisPosition.LEFT : AxisPosition.BOTTOM);
        var values = chart.createValueAxis(horizontal ? AxisPosition.BOTTOM : AxisPosition.LEFT);
        values.setCrosses(AxisCrosses.AUTO_ZERO);
        var labels = XDDFDataSourcesFactory.fromStringCellRange(t.sheet, new CellRangeAddress(header + 1, header + rows, 0, 0));
        var data = chart.createData(direction == null ? ChartTypes.LINE : ChartTypes.BAR, categories, values);
        if (data instanceof XDDFBarChartData bar) {
            bar.setBarDirection(direction);
            bar.setBarGrouping(stacked ? BarGrouping.STACKED : BarGrouping.CLUSTERED);

            bar.setVaryColors(false);
        } else {
            chart.displayBlanksAs(DisplayBlanks.GAP);
            values.setNumberFormat("0.0%");
        }
        if (stacked) chart.getOrAddLegend().setPosition(LegendPosition.BOTTOM);
        int seriesCount = stacked ? value : 1;
        for (int i = 0; i < seriesCount; i++) {
            int column = stacked ? i + 1 : value;
            var source = XDDFDataSourcesFactory.fromNumericCellRange(
                t.sheet,
                new CellRangeAddress(header + 1, header + rows, column, column)
            );
            var series = data.addSeries(labels, source);
            series.setTitle(
                t.sheet.getRow(header).getCell(column).getStringCellValue(),
                new org.apache.poi.ss.util.CellReference(t.sheet.getSheetName(), header, column, true, true)
            );
            if (data instanceof XDDFBarChartData) {
                var shape = new XDDFShapeProperties();
                shape.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(color(stacked ? codes.get(i) : ""))));
                series.setShapeProperties(shape);
            }
            if (series instanceof XDDFLineChartData.Series line) {
                var shape = new XDDFShapeProperties();
                shape.setLineProperties(new XDDFLineProperties(new XDDFSolidFillProperties(XDDFColor.from(color("")))));
                line.setShapeProperties(shape);
                line.setMarkerStyle(MarkerStyle.CIRCLE);
                line.setSmooth(false);
            }
        }
        chart.plot(data);
        if (stacked) {
            // poi-ooxml-lite omits a compiled type used by XDDFBarChartData.setOverlap.
            var overlap = chart.getCTChart().getPlotArea().getBarChartArray(0).addNewOverlap();
            try (var cursor = overlap.newCursor()) {
                cursor.toNextToken();
                cursor.insertAttributeWithValue(new QName("val"), "100");
            }
        }
        if (!stacked && direction != null && !codes.isEmpty()) {
            var bars = chart.getCTChart().getPlotArea().getBarChartArray(0).getSerArray(0);
            for (int i = 0; i < codes.size(); i++) {
                var point = bars.addNewDPt();
                point.addNewIdx().setVal(i);
                point.addNewSpPr().addNewSolidFill().addNewSrgbClr().setVal(color(codes.get(i)));
            }
        }
    }

    private static byte[] color(String code) {
        return HexFormat.of().parseHex(COLORS.getOrDefault(code, "16746D"));
    }

    private static String label(String code) {
        return LABELS.getOrDefault(code, code);
    }

    private static String typeLabel(String code) {
        return Arrays.stream(PqrsType.values())
            .filter(type -> type.name().equals(code))
            .findFirst()
            .map(PqrsType::getValue)
            .orElseGet(() -> label(code));
    }

    private static String stateLabel(String code) {
        return Arrays.stream(PqrsStatus.values())
            .filter(state -> state.name().equals(code))
            .findFirst()
            .map(PqrsStatus::getDisplayName)
            .orElse("Estado no reconocido");
    }

    private static String month(PqrsStatistics.Month month) {
        String label = month.month().format(MONTH);
        return Character.toUpperCase(label.charAt(0)) + label.substring(1) + (month.complete() ? "" : " *");
    }

    private static String completeness(PqrsStatistics.Month month) {
        return month.complete() ? "Completo" : "Parcial (*)";
    }

    private static final class OfficeLabels {

        private final Set<String> repeated = new HashSet<>();

        OfficeLabels(List<OfficeCount> offices) {
            var names = new HashSet<String>();
            for (var office : offices) if (!names.add(name(office))) repeated.add(name(office));
        }

        String label(OfficeCount office) {
            String name = name(office);
            return office.id() != null && repeated.contains(name) ? name + " · " + office.id() + " (" + office.idType() + ")" : name;
        }

        private static String name(OfficeCount office) {
            if (!"ASSIGNED".equals(office.category())) return PqrsStatisticsWorkbook.label(office.category());
            return office.name() == null || office.name().isBlank() ? "Dependencia sin nombre" : office.name();
        }
    }

    private static final class Table {

        private static final int HEADER = 3;
        private final XSSFSheet sheet;
        private final Styles styles;
        private final int columns;
        private int next;

        Table(XSSFWorkbook wb, Styles styles, String name, String title, int[] widths, String... headers) {
            this.sheet = wb.createSheet(name);
            this.styles = styles;
            this.columns = widths.length;
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
            sheet.setDisplayGridlines(false);
            sheet.createFreezePane(0, HEADER + 1);
            sheet.setFitToPage(true);
            sheet.setRepeatingRows(new CellRangeAddress(HEADER, HEADER, -1, -1));
            var print = sheet.getPrintSetup();
            print.setPaperSize(PrintSetup.A4_PAPERSIZE);
            print.setLandscape(true);
            print.setFitWidth((short) 1);
            print.setFitHeight((short) 0);
            write(INSTITUTION, styles.heading, title);
            write(PRELIMINARY, styles.text);
            write(title, styles.heading);
            write(null, styles.heading, (Object[]) headers);
        }

        int row(Object... values) {
            return write(null, styles.text, values);
        }

        void total(Object... values) {
            write(null, styles.total, values);
        }

        void filter() {
            if (next > HEADER + 1) sheet.setAutoFilter(new CellRangeAddress(HEADER, next - 1, 0, columns - 1));
        }

        private int write(String banner, CellStyle style, Object... values) {
            var row = sheet.createRow(next++);
            if (banner != null) {
                sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, columns - 1));
                values = new Object[] { banner };
            } else if (row.getRowNum() < HEADER) {
                sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, columns - 1));
            }
            for (int i = 0; i < values.length; i++) {
                var cell = row.createCell(i);
                cell.setCellStyle(style);
                Object value = values[i];
                if (value instanceof BigDecimal percent) {
                    cell.setCellValue(percent.movePointLeft(2).doubleValue());
                    cell.setCellStyle(styles.percent);
                } else if (value instanceof Number number) {
                    cell.setCellValue(number.doubleValue());
                    cell.setCellStyle(style == styles.total ? styles.totalCount : styles.count);
                } else if (value instanceof LocalDateTime date) {
                    cell.setCellValue(date);
                    cell.setCellStyle(styles.timestamp);
                } else if (value instanceof LocalDate date) {
                    cell.setCellValue(date);
                    cell.setCellStyle(styles.date);
                } else if (value instanceof String text) {
                    cell.setCellValue(text.length() <= 32767 ? text : text.substring(0, 32767));
                }
            }
            return row.getRowNum();
        }
    }

    private static final class Styles {

        final CellStyle heading;
        final CellStyle text;
        final CellStyle count;
        final CellStyle percent;
        final CellStyle total;
        final CellStyle totalCount;
        final CellStyle date;
        final CellStyle timestamp;

        Styles(XSSFWorkbook wb) {
            text = wb.createCellStyle();
            text.setWrapText(true);
            text.setVerticalAlignment(VerticalAlignment.CENTER);
            heading = copy(wb, text);
            emphatic(wb, heading, "173742", "FFFFFF");
            total = copy(wb, text);
            emphatic(wb, total, "DDEEEB", "173742");
            count = number(wb, text, "#,##0");
            totalCount = number(wb, total, "#,##0");
            percent = number(wb, text, "0.0%");
            date = number(wb, text, "dd/mm/yyyy");
            timestamp = number(wb, text, "dd/mm/yyyy hh:mm:ss");
        }

        private static CellStyle copy(XSSFWorkbook wb, CellStyle base) {
            var style = wb.createCellStyle();
            style.cloneStyleFrom(base);
            return style;
        }

        private static CellStyle number(XSSFWorkbook wb, CellStyle base, String format) {
            var style = copy(wb, base);
            style.setAlignment(HorizontalAlignment.RIGHT);
            style.setDataFormat(wb.createDataFormat().getFormat(format));
            return style;
        }

        private static void emphatic(XSSFWorkbook wb, CellStyle style, String fill, String foreground) {
            ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(HexFormat.of().parseHex(fill)));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var font = wb.createFont();
            font.setColor(new XSSFColor(HexFormat.of().parseHex(foreground)));
            font.setBold(true);
            style.setFont(font);
        }
    }
}
