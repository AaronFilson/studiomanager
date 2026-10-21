import type { MaintenanceTask } from '@studiomanager/api-client';
import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { renderWithQueries, stubApi } from '../test/api';
import { MyTasks } from './MyTasks';
import { ProblemReportForm } from './ProblemReportForm';

const trap: MaintenanceTask = {
  id: 't1',
  assetId: 'a1',
  assetName: 'Sink trap',
  title: 'Empty the trap',
  checklist: ['Scoop the settled clay', 'Wipe the baffles'],
  everyDays: 7,
  assignedDuty: 'MONITOR',
  lastDoneAt: '2026-10-13T12:00:00Z',
  dueAt: '2026-10-20T12:00:00Z',
  status: 'DUE',
  version: 0,
};

describe('my tasks', () => {
  it('ticks the checklist and marks a task done', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/maintenance/my-tasks': [trap],
      'POST /api/v1/studios/:studioId/maintenance/tasks/:taskId/completions': { ...trap, status: 'OK' },
    });

    await renderWithQueries(<MyTasks studioId="s1" />);
    expect(await screen.findByText('Empty the trap')).toBeOnTheScreen();
    expect(screen.getByText('is due')).toBeOnTheScreen();

    await fireEvent.press(screen.getByRole('button', { name: 'Mark done' }));
    await fireEvent.press(screen.getByRole('checkbox', { name: 'Scoop the settled clay' }));
    await fireEvent.changeText(screen.getByLabelText('Notes (optional)'), 'Half full');
    await fireEvent.press(screen.getByRole('button', { name: 'Done' }));

    await waitFor(() =>
      expect(calls.find((c) => c.method === 'POST')).toMatchObject({
        path: '/api/v1/studios/s1/maintenance/tasks/t1/completions',
        body: { checked: ['Scoop the settled clay'], note: 'Half full' },
      }),
    );
  });
});

describe('reporting a problem', () => {
  it('sends what the member picked and wrote', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/maintenance/reportable-assets': [
        { id: 'a1', kind: 'WHEEL', name: 'Wheel 3', available: true },
        { id: 'a2', kind: 'KILN', name: 'Skutt', available: false },
      ],
      'POST /api/v1/studios/:studioId/maintenance/problem-reports': {
        id: 'p1',
        assetId: 'a1',
        assetName: 'Wheel 3',
        description: 'Grinds',
        reportedBy: 'm1',
        reportedAt: '2026-10-21T15:00:00Z',
        open: true,
        version: 0,
      },
    });

    await renderWithQueries(<ProblemReportForm studioId="s1" />);
    expect(await screen.findByRole('button', { name: 'Skutt (already out of service)' })).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Wheel 3' }));
    await fireEvent.changeText(screen.getByLabelText("What's wrong?"), 'Grinds when it speeds up');
    await fireEvent.press(screen.getByRole('button', { name: 'Send report' }));

    await waitFor(() =>
      expect(calls.find((c) => c.method === 'POST')?.body).toEqual({
        assetId: 'a1',
        description: 'Grinds when it speeds up',
      }),
    );
    expect(await screen.findByText('Thanks, the staff have been told.')).toBeOnTheScreen();
  });
});
