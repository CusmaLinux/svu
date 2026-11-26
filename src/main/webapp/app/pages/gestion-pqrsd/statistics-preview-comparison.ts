import { label, monthName } from './statistics-labels';
import type { NarrativePreview, OfficeCount } from './statistics.model';

const officeKey = (office: OfficeCount) => JSON.stringify([office.category, office.idType, office.id]);
const officeLabel = (office: OfficeCount) =>
  office.category === 'ASSIGNED' ? `${office.name ?? 'Dependencia'} (${office.id}, ${office.idType})` : label(office.category);

function entries(preview: NarrativePreview) {
  const result = new Map<string, { title: string; value: string }>();
  const add = (key: string, title: string, value: string | number | null) =>
    result.set(key, { title, value: value == null ? 'Información no disponible' : String(value) });
  const statistics = preview.statistics;
  add('received', 'Total de solicitudes', statistics.received);
  add('versions', 'Modelo, metodología y catálogo', [preview.modelVersion, preview.methodologyVersion, preview.catalogVersion].join(' / '));
  const office = preview.resolvedOffice;
  if (office)
    add('resolved-office', 'Dependencia consultada', `${office.name ?? 'Dependencia sin nombre'} · ${office.id} (${office.idType})`);
  add('coverage', 'Cobertura', label(statistics.coverage.status));
  add('warnings', 'Advertencias', statistics.coverage.warnings.map(label).sort().join(' · ') || 'Sin advertencias');
  add('limits', 'Limitaciones', statistics.coverage.limitations.map(label).sort().join(' · ') || 'Sin limitaciones');
  add('available', 'Indicadores disponibles', [...preview.availableIndicators].sort().join(', '));
  for (const [code, status] of Object.entries(statistics.deferredIndicators)) add(code, label(code), label(status));
  for (const type of statistics.types) {
    add(
      'type-' + type.code,
      label(type.code),
      `${type.count} / ${type.denominator} · ${type.percent == null ? 'No calculable' : type.percent + ' %'}`,
    );
  }
  for (const [state, count] of Object.entries(statistics.currentStates)) add('state-' + state, label(state), count);
  for (const office of statistics.currentOffices) add('office-' + officeKey(office), officeLabel(office), office.count);
  for (const month of statistics.months) {
    const name = monthName(month.month);
    const variation =
      month.variationPercent == null
        ? {
            PARTIAL_MONTH: 'Mes parcial',
            NO_COMPARABLE_BASE: 'Sin base comparable',
            NO_CALCULABLE: 'No calculable',
            CALCULATED: 'No disponible',
          }[month.comparison]
        : month.variationPercent + ' %';
    add(month.month, name, `${month.received} recibidas · base ${month.previousCount ?? 'no disponible'} · ${variation}`);
    for (const [type, count] of Object.entries(month.types)) add(month.month + type, name + ' · ' + label(type), count);
    for (const office of month.currentOffices) add(month.month + officeKey(office), name + ' · ' + officeLabel(office), office.count);
  }
  return result;
}

export function compareNarrativePreviews(original: NarrativePreview, replacement: NarrativePreview) {
  const before = entries(original);
  const after = entries(replacement);
  return [...new Set([...before.keys(), ...after.keys()])].flatMap(key => {
    const previous = before.get(key);
    const next = after.get(key);
    if (previous?.value === next?.value && previous?.title === next?.title) return [];
    return [
      {
        key,
        title: next?.title ?? previous?.title ?? '',
        before: previous ? (next && previous.title !== next.title ? previous.title + ' · ' : '') + previous.value : 'Sin dato observado',
        after: next?.value ?? 'Sin dato observado',
      },
    ];
  });
}
