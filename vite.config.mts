import { URL, fileURLToPath } from 'node:url';
import { existsSync } from 'node:fs';
import { defineConfig, normalizePath } from 'vite';

import vue from '@vitejs/plugin-vue';
import { viteStaticCopy, type Target } from 'vite-plugin-static-copy';

const getFileFromRepo = (file: string) =>
  existsSync(fileURLToPath(new URL(`../node_modules/${file}`, import.meta.url)))
    ? fileURLToPath(new URL(`../node_modules/${file}`, import.meta.url))
    : fileURLToPath(new URL(`./node_modules/${file}`, import.meta.url));

const { getAbsoluteFSPath } = await import('swagger-ui-dist');
const swaggerUiPath = getAbsoluteFSPath();

// Receives the command ('build' or 'serve') build only applies for production mode
export default defineConfig(({ command }) => {
  // Define the base targets for viteStaticCopy, which are always active
  const copyTargets: Target[] = [
    {
      src: [
        `${normalizePath(swaggerUiPath)}/*.{js,css,html,png}`,
        `!${normalizePath(swaggerUiPath)}/**/index.html`,
        normalizePath(getFileFromRepo('axios/dist/axios.min.js')),
        normalizePath(fileURLToPath(new URL('./src/main/webapp/swagger-ui/index.html', import.meta.url))),
      ],
      dest: 'swagger-ui',
    },
  ];

  if (command === 'build') {
    copyTargets.push({
      src: normalizePath(fileURLToPath(new URL('./src/main/webapp/content', import.meta.url))),
      dest: '.',
    });
  }
  return {
    plugins: [
      vue(),
      viteStaticCopy({
        targets: copyTargets,
      }),
    ],
    root: fileURLToPath(new URL('./src/main/webapp/', import.meta.url)),
    publicDir: fileURLToPath(new URL('./target/classes/static/public', import.meta.url)),
    cacheDir: fileURLToPath(new URL('./target/.vite-cache', import.meta.url)),
    build: {
      emptyOutDir: true,
      outDir: fileURLToPath(new URL('./target/classes/static/', import.meta.url)),
      rollupOptions: {
        input: {
          app: fileURLToPath(new URL('./src/main/webapp/index.html', import.meta.url)),
        },
      },
    },
    resolve: {
      alias: {
        vue: '@vue/compat/dist/vue.esm-bundler.js',
        '@': fileURLToPath(new URL('./src/main/webapp/app/', import.meta.url)),
      },
    },
    define: {
      I18N_HASH: '"generated_hash"',
      SERVER_API_URL: '"/"',
      APP_VERSION: `"${process.env.APP_VERSION ? process.env.APP_VERSION : 'DEV'}"`,
    },
    server: {
      host: true,
      port: 9000,
      proxy: Object.fromEntries(
        ['/api', '/management', '/v3/api-docs'].map(res => [
          res,
          {
            target: 'http://localhost:8080',
          },
        ]),
      ),
    },
  };
});

// jhipster-needle-add-vite-config - JHipster will add custom config
