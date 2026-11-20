import type {
  Firing,
  ImportKilnLogRequest,
  Kiln,
  KilnLogImportResult,
  LoadKilnRequest,
  Notification,
  QueueEntry,
} from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aPiece, aStudio, renderApp } from './renderApp';
import { API, capped, server } from './server';

const studio = aStudio();
const kiln: Kiln = {
  id: 'k1',
  name: 'Skutt 1027',
  kind: 'ELECTRIC',
  maxPiecesPerMember: 1,
  totalFirings: 120,
  available: true,
  version: 3,
};

describe('kilns', () => {
  it('fills a load from the queue, leaving pieces over the per-member cap', async () => {
    const queue: QueueEntry[] = [
      { piece: aPiece({ id: 'p1', number: 1 }), waitingSince: '2026-10-01T10:00:00Z', fitsNextLoad: true },
      { piece: aPiece({ id: 'p2', number: 2 }), waitingSince: '2026-10-02T10:00:00Z', fitsNextLoad: false },
      { piece: aPiece({ id: 'p3', number: 3, makerName: 'Nia' }), waitingSince: '2026-10-03T10:00:00Z', fitsNextLoad: true },
    ];
    let loaded: LoadKilnRequest | undefined;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/kilns`, () => HttpResponse.json([kiln])),
      http.get(`${API}/api/v1/studios/:studioId/kilns/:kilnId/firings`, () => HttpResponse.json(capped([]))),
      http.get(`${API}/api/v1/studios/:studioId/kilns/:kilnId/queue`, () => HttpResponse.json(capped(queue))),
      http.post(`${API}/api/v1/studios/:studioId/kilns/:kilnId/firings`, async ({ request }) => {
        loaded = (await request.json()) as LoadKilnRequest;
        const firing: Firing = {
          id: 'f1',
          kilnId: 'k1',
          kilnName: kiln.name,
          kind: 'BISQUE',
          status: 'LOADING',
          createdAt: '2026-10-20T10:00:00Z',
          pieceCount: 2,
          version: 0,
          pieces: [],
        };
        return HttpResponse.json(firing, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/kilns/k1`);
    expect(await screen.findByRole('heading', { name: 'Skutt 1027' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Maintenance tasks and history for this kiln' })).toBeInTheDocument();

    await user.click(await screen.findByRole('button', { name: 'Fill from the queue' }));
    await user.type(screen.getByLabelText('Cone'), '04');
    await user.click(screen.getByRole('button', { name: 'Load 2 pieces' }));

    await expect.poll(() => loaded).toEqual({ kind: 'BISQUE', pieceIds: ['p1', 'p3'], cone: '04' });
  });

  it("shows a member their ready notice on the studio's home page", async () => {
    const notice: Notification = {
      id: 'n1',
      kind: 'PIECES_READY',
      title: 'Your 2 pieces are ready to collect',
      body: 'Piece 1: Jar\nPiece 2: Bowl\nPlease collect by 19 Nov 2026.',
      createdAt: '2026-10-20T10:00:00Z',
    };
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(aStudio({ myRole: 'MEMBER' }))),
      http.get(`${API}/api/v1/studios/:studioId/notifications`, () => HttpResponse.json(capped([notice]))),
    );

    renderApp(`/studios/${studio.id}`);

    expect(await screen.findByText('Your 2 pieces are ready to collect')).toBeInTheDocument();
    expect(screen.getByText(/Please collect by 19 Nov 2026/)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Tell every member' })).not.toBeInTheDocument();
  });

  it('previews a Genesis CSV log, suggests an overlapping firing, imports it, and shows curve telemetry', async () => {
    const existingFiring: Firing = {
      id: 'f-existing',
      kilnId: 'k1',
      kilnName: kiln.name,
      kind: 'GLAZE',
      cone: '6',
      status: 'COMPLETE',
      createdAt: '2026-11-10T14:00:00Z',
      startedAt: '2026-11-10T14:00:00Z',
      finishedAt: '2026-11-10T23:30:00Z',
      pieceCount: 12,
      version: 2,
      pieces: [],
    };
    const curve = [
      { elapsedMinutes: 0, temp: 72, setpoint: 72, zoneTemps: [72, 71, 72] },
      { elapsedMinutes: 240, temp: 1455, setpoint: 1460, zoneTemps: [1455, 1452, 1458] },
      { elapsedMinutes: 480, temp: 2232, setpoint: 2232, zoneTemps: [2232, 2230, 2234] },
      { elapsedMinutes: 570, temp: 1500, setpoint: 1500, zoneTemps: [1500, 1498, 1502] },
    ];
    let currentFirings: Firing[] = [existingFiring];
    const requests: ImportKilnLogRequest[] = [];

    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/kilns`, () => HttpResponse.json([kiln])),
      http.get(`${API}/api/v1/studios/:studioId/kilns/:kilnId/firings`, () => HttpResponse.json(capped(currentFirings))),
      http.get(`${API}/api/v1/studios/:studioId/kilns/:kilnId/queue`, () => HttpResponse.json(capped([]))),
      http.post(`${API}/api/v1/studios/:studioId/kilns/:kilnId/log-imports`, async ({ request }) => {
        const body = (await request.json()) as ImportKilnLogRequest;
        requests.push(body);
        if (body.dryRun) {
          const preview: KilnLogImportResult = {
            committed: false,
            source: body.source,
            presetUnverified: true,
            tempUnit: 'F',
            tempUnitInferred: true,
            startedAt: '2026-11-10T14:00:00Z',
            finishedAt: '2026-11-10T23:30:00Z',
            durationMinutes: 570,
            peakTemp: 2232,
            detectedCone: '6',
            errorCode: 'E-1',
            rowCount: 18,
            zoneCount: 3,
            detectedColumns: ['Elapsed', 'TC1', 'TC2', 'TC3', 'Setpoint', 'Error'],
            warnings: [],
            temperatureCurve: curve,
            suggestedMatch: {
              firingId: existingFiring.id,
              kind: 'GLAZE',
              cone: '6',
              status: 'COMPLETE',
              createdAt: existingFiring.createdAt,
              startedAt: existingFiring.startedAt,
              finishedAt: existingFiring.finishedAt,
              overlapDescription: 'Overlaps this log directly',
            },
          };
          return HttpResponse.json(preview);
        }
        const updated: Firing = {
          ...existingFiring,
          peakTemp: 2232,
          tempUnit: 'F',
          durationMinutes: 570,
          errorCode: 'E-1',
          logSource: 'GENESIS',
          temperatureCurve: curve,
        };
        currentFirings = [updated];
        const committed: KilnLogImportResult = {
          committed: true,
          source: body.source,
          presetUnverified: true,
          tempUnit: 'F',
          tempUnitInferred: true,
          startedAt: '2026-11-10T14:00:00Z',
          finishedAt: '2026-11-10T23:30:00Z',
          durationMinutes: 570,
          peakTemp: 2232,
          detectedCone: '6',
          errorCode: 'E-1',
          rowCount: 18,
          zoneCount: 3,
          detectedColumns: ['Elapsed', 'TC1', 'TC2', 'TC3', 'Setpoint', 'Error'],
          warnings: [],
          temperatureCurve: curve,
          firing: updated,
        };
        return HttpResponse.json(committed);
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/kilns/k1`);
    expect(await screen.findByRole('heading', { name: 'Import controller log' })).toBeInTheDocument();

    const csvFile = new File(['Elapsed,TC1,TC2,TC3,Setpoint,Error\n00:00:00,72,71,72,72,\n08:00:00,2232,2230,2234,2232,E-1\n'], 'genesis.csv', {
      type: 'text/csv',
    });
    await user.upload(screen.getByLabelText('Controller CSV'), csvFile);
    expect(await screen.findByText('Selected file: genesis.csv')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Preview log' }));

    const previewRegion = await screen.findByRole('region', { name: 'Log preview' });
    expect(within(previewRegion).getByText('2232 °F (inferred)')).toBeInTheDocument();
    expect(within(previewRegion).getByText('9h 30m')).toBeInTheDocument();
    expect(within(previewRegion).getByText('E-1')).toBeInTheDocument();
    expect(within(previewRegion).getByText(/Overlaps this log directly/)).toBeInTheDocument();
    expect(within(previewRegion).getByRole('img', { name: /peaking at 2232 °F/i })).toBeInTheDocument();
    expect(requests[0]?.dryRun).toBe(true);

    await user.click(within(previewRegion).getByRole('button', { name: 'Import log' }));
    expect(await screen.findByText(/Log imported \(2232 °F, 9h 30m\)/)).toBeInTheDocument();
    expect(requests[1]).toMatchObject({ dryRun: false, source: 'GENESIS', firingId: 'f-existing' });

    const firingLog = screen.getByRole('region', { name: 'Firing log' });
    expect(await within(firingLog).findByText('2232 °F')).toBeInTheDocument();
    expect(within(firingLog).getByText('Error: E-1')).toBeInTheDocument();
    expect(within(firingLog).getByText('Genesis')).toBeInTheDocument();
    expect(within(firingLog).getByRole('img', { name: /peaking at 2232 °F/i })).toBeInTheDocument();
  });

  it('creates a new complete firing from a TAP CSV log with unit override', async () => {
    const curve = [
      { elapsedMinutes: 0, temp: 22, setpoint: 22 },
      { elapsedMinutes: 390, temp: 1222, setpoint: 1222 },
      { elapsedMinutes: 510, temp: 820, setpoint: 0 },
    ];
    let currentFirings: Firing[] = [];
    const requests: ImportKilnLogRequest[] = [];

    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/kilns`, () => HttpResponse.json([kiln])),
      http.get(`${API}/api/v1/studios/:studioId/kilns/:kilnId/firings`, () => HttpResponse.json(capped(currentFirings))),
      http.get(`${API}/api/v1/studios/:studioId/kilns/:kilnId/queue`, () => HttpResponse.json(capped([]))),
      http.post(`${API}/api/v1/studios/:studioId/kilns/:kilnId/log-imports`, async ({ request }) => {
        const body = (await request.json()) as ImportKilnLogRequest;
        requests.push(body);
        if (body.dryRun) {
          return HttpResponse.json({
            committed: false,
            source: body.source,
            presetUnverified: true,
            tempUnit: 'C',
            tempUnitInferred: false,
            durationMinutes: 510,
            peakTemp: 1222,
            detectedCone: '6',
            rowCount: 13,
            zoneCount: 1,
            detectedColumns: ['Timestamp', 'Temp (C)', 'Target (C)', 'Output %', 'Event'],
            warnings: [],
            temperatureCurve: curve,
          } satisfies KilnLogImportResult);
        }
        const created: Firing = {
          id: 'f-new',
          kilnId: 'k1',
          kilnName: kiln.name,
          kind: body.kind ?? 'BISQUE',
          cone: body.cone,
          status: 'COMPLETE',
          createdAt: '2026-11-10T17:30:00Z',
          pieceCount: 0,
          version: 0,
          pieces: [],
          peakTemp: 1222,
          tempUnit: 'C',
          durationMinutes: 510,
          logSource: 'TAP',
          temperatureCurve: curve,
        };
        currentFirings = [created];
        return HttpResponse.json({
          committed: true,
          source: body.source,
          presetUnverified: true,
          tempUnit: 'C',
          tempUnitInferred: false,
          durationMinutes: 510,
          peakTemp: 1222,
          detectedCone: '6',
          rowCount: 13,
          zoneCount: 1,
          detectedColumns: ['Timestamp', 'Temp (C)', 'Target (C)', 'Output %', 'Event'],
          warnings: [],
          temperatureCurve: curve,
          firing: created,
        } satisfies KilnLogImportResult);
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/kilns/k1`);
    await screen.findByRole('heading', { name: 'Import controller log' });

    await user.selectOptions(screen.getByLabelText('Controller preset'), 'TAP');
    await user.selectOptions(screen.getByLabelText('Temperature unit'), 'C');
    await user.upload(
      screen.getByLabelText('Controller CSV'),
      new File(['Timestamp,Temp (C),Target (C)\n2026-11-10 09:00,22,22\n2026-11-10 15:30,1222,1222\n'], 'tap.csv', {
        type: 'text/csv',
      }),
    );
    expect(await screen.findByText('Selected file: tap.csv')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Preview log' }));

    const previewRegion = await screen.findByRole('region', { name: 'Log preview' });
    expect(within(previewRegion).getByText('1222 °C')).toBeInTheDocument();
    expect(within(previewRegion).getByLabelText('New firing cone')).toHaveValue('6');

    await user.selectOptions(within(previewRegion).getByLabelText('New firing kind'), 'GLAZE');
    await user.click(within(previewRegion).getByRole('button', { name: 'Import log' }));
    expect(await screen.findByText(/Log imported \(1222 °C, 8h 30m\)/)).toBeInTheDocument();

    expect(requests[1]).toMatchObject({ dryRun: false, source: 'TAP', tempUnit: 'C', kind: 'GLAZE', cone: '6' });
    expect(requests[1]?.firingId).toBeUndefined();

    const firingLog = screen.getByRole('region', { name: 'Firing log' });
    expect(await within(firingLog).findByText('1222 °C')).toBeInTheDocument();
    expect(within(firingLog).getByText('8h 30m')).toBeInTheDocument();
    expect(within(firingLog).getByText('TAP')).toBeInTheDocument();
  });
});
