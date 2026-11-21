import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { capped, renderWithQueries, stubApi } from '../test/api';
import { CalendarView } from './CalendarView';

describe('CalendarView', () => {
  it('lists bookings, creates a new reservation, and cancels an existing booking', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'MEMBER',
        roleName: 'Member',
        permissions: [],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/bookings': capped([
        {
          id: 'b1',
          resourceId: 'r1',
          resourceName: 'Wheel room',
          kind: 'RESERVATION',
          title: 'Open practice',
          startsAt: '2026-11-21T15:00:00Z',
          endsAt: '2026-11-21T17:00:00Z',
          accountName: 'Mo Maker',
          mine: true,
        },
      ]),
      'GET /api/v1/studios/:studioId/resources': [
        {
          id: 'r1',
          name: 'Wheel room',
          kind: 'WHEEL_ROOM',
          capacity: 8,
          requiredCertificationName: 'Wheel orientation',
          version: 1,
        },
      ],
      'POST /api/v1/studios/:studioId/bookings': {
        id: 'b2',
        resourceId: 'r1',
        resourceName: 'Wheel room',
        kind: 'RESERVATION',
        title: 'Reservation',
        startsAt: '2026-11-22T10:00:00Z',
        endsAt: '2026-11-22T12:00:00Z',
        mine: true,
      },
      'DELETE /api/v1/studios/:studioId/bookings/:bookingId': null,
    });

    await renderWithQueries(<CalendarView studioId="s1" />);

    expect(await screen.findByText('Open practice (Mo Maker)')).toBeOnTheScreen();
    expect(screen.getByText('Needs the “Wheel orientation” certification.')).toBeOnTheScreen();

    // Cancel own booking
    await fireEvent.press(screen.getByRole('button', { name: 'Cancel booking' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({ method: 'DELETE', path: '/api/v1/studios/s1/bookings/b1' }),
      ),
    );

    // Book a slot
    await fireEvent.changeText(screen.getByLabelText('Date (YYYY-MM-DD)'), '2026-11-22');
    await fireEvent.changeText(screen.getByLabelText('From (HH:MM)'), '10:00');
    await fireEvent.changeText(screen.getByLabelText('Until (HH:MM)'), '12:00');
    await fireEvent.press(screen.getByRole('button', { name: 'Book it' }));

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/bookings',
          body: expect.objectContaining({ resourceId: 'r1', kind: 'RESERVATION' }) as unknown,
        }),
      ),
    );
    expect(await screen.findByText('Booked.')).toBeOnTheScreen();

    // Reject impossible calendar dates like Feb 31
    await fireEvent.changeText(screen.getByLabelText('Date (YYYY-MM-DD)'), '2026-02-31');
    expect(screen.getByRole('button', { name: 'Book it' })).toBeDisabled();
  });
});
