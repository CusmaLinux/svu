import { createRouter as createVueRouter, createWebHistory, type RouterScrollBehavior } from 'vue-router';

const Root = () => import('@/pages/root/root.vue');
const Error = () => import('@/core/error/error.vue');
import account from '@/router/account';
import admin from '@/router/admin';
import entities from '@/router/entities';
import pages from '@/router/pages';

const scrollBehavior: RouterScrollBehavior = (to, from, savedPosition) => {
  if (to.hash) {
    return {
      el: to.hash,
      behavior: 'smooth',
    };
  }

  if (savedPosition) {
    return savedPosition;
  }

  return { top: 0, behavior: 'smooth' };
};

export const createRouter = () =>
  createVueRouter({
    history: createWebHistory(),
    routes: [
      {
        path: '/',
        name: 'Root',
        component: Root,
      },
      {
        path: '/forbidden',
        name: 'Forbidden',
        component: Error,
        meta: { error403: true },
      },
      {
        path: '/not-found',
        name: 'NotFound',
        component: Error,
        meta: { error404: true },
      },
      ...account,
      ...admin,
      entities,
      ...pages,
    ],
    scrollBehavior,
  });

const router = createRouter();

router.beforeResolve(async (to, from, next) => {
  if (!to.matched.length) {
    return next({ path: '/not-found' });
  }
  next();
});

export default router;
