/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount, flushPromises } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import InformePqrs from './informe-pqrs.vue';
import InformePqrsService from './informe-pqrs.service';
import AlertService from '@/shared/alert/alert.service';

type InformePqrsComponentType = InstanceType<typeof InformePqrs>;

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('InformePqrs Management Component', () => {
    let informePqrsServiceStub: SinonStubbedInstance<InformePqrsService>;
    let mountOptions: MountingOptions<InformePqrsComponentType>['global'];

    beforeEach(() => {
      informePqrsServiceStub = sinon.createStubInstance<InformePqrsService>(InformePqrsService);
      informePqrsServiceStub.retrieve.resolves({
        headers: { 'x-total-count': '0' },
        data: [],
      });
      informePqrsServiceStub.delete.resolves({});

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
        },
        provide: {
          alertService,
          informePqrsService: () => informePqrsServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        informePqrsServiceStub.retrieve.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        const wrapper = shallowMount(InformePqrs, { global: mountOptions });
        const comp = wrapper.vm;
        await flushPromises();

        // THEN
        expect(informePqrsServiceStub.retrieve.calledOnce).toBeTruthy();
        expect(comp.informePqrs[0]).toEqual(expect.objectContaining({ id: 'ABC' }));
      });

      it('should calculate the sort attribute for an id', async () => {
        // WHEN
        const wrapper = shallowMount(InformePqrs, { global: mountOptions });
        await flushPromises();

        // THEN
        expect(informePqrsServiceStub.retrieve.lastCall.firstArg).toMatchObject({
          sort: ['id,asc'],
        });
      });
    });

    describe('Handles', () => {
      let wrapper: any;
      let comp: InformePqrsComponentType;

      beforeEach(async () => {
        informePqrsServiceStub.retrieve.resolves({ headers: { 'x-total-count': '0' }, data: [] });
        wrapper = shallowMount(InformePqrs, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();
        informePqrsServiceStub.retrieve.resetHistory();
      });

      it('should load a page', async () => {
        // GIVEN
        informePqrsServiceStub.retrieve.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        comp.page = 2;
        await flushPromises();

        // THEN
        expect(informePqrsServiceStub.retrieve.called).toBeTruthy();
        expect(comp.informePqrs[0]).toEqual(expect.objectContaining({ id: 'ABC' }));
      });

      it('should not load a page if the page is the same as the previous page', async () => {
        // GIVEN
        comp.page = 1;

        // WHEN
        await flushPromises();

        // THEN
        expect(informePqrsServiceStub.retrieve.called).toBeFalsy();
      });

      it('should re-initialize the page', async () => {
        // GIVEN
        comp.page = 2;
        await flushPromises();
        informePqrsServiceStub.retrieve.resetHistory();
        informePqrsServiceStub.retrieve.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        comp.clear();
        await flushPromises();

        // THEN
        expect(comp.page).toEqual(1);
        expect(informePqrsServiceStub.retrieve.called).toBeTruthy();
      });

      it('should calculate the sort attribute for a non-id attribute', async () => {
        // WHEN
        comp.changeOrder('fechaInicio');
        await flushPromises();

        // THEN
        expect(informePqrsServiceStub.retrieve.lastCall.firstArg).toMatchObject({
          sort: ['fechaInicio,asc', 'id'],
        });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        informePqrsServiceStub.delete.resolves({});
        informePqrsServiceStub.retrieve.resolves({
          headers: { 'x-total-count': '0' },
          data: [],
        });

        // FIX: Manually mock the modal ref.
        comp.removeEntity = { show: sinon.stub(), hide: sinon.stub() } as any;

        // WHEN
        comp.prepareRemove({ id: 'ABC' });
        comp.removeInformePqrs();
        await flushPromises();

        // THEN
        expect(informePqrsServiceStub.delete.calledWith('ABC')).toBeTruthy();
        expect(informePqrsServiceStub.retrieve.called).toBeTruthy();
      });
    });
  });
});
