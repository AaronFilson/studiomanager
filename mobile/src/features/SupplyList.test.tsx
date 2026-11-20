import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { renderWithQueries, stubApi } from '../test/api';
import { SupplyList } from './SupplyList';

const clay = {
  id: 'sup1',
  name: 'B-Mix 5',
  category: 'CLAY' as const,
  unit: '25 lb bag',
  supplier: 'Laguna',
  salePrice: '28.00',
  lastPurchaseUnitPrice: '19.50',
  lastPurchasedAt: '2026-10-15T00:00:00Z',
  substitute: 'Use Speckled Buff while out',
  onHand: '2',
  onOrder: '0',
  low: true,
  needsOrdering: true,
  version: 1,
};

const tool = {
  id: 'sup2',
  name: 'Rib #2',
  category: 'TOOL' as const,
  unit: 'each',
  onHand: '10',
  onOrder: '0',
  low: false,
  needsOrdering: false,
  version: 1,
};

describe('SupplyList', () => {
  it('filters supplies, records stock movements, sells to a member, requests more, and decides purchase requests', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId': {
        id: 's1',
        name: 'Clay Works',
        unitSystem: 'US_CUSTOMARY',
        timeZone: 'America/Chicago',
        country: 'US',
        currency: 'USD',
        myRole: 'OWNER',
        memberCount: 5,
        createdAt: '2026-01-01T00:00:00Z',
      },
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'OWNER',
        roleName: 'Owner',
        permissions: ['supplies.manage', 'supplies.purchase.approve'],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/supplies': [clay, tool],
      'GET /api/v1/studios/:studioId/members': [
        { accountId: 'm1', displayName: 'Mo Maker', role: 'MEMBER', roleId: 'member', roleName: 'Member', joinedAt: '2026-01-01T00:00:00Z' },
      ],
      'POST /api/v1/studios/:studioId/supplies/:itemId/movements': {
        id: 'mv1',
        kind: 'USED',
        quantity: '-3',
        createdAt: '2026-11-20T12:00:00Z',
      },
      'POST /api/v1/studios/:studioId/supplies/:itemId/sales': {
        id: 'mv2',
        kind: 'SOLD',
        quantity: '-3',
        memberId: 'm1',
        createdAt: '2026-11-20T12:01:00Z',
      },
      'POST /api/v1/studios/:studioId/purchase-requests': {
        id: 'pr2',
        itemId: 'sup1',
        itemName: 'B-Mix 5',
        quantity: '20',
        status: 'REQUESTED',
        requestedBy: 'a1',
        requestedAt: '2026-11-20T12:02:00Z',
        version: 0,
      },
      'GET /api/v1/studios/:studioId/purchase-requests': [
        {
          id: 'pr1',
          itemId: 'sup1',
          itemName: 'B-Mix 5',
          quantity: '20',
          status: 'REQUESTED',
          requestedBy: 'a2',
          requestedByName: 'Sam Staff',
          requestedAt: '2026-11-19T12:00:00Z',
          version: 2,
        },
      ],
      'PUT /api/v1/studios/:studioId/purchase-requests/:requestId/status': {
        id: 'pr1',
        itemId: 'sup1',
        itemName: 'B-Mix 5',
        quantity: '20',
        status: 'APPROVED',
        requestedBy: 'a2',
        requestedAt: '2026-11-19T12:00:00Z',
        version: 3,
      },
    });

    await renderWithQueries(<SupplyList studioId="s1" />);
    expect(await screen.findByText('B-Mix 5')).toBeOnTheScreen();
    expect(screen.getByText(/Last paid \$19\.50\/25 lb bag/)).toBeOnTheScreen();
    expect(screen.getByText('Substitute: Use Speckled Buff while out')).toBeOnTheScreen();

    // Filter to Clay
    await fireEvent.press(screen.getByRole('button', { name: 'Clay' }));
    expect(screen.queryByText('Rib #2')).toBeNull();

    // Record Used with custom quantity
    await fireEvent.changeText(screen.getByLabelText('Quantity for B-Mix 5'), '3');
    await fireEvent.press(screen.getByRole('button', { name: 'Used' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/supplies/sup1/movements',
          body: { kind: 'USED', quantity: '3' },
        }),
      ),
    );

    // Request more
    await fireEvent.press(screen.getByRole('button', { name: 'Request more' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/purchase-requests',
          body: { itemId: 'sup1' },
        }),
      ),
    );

    // Sell to member
    await fireEvent.press(screen.getByRole('button', { name: 'Sell to member' }));
    await fireEvent.press(await screen.findByRole('button', { name: 'Mo Maker' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Confirm sale' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/supplies/sup1/sales',
          body: { memberId: 'm1', quantity: '3' },
        }),
      ),
    );

    // Approve open purchase request
    await fireEvent.press(screen.getByRole('button', { name: 'Approve' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/purchase-requests/pr1/status',
          body: { to: 'APPROVED', expectedVersion: 2 },
        }),
      ),
    );
  });
});
