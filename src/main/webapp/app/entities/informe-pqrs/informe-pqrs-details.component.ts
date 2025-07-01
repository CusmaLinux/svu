import { type Ref, defineComponent, inject, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import InformePqrsService from './informe-pqrs.service';
import { useDateFormat } from '@/shared/composables';
import { type IInformePqrs } from '@/shared/model/informe-pqrs.model';
import { useAlertService } from '@/shared/alert/alert.service';
import ReportService from '@/services/reports.service';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'InformePqrsDetails',
  setup() {
    const dateFormat = useDateFormat();
    const informePqrsService = inject('informePqrsService', () => new InformePqrsService());
    const reportService = inject('reportService', () => new ReportService());
    const alertService = inject('alertService', () => useAlertService(), true);
    const isDownloading = ref(false);

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const informePqrs: Ref<IInformePqrs> = ref({});

    const retrieveInformePqrs = async informePqrsId => {
      try {
        const res = await informePqrsService().find(informePqrsId);
        informePqrs.value = res;
      } catch (error: any) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.informePqrsId) {
      retrieveInformePqrs(route.params.informePqrsId);
    }

    const downloadReport = async (informId: string) => {
      if (!informId) return;

      isDownloading.value = true;

      try {
        const blob = await reportService().downloadPqrsReport(informId);

        const link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob);
        link.download = `informe-pqrs-${informId}.xlsx`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(link.href);
      } catch (error) {
        alertService.showError('Error descargando el reporte.');
      } finally {
        isDownloading.value = false;
      }
    };

    return {
      ...dateFormat,
      alertService,
      informePqrs,
      isDownloading,
      downloadReport,
      previousState,
      t$: useI18n().t,
    };
  },
});
