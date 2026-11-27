package co.edu.itp.svu.service.report;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/** Normalized literal text. Instances can only be created after validation. */
public final class PqrsStatisticsNarrativeText {

    private static final int FIELD_LIMIT = 4_000;
    private static final int TOTAL_LIMIT = 40_000;
    private static final Map<String, Field> FIELDS = Arrays.stream(Field.values()).collect(
        java.util.stream.Collectors.toUnmodifiableMap(Field::id, field -> field)
    );

    public enum Field {
        INTRODUCTION("introduction", "Introducción", 1),
        OBJECTIVE("objective", "Objetivo", 1),
        LEGAL_FRAMEWORK("legalFramework", "Marco normativo", 1),
        REQUESTS_ANALYSIS("requestsAnalysis", "Análisis del comportamiento de las solicitudes", 2),
        USERS_CHANNELS_ANALYSIS("usersChannelsAnalysis", "Observaciones y análisis de usuarios y canales", 3),
        ROUTING_ANALYSIS("routingAnalysis", "Observaciones y análisis de direccionamiento", 4),
        OFFICES_ANALYSIS("officesAnalysis", "Análisis de resultados por dependencia", 5),
        INDICATORS_INTERPRETATION("indicatorsInterpretation", "Interpretación de resultados y limitaciones", 6),
        CONCLUSIONS("conclusions", "Conclusiones", 7),
        RECOMMENDATIONS("recommendations", "Recomendaciones", 7);

        private final String id;
        private final String label;
        private final int section;

        Field(String id, String label, int section) {
            this.id = id;
            this.label = label;
            this.section = section;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public int section() {
            return section;
        }
    }

    public record Paragraph(Field field, String value) {
        public boolean drafted() {
            return !value.isBlank();
        }
        public String displayText() {
            return drafted() ? value : "Sin texto redactado.";
        }
        @Override
        public String toString() {
            return "Narrative paragraph";
        }
    }

    private final Map<Field, String> fields;

    private PqrsStatisticsNarrativeText(Map<Field, String> fields) {
        this.fields = Map.copyOf(fields);
    }

    /** The input tree must come from the duplicate-detecting request reader. */
    public static PqrsStatisticsNarrativeText validate(JsonNode narrative) {
        if (narrative == null || !narrative.isObject()) throw invalid();
        Map<Field, String> raw = new EnumMap<>(Field.class);
        int total = 0;
        var entries = narrative.fields();
        while (entries.hasNext()) {
            var entry = entries.next();
            var field = FIELDS.get(entry.getKey());
            if (field == null || !entry.getValue().isTextual()) throw invalid();
            String value = entry.getValue().textValue();
            total = checkLength(value, total);
            if (value.codePoints().anyMatch(PqrsStatisticsNarrativeText::forbidden)) throw invalid();
            raw.put(field, value);
        }

        return normalized(raw);
    }

    public static PqrsStatisticsNarrativeText validate(Map<String, String> narrative) {
        if (narrative == null) throw invalid();
        Map<Field, String> raw = new EnumMap<>(Field.class);
        int total = 0;
        for (var entry : narrative.entrySet()) {
            Field field = FIELDS.get(entry.getKey());
            String value = entry.getValue();
            if (field == null || value == null) throw invalid();
            total = checkLength(value, total);
            if (value.codePoints().anyMatch(PqrsStatisticsNarrativeText::forbidden)) throw invalid();
            raw.put(field, value);
        }
        return normalized(raw);
    }

    public Map<String, String> asMap() {
        Map<String, String> values = new LinkedHashMap<>();
        fields.forEach((field, value) -> values.put(field.id(), value));
        return Map.copyOf(values);
    }

    private static PqrsStatisticsNarrativeText normalized(Map<Field, String> raw) {
        Map<Field, String> normalized = new EnumMap<>(Field.class);
        int total = 0;
        for (var entry : raw.entrySet()) {
            String value = entry.getValue().replace("\r\n", "\n").replace('\r', '\n');
            total = checkLength(value, total);
            normalized.put(entry.getKey(), value);
        }
        return new PqrsStatisticsNarrativeText(normalized);
    }

    public List<Paragraph> section(int number) {
        return Arrays.stream(Field.values())
            .filter(field -> field.section() == number)
            .map(field -> new Paragraph(field, fields.getOrDefault(field, "")))
            .toList();
    }

    private static int checkLength(String value, int total) {
        if (value.length() > FIELD_LIMIT || value.length() > TOTAL_LIMIT - total) throw invalid();
        return total + value.length();
    }

    private static boolean forbidden(int cp) {
        return (
            (cp < 0x20 && cp != '\t' && cp != '\n' && cp != '\r') ||
            (cp >= 0x7f && cp <= 0x9f) ||
            (cp >= 0xd800 && cp <= 0xdfff) ||
            cp == 0xfffe ||
            cp == 0xffff ||
            (cp >= 0x202a && cp <= 0x202e) ||
            (cp >= 0x2066 && cp <= 0x2069)
        );
    }

    private static PqrsStatisticsReceiptException invalid() {
        return new PqrsStatisticsReceiptException(400, "invalidNarrative");
    }

    @Override
    public String toString() {
        return "Validated narrative";
    }
}
