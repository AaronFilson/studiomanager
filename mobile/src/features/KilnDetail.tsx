import {
  cappedNote,
  type Firing,
  type FiringKind,
  FROM_THE_EXPORT,
  getFiringOptions,
  getFiringQueryKey,
  getKilnQueueOptions,
  type Kiln,
  listKilnFiringsOptions,
  listKilnsOptions,
  loadKilnMutation,
  setKilnAvailabilityMutation,
  transitionFiringMutation,
  useCan,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Pressable, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';

import { formatDate, formatDateTime, kilnTypeLabel } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, SectionTitle, Title } from '../ui';

/** Kiln operations: load from queue, start/unload/cancel firing, availability, and firing log. */
export function KilnDetail({ studioId, kilnId }: { studioId: string; kilnId: string }) {
  const permissions = useCan(studioId);
  const canManageFirings = permissions.can('firings.manage');
  const canManageKilns = permissions.can('kilns.manage');

  const kilns = useQuery(listKilnsOptions({ path: { studioId } }));
  const firings = useQuery({
    ...listKilnFiringsOptions({ path: { studioId, kilnId } }),
    enabled: canManageFirings,
  });

  if (kilns.isPending || permissions.isPending) return <Loading />;
  if (kilns.isError) return <ErrorText error={kilns.error} />;
  if (permissions.isError) return <ErrorText error={permissions.error} />;

  const kiln = kilns.data.find((k) => k.id === kilnId);
  if (!kiln) {
    return <Body>No such kiln.</Body>;
  }

  const open = firings.data?.items.find((f) => f.status === 'LOADING' || f.status === 'FIRING');
  const logCapNote = cappedNote(firings.data, { order: 'newest', noun: 'firings' }, FROM_THE_EXPORT);

  return (
    <ScrollView
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
      refreshControl={
        <RefreshControl
          refreshing={kilns.isRefetching || (canManageFirings && firings.isRefetching)}
          onRefresh={() => {
            void kilns.refetch();
            if (canManageFirings) void firings.refetch();
          }}
        />
      }
    >
      <Title>{kiln.name}</Title>
      <Card>
        <View style={styles.badges}>
          <Badge>{kilnTypeLabel(kiln.kind)}</Badge>
          <Badge>{String(kiln.totalFirings)} firings</Badge>
          {kiln.maxPiecesPerMember !== undefined && (
            <Badge>At most {String(kiln.maxPiecesPerMember)} pieces per member</Badge>
          )}
          <Badge warning={!kiln.available}>
            {kiln.available ? 'In service' : `Out of service: ${kiln.downReason ?? ''}`}
          </Badge>
        </View>
      </Card>

      {canManageFirings && firings.isPending && <Loading />}
      {canManageFirings && firings.isError && <ErrorText error={firings.error} />}
      {canManageFirings &&
        firings.isSuccess &&
        (open ? (
          <OpenFiringCard key={open.id} studioId={studioId} firingId={open.id} />
        ) : (
          <LoadFormCard key={kiln.id} studioId={studioId} kiln={kiln} />
        ))}

      {canManageKilns && <AvailabilityCard key={kiln.id} studioId={studioId} kiln={kiln} />}

      {canManageFirings && firings.isSuccess && (
        <Card>
          <SectionTitle>Firing log</SectionTitle>
          {firings.data.items.length === 0 && <Body muted>No firings yet.</Body>}
          {logCapNote && <Body muted>{logCapNote}</Body>}
          {firings.data.items.map((firing) => (
            <View key={firing.id} style={styles.logRow}>
              <Body>
                {firing.kind === 'BISQUE' ? 'Bisque' : 'Glaze'}
                {firing.cone ? ` · cone ${firing.cone}` : ''} · {String(firing.pieceCount)} pieces ·{' '}
                {firing.status.toLowerCase()}
              </Body>
              <Body muted>{formatDateTime(firing.createdAt)}</Body>
              {firing.notes && <Body muted>{firing.notes}</Body>}
            </View>
          ))}
        </Card>
      )}
    </ScrollView>
  );
}

