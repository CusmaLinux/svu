import { defineComponent, ref, inject, type Ref } from 'vue';
import { useRouter } from 'vue-router';
import { useVuelidate } from '@vuelidate/core';
import { useValidation } from '@/shared/composables';
import { useRecaptcha } from '@/shared/composables/use-recaptcha';
import PqrsService from '@/entities/pqrs/pqrs.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { type IPqrs, Pqrs } from '@/shared/model/pqrs.model';

export default defineComponent({
  name: 'ConsultRequirement',
  compatConfig: { MODE: 3 },
  setup() {
    const router = useRouter();
    const requirement: Ref<IPqrs> = ref(new Pqrs());
    const isConsulting = ref(false);
    const pqrsService = inject('pqrsService', () => new PqrsService());
    const alertService = inject('alertService', () => useAlertService(), true);
    const { getToken } = useRecaptcha();

    const validations = useValidation();
    const validationRules = {
      fileNumber: {
        required: validations.required('Es necesario añadir un número de radicado'),
      },
    };

    const consult = async (): Promise<void> => {
      try {
        if (isConsulting.value) {
          return;
        }

        isConsulting.value = true;

        if (requirement.value.fileNumber) {
          const recaptchaToken = await getToken('consult_pulic_pqrs');
          const headers = { 'X-Recaptcha-Token': recaptchaToken };

          const accessToken = await pqrsService().retrieveAccessTokenByFileNumber(requirement.value.fileNumber, headers);
          alertService.showSuccess('Consulta de requerimiento exitosa');
          router.push({ name: 'PublicPqrsDetails', params: { accessToken: accessToken } });
        }
      } catch (error: any) {
        alertService.showHttpError(error.response ?? 'Ocurrió un error inesperado.');
      } finally {
        isConsulting.value = false;
      }
    };

    const previousState = () => {
      router.go(-1);
    };

    const v$ = useVuelidate(validationRules, requirement as any);

    return { previousState, v$, isConsulting, consult };
  },
});
