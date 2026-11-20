import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { renderWithQueries, stubApi } from '../test/api';
import { GlazeDetail } from './GlazeDetail';
import { GlazeList } from './GlazeList';

const studioGlaze = {
  id: 'g1',
  ownerKind: 'STUDIO' as const,
  name: 'Floating Blue',
  notes: 'Dip for 3 seconds.',
  cone: '6',
  atmosphere: 'OXIDATION' as const,
  foodSurface: true,
  targetSpecificGravity: '1.46',
  sharedWithStudio: false,
  source: 'MANUAL' as const,
  lines: [
    { kind: 'BASE' as const, material: 'Nepheline Syenite', amount: '40', unit: 'PARTS' as const },
    { kind: 'BASE' as const, material: 'Gerstley Borate', amount: '25', unit: 'PARTS' as const },
    { kind: 'BASE' as const, material: 'EPK', amount: '15', unit: 'PARTS' as const },
    { kind: 'BASE' as const, material: 'Silica', amount: '20', unit: 'PARTS' as const },
    { kind: 'ADDITION' as const, material: 'Cobalt Carbonate', amount: '1', unit: 'PERCENT' as const },
  ],
  photos: [],
  canEdit: false,
  version: 1,
  updatedAt: '2026-10-10T00:00:00Z',
};

describe('glazes', () => {
  it('switches glaze tabs and opens a glaze recipe', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/glazes': (call) =>
        call.query === '?owner=MINE'
          ? [{ ...studioGlaze, id: 'g2', ownerKind: 'MEMBER', ownerName: 'Mo Maker', name: 'My Celadon', sharedWithStudio: true }]
          : [studioGlaze],
    });
    const onOpen = jest.fn();

    await renderWithQueries(<GlazeList studioId="s1" onOpen={onOpen} />);
    expect(await screen.findByText('Floating Blue')).toBeOnTheScreen();
    expect(screen.getByText('Cone 6')).toBeOnTheScreen();
    expect(screen.getByText('SG 1.46')).toBeOnTheScreen();

    await fireEvent.press(screen.getByRole('button', { name: 'My recipes' }));
    expect(await screen.findByText('My Celadon')).toBeOnTheScreen();
    expect(calls[1]?.query).toBe('?owner=MINE');

    await fireEvent.press(screen.getByRole('button', { name: 'My Celadon' }));
    expect(onOpen).toHaveBeenCalledWith('g2');
  });

  it('shows full recipe details, UMF chemistry, slurry SG helper, batch weights, and studio mix log', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'OWNER',
        roleName: 'Owner',
        permissions: ['glazes.read', 'glazes.studio.edit'],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/glazes/:glazeId': studioGlaze,
      'GET /api/v1/studios/:studioId/glazes/:glazeId/mixes': [
        {
          id: 'mix1',
          dryWeightG: '5000',
          waterG: '3800',
          mixedBy: 'u1',
          mixedByName: 'Sam Staff',
          mixedAt: '2026-10-11T10:00:00Z',
          readings: [
            {
              id: 'r1',
              specificGravity: '1.52',
              readBy: 'u1',
              readAt: '2026-10-11T10:05:00Z',
              offTarget: true,
              warning: 'SG 1.52 is 0.06 above target 1.46',
            },
          ],
        },
      ],
      'POST /api/v1/studios/:studioId/glazes/:glazeId/mixes': {
        id: 'mix2',
        dryWeightG: '10000',
        waterG: '7500',
        mixedBy: 'u1',
        mixedAt: '2026-10-12T10:00:00Z',
        readings: [],
      },
      'POST /api/v1/studios/:studioId/glazes/:glazeId/mixes/:mixId/readings': {
        id: 'r2',
        specificGravity: '1.46',
        readBy: 'u1',
        readAt: '2026-10-12T10:05:00Z',
        offTarget: false,
      },
      'GET /api/v1/studios/:studioId/glazes/:glazeId/batch': {
        size: '500',
        unit: 'GRAM',
        lines: [
          { kind: 'BASE', material: 'Nepheline Syenite', weight: '200.0' },
          { kind: 'ADDITION', material: 'Cobalt Carbonate', weight: '5.0' },
        ],
        total: '505.0',
      },
    });

    await renderWithQueries(<GlazeDetail studioId="s1" glazeId="g1" />);
    expect(await screen.findByText('Floating Blue')).toBeOnTheScreen();
    expect(screen.getByText('Dip for 3 seconds.')).toBeOnTheScreen();
    expect(screen.getByText('40 parts')).toBeOnTheScreen();
    expect(screen.getByText('1 %')).toBeOnTheScreen();

    // UMF chemistry summary renders offline from @studiomanager/glaze-chemistry
    expect(screen.getByText('R2O : RO fluxes')).toBeOnTheScreen();
    expect(screen.getByText('SiO2 : Al2O3')).toBeOnTheScreen();

    // Existing studio mix log and off-target SG warning appear
    expect(await screen.findByText('SG 1.52 is 0.06 above target 1.46')).toBeOnTheScreen();

    // Slurry SG helper computes water addition when thick
    await fireEvent.changeText(screen.getByLabelText('Measured SG'), '1.52');
    expect(await screen.findByText(/Too thick: add/)).toBeOnTheScreen();

    // Batch calculator scales dry weights
    await fireEvent.press(screen.getByRole('button', { name: 'g' }));
    await fireEvent.changeText(screen.getByLabelText('Dry batch size'), '500');

    expect(await screen.findByText('505.0 g')).toBeOnTheScreen();
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'GET',
          path: '/api/v1/studios/s1/glazes/g1/batch',
          query: '?size=500&unit=GRAM',
        }),
      ),
    );

    // Staff can log a new studio mix and record an SG reading
    await fireEvent.changeText(screen.getByLabelText('Dry weight (g)'), '10000');
    await fireEvent.changeText(screen.getByLabelText('Water (g, optional)'), '7500');
    await fireEvent.press(screen.getByRole('button', { name: 'Log mix' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/glazes/g1/mixes',
          body: { dryWeightG: '10000', waterG: '7500' },
        }),
      ),
    );

    await fireEvent.changeText(screen.getByLabelText('New SG reading for mix mix1'), '1.46');
    await fireEvent.press(screen.getByRole('button', { name: 'Add reading' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/glazes/g1/mixes/mix1/readings',
          body: { specificGravity: '1.46' },
        }),
      ),
    );
  });
});
