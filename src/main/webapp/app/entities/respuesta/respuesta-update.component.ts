import { type Ref, computed, defineComponent, inject, ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { useVuelidate } from '@vuelidate/core';
import { required, helpers } from '@vuelidate/validators';

import RespuestaService from './respuesta.service';
import useDataUtils from '@/shared/data/data-utils.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import { useAlertService } from '@/shared/alert/alert.service';

import PqrsService from '@/entities/pqrs/pqrs.service';
import { type IPqrs } from '@/shared/model/pqrs.model';
import { type IRespuesta, Respuesta } from '@/shared/model/respuesta.model';

import ArchivoAdjuntoService from '@/entities/archivo-adjunto/archivo-adjunto.service';
import { type IArchivoAdjunto } from '@/shared/model/archivo-adjunto.model';
import type AccountService from '@/account/account.service';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'RespuestaUpdate',
  setup() {
    const respuestaService = inject('respuestaService', () => new RespuestaService());
    const alertService = inject('alertService', () => useAlertService(), true);
    const archivoAdjuntoService = inject('archivoAdjuntoService', () => new ArchivoAdjuntoService());
    const pqrsService = inject('pqrsService', () => new PqrsService());

    const respuesta: Ref<IRespuesta> = ref(new Respuesta());
    const files = ref<File[]>([]);
    const existingFilesInfo: Ref<IArchivoAdjunto[]> = ref([]);
    const filesToDelete: Ref<string[]> = ref([]);
    const fileInput = ref<HTMLInputElement | null>(null);
    const archivosAdjuntosDTO = ref<IArchivoAdjunto[]>([]);
    const isUploading = ref(false);
    const errorMessage = ref<string | null>(null);
    const successMessage = ref<string | null>(null);
    const input: Ref<HTMLInputElement | null> = ref(null);

    const allPqrs: Ref<IPqrs[]> = ref([]);
    const selectedPqrsInfo: Ref<IPqrs | null> = ref(null);
    const isPqrsFixed = ref(false);
    const pqrs: Ref<IPqrs[]> = ref([]);
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'es'), true);

    const route = useRoute();
    const router = useRouter();

    const dataUtils = useDataUtils();
    const { t: t$ } = useI18n();
    const validations = useValidation();
    const validationRules = {
      contenido: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      pqr: {
        required: helpers.withMessage(t$('entity.validation.required').toString(), value => {
          return !!respuesta.value.pqr || !!value;
        }),
      },
    };
    const v$ = useVuelidate(validationRules, respuesta as any);

    const previousState = () => {
      filesToDelete.value = [];
      router.go(-1);
    };

    const retrieveRespuesta = async (respuestaId: string) => {
      try {
        const res = await respuestaService().find(respuestaId);
        respuesta.value = res;
        if (res.pqr) {
          selectedPqrsInfo.value = res.pqr;
        }

        if (res.archivosAdjuntosDTO) {
          existingFilesInfo.value = res.archivosAdjuntosDTO;
        } else {
          existingFilesInfo.value = [];
        }
        files.value = [];
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    const initRelationshipsAndPqrs = async () => {
      const pqrsIdFromQuery = route.query.pqrsId as string;
      if (pqrsIdFromQuery && !respuesta.value.id) {
        isPqrsFixed.value = true;
        try {
          const foundPqrs = await pqrsService().find(pqrsIdFromQuery);
          if (foundPqrs) {
            respuesta.value.pqr = foundPqrs;
            selectedPqrsInfo.value = foundPqrs;
          }
        } catch (error: any) {
          alertService.showHttpError(error.response);
        }
      } else if (!respuesta.value.pqr) {
        try {
          const res = await pqrsService().retrieve();
          allPqrs.value = res.data;
        } catch (error: any) {
          alertService.showHttpError(error.response);
        }
      }
    };

    const onFileChange = (event: Event) => {
      const target = event.target as HTMLInputElement;
      if (target && target.files && target.files.length > 0) {
        const newFiles = Array.from(target.files);
        files.value = [...files.value, ...newFiles];
        target.value = ''; // Limpiar el input para permitir seleccionar el mismo archivo de nuevo
      }
    };

    const triggerFileInput = () => {
      fileInput.value?.click();
    };

    const removeFile = (index: number) => {
      files.value = files.value.filter((_, i) => i !== index);
    };

    const removeExistingFile = (index: number) => {
      if (existingFilesInfo.value?.[index]) {
        const fileToRemove = existingFilesInfo.value[index];
        if (fileToRemove.urlArchivo && !filesToDelete.value.includes(fileToRemove.urlArchivo)) {
          filesToDelete.value.push(fileToRemove.urlArchivo);
        }
        existingFilesInfo.value.splice(index, 1);
      }
    };

    const uploadFiles = async (): Promise<IArchivoAdjunto[]> => {
      if (files.value.length === 0) return [];

      isUploading.value = true;
      const formData = new FormData();
      files.value.forEach(file => {
        formData.append('files', file);
      });

      try {
        return await archivoAdjuntoService().uploadFiles(formData);
      } catch (error) {
        alertService.showError(t$('ventanillaUnicaApp.respuesta.errors.upload'));
        throw error; // Propagar el error para detener el proceso de guardado
      } finally {
        isUploading.value = false;
      }
    };

    const downloadAttachedFile = async (fileURL: string | undefined | null, fileName: string | undefined) => {
      if (!fileURL || !fileName) return;
      try {
        const { blob } = await archivoAdjuntoService().downloadAttachedFile(fileURL);
        const link = document.createElement('a');
        const url = window.URL.createObjectURL(blob);
        link.href = url;
        link.setAttribute('download', fileName);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      } catch (error: any) {
        alertService.showHttpError(error.response ?? 'Ocurrió un error inesperado.');
      }
    };

    // --- Función de Guardado Unificada ---
    const save = async () => {
      v$.value.$touch();
      if (v$.value.$invalid) {
        alertService.showError(t$('entity.validation.invalid'));
        return;
      }
      if (!respuesta.value.pqr && !isPqrsFixed.value) {
        alertService.showError(t$('ventanillaUnicaApp.respuesta.errors.pqrsNotSelected'));
        return;
      }

      isSaving.value = true;

      try {
        // 1. Eliminar archivos marcados para borrar
        if (filesToDelete.value.length > 0) {
          await archivoAdjuntoService().deleteMultiple(filesToDelete.value);
          filesToDelete.value = [];
        }

        // 2. Subir nuevos archivos
        const nuevosArchivosAdjuntos = await uploadFiles();

        // 3. Preparar el payload
        const payload: IRespuesta = { ...respuesta.value };

        // Combinar archivos existentes (que no se borraron) con los nuevos
        const archivosActuales = existingFilesInfo.value.map(file => ({
          id: file.id,
          urlArchivo: file.urlArchivo,
          nombreArchivo: file.nombre,
        }));
        payload.archivosAdjuntosDTO = [...archivosActuales, ...nuevosArchivosAdjuntos];

        // Limpiar la referencia a Pqrs para enviar solo el ID si existe
        if (payload.pqr && payload.pqr.id) {
          payload.pqr = { id: payload.pqr.id } as IPqrs;
        }

        // 4. Guardar la entidad Respuesta (crear o actualizar)
        let result;
        if (payload.id) {
          result = await respuestaService().update(payload);
          alertService.showInfo(t$('ventanillaUnicaApp.respuesta.updated', { param: result.id }));
        } else {
          result = await respuestaService().create(payload);
          alertService.showSuccess(t$('ventanillaUnicaApp.respuesta.created', { param: result.id }).toString());
        }
        previousState();
      } catch (error: any) {
        // El error ya se debería mostrar desde el servicio o la función de upload
        if (!isUploading.value) {
          // Si el error no fue de la subida de archivos
          alertService.showHttpError(error.response);
        }
      } finally {
        isSaving.value = false;
      }
    };

    // --- Hook onMounted ---
    onMounted(async () => {
      filesToDelete.value = [];
      if (route.params?.respuestaId) {
        await retrieveRespuesta(route.params.respuestaId as string);
      }
      await initRelationshipsAndPqrs();
    });

    return {
      alertService,
      respuesta,
      previousState,
      save,
      archivosAdjuntosDTO,
      uploadFiles,
      isSaving,
      pqrs: allPqrs,
      currentLanguage,
      allPqrs,
      selectedPqrsInfo,
      isPqrsFixed,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: respuesta }),
      t$,
      files,
      existingFilesInfo,
      fileInput,
      triggerFileInput,
      onFileChange,
      removeFile,
      removeExistingFile,
      downloadAttachedFile,
      isUploading,
    };
  },
});
