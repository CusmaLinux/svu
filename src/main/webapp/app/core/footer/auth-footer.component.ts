import { defineComponent, ref } from 'vue';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'AuthFooter',
  setup() {
    const currentYear = ref(new Date().getFullYear());

    return { currentYear };
  },
});
