import { type Ref, computed, defineComponent, inject, onMounted, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import { PqrsStatus } from '@/constants';
import PqrsService from './pqrs.service';
import useDataUtils from '@/shared/data/data-utils.service';
import { useDateFormat } from '@/shared/composables';
import { type IPqrs } from '@/shared/model/pqrs.model';
import { useAlertService } from '@/shared/alert/alert.service';
import { useAccountStore } from '@/shared/config/store/account-store';

import PqrsActionsSidebar from './sidebar.vue';
import ResponseItem from '@/entities/respuesta/response-item.vue';
import AttachmentList from '@/entities/archivo-adjunto/attachment-list.vue';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'PqrsDetails',
  components: {
    sidebar: PqrsActionsSidebar,
    'response-item': ResponseItem,
    'attachment-list': AttachmentList,
  },
  setup() {
    const { t } = useI18n();
    const dateFormat = useDateFormat();
    const pqrsService = inject('pqrsService', () => new PqrsService());
    const alertService = inject('alertService', () => useAlertService(), true);
    const { formatDateLong } = useDateFormat();
    const pqrsIdFromRoute = computed(() => route.params.pqrsId as string);
    const loading = ref(false);

    const dataUtils = useDataUtils();

    const route = useRoute();
    const router = useRouter();

    const accountStore = useAccountStore();

    const previousState = () => router.go(-1);
    const pqrs: Ref<IPqrs | null> = ref(null);

    const isConfirmCloseModalVisible = ref(false);
    const confirmCloseModalRef = ref(null);

    const isFunctionary = computed(() => {
      return accountStore.account?.authorities?.includes('ROLE_FUNCTIONARY') ?? false;
    });

    const isFrontdesk = computed(() => {
      return accountStore.account?.authorities?.includes('ROLE_FRONT_DESK_CS') ?? false;
    });

    const isAdmin = computed(() => {
      return accountStore.account?.authorities?.includes('ROLE_ADMIN') ?? false;
    });

    const statusClass = computed(() => {
      if (!pqrs.value?.estado) return 'status-default';
      switch (pqrs.value.estado.toUpperCase()) {
        case PqrsStatus.Pending:
          return 'status-open';
        case PqrsStatus.Resolved:
          return 'status-closed';
        case PqrsStatus.InProcess:
          return 'status-in-progress';
        default:
          return 'status-default';
      }
    });

    const retrievePqrs = async (id: string | string[]) => {
      if (!id) {
        pqrs.value = null;
        alertService.showError('PQRS ID is missing');
        return;
      }
      loading.value = true;
      try {
        const res = await pqrsService().find(id);
        pqrs.value = res;
      } catch (error: any) {
        alertService.showHttpError(error.response);
      } finally {
        loading.value = false;
      }
    };

    const toggleStatusPqrs = async () => {
      if (pqrs.value && pqrs.value.id) {
        let newState: string;
        let successMessageKey: string;

        if (pqrs.value.estado === PqrsStatus.Resolved) {
          newState = PqrsStatus.InProcess;
          successMessageKey = 'ventanillaUnicaApp.pqrs.messages.inProgresSuccess';
        } else {
          newState = PqrsStatus.Resolved;
          successMessageKey = 'ventanillaUnicaApp.pqrs.messages.resolvedSuccess';
        }

        const pqrsToUpdate: IPqrs = {
          ...pqrs.value,
          estado: newState,
        };

        try {
          const result = await pqrsService().update(pqrsToUpdate);
          pqrs.value = result;
          alertService.showSuccess(t(successMessageKey));
        } catch (error: any) {
          if (error.response) {
            alertService.showHttpError(error.response);
          } else {
            alertService.showError(t('ventanillaUnicaApp.pqrs.messages.resolvedError'));
          }
        }
      }
    };

    const openConfirmCloseModal = () => {
      isConfirmCloseModalVisible.value = true;
    };

    const handleConfirmClose = async () => {
      await confirmClosePqrs();
    };

    const confirmClosePqrs = async () => {
      if (pqrs.value && pqrs.value.id) {
        const pqrsToUpdate: IPqrs = {
          ...pqrs.value,
          estado: PqrsStatus.Closed,
        };

        try {
          const result = await pqrsService().update(pqrsToUpdate);
          pqrs.value = result;
          alertService.showSuccess(t('ventanillaUnicaApp.pqrs.messages.closedSuccess'));
          isConfirmCloseModalVisible.value = false;
        } catch (error: any) {
          if (error.response) {
            alertService.showHttpError(error.response);
          } else {
            alertService.showError(t('ventanillaUnicaApp.pqrs.messages.closeError'));
          }
        }
      }
    };

    onMounted(async () => {
      const pqrsId: string | string[] = route.params.pqrsId;
      if (pqrsId) {
        await retrievePqrs(pqrsId);
      } else {
        alertService.showError(t('ventanillaUnicaApp.pqrs.messages.notFound'));
        previousState();
      }
    });

    watch(
      pqrsIdFromRoute,
      async (newId, oldId) => {
        if (newId && newId !== oldId) {
          await retrievePqrs(newId);
        }
      },
      { immediate: false },
    );

    return {
      ...dateFormat,
      alertService,
      pqrs,
      PqrsStatus,
      isConfirmCloseModalVisible,
      confirmCloseModalRef,
      isFunctionary,
      isAdmin,
      isFrontdesk,
      openConfirmCloseModal,
      handleConfirmClose,
      pqrsId: pqrsIdFromRoute,
      loading,
      statusClass,

      ...dataUtils,

      previousState,
      t$: useI18n().t,
      t,
      formatDateLong,
      toggleStatusPqrs,
    };
  },
});
