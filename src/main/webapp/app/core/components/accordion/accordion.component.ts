import { defineComponent } from 'vue';

export default defineComponent({
  name: 'Accordion',
  props: {
    items: {
      type: Array,
      default: () => [],
    },
  },
  setup() {
    return {};
  },
});
