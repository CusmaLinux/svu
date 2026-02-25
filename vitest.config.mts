import { fileURLToPath } from 'node:url';
import { mergeConfig } from 'vite';
import { configDefaults, defineConfig } from 'vitest/config';
import viteConfig from './vite.config.mjs';

export default defineConfig(async params => {
  // Check if viteConfig is a function (callback) and await it to get the object
  const clientConfig = typeof viteConfig === 'function' ? await viteConfig(params) : viteConfig;

  return mergeConfig(
    clientConfig,
    defineConfig({
      test: {
        environment: 'happy-dom',
        // 'exclude' prevents Vitest from running these as test files
        exclude: [...configDefaults.exclude, 'src/test/javascript/e2e/**'],
        root: fileURLToPath(new URL('./', import.meta.url)),
        reporters: ['verbose', 'vitest-sonar-reporter'],
        outputFile: {
          'vitest-sonar-reporter': 'target/test-results/TESTS-results-sonar.xml',
        },
        watch: false,
        globals: true,
        setupFiles: ['src/main/webapp/app/test-setup.ts'],
        coverage: {
          provider: 'v8',
          reporter: ['text', 'json', 'lcov', 'text-summary'],
          reportsDirectory: 'target/test-results/coverage',
          exclude: [
            ...configDefaults.coverage.exclude,
            'src/main/webapp/app/router/index.ts',
            'src/main/webapp/app/main.ts',
            'src/main/webapp/app/shared/config/config.ts',
            'src/test/**/*',
            'target/**/*',
          ],
        },
      },
    }),
  );
});
