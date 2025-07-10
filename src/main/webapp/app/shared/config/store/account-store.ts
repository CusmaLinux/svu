import { Authority } from '@/shared/security/authority';
import { defineStore } from 'pinia';

export interface AccountStateStorable {
  logon: boolean | null | Promise<any>;
  userIdentity: null | any;
  authenticated: boolean;
  profilesLoaded: boolean;
  ribbonOnProfiles: string;
  activeProfiles: string;
}

export const defaultAccountState: AccountStateStorable = {
  logon: null,
  userIdentity: null,
  authenticated: false,
  profilesLoaded: false,
  ribbonOnProfiles: '',
  activeProfiles: '',
};

export const useAccountStore = defineStore('main', {
  state: (): AccountStateStorable => ({ ...defaultAccountState }),
  getters: {
    account: state => state.userIdentity,
    userRole(state): 'admin' | 'functionary' | 'front_desk' | 'user' | 'anonymous' {
      if (!state.authenticated || !state.userIdentity?.authorities) {
        return 'anonymous';
      }
      const authorities = state.userIdentity.authorities;
      if (authorities.includes(Authority.ADMIN)) {
        return 'admin';
      }
      if (authorities.includes(Authority.FRONT_DESK_CS)) {
        return 'front_desk';
      }
      if (authorities.includes(Authority.FUNCTIONARY)) {
        return 'functionary';
      }
      if (authorities.includes(Authority.USER)) {
        return 'user';
      }

      return 'user';
    },
  },
  actions: {
    authenticate(promise) {
      this.logon = promise;
    },
    setAuthentication(identity) {
      this.userIdentity = identity;
      this.authenticated = true;
      this.logon = null;
    },
    logout() {
      this.userIdentity = null;
      this.authenticated = false;
      this.logon = null;
    },
    setProfilesLoaded() {
      this.profilesLoaded = true;
    },
    setActiveProfiles(profile) {
      this.activeProfiles = profile;
    },
    setRibbonOnProfiles(ribbon) {
      this.ribbonOnProfiles = ribbon;
    },
  },
});
