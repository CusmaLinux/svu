<script setup lang="ts">
import { computed } from 'vue';
import StatisticsChart from './statistics-chart.vue';
import StatisticsTable from './statistics-table.vue';
import { label, percentage, quantity } from './statistics-labels';
import type { ChartOptions, MonthlyStatistics, OfficeCount, PqrsStatistics } from './statistics.model';

const props = defineProps<{ report: PqrsStatistics }>();
const palette = ['#16746d', '#355a9c', '#a36123', '#6456a0', '#b24464', '#457180', '#916725', '#476333'];
const states = ['RESOLVED', 'PENDING', 'IN_PROGRESS', 'CLOSED', 'REJECTED'];
const coverage = computed(() => [...new Set([...props.report.coverage.warnings, ...props.report.coverage.limitations])]);
const months = computed(() => props.report.months.map(row => row.month + (row.complete ? '' : ' *')));
const stateRows = computed(() => Object.entries(props.report.currentStates).map(([code, count]) => [label(code), count] as const));
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
  { id: 3, title: 'Caracterización de usuarios y canales', codes: ['IND-18', 'IND-17', 'IND-19'] },
  { id: 4, title: 'Direccionamiento y traslados', codes: ['IND-16'] },
  {
    id: 6,
    title: 'Oportunidad de respuesta y satisfacción',
    codes: ['IND-08', 'IND-09', 'IND-10', 'IND-11', 'IND-12', 'IND-13', 'IND-14', 'IND-15'],
  },
];

function observed(value: string) {
  return new Intl.DateTimeFormat('es-CO', { timeZone: props.report.period.zone, dateStyle: 'medium', timeStyle: 'medium' }).format(
    new Date(value),
  );
}

function variation(month: MonthlyStatistics) {
  if (month.variationPercent != null) return percentage(month.variationPercent);
  if (month.comparison === 'PARTIAL_MONTH') return 'No comparable · mes parcial';
  if (month.comparison === 'NO_COMPARABLE_BASE') return 'Sin base comparable';
  return month.comparison === 'NO_CALCULABLE' ? 'No calculable · base cero' : 'Dato no disponible';
}

function horizontal(rows: readonly (readonly [string, number])[]): ChartOptions | null {
  if (!rows.length) return null;
  return {
    color: palette,
    grid: { left: 4, right: rows.length > 8 ? 36 : 16, top: 10, bottom: 26, containLabel: true },
    tooltip: { trigger: 'axis', renderMode: 'richText', axisPointer: { type: 'shadow' } },
    xAxis: { type: 'value', minInterval: 1, name: 'Solicitudes', nameLocation: 'middle', nameGap: 23 },
    yAxis: { type: 'category', inverse: true, data: rows.map(([name]) => name), axisLabel: { width: 140, overflow: 'truncate' } },
    dataZoom: rows.length > 8 ? [{ type: 'slider', yAxisIndex: 0, startValue: 0, endValue: 7, showDetail: false }] : [],
    series: [{ type: 'bar', name: 'Solicitudes', data: rows.map(([, count]) => count), barMaxWidth: 24 }],
  };
}

const seriesBase = computed<ChartOptions>(() => ({
  color: palette,
  grid: { top: 55, left: 8, right: 16, bottom: 12, containLabel: true },
  tooltip: { trigger: 'axis', renderMode: 'richText', confine: true },
  xAxis: { type: 'category', data: months.value, axisLabel: { rotate: 35 } },
  yAxis: { type: 'value', minInterval: 1 },
  legend: { type: 'scroll', top: 4 },
}));

const receivedChart = computed<ChartOptions>(() => ({
  ...seriesBase.value,
  series: [
    {
      type: 'bar',
      name: 'Recibidas',
      barMaxWidth: 35,
      data: props.report.months.map(row => ({ value: row.received, itemStyle: { color: row.complete ? palette[0] : '#a36123' } })),
    },
  ],
}));
const variationChart = computed<ChartOptions | null>(() =>
  props.report.months.some(row => row.variationPercent != null)
    ? {
        ...seriesBase.value,
        yAxis: { type: 'value', axisLabel: { formatter: '{value} %' } },
        series: [{ type: 'line', name: 'Variación (%)', connectNulls: false, data: props.report.months.map(row => row.variationPercent) }],
      }
    : null,
);
const typeChart = computed(() => horizontal(props.report.types.map(type => [label(type.code), type.count])));
const stateChart = computed(() => horizontal(stateRows.value));
const officeChart = computed(() => horizontal(props.report.currentOffices.map(office => [officeName(office), office.count])));
const monthlyTypeChart = computed<ChartOptions>(() => ({
  ...seriesBase.value,
  series: props.report.types.map(type => ({
    name: label(type.code),
    type: 'bar',
    stack: 'tipologías',
    barMaxWidth: 40,
    data: props.report.months.map(month => month.types[type.code] ?? null),
  })),
}));
</script>

