import { defineComponent, type PropType } from 'vue';
import { useI18n } from 'vue-i18n';
import { type IRespuesta } from '@/shared/model/respuesta.model';
import { useDateFormat } from '@/shared/composables';
import AttachmentList from '@/entities/archivo-adjunto/attachment-list.vue'; // <-- RUTA ACTUALIZADA

export default defineComponent({
  name: 'ResponseItem',
  components: {
    'attachment-list': AttachmentList,
  },
  props: {
    response: {
      type: Object as PropType<IRespuesta>,
      required: true,
    },
  },
  setup() {
    const { t } = useI18n();
    const { formatDateLong } = useDateFormat();

    return {
      t,
      formatDateLong,
    };
  },
});
