import { type Ref, defineComponent, inject, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';

import OficinaService from './oficina.service';
import { type IOficina } from '@/shared/model/oficina.model';
import { useAlertService } from '@/shared/alert/alert.service';

import useDataUtils from '@/shared/data/data-utils.service';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'Oficina',
  setup() {
    const { t: t$ } = useI18n();
    const oficinaService = inject('oficinaService', () => new OficinaService());
    const alertService = inject('alertService', () => useAlertService(), true);
    const oficinas: Ref<IOficina[]> = ref([]);
    const isFetching = ref(false);

    const dataUtils = useDataUtils();
    const searchQuery: Ref<string> = ref('');
    const page: Ref<number> = ref(1);
    const itemsPerPage = ref(20);
    const queryCount: Ref<number | null> = ref(null);
    const propOrder = ref('nombre');
    const reverse = ref(true);
    const totalItems = ref(0);

    const retrieveOficinas = async () => {
      isFetching.value = true;
      try {
        const paginationQuery = {
          page: page.value - 1,
          size: itemsPerPage.value,
          sort: sort(),
        };
        const res = await oficinaService().search(paginationQuery, searchQuery.value.trim());
        totalItems.value = Number(res.headers['x-total-count']);
        queryCount.value = totalItems.value;
        oficinas.value = res.data;
      } catch (err: any) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrieveOficinas();
    };

    onMounted(async () => {
      await retrieveOficinas();
    });

    const removeId: Ref<string | undefined | null> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: IOficina) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeOficina = async () => {
      try {
        await oficinaService().delete(removeId.value);
        const message = t$('ventanillaUnicaApp.oficina.deleted', { param: removeId.value }).toString();
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveOficinas();
        closeDialog();
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

    const changeOrder = (newOrder: string) => {
      if (propOrder.value === newOrder) {
        reverse.value = !reverse.value;
      } else {
        reverse.value = false;
      }
      propOrder.value = newOrder;

      page.value = 1;
      retrieveOficinas();
    };

    const clearSearch = () => {
      searchQuery.value = '';
    };

    const debouncedSearch = dataUtils.debounce(() => {
      if (page.value !== 1) {
        page.value = 1;
      } else {
        retrieveOficinas();
      }
    }, 500);

    watch(searchQuery, () => {
      debouncedSearch();
    });

    watch(page, async () => {
      await retrieveOficinas();
    });

    return {
      oficinas,
      handleSyncList,
      isFetching,
      retrieveOficinas,
      clearSearch,
      changeOrder,
      removeId,
      removeEntity,
      searchQuery,
      page,
      queryCount,
      itemsPerPage,
      totalItems,
      prepareRemove,
      closeDialog,
      removeOficina,
      t$,
    };
  },
});
