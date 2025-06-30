import { type Ref, computed, defineComponent, inject, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { useVuelidate } from '@vuelidate/core';

import InformePqrsService from './informe-pqrs.service';
import { useDateFormat, useValidation } from '@/shared/composables';
import { useAlertService } from '@/shared/alert/alert.service';

import OficinaService from '@/entities/oficina/oficina.service';
import { type IOficina } from '@/shared/model/oficina.model';
import { type IInformePqrs, InformePqrs } from '@/shared/model/informe-pqrs.model';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'InformePqrsUpdate',
  setup() {
    const informePqrsService = inject('informePqrsService', () => new InformePqrsService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const informePqrs: Ref<IInformePqrs> = ref(new InformePqrs());

    const oficinaService = inject('oficinaService', () => new OficinaService());

    const oficinas: Ref<IOficina[]> = ref([]);
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'es'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveInformePqrs = async (informePqrsId: string | string[]) => {
      try {
        const res = await informePqrsService().find(informePqrsId);
        res.fechaInicio = new Date(res.fechaInicio);
        res.fechaFin = new Date(res.fechaFin);
        informePqrs.value = res;
      } catch (error: any) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.informePqrsId) {
      retrieveInformePqrs(route.params.informePqrsId);
    }

    const initRelationships = () => {
      oficinaService()
        .retrieve()
        .then(res => {
          oficinas.value = res.data;
        });
    };

    initRelationships();

    const { t: t$ } = useI18n();
    const validations = useValidation();
    const validationRules = {
      fechaInicio: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      fechaFin: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      oficina: {},
    };
    const v$ = useVuelidate(validationRules, informePqrs as any);
    v$.value.$validate();

    return {
      informePqrsService,
      alertService,
      informePqrs,
      previousState,
      isSaving,
      currentLanguage,
      oficinas,
      v$,
      ...useDateFormat({ entityRef: informePqrs }),
      t$,
    };
  },
  created(): void {},
  methods: {
    save(): void {
      this.isSaving = true;
      if (this.informePqrs.id) {
        this.informePqrsService()
          .update(this.informePqrs)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showInfo(this.t$('ventanillaUnicaApp.informePqrs.updated', { param: param.id }));
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      } else {
        this.informePqrsService()
          .create(this.informePqrs)
          .then(param => {
            this.isSaving = false;
            this.previousState();
            this.alertService.showSuccess(this.t$('ventanillaUnicaApp.informePqrs.created', { param: param.id }).toString());
          })
          .catch(error => {
            this.isSaving = false;
            this.alertService.showHttpError(error.response);
          });
      }
    },
  },
});
