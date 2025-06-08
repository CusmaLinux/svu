import { defineComponent, ref, onMounted, inject, computed } from 'vue';
import { useRoute } from 'vue-router';
import { useI18n } from 'vue-i18n';
import PqrsService from './pqrs.service';
import { type IPublicPqrs } from '@/shared/model/pqrs.model';
import { useDateFormat } from '@/shared/date/filters';

// --- RUTAS DE IMPORTACIÓN ACTUALIZADAS ---
import ResponseItem from '@/entities/respuesta/response-item.vue';
import AttachmentList from '@/entities/archivo-adjunto/attachment-list.vue';

export default defineComponent({
  name: 'PublicPqrsView',
  components: {
    ResponseItem,
    AttachmentList,
  },
  setup() {
    const pqrsService = inject('pqrsService', () => new PqrsService(), true);
    const route = useRoute();
    const { t } = useI18n();
    const { formatDateLong } = useDateFormat();

    const pqrs = ref<IPublicPqrs | null>(null);
    const loading = ref(true);
    const error = ref<Error | null>(null);

    const loadPqrs = async (accessToken: string) => {
      loading.value = true;
      error.value = null;
      try {
        const result = await pqrsService.findPublicByAccessToken(accessToken);
        pqrs.value = result;
      } catch (err) {
        error.value = err;
        pqrs.value = null;
      } finally {
        loading.value = false;
      }
    };

    const sortedResponses = computed(() => {
      if (!pqrs.value || !pqrs.value._transientResponses) {
        return [];
      }
      return [...pqrs.value._transientResponses].sort(
        (a, b) => new Date(a.fechaRespuesta).getTime() - new Date(b.fechaRespuesta).getTime(),
      );
    });

    onMounted(() => {
      const accessToken = route.params.accessToken as string;
      if (accessToken) {
        loadPqrs(accessToken);
      } else {
        error.value = new Error('Access Token no proporcionado.');
        loading.value = false;
      }
    });

    return {
      t,
      pqrs,
      loading,
      error,
      formatDateLong,
      sortedResponses,
    };
  },
});
