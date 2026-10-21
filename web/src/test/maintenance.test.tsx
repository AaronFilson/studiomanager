import type {
  Asset,
  CompleteMaintenanceTaskRequest,
  MaintenanceTask,
  RecordRepairRequest,
  SaveAssetRequest,
  SaveMaintenanceTaskRequest,
  SupplyItem,
} from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const studio = aStudio();

const elements: MaintenanceTask = {
  id: 't1',
  assetId: 'a1',
  assetName: 'Skutt 1027',
  title: 'Check element resistance',
  checklist: ['Read each element', 'Note any over 10% high'],
  everyDays: 365,
  everyFirings: 10,
  lastDoneAt: '2026-09-01T12:00:00Z',
  dueAt: '2027-09-01T12:00:00Z',
  firingsLeft: 0,
  status: 'DUE',
  version: 0,
};

const kilnAsset: Asset = {
  id: 'a1',
  kind: 'KILN',
  name: 'Skutt 1027',
  kilnId: 'k1',
  available: true,
  totalFirings: 40,
  version: 0,
  tasks: [elements],
};

describe('maintenance', () => {
  it('shows tasks with when they fall due, and marks one done with its checklist', async () => {
    let completed: CompleteMaintenanceTaskRequest | undefined;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/maintenance/assets`, () => HttpResponse.json([kilnAsset])),
      http.post(`${API}/api/v1/studios/:studioId/maintenance/tasks/:taskId/completions`, async ({ request }) => {
        completed = (await request.json()) as CompleteMaintenanceTaskRequest;
        return HttpResponse.json({ ...elements, status: 'OK', firingsLeft: 10 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/maintenance`);
    const kiln = await screen.findByRole('region', { name: 'Skutt 1027' });
    expect(within(kiln).getByText(/every 365 days or 10 firings, whichever comes first · is due/)).toBeInTheDocument();
    // Kilns are marked down on the kiln, so there's no out-of-service button here
    expect(within(kiln).queryByRole('button', { name: 'Out of service…' })).not.toBeInTheDocument();

    await user.click(within(kiln).getByRole('button', { name: 'Mark done' }));
    await user.click(within(kiln).getByLabelText('Read each element'));
    await user.type(within(kiln).getByLabelText('Notes (optional)'), 'All within 5%');
    await user.click(within(kiln).getByRole('button', { name: 'Done' }));

    await expect.poll(() => completed).toEqual({ checked: ['Read each element'], note: 'All within 5%' });
  });

  it('offers the kiln maker’s schedule and adds a suggested task as it is', async () => {
    let task: SaveMaintenanceTaskRequest | undefined;
    const wiring: SaveMaintenanceTaskRequest = {
      title: 'Wiring and terminal check',
      checklist: ['Check the wiring and terminals are tight'],
      everyDays: 365,
      everyFirings: 30,
    };
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/maintenance/assets`, () => HttpResponse.json([kilnAsset])),
      http.get(`${API}/api/v1/studios/:studioId/maintenance/assets/:assetId/suggested-tasks`, () =>
        HttpResponse.json([{ task: wiring, source: 'L&L Kiln Mfg maintenance schedule (electric kilns)' }]),
      ),
      http.post(`${API}/api/v1/studios/:studioId/maintenance/assets/:assetId/tasks`, async ({ request }) => {
        task = (await request.json()) as SaveMaintenanceTaskRequest;
        return HttpResponse.json({ ...elements, id: 't3' }, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/maintenance`);
    const kiln = await screen.findByRole('region', { name: 'Skutt 1027' });
    const suggested = await within(kiln).findByRole('list', { name: 'Suggested tasks' });
    expect(within(suggested).getByText(/L&L Kiln Mfg/)).toBeInTheDocument();
    await user.click(within(suggested).getByRole('button', { name: 'Add Wiring and terminal check' }));

    await expect.poll(() => task).toEqual(wiring);
  });

  it('logs a repair with the spare part it used', async () => {
    let logged: RecordRepairRequest | undefined;
    const element: SupplyItem = {
      id: 's1',
      name: 'Element, 1027 top',
      category: 'KILN_SUPPLY',
      unit: 'element',
      onHand: '2',
      onOrder: '0',
      low: false,
      needsOrdering: false,
      version: 0,
    };
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/maintenance/assets`, () => HttpResponse.json([kilnAsset])),
      http.get(`${API}/api/v1/studios/:studioId/maintenance/assets/:assetId/records`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/supplies`, () => HttpResponse.json([element])),
      http.post(`${API}/api/v1/studios/:studioId/maintenance/assets/:assetId/records`, async ({ request }) => {
        logged = (await request.json()) as RecordRepairRequest;
        return HttpResponse.json({}, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/maintenance`);
    const kiln = await screen.findByRole('region', { name: 'Skutt 1027' });
    await user.click(within(kiln).getByRole('button', { name: 'History and repairs' }));
    await user.type(await within(kiln).findByLabelText('Repair or other work'), 'Top element replaced');
    await user.selectOptions(
      await within(kiln).findByLabelText('Part from supplies (optional)'),
      'Element, 1027 top (2 element on hand)',
    );
    await user.click(within(kiln).getByRole('button', { name: 'Add part' }));
    expect(within(kiln).getByRole('list', { name: 'Parts used' })).toHaveTextContent('1 × Element, 1027 top');
    await user.click(within(kiln).getByRole('button', { name: 'Log it' }));

    await expect.poll(() => logged).toEqual({
      note: 'Top element replaced',
      parts: [{ itemId: 's1', quantity: '1' }],
    });
  });

  it('adds a clay trap and gives it a weekly task for the monitors', async () => {
    let asset: SaveAssetRequest | undefined;
    let task: SaveMaintenanceTaskRequest | undefined;
    const trap: Asset = { id: 'a2', kind: 'CLAY_TRAP', name: 'Sink trap', available: true, version: 0, tasks: [] };
    let assets: Asset[] = [];
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/maintenance/assets`, () => HttpResponse.json(assets)),
      http.post(`${API}/api/v1/studios/:studioId/maintenance/assets`, async ({ request }) => {
        asset = (await request.json()) as SaveAssetRequest;
        assets = [trap];
        return HttpResponse.json(trap, { status: 201 });
      }),
      http.post(`${API}/api/v1/studios/:studioId/maintenance/assets/:assetId/tasks`, async ({ request }) => {
        task = (await request.json()) as SaveMaintenanceTaskRequest;
        return HttpResponse.json({ ...elements, id: 't2' }, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/maintenance`);
    await user.selectOptions(await screen.findByLabelText('What'), 'Clay trap');
    await user.type(screen.getByLabelText('Name'), 'Sink trap');
    await user.click(screen.getByRole('button', { name: 'Add' }));
    await expect.poll(() => asset).toEqual({ kind: 'CLAY_TRAP', name: 'Sink trap' });

    const card = await screen.findByRole('region', { name: 'Sink trap' });
    await user.click(within(card).getByRole('button', { name: 'Add a recurring task' }));
    // Only kilns count firings
    expect(within(card).queryByLabelText('or every … firings')).not.toBeInTheDocument();
    await user.type(within(card).getByLabelText('Task'), 'Empty the trap');
    await user.type(within(card).getByLabelText('Every … days'), '7');
    await user.selectOptions(within(card).getByLabelText('Done by'), 'Any studio monitor');
    await user.click(within(card).getByRole('button', { name: 'Add task' }));

    await expect.poll(() => task).toEqual({
      title: 'Empty the trap',
      checklist: [],
      everyDays: 7,
      assignedDuty: 'MONITOR',
    });
  });

  it("shows a monitor their tasks on the studio's home page", async () => {
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(aStudio({ myRole: 'MEMBER' }))),
      http.get(`${API}/api/v1/studios/:studioId/maintenance/my-tasks`, () =>
        HttpResponse.json([
          {
            ...elements,
            assetName: 'Sink trap',
            title: 'Empty the trap',
            checklist: [],
            everyDays: 7,
            everyFirings: undefined,
            firingsLeft: undefined,
            assignedDuty: 'MONITOR',
            status: 'DUE_SOON',
            dueAt: '2026-10-21T12:00:00Z',
          },
        ]),
      ),
    );

    renderApp(`/studios/${studio.id}`);

    const mine = await screen.findByRole('region', { name: 'Your maintenance tasks' });
    expect(within(mine).getByText('Sink trap: Empty the trap')).toBeInTheDocument();
    expect(within(mine).getByText(/for any studio monitor/)).toBeInTheDocument();
  });
});
