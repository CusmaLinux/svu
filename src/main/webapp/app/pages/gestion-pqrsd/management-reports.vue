<script setup lang="ts">
import StatisticsReport from './statistics-report.vue';
import { useStatisticsPreview } from './use-statistics-preview';

const {
  form,
  configuration,
  offices,
  configurationState,
  configurationError,
  catalogState,
  catalogError,
  catalogLoaded,
  preview,
  previewState,
  previewError,
  downloading,
  downloadError,
  downloadExcel,
  validationError,
  accessError,
  clearFilters,
  generate,
  loadConfiguration,
  loadOffices,
} = useStatisticsPreview();
</script>

<template>
  <main class="management-reports">
    <header class="page-heading">
      <h1>Informes de Gestión PQRSD</h1>
      <p>Consulta la recepción y distribución de solicitudes de UNIPUTUMAYO por periodo y dependencia.</p>
    </header>

    <p v-if="accessError" class="alert alert-warning" role="alert">{{ accessError }}</p>
    <p v-else-if="configurationState === 'loading'" role="status">Cargando configuración del informe…</p>
    <div v-else-if="configurationState === 'error'" class="alert alert-warning" role="alert">
      <p>{{ configurationError }}</p>
      <button type="button" class="btn btn-outline-secondary" @click="loadConfiguration">Reintentar configuración</button>
    </div>

    <template v-else-if="configuration">
      <form class="filter-card" aria-labelledby="filters-title" novalidate @submit.prevent="generate">
        <h2 id="filters-title">Filtros del informe</h2>
        <p id="period-help" class="text-muted">
          Hasta {{ configuration.maxPeriodDays }} días, incluidas ambas fechas · {{ configuration.zone }}
        </p>
        <div class="filter-grid">
          <div>
            <label for="report-start">Fecha inicial</label
            ><input
              id="report-start"
              v-model="form.start"
              type="date"
              class="form-control"
              required
              min="0001-01-01"
              max="9999-12-31"
              aria-describedby="period-help"
            />
          </div>
          <div>
            <label for="report-end">Fecha final</label
            ><input
              id="report-end"
              v-model="form.end"
              type="date"
              class="form-control"
              required
              min="0001-01-01"
              max="9999-12-31"
              aria-describedby="period-help"
            />
          </div>
          <div>
            <label for="report-scope">Alcance</label>
            <select id="report-scope" v-model="form.scope" class="form-control" required>
              <option value="" disabled>Selecciona un alcance</option>
              <option v-for="scope in configuration.scopes" :key="scope" :value="scope">
                {{ scope === 'INSTITUTIONAL' ? 'Institucional' : 'Por dependencia' }}
              </option>
            </select>
          </div>
          <div v-if="form.scope === 'OFFICE'">
            <label for="report-office">Dependencia</label>
            <select id="report-office" v-model="form.officeId" class="form-control" required :disabled="!catalogLoaded || !offices.length">
              <option value="" disabled>Selecciona una dependencia</option>
              <option v-for="office in offices" :key="office.id" :value="office.id">{{ office.nombre }}</option>
            </select>
          </div>
        </div>
        <div v-if="form.scope === 'OFFICE'">
          <p v-if="catalogState === 'loading'" class="mb-0 mt-3" role="status">Cargando catálogo de dependencias…</p>
          <div v-else-if="catalogState === 'error'" class="mt-3" role="alert">
            <p>{{ catalogError }}</p>
            <button type="button" class="btn btn-outline-secondary" @click="loadOffices">Reintentar catálogo</button>
          </div>
          <p v-else-if="catalogLoaded && !offices.length" class="mb-0 mt-3">El catálogo no contiene dependencias.</p>
        </div>

        <div class="actions">
          <button
            type="submit"
            class="btn report-submit"
            :disabled="previewState === 'loading' || (form.scope === 'OFFICE' && !catalogLoaded)"
          >
            Generar vista previa
          </button>
          <button type="button" class="btn btn-outline-secondary" @click="clearFilters">Limpiar filtros</button>
        </div>
        <p v-if="validationError" class="text-danger mb-0 mt-3" role="alert">{{ validationError }}</p>
      </form>

      <p v-if="previewState === 'loading'" class="mt-3" role="status">Preparando vista previa…</p>
      <div v-else-if="previewError" class="alert alert-warning mt-3" role="alert">
        <p class="mb-0">{{ previewError.message }}</p>
        <button v-if="previewError.retryable" type="button" class="btn btn-outline-secondary mt-2" @click="generate">
          Reintentar vista previa
        </button>
      </div>
      <div class="mt-3 d-flex flex-wrap align-items-baseline" style="gap: 0.75rem">
        <button type="button" class="btn btn-outline-secondary" :disabled="!preview || downloading" @click="downloadExcel">
          Descargar Excel
        </button>
        <small class="text-muted">El Excel actualiza las cifras con los filtros de la vista previa. Los datos pueden haber cambiado.</small>
      </div>
      <p v-if="downloading" class="mt-2" role="status">Generando el archivo Excel…</p>
      <p v-if="downloadError" class="alert alert-warning mt-2" role="alert">{{ downloadError }}</p>
      <statistics-report v-if="preview" class="mt-4" :report="preview" />
    </template>
  </main>
</template>

<style scoped>
.management-reports {
  max-width: 1320px;
  margin-inline: auto;
  padding: clamp(0.75rem, 2.2vw, 1.7rem);
  color: #223841;
}
.page-heading {
  border-bottom: 3px solid #24867c;
  padding: 1.7rem clamp(1rem, 3vw, 2rem);
  margin-bottom: 1.1rem;
  background: #173742;
  color: #fff;
  border-radius: 0.85rem;
}
h1 {
  font-size: clamp(1.6rem, 4vw, 2.25rem);
  font-weight: 680;
}
.page-heading p {
  color: #d4e7e8;
  margin-bottom: 0;
}
.filter-card {
  padding: clamp(1rem, 2.5vw, 1.6rem);
  border: 1px solid #dbe6e8;
  border-radius: 0.8rem;
  background: #f8fbfb;
}
h2 {
  font-size: 1.25rem;
  font-weight: 650;
}
.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 12rem), 1fr));
  gap: 1rem;
}
.filter-grid > div {
  min-width: 0;
}
label {
  font-weight: 600;
}
.actions {
  display: flex;
  gap: 0.6rem;
  flex-wrap: wrap;
  margin-top: 1.15rem;
}
.report-submit {
  background: #17655e;
  border: 1px solid #17655e;
  color: #fff;
}
.report-submit:hover:not(:disabled) {
  background: #0c504a;
  color: #fff;
}
</style>
