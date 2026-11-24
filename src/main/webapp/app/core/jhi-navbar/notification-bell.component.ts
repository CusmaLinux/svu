import { defineComponent, computed } from 'vue';
import type { NotificationItem } from '@/shared/config/store/notification-store';
import { useNotificationStore } from '@//shared/config/store/notification-store';
import { useRouter } from 'vue-router';
import { NotificationType } from '@/constants';
import useDataUtils from '@/shared/data/data-utils.service';
import { useI18n } from 'vue-i18n';
import NotificationService from '@/entities/notification/notification.service';

export default defineComponent({
  name: 'NotificationBell',
  setup() {
    const notificationStore = useNotificationStore();
    const router = useRouter();
    const dataUtils = useDataUtils();
    const inbox = new NotificationService();

    const notificationsToDisplay = computed<NotificationItem[]>(() =>
      notificationStore.persistentNotifications
        .slice()
        .sort((a, b) => new Date(b.creationDate || 0).getTime() - new Date(a.creationDate || 0).getTime())
        .slice(0, 10),
    );
    const unreadCount = computed(() => notificationStore.unreadCount);
    const isLoading = computed(() => notificationStore.isLoading);

    const isNotificationEntityGenerated = computed(() => router.hasRoute('Notifications'));

    const handleNotificationClick = async (notification: NotificationItem) => {
      if (notification.id && !notification.read) {
        notificationStore.markAsRead(notification.id);
      }

      if (!notification.id) return;
      try {
        const caseId = await inbox.findCaseId(notification.id);
        router.push({ name: 'PqrsView', params: { pqrsId: caseId }, replace: true });
      } catch {
        router.push({ name: 'NotificacionView', params: { notificacionId: notification.id } });
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
      const message = notification.message || '';
      return message.length > length ? message.substring(0, length) + '...' : message;
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
