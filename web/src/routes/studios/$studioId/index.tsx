import {
  getMeOptions,
  getStatementOptions,
  getStudioOptions,
  listMyMaintenanceTasksOptions,
  listMyNotificationsOptions,
  listMyNotificationsQueryKey,
  markNotificationReadMutation,
  sendNoticeMutation,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute, Link } from '@tanstack/react-router';
import { type SubmitEvent, useId, useState } from 'react';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { MaintenanceTaskRow } from '../../../components/MaintenanceTaskRow';
import { formatDateTime, formatMoney, isStaff, roleLabel, unitSystemLabel } from '../../../labels';

export const Route = createFileRoute('/studios/$studioId/')({
  component: StudioHome,
});

function StudioHome() {
  const { studioId } = Route.useParams();
  // The layout has already loaded the studio, so this reads the cache
  const { data: studio } = useSuspenseQuery(getStudioOptions({ path: { studioId } }));
  const countryName = new Intl.DisplayNames(undefined, { type: 'region' }).of(studio.country) ?? studio.country;

  return (
    <article className="stack">
      <h1>Hello, {studio.name}</h1>
      <Inbox studioId={studioId} />
      <MyTasks studioId={studioId} />
      <MyBalance studioId={studioId} currency={studio.currency} />
      {isStaff(studio.myRole) && <NoticeForm studioId={studioId} />}
      <dl className="facts">
        <dt>Your role</dt>
        <dd>{roleLabel(studio.myRole)}</dd>
        <dt>Members</dt>
        <dd>{studio.memberCount}</dd>
        <dt>Units</dt>
        <dd>{unitSystemLabel(studio.unitSystem)}</dd>
        <dt>Time zone</dt>
        <dd>{studio.timeZone}</dd>
        <dt>Country</dt>
        <dd>{countryName}</dd>
        <dt>Currency</dt>
        <dd>{studio.currency}</dd>
      </dl>
      <p>
        <Link to="/studios">All your studios</Link>
      </p>
    </article>
  );
}

function Inbox({ studioId }: { studioId: string }) {
  const queryClient = useQueryClient();
  const notifications = useQuery(listMyNotificationsOptions({ path: { studioId } }));
  const markRead = useMutation({
    ...markNotificationReadMutation(),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: listMyNotificationsQueryKey({ path: { studioId } }) }),
  });

  if (notifications.isError) {
    return <ErrorMessage error={notifications.error} />;
  }
  const unread = notifications.data?.filter((n) => !n.readAt) ?? [];
  if (unread.length === 0) {
    return null;
  }
  return (
    <section className="card stack" aria-label="Notifications">
      <h2>For you</h2>
      <ul className="studio-list">
        {unread.map((n) => (
          <li key={n.id}>
            <div>
              <strong>{n.title}</strong>
              <div className="pre">{n.body}</div>
              <div className="badge">{formatDateTime(n.createdAt)}</div>
            </div>
            <button type="button" onClick={() => markRead.mutate({ path: { studioId, notificationId: n.id } })}>
              Got it
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}

/** Maintenance assigned to this person, by name or through a duty such as monitor. */
function MyTasks({ studioId }: { studioId: string }) {
  const tasks = useQuery(listMyMaintenanceTasksOptions({ path: { studioId } }));
  if (tasks.isError) {
    return <ErrorMessage error={tasks.error} />;
  }
  if (!tasks.data?.length) {
    return null;
  }
  return (
    <section className="card stack" aria-label="Your maintenance tasks">
      <h2>Your maintenance tasks</h2>
      <ul className="studio-list">
        {tasks.data.map((task) => (
          <MaintenanceTaskRow key={task.id} studioId={studioId} task={task} showAsset />
        ))}
      </ul>
    </section>
  );
}

function MyBalance({ studioId, currency }: { studioId: string; currency: string }) {
  const me = useQuery(getMeOptions());
  const accountId = me.data?.account.id;
  const statement = useQuery({
    ...getStatementOptions({ path: { studioId, accountId: accountId ?? '' } }),
    enabled: accountId !== undefined,
  });
  if (!statement.isSuccess) {
    return null;
  }
  const owed = Number(statement.data.owed);
  const prepaid = statement.data.storedValues.filter((sv) => Number(sv.remaining) > 0);
  if (owed === 0 && prepaid.length === 0) {
    return null;
  }
  return (
    <section className="card stack" aria-label="Your account">
      <h2>Your account</h2>
      {owed > 0 && <p>You owe {formatMoney(statement.data.owed, currency)}.</p>}
      {owed < 0 && <p>You have {formatMoney(String(-owed), currency)} in credit.</p>}
      {prepaid.map((sv) => (
        <p key={sv.id}>
          {sv.label ?? sv.kind.replace('_', ' ').toLowerCase()}: {formatMoney(sv.remaining, currency)} left
        </p>
      ))}
    </section>
  );
}

function NoticeForm({ studioId }: { studioId: string }) {
  const id = useId();
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const send = useMutation({
    ...sendNoticeMutation(),
    onSuccess: () => {
      setTitle('');
      setBody('');
    },
  });

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    send.mutate({ path: { studioId }, body: { title: title.trim(), body: body.trim() } });
  }

  return (
    <form className="card stack" onSubmit={submit} aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Tell every member</h2>
      <div className="field">
        <label htmlFor={`${id}-subject`}>Title</label>
        <input id={`${id}-subject`} required maxLength={120} value={title} onChange={(e) => setTitle(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor={`${id}-body`}>Message</label>
        <textarea id={`${id}-body`} rows={3} maxLength={2000} value={body} onChange={(e) => setBody(e.target.value)} />
      </div>
      {send.isError && <ErrorMessage error={send.error} />}
      {send.isSuccess && <p aria-live="polite">Sent to {send.data.sent} members.</p>}
      <div>
        <button type="submit" className="primary" disabled={send.isPending}>
          Send notice
        </button>
      </div>
    </form>
  );
}
