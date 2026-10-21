import type { IntakePieceRequest, MovePieceRequest, Piece } from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { preparePhoto } from '../photos';
import { aPiece, aStudio, renderApp } from './renderApp';
import { API, server } from './server';

// jsdom has no canvas to shrink images with, so stand in for it
vi.mock('../photos', () => ({
  preparePhoto: vi.fn(() =>
    Promise.resolve({
      photo: new Blob(['full'], { type: 'image/jpeg' }),
      thumbnail: new Blob(['thumb'], { type: 'image/jpeg' }),
    }),
  ),
}));

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

describe('piece photos', () => {
  const photo = {
    id: '3f0e6f1c-4c7a-4d55-8d2f-2b8b4a100b7f',
    url: 'https://blob.test/full.jpg?sig=r',
    thumbnailUrl: 'https://blob.test/thumb.jpg?sig=r',
    uploadedByAccountId: 'a1',
    uploadedAt: '2026-10-20T15:00:00Z',
  };

  it('lets a member add a photo, uploading the shrunk images straight to storage', async () => {
    const puts: string[] = [];
    let completed = false;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(aStudio({ myRole: 'MEMBER' }))),
      http.get(`${API}/api/v1/studios/:studioId/pieces/:pieceId`, () => HttpResponse.json(aPiece())),
      http.post(`${API}/api/v1/studios/:studioId/pieces/:pieceId/photos`, () =>
        HttpResponse.json(
          {
            photoId: photo.id,
            photoUploadUrl: 'https://blob.test/full.jpg?sig=w',
            thumbnailUploadUrl: 'https://blob.test/thumb.jpg?sig=w',
            expiresAt: '2026-10-20T15:15:00Z',
            maxPhotoBytes: 8388608,
            maxThumbnailBytes: 524288,
          },
          { status: 201 },
        ),
      ),
      http.put('https://blob.test/:name', ({ request }) => {
        puts.push(new URL(request.url).pathname);
        return new HttpResponse(null, { status: 201 });
      }),
      http.post(`${API}/api/v1/studios/:studioId/pieces/:pieceId/photos/:photoId/complete`, () => {
        completed = true;
        return HttpResponse.json(aPiece({ photos: [photo] }));
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/pieces/${aPiece().id}`);
    expect(await screen.findByText(/No photos yet/)).toBeInTheDocument();

    await user.upload(screen.getByLabelText('Add a photo'), new File(['raw'], 'jar.heic', { type: 'image/heic' }));

    expect(await screen.findByRole('img', { name: 'Photo 1 of piece 42' })).toHaveAttribute('src', photo.thumbnailUrl);
    expect(puts.sort()).toEqual(['/full.jpg', '/thumb.jpg']);
    expect(completed).toBe(true);
  });

  it('shows a thumbnail on the shelf and lets staff remove a photo', async () => {
    let deleted: string | undefined;
    let current = aPiece({ photos: [photo] });
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/pieces`, () => HttpResponse.json([current])),
      http.get(`${API}/api/v1/studios/:studioId/pieces/:pieceId`, () => HttpResponse.json(current)),
      http.delete(`${API}/api/v1/studios/:studioId/pieces/:pieceId/photos/:photoId`, ({ params }) => {
        deleted = params.photoId as string;
        current = aPiece();
        return new HttpResponse(null, { status: 204 });
      }),
    );

    const { user, router } = renderApp(`/studios/${studio.id}/pieces`);
    const row = (await screen.findByRole('link', { name: '42' })).closest('tr') as HTMLElement;
    expect(row.querySelector('img')).toHaveAttribute('src', photo.thumbnailUrl);

    await router.navigate({ to: '/studios/$studioId/pieces/$pieceId', params: { studioId: studio.id, pieceId: current.id } });
    await user.click(await screen.findByRole('button', { name: 'Remove photo 1' }));

    expect(await screen.findByText(/No photos yet/)).toBeInTheDocument();
    expect(deleted).toBe(photo.id);
  });

  it('says so when a picked file isn’t an image it can read', async () => {
    vi.mocked(preparePhoto).mockRejectedValueOnce(new Error("This image couldn't be read. Try a JPEG or PNG."));
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/pieces/:pieceId`, () => HttpResponse.json(aPiece())),
    );

    const { user } = renderApp(`/studios/${studio.id}/pieces/${aPiece().id}`);
    await user.upload(await screen.findByLabelText('Add a photo'), new File(['x'], 'notes.png', { type: 'image/png' }));

    expect(await screen.findByRole('alert')).toHaveTextContent("This image couldn't be read. Try a JPEG or PNG.");
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
