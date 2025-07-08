import { defineComponent, computed } from 'vue';
import type { NotificationItem } from '@/shared/config/store/notification-store';
import { useNotificationStore } from '@//shared/config/store/notification-store';
import { useRouter } from 'vue-router';
import { NotificationType } from '@/constants';
import useDataUtils from '@/shared/data/data-utils.service';
import { useI18n } from 'vue-i18n';
import { useDateFormat } from '@/shared/composables';

export default defineComponent({
  name: 'NotificationBell',
  setup() {
    const notificationStore = useNotificationStore();
    const router = useRouter();
    const dataUtils = useDataUtils();
    const { t: t$ } = useI18n();
    const dateFormat = useDateFormat();

    const notificationsToDisplay = computed<NotificationItem[]>(() =>
      notificationStore.persistentNotifications
        .slice()
        .sort((a, b) => new Date(b.creationDate || 0).getTime() - new Date(a.creationDate || 0).getTime())
        .slice(0, 10),
    );
    const unreadCount = computed(() => notificationStore.unreadCount);
    const isLoading = computed(() => notificationStore.isLoading);

    const isNotificationEntityGenerated = computed(() => router.hasRoute('Notifications'));

    const handleNotificationClick = (notification: NotificationItem) => {
      if (notification.id && !notification.read) {
        notificationStore.markAsRead(notification.id);
      }

      if (notification.pqrsId) {
        router.push({ name: 'PqrsView', params: { pqrsId: notification.pqrsId }, replace: true });
      }
    };

    const markAllAsRead = () => {
      notificationStore.markAllPersistentAsRead();
    };

    const formatDateTime = (dateString?: string | Date): string => {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    const truncateMessage = (notification: NotificationItem, length: number): string => {
      const messageLang = getTranslationByNotificationType(notification);

      return messageLang.length > length ? messageLang.substring(0, length) + '...' : messageLang;
    };

    const getTranslationByNotificationType = (notification: NotificationItem) => {
      switch (notification.type) {
        case 'PQRS_DUE_DATE_REMINDER':
          return t$('sse-notification.due-date-message', {
            pqrsTitle: notification.pqrsTitle,
            pqrsId: notification.pqrsId,
            pqrsDueDate: dateFormat.formatDateLong(notification.pqrsResponseDueDate),
          });
        case 'PQRS_STATE_UPDATE':
          return t$('sse-notification.state-update-message', {
            pqrsTitle: notification.pqrsTitle,
            pqrsId: notification.pqrsId,
          });
        case 'PQRS_CREATED':
          return t$('sse-notification.created-message', {
            pqrsTitle: notification.pqrsTitle,
            pqrsId: notification.pqrsId,
          });
        case 'PQRS_ASSIGNED':
          return t$('sse-notification.assigned-message', {
            pqrsTitle: notification.pqrsTitle,
            pqrsId: notification.pqrsId,
          });
      }
      return t$('sse-notification.error');
    };

    return {
      notificationsToDisplay,
      unreadCount,
      isLoading,
      handleNotificationClick,
      markAllAsRead,
      formatDateTime,
      truncateMessage,
      isNotificationEntityGenerated,
      NotificationType,
      ...dataUtils,

      t$: useI18n().t,
    };
  },
});
