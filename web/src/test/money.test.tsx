import type { ImportPayoutRequest } from '@studiomanager/api-client';
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const studio = aStudio({ currency: 'USD' });

describe('money', () => {
  it('shows what is owed and awaiting payout, and records a payout that adds up', async () => {
    let imported: ImportPayoutRequest | undefined;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/balances`, () =>
        HttpResponse.json([{ memberId: 'm1', memberName: 'Mo', owed: '10.83' }]),
      ),
      http.get(`${API}/api/v1/studios/:studioId/stored-values`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/clearing`, () =>
        HttpResponse.json([{ provider: 'STRIPE', awaitingPayout: '50.00' }]),
      ),
      http.get(`${API}/api/v1/studios/:studioId/payouts`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/members`, () => HttpResponse.json([])),
      http.post(`${API}/api/v1/studios/:studioId/payouts`, async ({ request }) => {
        imported = (await request.json()) as ImportPayoutRequest;
        return HttpResponse.json({ id: 'p1', currency: 'USD', ...imported });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/money`);
    expect(await screen.findByText('$10.83')).toBeInTheDocument();
    expect(screen.getByText('Stripe: taken but not yet paid out')).toBeInTheDocument();

    await user.type(screen.getByLabelText('Payout id'), 'po_123');
    await user.type(screen.getByLabelText('Arrived'), '2026-10-20');
    await user.type(screen.getByLabelText('Gross'), '50.00');
    await user.clear(screen.getByLabelText('Fees'));
    await user.type(screen.getByLabelText('Fees'), '1.75');
    expect(screen.getByText('Net: 48.25')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Record payout' }));

    await expect.poll(() => imported).toEqual({
      provider: 'STRIPE',
      externalId: 'po_123',
      arrivalOn: '2026-10-20',
      gross: '50.00',
      refunds: '0',
      fees: '1.75',
      adjustments: '0',
      net: '48.25',
    });
  });

  it("shows a member what they owe on the studio's home page", async () => {
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(aStudio({ myRole: 'MEMBER' }))),
      http.get(`${API}/api/v1/studios/:studioId/members/:accountId/statement`, () =>
        HttpResponse.json({
          memberId: 'a1',
          owed: '5.83',
          storedValues: [{ id: 'sv1', kind: 'FIRING_CARD', label: '10-firing card', remaining: '44.17' }],
        }),
      ),
    );

    renderApp(`/studios/${studio.id}`);

    expect(await screen.findByText('You owe $5.83.')).toBeInTheDocument();
    expect(screen.getByText('10-firing card: $44.17 left')).toBeInTheDocument();
  });
});
