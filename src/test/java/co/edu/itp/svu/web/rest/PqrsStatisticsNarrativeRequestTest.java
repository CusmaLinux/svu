package co.edu.itp.svu.web.rest;

import static org.assertj.core.api.Assertions.*;

import co.edu.itp.svu.service.report.PqrsStatisticsNarrativeText.Field;
import co.edu.itp.svu.service.report.PqrsStatisticsReceiptException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

class PqrsStatisticsNarrativeRequestTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PREFIX =
        """
        {"receipt":"SYNTHETIC-REFERENCE","filter":{"start":"2024-01-01","end":"2024-03-20","scope":"INSTITUTIONAL"},"narrative":
        """;

    @Test
    void acceptsRequiredEmptyObjectAndKeepsAbsentParagraphsExplicit() {
        var input = read("{}");
        assertThat(input.narrative().section(1))
            .hasSize(3)
            .allSatisfy(paragraph -> {
                assertThat(paragraph.drafted()).isFalse();
                assertThat(paragraph.displayText()).isEqualTo("Sin texto redactado.");
            });
        assertThat(input.toString()).doesNotContain("SYNTHETIC-REFERENCE");
    }

    @Test
    void acceptsAllTenFieldsAt4000Utf16UnitsEach() throws Exception {
        var fields = new LinkedHashMap<String, String>();
        for (var field : Field.values()) fields.put(field.id(), "🌳".repeat(2000));
        var input = read(JSON.writeValueAsString(fields));
        assertThat(input.narrative().section(7)).extracting(p -> p.value().length()).containsExactly(4000, 4000);
        assertThat(input.narrative().section(1))
            .extracting(p -> p.field().id())
            .containsExactly("introduction", "objective", "legalFramework");
        for (int section = 1; section <= 7; section++) assertThat(input.narrative().section(section)).allMatch(
            p -> p.value().length() == 4000
        );
    }

    @Test
    void checksLimitsBeforeNormalizationAndPreservesLiteralMultilineText() throws Exception {
        String literal = "  <script>run()</script> [enlace](https://example.test) =SUM(A1)\tñ 🌳\r\n\r\npárrafo\rfinal  ";
        var request = read(JSON.writeValueAsString(Map.of("objective", literal)));
        var p = request.narrative().section(1).get(1);
        assertThat(p.value()).isEqualTo(literal.replace("\r\n", "\n").replace('\r', '\n'));
        assertThat(p.toString()).doesNotContain("run()");
        String tooLong = JSON.writeValueAsString(Map.of("objective", "\r\n".repeat(2000) + "x"));
        assertThatThrownBy(() -> read(tooLong)).hasMessage("invalidNarrative");
        assertThatThrownBy(() -> read(JSON.writeValueAsString(Map.of("objective", "🌳".repeat(2001))))).hasMessage("invalidNarrative");
        var fields = new LinkedHashMap<String, String>();
        for (var field : Field.values()) fields.put(field.id(), "x".repeat(4000));
        fields.put(Field.INTRODUCTION.id(), "x".repeat(4001));
        assertThatThrownBy(() -> read(JSON.writeValueAsString(fields))).hasMessage("invalidNarrative");
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "null",
            "[]",
            "7",
            "true",
            "{\"objective\":null}",
            "{\"objective\":false}",
            "{\"objective\":2}",
            "{\"objective\":[]}",
            "{\"objective\":{}}",
            "{\"unknown\":\"SYNTHETIC-PRIVATE\"}",
        }
    )
    void rejectsInvalidNarrativeShapes(String body) {
        assertThatThrownBy(() -> read(body)).isInstanceOf(PqrsStatisticsReceiptException.class).hasMessage("invalidNarrative").hasNoCause();
    }

    @ParameterizedTest
    @ValueSource(
        strings = { "d800", "dc00", "0000", "0001", "000b", "001f", "007f", "0085", "009f", "fffe", "ffff", "202a", "202e", "2066", "2069" }
    )
    void rejectsForbiddenUnicodeRatherThanRemovingIt(String hex) {
        assertThatThrownBy(() -> read("{\"objective\":\"\\" + "u" + hex + "\"}")).hasMessage("invalidNarrative");
    }

    @Test
    void rejectsDuplicatesIncludingEquivalentEscapesAndStatisticsFromClient() {
        for (String json : List.of(
            PREFIX + "{\"objective\":\"a\",\"objective\":\"b\"}}",
            PREFIX + "{\"objective\":\"a\",\"\\" + "u006fbjective\":\"b\"}}",
            PREFIX.replace("\"receipt\":", "\"receipt\":\"another\",\"receipt\":") + "{}}",
            PREFIX.replace("\"scope\":", "\"scope\":\"OFFICE\",\"scope\":") + "{}}",
            PREFIX.replace("\"narrative\":", "\"statistics\":{},\"narrative\":") + "{}}",
            PREFIX.replace("\"scope\":", "\"officeName\":\"synthetic\",\"scope\":") + "{}}",
            PREFIX.substring(0, PREFIX.indexOf(",\"narrative\"")) + "}",
            PREFIX + "{\"objective\":[[[[[[\"too deep\"]]]]]]}}"
        )) {
            assertThatThrownBy(() -> parse(json)).hasMessage("invalidExportRequest").hasNoCause();
        }
    }

    @Test
    void requiresExplicitOfficeForOfficeScope() {
        var office = PREFIX.replace("\"INSTITUTIONAL\"", "\"OFFICE\",\"officeId\":\"synthetic\"");
        assertThat(parse(office + "{}}").filter().officeId()).isEqualTo("synthetic");
        assertThatThrownBy(() -> parse(PREFIX.replace("INSTITUTIONAL", "OFFICE") + "{}}")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enforcesByteLimitWithoutContentLengthAndRejectsInvalidUtf8AndCompression() {
        var request = new MockHttpServletRequest() {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContentType("application/json");
        String exact = PREFIX + "{}}";
        var bytes = (exact + " ".repeat(256 * 1024 - exact.getBytes(StandardCharsets.UTF_8).length)).getBytes(StandardCharsets.UTF_8);
        request.setContent(bytes);
        assertThat(PqrsStatisticsNarrativeRequest.export(request).receipt()).isEqualTo("SYNTHETIC-REFERENCE");
        request.setContent(Arrays.copyOf(bytes, bytes.length + 1));
        assertThatThrownBy(() -> PqrsStatisticsNarrativeRequest.export(request)).hasMessage("exportBodyTooLarge");
        request.setContent(new byte[] { (byte) 0xc0, (byte) 0xaf });
        assertThatThrownBy(() -> PqrsStatisticsNarrativeRequest.export(request)).hasMessage("invalidExportRequest").hasNoCause();
        request.addHeader("Content-Encoding", "gzip");
        assertThatThrownBy(() -> PqrsStatisticsNarrativeRequest.export(request)).hasMessage("unsupportedExportMedia");
    }

    private static co.edu.itp.svu.service.report.PqrsStatisticsDocumentService.ExportRequest read(String object) {
        return parse(PREFIX + object + "}");
    }

    private static co.edu.itp.svu.service.report.PqrsStatisticsDocumentService.ExportRequest parse(String json) {
        var request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent(json.getBytes(StandardCharsets.UTF_8));
        return PqrsStatisticsNarrativeRequest.export(request);
    }
}
