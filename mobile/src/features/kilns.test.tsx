import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { aPiece, capped, renderWithQueries, stubApi } from '../test/api';
import { KilnDetail } from './KilnDetail';
import { KilnList } from './KilnList';

const kiln = {
  id: 'k1',
  name: 'Skutt 1027',
  site: 'Main room',
  kind: 'ELECTRIC' as const,
  maxPiecesPerMember: 2,
  totalFirings: 30,
  available: true,
  version: 5,
};

describe('kilns', () => {
  it('lists kilns and opens one when tapped', async () => {
    stubApi({ 'GET /api/v1/studios/:studioId/kilns': [kiln] });
    const onOpen = jest.fn();

    await renderWithQueries(<KilnList studioId="s1" onOpen={onOpen} />);
    expect(await screen.findByText('Skutt 1027 (Main room)')).toBeOnTheScreen();
    expect(screen.getByText('30 firings')).toBeOnTheScreen();

    await fireEvent.press(screen.getByRole('button', { name: 'Skutt 1027' }));
    expect(onOpen).toHaveBeenCalledWith('k1');
  });

  it('fills from queue respecting per-member cap, shows cappedNote, loads kiln, and toggles availability', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'OWNER',
        roleName: 'Owner',
        permissions: ['kilns.read', 'kilns.manage', 'firings.manage'],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/kilns': [kiln],
      'GET /api/v1/studios/:studioId/kilns/:kilnId/firings': capped([], false, 500),
      'GET /api/v1/studios/:studioId/kilns/:kilnId/queue': capped(
        [
          { piece: aPiece({ id: 'p1', number: 11, description: 'Mug' }), waitingSince: '2026-11-18T10:00:00Z', fitsNextLoad: true },
          { piece: aPiece({ id: 'p2', number: 12, description: 'Bowl' }), waitingSince: '2026-11-19T10:00:00Z', fitsNextLoad: false },
        ],
        true,
        1000,
      ),
      'POST /api/v1/studios/:studioId/kilns/:kilnId/firings': {
        id: 'f1',
        kilnId: 'k1',
        kilnName: 'Skutt 1027',
        kind: 'BISQUE',
        cone: '04',
        status: 'LOADING',
        createdAt: '2026-11-20T12:00:00Z',
        pieceCount: 1,
        version: 1,
        pieces: [aPiece({ id: 'p1', number: 11, description: 'Mug' })],
      },
      'PUT /api/v1/studios/:studioId/kilns/:kilnId/availability': {
        ...kiln,
        available: false,
        downReason: 'Element check',
        version: 6,
      },
    });

    await renderWithQueries(<KilnDetail studioId="s1" kilnId="k1" />);
    expect(await screen.findByText(/Showing the longest-waiting 1,000 pieces/)).toBeOnTheScreen();
    expect(screen.getByText('Over cap')).toBeOnTheScreen();

    await fireEvent.changeText(screen.getByLabelText('Cone (optional)'), '04');
    await fireEvent.press(screen.getByRole('button', { name: 'Fill from queue' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Load 1 piece' }));

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/kilns/k1/firings',
          body: { kind: 'BISQUE', pieceIds: ['p1'], cone: '04' },
        }),
      ),
    );

    await fireEvent.changeText(screen.getByLabelText('Out of service reason'), 'Element check');
    await fireEvent.press(screen.getByRole('button', { name: 'Take out of service' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/kilns/k1/availability',
          body: { downReason: 'Element check', expectedVersion: 5 },
        }),
      ),
    );
  });

  it('starts, unloads, and cancels an open firing', async () => {
    const openFiring = {
      id: 'f2',
      kilnId: 'k1',
      kilnName: 'Skutt 1027',
      kind: 'GLAZE' as const,
      cone: '6',
      status: 'FIRING' as const,
      createdAt: '2026-11-20T08:00:00Z',
      pieceCount: 1,
      version: 2,
      pieces: [aPiece({ id: 'p5', number: 77, description: 'Vase' })],
    };
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'OWNER',
        roleName: 'Owner',
        permissions: ['kilns.read', 'kilns.manage', 'firings.manage'],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/kilns': [kiln],
      'GET /api/v1/studios/:studioId/kilns/:kilnId/firings': capped([openFiring], false, 500),
      'GET /api/v1/studios/:studioId/firings/:firingId': openFiring,
      'POST /api/v1/studios/:studioId/firings/:firingId/transitions': { ...openFiring, status: 'COMPLETE', version: 3 },
    });

    await renderWithQueries(<KilnDetail studioId="s1" kilnId="k1" />);
    expect(await screen.findByText('Firing: glaze, cone 6')).toBeOnTheScreen();
    await fireEvent.changeText(screen.getByLabelText('Notes (optional)'), 'Even cone 6 bend');
    await fireEvent.press(screen.getByRole('button', { name: 'Unload and notify makers' }));

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/firings/f2/transitions',
          body: { to: 'COMPLETE', expectedVersion: 2, notes: 'Even cone 6 bend' },
        }),
      ),
    );
  });

  it('skips firings and queue endpoints when caller lacks firings.manage', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'STAFF',
        roleName: 'Kiln maintenance',
        permissions: ['kilns.read', 'kilns.manage'],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/kilns': [kiln],
    });

    await renderWithQueries(<KilnDetail studioId="s1" kilnId="k1" />);
    expect(await screen.findByText('Availability')).toBeOnTheScreen();
    expect(screen.queryByText('Load the kiln')).toBeNull();
    expect(screen.queryByText('Firing log')).toBeNull();
    expect(calls.some((c) => c.path.includes('/firings'))).toBe(false);
    expect(calls.some((c) => c.path.includes('/queue'))).toBe(false);
  });
});
