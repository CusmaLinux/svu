<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import ReportNarrativeSection from './report-narrative-section.vue';
import StatisticsReport from './statistics-report.vue';
import { statisticsService, type SavedReport, type SavedReportSummary } from './statistics.service';
import type { NarrativeField, ReportNarrative } from './report-narrative';

const reports = ref<SavedReportSummary[]>([]);
const selected = ref<SavedReport>();
const narrative = reactive<ReportNarrative>({});
const reading = ref(true);
const loading = ref(false);
const saving = ref(false);
const downloading = ref(false);
const error = ref('');
const page = ref(0);
const total = ref(0);
const size = 10;
const filters = reactive({ start: '', end: '', scope: '' });
const sections = [1, 2, 3, 4, 5, 6, 7] as const;

async function reload() {
  loading.value = true;
  error.value = '';
  try {
    const result = await statisticsService.savedReports({
      page: page.value,
      size,
      ...(filters.scope ? { scope: filters.scope } : {}),
      ...(filters.start ? { start: filters.start } : {}),
      ...(filters.end ? { end: filters.end } : {}),
    });
    reports.value = result.reports;
    total.value = result.total;
    if (selected.value && !reports.value.some(report => report.id === selected.value?.id)) selected.value = undefined;
  } catch {
    error.value = 'No fue posible cargar el historial autorizado.';
  } finally {
    loading.value = false;
  }
}

async function open(report: SavedReportSummary) {
  loading.value = true;
  error.value = '';
  try {
    selected.value = await statisticsService.savedReport(report.id);
    Object.keys(narrative).forEach(key => delete narrative[key as NarrativeField]);
    Object.assign(narrative, selected.value.narrative);
    reading.value = true;
  } catch {
    error.value = 'No fue posible abrir este informe.';
  } finally {
    loading.value = false;
  }
}

function edit(field: NarrativeField, text: string) {
  narrative[field] = text;
}

async function saveNarrative() {
  if (!selected.value || saving.value) return;
  saving.value = true;
  error.value = '';
  try {
    selected.value = await statisticsService.updateSavedNarrative(selected.value.id, selected.value.version, { ...narrative });
    Object.assign(narrative, selected.value.narrative);
    await reload();
    reading.value = true;
  } catch (failure) {
    const status = (failure as { response?: { status?: number } }).response?.status;
    error.value =
      status === 409
        ? 'El informe cambió en otra sesión. Ábrelo de nuevo para recuperar la versión vigente antes de editar.'
        : 'No fue posible guardar la narrativa.';
  } finally {
    saving.value = false;
  }
}

async function download(format: 'docx' | 'xlsx') {
  if (!selected.value || downloading.value) return;
  downloading.value = true;
  error.value = '';
  try {
    const file =
      format === 'docx'
        ? await statisticsService.exportSavedWord(selected.value.id)
        : await statisticsService.exportSavedXlsx(selected.value.id);
    const url = URL.createObjectURL(file.blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = file.filename;
    link.click();
    URL.revokeObjectURL(url);
  } catch {
    error.value = `No fue posible generar el archivo ${format.toUpperCase()}.`;
  } finally {
    downloading.value = false;
  }
}

function applyFilters() {
  page.value = 0;
  void reload();
}

defineExpose({ reload });
onMounted(() => void reload());
</script>

<template>
  <section class="filter-card" aria-labelledby="saved-reports-title">
    <h2 id="saved-reports-title">Mis informes guardados</h2>
    <p class="text-muted">Las cifras corresponden a la instantánea guardada. Una nueva vista previa crea otro informe.</p>
    <form class="saved-filters" @submit.prevent="applyFilters">
      <label>Desde <input v-model="filters.start" class="form-control" type="date" /></label>
      <label>Hasta <input v-model="filters.end" class="form-control" type="date" /></label>
      <label
        >Alcance
        <select v-model="filters.scope" class="form-control">
          <option value="">Todos</option>
          <option value="INSTITUTIONAL">Institucional</option>
          <option value="OFFICE">Por dependencia</option>
        </select>
      </label>
      <button type="submit" class="btn btn-outline-secondary">Filtrar</button>
    </form>
    <p v-if="loading" role="status">Cargando informes…</p>
    <p v-if="error" class="alert alert-warning" role="alert">{{ error }}</p>
    <div v-if="reports.length" class="table-responsive" tabindex="0">
      <table class="table table-sm">
        <caption>
          Informes guardados por el usuario actual
        </caption>
        <thead>
          <tr>
            <th>Título</th>
            <th>Periodo</th>
            <th>Alcance</th>
            <th>Recibidas</th>
            <th>Corte estadístico</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="report in reports" :key="report.id">
            <td>{{ report.title }}</td>
            <td>{{ report.filter.start }} – {{ report.filter.end }}</td>
            <td>{{ report.filter.scope === 'INSTITUTIONAL' ? 'Institucional' : (report.office?.name ?? 'Dependencia') }}</td>
            <td>{{ report.received }}</td>
            <td>{{ report.cutoff }}</td>
            <td><button type="button" class="btn btn-sm btn-outline-primary" @click="open(report)">Abrir</button></td>
          </tr>
        </tbody>
      </table>
      <div class="saved-pagination">
        <button
          type="button"
          class="btn btn-sm btn-outline-secondary"
          :disabled="page === 0 || loading"
          @click="
            page--;
            reload();
          "
        >
          Anterior
        </button>
        <span>Página {{ page + 1 }} · {{ total }} informes</span>
        <button
          type="button"
          class="btn btn-sm btn-outline-secondary"
          :disabled="(page + 1) * size >= total || loading"
          @click="
            page++;
            reload();
          "
        >
          Siguiente
        </button>
      </div>
    </div>
    <p v-else-if="!loading && !error">No hay informes guardados para estos filtros.</p>
    <template v-if="selected">
      <hr />
      <h3>{{ selected.title }}</h3>
      <p>
        Fecha de corte: {{ selected.cutoff }} · Instantánea técnica {{ selected.modelVersion }}/{{ selected.methodologyVersion }}/{{
          selected.catalogVersion
        }}
      </p>
      <div class="d-flex flex-wrap" style="gap: 0.5rem">
        <button type="button" class="btn btn-outline-secondary" @click="reading = !reading">
          {{ reading ? 'Editar narrativa' : 'Ver informe integrado' }}
        </button>
        <button v-if="!reading" type="button" class="btn btn-primary" :disabled="saving" @click="saveNarrative">
          {{ saving ? 'Guardando…' : 'Guardar narrativa' }}
        </button>
        <button type="button" class="btn btn-outline-secondary" :disabled="downloading" @click="download('docx')">Descargar Word</button>
        <button type="button" class="btn btn-outline-secondary" :disabled="downloading" @click="download('xlsx')">Descargar Excel</button>
      </div>
      <statistics-report class="mt-3" :report="selected.statistics">
        <template #narrative="{ section }">
          <report-narrative-section :section="section" :fields="narrative" :reading="reading" @edit="edit" />
        </template>
      </statistics-report>
    </template>
  </section>
</template>

<style scoped>
.saved-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 0.75rem;
  margin-bottom: 1rem;
}
.saved-filters label {
  min-width: 10rem;
}
.saved-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}
</style>
