import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils';
import { createMemoryHistory, createRouter, RouterView, type Router } from 'vue-router';
import axios, { AxiosError, type AxiosResponse } from 'axios';
import { useStore } from '@/store';
import { Authority } from '@/shared/security/authority';
import ManagementReports from './management-reports.vue';
import { narrativeLimit, narrativeSections } from './report-narrative';
import { testConfiguration, testReport } from './statistics.fixture';

vi.mock('vue-echarts', () => ({ default: { name: 'TestChart', props: ['option'], template: '<div data-test="chart"></div>' } }));

const root = 'api/reports/gestion-pqrsd';
const configUrl = root + '/configuration';
const previewUrl = root + '/preview';
const exportUrl = root + '/export/xlsx';
const xlsxType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
const catalogUrl = 'api/oficinas/catalogo';
const reply = (data: unknown, headers = {}) => ({ data, headers }) as AxiosResponse;
const failure = (status: number, data = {}) => Object.assign(new AxiosError('synthetic error'), { response: { status, data } });

describe('Vista previa de gestión PQRSD', () => {
  const get = vi.spyOn(axios, 'get');
  let view: VueWrapper;
  let router: Router;

  async function open() {
    router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/informes-gestion-pqrsd', component: ManagementReports },
        { path: '/', component: { template: '<p>Inicio</p>' } },
      ],
    });
    await router.push('/informes-gestion-pqrsd');
    view = mount(RouterView, { attachTo: document.body, global: { plugins: [router] } });
    await flushPromises();
  }

  async function dates(start = testReport.filter.start, end = testReport.filter.end) {
    await view.get('#report-start').setValue(start);
    await view.get('#report-end').setValue(end);
  }

  const submit = () => view.get('form').trigger('submit');
  const previews = () => get.mock.calls.filter(([url]) => url === previewUrl);
  const button = (label: string) => view.findAll('button').find(control => control.text() === label)!;
  const introduction = () => view.get<HTMLTextAreaElement>('#narrative-introduction');
  const dialogIsOpen = () => view.get<HTMLDialogElement>('dialog').element.open;

  async function openPreview() {
    await open();
    await dates();
    await submit();
    await flushPromises();
  }

  beforeEach(() => {
    useStore().setAuthentication({ login: 'synthetic', authorities: [Authority.PQRSD_REPORTS] });
    get.mockReset();
    get.mockImplementation(url => Promise.resolve(reply(url === configUrl ? testConfiguration : testReport)));
  });

  afterEach(() => {
    view?.unmount();
    useStore().logout();
  });

  it('carga configuración y presenta seis gráficas, tablas, cobertura y los valores del motor', async () => {
    await open();
    expect(get.mock.calls.map(([url]) => url)).toEqual([configUrl]);
    expect(view.text()).toContain('Hasta 366 días');
    await dates();
    await submit();
    await flushPromises();
    expect(previews()).toHaveLength(1);
    expect(previews()[0][1]?.params).toStrictEqual({ start: '2024-01-01', end: '2024-03-20', scope: 'INSTITUTIONAL' });
    expect(view.findAll('[data-test="chart"]')).toHaveLength(6);
    expect(view.findAll('article > section')).toHaveLength(7);
    expect(view.get('.metric.primary').text()).toContain('13');
    expect(view.get('[data-state="REJECTED"] strong').text()).toBe('0');
    expect(view.get('[data-state="REJECTED"]').classes()).toContain('zero');
    expect(view.get('[data-state="UNKNOWN"] strong').text()).toBe('1');
    expect(view.findAll('nav[aria-label="Secciones del informe"] a')).toHaveLength(7);
    expect(view.findAll('tfoot').map(row => row.text())).toContain('Total del periodo13');
    expect(view.get('th.numeric').exists()).toBe(true);
    expect(view.text()).toContain('Enero 2024');
    expect(view.text()).toContain('Marzo 2024 *');
    expect(view.text()).toContain('Ver tabla de datos');
    const charts = view.findAllComponents({ name: 'TestChart' }).map(chart => chart.props('option'));
    expect(charts[0].series[0].label.show).toBe(true);
    expect(charts[1].series[0].data).toEqual([null, -50, null]);
    expect(charts[2].series[0].label.formatter({ dataIndex: 0 })).toBe('6 · 46,2 %');
    expect(charts[4].series.at(-1).data).toEqual([6, 3, 4]);
    expect(charts[5].yAxis.axisLabel.overflow).toBe('break');
    for (const text of [
      'Demanda',
      '46,2 %',
      'Sin dependencia asignada',
      'Referencia no verificable',
      'Mes parcial',
      'Variación no calculable · base cero',
      'Información parcialmente disponible',
      'la información puede variar',
      'Información no disponible',
      'Indicador pendiente de definición institucional',
    ]) {
      expect(view.text()).toContain(text);
    }
    expect(view.text()).not.toContain('Denuncia');
  });

  it('pagina el catálogo, exige una oficina válida y envía su ID sin cambiar el alcance', async () => {
    get.mockImplementation((url, options) => {
      if (url !== catalogUrl) return Promise.resolve(reply(url === configUrl ? testConfiguration : testReport));
      const page = options?.params.page;
      return Promise.resolve(reply([{ id: 'office-' + page, nombre: 'Oficina ' + page }], { 'x-total-count': '2' }));
    });
    await open();
    await dates();
    await view.get('#report-scope').setValue('OFFICE');
    await flushPromises();
    expect(get.mock.calls.filter(([url]) => url === catalogUrl).map(([, config]) => config?.params.page)).toEqual([0, 1]);
    await submit();
    expect(view.text()).toContain('Selecciona una dependencia del catálogo.');
    expect(previews()).toHaveLength(0);
    await view.get('#report-office').setValue('office-1');
    await submit();
    await flushPromises();
    expect(previews()[0][1]?.params).toEqual({ start: '2024-01-01', end: '2024-03-20', scope: 'OFFICE', officeId: 'office-1' });
    await view.get('#report-scope').setValue('INSTITUTIONAL');
    expect(view.find('article').exists()).toBe(false);
    await submit();
    expect(previews()[1][1]?.params).not.toHaveProperty('officeId');
  });

  it.each([
    ['', '2024-02-01', 'Selecciona dos fechas válidas.'],
    ['2024-03-01', '2024-02-01', 'La fecha inicial debe ser anterior'],
    ['2023-01-01', '2024-02-01', 'El periodo admite hasta'],
    ['2024-01-01', '9999-12-31', 'La fecha final no puede estar en el futuro.'],
  ])('rechaza fechas inválidas (%s, %s)', async (start, end, message) => {
    await open();
    await dates(start, end);
    await submit();
    expect(view.text()).toContain(message);
    expect(previews()).toHaveLength(0);
  });

  it('presenta un conjunto vacío y conserva valores nulos', async () => {
    get.mockResolvedValueOnce(reply(testConfiguration)).mockResolvedValueOnce(
      reply({
        ...testReport,
        received: 0,
        currentOffices: [],
        types: [{ code: 'REQUEST', count: 0, denominator: 0, percent: null }],
        currentStates: { RESOLVED: 0 },
      }),
    );
    await open();
    await dates();
    await submit();
    await flushPromises();
    expect(view.text()).toContain('No se registraron solicitudes');
    expect(view.text()).toContain('Información no disponible');
    expect(view.get('[data-state="PENDING"]').classes()).toContain('unavailable');
    expect(view.find('[data-state="UNKNOWN"]').exists()).toBe(false);
    expect(view.text()).toContain('No calculable');
  });

  it('cancela consultas antiguas, descarta su resultado y bloquea el envío duplicado', async () => {
    let oldResolve!: (response: AxiosResponse) => void;
    let newResolve!: (response: AxiosResponse) => void;
    get
      .mockResolvedValueOnce(reply(testConfiguration))
      .mockImplementationOnce(
        () =>
          new Promise(resolve => {
            oldResolve = resolve;
          }),
      )
      .mockImplementationOnce(
        () =>
          new Promise(resolve => {
            newResolve = resolve;
          }),
      );
    await open();
    await dates();
    await submit();
    await submit();
    expect(previews()).toHaveLength(1);
    expect(view.text()).toContain('Preparando vista previa');
    await dates('2024-02-01');
    expect(previews()[0][1]?.signal?.aborted).toBe(true);
    await submit();
    newResolve(reply({ ...testReport, received: 7, filter: { ...testReport.filter, start: '2024-02-01' } }));
    await flushPromises();
    oldResolve(reply(testReport));
    await flushPromises();
    expect(view.get('.metric.primary').text()).toContain('7');
    const clear = view.findAll('button').find(button => button.text() === 'Limpiar filtros')!;
    await clear.trigger('click');
    expect(view.find('article').exists()).toBe(false);
    expect((view.get('#report-start').element as HTMLInputElement).value).toBe('');
  });

  it.each([
    [400, 'Revisa los filtros solicitados.'],
    [401, 'La sesión finalizó.'],
    [403, 'Ya no tienes permiso'],
    [503, 'temporalmente indisponible'],
  ])('maneja el estado HTTP %s y sólo permite reintentar el fallo temporal', async (status, message) => {
    get.mockResolvedValueOnce(reply(testConfiguration)).mockRejectedValueOnce(failure(status));
    await open();
    await dates();
    await submit();
    await flushPromises();
    expect(view.text()).toContain(message);
    expect(view.find('article').exists()).toBe(false);
    expect(view.text().includes('Reintentar vista previa')).toBe(status === 503);
    if (status === 401 || status === 403) expect(view.find('form').exists()).toBe(false);
    if (status === 503) {
      await view
        .findAll('button')
        .find(button => button.text() === 'Reintentar vista previa')
        ?.trigger('click');
      await flushPromises();
      expect(view.find('article').exists()).toBe(true);
      expect(previews()).toHaveLength(2);
    }
  });

  it('permite enfocar las siete secciones desde el índice', async () => {
    await open();
    await dates();
    await submit();
    await flushPromises();
    for (const link of view.findAll('nav a')) {
      const heading = view.get(link.attributes('href')).element as HTMLElement;
      const focus = vi.spyOn(heading, 'focus');
      await link.trigger('click');
      expect(focus).toHaveBeenCalledWith({ preventScroll: true });
    }
  });

  it.each([
    ['error.invalidDate', 'ambas fechas'],
    ['error.invalidPeriod', 'La fecha inicial debe ser anterior'],
    ['error.futurePeriod', 'La fecha final no puede'],
    ['error.periodTooLong', 'supera el máximo'],
    ['error.invalidScope', 'Selecciona Institucional'],
    ['error.officeRequired', 'Selecciona una dependencia'],
    ['error.unknownOrAmbiguousOffice', 'no existe o su identificador es ambiguo'],
    ['__proto__', 'Revisa los filtros solicitados'],
  ])('traduce el problema HTTP 400 %s sin mostrar el detalle interno', async (code, message) => {
    get.mockResolvedValueOnce(reply(testConfiguration)).mockRejectedValueOnce(failure(400, { message: code, detail: 'detalle interno' }));
    await open();
    await dates();
    await submit();
    await flushPromises();
    expect(view.text()).toContain(message);
    expect(view.text()).not.toContain('detalle interno');
  });

  it('ofrece un reintento seguro tras un fallo de comunicación', async () => {
    get.mockResolvedValueOnce(reply(testConfiguration)).mockRejectedValueOnce(new AxiosError('detalle interno'));
    await open();
    await dates();
    await submit();
    await flushPromises();
    expect(view.text()).toContain('No fue posible comunicarse con el servicio');
    expect(view.text()).not.toContain('detalle interno');
    expect(view.text()).toContain('Reintentar vista previa');
  });

  it('suelta la carga cuando el interceptor consume el 401 y cierra la sesión', async () => {
    await open();
    await dates();
    await submit();
    await flushPromises();
    expect(view.find('article').exists()).toBe(true);
    get.mockImplementationOnce(async () => {
      useStore().logout();
      return undefined;
    });
    await submit();
    await flushPromises();
    expect(view.find('article').exists()).toBe(false);
    expect(view.find('form').exists()).toBe(false);
    expect(view.text()).not.toContain('Preparando vista previa');
  });

  it('interpreta una respuesta consumida como sesión expirada aunque el estado local siga activo', async () => {
    get.mockResolvedValueOnce(reply(testConfiguration)).mockResolvedValueOnce(undefined);
    await open();
    await dates();
    await submit();
    await flushPromises();
    expect(view.text()).toContain('La sesión finalizó.');
    expect(view.find('form').exists()).toBe(false);
    expect(view.text()).not.toContain('Preparando vista previa');
  });

  const downloadButton = () => view.findAll('button').find(button => button.text() === 'Descargar Excel')!;
  const blobResponse = () => reply(new Blob(['xlsx']), { 'content-type': xlsxType });

  it('habilita el XLSX tras la vista previa, evita duplicados y libera el Blob', async () => {
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    const url = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:synthetic');
    const revoke = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    try {
      await open();
      expect((downloadButton().element as HTMLButtonElement).disabled).toBe(true);
      await dates();
      await submit();
      await flushPromises();
      expect((downloadButton().element as HTMLButtonElement).disabled).toBe(false);
      await introduction().setValue('Texto narrativo excluido del XLSX');
      let done!: (res: AxiosResponse) => void;
      get.mockImplementationOnce(
        () =>
          new Promise(resolve => {
            done = resolve;
          }),
      );
      await downloadButton().trigger('click');
      expect((downloadButton().element as HTMLButtonElement).disabled).toBe(true);
      await downloadButton().trigger('click');
      expect(get.mock.calls.filter(([path]) => path === exportUrl)).toHaveLength(1);
      expect(get.mock.calls.at(-1)?.[1]).toEqual(
        expect.objectContaining({
          responseType: 'blob',
          params: { start: '2024-01-01', end: '2024-03-20', scope: 'INSTITUTIONAL' },
        }),
      );
      done(blobResponse());
      await flushPromises();
      await vi.waitFor(() => expect(revoke).toHaveBeenCalledWith('blob:synthetic'));
      expect(url).toHaveBeenCalledTimes(1);
      expect(click).toHaveBeenCalledTimes(1);
      expect(document.querySelector('a[download]')).toBeNull();
      expect(view.find('article').exists()).toBe(true);
      expect(introduction().element.value).toBe('Texto narrativo excluido del XLSX');
    } finally {
      click.mockRestore();
      url.mockRestore();
      revoke.mockRestore();
    }
  });

  it.each(['filtros', 'sesión'])('descarta una descarga obsoleta al cambiar %s', async change => {
    await open();
    await dates();
    await submit();
    await flushPromises();
    let done!: (res: AxiosResponse) => void;
    get.mockImplementationOnce(
      () =>
        new Promise(resolve => {
          done = resolve;
        }),
    );
    await downloadButton().trigger('click');
    const signal = get.mock.calls.at(-1)?.[1]?.signal;
    if (change === 'sesión') useStore().logout();
    else await dates('2024-02-01');
    expect(signal?.aborted).toBe(true);
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click');
    try {
      done(blobResponse());
      await flushPromises();
      expect(click).not.toHaveBeenCalled();
      expect(view.find('article').exists()).toBe(false);
      if (change === 'filtros') expect((downloadButton().element as HTMLButtonElement).disabled).toBe(true);
    } finally {
      click.mockRestore();
    }
  });

  it.each([
    [400, 'La dependencia no existe'],
    [401, 'La sesión finalizó'],
    [403, 'Ya no tienes permiso'],
    [503, 'temporalmente indisponible'],
  ])('maneja HTTP %s al descargar', async (status, message) => {
    await open();
    await dates();
    await submit();
    await flushPromises();
    await introduction().setValue('Texto narrativo sintético');
    const problem = new Blob([JSON.stringify({ message: 'error.unknownOrAmbiguousOffice', detail: 'dato interno' })], {
      type: 'application/problem+json',
    });
    get.mockRejectedValueOnce(failure(status, problem));
    await downloadButton().trigger('click');
    await flushPromises();
    expect(view.text()).toContain(message);
    expect(view.text()).not.toContain('dato interno');
    expect(view.find('article').exists()).toBe(status !== 401 && status !== 403);
    expect(view.find('#narrative-introduction').exists()).toBe(status !== 401 && status !== 403);
    expect(dialogIsOpen()).toBe(false);
  });

  it('exige la vista previa de oficina y rechaza una respuesta JSON con HTTP 200', async () => {
    get.mockImplementation(path =>
      Promise.resolve(
        path === configUrl
          ? reply(testConfiguration)
          : path === catalogUrl
            ? reply([{ id: 'synthetic-a', nombre: 'Atención al Usuario' }], { 'x-total-count': '1' })
            : reply(testReport),
      ),
    );
    await open();
    await dates();
    await view.get('#report-scope').setValue('OFFICE');
    await flushPromises();
    await view.get('#report-office').setValue('synthetic-a');
    await submit();
    await flushPromises();
    get.mockResolvedValueOnce(reply(new Blob(['{}']), { 'content-type': 'application/json' }));
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click');
    try {
      await downloadButton().trigger('click');
      await flushPromises();
      expect(get.mock.calls.at(-1)?.[1]?.params).toEqual({
        start: '2024-01-01',
        end: '2024-03-20',
        scope: 'OFFICE',
        officeId: 'synthetic-a',
      });
      expect(click).not.toHaveBeenCalled();
      expect(view.text()).toContain('No fue posible comunicarse con el servicio');
      expect(view.find('article').exists()).toBe(true);
    } finally {
      click.mockRestore();
    }
  });

  it.each(['logout', 'revocation'])('elimina resultados cuando cambia el acceso (%s)', async action => {
    await open();
    await dates();
    await submit();
    await flushPromises();
    expect(view.find('article').exists()).toBe(true);
    if (action === 'logout') useStore().logout();
    else useStore().account.authorities = [Authority.USER];
    await flushPromises();
    expect(view.find('article').exists()).toBe(false);
    expect(view.find('form').exists()).toBe(false);
    expect(view.text()).toContain('cuenta autorizada');
  });

  it('ofrece diez campos vacíos en las siete secciones sólo después de obtener estadísticas', async () => {
    await open();
    expect(view.find('textarea').exists()).toBe(false);
    await dates();
    await submit();
    await flushPromises();
    const fields = Object.values(narrativeSections).flat();
    expect(fields).toHaveLength(10);
    expect(view.findAll('article textarea')).toHaveLength(10);
    expect(view.findAll('article > section').every(section => section.find('textarea').exists())).toBe(true);
    for (const field of fields) {
      const input = view.get<HTMLTextAreaElement>('#narrative-' + field.id);
      expect(input.element.value).toBe('');
      expect(view.get('label[for="narrative-' + field.id + '"]').text()).toBe(field.label);
      await input.setValue('Texto de ' + field.label);
    }
    await button('Ver informe integrado').trigger('click');
    expect(view.find('article textarea').exists()).toBe(false);
    for (const field of fields) {
      expect(view.get('[data-narrative="' + field.id + '"]').text()).toBe('Texto de ' + field.label);
    }
    expect(view.get('.metric.primary strong').text()).toBe('13');
    expect(view.get('article').findAll('[contenteditable], input')).toHaveLength(0);
    expect(view.findAll('[data-test="chart"]')).toHaveLength(6);
    expect(testReport.received).toBe(13);
    expect(previews()).toHaveLength(1);
  });

  it('mantiene párrafos y muestra marcado ingresado como texto literal', async () => {
    await openPreview();
    const text = 'Primer párrafo.\n\nSegundo párrafo.\n<strong>texto</strong><script>example()</script>';
    await introduction().setValue(text);
    await button('Ver informe integrado').trigger('click');
    const paragraph = view.get('[data-narrative="introduction"]');
    expect(paragraph.element.textContent).toBe(text);
    expect(paragraph.find('strong, script').exists()).toBe(false);
    await button('Editar narrativa').trigger('click');
    expect(introduction().element.value).toBe(text);
  });

  it('aplica el máximo por campo y actualiza el contador, incluso con entrada superior al límite', async () => {
    await openPreview();
    expect(introduction().attributes('maxlength')).toBe(String(narrativeLimit));
    await introduction().setValue('a'.repeat(narrativeLimit + 1));
    expect(introduction().element.value).toHaveLength(narrativeLimit);
    expect(view.get('#narrative-count-introduction').text()).toBe(narrativeLimit + ' / ' + narrativeLimit + ' caracteres');
  });

  it.each(['fecha', 'alcance', 'limpieza', 'regeneración'])('protege el borrador ante %s', async action => {
    await openPreview();
    await introduction().setValue('Texto que puede descartarse');
    const originalDate = testReport.filter.start;
    const change = () => {
      if (action === 'fecha') return view.get('#report-start').setValue('2024-02-01');
      if (action === 'alcance') return view.get('#report-scope').setValue('OFFICE');
      if (action === 'limpieza') return button('Limpiar filtros').trigger('click');
      return submit();
    };
    if (action === 'alcance') {
      get.mockImplementation(path => Promise.resolve(reply(path === catalogUrl ? [] : testReport, { 'x-total-count': '0' })));
    }
    await change();
    expect(dialogIsOpen()).toBe(true);
    await button('Conservar texto').trigger('click');
    expect(dialogIsOpen()).toBe(false);
    expect(view.get<HTMLInputElement>('#report-start').element.value).toBe(originalDate);
    expect(view.get<HTMLSelectElement>('#report-scope').element.value).toBe('INSTITUTIONAL');
    expect(introduction().element.value).toBe('Texto que puede descartarse');
    expect(view.get('.metric.primary strong').text()).toBe('13');
    expect(previews()).toHaveLength(1);
    await change();
    await button('Descartar texto').trigger('click');
    await flushPromises();
    if (action === 'regeneración') {
      expect(previews()).toHaveLength(2);
      expect(introduction().element.value).toBe('');
    } else {
      expect(view.find('article').exists()).toBe(false);
      expect(view.find('#narrative-introduction').exists()).toBe(false);
      if (action === 'alcance') expect(view.get<HTMLSelectElement>('#report-scope').element.value).toBe('OFFICE');
      else expect(view.get<HTMLInputElement>('#report-start').element.value).toBe(action === 'fecha' ? '2024-02-01' : '');
    }
  });

  it('permite cancelar la salida por ruta y elimina la narrativa al volver tras confirmar', async () => {
    await openPreview();
    await introduction().setValue('Texto temporal');
    let navigation = router.push('/');
    await flushPromises();
    expect(dialogIsOpen()).toBe(true);
    await button('Conservar texto').trigger('click');
    await navigation;
    expect(router.currentRoute.value.path).toBe('/informes-gestion-pqrsd');
    expect(introduction().element.value).toBe('Texto temporal');
    navigation = router.push('/');
    await flushPromises();
    await button('Descartar texto').trigger('click');
    await navigation;
    expect(router.currentRoute.value.path).toBe('/');
    await router.push('/informes-gestion-pqrsd');
    await flushPromises();
    await dates();
    await submit();
    await flushPromises();
    expect(introduction().element.value).toBe('');
  });

  it.each(['logout', 'cambio de usuario', 'permisos'])('retira texto y diálogo de inmediato ante %s', async action => {
    await openPreview();
    await introduction().setValue('Texto de la sesión anterior');
    await submit();
    expect(dialogIsOpen()).toBe(true);
    if (action === 'logout') useStore().logout();
    else if (action === 'permisos') useStore().account.authorities = [Authority.USER];
    else useStore().setAuthentication({ login: 'other-synthetic', authorities: [Authority.ADMIN] });
    expect(dialogIsOpen()).toBe(false);
    await flushPromises();
    expect(view.find('article').exists()).toBe(false);
    expect(previews()).toHaveLength(1);
    if (action === 'cambio de usuario') {
      await dates();
      await submit();
      await flushPromises();
      expect(introduction().element.value).toBe('');
    }
  });

  it.each([401, 403])('cierra un descarte pendiente al recibir HTTP %s de una descarga en curso', async status => {
    await openPreview();
    await introduction().setValue('Texto de acceso restringido');
    let rejectDownload!: (error: AxiosError) => void;
    get.mockImplementationOnce(
      () =>
        new Promise((_, reject) => {
          rejectDownload = reject;
        }),
    );
    await downloadButton().trigger('click');
    await submit();
    expect(dialogIsOpen()).toBe(true);
    rejectDownload(failure(status));
    await flushPromises();
    expect(dialogIsOpen()).toBe(false);
    expect(view.find('article').exists()).toBe(false);
    expect(previews()).toHaveLength(1);
  });

  it('mantiene el texto fuera del almacenamiento y protege una recarga mientras hay cambios', async () => {
    await openPreview();
    const storage = vi.spyOn(Storage.prototype, 'setItem');
    try {
      await introduction().setValue('Texto que sólo vive en memoria');
      await button('Ver informe integrado').trigger('click');
      expect(storage).not.toHaveBeenCalled();
      expect(get.mock.calls.map(([url]) => url)).toEqual([configUrl, previewUrl]);
      const reload = new Event('beforeunload', { cancelable: true });
      expect(window.dispatchEvent(reload)).toBe(false);
      window.dispatchEvent(new Event('pagehide'));
      await flushPromises();
      expect(view.find('article').exists()).toBe(false);
      expect(window.dispatchEvent(new Event('beforeunload', { cancelable: true }))).toBe(true);
    } finally {
      storage.mockRestore();
    }
  });
});
