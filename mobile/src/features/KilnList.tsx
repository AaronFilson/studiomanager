import { type Kiln, listKilnsOptions } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { FlatList, Pressable, RefreshControl, StyleSheet, View } from 'react-native';

import { kilnTypeLabel } from '../labels';
import { Badge, Body, Card, ErrorText, Loading, SectionTitle } from '../ui';

/** Lists the studio's kilns with availability and firing counts. */
export function KilnList({ studioId, onOpen }: { studioId: string; onOpen: (kilnId: string) => void }) {
  const kilns = useQuery(listKilnsOptions({ path: { studioId } }));

  if (kilns.isPending) return <Loading />;
  if (kilns.isError) return <ErrorText error={kilns.error} />;

  return (
    <FlatList
      data={kilns.data}
      keyExtractor={(kiln) => kiln.id}
      refreshControl={<RefreshControl refreshing={kilns.isRefetching} onRefresh={() => void kilns.refetch()} />}
      ListEmptyComponent={<Body muted>No kilns yet.</Body>}
      contentContainerStyle={styles.items}
      renderItem={({ item }) => <KilnRow kiln={item} onOpen={onOpen} />}
    />
  );
}

function KilnRow({ kiln, onOpen }: { kiln: Kiln; onOpen: (kilnId: string) => void }) {
  return (
    <Pressable accessibilityRole="button" accessibilityLabel={kiln.name} onPress={() => onOpen(kiln.id)}>
      <Card>
        <SectionTitle>
          {kiln.name}
          {kiln.site ? ` (${kiln.site})` : ''}
        </SectionTitle>
        <View style={styles.badges}>
          <Badge>{kilnTypeLabel(kiln.kind)}</Badge>
          <Badge>{String(kiln.totalFirings)} firings</Badge>
          {kiln.maxPiecesPerMember !== undefined && (
            <Badge>Max {String(kiln.maxPiecesPerMember)}/member</Badge>
          )}
          {!kiln.available && <Badge warning>Out of service</Badge>}
        </View>
        {!kiln.available && kiln.downReason && <Body muted>{kiln.downReason}</Body>}
      </Card>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  items: { gap: 8, paddingBottom: 24 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
});
