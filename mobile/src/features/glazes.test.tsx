import type { GlazeRecipe } from '@studiomanager/api-client';
import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { pickPhoto } from '../photos';
import { renderWithQueries, stubApi } from '../test/api';
import { GlazeDetail } from './GlazeDetail';
import { GlazeList } from './GlazeList';

jest.mock('../photos', () => ({
  pickPhoto: jest.fn(() =>
    Promise.resolve({ photo: new Blob(['full']), thumbnail: new Blob(['thumb']), previewUri: 'file:///thumb.jpg' }),
  ),
}));

const studioGlaze: GlazeRecipe = {
  id: 'g1',
  ownerKind: 'STUDIO',
  name: 'Floating Blue',
  notes: 'Dip for 3 seconds.',
  cone: '6',
  atmosphere: 'OXIDATION',
  foodSurface: true,
  targetSpecificGravity: '1.46',
  sharedWithStudio: false,
  source: 'MANUAL',
  lines: [
    { kind: 'BASE', material: 'Nepheline Syenite', amount: '40', unit: 'PARTS' },
    { kind: 'BASE', material: 'Gerstley Borate', amount: '25', unit: 'PARTS' },
    { kind: 'BASE', material: 'EPK', amount: '15', unit: 'PARTS' },
    { kind: 'BASE', material: 'Silica', amount: '20', unit: 'PARTS' },
    { kind: 'ADDITION', material: 'Cobalt Carbonate', amount: '1', unit: 'PERCENT' },
  ],
  photos: [],
  canEdit: false,
  version: 1,
  updatedAt: '2026-10-10T00:00:00Z',
};

const tilePhoto1 = {
  id: 'ph1',
  url: 'http://blob.test/tile-1.jpg?sig=r',
  thumbnailUrl: 'http://blob.test/tile-1-thumb.jpg?sig=r',
  uploadedByAccountId: 'u1',
  uploadedAt: '2026-10-12T12:00:00Z',
};

const tilePhoto2 = {
  id: 'ph2',
  url: 'http://blob.test/tile-2.jpg?sig=r',
  thumbnailUrl: 'http://blob.test/tile-2-thumb.jpg?sig=r',
  uploadedByAccountId: 'u1',
  uploadedAt: '2026-10-13T12:00:00Z',
};

const uploadUrls = {
  photoId: 'ph2',
  photoUploadUrl: 'http://blob.test/tile-2.jpg?sig=w',
  thumbnailUploadUrl: 'http://blob.test/tile-2-thumb.jpg?sig=w',
  expiresAt: '2026-10-13T12:15:00Z',
  maxPhotoBytes: 8388608,
  maxThumbnailBytes: 524288,
};

