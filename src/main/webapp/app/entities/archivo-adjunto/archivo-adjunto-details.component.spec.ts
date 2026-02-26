/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';
import { type RouteLocation } from 'vue-router';

import ArchivoAdjuntoDetails from './archivo-adjunto-details.vue';
import ArchivoAdjuntoService from './archivo-adjunto.service';
import AlertService from '@/shared/alert/alert.service';

type ArchivoAdjuntoDetailsComponentType = InstanceType<typeof ArchivoAdjuntoDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const archivoAdjuntoSample = { id: 'ABC' };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('ArchivoAdjunto Management Detail Component', () => {
    let archivoAdjuntoServiceStub: SinonStubbedInstance<ArchivoAdjuntoService>;
    let mountOptions: MountingOptions<ArchivoAdjuntoDetailsComponentType>['global'];

    beforeEach(() => {
      route = { params: {} };
      archivoAdjuntoServiceStub = sinon.createStubInstance<ArchivoAdjuntoService>(ArchivoAdjuntoService);

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
          archivoAdjuntoService: () => archivoAdjuntoServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        archivoAdjuntoServiceStub.find.resolves(archivoAdjuntoSample);
        route = {
          params: {
            archivoAdjuntoId: '' + 'ABC',
          },
        };
        const wrapper = shallowMount(ArchivoAdjuntoDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.archivoAdjunto).toMatchObject(archivoAdjuntoSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        archivoAdjuntoServiceStub.find.resolves(archivoAdjuntoSample);
        const wrapper = shallowMount(ArchivoAdjuntoDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
