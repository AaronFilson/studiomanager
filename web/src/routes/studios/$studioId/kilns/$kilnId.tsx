import {
  type Firing,
  type FiringKind,
  getFiringOptions,
  getFiringQueryKey,
  getKilnQueueOptions,
  type Kiln,
  listKilnFiringsOptions,
  listKilnMaintenanceOptions,
  listKilnsOptions,
  loadKilnMutation,
  recordKilnMaintenanceMutation,
  setKilnAvailabilityMutation,
  transitionFiringMutation,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute, Link } from '@tanstack/react-router';
import { useId, useState } from 'react';

import { ErrorMessage } from '../../../../components/ErrorMessage';
import { formatDate, formatDateTime } from '../../../../labels';

export const Route = createFileRoute('/studios/$studioId/kilns/$kilnId')({
  component: KilnPage,
});

function KilnPage() {
  const { studioId, kilnId } = Route.useParams();
  const kilns = useQuery(listKilnsOptions({ path: { studioId } }));
  const firings = useQuery(listKilnFiringsOptions({ path: { studioId, kilnId } }));

  if (kilns.isPending) {
    return <p>Loading kiln…</p>;
  }
  if (kilns.isError) {
    return <ErrorMessage error={kilns.error} />;
  }
  const kiln = kilns.data.find((k) => k.id === kilnId);
  if (!kiln) {
    return <p role="alert">No such kiln.</p>;
  }
  const open = firings.data?.find((f) => f.status === 'LOADING' || f.status === 'FIRING');

  return (
    <div className="stack">
      <p>
        <Link to="/studios/$studioId/kilns" params={{ studioId }}>
          ← All kilns
        </Link>
      </p>
      <h1>{kiln.name}</h1>
      <dl className="facts">
        <dt>Firings</dt>
        <dd>{kiln.totalFirings}</dd>
        {kiln.maintenanceEveryFirings && (
          <>
            <dt>Since service</dt>
            <dd>
              {kiln.firingsSinceMaintenance} of {kiln.maintenanceEveryFirings}
              {kiln.maintenanceDue && <strong className="warning"> · maintenance due</strong>}
            </dd>
          </>
        )}
        {kiln.maxPiecesPerMember && (
          <>
            <dt>Per member</dt>
            <dd>at most {kiln.maxPiecesPerMember} pieces per load</dd>
          </>
        )}
        <dt>Status</dt>
        <dd>{kiln.available ? 'In service' : `Out of service: ${kiln.downReason ?? ''}`}</dd>
      </dl>
      {open ? <OpenFiring studioId={studioId} firingId={open.id} /> : <LoadForm studioId={studioId} kiln={kiln} />}
      <Availability studioId={studioId} kiln={kiln} />
      <Maintenance studioId={studioId} kilnId={kilnId} />
      <section className="stack" aria-label="Firing log">
        <h2>Firing log</h2>
        {firings.isSuccess && firings.data.length === 0 && <p>No firings yet.</p>}
        <ol className="timeline">
          {firings.data?.map((firing) => (
            <li key={firing.id}>
              <strong>{firing.kind === 'BISQUE' ? 'Bisque' : 'Glaze'}</strong>
              {firing.cone && ` · cone ${firing.cone}`} · {firing.pieceCount} pieces · {firing.status.toLowerCase()} ·{' '}
              {formatDateTime(firing.createdAt)}
              {firing.notes && <div className="badge">{firing.notes}</div>}
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
}

function LoadForm({ studioId, kiln }: { studioId: string; kiln: Kiln }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [kind, setKind] = useState<FiringKind>('BISQUE');
  const [cone, setCone] = useState('');
  const [chosen, setChosen] = useState<Set<string>>(new Set());
  const queue = useQuery(getKilnQueueOptions({ path: { studioId, kilnId: kiln.id }, query: { kind } }));
  const load = useMutation({
    ...loadKilnMutation(),
    onSuccess: async () => {
      setChosen(new Set());
      await queryClient.invalidateQueries();
    },
  });

  function toggle(pieceId: string) {
    setChosen((current) => {
      const next = new Set(current);
      if (next.has(pieceId)) next.delete(pieceId);
      else next.add(pieceId);
      return next;
    });
  }

  function fillFromQueue() {
    setChosen(new Set(queue.data?.filter((e) => e.fitsNextLoad).map((e) => e.piece.id)));
  }

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Load the kiln</h2>
      {!kiln.available && <p className="error">This kiln is out of service.</p>}
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-kind`}>Firing</label>
          <select
            id={`${id}-kind`}
            value={kind}
            onChange={(e) => {
              setKind(e.target.value as FiringKind);
              setChosen(new Set());
            }}
          >
            <option value="BISQUE">Bisque</option>
            <option value="GLAZE">Glaze</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-cone`}>Cone</label>
          <input id={`${id}-cone`} maxLength={10} value={cone} onChange={(e) => setCone(e.target.value)} />
        </div>
      </div>
      {queue.isError && <ErrorMessage error={queue.error} />}
      {queue.isSuccess && queue.data.length === 0 && <p>Nothing is waiting for a {kind.toLowerCase()} firing.</p>}
      {queue.isSuccess && queue.data.length > 0 && (
        <>
          <p className="badge">Longest-waiting first. Greyed pieces are over the per-member cap for this load.</p>
          <ul className="studio-list">
            {queue.data.map(({ piece, waitingSince, fitsNextLoad }) => (
              <li key={piece.id} className={fitsNextLoad ? undefined : 'muted'}>
                <label className="choice">
                  <input type="checkbox" checked={chosen.has(piece.id)} onChange={() => toggle(piece.id)} />
                  Piece {piece.number} · {piece.makerName ?? 'Unnamed member'}
                  {piece.description && ` · ${piece.description}`}
                </label>
                <span className="badge">since {formatDate(waitingSince)}</span>
              </li>
            ))}
          </ul>
        </>
      )}
      {load.isError && <ErrorMessage error={load.error} />}
      <div className="row">
        <button type="button" onClick={fillFromQueue} disabled={!queue.data?.length}>
          Fill from the queue
        </button>
        <button
          type="button"
          className="primary"
          disabled={chosen.size === 0 || load.isPending || !kiln.available}
          onClick={() =>
            load.mutate({
              path: { studioId, kilnId: kiln.id },
              body: { kind, pieceIds: [...chosen], ...(cone.trim() ? { cone: cone.trim() } : {}) },
            })
          }
        >
          Load {chosen.size} {chosen.size === 1 ? 'piece' : 'pieces'}
        </button>
      </div>
    </section>
  );
}

function OpenFiring({ studioId, firingId }: { studioId: string; firingId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const firing = useQuery(getFiringOptions({ path: { studioId, firingId } }));
  const [notes, setNotes] = useState('');
  const transition = useMutation({
    ...transitionFiringMutation(),
    onSuccess: async (updated: Firing) => {
      queryClient.setQueryData(getFiringQueryKey({ path: { studioId, firingId } }), updated);
      await queryClient.invalidateQueries();
    },
  });

  if (!firing.isSuccess) {
    return firing.isError ? <ErrorMessage error={firing.error} /> : <p>Loading the current firing…</p>;
  }
  const f = firing.data;
  const move = (to: 'FIRING' | 'COMPLETE' | 'CANCELLED') =>
    transition.mutate({
      path: { studioId, firingId },
      body: { to, expectedVersion: f.version, ...(notes.trim() ? { notes: notes.trim() } : {}) },
    });

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>
        {f.status === 'LOADING' ? 'Loading' : 'Firing'}: {f.kind === 'BISQUE' ? 'bisque' : 'glaze'}
        {f.cone && `, cone ${f.cone}`}
      </h2>
      <ul>
        {f.pieces.map((piece) => (
          <li key={piece.id}>
            Piece {piece.number} · {piece.makerName ?? 'Unnamed member'}
            {piece.description && ` · ${piece.description}`}
          </li>
        ))}
      </ul>
      <div className="field">
        <label htmlFor={`${id}-notes`}>Notes (optional)</label>
        <input id={`${id}-notes`} maxLength={1000} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </div>
      {transition.isError && <ErrorMessage error={transition.error} />}
      <div className="row">
        {f.status === 'LOADING' && (
          <button type="button" className="primary" onClick={() => move('FIRING')}>
            Start firing
          </button>
        )}
        {f.status === 'FIRING' && (
          <button type="button" className="primary" onClick={() => move('COMPLETE')}>
            Unload and notify makers
          </button>
        )}
        <button type="button" onClick={() => move('CANCELLED')}>
          Cancel this load
        </button>
      </div>
    </section>
  );
}

function Availability({ studioId, kiln }: { studioId: string; kiln: Kiln }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState('');
  const save = useMutation({
    ...setKilnAvailabilityMutation(),
    onSuccess: async () => {
      setReason('');
      await queryClient.invalidateQueries();
    },
  });

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Availability</h2>
      {kiln.available ? (
        <div className="row">
          <div className="field">
            <label htmlFor={`${id}-reason`}>Reason</label>
            <input id={`${id}-reason`} maxLength={200} value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
          <button
            type="button"
            disabled={!reason.trim()}
            onClick={() =>
              save.mutate({ path: { studioId, kilnId: kiln.id }, body: { downReason: reason.trim(), expectedVersion: kiln.version } })
            }
          >
            Take out of service
          </button>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => save.mutate({ path: { studioId, kilnId: kiln.id }, body: { expectedVersion: kiln.version } })}
        >
          Put back in service
        </button>
      )}
      {save.isError && <ErrorMessage error={save.error} />}
    </section>
  );
}

