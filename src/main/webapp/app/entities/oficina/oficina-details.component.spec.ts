/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';
import { type RouteLocation } from 'vue-router';

import OficinaDetails from './oficina-details.vue';
import OficinaService from './oficina.service';
import AlertService from '@/shared/alert/alert.service';

type OficinaDetailsComponentType = InstanceType<typeof OficinaDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const oficinaSample = { id: 'ABC' };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('Oficina Management Detail Component', () => {
    let oficinaServiceStub: SinonStubbedInstance<OficinaService>;
    let mountOptions: MountingOptions<OficinaDetailsComponentType>['global'];

    beforeEach(() => {
      route = { params: {} };
      oficinaServiceStub = sinon.createStubInstance<OficinaService>(OficinaService);

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
          oficinaService: () => oficinaServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        oficinaServiceStub.find.resolves(oficinaSample);
        route = {
          params: {
            oficinaId: '' + 'ABC',
          },
        };
        const wrapper = shallowMount(OficinaDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.oficina).toMatchObject(oficinaSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        oficinaServiceStub.find.resolves(oficinaSample);
        const wrapper = shallowMount(OficinaDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
