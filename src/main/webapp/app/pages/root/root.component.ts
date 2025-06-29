import { type ComputedRef, defineComponent, inject, computed, defineAsyncComponent } from 'vue';
export default defineComponent({
  compatConfig: { MODE: 3 },
  setup() {
    const authenticated = inject<ComputedRef<boolean>>('authenticated');
    const Welcome = defineAsyncComponent(() => import('@/pages/welcome/welcome.vue'));
    const Home = defineAsyncComponent(() => import('@/core/home/home.vue'));

    const currentComponent = computed(() => {
      if (authenticated && authenticated.value) {
        return Home;
      }
      return Welcome;
    });

    return { currentComponent };
  },
});
