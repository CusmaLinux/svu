import { type Ref, computed, defineComponent, inject, onMounted, ref } from 'vue';
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
    const oficinaService = inject('oficinaService', () => new OficinaService());

    const informePqrs: Ref<IInformePqrs> = ref(new InformePqrs());
    const oficinas: Ref<IOficina[]> = ref([]);
    const isSaving = ref(false);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'es'), true);

    const route = useRoute();
    const router = useRouter();
    const { t: t$ } = useI18n();
    const validations = useValidation();
    const dateFormatUtils = useDateFormat({ entityRef: informePqrs });

    const page: Ref<number> = ref(1);
    const itemsPerPage: Ref<number> = ref(200);
    const propOrder = ref('nombre');
    const reverse = ref(false);

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

    const sort = (): Array<any> => {
      const result = [`${propOrder.value},${reverse.value ? 'desc' : 'asc'}`];
      if (propOrder.value !== 'id') {
        result.push('id');
      }
      return result;
    };

    const initRelationships = () => {
      const paginationQuery = {
        page: page.value - 1,
        size: itemsPerPage.value,
        sort: sort(),
      };

      oficinaService()
        .retrieve(paginationQuery)
        .then(res => {
          oficinas.value = res.data;
        })
        .catch(error => {
          alertService.showHttpError(error.response);
        });
    };

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

    const save = () => {
      isSaving.value = true;
      if (informePqrs.value.id) {
        informePqrsService()
          .update(informePqrs.value)
          .then(param => {
            isSaving.value = false;
            previousState();
            alertService.showInfo(t$('ventanillaUnicaApp.informePqrs.updated', { param: param.id }));
          })
          .catch(error => {
            isSaving.value = false;
            alertService.showHttpError(error.response);
          });
      } else {
        informePqrsService()
          .create(informePqrs.value)
          .then(param => {
            isSaving.value = false;
            previousState();
            alertService.showSuccess(t$('ventanillaUnicaApp.informePqrs.created', { param: param.id }).toString());
          })
          .catch(error => {
            isSaving.value = false;
            alertService.showHttpError(error.response);
          });
      }
    };

    onMounted(() => {
      initRelationships();
      if (route.params?.informePqrsId) {
        retrieveInformePqrs(route.params.informePqrsId);
      }
    });

    return {
      informePqrsService,
      alertService,
      informePqrs,
      previousState,
      isSaving,
      currentLanguage,
      oficinas,
      v$,
      ...dateFormatUtils,
      t$,
      save,
    };
  },
});
