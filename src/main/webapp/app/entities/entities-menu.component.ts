import { computed, defineComponent } from 'vue';
import { useI18n } from 'vue-i18n';
import { useStore } from '@/store';
import { hasStatisticsAccess } from '@/shared/security/statistics-access';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'EntitiesMenu',
  setup() {
    const i18n = useI18n();
    const store = useStore();
    return {
      showManagementReports: computed(() => hasStatisticsAccess(store.authenticated, store.account?.authorities)),
      t$: i18n.t,
    };
  },
});
