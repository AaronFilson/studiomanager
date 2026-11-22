import type { MaintenanceRecord } from '@studiomanager/api-client';
import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { pickPhoto } from '../photos';
import { capped, renderWithQueries, stubApi } from '../test/api';
import { MaintenanceStaff } from './MaintenanceStaff';

jest.mock('../photos', () => ({
  pickPhoto: jest.fn(() =>
    Promise.resolve({ photo: new Blob(['full']), thumbnail: new Blob(['thumb']), previewUri: 'file:///thumb.jpg' }),
  ),
}));

const staffPermissions = {
  role: 'STAFF',
  roleName: 'Staff',
  permissions: ['maintenance.manage', 'kilns.read', 'kilns.manage'],
  duties: [],
};

const wheelAsset = {
  id: 'a2',
  kind: 'WHEEL' as const,
  name: 'Wheel 3',
  location: 'North wall',
  available: true,
  version: 4,
  tasks: [
    {
      id: 't1',
      assetId: 'a2',
      assetName: 'Wheel 3',
      title: 'Check belt tension',
      checklist: [],
      everyDays: 30,
      lastDoneAt: '2026-10-01T00:00:00Z',
      status: 'DUE' as const,
      version: 0,
    },
  ],
};

const existingPhoto = {
  id: 'ph1',
  url: 'http://blob.test/rec-full-1.jpg?sig=r',
  thumbnailUrl: 'http://blob.test/rec-thumb-1.jpg?sig=r',
  uploadedByAccountId: 'u1',
  uploadedAt: '2026-11-18T10:00:00Z',
};

const uploadedPhoto = {
  id: 'ph2',
  url: 'http://blob.test/rec-full-2.jpg?sig=r',
  thumbnailUrl: 'http://blob.test/rec-thumb-2.jpg?sig=r',
  uploadedByAccountId: 'u1',
  uploadedAt: '2026-11-21T10:00:00Z',
};

const uploadUrls = {
  photoId: 'ph2',
  photoUploadUrl: 'http://blob.test/rec-full-2.jpg?sig=w',
  thumbnailUploadUrl: 'http://blob.test/rec-thumb-2.jpg?sig=w',
  expiresAt: '2026-11-21T10:15:00Z',
  maxPhotoBytes: 8388608,
  maxThumbnailBytes: 524288,
};

const baseRecord: MaintenanceRecord = {
  id: 'rec1',
  assetId: 'a2',
  taskId: 't1',
  taskTitle: 'Check belt tension',
  note: 'Tightened motor mount bolts',
  cost: '24.50',
  checked: ['Inspect belt', 'Test foot pedal'],
  doneBy: 'u1',
  doneByName: 'Sam Staff',
  doneAt: '2026-11-18T09:30:00Z',
  parts: [],
  photos: [existingPhoto],
};

