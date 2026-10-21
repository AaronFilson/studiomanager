import { getTodayOptions } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { createFileRoute, Link } from '@tanstack/react-router';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { dueText, formatDate, formatTime } from '../../../labels';

export const Route = createFileRoute('/studios/$studioId/today')({
  component: TodayPage,
});

function count(n: number, one: string, many: string): string {
  return `${String(n)} ${n === 1 ? one : many}`;
}

/** The handover view: everything whoever is covering the studio needs, on one page. */
function TodayPage() {
  const { studioId } = Route.useParams();
  const today = useQuery({ ...getTodayOptions({ path: { studioId } }), refetchInterval: 60_000 });

  if (today.isPending) {
    return <p>Loading today…</p>;
  }
  if (today.isError) {
    return <ErrorMessage error={today.error} />;
  }
  const t = today.data;
  const attention = [
    ...t.outOfService.map((k) => `${k.name} is out of service: ${k.downReason ?? ''}`),
    ...t.maintenanceDue.map((task) => `${task.assetName}: ${task.title} ${dueText(task)}`),
    ...(t.pastHold > 0 ? [`${count(t.pastHold, 'ready piece is', 'ready pieces are')} past their hold`] : []),
    ...(t.unclaimed > 0 ? [`${count(t.unclaimed, 'piece is', 'pieces are')} unclaimed`] : []),
    ...t.lowStock.map((item) => `${item.name} is low (${item.onHand} left) and not on order`),
    ...(t.openPurchaseRequests > 0 ? [`${count(t.openPurchaseRequests, 'purchase request is', 'purchase requests are')} open`] : []),
    ...(t.uncoveredShifts.length > 0
      ? [`${count(t.uncoveredShifts.length, 'shift', 'shifts')} in the next week ${t.uncoveredShifts.length === 1 ? 'has' : 'have'} nobody on ${t.uncoveredShifts.length === 1 ? 'it' : 'them'}`]
      : []),
  ];

  return (
    <div className="stack">
      <h1>Today, {formatDate(`${t.date}T12:00:00`)}</h1>

      {attention.length > 0 && (
        <section className="card stack" aria-label="Needs attention">
          <h2>Needs attention</h2>
          <ul>
            {attention.map((line) => (
              <li key={line}>{line}</li>
            ))}
          </ul>
        </section>
      )}

      <section className="card stack" aria-label="Kilns">
        <h2>Kilns</h2>
        <ul className="studio-list">
          {t.kilns.map((kiln) => {
            const firing = t.openFirings.find((f) => f.kilnId === kiln.id);
            return (
              <li key={kiln.id}>
                <Link to="/studios/$studioId/kilns/$kilnId" params={{ studioId, kilnId: kiln.id }}>
                  {kiln.name}
                </Link>
                <span className="badge">
                  {!kiln.available
                    ? 'out of service'
                    : firing
                      ? `${firing.status === 'FIRING' ? 'firing' : 'loading'} ${firing.kind.toLowerCase()}, ${count(firing.pieceCount, 'piece', 'pieces')}`
                      : 'empty'}
                </span>
              </li>
            );
          })}
        </ul>
        <p>
          Waiting: {t.waitingForBisque} for bisque, {t.waitingForGlaze} for glaze. Ready for pickup: {t.readyForPickup}.
        </p>
      </section>

      <section className="card stack" aria-label="On today">
        <h2>On today</h2>
        {t.shifts.length === 0 && t.bookings.length === 0 && <p>Nothing on the calendar or roster today.</p>}
        <ul>
          {t.shifts.map((shift) => (
            <li key={shift.id}>
              {formatTime(shift.startsAt)}–{formatTime(shift.endsAt)} {shift.title}:{' '}
              {shift.accountName ?? <strong className="warning">nobody yet</strong>}
            </li>
          ))}
          {t.bookings.map((booking) => (
            <li key={booking.id}>
              {formatTime(booking.startsAt)}–{formatTime(booking.endsAt)} {booking.resourceName}: {booking.title}
              {booking.accountName && ` (${booking.accountName})`}
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
