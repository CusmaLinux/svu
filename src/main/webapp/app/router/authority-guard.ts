import type AccountService from '@/account/account.service';
import type { RouteLocationNormalized } from 'vue-router';

export async function checkRouteAuthorities(
  to: RouteLocationNormalized,
  account: Pick<AccountService, 'authenticated' | 'update' | 'hasAnyAuthorityAndCheckAuth'>,
) {
  if (!account.authenticated) await account.update();
  const authorities = to.meta.authorities;
  if (
    Array.isArray(authorities) &&
    authorities.length &&
    (!account.authenticated || !(await account.hasAnyAuthorityAndCheckAuth(authorities)))
  ) {
    return { name: 'Forbidden' };
  }
}
