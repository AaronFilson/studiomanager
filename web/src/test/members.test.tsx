import type { EmailInvitesRequest, OpenInvite } from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

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
});