function Maintenance({ studioId, kilnId }: { studioId: string; kilnId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const log = useQuery(listKilnMaintenanceOptions({ path: { studioId, kilnId } }));
  const [note, setNote] = useState('');
  const [cost, setCost] = useState('');
  const record = useMutation({
    ...recordKilnMaintenanceMutation(),
    onSuccess: async () => {
      setNote('');
      setCost('');
      await queryClient.invalidateQueries();
    },
  });

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Maintenance</h2>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-note`}>Work done</label>
          <input id={`${id}-note`} maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-cost`}>Cost (optional)</label>
          <input id={`${id}-cost`} inputMode="decimal" value={cost} onChange={(e) => setCost(e.target.value.trim())} />
        </div>
        <button
          type="button"
          disabled={!note.trim() || record.isPending}
          onClick={() =>
            record.mutate({ path: { studioId, kilnId }, body: { note: note.trim(), ...(cost ? { cost } : {}) } })
          }
        >
          Log maintenance
        </button>
      </div>
      {record.isError && <ErrorMessage error={record.error} />}
      <ol className="timeline">
        {log.data?.map((entry, index) => (
          <li key={index}>
            {entry.note} · after firing {entry.atFiring} · {formatDate(entry.performedAt)}
            {entry.performedByName && ` · ${entry.performedByName}`}
          </li>
        ))}
      </ol>
    </section>
  );
}
