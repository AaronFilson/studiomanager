import {
  endRateLimitBoostMutation,
  getMeOptions,
  getRateLimitsOptions,
  getRateLimitsQueryKey,
  listSettingChangesOptions,
  listSettingChangesQueryKey,
  resetNormalRateLimitsMutation,
  scheduleRateLimitBoostMutation,
  setNormalRateLimitsMutation,
  type ChannelRateLimits,
  type RateLimitBoost,
  type RateLimitOverview,
  type RateLimitValues,
  type SettingChange,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { useId, useState } from 'react';

import { requireSignIn } from '../auth/requireSignIn';
import { ErrorMessage } from '../components/ErrorMessage';

/** Running the service: live rate limits, boosts, and the history of every change (ADR 24). */
export const Route = createFileRoute('/admin')({
  beforeLoad: requireSignIn,
  pendingComponent: () => <p>Taking you to sign in…</p>,
  component: AdminPage,
});

function AdminPage() {
  const me = useQuery(getMeOptions());
  if (me.isPending) {
    return <p>Loading…</p>;
  }
  if (me.isError) {
    return <ErrorMessage error={me.error} />;
  }
  if (!me.data.platformAdmin) {
    return (
      <>
        <h1>Admin</h1>
        <p>This page is for the people who run Studio Manager.</p>
      </>
    );
  }
  return (
    <div className="stack">
      <h1>Admin</h1>
      <Limits />
      <History />
    </div>
  );
}

function useRefreshAdmin() {
  const queryClient = useQueryClient();
  return (overview: RateLimitOverview) => {
    queryClient.setQueryData(getRateLimitsQueryKey(), overview);
    void queryClient.invalidateQueries({ queryKey: listSettingChangesQueryKey() });
  };
}

function Limits() {
  const id = useId();
  const limits = useQuery(getRateLimitsOptions());
  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Limits</h2>
      {limits.isPending && <p>Loading the limits…</p>}
      {limits.isError && <ErrorMessage error={limits.error} />}
      {limits.data && (
        <>
          <p role="status">{describeLimits(limits.data)}</p>
          <NormalLimits overview={limits.data} />
          <Boost overview={limits.data} />
        </>
      )}
    </section>
  );
}

const channels: { key: keyof ChannelRateLimits; label: string }[] = [
  { key: 'mcp', label: 'MCP tool calls' },
  { key: 'token', label: 'API token calls' },
];

const fields: { key: keyof RateLimitValues; label: string }[] = [
  { key: 'burst', label: 'Burst' },
  { key: 'refillTokens', label: 'Refill (calls)' },
  { key: 'refillPeriodSeconds', label: 'Refill every (seconds)' },
  { key: 'dailyCap', label: 'Daily cap' },
];

