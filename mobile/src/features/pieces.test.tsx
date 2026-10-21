import { fireEvent, screen, waitFor } from '@testing-library/react-native';

import { aPiece, renderWithQueries, stubApi } from '../test/api';
import { IntakeForm } from './IntakeForm';
import { PieceDetail } from './PieceDetail';
import { PieceList } from './PieceList';

const studioId = 's1';

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

  it('holds back a half-entered size', async () => {
    stubApi({});

    await renderWithQueries(<IntakeForm studioId={studioId} unitSystem="METRIC" staff={false} />);
    await fireEvent.changeText(screen.getByLabelText('Length'), '20');

    expect(screen.getByText('Enter all three measurements as numbers, or none.')).toBeOnTheScreen();
    expect(screen.getByRole('button', { name: 'Take in piece' })).toBeDisabled();
  });
});
