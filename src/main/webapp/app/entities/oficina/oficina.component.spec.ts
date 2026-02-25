/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount, flushPromises } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import Oficina from './oficina.vue';
import OficinaService from './oficina.service';
import AlertService from '@/shared/alert/alert.service';

type OficinaComponentType = InstanceType<typeof Oficina>;

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('Oficina Management Component', () => {
    let oficinaServiceStub: SinonStubbedInstance<OficinaService>;
    let mountOptions: MountingOptions<OficinaComponentType>['global'];

    beforeEach(() => {
      oficinaServiceStub = sinon.createStubInstance<OficinaService>(OficinaService);
      oficinaServiceStub.search.resolves({
        headers: { 'x-total-count': '0' },
        data: [],
      });
      oficinaServiceStub.delete.resolves({});

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
          oficinaService: () => oficinaServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        oficinaServiceStub.search.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        const wrapper = shallowMount(Oficina, { global: mountOptions });
        const comp = wrapper.vm;
        await flushPromises();

        // THEN
        expect(oficinaServiceStub.search.calledOnce).toBeTruthy();
        expect(comp.oficinas[0]).toEqual(expect.objectContaining({ id: 'ABC' }));
      });
    });
    describe('Handles', () => {
      let wrapper: any;
      let comp: OficinaComponentType;

      beforeEach(async () => {
        oficinaServiceStub.search.resolves({ headers: { 'x-total-count': '0' }, data: [] });
        wrapper = shallowMount(Oficina, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();
        oficinaServiceStub.search.resetHistory();
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        oficinaServiceStub.delete.resolves({});
        oficinaServiceStub.search.resolves({
          headers: { 'x-total-count': '0' },
          data: [],
        });

        // FIX: Manually mock the modal ref.
        comp.removeEntity = { show: sinon.stub(), hide: sinon.stub() } as any;

        // WHEN
        comp.prepareRemove({ id: 'ABC' });
        comp.removeOficina();
        await flushPromises();

        // THEN
        expect(oficinaServiceStub.delete.calledWith('ABC')).toBeTruthy();

        // THEN
        await flushPromises();
        expect(oficinaServiceStub.search.called).toBeTruthy();
      });
    });
  });
});
