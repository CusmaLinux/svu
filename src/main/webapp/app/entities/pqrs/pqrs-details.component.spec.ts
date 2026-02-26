/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';
import { type RouteLocation } from 'vue-router';

import PqrsDetails from './pqrs-details.vue';
import PqrsService from './pqrs.service';
import AlertService from '@/shared/alert/alert.service';

type PqrsDetailsComponentType = InstanceType<typeof PqrsDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const pqrsSample = { id: 'ABC' };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('Pqrs Management Detail Component', () => {
    let pqrsServiceStub: SinonStubbedInstance<PqrsService>;
    let mountOptions: MountingOptions<PqrsDetailsComponentType>['global'];

    beforeEach(() => {
      route = { params: {} };
      pqrsServiceStub = sinon.createStubInstance<PqrsService>(PqrsService);

      alertService = new AlertService({
        i18n: { t: vitest.fn() } as any,
        bvToast: {
          toast: vitest.fn(),
        } as any,
      });

      mountOptions = {
        stubs: {
          'font-awesome-icon': true,
        },
        provide: {
          alertService,
          pqrsService: () => pqrsServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        pqrsServiceStub.find.resolves(pqrsSample);
        route = {
          params: {
            pqrsId: '' + 'ABC',
          },
        };
        const wrapper = shallowMount(PqrsDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.pqrs).toMatchObject(pqrsSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        pqrsServiceStub.find.resolves(pqrsSample);
        const wrapper = shallowMount(PqrsDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
