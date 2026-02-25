/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';
import { type RouteLocation } from 'vue-router';

import InformePqrsDetails from './informe-pqrs-details.vue';
import InformePqrsService from './informe-pqrs.service';
import AlertService from '@/shared/alert/alert.service';

type InformePqrsDetailsComponentType = InstanceType<typeof InformePqrsDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const informePqrsSample = { id: 'ABC' };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('InformePqrs Management Detail Component', () => {
    let informePqrsServiceStub: SinonStubbedInstance<InformePqrsService>;
    let mountOptions: MountingOptions<InformePqrsDetailsComponentType>['global'];

    beforeEach(() => {
      route = { params: {} };
      informePqrsServiceStub = sinon.createStubInstance<InformePqrsService>(InformePqrsService);

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
          informePqrsService: () => informePqrsServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        informePqrsServiceStub.find.resolves(informePqrsSample);
        route = {
          params: {
            informePqrsId: '' + 'ABC',
          },
        };
        const wrapper = shallowMount(InformePqrsDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.informePqrs).toMatchObject(informePqrsSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        informePqrsServiceStub.find.resolves(informePqrsSample);
        const wrapper = shallowMount(InformePqrsDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
