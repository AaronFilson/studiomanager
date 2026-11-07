import type { Studio } from '@studiomanager/api-client';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';

import { permissionsFor } from './permissions';

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
  // The caller's permissions follow the role of whatever studio the test serves, so a test that
  // sets up "a member's studio" gets a member's permissions without saying so twice
  http.get(`${API}/api/v1/studios/:studioId/me/permissions`, async ({ request, params }) => {
    // Forward the caller's headers, so a test reading the studio request's token still sees it
    const studio = await fetch(`${API}/api/v1/studios/${String(params.studioId)}`, { headers: request.headers });
    if (!studio.ok) {
      return new HttpResponse(null, { status: studio.status });
    }
    const { myRole } = (await studio.json()) as Studio;
    return HttpResponse.json(permissionsFor(myRole));
  }),
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
