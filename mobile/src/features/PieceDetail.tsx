import { getPieceOptions, movePieceMutation, type Piece } from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ScrollView, StyleSheet, View } from 'react-native';

import { formatDate, formatDateTime, formatMoney, stageLabel } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Loading, Title } from '../ui';
import { PiecePhotos } from './PiecePhotos';

export function PieceDetail({ studioId, pieceId, staff }: { studioId: string; pieceId: string; staff: boolean }) {
  const piece = useQuery(getPieceOptions({ path: { studioId, pieceId } }));

  if (piece.isPending) return <Loading />;
  if (piece.isError) return <ErrorText error={piece.error} />;
  return (
    <ScrollView contentContainerStyle={styles.content}>
      <Title>
        Piece {piece.data.number}
        {piece.data.description ? `: ${piece.data.description}` : ''}
      </Title>
      <View style={styles.badges}>
        <Badge>{stageLabel(piece.data.stage)}</Badge>
        {piece.data.overdue && <Badge warning>Past its pickup date</Badge>}
      </View>
      <Details piece={piece.data} />
      {staff && <Moves studioId={studioId} piece={piece.data} />}
      <PiecePhotos studioId={studioId} piece={piece.data} />
      <Title>History</Title>
      {piece.data.timeline.map((event) => (
        <Card key={`${event.occurredAt}-${event.to}`}>
          <Body>{stageLabel(event.to)}</Body>
          <Body muted>
            {formatDateTime(event.occurredAt)}
            {event.actorName ? ` · ${event.actorName}` : ''}
          </Body>
          {event.note && <Body>{event.note}</Body>}
        </Card>
      ))}
    </ScrollView>
  );
}

function Details({ piece }: { piece: Piece }) {
  return (
    <Card>
      {piece.makerName && <Body>Made by {piece.makerName}</Body>}
      {piece.makerMark && <Body>Maker&apos;s mark: {piece.makerMark}</Body>}
      {piece.clayBody && <Body>Clay: {piece.clayBody}</Body>}
      <Body>Firing fee: {formatMoney(piece.fee.amount.amount, piece.fee.amount.currency)}</Body>
      {piece.holdUntil && <Body>Collect by {formatDate(piece.holdUntil)}</Body>}
    </Card>
  );
}

/** Staff move a piece to any of the stages it can go to next. */
function Moves({ studioId, piece }: { studioId: string; piece: Piece }) {
  const queryClient = useQueryClient();
  const move = useMutation({
    ...movePieceMutation(),
    // Lists, the piece and the Today counts all change
    onSuccess: () => queryClient.invalidateQueries(),
  });
  if (piece.nextStages.length === 0) return null;
  return (
    <View style={styles.moves}>
      {piece.nextStages.map((to) => (
        <Button
          key={to}
          primary
          disabled={move.isPending}
          label={`Move to ${stageLabel(to)}`}
          onPress={() =>
            move.mutate({ path: { studioId, pieceId: piece.id }, body: { to, expectedVersion: piece.version } })
          }
        />
      ))}
      {move.isError && <ErrorText error={move.error} />}
    </View>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  moves: { gap: 8 },
});
