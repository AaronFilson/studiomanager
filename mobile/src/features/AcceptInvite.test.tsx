import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { renderWithQueries, stubApi } from '../test/api';
import { AcceptInvite, extractInviteCode, JoinWithCodeCard } from './AcceptInvite';

describe('AcceptInvite', () => {
  it('extracts invite codes from bare codes and deep or web links', () => {
    expect(extractInviteCode('  abc-123  ')).toBe('abc-123');
    expect(extractInviteCode('studiomanager://invites/xyz-789')).toBe('xyz-789');
    expect(extractInviteCode('https://studio.example.com/invites/web-code?foo=1')).toBe('web-code');
  });

  it('accepts an invite code and navigates into the joined studio', async () => {
    const calls = stubApi({
      'POST /api/v1/invites/:code/accept': {
        studio: { id: 's2', name: 'Kiln House' },
        role: 'MEMBER',
      },
    });
    const onJoined = jest.fn();

    await renderWithQueries(<AcceptInvite code="inv-42" onJoined={onJoined} />);
    await fireEvent.press(screen.getByRole('button', { name: 'Accept invite' }));

    await waitFor(() => expect(onJoined).toHaveBeenCalledWith('s2'));
    expect(calls[0]).toMatchObject({ method: 'POST', path: '/api/v1/invites/inv-42/accept' });
  });

  it('lets a user join manually by pasting an invite link or code', async () => {
    const calls = stubApi({
      'POST /api/v1/invites/:code/accept': {
        studio: { id: 's3', name: 'Wheel Room' },
        role: 'STAFF',
      },
    });
    const onJoined = jest.fn();

    await renderWithQueries(<JoinWithCodeCard onJoined={onJoined} />);
    await fireEvent.changeText(screen.getByLabelText('Invite code'), 'studiomanager://invites/code-99');
    await fireEvent.press(screen.getByRole('button', { name: 'Join studio' }));

    await waitFor(() => expect(onJoined).toHaveBeenCalledWith('s3'));
    expect(calls[0]).toMatchObject({ method: 'POST', path: '/api/v1/invites/code-99/accept' });
  });
});
