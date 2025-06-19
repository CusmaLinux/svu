import { defineComponent, inject, type PropType } from 'vue';
import { useI18n } from 'vue-i18n';
import { type IArchivoAdjunto } from '@/shared/model/archivo-adjunto.model';
import ArchivoAdjuntoService from './archivo-adjunto.service';
import { useAlertService } from '@/shared/alert/alert.service';

export default defineComponent({
  name: 'AttachmentList',
  props: {
    attachments: {
      type: Array as PropType<IArchivoAdjunto[]>,
      default: () => [],
    },
  },
  setup() {
    const { t: t$ } = useI18n();
    const archivoAdjuntoService = inject('archivoAdjuntoService', () => new ArchivoAdjuntoService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const downloadAttachedFile = async (fileURL: string | undefined | null, fileName: string | undefined) => {
      if (!fileURL && !fileName) {
        return;
      }
      try {
        if (fileURL && fileName) {
          const { blob } = await archivoAdjuntoService().downloadAttachedFile(fileURL);
          const link = document.createElement('a');
          const url = window.URL.createObjectURL(blob);
          link.href = url;
          link.setAttribute('download', fileName);
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);
        }
      } catch (error: any) {
        alertService.showHttpError(error.response ?? 'Ocurrió un error inesperado.');
      }
    };

    return { t$, downloadAttachedFile };
  },
});
