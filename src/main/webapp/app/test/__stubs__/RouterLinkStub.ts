import { h, defineComponent } from 'vue';

export default defineComponent({
  name: 'RouterLink',
  compatConfig: { MODE: 3 },
  props: {
    to: {
      type: [String, Object],
      required: true,
    },
    custom: {
      type: Boolean,
      default: false,
    },
  },
  setup(props, { slots }) {
    return () => {
      const navigate = () => {};
      const isActive = false;
      const isExactActive = false;
      const href = '#';

      const scope = { navigate, isActive, isExactActive, href };

      if (props.custom && slots.default) {
        return slots.default(scope);
      }

      return h('a', { href }, slots.default ? slots.default(scope) : []);
    };
  },
});
