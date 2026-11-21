import type { Invoice, SupplyItem } from '@studiomanager/api-client';
import { screen, waitFor, within } from '@testing-library/react';
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

  it('lets a confirm that failed after its save be fixed and tried again', async () => {
    const studio = aStudio();
    const saves: { expectedVersion: number }[] = [];
    let confirms = 0;
    server.use(
      http.put(`${API}/api/v1/studios/:studioId/invoices/:invoiceId`, async ({ request }) => {
        const body = (await request.json()) as { expectedVersion: number };
        saves.push(body);
        if (body.expectedVersion !== 2 + saves.length - 1) {
          return HttpResponse.json({ title: 'Conflict', status: 409 }, { status: 409 });
        }
        return HttpResponse.json({ ...read, version: body.expectedVersion + 1 });
      }),
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/supplies`, () => HttpResponse.json([clay])),
      http.get(`${API}/api/v1/studios/:studioId/purchase-requests`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/invoices`, () =>
        HttpResponse.json({
          items: [{ id: 'inv1', status: 'NEEDS_LOOK', fileType: 'PDF', uploadedAt: '2026-11-06T08:00:00Z' }],
          truncated: false,
          limit: 100,
        }),
      ),
      http.get(`${API}/api/v1/studios/:studioId/invoices/:invoiceId`, () => HttpResponse.json(read)),
      http.post(`${API}/api/v1/studios/:studioId/invoices/:invoiceId/confirm`, () => {
        confirms += 1;
        return confirms === 1
          ? HttpResponse.json(
              { title: 'Invalid input', status: 400, detail: 'Line 1 needs a quantity above zero to receive.' },
              { status: 400 },
            )
          : HttpResponse.json({ ...read, status: 'CONFIRMED', version: 5 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/supplies`);
    await user.click(await screen.findByRole('button', { name: 'Open' }));
    await user.click(await screen.findByRole('button', { name: 'Confirm and receive stock' }));
    await waitFor(() => expect(confirms).toBe(1));
    await user.click(screen.getByRole('button', { name: 'Confirm and receive stock' }));

    await waitFor(() => expect(confirms).toBe(2));
    // The second save named the version the first one made, so it didn't conflict
    expect(saves.map((save) => save.expectedVersion)).toEqual([2, 3]);
  });

  it('says what a line receives, and warns when the invoice counts in another unit', async () => {
    const studio = aStudio();
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/supplies`, () => HttpResponse.json([clay])),
      http.get(`${API}/api/v1/studios/:studioId/purchase-requests`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/invoices`, () =>
        HttpResponse.json({
          items: [{ id: 'inv1', status: 'READY', fileType: 'PDF', uploadedAt: '2026-11-06T08:00:00Z' }],
          truncated: false,
          limit: 100,
        }),
      ),
      http.get(`${API}/api/v1/studios/:studioId/invoices/:invoiceId`, () =>
        HttpResponse.json({
          ...read,
          status: 'READY',
          problems: [],
          lines: [
            { description: 'B-Mix 5', quantity: '5', unit: 'boxes', lineTotal: '162.50', supplyItemId: 'i1', matchSource: 'NAME' },
            { description: 'B-Mix 5 loose', quantity: '100', unit: 'lb', lineTotal: '65.00', supplyItemId: 'i1', matchSource: 'STAFF' },
          ],
        }),
      ),
    );

    const { user } = renderApp(`/studios/${studio.id}/supplies`);
    await user.click(await screen.findByRole('button', { name: 'Open' }));

    // "boxes" counts in the item's own "50 lb box"
    expect(await screen.findByLabelText('Line 1 item')).toHaveValue('i1');
    expect(screen.getByText(/Receives 5/)).toHaveTextContent('Receives 5 50 lb box');
    // 100 lb would be received as 100 boxes
    expect(screen.getByText(/The invoice counts in lb; B-Mix 5 is counted in 50 lb box/)).toBeInTheDocument();
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

  it('creates a new supply item inline from an unmatched line and selects it immediately', async () => {
    const studio = aStudio();
    let createdPayload: unknown;
    const supplyList: SupplyItem[] = [clay];
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/supplies`, () => HttpResponse.json(supplyList)),
      http.post(`${API}/api/v1/studios/:studioId/supplies`, async ({ request }) => {
        createdPayload = await request.json();
        const created: SupplyItem = {
          id: 'i-new',
          name: 'EPK Kaolin 50 lb',
          category: 'GLAZE_MATERIAL',
          unit: 'lb',
          supplier: 'Riverbend Clay Supply',
          onHand: '0',
          onOrder: '0',
          low: false,
          needsOrdering: false,
          version: 0,
        };
        supplyList.push(created);
        return HttpResponse.json(created, { status: 201 });
      }),
      http.get(`${API}/api/v1/studios/:studioId/purchase-requests`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/invoices`, () =>
        HttpResponse.json({
          items: [{ id: 'inv1', status: 'READY', fileType: 'PDF', vendorName: 'Riverbend Clay Supply', uploadedAt: '2026-11-06T08:00:00Z' }],
          truncated: false,
          limit: 100,
        }),
      ),
      http.get(`${API}/api/v1/studios/:studioId/invoices/:invoiceId`, () =>
        HttpResponse.json({
          ...read,
          status: 'READY',
          problems: [],
          lines: [{ description: 'EPK Kaolin 50 lb', quantity: '50', unit: 'lb', unitPrice: '1.20', lineTotal: '60.00', kind: 'ITEM' }],
        }),
      ),
    );

    const { user } = renderApp(`/studios/${studio.id}/supplies`);
    await user.click(await screen.findByRole('button', { name: 'Open' }));

    expect(await screen.findByLabelText('Line 1 item')).toHaveValue('');
    await user.click(screen.getByRole('button', { name: '+ New item for line 1' }));

    const inlineForm = within(screen.getByRole('group', { name: 'New supply item for line 1' }));
    expect(inlineForm.getByLabelText('Item name')).toHaveValue('EPK Kaolin 50 lb');
    expect(inlineForm.getByLabelText('Category')).toHaveValue('GLAZE_MATERIAL');
    expect(inlineForm.getByLabelText('Unit')).toHaveValue('lb');
    expect(inlineForm.getByLabelText('Supplier')).toHaveValue('Riverbend Clay Supply');

    await user.click(inlineForm.getByRole('button', { name: 'Create item' }));

    await waitFor(() => expect(screen.getByLabelText('Line 1 item')).toHaveValue('i-new'));
    expect(createdPayload).toEqual({
      name: 'EPK Kaolin 50 lb',
      category: 'GLAZE_MATERIAL',
      unit: 'lb',
      supplier: 'Riverbend Clay Supply',
    });
  });

  it('shows last purchase price and date on supply items', async () => {
    const studio = aStudio();
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/supplies`, () =>
        HttpResponse.json([
          {
            ...clay,
            lastPurchaseUnitPrice: '32.50',
            lastPurchasedAt: '2026-10-14T15:30:00Z',
          },
        ]),
      ),
      http.get(`${API}/api/v1/studios/:studioId/purchase-requests`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/invoices`, () =>
        HttpResponse.json({ items: [], truncated: false, limit: 100 }),
      ),
    );

    renderApp(`/studios/${studio.id}/supplies`);
    expect(await screen.findByText(/last paid .*32\.50\/50 lb box/)).toBeInTheDocument();
  });

  it('shows v2 document type, reader notes, crossed-out rows, payments, payable amount, rotation and editable line kind', async () => {
    const studio = aStudio();
    let saved: { documentType?: string; lines: { kind?: string }[] } | undefined;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/supplies`, () => HttpResponse.json([clay])),
      http.get(`${API}/api/v1/studios/:studioId/purchase-requests`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/invoices`, () =>
        HttpResponse.json({
          items: [{ id: 'inv2', status: 'READY', fileType: 'JPEG', vendorName: 'Cascade Clay', uploadedAt: '2026-11-06T08:00:00Z' }],
          truncated: false,
          limit: 100,
        }),
      ),
      http.get(`${API}/api/v1/studios/:studioId/invoices/:invoiceId`, () =>
        HttpResponse.json({
          ...read,
          id: 'inv2',
          status: 'READY',
          fileType: 'JPEG',
          documentType: 'ORDER_FORM',
          vendorName: 'Cascade Clay',
          orderNumber: 'CTR-514',
          invoiceDate: '2026-10-18',
          invoiceDateYear: 'ASSUMED',
          total: '95.00',
          payableAmount: '177.70',
          payments: [
            { date: '2026-10-18', amount: '95.00', method: 'CARD', source: 'SLIP' },
            { date: '2026-10-19', amount: '82.70', method: 'CARD', source: 'RECEIPT' },
          ],
          crossedOut: ['25 lb Redart Clay — 19.00'],
          otherPapers: 'Two till receipts on top',
          notes: ['Two card payments cover lines plus tax.'],
          rotation: 90,
          problems: [],
          explanations: ['Paid 95.00 on 2026-10-18 and 82.70 on 2026-10-19: 95.00 + 82.70 = 177.70 matches lines and tax (written total is 95.00).'],
          lines: [
            { description: 'Lift-gate freight', quantity: '1', lineTotal: '75.00', kind: 'SHIPPING', unclear: true, serialNumbers: ['SN-100'] },
          ],
        }),
      ),
      http.put(`${API}/api/v1/studios/:studioId/invoices/:invoiceId`, async ({ request }) => {
        saved = (await request.json()) as typeof saved;
        return HttpResponse.json({ ...read, id: 'inv2', status: 'READY', version: 3 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/supplies`);
    await user.click(await screen.findByRole('button', { name: 'Open' }));

    expect(await screen.findByLabelText('Document type')).toHaveValue('ORDER_FORM');
    expect(screen.getByText(/Two card payments cover lines plus tax/)).toBeInTheDocument();
    expect(screen.getByText('25 lb Redart Clay — 19.00')).toBeInTheDocument();
    expect(screen.getByText(/Two till receipts on top/)).toBeInTheDocument();
    expect(screen.getByText(/95\.00 on 2026-10-18 \(card\) — from slip/)).toBeInTheDocument();
    expect(screen.getByText(/Payable:/)).toHaveTextContent('Payable: 177.70');
    expect(screen.getByText(/Year assumed/)).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'The invoice' })).toHaveAttribute('data-rotation', '90');
    expect(screen.getByLabelText('Line 1 kind')).toHaveValue('SHIPPING');
    expect(screen.getByText(/Unclear/)).toBeInTheDocument();
    expect(screen.getByText('S/N: SN-100')).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText('Line 1 kind'), 'FEE');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => expect(saved?.lines[0]?.kind).toBe('FEE'));
    expect(saved?.documentType).toBe('ORDER_FORM');
  });

  it('formats documentType labels cleanly in Compare readers', async () => {
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
            {
              reader: 'CLAUDE',
              outcome: 'READ',
              documentType: 'ORDER_FORM',
              elapsedMs: 1200,
              total: '170.00',
              lines: read.lines,
              problems: [],
            },
          ],
        }),
      ),
    );

    const { user } = renderApp(`/studios/${studio.id}/supplies`);
    await user.click(await screen.findByRole('button', { name: 'Open' }));
    await user.click(await screen.findByRole('button', { name: 'Compare readers' }));

    const compareSection = screen.getByRole('heading', { name: 'Compare readers' }).closest('section');
    expect(compareSection).not.toBeNull();
    if (compareSection) {
      expect(await within(compareSection).findByText('Order form')).toBeInTheDocument();
      expect(within(compareSection).queryByText('ORDER_FORM')).not.toBeInTheDocument();
    }
  });

  it('submits inline new-item form on Enter without saving the invoice draft, and shifts index when an earlier line is removed', async () => {
    const studio = aStudio();
    let draftSaved = false;
    const supplyList: SupplyItem[] = [clay];
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/supplies`, () => HttpResponse.json(supplyList)),
      http.post(`${API}/api/v1/studios/:studioId/supplies`, () => {
        const created: SupplyItem = {
          id: 'i-enter',
          name: 'EPK Kaolin',
          category: 'GLAZE_MATERIAL',
          unit: 'lb',
          onHand: '0',
          onOrder: '0',
          low: false,
          needsOrdering: false,
          version: 0,
        };
        supplyList.push(created);
        return HttpResponse.json(created, { status: 201 });
      }),
      http.put(`${API}/api/v1/studios/:studioId/invoices/:invoiceId`, () => {
        draftSaved = true;
        return HttpResponse.json(read);
      }),
      http.get(`${API}/api/v1/studios/:studioId/purchase-requests`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/invoices`, () =>
        HttpResponse.json({
          items: [{ id: 'inv1', status: 'READY', fileType: 'PDF', uploadedAt: '2026-11-06T08:00:00Z' }],
          truncated: false,
          limit: 100,
        }),
      ),
      http.get(`${API}/api/v1/studios/:studioId/invoices/:invoiceId`, () =>
        HttpResponse.json({
          ...read,
          status: 'READY',
          problems: [],
          lines: [
            { description: 'Scrap line', quantity: '1', unit: 'each', kind: 'OTHER' },
            { description: 'EPK Kaolin', quantity: '50', unit: 'lb', kind: 'ITEM' },
          ],
        }),
      ),
    );

    const { user } = renderApp(`/studios/${studio.id}/supplies`);
    await user.click(await screen.findByRole('button', { name: 'Open' }));

    await user.click(await screen.findByRole('button', { name: '+ New item for line 2' }));
    await user.click(screen.getByRole('button', { name: 'Remove line 1' }));

    const shiftedForm = within(screen.getByRole('group', { name: 'New supply item for line 1' }));
    await user.type(shiftedForm.getByLabelText('Item name'), '{Enter}');

    await waitFor(() => expect(screen.getByLabelText('Line 1 item')).toHaveValue('i-enter'));
    expect(draftSaved).toBe(false);
  });

  it('shows purchase request load errors and clears all Add item fields after creating a supply', async () => {
    const studio = aStudio();
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/supplies`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/purchase-requests`, () =>
        HttpResponse.json(
          { title: 'Forbidden', status: 403, detail: 'Purchase requests unavailable.' },
          { status: 403, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
      http.get(`${API}/api/v1/studios/:studioId/invoices`, () =>
        HttpResponse.json({ items: [], truncated: false, limit: 100 }),
      ),
      http.post(`${API}/api/v1/studios/:studioId/supplies`, () =>
        HttpResponse.json({ ...clay, id: 'i-added', name: 'Porcelain' }, { status: 201 }),
      ),
    );

    const { user } = renderApp(`/studios/${studio.id}/supplies`);
    expect(await screen.findByText('Purchase requests unavailable.')).toBeInTheDocument();

    const addForm = within(screen.getByRole('form', { name: 'Add an item' }));
    await user.type(addForm.getByLabelText('Name'), 'Porcelain');
    await user.type(addForm.getByLabelText('Unit'), '25 lb bag');
    await user.type(addForm.getByLabelText('Supplier'), 'Laguna');
    await user.type(addForm.getByLabelText('Reorder at'), '4');
    await user.type(addForm.getByLabelText('Order this many'), '10');
    await user.type(addForm.getByLabelText('Price to members (USD)'), '28');
    await user.click(addForm.getByRole('button', { name: 'Add item' }));

    await waitFor(() => expect(addForm.getByLabelText('Name')).toHaveValue(''));
    expect(addForm.getByLabelText('Unit')).toHaveValue('');
    expect(addForm.getByLabelText('Supplier')).toHaveValue('');
    expect(addForm.getByLabelText('Reorder at')).toHaveValue('');
    expect(addForm.getByLabelText('Order this many')).toHaveValue('');
    expect(addForm.getByLabelText('Price to members (USD)')).toHaveValue('');
  });
});
