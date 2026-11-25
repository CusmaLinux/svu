<script setup lang="ts">
import { use } from 'echarts/core';
import { BarChart, LineChart } from 'echarts/charts';
import { DataZoomComponent, GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import VChart from 'vue-echarts';
import StatisticsTable from './statistics-table.vue';
import type { ChartOptions } from './statistics.model';

use([BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent, DataZoomComponent, CanvasRenderer]);

defineProps<{ title: string; options: ChartOptions | null; columns: string[]; rows: string[][] }>();
</script>

<template>
  <div class="chart-panel">
    <h3>{{ title }}</h3>
    <v-chart v-if="options" class="chart" :option="options" autoresize aria-hidden="true" />
    <p v-else class="text-muted">No hay valores para graficar.</p>
    <details>
      <summary>Datos de la gráfica</summary>
      <statistics-table :caption="title" :columns="columns" :rows="rows" />
    </details>
  </div>
</template>

<style scoped>
.chart-panel {
  min-width: 0;
  border: 1px solid #dbe6e8;
  padding: 1rem;
  border-radius: 0.65rem;
  background: #fff;
}
h3 {
  font-size: 1rem;
  font-weight: 650;
  margin-bottom: 0.5rem;
}
.chart {
  height: 285px;
}
summary {
  color: #165c62;
  font-weight: 600;
  padding: 0.5rem 0;
}
</style>
