import { type Ref, defineComponent, inject, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import RespuestaService from './respuesta.service';
import useDataUtils from '@/shared/data/data-utils.service';
import { useDateFormat } from '@/shared/composables';
import { type IRespuesta } from '@/shared/model/respuesta.model';
import { useAlertService } from '@/shared/alert/alert.service';
import AttachmentList from '@/entities/archivo-adjunto/attachment-list.vue';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'RespuestaDetails',
  components: {
    'attachment-list': AttachmentList,
  },
  setup() {
    const dateFormat = useDateFormat();
    const respuestaService = inject('respuestaService', () => new RespuestaService());
    const alertService = inject('alertService', () => useAlertService(), true);

    const dataUtils = useDataUtils();

    const route = useRoute();
    const router = useRouter();

    const previousState = () => router.go(-1);
    const respuesta: Ref<IRespuesta> = ref({});

    const retrieveRespuesta = async respuestaId => {
      try {
        const res = await respuestaService().find(respuestaId);
        respuesta.value = res;
      } catch (error) {
        alertService.showHttpError(error.response);
      }
    };

    if (route.params?.respuestaId) {
      retrieveRespuesta(route.params.respuestaId);
    }

    return {
      ...dateFormat,
      alertService,
      respuesta,

      ...dataUtils,

      previousState,
      t$: useI18n().t,
    };
  },
});
