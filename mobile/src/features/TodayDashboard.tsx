import { getTodayOptions, useCan } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { Pressable, RefreshControl, ScrollView, StyleSheet, View } from 'react-native';

import { dueText, formatDate, formatTime } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Loading, SectionTitle, Title } from '../ui';

function count(n: number, one: string, many: string): string {
  return `${String(n)} ${n === 1 ? one : many}`;
}

/** The staff handover view: everything whoever is covering the studio needs on one screen. */
export function TodayDashboard({
  studioId,
  onOpenKiln,
  onOpenKilns,
  onOpenPieces,
  onOpenSupplies,
}: {
  studioId: string;
  onOpenKiln: (kilnId: string) => void;
  onOpenKilns: () => void;
  onOpenPieces: () => void;
  onOpenSupplies: () => void;
}) {
  const today = useQuery(getTodayOptions({ path: { studioId } }));
  const permissions = useCan(studioId);

  if (today.isPending || permissions.isPending) return <Loading />;
  if (today.isError) return <ErrorText error={today.error} />;
  if (permissions.isError) return <ErrorText error={permissions.error} />;

  const canOpenKilns =
    permissions.can('kilns.read') || permissions.can('kilns.manage') || permissions.can('firings.manage');
  const canOpenSupplies = permissions.can('supplies.manage');

  const t = today.data;
  const attention = [
    ...t.outOfService.map((k) => `${k.name} is out of service: ${k.downReason ?? ''}`),
    ...t.maintenanceDue.map((task) => `${task.assetName}: ${task.title} ${dueText(task)}`),
    ...(t.pastHold > 0 ? [`${count(t.pastHold, 'ready piece is', 'ready pieces are')} past their hold`] : []),
    ...(t.unclaimed > 0 ? [`${count(t.unclaimed, 'piece is', 'pieces are')} unclaimed`] : []),
    ...t.lowStock.map((item) => `${item.name} is low (${item.onHand} left) and not on order`),
    ...(t.openPurchaseRequests > 0
      ? [`${count(t.openPurchaseRequests, 'purchase request is', 'purchase requests are')} open`]
      : []),
    ...(t.uncoveredShifts.length > 0
      ? [
          `${count(t.uncoveredShifts.length, 'shift', 'shifts')} in the next week ${
            t.uncoveredShifts.length === 1 ? 'needs' : 'need'
          } cover`,
        ]
      : []),
    ...t.openProblems.map(
      (p) => `Reported by ${p.reportedByName ?? 'a member'}: ${p.assetName ?? 'something else'}: ${p.description}`,
    ),
  ];

  return (
    <ScrollView
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={today.isRefetching} onRefresh={() => void today.refetch()} />}
    >
      <Title>Today, {formatDate(`${t.date}T12:00:00`)}</Title>

      {attention.length > 0 && (
        <Card>
          <SectionTitle>Needs attention</SectionTitle>
          {attention.map((line, index) => (
            <Body key={`${String(index)}-${line}`}>• {line}</Body>
          ))}
        </Card>
      )}

      <Card>
        <SectionTitle>Kilns</SectionTitle>
        {t.kilns.length === 0 && <Body muted>No kilns yet.</Body>}
        {t.kilns.map((kiln) => {
          const firing = t.openFirings.find((f) => f.kilnId === kiln.id);
          const status = !kiln.available
            ? 'out of service'
            : firing
              ? `${firing.status === 'FIRING' ? 'firing' : 'loading'} ${firing.kind.toLowerCase()}, ${count(
                  firing.pieceCount,
                  'piece',
                  'pieces',
                )}`
              : 'empty';
          if (!canOpenKilns) {
            return (
              <View key={kiln.id} style={styles.kilnRow}>
                <Body>{kiln.name}</Body>
                <Badge warning={!kiln.available}>{status}</Badge>
              </View>
            );
          }
          return (
            <Pressable
              key={kiln.id}
              accessibilityRole="button"
              accessibilityLabel={`${kiln.name}: ${status}`}
              onPress={() => onOpenKiln(kiln.id)}
              style={styles.kilnRow}
            >
              <Body>{kiln.name}</Body>
              <Badge warning={!kiln.available}>{status}</Badge>
            </Pressable>
          );
        })}
        <Body muted>
          Waiting: {String(t.waitingForBisque)} for bisque, {String(t.waitingForGlaze)} for glaze. Ready for pickup:{' '}
          {String(t.readyForPickup)}.
        </Body>
        <View style={styles.actions}>
          {canOpenKilns && <Button compact label="Kilns" onPress={onOpenKilns} />}
          <Button compact label="Pieces" onPress={onOpenPieces} />
          {canOpenSupplies && <Button compact label="Supplies" onPress={onOpenSupplies} />}
        </View>
      </Card>

      <Card>
        <SectionTitle>On today</SectionTitle>
        {t.shifts.length === 0 && t.bookings.length === 0 && (
          <Body muted>Nothing on the calendar or roster today.</Body>
        )}
        {t.shifts.map((shift) => (
          <View key={shift.id} style={styles.itemRow}>
            <Body>
              {formatTime(shift.startsAt)}–{formatTime(shift.endsAt)} {shift.title}:{' '}
              {shift.accountName ?? 'nobody yet'}
            </Body>
            {!shift.accountName && <Badge warning>Uncovered</Badge>}
          </View>
        ))}
        {t.bookings.map((booking) => (
          <Body key={booking.id}>
            {formatTime(booking.startsAt)}–{formatTime(booking.endsAt)} {booking.resourceName}: {booking.title}
            {booking.accountName ? ` (${booking.accountName})` : ''}
          </Body>
        ))}
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  kilnRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 6, gap: 8 },
  itemRow: { gap: 4 },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 4 },
});
