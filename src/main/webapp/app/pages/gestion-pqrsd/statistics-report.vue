<script setup lang="ts">
import { computed } from 'vue';
import StatisticsChart from './statistics-chart.vue';
import StatisticsTable from './statistics-table.vue';
import { civilDate, label, monthName, percentage, quantity } from './statistics-labels';
import type { NarrativeSlot } from './report-narrative';
import type { ChartOptions, MonthlyStatistics, OfficeCount, PqrsStatistics } from './statistics.model';

const props = defineProps<{ report: PqrsStatistics }>();
defineSlots<{ narrative: NarrativeSlot }>();
const teal = '#16746d';
const colors: Record<string, string> = {
  REQUEST: teal,
  COMPLAINT: '#355a9c',
  CLAIM: '#a36123',
  SUGGESTION: '#6456a0',
  DEMAND: '#a13760',
  MISSING: '#6f7477',
  HISTORICALLY_AMBIGUOUS: '#926528',
  UNKNOWN: '#555b65',
  RESOLVED: teal,
  PENDING: '#a36123',
  IN_PROGRESS: '#355a9c',
  CLOSED: '#6456a0',
  REJECTED: '#a13760',
  UNASSIGNED: '#6f7477',
  UNVERIFIABLE: '#926528',
};
const knownStates = ['RESOLVED', 'PENDING', 'IN_PROGRESS', 'CLOSED', 'REJECTED'];
const states = computed(() => ((props.report.currentStates.UNKNOWN ?? 0) > 0 ? [...knownStates, 'UNKNOWN'] : knownStates));
const stateName = (code: string) => (code === 'UNKNOWN' ? 'Estado no reconocido' : label(code));
const coverage = computed(() => [...new Set([...props.report.coverage.warnings, ...props.report.coverage.limitations])]);
const months = computed(() => props.report.months.map(row => monthName(row.month) + (row.complete ? '' : ' *')));
const stateRows = computed(() =>
  Object.entries(props.report.currentStates).map(([code, count]) => ({
    name: stateName(code),
    count,
    color: colors[code],
  })),
);
const index = [
  ['introduction', 'Introducción'],
  ['distribution', 'Consolidado'],
  ['section-3', 'Usuarios y canales'],
  ['section-4', 'Traslados'],
  ['offices', 'Dependencias'],
  ['section-6', 'Oportunidad y satisfacción'],
  ['conclusions', 'Conclusiones'],
];
function goToSection(id: string) {
  const heading = document.getElementById(id);
  heading?.focus({ preventScroll: true });
  heading?.scrollIntoView?.({ block: 'start' });
}
const officeKey = (office: OfficeCount) => JSON.stringify([office.category, office.idType, office.id]);
const officeLabels = computed(() => {
  const labels = new Map<string, string>();
  const allOffices = [...props.report.currentOffices, ...props.report.months.flatMap(month => month.currentOffices)];
  for (const office of allOffices) {
    labels.set(officeKey(office), office.category === 'ASSIGNED' ? office.name || 'Dependencia sin nombre' : label(office.category));
  }
  const duplicateLabels = new Map<string, number>();
  labels.forEach(name => duplicateLabels.set(name, (duplicateLabels.get(name) ?? 0) + 1));
  for (const office of allOffices) {
    const key = officeKey(office);
    const name = labels.get(key);
    if (name && duplicateLabels.has(name) && (duplicateLabels.get(name) ?? 0) > 1 && office.id) {
      labels.set(key, `${name} · ${office.id} (${office.idType})`);
    }
  }
  return labels;
});
const officeName = (office: OfficeCount) => officeLabels.value.get(officeKey(office)) ?? label(office.category);
const selectedOffice = computed(() => {
  const office = props.report.currentOffices.find(entry => entry.id === props.report.filter.officeId && entry.category === 'ASSIGNED');
  return office ? officeName(office) : props.report.filter.officeId;
});
const officeRows = computed(() => props.report.currentOffices.map(office => [officeName(office), quantity(office.count)]));
const monthlyOfficeRows = computed(() =>
  props.report.months.flatMap((month, index) =>
    month.currentOffices.map(office => [months.value[index], officeName(office), quantity(office.count)]),
  ),
);
const typeRows = computed(() =>
  props.report.types.map(type => [
    label(type.code),
    quantity(type.count),
    quantity(type.denominator),
    type.percent == null ? 'No calculable' : percentage(type.percent),
  ]),
);
const deferredSections = [
  {
    id: 3,
    title: 'Caracterización de usuarios y canales',
    note: 'El registro actual carece de datos estructurados para identificar grupos de usuarios, canales de ingreso y sedes.',
    codes: ['IND-18', 'IND-17', 'IND-19'],
  },
  {
    id: 4,
    title: 'Direccionamiento y traslados',
    note: 'La dependencia responsable actual está disponible. Para contar traslados se necesita el historial de cambios de asignación.',
    codes: ['IND-16'],
  },
  {
    id: 6,
    title: 'Oportunidad de respuesta y satisfacción',
    note: 'Para medir la oportunidad se necesita identificar la respuesta final y el plazo aplicable. Los pendientes al corte requieren un historial de estados. Las encuestas tienen cobertura parcial y algunos indicadores de satisfacción siguen pendientes de definición.',
    codes: ['IND-08', 'IND-09', 'IND-10', 'IND-11', 'IND-12', 'IND-13', 'IND-14', 'IND-15'],
  },
] as const;

