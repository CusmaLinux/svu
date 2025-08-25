import { computed, defineComponent, inject, ref, onMounted, type Ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AttachmentList from '@/entities/archivo-adjunto/attachment-list.vue';
import { useValidation } from '@/shared/composables';
import { useVuelidate } from '@vuelidate/core';
import { useDateFormat } from '@/shared/composables';
import { useRecaptcha } from '@/shared/composables/use-recaptcha';
import { useI18n } from 'vue-i18n';
import PqrsService from '@/entities/pqrs/pqrs.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { type IPqrs } from '@/shared/model/pqrs.model';
import { Respuesta, type IRespuesta } from '@/shared/model/respuesta.model';
import { PqrsStatus } from '@/constants';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'PublicPqrsDetails',
  components: {
    'attachment-list': AttachmentList,
  },
  props: {
    accessToken: {
      type: String,
      default: null,
    },
  },

  setup(props) {
    const pqrsService = inject('pqrsService', () => new PqrsService());
    const alertService = inject('alertService', () => useAlertService(), true);
    const router = useRouter();
    const route = useRoute();
    const { t: t$ } = useI18n();

    const pqrs: Ref<IPqrs | null> = ref(null);
    const files = ref<File[]>([]);
    const fileInput = ref<HTMLInputElement | null>(null);
    const response: Ref<IRespuesta> = ref(new Respuesta());
    const newReply = ref('');
    const isLoading = ref(true);
    const isSendingReply = ref(false);
    const isUploading = ref(false);
    const dateFormat = useDateFormat();
    const { getToken } = useRecaptcha();

    const validations = useValidation();
    const validationRules = {
      contenido: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
    };
    const v$ = useVuelidate(validationRules, response as any);

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

    const effectiveAccessToken = computed<string | null>(() => {
      if (props.accessToken) {
        return props.accessToken;
      }
      if (route.params.accessToken) {
        return route.params.accessToken as string;
      }
      return null;
    });

    const retrievePqrs = async (token: string) => {
      isLoading.value = true;
      try {
        const res = await pqrsService().findPublicByAccessToken(token);
        pqrs.value = res;
      } catch (error: any) {
        if (error.response && (error.response.status === 404 || error.response.status === 400)) {
          alertService.showError(t$('ventanillaUnicaApp.pqrs.errors.notFound'));
        } else {
          alertService.showHttpError(error.response);
        }
        router.push({ name: 'Home' });
      } finally {
        isLoading.value = false;
      }
    };

    const sendReply = async (): Promise<void> => {
      try {
        if (isSendingReply.value) {
          return;
        }

        isSendingReply.value = true;
        const formData = new FormData();

        const responseDtoBlob = new Blob([JSON.stringify(response.value)], {
          type: 'application/json',
        });

        formData.append('response', responseDtoBlob);

        if (files.value.length > 0) {
          files.value.forEach(file => {
            formData.append('files', file);
          });
        }

        const accessToken = effectiveAccessToken.value;

        if (accessToken) {
          const recaptchaToken = await getToken('response_pulic_pqrs');
          const headers = { 'X-Recaptcha-Token': recaptchaToken };

          const savedResponse = await pqrsService().submitPublicResponse(accessToken, formData, headers);
          alertService.showSuccess(t$('ventanillaUnicaApp.respuesta.created', { param: savedResponse.id }).toString());
          await retrievePqrs(accessToken);
          resetForm();
        }
      } catch (error: any) {
        alertService.showHttpError(error.response ?? 'Ocurrió un error inesperado.');
      } finally {
        isSendingReply.value = false;
      }
    };

    const goBack = () => {
      router.push({ path: '/', replace: true });
    };

    const resetForm = () => {
      response.value = new Respuesta();
      files.value = [];

      if (fileInput.value) {
        fileInput.value.value = '';
      }

      v$.value.$reset();
    };

    const statusClass = computed(() => {
      if (!pqrs.value?.estado) return 'status-default';
      switch (pqrs.value.estado.toUpperCase()) {
        case PqrsStatus.Pending:
          return 'status-open';
        case PqrsStatus.Resolved:
          return 'status-closed';
        case PqrsStatus.InProcess:
          return 'status-in-progress';
        default:
          return 'status-default';
      }
    });

    onMounted(() => {
      const token = effectiveAccessToken.value;
      if (token) {
        retrievePqrs(token);
      } else {
        isLoading.value = false;
        alertService.showError(t$('ventanillaUnicaApp.pqrs.errors.idMissing'));
        goBack();
      }
    });

    // --- Expose to Template ---
    return {
      pqrs,
      newReply,
      isLoading,
      isSendingReply,
      PqrsStatus,
      onDrop,
      sendReply,
      goBack,
      onFileChange,
      triggerFileInput,
      removeFile,
      t$,
      statusClass,
      isUploading,
      files,
      fileInput,
      v$,
      ...dateFormat,
    };
  },
});
