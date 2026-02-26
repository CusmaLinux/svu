/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount, flushPromises } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import Respuesta from './respuesta.vue';
import RespuestaService from './respuesta.service';
import AlertService from '@/shared/alert/alert.service';

type RespuestaComponentType = InstanceType<typeof Respuesta>;

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('Respuesta Management Component', () => {
    let respuestaServiceStub: SinonStubbedInstance<RespuestaService>;
    let mountOptions: MountingOptions<RespuestaComponentType>['global'];

    beforeEach(() => {
      respuestaServiceStub = sinon.createStubInstance<RespuestaService>(RespuestaService);
      respuestaServiceStub.search.resolves({
        headers: { 'x-total-count': '0' },
        data: [],
      });
      respuestaServiceStub.delete.resolves({});

      alertService = new AlertService({
        i18n: { t: vitest.fn() } as any,
        bvToast: { toast: vitest.fn() } as any,
      });

      mountOptions = {
        stubs: {
          jhiItemCount: true,
          bPagination: true,
          bModal: true,
          'font-awesome-icon': true,
          'b-badge': true,
          'jhi-sort-indicator': true,
          'b-button': true,
          RouterLink: true,
          'router-link': true,
        },
        directives: {
          'b-modal': {},
          can: {},
        },
        provide: {
          alertService,
          respuestaService: () => respuestaServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        respuestaServiceStub.search.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        const wrapper = shallowMount(Respuesta, { global: mountOptions });
        const comp = wrapper.vm;
        await flushPromises();

        // THEN
        expect(respuestaServiceStub.search.calledOnce).toBeTruthy();
        expect(comp.respuestas[0]).toEqual(expect.objectContaining({ id: 'ABC' }));
      });

      it('should calculate the sort attribute for an id', async () => {
        // WHEN
        const wrapper = shallowMount(Respuesta, { global: mountOptions });
        await flushPromises();

        // THEN
        expect(respuestaServiceStub.search.lastCall.firstArg).toMatchObject({
          sort: ['fechaRespuesta,desc', 'id'],
        });
      });
    });

    describe('Handles', () => {
      let wrapper: any;
      let comp: RespuestaComponentType;

      beforeEach(async () => {
        respuestaServiceStub.search.resolves({ headers: { 'x-total-count': '0' }, data: [] });
        wrapper = shallowMount(Respuesta, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();
        respuestaServiceStub.search.resetHistory();
      });

      it('should load a page', async () => {
        // GIVEN
        respuestaServiceStub.search.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        comp.page = 2;
        await flushPromises();

        // THEN
        expect(respuestaServiceStub.search.called).toBeTruthy();
        expect(comp.respuestas[0]).toEqual(expect.objectContaining({ id: 'ABC' }));
      });

      it('should not load a page if the page is the same as the previous page', async () => {
        // GIVEN
        comp.page = 1;

        // WHEN
        await flushPromises();

        // THEN
        expect(respuestaServiceStub.search.called).toBeFalsy();
      });

      it('should re-initialize the page', async () => {
        // GIVEN
        comp.page = 2;
        await flushPromises();
        respuestaServiceStub.search.resetHistory();
        respuestaServiceStub.search.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        comp.clear();
        await flushPromises();

        // THEN
        expect(comp.page).toEqual(1);
        expect(respuestaServiceStub.search.called).toBeTruthy();
      });

      it('should calculate the sort attribute for a non-id attribute', async () => {
        // WHEN
        comp.changeOrder('contenido');
        await flushPromises();

        // THEN
        expect(respuestaServiceStub.search.lastCall.firstArg).toMatchObject({
          sort: ['contenido,asc', 'id'],
        });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        respuestaServiceStub.delete.resolves({});
        respuestaServiceStub.search.resolves({
          headers: { 'x-total-count': '0' },
          data: [],
        });

        // FIX: Manually mock the modal ref.
        comp.removeEntity = { show: sinon.stub(), hide: sinon.stub() } as any;

        // WHEN
        comp.prepareRemove({ id: 'ABC' });
        comp.removeRespuesta();
        await flushPromises();

        // THEN
        expect(respuestaServiceStub.delete.calledWith('ABC')).toBeTruthy();
        expect(respuestaServiceStub.search.called).toBeTruthy();
      });
    });
  });
});
