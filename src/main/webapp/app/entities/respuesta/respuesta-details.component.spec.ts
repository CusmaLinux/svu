/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';
import { type RouteLocation } from 'vue-router';

import RespuestaDetails from './respuesta-details.vue';
import RespuestaService from './respuesta.service';
import AlertService from '@/shared/alert/alert.service';

type RespuestaDetailsComponentType = InstanceType<typeof RespuestaDetails>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const respuestaSample = { id: 'ABC' };

describe('Component Tests', () => {
  let alertService: AlertService;

  afterEach(() => {
    vitest.resetAllMocks();
  });

  describe('Respuesta Management Detail Component', () => {
    let respuestaServiceStub: SinonStubbedInstance<RespuestaService>;
    let mountOptions: MountingOptions<RespuestaDetailsComponentType>['global'];

    beforeEach(() => {
      route = { params: {} };
      respuestaServiceStub = sinon.createStubInstance<RespuestaService>(RespuestaService);

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
          respuestaService: () => respuestaServiceStub,
        },
      };
    });

    describe('Navigate to details', () => {
      it('Should call load all on init', async () => {
        // GIVEN
        respuestaServiceStub.find.resolves(respuestaSample);
        route = {
          params: {
            respuestaId: '' + 'ABC',
          },
        };
        const wrapper = shallowMount(RespuestaDetails, { global: mountOptions });
        const comp = wrapper.vm;
        // WHEN
        await comp.$nextTick();

        // THEN
        expect(comp.respuesta).toMatchObject(respuestaSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        respuestaServiceStub.find.resolves(respuestaSample);
        const wrapper = shallowMount(RespuestaDetails, { global: mountOptions });
        const comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
