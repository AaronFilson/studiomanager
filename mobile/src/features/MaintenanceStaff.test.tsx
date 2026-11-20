import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { capped, renderWithQueries, stubApi } from '../test/api';
import { MaintenanceStaff } from './MaintenanceStaff';

describe('MaintenanceStaff', () => {
  it('resolves open problem reports and toggles non-kiln asset availability', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/maintenance/assets': [
        {
          id: 'a1',
          kind: 'KILN',
          name: 'Skutt 1027',
          kilnId: 'k1',
          available: true,
          totalFirings: 45,
          version: 1,
          tasks: [],
        },
        {
          id: 'a2',
          kind: 'WHEEL',
          name: 'Wheel 3',
          location: 'North wall',
          available: true,
          version: 4,
          tasks: [
            {
              id: 't1',
              assetId: 'a2',
              assetName: 'Wheel 3',
              title: 'Check belt tension',
              checklist: [],
              everyDays: 30,
              lastDoneAt: '2026-10-01T00:00:00Z',
              status: 'DUE',
              version: 0,
            },
          ],
        },
      ],
      'GET /api/v1/studios/:studioId/maintenance/problem-reports': capped([
        {
          id: 'pr1',
          assetId: 'a2',
          assetName: 'Wheel 3',
          description: 'Pedal sticks at low speed',
          reportedBy: 'm1',
          reportedByName: 'Mo Maker',
          reportedAt: '2026-11-20T09:00:00Z',
          open: true,
          version: 2,
        },
      ]),
      'POST /api/v1/studios/:studioId/maintenance/problem-reports/:reportId/resolution': {
        id: 'pr1',
        assetId: 'a2',
        assetName: 'Wheel 3',
        description: 'Pedal sticks at low speed',
        reportedBy: 'm1',
        reportedAt: '2026-11-20T09:00:00Z',
        open: false,
        resolution: 'Cleaned potentiometer',
        version: 3,
      },
      'PUT /api/v1/studios/:studioId/maintenance/assets/:assetId/availability': {
        id: 'a2',
        kind: 'WHEEL',
        name: 'Wheel 3',
        available: false,
        downReason: 'Waiting on replacement belt',
        version: 5,
        tasks: [],
      },
    });
    const onOpenKiln = jest.fn();

    await renderWithQueries(<MaintenanceStaff studioId="s1" onOpenKiln={onOpenKiln} />);

    expect(await screen.findByText('Pedal sticks at low speed')).toBeOnTheScreen();
    expect(screen.getByText('Check belt tension')).toBeOnTheScreen();

    // Resolve open problem report
    await fireEvent.press(screen.getByRole('button', { name: 'Resolve' }));
    await fireEvent.changeText(screen.getByLabelText('What was done? (optional)'), 'Cleaned potentiometer');
    await fireEvent.press(screen.getByRole('button', { name: 'Confirm resolve' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/maintenance/problem-reports/pr1/resolution',
          body: { expectedVersion: 2, resolution: 'Cleaned potentiometer' },
        }),
      ),
    );

    // Take Wheel 3 out of service
    await fireEvent.press(screen.getByRole('button', { name: 'Take out of service' }));
    await fireEvent.changeText(screen.getByLabelText('Reason Wheel 3 is out of service'), 'Waiting on replacement belt');
    await fireEvent.press(screen.getByRole('button', { name: 'Confirm out of service' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/maintenance/assets/a2/availability',
          body: { downReason: 'Waiting on replacement belt', expectedVersion: 4 },
        }),
      ),
    );

    // Kiln links out to kiln page rather than asset availability
    await fireEvent.press(screen.getByRole('button', { name: 'Open kiln' }));
    expect(onOpenKiln).toHaveBeenCalledWith('k1');
  });
});
