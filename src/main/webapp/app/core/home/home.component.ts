import { AuthorityDisplay } from '@/shared/security/authority';
import { type ComputedRef, defineComponent, inject, computed } from 'vue';
import { useAccountStore } from '@/shared/config/store/account-store';

export default defineComponent({
  compatConfig: { MODE: 3 },
  setup() {
    const authenticated = inject<ComputedRef<boolean>>('authenticated');
    const username = inject<ComputedRef<string>>('currentUsername');
    const accountStore = useAccountStore();

    const displayMainRole = computed(() => {
      const roleKey = accountStore.userRole as keyof typeof AuthorityDisplay;
      return AuthorityDisplay[roleKey] || roleKey;
    });

    return {
      authenticated,
      username,
      displayMainRole,
    };
  },
});
