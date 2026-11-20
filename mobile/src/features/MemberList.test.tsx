import { fireEvent, screen, waitFor } from '@testing-library/react-native';
import { Share } from 'react-native';

import { renderWithQueries, stubApi } from '../test/api';
import { MemberList } from './MemberList';

const members = [
  {
    accountId: 'a1',
    displayName: 'Olive Owner',
    role: 'OWNER' as const,
    roleId: 'r-owner',
    roleName: 'Owner',
    joinedAt: '2026-01-10T00:00:00Z',
  },
  {
    accountId: 'a2',
    displayName: 'Sam Staff',
    role: 'STAFF' as const,
    roleId: 'r-staff',
    roleName: 'Staff',
    joinedAt: '2026-02-10T00:00:00Z',
  },
];

const assignableRoles = [
  { id: 'r-owner', key: 'owner', role: 'OWNER' as const, name: 'Owner', custom: false, permissions: [], editable: false, memberCount: 1 },
  { id: 'r-staff', key: 'staff', role: 'STAFF' as const, name: 'Staff', custom: false, permissions: [], editable: true, memberCount: 1 },
  { id: 'r-member', key: 'member', role: 'MEMBER' as const, name: 'Member', custom: false, permissions: [], editable: true, memberCount: 0 },
  { id: 'r-kiln', key: 'kiln_tech', role: 'STAFF' as const, name: 'Kiln tech', custom: true, permissions: [], editable: true, memberCount: 0 },
];

