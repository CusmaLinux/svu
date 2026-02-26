import { vitest } from 'vitest';
import { computed, nextTick } from 'vue';
import { shallowMount } from '@vue/test-utils';
import { type Router } from 'vue-router';
import { createTestingPinia } from '@pinia/testing';
import JhiNavbar from './jhi-navbar.vue';

import { useStore } from '@/store';
import { createRouter } from '@/router';
import LoginService from '@/account/login.service';

type JhiNavbarComponentType = InstanceType<typeof JhiNavbar>;

describe('JhiNavbar', () => {
  let loginService: LoginService;
  const accountService = { hasAnyAuthorityAndCheckAuth: vitest.fn().mockImplementation(() => Promise.resolve(true)) };
  const changeLanguage = vitest.fn();
  let router: Router;

  beforeEach(() => {
    router = createRouter();
    loginService = new LoginService({ emit: vitest.fn() });
    vitest.spyOn(loginService, 'openLogin');
  });

  const createWrapper = (initialState = {}) => {
    const pinia = createTestingPinia({
      stubActions: false,
      initialState: {
        main: initialState,
      },
    });
    return {
      wrapper: shallowMount(JhiNavbar, {
        global: {
          plugins: [pinia, router],
          stubs: {
            'font-awesome-icon': true,
            'b-navbar': true,
            'b-navbar-nav': true,
            'b-dropdown-item': true,
            'b-collapse': true,
            'b-nav-item': true,
            'b-nav-item-dropdown': true,
            'b-navbar-toggle': true,
            'b-navbar-brand': true,
          },
          provide: {
            loginService,
            currentLanguage: computed(() => 'foo'),
            changeLanguage,
            accountService,
          },
        },
      }),
      pinia,
    };
  };

  it('should not have user data set', async () => {
    const { wrapper } = createWrapper();
    await nextTick();
    const jhiNavbar = wrapper.vm;
    expect(jhiNavbar.authenticated).toBeFalsy();
    expect(jhiNavbar.openAPIEnabled).toBeFalsy();
    expect(jhiNavbar.inProduction).toBeFalsy();
  });

  it('should have user data set after authentication', async () => {
    const { wrapper } = createWrapper({
      authenticated: true,
      userIdentity: { login: 'test' },
    });
    await nextTick();
    const jhiNavbar = wrapper.vm;
    expect(jhiNavbar.authenticated).toBeTruthy();
  });

  it('should have profile info set after info retrieved', async () => {
    const { wrapper } = createWrapper({
      activeProfiles: ['prod', 'api-docs'],
    });
    await nextTick();
    const jhiNavbar = wrapper.vm;
    expect(jhiNavbar.openAPIEnabled).toBeTruthy();
    expect(jhiNavbar.inProduction).toBeTruthy();
  });

  it('should use login service', async () => {
    const { wrapper } = createWrapper();
    await nextTick();
    wrapper.vm.openLogin();
    expect(loginService.openLogin).toHaveBeenCalled();
  });

  it('should use account service', async () => {
    const { wrapper } = createWrapper();
    await nextTick();
    wrapper.vm.hasAnyAuthority('auth');
    expect(accountService.hasAnyAuthorityAndCheckAuth).toHaveBeenCalled();
  });

  it('logout should clear credentials', async () => {
    const { wrapper, pinia } = createWrapper({
      userIdentity: { login: 'test' },
    });
    const jhiNavbar = wrapper.vm;
    const store = useStore(pinia);

    await jhiNavbar.logout();

    await nextTick();

    expect(store.logout).toHaveBeenCalled();
    expect(jhiNavbar.authenticated).toBeFalsy();
  });

  it('should determine active route', async () => {
    const { wrapper } = createWrapper();
    await nextTick();
    const jhiNavbar = wrapper.vm;
    await router.push('/toto');

    expect(jhiNavbar.subIsActive('/titi')).toBeFalsy();
    expect(jhiNavbar.subIsActive('/toto')).toBeTruthy();
    expect(jhiNavbar.subIsActive(['/toto', 'toto'])).toBeTruthy();
  });
});
