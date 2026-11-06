import type { WeeklyActivity } from '@studiomanager/api-client';
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

describe('activity', () => {
  it('shows each week’s totals', async () => {
    const studio = aStudio();
    const activity: WeeklyActivity = {
      streaming: true,
      weeks: [
        {
          weekStart: '2026-11-02',
          firings: 2,
          piecesFired: 40,
          membersJoined: 1,
          kilns: [{ kilnId: 'k1', kiln: 'Big Skutt', firings: 2 }],
          fees: [{ currency: 'USD', amount: '60.00', count: 3 }],
        },
      ],
    };
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/activity/weekly`, () => HttpResponse.json(activity)),
    );

    renderApp(`/studios/${studio.id}/activity`);
    expect(await screen.findByText('Big Skutt 2')).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: '40' })).toBeInTheDocument();
    expect(screen.getByText(/\$60\.00 \(3\)/)).toBeInTheDocument();
  });

  it('says when streaming is off', async () => {
    const studio = aStudio();
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/activity/weekly`, () =>
        HttpResponse.json({ streaming: false, weeks: [] } satisfies WeeklyActivity),
      ),
    );

    renderApp(`/studios/${studio.id}/activity`);
    expect(await screen.findByText(/Event streaming is off/)).toBeInTheDocument();
  });
});
