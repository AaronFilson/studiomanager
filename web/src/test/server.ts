import { setupServer } from 'msw/node';

/** API origin the tests point the client at; MSW handlers match on it. */
export const API = 'http://api.test';

/** Per-test handlers are added with `server.use(...)`. Unmatched requests are errors (see setup.ts). */
export const server = setupServer();
