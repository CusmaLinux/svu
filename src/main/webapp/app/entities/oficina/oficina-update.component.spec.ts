/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';
import { type RouteLocation } from 'vue-router';

import OficinaUpdate from './oficina-update.vue';
import OficinaService from './oficina.service';
import UserService from '@/entities/user/user.service';
import AlertService from '@/shared/alert/alert.service';

type OficinaUpdateComponentType = InstanceType<typeof OficinaUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

const oficinaSample = { id: 'ABC' };

describe('Component Tests', () => {
  let mountOptions: MountingOptions<OficinaUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('Oficina Management Update Component', () => {
    let comp: OficinaUpdateComponentType;
    let oficinaServiceStub: SinonStubbedInstance<OficinaService>;
    let userServiceStub: SinonStubbedInstance<UserService>;

    beforeEach(() => {
      route = {};
      oficinaServiceStub = sinon.createStubInstance<OficinaService>(OficinaService);
      oficinaServiceStub.retrieve.onFirstCall().resolves(Promise.resolve([]));

      userServiceStub = sinon.createStubInstance<UserService>(UserService);
      userServiceStub.retrieve.resolves({ data: [] });

      alertService = new AlertService({
        i18n: { t: vitest.fn().mockReturnValue('mocked-string') } as any,
        bvToast: {
          toast: vitest.fn(),
        } as any,
      });

      mountOptions = {
        stubs: {
          'font-awesome-icon': true,
          'b-input-group': true,
          'b-input-group-prepend': true,
          'b-form-datepicker': true,
          'b-form-input': true,
        },
        provide: {
          alertService,
          oficinaService: () => oficinaServiceStub,
          userService: () => userServiceStub,
        },
      };
    });

    afterEach(() => {
      vitest.resetAllMocks();
    });

    describe('save', () => {
      it('Should call update service on save for existing entity', async () => {
        // GIVEN
        const wrapper = shallowMount(OficinaUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.oficina = oficinaSample;
        oficinaServiceStub.update.resolves(oficinaSample);

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(oficinaServiceStub.update.calledWith(oficinaSample)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call create service on save for new entity', async () => {
        // GIVEN
        const entity = {};
        oficinaServiceStub.create.resolves(entity);
        const wrapper = shallowMount(OficinaUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.oficina = entity;

        // WHEN
        comp.save();
        await comp.$nextTick();

        // THEN
        expect(oficinaServiceStub.create.calledWith(entity)).toBeTruthy();
        expect(comp.isSaving).toEqual(false);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        oficinaServiceStub.find.resolves(oficinaSample);
        oficinaServiceStub.retrieve.resolves([oficinaSample]);

        // WHEN
        route = {
          params: {
            oficinaId: `${oficinaSample.id}`,
          },
        };
        const wrapper = shallowMount(OficinaUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        // THEN
        expect(comp.oficina).toMatchObject(oficinaSample);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        oficinaServiceStub.find.resolves(oficinaSample);
        const wrapper = shallowMount(OficinaUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await comp.$nextTick();

        comp.previousState();
        await comp.$nextTick();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
