import { screen } from '@testing-library/react-native';

import { renderWithQueries, stubApi } from '../test/api';
import { MyStatement } from './MyStatement';

const studio = {
  id: 's1',
  name: 'Clay Works',
  unitSystem: 'US_CUSTOMARY' as const,
  timeZone: 'America/Chicago',
  country: 'US',
  currency: 'USD',
  myRole: 'MEMBER' as const,
  memberCount: 15,
  createdAt: '2026-01-01T00:00:00Z',
};

describe('MyStatement', () => {
  it('shows own balance, prepaid cards, and consignment summary when the member is a consignor', async () => {
    stubApi({
      'GET /api/v1/me': { account: { id: 'm1', displayName: 'Mo Maker' }, memberships: [] },
      'GET /api/v1/studios/:studioId': studio,
      'GET /api/v1/studios/:studioId/members/:accountId/statement': {
        memberId: 'm1',
        owed: '18.00',
        storedValues: [
          {
            id: 'sv1',
            kind: 'FIRING_CARD',
            label: 'Spring firing card',
            remaining: '35.00',
          },
        ],
      },
      'GET /api/v1/studios/:studioId/my-consignment': {
        payeeId: 'pay1',
        name: 'Mo Maker',
        owed: '64.00',
        items: [
          {
            id: 'ci1',
            payeeId: 'pay1',
            payeeName: 'Mo Maker',
            description: 'Speckled teapot',
            retailPrice: '80.00',
            consignorShare: '0.6',
            discountsFrom: 'SHARED',
            quantity: 2,
            sold: 1,
            returned: 0,
            inStock: 1,
            receivedOn: '2026-10-01',
            dueBack: true,
          },
        ],
        sales: [
          {
            id: 'cs1',
            itemId: 'ci1',
            description: 'Speckled teapot',
            quantity: 1,
            price: '80.00',
            consignorAmount: '48.00',
            tax: '6.40',
            provider: 'SQUARE',
            soldOn: '2026-10-15',
          },
        ],
      },
    });

    await renderWithQueries(<MyStatement studioId="s1" />);

    expect(await screen.findByText(/You owe .*18\.00/)).toBeOnTheScreen();
    expect(screen.getByText(/Spring firing card: .*35\.00 left/)).toBeOnTheScreen();
    expect(screen.getByText(/The studio owes you .*64\.00\./)).toBeOnTheScreen();
    expect(screen.getByText(/Speckled teapot · .*80\.00/)).toBeOnTheScreen();
    expect(screen.getByText('Ready to collect')).toBeOnTheScreen();
    expect(screen.getByText(/1 × Speckled teapot · .*48\.00 to you/)).toBeOnTheScreen();
  });

  it('hides consignment section cleanly when member is not a consignor (404)', async () => {
    stubApi({
      'GET /api/v1/me': { account: { id: 'm1', displayName: 'Mo Maker' }, memberships: [] },
      'GET /api/v1/studios/:studioId': studio,
      'GET /api/v1/studios/:studioId/members/:accountId/statement': {
        memberId: 'm1',
        owed: '0.00',
        storedValues: [],
      },
      'GET /api/v1/studios/:studioId/my-consignment': () =>
        new Response(JSON.stringify({ status: 404, detail: 'Not found' }), {
          status: 404,
          headers: { 'Content-Type': 'application/problem+json' },
        }),
    });

    await renderWithQueries(<MyStatement studioId="s1" />);

    expect(await screen.findByText(/Settled \(.*0\.00\)/)).toBeOnTheScreen();
    expect(screen.getByText('No active prepaid balances.')).toBeOnTheScreen();
    expect(screen.queryByText('My consignment')).toBeNull();
  });
});
