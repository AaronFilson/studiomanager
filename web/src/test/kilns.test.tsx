import type { Firing, Kiln, LoadKilnRequest, Notification, QueueEntry } from '@studiomanager/api-client';
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aPiece, aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const studio = aStudio();
const kiln: Kiln = {
  id: 'k1',
  name: 'Skutt 1027',
  kind: 'ELECTRIC',
  maxPiecesPerMember: 1,
  maintenanceEveryFirings: 50,
  firingsSinceMaintenance: 50,
  totalFirings: 120,
  maintenanceDue: true,
  available: true,
  version: 3,
};

describe('kilns', () => {
  it('fills a load from the queue, leaving pieces over the per-member cap', async () => {
    const queue: QueueEntry[] = [
      { piece: aPiece({ id: 'p1', number: 1 }), waitingSince: '2026-10-01T10:00:00Z', fitsNextLoad: true },
      { piece: aPiece({ id: 'p2', number: 2 }), waitingSince: '2026-10-02T10:00:00Z', fitsNextLoad: false },
      { piece: aPiece({ id: 'p3', number: 3, makerName: 'Nia' }), waitingSince: '2026-10-03T10:00:00Z', fitsNextLoad: true },
    ];
    let loaded: LoadKilnRequest | undefined;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/kilns`, () => HttpResponse.json([kiln])),
      http.get(`${API}/api/v1/studios/:studioId/kilns/:kilnId/firings`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/kilns/:kilnId/maintenance`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/kilns/:kilnId/queue`, () => HttpResponse.json(queue)),
      http.post(`${API}/api/v1/studios/:studioId/kilns/:kilnId/firings`, async ({ request }) => {
        loaded = (await request.json()) as LoadKilnRequest;
        const firing: Firing = {
          id: 'f1',
          kilnId: 'k1',
          kilnName: kiln.name,
          kind: 'BISQUE',
          status: 'LOADING',
          createdAt: '2026-10-20T10:00:00Z',
          pieceCount: 2,
          version: 0,
          pieces: [],
        };
        return HttpResponse.json(firing, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/kilns/k1`);
    expect(await screen.findByRole('heading', { name: 'Skutt 1027' })).toBeInTheDocument();
    expect(screen.getByText('· maintenance due')).toBeInTheDocument();

    await user.click(await screen.findByRole('button', { name: 'Fill from the queue' }));
    await user.type(screen.getByLabelText('Cone'), '04');
    await user.click(screen.getByRole('button', { name: 'Load 2 pieces' }));

    await expect.poll(() => loaded).toEqual({ kind: 'BISQUE', pieceIds: ['p1', 'p3'], cone: '04' });
  });

  it("shows a member their ready notice on the studio's home page", async () => {
    const notice: Notification = {
      id: 'n1',
      kind: 'PIECES_READY',
      title: 'Your 2 pieces are ready to collect',
      body: 'Piece 1: Jar\nPiece 2: Bowl\nPlease collect by 19 Nov 2026.',
      createdAt: '2026-10-20T10:00:00Z',
    };
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(aStudio({ myRole: 'MEMBER' }))),
      http.get(`${API}/api/v1/studios/:studioId/notifications`, () => HttpResponse.json([notice])),
    );

    renderApp(`/studios/${studio.id}`);

    expect(await screen.findByText('Your 2 pieces are ready to collect')).toBeInTheDocument();
    expect(screen.getByText(/Please collect by 19 Nov 2026/)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Tell every member' })).not.toBeInTheDocument();
  });
});
