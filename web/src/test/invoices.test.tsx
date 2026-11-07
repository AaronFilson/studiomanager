import type { Invoice, SupplyItem } from '@studiomanager/api-client';
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const clay: SupplyItem = {
  id: 'i1',
  name: 'B-Mix 5',
  category: 'CLAY',
  unit: '50 lb box',
  onHand: '0',
  onOrder: '0',
  low: false,
  needsOrdering: false,
  version: 0,
};

const read: Invoice = {
  id: 'inv1',
  status: 'NEEDS_LOOK',
  fileType: 'PDF',
  fileUrl: 'https://blobs.example/invoices/inv1/invoice.pdf',
  reader: 'CLAUDE',
  vendorName: 'Riverbend Clay Supply',
  invoiceNumber: 'RB-20417',
  total: '170.00',
  lines: [
    { description: 'B-MIX 5 CONE 5 CLAY 50LB BOX', quantity: '5', unitPrice: '32.50', lineTotal: '162.50', supplyItemId: 'i1', matchSource: 'NAME' },
  ],
  problems: ['The subtotal, tax and shipping add up to 162.50, but the total is 170.00.'],
  uploadedBy: 'a1',
  uploadedAt: '2026-11-06T08:00:00Z',
  version: 2,
};

describe('Supplies inbox', () => {
  it('shows what does not add up, and confirms the invoice into stock', async () => {
    const studio = aStudio();
    let confirmed: unknown;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/supplies`, () => HttpResponse.json([clay])),
      http.get(`${API}/api/v1/studios/:studioId/purchase-requests`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/invoices`, () =>
        HttpResponse.json({
          items: [{ id: 'inv1', status: 'NEEDS_LOOK', fileType: 'PDF', vendorName: 'Riverbend Clay Supply', uploadedAt: '2026-11-06T08:00:00Z' }],
          truncated: false,
          limit: 100,
        }),
      ),
      http.get(`${API}/api/v1/studios/:studioId/invoices/:invoiceId`, () => HttpResponse.json(read)),
      http.post(`${API}/api/v1/studios/:studioId/invoices/:invoiceId/confirm`, async ({ request }) => {
        confirmed = await request.json();
        return HttpResponse.json({ ...read, status: 'CONFIRMED', version: 3 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/supplies`);
    await user.click(await screen.findByRole('button', { name: 'Open' }));

    expect(await screen.findByText(/add up to 162.50, but the total is 170.00/)).toBeInTheDocument();
    expect(screen.getByLabelText('Line 1 item')).toHaveValue('i1');
    await user.click(screen.getByRole('button', { name: 'Confirm and receive stock' }));
    expect(confirmed).toEqual({ expectedVersion: 2 });
  });
});
