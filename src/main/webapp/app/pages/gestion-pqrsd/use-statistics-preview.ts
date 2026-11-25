import { computed, onScopeDispose, reactive, ref, shallowRef, watch } from 'vue';
import { isAxiosError } from 'axios';
import { useStore } from '@/store';
import { hasStatisticsAccess } from '@/shared/security/statistics-access';
import type { PqrsStatistics, PreviewFilter, Scope, StatisticsConfiguration } from './statistics.model';
import { type OfficeOption, statisticsService } from './statistics.service';

type RequestState = 'idle' | 'loading' | 'error';
type RequestError = { message: string; retryable: boolean };

const filterErrors: Record<string, string> = {
  'error.unknownOrAmbiguousOffice': 'La dependencia no existe o su identificador es ambiguo. Selecciona otra dependencia.',
  'error.invalidDate': 'Revisa que ambas fechas sean válidas.',
  'error.invalidPeriod': 'Revisa el periodo. La fecha inicial debe ser anterior o igual a la fecha final.',
  'error.invalidScope': 'Selecciona Institucional o Por dependencia.',
  'error.officeRequired': 'Selecciona una dependencia del catálogo.',
  'error.invalidOffice': 'La dependencia seleccionada tiene un identificador inválido.',
  'error.unexpectedOffice': 'El alcance institucional abarca todas las dependencias.',
  'error.repeatedParameter': 'Hay filtros repetidos. Limpia el formulario e inténtalo de nuevo.',
  'error.futurePeriod': 'La fecha final no puede estar en el futuro.',
  'error.periodTooLong': 'El periodo supera el máximo permitido. Revisa las fechas.',
};

function httpError(error: unknown): RequestError {
  if (isAxiosError(error)) {
    switch (error.response?.status) {
      case 400: {
        const code = error.response.data?.message;
        const message =
          typeof code === 'string' && Object.prototype.hasOwnProperty.call(filterErrors, code)
            ? filterErrors[code]
            : 'Revisa los filtros solicitados.';
        return { message, retryable: false };
      }
      case 401:
        return { message: 'La sesión finalizó. Ingresa de nuevo para consultar el informe.', retryable: false };
      case 403:
        return { message: 'Ya no tienes permiso para consultar informes de gestión PQRSD.', retryable: false };
      case 503:
        return { message: 'El servicio de informes está temporalmente indisponible.', retryable: true };
    }
  }
  return { message: 'No fue posible comunicarse con el servicio de informes. Inténtalo de nuevo.', retryable: true };
}

function civilDay(value: string) {
  if (!/^(?!0000)\d{4}-\d{2}-\d{2}$/.test(value)) return NaN;
  // UTC is only used as an ordinal for calendar arithmetic. The API receives the original civil dates.
  const day = Date.parse(value + 'T00:00:00Z');
  return Number.isFinite(day) && new Date(day).toISOString().slice(0, 10) === value ? day : NaN;
}