<template>
  <article class="report" aria-labelledby="report-title">
    <header class="report-heading">
      <h2 id="report-title">Vista previa del informe</h2>
      <dl class="metadata">
        <div>
          <dt>Periodo solicitado</dt>
          <dd>{{ report.filter.start }} a {{ report.filter.end }}</dd>
        </div>
        <div>
          <dt>Alcance</dt>
          <dd>{{ report.filter.scope === 'INSTITUTIONAL' ? 'Institucional' : selectedOffice }}</dd>
        </div>
        <div>
          <dt>Observado</dt>
          <dd>{{ observed(report.period.observedAt) }}</dd>
        </div>
        <div>
          <dt>Corte efectivo (exclusivo)</dt>
          <dd>{{ observed(report.period.effectiveCutoff) }} · {{ report.period.zone }}</dd>
        </div>
      </dl>
      <div class="coverage">
        <strong>{{ label(report.coverage.status) }}</strong>
        <ul class="mb-0">
          <li v-for="code in coverage" :key="code">{{ label(code) }}</li>
        </ul>
      </div>
    </header>

    <section aria-labelledby="introduction">
      <h3 id="introduction">1. Introducción y marco normativo</h3>
      <p>
        Este informe consolida las PQRSD recibidas en el periodo y alcance indicados. El marco normativo está pendiente de validación
        institucional.
      </p>
    </section>

    <section aria-labelledby="distribution">
      <h3 id="distribution">2. Consolidado de PQRSD por tipología y periodo</h3>
      <p>Los estados actuales se observan sobre las solicitudes recibidas durante el periodo seleccionado.</p>
      <div class="cards">
        <div class="metric primary">
          <span>PQRSD recibidas</span><strong>{{ quantity(report.received) }}</strong>
        </div>
        <div v-for="state in states" :key="state" class="metric">
          <span>{{ label(state) }} actualmente</span><strong>{{ quantity(report.currentStates[state]) }}</strong>
        </div>
      </div>
      <p v-if="report.received === 0" role="status" class="empty">No se registraron solicitudes para los filtros aplicados.</p>
      <p v-if="report.months.some(month => !month.complete)" class="text-muted">
        * Mes parcial. Su variación mensual se muestra como no comparable.
      </p>
      <div class="charts">
        <statistics-chart
          title="Evolución mensual de recibidas"
          :options="receivedChart"
          :columns="['Mes', 'Cobertura', 'Recibidas']"
          :rows="report.months.map((month, i) => [months[i], month.complete ? 'Completo' : 'Parcial', quantity(month.received)])"
        />
        <statistics-chart
          title="Variación mensual"
          :options="variationChart"
          :columns="['Mes', 'Recibidas', 'Mes previo', 'Variación']"
          :rows="report.months.map((month, i) => [months[i], quantity(month.received), quantity(month.previousCount), variation(month)])"
        />
        <statistics-chart
          title="Distribución por tipología"
          :options="typeChart"
          :columns="['Tipología', 'Solicitudes', 'Base', 'Porcentaje']"
          :rows="typeRows"
        />
        <statistics-chart
          title="Distribución por estado actual"
          :options="stateChart"
          :columns="['Estado actual', 'Solicitudes']"
          :rows="stateRows.map(([name, count]) => [name, quantity(count)])"
        />
        <statistics-chart
          class="wide"
          title="Distribución mensual por tipología"
          :options="monthlyTypeChart"
          :columns="['Mes', ...report.types.map(type => label(type.code))]"
          :rows="report.months.map((month, i) => [months[i], ...report.types.map(type => quantity(month.types[type.code]))])"
        />
      </div>
    </section>

    <template v-for="deferred in deferredSections" :key="deferred.id">
      <section v-if="deferred.id === 6" aria-labelledby="offices">
        <h3 id="offices">5. Distribución por dependencia y mes</h3>
        <p>La dependencia es la responsable actual. El mes corresponde a la recepción.</p>
        <statistics-chart
          title="Distribución por dependencia actual"
          :options="officeChart"
          :columns="['Dependencia actual', 'Solicitudes']"
          :rows="officeRows"
        />
        <h4 class="mt-3">Cruce mensual por dependencia actual</h4>
        <statistics-table
          caption="Cruce mensual por dependencia actual"
          :columns="['Mes', 'Dependencia actual', 'Solicitudes']"
          :rows="monthlyOfficeRows"
        />
      </section>
      <section :aria-labelledby="'section-' + deferred.id">
        <h3 :id="'section-' + deferred.id">{{ deferred.id }}. {{ deferred.title }}</h3>
        <statistics-table
          :caption="deferred.title"
          :columns="['Indicador', 'Disponibilidad']"
          :rows="
            deferred.codes.map(code => [
              label(code),
              report.deferredIndicators[code] ? label(report.deferredIndicators[code]) : 'Dato no disponible',
            ])
          "
        />
      </section>
    </template>
    <section aria-labelledby="conclusions">
      <h3 id="conclusions">7. Conclusiones y recomendaciones</h3>
      <p class="mb-0">Pendientes de elaboración y revisión por Atención al Usuario.</p>
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
  font-size: 1.45rem;
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
