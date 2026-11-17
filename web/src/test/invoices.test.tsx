import type { Invoice, SupplyItem } from '@studiomanager/api-client';
import { screen, waitFor } from '@testing-library/react';
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
  problems: [
    'The line adds up to 162.50. The total is 170.00, 7.50 more: check for tax, shipping or a fee that wasn’t read.',
  ],
  uploadedBy: 'a1',
  uploadedAt: '2026-11-06T08:00:00Z',
  version: 2,
};

describe('Supplies inbox', () => {
  it('shows what does not add up, and confirms the invoice into stock with the edits on screen', async () => {
    const studio = aStudio();
    let confirmed: unknown;
    let saved: { total?: string; lines: { quantity?: string }[] } | undefined;
    server.use(
      http.put(`${API}/api/v1/studios/:studioId/invoices/:invoiceId`, async ({ request }) => {
        saved = (await request.json()) as typeof saved;
        return HttpResponse.json({ ...read, version: 3 });
      }),
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
        return HttpResponse.json({ ...read, status: 'CONFIRMED', version: 4 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/supplies`);
    await user.click(await screen.findByRole('button', { name: 'Open' }));

    expect(await screen.findByText(/adds up to 162.50. The total is 170.00, 7.50 more/)).toBeInTheDocument();
    expect(screen.getByLabelText('Line 1 item')).toHaveValue('i1');
    // An edit that was never saved separately still counts
    await user.clear(screen.getByLabelText('Line 1 quantity'));
    await user.type(screen.getByLabelText('Line 1 quantity'), '6');
    await user.click(screen.getByRole('button', { name: 'Confirm and receive stock' }));

    await waitFor(() => expect(confirmed).toEqual({ expectedVersion: 3 }));
    expect(saved?.lines[0]?.quantity).toBe('6');
    expect(saved?.total).toBe('170.00');
  });

  it('shows the implied unit price when the invoice prints only the line amount', async () => {
    const studio = aStudio();
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/supplies`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/purchase-requests`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/invoices`, () =>
        HttpResponse.json({
          items: [{ id: 'inv1', status: 'READY', fileType: 'JPEG', uploadedAt: '2026-11-06T08:00:00Z' }],
          truncated: false,
          limit: 100,
        }),
      ),
      http.get(`${API}/api/v1/studios/:studioId/invoices/:invoiceId`, () =>
        HttpResponse.json({
          ...read,
          status: 'READY',
          fileType: 'JPEG',
          problems: [],
          lines: [{ description: 'Fireclay', quantity: '100', lineTotal: '17.50' }],
        }),
      ),
    );

    const { user } = renderApp(`/studios/${studio.id}/supplies`);
    await user.click(await screen.findByRole('button', { name: 'Open' }));

    expect(await screen.findByLabelText('Line 1 unit price')).toHaveAttribute('placeholder', '≈ 0.175');
    expect(screen.getByLabelText('Line 1 unit price')).toHaveValue('');
  });

  it('compares the readers, saying once why one did not run', async () => {
    const studio = aStudio();
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/supplies`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/purchase-requests`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/invoices`, () =>
        HttpResponse.json({
          items: [{ id: 'inv1', status: 'NEEDS_LOOK', fileType: 'PDF', uploadedAt: '2026-11-06T08:00:00Z' }],
          truncated: false,
          limit: 100,
        }),
      ),
      http.get(`${API}/api/v1/studios/:studioId/invoices/:invoiceId`, () => HttpResponse.json(read)),
      http.post(`${API}/api/v1/studios/:studioId/invoices/:invoiceId/compare-readers`, () =>
        HttpResponse.json({
          results: [
            { reader: 'CLAUDE', outcome: 'READ', elapsedMs: 4200, total: '170.00', lines: read.lines, problems: [] },
            {
              reader: 'GOOGLE_DOCUMENT_AI',
              outcome: 'SKIPPED',
              skipReason: 'NOT_SET_UP',
              note: 'This reader isn’t set up on this server.',
              elapsedMs: 0,
              lines: [],
              problems: [],
            },
          ],
        }),
      ),
    );

    const { user } = renderApp(`/studios/${studio.id}/supplies`);
    await user.click(await screen.findByRole('button', { name: 'Open' }));
    await user.click(await screen.findByRole('button', { name: 'Compare readers' }));

    expect(await screen.findByText('This reader isn’t set up on this server.')).toBeInTheDocument();
    expect(screen.getAllByText('This reader isn’t set up on this server.')).toHaveLength(1);
    expect(screen.getByText('didn’t run')).toBeInTheDocument();
  });
});