function LoadFormCard({ studioId, kiln }: { studioId: string; kiln: Kiln }) {
  const queryClient = useQueryClient();
  const [kind, setKind] = useState<FiringKind>('BISQUE');
  const [cone, setCone] = useState('');
  const [chosen, setChosen] = useState<Set<string>>(() => new Set());
  const queue = useQuery(getKilnQueueOptions({ path: { studioId, kilnId: kiln.id }, query: { kind } }));
  const load = useMutation({
    ...loadKilnMutation(),
    onSuccess: async () => {
      setChosen(new Set());
      setCone('');
      await queryClient.invalidateQueries();
    },
  });

  function switchKind(next: FiringKind) {
    setKind(next);
    setChosen(new Set());
  }

  function toggle(pieceId: string) {
    setChosen((current) => {
      const next = new Set(current);
      if (next.has(pieceId)) next.delete(pieceId);
      else next.add(pieceId);
      return next;
    });
  }

  function fillFromQueue() {
    setChosen(new Set(queue.data?.items.filter((e) => e.fitsNextLoad).map((e) => e.piece.id)));
  }

  const queueCapNote = cappedNote(
    queue.data,
    { order: 'longest-waiting', noun: 'pieces' },
    'Load some of these, and the rest will show up next.',
  );

  return (
    <Card>
      <SectionTitle>Load the kiln</SectionTitle>
      {!kiln.available && <Badge warning>This kiln is out of service.</Badge>}

      <View style={styles.rowWrap}>
        <Button compact primary={kind === 'BISQUE'} selected={kind === 'BISQUE'} label="Bisque" onPress={() => switchKind('BISQUE')} />
        <Button compact primary={kind === 'GLAZE'} selected={kind === 'GLAZE'} label="Glaze" onPress={() => switchKind('GLAZE')} />
      </View>

      <Field label="Cone (optional)" value={cone} onChangeText={setCone} maxLength={10} placeholder="e.g. 04 or 6" />

      {queue.isPending && <Loading />}
      {queue.isError && <ErrorText error={queue.error} />}
      {queue.isSuccess && queue.data.items.length === 0 && (
        <Body muted>Nothing is waiting for a {kind.toLowerCase()} firing.</Body>
      )}
      {queue.isSuccess && queue.data.items.length > 0 && (
        <View style={styles.group}>
          <Body muted>Longest-waiting first. Dimmed pieces are over the per-member cap for this load.</Body>
          {queueCapNote && <Body muted>{queueCapNote}</Body>}
          {queue.data.items.map(({ piece, waitingSince, fitsNextLoad }) => {
            const checked = chosen.has(piece.id);
            const label = `Piece ${String(piece.number)} · ${piece.makerName ?? 'Unnamed member'}${
              piece.description ? ` · ${piece.description}` : ''
            }`;
            return (
              <Pressable
                key={piece.id}
                accessibilityRole="checkbox"
                accessibilityState={{ checked }}
                accessibilityLabel={label}
                onPress={() => toggle(piece.id)}
                style={[styles.queueRow, !fitsNextLoad && styles.dimmed]}
              >
                <Body>
                  {checked ? '☑' : '☐'} {label}
                </Body>
                <View style={styles.badges}>
                  <Badge>since {formatDate(waitingSince)}</Badge>
                  {!fitsNextLoad && <Badge warning>Over cap</Badge>}
                </View>
              </Pressable>
            );
          })}
        </View>
      )}

      {load.isError && <ErrorText error={load.error} />}

      <View style={styles.rowWrap}>
        <Button
          compact
          label="Fill from queue"
          disabled={!queue.data?.items.length}
          onPress={fillFromQueue}
        />
        <Button
          primary
          label={`Load ${String(chosen.size)} ${chosen.size === 1 ? 'piece' : 'pieces'}`}
          disabled={chosen.size === 0 || load.isPending || !kiln.available}
          onPress={() =>
            load.mutate({
              path: { studioId, kilnId: kiln.id },
              body: { kind, pieceIds: [...chosen], ...(cone.trim() ? { cone: cone.trim() } : {}) },
            })
          }
        />
      </View>
    </Card>
  );
}

