/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { type MountingOptions, shallowMount, flushPromises } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';
import { type RouteLocation } from 'vue-router';

import dayjs from 'dayjs';
import PqrsUpdate from './pqrs-update.vue';
import PqrsService from './pqrs.service';
import { DATE_TIME_LONG_FORMAT } from '@/shared/composables/date-format';
import AlertService from '@/shared/alert/alert.service';

import OficinaService from '@/entities/oficina/oficina.service';
import ArchivoAdjuntoService from '@/entities/archivo-adjunto/archivo-adjunto.service';
import { PqrsType } from '@/constants';

type PqrsUpdateComponentType = InstanceType<typeof PqrsUpdate>;

let route: Partial<RouteLocation>;
const routerGoMock = vitest.fn();

vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ go: routerGoMock }),
}));

// Mock Recaptcha since it is used in the setup/save flow
vitest.mock('@/shared/composables/use-recaptcha', () => ({
  useRecaptcha: () => ({
    getToken: vitest.fn().mockResolvedValue('mock-token'),
  }),
}));

const pqrsSample = {
  id: 'ABC',
  titulo: 'Test Title',
  descripcion: 'Test Description',
  type: PqrsType.PETICION,
};

describe('Component Tests', () => {
  let mountOptions: MountingOptions<PqrsUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('Pqrs Management Update Component', () => {
    let comp: PqrsUpdateComponentType;
    let pqrsServiceStub: SinonStubbedInstance<PqrsService>;
    let oficinaServiceStub: SinonStubbedInstance<OficinaService>;
    let archivoAdjuntoServiceStub: SinonStubbedInstance<ArchivoAdjuntoService>;

    beforeEach(() => {
      route = {};

      // Mocks
      pqrsServiceStub = sinon.createStubInstance<PqrsService>(PqrsService);
      pqrsServiceStub.retrieve.resolves({ data: [], headers: {} });
      pqrsServiceStub.find.resolves(pqrsSample);

      oficinaServiceStub = sinon.createStubInstance<OficinaService>(OficinaService);
      oficinaServiceStub.retrieve.resolves({ data: [] });

      archivoAdjuntoServiceStub = sinon.createStubInstance<ArchivoAdjuntoService>(ArchivoAdjuntoService);
      archivoAdjuntoServiceStub.uploadFiles.resolves([]);
      archivoAdjuntoServiceStub.deleteMultiple.resolves({});

      alertService = new AlertService({
        i18n: { t: vitest.fn() } as any,
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
          'b-form-textarea': true,
          'b-form-select': true,
          'b-form-group': true,
          'b-form-invalid-feedback': true,
          'b-card': true,
          'b-card-header': true,
          'b-card-body': true,
          'b-alert': true,
          'b-row': true,
          'b-col': true,
          'b-form': true,
          'b-button': true,
          'b-spinner': true,
          'v-multiselect': true,
        },
        provide: {
          alertService,
          pqrsService: () => pqrsServiceStub,
          oficinaService: () => oficinaServiceStub,
          archivoAdjuntoService: () => archivoAdjuntoServiceStub,
          currentLanguage: { value: 'es' },
        },
      };
    });

    afterEach(() => {
      vitest.resetAllMocks();
    });

    describe('load', () => {
      beforeEach(() => {
        const wrapper = shallowMount(PqrsUpdate, { global: mountOptions });
        comp = wrapper.vm;
      });

      it('Should convert date from string', () => {
        // GIVEN
        const date = new Date('2019-10-15T11:42:02Z');

        // WHEN
        const convertedDate = comp.convertDateTimeFromServer(date);

        // THEN
        expect(convertedDate).toEqual(dayjs(date).format(DATE_TIME_LONG_FORMAT));
      });

      it('Should not convert date if date is not present', () => {
        expect(comp.convertDateTimeFromServer(null)).toBeNull();
      });
    });

    describe('save', () => {
      it('Should call update service on save for existing entity', async () => {
        // GIVEN
        const wrapper = shallowMount(PqrsUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.pqrs = { ...pqrsSample }; // existing entity with ID
        pqrsServiceStub.update.resolves(pqrsSample);

        // WHEN
        await comp.save();
        await flushPromises();

        // THEN
        // Fix: Use calledOnce to verify call, then check args manually
        expect(pqrsServiceStub.update.calledOnce).toBeTruthy();
        expect(pqrsServiceStub.update.firstCall.args[0]).toMatchObject(pqrsSample);
        expect(comp.isSaving).toEqual(false);
      });

      it('Should call submitPqrsRequest (create) service on save for new entity', async () => {
        // GIVEN
        const entity = {
          titulo: 'New Title',
          descripcion: 'New Desc',
          type: PqrsType.QUEJA,
        }; // No ID = Create

        pqrsServiceStub.submitPqrsRequest.resolves({ id: '123' });
        const wrapper = shallowMount(PqrsUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.pqrs = entity;

        // WHEN
        await comp.save();
        await flushPromises();

        // THEN
        // Fix: Use calledOnce to verify call, then check args manually
        expect(pqrsServiceStub.submitPqrsRequest.calledOnce).toBeTruthy();
        expect(pqrsServiceStub.submitPqrsRequest.firstCall.args[0]).toMatchObject(entity);
        expect(comp.isSaving).toEqual(false);
      });

      it('Should upload files before saving if files are present', async () => {
        // GIVEN
        const entity = { ...pqrsSample };
        const file = new File(['content'], 'test.txt', { type: 'text/plain' });

        const wrapper = shallowMount(PqrsUpdate, { global: mountOptions });
        comp = wrapper.vm;
        comp.pqrs = entity;

        // Add a file to upload
        comp.files = [file];

        // Mock successful upload response
        const uploadedFile = { id: 1, nombre: 'test.txt' };
        archivoAdjuntoServiceStub.uploadFiles.resolves([uploadedFile]);
        pqrsServiceStub.update.resolves(entity);

        // WHEN
        await comp.save();
        await flushPromises();

        // THEN
        expect(archivoAdjuntoServiceStub.uploadFiles.called).toBeTruthy();
        expect(pqrsServiceStub.update.called).toBeTruthy();
        // The service should receive the entity with transient attachments
        expect(pqrsServiceStub.update.lastCall.args[0]._transientAttachments).toEqual([{ id: 1 }]);
      });
    });

    describe('Before route enter', () => {
      it('Should retrieve data', async () => {
        // GIVEN
        const incomingData = {
          id: 'ABC',
          fechaCreacion: '2023-01-01T10:00:00',
          _transientAttachments: [{ id: 1, urlArchivo: 'url' }],
        };
        pqrsServiceStub.find.resolves(incomingData);

        // WHEN
        route = {
          params: {
            pqrsId: 'ABC',
          },
        };
        const wrapper = shallowMount(PqrsUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();

        // THEN
        expect(comp.pqrs.id).toBe('ABC');
        expect(comp.pqrs.fechaCreacion).toBeInstanceOf(Date);
        expect(comp.existingFilesInfo).toHaveLength(1);
      });
    });

    describe('Initialization', () => {
      it('Should load oficinas on init', async () => {
        const oficinas = [{ id: '1', nombre: 'Oficina Test' }];
        oficinaServiceStub.retrieve.resolves({ data: oficinas });

        const wrapper = shallowMount(PqrsUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();

        expect(oficinaServiceStub.retrieve.called).toBeTruthy();
        expect(comp.oficinas).toEqual(oficinas);
      });
    });

    describe('Previous state', () => {
      it('Should go previous state', async () => {
        const wrapper = shallowMount(PqrsUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();

        comp.previousState();
        await flushPromises();

        expect(routerGoMock).toHaveBeenCalledWith(-1);
      });
    });
  });
});