function observed(value: string) {
  return new Intl.DateTimeFormat('es-CO', { timeZone: props.report.period.zone, dateStyle: 'medium', timeStyle: 'medium' }).format(
    new Date(value),
  );
}

function variation(month: MonthlyStatistics) {
  if (month.variationPercent != null) return percentage(month.variationPercent);
  if (month.comparison === 'PARTIAL_MONTH') return 'Variación no calculable · mes parcial';
  if (month.comparison === 'NO_COMPARABLE_BASE') return 'Variación no calculable · sin base comparable';
  return month.comparison === 'NO_CALCULABLE' ? 'Variación no calculable · base cero' : 'Información no disponible';
}

type Bar = { name: string; count: number; color?: string; percent?: number | null };
const chartHeight = (rows: number) => Math.min(500, Math.max(245, 85 + rows * 48));
function horizontal(rows: Bar[]): ChartOptions | null {
  if (!rows.length) return null;
  const axisLabel = { width: 170, overflow: 'break' as const, interval: 0 };
  return {
    grid: { left: 8, right: rows.length > 8 ? 38 : 22, top: 12, bottom: 32, containLabel: true },
    tooltip: { trigger: 'axis', renderMode: 'richText', confine: true, axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', minInterval: 1, name: 'Solicitudes', nameLocation: 'middle', nameGap: 25, boundaryGap: [0, '30%'] },
    yAxis: { type: 'category', inverse: true, data: rows.map(row => row.name), axisLabel },
    dataZoom: rows.length > 8 ? [{ type: 'slider', yAxisIndex: 0, startValue: 0, endValue: 7, showDetail: false }] : [],
    series: [
      {
        type: 'bar',
        name: 'Solicitudes',
        barMaxWidth: 24,
        label: {
          show: true,
          position: 'right',
          formatter: params => {
            const row = rows[params.dataIndex];
            return quantity(row.count) + (row.percent == null ? '' : ' · ' + percentage(row.percent));
          },
        },
        labelLayout: { hideOverlap: true },
        data: rows.map(row => ({
          value: row.count,
          itemStyle: { color: row.color ?? teal },
          tooltip: { valueFormatter: () => quantity(row.count) + (row.percent == null ? '' : ' · ' + percentage(row.percent)) },
        })),
      },
    ],
    media: [
      { query: { maxWidth: 365 }, option: { yAxis: { axisLabel: { ...axisLabel, width: 112 } }, series: [{ label: { show: false } }] } },
      { option: { yAxis: { axisLabel }, series: [{ label: { show: true } }] } },
    ],
  };
}

const seriesBase = computed<ChartOptions>(() => ({
  color: [teal],
  grid: { top: 60, left: 8, right: 22, bottom: 16, containLabel: true },
  tooltip: { trigger: 'axis', renderMode: 'richText', confine: true },
  xAxis: { type: 'category', data: months.value, axisLabel: { rotate: 35, hideOverlap: true } },
  yAxis: { type: 'value', minInterval: 1, boundaryGap: [0, '15%'] },
  legend: { type: 'scroll', top: 4 },
}));

const quantityLabel = { show: true, position: 'top' as const };
const narrowChart = [
  { query: { maxWidth: 365 }, option: { series: [{ label: { show: false } }] } },
  { option: { series: [{ label: { show: true } }] } },
];
const receivedChart = computed<ChartOptions>(() => ({
  ...seriesBase.value,
  series: [
    {
      type: 'bar',
      name: 'Recibidas',
      barMaxWidth: 35,
      label: quantityLabel,
      labelLayout: { hideOverlap: true },
      data: props.report.months.map(row => ({ value: row.received, itemStyle: { color: row.complete ? teal : '#a36123' } })),
    },
  ],
  media: narrowChart,
}));
const variationChart = computed<ChartOptions | null>(() =>
  props.report.months.some(row => row.variationPercent != null)
    ? {
        ...seriesBase.value,
        tooltip: {
          trigger: 'axis',
          renderMode: 'richText',
          confine: true,
          valueFormatter: value => (typeof value === 'number' ? percentage(value) : 'Variación no calculable'),
        },
        yAxis: { type: 'value', axisLabel: { formatter: '{value} %' } },
        series: [{ type: 'line', name: 'Variación (%)', connectNulls: false, data: props.report.months.map(row => row.variationPercent) }],
      }
    : null,
);
const typeChart = computed(() =>
  horizontal(
    props.report.types.map(type => ({ name: label(type.code), count: type.count, color: colors[type.code], percent: type.percent })),
  ),
);
const stateChart = computed(() => horizontal(stateRows.value));
const officeChart = computed(() =>
  horizontal(
    props.report.currentOffices.map(office => ({ name: officeName(office), count: office.count, color: colors[office.category] })),
  ),
);
const monthlyTypeChart = computed<ChartOptions>(() => ({
  ...seriesBase.value,
  legend: { type: 'scroll', top: 4, data: props.report.types.map(type => label(type.code)) },
  series: [
    ...props.report.types.map(type => ({
      name: label(type.code),
      type: 'bar' as const,
      stack: 'tipologías',
      barMaxWidth: 40,
      itemStyle: { color: colors[type.code] ?? teal },
      data: props.report.months.map(month => month.types[type.code] ?? null),
    })),
    {
      name: 'Total recibido',
      type: 'line' as const,
      symbolSize: 0,
      lineStyle: { opacity: 0 },
      label: { ...quantityLabel, formatter: '{c}', color: '#163a43' },
      labelLayout: { hideOverlap: true },
      data: props.report.months.map(month => month.received),
    },
  ],
}));
</script>

<template>
  <article class="report" aria-labelledby="report-title">
    <header class="report-heading">
      <h2 id="report-title">Vista previa del informe</h2>
      <dl class="metadata">
        <div>
          <dt>Periodo consultado</dt>
          <dd>{{ civilDate(report.filter.start) }} a {{ civilDate(report.filter.end) }}</dd>
        </div>
        <div>
          <dt>Alcance</dt>
          <dd>{{ report.filter.scope === 'INSTITUTIONAL' ? 'Institucional' : selectedOffice }}</dd>
        </div>
        <div>
          <dt>Fecha y hora de consulta</dt>
          <dd>{{ observed(report.period.observedAt) }}</dd>
        </div>
        <div>
          <dt>Corte de la información</dt>
          <dd>{{ observed(report.period.effectiveCutoff) }} · Hora de Colombia</dd>
        </div>
      </dl>
      <p class="text-muted">El corte excluye las solicitudes registradas a partir de la fecha y hora indicadas.</p>
      <div class="coverage">
        <strong>{{ label(report.coverage.status) }}</strong>
        <ul class="mb-0">
          <li v-for="code in coverage" :key="code">{{ label(code) }}</li>
        </ul>
      </div>
      <nav class="section-index" aria-label="Secciones del informe">
        <a v-for="([id, title], position) in index" :key="id" :href="'#' + id" @click.prevent="goToSection(id)"
          >{{ position + 1 }}. {{ title }}</a
        >
      </nav>
    </header>

    <section aria-labelledby="introduction">
      <h3 id="introduction" tabindex="-1">1. Introducción y marco normativo</h3>
      <p>
        Este informe consolida las PQRSD recibidas en el periodo y alcance indicados. El marco normativo está pendiente de validación
        institucional.
      </p>
      <slot name="narrative" :section="1" />
    </section>

    <section aria-labelledby="distribution">
      <h3 id="distribution" tabindex="-1">2. Consolidado de PQRSD por tipología y periodo</h3>
      <p>
        Las tarjetas muestran el estado actual de las solicitudes recibidas durante el periodo. Cada solicitud pertenece a un solo estado y
        ya está incluida en el total.
      </p>
      <div class="cards">
        <div class="metric primary">
          <span>Solicitudes recibidas</span><strong>{{ quantity(report.received) }}</strong>
        </div>
        <div
          v-for="state in states"
          :key="state"
          class="metric"
          :data-state="state"
          :class="{ unavailable: report.currentStates[state] == null, zero: report.currentStates[state] === 0 }"
        >
          <span>{{ stateName(state) }}</span
          ><strong>{{ quantity(report.currentStates[state]) }}</strong
          ><small>Estado actual</small>
        </div>
      </div>
      <p v-if="report.received === 0" role="status" class="empty">No se registraron solicitudes para los filtros aplicados.</p>
      <p v-if="report.months.some(month => !month.complete)" class="text-muted">
        * Mes parcial. Su variación mensual se muestra como no comparable.
      </p>
      <div class="charts">
        <statistics-chart
          title="Evolución mensual de solicitudes recibidas"
          :options="receivedChart"
          :columns="['Mes', 'Cobertura', 'Recibidas']"
          :numeric="[2]"
          :total="['Total del periodo', '', quantity(report.received)]"
          :rows="report.months.map((month, i) => [months[i], month.complete ? 'Completo' : 'Parcial', quantity(month.received)])"
        />
        <statistics-chart
          title="Variación mensual"
          :options="variationChart"
          :columns="['Mes', 'Recibidas', 'Mes previo', 'Variación']"
          :numeric="[1, 2, 3]"
          :rows="report.months.map((month, i) => [months[i], quantity(month.received), quantity(month.previousCount), variation(month)])"
        />
        <statistics-chart
          title="Distribución por tipología"
          :options="typeChart"
          :columns="['Tipología', 'Solicitudes', 'Base', 'Porcentaje']"
          :rows="typeRows"
          :numeric="[1, 2, 3]"
          :height="chartHeight(typeRows.length)"
          :total="['Total del periodo', quantity(report.received), '', '']"
        />
        <statistics-chart
          title="Distribución por estado actual"
          :options="stateChart"
          :columns="['Estado actual', 'Solicitudes']"
          :rows="stateRows.map(row => [row.name, quantity(row.count)])"
          :numeric="[1]"
          :height="chartHeight(stateRows.length)"
          :total="['Total del periodo', quantity(report.received)]"
        />
        <statistics-chart
          class="wide"
          title="Distribución mensual por tipología"
          :options="monthlyTypeChart"
          :columns="['Mes', ...report.types.map(type => label(type.code)), 'Total recibido']"
          :numeric="report.types.map((_, i) => i + 1).concat(report.types.length + 1)"
          :total="['Total del periodo', ...report.types.map(type => quantity(type.count)), quantity(report.received)]"
          :rows="
            report.months.map((month, i) => [
              months[i],
              ...report.types.map(type => quantity(month.types[type.code])),
              quantity(month.received),
            ])
          "
        />
      </div>
      <slot name="narrative" :section="2" />
    </section>

    <template v-for="deferred in deferredSections" :key="deferred.id">
      <section v-if="deferred.id === 6" aria-labelledby="offices">
        <h3 id="offices" tabindex="-1">5. Distribución por dependencia y mes</h3>
        <p>La dependencia es la responsable actual. El mes corresponde a la recepción.</p>
        <statistics-chart
          title="Distribución por dependencia responsable actual"
          :options="officeChart"
          :columns="['Dependencia responsable actual', 'Solicitudes']"
          :rows="officeRows"
          :numeric="[1]"
          :height="chartHeight(officeRows.length)"
          :total="['Total del periodo', quantity(report.received)]"
        />
        <h4 class="mt-3">Cruce mensual por dependencia actual</h4>
        <statistics-table
          caption="Cruce mensual por dependencia actual"
          :columns="['Mes', 'Dependencia responsable actual', 'Solicitudes']"
          :rows="monthlyOfficeRows"
          :numeric="[2]"
          :total="['Total del periodo', '', quantity(report.received)]"
        />
        <slot name="narrative" :section="5" />
      </section>
      <section :aria-labelledby="'section-' + deferred.id">
        <h3 :id="'section-' + deferred.id" tabindex="-1">{{ deferred.id }}. {{ deferred.title }}</h3>
        <p>{{ deferred.note }}</p>
        <statistics-table
          :caption="deferred.title"
          :columns="['Indicador', 'Disponibilidad']"
          :rows="
            deferred.codes.map(code => [
              label(code),
              report.deferredIndicators[code] ? label(report.deferredIndicators[code]) : 'Información no disponible',
            ])
          "
        />
        <slot name="narrative" :section="deferred.id" />
      </section>
    </template>
    <section aria-labelledby="conclusions">
      <h3 id="conclusions" tabindex="-1">7. Conclusiones y recomendaciones</h3>
      <p class="mb-0">Contenido sujeto a revisión por Atención al Usuario.</p>
      <slot name="narrative" :section="7" />
    </section>
  </article>
</template>

<style scoped>
.report {
  display: grid;
  gap: 1.1rem;
}
.report-heading,
section {
  border: 1px solid #dbe6e8;
  border-radius: 0.8rem;
  background: #fff;
  padding: clamp(1rem, 2.5vw, 1.6rem);
}
h2,
h3,
h4 {
  color: #163a43;
  font-weight: 650;
}
h2 {
  font-size: 1.45rem;
}
h3 {
  font-size: 1.22rem;
}
h4 {
  font-size: 1rem;
}
.metadata,
.cards,
.charts {
  display: grid;
  gap: 0.85rem;
}
.metadata {
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 13rem), 1fr));
  margin: 0.9rem 0;
}
.metadata dt {
  color: #465a61;
  font-size: 0.85rem;
}
.metadata dd {
  margin-bottom: 0;
  overflow-wrap: anywhere;
}
.coverage,
.empty {
  background: #edf5f5;
  border-left: 4px solid #23786e;
  padding: 0.75rem 1rem;
}
.cards {
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 9rem), 1fr));
  margin: 1rem 0;
}
.metric {
  display: flex;
  flex-direction: column;
  padding: 0.8rem;
  border-radius: 0.55rem;
  background: #f0f5f6;
  color: #1f4149;
}
.metric span {
  font-size: 0.88rem;
}
.metric strong {
  font-size: clamp(1.65rem, 2.5vw, 2rem);
  font-variant-numeric: tabular-nums;
}
.metric small {
  color: #52686e;
}
.metric.zero strong {
  color: #52686e;
}
.metric.unavailable {
  border: 1px dashed #677c82;
}
.metric.unavailable strong {
  font-size: 0.9rem;
  padding-block: 0.35rem;
}
.section-index {
  display: flex;
  flex-wrap: wrap;
  gap: 0.2rem 0.75rem;
  margin-top: 0.75rem;
}
.section-index a {
  color: #165c62;
  padding-block: 0.35rem;
  text-decoration: underline;
}
h3[tabindex] {
  scroll-margin-top: 1rem;
}
.metric.primary {
  background: #175958;
  color: #fff;
}
.charts {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}
.wide {
  grid-column: 1 / -1;
}
@media (max-width: 850px) {
  .charts {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
