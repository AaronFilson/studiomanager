import type { IntakePieceRequest, MovePieceRequest, Piece } from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aPiece, aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const studio = aStudio({ unitSystem: 'US_CUSTOMARY' });

describe('pieces', () => {
  it('lets a member drop off a piece in the studio units and see its fee', async () => {
    const member = aStudio({ ...studio, myRole: 'MEMBER' });
    let posted: IntakePieceRequest | undefined;
    let pieces: Piece[] = [];
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(member)),
      http.get(`${API}/api/v1/studios/:studioId/pieces`, () => HttpResponse.json(pieces)),
      http.post(`${API}/api/v1/studios/:studioId/pieces`, async ({ request }) => {
        posted = (await request.json()) as IntakePieceRequest;
        const created = aPiece();
        pieces = [created];
        return HttpResponse.json(created, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/pieces`);
    expect(await screen.findByRole('heading', { name: 'My pieces' })).toBeInTheDocument();
    // Members don't get the staff sections
    expect(screen.queryByRole('link', { name: 'Members' })).not.toBeInTheDocument();

    await user.type(screen.getByLabelText('Description'), 'Lidded jar');
    await user.type(screen.getByLabelText('Length (in)'), '12');
    await user.type(screen.getByLabelText('Width (in)'), '10');
    await user.type(screen.getByLabelText('Height (in)'), '8');
    await user.click(screen.getByRole('button', { name: 'Drop off' }));

    expect(await screen.findByText('Piece 42 is in.')).toBeInTheDocument();
    expect(posted).toEqual({
      description: 'Lidded jar',
      size: { dimensions: { length: '12', width: '10', height: '8', unit: 'INCH' } },
    });
    const row = (await screen.findByRole('link', { name: '42' })).closest('tr');
    expect(row).not.toBeNull();
    expect(within(row as HTMLElement).getByText('$48.00')).toBeInTheDocument();
  });

  it('lets staff move a piece on, sending the version they saw', async () => {
    let moved: MovePieceRequest | undefined;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/pieces/:pieceId`, () => HttpResponse.json(aPiece())),
      http.get(`${API}/api/v1/studios/:studioId/pieces`, () => HttpResponse.json([])),
      http.post(`${API}/api/v1/studios/:studioId/pieces/:pieceId/moves`, async ({ request }) => {
        moved = (await request.json()) as MovePieceRequest;
        return HttpResponse.json(
          aPiece({
            stage: 'BISQUE_FIRED',
            version: 1,
            nextStages: ['GLAZED', 'READY'],
            timeline: [
              ...aPiece().timeline,
              { from: 'RECEIVED', to: 'BISQUE_FIRED', note: 'Cone 04', actorName: 'Olive', actorAccountId: 'a2', occurredAt: '2026-10-20T15:00:00Z' },
            ],
          }),
        );
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/pieces/${aPiece().id}`);
    expect(await screen.findByRole('heading', { name: 'Piece 42: Lidded jar' })).toBeInTheDocument();

    await user.type(screen.getByLabelText('Note (optional)'), 'Cone 04');
    await user.click(screen.getByRole('button', { name: 'Bisque fired' }));

    expect(await screen.findByText('Cone 04')).toBeInTheDocument();
    expect(moved).toEqual({ to: 'BISQUE_FIRED', expectedVersion: 0, note: 'Cone 04' });
    expect(screen.getByRole('button', { name: 'Ready for pickup' })).toBeInTheDocument();
  });
});

describe('invites', () => {
  it('accepts an invite and opens the studio', async () => {
    server.use(
      http.post(`${API}/api/v1/invites/:code/accept`, ({ params }) =>
        params.code === 'invite-code-123'
          ? HttpResponse.json({ studio: { id: studio.id, name: studio.name }, role: 'MEMBER' })
          : new HttpResponse(null, { status: 404 }),
      ),
      http.get(`${API}/api/v1/me`, () => HttpResponse.json({ account: { id: 'a1' }, memberships: [] })),
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(aStudio({ myRole: 'MEMBER' }))),
    );

    const { user, router } = renderApp('/invites/invite-code-123');
    await user.click(await screen.findByRole('button', { name: 'Accept the invite' }));

    expect(await screen.findByRole('heading', { name: 'Hello, Clay Collective' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe(`/studios/${studio.id}`);
  });
});
