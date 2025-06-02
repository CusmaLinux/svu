import { defineStore } from 'pinia';
import { type INotification } from '@/shared/model/notification.model';
import NotificationService from '@/entities/notification/notification.service';
import useDataUtils from '@/shared/data/data-utils.service';
import { logger } from '@/shared/logger';

const dataUtils = useDataUtils();
const notificationService = new NotificationService();

export interface NotificationItem {
  id?: number | string;
  type: string;
  creationDate?: Date | string;
  message?: string | undefined;
  read?: boolean | null | undefined;
  userLogin?: string;
  pqrsId?: string | undefined | null;
  pqrsTitle?: string | undefined | null;
  pqrsResponseDueDate?: Date | undefined | null;
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
        ...state.sseNotifications.filter(sse => !state.persistentNotifications.find(p => p.pqrsId === sse.pqrsId && !p.isSse)),
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
        this.totalUnread = Number(response.headers['x-total-count']);
        const notifications: INotification[] = response.data;
        const mappedNotifications: (NotificationItem | null)[] = notifications.map((n: INotification) => normalizeInput(n, false));
        this.persistentNotifications = mappedNotifications.filter((item): item is NotificationItem => item !== null);
      } catch (err: any) {
        this.error = err.message || 'Failed to fetch notifications';
        logger.error('Error fetching notifications', err);
      } finally {
        this.isLoading = false;
      }
    },

    async markAsRead(notificationId: number | string) {
      const notification = this.persistentNotifications.find(n => n.id === notificationId);
      if (notification && !notification.read) {
        try {
          await notificationService.markAsRead(notification.id);
          notification.read = true;
          this.totalUnread -= 1;
        } catch (err) {
          logger.error(`Error marking notification ${notificationId} as read:`, err);
        }
      }
    },

    async markAllPersistentAsRead() {
      try {
        await notificationService.markAllAsRead();
        this.persistentNotifications.forEach(n => (n.read = true));
        this.totalUnread = 0;
      } catch (err) {
        logger.error('Error marking all notifications as read:', err);
      }
    },
  },
});

const normalizeInput = (notification: any, isSse: boolean): NotificationItem => {
  if (isSse) {
    const sseData = notification as NotificationItem;
    return {
      id: sseData.id,
      pqrsId: sseData.pqrsId,
      type: sseData.type,
      message: sseData.message,
      pqrsTitle: sseData.pqrsTitle,
      creationDate: sseData.creationDate,
      pqrsResponseDueDate: sseData.pqrsResponseDueDate,
      read: sseData.read,
      isSse,
    };
  } else {
    const notifyData = notification as INotification;
    return {
      id: notifyData.id,
      pqrsId: dataUtils.extractIdFromMessage(notifyData.mensaje),
      type: notifyData.tipo,
      message: notifyData.mensaje,
      pqrsTitle: dataUtils.extractPqrsTitle(notifyData.mensaje),
      creationDate: notifyData.fecha,
      read: notifyData.leido,
      pqrsResponseDueDate: dataUtils.extractLastParameterBeforeDot(notifyData.mensaje),
      isSse,
    };
  }
};
