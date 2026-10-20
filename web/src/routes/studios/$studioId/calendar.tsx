import {
  type BookingKind,
  cancelBookingMutation,
  createBookingMutation,
  createResourceMutation,
  getStudioOptions,
  listBookingsOptions,
  listCertificationTypesOptions,
  listResourcesOptions,
  listResourcesQueryKey,
  type ResourceKind,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { type SubmitEvent, useId, useMemo, useState } from 'react';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { formatDate, formatTime, isStaff } from '../../../labels';

export const Route = createFileRoute('/studios/$studioId/calendar')({
  component: CalendarPage,
});

const staffKinds: BookingKind[] = ['RESERVATION', 'CLASS', 'EVENT', 'RENTAL', 'FIRING', 'MAINTENANCE', 'CLOSURE'];
const kindLabels: Record<BookingKind, string> = {
  RESERVATION: 'Reservation',
  CLASS: 'Class',
  EVENT: 'Event',
  RENTAL: 'Rental',
  FIRING: 'Firing',
  MAINTENANCE: 'Maintenance',
  CLOSURE: 'Closure',
};

function startOfDay(date: Date): Date {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  return d;
}

function CalendarPage() {
  const { studioId } = Route.useParams();
  const { data: studio } = useSuspenseQuery(getStudioOptions({ path: { studioId } }));
  const staff = isStaff(studio.myRole);
  const [weekOffset, setWeekOffset] = useState(0);
  const { from, to } = useMemo(() => {
    const start = startOfDay(new Date());
    start.setDate(start.getDate() + weekOffset * 7);
    const end = new Date(start);
    end.setDate(end.getDate() + 7);
    return { from: start.toISOString(), to: end.toISOString() };
  }, [weekOffset]);
  const bookings = useQuery(listBookingsOptions({ path: { studioId }, query: { from, to } }));
  const resources = useQuery(listResourcesOptions({ path: { studioId } }));
  const queryClient = useQueryClient();
  const cancel = useMutation({ ...cancelBookingMutation(), onSuccess: () => queryClient.invalidateQueries() });

  return (
    <div className="stack">
      <h1>Calendar</h1>
      <div className="row">
        <button type="button" onClick={() => setWeekOffset((w) => w - 1)}>
          ← Previous week
        </button>
        <span>
          {formatDate(from)} – {formatDate(new Date(new Date(to).getTime() - 1).toISOString())}
        </span>
        <button type="button" onClick={() => setWeekOffset((w) => w + 1)}>
          Next week →
        </button>
      </div>
      {bookings.isError && <ErrorMessage error={bookings.error} />}
      {bookings.isSuccess && bookings.data.length === 0 && <p>Nothing booked this week.</p>}
      <ul className="studio-list">
        {bookings.data?.map((b) => (
          <li key={b.id}>
            <span>
              {formatDate(b.startsAt)} {formatTime(b.startsAt)}–{formatTime(b.endsAt)} · <strong>{b.resourceName}</strong>{' '}
              · {b.title}
              {b.accountName && ` (${b.accountName})`}
            </span>
            {(staff || b.mine) && (
              <button type="button" onClick={() => cancel.mutate({ path: { studioId, bookingId: b.id } })}>
                Cancel
              </button>
            )}
          </li>
        ))}
      </ul>
      {cancel.isError && <ErrorMessage error={cancel.error} />}
      {resources.isSuccess && resources.data.length > 0 && <BookingForm studioId={studioId} staff={staff} />}
      {staff && <ResourceForm studioId={studioId} />}
    </div>
  );
}

function BookingForm({ studioId, staff }: { studioId: string; staff: boolean }) {
  const id = useId();
  const queryClient = useQueryClient();
  const resources = useQuery(listResourcesOptions({ path: { studioId } }));
  const [resourceId, setResourceId] = useState('');
  const [kind, setKind] = useState<BookingKind>('RESERVATION');
  const [title, setTitle] = useState('');
  const [start, setStart] = useState('');
  const [end, setEnd] = useState('');
  const book = useMutation({ ...createBookingMutation(), onSuccess: () => queryClient.invalidateQueries() });
  const chosen = resourceId || resources.data?.[0]?.id || '';
  const resource = resources.data?.find((r) => r.id === chosen);

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    book.mutate({
      path: { studioId },
      body: {
        resourceId: chosen,
        kind,
        startsAt: new Date(start).toISOString(),
        endsAt: new Date(end).toISOString(),
        ...(title.trim() ? { title: title.trim() } : {}),
      },
    });
  }

  return (
    <form className="card stack" onSubmit={submit} aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>{staff ? 'Book' : 'Reserve time'}</h2>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-resource`}>What</label>
          <select id={`${id}-resource`} value={chosen} onChange={(e) => setResourceId(e.target.value)}>
            {resources.data?.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name}
              </option>
            ))}
          </select>
        </div>
        {staff && (
          <div className="field">
            <label htmlFor={`${id}-kind`}>Kind</label>
            <select id={`${id}-kind`} value={kind} onChange={(e) => setKind(e.target.value as BookingKind)}>
              {staffKinds.map((k) => (
                <option key={k} value={k}>
                  {kindLabels[k]}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>
      {resource?.requiredCertificationName && (
        <p className="badge">Needs the “{resource.requiredCertificationName}” certification.</p>
      )}
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-start`}>From</label>
          <input id={`${id}-start`} type="datetime-local" required value={start} onChange={(e) => setStart(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-end`}>Until</label>
          <input id={`${id}-end`} type="datetime-local" required value={end} onChange={(e) => setEnd(e.target.value)} />
        </div>
      </div>
      {staff && (
        <div className="field">
          <label htmlFor={`${id}-name`}>Title (optional)</label>
          <input id={`${id}-name`} maxLength={120} value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
      )}
      {book.isError && <ErrorMessage error={book.error} />}
      {book.isSuccess && <p aria-live="polite">Booked.</p>}
      <div>
        <button type="submit" className="primary" disabled={book.isPending}>
          Book it
        </button>
      </div>
    </form>
  );
}

const resourceKinds: ResourceKind[] = ['WHEEL_ROOM', 'KILN', 'GLAZE_ROOM', 'STUDIO', 'EQUIPMENT', 'OTHER'];
const resourceKindLabels: Record<ResourceKind, string> = {
  WHEEL_ROOM: 'Wheel room',
  KILN: 'Kiln',
  GLAZE_ROOM: 'Glaze room',
  STUDIO: 'Whole studio',
  EQUIPMENT: 'Equipment',
  OTHER: 'Other',
};

function ResourceForm({ studioId }: { studioId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const types = useQuery(listCertificationTypesOptions({ path: { studioId } }));
  const [name, setName] = useState('');
  const [kind, setKind] = useState<ResourceKind>('WHEEL_ROOM');
  const [capacity, setCapacity] = useState('1');
  const [certification, setCertification] = useState('');
  const create = useMutation({
    ...createResourceMutation(),
    onSuccess: async () => {
      setName('');
      await queryClient.invalidateQueries({ queryKey: listResourcesQueryKey({ path: { studioId } }) });
    },
  });

  return (
    <form
      className="card stack"
      aria-labelledby={`${id}-title`}
      onSubmit={(event) => {
        event.preventDefault();
        create.mutate({
          path: { studioId },
          body: {
            name: name.trim(),
            kind,
            capacity: Number(capacity),
            ...(certification ? { requiredCertification: certification } : {}),
          },
        });
      }}
    >
      <h2 id={`${id}-title`}>Add something bookable</h2>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-name`}>Name</label>
          <input id={`${id}-name`} required maxLength={60} value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-kind`}>Kind</label>
          <select id={`${id}-kind`} value={kind} onChange={(e) => setKind(e.target.value as ResourceKind)}>
            {resourceKinds.map((k) => (
              <option key={k} value={k}>
                {resourceKindLabels[k]}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-capacity`}>Places</label>
          <input id={`${id}-capacity`} type="number" min={1} max={200} value={capacity} onChange={(e) => setCapacity(e.target.value)} />
        </div>
      </div>
      <div className="field">
        <label htmlFor={`${id}-cert`}>Needs certification</label>
        <select id={`${id}-cert`} value={certification} onChange={(e) => setCertification(e.target.value)}>
          <option value="">None</option>
          {types.data?.map((t) => (
            <option key={t.id} value={t.id}>
              {t.name}
            </option>
          ))}
        </select>
      </div>
      {create.isError && <ErrorMessage error={create.error} />}
      <div>
        <button type="submit" className="primary" disabled={create.isPending}>
          Add
        </button>
      </div>
    </form>
  );
}
