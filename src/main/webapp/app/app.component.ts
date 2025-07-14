import { defineComponent, onBeforeUnmount, onMounted, provide, watch, inject, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import Ribbon from '@/core/ribbon/ribbon.vue';
import JhiFooter from '@/core/jhi-footer/jhi-footer.vue';
import JhiNavbar from '@/core/jhi-navbar/jhi-navbar.vue';
import Footer from '@/core/footer/footer.vue';
import AuthFooter from './core/footer/auth-footer.vue';
import LoginForm from '@/account/login-form/login-form.vue';
import { useDateFormat } from '@/shared/composables';

import '@/shared/config/dayjs';

import { useAccountStore } from './shared/config/store/account-store';
import { useNotificationStore, type NotificationItem } from './shared/config/store/notification-store';
import useDataUtils from './shared/data/data-utils.service';

import sseNotificationService from './services/sse-notification.service';
import { useAlertService } from '@/shared/alert/alert.service';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'App',
  components: {
    ribbon: Ribbon,
    'jhi-navbar': JhiNavbar,
    'login-form': LoginForm,
    'jhi-footer': JhiFooter,
    'svu-footer': Footer,
    'svu-auth-footer': AuthFooter,
  },
  setup() {
    const accountStore = useAccountStore();
    const notificationStore = useNotificationStore();
    const alertService = inject('alertService', () => useAlertService(), true);
    const dataUtils = useDataUtils();
    const dateFormat = useDateFormat();
    const authenticated = computed(() => accountStore.authenticated);
    const { t } = useI18n();

    const manageSseConnection = () => {
      if (accountStore.authenticated) {
        const connectedUser = sseNotificationService.connect();
        if (connectedUser) {
          notificationStore.fetchUserNotifications();
        }

        if (Notification.permission !== 'granted' && Notification.permission !== 'denied') {
          Notification.requestPermission();
        }
      } else {
        sseNotificationService.disconnect();
      }
    };

    watch(
      () => accountStore.authenticated,
      newAuthStatus => {
        console.info('App.vue: Auth status changed to: ', newAuthStatus);
        manageSseConnection();
      },
    );

    watch(
      () => [...notificationStore.sseNotifications],
      (newSseItems, oldSseItems) => {
        const latestItem = newSseItems.find(newItem => !oldSseItems.some(oldItem => oldItem.id === newItem.id));

        if (latestItem && latestItem.isSse) {
          getAlertByNotificationType(latestItem);
        }
      },
      { deep: true },
    );

    onMounted(() => {
      manageSseConnection();
    });

    onBeforeUnmount(() => {
      sseNotificationService.cleanup();
    });

    const getAlertByNotificationType = (notification: NotificationItem) => {
      switch (notification.type) {
        case 'PQRS_DUE_DATE_REMINDER':
          return alertService.showInfo(
            t('sse-notification.due-date-message', {
              pqrsTitle: notification.pqrsTitle,
              pqrsId: notification.pqrsId,
              pqrsDueDate: dateFormat.formatDateLong(notification.pqrsResponseDueDate),
            }),
            {
              href: `/pqrs/${notification.pqrsId}/view`,
              title: `${notification.pqrsTitle}`,
            },
          );
        case 'PQRS_STATE_UPDATE':
          return alertService.showInfo(
            t('sse-notification.state-update-message', {
              pqrsTitle: notification.pqrsTitle,
              pqrsId: notification.pqrsId,
            }),
            {
              href: `/pqrs/${notification.pqrsId}/view`,
              title: `${notification.pqrsTitle}`,
            },
          );
        case 'PQRS_CREATED':
          return alertService.showInfo(
            t('sse-notification.created-message', {
              pqrsTitle: notification.pqrsTitle,
              pqrsId: notification.pqrsId,
            }),
            {
              href: `/pqrs/${notification.pqrsId}/view`,
              title: `${notification.pqrsTitle}`,
            },
          );
        case 'PQRS_ASSIGNED':
          return alertService.showInfo(
            t('sse-notification.assigned-message', {
              pqrsTitle: notification.pqrsTitle,
              pqrsId: notification.pqrsId,
            }),
            {
              href: `/pqrs/${notification.pqrsId}/view`,
              title: `${notification.pqrsTitle}`,
            },
          );
      }
    };

    provide('alertService', useAlertService());
    return {
      t$: useI18n().t,
      authenticated,
      ...dataUtils,
    };
  },
});
