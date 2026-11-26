package co.edu.itp.svu.service.report;

import co.edu.itp.svu.service.report.PqrsStatisticsDocument.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import javax.imageio.ImageIO;

/** A bounded headless PNG renderer. Complete labels and values remain in the chart model and companion table. */
public final class PqrsStatisticsChartImage {

    static final int WIDTH = 1200;
    static final int HEIGHT = 720;
    static final int MAX_CATEGORIES = 24;
    static final int MAX_SERIES = 12;
    static final int MAX_LABEL_UNITS = 512;
    private static final Color TEXT = new Color(0x253744);
    private static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 18);

    private PqrsStatisticsChartImage() {}

    public static byte[] png(Chart chart) {
        validate(chart);
        var image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        var canvas = image.createGraphics();
        try {
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setColor(Color.WHITE);
            canvas.fillRect(0, 0, WIDTH, HEIGHT);
            canvas.setColor(TEXT);
            canvas.setFont(FONT.deriveFont(Font.BOLD, 26f));
            drawText(canvas, chart.title(), 30, 39, WIDTH - 60);
            canvas.setFont(FONT);
            canvas.drawString(chart.unit() == Unit.PERCENT ? "Variación (%)" : "Solicitudes", 30, 63);
            if (chart.kind() == ChartKind.BARS) horizontal(canvas, chart);
            else vertical(canvas, chart);
            if (chart.categories().stream().anyMatch(ChartCategory::partial)) canvas.drawString("* Mes parcial", 30, HEIGHT - 19);
            if (chart.notes().stream().anyMatch(note -> note.startsWith("Se muestran"))) {
                canvas.drawString("Gráfica parcial. La tabla conserva todas las categorías.", WIDTH / 2, HEIGHT - 19);
            }
        } finally {
            canvas.dispose();
        }
        try (var bytes = new ByteArrayOutputStream()) {
            var writer = ImageIO.getImageWritersByFormatName("png");
            if (!writer.hasNext()) throw PqrsStatisticsReceiptException.unavailable();
            // Use a memory-backed stream without creating ImageIO's optional disk cache.
            try (var output = new javax.imageio.stream.MemoryCacheImageOutputStream(bytes)) {
                var encoder = writer.next();
                try {
                    encoder.setOutput(output);
                    encoder.write(image);
                } finally {
                    encoder.dispose();
                }
            }
            return bytes.toByteArray();
        } catch (IOException failure) {
            throw PqrsStatisticsReceiptException.unavailable();
        }
    }

    private static void validate(Chart chart) {
        int count = chart.categories().size();
        if (
            count == 0 ||
            count > MAX_CATEGORIES ||
            chart.series().isEmpty() ||
            chart.series().size() > MAX_SERIES ||
            chart.title().length() > MAX_LABEL_UNITS ||
            chart.categories().stream().anyMatch(c -> c.label().length() > MAX_LABEL_UNITS) ||
            chart.series().stream().anyMatch(s -> s.points().size() != count || s.label().length() > MAX_LABEL_UNITS) ||
            ((chart.kind() == ChartKind.BARS || chart.kind() == ChartKind.LINE) && chart.series().size() != 1)
        ) {
            throw new PqrsStatisticsReceiptException(503, "exportChartCapacity");
        }
    }

    private static void horizontal(Graphics2D g, Chart chart) {
        var series = chart.series().get(0);
        var maximum = domain(chart).maximum();
        int spacing = 550 / chart.categories().size();
        int left = 395, width = 615;
        var format = numberFormat(chart);
        for (int i = 0; i < chart.categories().size(); i++) {
            int y = 92 + i * spacing;
            drawText(g, chart.categories().get(i).label(), 30, y + 14, left - 55);
            BigDecimal value = series.points().get(i).value();
            if (value == null) continue;
            int length = (int) Math.round((value.doubleValue() / maximum) * width);
            g.setColor(new Color(chart.categories().get(i).rgb()));
            g.fillRect(left, y, length, Math.min(22, spacing - 5));
            g.setColor(TEXT);
            g.drawString(format.format(value), left + length + 6, y + 14);
        }
    }

    private static void vertical(Graphics2D g, Chart chart) {
        int left = 110, right = 1160, top = 85;
        int bottom = chart.kind() == ChartKind.STACKED_COLUMNS ? 500 : 620;
        var bounds = domain(chart);
        var format = numberFormat(chart);
        double previousTick = Double.NEGATIVE_INFINITY;
        for (int step = 0; step <= 4; step++) {
            double value = bounds.minimum() * (1 - step / 4d) + bounds.maximum() * (step / 4d);
            if (chart.unit() == Unit.REQUESTS) value = Math.floor(value);
            if (value <= previousTick) continue;
            previousTick = value;
            int y = bounds.y(value, top, bottom);
            g.setColor(new Color(0xDCE3E8));
            g.drawLine(left, y, right, y);
            g.setColor(TEXT);
            drawText(g, format.format(value), 12, y + 4, left - 18);
        }

        int slot = (right - left) / chart.categories().size();
        int width = Math.min(42, slot - 7);
        int previousY = 0;
        boolean previousExists = false;
        for (int i = 0; i < chart.categories().size(); i++) {
            var category = chart.categories().get(i);
            int x = left + slot * i + slot / 2;
            String suffix = category.partial() ? "*" : "";
            var metrics = g.getFontMetrics();
            String label = fit(metrics, category.label(), slot - 4 - metrics.stringWidth(suffix)) + suffix;
            drawText(g, label, x - metrics.stringWidth(label) / 2, bottom + 22, slot - 4);
            if (chart.kind() == ChartKind.LINE) {
                var series = chart.series().get(0);
                BigDecimal value = series.points().get(i).value();
                if (value != null) {
                    int y = bounds.y(value.doubleValue(), top, bottom);
                    g.setColor(new Color(series.rgb()));
                    g.setStroke(new BasicStroke(2));
                    if (previousExists) g.drawLine(x - slot, previousY, x, y);
                    g.fillOval(x - 4, y - 4, 8, 8);
                    g.setColor(TEXT);
                    previousY = y;
                }
                previousExists = value != null;
            } else {
                double lower = 0;
                for (var series : chart.series()) {
                    BigDecimal value = series.points().get(i).value();
                    if (value == null) continue;
                    double upper = lower + value.doubleValue();
                    int y = bounds.y(upper, top, bottom);
                    g.setColor(new Color(series.rgb()));
                    g.fillRect(x - width / 2, y, width, bounds.y(lower, top, bottom) - y);
                    lower = upper;
                }
                g.setColor(TEXT);
            }
        }
        if (chart.kind() == ChartKind.STACKED_COLUMNS) {
            for (int i = 0; i < chart.series().size(); i++) {
                var series = chart.series().get(i);
                int x = 30 + ((i % 3) * 390), y = 559 + (i / 3) * 25;
                g.setColor(new Color(series.rgb()));
                g.fillRect(x, y - 11, 12, 12);
                g.setColor(TEXT);
                drawText(g, series.label(), x + 18, y, 350);
            }
        }
    }

    private static NumberFormat numberFormat(Chart chart) {
        var format = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));
        format.setMaximumFractionDigits(chart.unit() == Unit.PERCENT ? 1 : 0);
        return format;
    }

    private static Domain domain(Chart chart) {
        var values = new ArrayList<Double>();
        for (int index = 0; index < chart.categories().size(); index++) {
            double sum = 0;
            for (var series : chart.series()) {
                var point = series.points().get(index);
                if (point.value() == null) continue;
                double value = point.value().doubleValue();
                if (!Double.isFinite(value) || (chart.kind() != ChartKind.LINE && value < 0)) {
                    throw PqrsStatisticsReceiptException.unavailable();
                }
                sum += value;
                values.add(value);
            }
            if (chart.kind() == ChartKind.STACKED_COLUMNS) values.add(sum);
        }
        double min = Math.min(0, values.stream().mapToDouble(Double::doubleValue).min().orElse(0));
        double max = Math.max(0, values.stream().mapToDouble(Double::doubleValue).max().orElse(0));
        if (!Double.isFinite(max - min)) throw PqrsStatisticsReceiptException.unavailable();
        return new Domain(min, max == min ? 1 : max);
    }

    private record Domain(double minimum, double maximum) {
        int y(double value, int top, int bottom) {
            return bottom - (int) Math.round(((value - minimum) / (maximum - minimum)) * (bottom - top));
        }
    }

    private static void drawText(Graphics2D g, String value, int x, int y, int width) {
        String fitted = fit(g.getFontMetrics(), value, width);
        if (g.getFont().canDisplayUpTo(fitted) != -1) {
            throw new PqrsStatisticsReceiptException(503, "exportChartFontUnavailable");
        }
        g.drawString(fitted, x, y);
    }

    /** Visual elision only. The chart definition and table retain the full label. */
    private static String fit(java.awt.FontMetrics metrics, String value, int width) {
        if (metrics.stringWidth(value) <= width) return value;
        int end = value.length();
        while (end > 0 && metrics.stringWidth(value.substring(0, end) + "…") > width) end = value.offsetByCodePoints(end, -1);
        return value.substring(0, end) + "…";
    }
}
