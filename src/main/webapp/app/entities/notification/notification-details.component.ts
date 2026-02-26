import { type Ref, defineComponent, inject, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import NotificationService from './notification.service';
import useDataUtils from '@/shared/data/data-utils.service';
import { useDateFormat } from '@/shared/composables';
import { type INotification } from '@/shared/model/notification.model';
import { useAlertService } from '@/shared/alert/alert.service';
import { NotificationType } from '@/constants';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'NotificationDetails',
  setup() {
    const dateFormat = useDateFormat();
    const notificationService = inject('notificationService', () => new NotificationService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const dataUtils = useDataUtils();

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const notification: Ref<INotification> = ref({});

    const retrieveNotificacion = async (notificationId: string | string[]) => {
      try {
        const res = await notificationService().find(notificationId);
        notification.value = res;
      } catch (error: any) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.notificacionId) {
      retrieveNotificacion(route.params.notificacionId);
    }

    return {
      ...dateFormat,
      alertService,
      notification,
      NotificationType,
      ...dataUtils,

      previousState,
      t$: useI18n().t,
    };
  },
});
