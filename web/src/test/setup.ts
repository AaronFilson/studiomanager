import '@testing-library/jest-dom/vitest';

import { cleanup, configure } from '@testing-library/react';
import { afterAll, afterEach, beforeAll } from 'vitest';

import { server } from './server';

// Each route is code-split, and its first import is transformed on demand. On a cold CI runner
// that alone can take longer than findBy's default second, while the page still says "Loading".
configure({ asyncUtilTimeout: 5000 });

// jsdom has no layout engine; the router's scroll restoration calls this on navigation.
window.scrollTo = () => undefined;

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }));
afterEach(() => {
  cleanup();
  server.resetHandlers();
  server.events.removeAllListeners();
});
afterAll(() => server.close());
