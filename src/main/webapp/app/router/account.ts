import { Authority } from '@/shared/security/authority';

const Register = () => import('@/account/register/register.vue');
const Activate = () => import('@/account/activate/activate.vue');
const ResetPasswordInit = () => import('@/account/reset-password/init/reset-password-init.vue');
const ResetPasswordFinish = () => import('@/account/reset-password/finish/reset-password-finish.vue');
const ChangePassword = () => import('@/account/change-password/change-password.vue');
const Settings = () => import('@/account/settings/settings.vue');

export default [
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { authorities: [Authority.ADMIN] },
  },
  {
    path: '/account/activate',
    name: 'Activate',
    component: Activate,
    meta: { authorities: [Authority.ADMIN] },
  },
  {
    path: '/account/reset/request',
    name: 'ResetPasswordInit',
    component: ResetPasswordInit,
    meta: { authorities: [Authority.ADMIN] },
  },
  {
    path: '/account/reset/finish',
    name: 'ResetPasswordFinish',
    component: ResetPasswordFinish,
    meta: { authorities: [Authority.ADMIN] },
  },
  {
    path: '/account/password',
    name: 'ChangePassword',
    component: ChangePassword,
    meta: { authorities: [Authority.ADMIN] },
  },
  {
    path: '/account/settings',
    name: 'Settings',
    component: Settings,
    meta: { authorities: [Authority.USER, Authority.FRONT_DESK_CS, Authority.FUNCTIONARY, Authority.ADMIN] },
  },
];