describe('MemberList', () => {
  it('supports owner management, billing contact, custom role assignment, duties, certifications, and invites', async () => {
    const shareSpy = jest.spyOn(Share, 'share').mockResolvedValue({ action: 'sharedAction', activityType: undefined });
    const calls = stubApi({
      'GET /api/v1/me': { account: { id: 'a1', displayName: 'Olive Owner' }, memberships: [] },
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'OWNER',
        roleName: 'Owner',
        permissions: ['owners.manage', 'members.read', 'members.invite', 'members.role.assign'],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/members': members,
      'GET /api/v1/studios/:studioId/members/billing-contact': { chooseNeeded: true },
      'PUT /api/v1/studios/:studioId/members/billing-contact': { accountId: 'a1', chooseNeeded: false },
      'GET /api/v1/studios/:studioId/duties': [{ accountId: 'a2', duties: ['MONITOR'] }],
      'PUT /api/v1/studios/:studioId/members/:accountId/duties': { accountId: 'a2', duties: ['MONITOR', 'TECHNICIAN'] },
      'GET /api/v1/studios/:studioId/certification-types': [{ id: 'ct1', name: 'Kiln loading', validMonths: 12 }],
      'POST /api/v1/studios/:studioId/certification-types': { id: 'ct2', name: 'Wheel room' },
      'GET /api/v1/studios/:studioId/certifications': [],
      'PUT /api/v1/studios/:studioId/certifications/:typeId/holders/:accountId': {
        typeId: 'ct1',
        name: 'Kiln loading',
        accountId: 'a2',
        grantedAt: '2026-10-20T00:00:00Z',
        valid: true,
      },
      'GET /api/v1/studios/:studioId/roles/assignable': assignableRoles,
      'GET /api/v1/studios/:studioId/invites': [
        {
          id: 'inv1',
          email: 'pat@example.com',
          role: 'MEMBER',
          roleName: 'Member',
          createdAt: '2026-10-10T00:00:00Z',
          expiresAt: '2026-10-24T00:00:00Z',
          expired: false,
          lastSentAt: '2026-10-10T00:01:00Z',
        },
      ],
      'POST /api/v1/studios/:studioId/invites': {
        id: 'inv2',
        code: 'NEW-CODE-1',
        role: 'MEMBER',
        roleId: 'r-member',
        roleName: 'Member',
        expiresAt: '2026-11-05T00:00:00Z',
      },
      'POST /api/v1/studios/:studioId/invites/emails': {
        invited: [],
        skipped: [{ email: 'sam@example.com', reason: 'ALREADY_MEMBER' }],
      },
      'POST /api/v1/studios/:studioId/invites/:inviteId/resend': {
        id: 'inv1',
        email: 'pat@example.com',
        role: 'MEMBER',
        createdAt: '2026-10-10T00:00:00Z',
        expiresAt: '2026-11-05T00:00:00Z',
        expired: false,
      },
      'DELETE /api/v1/studios/:studioId/invites/:inviteId': null,
      'PUT /api/v1/studios/:studioId/members/:accountId/owner': { ...members[1], role: 'OWNER', roleName: 'Owner' },
      'PUT /api/v1/studios/:studioId/members/:accountId/role': { ...members[1], roleId: 'r-kiln', roleName: 'Kiln tech' },
    });

    await renderWithQueries(<MemberList studioId="s1" />);

    expect(await screen.findByText('Olive Owner')).toBeOnTheScreen();
    expect(await screen.findByText(/The billing contact is no longer an owner/)).toBeOnTheScreen();
    expect(screen.getByText('The only owner. Add another owner before they can step down.')).toBeOnTheScreen();

    // Pick billing contact
    await fireEvent.press(screen.getByRole('button', { name: 'Olive Owner' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/members/billing-contact',
          body: { accountId: 'a1' },
        }),
      ),
    );

    // Make Sam an owner
    await fireEvent.press(screen.getByRole('button', { name: 'Make Sam Staff an owner' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({ method: 'PUT', path: '/api/v1/studios/s1/members/a2/owner' }),
      ),
    );

    // Assign custom role to Sam
    const kilnTechButtons = screen.getAllByRole('button', { name: 'Kiln tech' });
    const memberRoleButton = kilnTechButtons[kilnTechButtons.length - 1];
    if (!memberRoleButton) throw new Error('Expected Kiln tech role button');
    await fireEvent.press(memberRoleButton);
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/members/a2/role',
          body: { roleId: 'r-kiln' },
        }),
      ),
    );

    // Toggle a duty
    await fireEvent.press(screen.getByRole('checkbox', { name: 'Sam Staff: Technician' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/members/a2/duties',
          body: { duties: ['MONITOR', 'TECHNICIAN'] },
        }),
      ),
    );

    // Grant certification
    const certifyButtons = screen.getAllByRole('button', { name: 'Certify: Kiln loading' });
    const samCertify = certifyButtons[1];
    if (!samCertify) throw new Error('Expected certify button for Sam');
    await fireEvent.press(samCertify);
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/certifications/ct1/holders/a2',
        }),
      ),
    );

    // Create invite link and share
    await fireEvent.press(screen.getByRole('button', { name: 'Create an invite link' }));
    expect(await screen.findByText('NEW-CODE-1')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Share invite' }));
    expect(shareSpy).toHaveBeenCalledWith(expect.objectContaining({ message: expect.stringContaining('NEW-CODE-1') as unknown }));

    // Email invites with skipped reason
    await fireEvent.changeText(screen.getByLabelText('Email addresses'), 'sam@example.com');
    await fireEvent.press(screen.getByRole('button', { name: 'Send invites' }));
    expect(await screen.findByText('Skipped sam@example.com: already a member')).toBeOnTheScreen();

    // Resend and withdraw open invite
    await fireEvent.press(screen.getByRole('button', { name: 'Resend' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Withdraw' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({ method: 'POST', path: '/api/v1/studios/s1/invites/inv1/resend' }),
      ),
    );
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({ method: 'DELETE', path: '/api/v1/studios/s1/invites/inv1' }),
      ),
    );

    // Create certification type
    await fireEvent.changeText(screen.getByLabelText('Certification name'), 'Wheel room');
    await fireEvent.press(screen.getByRole('button', { name: 'Add certification' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/certification-types',
          body: { name: 'Wheel room' },
        }),
      ),
    );

    shareSpy.mockRestore();
  });

  it('does not request billing contact or invite endpoints when the caller lacks those permissions', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'STAFF',
        roleName: 'Staff',
        permissions: ['members.read'],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/members': members,
      'GET /api/v1/studios/:studioId/duties': [],
      'GET /api/v1/studios/:studioId/certification-types': [],
      'GET /api/v1/studios/:studioId/certifications': [],
    });

    await renderWithQueries(<MemberList studioId="s1" />);
    expect(await screen.findByText('Olive Owner')).toBeOnTheScreen();
    expect(screen.queryByText('Billing contact')).toBeNull();
    expect(screen.queryByText('Invite with a link')).toBeNull();
    expect(calls.some((c) => c.path.includes('billing-contact'))).toBe(false);
    expect(calls.some((c) => c.path.includes('invites'))).toBe(false);
  });
});
