import type { Today } from '@studiomanager/api-client';
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

describe('studio today', () => {
  it('puts what needs attention first, for whoever is covering', async () => {
    const today: Today = {
      date: '2026-10-20',
      kilns: [
        {
          id: 'k1',
          name: 'Skutt',
          kind: 'ELECTRIC',
          totalFirings: 4,
          available: true,
          version: 1,
        },
      ],
      openFirings: [
        {
          id: 'f1',
          kilnId: 'k1',
          kilnName: 'Skutt',
          kind: 'GLAZE',
          status: 'FIRING',
          createdAt: '2026-10-20T08:00:00Z',
          pieceCount: 23,
          version: 1,
          pieces: [],
        },
      ],
      waitingForBisque: 12,
      waitingForGlaze: 3,
      readyForPickup: 9,
      pastHold: 2,
      unclaimed: 0,
      maintenanceDue: [
        {
          id: 't1',
          assetId: 'a1',
          assetName: 'Skutt',
          title: 'Check element resistance',
          checklist: [],
          everyFirings: 10,
          lastDoneAt: '2026-09-01T12:00:00Z',
          firingsLeft: 1,
          status: 'DUE_SOON',
          version: 0,
        },
      ],
      outOfService: [],
      bookings: [],
      shifts: [
        {
          id: 's1',
          title: 'Open studio monitor',
          startsAt: '2026-10-20T18:00:00Z',
          endsAt: '2026-10-20T21:00:00Z',
          open: true,
          offered: false,
          minutes: 180,
          version: 0,
        },
      ],
      uncoveredShifts: [
        {
          id: 's1',
          title: 'Open studio monitor',
          startsAt: '2026-10-20T18:00:00Z',
          endsAt: '2026-10-20T21:00:00Z',
          open: true,
          offered: false,
          minutes: 180,
          version: 0,
        },
      ],
      lowStock: [
        {
          id: 'i1',
          name: 'B-Mix 5',
          category: 'CLAY',
          unit: '25 lb bag',
          onHand: '3',
          onOrder: '0',
          low: true,
          needsOrdering: true,
          version: 1,
        },
      ],
      openPurchaseRequests: 0,
    };
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(aStudio())),
      http.get(`${API}/api/v1/studios/:studioId/today`, () => HttpResponse.json(today)),
    );

    renderApp(`/studios/${aStudio().id}/today`);

    expect(await screen.findByText('2 ready pieces are past their hold')).toBeInTheDocument();
    expect(screen.getByText('1 shift in the next week has nobody on it')).toBeInTheDocument();
    expect(screen.getByText('firing glaze, 23 pieces')).toBeInTheDocument();
    expect(screen.getByText('B-Mix 5 is low (3 left) and not on order')).toBeInTheDocument();
    expect(screen.getByText('nobody yet')).toBeInTheDocument();
    expect(screen.getByText('Skutt: Check element resistance is due soon (1 firing left)')).toBeInTheDocument();
  });
});
