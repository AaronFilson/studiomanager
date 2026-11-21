import {
  type BookingKind,
  cancelBookingMutation,
  cappedNote,
  createBookingMutation,
  listBookingsOptions,
  listResourcesOptions,
  type Resource,
  useCan,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';

import { bookingKindLabel, formatDate, formatTime, staffBookingKinds } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, SectionTitle, Title } from '../ui';

const datePattern = /^\d{4}-\d{2}-\d{2}$/;
const timePattern = /^\d{2}:\d{2}$/;

function startOfToday(): Date {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
}

function toDateInput(d: Date): string {
  const year = String(d.getFullYear());
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function parseLocalDateTime(dateInput: string, timeInput: string): Date | null {
  const trimmedDate = dateInput.trim();
  const trimmedTime = timeInput.trim();
  if (!datePattern.test(trimmedDate) || !timePattern.test(trimmedTime)) return null;
  const [yearStr, monthStr, dayStr] = trimmedDate.split('-');
  const [hourStr, minuteStr] = trimmedTime.split(':');
  if (!yearStr || !monthStr || !dayStr || !hourStr || !minuteStr) return null;
  const year = Number(yearStr);
  const month = Number(monthStr);
  const day = Number(dayStr);
  const hour = Number(hourStr);
  const minute = Number(minuteStr);
  if (month < 1 || month > 12 || day < 1 || day > 31 || hour < 0 || hour > 23 || minute < 0 || minute > 59) {
    return null;
  }
  const parsed = new Date(year, month - 1, day, hour, minute, 0, 0);
  if (
    Number.isNaN(parsed.getTime()) ||
    parsed.getFullYear() !== year ||
    parsed.getMonth() !== month - 1 ||
    parsed.getDate() !== day ||
    parsed.getHours() !== hour ||
    parsed.getMinutes() !== minute
  ) {
    return null;
  }
  return parsed;
}

/** Upcoming studio bookings by week, with resource reservation and cancellation. */
export function CalendarView({ studioId }: { studioId: string }) {
  const queryClient = useQueryClient();
  const { can } = useCan(studioId);
  const staff = can('schedule.manage');
  const [baseDay, setBaseDay] = useState(startOfToday);
  const [weekOffset, setWeekOffset] = useState(0);

  const { from, to } = useMemo(() => {
    const start = new Date(baseDay);
    start.setDate(start.getDate() + weekOffset * 7);
    const end = new Date(start);
    end.setDate(end.getDate() + 7);
    return { from: start.toISOString(), to: end.toISOString() };
  }, [baseDay, weekOffset]);

  const bookings = useQuery(listBookingsOptions({ path: { studioId }, query: { from, to } }));
  const resources = useQuery(listResourcesOptions({ path: { studioId } }));
  const cancel = useMutation({
    ...cancelBookingMutation(),
    onSuccess: () => queryClient.invalidateQueries(),
  });

  const capNote = cappedNote(
    bookings.data,
    { order: 'first', noun: 'bookings' },
    "The rest of the week isn't listed.",
  );

  return (
    <ScrollView
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
      refreshControl={
        <RefreshControl
          refreshing={bookings.isRefetching}
          onRefresh={() => {
            const today = startOfToday();
            if (today.getTime() !== baseDay.getTime()) {
              setBaseDay(today);
            } else {
              void bookings.refetch();
            }
            void resources.refetch();
          }}
        />
      }
    >
      <Title>Calendar</Title>

      <View style={styles.weekBar}>
        <Button compact label="← Prev" onPress={() => setWeekOffset((w) => w - 1)} />
        <Body muted>
          {formatDate(from)} – {formatDate(new Date(new Date(to).getTime() - 1).toISOString())}
        </Body>
        <Button compact label="Next →" onPress={() => setWeekOffset((w) => w + 1)} />
      </View>

      {bookings.isPending && <Loading />}
      {bookings.isError && <ErrorText error={bookings.error} />}
      {cancel.isError && <ErrorText error={cancel.error} />}
      {bookings.isSuccess && bookings.data.items.length === 0 && (
        <Body muted>Nothing booked this week.</Body>
      )}
      {capNote && <Body muted>{capNote}</Body>}

      {bookings.data?.items.map((booking) => (
        <Card key={booking.id}>
          <SectionTitle>{booking.resourceName}</SectionTitle>
          <Body>
            {booking.title}
            {booking.accountName ? ` (${booking.accountName})` : ''}
          </Body>
          <Body muted>
            {formatDate(booking.startsAt)} {formatTime(booking.startsAt)}–{formatTime(booking.endsAt)}
          </Body>
          <View style={styles.rowWrap}>
            <Badge>{bookingKindLabel(booking.kind)}</Badge>
            {(staff || booking.mine) && (
              <Button
                compact
                label="Cancel booking"
                disabled={cancel.isPending}
                onPress={() => cancel.mutate({ path: { studioId, bookingId: booking.id } })}
              />
            )}
          </View>
        </Card>
      ))}

      {resources.isError && <ErrorText error={resources.error} />}
      {resources.isSuccess && resources.data.length > 0 && (
        <BookResourceCard
          studioId={studioId}
          staff={staff}
          resources={resources.data}
          defaultDate={toDateInput(baseDay)}
        />
      )}
    </ScrollView>
  );
}

function BookResourceCard({
  studioId,
  staff,
  resources,
  defaultDate,
}: {
  studioId: string;
  staff: boolean;
  resources: Resource[];
  defaultDate: string;
}) {
  const queryClient = useQueryClient();
  const [resourceId, setResourceId] = useState('');
  const [kind, setKind] = useState<BookingKind>('RESERVATION');
  const [date, setDate] = useState(defaultDate);
  const [startTime, setStartTime] = useState('10:00');
  const [endTime, setEndTime] = useState('12:00');
  const [title, setTitle] = useState('');
  const book = useMutation({
    ...createBookingMutation(),
    onSuccess: async () => {
      setTitle('');
      await queryClient.invalidateQueries();
    },
  });

  const chosenId = resourceId || resources[0]?.id || '';
  const selected = resources.find((r) => r.id === chosenId);

  const startsAtDate = parseLocalDateTime(date, startTime);
  const endsAtDate = parseLocalDateTime(date, endTime);
  const rangeValid =
    startsAtDate !== null &&
    endsAtDate !== null &&
    endsAtDate.getTime() > startsAtDate.getTime();

  function submit() {
    if (!chosenId || !startsAtDate || !endsAtDate || !rangeValid) return;
    book.mutate({
      path: { studioId },
      body: {
        resourceId: chosenId,
        kind: staff ? kind : 'RESERVATION',
        startsAt: startsAtDate.toISOString(),
        endsAt: endsAtDate.toISOString(),
        ...(title.trim() ? { title: title.trim() } : {}),
      },
    });
  }

  return (
    <Card>
      <SectionTitle>{staff ? 'Book a resource' : 'Reserve time'}</SectionTitle>

      <Body muted>Resource</Body>
      <View style={styles.rowWrap}>
        {resources.map((r) => (
          <Button
            key={r.id}
            compact
            primary={chosenId === r.id}
            selected={chosenId === r.id}
            label={r.downReason ? `${r.name} (out of service)` : r.name}
            onPress={() => setResourceId(r.id)}
          />
        ))}
      </View>

      {selected?.requiredCertificationName && (
        <Badge>Needs the “{selected.requiredCertificationName}” certification.</Badge>
      )}
      {selected?.downReason && (
        <Badge warning>
          {selected.name} is out of service: {selected.downReason}.
        </Badge>
      )}

      {staff && (
        <View style={styles.group}>
          <Body muted>Kind</Body>
          <View style={styles.rowWrap}>
            {staffBookingKinds.map((k) => (
              <Button
                key={k}
                compact
                primary={kind === k}
                selected={kind === k}
                label={bookingKindLabel(k)}
                onPress={() => setKind(k)}
              />
            ))}
          </View>
        </View>
      )}

      <Field label="Date (YYYY-MM-DD)" value={date} onChangeText={setDate} placeholder="2026-11-20" />
      <View style={styles.timeRow}>
        <Field label="From (HH:MM)" value={startTime} onChangeText={setStartTime} placeholder="10:00" />
        <Field label="Until (HH:MM)" value={endTime} onChangeText={setEndTime} placeholder="12:00" />
      </View>

      {staff && <Field label="Title (optional)" value={title} onChangeText={setTitle} maxLength={120} />}

      {!rangeValid && <Body muted>Enter a valid date and an end time after the start time.</Body>}
      {book.isError && <ErrorText error={book.error} />}
      {book.isSuccess && <Body>Booked.</Body>}

      <Button primary label="Book it" disabled={book.isPending || !chosenId || !rangeValid} onPress={submit} />
    </Card>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  weekBar: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 8 },
  rowWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, alignItems: 'center' },
  timeRow: { flexDirection: 'row', gap: 8 },
  group: { gap: 6 },
});
