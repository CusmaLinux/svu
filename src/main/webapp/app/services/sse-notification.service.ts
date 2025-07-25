import type { EventSourceMessage } from '@microsoft/fetch-event-source';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { useAccountStore } from '@/shared/config/store/account-store';
import { useNotificationStore } from '@/shared/config/store/notification-store';
import { logger } from '@/shared/logger';
import { NotificationType } from '@/constants';

class SseNotificationService {
  private ctrl: AbortController | null = null;
  private pageVisibilityHandler: (() => void) | null = null;
  private isPageVisible: boolean = !document.hidden;

  constructor() {
    this.setupPageVisibilityListener();
  }

  private setupPageVisibilityListener(): void {
    this.pageVisibilityHandler = () => {
      this.isPageVisible = !document.hidden;
    };
    document.addEventListener('visibilitychange', this.pageVisibilityHandler, false);
  }

  private shouldShowNativeNotification(): boolean {
    return !this.isPageVisible;
  }

  public connect(): string | null {
    const accountStore = useAccountStore();

    const token = localStorage.getItem('jhi-authenticationToken') || sessionStorage.getItem('jhi-authenticationToken');

    if (!accountStore.authenticated || !token || !accountStore.account?.login) {
      this.disconnect();
      return null;
    }
    const userLogin = accountStore.account.login;

    if (this.ctrl && !this.ctrl.signal.aborted) {
      this.disconnect();
      return userLogin;
    }

    this.ctrl = new AbortController();
    const notificationStore = useNotificationStore();

    fetchEventSource('/api/sse-notifications/subscribe', {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'text/event-stream',
      },
      signal: this.ctrl.signal,

      onopen: async (response: any) => {
        if (response.ok && response.headers.get('content-type') === 'text/event-stream') {
          logger.info('SSE connection opened successfully by fetchEventSource for user:', userLogin);
          return;
        } else if (response.status >= 400 && response.status < 500 && response.status !== 429) {
          logger.error(`SSE connection failed (client error): ${response.status} ${response.statusText}`);
          notificationStore.error = `SSE Connection Failed: ${response.status}`;
          throw new Error(`Client error ${response.status}`);
        } else {
          logger.warn(`SSE connection issue (will retry): ${response.status} ${response.statusText}`);
          notificationStore.error = `SSE Connection Issue: ${response.status}`;
        }
      },
      onmessage: (event: EventSourceMessage) => {
        const eventType: string = event.event;
        if (NotificationType[eventType as keyof typeof NotificationType] !== undefined) {
          try {
            const notificationData = JSON.parse(event.data);
            const sseNotif = notificationStore.addSseEvent(notificationData);
            if (sseNotif) {
              if (this.shouldShowNativeNotification()) {
                if (Notification.permission === 'granted') {
                  new Notification(event.event, {
                    icon: '/content/images/logo-itp.png',
                    body: sseNotif.message || `PQRS '${sseNotif.pqrsTitle}'`,
                  });
                }
              }
            }
          } catch (e) {
            logger.error('Error parsing or handling pqrsDueDateReminder event data:', e);
          }
        }
      },
      onclose: () => {
        logger.info('SSE connection closed by fetchEventSource.');
      },
      onerror: (err: any) => {
        logger.error('SSE connection error (fetchEventSource):', err);
        notificationStore.error = `SSE Error: ${err.message || 'Unknown error'}`;

        if (this.ctrl?.signal.aborted) {
          logger.error('SSE error due to abort, stopping retries.');
          throw err;
        }
      },
      openWhenHidden: true,
    }).catch(err => {
      if (err.name !== 'AbortError') {
        logger.error('fetchEventSource fatal error, retries stopped:', err);
        notificationStore.error = `SSE Fatal Error: ${err.message}`;
      }
    });

    return userLogin;
  }

  public disconnect(): void {
    if (this.ctrl) {
      this.ctrl.abort();
      this.ctrl = null;
    }
  }

  public cleanup(): void {
    this.disconnect();
    if (this.pageVisibilityHandler) {
      document.removeEventListener('visibilitychange', this.pageVisibilityHandler);
      this.pageVisibilityHandler = null;
    }
  }
}

const sseNotificationService = new SseNotificationService();
export default sseNotificationService;
