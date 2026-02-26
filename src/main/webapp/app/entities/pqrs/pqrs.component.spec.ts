/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount, flushPromises } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import Pqrs from './pqrs.vue';
import PqrsService from './pqrs.service';
import AlertService from '@/shared/alert/alert.service';

type PqrsComponentType = InstanceType<typeof Pqrs>;

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('Pqrs Management Component', () => {
    let pqrsServiceStub: SinonStubbedInstance<PqrsService>;
    let mountOptions: MountingOptions<PqrsComponentType>['global'];

    beforeEach(() => {
      pqrsServiceStub = sinon.createStubInstance<PqrsService>(PqrsService);
      pqrsServiceStub.search.resolves({
        headers: { 'x-total-count': '0' },
        data: [],
      });
      pqrsServiceStub.delete.resolves({});

      alertService = new AlertService({
        i18n: { t: vitest.fn() } as any,
        bvToast: { toast: vitest.fn() } as any,
      });

      mountOptions = {
        stubs: {
          jhiItemCount: true,
          bPagination: true,
          bModal: true, // Use standard stub (we will mock the ref manually)
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
          'b-tooltip': {},
        },
        provide: {
          alertService,
          pqrsService: () => pqrsServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        pqrsServiceStub.search.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        const wrapper = shallowMount(Pqrs, { global: mountOptions });
        await flushPromises();

        // THEN
        expect(pqrsServiceStub.search.calledOnce).toBeTruthy();
        expect(wrapper.vm.pqrs[0]).toEqual(expect.objectContaining({ id: 'ABC' }));
      });

      it('should calculate the sort attribute for an id', async () => {
        // WHEN
        const wrapper = shallowMount(Pqrs, { global: mountOptions });
        await flushPromises();

        // THEN
        expect(pqrsServiceStub.search.lastCall.firstArg).toMatchObject({
          sort: ['fecha_creacion,desc', 'id'],
        });
      });
    });

    describe('Handles', () => {
      let wrapper: any;
      let comp: PqrsComponentType;

      beforeEach(async () => {
        pqrsServiceStub.search.resolves({ headers: { 'x-total-count': '0' }, data: [] });
        wrapper = shallowMount(Pqrs, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();
        pqrsServiceStub.search.resetHistory();
      });

      it('should load a page', async () => {
        // GIVEN
        pqrsServiceStub.search.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        comp.page = 2;
        await flushPromises();

        // THEN
        expect(pqrsServiceStub.search.called).toBeTruthy();
        expect(comp.pqrs[0]).toEqual(expect.objectContaining({ id: 'ABC' }));
      });

      it('should not load a page if the page is the same as the previous page', async () => {
        // GIVEN
        comp.page = 1;

        // WHEN
        await flushPromises();

        // THEN
        expect(pqrsServiceStub.search.called).toBeFalsy();
      });

      it('should re-initialize the page', async () => {
        // GIVEN
        comp.page = 2;
        await flushPromises();
        pqrsServiceStub.search.resetHistory();
        pqrsServiceStub.search.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        comp.clear();
        await flushPromises();

        // THEN
        expect(comp.page).toEqual(1);
        expect(pqrsServiceStub.search.called).toBeTruthy();
      });

      it('should calculate the sort attribute for a non-id attribute', async () => {
        // WHEN
        // FIX 1: Call changeOrder instead of setting propOrder directly.
        // Direct assignment doesn't trigger the search because there is no watcher on propOrder.
        comp.changeOrder('name');
        await flushPromises();

        // THEN
        expect(pqrsServiceStub.search.lastCall.firstArg).toMatchObject({
          sort: ['name,asc', 'id'],
        });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        pqrsServiceStub.delete.resolves({});
        pqrsServiceStub.search.resolves({
          headers: { 'x-total-count': '0' },
          data: [],
        });

        // FIX 2: Manually mock the modal ref.
        // In shallowMount, template refs to stubs are often null.
        comp.removeEntity = { show: sinon.stub(), hide: sinon.stub() };

        // WHEN
        comp.prepareRemove({ id: 'ABC' }); // This calls .show() on our mock
        comp.removePqrs(); // This calls .hide() on our mock
        await flushPromises();

        // THEN
        expect(pqrsServiceStub.delete.calledWith('ABC')).toBeTruthy();
        expect(pqrsServiceStub.search.called).toBeTruthy();
      });
    });
  });
});
