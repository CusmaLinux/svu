import { afterEach, describe, expect, it } from 'vitest';
import { mount } from '@vue/test-utils';
import { createMemoryHistory, createRouter, type RouteRecordRaw } from 'vue-router';
import AccountService from '@/account/account.service';
import EntitiesMenu from '@/entities/entities-menu.vue';
import entities from '@/router/entities';
import { checkRouteAuthorities } from '@/router/authority-guard';
import { Authority } from '@/shared/security/authority';
import { useStore } from '@/store';

const profiles: [string, string[] | null, boolean][] = [
  ['administración', [Authority.ADMIN], true],
  ['informes', [Authority.PQRSD_REPORTS], true],
  ['combinado', [Authority.FUNCTIONARY, Authority.PQRSD_REPORTS], true],
  ['funcionario', [Authority.FUNCTIONARY], false],
  ['ventanilla', [Authority.FRONT_DESK_CS], false],
  ['usuario ordinario', [Authority.USER], false],
  ['anónimo', null, false],
];

describe('Acceso al informe de gestión', () => {
  afterEach(() => useStore().logout());

  it.each(profiles)('aplica menú y ruta para %s', async (_label, authorities, allowed) => {
    const store = useStore();
    if (authorities) store.setAuthentication({ login: 'synthetic', authorities });
    const menu = mount(EntitiesMenu, {
      global: {
        directives: { can: {} },
        stubs: {
          'font-awesome-icon': true,
          BDropdownItem: { props: ['to'], template: '<a :data-destination="to?.name ?? to"><slot /></a>' },
        },
      },
    });
    try {
      expect(menu.find('[data-destination="PqrsManagementReports"]').exists()).toBe(allowed);
      expect(menu.find('[data-destination="/informe-pqrs"]').exists()).toBe(true);
    } finally {
      menu.unmount();
    }
    const empty = { template: '<div />' };
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [entities as RouteRecordRaw, { path: '/forbidden', name: 'Forbidden', component: empty }],
    });
    const account = new AccountService(store);
    router.beforeResolve(to =>
      checkRouteAuthorities(to, {
        get authenticated() {
          return account.authenticated;
        },
        update: async () => {},
        hasAnyAuthorityAndCheckAuth: account.hasAnyAuthorityAndCheckAuth.bind(account),
      }),
    );
    await router.push('/informes-gestion-pqrsd');
    expect(router.currentRoute.value.name).toBe(allowed ? 'PqrsManagementReports' : 'Forbidden');
    if (!allowed) {
      await router.push('/informes-gestion-pqrsd');
      expect(router.currentRoute.value.name).toBe('Forbidden');
    }
    expect(router.resolve('/informe-pqrs').meta.authorities).toEqual([Authority.ADMIN]);
  });
});
