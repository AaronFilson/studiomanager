import {
  listMyNotificationsOptions,
  listMyNotificationsQueryKey,
  markNotificationReadMutation,
  type Notification,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FlatList, Pressable, RefreshControl, StyleSheet } from 'react-native';

import { formatDateTime } from '../labels';
import { Badge, Body, Card, ErrorText, Loading } from '../ui';

/** The member's notices in this studio. Opening one marks it read. */
export function NoticeList({ studioId }: { studioId: string }) {
  const queryClient = useQueryClient();
  const notices = useQuery(listMyNotificationsOptions({ path: { studioId } }));
  const markRead = useMutation({
    ...markNotificationReadMutation(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: listMyNotificationsQueryKey({ path: { studioId } }) }),
  });

  if (notices.isPending) return <Loading />;
  if (notices.isError) return <ErrorText error={notices.error} />;
  return (
    <FlatList
      data={notices.data}
      keyExtractor={(notice) => notice.id}
      refreshControl={<RefreshControl refreshing={notices.isRefetching} onRefresh={() => void notices.refetch()} />}
      ListEmptyComponent={<Body muted>No notices yet.</Body>}
      contentContainerStyle={styles.items}
      renderItem={({ item }) => (
        <NoticeRow
          notice={item}
          onRead={() => {
            if (!item.readAt) markRead.mutate({ path: { studioId, notificationId: item.id } });
          }}
        />
      )}
    />
  );
}

function NoticeRow({ notice, onRead }: { notice: Notification; onRead: () => void }) {
  return (
    <Pressable accessibilityRole="button" accessibilityLabel={notice.title} onPress={onRead}>
      <Card>
        <Body>{notice.title}</Body>
        {!notice.readAt && <Badge>New</Badge>}
        <Body muted>{formatDateTime(notice.createdAt)}</Body>
        {notice.body ? <Body>{notice.body}</Body> : null}
      </Card>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  items: { gap: 8, paddingBottom: 24 },
});
