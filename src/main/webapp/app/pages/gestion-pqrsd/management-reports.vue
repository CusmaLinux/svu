<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import ReportNarrativeSection from './report-narrative-section.vue';
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
  narrative,
  narrativeDirty,
  editNarrative,
  previewState,
  previewError,
  downloading,
  downloadError,
  downloadExcel,
  downloadWord,
  authenticatedPreview,
  replacement,
  reviewRequired,
  reviewing,
  differences,
  loadReplacement,
  associate,
  validationError,
  accessError,
  clearFilters,
  clearPreview,
  generate,
  loadConfiguration,
  loadOffices,
} = useStatisticsPreview();

const reading = ref(false);
const discardDialog = ref<HTMLDialogElement>();
let discardDecision: { accept: () => void; cancel: () => void } | undefined;

function confirmDiscard(): boolean | Promise<boolean> {
  if (!narrativeDirty.value) return true;
  if (discardDecision || !discardDialog.value) return false;
  discardDialog.value.showModal();
  return new Promise(resolve => {
    discardDecision = { accept: () => resolve(true), cancel: () => resolve(false) };
  });
}

function finishDiscard(confirmed: boolean) {
  const decision = discardDecision;
  discardDecision = undefined;
  if (discardDialog.value?.open) discardDialog.value.close();
  if (confirmed) decision?.accept();
  else decision?.cancel();
}

function afterDiscard(action: () => void) {
  const draftId = narrative.value?.previewId;
  const decision = confirmDiscard();
  if (decision === true) action();
  else if (decision !== false) {
    void decision.then(confirmed => {
      if (confirmed && narrative.value?.previewId === draftId) action();
    });
  }
}

function editFilter<Key extends keyof typeof form>(key: Key, event: Event) {
  const input = event.target as HTMLInputElement | HTMLSelectElement;
  const value = input.value as (typeof form)[Key];
  if (value === form[key]) return;
  if (!narrativeDirty.value) {
    form[key] = value;
    return;
  }
  // A native control has already changed when it emits the event.
  input.value = form[key];
  afterDiscard(() => {
    form[key] = value;
  });
}

function beforeUnload(event: BeforeUnloadEvent) {
  if (!narrativeDirty.value) return;
  event.preventDefault();
  event.returnValue = true;
}

