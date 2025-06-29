import { AuthorityDisplay } from '@/shared/security/authority';
import { type ComputedRef, defineComponent, inject, computed } from 'vue';

export default defineComponent({
  compatConfig: { MODE: 3 },
  setup() {
    const authenticated = inject<ComputedRef<boolean>>('authenticated');
    const username = inject<ComputedRef<string>>('currentUsername');
    const mainRole = inject<ComputedRef<string>>('mainRole');
    const displayMainRole = computed(() => {
      if (!mainRole) {
        return 'Rol no asignado';
      }
      const roleKey = mainRole.value as keyof typeof AuthorityDisplay;
      return AuthorityDisplay[roleKey] || roleKey;
    });

    return {
      authenticated,
      username,
      displayMainRole,
    };
  },
});
