import {
  createWebhookEndpointMutation,
  deleteWebhookEndpointMutation,
  listWebhookDeliveriesOptions,
  listWebhookEndpointsOptions,
  listWebhookEndpointsQueryKey,
  listWebhookEventTypesOptions,
  resendWebhookDeliveryMutation,
  rotateWebhookSecretMutation,
  testWebhookEndpointMutation,
  updateWebhookEndpointMutation,
  type WebhookDelivery,
  type WebhookEndpoint,
  type WebhookEventType,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { useId, useState } from 'react';

import { ErrorMessage } from '../../../components/ErrorMessage';

export const Route = createFileRoute('/studios/$studioId/webhooks')({
  component: WebhooksPage,
});

const subscribable: WebhookEventType[] = ['pieces.ready', 'fee.charged', 'member.joined'];

function WebhooksPage() {
  const { studioId } = Route.useParams();
  const endpoints = useQuery(listWebhookEndpointsOptions({ path: { studioId } }));
  const [secret, setSecret] = useState<string>();

  return (
    <div className="stack">
      <h1>Webhooks</h1>
      <p>
        Send what happens in the studio to your own tools, such as Zapier, Make or a spreadsheet script. Each event is
        POSTed as JSON and signed, so the receiver can check it came from here.
      </p>
      <ZapierHowTo />
      {secret && <SecretCard secret={secret} onDone={() => setSecret(undefined)} />}
      {endpoints.isError && <ErrorMessage error={endpoints.error} />}
      {endpoints.data?.map((endpoint) => (
        <EndpointCard key={endpoint.id} studioId={studioId} endpoint={endpoint} onSecret={setSecret} />
      ))}
      <AddEndpointForm studioId={studioId} onCreated={setSecret} />
      <EventTypes studioId={studioId} />
    </div>
  );
}

function ZapierHowTo() {
  return (
    <details className="card">
      <summary>Connect Zapier</summary>
      <ol>
        <li>In Zapier, make a Zap whose trigger is “Webhooks by Zapier”, then “Catch Hook”.</li>
        <li>Copy the URL it gives you and add it below, with the events you want.</li>
        <li>Press “Send test” here, then “Test trigger” in Zapier. The fields appear to map.</li>
      </ol>
      <p className="badge">
        A delivery can arrive more than once. Each has a <code>webhook-id</code> header that stays the same, so a Zap can
        skip ones it has seen.
      </p>
    </details>
  );
}

function SecretCard({ secret, onDone }: { secret: string; onDone: () => void }) {
  const [copied, setCopied] = useState(false);
  return (
    <section className="card stack" aria-label="Signing secret">
      <h2>Signing secret</h2>
      <p>Copy it now: it won&apos;t be shown again. Your receiver uses it to check each delivery&apos;s signature.</p>
      <code>{secret}</code>
      <div className="row">
        <button
          type="button"
          onClick={() => {
            void navigator.clipboard.writeText(secret).then(() => setCopied(true));
          }}
        >
          {copied ? 'Copied' : 'Copy'}
        </button>
        <button type="button" onClick={onDone}>
          Done
        </button>
      </div>
    </section>
  );
}

function AddEndpointForm({ studioId, onCreated }: { studioId: string; onCreated: (secret: string) => void }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [url, setUrl] = useState('');
  const [description, setDescription] = useState('');
  const [types, setTypes] = useState<WebhookEventType[]>(['pieces.ready']);
  const create = useMutation({
    ...createWebhookEndpointMutation(),
    onSuccess: (created) => {
      onCreated(created.secret);
      setUrl('');
      setDescription('');
      void queryClient.invalidateQueries({ queryKey: listWebhookEndpointsQueryKey({ path: { studioId } }) });
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
          body: { url: url.trim(), eventTypes: types, ...(description.trim() ? { description: description.trim() } : {}) },
        });
      }}
    >
      <h2 id={`${id}-title`}>Add an endpoint</h2>
      <div className="field">
        <label htmlFor={`${id}-url`}>URL</label>
        <input
          id={`${id}-url`}
          type="url"
          required
          maxLength={2000}
          placeholder="https://hooks.zapier.com/hooks/catch/…"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
        />
      </div>
      <div className="field">
        <label htmlFor={`${id}-description`}>Description (optional)</label>
        <input
          id={`${id}-description`}
          maxLength={200}
          placeholder="Zapier: ready email"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>
      <EventChoices value={types} onChange={setTypes} name={id} />
      <p className="badge">
        Payloads include members&apos; names and email addresses, so only add a receiver the studio controls.
      </p>
      {create.isError && <ErrorMessage error={create.error} />}
      <div>
        <button type="submit" className="primary" disabled={create.isPending || types.length === 0}>
          Add endpoint
        </button>
      </div>
    </form>
  );
}

function EventChoices({
  value,
  onChange,
  name,
}: {
  value: WebhookEventType[];
  onChange: (types: WebhookEventType[]) => void;
  name: string;
}) {
  return (
    <fieldset className="field">
      <legend>Events</legend>
      {subscribable.map((type) => (
        <label key={type} className="choice">
          <input
            type="checkbox"
            name={`${name}-events`}
            checked={value.includes(type)}
            onChange={(e) => onChange(e.target.checked ? [...value, type] : value.filter((t) => t !== type))}
          />
          {eventLabel(type)}
        </label>
      ))}
    </fieldset>
  );
}

