import { beforeAll } from 'vitest';
import axios from 'axios';
import { config } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { initI18N } from './shared/config/config';

beforeAll(() => {
  window.location.href = 'https://jhipster.tech/';

  // Make sure axios is never executed.
  axios.interceptors.request.use(request => {
    throw new Error(`Error axios should be mocked ${request.url}`);
  });

  const pinia = createPinia();
  setActivePinia(pinia);
  config.global.plugins.push(pinia);
  config.global.plugins.push(initI18N());
});
