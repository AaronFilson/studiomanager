import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { pickPhoto } from '../photos';
import { aPiece, renderWithQueries, stubApi } from '../test/api';
import { IntakeForm } from './IntakeForm';
import { PieceDetail } from './PieceDetail';
import { PieceList } from './PieceList';

// No camera in tests: stand in for taking and shrinking a photo
jest.mock('../photos', () => ({
  pickPhoto: jest.fn(() =>
    Promise.resolve({ photo: new Blob(['full']), thumbnail: new Blob(['thumb']), previewUri: 'file:///thumb.jpg' }),
  ),
}));

const studioId = 's1';

const photo = {
  id: 'ph1',
  url: 'http://blob.test/full.jpg?sig=r',
  thumbnailUrl: 'http://blob.test/thumb.jpg?sig=r',
  uploadedByAccountId: 'a1',
  uploadedAt: '2026-10-20T15:00:00Z',
};

const upload = {
  photoId: 'ph1',
  photoUploadUrl: 'http://blob.test/full.jpg?sig=w',
  thumbnailUploadUrl: 'http://blob.test/thumb.jpg?sig=w',
  expiresAt: '2026-10-20T15:15:00Z',
  maxPhotoBytes: 8388608,
  maxThumbnailBytes: 524288,
};

describe('pieces', () => {
  it("lists a member's pieces with their stage and pickup date, and opens one", async () => {
    stubApi({
      'GET /api/v1/studios/:studioId/pieces': [
        aPiece({ stage: 'READY', holdUntil: '2026-11-04T05:59:59Z', nextStages: ['PICKED_UP'] }),
        aPiece({ id: 'p2', number: 43, description: 'Bowl', stage: 'UNCLAIMED', overdue: true }),
      ],
    });
    const onOpen = jest.fn();

    await renderWithQueries(<PieceList studioId={studioId} staff={false} onOpen={onOpen} />);

    expect(await screen.findByText('Piece 42: Lidded jar')).toBeOnTheScreen();
    expect(screen.getByText('Ready for pickup')).toBeOnTheScreen();
    expect(screen.getByText(/^Collect by/)).toBeOnTheScreen();
    expect(screen.getByText('Past its pickup date')).toBeOnTheScreen();
    // Members don't get the shelf filters
    expect(screen.queryByRole('button', { name: 'Glazed' })).toBeNull();

    await fireEvent.press(screen.getByRole('button', { name: 'Piece 43' }));
    expect(onOpen).toHaveBeenCalledWith('p2');
  });

  it('lets staff filter the shelf by stage', async () => {
    const calls = stubApi({ 'GET /api/v1/studios/:studioId/pieces': [aPiece()] });

    await renderWithQueries(<PieceList studioId={studioId} staff onOpen={jest.fn()} />);
    expect(await screen.findByText('Mo Maker')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Glazed' }));

    await waitFor(() => expect(calls).toHaveLength(2));
    expect(calls[1]?.path).toBe('/api/v1/studios/s1/pieces');
    expect(calls[1]?.query).toBe('?stage=GLAZED');
  });

  it('lets staff move a piece to its next stage, sending the version they saw', async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/pieces/:pieceId': aPiece(),
      'POST /api/v1/studios/:studioId/pieces/:pieceId/moves': aPiece({ stage: 'BISQUE_FIRED', version: 4 }),
    });

    await renderWithQueries(<PieceDetail studioId={studioId} pieceId="p1" staff />);
    await fireEvent.press(await screen.findByRole('button', { name: 'Move to Bisque fired' }));

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({
          method: 'POST',
          path: '/api/v1/studios/s1/pieces/p1/moves',
          body: { to: 'BISQUE_FIRED', expectedVersion: 3 },
        }),
      ),
    );
  });

  it("doesn't offer members a way to move pieces", async () => {
    stubApi({ 'GET /api/v1/studios/:studioId/pieces/:pieceId': aPiece() });

    await renderWithQueries(<PieceDetail studioId={studioId} pieceId="p1" staff={false} />);

    expect(await screen.findByText('Made by Mo Maker')).toBeOnTheScreen();
    expect(screen.queryByRole('button', { name: /^Move to/ })).toBeNull();
  });

  it('takes in a piece measured in the studio units and shows its number for the label', async () => {
    const calls = stubApi({
      'POST /api/v1/studios/:studioId/pieces': aPiece({ number: 57 }),
    });

    await renderWithQueries(<IntakeForm studioId={studioId} unitSystem="US_CUSTOMARY" staff={false} />);
    await fireEvent.changeText(screen.getByLabelText('What is it?'), ' Teapot ');
    await fireEvent.changeText(screen.getByLabelText('Length'), '8');
    await fireEvent.changeText(screen.getByLabelText('Width'), '6');
    await fireEvent.changeText(screen.getByLabelText('Height'), '7.5');
    await fireEvent.press(screen.getByRole('button', { name: 'Take in piece' }));

    expect(await screen.findByText('Write 57 on its label.')).toBeOnTheScreen();
    expect(calls[0]?.body).toEqual({
      description: 'Teapot',
      size: { dimensions: { length: '8', width: '6', height: '7.5', unit: 'INCH' } },
    });
  });

  it('adds a photo taken at drop-off once the piece is in, uploading straight to storage', async () => {
    const calls = stubApi({
      'POST /api/v1/studios/:studioId/pieces': aPiece({ id: 'p9', number: 57 }),
      'POST /api/v1/studios/:studioId/pieces/:pieceId/photos': upload,
      'PUT /full.jpg': null,
      'PUT /thumb.jpg': null,
      'POST /api/v1/studios/:studioId/pieces/:pieceId/photos/:photoId/complete': aPiece({ photos: [photo] }),
    });

    await renderWithQueries(<IntakeForm studioId={studioId} unitSystem="METRIC" staff={false} />);
    await fireEvent.press(screen.getByRole('button', { name: 'Take a photo' }));
    expect(await screen.findByLabelText('The photo to add')).toBeOnTheScreen();
    expect(pickPhoto).toHaveBeenCalledWith('camera');
    await fireEvent.press(screen.getByRole('button', { name: 'Take in piece' }));

    expect(await screen.findByText('Write 57 on its label.')).toBeOnTheScreen();
    expect(calls.map((c) => `${c.method} ${c.path}`)).toEqual(
      expect.arrayContaining([
        'POST /api/v1/studios/s1/pieces/p9/photos',
        'PUT /full.jpg',
        'PUT /thumb.jpg',
        'POST /api/v1/studios/s1/pieces/p9/photos/ph1/complete',
      ]),
    );
  });

  it("keeps the piece when its photo doesn't upload, and says so", async () => {
    stubApi({
      'POST /api/v1/studios/:studioId/pieces': aPiece({ id: 'p9', number: 57 }),
      'POST /api/v1/studios/:studioId/pieces/:pieceId/photos': () => {
        throw new Error('offline');
      },
    });

    await renderWithQueries(<IntakeForm studioId={studioId} unitSystem="METRIC" staff={false} />);
    await fireEvent.press(screen.getByRole('button', { name: 'Take a photo' }));
    await screen.findByLabelText('The photo to add');
    await fireEvent.press(screen.getByRole('button', { name: 'Take in piece' }));

    expect(await screen.findByText('Write 57 on its label.')).toBeOnTheScreen();
    expect(screen.getByText(/its photo didn't upload/)).toBeOnTheScreen();
  });

  it("shows a piece's photos, with a thumbnail on the shelf, and removes one", async () => {
    const calls = stubApi({
      'GET /api/v1/studios/:studioId/pieces/:pieceId': aPiece({ photos: [photo] }),
      'DELETE /api/v1/studios/:studioId/pieces/:pieceId/photos/:photoId': null,
    });

    await renderWithQueries(<PieceDetail studioId={studioId} pieceId="p1" staff={false} />);
    expect(await screen.findByLabelText('Photo 1 of piece 42')).toBeOnTheScreen();
    await fireEvent.press(screen.getByRole('button', { name: 'Remove photo 1' }));

    await waitFor(() =>
      expect(calls).toContainEqual(
        expect.objectContaining({ method: 'DELETE', path: '/api/v1/studios/s1/pieces/p1/photos/ph1' }),
      ),
    );
  });

  it('shows the first photo as a thumbnail on the shelf', async () => {
    stubApi({ 'GET /api/v1/studios/:studioId/pieces': [aPiece({ photos: [photo] })] });

    await renderWithQueries(<PieceList studioId={studioId} staff onOpen={jest.fn()} />);

    expect(await screen.findByTestId('thumbnail')).toHaveProp('source', { uri: photo.thumbnailUrl });
  });

  it('holds back a half-entered size', async () => {
    stubApi({});

    await renderWithQueries(<IntakeForm studioId={studioId} unitSystem="METRIC" staff={false} />);
    await fireEvent.changeText(screen.getByLabelText('Length'), '20');

    expect(screen.getByText('Enter all three measurements as numbers, or none.')).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Take in piece' })).toBeDisabled();
  });
});
