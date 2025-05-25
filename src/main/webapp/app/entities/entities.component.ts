import { defineComponent, provide } from 'vue';

import OficinaService from './oficina/oficina.service';
import PqrsService from './pqrs/pqrs.service';
import RespuestaService from './respuesta/respuesta.service';
import ArchivoAdjuntoService from './archivo-adjunto/archivo-adjunto.service';
import NotificationService from './notification/notification.service';
import InformePqrsService from './informe-pqrs/informe-pqrs.service';
import UserService from '@/entities/user/user.service';
// jhipster-needle-add-entity-service-to-entities-component-import - JHipster will import entities services here

export default defineComponent({
  compatConfig: { MODE: 3 },
  name: 'Entities',
  setup() {
    provide('userService', () => new UserService());
    provide('oficinaService', () => new OficinaService());
    provide('pqrsService', () => new PqrsService());
    provide('respuestaService', () => new RespuestaService());
    provide('archivoAdjuntoService', () => new ArchivoAdjuntoService());
    provide('notificationService', () => new NotificationService());
    provide('informePqrsService', () => new InformePqrsService());
    // jhipster-needle-add-entity-service-to-entities-component - JHipster will import entities services here
  },
});
