import { type Ref, defineComponent, inject, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';

import PqrsService from './pqrs.service';
import { type IPqrs } from '@/shared/model/pqrs.model';
import useDataUtils from '@/shared/data/data-utils.service';
import { useDateFormat } from '@/shared/composables';
import { useAlertService } from '@/shared/alert/alert.service';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'Pqrs',
  setup() {
    const { t: t$ } = useI18n();
    const dateFormat = useDateFormat();
    const dataUtils = useDataUtils();
    const pqrsService = inject('pqrsService', () => new PqrsService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const itemsPerPage = ref(20);
    const queryCount: Ref<number | null> = ref(null);
    const page: Ref<number> = ref(1);
    const propOrder = ref('fecha_creacion');
    const reverse = ref(true);
    const totalItems = ref(0);

    const pqrs: Ref<IPqrs[]> = ref([]);

    const isFetching = ref(false);
    const searchQuery: Ref<string> = ref('');

    const clear = () => {
      page.value = 1;
      searchQuery.value = '';
    };

    const sort = (): Array<any> => {
      const result = [`${propOrder.value},${reverse.value ? 'desc' : 'asc'}`];
      if (propOrder.value !== 'id') {
        result.push('id');
      }
      return result;
    };

    const retrievePqrss = async () => {
      isFetching.value = true;
      try {
        const paginationQuery = {
          page: page.value - 1,
          size: itemsPerPage.value,
          sort: sort(),
        };
        const res = await pqrsService().search(paginationQuery, searchQuery.value.trim());
        totalItems.value = Number(res.headers['x-total-count']);
        queryCount.value = totalItems.value;
        pqrs.value = res.data;
      } catch (err: any) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      retrievePqrss();
    };

    onMounted(async () => {
      await retrievePqrss();
    });

    const removeId: Ref<string | null | undefined> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: IPqrs) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removePqrs = async () => {
      try {
        if (removeId.value) await pqrsService().delete(removeId.value);
        const message = t$('ventanillaUnicaApp.pqrs.deleted', { param: removeId.value }).toString();
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrievePqrss();
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
      retrievePqrss();
    };

    const clearSearch = () => {
      searchQuery.value = '';
    };

    const debouncedSearch = dataUtils.debounce(() => {
      if (page.value !== 1) {
        page.value = 1;
      } else {
        retrievePqrss();
      }
    }, 500);

    watch(searchQuery, () => {
      debouncedSearch();
    });

    watch(page, async () => {
      await retrievePqrss();
    });

    return {
      pqrs,
      handleSyncList,
      isFetching,
      retrievePqrss,
      clear,
      ...dateFormat,
      removeId,
      removeEntity,
      prepareRemove,
      closeDialog,
      removePqrs,
      searchQuery,
      clearSearch,
      itemsPerPage,
      queryCount,
      page,
      propOrder,
      reverse,
      totalItems,
      changeOrder,
      t$,
      ...dataUtils,
    };
  },
});