function EndpointCard({
  studioId,
  endpoint,
  onSecret,
}: {
  studioId: string;
  endpoint: WebhookEndpoint;
  onSecret: (secret: string) => void;
}) {
  const id = useId();
  const queryClient = useQueryClient();
  const [showLog, setShowLog] = useState(false);
  const refresh = () => queryClient.invalidateQueries();
  const path = { studioId, endpointId: endpoint.id };
  const update = useMutation({ ...updateWebhookEndpointMutation(), onSuccess: refresh });
  const remove = useMutation({ ...deleteWebhookEndpointMutation(), onSuccess: refresh });
  const rotate = useMutation({ ...rotateWebhookSecretMutation(), onSuccess: (r) => onSecret(r.secret) });
  const test = useMutation({ ...testWebhookEndpointMutation(), onSuccess: refresh });

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>{endpoint.description ?? endpoint.url}</h2>
      {endpoint.description && <p className="badge">{endpoint.url}</p>}
      <p>
        {endpoint.enabled ? 'On' : 'Off'} · {endpoint.eventTypes.map(eventLabel).join(', ')}
        {endpoint.consecutiveFailures > 0 && ` · ${String(endpoint.consecutiveFailures)} failed in a row`}
      </p>
      {!endpoint.enabled && endpoint.disabledReason && (
        <p role="alert" className="error">
          Turned off: {endpoint.disabledReason}
        </p>
      )}
      {endpoint.previousSecretValidUntil && (
        <p className="badge">
          The old secret also signs until {new Date(endpoint.previousSecretValidUntil).toLocaleString()}.
        </p>
      )}
      {test.isSuccess && <p aria-live="polite">Test: {deliveryOutcome(test.data)}</p>}
      {[update, remove, rotate, test].map((m, i) => m.isError && <ErrorMessage key={i} error={m.error} />)}
      <div className="row">
        <button type="button" disabled={test.isPending || !endpoint.enabled} onClick={() => test.mutate({ path })}>
          {test.isPending ? 'Sending…' : 'Send test'}
        </button>
        <button type="button" onClick={() => update.mutate({ path, body: { enabled: !endpoint.enabled } })}>
          {endpoint.enabled ? 'Turn off' : 'Turn on'}
        </button>
        <button type="button" onClick={() => rotate.mutate({ path })}>
          New secret
        </button>
        <button type="button" onClick={() => setShowLog(!showLog)} aria-expanded={showLog}>
          {showLog ? 'Hide log' : 'Delivery log'}
        </button>
        <button type="button" className="danger" onClick={() => remove.mutate({ path })}>
          Delete
        </button>
      </div>
      {showLog && <DeliveryLog studioId={studioId} endpointId={endpoint.id} />}
    </section>
  );
}

function DeliveryLog({ studioId, endpointId }: { studioId: string; endpointId: string }) {
  const queryClient = useQueryClient();
  const deliveries = useQuery(listWebhookDeliveriesOptions({ path: { studioId, endpointId } }));
  const resend = useMutation({ ...resendWebhookDeliveryMutation(), onSuccess: () => queryClient.invalidateQueries() });

  if (deliveries.isError) {
    return <ErrorMessage error={deliveries.error} />;
  }
  if (deliveries.data?.length === 0) {
    return <p>Nothing sent in the last 30 days.</p>;
  }
  return (
    <table>
      <caption>Last 30 days</caption>
      <thead>
        <tr>
          <th scope="col">When</th>
          <th scope="col">Event</th>
          <th scope="col">Result</th>
          <th scope="col" />
        </tr>
      </thead>
      <tbody>
        {deliveries.data?.map((d) => (
          <tr key={d.id}>
            <td>{new Date(d.createdAt).toLocaleString()}</td>
            <td>{eventLabel(d.eventType)}</td>
            <td>{deliveryOutcome(d)}</td>
            <td>
              <button
                type="button"
                disabled={resend.isPending}
                onClick={() => resend.mutate({ path: { studioId, deliveryId: d.id } })}
              >
                Resend
              </button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function EventTypes({ studioId }: { studioId: string }) {
  const types = useQuery(listWebhookEventTypesOptions({ path: { studioId } }));
  return (
    <details className="card">
      <summary>What each event sends</summary>
      {types.data?.map((t) => (
        <div key={t.type} className="stack">
          <h3>
            <code>{t.type}</code>
          </h3>
          <p>{t.description}</p>
          <pre>{JSON.stringify(t.example, null, 2)}</pre>
        </div>
      ))}
    </details>
  );
}

function eventLabel(type: WebhookEventType): string {
  switch (type) {
    case 'pieces.ready':
      return 'Pieces ready';
    case 'fee.charged':
      return 'Fee charged';
    case 'member.joined':
      return 'Member joined';
    case 'ping':
      return 'Test';
  }
}

function deliveryOutcome(d: WebhookDelivery): string {
  const last = d.attemptLog[0];
  const answer = last ? (last.statusCode !== undefined ? `HTTP ${String(last.statusCode)}` : (last.error ?? '')) : '';
  switch (d.status) {
    case 'DELIVERED':
      return `Delivered (${answer})`;
    case 'FAILED':
      return `Failed: ${answer}`;
    case 'PENDING':
      return d.attempts === 0
        ? 'Sending…'
        : `Failed: ${answer}. Retrying${d.nextAttemptAt ? ` at ${new Date(d.nextAttemptAt).toLocaleTimeString()}` : ''}`;
  }
}
