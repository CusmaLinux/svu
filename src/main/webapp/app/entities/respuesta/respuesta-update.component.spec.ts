/* tslint:disable max-line-length */
import { vitest } from 'vitest';
import { shallowMount, type MountingOptions, flushPromises } from '@vue/test-utils';
import sinon, { type SinonStubbedInstance } from 'sinon';

import RespuestaUpdate from './respuesta-update.vue';
import RespuestaService from './respuesta.service';
import PqrsService from '@/entities/pqrs/pqrs.service';
import ArchivoAdjuntoService from '@/entities/archivo-adjunto/archivo-adjunto.service';
import AlertService from '@/shared/alert/alert.service';

type RespuestaUpdateComponentType = InstanceType<typeof RespuestaUpdate>;

// 1. Stable Route Mocks (Fixed to prevent "undefined" errors)
const route = {
  query: {} as any,
  params: {} as any,
};
const router = {
  go: vitest.fn(),
  push: vitest.fn(),
};

// 2. Mock Vue Router
vitest.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => router,
}));

// 3. Mock Vue I18n
vitest.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}));

describe('Component Tests', () => {
  let mountOptions: MountingOptions<RespuestaUpdateComponentType>['global'];
  let alertService: AlertService;

  describe('Respuesta Management Update Component', () => {
    let comp: RespuestaUpdateComponentType;
    let respuestaServiceStub: SinonStubbedInstance<RespuestaService>;
    let pqrsServiceStub: SinonStubbedInstance<PqrsService>;
    let archivoAdjuntoServiceStub: SinonStubbedInstance<ArchivoAdjuntoService>;

    beforeEach(() => {
      // Reset mocks and route state before EVERY test
      route.query = {};
      route.params = {};
      router.go.mockClear();

      // Mocks for Services
      respuestaServiceStub = sinon.createStubInstance<RespuestaService>(RespuestaService);
      respuestaServiceStub.retrieve.resolves({});
      respuestaServiceStub.create.resolves({ id: '123' });
      respuestaServiceStub.update.resolves({ id: '123' });
      // IMPORTANT: Resolve stub with plain data, do NOT call new Service()
      respuestaServiceStub.find.resolves({ id: '123' });

      pqrsServiceStub = sinon.createStubInstance<PqrsService>(PqrsService);
      pqrsServiceStub.retrieve.resolves({ headers: {}, data: [] });
      pqrsServiceStub.find.resolves({ id: '123' });

      archivoAdjuntoServiceStub = sinon.createStubInstance<ArchivoAdjuntoService>(ArchivoAdjuntoService);
      archivoAdjuntoServiceStub.uploadFiles.resolves([]);
      archivoAdjuntoServiceStub.deleteMultiple.resolves({});
      archivoAdjuntoServiceStub.downloadAttachedFile.resolves({ blob: new Blob() } as any);

      alertService = new AlertService({
        i18n: { t: vitest.fn() } as any,
        bvToast: { toast: vitest.fn() } as any,
      });

      mountOptions = {
        stubs: {
          'b-container': true,
          'b-row': true,
          'b-col': true,
          'b-card': true,
          'b-card-header': true,
          'b-card-body': true,
          'b-form': true,
          'b-form-group': true,
          'b-form-input': true,
          'b-form-textarea': true,
          'b-form-invalid-feedback': true,
          'b-alert': true,
          'b-button': true,
          'b-spinner': true,
          'b-list-group-item': true,
          'font-awesome-icon': true,
          'transition-group': true,
        },
        provide: {
          alertService,
          respuestaService: () => respuestaServiceStub,
          pqrsService: () => pqrsServiceStub,
          archivoAdjuntoService: () => archivoAdjuntoServiceStub,
        },
      };
    });

    afterEach(() => {
      vitest.clearAllMocks();
    });

    describe('Init', () => {
      it('Should load "Respuesta" by ID and existing attachments', async () => {
        // GIVEN
        const existingRespuesta = {
          id: '123',
          pqrs: { id: 'PQRS-1' },
          _transientAttachments: [{ id: 'file1', urlArchivo: 'url1', nombre: 'doc.pdf' }],
        };
        respuestaServiceStub.find.resolves(existingRespuesta);
        route.params = { respuestaId: '123' };

        // WHEN
        const wrapper = shallowMount(RespuestaUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();

        // THEN
        expect(respuestaServiceStub.find.calledWith('123')).toBeTruthy();
        expect(comp.respuesta).toMatchObject(existingRespuesta);
        expect(comp.existingFilesInfo).toHaveLength(1);
        expect(comp.existingFilesInfo[0].nombre).toBe('doc.pdf');
      });

      it('Should load "PQRS" from query param if creating new Respuesta', async () => {
        // GIVEN
        const targetPqrs = { id: 'PQRS-99', titulo: 'Problem' };
        pqrsServiceStub.find.resolves(targetPqrs);
        route.query = { pqrsId: 'PQRS-99' };

        // WHEN
        const wrapper = shallowMount(RespuestaUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();

        // THEN
        expect(pqrsServiceStub.find.calledWith('PQRS-99')).toBeTruthy();
        expect(comp.selectedPqrsInfo).toMatchObject(targetPqrs);
        expect(comp.respuesta.pqrs).toMatchObject(targetPqrs);
        expect(comp.isPqrsFixed).toBe(true);
      });

      it('Should load all PQRS if no specific ID is provided and creating new', async () => {
        // GIVEN
        pqrsServiceStub.retrieve.resolves({ data: [{ id: 'A' }, { id: 'B' }] });
        // route.query is empty by default

        // WHEN
        const wrapper = shallowMount(RespuestaUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();

        // THEN
        expect(pqrsServiceStub.retrieve.called).toBeTruthy();
        expect(comp.allPqrs).toHaveLength(2);
      });
    });

    describe('File Management', () => {
      beforeEach(async () => {
        const wrapper = shallowMount(RespuestaUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();
      });

      it('should add files to the list on selection', () => {
        // GIVEN
        const file = new File([''], 'test.png', { type: 'image/png' });
        const event = { target: { files: [file], value: '' } };

        // WHEN
        comp.onFileChange(event as any);

        // THEN
        expect(comp.files).toHaveLength(1);
        expect(comp.files[0].name).toBe('test.png');
      });

      it('should remove a new file from the list', () => {
        // GIVEN
        comp.files.push(new File([''], 'file1.png'));
        comp.files.push(new File([''], 'file2.png'));

        // WHEN
        comp.removeFile(0);

        // THEN
        expect(comp.files).toHaveLength(1);
        expect(comp.files[0].name).toBe('file2.png');
      });

      it('should remove existing files from the visible list', () => {
        // GIVEN
        comp.existingFilesInfo = [{ id: '1', urlArchivo: 'http://bucket/file1.pdf', nombre: 'file1.pdf' }];

        // WHEN
        comp.removeExistingFile(0);

        // THEN
        expect(comp.existingFilesInfo).toHaveLength(0);
      });
    });

    describe('Save', () => {
      it('Should abort save if validation fails', async () => {
        // GIVEN
        const wrapper = shallowMount(RespuestaUpdate, { global: mountOptions });
        comp = wrapper.vm;
        await flushPromises();

        // We do not populate the required fields, so Vuelidate (real) will be invalid
        comp.respuesta.contenido = '';

        // WHEN
        await comp.save();

        // THEN
        expect(respuestaServiceStub.create.called).toBeFalsy();
        expect(respuestaServiceStub.update.called).toBeFalsy();
      });
    });
  });
});
