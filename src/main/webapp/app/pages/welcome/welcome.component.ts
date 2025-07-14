import { defineComponent, onMounted, watch, nextTick } from 'vue';
import Accordion from '@/core/components/accordion/accordion.vue';
import { useRoute } from 'vue-router';

export default defineComponent({
  compatConfig: { MODE: 3 },
  components: { 'b-accordion': Accordion },
  setup() {
    const faqItems = [
      {
        question: '¿Cómo puedo radicar una PQRSD?',
        answer:
          'Puede radicar una PQRSD haciendo clic en el botón "Crear PQRSD" en la sección principal o en la barra de navegación en móviles. Podrá hacerlo de forma anónima y adjuntar archivos si es necesario.',
      },
      {
        question: '¿Cómo consulto el estado de mi PQRSD?',
        answer:
          'Utilice la opción "Consultar PQRSD". Necesitará el número de radicado que se le proporcionó al crearla, o el enlace directo con el token de acceso.',
      },
      {
        question: '¿Puedo responder a un funcionario a través del sistema?',
        answer:
          'Sí, el sistema permite un mecanismo de comunicación. Podrá responder a las comunicaciones de los funcionarios encargados y adjuntar archivos si es necesario para aclarar su PQRSD.',
      },
      {
        question: '¿Es obligatorio registrarme para usar el sistema?',
        answer:
          'No, para la radicación de PQRSD anónimas y su consulta no es necesario un registro previo. Esto facilita el acceso y garantiza la confidencialidad si así lo desea.',
      },
    ];

    const route = useRoute();

    const scrollToHash = async (hash: string) => {
      if (!hash) return;

      await nextTick();

      const element = document.querySelector(hash);
      if (element) {
        element.scrollIntoView({ behavior: 'smooth' });
      }
    };

    onMounted(() => {
      scrollToHash(route.hash);
    });

    watch(
      () => route.hash,
      newHash => {
        scrollToHash(newHash);
      },
    );

    return { faqItems };
  },
});
