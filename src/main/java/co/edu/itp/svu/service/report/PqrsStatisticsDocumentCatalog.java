package co.edu.itp.svu.service.report;

import java.util.List;
import java.util.Map;

final class PqrsStatisticsDocumentCatalog {

    private PqrsStatisticsDocumentCatalog() {}

    static final String INSTITUTION = "Institución Universitaria del Putumayo — UNIPUTUMAYO";
    static final String PRELIMINARY = "Informe estadístico preliminar · No aprobado";
    static final String TOTAL = "Total del periodo";
    static final Map<String, String> LABELS = Map.ofEntries(
        Map.entry("MISSING", "Tipología ausente"),
        Map.entry("HISTORICALLY_AMBIGUOUS", "Tipología histórica ambigua"),
        Map.entry("UNKNOWN", "No reconocido"),
        Map.entry("UNASSIGNED", "Sin dependencia asignada"),
        Map.entry("UNVERIFIABLE", "Referencia no verificable"),
        Map.entry("OBSERVED", "Datos observados"),
        Map.entry("PARTIAL", "Información parcialmente disponible"),
        Map.entry("PARTIAL_SOURCE", "Información parcialmente disponible"),
        Map.entry("SOURCE_UNAVAILABLE", "Información no disponible"),
        Map.entry("DEFINITION_PENDING", "Indicador pendiente de definición institucional"),
        Map.entry("TYPE_HISTORICALLY_AMBIGUOUS", "Hay solicitudes antiguas cuya tipología es ambigua."),
        Map.entry("TYPE_MISSING", "Hay solicitudes sin tipología registrada."),
        Map.entry("TYPE_UNKNOWN", "Hay solicitudes con tipología no reconocida."),
        Map.entry("STATE_UNKNOWN", "Hay solicitudes con un estado no reconocido."),
        Map.entry("OFFICE_UNASSIGNED", "Hay solicitudes sin dependencia asignada."),
        Map.entry("OFFICE_UNVERIFIABLE", "Hay referencias a dependencias que no pudieron verificarse."),
        Map.entry("PROVISIONAL_PERIOD", "El periodo incluye el día en curso y tiene un corte provisional."),
        Map.entry("ONLY_DATED_ROOTS", "El cálculo incluye solicitudes con fecha de recepción válida."),
        Map.entry("OFFICE_AND_STATE_AT_OBSERVATION", "Los estados y dependencias corresponden a la fecha y hora de consulta."),
        Map.entry("HISTORICAL_COMPLETENESS_UNMEASURED", "La integridad histórica del registro está pendiente de medición."),
        Map.entry("OBSERVATION_NOT_TRANSACTIONAL", "Las actualizaciones durante la consulta pueden afectar la información."),
        Map.entry("CALCULATED", "Calculada"),
        Map.entry("NO_CALCULABLE", "Variación no calculable · base cero"),
        Map.entry("NO_COMPARABLE_BASE", "Variación no calculable · sin base comparable"),
        Map.entry("PARTIAL_MONTH", "Variación no calculable · mes parcial")
    );
    static final Map<String, String> COLORS = Map.ofEntries(
        Map.entry("REQUEST", "16746D"),
        Map.entry("COMPLAINT", "355A9C"),
        Map.entry("CLAIM", "A36123"),
        Map.entry("SUGGESTION", "6456A0"),
        Map.entry("DEMAND", "A13760"),
        Map.entry("MISSING", "6F7477"),
        Map.entry("HISTORICALLY_AMBIGUOUS", "926528"),
        Map.entry("UNKNOWN", "555B65"),
        Map.entry("RESOLVED", "16746D"),
        Map.entry("PENDING", "A36123"),
        Map.entry("IN_PROGRESS", "355A9C"),
        Map.entry("CLOSED", "6456A0"),
        Map.entry("REJECTED", "A13760"),
        Map.entry("UNASSIGNED", "6F7477"),
        Map.entry("UNVERIFIABLE", "926528")
    );
    static final List<String> INDICATORS = List.of(
        "Solicitudes recibidas",
        "Distribución mensual",
        "Distribución por tipología",
        "Porcentaje por tipología",
        "Variación mensual",
        "Dependencia actual",
        "Estado actual",
        "Solicitudes respondidas",
        "Pendientes al corte",
        "Solicitudes vencidas",
        "Respuestas en término",
        "Tiempo medio de respuesta",
        "Promedio de satisfacción",
        "Porcentaje de satisfacción",
        "Participación en encuestas",
        "Traslados",
        "Canales de atención",
        "Grupos poblacionales",
        "Sede"
    );
    static final List<String> INDICATOR_NOTES = List.of(
        "Conteo de solicitudes con fecha válida en el periodo.",
        "Mes civil en Colombia.",
        "Incluye categorías de calidad del registro.",
        "Utiliza el total recibido como denominador.",
        "Requiere meses comparables y una base anterior positiva.",
        "Oficina responsable al consultar.",
        "Estado al consultar.",
        "Las intervenciones institucionales registradas no identifican la respuesta final.",
        "Se necesita el historial de estados para reconstruir un corte anterior.",
        "El plazo actual no acredita el vencimiento histórico.",
        "Requiere respuesta final y plazo aplicable.",
        "Se necesita identificar el momento de respuesta final.",
        "Se requiere verificar notas y vínculos de la encuesta.",
        "El umbral de satisfacción requiere definición institucional.",
        "La población elegible y la ventana de respuesta requieren definición.",
        "Falta un historial de cambios de dependencia.",
        "Falta una fuente estructurada de canales.",
        "Falta una clasificación autorizada de grupos.",
        "Falta una relación verificable con la sede."
    );
}
