package co.edu.itp.svu.service.report;

import co.edu.itp.svu.domain.InformePqrs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;

public class PqrsExcelGenerator {

    public static ByteArrayInputStream generate(InformePqrs informPqrs) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Informe PQRS");

            createDataTable(sheet, informPqrs);

            createChart(sheet, informPqrs);

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private static void createDataTable(XSSFSheet sheet, InformePqrs inform) {
        Font headerFont = sheet.getWorkbook().createFont();
        headerFont.setBold(true);
        CellStyle headerCellStyle = sheet.getWorkbook().createCellStyle();
        headerCellStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(0);
        Cell headerCell1 = headerRow.createCell(0);
        headerCell1.setCellValue("Campo");
        headerCell1.setCellStyle(headerCellStyle);

        Cell headerCell2 = headerRow.createCell(1);
        headerCell2.setCellValue("Valor");
        headerCell2.setCellStyle(headerCellStyle);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

        List<Object[]> data = List.of(
            new Object[] { "ID:", inform.getId() },
            new Object[] { "Fecha Inicio:", formatter.format(inform.getFechaInicio()) },
            new Object[] { "Fecha Fin:", formatter.format(inform.getFechaFin()) },
            new Object[] { "Oficina:", inform.getOficina() != null ? inform.getOficina().getNombre() : "N/A" },
            new Object[] { "Total PQRS:", inform.getTotalPqrs() },
            new Object[] { "Total Resueltas:", inform.getTotalResueltas() },
            new Object[] { "Total Pendientes:", inform.getTotalPendientes() }
        );

        int rowIdx = 1;
        for (Object[] rowData : data) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue((String) rowData[0]);

            Cell valueCell = row.createCell(1);
            if (rowData[1] instanceof Integer) {
                valueCell.setCellValue((Integer) rowData[1]);
            } else {
                valueCell.setCellValue((String) rowData[1]);
            }
        }
    }

    private static void createChart(XSSFSheet sheet, InformePqrs inform) {
        // --- Chart Data Source ---
        // It starting at column E (index 4).
        Row categoryRow = sheet.getRow(0) != null ? sheet.getRow(0) : sheet.createRow(0);
        categoryRow.createCell(4).setCellValue("Resumen");

        Row valueRow = sheet.getRow(1) != null ? sheet.getRow(1) : sheet.createRow(1);

        // Categories for X-Axis
        categoryRow.createCell(5).setCellValue("Total PQRS");
        categoryRow.createCell(6).setCellValue("Total Resueltas");
        categoryRow.createCell(7).setCellValue("Total Pendientes");

        // Values for Y-Axis
        valueRow.createCell(5).setCellValue(inform.getTotalPqrs());
        valueRow.createCell(6).setCellValue(inform.getTotalResueltas());
        valueRow.createCell(7).setCellValue(inform.getTotalPendientes());

        XSSFDrawing drawing = sheet.createDrawingPatriarch();

        // Place the chart from column D, row 9 to column K, row 25
        XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 9, 11, 25);

        XSSFChart chart = drawing.createChart(anchor);

        chart.setTitleText(getQuarterTitle(inform.getFechaInicio(), inform.getFechaFin()));
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP_RIGHT);

        // Axes
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Estado PQRS");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Cantidad");

        // Data Sources
        XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(sheet, new CellRangeAddress(0, 0, 5, 7));
        XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(sheet, new CellRangeAddress(1, 1, 5, 7));

        // Chart Type (Bar Chart)
        XDDFChartData data = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        XDDFBarChartData.Series series = (XDDFBarChartData.Series) data.addSeries(categories, values);
        series.setTitle("Resumen", null);

        data.setVaryColors(true);
        chart.plot(data);
    }

    private static String getQuarterTitle(Instant fechaInicio, Instant fechaFin) {
        LocalDate date = LocalDate.ofInstant(fechaInicio, ZoneId.systemDefault());
        int year = date.getYear();
        int quarter = (date.getMonthValue() - 1) / 3 + 1;
        return String.format("Resumen de PQRS - Trimestre %d del %d", quarter, year);
    }
}
