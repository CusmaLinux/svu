import { type Ref, defineComponent, inject, ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import InformePqrsService from './informe-pqrs.service';
import { useDateFormat } from '@/shared/composables';
import { type IInformePqrs } from '@/shared/model/informe-pqrs.model';
import { useAlertService } from '@/shared/alert/alert.service';
import ReportService from '@/services/reports.service';

import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { BarChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, GridComponent, LegendComponent } from 'echarts/components';
import VChart from 'vue-echarts';

use([CanvasRenderer, BarChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent]);

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'InformePqrsDetails',
  components: {
    VChart,
  },
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

    const retrieveInformePqrs = async (informePqrsId: string | string[]) => {
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

    const getChartTitle = (startDate: string | number | Date | undefined): string => {
      if (!startDate) {
        return 'Resumen de PQRS';
      }
      try {
        const date = new Date(startDate);
        const year = date.getFullYear();
        const quarter = Math.floor(date.getMonth() / 3) + 1;
        return `Resumen PQRS - Trimestre Q${quarter} ${year}`;
      } catch (e) {
        return 'Resumen de PQRS';
      }
    };

    const chartOptions = computed(() => {
      const data = informePqrs.value;
      if (!data || data.id === undefined) {
        return {};
      }

      return {
        title: {
          text: getChartTitle(data.fechaInicio),
          left: 'center',
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow',
          },
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true,
        },
        xAxis: [
          {
            type: 'category',
            data: ['Total PQRS', 'Total Resueltas', 'Total Pendientes'],
            axisTick: {
              alignWithLabel: true,
            },
          },
        ],
        yAxis: [
          {
            type: 'value',
          },
        ],
        series: [
          {
            name: 'Cantidad',
            type: 'bar',
            barWidth: '60%',
            data: [
              { value: data.totalPqrs, itemStyle: { color: '#5470C6' } },
              { value: data.totalResueltas, itemStyle: { color: '#91CC75' } },
              { value: data.totalPendientes, itemStyle: { color: '#FAC858' } },
            ],
          },
        ],
      };
    });

    return {
      ...dateFormat,
      alertService,
      informePqrs,
      isDownloading,
      downloadReport,
      previousState,
      chartOptions,
      t$: useI18n().t,
    };
  },
});
