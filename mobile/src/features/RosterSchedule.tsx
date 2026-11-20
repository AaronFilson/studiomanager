import {
  assignShiftMutation,
  cappedNote,
  getHoursOptions,
  getMeOptions,
  listShiftsOptions,
  markShiftWorkedMutation,
  offerShiftMutation,
  type Shift,
  useCan,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';

import { dutyLabel, formatDate, formatDuration, formatTime } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Loading, SectionTitle, Title } from '../ui';

function createWindow() {
  const opened = new Date();
  const start = new Date(opened);
  start.setHours(0, 0, 0, 0);
  const end = new Date(start);
  end.setDate(end.getDate() + 28);
  return { from: start.toISOString(), to: end.toISOString(), now: opened.getTime() };
}

/**
 * Upcoming studio shifts (next 4 weeks) and the caller's credited hours ledger.
 * Members can take open shifts, cover offered shifts, or ask for cover; staff can mark shifts worked.
 */
export function RosterSchedule({ studioId }: { studioId: string }) {
  const [{ from, to, now }] = useState(createWindow);
  const { can } = useCan(studioId);
  const staff = can('schedule.manage');
  const me = useQuery(getMeOptions());
  const myId = me.data?.account.id;
  const shifts = useQuery(listShiftsOptions({ path: { studioId }, query: { from, to } }));

  const capNote = cappedNote(
    shifts.data,
    { order: 'first', noun: 'shifts' },
    "The rest of the four weeks isn't listed.",
  );

  return (
    <ScrollView
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={shifts.isRefetching} onRefresh={() => void shifts.refetch()} />}
    >
      <Title>Roster</Title>

      {myId && <MyHoursCard studioId={studioId} accountId={myId} />}

      {shifts.isPending && <Loading />}
      {shifts.isError && <ErrorText error={shifts.error} />}
      {shifts.isSuccess && shifts.data.items.length === 0 && (
        <Body muted>No shifts in the next four weeks.</Body>
      )}
      {capNote && <Body muted>{capNote}</Body>}

      {shifts.data?.items.map((shift) => (
        <ShiftCard key={shift.id} studioId={studioId} shift={shift} staff={staff} myId={myId} now={now} />
      ))}
    </ScrollView>
  );
}

function MyHoursCard({ studioId, accountId }: { studioId: string; accountId: string }) {
  const ledger = useQuery(getHoursOptions({ path: { studioId, accountId } }));

  if (ledger.isPending) return null;
  if (ledger.isError) return <ErrorText error={ledger.error} />;

  return (
    <Card>
      <SectionTitle>My hours: {formatDuration(ledger.data.totalMinutes)}</SectionTitle>
      {ledger.data.entries.length === 0 ? (
        <Body muted>No hours credited yet.</Body>
      ) : (
        ledger.data.entries.slice(0, 10).map((entry) => (
          <Body key={entry.id} muted>
            {formatDuration(entry.minutes)} · {entry.note ?? 'Shift'} · {formatDate(entry.creditedAt)}
          </Body>
        ))
      )}
    </Card>
  );
}

function ShiftCard({
  studioId,
  shift,
  staff,
  myId,
  now,
}: {
  studioId: string;
  shift: Shift;
  staff: boolean;
  myId?: string;
  now: number;
}) {
  const queryClient = useQueryClient();
  const onSuccess = () => queryClient.invalidateQueries();
  const assign = useMutation({ ...assignShiftMutation(), onSuccess });
  const offer = useMutation({ ...offerShiftMutation(), onSuccess });
  const worked = useMutation({ ...markShiftWorkedMutation(), onSuccess });
  const error = assign.error ?? offer.error ?? worked.error;

  const mine = myId !== undefined && shift.accountId === myId;
  const upcoming = !shift.workedAt && new Date(shift.endsAt).getTime() > now;

  return (
    <Card>
      <SectionTitle>{shift.title}</SectionTitle>
      <Body muted>
        {formatDate(shift.startsAt)} {formatTime(shift.startsAt)}–{formatTime(shift.endsAt)} ({formatDuration(shift.minutes)})
      </Body>
      <View style={styles.badges}>
        {shift.duty && <Badge>{dutyLabel(shift.duty)}</Badge>}
        <Badge warning={shift.open}>{shift.accountName ?? (shift.open ? 'Open' : 'Taken')}</Badge>
        {shift.offered && <Badge warning>Needs cover</Badge>}
        {shift.workedAt && <Badge>Worked · hours credited</Badge>}
      </View>

      <View style={styles.rowWrap}>
        {shift.open && myId && (
          <Button
            compact
            primary
            label="Take shift"
            disabled={assign.isPending}
            onPress={() =>
              assign.mutate({
                path: { studioId, shiftId: shift.id },
                body: { accountId: myId, expectedVersion: shift.version },
              })
            }
          />
        )}
        {shift.offered && !mine && myId && upcoming && (
          <Button
            compact
            primary
            label="Cover shift"
            disabled={assign.isPending}
            onPress={() =>
              assign.mutate({
                path: { studioId, shiftId: shift.id },
                body: { accountId: myId, expectedVersion: shift.version },
              })
            }
          />
        )}
        {mine && upcoming && (
          <Button
            compact
            label={shift.offered ? 'I can do it after all' : 'Ask for cover'}
            disabled={offer.isPending}
            onPress={() =>
              offer.mutate({
                path: { studioId, shiftId: shift.id },
                body: { offered: !shift.offered, expectedVersion: shift.version },
              })
            }
          />
        )}
        {staff && shift.accountId && !shift.workedAt && (
          <Button
            compact
            label="Mark worked"
            disabled={worked.isPending}
            onPress={() =>
              worked.mutate({
                path: { studioId, shiftId: shift.id },
                body: { expectedVersion: shift.version },
              })
            }
          />
        )}
      </View>

      {error && <ErrorText error={error} />}
    </Card>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  badges: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  rowWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 4 },
});
