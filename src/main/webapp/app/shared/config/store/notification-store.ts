import { defineStore } from 'pinia';
import { type INotification } from '@/shared/model/notification.model';
import NotificationService from '@/entities/notification/notification.service';
import { useAccountStore } from './account-store';
import { logger } from '@/shared/logger';

const notificationService = new NotificationService();

export interface NotificationItem {
  id?: number | string;
  type: string;
  creationDate?: Date | string;
  message?: string | undefined;
  read?: boolean | null | undefined;
  isSse?: boolean;
}

export interface NotificationState {
  sseNotifications: NotificationItem[];
  persistentNotifications: NotificationItem[];
  totalUnread: number;
  isLoading: boolean;
  error: string | null;
}

export const useNotificationStore = defineStore('notification', {
  state: (): NotificationState => ({
    sseNotifications: [],
    persistentNotifications: [],
    totalUnread: 0,
    isLoading: false,
    error: null,
  }),
  getters: {
    allDisplayNotifications(state): NotificationItem[] {
      const combined = [
        ...state.persistentNotifications,
        ...state.sseNotifications.filter(sse => !state.persistentNotifications.find(p => p.id === sse.id && !p.isSse)),
      ];
      return combined.sort((a, b) => new Date(b.creationDate || 0).getTime() - new Date(a.creationDate || 0).getTime());
    },
    unreadCount(state): number {
      return state.totalUnread;
    },
  },
  actions: {
    addSseEvent(eventData: any) {
      const sseNotificationItem: NotificationItem = normalizeInput(eventData, true);
      if (sseNotificationItem) {
        this.sseNotifications.unshift(sseNotificationItem);
        if (this.sseNotifications.length > 10) {
          this.sseNotifications.pop();
        }
      }
      return sseNotificationItem;
    },

    async fetchUserNotifications() {
      if (this.isLoading) return;
      const identity = useAccountStore().account;
      this.isLoading = true;
      this.error = null;
      try {
        const paginationQuery = {
          page: 0,
          size: 10,
          sort: ['fecha,desc'],
          read: false,
        };
        const response = await notificationService.retrieve(paginationQuery);
        if (identity !== useAccountStore().account) return;
        this.totalUnread = Number(response.headers['x-total-count']);
        const notifications: INotification[] = response.data;
        const mappedNotifications: (NotificationItem | null)[] = notifications.map((n: INotification) => normalizeInput(n, false));
        this.persistentNotifications = mappedNotifications.filter((item): item is NotificationItem => item !== null);
      } catch (err: any) {
        if (identity !== useAccountStore().account) return;
        this.error = err.message || 'Failed to fetch notifications';
        logger.error('Error fetching notifications');
      } finally {
        if (identity === useAccountStore().account) this.isLoading = false;
      }
    },

    async markAsRead(notificationId: number | string) {
      const identity = useAccountStore().account;
      const notification = this.persistentNotifications.find(n => n.id === notificationId);
      if (notification && !notification.read) {
        try {
          await notificationService.markAsRead(notification.id);
          if (identity !== useAccountStore().account) return;
          notification.read = true;
          this.totalUnread -= 1;
        } catch (err) {
          logger.error(`Error marking notification ${notificationId} as read:`);
        }
      }
    },

    async markAllPersistentAsRead() {
      const identity = useAccountStore().account;
      try {
        await notificationService.markAllAsRead();
        if (identity !== useAccountStore().account) return;
        this.persistentNotifications.forEach(n => (n.read = true));
        this.totalUnread = 0;
      } catch (err) {
        logger.error('Error marking all notifications as read:');
      }
    },
  },
});

const normalizeInput = (notification: any, isSse: boolean): NotificationItem => {
  if (isSse) {
    const sseData = notification as NotificationItem;
    return {
      id: sseData.id,
      type: sseData.type,
      message: sseData.message,
      creationDate: sseData.creationDate,
      read: sseData.read,
      isSse,
    };
  } else {
    const notifyData = notification as INotification;
    return {
      id: notifyData.id,
      type: notifyData.tipo,
      message: notifyData.mensaje,
      creationDate: notifyData.fecha,
      read: notifyData.leido,
      isSse,
    };
  }
};
