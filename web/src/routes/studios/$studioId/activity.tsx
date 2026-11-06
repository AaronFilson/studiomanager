import { getWeeklyActivityOptions, type ActivityWeek } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { formatDate, formatMoney } from '../../../labels';

export const Route = createFileRoute('/studios/$studioId/activity')({
  component: ActivityPage,
});

/** Weekly totals, built from the studio's event stream. */
function ActivityPage() {
  const { studioId } = Route.useParams();
  const activity = useQuery(getWeeklyActivityOptions({ path: { studioId }, query: { weeks: 12 } }));

  if (activity.isPending) {
    return <p>Loading activity…</p>;
  }
  if (activity.isError) {
    return <ErrorMessage error={activity.error} />;
  }
  const { streaming, weeks } = activity.data;

  return (
    <div className="stack">
      <h1>Activity</h1>
      <p>The last 12 weeks: firings, pieces fired, members joined and fees charged. Weeks start on Monday.</p>
      {!streaming && <p className="badge">Event streaming is off for this server, so nothing is being counted.</p>}
      {streaming && weeks.length === 0 && <p>Nothing counted yet. Totals appear a few seconds after things happen.</p>}
      {weeks.length > 0 && (
        <table>
          <thead>
            <tr>
              <th scope="col">Week of</th>
              <th scope="col">Firings</th>
              <th scope="col">Pieces fired</th>
              <th scope="col">Members joined</th>
              <th scope="col">Fees charged</th>
            </tr>
          </thead>
          <tbody>
            {weeks.map((week) => (
              <tr key={week.weekStart}>
                <th scope="row">{formatDate(`${week.weekStart}T12:00:00`)}</th>
                <td>
                  {week.firings}
                  {week.kilns.length > 0 && <span className="badge">{kilnText(week)}</span>}
                </td>
                <td>{week.piecesFired}</td>
                <td>{week.membersJoined}</td>
                <td>{week.fees.length === 0 ? '—' : feeText(week)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function kilnText(week: ActivityWeek): string {
  return week.kilns.map((k) => `${k.kiln} ${String(k.firings)}`).join(', ');
}

function feeText(week: ActivityWeek): string {
  return week.fees.map((f) => `${formatMoney(f.amount, f.currency)} (${String(f.count)})`).join(', ');
}
