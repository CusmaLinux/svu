/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount, flushPromises } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import ArchivoAdjunto from './archivo-adjunto.vue';
import ArchivoAdjuntoService from './archivo-adjunto.service';
import AlertService from '@/shared/alert/alert.service';

type ArchivoAdjuntoComponentType = InstanceType<typeof ArchivoAdjunto>;

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('ArchivoAdjunto Management Component', () => {
    let archivoAdjuntoServiceStub: SinonStubbedInstance<ArchivoAdjuntoService>;
    let mountOptions: MountingOptions<ArchivoAdjuntoComponentType>['global'];

    beforeEach(() => {
      archivoAdjuntoServiceStub = sinon.createStubInstance<ArchivoAdjuntoService>(ArchivoAdjuntoService);
      archivoAdjuntoServiceStub.search.resolves({
        headers: { 'x-total-count': '0' },
        data: [],
      });
      archivoAdjuntoServiceStub.delete.resolves({});

      alertService = new AlertService({
        i18n: { t: vitest.fn() } as any,
        bvToast: {
          toast: vitest.fn(),
        } as any,
      });

      mountOptions = {
        stubs: {
          jhiItemCount: true,
          bPagination: true,
          bModal: true,
          'font-awesome-icon': true,
          'b-badge': true,
          'b-button': true,
          RouterLink: true,
          'router-link': true,
        },
        directives: {
          'b-modal': {},
        },
        provide: {
          alertService,
          archivoAdjuntoService: () => archivoAdjuntoServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        archivoAdjuntoServiceStub.search.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        const wrapper = shallowMount(ArchivoAdjunto, { global: mountOptions });
        const comp = wrapper.vm;
        await flushPromises();

        // THEN
        expect(archivoAdjuntoServiceStub.search.calledOnce).toBeTruthy();
        expect(comp.archivoAdjuntos[0]).toEqual(expect.objectContaining({ id: 'ABC' }));
      });
    });
    describe('Handles', () => {
      let wrapper: any;
      let comp: ArchivoAdjuntoComponentType;

      beforeEach(async () => {
        archivoAdjuntoServiceStub.search.resolves({ headers: { 'x-total-count': '0' }, data: [] });
        wrapper = shallowMount(ArchivoAdjunto, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();
        archivoAdjuntoServiceStub.search.resetHistory();
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        archivoAdjuntoServiceStub.delete.resolves({});
        archivoAdjuntoServiceStub.search.resolves({
          headers: { 'x-total-count': '0' },
          data: [],
        });

        // FIX: Manually mock the modal ref.
        comp.removeEntity = { show: sinon.stub(), hide: sinon.stub() } as any;

        // WHEN
        comp.prepareRemove({ id: 'ABC' });
        comp.removeArchivoAdjunto();
        await flushPromises();

        // THEN
        expect(archivoAdjuntoServiceStub.delete.calledWith('ABC')).toBeTruthy();

        // THEN
        await flushPromises();
        expect(archivoAdjuntoServiceStub.search.called).toBeTruthy();
      });
    });
  });
});
