import { computed, defineComponent, inject, ref, onUnmounted, watch, type Ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { useVuelidate } from '@vuelidate/core';

import { PqrsStatus, PqrsType } from '@/constants';
import PqrsService from './pqrs.service';
import useDataUtils from '@/shared/data/data-utils.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import { useAlertService } from '@/shared/alert/alert.service';
import { helpers, email, maxLength, minLength } from '@vuelidate/validators';

import OficinaService from '@/entities/oficina/oficina.service';
import { type IOficina } from '@/shared/model/oficina.model';
import { type IPqrs, Pqrs } from '@/shared/model/pqrs.model';

import ArchivoAdjuntoService from '@/entities/archivo-adjunto/archivo-adjunto.service';
import { type IArchivoAdjunto } from '@/shared/model/archivo-adjunto.model';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'PqrsUpdate',
  setup() {
    const pqrsService = inject('pqrsService', () => new PqrsService());
    const alertService = inject('alertService', () => useAlertService(), true);
    const archivoAdjuntoService = inject('archivoAdjuntoService', () => new ArchivoAdjuntoService());
    const oficinaService = inject('oficinaService', () => new OficinaService());
    const { t: t$ } = useI18n();

    const files = ref<File[]>([]);
    const existingFilesInfo: Ref<IArchivoAdjunto[]> = ref([]);
    const filesToDelete: Ref<string[]> = ref([]);
    const fileInput = ref<HTMLInputElement | null>(null);
    const archivosAdjuntosDTO = ref<IArchivoAdjunto[]>([]);
    const isUploading = ref(false);
    const errorMessage = ref<string | null>(null);
    const successMessage = ref<string | null>(null);

    const pqrs: Ref<IPqrs> = ref(new Pqrs());
    const oficinas: Ref<IOficina[]> = ref([]);
    const isSaving = ref(false);

    const input: Ref<HTMLInputElement | null> = ref(null);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'es'), true);

    const route = useRoute();
    const router = useRouter();

    const statesPqrsRef = ref(PqrsStatus);

    const isUpdateMode = computed(() => {
      return !!pqrs.value.id;
    });

    const updateDueDate = (event: Event) => {
      const target = event.target as HTMLInputElement;
      if (target.value) {
        pqrs.value.fechaLimiteRespuesta = new Date(target.value);
      } else {
        pqrs.value.fechaLimiteRespuesta = null;
      }
    };

    const requesterEmailModel = computed({
      get() {
        return pqrs.value.requesterEmail;
      },
      set(newValue: string) {
        pqrs.value.requesterEmail = newValue === '' ? null : newValue;
      },
    });

    const optionalEmail = helpers.withMessage(
      t$('global.messages.validate.email.invalid').toString(),
      (value: string) => !helpers.req(value) || email.$validator(value, {}, {}),
    );

    const optionalMinLength = helpers.withMessage(t$('global.messages.validate.email.minlength').toString(), (value: string) => {
      if (!helpers.req(value)) {
        return true;
      }
      return minLength(5).$validator(value, {}, {});
    });

    const retrievePqrs = async (pqrsId: string) => {
      try {
        const res = await pqrsService().find(pqrsId);

        res.fechaCreacion = res.fechaCreacion ? new Date(res.fechaCreacion) : null;
        res.fechaLimiteRespuesta = res.fechaLimiteRespuesta ? new Date(res.fechaLimiteRespuesta) : null;

        pqrs.value = res;

        if (res._transientAttachments) {
          existingFilesInfo.value = res._transientAttachments;
        } else {
          existingFilesInfo.value = [];
        }
        files.value = [];
      } catch (error: any) {
        if (error && error.response) {
          alertService.showHttpError(error.response);
        } else {
          alertService.showError(t$('ventanillaUnicaApp.pqrs.errors.retrieveError'));
          console.error('Error retrieving PQRS data:', error);
        }
      }
    };
    const previousState = () => {
      filesToDelete.value = [];
      router.go(-1);
    };

    const removeFile = (index: number) => {
      files.value = files.value.filter((_, i) => i !== index);

      if (fileInput.value) {
        fileInput.value.value = '';
      }
    };
    const removeExistingFile = (index: number) => {
      if (existingFilesInfo.value && Array.isArray(existingFilesInfo.value) && existingFilesInfo.value[index]) {
        const fileToRemove = existingFilesInfo.value[index];

        if (fileToRemove.urlArchivo && !filesToDelete.value.includes(fileToRemove.urlArchivo)) {
          filesToDelete.value.push(fileToRemove.urlArchivo);
        }

        existingFilesInfo.value = existingFilesInfo.value.filter((_, currentIndex) => currentIndex !== index);
      }
    };
    const triggerFileInput = () => {
      if (fileInput.value) {
        fileInput.value.click();
      }
    };
    const onFileChange = (event: Event) => {
      const target = event.target as HTMLInputElement;
      if (target && target.files && target.files.length > 0) {
        input.value = target;
        const newFiles = Array.from(target.files);
        files.value = [...files.value, ...newFiles];
        target.value = '';
      }
    };
    const uploadFiles = async () => {
      isUploading.value = true;
      errorMessage.value = null;
      successMessage.value = null;

      const formData = new FormData();
      files.value.forEach(file => {
        formData.append('files', file);
      });

      try {
        const uploadResponse = await archivoAdjuntoService().uploadFiles(formData);
        archivosAdjuntosDTO.value = uploadResponse;

        successMessage.value = 'Archivos subidos correctamente';
      } catch (error) {
        errorMessage.value = 'Error al subir archivos';
        throw error;
      } finally {
        isUploading.value = false;
      }
    };
    const downloadAttachedFile = async (fileURL: string | undefined | null, fileName: string | undefined) => {
      if (!fileURL && !fileName) {
        return;
      }
      try {
        if (fileURL && fileName) {
          const { blob } = await archivoAdjuntoService().downloadAttachedFile(fileURL);
          const link = document.createElement('a');
          const url = window.URL.createObjectURL(blob);
          link.href = url;
          link.setAttribute('download', fileName);
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);
        }
      } catch (error: any) {
        alertService.showHttpError(error.response ?? 'Ocurrió un error inesperado.');
      }
    };
    const save = async (): Promise<void> => {
      try {
        if (isSaving.value) {
          return;
        }

        isSaving.value = true;
        if (filesToDelete.value.length > 0) {
          await archivoAdjuntoService().deleteMultiple(filesToDelete.value);
          filesToDelete.value = [];
        }

        if (files.value.length > 0) {
          await uploadFiles();
          pqrs.value._transientAttachments = archivosAdjuntosDTO.value.map(attachedFile => ({ id: attachedFile.id }));
        }

        let savedPqrs;
        if (pqrs.value.id) {
          savedPqrs = await pqrsService().update(pqrs.value);
          alertService.showInfo(t$('ventanillaUnicaApp.pqrs.updated', { param: savedPqrs.id }));
        } else {
          savedPqrs = await pqrsService().submitPqrsRequest(pqrs.value);
          alertService.showSuccess(t$('ventanillaUnicaApp.pqrs.created', { param: savedPqrs.id }).toString());
        }

        previousState();
      } catch (error: any) {
        alertService.showHttpError(error.response ?? 'Ocurrió un error inesperado.');
      } finally {
        isSaving.value = false;
      }
    };

    onUnmounted(() => {
      filesToDelete.value = [];
    });

    if (route.params?.pqrsId) {
      retrievePqrs(route.params.pqrsId as string);
    }

    const initRelationships = () => {
      oficinaService()
        .retrieve()
        .then(res => {
          oficinas.value = res.data;
        });
    };
    initRelationships();

    const dataUtils = useDataUtils();
    const validations = useValidation();
    const validationRules = {
      type: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      requesterEmail: {
        minLength: optionalMinLength,
        maxLength: maxLength(254),
        email: optionalEmail,
      },
      titulo: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      descripcion: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      fechaCreacion: {},
      fechaLimiteRespuesta: {},
      daysToReply: {},
      estado: {},
      oficinaResponder: {},
    };
    const v$ = useVuelidate(validationRules, pqrs as any);
    return {
      input,
      pqrsService,
      alertService,
      pqrs,
      previousState,
      isSaving,
      currentLanguage,
      oficinas,
      files,
      existingFilesInfo,
      downloadAttachedFile,
      fileInput,
      archivosAdjuntosDTO,
      uploadFiles,
      onFileChange,
      triggerFileInput,
      removeFile,
      removeExistingFile,
      save,
      isUploading,
      errorMessage,
      successMessage,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: pqrs }),
      t$,
      isUpdateMode,
      statesPqrs: statesPqrsRef,
      requesterEmailModel,
      PqrsType,
      updateDueDate,
    };
  },
});
