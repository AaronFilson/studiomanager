import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { renderWithQueries, stubApi } from '../test/api';
import { NoticeList } from './NoticeList';

describe('NoticeList', () => {
  it('shows notices and marks an unread one read when opened', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/notifications': [
        {
          id: 'n1',
          kind: 'PIECES_READY',
          title: 'Your 2 pieces are ready to collect',
          body: 'Piece 42: Lidded jar',
          createdAt: '2026-10-20T12:00:00Z',
        },
        { id: 'n2', kind: 'STUDIO_NOTICE', title: 'Closed Monday', body: '', createdAt: '2026-10-19T12:00:00Z', readAt: '2026-10-19T13:00:00Z' },
      ],
      'POST /api/v1/studios/:studioId/notifications/:notificationId/read': null,
    });

    await renderWithQueries(<NoticeList studioId="s1" />);
    expect(await screen.findByText('Piece 42: Lidded jar')).toBeOnTheScreen();
    expect(screen.getAllByText('New')).toHaveLength(1);

    // Already read: nothing to send
    await fireEvent.press(screen.getByRole('button', { name: 'Closed Monday' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Your 2 pieces are ready to collect' }));

    await waitFor(() =>
      expect(calls.filter((c) => c.method === 'POST').map((c) => c.path)).toEqual([
        '/api/v1/studios/s1/notifications/n1/read',
      ]),
    );
  });
});
