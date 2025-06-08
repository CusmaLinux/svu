import { defineComponent, type PropType } from 'vue';
import { useI18n } from 'vue-i18n';
// Asegúrate de que la ruta al modelo sea correcta
import { type IArchivoAdjunto } from '@/shared/model/archivo-adjunto.model';

export default defineComponent({
  name: 'AttachmentList',
  props: {
    attachments: {
      type: Array as PropType<IArchivoAdjunto[]>,
      default: () => [],
    },
  },
  setup() {
    const { t } = useI18n();
    return { t };
  },
});
