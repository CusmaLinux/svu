import { defineComponent, onBeforeUnmount, onMounted, provide, watch, inject, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import Ribbon from '@/core/ribbon/ribbon.vue';
import JhiFooter from '@/core/jhi-footer/jhi-footer.vue';
import JhiNavbar from '@/core/jhi-navbar/jhi-navbar.vue';
import Footer from '@/core/footer/footer.vue';
import AuthFooter from './core/footer/auth-footer.vue';
import LoginForm from '@/account/login-form/login-form.vue';

import '@/shared/config/dayjs';

import { useAccountStore } from './shared/config/store/account-store';
import { useNotificationStore } from './shared/config/store/notification-store';
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

    const authenticated = computed(() => accountStore.authenticated);

    const manageSseConnection = () => {
      if (accountStore.authenticated) {
        notificationStore.fetchUserNotifications();
        if (!sseNotificationService.connect()) return;

        if (Notification.permission !== 'granted' && Notification.permission !== 'denied') {
          Notification.requestPermission();
        }
      } else {
        sseNotificationService.disconnect();
      }
    };

    watch(
      () => [accountStore.account?.login, accountStore.userRole, accountStore.authenticated],
      () => {
        sseNotificationService.disconnect();
        notificationStore.$reset();
        manageSseConnection();
      },
    );

    watch(
      () => [...notificationStore.sseNotifications],
      (newSseItems, oldSseItems) => {
        const latestItem = newSseItems.find(newItem => !oldSseItems.some(oldItem => oldItem.id === newItem.id));

        if (latestItem && latestItem.isSse) {
          alertService.showInfo(latestItem.message || '', { href: `/notificacion/${latestItem.id}/view` });
          notificationStore.fetchUserNotifications();
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

    provide('alertService', useAlertService());
    return {
      t$: useI18n().t,
      authenticated,
      ...dataUtils,
    };
  },
});
