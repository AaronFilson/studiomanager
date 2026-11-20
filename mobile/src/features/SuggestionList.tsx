import {
  cappedNote,
  decidePendingActionMutation,
  listPendingActionsOptions,
  type PendingAction,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';

import { formatDateTime } from '../labels';
import { Body, Button, Card, ErrorText, Field, Loading, SectionTitle, Title } from '../ui';

function interval(everyDays: number | undefined, everyFirings: number | undefined): string {
  const parts = [
    ...(everyDays !== undefined ? [`every ${String(everyDays)} days`] : []),
    ...(everyFirings !== undefined ? [`every ${String(everyFirings)} firings`] : []),
  ];
  return parts.join(' or ');
}

function describe(action: PendingAction): string {
  switch (action.kind) {
    case 'PURCHASE_REQUEST': {
      const request = action.purchaseRequest;
      const quantity = request ? `${request.quantity} × ` : '';
      const note = request?.note ? ` (${request.note})` : '';
      return `Request ${quantity}${action.subjectName}${note}`;
    }
    case 'MAINTENANCE_TASK': {
      const task = action.maintenanceTask;
      const when = task ? interval(task.everyDays, task.everyFirings) : '';
      return `Add “${task?.title ?? 'a task'}” on ${action.subjectName}${when ? `, ${when}` : ''}`;
    }
  }
}

/** Staff review queue for changes proposed by an AI assistant over MCP. */
export function SuggestionList({ studioId }: { studioId: string }) {
  const waiting = useQuery(listPendingActionsOptions({ path: { studioId } }));

  if (waiting.isPending) return <Loading />;
  if (waiting.isError) return <ErrorText error={waiting.error} />;

  const capNote = cappedNote(
    waiting.data,
    { order: 'oldest', noun: 'suggestions' },
    'Decide these to see the rest.',
  );

  return (
    <ScrollView
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
      refreshControl={<RefreshControl refreshing={waiting.isRefetching} onRefresh={() => void waiting.refetch()} />}
    >
      <Title>Assistant suggestions</Title>
      <Body muted>Nothing changes until staff approve a suggestion.</Body>
      {capNote && <Body muted>{capNote}</Body>}
      {waiting.data.items.length === 0 && <Body muted>Nothing is waiting.</Body>}
      {waiting.data.items.map((action) => (
        <SuggestionCard key={action.id} studioId={studioId} action={action} />
      ))}
    </ScrollView>
  );
}

function SuggestionCard({ studioId, action }: { studioId: string; action: PendingAction }) {
  const queryClient = useQueryClient();
  const [note, setNote] = useState('');
  const decide = useMutation({
    ...decidePendingActionMutation(),
    onSuccess: () => queryClient.invalidateQueries(),
  });

  function send(decision: 'APPROVED' | 'REJECTED') {
    decide.mutate({
      path: { studioId, actionId: action.id },
      body: {
        decision,
        expectedVersion: action.version,
        ...(decision === 'REJECTED' && note.trim() ? { note: note.trim() } : {}),
      },
    });
  }

  return (
    <Card>
      <SectionTitle>{describe(action)}</SectionTitle>
      <Body muted>
        from {action.proposedByName ?? 'someone'}
        {action.proposedViaTokenName ? `, through ‘${action.proposedViaTokenName}’` : ''} ·{' '}
        {formatDateTime(action.proposedAt)}
      </Body>
      {action.reason && <Body>{action.reason}</Body>}
      <Field label="Why not (optional)" value={note} onChangeText={setNote} maxLength={500} />
      {decide.isError && <ErrorText error={decide.error} />}
      <View style={styles.rowWrap}>
        <Button primary label="Approve" disabled={decide.isPending} onPress={() => send('APPROVED')} />
        <Button label="Reject" disabled={decide.isPending} onPress={() => send('REJECTED')} />
      </View>
    </Card>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  rowWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
});
