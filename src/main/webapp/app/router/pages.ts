const PublicPqrsDetails = () => import('@/entities/pqrs/api/public-pqrs-details.vue');
const PublicPqrsNew = () => import('@/entities/pqrs/api/public-pqrs-create.vue');
const PublicPqrsVerify = () => import('@/entities/pqrs/api/public-pqrs-verify.vue');
const ConsultRequirement = () => import('@/pages/consult-requirement/consult-requirement.vue');

export default [
  {
    path: '/public/pqrs/new',
    name: 'PublicPqrsNew',
    component: PublicPqrsNew,
  },
  {
    path: '/public/pqrs/track/:accessToken',
    name: 'PublicPqrsDetails',
    component: PublicPqrsDetails,
    props: true,
  },

  {
    path: '/public/pqrs/verify',
    name: 'PublicPqrsVerify',
    component: PublicPqrsVerify,
    props: (route: { query: { accessToken: string } }) => ({ accessToken: route.query.accessToken }),
  },

  {
    path: '/public/pqrs/consult',
    name: 'ConsultRequirement',
    component: ConsultRequirement,
  },
];
