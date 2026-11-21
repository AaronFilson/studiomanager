import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { capped, renderWithQueries, stubApi } from '../test/api';
import { Financials } from './Financials';

const studio = {
  id: 's1',
  name: 'Clay Works',
  unitSystem: 'METRIC' as const,
  timeZone: 'Europe/London',
  country: 'GB',
  currency: 'GBP',
  myRole: 'OWNER' as const,
  memberCount: 10,
  createdAt: '2026-01-01T00:00:00Z',
};

const members = [
  { accountId: 'm1', displayName: 'Mo Maker', role: 'MEMBER' as const, roleId: 'member', roleName: 'Member', joinedAt: '2026-01-01T00:00:00Z' },
  { accountId: 'm2', displayName: 'Pat Potter', role: 'MEMBER' as const, roleId: 'member', roleName: 'Member', joinedAt: '2026-01-02T00:00:00Z' },
];

describe('Financials', () => {
  it('records payments, redeems prepaid cards using a member picker, and displays payables with W-9 warning', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId': studio,
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'OWNER',
        roleName: 'Owner',
        permissions: [
          'money.read',
          'money.take_payment',
          'money.stored_value.manage',
          'money.payables.manage',
          'members.read',
        ],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/balances': [
        { memberId: 'm1', memberName: 'Mo Maker', owed: '24.50' },
        { memberId: 'm2', memberName: 'Pat Potter', owed: '-10.00' },
      ],
      'GET /api/v1/studios/:studioId/members': members,
      'POST /api/v1/studios/:studioId/payments': null,
      'GET /api/v1/studios/:studioId/stored-values': capped([
        {
          id: 'sv1',
          kind: 'GIFT_CARD',
          code: 'GIFT-123',
          label: 'Holiday card',
          remaining: '50.00',
          expiresOn: '2027-12-31',
        },
      ]),
      'POST /api/v1/studios/:studioId/stored-values/:storedValueId/uses': null,
      'GET /api/v1/studios/:studioId/payees': capped([
        {
          id: 'pay1',
          name: 'Ada Artist',
          files1099: true,
          consignorShare: '0.6',
          discountsFrom: 'SHARED',
          archived: false,
          owed: '120.00',
        },
      ]),
    });

    await renderWithQueries(<Financials studioId="s1" />);

    expect(await screen.findByRole('header', { name: 'Mo Maker' })).toBeOnTheScreen();
    expect(screen.getByText(/Owes .*24\.50/)).toBeOnTheScreen();
    expect(screen.getByText(/10\.00 in credit/)).toBeOnTheScreen();

    // Record payment for Mo Maker via SQUARE
    const recordButtons = screen.getAllByRole('button', { name: 'Record payment' });
    const moRecord = recordButtons[0];
    if (!moRecord) throw new Error('Expected Record payment button');
    await fireEvent.press(moRecord);
    await fireEvent.press(screen.getByRole('button', { name: 'Square' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Save payment' }));

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/payments',
          body: { memberId: 'm1', amount: '24.50', provider: 'SQUARE' },
        }),
      ),
    );

    // Switch to Prepaid cards and redeem towards Pat Potter via member picker
    await fireEvent.press(screen.getByRole('button', { name: 'Prepaid cards' }));
    expect(await screen.findByText('GIFT-123')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Redeem' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Pat Potter' }));
    await fireEvent.changeText(screen.getByLabelText('Redeem amount (GBP)'), '15');
    await fireEvent.press(screen.getByRole('button', { name: 'Apply to balance' }));

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/stored-values/sv1/uses',
          body: { memberId: 'm2', amount: '15' },
        }),
      ),
    );

    // Switch to Payables
    await fireEvent.press(screen.getByRole('button', { name: 'Payables' }));
    expect(await screen.findByText('Ada Artist')).toBeOnTheScreen();
    expect(screen.getByText('60% to artist')).toBeOnTheScreen();
    expect(screen.getByText('W-9 missing')).toBeOnTheScreen();
  });

  it('hides payment controls and prepaid tab without those permissions and skips /members', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId': studio,
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'STAFF',
        roleName: 'Bookkeeper',
        permissions: ['money.read'],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/balances': [{ memberId: 'm1', memberName: 'Mo Maker', owed: '24.50' }],
    });

    await renderWithQueries(<Financials studioId="s1" />);

    expect(await screen.findByRole('header', { name: 'Mo Maker' })).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Prepaid cards' })).toBeNull();
    expect(screen.queryByText('Record a payment')).toBeNull();
    expect(calls.some((c) => c.path.includes('/members'))).toBe(false);
    expect(calls.some((c) => c.path.includes('/stored-values'))).toBe(false);
  });
});
