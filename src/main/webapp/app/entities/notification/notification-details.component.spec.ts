/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount, flushPromises } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';
import { type RouteLocation } from 'vue-router';

import NotificationDetails from './notification-details.vue';
import NotificationService from './notification.service';
import AlertService from '@/shared/alert/alert.service';

type NotificationDetailsComponentType = InstanceType<typeof NotificationDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const notificacionSample = { id: 'ABC' };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('Notification Management Detail Component', () => {
    let notificacionServiceStub: SinonStubbedInstance<NotificationService>;
    let mountOptions: MountingOptions<NotificationDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      notificacionServiceStub = sinon.createStubInstance<NotificationService>(NotificationService);

      alertService = new AlertService({
        i18n: { t: vitest.fn() } as any,
        bvToast: {
          toast: vitest.fn(),
        } as any,
      });

      mountOptions = {
        stubs: {
          'font-awesome-icon': true,
          'router-link': true,
        },
        provide: {
          alertService,
          notificationService: () => notificacionServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        notificacionServiceStub.find.resolves(notificacionSample);
        route = {
          params: {
            notificacionId: '' + 'ABC',
          },
        };
        const wrapper = shallowMount(NotificationDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await flushPromises();

        // THEN
        expect(comp.notification).toMatchObject(notificacionSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        notificacionServiceStub.find.resolves(notificacionSample);
        const wrapper = shallowMount(NotificationDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await flushPromises();

        comp.previousState();
        await flushPromises();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
