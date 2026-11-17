import {
  deleteAiKeyMutation,
  getAiKeyOptions,
  getAiKeyQueryKey,
  isApiError,
  setAiKeyMutation,
  setPlatformReaderConsentMutation,
  testAiKeyMutation,
  type AiKeyStatus,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useId, useState } from 'react';

import { ErrorMessage } from './ErrorMessage';

/**
 * The studio's own Anthropic key, for reading supplier invoices, and the owner's consent to the
 * platform's own readers. Owner only. The key goes in and never comes back: only its last four
 * characters are shown. The consent is its own switch, so replacing the key doesn't change it.
 */
export function AiKeyCard({ studioId }: { studioId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const key = useQuery({ ...getAiKeyOptions({ path: { studioId } }), retry: false });
  const [apiKey, setApiKey] = useState('');
  const [platformReaders, setPlatformReaders] = useState(false);
  const update = (status: AiKeyStatus | null) =>
    queryClient.setQueryData(getAiKeyQueryKey({ path: { studioId } }), status ?? undefined);
  const save = useMutation({
    ...setAiKeyMutation(),
    onSuccess: (status) => {
      setApiKey('');
      update(status);
    },
  });
  const test = useMutation({ ...testAiKeyMutation(), onSuccess: update });
  const consent = useMutation({ ...setPlatformReaderConsentMutation(), onSuccess: update });
  const remove = useMutation({
    ...deleteAiKeyMutation(),
    onSuccess: () => queryClient.removeQueries({ queryKey: getAiKeyQueryKey({ path: { studioId } }) }),
  });
  const none = key.isError && isApiError(key.error) && key.error.status === 404;

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>AI for invoices</h2>
      <p>
        Add the studio&apos;s own Anthropic API key to have supplier invoices read for you. Invoices are sent to Anthropic
        under your account and its terms. Nothing changes stock until staff check and confirm.
      </p>
      {key.isError && !none && <ErrorMessage error={key.error} />}
      {key.data && (
        <div className="stack">
          <p>
            Anthropic key ending …{key.data.lastFour}: <strong>{statusText(key.data)}</strong>
            {key.data.reason && <span className="badge"> {key.data.reason}</span>}
          </p>
          <div className="row">
            <button type="button" disabled={test.isPending} onClick={() => test.mutate({ path: { studioId } })}>
              {test.isPending ? 'Checking…' : 'Test key'}
            </button>
            <button type="button" className="danger" onClick={() => remove.mutate({ path: { studioId } })}>
              Remove key
            </button>
          </div>
          <label className="choice">
            <input
              type="checkbox"
              checked={key.data.allowPlatformReaders}
              disabled={consent.isPending}
              onChange={(e) =>
                consent.mutate({ path: { studioId }, body: { allowPlatformReaders: e.target.checked } })
              }
            />
            Allow the platform&apos;s readers (Google Document AI, Azure AI Document Intelligence) when the platform turns
            them on for this studio
          </label>
          <p className="badge" role="status">
            {key.data.allowPlatformReaders
              ? 'The platform’s readers may read this studio’s invoices.'
              : 'The platform’s readers don’t read this studio’s invoices.'}
          </p>
        </div>
      )}
      {[save, test, consent, remove].map((m, i) => m.isError && <ErrorMessage key={i} error={m.error} />)}
      <form
        className="stack"
        onSubmit={(event) => {
          event.preventDefault();
          save.mutate({
            path: { studioId },
            // Replacing a key leaves the consent as it is; a first key says it here
            body: key.data
              ? { provider: 'ANTHROPIC', apiKey: apiKey.trim() }
              : { provider: 'ANTHROPIC', apiKey: apiKey.trim(), allowPlatformReaders: platformReaders },
          });
        }}
      >
        <div className="field">
          <label htmlFor={`${id}-key`}>{key.data ? 'Replace the key' : 'Anthropic API key'}</label>
          <input
            id={`${id}-key`}
            type="password"
            autoComplete="off"
            required
            minLength={20}
            maxLength={300}
            placeholder="sk-ant-…"
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
          />
        </div>
        {!key.data && (
          <label className="choice">
            <input type="checkbox" checked={platformReaders} onChange={(e) => setPlatformReaders(e.target.checked)} />
            Also allow the platform&apos;s readers (Google Document AI, Azure AI Document Intelligence) when they&apos;re
            offered
          </label>
        )}
        <div>
          <button type="submit" className="primary" disabled={save.isPending}>
            Save key
          </button>
        </div>
      </form>
    </section>
  );
}

function statusText(status: AiKeyStatus): string {
  switch (status.status) {
    case 'WORKING':
      return 'working';
    case 'REJECTED':
      return 'refused by Anthropic';
    case 'UNTESTED':
      return 'not checked yet';
  }
}
