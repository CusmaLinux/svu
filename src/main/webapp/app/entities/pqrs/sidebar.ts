import { defineComponent, type PropType, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { type IPqrs } from '@/shared/model/pqrs.model';
import { PqrsStatus } from '@/constants';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'PqrsActionsSidebar',
  props: {
    pqrs: {
      type: Object as PropType<IPqrs | null>,
      default: null,
    },
    isFunctionary: {
      type: Boolean,
      default: false,
    },
    isAdmin: {
      type: Boolean,
      default: false,
    },
    isFrontdesk: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['toggle-status', 'open-close-modal'],
  setup(props, { emit }) {
    const { t } = useI18n();
    const router = useRouter();

    const navigateToCreateResponse = () => {
      if (props.pqrs && props.pqrs.id) {
        router.push({ name: 'RespuestaCreate', query: { pqrsId: props.pqrs.id } });
      }
    };

    return {
      t$: t,
      t,
      PqrsStatus,
      navigateToCreateResponse,
    };
  },
});
