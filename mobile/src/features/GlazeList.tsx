import {
  createGlazeMutation,
  type GlazeListFilter,
  type GlazeRecipe,
  listGlazesOptions,
  useCan,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { FlatList, Pressable, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';

import { atmosphereLabel } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, SectionTitle } from '../ui';
import { GlazeForm } from './GlazeForm';

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

/** Browse studio bucket glazes, personal recipes, and member-shared recipes, and add new ones. */
export function GlazeList({
  studioId,
  onOpen,
}: {
  studioId: string;
  onOpen: (glazeId: string) => void;
}) {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<GlazeListFilter>('STUDIO');
  const [search, setSearch] = useState('');
  const [adding, setAdding] = useState(false);

  const permissions = useCan(studioId);
  const glazes = useQuery(listGlazesOptions({ path: { studioId }, query: { owner: filter } }));

  const create = useMutation({
    ...createGlazeMutation(),
    onSuccess: async (created) => {
      setAdding(false);
      await queryClient.invalidateQueries({ queryKey: [{ _id: 'listGlazes', path: { studioId } }] });
      onOpen(created.id);
    },
  });

  const canAdd = filter === 'MINE' || (filter === 'STUDIO' && permissions.can('glazes.studio.edit'));

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
            selected={filter === tab.filter}
            label={tab.label}
            onPress={() => {
              setFilter(tab.filter);
              setAdding(false);
              create.reset();
            }}
          />
        ))}
      </View>

      {canAdd && !adding && (
        <View style={styles.actions}>
          <Button
            compact
            primary
            label="+ New glaze"
            onPress={() => {
              create.reset();
              setAdding(true);
            }}
          />
        </View>
      )}

      {adding ? (
        <ScrollView contentContainerStyle={styles.formContent} keyboardShouldPersistTaps="handled">
          <SectionTitle>{filter === 'STUDIO' ? 'New studio glaze' : 'New recipe'}</SectionTitle>
          <GlazeForm
            key={filter}
            canShare={filter === 'MINE'}
            submitLabel="Save glaze"
            pending={create.isPending}
            error={create.isError ? create.error : undefined}
            onCancel={() => {
              create.reset();
              setAdding(false);
            }}
            onSubmit={(body) =>
              create.mutate({
                path: { studioId },
                body: { ...body, ownerKind: filter === 'STUDIO' ? 'STUDIO' : 'MEMBER' },
              })
            }
          />
        </ScrollView>
      ) : (
        <>
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
              keyboardShouldPersistTaps="handled"
              refreshControl={
                <RefreshControl refreshing={glazes.isRefetching} onRefresh={() => void glazes.refetch()} />
              }
              ListEmptyComponent={<Body muted>{term ? 'No recipes match your search.' : emptyText(filter)}</Body>}
              contentContainerStyle={styles.items}
              renderItem={({ item }) => <GlazeRow glaze={item} onOpen={onOpen} />}
            />
          )}
        </>
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
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  formContent: { gap: 10, paddingBottom: 32 },
  items: { gap: 8, paddingBottom: 24 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
});
