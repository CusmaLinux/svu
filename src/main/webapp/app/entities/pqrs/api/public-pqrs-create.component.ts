import { computed, defineComponent, inject, ref, onUnmounted, type Ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { useVuelidate } from '@vuelidate/core';

import PqrsService from '@/entities/pqrs/pqrs.service';
import useDataUtils from '@/shared/data/data-utils.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import { useAlertService } from '@/shared/alert/alert.service';
import { useRecaptcha } from '@/shared/composables/use-recaptcha';

import { type IPqrs, Pqrs } from '@/shared/model/pqrs.model';
import { PqrsType } from '@/constants';
import { helpers, email, maxLength, minLength } from '@vuelidate/validators';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'PublicPqrsCreate',
  setup() {
    const pqrsService = inject('pqrsService', () => new PqrsService());
    const alertService = inject('alertService', () => useAlertService(), true);
    const { t: t$ } = useI18n();
    const { getToken } = useRecaptcha();

    const files = ref<File[]>([]);
    const filesToDelete: Ref<string[]> = ref([]);
    const fileInput = ref<HTMLInputElement | null>(null);
    const isUploading = ref(false);
    const idempotencyKey = ref<string | null>(null);

    const pqrsTypeOptions = computed(() => {
      return [
        { value: null, text: '-- Seleccione un tipo --', disabled: true },
        ...Object.values(PqrsType).map(value => ({ value: value, text: value })),
      ];
    });

    const hasFiles = computed(() => {
      return files.value?.length > 0;
    });

    const pqrs: Ref<IPqrs> = ref(new Pqrs());
    const isSaving = ref(false);
    watch(
      [pqrs, files],
      () => {
        if (!isSaving.value) idempotencyKey.value = null;
      },
      { deep: true },
    );

    const input: Ref<HTMLInputElement | null> = ref(null);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'es'), true);

    const router = useRouter();

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

    const navigateToVerifyPage = (accessToken: string | undefined) => {
      filesToDelete.value = [];
      router.push({
        name: 'PublicPqrsVerify',
        query: {
          accessToken: accessToken,
        },
      });
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
    const onDrop = (event: DragEvent) => {
      event.preventDefault();
      if (isUploading.value) {
        return;
      }
      const droppedFiles = event.dataTransfer?.files;
      if (droppedFiles) {
        Array.from(droppedFiles).forEach(file => files.value.push(file));
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
        isUploading.value = true;
        const formData = new FormData();
        const request = { ...pqrs.value, _transientAttachments: [] };
        formData.append('pqrs', new Blob([JSON.stringify(request)], { type: 'application/json' }));
        files.value.forEach(file => formData.append('files', file));
        const recaptchaToken = await getToken('submit_pulic_pqrs');
        const headers = { 'X-Recaptcha-Token': recaptchaToken, 'Idempotency-Key': (idempotencyKey.value ??= crypto.randomUUID()) };
        const savedPqrs = await pqrsService().submitPqrsRequest(formData, headers);
        isUploading.value = false;
        idempotencyKey.value = null;
        alertService.showSuccess(t$('ventanillaUnicaApp.pqrs.created', { param: savedPqrs.id }).toString());

        navigateToVerifyPage(savedPqrs.accessToken);
      } catch (error: any) {
        isUploading.value = false;
        alertService.showHttpError(error.response ?? 'Ocurrió un error inesperado.');
      } finally {
        isSaving.value = false;
      }
    };

    onUnmounted(() => {
      filesToDelete.value = [];
    });

    const dataUtils = useDataUtils();
    const validations = useValidation();
    const validationRules = {
      type: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      titulo: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      descripcion: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      requesterEmail: {
        minLength: optionalMinLength,
        maxLength: maxLength(254),
        email: optionalEmail,
      },
      fechaCreacion: {},
      fechaLimiteRespuesta: {},
      estado: {},
      oficinaResponder: {},
    };
    const v$ = useVuelidate(validationRules, pqrs as any);
    return {
      input,
      pqrsService,
      alertService,
      pqrs,
      navigateToVerifyPage,
      previousState,
      isSaving,
      currentLanguage,
      files,
      downloadAttachedFile,
      fileInput,
      onFileChange,
      triggerFileInput,
      removeFile,
      save,
      onDrop,
      isUploading,
      requesterEmailModel,
      pqrsTypeOptions,
      hasFiles,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: pqrs }),
      t$,
    };
  },
});
