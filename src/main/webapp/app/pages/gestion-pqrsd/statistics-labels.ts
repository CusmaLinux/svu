const labels: Record<string, string> = {
  REQUEST: 'Petición',
  COMPLAINT: 'Queja',
  CLAIM: 'Reclamo',
  SUGGESTION: 'Sugerencia',
  DEMAND: 'Demanda',
  MISSING: 'Tipología ausente',
  HISTORICALLY_AMBIGUOUS: 'Tipología histórica ambigua',
  UNKNOWN: 'No reconocida',
  PENDING: 'Pendientes',
  IN_PROGRESS: 'En progreso',
  RESOLVED: 'Resueltas',
  CLOSED: 'Cerradas',
  REJECTED: 'Rechazadas',
  UNASSIGNED: 'Sin dependencia asignada',
  UNVERIFIABLE: 'Referencia no verificable',
  OBSERVED: 'Datos observados',
  PARTIAL: 'Información parcialmente disponible',
  TYPE_HISTORICALLY_AMBIGUOUS: 'Hay solicitudes antiguas cuya tipología es ambigua.',
  TYPE_MISSING: 'Hay solicitudes sin tipología registrada.',
  TYPE_UNKNOWN: 'Hay solicitudes con una tipología no reconocida.',
  STATE_UNKNOWN: 'Hay solicitudes con un estado no reconocido.',
  OFFICE_UNASSIGNED: 'Hay solicitudes sin dependencia asignada.',
  OFFICE_UNVERIFIABLE: 'Hay referencias a dependencias que no pudieron verificarse.',
  PROVISIONAL_PERIOD: 'El periodo incluye el día en curso y tiene un corte provisional.',
  ONLY_DATED_ROOTS: 'El cálculo incluye solicitudes con fecha de recepción válida.',
  OFFICE_AND_STATE_AT_OBSERVATION: 'Los estados y dependencias corresponden a la fecha y hora de consulta.',
  HISTORICAL_COMPLETENESS_UNMEASURED: 'La integridad histórica del registro está pendiente de medición.',
  OBSERVATION_NOT_TRANSACTIONAL: 'Si se actualizan solicitudes durante la consulta, la información puede variar.',
  PARTIAL_SOURCE: 'Información parcialmente disponible',
  SOURCE_UNAVAILABLE: 'Información no disponible',
  DEFINITION_PENDING: 'Indicador pendiente de definición institucional',
  'IND-08': 'Solicitudes respondidas',
  'IND-09': 'Pendientes al corte',
  'IND-10': 'Solicitudes vencidas',
  'IND-11': 'Respuestas en término',
  'IND-12': 'Tiempo medio de respuesta',
  'IND-13': 'Promedio de satisfacción',
  'IND-14': 'Porcentaje de satisfacción',
  'IND-15': 'Participación en encuestas',
  'IND-16': 'Traslados',
  'IND-17': 'Canales de atención',
  'IND-18': 'Grupos poblacionales',
  'IND-19': 'Sede',
};

export function label(code: string) {
  return Object.prototype.hasOwnProperty.call(labels, code) ? labels[code] : code;
}

const countFormatter = new Intl.NumberFormat('es-CO');
const percentFormatter = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 });

export const quantity = (value: number | null | undefined) => (value == null ? 'Información no disponible' : countFormatter.format(value));
export const percentage = (value: number) => percentFormatter.format(value) + ' %';

const monthFormatter = new Intl.DateTimeFormat('es-CO', { month: 'long', year: 'numeric', timeZone: 'UTC' });
export function monthName(month: string) {
  const name = monthFormatter.format(new Date(month + '-01T00:00:00Z')).replace(' de ', ' ');
  return name.charAt(0).toUpperCase() + name.slice(1);
}

export function civilDate(value: string) {
  const [year, month, day] = value.split('-');
  return `${day}/${month}/${year}`;
}
