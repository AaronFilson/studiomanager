import { defineConfig } from '@hey-api/openapi-ts';

// The OpenAPI file in /api is the source of truth. Regenerate with `pnpm run generate`;
// CI fails if the committed output in src/generated differs from a fresh run.
export default defineConfig({
  input: '../../api/src/main/openapi/studiomanager.yaml',
  output: {
    path: 'src/generated',
  },
  plugins: [
    '@hey-api/client-fetch',
    '@hey-api/typescript',
    '@hey-api/sdk',
    '@tanstack/react-query',
  ],
});