function NormalLimits({ overview }: { overview: RateLimitOverview }) {
  const id = useId();
  const refresh = useRefreshAdmin();
  const [values, setValues] = useState<ChannelRateLimits>(overview.normal);
  const save = useMutation({ ...setNormalRateLimitsMutation(), onSuccess: refresh });
  const reset = useMutation({
    ...resetNormalRateLimitsMutation(),
    onSuccess: (after) => {
      setValues(after.normal);
      refresh(after);
    },
  });

  return (
    <form
      className="stack"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate({ body: values });
      }}
    >
      <h3>Normal limits</h3>
      <p>{overview.override ? 'Changed here, overriding the configured defaults.' : 'The configured defaults.'}</p>
      <table>
        <caption>Limits per person, per studio</caption>
        <thead>
          <tr>
            <th scope="col" />
            {fields.map((field) => (
              <th key={field.key} scope="col">
                {field.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {channels.map((channel) => (
            <tr key={channel.key}>
              <th scope="row">{channel.label}</th>
              {fields.map((field) => (
                <td key={field.key}>
                  <input
                    type="number"
                    min={1}
                    required
                    aria-label={`${channel.label}: ${field.label}`}
                    id={`${id}-${channel.key}-${field.key}`}
                    value={values[channel.key][field.key]}
                    onChange={(e) =>
                      setValues({
                        ...values,
                        [channel.key]: { ...values[channel.key], [field.key]: Number(e.target.value) },
                      })
                    }
                  />
                  <div className="badge">default {overview.defaults[channel.key][field.key]}</div>
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      {save.isError && <ErrorMessage error={save.error} />}
      {reset.isError && <ErrorMessage error={reset.error} />}
      <div>
        <button type="submit" className="primary" disabled={save.isPending}>
          Save limits
        </button>{' '}
        {overview.override && (
          <button type="button" disabled={reset.isPending} onClick={() => reset.mutate({})}>
            Back to defaults
          </button>
        )}
      </div>
    </form>
  );
}

const presets = [
  { label: 'Demo: 30 minutes unlimited, then ease down over an hour', unlimited: 30, easing: 60 },
  { label: '15 minutes unlimited, no easing', unlimited: 15, easing: 0 },
];

function Boost({ overview }: { overview: RateLimitOverview }) {
  const id = useId();
  const refresh = useRefreshAdmin();
  const [unlimited, setUnlimited] = useState('30');
  const [easing, setEasing] = useState('60');
  const [startsAt, setStartsAt] = useState('');
  const [studioId, setStudioId] = useState('');
  const schedule = useMutation({ ...scheduleRateLimitBoostMutation(), onSuccess: refresh });
  const end = useMutation({ ...endRateLimitBoostMutation(), onSuccess: refresh });

  return (
    <form
      className="stack"
      onSubmit={(event) => {
        event.preventDefault();
        schedule.mutate({
          body: {
            unlimitedMinutes: Number(unlimited),
            easingMinutes: Number(easing),
            startsAt: startsAt ? new Date(startsAt).toISOString() : undefined,
            studioId: studioId.trim() || undefined,
          },
        });
      }}
    >
      <h3>Boost</h3>
      <p>
        Lifts the limits for a while: none at first, then 8×, 4× and 2× the normal limits, then normal. A boost lasts at
        most 4 hours, so it always ends on its own.
      </p>
      {overview.boost && (
        <div>
          <button type="button" className="danger" disabled={end.isPending} onClick={() => end.mutate({})}>
            End now
          </button>
        </div>
      )}
      {end.isError && <ErrorMessage error={end.error} />}
      <div>
        {presets.map((preset) => (
          <button
            key={preset.label}
            type="button"
            onClick={() => {
              setUnlimited(String(preset.unlimited));
              setEasing(String(preset.easing));
            }}
          >
            {preset.label}
          </button>
        ))}
      </div>
      <div className="field">
        <label htmlFor={`${id}-unlimited`}>Unlimited for (minutes)</label>
        <input
          id={`${id}-unlimited`}
          type="number"
          min={0}
          max={240}
          required
          value={unlimited}
          onChange={(e) => setUnlimited(e.target.value)}
        />
      </div>
      <div className="field">
        <label htmlFor={`${id}-easing`}>Then ease down over (minutes)</label>
        <input
          id={`${id}-easing`}
          type="number"
          min={0}
          max={240}
          required
          value={easing}
          onChange={(e) => setEasing(e.target.value)}
        />
      </div>
      <div className="field">
        <label htmlFor={`${id}-starts`}>Starts (leave empty for now)</label>
        <input id={`${id}-starts`} type="datetime-local" value={startsAt} onChange={(e) => setStartsAt(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor={`${id}-studio`}>Only this studio (its id; leave empty for every studio)</label>
        <input id={`${id}-studio`} value={studioId} onChange={(e) => setStudioId(e.target.value)} />
      </div>
      {schedule.isError && <ErrorMessage error={schedule.error} />}
      <div>
        <button type="submit" className="primary" disabled={schedule.isPending}>
          {startsAt ? 'Schedule boost' : 'Start boost'}
        </button>
      </div>
    </form>
  );
}

function History() {
  const id = useId();
  const changes = useQuery(listSettingChangesOptions());
  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>History</h2>
      {changes.isPending && <p>Loading the history…</p>}
      {changes.isError && <ErrorMessage error={changes.error} />}
      {changes.data && changes.data.length === 0 && <p>Nothing has been changed yet.</p>}
      {changes.data && changes.data.length > 0 && (
        <table>
          <caption>Every change, newest first</caption>
          <thead>
            <tr>
              <th scope="col">When</th>
              <th scope="col">Setting</th>
              <th scope="col">Change</th>
              <th scope="col">By</th>
            </tr>
          </thead>
          <tbody>
            {changes.data.map((change) => (
              <tr key={`${change.key}-${String(change.version)}`}>
                <td>{new Date(change.changedAt).toLocaleString()}</td>
                <td>{settingLabel(change.key)}</td>
                <td>{changeText(change)}</td>
                <td>{change.changedByName ?? change.changedById}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}

function settingLabel(key: string): string {
  switch (key) {
    case 'rate-limit.normal':
      return 'Normal limits';
    case 'rate-limit.boost':
      return 'Boost';
    default:
      return key;
  }
}

function changeText(change: SettingChange): string {
  const before = change.oldValue ? JSON.stringify(change.oldValue) : 'default';
  const after = change.newValue ? JSON.stringify(change.newValue) : 'default';
  return `${before} → ${after}`;
}

function clock(iso: string): string {
  return new Date(iso).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function minutesBetween(fromIso: string, toIso: string): number {
  return Math.ceil((new Date(toIso).getTime() - new Date(fromIso).getTime()) / 60_000);
}

/** What applies right now, in words: "Unlimited for 23 more minutes, then easing to normal by 15:40". */
function describeLimits(overview: RateLimitOverview): string {
  const boost: RateLimitBoost | undefined = overview.boost;
  if (!boost) {
    return 'Normal limits apply.';
  }
  const where = boost.studioId ? `in studio ${boost.studioId}` : 'in every studio';
  const now = new Date(overview.now).getTime();
  if (now < new Date(boost.startsAt).getTime()) {
    return `Normal limits apply. A boost ${where} starts at ${new Date(boost.startsAt).toLocaleString()}.`;
  }
  const step = boost.steps.find(
    (candidate) => now >= new Date(candidate.startsAt).getTime() && now < new Date(candidate.endsAt).getTime(),
  );
  if (!step) {
    return 'Normal limits apply.';
  }
  const back = boost.easingMinutes > 0 ? `easing to normal by ${clock(boost.endsAt)}` : `normal at ${clock(boost.endsAt)}`;
  switch (step.kind) {
    case 'UNLIMITED':
      return `Unlimited ${where} for ${String(minutesBetween(overview.now, step.endsAt))} more minutes, then ${back}.`;
    case 'EASING':
      return `Easing ${where}: ${String(step.multiplier ?? 1)}× the normal limits, back to normal by ${clock(boost.endsAt)}.`;
  }
}
