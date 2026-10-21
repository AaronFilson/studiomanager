import { tanstackRouter } from '@tanstack/router-plugin/vite';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [
    // Must come before the React plugin: it generates src/routeTree.gen.ts from src/routes.
    // Not split under Vitest: a split route is transformed on first navigation, inside a test's
    // time limit, which on a slow CI runner took longer than the limit. Unsplit, routes are
    // transformed when the test file is imported, before any test's clock starts.
    tanstackRouter({ target: 'react', autoCodeSplitting: !process.env.VITEST }),
    react(),
  ],
  server: {
    port: 5173,
    strictPort: true,
    // Locally the API runs on :8080; proxying keeps the browser on one origin (no CORS).
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    rolldownOptions: {
      output: {
        // Vendor chunks change less often than app code, so browsers keep them cached
        // across deploys. MSAL is the largest dependency.
        codeSplitting: {
          groups: [
            { name: 'msal', test: /[\\/]node_modules[\\/]@azure[\\/]/, priority: 2 },
            { name: 'vendor', test: /[\\/]node_modules[\\/]/, priority: 1 },
          ],
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    restoreMocks: true,
    // Above the 10 s that findBy may wait (see src/test/setup.ts)
    testTimeout: 30000,
  },
});
