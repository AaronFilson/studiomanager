import {
  assignShiftMutation,
  createShiftsMutation,
  type Duty,
  getHoursOptions,
  getMeOptions,
  getStudioOptions,
  listMembersOptions,
  listShiftsOptions,
  markShiftWorkedMutation,
  offerShiftMutation,
  type Shift,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { type SubmitEvent, useId, useMemo, useState } from 'react';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { dutyLabel, formatDate, formatTime, isStaff } from '../../../labels';

export const Route = createFileRoute('/studios/$studioId/roster')({
  component: RosterPage,
});

const duties: Duty[] = ['MONITOR', 'WORK_EXCHANGE', 'TECHNICIAN', 'VOLUNTEER_OFFICER', 'INSTRUCTOR'];

function hours(minutes: number): string {
  const sign = minutes < 0 ? '−' : '';
  const abs = Math.abs(minutes);
  return `${sign}${String(Math.floor(abs / 60))} h ${String(abs % 60).padStart(2, '0')} min`;
}

function RosterPage() {
  const { studioId } = Route.useParams();
  const { data: studio } = useSuspenseQuery(getStudioOptions({ path: { studioId } }));
  const me = useQuery(getMeOptions());
  const staff = isStaff(studio.myRole);
  const { from, to, now } = useMemo(() => {
    const opened = new Date();
    const start = new Date(opened);
    start.setHours(0, 0, 0, 0);
    const end = new Date(start);
    end.setDate(end.getDate() + 28);
    return { from: start.toISOString(), to: end.toISOString(), now: opened.getTime() };
  }, []);
  const shifts = useQuery(listShiftsOptions({ path: { studioId }, query: { from, to } }));
  const myId = me.data?.account.id;

  return (
    <div className="stack">
      <h1>Roster</h1>
      {myId && <MyHours studioId={studioId} accountId={myId} />}
      {shifts.isError && <ErrorMessage error={shifts.error} />}
      {shifts.isSuccess && shifts.data.length === 0 && <p>No shifts in the next four weeks.</p>}
      <ul className="studio-list">
        {shifts.data?.map((shift) => (
          <ShiftRow key={shift.id} studioId={studioId} shift={shift} staff={staff} myId={myId} now={now} />
        ))}
      </ul>
      {staff && <ShiftForm studioId={studioId} />}
    </div>
  );
}

function ShiftRow({
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
  /** When the page opened, in milliseconds */
  now: number;
}) {
  const queryClient = useQueryClient();
  const members = useQuery({ ...listMembersOptions({ path: { studioId } }), enabled: staff });
  const onSuccess = () => queryClient.invalidateQueries();
  const assign = useMutation({ ...assignShiftMutation(), onSuccess });
  const worked = useMutation({ ...markShiftWorkedMutation(), onSuccess });
  const offer = useMutation({ ...offerShiftMutation(), onSuccess });
  const error = assign.error ?? worked.error ?? offer.error;
  const mine = myId !== undefined && shift.accountId === myId;
  const upcoming = !shift.workedAt && new Date(shift.endsAt).getTime() > now;

  return (
    <li>
      <div>
        <strong>{shift.title}</strong> · {formatDate(shift.startsAt)} {formatTime(shift.startsAt)}–{formatTime(shift.endsAt)}
        {shift.duty && <span className="badge"> · {dutyLabel(shift.duty)}</span>}
        <div>
          {shift.accountName ?? (shift.open ? <span className="warning">Open</span> : 'Taken')}
          {shift.offered && <span className="warning"> · needs cover</span>}
          {shift.workedAt && <span className="badge"> · worked, hours credited</span>}
        </div>
        {error && <ErrorMessage error={error} />}
      </div>
      <div className="row">
        {shift.open && myId && (
          <button
            type="button"
            onClick={() => assign.mutate({ path: { studioId, shiftId: shift.id }, body: { accountId: myId, expectedVersion: shift.version } })}
          >
            Take it
          </button>
        )}
        {shift.offered && !mine && myId && upcoming && (
          <button
            type="button"
            onClick={() => assign.mutate({ path: { studioId, shiftId: shift.id }, body: { accountId: myId, expectedVersion: shift.version } })}
          >
            Cover it
          </button>
        )}
        {mine && upcoming && (
          <button
            type="button"
            onClick={() =>
              offer.mutate({ path: { studioId, shiftId: shift.id }, body: { offered: !shift.offered, expectedVersion: shift.version } })
            }
          >
            {shift.offered ? 'I can do it after all' : 'Ask for cover'}
          </button>
        )}
        {staff && !shift.workedAt && (
          <select
            aria-label={`Assign ${shift.title}`}
            value={shift.accountId ?? ''}
            onChange={(e) =>
              assign.mutate({
                path: { studioId, shiftId: shift.id },
                body: { expectedVersion: shift.version, ...(e.target.value ? { accountId: e.target.value } : {}) },
              })
            }
          >
            <option value="">Nobody</option>
            {members.data?.map((m) => (
              <option key={m.accountId} value={m.accountId}>
                {m.displayName ?? m.accountId}
              </option>
            ))}
          </select>
        )}
        {staff && shift.accountId && !shift.workedAt && (
          <button
            type="button"
            onClick={() => worked.mutate({ path: { studioId, shiftId: shift.id }, body: { expectedVersion: shift.version } })}
          >
            Mark worked
          </button>
        )}
      </div>
    </li>
  );
}

function MyHours({ studioId, accountId }: { studioId: string; accountId: string }) {
  const ledger = useQuery(getHoursOptions({ path: { studioId, accountId } }));
  if (!ledger.isSuccess || ledger.data.entries.length === 0) {
    return null;
  }
  return (
    <section className="card stack" aria-label="My hours">
      <h2>My hours: {hours(ledger.data.totalMinutes)}</h2>
      <ol className="timeline">
        {ledger.data.entries.slice(0, 10).map((entry) => (
          <li key={entry.id}>
            {hours(entry.minutes)} · {entry.note ?? 'Shift'} · {formatDate(entry.creditedAt)}
          </li>
        ))}
      </ol>
    </section>
  );
}

function ShiftForm({ studioId }: { studioId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [title, setTitle] = useState('Open studio monitor');
  const [duty, setDuty] = useState<Duty | ''>('MONITOR');
  const [start, setStart] = useState('');
  const [minutes, setMinutes] = useState('180');
  const [weeks, setWeeks] = useState('10');
  const create = useMutation({ ...createShiftsMutation(), onSuccess: () => queryClient.invalidateQueries() });

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    create.mutate({
      path: { studioId },
      body: {
        title: title.trim(),
        firstStartsAt: new Date(start).toISOString(),
        minutes: Number(minutes),
        weeks: Number(weeks),
        ...(duty ? { duty } : {}),
      },
    });
  }

  return (
    <form className="card stack" onSubmit={submit} aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Add a weekly shift</h2>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-name`}>Title</label>
          <input id={`${id}-name`} required maxLength={120} value={title} onChange={(e) => setTitle(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-duty`}>For</label>
          <select id={`${id}-duty`} value={duty} onChange={(e) => setDuty(e.target.value as Duty | '')}>
            <option value="">Anyone</option>
            {duties.map((d) => (
              <option key={d} value={d}>
                {dutyLabel(d)}
              </option>
            ))}
          </select>
        </div>
      </div>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-start`}>First one starts</label>
          <input id={`${id}-start`} type="datetime-local" required value={start} onChange={(e) => setStart(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-minutes`}>Minutes</label>
          <input id={`${id}-minutes`} type="number" min={15} max={1440} value={minutes} onChange={(e) => setMinutes(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-weeks`}>Weeks</label>
          <input id={`${id}-weeks`} type="number" min={1} max={26} value={weeks} onChange={(e) => setWeeks(e.target.value)} />
        </div>
      </div>
      {create.isError && <ErrorMessage error={create.error} />}
      {create.isSuccess && <p aria-live="polite">Added {create.data.length} shifts.</p>}
      <div>
        <button type="submit" className="primary" disabled={create.isPending}>
          Add shifts
        </button>
      </div>
    </form>
  );
}
