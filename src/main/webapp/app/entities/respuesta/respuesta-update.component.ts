import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { useVuelidate } from '@vuelidate/core';

import RespuestaService from './respuesta.service';
import useDataUtils from '@/shared/data/data-utils.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import { useAlertService } from '@/shared/alert/alert.service';

import PqrsService from '@/entities/pqrs/pqrs.service';
import { type IPqrs } from '@/shared/model/pqrs.model';
import { type IRespuesta, Respuesta } from '@/shared/model/respuesta.model';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'RespuestaUpdate',
  setup() {
    const respuestaService = inject('respuestaService', () => new RespuestaService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const respuesta: Ref<IRespuesta> = ref(new Respuesta());

    const pqrsService = inject('pqrsService', () => new PqrsService());

    const pqrs: Ref<IPqrs[]> = ref([]);
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'es'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveRespuesta = async respuestaId => {
      try {
        const res = await respuestaService().find(respuestaId);
        res.fechaRespuesta = new Date(res.fechaRespuesta);
        respuesta.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.respuestaId) {
      retrieveRespuesta(route.params.respuestaId);
    }

    const initRelationships = () => {
      pqrsService()
        .retrieve()
        .then(res => {
          pqrs.value = res.data;
        });
    };

    initRelationships();

    const dataUtils = useDataUtils();

    const { t: t$ } = useI18n();
    const validations = useValidation();
    const validationRules = {
      contenido: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      fechaRespuesta: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      pqr: {},
    };
    const v$ = useVuelidate(validationRules, respuesta as any);
    v$.value.$validate();

    return {
      respuestaService,
      alertService,
      respuesta,
      previousState,
      isSaving,
      currentLanguage,
      pqrs,
      ...dataUtils,
      v$,
      ...useDateFormat({ entityRef: respuesta }),
      t$,
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.respuesta.id) {
        this.respuestaService()
          .update(this.respuesta)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(this.t$('ventanillaUnicaApp.respuesta.updated', { param: param.id }));
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.respuestaService()
          .create(this.respuesta)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(this.t$('ventanillaUnicaApp.respuesta.created', { param: param.id }).toString());
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
