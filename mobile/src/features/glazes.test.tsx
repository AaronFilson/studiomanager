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
    { kind: 'ADDITION' as const, material: 'Cobalt Carbonate', amount: '1', unit: 'PERCENT' as const },
  ],
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

  it('shows full recipe details and calculates scaled dry batch weights', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/glazes/:glazeId': studioGlaze,
      'GET /api/v1/studios/:studioId/glazes/:glazeId/batch': {
        size: '500',
        unit: 'GRAM',
        lines: [
          { kind: 'BASE', material: 'Nepheline Syenite', weight: '500.0' },
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
  });
});
