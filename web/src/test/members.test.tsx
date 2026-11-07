import type {
  CreateInviteRequest,
  EmailInvitesRequest,
  Member,
  OpenInvite,
  Role,
  SetBillingContactRequest,
  SetMemberRoleRequest,
} from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { systemRoles } from './permissions';
import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const studio = aStudio();

const niaInvite: OpenInvite = {
  id: 'i1',
  email: 'nia@example.com',
  role: 'MEMBER',
  createdAt: '2026-10-20T12:00:00Z',
  expiresAt: '2026-11-03T12:00:00Z',
  expired: false,
  lastSentAt: '2026-10-20T12:00:05Z',
};

describe('members', () => {
  it('emails invites to a pasted list and says who was skipped', async () => {
    let sent: EmailInvitesRequest | undefined;
    let invites: OpenInvite[] = [];
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/members`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/invites`, () => HttpResponse.json(invites)),
      http.post(`${API}/api/v1/studios/:studioId/invites/emails`, async ({ request }) => {
        sent = (await request.json()) as EmailInvitesRequest;
        invites = [{ ...niaInvite, lastSentAt: undefined }];
        return HttpResponse.json({
          invited: invites,
          skipped: [{ email: 'mo@example.com', reason: 'ALREADY_MEMBER' }],
        });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/members`);
    await user.type(await screen.findByLabelText('Email addresses'), 'nia@example.com, mo@example.com');
    await user.click(screen.getByRole('button', { name: 'Send invites' }));

    expect(await screen.findByText('Sending 1 invite.')).toBeInTheDocument();
    expect(screen.getByText('Skipped mo@example.com: already a member')).toBeInTheDocument();
    expect(sent).toEqual({ text: 'nia@example.com, mo@example.com', role: 'MEMBER' });
    const waiting = await screen.findByRole('region', { name: 'Waiting to join' });
    expect(within(waiting).getByText('nia@example.com')).toBeInTheDocument();
    expect(within(waiting).getByText(/sending/)).toBeInTheDocument();
  });

  it('resends and withdraws open invites', async () => {
    const calls: string[] = [];
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/members`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/invites`, () => HttpResponse.json([niaInvite])),
      http.post(`${API}/api/v1/studios/:studioId/invites/:inviteId/resend`, ({ params }) => {
        calls.push(`resend ${String(params.inviteId)}`);
        return HttpResponse.json(niaInvite);
      }),
      http.delete(`${API}/api/v1/studios/:studioId/invites/:inviteId`, ({ params }) => {
        calls.push(`revoke ${String(params.inviteId)}`);
        return new HttpResponse(null, { status: 204 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/members`);
    const waiting = await screen.findByRole('region', { name: 'Waiting to join' });
    await user.click(within(waiting).getByRole('button', { name: 'Resend' }));
    await user.click(within(waiting).getByRole('button', { name: 'Withdraw' }));

    await expect.poll(() => calls).toEqual(['resend i1', 'revoke i1']);
  });

  describe('custom roles', () => {
    const kilnTech: Role = {
      id: 'r-kiln',
      key: 'kiln_tech',
      role: 'MEMBER',
      name: 'Kiln tech',
      permissions: ['money.own.read'],
      editable: true,
      custom: true,
      memberCount: 0,
    };
    const jo: Member = {
      accountId: 'a2',
      displayName: 'Jo',
      role: 'MEMBER',
      roleId: 'r-member',
      roleName: 'Member',
      joinedAt: '2026-10-02T12:00:00Z',
    };

    function serveCustomRoles(calls: string[]) {
      server.use(
        http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
        http.get(`${API}/api/v1/studios/:studioId/members`, () => HttpResponse.json([jo])),
        http.get(`${API}/api/v1/studios/:studioId/invites`, () => HttpResponse.json([])),
        http.get(`${API}/api/v1/studios/:studioId/duties`, () => HttpResponse.json([])),
        http.get(`${API}/api/v1/studios/:studioId/certification-types`, () => HttpResponse.json([])),
        http.get(`${API}/api/v1/studios/:studioId/certifications`, () => HttpResponse.json([])),
        http.get(`${API}/api/v1/studios/:studioId/roles/assignable`, () =>
          HttpResponse.json([...systemRoles, kilnTech]),
        ),
        http.put(`${API}/api/v1/studios/:studioId/members/:accountId/role`, async ({ params, request }) => {
          const body = (await request.json()) as SetMemberRoleRequest;
          calls.push(`role ${String(params.accountId)} ${body.roleId}`);
          return new HttpResponse(null, { status: 204 });
        }),
        http.post(`${API}/api/v1/studios/:studioId/invites`, async ({ request }) => {
          const body = (await request.json()) as CreateInviteRequest;
          calls.push(`invite ${body.role} ${body.roleId ?? '-'}`);
          return HttpResponse.json(
            { id: 'i9', code: 'c0de', role: body.role, roleId: 'r-kiln', roleName: 'Kiln tech', expiresAt: '2026-11-21T12:00:00Z' },
            { status: 201 },
          );
        }),
      );
    }

    it("chooses a member's role among the built-in and custom roles, never owner", async () => {
      const calls: string[] = [];
      serveCustomRoles(calls);

      const { user } = renderApp(`/studios/${studio.id}/members`);
      const picker = await screen.findByLabelText("Jo's role");
      expect(within(picker).queryByRole('option', { name: 'Owner' })).not.toBeInTheDocument();
      await user.selectOptions(picker, 'Kiln tech');

      await expect.poll(() => calls).toEqual(['role a2 r-kiln']);
    });

    it('invites with a custom role by its id and base', async () => {
      const calls: string[] = [];
      serveCustomRoles(calls);

      const { user } = renderApp(`/studios/${studio.id}/members`);
      const link = await screen.findByRole('region', { name: 'Invite with a link' });
      await within(link).findByRole('option', { name: 'Kiln tech' });
      await user.selectOptions(within(link).getByLabelText('As'), 'Kiln tech');
      await user.click(within(link).getByRole('button', { name: 'Create an invite link' }));

      await expect.poll(() => calls).toEqual(['invite MEMBER r-kiln']);
    });
  });

  describe('co-owners', () => {
    // The signed-in account is a1 (see server.ts)
    const mara: Member = {
      accountId: 'a1',
      displayName: 'Mara',
      role: 'OWNER',
      roleId: 'r-owner',
      roleName: 'Owner',
      joinedAt: '2026-10-01T12:00:00Z',
    };
    const jo: Member = {
      accountId: 'a2',
      displayName: 'Jo',
      role: 'STAFF',
      roleId: 'r-staff',
      roleName: 'Staff',
      joinedAt: '2026-10-02T12:00:00Z',
    };
    const owners = [mara, jo];

    function serveMembers(members: Member[], calls: string[]) {
      server.use(
        http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
        http.get(`${API}/api/v1/studios/:studioId/members`, () => HttpResponse.json(members)),
        http.get(`${API}/api/v1/studios/:studioId/invites`, () => HttpResponse.json([])),
        http.get(`${API}/api/v1/studios/:studioId/duties`, () => HttpResponse.json([])),
        http.get(`${API}/api/v1/studios/:studioId/certification-types`, () => HttpResponse.json([])),
        http.get(`${API}/api/v1/studios/:studioId/certifications`, () => HttpResponse.json([])),
        http.put(`${API}/api/v1/studios/:studioId/members/:accountId/owner`, ({ params }) => {
          calls.push(`add ${String(params.accountId)}`);
          return new HttpResponse(null, { status: 204 });
        }),
        http.delete(`${API}/api/v1/studios/:studioId/members/:accountId/owner`, ({ params }) => {
          calls.push(`remove ${String(params.accountId)}`);
          return new HttpResponse(null, { status: 204 });
        }),
        http.put(`${API}/api/v1/studios/:studioId/members/billing-contact`, async ({ request }) => {
          const body = (await request.json()) as SetBillingContactRequest;
          calls.push(`bill ${body.accountId}`);
          return HttpResponse.json({ accountId: body.accountId, chooseNeeded: false });
        }),
      );
    }

    it('lets an owner make a member an owner, and offers no way to remove the last owner', async () => {
      const calls: string[] = [];
      serveMembers(owners, calls);

      const { user } = renderApp(`/studios/${studio.id}/members`);
      await user.click(await screen.findByRole('button', { name: 'Make Jo an owner' }));

      await expect.poll(() => calls).toEqual(['add a2']);
      expect(screen.queryByRole('button', { name: /as owner/ })).not.toBeInTheDocument();
      expect(screen.getByText(/The only owner/)).toBeInTheDocument();
    });

    it('lets an owner step down while another owner remains, and pick the billing contact', async () => {
      const calls: string[] = [];
      serveMembers([mara, { ...jo, role: 'OWNER' }], calls);

      const { user } = renderApp(`/studios/${studio.id}/members`);
      await user.click(await screen.findByRole('button', { name: 'Step down as owner' }));
      const billing = await screen.findByRole('region', { name: 'Billing contact' });
      await user.selectOptions(within(billing).getByLabelText('Owner the studio is billed through'), 'a2');

      await expect.poll(() => calls).toEqual(['remove a1', 'bill a2']);
      expect(screen.getByRole('button', { name: 'Remove Jo as owner' })).toBeInTheDocument();
    });

    it('asks the owners to pick a billing contact when there is none', async () => {
      serveMembers(owners, []);
      server.use(
        http.get(`${API}/api/v1/studios/:studioId/members/billing-contact`, () =>
          HttpResponse.json({ chooseNeeded: true }),
        ),
      );

      renderApp(`/studios/${studio.id}/members`);

      expect(await screen.findByText(/The billing contact is no longer an owner/)).toBeInTheDocument();
    });

    it('shows no owner controls to staff', async () => {
      serveMembers(owners, []);
      server.use(
        http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(aStudio({ myRole: 'STAFF' }))),
      );

      renderApp(`/studios/${studio.id}/members`);

      expect(await screen.findByText('Mara')).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /owner/ })).not.toBeInTheDocument();
      expect(screen.queryByRole('region', { name: 'Billing contact' })).not.toBeInTheDocument();
    });
  });
});
