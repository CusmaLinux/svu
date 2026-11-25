import { Authority } from './authority';

export const statisticsAuthorities = [Authority.ADMIN, Authority.PQRSD_REPORTS];

export function hasStatisticsAccess(authenticated: boolean, authorities: readonly string[] = []): boolean {
  return authenticated && statisticsAuthorities.some(authority => authorities.includes(authority));
}
