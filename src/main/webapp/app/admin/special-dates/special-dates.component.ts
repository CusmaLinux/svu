import { type Ref, defineComponent, inject, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';

import SpecialDatesService from './special-dates.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { type ISpecialDate } from '@/shared/model/special-date.model';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'SpecialDates',
  setup() {
    const { t: t$ } = useI18n();
    const specialDatesService = inject('specialDatesService', () => new SpecialDatesService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const specialDates: Ref<ISpecialDate[]> = ref([]);
    const isFetching = ref(false);
    const formCard = ref<any>(null);

    const itemsPerPage = ref(20);
    const queryCount: Ref<number | null> = ref(null);
    const page: Ref<number> = ref(1);
    const propOrder = ref('date');
    const reverse = ref(false);
    const totalItems = ref(0);

    const isUpdating = ref(false);
    const specialDateForm: Ref<ISpecialDate> = ref({ date: null, description: '' });

    const removeEntity = ref<any>(null);
    const specialDateToRemove: Ref<ISpecialDate | null> = ref(null);

    const tableFields = [
      { key: 'date', label: 'Fecha' },
      { key: 'description', label: 'Descripción' },
      { key: 'actions', label: 'Acciones', class: 'text-right' },
    ];

    const sort = (): Array<string> => {
      const result = [`${propOrder.value},${reverse.value ? 'desc' : 'asc'}`];
      if (propOrder.value !== 'id') {
        result.push('id,asc');
      }
      return result;
    };

    const loadAll = async () => {
      isFetching.value = true;
      try {
        const paginationQuery = {
          page: page.value - 1,
          size: itemsPerPage.value,
          sort: sort(),
        };
        const res = await specialDatesService().retrieve(paginationQuery);
        totalItems.value = Number(res.headers['x-total-count']);
        queryCount.value = totalItems.value;
        specialDates.value = res.data;
      } catch (err: any) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      loadAll();
    };

    watch(page, () => {
      loadAll();
    });

    onMounted(async () => {
      await loadAll();
    });

    const changeOrder = (newOrder: string) => {
      if (propOrder.value === newOrder) {
        reverse.value = !reverse.value;
      } else {
        reverse.value = false;
      }
      propOrder.value = newOrder;
      page.value = 1;
      loadAll();
    };

    const resetForm = () => {
      isUpdating.value = false;
      specialDateForm.value = { date: null, description: '' };
    };

    const handleUpdate = (instance: ISpecialDate) => {
      isUpdating.value = true;
      specialDateForm.value = { ...instance };
      if (formCard.value) {
        formCard.value.scrollIntoView({ behavior: 'smooth' });
      }
    };

    const saveSpecialDate = async () => {
      try {
        if (isUpdating.value) {
          await specialDatesService().update(specialDateForm.value);
          alertService.showSuccess('Día no hábil actualizado exitosamente.');
        } else {
          await specialDatesService().create(specialDateForm.value);
          alertService.showSuccess('Día no hábil creado exitosamente.');
        }
        resetForm();
        handleSyncList();
      } catch (err: any) {
        alertService.showHttpError(err.response);
      }
    };

    const prepareRemove = (instance: ISpecialDate) => {
      specialDateToRemove.value = instance;
      removeEntity.value.show();
    };

    const closeDeleteDialog = () => {
      removeEntity.value.hide();
    };

    const removeSpecialDate = async () => {
      try {
        if (specialDateToRemove.value?.id) {
          await specialDatesService().delete(specialDateToRemove.value.id);
          alertService.showInfo('Día no hábil eliminado.', { variant: 'danger' });
          closeDeleteDialog();
          if (page.value > 1 && specialDates.value.length === 1) {
            page.value--;
          }
          await loadAll();
        }
      } catch (error: any) {
        alertService.showHttpError(error.response);
      }
    };

    return {
      t$,
      specialDates,
      isFetching,
      tableFields,
      formCard,
      itemsPerPage,
      queryCount,
      page,
      propOrder,
      reverse,
      totalItems,
      handleSyncList,
      changeOrder,
      isUpdating,
      specialDateForm,
      resetForm,
      handleUpdate,
      saveSpecialDate,
      removeEntity,
      specialDateToRemove,
      prepareRemove,
      closeDeleteDialog,
      removeSpecialDate,
    };
  },
});
