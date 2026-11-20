import {
  completeGlazecalcCallbackMutation,
  connectGlazecalcMutation,
  disconnectGlazecalcMutation,
  getGlazecalcStatusOptions,
  getGlazecalcStatusQueryKey,
  type GlazeImportOutcome,
  type GlazeImportResult,
  importGlazesMutation,
  importGlazeShareMutation,
  syncGlazecalcMutation,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { type ChangeEvent, useEffect, useId, useRef, useState } from 'react';

import { formatDateTime } from '../labels';
import { ErrorMessage } from './ErrorMessage';

const outcomeLabels: Record<GlazeImportOutcome, string> = {
  NEW: 'Add',
  UPDATED: 'Update',
  UNCHANGED: 'Unchanged',
  ERROR: 'Error',
};

/**
 * Import recipes from a `glazecalc-export` v1 or `glaze-vault-export` v1 JSON file, paste a
 * Glazecalc share link, or connect and sync directly with a Glazecalc account via OAuth.
 */
export function GlazeImportWizard({
  studioId,
  onImported,
}: {
  studioId: string;
  onImported: () => void;
}) {
  const id = useId();
  const queryClient = useQueryClient();

  // 1. JSON file import state
  const [fileName, setFileName] = useState<string>();
  const [parsedPayload, setParsedPayload] = useState<Record<string, unknown>>();
  const [parseError, setParseError] = useState<string>();
  const [fileResult, setFileResult] = useState<GlazeImportResult>();

  // 2. Glazecalc share link state
  const [shareInput, setShareInput] = useState('');
  const [shareResult, setShareResult] = useState<GlazeImportResult>();

  // 3. Glazecalc OAuth sync state
  const [syncResult, setSyncResult] = useState<GlazeImportResult>();
  const statusKey = getGlazecalcStatusQueryKey({ path: { studioId } });
  const status = useQuery(getGlazecalcStatusOptions({ path: { studioId } }));

  const importFile = useMutation({
    ...importGlazesMutation(),
    onSuccess: (res) => {
      setFileResult(res);
      if (res.committed) {
        void queryClient.invalidateQueries();
        onImported();
      }
    },
  });

  const importShare = useMutation({
    ...importGlazeShareMutation(),
    onSuccess: (res) => {
      setShareResult(res);
      if (res.committed) {
        setShareInput('');
        void queryClient.invalidateQueries();
        onImported();
      }
    },
  });

  const connect = useMutation({
    ...connectGlazecalcMutation(),
    onSuccess: (res) => {
      window.location.assign(res.authorizationUrl);
    },
  });

  const completeCallback = useMutation({
    ...completeGlazecalcCallbackMutation(),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: statusKey });
    },
  });

  const sync = useMutation({
    ...syncGlazecalcMutation(),
    onSuccess: (res) => {
      setSyncResult(res);
      void queryClient.invalidateQueries();
      if (res.committed) {
        onImported();
      }
    },
  });

  const disconnect = useMutation({
    ...disconnectGlazecalcMutation(),
    onSuccess: () => {
      setSyncResult(undefined);
      void queryClient.invalidateQueries({ queryKey: statusKey });
    },
  });

  // Handle OAuth redirect callback parameters (`?code=...&state=...`) once on mount
  const callbackHandled = useRef(false);
  const { mutate: runCallback } = completeCallback;
  useEffect(() => {
    if (callbackHandled.current) {
      return;
    }
    const params = new URLSearchParams(window.location.search);
    const code = params.get('glazecalc_code') ?? params.get('code');
    const state = params.get('glazecalc_state') ?? params.get('state');
    if (code && state) {
      callbackHandled.current = true;
      const redirectUri = `${window.location.origin}${window.location.pathname}`;
      runCallback({ path: { studioId }, body: { code, state, redirectUri } });
    }
  }, [studioId, runCallback]);

  async function loadJsonFile(file: File): Promise<void> {
    setFileName(file.name);
    setParseError(undefined);
    setFileResult(undefined);
    setParsedPayload(undefined);
    try {
      const text = await file.text();
      const parsed: unknown = JSON.parse(text);
      if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
        setParseError('The file must contain a JSON object.');
        return;
      }
      const payload = parsed as Record<string, unknown>;
      setParsedPayload(payload);
      await importFile.mutateAsync({ path: { studioId }, query: { dryRun: true }, body: payload });
    } catch (err: unknown) {
      if (err instanceof SyntaxError) {
        setParseError("That file isn't valid JSON.");
      }
    }
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (file) {
      void loadJsonFile(file);
    }
  }

  const currentRedirectUri = `${window.location.origin}${window.location.pathname}`;

  return (
    <div className="stack">
      <section className="card stack" aria-labelledby={`${id}-file-title`}>
        <h2 id={`${id}-file-title`}>Import from Glazecalc or glaze-vault JSON</h2>
        <p className="badge">
          Upload a <code>glazecalc-export</code> v1 or <code>glaze-vault-export</code> v1 JSON file (up to 2 MB).
          Recipes import into <strong>My recipes</strong> after a preview; nothing is saved if any recipe has an error.
        </p>
        <div className="field">
          <label htmlFor={`${id}-json-file`}>Export JSON file</label>
          <input id={`${id}-json-file`} type="file" accept=".json,application/json" onChange={handleFileChange} />
        </div>
        {fileName && <p className="badge">Selected: {fileName}</p>}
        {parseError && <p className="error">{parseError}</p>}
        {importFile.isError && <ErrorMessage error={importFile.error} />}
        {fileResult && (
          <ImportResultPreview
            result={fileResult}
            pending={importFile.isPending}
            onCommit={() => {
              if (parsedPayload) {
                importFile.mutate({ path: { studioId }, query: { dryRun: false }, body: parsedPayload });
              }
            }}
          />
        )}
      </section>

      <section className="card stack" aria-labelledby={`${id}-share-title`}>
        <h2 id={`${id}-share-title`}>Import from a Glazecalc share link</h2>
        <form
          className="stack"
          onSubmit={(event) => {
            event.preventDefault();
            importShare.mutate({
              path: { studioId },
              body: { shareUrlOrSecret: shareInput.trim(), dryRun: true },
            });
          }}
        >
          <div className="field">
            <label htmlFor={`${id}-share`}>Glazecalc share URL or secret</label>
            <input
              id={`${id}-share`}
              required
              placeholder="https://glazecalcapp.com/shared#…"
              value={shareInput}
              onChange={(e) => {
                setShareInput(e.target.value);
                setShareResult(undefined);
              }}
            />
          </div>
          <div>
            <button type="submit" disabled={importShare.isPending || !shareInput.trim()}>
              {importShare.isPending ? 'Loading…' : 'Preview share link'}
            </button>
          </div>
        </form>
        {importShare.isError && <ErrorMessage error={importShare.error} />}
        {shareResult && (
          <ImportResultPreview
            result={shareResult}
            pending={importShare.isPending}
            onCommit={() => {
              importShare.mutate({
                path: { studioId },
                body: { shareUrlOrSecret: shareInput.trim(), dryRun: false },
              });
            }}
          />
        )}
      </section>

      <section className="card stack" aria-labelledby={`${id}-oauth-title`}>
        <h2 id={`${id}-oauth-title`}>Connect &amp; sync with Glazecalc</h2>
        {status.isError && <ErrorMessage error={status.error} />}
        {status.data && !status.data.configured && (
          <p className="badge">
            Live Glazecalc OAuth sync isn&apos;t configured on this server yet. You can still upload a Glazecalc export
            file or paste a share link above.
          </p>
        )}
        {status.data && status.data.configured && !status.data.connected && (
          <div className="stack">
            <p>Connect your Glazecalc account to pull your recipes directly into My recipes.</p>
            {connect.isError && <ErrorMessage error={connect.error} />}
            {completeCallback.isError && <ErrorMessage error={completeCallback.error} />}
            <div>
              <button
                type="button"
                className="primary"
                disabled={connect.isPending}
                onClick={() => connect.mutate({ path: { studioId }, body: { redirectUri: currentRedirectUri } })}
              >
                {connect.isPending ? 'Connecting…' : 'Connect Glazecalc'}
              </button>
            </div>
          </div>
        )}
        {status.data?.connected && (
          <div className="stack">
            <p>
              Connected to Glazecalc
              {status.data.remoteAccountLabel ? ` as ${status.data.remoteAccountLabel}` : ''}
              {status.data.lastSyncedAt ? ` · last synced ${formatDateTime(status.data.lastSyncedAt)}` : ''}.
            </p>
            {sync.isError && <ErrorMessage error={sync.error} />}
            {disconnect.isError && <ErrorMessage error={disconnect.error} />}
            <div className="row">
              <button
                type="button"
                className="primary"
                disabled={sync.isPending}
                onClick={() => sync.mutate({ path: { studioId }, query: { dryRun: false } })}
              >
                {sync.isPending ? 'Syncing…' : 'Sync from Glazecalc'}
              </button>
              <button
                type="button"
                disabled={disconnect.isPending}
                onClick={() => disconnect.mutate({ path: { studioId } })}
              >
                Disconnect
              </button>
            </div>
            {syncResult && <ImportResultPreview result={syncResult} pending={false} />}
          </div>
        )}
      </section>
    </div>
  );
}

