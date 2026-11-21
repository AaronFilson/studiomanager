import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { capped, renderWithQueries, stubApi } from '../test/api';
import { RosterSchedule } from './RosterSchedule';

const start = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
const end = new Date(Date.now() + 27 * 60 * 60 * 1000).toISOString();

describe('RosterSchedule', () => {
  it('shows My hours and supports taking, covering, offering for cover, and marking worked', async () => {
    const calls = stubApi({
      'GET /api/v1/me': { account: { id: 'm1', displayName: 'Mo Maker' }, memberships: [] },
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'STAFF',
        roleName: 'Staff',
        permissions: ['schedule.manage', 'schedule.hours.manage'],
        duties: ['MONITOR'],
      },
      'GET /api/v1/studios/:studioId/hours/:accountId': {
        accountId: 'm1',
        accountName: 'Mo Maker',
        totalMinutes: 180,
        entries: [
          {
            id: 'h1',
            minutes: 180,
            note: 'Saturday monitor',
            creditedAt: '2026-11-15T12:00:00Z',
          },
        ],
        truncated: false,
        limit: 200,
      },
      'GET /api/v1/studios/:studioId/shifts': capped([
        {
          id: 'sh-open',
          title: 'Open desk shift',
          duty: 'MONITOR',
          startsAt: start,
          endsAt: end,
          open: true,
          offered: false,
          minutes: 180,
          version: 1,
        },
        {
          id: 'sh-offered',
          title: 'Kiln unload helper',
          startsAt: start,
          endsAt: end,
          accountId: 'm2',
          accountName: 'Pat Potter',
          open: false,
          offered: true,
          minutes: 120,
          version: 2,
        },
        {
          id: 'sh-mine',
          title: 'Evening monitor',
          startsAt: start,
          endsAt: end,
          accountId: 'm1',
          accountName: 'Mo Maker',
          open: false,
          offered: false,
          minutes: 180,
          version: 3,
        },
      ]),
      'PUT /api/v1/studios/:studioId/shifts/:shiftId/assignment': {
        id: 'sh-open',
        title: 'Open desk shift',
        startsAt: start,
        endsAt: end,
        accountId: 'm1',
        accountName: 'Mo Maker',
        open: false,
        offered: false,
        minutes: 180,
        version: 2,
      },
      'PUT /api/v1/studios/:studioId/shifts/:shiftId/offer': {
        id: 'sh-mine',
        title: 'Evening monitor',
        startsAt: start,
        endsAt: end,
        accountId: 'm1',
        accountName: 'Mo Maker',
        open: false,
        offered: true,
        minutes: 180,
        version: 4,
      },
      'POST /api/v1/studios/:studioId/shifts/:shiftId/worked': {
        id: 'sh-mine',
        title: 'Evening monitor',
        startsAt: start,
        endsAt: end,
        accountId: 'm1',
        accountName: 'Mo Maker',
        open: false,
        offered: false,
        minutes: 180,
        workedAt: end,
        version: 4,
      },
    });

    await renderWithQueries(<RosterSchedule studioId="s1" />);

    expect(await screen.findByText('My hours: 3 h 00 min')).toBeOnTheScreen();
    expect(screen.getByText('Open desk shift')).toBeOnTheScreen();
    expect(screen.getByText('Kiln unload helper')).toBeOnTheScreen();

    await fireEvent.press(screen.getByRole('button', { name: 'Take shift' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/shifts/sh-open/assignment',
          body: { accountId: 'm1', expectedVersion: 1 },
        }),
      ),
    );

    await fireEvent.press(screen.getByRole('button', { name: 'Cover shift' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/shifts/sh-offered/assignment',
          body: { accountId: 'm1', expectedVersion: 2 },
        }),
      ),
    );

    await fireEvent.press(screen.getByRole('button', { name: 'Ask for cover' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/shifts/sh-mine/offer',
          body: { offered: true, expectedVersion: 3 },
        }),
      ),
    );

    const markWorkedButtons = screen.getAllByRole('button', { name: 'Mark worked' });
    const myWorked = markWorkedButtons[1];
    if (!myWorked) throw new Error('Expected Mark worked button for own shift');
    await fireEvent.press(myWorked);
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/shifts/sh-mine/worked',
          body: { expectedVersion: 3 },
        }),
      ),
    );
  });
});
