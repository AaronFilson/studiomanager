import {
  listMyNotificationsOptions,
  listMyNotificationsQueryKey,
  markNotificationReadMutation,
  type Notification,
  sendNoticeMutation,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, View } from 'react-native';

import { formatDateTime } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, SectionTitle } from '../ui';

/** The member's notices in this studio. Opening one marks it read. Staff with notices.send can broadcast. */
export function NoticeList({ studioId, canSend = false }: { studioId: string; canSend?: boolean }) {
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
      data={notices.data.items}
      keyExtractor={(notice) => notice.id}
      keyboardShouldPersistTaps="handled"
      refreshControl={<RefreshControl refreshing={notices.isRefetching} onRefresh={() => void notices.refetch()} />}
      ListHeaderComponent={canSend ? <SendNoticeCard studioId={studioId} /> : undefined}
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

function SendNoticeCard({ studioId }: { studioId: string }) {
  const queryClient = useQueryClient();
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const send = useMutation({
    ...sendNoticeMutation(),
    onSuccess: async () => {
      setTitle('');
      setBody('');
      await queryClient.invalidateQueries({ queryKey: listMyNotificationsQueryKey({ path: { studioId } }) });
    },
  });

  return (
    <View style={styles.header}>
      <Card>
        <SectionTitle>Send a studio notice</SectionTitle>
        <Field label="Notice title" value={title} onChangeText={setTitle} maxLength={120} />
        <Field label="Message" value={body} onChangeText={setBody} maxLength={2000} multiline />
        {send.isError && <ErrorText error={send.error} />}
        {send.isSuccess && (
          <Body>
            Sent to {String(send.data.sent)} {send.data.sent === 1 ? 'member' : 'members'}.
          </Body>
        )}
        <Button
          primary
          label="Send notice"
          disabled={send.isPending || !title.trim()}
          onPress={() =>
            send.mutate({
              path: { studioId },
              body: { title: title.trim(), body: body.trim() },
            })
          }
        />
      </Card>
    </View>
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
  header: { marginBottom: 8 },
});
