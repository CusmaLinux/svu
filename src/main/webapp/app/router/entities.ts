import { Authority } from '@/shared/security/authority';
/* tslint:disable */
// prettier-ignore
const Entities = () => import('@/entities/entities.vue');

const Oficina = () => import('@/entities/oficina/oficina.vue');
const OficinaUpdate = () => import('@/entities/oficina/oficina-update.vue');
const OficinaDetails = () => import('@/entities/oficina/oficina-details.vue');

const Pqrs = () => import('@/entities/pqrs/pqrs.vue');
const PqrsUpdate = () => import('@/entities/pqrs/pqrs-update.vue');
const PqrsDetails = () => import('@/entities/pqrs/pqrs-details.vue');

const Respuesta = () => import('@/entities/respuesta/respuesta.vue');
const RespuestaUpdate = () => import('@/entities/respuesta/respuesta-update.vue');
const RespuestaDetails = () => import('@/entities/respuesta/respuesta-details.vue');

const ArchivoAdjunto = () => import('@/entities/archivo-adjunto/archivo-adjunto.vue');
const ArchivoAdjuntoUpdate = () => import('@/entities/archivo-adjunto/archivo-adjunto-update.vue');
const ArchivoAdjuntoDetails = () => import('@/entities/archivo-adjunto/archivo-adjunto-details.vue');

const Notification = () => import('@/entities/notification/notification.vue');
const NotificationDetails = () => import('@/entities/notification/notification-details.vue');

const InformePqrs = () => import('@/entities/informe-pqrs/informe-pqrs.vue');
const InformePqrsUpdate = () => import('@/entities/informe-pqrs/informe-pqrs-update.vue');
const InformePqrsDetails = () => import('@/entities/informe-pqrs/informe-pqrs-details.vue');

// jhipster-needle-add-entity-to-router-import - JHipster will import entities to the router here

export default {
  path: '/',
  component: Entities,
  children: [
    {
      path: 'oficina',
      name: 'Oficina',
      component: Oficina,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'oficina/new',
      name: 'OficinaCreate',
      component: OficinaUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'oficina/:oficinaId/edit',
      name: 'OficinaEdit',
      component: OficinaUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'oficina/:oficinaId/view',
      name: 'OficinaView',
      component: OficinaDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'pqrs',
      name: 'Pqrs',
      component: Pqrs,
      meta: { authorities: [Authority.FUNCTIONARY, Authority.ADMIN, Authority.FRONT_DESK_CS] },
    },
    {
      path: 'pqrs/new',
      name: 'PqrsCreate',
      component: PqrsUpdate,
      meta: { authorities: [Authority.FRONT_DESK_CS, Authority.ADMIN] },
    },
    {
      path: 'pqrs/:pqrsId/edit',
      name: 'PqrsEdit',
      component: PqrsUpdate,
      meta: { authorities: [Authority.FRONT_DESK_CS, Authority.ADMIN] },
    },
    {
      path: 'pqrs/:pqrsId/view',
      name: 'PqrsView',
      component: PqrsDetails,
      meta: { authorities: [Authority.ADMIN, Authority.FUNCTIONARY, Authority.FRONT_DESK_CS] },
    },
    {
      path: 'respuesta',
      name: 'Respuesta',
      component: Respuesta,
      meta: { authorities: [Authority.ADMIN, Authority.FUNCTIONARY] },
    },
    {
      path: 'respuesta/new',
      name: 'RespuestaCreate',
      component: RespuestaUpdate,
      meta: { authorities: [Authority.ADMIN, Authority.FUNCTIONARY] },
    },
    {
      path: 'respuesta/:respuestaId/edit',
      name: 'RespuestaEdit',
      component: RespuestaUpdate,
      meta: { authorities: [Authority.ADMIN, Authority.FUNCTIONARY] },
    },
    {
      path: 'respuesta/:respuestaId/view',
      name: 'RespuestaView',
      component: RespuestaDetails,
      meta: { authorities: [Authority.ADMIN, Authority.FUNCTIONARY, Authority.FRONT_DESK_CS] },
    },
    {
      path: 'archivo-adjunto',
      name: 'ArchivoAdjunto',
      component: ArchivoAdjunto,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'archivo-adjunto/new',
      name: 'ArchivoAdjuntoCreate',
      component: ArchivoAdjuntoUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'archivo-adjunto/:archivoAdjuntoId/edit',
      name: 'ArchivoAdjuntoEdit',
      component: ArchivoAdjuntoUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'archivo-adjunto/:archivoAdjuntoId/view',
      name: 'ArchivoAdjuntoView',
      component: ArchivoAdjuntoDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'notificaciones',
      name: 'Notifications',
      component: Notification,
      meta: { authorities: [Authority.USER, Authority.FUNCTIONARY, Authority.ADMIN, Authority.FRONT_DESK_CS] },
    },
    {
      path: 'notificacion/:notificacionId/view',
      name: 'NotificacionView',
      component: NotificationDetails,
      meta: { authorities: [Authority.USER, Authority.FUNCTIONARY, Authority.ADMIN, Authority.FRONT_DESK_CS] },
    },
    {
      path: 'informe-pqrs',
      name: 'InformePqrs',
      component: InformePqrs,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'informe-pqrs/new',
      name: 'InformePqrsCreate',
      component: InformePqrsUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'informe-pqrs/:informePqrsId/edit',
      name: 'InformePqrsEdit',
      component: InformePqrsUpdate,
      meta: { authorities: [Authority.ADMIN] },
    },
    {
      path: 'informe-pqrs/:informePqrsId/view',
      name: 'InformePqrsView',
      component: InformePqrsDetails,
      meta: { authorities: [Authority.ADMIN] },
    },
    // jhipster-needle-add-entity-to-router - JHipster will add entities to the router here
  ],
};
