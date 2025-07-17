import { logger } from '@/shared/logger';
declare const grecaptcha: any;

export function useRecaptcha() {
  const recaptchaSiteKey = '6LdUgYUrAAAAANDMJgId1RFXJsgb9wiyhGq9IDZp';

  const getToken = (action: string): Promise<string> => {
    return new Promise((resolve, reject) => {
      if (typeof grecaptcha === 'undefined' || typeof grecaptcha.execute === 'undefined') {
        logger.error('reCAPTCHA script not loaded');
        return reject(new Error('reCAPTCHA script not loaded'));
      }

      grecaptcha.ready(() => {
        grecaptcha
          .execute(recaptchaSiteKey, { action: action })
          .then((token: string) => {
            resolve(token);
          })
          .catch((err: any) => {
            logger.error('Error executing reCAPTCHA:', err);
            reject(err);
          });
      });
    });
  };

  return {
    getToken,
  };
}
