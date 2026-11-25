import { fileURLToPath } from 'node:url';
import { defineWorkspace } from 'vitest/config';

const managementSpecs = 'src/main/webapp/app/pages/gestion-pqrsd/*.spec.ts';

export default defineWorkspace([
  {
    extends: './vitest.config.mts',
    test: { name: 'SVU', exclude: [managementSpecs] },
  },
  {
    extends: './vitest.config.mts',
    resolve: {
      alias: {
        // Mounts must share the application's Vue compat runtime and effect scope.
        '@vue/test-utils': fileURLToPath(new URL('./node_modules/@vue/test-utils/dist/vue-test-utils.esm-bundler.mjs', import.meta.url)),
      },
    },
    test: {
      name: 'Informes de Gestión PQRSD',
      include: [managementSpecs],
      server: { deps: { inline: ['@vue/test-utils', 'pinia', 'vue-i18n', 'vue-router'] } },
    },
  },
]);
