import type { AssignShiftRequest, OfferShiftRequest, Shift } from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const studio = aStudio({ myRole: 'MEMBER' });
const tomorrow = new Date(Date.now() + 24 * 3600 * 1000);
const later = new Date(tomorrow.getTime() + 3 * 3600 * 1000);

function aShift(overrides: Partial<Shift>): Shift {
  return {
    id: 's1',
    title: 'Open studio monitor',
    duty: 'MONITOR',
    startsAt: tomorrow.toISOString(),
    endsAt: later.toISOString(),
    open: false,
    offered: false,
    minutes: 180,
    version: 1,
    ...overrides,
  };
}

function rosterHandlers(shifts: Shift[]) {
  return [
    http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
    http.get(`${API}/api/v1/studios/:studioId/shifts`, () => HttpResponse.json(shifts)),
    http.get(`${API}/api/v1/studios/:studioId/hours/:accountId`, ({ params }) =>
      HttpResponse.json({ accountId: params.accountId, totalMinutes: 0, entries: [] }),
    ),
  ];
}

describe('roster', () => {
  it('lets whoever has a shift ask for cover', async () => {
    let sent: OfferShiftRequest | undefined;
    server.use(
      ...rosterHandlers([aShift({ accountId: 'a1', accountName: 'Pat Potter' })]),
      http.put(`${API}/api/v1/studios/:studioId/shifts/:shiftId/offer`, async ({ request }) => {
        sent = (await request.json()) as OfferShiftRequest;
        return HttpResponse.json(aShift({ accountId: 'a1', offered: true, version: 2 }));
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/roster`);
    await user.click(await screen.findByRole('button', { name: 'Ask for cover' }));

    await expect.poll(() => sent).toEqual({ offered: true, expectedVersion: 1 });
  });

  it('lets another member cover an offered shift', async () => {
    let taken: AssignShiftRequest | undefined;
    server.use(
      ...rosterHandlers([aShift({ accountId: 'a9', accountName: 'Nia', offered: true })]),
      http.put(`${API}/api/v1/studios/:studioId/shifts/:shiftId/assignment`, async ({ request }) => {
        taken = (await request.json()) as AssignShiftRequest;
        return HttpResponse.json(aShift({ accountId: 'a1', version: 2 }));
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/roster`);
    const row = (await screen.findByText('Open studio monitor')).closest('li');
    expect(row).not.toBeNull();
    expect(within(row as HTMLElement).getByText(/needs cover/)).toBeInTheDocument();
    expect(within(row as HTMLElement).queryByRole('button', { name: 'Ask for cover' })).not.toBeInTheDocument();
    await user.click(within(row as HTMLElement).getByRole('button', { name: 'Cover it' }));

    await expect.poll(() => taken).toEqual({ accountId: 'a1', expectedVersion: 1 });
  });
});
