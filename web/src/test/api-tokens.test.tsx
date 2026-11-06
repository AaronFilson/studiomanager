import type { ApiToken } from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const token: ApiToken = {
  id: 't1',
  name: 'Claude desktop',
  ownerAccountId: 'a1',
  ownerName: 'Olive',
  scopes: ['read', 'write:pieces'],
  lastFour: 'Wx9z',
  createdAt: '2026-11-06T08:00:00Z',
  expiresAt: '2099-01-01T00:00:00Z',
};

function settings(studio = aStudio()) {
  return [
    http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
    http.get(`${API}/api/v1/studios/:studioId/fee-schedule`, () =>
      HttpResponse.json({ version: 0, ruleType: 'FLAT', rate: '0', rounding: 'NONE', minimum: '0', taxCategory: 'FIRING_SERVICE' }),
    ),
    http.get(`${API}/api/v1/studios/:studioId/piece-policy`, () => HttpResponse.json({ holdDays: 30 })),
    http.get(`${API}/api/v1/studios/:studioId/tax-rates`, () => HttpResponse.json([])),
    http.get(`${API}/api/v1/studios/:studioId/ai-key`, () => new HttpResponse(null, { status: 404 })),
  ];
}

describe('AI for invoices', () => {
  it('saves a key without ever showing it, then tests it', async () => {
    const studio = aStudio();
    let sent: unknown;
    server.use(
      ...settings(studio),
      http.get(`${API}/api/v1/studios/:studioId/api-tokens`, () => HttpResponse.json([])),
      http.put(`${API}/api/v1/studios/:studioId/ai-key`, async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json({
          provider: 'ANTHROPIC',
          lastFour: 'Wxyz',
          status: 'UNTESTED',
          allowGoogleFallback: true,
          setAt: '2026-11-06T08:00:00Z',
        });
      }),
      http.post(`${API}/api/v1/studios/:studioId/ai-key/test`, () =>
        HttpResponse.json({
          provider: 'ANTHROPIC',
          lastFour: 'Wxyz',
          status: 'WORKING',
          allowGoogleFallback: true,
          setAt: '2026-11-06T08:00:00Z',
        }),
      ),
    );

    const { user } = renderApp(`/studios/${studio.id}/settings`);
    await user.type(await screen.findByLabelText('Anthropic API key'), 'sk-ant-api03-secret-Wxyz');
    await user.click(screen.getByLabelText(/let Google Document AI try/));
    await user.click(screen.getByRole('button', { name: 'Save key' }));

    expect(await screen.findByText('not checked yet')).toBeInTheDocument();
    expect(sent).toEqual({ provider: 'ANTHROPIC', apiKey: 'sk-ant-api03-secret-Wxyz', allowGoogleFallback: true });
    expect(screen.queryByText(/sk-ant-api03-secret/)).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Test key' }));
    expect(await screen.findByText('working')).toBeInTheDocument();
  });
});

describe('API access', () => {
  it('makes a token with the chosen scopes and shows it once', async () => {
    const studio = aStudio();
    let sent: unknown;
    let tokens: ApiToken[] = [];
    server.use(
      ...settings(studio),
      http.get(`${API}/api/v1/studios/:studioId/api-tokens`, () => HttpResponse.json(tokens)),
      http.post(`${API}/api/v1/studios/:studioId/api-tokens`, async ({ request }) => {
        sent = await request.json();
        tokens = [token];
        return HttpResponse.json({ token, secret: 'smpat_secret' }, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/settings`);
    await user.type(await screen.findByLabelText('What will use it'), 'Claude desktop');
    await user.click(screen.getByLabelText('Change pieces'));
    await user.selectOptions(screen.getByLabelText('Expires after'), '30');
    await user.click(screen.getByRole('button', { name: 'Make token' }));

    expect(await screen.findByText('smpat_secret')).toBeInTheDocument();
    expect(sent).toEqual({ name: 'Claude desktop', scopes: ['read', 'write:pieces'], expiresInDays: 30 });
    const row = (await screen.findByText('Claude desktop', { selector: 'td' })).closest('tr');
    expect(within(row as HTMLElement).getByText('read, write:pieces')).toBeInTheDocument();
  });

  it('revokes a token', async () => {
    const studio = aStudio();
    let revoked = false;
    server.use(
      ...settings(studio),
      http.get(`${API}/api/v1/studios/:studioId/api-tokens`, () =>
        HttpResponse.json([revoked ? { ...token, revokedAt: '2026-11-06T09:00:00Z' } : token]),
      ),
      http.delete(`${API}/api/v1/studios/:studioId/api-tokens/:tokenId`, () => {
        revoked = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/settings`);
    await user.click(await screen.findByRole('button', { name: 'Revoke' }));

    expect(await screen.findByText('Revoked')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Revoke' })).not.toBeInTheDocument();
  });
});