describe('glazes', () => {
  beforeEach(() => {
    jest.mocked(pickPhoto).mockReset();
    jest.mocked(pickPhoto).mockResolvedValue({
      photo: new Blob(['full']),
      thumbnail: new Blob(['thumb']),
      previewUri: 'file:///thumb.jpg',
    });
  });

  it('switches glaze tabs, gates + New glaze by studio edit permission, and opens a glaze recipe', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'MEMBER',
        roleName: 'Member',
        permissions: [],
        duties: [],
      },
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
    // Regular member cannot add studio glazes on STUDIO tab
    expect(screen.queryByRole('button', { name: '+ New glaze' })).toBeNull();

    // Regular member CAN add personal glazes on MINE tab
    await fireEvent.press(screen.getByRole('button', { name: 'My recipes' }));
    expect(await screen.findByText('My Celadon')).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: '+ New glaze' })).toBeOnTheScreen();
    expect(calls.filter((c) => c.path === '/api/v1/studios/s1/glazes').map((c) => c.query)).toEqual([
      '?owner=STUDIO',
      '?owner=MINE',
    ]);

    // SHARED tab never offers + New glaze
    await fireEvent.press(screen.getByRole('button', { name: 'Shared' }));
    await waitFor(() => expect(screen.queryByRole('button', { name: '+ New glaze' })).toBeNull());

    await fireEvent.press(screen.getByRole('button', { name: 'My recipes' }));
    await fireEvent.press(await screen.findByRole('button', { name: 'My Celadon' }));
    expect(onOpen).toHaveBeenCalledWith('g2');
  });

  it('creates a personal glaze with dynamic recipe lines and opens it on success', async () => {
    const createdGlaze: GlazeRecipe = {
      ...studioGlaze,
      id: 'g-new',
      ownerKind: 'MEMBER',
      name: 'Kaki Iron',
      cone: '10',
      atmosphere: 'REDUCTION',
      targetSpecificGravity: '1.48',
      foodSurface: true,
      sharedWithStudio: true,
      notes: 'Thin dip on porcelain.',
      canEdit: true,
    };
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'MEMBER',
        roleName: 'Member',
        permissions: [],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/glazes': [],
      'POST /api/v1/studios/:studioId/glazes': createdGlaze,
    });
    const onOpen = jest.fn();

    await renderWithQueries(<GlazeList studioId="s1" onOpen={onOpen} />);
    await fireEvent.press(screen.getByRole('button', { name: 'My recipes' }));
    await fireEvent.press(await screen.findByRole('button', { name: '+ New glaze' }));

    await fireEvent.changeText(screen.getByLabelText('Name'), ' Kaki Iron ');
    await fireEvent.changeText(screen.getByLabelText('Cone'), '10');
    await fireEvent.press(screen.getByRole('button', { name: 'Reduction' }));
    await fireEvent.changeText(screen.getByLabelText('Target specific gravity'), '1.48');
    await fireEvent.press(screen.getByRole('checkbox', { name: 'Meant for food surfaces' }));
    await fireEvent.press(screen.getByRole('checkbox', { name: 'Share with the studio' }));

    // Fill line 1 (BASE, PARTS)
    await fireEvent.changeText(screen.getByLabelText('Material 1'), 'Custer Feldspar');
    await fireEvent.changeText(screen.getByLabelText('Amount 1'), '45');

    // Add line 2 (ADDITION, PERCENT) and line 3 (then remove line 3)
    await fireEvent.press(screen.getByRole('button', { name: 'Add a line' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Addition for line 2' }));
    await fireEvent.changeText(screen.getByLabelText('Material 2'), 'Red Iron Oxide');
    await fireEvent.changeText(screen.getByLabelText('Amount 2'), '8');

    await fireEvent.press(screen.getByRole('button', { name: 'Add a line' }));
    await fireEvent.changeText(screen.getByLabelText('Material 3'), 'Temp Material');
    await fireEvent.press(screen.getByRole('button', { name: 'Remove line 3' }));
    expect(screen.queryByLabelText('Material 3')).toBeNull();

    await fireEvent.changeText(screen.getByLabelText('Notes'), 'Thin dip on porcelain.');
    await fireEvent.press(screen.getByRole('button', { name: 'Save glaze' }));

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/glazes',
          body: {
            ownerKind: 'MEMBER',
            name: 'Kaki Iron',
            cone: '10',
            atmosphere: 'REDUCTION',
            targetSpecificGravity: '1.48',
            foodSurface: true,
            sharedWithStudio: true,
            notes: 'Thin dip on porcelain.',
            lines: [
              { kind: 'BASE', material: 'Custer Feldspar', amount: '45', unit: 'PARTS' },
              { kind: 'ADDITION', material: 'Red Iron Oxide', amount: '8', unit: 'PERCENT' },
            ],
          },
        }),
      ),
    );
    await waitFor(() => expect(onOpen).toHaveBeenCalledWith('g-new'));
  });

  it('lets staff with glazes.studio.edit create a studio glaze without a share toggle', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'STAFF',
        roleName: 'Staff',
        permissions: ['glazes.studio.edit'],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/glazes': [studioGlaze],
      'POST /api/v1/studios/:studioId/glazes': { ...studioGlaze, id: 'g-studio-new', name: 'Clear Liner' },
    });
    const onOpen = jest.fn();

    await renderWithQueries(<GlazeList studioId="s1" onOpen={onOpen} />);
    await fireEvent.press(await screen.findByRole('button', { name: '+ New glaze' }));
    expect(screen.queryByRole('checkbox', { name: 'Share with the studio' })).toBeNull();

    await fireEvent.changeText(screen.getByLabelText('Name'), 'Clear Liner');
    await fireEvent.changeText(screen.getByLabelText('Material 1'), 'Silica');
    await fireEvent.changeText(screen.getByLabelText('Amount 1'), '30');
    await fireEvent.press(screen.getByRole('button', { name: 'Save glaze' }));

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/glazes',
          body: {
            ownerKind: 'STUDIO',
            name: 'Clear Liner',
            foodSurface: false,
            lines: [{ kind: 'BASE', material: 'Silica', amount: '30', unit: 'PARTS' }],
          },
        }),
      ),
    );
    await waitFor(() => expect(onOpen).toHaveBeenCalledWith('g-studio-new'));
  });

  it('shows full recipe details, UMF chemistry, slurry SG helper, batch weights, and studio mix log, hiding edit controls when canEdit is false', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'OWNER',
        roleName: 'Owner',
        permissions: ['glazes.read', 'glazes.studio.edit'],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/glazes/:glazeId': { ...studioGlaze, photos: [tilePhoto1], canEdit: false },
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

    // Test-tile photo thumbnail is visible, but edit/photo mutation actions are hidden when canEdit is false
    expect(screen.getByLabelText('Test tile 1 of Floating Blue')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: 'Edit recipe' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Take a photo' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Choose a photo' })).toBeNull();
    expect(screen.queryByRole('button', { name: 'Remove photo 1' })).toBeNull();

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

    // Staff can log a new studio mix and record an SG reading (clean visible labels without mix UUID)
    expect(screen.getByText('New SG reading')).toBeOnTheScreen();
    expect(screen.queryByText('New SG reading for mix mix1')).toBeNull();
    await fireEvent.changeText(screen.getByLabelText('Dry weight (g)'), '10000');
    await fireEvent.changeText(screen.getByLabelText('Water (g, optional)'), 'not-a-number');
    expect(screen.getByRole('button', { name: 'Log mix' })).toBeDisabled();
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

  it('edits a glaze recipe with optimistic version and clears stale imported chemistry on line changes', async () => {
    const editableMemberGlaze: GlazeRecipe = {
      ...studioGlaze,
      id: 'g-own',
      ownerKind: 'MEMBER',
      ownerName: 'Mo Maker',
      sharedWithStudio: false,
      canEdit: true,
      version: 3,
      lines: [
        {
          kind: 'BASE',
          material: 'Imported Spar',
          amount: '40',
          amountText: '40.0',
          unit: 'PARTS',
          chemistry: { analysis: { SiO2: 65, Al2O3: 18, K2O: 12 }, loi: 0.5 },
        },
      ],
    };
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'MEMBER',
        roleName: 'Member',
        permissions: [],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/glazes/:glazeId': editableMemberGlaze,
      'PUT /api/v1/studios/:studioId/glazes/:glazeId': (call) => ({
        ...editableMemberGlaze,
        ...(call.body as Partial<GlazeRecipe>),
        version: 4,
      }),
    });

    await renderWithQueries(<GlazeDetail studioId="s1" glazeId="g-own" />);
    await fireEvent.press(await screen.findByRole('button', { name: 'Edit recipe' }));

    await fireEvent.changeText(screen.getByLabelText('Name'), 'Floating Blue v2');
    await fireEvent.changeText(screen.getByLabelText('Material 1'), 'Custer Feldspar');
    await fireEvent.changeText(screen.getByLabelText('Amount 1'), '45');
    await fireEvent.press(screen.getByRole('checkbox', { name: 'Share with the studio' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/glazes/g-own',
          body: {
            name: 'Floating Blue v2',
            cone: '6',
            atmosphere: 'OXIDATION',
            targetSpecificGravity: '1.46',
            foodSurface: true,
            sharedWithStudio: true,
            notes: 'Dip for 3 seconds.',
            lines: [{ kind: 'BASE', material: 'Custer Feldspar', amount: '45', unit: 'PARTS' }],
            version: 3,
          },
        }),
      ),
    );
    expect(await screen.findByText('Floating Blue v2')).toBeOnTheScreen();
    expect(screen.getByText('Custer Feldspar')).toBeOnTheScreen();
  });

  it('uploads and removes test-tile photos on an editable glaze recipe', async () => {
    let currentGlaze: GlazeRecipe = {
      ...studioGlaze,
      ownerKind: 'MEMBER',
      ownerName: 'Mo Maker',
      photos: [tilePhoto1],
      canEdit: true,
    };
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': {
        role: 'MEMBER',
        roleName: 'Member',
        permissions: [],
        duties: [],
      },
      'GET /api/v1/studios/:studioId/glazes/:glazeId': () => currentGlaze,
      'POST /api/v1/studios/:studioId/glazes/:glazeId/photos': uploadUrls,
      'PUT /tile-2.jpg': null,
      'PUT /tile-2-thumb.jpg': null,
      'POST /api/v1/studios/:studioId/glazes/:glazeId/photos/:photoId/complete': () => {
        currentGlaze = { ...currentGlaze, photos: [tilePhoto1, tilePhoto2] };
        return currentGlaze;
      },
      'DELETE /api/v1/studios/:studioId/glazes/:glazeId/photos/:photoId': () => {
        currentGlaze = { ...currentGlaze, photos: [tilePhoto2] };
        return null;
      },
    });

    await renderWithQueries(<GlazeDetail studioId="s1" glazeId="g1" />);
    expect(await screen.findByLabelText('Test tile 1 of Floating Blue')).toBeOnTheScreen();

    // Upload a second test-tile photo
    await fireEvent.press(screen.getByRole('button', { name: 'Take a photo' }));
    expect(await screen.findByLabelText('Test tile 2 of Floating Blue')).toBeOnTheScreen();
    expect(pickPhoto).toHaveBeenCalledWith('camera');
    expect(calls.map((c) => `${c.method} ${c.path}`)).toEqual(
      expect.arrayContaining([
        'POST /api/v1/studios/s1/glazes/g1/photos',
        'PUT /tile-2.jpg',
        'PUT /tile-2-thumb.jpg',
        'POST /api/v1/studios/s1/glazes/g1/photos/ph2/complete',
      ]),
    );

    // Remove the first test-tile photo
    await fireEvent.press(screen.getByRole('button', { name: 'Remove photo 1' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'DELETE',
          path: '/api/v1/studios/s1/glazes/g1/photos/ph1',
        }),
      ),
    );
    await waitFor(() => expect(screen.queryByLabelText('Test tile 2 of Floating Blue')).toBeNull());
  });
});
