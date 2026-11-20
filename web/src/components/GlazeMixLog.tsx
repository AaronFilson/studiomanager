import {
  addGlazeMixReadingMutation,
  type GlazeMix,
  type GlazeMixReading,
  type GlazeRecipe,
  listGlazeMixesOptions,
  listGlazeMixesQueryKey,
  logGlazeMixMutation,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useId, useMemo, useState } from 'react';

import { formatDateTime } from '../labels';
import { ErrorMessage } from './ErrorMessage';

/**
 * Studio glaze mix history, SG reading log, off-target warnings, and compact SVG SG trend chart.
 */
export function GlazeMixLog({
  studioId,
  glaze,
  canEdit,
}: {
  studioId: string;
  glaze: GlazeRecipe;
  canEdit: boolean;
}) {
  const id = useId();
  const queryClient = useQueryClient();
  const mixesKey = listGlazeMixesQueryKey({ path: { studioId, glazeId: glaze.id } });
  const mixes = useQuery(listGlazeMixesOptions({ path: { studioId, glazeId: glaze.id } }));

  const [dryWeightG, setDryWeightG] = useState('');
  const [waterG, setWaterG] = useState('');
  const [initialSg, setInitialSg] = useState('');
  const [notes, setNotes] = useState('');

  const logMix = useMutation({
    ...logGlazeMixMutation(),
    onSuccess: () => {
      setDryWeightG('');
      setWaterG('');
      setInitialSg('');
      setNotes('');
      void queryClient.invalidateQueries({ queryKey: mixesKey });
    },
  });

  const chronologicalReadings = useMemo(() => {
    const all: GlazeMixReading[] = [];
    for (const mix of mixes.data ?? []) {
      for (const reading of mix.readings) {
        all.push(reading);
      }
    }
    return all.sort((a, b) => new Date(a.readAt).getTime() - new Date(b.readAt).getTime());
  }, [mixes.data]);

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Mix log &amp; specific gravity</h2>

      {chronologicalReadings.length > 0 && (
        <SgTrendChart readings={chronologicalReadings} targetSg={glaze.targetSpecificGravity} />
      )}

      {canEdit && (
        <form
          className="stack"
          aria-label="Log a mix"
          onSubmit={(event) => {
            event.preventDefault();
            logMix.mutate({
              path: { studioId, glazeId: glaze.id },
              body: {
                dryWeightG: dryWeightG.trim(),
                ...(waterG.trim() ? { waterG: waterG.trim() } : {}),
                ...(initialSg.trim() ? { specificGravity: initialSg.trim() } : {}),
                ...(notes.trim() ? { notes: notes.trim() } : {}),
              },
            });
          }}
        >
          <h3>Log a new mix</h3>
          <div className="row">
            <div className="field">
              <label htmlFor={`${id}-dry`}>Dry weight (g)</label>
              <input
                id={`${id}-dry`}
                required
                inputMode="decimal"
                placeholder="10000"
                value={dryWeightG}
                onChange={(e) => setDryWeightG(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor={`${id}-water`}>Water (g, optional)</label>
              <input
                id={`${id}-water`}
                inputMode="decimal"
                placeholder="7500"
                value={waterG}
                onChange={(e) => setWaterG(e.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor={`${id}-sg`}>Initial SG (optional)</label>
              <input
                id={`${id}-sg`}
                inputMode="decimal"
                placeholder={glaze.targetSpecificGravity ?? '1.45'}
                value={initialSg}
                onChange={(e) => setInitialSg(e.target.value)}
              />
            </div>
          </div>
          <div className="field">
            <label htmlFor={`${id}-notes`}>Mix notes (optional)</label>
            <input
              id={`${id}-notes`}
              maxLength={2000}
              placeholder="Sieved 80 mesh, bucket #2"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>
          {logMix.isError && <ErrorMessage error={logMix.error} />}
          <div>
            <button type="submit" className="primary" disabled={logMix.isPending}>
              {logMix.isPending ? 'Logging…' : 'Log mix'}
            </button>
          </div>
        </form>
      )}

      {mixes.isError && <ErrorMessage error={mixes.error} />}
      {mixes.data?.length === 0 && <p className="badge">No mixes logged for this bucket yet.</p>}
      {mixes.data && mixes.data.length > 0 && (
        <div className="stack">
          {mixes.data.map((mix) => (
            <MixCard
              key={mix.id}
              studioId={studioId}
              glazeId={glaze.id}
              mix={mix}
              canEdit={canEdit}
              onReadingAdded={() => {
                void queryClient.invalidateQueries({ queryKey: mixesKey });
              }}
            />
          ))}
        </div>
      )}
    </section>
  );
}

function MixCard({
  studioId,
  glazeId,
  mix,
  canEdit,
  onReadingAdded,
}: {
  studioId: string;
  glazeId: string;
  mix: GlazeMix;
  canEdit: boolean;
  onReadingAdded: () => void;
}) {
  const id = useId();
  const [sg, setSg] = useState('');
  const [note, setNote] = useState('');
  const addReading = useMutation({
    ...addGlazeMixReadingMutation(),
    onSuccess: () => {
      setSg('');
      setNote('');
      onReadingAdded();
    },
  });

  return (
    <article className="stack" style={{ borderTop: '1px solid var(--border)', paddingTop: '0.75rem' }}>
      <p>
        <strong>{mix.dryWeightG} g dry</strong>
        {mix.waterG ? ` · ${mix.waterG} g water` : ''} · mixed by {mix.mixedByName ?? 'Staff'} on{' '}
        {formatDateTime(mix.mixedAt)}
      </p>
      {mix.notes && <p className="badge">{mix.notes}</p>}

      {mix.readings.length > 0 && (
        <ul className="timeline">
          {mix.readings.map((r) => (
            <li key={r.id}>
              <strong>SG {r.specificGravity}</strong> · {formatDateTime(r.readAt)}
              {r.note ? ` — ${r.note}` : ''}
              {r.offTarget && (
                <span className="badge warning" role="alert">
                  {' '}
                  {r.warning ?? 'Off target specific gravity.'}
                </span>
              )}
            </li>
          ))}
        </ul>
      )}

      {canEdit && (
        <form
          className="row"
          aria-label={`Add SG reading for mix ${mix.id}`}
          onSubmit={(event) => {
            event.preventDefault();
            addReading.mutate({
              path: { studioId, glazeId, mixId: mix.id },
              body: {
                specificGravity: sg.trim(),
                ...(note.trim() ? { note: note.trim() } : {}),
              },
            });
          }}
        >
          <div className="field">
            <label htmlFor={`${id}-sg`}>New SG reading</label>
            <input
              id={`${id}-sg`}
              required
              inputMode="decimal"
              placeholder="1.45"
              value={sg}
              onChange={(e) => setSg(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor={`${id}-note`}>Reading note</label>
            <input
              id={`${id}-note`}
              maxLength={500}
              placeholder="Added 200 mL water"
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
          </div>
          <button type="submit" disabled={addReading.isPending}>
            {addReading.isPending ? 'Saving…' : 'Add reading'}
          </button>
          {addReading.isError && <ErrorMessage error={addReading.error} />}
        </form>
      )}
    </article>
  );
}

function SgTrendChart({ readings, targetSg }: { readings: GlazeMixReading[]; targetSg?: string }) {
  const width = 480;
  const height = 140;
  const padX = 42;
  const padY = 18;
  const values = readings.map((r) => Number(r.specificGravity));
  const target = targetSg ? Number(targetSg) : undefined;
  const allVals = target !== undefined ? [...values, target - 0.04, target + 0.04] : values;
  const minVal = Math.min(...allVals) - 0.02;
  const maxVal = Math.max(...allVals) + 0.02;
  const span = Math.max(0.05, maxVal - minVal);

  const yOf = (val: number) => padY + ((maxVal - val) / span) * (height - 2 * padY);
  const xOf = (idx: number) =>
    readings.length === 1
      ? width / 2
      : padX + (idx / (readings.length - 1)) * (width - 2 * padX);

  const polyline = readings.map((r, i) => `${xOf(i).toFixed(1)},${yOf(Number(r.specificGravity)).toFixed(1)}`).join(' ');

  return (
    <figure style={{ margin: 0 }}>
      <svg
        role="img"
        aria-label={`Specific gravity trend across ${String(readings.length)} readings`}
        viewBox={`0 0 ${String(width)} ${String(height)}`}
        style={{ width: '100%', maxHeight: '150px', display: 'block' }}
      >
        {target !== undefined && (
          <>
            <rect
              x={padX}
              y={yOf(target + 0.03)}
              width={width - 2 * padX}
              height={Math.max(2, yOf(target - 0.03) - yOf(target + 0.03))}
              fill="var(--accent)"
              opacity={0.12}
            />
            <line
              x1={padX}
              x2={width - padX}
              y1={yOf(target)}
              y2={yOf(target)}
              stroke="var(--accent)"
              strokeDasharray="4 4"
              strokeWidth={1.5}
            />
          </>
        )}
        <text x={4} y={padY + 4} fontSize={11} fill="var(--muted)">
          {maxVal.toFixed(2)}
        </text>
        <text x={4} y={height - padY} fontSize={11} fill="var(--muted)">
          {minVal.toFixed(2)}
        </text>
        {readings.length > 1 && (
          <polyline fill="none" stroke="var(--accent)" strokeWidth={2} points={polyline} />
        )}
        {readings.map((r, i) => (
          <circle
            key={r.id}
            cx={xOf(i)}
            cy={yOf(Number(r.specificGravity))}
            r={4.5}
            fill={r.offTarget ? 'var(--error)' : 'var(--accent)'}
          >
            <title>
              SG {r.specificGravity} ({formatDateTime(r.readAt)})
            </title>
          </circle>
        ))}
      </svg>
    </figure>
  );
}
