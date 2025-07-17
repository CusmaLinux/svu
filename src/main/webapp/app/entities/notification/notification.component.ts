import { type Ref, defineComponent, inject, onMounted, ref, watch, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import NotificationService from './notification.service';
import { type INotification } from '@/shared/model/notification.model';
import useDataUtils from '@/shared/data/data-utils.service';
import { useDateFormat } from '@/shared/composables';
import { useAlertService } from '@/shared/alert/alert.service';
import { NotificationType } from '@/constants';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'Notifications',
  setup() {
    const { t: t$ } = useI18n();
    const dateFormat = useDateFormat();
    const dataUtils = useDataUtils();
    const notificationService = inject('notificationService', () => new NotificationService());
    const alertService = inject('alertService', () => useAlertService(), true);
    const router = useRouter();

    const notifications: Ref<INotification[]> = ref([]);
    const selectedNotificationIds = ref<string[]>([]);
    const itemsPerPage = ref(20);
    const queryCount: Ref<number | null> = ref(null);
    const page: Ref<number> = ref(1);
    const propOrder = ref('fecha');
    const reverse = ref(true);
    const totalItems = ref(0);
    const isFetching = ref(false);
    const isSaving = ref(false);
    const isDeleting = ref(false);
    const bulkDelete = ref(false);

    const removeNotificationModalRef = ref<any>(null);

    let notificationIdToDelete: string | null = null;

    const clear = () => {
      page.value = 1;
    };

    const sort = (): Array<any> => {
      const result = [`${propOrder.value},${reverse.value ? 'desc' : 'asc'}`];
      if (propOrder.value !== 'id') {
        result.push('id');
      }
      return result;
    };

    const retrieveNotifications = async () => {
      isFetching.value = true;
      try {
        const paginationQuery = { page: page.value - 1, size: itemsPerPage.value, sort: sort() };
        const res = await notificationService().retrieve(paginationQuery);
        totalItems.value = Number(res.headers['x-total-count']);
        queryCount.value = totalItems.value;
        notifications.value = res.data;
      } catch (err: any) {
        alertService.showHttpError(err.response);
      } finally {
        isFetching.value = false;
      }
    };

    const handleSyncList = () => {
      clear();
      retrieveNotifications();
    };

    onMounted(async () => {
      await retrieveNotifications();
    });

    const changeOrder = (newOrder: string) => {
      if (propOrder.value === newOrder) {
        reverse.value = !reverse.value;
      } else {
        reverse.value = false;
      }
      propOrder.value = newOrder;
    };

    watch([propOrder, reverse], () => {
      handleSyncList();
    });
    watch(page, () => retrieveNotifications());

    const isAnySelected = computed(() => selectedNotificationIds.value.length > 0);

    const isAllSelected = computed({
      get: () => {
        const pageIds = notifications.value.map(n => n.id);
        return pageIds.length > 0 && pageIds.every(id => selectedNotificationIds.value.includes(id));
      },
      set: value => {
        const pageIds = notifications.value.map(n => n.id);
        if (value) {
          selectedNotificationIds.value = [...new Set([...selectedNotificationIds.value, ...pageIds])];
        } else {
          selectedNotificationIds.value = selectedNotificationIds.value.filter(id => !pageIds.includes(id));
        }
      },
    });

    const saveChanges = async () => {
      isSaving.value = true;
      try {
        const promises = selectedNotificationIds.value.map(id => notificationService().markAsRead(id));
        await Promise.all(promises);
        notifications.value.forEach(n => {
          if (selectedNotificationIds.value.includes(n.id)) n.leido = true;
        });
        alertService.showSuccess(t$('ventanillaUnicaApp.notification.changesSaved', 'Cambios guardados con éxito'));
        selectedNotificationIds.value = [];
      } catch (err: any) {
        alertService.showHttpError(err.response);
      } finally {
        isSaving.value = false;
      }
    };

    const handleViewClick = async (notification: INotification) => {
      if (!notification.leido) {
        try {
          await notificationService().markAsRead(notification.id);
          const notifInList = notifications.value.find(n => n.id === notification.id);
          if (notifInList) notifInList.leido = true;
        } catch (err) {
          alertService.showError('No se ha podido marcar como leído.');
        }
      }
      router.push({ name: 'NotificacionView', params: { notificacionId: notification.id } });
    };

    const closeDialog = () => {
      removeNotificationModalRef.value?.hide();
    };

    const prepareRemove = (notification: INotification) => {
      notificationIdToDelete = notification.id;
      bulkDelete.value = false;
      removeNotificationModalRef.value?.show();
    };

    const prepareRemoveSelected = () => {
      bulkDelete.value = true;
      removeNotificationModalRef.value?.show();
    };

    const deleteNotifications = async () => {
      isDeleting.value = true;
      try {
        const idsToDelete = bulkDelete.value ? selectedNotificationIds.value : [notificationIdToDelete];
        const deletePromises = idsToDelete.map(id => notificationService().delete(id!));
        await Promise.all(deletePromises);

        if (idsToDelete.length > 1) {
          alertService.showSuccess('Notificaciones eliminadas');
        } else if (bulkDelete.value) {
          alertService.showSuccess(t$('ventanillaUnicaApp.notification.deleted', { param: idsToDelete[0] }));
        } else {
          alertService.showSuccess(t$('ventanillaUnicaApp.notification.deleted', { param: notificationIdToDelete }));
        }

        closeDialog();
        handleSyncList();
        selectedNotificationIds.value = [];
      } catch (err: any) {
        alertService.showHttpError(err.response);
      } finally {
        isDeleting.value = false;
      }
    };

    return {
      notifications,
      handleSyncList,
      isFetching,
      ...dateFormat,
      itemsPerPage,
      queryCount,
      page,
      propOrder,
      reverse,
      totalItems,
      NotificationType,
      changeOrder,
      t$,
      ...dataUtils,
      isSaving,
      isDeleting,
      selectedNotificationIds,
      isAnySelected,
      isAllSelected,
      saveChanges,
      handleViewClick,
      removeNotificationModalRef,
      bulkDelete,
      prepareRemove,
      prepareRemoveSelected,
      closeDialog,
      deleteNotifications,
    };
  },
});
