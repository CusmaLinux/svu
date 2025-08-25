import { type Ref, computed, defineComponent, inject, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { useVuelidate } from '@vuelidate/core';

import OficinaService from './oficina.service';
import UserService from '@/entities/user/user.service';
import { useValidation } from '@/shared/composables';
import { useAlertService } from '@/shared/alert/alert.service';

import { type IOficina, Oficina } from '@/shared/model/oficina.model';
import { type IUser } from '@/shared/model/user.model';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'OficinaUpdate',
  setup() {
    const oficinaService = inject('oficinaService', () => new OficinaService());
    const userService = inject('userService', () => new UserService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const oficina: Ref<IOficina> = ref(new Oficina());
    const users: Ref<IUser[]> = ref([]);
    const isSaving = ref(false);
    const officeSelect: Ref<string | null> = ref(null);
    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'es'), true);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);

    const retrieveOficina = async (oficinaId: string | string[]) => {
      try {
        const res = await oficinaService().find(oficinaId);
        oficina.value = res;
      } catch (error: any) {
        alertService.showHttpError(error.response);
      }
    };

    const { t: t$ } = useI18n();
    const validations = useValidation();
    const validationRules = {
      nombre: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      descripcion: {},
      nivel: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
      oficinaSuperior: {},
      responsableDTO: {
        required: validations.required(t$('entity.validation.required').toString()),
      },
    };
    const v$ = useVuelidate(validationRules, oficina as any);
    const page: Ref<number> = ref(1);
    const propOrder = ref('createdDate');
    const reverse = ref(true);
    const itemsPerPage = ref(200);

    const sort = (): Array<any> => {
      const result = [`${propOrder.value},${reverse.value ? 'desc' : 'asc'}`];
      if (propOrder.value !== 'id') {
        result.push('id');
      }
      return result;
    };
    const loadUsers = async () => {
      try {
        const paginationQuery = {
          page: page.value - 1,
          size: itemsPerPage.value,
          sort: sort(),
        };
        const response = await userService().retrieve(paginationQuery);
        users.value = response.data;
      } catch (error: any) {
        alertService.showHttpError(error.response);
      }
    };

    const save = () => {
      isSaving.value = true;
      if (oficina.value.id) {
        oficinaService()
          .update(oficina.value)
          .then(param => {
            isSaving.value = false;
            previousState();
            alertService.showInfo(t$('ventanillaUnicaApp.oficina.updated', { param: param.id }));
          })
          .catch(error => {
            isSaving.value = false;
            alertService.showHttpError(error.response);
          });
      } else {
        oficinaService()
          .create(oficina.value)
          .then(param => {
            isSaving.value = false;
            previousState();
            alertService.showSuccess(t$('ventanillaUnicaApp.oficina.created', { param: param.id }).toString());
          })
          .catch(error => {
            isSaving.value = false;
            alertService.showHttpError(error.response);
          });
      }
    };

    onMounted(() => {
      loadUsers();
      if (route.params?.oficinaId) {
        retrieveOficina(route.params.oficinaId);
      }
    });

    return {
      oficinaService,
      alertService,
      oficina,
      previousState,
      save,
      loadUsers,
      users,
      isSaving,
      currentLanguage,
      officeSelect,
      v$,
      t$,
    };
  },
});
