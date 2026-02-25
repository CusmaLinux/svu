/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount, flushPromises } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import Notification from './notification.vue';
import NotificationService from './notification.service';
import AlertService from '@/shared/alert/alert.service';

type NotificationComponentType = InstanceType<typeof Notification>;

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('Notification Management Component', () => {
    let notificationServiceStub: SinonStubbedInstance<NotificationService>;
    let mountOptions: MountingOptions<NotificationComponentType>['global'];

    beforeEach(() => {
      notificationServiceStub = sinon.createStubInstance<NotificationService>(NotificationService);
      notificationServiceStub.retrieve.resolves({
        headers: { 'x-total-count': '0' },
        data: [],
      });

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
          'b-tooltip': {},
        },
        provide: {
          alertService,
          notificationService: () => notificationServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        notificationServiceStub.retrieve.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        const wrapper = shallowMount(Notification, { global: mountOptions });
        const comp = wrapper.vm;
        await flushPromises();

        // THEN
        expect(notificationServiceStub.retrieve.calledOnce).toBeTruthy();
        expect(comp.notifications[0]).toEqual(expect.objectContaining({ id: 'ABC' }));
      });

      it('should calculate the sort attribute for an id', async () => {
        // WHEN
        const wrapper = shallowMount(Notification, { global: mountOptions });
        await flushPromises();

        // THEN
        expect(notificationServiceStub.retrieve.lastCall.firstArg).toMatchObject({
          sort: ['fecha,desc', 'id'],
        });
      });
    });

    describe('Handles', () => {
      let wrapper: any;
      let comp: NotificationComponentType;

      beforeEach(async () => {
        notificationServiceStub.retrieve.resolves({ headers: { 'x-total-count': '0' }, data: [] });
        wrapper = shallowMount(Notification, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();
        notificationServiceStub.retrieve.resetHistory();
      });

      it('should load a page', async () => {
        // GIVEN
        notificationServiceStub.retrieve.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        comp.page = 2;
        await flushPromises();

        // THEN
        expect(notificationServiceStub.retrieve.called).toBeTruthy();
        expect(comp.notifications[0]).toEqual(expect.objectContaining({ id: 'ABC' }));
      });

      it('should not load a page if the page is the same as the previous page', async () => {
        // GIVEN
        comp.page = 1;

        // WHEN
        await flushPromises();

        // THEN
        expect(notificationServiceStub.retrieve.called).toBeFalsy();
      });

      it('should re-initialize the page', async () => {
        // GIVEN
        comp.page = 2;
        await flushPromises();
        notificationServiceStub.retrieve.resetHistory();
        notificationServiceStub.retrieve.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ id: 'ABC' }],
        });

        // WHEN
        comp.handleSyncList();
        await flushPromises();

        // THEN
        expect(comp.page).toEqual(1);
        expect(notificationServiceStub.retrieve.called).toBeTruthy();
      });

      it('should calculate the sort attribute for a non-id attribute', async () => {
        // WHEN
        comp.changeOrder('tipo');
        await flushPromises();

        // THEN
        expect(notificationServiceStub.retrieve.lastCall.firstArg).toMatchObject({
          sort: ['tipo,asc', 'id'],
        });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        notificationServiceStub.delete.resolves({});
        notificationServiceStub.retrieve.resolves({
          headers: { 'x-total-count': '0' },
          data: [],
        });

        // FIX: Manually mock the modal ref.
        comp.removeNotificationModalRef = { show: sinon.stub(), hide: sinon.stub() } as any;

        // WHEN
        comp.prepareRemove({ id: 'ABC' });
        comp.deleteNotifications();
        await flushPromises();

        // THEN
        expect(notificationServiceStub.delete.calledWith('ABC')).toBeTruthy();
        expect(notificationServiceStub.retrieve.called).toBeTruthy();
      });
    });
  });
});
