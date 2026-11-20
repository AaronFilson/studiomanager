import type { GlazeImportResult, GlazeMix, GlazeRecipe } from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const tenmoku: GlazeRecipe = {
  id: 'g1',
  ownerKind: 'STUDIO',
  name: 'Tenmoku',
  cone: '6',
  atmosphere: 'OXIDATION',
  foodSurface: true,
  targetSpecificGravity: '1.45',
  sharedWithStudio: false,
  source: 'MANUAL',
  lines: [
    { kind: 'BASE', material: 'Custer Feldspar', amount: '45', unit: 'PARTS' },
    { kind: 'ADDITION', material: 'Red Iron Oxide', amount: '10.5', unit: 'PERCENT' },
  ],
  canEdit: false,
  version: 1,
  updatedAt: '2026-11-05T12:00:00Z',
};

const leachClear: GlazeRecipe = {
  id: 'g-leach',
  ownerKind: 'STUDIO',
  name: 'Leach 4321',
  cone: '10',
  atmosphere: 'REDUCTION',
  foodSurface: true,
  targetSpecificGravity: '1.45',
  sharedWithStudio: false,
  source: 'MANUAL',
  lines: [
    { kind: 'BASE', material: 'Custer Feldspar', amount: '40', unit: 'PARTS' },
    { kind: 'BASE', material: 'Silica', amount: '30', unit: 'PARTS' },
    { kind: 'BASE', material: 'Whiting', amount: '20', unit: 'PARTS' },
    { kind: 'BASE', material: 'EPK', amount: '10', unit: 'PARTS' },
    { kind: 'ADDITION', material: 'Mystery Stain X', amount: '2', unit: 'PERCENT' },
  ],
  photos: [
    {
      id: 'p1',
      url: 'https://blob.test/full.jpg',
      thumbnailUrl: 'https://blob.test/thumb.jpg',
      uploadedByAccountId: 'a1',
      uploadedAt: '2026-11-06T12:00:00Z',
    },
  ],
  canEdit: true,
  version: 1,
  updatedAt: '2026-11-05T12:00:00Z',
};

