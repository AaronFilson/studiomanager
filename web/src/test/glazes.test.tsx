import type { GlazeRecipe } from '@studiomanager/api-client';
import { screen } from '@testing-library/react';
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
});
