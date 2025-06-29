import { useAccountStore } from '@/shared/config/store/account-store';

export default class LoginService {
  private emit: (event: string, ...args: any[]) => void;

  constructor({ emit }: { emit: (event: string, ...args: any[]) => void }) {
    this.emit = emit;
  }

  public openLogin(): void {
    this.emit('bv::show::modal', 'login-page');
  }

  public hideLogin(): void {
    this.emit('bv::hide::modal', 'login-page');
  }

  public getUserRole(): string | null {
    const accountStore = useAccountStore();
    const account = accountStore.account; // Obtiene el estado del usuario

    // Verifica roles y devuelve el más alto
    if (account && account.authorities) {
      if (account.authorities.includes('ROLE_ADMIN')) {
        return 'ROLE_ADMIN';
      }
      if (account.authorities.includes('ROLE_USER')) {
        return 'ROLE_USER';
      }
    }
    return null; // Si no hay roles
  }
}
