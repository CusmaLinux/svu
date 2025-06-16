import { Authority } from '@/shared/security/authority';

const OficinaUserCreate = () => import('@/pages/oficina/oficina-user-create.vue');
const OficinaUserHome = () => import('@/pages/oficina/oficina-user-home.vue');
const PublicPqrsTrack = () => import('@/entities/pqrs/pqrs-details.vue');
const PublicPqrsNew = () => import('@/entities/pqrs/api/public-pqrs-create.vue');
const PublicPqrsVerify = () => import('@/entities/pqrs/api/public-pqrs-verify.vue');

export default [
  {
    path: '/oficina-user/create',
    name: 'OficinaUserCreate',
    component: OficinaUserCreate,
    meta: { authorities: [Authority.ADMIN] },
  },
  {
    path: '/oficina-user/:login/home',
    name: 'OficinaUserHome',
    component: OficinaUserHome,
    meta: { authorities: [Authority.USER] },
  },
  {
    path: '/public/pqrs/new',
    name: 'PublicPqrsNew',
    component: PublicPqrsNew,
  },
  {
    path: '/public/pqrs/track/:accessToken',
    name: 'PublicPqrsTrack',
    component: PublicPqrsTrack,
    props: true,
  },

  {
    path: '/public/pqrs/verify',
    name: 'PublicPqrsVerify',
    component: PublicPqrsVerify,
    props: (route: { query: { accessToken: string } }) => ({ accessToken: route.query.accessToken }),
  },
];
