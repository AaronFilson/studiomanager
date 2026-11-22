import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { capped, renderWithQueries, stubApi } from '../test/api';
import { SuggestionList } from './SuggestionList';

describe('SuggestionList', () => {
  it('shows maintenance interval, cappedNote, and approves or rejects suggestions', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/pending-actions': capped(
        [
          {
            id: 'pa1',
            kind: 'MAINTENANCE_TASK',
            status: 'PENDING',
            reason: 'Elements are nearing 100 firings.',
            subjectName: 'Skutt 1027',
            maintenanceTask: {
              assetId: 'a1',
              title: 'Check relays',
              everyDays: 30,
              everyFirings: 20,
            },
            proposedBy: 'u1',
            proposedByName: 'Olive Owner',
            proposedViaTokenName: 'Claude desktop',
            proposedAt: '2026-11-20T10:00:00Z',
            version: 2,
          },
          {
            id: 'pa2',
            kind: 'PURCHASE_REQUEST',
            status: 'PENDING',
            subjectName: 'B-Mix 5',
            purchaseRequest: {
              itemId: 'sup1',
              quantity: '10',
              note: 'Running low before workshop',
            },
            proposedBy: 'u1',
            proposedByName: 'Olive Owner',
            proposedAt: '2026-11-20T10:05:00Z',
            version: 1,
          },
          {
            id: 'pa3',
            kind: 'STUDIO_NOTICE',
            status: 'PENDING',
            subjectName: 'Kiln unload Friday',
            studioNotice: {
              title: 'Kiln unload Friday',
              body: 'Cone 6 glaze load comes out at noon.',
            },
            proposedBy: 'u1',
            proposedByName: 'Olive Owner',
            proposedAt: '2026-11-20T10:10:00Z',
            version: 1,
          },
        ],
        true,
        50,
      ),
      'PUT /api/v1/studios/:studioId/pending-actions/:actionId/decision': {
        id: 'pa1',
        kind: 'MAINTENANCE_TASK',
        status: 'APPROVED',
        subjectName: 'Skutt 1027',
        proposedBy: 'u1',
        proposedAt: '2026-11-20T10:00:00Z',
        version: 3,
      },
    });

    await renderWithQueries(<SuggestionList studioId="s1" />);

    expect(
      await screen.findByText('Add “Check relays” on Skutt 1027, every 30 days or every 20 firings'),
    ).toBeOnTheScreen();
    expect(screen.getByText('Request 10 × B-Mix 5 (Running low before workshop)')).toBeOnTheScreen();
    expect(screen.getByText('Send studio notice: “Kiln unload Friday”')).toBeOnTheScreen();
    expect(screen.getByText('Cone 6 glaze load comes out at noon.')).toBeOnTheScreen();
    expect(screen.getByText('Showing the oldest 50 suggestions. Decide these to see the rest.')).toBeOnTheScreen();
    expect(screen.getByText('Elements are nearing 100 firings.')).toBeOnTheScreen();

    const approveButtons = screen.getAllByRole('button', { name: 'Approve' });
    const firstApprove = approveButtons[0];
    if (!firstApprove) throw new Error('Expected Approve button');
    await fireEvent.press(firstApprove);

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/pending-actions/pa1/decision',
          body: { decision: 'APPROVED', expectedVersion: 2 },
        }),
      ),
    );

    const noteInputs = screen.getAllByLabelText('Why not (optional)');
    const secondNote = noteInputs[1];
    if (!secondNote) throw new Error('Expected second note input');
    await fireEvent.changeText(secondNote, 'Already ordered');

    const rejectButtons = screen.getAllByRole('button', { name: 'Reject' });
    const secondReject = rejectButtons[1];
    if (!secondReject) throw new Error('Expected Reject button');
    await fireEvent.press(secondReject);

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/pending-actions/pa2/decision',
          body: { decision: 'REJECTED', expectedVersion: 1, note: 'Already ordered' },
        }),
      ),
    );
  });
});
