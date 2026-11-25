import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils';
import axios, { AxiosError, type AxiosResponse } from 'axios';
import { useStore } from '@/store';
import { Authority } from '@/shared/security/authority';
import ManagementReports from './management-reports.vue';
import { testConfiguration, testReport } from './statistics.fixture';

vi.mock('vue-echarts', () => ({ default: { props: ['option'], template: '<div data-test="chart"></div>' } }));

const root = 'api/reports/gestion-pqrsd';
const configUrl = root + '/configuration';
const previewUrl = root + '/preview';
const catalogUrl = 'api/oficinas/catalogo';
const reply = (data: unknown, headers = {}) => ({ data, headers }) as AxiosResponse;
const failure = (status: number, data = {}) => Object.assign(new AxiosError('synthetic error'), { response: { status, data } });

describe('Vista previa de gestión PQRSD', () => {
  const get = vi.spyOn(axios, 'get');
  let view: VueWrapper;

  async function open() {
    view = mount(ManagementReports);
    await flushPromises();
  }

  async function dates(start = testReport.filter.start, end = testReport.filter.end) {
    await view.get('#report-start').setValue(start);
    await view.get('#report-end').setValue(end);
  }

  const submit = () => view.get('form').trigger('submit');
  const previews = () => get.mock.calls.filter(([url]) => url === previewUrl);

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
    expect(view.findAll('.metric').at(-1)?.text()).toBe('Rechazadas actualmente0');
    for (const text of [
      'Demanda',
      '46,2 %',
      'Sin asignación',
      'Referencia no verificable',
      'Mes parcial',
      'No calculable · base cero',
      'Cobertura parcial',
      'instantánea transaccional',
      'Fuente no disponible',
      'Definición institucional pendiente',
      'Fuente parcial',
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
    expect(view.text()).toContain('Dato no disponible');
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
});
