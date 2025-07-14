import { defineComponent, ref } from 'vue';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'Footer',
  setup() {
    const currentYear = ref(new Date().getFullYear());

    return { currentYear };
  },
});
