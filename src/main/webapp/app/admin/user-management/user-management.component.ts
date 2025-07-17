import { type ComputedRef, type Ref, defineComponent, inject, ref, watch, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import UserManagementService from './user-management.service';
import UserService from '@/entities/user/user.service';
import { useAlertService } from '@/shared/alert/alert.service';
import { logger } from '@/shared/logger';

import { useDateFormat } from '@/shared/composables';
import useDataUtils from '@/shared/data/data-utils.service';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'JhiUserManagementComponent',
  setup(prop) {
    const alertService = inject('alertService', () => useAlertService(), true);
    const { formatDateShort: formatDate } = useDateFormat();
    const userManagementService = inject('userManagementService', () => new UserManagementService(), true);
    const userService = inject('userService', () => new UserService(), true);
    const username = inject<ComputedRef<string>>('currentUsername');
    const searchQuery: Ref<string> = ref('');
    const dataUtils = useDataUtils();

    const error = ref('');
    const success = ref('');
    const itemsPerPage = ref(20);
    const page = ref(1);
    const previousPage = ref(1);
    const propOrder = ref('created_date');
    const reverse = ref(true);
    const isLoading = ref(false);
    const removeId: Ref<string> = ref(null);
    const users: Ref<any[]> = ref([]);
    const totalItems = ref(0);
    const queryCount: Ref<number> = ref(null);
    const { t: t$ } = useI18n();

    const removeUser = ref<any>(null);

    const sort = (): any => {
      const result = [`${propOrder.value},${reverse.value ? 'desc' : 'asc'}`];
      if (propOrder.value !== 'id') {
        result.push('id');
      }
      return result;
    };

    const loadAll = async () => {
      isLoading.value = true;
      try {
        const res = await userService.search(
          {
            sort: sort(),
            page: page.value - 1,
            size: itemsPerPage.value,
          },
          searchQuery.value,
        );
        users.value = res.data;
        totalItems.value = Number(res.headers['x-total-count']);
        queryCount.value = totalItems.value;
      } catch (err) {
        logger.error(err);
      } finally {
        isLoading.value = false;
      }
    };

    const transition = (): void => {
      loadAll();
    };

    const setActive = (user, isActivated): void => {
      user.activated = isActivated;
      userManagementService
        .update(user)
        .then(() => {
          error.value = null;
          success.value = 'OK';
          loadAll();
        })
        .catch(() => {
          success.value = null;
          error.value = 'ERROR';
          user.activated = !isActivated;
        });
    };

    const loadPage = (pageNumber: number): void => {
      if (pageNumber !== previousPage.value) {
        previousPage.value = pageNumber;
        page.value = pageNumber;
        transition();
      }
    };

    const changeOrder = (newOrder: string): void => {
      if (propOrder.value === newOrder) {
        reverse.value = !reverse.value;
      } else {
        reverse.value = false;
      }
      propOrder.value = newOrder;

      page.value = 1;
      transition();
    };

    const closeDialog = (): void => {
      if (removeUser.value) {
        removeUser.value.hide();
      }
    };

    const deleteUser = (): void => {
      userManagementService
        .remove(removeId.value)
        .then(res => {
          const alertMessage = res.headers['x-ventanillaunicaapp-alert']?.toString() ?? '';
          const alertParam = decodeURIComponent(res.headers['x-ventanillaunicaapp-params']?.replace(/\+/g, ' ') ?? '');

          alertService.showInfo(t$(alertMessage, { param: alertParam }), { variant: 'danger' });
          removeId.value = null;
          loadAll();
          closeDialog();
        })
        .catch(error => {
          alertService.showHttpError(error.response);
        });
    };

    const prepareRemove = (instance): void => {
      removeId.value = instance.login;
      if (removeUser.value) {
        removeUser.value.show();
      }
    };

    const handleSyncList = () => {
      loadAll();
    };

    const clear = () => {
      page.value = 1;
      searchQuery.value = '';
      loadAll();
    };

    const clearSearch = () => {
      searchQuery.value = '';
    };

    const debouncedSearch = dataUtils.debounce(() => {
      if (page.value !== 1) {
        page.value = 1;
      } else {
        loadAll();
      }
    }, 500);

    watch(searchQuery, () => {
      debouncedSearch();
    });

    watch(page, (newPage, oldPage) => {
      if (newPage !== oldPage) {
        transition();
      }
    });

    onMounted(() => {
      loadAll();
    });

    return {
      // Existing
      formatDate,
      userManagementService,
      userService,
      alertService,
      searchQuery,
      error,
      success,
      itemsPerPage,
      page,
      previousPage,
      propOrder,
      reverse,
      isLoading,
      removeId,
      users,
      username,
      totalItems,
      queryCount,
      t$,
      clear,
      clearSearch,
      loadAll,
      setActive,
      loadPage,
      transition,
      changeOrder,
      deleteUser,
      prepareRemove,
      closeDialog,
      handleSyncList,
      removeUser,
    };
  },
});