describe('MaintenanceStaff', () => {
  beforeEach(() => {
    jest.mocked(pickPhoto).mockReset();
    jest.mocked(pickPhoto).mockResolvedValue({
      photo: new Blob(['full']),
      thumbnail: new Blob(['thumb']),
      previewUri: 'file:///thumb.jpg',
    });
  });

  it('resolves open problem reports and toggles non-kiln asset availability without eagerly fetching history', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': staffPermissions,
      'GET /api/v1/studios/:studioId/maintenance/assets': [
        {
          id: 'a1',
          kind: 'KILN',
          name: 'Skutt 1027',
          kilnId: 'k1',
          available: true,
          totalFirings: 45,
          version: 1,
          tasks: [],
        },
        wheelAsset,
      ],
      'GET /api/v1/studios/:studioId/maintenance/problem-reports': capped([
        {
          id: 'pr1',
          assetId: 'a2',
          assetName: 'Wheel 3',
          description: 'Pedal sticks at low speed',
          reportedBy: 'm1',
          reportedByName: 'Mo Maker',
          reportedAt: '2026-11-20T09:00:00Z',
          open: true,
          version: 2,
        },
      ]),
      'POST /api/v1/studios/:studioId/maintenance/problem-reports/:reportId/resolution': {
        id: 'pr1',
        assetId: 'a2',
        assetName: 'Wheel 3',
        description: 'Pedal sticks at low speed',
        reportedBy: 'm1',
        reportedAt: '2026-11-20T09:00:00Z',
        open: false,
        resolution: 'Cleaned potentiometer',
        version: 3,
      },
      'PUT /api/v1/studios/:studioId/maintenance/assets/:assetId/availability': {
        id: 'a2',
        kind: 'WHEEL',
        name: 'Wheel 3',
        available: false,
        downReason: 'Waiting on replacement belt',
        version: 5,
        tasks: [],
      },
    });
    const onOpenKiln = jest.fn();

    await renderWithQueries(<MaintenanceStaff studioId="s1" onOpenKiln={onOpenKiln} />);

    expect(await screen.findByText('Pedal sticks at low speed')).toBeOnTheScreen();
    expect(screen.getByText('Check belt tension')).toBeOnTheScreen();
    // Maintenance history is fetched lazily only when expanded
    expect(calls.some((c) => c.path.includes('/records'))).toBe(false);

    // Resolve open problem report
    await fireEvent.press(screen.getByRole('button', { name: 'Resolve' }));
    await fireEvent.changeText(screen.getByLabelText('What was done? (optional)'), 'Cleaned potentiometer');
    await fireEvent.press(screen.getByRole('button', { name: 'Confirm resolve' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/maintenance/problem-reports/pr1/resolution',
          body: { expectedVersion: 2, resolution: 'Cleaned potentiometer' },
        }),
      ),
    );

    // Take Wheel 3 out of service
    await fireEvent.press(screen.getByRole('button', { name: 'Take out of service' }));
    await fireEvent.changeText(screen.getByLabelText('Reason Wheel 3 is out of service'), 'Waiting on replacement belt');
    await fireEvent.press(screen.getByRole('button', { name: 'Confirm out of service' }));
    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'PUT',
          path: '/api/v1/studios/s1/maintenance/assets/a2/availability',
          body: { downReason: 'Waiting on replacement belt', expectedVersion: 4 },
        }),
      ),
    );

    // Kiln links out to kiln page rather than asset availability
    await fireEvent.press(screen.getByRole('button', { name: 'Open kiln' }));
    expect(onOpenKiln).toHaveBeenCalledWith('k1');
  });

  it('expands maintenance history lazily, displays record details, and uploads record photos', async () => {
    let records: MaintenanceRecord[] = [baseRecord];
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': staffPermissions,
      'GET /api/v1/studios/:studioId/maintenance/assets': [wheelAsset],
      'GET /api/v1/studios/:studioId/maintenance/problem-reports': capped([]),
      'GET /api/v1/studios/:studioId': { id: 's1', name: 'Clay Co', currency: 'USD', unitSystem: 'METRIC' },
      'GET /api/v1/studios/:studioId/maintenance/assets/:assetId/records': () => capped(records),
      'POST /api/v1/studios/:studioId/maintenance/records/:recordId/photos': uploadUrls,
      'PUT /rec-full-2.jpg': null,
      'PUT /rec-thumb-2.jpg': null,
      'POST /api/v1/studios/:studioId/maintenance/records/:recordId/photos/:photoId/complete': () => {
        records = [{ ...baseRecord, photos: [existingPhoto, uploadedPhoto] }];
        return null;
      },
    });

    await renderWithQueries(<MaintenanceStaff studioId="s1" />);
    expect(await screen.findByText('Wheel 3')).toBeOnTheScreen();
    expect(calls.some((c) => c.path.endsWith('/records'))).toBe(false);

    await fireEvent.press(screen.getByRole('button', { name: 'History & photos' }));
    expect(await screen.findByText('Tightened motor mount bolts')).toBeOnTheScreen();
    expect(screen.getByText(/Sam Staff/)).toBeOnTheScreen();
    expect(screen.getByText(/\$24\.50/)).toBeOnTheScreen();
    expect(screen.getByText('2 items checked')).toBeOnTheScreen();
    expect(screen.getByLabelText('Photo 1 of Check belt tension')).toBeOnTheScreen();

    // Cancelled photo pick does not call the upload API
    jest.mocked(pickPhoto).mockResolvedValueOnce(null);
    await fireEvent.press(screen.getByRole('button', { name: 'Choose a photo' }));
    await waitFor(() => expect(pickPhoto).toHaveBeenCalledWith('library'));
    expect(calls.some((c) => c.path.includes('/records/rec1/photos'))).toBe(false);

    // Taking a photo uploads both blobs, completes upload, and refreshes records
    await fireEvent.press(screen.getByRole('button', { name: 'Take a photo' }));
    expect(await screen.findByLabelText('Photo 2 of Check belt tension')).toBeOnTheScreen();
    expect(pickPhoto).toHaveBeenCalledWith('camera');
    expect(calls.map((c) => `${c.method} ${c.path}`)).toEqual(
      expect.arrayContaining([
        'POST /api/v1/studios/s1/maintenance/records/rec1/photos',
        'PUT /rec-full-2.jpg',
        'PUT /rec-thumb-2.jpg',
        'POST /api/v1/studios/s1/maintenance/records/rec1/photos/ph2/complete',
      ]),
    );
  });

  it('shows photo upload errors and hides upload buttons once a record reaches 4 photos', async () => {
    const fourPhotos = [1, 2, 3, 4].map((n) => ({
      ...existingPhoto,
      id: `ph-${String(n)}`,
      thumbnailUrl: `http://blob.test/t-${String(n)}.jpg`,
    }));

    stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': staffPermissions,
      'GET /api/v1/studios/:studioId/maintenance/assets': [wheelAsset],
      'GET /api/v1/studios/:studioId/maintenance/problem-reports': capped([]),
      'GET /api/v1/studios/:studioId/maintenance/assets/:assetId/records': capped([
        { ...baseRecord, id: 'rec-full', cost: undefined, photos: fourPhotos },
        { ...baseRecord, id: 'rec-open', taskTitle: undefined, note: 'Loose screw', cost: undefined, photos: [] },
      ]),
      'POST /api/v1/studios/:studioId/maintenance/records/:recordId/photos': () => {
        throw new Error('Upload failed');
      },
    });

    await renderWithQueries(<MaintenanceStaff studioId="s1" />);
    await fireEvent.press(await screen.findByRole('button', { name: 'History & photos' }));
    expect(await screen.findByText('Loose screw')).toBeOnTheScreen();

    // Only the open record (0 photos) shows camera/library buttons; the 4-photo record does not
    expect(screen.getAllByRole('button', { name: 'Take a photo' })).toHaveLength(1);
    await fireEvent.press(screen.getByRole('button', { name: 'Take a photo' }));
    expect(await screen.findByText('Upload failed')).toBeOnTheScreen();
  });

  it('logs a repair from the asset card and reveals it in history', async () => {
    let records: MaintenanceRecord[] = [];
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/me/permissions': staffPermissions,
      'GET /api/v1/studios/:studioId/maintenance/assets': [wheelAsset],
      'GET /api/v1/studios/:studioId/maintenance/problem-reports': capped([]),
      'GET /api/v1/studios/:studioId': { id: 's1', name: 'Clay Co', currency: 'USD', unitSystem: 'METRIC' },
      'GET /api/v1/studios/:studioId/maintenance/assets/:assetId/records': () => capped(records),
      'POST /api/v1/studios/:studioId/maintenance/assets/:assetId/records': (call) => {
        const created: MaintenanceRecord = {
          id: 'rec-new',
          assetId: 'a2',
          note: (call.body as { note: string }).note,
          cost: (call.body as { cost?: string }).cost,
          checked: [],
          doneBy: 'u1',
          doneByName: 'Sam Staff',
          doneAt: '2026-11-21T12:00:00Z',
          parts: [],
          photos: [],
        };
        records = [created];
        return created;
      },
    });

    await renderWithQueries(<MaintenanceStaff studioId="s1" />);
    await fireEvent.press(await screen.findByRole('button', { name: 'Log a repair' }));

    await fireEvent.changeText(screen.getByLabelText('Repair or other work'), 'Replaced drive belt');
    await fireEvent.changeText(screen.getByLabelText('Cost (optional)'), 'not-valid');
    expect(screen.getByRole('button', { name: 'Save repair' })).toBeDisabled();

    await fireEvent.changeText(screen.getByLabelText('Cost (optional)'), '18.75');
    await fireEvent.press(screen.getByRole('button', { name: 'Save repair' }));

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/maintenance/assets/a2/records',
          body: { note: 'Replaced drive belt', cost: '18.75' },
        }),
      ),
    );
    // History auto-expands with the new repair and photo buttons ready
    expect(await screen.findByText('Replaced drive belt')).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Take a photo' })).toBeOnTheScreen();
  });
});
