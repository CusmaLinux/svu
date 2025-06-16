import { computed, defineComponent, inject, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter, useRoute } from 'vue-router';

import useDataUtils from '@/shared/data/data-utils.service';
import { useAlertService } from '@/shared/alert/alert.service';

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'PublicPqrsVerify',
  setup() {
    const alertService = inject('alertService', () => useAlertService(), true);
    const { t: t$ } = useI18n();

    const currentLanguage = inject('currentLanguage', () => computed(() => navigator.language ?? 'es'), true);

    const route = useRoute();
    const router = useRouter();

    const accessToken = ref<string | null>(null);
    const isCopied = ref(false);
    const baseURL = import.meta.env.VITE_BASE_URI;

    const trackingUrl = computed(() => {
      if (accessToken.value && baseURL) {
        return `${baseURL}/public/pqrs/track/${accessToken.value}`;
      }
      return null;
    });

    const copyLink = () => {
      if (trackingUrl.value) {
        navigator.clipboard
          .writeText(trackingUrl.value)
          .then(() => {
            isCopied.value = true;
            setTimeout(() => {
              isCopied.value = false;
            }, 2000);
          })
          .catch(err => {
            console.error('Error al copiar el enlace: ', err);
            alert('No se pudo copiar el enlace. Por favor, cópielo manualmente.');
          });
      }
    };

    onMounted(() => {
      const tokenFromQuery = route.query.accessToken;
      if (typeof tokenFromQuery === 'string' && tokenFromQuery) {
        accessToken.value = tokenFromQuery;
      } else {
        router.push('/');
      }
    });

    const dataUtils = useDataUtils();
    return {
      copyLink,
      alertService,
      isCopied,
      currentLanguage,
      accessToken,
      trackingUrl,
      ...dataUtils,
      t$,
    };
  },
});