describe('glaze book', () => {
  it('lists the studio glazes, and a member adds a recipe of their own', async () => {
    const studio = aStudio({ myRole: 'MEMBER' });
    let sent: unknown;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/glazes`, ({ request }) =>
        HttpResponse.json(new URL(request.url).searchParams.get('owner') === 'STUDIO' ? [tenmoku] : []),
      ),
      http.post(`${API}/api/v1/studios/:studioId/glazes`, async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json({ ...tenmoku, id: 'g2', ownerKind: 'MEMBER', name: 'Kaki' }, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/glazes`);
    expect(await screen.findByRole('link', { name: 'Tenmoku' })).toBeInTheDocument();
    // Members can't add studio glazes
    expect(screen.queryByRole('button', { name: 'Add a studio glaze' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: 'My recipes' }));
    expect(await screen.findByText('You have no recipes yet.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Add a recipe' }));
    await user.type(screen.getByLabelText('Name'), 'Kaki');
    await user.type(screen.getByLabelText('Material 1'), 'Silica');
    await user.type(screen.getByLabelText('Amount 1'), '30');
    await user.click(screen.getByLabelText('Share with the studio'));
    await user.click(screen.getByRole('button', { name: 'Save' }));

    await screen.findByRole('button', { name: 'Add a recipe' });
    expect(sent).toEqual({
      name: 'Kaki',
      lines: [{ kind: 'BASE', material: 'Silica', amount: '30', unit: 'PARTS' }],
      foodSurface: false,
      sharedWithStudio: true,
      ownerKind: 'MEMBER',
    });
  });

  it('shows a recipe, with additions marked, and no edit for a member', async () => {
    const studio = aStudio({ myRole: 'MEMBER' });
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/glazes/:glazeId`, () => HttpResponse.json(tenmoku)),
    );

    renderApp(`/studios/${studio.id}/glazes/g1`);

    expect(await screen.findByRole('heading', { name: 'Tenmoku' })).toBeInTheDocument();
    expect(screen.getByText('Studio glaze · cone 6 · oxidation · target SG 1.45')).toBeInTheDocument();
    expect(screen.getByText('+ Red Iron Oxide')).toBeInTheDocument();
    expect(screen.getByText('10.5 %')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument();
  });

  it('weighs out a batch in the unit asked for', async () => {
    const studio = aStudio({ myRole: 'MEMBER' });
    let asked = '';
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/glazes/:glazeId`, () => HttpResponse.json(tenmoku)),
      http.get(`${API}/api/v1/studios/:studioId/glazes/:glazeId/batch`, ({ request }) => {
        asked = new URL(request.url).search;
        return HttpResponse.json({
          size: '10',
          unit: 'KILOGRAM',
          lines: [
            { kind: 'BASE', material: 'Custer Feldspar', weight: '10.000' },
            { kind: 'ADDITION', material: 'Red Iron Oxide', weight: '1.050' },
          ],
          total: '11.050',
        });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/glazes/g1`);
    await user.type(await screen.findByLabelText('Dry batch size'), '10');

    expect(await screen.findByText('11.050 kg')).toBeInTheDocument();
    expect(screen.getByText('1.050 kg')).toBeInTheDocument();
    expect(asked).toBe('?size=10&unit=KILOGRAM');
  });

  it('renders UMF chemistry analysis, unmatched materials note, and slurry SG adjustments', async () => {
    const studio = aStudio({ myRole: 'STAFF' });
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/glazes/:glazeId`, () => HttpResponse.json(leachClear)),
    );

    const { user } = renderApp(`/studios/${studio.id}/glazes/${leachClear.id}`);

    expect(await screen.findByRole('heading', { name: 'Chemistry & UMF analysis' })).toBeInTheDocument();
    expect(screen.getByText(/Glossy/)).toBeInTheDocument();
    expect(screen.getByText(/within typical Cone 10/)).toBeInTheDocument();
    expect(screen.getByText(/Unmatched materials.*Mystery Stain X/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open Glazecalc' })).toHaveAttribute('href', 'https://glazecalcapp.com');
    expect(screen.getByAltText('Test tile 1 of Leach 4321')).toBeInTheDocument();

    // Slurry SG Helper: measured 1.50 vs target 1.45 for 1000 mL -> add 111.1 g water
    await user.type(screen.getByLabelText('Measured SG'), '1.50');
    expect(await screen.findByText(/111\.1 g/)).toBeInTheDocument();
  });

  it('logs a studio mix, records an off-target SG reading, and displays warnings and SVG chart', async () => {
    const studio = aStudio({ myRole: 'STAFF' });
    let mixes: GlazeMix[] = [];
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/glazes/:glazeId`, () => HttpResponse.json(leachClear)),
      http.get(`${API}/api/v1/studios/:studioId/glazes/:glazeId/mixes`, () => HttpResponse.json(mixes)),
      http.post(`${API}/api/v1/studios/:studioId/glazes/:glazeId/mixes`, () => {
        const created: GlazeMix = {
          id: 'mix-1',
          recipeId: leachClear.id,
          mixedByAccountId: 'a1',
          mixedByName: 'Olive',
          mixedAt: '2026-11-10T10:00:00Z',
          dryWeightG: '10000',
          waterG: '7500',
          notes: 'Fresh bucket',
          readings: [
            {
              id: 'r1',
              readByAccountId: 'a1',
              readByName: 'Olive',
              readAt: '2026-11-10T10:00:00Z',
              specificGravity: '1.46',
              note: 'Initial reading',
              offTarget: false,
            },
          ],
        };
        mixes = [created];
        return HttpResponse.json(created, { status: 201 });
      }),
      http.post(`${API}/api/v1/studios/:studioId/glazes/:glazeId/mixes/:mixId/readings`, () => {
        const reading = {
          id: 'r2',
          readByAccountId: 'a1',
          readByName: 'Olive',
          readAt: '2026-11-12T10:00:00Z',
          specificGravity: '1.52',
          note: 'Evaporated',
          offTarget: true,
          warning: 'Specific gravity 1.52 is 0.07 above the target 1.45 (±0.03).',
        };
        if (mixes[0]) {
          mixes = [{ ...mixes[0], readings: [...mixes[0].readings, reading] }];
        }
        return HttpResponse.json(reading, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/glazes/${leachClear.id}`);

    const mixForm = await screen.findByRole('form', { name: 'Log a mix' });
    await user.type(within(mixForm).getByLabelText('Dry weight (g)'), '10000');
    await user.type(within(mixForm).getByLabelText('Water (g, optional)'), '7500');
    await user.type(within(mixForm).getByLabelText('Initial SG (optional)'), '1.46');
    await user.click(within(mixForm).getByRole('button', { name: 'Log mix' }));

    expect(await screen.findByText(/10000 g dry/)).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Specific gravity trend/ })).toBeInTheDocument();

    const readingForm = screen.getByRole('form', { name: /Add SG reading/ });
    await user.type(within(readingForm).getByLabelText('New SG reading'), '1.52');
    await user.click(within(readingForm).getByRole('button', { name: 'Add reading' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/Specific gravity 1\.52 is 0\.07 above/);
  });

  it('allows staff to promote a shared member recipe into a studio glaze', async () => {
    const studio = aStudio({ myRole: 'STAFF' });
    const sharedMemberGlaze: GlazeRecipe = {
      ...tenmoku,
      id: 'g-shared',
      ownerKind: 'MEMBER',
      ownerAccountId: 'm2',
      ownerName: 'Mo',
      sharedWithStudio: true,
      canEdit: false,
    };
    const promotedStudioGlaze: GlazeRecipe = {
      ...sharedMemberGlaze,
      id: 'g-promoted',
      ownerKind: 'STUDIO',
      sharedWithStudio: false,
      promotedFrom: 'g-shared',
      canEdit: true,
    };
    let promotedCalled = false;

    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/glazes/:glazeId`, ({ params }) =>
        HttpResponse.json(params.glazeId === 'g-promoted' ? promotedStudioGlaze : sharedMemberGlaze),
      ),
      http.post(`${API}/api/v1/studios/:studioId/glazes/:glazeId/promote`, () => {
        promotedCalled = true;
        return HttpResponse.json(promotedStudioGlaze, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/glazes/g-shared`);
    const button = await screen.findByRole('button', { name: 'Promote to studio glaze' });
    await user.click(button);

    expect(await screen.findByRole('button', { name: 'Edit' })).toBeInTheDocument();
    expect(promotedCalled).toBe(true);
  });

  it('imports JSON exports with preview, imports share links, and syncs connected Glazecalc accounts', async () => {
    const studio = aStudio({ myRole: 'MEMBER' });
    const previewRow = {
      index: 1,
      name: 'Floating Blue',
      sourceRef: 'gc-1',
      outcome: 'NEW' as const,
      message: 'Will create a new recipe.',
      warnings: [] as string[],
    };
    const previewResult: GlazeImportResult = {
      format: 'glazecalc-export',
      committed: false,
      createdCount: 1,
      updatedCount: 0,
      unchangedCount: 0,
      errorCount: 0,
      rows: [previewRow],
    };
    const committedResult: GlazeImportResult = {
      ...previewResult,
      committed: true,
      rows: [{ ...previewRow, recipeId: 'g-imported', message: 'Created.' }],
    };

    let synced = false;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/glazes`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/glazes/glazecalc/status`, () =>
        HttpResponse.json({
          configured: true,
          connected: true,
          baseUrl: 'https://glazecalcapp.com',
          remoteAccountLabel: 'mo@glazecalc.example',
          connectedAt: '2026-11-10T12:00:00Z',
        }),
      ),
      http.post(`${API}/api/v1/studios/:studioId/glazes/import`, ({ request }) => {
        const dryRun = new URL(request.url).searchParams.get('dryRun') !== 'false';
        return HttpResponse.json(dryRun ? previewResult : committedResult);
      }),
      http.post(`${API}/api/v1/studios/:studioId/glazes/import-share`, async ({ request }) => {
        const body = (await request.json()) as { dryRun?: boolean };
        return HttpResponse.json(body.dryRun ? previewResult : committedResult);
      }),
      http.post(`${API}/api/v1/studios/:studioId/glazes/glazecalc/sync`, () => {
        synced = true;
        return HttpResponse.json(committedResult);
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/glazes`);
    await user.click(await screen.findByRole('button', { name: 'Import / Glazecalc' }));

    // 1. Upload JSON export -> dry-run preview -> commit
    const exportFile = new File(
      [JSON.stringify({ format: 'glazecalc-export', version: 1, records: { recipes: [] } })],
      'my-glazes.json',
      { type: 'application/json' },
    );
    await user.upload(screen.getByLabelText('Export JSON file'), exportFile);
    expect(await screen.findByText('Floating Blue')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Import 1 recipe' }));
    expect(await screen.findByText('Created.')).toBeInTheDocument();

    // 2. Paste Glazecalc share link -> preview -> import
    await user.type(screen.getByLabelText('Glazecalc share URL or secret'), 'https://glazecalcapp.com/shared#abc123');
    await user.click(screen.getByRole('button', { name: 'Preview share link' }));
    expect(await screen.findByText('Will create a new recipe.')).toBeInTheDocument();

    // 3. Connected Glazecalc OAuth account -> Sync now
    expect(screen.getByText(/Connected to Glazecalc as mo@glazecalc\.example/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Sync from Glazecalc' }));
    expect(synced).toBe(true);
  });
});