watch(
  narrativeDirty,
  (dirty, _, cleanup) => {
    if (!dirty) return;
    window.addEventListener('beforeunload', beforeUnload);
    cleanup(() => window.removeEventListener('beforeunload', beforeUnload));
  },
  { flush: 'sync' },
);
watch(
  narrative,
  () => {
    reading.value = false;
    // Access loss makes any pending discard decision irrelevant.
    finishDiscard(true);
  },
  { flush: 'sync' },
);
onBeforeRouteLeave(() => confirmDiscard());
onMounted(() => window.addEventListener('pagehide', clearPreview));
onBeforeUnmount(() => {
  window.removeEventListener('pagehide', clearPreview);
  finishDiscard(false);
});
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
      <form class="filter-card" aria-labelledby="filters-title" novalidate @submit.prevent="afterDiscard(generate)">
        <h2 id="filters-title">Filtros del informe</h2>
        <p id="period-help" class="text-muted">
          Hasta {{ configuration.maxPeriodDays }} días, incluidas ambas fechas · {{ configuration.zone }}
        </p>
        <div class="filter-grid">
          <div>
            <label for="report-start">Fecha inicial</label
            ><input
              id="report-start"
              :value="form.start"
              @input="editFilter('start', $event)"
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
              :value="form.end"
              @input="editFilter('end', $event)"
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
            <select id="report-scope" :value="form.scope" class="form-control" required @change="editFilter('scope', $event)">
              <option value="" disabled>Selecciona un alcance</option>
              <option v-for="scope in configuration.scopes" :key="scope" :value="scope">
                {{ scope === 'INSTITUTIONAL' ? 'Institucional' : 'Por dependencia' }}
              </option>
            </select>
          </div>
          <div v-if="form.scope === 'OFFICE'">
            <label for="report-office">Dependencia</label>
            <select
              id="report-office"
              :value="form.officeId"
              class="form-control"
              required
              :disabled="!catalogLoaded || !offices.length"
              @change="editFilter('officeId', $event)"
            >
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
          <button type="button" class="btn btn-outline-secondary" @click="afterDiscard(clearFilters)">Limpiar filtros</button>
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
        <button
          type="button"
          class="btn report-submit"
          :disabled="!authenticatedPreview || !!downloading || reviewRequired"
          @click="downloadWord"
        >
          Descargar Word
        </button>
        <small class="text-muted">
          El Excel contiene estadísticas y se genera con los filtros de la vista previa. Los datos pueden haber cambiado.
        </small>
      </div>
      <p v-if="downloading" class="mt-2" role="status">Generando el archivo {{ downloading === 'docx' ? 'Word' : 'Excel' }}…</p>
      <p v-if="preview && !authenticatedPreview" class="text-muted mt-2">La descarga Word requiere una vista previa con comprobante.</p>
      <p v-if="downloadError" class="alert alert-warning mt-2" role="alert">{{ downloadError }}</p>
      <section v-if="reviewRequired && authenticatedPreview" class="filter-card mt-3" aria-labelledby="comparison-title">
        <h2 id="comparison-title">Revisar una nueva vista previa</h2>
        <p>Tu texto y la vista original se conservan hasta que confirmes la asociación.</p>
        <button type="button" class="btn btn-outline-secondary" :disabled="reviewing" @click="loadReplacement">
          {{ reviewing ? 'Consultando cifras…' : 'Obtener nueva vista previa' }}
        </button>
        <div v-if="replacement" class="mt-3">
          <p><strong>Corte original</strong> · {{ authenticatedPreview.statistics.period.effectiveCutoff }}</p>
          <p><strong>Corte nuevo</strong> · {{ replacement.statistics.period.effectiveCutoff }}</p>
          <div v-if="differences.length" class="comparison-table table-responsive" tabindex="0">
            <table class="table table-sm">
              <caption>
                Cambios entre vistas previas
              </caption>
              <thead>
                <tr>
                  <th scope="col">Dato</th>
                  <th scope="col">Original</th>
                  <th scope="col">Nuevo</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="difference in differences" :key="difference.key">
                  <th scope="row">{{ difference.title }}</th>
                  <td>{{ difference.before }}</td>
                  <td>{{ difference.after }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else>Las cifras comparadas coinciden. Puedes asociar tu texto a la nueva vista.</p>
          <button type="button" class="btn report-submit" @click="associate">Asociar mi texto a esta vista previa</button>
        </div>
      </section>
      <template v-if="preview && narrative">
        <div class="filter-card mt-4">
          <h2>Informe preliminar</h2>
          <p>
            Las cifras se calculan automáticamente. El texto narrativo se conserva en esta vista hasta que la abandones. Evita incluir datos
            personales o información confidencial.
          </p>
          <button type="button" class="btn btn-outline-secondary" @click="reading = !reading">
            {{ reading ? 'Editar narrativa' : 'Ver informe integrado' }}
          </button>
        </div>
        <statistics-report :key="narrative.previewId" class="mt-4" :report="preview">
          <template #narrative="{ section }">
            <report-narrative-section :section="section" :fields="narrative.fields" :reading="reading" @edit="editNarrative" />
          </template>
        </statistics-report>
      </template>
    </template>
    <dialog
      ref="discardDialog"
      class="discard-dialog"
      aria-labelledby="discard-title"
      aria-describedby="discard-description"
      @cancel.prevent="finishDiscard(false)"
    >
      <h2 id="discard-title">¿Descartar el texto narrativo?</h2>
      <p id="discard-description">Si continúas, se perderá el texto asociado a esta vista previa.</p>
      <div class="actions">
        <button type="button" class="btn btn-outline-secondary" autofocus @click="finishDiscard(false)">Conservar texto</button>
        <button type="button" class="btn btn-danger" @click="finishDiscard(true)">Descartar texto</button>
      </div>
    </dialog>
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
.comparison-table {
  max-height: 24rem;
  overflow: auto;
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
.discard-dialog {
  max-width: min(30rem, calc(100% - 2rem));
  padding: 1.4rem;
  border: 1px solid #dbe6e8;
  border-radius: 0.8rem;
  color: #223841;
}
.discard-dialog::backdrop {
  background: rgb(23 55 66 / 55%);
}
</style>