function todayIn(zone: string) {
  const parts = new Intl.DateTimeFormat('en', { timeZone: zone, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts();
  return ['year', 'month', 'day'].map(type => parts.find(part => part.type === type)?.value).join('-');
}

export function useStatisticsPreview() {
  const store = useStore();
  const allowed = computed(() => hasStatisticsAccess(store.authenticated, store.account?.authorities));
  const accessError = ref('');
  const form = reactive({ start: '', end: '', scope: '' as Scope | '', officeId: '' });
  const configuration = shallowRef<StatisticsConfiguration>();
  const offices = shallowRef<OfficeOption[]>([]);
  const configurationState = ref<RequestState>('idle');
  const catalogState = ref<RequestState>('idle');
  const configurationError = ref('');
  const catalogError = ref('');
  const catalogLoaded = ref(false);
  const preview = shallowRef<PqrsStatistics>();
  const previewState = ref<RequestState>('idle');
  const previewError = shallowRef<RequestError>();
  const validationError = ref('');
  const downloading = ref(false);
  const downloadError = ref('');
  let appliedFilter: PreviewFilter | undefined;
  let downloadRequest: AbortController | undefined;
  let configurationRequest: AbortController | undefined;
  let catalogRequest: AbortController | undefined;
  let previewRequest: AbortController | undefined;

  function clearPreview() {
    downloadRequest?.abort();
    downloadRequest = undefined;
    downloading.value = false;
    downloadError.value = '';
    appliedFilter = undefined;
    previewRequest?.abort();
    previewRequest = undefined;
    preview.value = undefined;
    previewState.value = 'idle';
    previewError.value = undefined;
    validationError.value = '';
  }

  function clearAccess() {
    configurationRequest?.abort();
    configurationRequest = undefined;
    catalogRequest?.abort();
    catalogRequest = undefined;
    configuration.value = undefined;
    configurationState.value = 'idle';
    configurationError.value = '';
    offices.value = [];
    catalogLoaded.value = false;
    catalogState.value = 'idle';
    catalogError.value = '';
    clearPreview();
  }

  function handleAccessError(error: unknown) {
    const status = isAxiosError(error) ? error.response?.status : undefined;
    if (status !== 401 && status !== 403) return false;
    clearAccess();
    accessError.value = httpError(error).message;
    return true;
  }

  async function loadConfiguration() {
    if (!allowed.value || accessError.value || configurationRequest) return;
    const request = new AbortController();
    configurationRequest = request;
    configurationState.value = 'loading';
    try {
      const data = await statisticsService.configuration(request.signal);
      if (configurationRequest !== request || !allowed.value) return;
      configuration.value = data;
      form.scope = data.scopes[0] ?? '';
      configurationState.value = 'idle';
    } catch (error) {
      if (configurationRequest !== request || handleAccessError(error)) return;
      configurationState.value = 'error';
      configurationError.value = httpError(error).message;
    } finally {
      if (configurationRequest === request) configurationRequest = undefined;
    }
  }

  async function loadOffices() {
    if (!allowed.value || accessError.value || catalogRequest || catalogLoaded.value) return;
    const request = new AbortController();
    catalogRequest = request;
    catalogState.value = 'loading';
    try {
      const data = await statisticsService.offices(request.signal);
      if (catalogRequest !== request || !allowed.value) return;
      offices.value = data;
      catalogLoaded.value = true;
      catalogState.value = 'idle';
    } catch (error) {
      if (catalogRequest !== request || handleAccessError(error)) return;
      catalogState.value = 'error';
      catalogError.value = httpError(error).message;
    } finally {
      if (catalogRequest === request) catalogRequest = undefined;
    }
  }

  function clearFilters() {
    Object.assign(form, { start: '', end: '', scope: configuration.value?.scopes[0] ?? '', officeId: '' });
    clearPreview();
  }

  function validate(config: StatisticsConfiguration): string {
    const start = civilDay(form.start);
    const end = civilDay(form.end);
    if (!Number.isFinite(start) || !Number.isFinite(end)) return 'Selecciona dos fechas válidas.';
    if (start > end) return 'La fecha inicial debe ser anterior o igual a la fecha final.';
    if (form.end > todayIn(config.zone)) return 'La fecha final no puede estar en el futuro.';
    if ((end - start) / 86_400_000 + 1 > config.maxPeriodDays) return `El periodo admite hasta ${config.maxPeriodDays} días.`;
    if (!form.scope || !config.scopes.includes(form.scope)) return 'Selecciona un alcance disponible.';
    if (
      form.scope === 'OFFICE' &&
      (!form.officeId || form.officeId.length > config.maxOfficeIdLength || !offices.value.some(office => office.id === form.officeId))
    ) {
      return 'Selecciona una dependencia del catálogo.';
    }
    return '';
  }

  async function generate() {
    const config = configuration.value;
    if (!config || !allowed.value || accessError.value || previewRequest) return;
    clearPreview();
    validationError.value = validate(config);
    if (validationError.value) return;
    const { start, end, scope, officeId } = form;
    const filter: PreviewFilter = scope === 'OFFICE' ? { start, end, scope, officeId } : { start, end, scope: 'INSTITUTIONAL' };
    const request = new AbortController();
    previewRequest = request;
    previewState.value = 'loading';
    try {
      const data = await statisticsService.preview(filter, request.signal);
      if (previewRequest !== request || !allowed.value) return;
      appliedFilter = filter;
      preview.value = data;
      previewState.value = 'idle';
    } catch (error) {
      if (previewRequest !== request || handleAccessError(error)) return;
      previewState.value = 'error';
      previewError.value = httpError(error);
    } finally {
      if (previewRequest === request) previewRequest = undefined;
    }
  }

  async function downloadExcel() {
    if (!appliedFilter || !preview.value || !allowed.value || accessError.value || downloadRequest) return;
    const request = new AbortController();
    downloadRequest = request;
    downloading.value = true;
    downloadError.value = '';
    try {
      const { blob, filename } = await statisticsService.exportXlsx(appliedFilter, request.signal);
      if (downloadRequest !== request || !allowed.value) return;
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      try {
        anchor.href = url;
        anchor.download = filename;
        document.body.append(anchor);
        anchor.click();
      } finally {
        anchor.remove();
        // Give the browser a turn to consume the navigation before releasing the Blob.
        setTimeout(() => URL.revokeObjectURL(url), 0);
      }
    } catch (error) {
      if (downloadRequest !== request || handleAccessError(error)) return;
      if (isAxiosError(error) && error.response?.status === 400 && error.response.data instanceof Blob) {
        try {
          error.response.data = JSON.parse(await error.response.data.text());
        } catch {
          // Fall back to the generic filter error for malformed problem details.
        }
      }
      if (downloadRequest === request) downloadError.value = httpError(error).message;
    } finally {
      if (downloadRequest === request) {
        downloadRequest = undefined;
        downloading.value = false;
      }
    }
  }

  watch(form, clearPreview, { flush: 'sync' });
  watch(
    () => form.scope,
    scope => {
      if (scope === 'OFFICE') void loadOffices();
      else form.officeId = '';
    },
    { flush: 'sync' },
  );
  watch(
    [allowed, () => store.account?.login],
    ([canAccess]) => {
      clearAccess();
      accessError.value = canAccess ? '' : 'Ingresa con una cuenta autorizada para consultar informes de gestión PQRSD.';
      clearFilters();
      if (canAccess) void loadConfiguration();
    },
    { immediate: true, flush: 'sync' },
  );
  onScopeDispose(clearAccess);

  return {
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
  };
}
