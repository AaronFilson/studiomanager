import { type GlazeListFilter, type GlazeRecipe, listGlazesOptions } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, View } from 'react-native';

import { atmosphereLabel } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, SectionTitle } from '../ui';

const tabs: { filter: GlazeListFilter; label: string }[] = [
  { filter: 'STUDIO', label: 'Studio glazes' },
  { filter: 'MINE', label: 'My recipes' },
  { filter: 'SHARED', label: 'Shared' },
];

function emptyText(filter: GlazeListFilter): string {
  switch (filter) {
    case 'STUDIO':
      return 'The studio has no glazes in the book yet.';
    case 'MINE':
      return 'You have no recipes yet.';
    case 'SHARED':
      return 'No one has shared a recipe yet.';
  }
}

/** Browse studio bucket glazes, personal recipes, and member-shared recipes. */
export function GlazeList({
  studioId,
  onOpen,
}: {
  studioId: string;
  onOpen: (glazeId: string) => void;
}) {
  const [filter, setFilter] = useState<GlazeListFilter>('STUDIO');
  const [search, setSearch] = useState('');
  const glazes = useQuery(listGlazesOptions({ path: { studioId }, query: { owner: filter } }));

  const term = search.trim().toLowerCase();
  const items = (glazes.data ?? []).filter((g) => {
    if (!term) return true;
    return (
      g.name.toLowerCase().includes(term) ||
      (g.cone?.toLowerCase().includes(term) ?? false) ||
      g.lines.some((line) => line.material.toLowerCase().includes(term))
    );
  });

  return (
    <View style={styles.container}>
      <View style={styles.tabs} accessibilityRole="tablist" accessibilityLabel="Glaze collection">
        {tabs.map((tab) => (
          <Button
            key={tab.filter}
            compact
            primary={filter === tab.filter}
            label={tab.label}
            onPress={() => setFilter(tab.filter)}
          />
        ))}
      </View>

      <Field
        label="Search glazes"
        value={search}
        onChangeText={setSearch}
        autoCorrect={false}
        placeholder="Name, cone, or material"
      />

      {glazes.isPending && <Loading />}
      {glazes.isError && <ErrorText error={glazes.error} />}
      {glazes.isSuccess && (
        <FlatList
          data={items}
          keyExtractor={(g) => g.id}
          refreshControl={<RefreshControl refreshing={glazes.isRefetching} onRefresh={() => void glazes.refetch()} />}
          ListEmptyComponent={<Body muted>{term ? 'No recipes match your search.' : emptyText(filter)}</Body>}
          contentContainerStyle={styles.items}
          renderItem={({ item }) => <GlazeRow glaze={item} onOpen={onOpen} />}
        />
      )}
    </View>
  );
}

function GlazeRow({ glaze, onOpen }: { glaze: GlazeRecipe; onOpen: (glazeId: string) => void }) {
  return (
    <Pressable accessibilityRole="button" accessibilityLabel={glaze.name} onPress={() => onOpen(glaze.id)}>
      <Card>
        <SectionTitle>{glaze.name}</SectionTitle>
        <Body muted>
          {glaze.ownerKind === 'STUDIO' ? 'Studio glaze' : (glaze.ownerName ?? 'Member recipe')}
        </Body>
        <View style={styles.badges}>
          {glaze.cone && <Badge>Cone {glaze.cone}</Badge>}
          {glaze.atmosphere && <Badge>{atmosphereLabel(glaze.atmosphere)}</Badge>}
          {glaze.foodSurface && <Badge>Food surface</Badge>}
          {glaze.targetSpecificGravity && <Badge>SG {glaze.targetSpecificGravity}</Badge>}
          {glaze.sharedWithStudio && <Badge>Shared</Badge>}
        </View>
      </Card>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, gap: 10 },
  tabs: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  items: { gap: 8, paddingBottom: 24 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
});
