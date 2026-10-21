import { listPiecesOptions, type Piece, type PieceStage } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, View } from 'react-native';

import { formatDate, stageLabel } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Loading } from '../ui';

/** The stages staff work through on the shelf. Members see all of their own pieces. */
const shelfStages: PieceStage[] = ['RECEIVED', 'BISQUE_FIRED', 'GLAZED', 'READY'];

export function PieceList({
  studioId,
  staff,
  onOpen,
}: {
  studioId: string;
  staff: boolean;
  onOpen: (pieceId: string) => void;
}) {
  const [stage, setStage] = useState<PieceStage | undefined>(undefined);
  const pieces = useQuery(listPiecesOptions({ path: { studioId }, query: stage ? { stage } : undefined }));

  return (
    <View style={styles.list}>
      {staff && (
        <View style={styles.filters} accessibilityRole="radiogroup" accessibilityLabel="Stage">
          <Button label="All" primary={!stage} onPress={() => setStage(undefined)} />
          {shelfStages.map((s) => (
            <Button key={s} label={stageLabel(s)} primary={stage === s} onPress={() => setStage(s)} />
          ))}
        </View>
      )}
      {pieces.isPending && <Loading />}
      {pieces.isError && <ErrorText error={pieces.error} />}
      {pieces.isSuccess && (
        <FlatList
          data={pieces.data}
          keyExtractor={(piece) => piece.id}
          refreshControl={<RefreshControl refreshing={pieces.isRefetching} onRefresh={() => void pieces.refetch()} />}
          ListEmptyComponent={<Body muted>{staff ? 'No pieces here.' : 'You have no pieces yet.'}</Body>}
          contentContainerStyle={styles.items}
          renderItem={({ item }) => <PieceRow piece={item} staff={staff} onOpen={onOpen} />}
        />
      )}
    </View>
  );
}

function PieceRow({ piece, staff, onOpen }: { piece: Piece; staff: boolean; onOpen: (pieceId: string) => void }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`Piece ${String(piece.number)}`}
      onPress={() => onOpen(piece.id)}
    >
      <Card>
        <Body>
          Piece {piece.number}
          {piece.description ? `: ${piece.description}` : ''}
        </Body>
        {staff && piece.makerName && <Body muted>{piece.makerName}</Body>}
        <View style={styles.badges}>
          <Badge>{stageLabel(piece.stage)}</Badge>
          {piece.overdue && <Badge warning>Past its pickup date</Badge>}
          {piece.stage === 'READY' && piece.holdUntil && !piece.overdue && (
            <Badge>Collect by {formatDate(piece.holdUntil)}</Badge>
          )}
        </View>
      </Card>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  list: { flex: 1, gap: 12 },
  filters: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  items: { gap: 8, paddingBottom: 24 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
});
