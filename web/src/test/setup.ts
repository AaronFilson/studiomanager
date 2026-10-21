import '@testing-library/jest-dom/vitest';

import { cleanup, configure } from '@testing-library/react';
import { afterAll, afterEach, beforeAll } from 'vitest';

import { server } from './server';

// A test file's first render (React, the router, the query client) can take several seconds on a
// cold CI runner, longer than findBy's default second, while the page still says "Loading".
configure({ asyncUtilTimeout: 10000 });

// jsdom has no layout engine; the router's scroll restoration calls this on navigation.
window.scrollTo = () => undefined;

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }));
afterEach(() => {
  cleanup();
  server.resetHandlers();
  server.events.removeAllListeners();
});
afterAll(() => server.close());
