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
    caseId: { type: String, default: null },
    accessToken: { type: String, default: null },
  },
  setup(props) {
    const { t: t$ } = useI18n();
    const archivoAdjuntoService = inject('archivoAdjuntoService', () => new ArchivoAdjuntoService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const downloadAttachedFile = async (
      attachmentId: string | undefined,
      fileURL: string | undefined | null,
      fileName: string | undefined,
    ) => {
      if (!attachmentId || !fileName) return;
      try {
        const result = props.accessToken
          ? await archivoAdjuntoService().downloadForCitizen(props.accessToken, attachmentId)
          : props.caseId
            ? await archivoAdjuntoService().downloadForCase(props.caseId, attachmentId)
            : fileURL
              ? await archivoAdjuntoService().downloadAttachedFile(fileURL)
              : null;
        if (!result) return;
        const link = document.createElement('a');
        const url = window.URL.createObjectURL(result.blob);
        link.href = url;
        link.setAttribute('download', fileName);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      } catch (error: any) {
        alertService.showHttpError(error.response ?? 'Ocurrió un error inesperado.');
      }
    };

    return { t$, downloadAttachedFile };
  },
});
