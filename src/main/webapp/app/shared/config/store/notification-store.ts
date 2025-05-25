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
      const sseNotificationItem: NotificationItem = {
        id: `sse-${Date.now()}-${Math.random()}`,
        pqrsId: eventData.id,
        type: eventData.type || 'PQRS_DUE_DATE_REMINDER',
        message: eventData.message || `PQRS '${eventData.title}' is due soon.`,
        pqrsTitle: eventData.title,
        pqrsResponseDueDate: eventData.pqrsResponseDueDate,
        creationDate: new Date(),
        read: false,
        isSse: true,
      };
      this.sseNotifications.unshift(sseNotificationItem);

      if (this.sseNotifications.length > 10) {
        this.sseNotifications.pop();
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
        this.persistentNotifications = notifications.map((n: INotification) => ({
          id: n.id,
          pqrsId: dataUtils.extractIdFromMessage(n.mensaje),
          type: n.tipo,
          message: n.mensaje,
          pqrsTitle: dataUtils.extractPqrsTitle(n.mensaje),
          pqrsResponseDueDate: dataUtils.extractLastParameterBeforeDot(n.mensaje),
          creationDate: n.fecha,
          read: n.leido,
          isSse: false,
        }));
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
