import type { PaymentRunRequest, PayPayeeRequest, Payee, TakeInConsignmentRequest } from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const studio = aStudio();
const ana: Payee = {
  id: 'p1',
  name: 'Ana Ruiz',
  files1099: true,
  consignorShare: '0.6',
  discountsFrom: 'SHARED',
  archived: false,
  owed: '42.00',
};

function payablesApi() {
  server.use(
    http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
    http.get(`${API}/api/v1/studios/:studioId/payees`, () => HttpResponse.json([ana])),
    http.get(`${API}/api/v1/studios/:studioId/consignment-items`, () => HttpResponse.json([])),
    http.get(`${API}/api/v1/studios/:studioId/consignment-sales`, () => HttpResponse.json([])),
    http.get(`${API}/api/v1/studios/:studioId/payables/1099-nec`, () =>
      HttpResponse.json({
        year: 2026,
        threshold: '2000',
        payees: [{ payeeId: 'p1', name: 'Ana Ruiz', reportable: '2400.00', paidThroughNetworks: '0', meetsThreshold: true }],
      }),
    ),
  );
}

describe('artists and instructors', () => {
  it('shows who is owed, opens a statement and records a payment', async () => {
    payablesApi();
    let paid: PayPayeeRequest | undefined;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId/payees/:payeeId/statement`, () =>
        HttpResponse.json({
          payeeId: 'p1',
          payeeName: 'Ana Ruiz',
          from: '2026-10-01',
          to: '2026-10-21',
          opening: '0',
          lines: [{ date: '2026-10-20', kind: 'CONSIGNMENT_SALE', memo: 'Sold 1 × Celadon mug (Ana Ruiz)', amount: '24.00' }],
          closing: '42.00',
        }),
      ),
      http.post(`${API}/api/v1/studios/:studioId/payees/:payeeId/payments`, async ({ request }) => {
        paid = (await request.json()) as PayPayeeRequest;
        return HttpResponse.json({ id: 'pay1', payeeId: 'p1', paidOn: '2026-10-21', ...paid }, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/payables`);
    const owed = await screen.findByRole('region', { name: 'Who the studio owes' });
    expect(await within(owed).findByText('$42.00')).toBeInTheDocument();
    expect(within(owed).getByText(/W-9 missing/)).toBeInTheDocument();
    expect(within(owed).getByText(/60% to the artist/)).toBeInTheDocument();

    await user.click(within(owed).getByRole('button', { name: 'Ana Ruiz' }));
    expect(await screen.findByText(/Sold 1 × Celadon mug/)).toBeInTheDocument();

    const payForm = screen.getByRole('form', { name: 'Record a payment to them' });
    expect(within(payForm).getByLabelText('Amount (USD)')).toHaveValue('42.00');
    await user.type(within(payForm).getByLabelText('Reference (optional)'), '1043');
    await user.click(within(payForm).getByRole('button', { name: 'Record payment' }));

    await expect.poll(() => paid).toEqual({ amount: '42.00', method: 'CHECK', reference: '1043' });
  });

  it('takes in work with the artist share as a percentage, and lists the 1099-NEC', async () => {
    payablesApi();
    let takenIn: TakeInConsignmentRequest | undefined;
    server.use(
      http.post(`${API}/api/v1/studios/:studioId/consignment-items`, async ({ request }) => {
        takenIn = (await request.json()) as TakeInConsignmentRequest;
        return HttpResponse.json({}, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/payables`);
    const form = await screen.findByRole('form', { name: 'Take in work' });
    expect(await screen.findByText(/form needed/)).toBeInTheDocument();

    await user.selectOptions(await within(form).findByLabelText('Artist'), 'p1');
    await user.type(within(form).getByLabelText('What'), 'Celadon mug');
    await user.type(within(form).getByLabelText('Retail price'), '40.00');
    await user.type(within(form).getByLabelText("Artist's share, %"), '55');
    await user.click(within(form).getByRole('button', { name: 'Take in' }));

    await expect.poll(() => takenIn).toEqual({
      payeeId: 'p1',
      description: 'Celadon mug',
      retailPrice: '40.00',
      quantity: 1,
      consignorShare: '0.55',
    });
  });

  it('pays everyone owed in one run, leaving out anyone whose amount is cleared', async () => {
    const ben: Payee = { ...ana, id: 'p2', name: 'Ben Lee', owed: '15.00' };
    payablesApi();
    let run: PaymentRunRequest | undefined;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId/payees`, () => HttpResponse.json([ana, ben])),
      http.post(`${API}/api/v1/studios/:studioId/payment-runs`, async ({ request }) => {
        run = (await request.json()) as PaymentRunRequest;
        return HttpResponse.json([], { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/payables`);
    const form = await screen.findByRole('form', { name: 'Pay everyone owed' });
    await user.clear(await within(form).findByLabelText('Pay Ben Lee'));
    await user.type(within(form).getByLabelText('Reference for Ana Ruiz'), '1060');
    await user.click(within(form).getByRole('button', { name: 'Pay 1 person' }));

    await expect.poll(() => run).toEqual({
      method: 'CHECK',
      payments: [{ payeeId: 'p1', amount: '42.00', reference: '1060' }],
    });
  });
});
