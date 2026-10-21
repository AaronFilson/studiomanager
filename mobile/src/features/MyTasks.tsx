import {
  completeMaintenanceTaskMutation,
  listMyMaintenanceTasksOptions,
  type MaintenanceTask,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { type ReactElement, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, View } from 'react-native';

import { dueText } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, Title } from '../ui';

/**
 * Maintenance that falls to this person, by name or through a duty such as monitor, most urgent
 * first. Done at the clay trap with the phone in hand: tick the checklist, add a note, done.
 */
export function MyTasks({ studioId, footer }: { studioId: string; footer?: ReactElement }) {
  const tasks = useQuery(listMyMaintenanceTasksOptions({ path: { studioId } }));

  if (tasks.isPending) return <Loading />;
  if (tasks.isError) return <ErrorText error={tasks.error} />;
  return (
    <FlatList
      data={tasks.data}
      keyExtractor={(task) => task.id}
      refreshControl={<RefreshControl refreshing={tasks.isRefetching} onRefresh={() => void tasks.refetch()} />}
      ListEmptyComponent={<Body muted>No maintenance falls to you.</Body>}
      ListFooterComponent={footer}
      contentContainerStyle={styles.items}
      renderItem={({ item }) => <TaskCard studioId={studioId} task={item} />}
    />
  );
}

function TaskCard({ studioId, task }: { studioId: string; task: MaintenanceTask }) {
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [checked, setChecked] = useState<string[]>([]);
  const [note, setNote] = useState('');
  const complete = useMutation({
    ...completeMaintenanceTaskMutation(),
    onSuccess: async () => {
      setOpen(false);
      setChecked([]);
      setNote('');
      await queryClient.invalidateQueries();
    },
  });
  const due = dueText(task);

  return (
    <Card>
      <Title>{task.title}</Title>
      <Body muted>{task.assetName}</Body>
      {due ? <Badge warning={task.status !== 'OK'}>{due}</Badge> : null}
      {open ? (
        <View style={styles.form}>
          {task.checklist.map((line) => {
            const ticked = checked.includes(line);
            return (
              <Pressable
                key={line}
                accessibilityRole="checkbox"
                accessibilityState={{ checked: ticked }}
                accessibilityLabel={line}
                onPress={() => setChecked((c) => (ticked ? c.filter((l) => l !== line) : [...c, line]))}
              >
                <Body>
                  {ticked ? '☑' : '☐'} {line}
                </Body>
              </Pressable>
            );
          })}
          <Field label="Notes (optional)" value={note} onChangeText={setNote} maxLength={1000} />
          {complete.isError && <ErrorText error={complete.error} />}
          <Button
            primary
            label="Done"
            disabled={complete.isPending}
            onPress={() =>
              complete.mutate({
                path: { studioId, taskId: task.id },
                body: { checked, ...(note.trim() ? { note: note.trim() } : {}) },
              })
            }
          />
          <Button label="Cancel" onPress={() => setOpen(false)} />
        </View>
      ) : (
        <Button label="Mark done" onPress={() => setOpen(true)} />
      )}
    </Card>
  );
}

const styles = StyleSheet.create({
  items: { gap: 8, paddingBottom: 24 },
  form: { gap: 8 },
});