function OpenFiringCard({ studioId, firingId }: { studioId: string; firingId: string }) {
  const queryClient = useQueryClient();
  const firing = useQuery(getFiringOptions({ path: { studioId, firingId } }));
  const [notes, setNotes] = useState('');
  const transition = useMutation({
    ...transitionFiringMutation(),
    onSuccess: async (updated: Firing) => {
      queryClient.setQueryData(getFiringQueryKey({ path: { studioId, firingId } }), updated);
      setNotes('');
      await queryClient.invalidateQueries();
    },
  });

  if (firing.isPending) return <Loading />;
  if (firing.isError) return <ErrorText error={firing.error} />;

  const f = firing.data;
  const move = (to: 'FIRING' | 'COMPLETE' | 'CANCELLED') =>
    transition.mutate({
      path: { studioId, firingId },
      body: { to, expectedVersion: f.version, ...(notes.trim() ? { notes: notes.trim() } : {}) },
    });

  return (
    <Card>
      <SectionTitle>
        {f.status === 'LOADING' ? 'Loading' : 'Firing'}: {f.kind === 'BISQUE' ? 'bisque' : 'glaze'}
        {f.cone ? `, cone ${f.cone}` : ''}
      </SectionTitle>
      {f.pieces.map((piece) => (
        <Body key={piece.id}>
          Piece {String(piece.number)} · {piece.makerName ?? 'Unnamed member'}
          {piece.description ? ` · ${piece.description}` : ''}
        </Body>
      ))}
      <Field label="Notes (optional)" value={notes} onChangeText={setNotes} maxLength={1000} />
      {transition.isError && <ErrorText error={transition.error} />}
      <View style={styles.group}>
        {f.status === 'LOADING' && (
          <Button primary label="Start firing" disabled={transition.isPending} onPress={() => move('FIRING')} />
        )}
        {f.status === 'FIRING' && (
          <Button
            primary
            label="Unload and notify makers"
            disabled={transition.isPending}
            onPress={() => move('COMPLETE')}
          />
        )}
        <Button label="Cancel this load" disabled={transition.isPending} onPress={() => move('CANCELLED')} />
      </View>
    </Card>
  );
}

function AvailabilityCard({ studioId, kiln }: { studioId: string; kiln: Kiln }) {
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const save = useMutation({
    ...setKilnAvailabilityMutation(),
    onSuccess: async () => {
      setReason('');
      await queryClient.invalidateQueries();
    },
  });

  return (
    <Card>
      <SectionTitle>Availability</SectionTitle>
      {kiln.available ? (
        <View style={styles.group}>
          <Field label="Out of service reason" value={reason} onChangeText={setReason} maxLength={200} />
          <Button
            label="Take out of service"
            disabled={!reason.trim() || save.isPending}
            onPress={() =>
              save.mutate({
                path: { studioId, kilnId: kiln.id },
                body: { downReason: reason.trim(), expectedVersion: kiln.version },
              })
            }
          />
        </View>
      ) : (
        <Button
          primary
          label="Put back in service"
          disabled={save.isPending}
          onPress={() =>
            save.mutate({
              path: { studioId, kilnId: kiln.id },
              body: { expectedVersion: kiln.version },
            })
          }
        />
      )}
      {save.isError && <ErrorText error={save.error} />}
    </Card>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  rowWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, alignItems: 'center' },
  group: { gap: 8 },
  queueRow: { gap: 4, paddingVertical: 6 },
  dimmed: { opacity: 0.55 },
  logRow: { gap: 2, paddingVertical: 4 },
});
