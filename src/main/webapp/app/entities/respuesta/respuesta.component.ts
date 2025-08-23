import { type Ref, defineComponent, inject, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';

import RespuestaService from './respuesta.service';
import { type IRespuesta } from '@/shared/model/respuesta.model';
import useDataUtils from '@/shared/data/data-utils.service';
import { useDateFormat } from '@/shared/composables';
import { useAlertService } from '@/shared/alert/alert.service';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'Respuesta',
  setup() {
    const { t: t$ } = useI18n();
    const dateFormat = useDateFormat();
    const dataUtils = useDataUtils();
    const respuestaService = inject('respuestaService', () => new RespuestaService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const itemsPerPage = ref(20);
    const queryCount: Ref<number> = ref(null);
    const page: Ref<number> = ref(1);
    const propOrder = ref('fechaRespuesta');
    const reverse = ref(true);
    const totalItems = ref(0);

    const searchQuery: Ref<string> = ref('');

    const respuestas: Ref<IRespuesta[]> = ref([]);
    const isFetching = ref(false);

    const sort = (): Array<any> => {
      const result = [`${propOrder.value},${reverse.value ? 'desc' : 'asc'}`];
      if (propOrder.value !== 'id') {
        result.push('id');
      }
      return result;
    };

    const retrieveRespuestas = async () => {
      isFetching.value = true;
      try {
        const paginationQuery = {
          page: page.value - 1,
          size: itemsPerPage.value,
          sort: sort(),
        };
        const res = await respuestaService().search(paginationQuery, searchQuery.value.trim());

        totalItems.value = Number(res.headers['x-total-count']);
        queryCount.value = totalItems.value;
        respuestas.value = res.data;
      } catch (err: any) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrieveRespuestas();
    };

    onMounted(async () => {
      await retrieveRespuestas();
    });

    const removeId: Ref<string> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: IRespuesta) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeRespuesta = async () => {
      try {
        await respuestaService().delete(removeId.value);
        const message = t$('ventanillaUnicaApp.respuesta.deleted', { param: removeId.value }).toString();
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveRespuestas();
        closeDialog();
      } catch (error: any) {
        alertService.showHttpError(error.response);
      }
    };

    const changeOrder = (newOrder: string) => {
      if (propOrder.value === newOrder) {
        reverse.value = !reverse.value;
      } else {
        reverse.value = false;
      }
      propOrder.value = newOrder;
      page.value = 1;
      retrieveRespuestas();
    };

    const clearSearch = () => {
      searchQuery.value = '';
    };

    const debouncedSearch = dataUtils.debounce(() => {
      if (page.value !== 1) {
        page.value = 1;
      } else {
        retrieveRespuestas();
      }
    }, 500);

    watch(searchQuery, () => {
      debouncedSearch();
    });

    watch(page, async () => {
      await retrieveRespuestas();
    });

    return {
      respuestas,
      handleSyncList,
      isFetching,
      retrieveRespuestas,
      ...dateFormat,
      removeId,
      removeEntity,
      prepareRemove,
      closeDialog,
      removeRespuesta,
      itemsPerPage,
      queryCount,
      page,
      propOrder,
      reverse,
      totalItems,
      changeOrder,
      t$,
      ...dataUtils,
      searchQuery,
      clearSearch,
    };
  },
});
