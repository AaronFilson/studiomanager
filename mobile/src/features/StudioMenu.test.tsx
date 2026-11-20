import { fireEvent, screen } from '@testing-library/react-native';

import { renderWithQueries, stubApi } from '../test/api';
import { StudioMenu } from './StudioMenu';

const studio = {
  id: 's1',
  name: 'Clay Works',
  unitSystem: 'US_CUSTOMARY' as const,
  timeZone: 'America/Chicago',
  country: 'US',
  currency: 'USD',
  myRole: 'OWNER' as const,
  memberCount: 24,
  createdAt: '2026-01-01T00:00:00Z',
};

describe('StudioMenu', () => {
  it('shows staff operations gated by permissions and fires navigation, switch studio, and sign out', async () => {
    stubApi({
      'GET /api/v1/studios/:studioId': studio,
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'OWNER',
        roleName: 'Owner',
        permissions: [
          'today.read',
          'kilns.manage',
          'supplies.manage',
          'maintenance.manage',
          'proposals.review',
          'money.read',
          'money.payables.manage',
          'members.read',
          'notices.send',
        ],
        duties: [],
      },
    });
    const onNavigate = jest.fn();
    const onSwitchStudio = jest.fn();
    const onSignOut = jest.fn();

    await renderWithQueries(
      <StudioMenu
        studioId="s1"
        onNavigate={onNavigate}
        onSwitchStudio={onSwitchStudio}
        onSignOut={onSignOut}
      />,
    );

    expect(await screen.findByText('Clay Works')).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Today' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Kilns' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Supplies' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Maintenance' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Assistant suggestions' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Money & payables' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Members' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Send a studio notice' })).toBeOnTheScreen();

    await fireEvent.press(screen.getByRole('button', { name: 'Kilns' }));
    expect(onNavigate).toHaveBeenCalledWith('kilns');

    await fireEvent.press(screen.getByRole('button', { name: 'Switch studio' }));
    expect(onSwitchStudio).toHaveBeenCalledTimes(1);

    await fireEvent.press(screen.getByRole('button', { name: 'Sign out' }));
    expect(onSignOut).toHaveBeenCalledTimes(1);
  });

  it('hides staff links for members and surfaces My consignment when the member sells on consignment', async () => {
    stubApi({
      'GET /api/v1/studios/:studioId': { ...studio, myRole: 'MEMBER' },
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'MEMBER',
        roleName: 'Member',
        permissions: [],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/my-consignment': {
        payeeId: 'pay1',
        name: 'Mo Maker',
        owed: '42.00',
        items: [],
        sales: [],
      },
    });
    const onNavigate = jest.fn();

    await renderWithQueries(
      <StudioMenu
        studioId="s1"
        onNavigate={onNavigate}
        onSwitchStudio={jest.fn()}
        onSignOut={jest.fn()}
      />,
    );

    expect(await screen.findByRole('button', { name: 'Glaze book' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Roster' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Calendar' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'My statement' })).toBeOnTheScreen();
    expect(await screen.findByRole('button', { name: 'My consignment' })).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Report a problem' })).toBeOnTheScreen();

    expect(screen.queryByRole('button', { name: 'Today' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Kilns' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Members' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Money & payables' })).toBeNull();

    await fireEvent.press(screen.getByRole('button', { name: 'My consignment' }));
    expect(onNavigate).toHaveBeenCalledWith('statement');
  });
});
