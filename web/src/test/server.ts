import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

/** API origin the tests point the client at; MSW handlers match on it. */
export const API = 'http://api.test';

/** A capped list response, as the API sends it: these rows, none left out. */
export function capped<T>(items: T[], truncated = false, limit = 2000) {
  return { items, truncated, limit };
}

/**
 * Requests every studio page makes in passing, answered with "nothing" so tests only set up what
 * they are about. Per-test handlers are added with `server.use(...)` and take precedence.
 * Unmatched requests are errors (see setup.ts).
 */
const background = [
  http.get(`${API}/api/v1/studios/:studioId/notifications`, () => HttpResponse.json(capped([]))),
  http.get(`${API}/api/v1/studios/:studioId/maintenance/my-tasks`, () => HttpResponse.json([])),
  http.get(`${API}/api/v1/studios/:studioId/maintenance/assets/:assetId/suggested-tasks`, () => HttpResponse.json([])),
  http.get(`${API}/api/v1/studios/:studioId/maintenance/problem-reports`, () => HttpResponse.json(capped([]))),
  http.get(`${API}/api/v1/studios/:studioId/maintenance/reportable-assets`, () => HttpResponse.json([])),
  http.get(`${API}/api/v1/me`, () => HttpResponse.json({ account: { id: 'a1' }, memberships: [] })),
  // Staff pages load the member list for pickers, such as whose piece it is
  http.get(`${API}/api/v1/studios/:studioId/members`, () => HttpResponse.json([])),
  // Maintenance forms offer parts from supplies
  http.get(`${API}/api/v1/studios/:studioId/supplies`, () => HttpResponse.json([])),
  http.get(`${API}/api/v1/studios/:studioId/members/:accountId/statement`, ({ params }) =>
    HttpResponse.json({ memberId: params.accountId, owed: '0', storedValues: [] }),
  ),
];

export const server = setupServer(...background);
