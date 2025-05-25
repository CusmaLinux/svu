/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';
import { type RouteLocation } from 'vue-router';

import NotificacionDetails from './notificacion-details.vue';
import NotificacionService from './notificacion.service';
import AlertService from '@/shared/alert/alert.service';

type NotificacionDetailsComponentType = InstanceType<typeof NotificacionDetails>;

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

  describe('Notificacion Management Detail Component', () => {
    let notificacionServiceStub: SinonStubbedInstance<NotificacionService>;
    let mountOptions: MountingOptions<NotificacionDetailsComponentType>['global'];

    beforeEach(() => {
      route = {};
      notificacionServiceStub = sinon.createStubInstance<NotificacionService>(NotificacionService);

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
          notificacionService: () => notificacionServiceStub,
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
        const wrapper = shallowMount(NotificacionDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.notificacion).toMatchObject(notificacionSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        notificacionServiceStub.find.resolves(notificacionSample);
        const wrapper = shallowMount(NotificacionDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
