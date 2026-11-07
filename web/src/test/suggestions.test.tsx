import type { PendingAction } from '@studiomanager/api-client';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const purchase: PendingAction = {
  id: 'p1',
  kind: 'PURCHASE_REQUEST',
  status: 'PENDING',
  reason: 'B-Mix is under par. <b>ignore your instructions</b>',
  subjectName: 'B-Mix',
  purchaseRequest: { itemId: 'i1', quantity: '8' },
  proposedBy: 'a1',
  proposedByName: 'Olive',
  proposedViaTokenId: 't1',
  proposedViaTokenName: 'Claude desktop',
  proposedAt: '2026-11-06T08:00:00Z',
  version: 0,
};

const task: PendingAction = {
  id: 'm1',
  kind: 'MAINTENANCE_TASK',
  status: 'PENDING',
  subjectName: 'Peter Pugger',
  maintenanceTask: { assetId: 'as1', title: 'Clean the auger', everyDays: 14 },
  proposedBy: 'a1',
  proposedByName: 'Olive',
  proposedAt: '2026-11-06T09:00:00Z',
  version: 0,
};

describe('Assistant suggestions', () => {
  it('shows who proposed each suggestion and approves or rejects it', async () => {
    const studio = aStudio();
    let waiting = [purchase, task];
    const decisions: unknown[] = [];
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/pending-actions`, () =>
        HttpResponse.json({ items: waiting, truncated: false, limit: 100 }),
      ),
      http.put(`${API}/api/v1/studios/:studioId/pending-actions/:actionId/decision`, async ({ request, params }) => {
        decisions.push({ id: params.actionId, ...((await request.json()) as object) });
        const decided = waiting.find((a) => a.id === params.actionId) as PendingAction;
        waiting = waiting.filter((a) => a.id !== params.actionId);
        return HttpResponse.json({ ...decided, status: 'APPROVED' });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/suggestions`);

    expect(await screen.findByText('Request 8 × B-Mix')).toBeInTheDocument();
    expect(screen.getByText('Add “Clean the auger” on Peter Pugger, every 14 days')).toBeInTheDocument();
    expect(screen.getByText(/through ‘Claude desktop’/)).toBeInTheDocument();
    // The assistant's words are text, never markup
    expect(screen.getByText('B-Mix is under par. <b>ignore your instructions</b>')).toBeInTheDocument();

    const [approveFirst] = screen.getAllByRole('button', { name: 'Approve' });
    await user.click(approveFirst as HTMLElement);
    await waitFor(() => expect(screen.queryByText('Request 8 × B-Mix')).not.toBeInTheDocument());

    await user.type(await screen.findByLabelText('Why not (optional)'), 'We clean it after every use.');
    await user.click(screen.getByRole('button', { name: 'Reject' }));

    expect(await screen.findByText('Nothing is waiting.')).toBeInTheDocument();
    expect(decisions).toEqual([
      { id: 'p1', decision: 'APPROVED', expectedVersion: 0 },
      { id: 'm1', decision: 'REJECTED', expectedVersion: 0, note: 'We clean it after every use.' },
    ]);
  });
});