function ImportResultPreview({
  result,
  pending,
  onCommit,
}: {
  result: GlazeImportResult;
  pending: boolean;
  onCommit?: () => void;
}) {
  return (
    <div className="stack" aria-live="polite">
      <p>
        <strong>{result.committed ? 'Imported' : 'Preview'}</strong> ({result.format}): {String(result.createdCount)}{' '}
        new, {String(result.updatedCount)} updated, {String(result.unchangedCount)} unchanged
        {result.errorCount > 0 ? `, ${String(result.errorCount)} error(s)` : ''}.
      </p>
      {result.rows.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th scope="col">#</th>
              <th scope="col">Recipe</th>
              <th scope="col">Action</th>
              <th scope="col">Detail</th>
            </tr>
          </thead>
          <tbody>
            {result.rows.map((row) => (
              <tr key={row.index} className={row.outcome === 'ERROR' ? 'error' : undefined}>
                <td>{String(row.index)}</td>
                <td>{row.name}</td>
                <td>{outcomeLabels[row.outcome]}</td>
                <td>
                  <div>{row.message}</div>
                  {row.warnings.map((w, i) => (
                    <div key={i} className="badge warning">
                      {w}
                    </div>
                  ))}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {!result.committed && onCommit && (
        <div>
          <button
            type="button"
            className="primary"
            disabled={pending || result.errorCount > 0 || result.rows.length === 0}
            onClick={onCommit}
          >
            {pending ? 'Importing…' : `Import ${String(result.rows.length)} ${result.rows.length === 1 ? 'recipe' : 'recipes'}`}
          </button>
        </div>
      )}
    </div>
  );
}
