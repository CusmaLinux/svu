import axios from 'axios';

import { type AccountStore } from '@/store';
import { Authority } from '@/shared/security/authority';

export default class AccountService {
  constructor(private store: AccountStore) {}

  public async update(): Promise<void> {
    if (!this.store.profilesLoaded) {
      await this.retrieveProfiles();
      this.store.setProfilesLoaded();
    }
    await this.loadAccount();
  }

  public async retrieveProfiles(): Promise<boolean> {
    try {
      const res = await axios.get<any>('management/info');
      if (res.data && res.data.activeProfiles) {
        this.store.setRibbonOnProfiles(res.data['display-ribbon-on-profiles']);
        this.store.setActiveProfiles(res.data.activeProfiles);
      }
      return true;
    } catch (error) {
      return false;
    }
  }

  public async retrieveAccount(): Promise<boolean> {
    try {
      const response = await axios.get<any>('api/account');
      if (response.status === 200 && response.data?.login) {
        const account = response.data;
        this.store.setAuthentication(account);
        return true;
      }
    } catch (error) {
      // Ignore error
    }

    this.store.logout();
    return false;
  }

  public async loadAccount() {
    if (this.store.logon) {
      return this.store.logon;
    }
    const token = localStorage.getItem('jhi-authenticationToken') || sessionStorage.getItem('jhi-authenticationToken');
    if (this.authenticated && this.userAuthorities && token) {
      return;
    }

    const promise = this.retrieveAccount();
    this.store.authenticate(promise);
    promise.then(() => this.store.authenticate(null));
    await promise;
  }

  public async hasAnyAuthorityAndCheckAuth(authorities: any): Promise<boolean> {
    if (typeof authorities === 'string') {
      authorities = [authorities];
    }

    return this.checkAuthorities(authorities);
  }

  public hasAnyAuthoritySync(authorities: string | string[]): boolean {
    if (!this.authenticated) return false;
    if (typeof authorities === 'string') {
      authorities = [authorities];
    }
    return this.checkAuthorities(authorities);
  }

  public retrieveMainRole() {
    for (const value of Object.values(Authority)) {
      if (typeof value === 'string') {
        if (this.checkAuthorities([value])) {
          return value;
        }
      }
    }
  }

  public get authenticated(): boolean {
    return this.store.authenticated;
  }

  public get userAuthorities(): string[] {
    return this.store.account?.authorities;
  }

  private checkAuthorities(authorities: string[]): boolean {
    if (this.userAuthorities) {
      for (const authority of authorities) {
        if (this.userAuthorities.includes(authority)) {
          return true;
        }
      }
    }
    return false;
  }
}
