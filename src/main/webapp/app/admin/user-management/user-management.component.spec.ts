/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount, flushPromises } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import UserManagement from './user-management.vue';
import UserManagementService from './user-management.service';
import UserService from '@/entities/user/user.service';
import AlertService from '@/shared/alert/alert.service';

type UserManagementComponentType = InstanceType<typeof UserManagement>;

describe('Component Tests', () => {
  let alertService: AlertService;

  describe('User Management Component', () => {
    let userManagementServiceStub: SinonStubbedInstance<UserManagementService>;
    let userServiceStub: SinonStubbedInstance<UserService>;
    let mountOptions: MountingOptions<UserManagementComponentType>['global'];

    beforeEach(() => {
      userManagementServiceStub = sinon.createStubInstance<UserManagementService>(UserManagementService);
      userServiceStub = sinon.createStubInstance<UserService>(UserService);
      userServiceStub.search.resolves({
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
          userManagementService: userManagementServiceStub,
          userService: userServiceStub,
        },
      };
    });

    describe('Mount', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        userServiceStub.search.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ login: 'ABC' }],
        });

        // WHEN
        const wrapper = shallowMount(UserManagement, { global: mountOptions });
        await flushPromises();

        // THEN
        expect(userServiceStub.search.calledOnce).toBeTruthy();
        expect(wrapper.vm.users[0]).toEqual(expect.objectContaining({ login: 'ABC' }));
      });
    });

    describe('Handles', () => {
      let wrapper: any;
      let comp: UserManagementComponentType;

      beforeEach(async () => {
        userServiceStub.search.resolves({ headers: { 'x-total-count': '0' }, data: [] });
        wrapper = shallowMount(UserManagement, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();
        userServiceStub.search.resetHistory();
      });

      it('should load a page', async () => {
        // GIVEN
        userServiceStub.search.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ login: 'ABC' }],
        });

        // WHEN
        comp.page = 2;
        await flushPromises();

        // THEN
        expect(userServiceStub.search.called).toBeTruthy();
        expect(comp.users[0]).toEqual(expect.objectContaining({ login: 'ABC' }));
      });

      it('should not load a page if the page is the same as the previous page', async () => {
        // GIVEN
        comp.page = 1;

        // WHEN
        await flushPromises();

        // THEN
        expect(userServiceStub.search.called).toBeFalsy();
      });

      it('should re-initialize the page', async () => {
        // GIVEN
        comp.page = 2;
        await flushPromises();
        userServiceStub.search.resetHistory();
        userServiceStub.search.resolves({
          headers: { 'x-total-count': '1' },
          data: [{ login: 'ABC' }],
        });

        // WHEN
        comp.clear();
        await flushPromises();

        // THEN
        expect(comp.page).toEqual(1);
        expect(userServiceStub.search.called).toBeTruthy();
      });

      it('should calculate the sort attribute for a non-id attribute', async () => {
        // WHEN
        comp.changeOrder('email');
        await flushPromises();

        // THEN
        expect(userServiceStub.search.lastCall.firstArg).toMatchObject({
          sort: ['email,asc', 'id'],
        });
      });

      it('Should call delete service on confirmDelete', async () => {
        // GIVEN
        userManagementServiceStub.remove.resolves({ headers: {} });
        userServiceStub.search.resolves({
          headers: { 'x-total-count': '0' },
          data: [],
        });

        // FIX: Manually mock the modal ref.
        comp.removeUser = { show: sinon.stub(), hide: sinon.stub() } as any;

        // WHEN
        comp.prepareRemove({ login: 'ABC' });
        comp.deleteUser();
        await flushPromises();

        // THEN
        expect(userManagementServiceStub.remove.calledWith('ABC')).toBeTruthy();
        expect(userServiceStub.search.called).toBeTruthy();
      });
    });
  });
});
