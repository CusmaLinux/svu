import { type Ref, defineComponent, inject, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';

import ArchivoAdjuntoService from './archivo-adjunto.service';
import { type IArchivoAdjunto } from '@/shared/model/archivo-adjunto.model';
import { useAlertService } from '@/shared/alert/alert.service';
import useDataUtils from '@/shared/data/data-utils.service';
import { useDateFormat } from '@/shared/composables';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'ArchivoAdjunto',
  setup() {
    const { t: t$ } = useI18n();
    const { formatDateShort } = useDateFormat();
    const archivoAdjuntoService = inject('archivoAdjuntoService', () => new ArchivoAdjuntoService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const archivoAdjuntos: Ref<IArchivoAdjunto[]> = ref([]);
    const isFetching = ref(false);

    const dataUtils = useDataUtils();
    const searchQuery: Ref<string> = ref('');
    const page: Ref<number> = ref(1);
    const itemsPerPage = ref(20);
    const queryCount: Ref<number | null> = ref(null);
    const propOrder = ref('id');
    const reverse = ref(false);
    const totalItems = ref(0);

    const clear = () => {
      page.value = 1;
      searchQuery.value = '';
      retrieveArchivoAdjuntos();
    };

    const retrieveArchivoAdjuntos = async () => {
      isFetching.value = true;
      try {
        const paginationQuery = {
          page: page.value - 1,
          size: itemsPerPage.value,
          sort: sort(),
        };
        const res = await archivoAdjuntoService().search(paginationQuery, searchQuery.value.trim());
        totalItems.value = Number(res.headers['x-total-count']);
        queryCount.value = totalItems.value;
        archivoAdjuntos.value = res.data;
      } catch (err: any) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      clear();
    };

    onMounted(async () => {
      await retrieveArchivoAdjuntos();
    });

    const removeId: Ref<number | null> = ref(null);
    const removeEntity = ref<any>(null);
    const prepareRemove = (instance: IArchivoAdjunto) => {
      removeId.value = instance.id;
      removeEntity.value.show();
    };
    const closeDialog = () => {
      removeEntity.value.hide();
    };
    const removeArchivoAdjunto = async () => {
      try {
        await archivoAdjuntoService().delete(removeId.value);
        const message = t$('ventanillaUnicaApp.archivoAdjunto.deleted', { param: removeId.value }).toString();
        alertService.showInfo(message, { variant: 'danger' });
        removeId.value = null;
        retrieveArchivoAdjuntos(); // Refresh list after deleting
        closeDialog();
      } catch (error: any) {
        alertService.showHttpError(error.response);
      }
    };

    const sort = (): Array<any> => {
      const result = [`${propOrder.value},${reverse.value ? 'asc' : 'desc'}`];
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
      retrieveArchivoAdjuntos();
    };

    const clearSearch = () => {
      searchQuery.value = '';
    };

    const debouncedSearch = dataUtils.debounce(() => {
      if (page.value !== 1) {
        page.value = 1;
      } else {
        retrieveArchivoAdjuntos();
      }
    }, 500);

    watch(searchQuery, () => {
      debouncedSearch();
    });

    watch(page, async () => {
      await retrieveArchivoAdjuntos();
    });

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

    return {
      t$,
      formatDateShort,
      archivoAdjuntos,
      handleSyncList,
      isFetching,
      retrieveArchivoAdjuntos,
      clear,
      prepareRemove,
      removeId,
      removeEntity,
      closeDialog,
      removeArchivoAdjunto,
      searchQuery,
      page,
      itemsPerPage,
      queryCount,
      totalItems,
      clearSearch,
      changeOrder,
      downloadAttachedFile,
    };
  },
});
