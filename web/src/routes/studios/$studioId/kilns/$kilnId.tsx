import {
  type Firing,
  type FiringKind,
  FROM_THE_EXPORT,
  getFiringOptions,
  getFiringQueryKey,
  getKilnQueueOptions,
  importKilnLogMutation,
  type Kiln,
  type KilnLogColumnMap,
  type KilnLogImportResult,
  type KilnLogSource,
  listKilnFiringsOptions,
  listKilnsOptions,
  loadKilnMutation,
  setKilnAvailabilityMutation,
  type TemperaturePoint,
  type TemperatureUnit,
  transitionFiringMutation,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute, Link } from '@tanstack/react-router';
import { useId, useState } from 'react';

import { CappedNote } from '../../../../components/CappedNote';
import { ErrorMessage } from '../../../../components/ErrorMessage';
import { formatDate, formatDateTime } from '../../../../labels';

const logSourceOptions: { value: KilnLogSource; label: string }[] = [
  { value: 'GENESIS', label: 'Genesis (unverified)' },
  { value: 'TAP', label: 'TAP (unverified)' },
  { value: 'OTHER', label: 'Other' },
];

const logSourceBadges: Record<KilnLogSource, string> = {
  GENESIS: 'Genesis',
  TAP: 'TAP',
  OTHER: 'Other log',
};

function formatDuration(minutes: number): string {
  if (minutes < 60) {
    return `${String(minutes)}m`;
  }
  const hours = Math.floor(minutes / 60);
  const rem = minutes % 60;
  return rem === 0 ? `${String(hours)}h` : `${String(hours)}h ${String(rem)}m`;
}

function formatTemp(value: number, unit?: TemperatureUnit): string {
  const rounded = Number.isInteger(value) ? String(value) : value.toFixed(1);
  return `${rounded} °${unit ?? 'F'}`;
}

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
  const open = firings.data?.items.find((f) => f.status === 'LOADING' || f.status === 'FIRING');

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
      <ImportKilnLog studioId={studioId} kilnId={kiln.id} firings={firings.data?.items ?? []} />
      <Availability studioId={studioId} kiln={kiln} />
      <p>
        <Link to="/studios/$studioId/maintenance" params={{ studioId }} hash={kiln.id}>
          Maintenance tasks and history for this kiln
        </Link>
      </p>
      <section className="stack" aria-label="Firing log">
        <h2>Firing log</h2>
        {firings.isSuccess && firings.data.items.length === 0 && <p>No firings yet.</p>}
        <CappedNote list={firings.data} noun="firings" findTheRest={FROM_THE_EXPORT} />
        <ol className="timeline">
          {firings.data?.items.map((firing) => (
            <li key={firing.id} className="firing-entry">
              <div>
                <strong>{firing.kind === 'BISQUE' ? 'Bisque' : 'Glaze'}</strong>
                {firing.cone && ` · cone ${firing.cone}`} · {firing.pieceCount} pieces · {firing.status.toLowerCase()} ·{' '}
                {formatDateTime(firing.createdAt)}
              </div>
              {(firing.peakTemp !== undefined ||
                firing.durationMinutes !== undefined ||
                firing.errorCode ||
                firing.logSource) && (
                <div className="telemetry-badges">
                  {firing.peakTemp !== undefined && (
                    <span className="telemetry-pill">{formatTemp(firing.peakTemp, firing.tempUnit)}</span>
                  )}
                  {firing.durationMinutes !== undefined && (
                    <span className="telemetry-pill">{formatDuration(firing.durationMinutes)}</span>
                  )}
                  {firing.logSource && <span className="telemetry-pill">{logSourceBadges[firing.logSource]}</span>}
                  {firing.errorCode && <span className="telemetry-pill warning">Error: {firing.errorCode}</span>}
                </div>
              )}
              {firing.temperatureCurve && firing.temperatureCurve.length >= 2 && (
                <FiringCurveChart
                  curve={firing.temperatureCurve}
                  tempUnit={firing.tempUnit}
                  peakTemp={firing.peakTemp}
                  durationMinutes={firing.durationMinutes}
                  compact
                />
              )}
              {firing.notes && <div className="badge">{firing.notes}</div>}
            </li>
          ))}
        </ol>
      </section>
    </div>
  );
}

function FiringCurveChart({
  curve,
  tempUnit,
  peakTemp,
  durationMinutes,
  compact = false,
}: {
  curve: TemperaturePoint[];
  tempUnit?: TemperatureUnit;
  peakTemp?: number;
  durationMinutes?: number;
  compact?: boolean;
}) {
  const firstPoint = curve[0];
  if (!firstPoint || curve.length < 2) {
    return null;
  }
  const width = compact ? 220 : 320;
  const height = compact ? 52 : 92;
  const padX = 6;
  const padY = 8;
  const plotW = width - padX * 2;
  const plotH = height - padY * 2;

  const maxElapsed = Math.max(...curve.map((p) => p.elapsedMinutes), 1);
  const temps = curve.flatMap((p) => (p.setpoint !== undefined ? [p.temp, p.setpoint] : [p.temp]));
  const minTemp = Math.min(...temps);
  const maxTemp = Math.max(...temps, minTemp + 1);
  const span = maxTemp - minTemp;

  const toX = (elapsed: number) => padX + (elapsed / maxElapsed) * plotW;
  const toY = (temp: number) => padY + plotH - ((temp - minTemp) / span) * plotH;

  const actualLine = curve.map((p) => `${toX(p.elapsedMinutes).toFixed(1)},${toY(p.temp).toFixed(1)}`).join(' ');
  const setpointPoints = curve.filter((p) => p.setpoint !== undefined);
  const setpointLine =
    setpointPoints.length >= 2
      ? setpointPoints
          .map((p) => `${toX(p.elapsedMinutes).toFixed(1)},${toY(p.setpoint ?? p.temp).toFixed(1)}`)
          .join(' ')
      : null;

  let peakPoint = firstPoint;
  for (const pt of curve) {
    if (pt.temp > peakPoint.temp) {
      peakPoint = pt;
    }
  }
  const peakLabel = formatTemp(peakTemp ?? peakPoint.temp, tempUnit);
  const durLabel = durationMinutes !== undefined ? ` over ${formatDuration(durationMinutes)}` : '';

  return (
    <div className={compact ? 'curve-wrap compact' : 'curve-wrap'}>
      <svg
        className="firing-curve"
        viewBox={`0 0 ${String(width)} ${String(height)}`}
        role="img"
        aria-label={`Firing temperature curve peaking at ${peakLabel}${durLabel}`}
      >
        <line
          x1={padX}
          y1={height - padY}
          x2={width - padX}
          y2={height - padY}
          stroke="var(--border)"
          strokeWidth="1"
        />
        {setpointLine && (
          <polyline
            points={setpointLine}
            fill="none"
            stroke="var(--muted)"
            strokeWidth="1.25"
            strokeDasharray="4 3"
          />
        )}
        <polyline points={actualLine} fill="none" stroke="var(--accent)" strokeWidth="2" />
        <circle cx={toX(peakPoint.elapsedMinutes)} cy={toY(peakPoint.temp)} r={compact ? 2.5 : 3.5} fill="var(--accent)" />
      </svg>
      {!compact && (
        <div className="badge curve-caption">
          <span>Start</span>
          <span>
            Peak {peakLabel}
            {setpointLine ? ' · dashed: setpoint' : ''}
          </span>
          <span>Total {formatDuration(durationMinutes ?? Math.round(maxElapsed))}</span>
        </div>
      )}
    </div>
  );
}

function ImportKilnLog({
  studioId,
  kilnId,
  firings,
}: {
  studioId: string;
  kilnId: string;
  firings: Firing[];
}) {
  const id = useId();
  const queryClient = useQueryClient();
  const [source, setSource] = useState<KilnLogSource>('GENESIS');
  const [tempUnit, setTempUnit] = useState<'' | TemperatureUnit>('');
  const [fileName, setFileName] = useState<string>();
  const [csvText, setCsvText] = useState('');
  const [colTime, setColTime] = useState('');
  const [colTemp, setColTemp] = useState('');
  const [colSetpoint, setColSetpoint] = useState('');
  const [colError, setColError] = useState('');
  const [preview, setPreview] = useState<KilnLogImportResult>();
  const [targetFiringId, setTargetFiringId] = useState('NEW');
  const [kind, setKind] = useState<FiringKind>('BISQUE');
  const [cone, setCone] = useState('');
  const [committedMessage, setCommittedMessage] = useState<string>();

  const importLog = useMutation({ ...importKilnLogMutation() });

  async function onChooseFile(file: File) {
    const text = await file.text();
    setFileName(file.name);
    setCsvText(text);
    setPreview(undefined);
    setCommittedMessage(undefined);
  }

  function buildColumnMap(): KilnLogColumnMap | undefined {
    const map: KilnLogColumnMap = {};
    if (colTime.trim()) map.time = colTime.trim();
    if (colTemp.trim()) map.temperature = colTemp.trim();
    if (colSetpoint.trim()) map.setpoint = colSetpoint.trim();
    if (colError.trim()) map.error = colError.trim();
    return Object.keys(map).length > 0 ? map : undefined;
  }

  async function runImport(dryRun: boolean) {
    setCommittedMessage(undefined);
    const columnMap = buildColumnMap();
    const attachId = !dryRun && targetFiringId !== 'NEW' ? targetFiringId : undefined;
    const result = await importLog.mutateAsync({
      path: { studioId, kilnId },
      body: {
        csvText,
        source,
        dryRun,
        ...(tempUnit ? { tempUnit } : {}),
        ...(columnMap ? { columnMap } : {}),
        ...(attachId ? { firingId: attachId } : {}),
        ...(!dryRun && !attachId ? { kind, ...(cone.trim() ? { cone: cone.trim() } : {}) } : {}),
      },
    });
    if (dryRun) {
      setPreview(result);
      setTargetFiringId(result.suggestedMatch ? result.suggestedMatch.firingId : 'NEW');
      if (result.detectedCone) {
        setCone(result.detectedCone);
      }
    } else {
      setPreview(undefined);
      setCommittedMessage(
        `Log imported (${formatTemp(result.peakTemp, result.tempUnit)}, ${formatDuration(result.durationMinutes)}).`,
      );
      await queryClient.invalidateQueries();
    }
  }

  const candidateFirings = firings.slice(0, 20);
  const suggestedId = preview?.suggestedMatch?.firingId;

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Import controller log</h2>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-preset`}>Controller preset</label>
          <select
            id={`${id}-preset`}
            value={source}
            onChange={(e) => {
              setSource(e.target.value as KilnLogSource);
              setPreview(undefined);
            }}
          >
            {logSourceOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-unit`}>Temperature unit</label>
          <select
            id={`${id}-unit`}
            value={tempUnit}
            onChange={(e) => {
              setTempUnit(e.target.value as '' | TemperatureUnit);
              setPreview(undefined);
            }}
          >
            <option value="">Auto-detect</option>
            <option value="F">°F</option>
            <option value="C">°C</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-file`}>Controller CSV</label>
          <input
            id={`${id}-file`}
            type="file"
            accept=".csv,.tsv,.txt,text/csv,text/plain"
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) void onChooseFile(file);
            }}
          />
        </div>
      </div>
      {source !== 'OTHER' && (
        <p className="badge">
          Genesis and TAP presets are unverified against real controller exports. Preview the curve before importing.
        </p>
      )}
      <details>
        <summary className="badge">Custom column names (optional)</summary>
        <div className="row" style={{ marginTop: '0.5rem' }}>
          <div className="field">
            <label htmlFor={`${id}-col-time`}>Time column</label>
            <input
              id={`${id}-col-time`}
              maxLength={80}
              placeholder="e.g. Elapsed"
              value={colTime}
              onChange={(e) => {
                setColTime(e.target.value);
                setPreview(undefined);
              }}
            />
          </div>
          <div className="field">
            <label htmlFor={`${id}-col-temp`}>Temperature column</label>
            <input
              id={`${id}-col-temp`}
              maxLength={160}
              placeholder="e.g. TC1, TC2, TC3"
              value={colTemp}
              onChange={(e) => {
                setColTemp(e.target.value);
                setPreview(undefined);
              }}
            />
          </div>
          <div className="field">
            <label htmlFor={`${id}-col-sp`}>Setpoint column</label>
            <input
              id={`${id}-col-sp`}
              maxLength={80}
              placeholder="e.g. Setpoint"
              value={colSetpoint}
              onChange={(e) => {
                setColSetpoint(e.target.value);
                setPreview(undefined);
              }}
            />
          </div>
          <div className="field">
            <label htmlFor={`${id}-col-err`}>Error/event column</label>
            <input
              id={`${id}-col-err`}
              maxLength={80}
              placeholder="e.g. Error"
              value={colError}
              onChange={(e) => {
                setColError(e.target.value);
                setPreview(undefined);
              }}
            />
          </div>
        </div>
      </details>
      {fileName && <p className="badge">Selected file: {fileName}</p>}
      {importLog.isError && <ErrorMessage error={importLog.error} />}
      {committedMessage && (
        <p className="badge" role="status">
          {committedMessage}
        </p>
      )}
      <div className="row">
        <button
          type="button"
          disabled={!csvText.trim() || importLog.isPending}
          onClick={() => void runImport(true)}
        >
          Preview log
        </button>
      </div>
      {preview && (
        <div className="stack" role="region" aria-label="Log preview" aria-live="polite">
          <dl className="facts">
            <dt>Peak temp</dt>
            <dd>
              {formatTemp(preview.peakTemp, preview.tempUnit)}
              {preview.tempUnitInferred ? ' (inferred)' : ''}
            </dd>
            <dt>Duration</dt>
            <dd>{formatDuration(preview.durationMinutes)}</dd>
            <dt>Samples</dt>
            <dd>
              {String(preview.rowCount)} rows · {String(preview.zoneCount)}{' '}
              {preview.zoneCount === 1 ? 'zone' : 'zones'}
            </dd>
            {preview.startedAt && (
              <>
                <dt>Started</dt>
                <dd>{formatDateTime(preview.startedAt)}</dd>
              </>
            )}
            {preview.detectedCone && (
              <>
                <dt>Detected cone</dt>
                <dd>Cone {preview.detectedCone}</dd>
              </>
            )}
            {preview.errorCode && (
              <>
                <dt>Error code</dt>
                <dd className="warning">{preview.errorCode}</dd>
              </>
            )}
          </dl>
          <FiringCurveChart
            curve={preview.temperatureCurve}
            tempUnit={preview.tempUnit}
            peakTemp={preview.peakTemp}
            durationMinutes={preview.durationMinutes}
          />
          {preview.warnings.length > 0 && (
            <ul className="badge">
              {preview.warnings.map((w, i) => (
                <li key={i}>{w}</li>
              ))}
            </ul>
          )}
          {preview.suggestedMatch && (
            <p className="badge">
              Suggested match: {preview.suggestedMatch.kind === 'BISQUE' ? 'Bisque' : 'Glaze'}
              {preview.suggestedMatch.cone ? ` · cone ${preview.suggestedMatch.cone}` : ''} (
              {preview.suggestedMatch.overlapDescription})
            </p>
          )}
          <div className="row">
            <div className="field">
              <label htmlFor={`${id}-target`}>Attach to firing</label>
              <select
                id={`${id}-target`}
                value={targetFiringId}
                onChange={(e) => setTargetFiringId(e.target.value)}
              >
                <option value="NEW">Create a new complete firing</option>
                {preview.suggestedMatch &&
                  !candidateFirings.some((f) => f.id === preview.suggestedMatch?.firingId) && (
                    <option value={preview.suggestedMatch.firingId}>
                      {preview.suggestedMatch.kind === 'BISQUE' ? 'Bisque' : 'Glaze'}
                      {preview.suggestedMatch.cone ? ` · cone ${preview.suggestedMatch.cone}` : ''} ·{' '}
                      {formatDateTime(preview.suggestedMatch.createdAt)} (suggested match)
                    </option>
                  )}
                {candidateFirings.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.kind === 'BISQUE' ? 'Bisque' : 'Glaze'}
                    {f.cone ? ` · cone ${f.cone}` : ''} · {formatDateTime(f.createdAt)}
                    {f.id === suggestedId ? ' (suggested match)' : ''}
                  </option>
                ))}
              </select>
            </div>
            {targetFiringId === 'NEW' && (
              <>
                <div className="field">
                  <label htmlFor={`${id}-import-kind`}>New firing kind</label>
                  <select
                    id={`${id}-import-kind`}
                    value={kind}
                    onChange={(e) => setKind(e.target.value as FiringKind)}
                  >
                    <option value="BISQUE">Bisque</option>
                    <option value="GLAZE">Glaze</option>
                  </select>
                </div>
                <div className="field">
                  <label htmlFor={`${id}-import-cone`}>New firing cone</label>
                  <input
                    id={`${id}-import-cone`}
                    maxLength={10}
                    value={cone}
                    onChange={(e) => setCone(e.target.value)}
                  />
                </div>
              </>
            )}
          </div>
          <div>
            <button
              type="button"
              className="primary"
              disabled={importLog.isPending}
              onClick={() => void runImport(false)}
            >
              Import log
            </button>
          </div>
        </div>
      )}
    </section>
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
    setChosen(new Set(queue.data?.items.filter((e) => e.fitsNextLoad).map((e) => e.piece.id)));
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
      {queue.isSuccess && queue.data.items.length === 0 && <p>Nothing is waiting for a {kind.toLowerCase()} firing.</p>}
      {queue.isSuccess && queue.data.items.length > 0 && (
        <>
          <p className="badge">Longest-waiting first. Greyed pieces are over the per-member cap for this load.</p>
          <CappedNote
            list={queue.data}
            order="longest-waiting"
            noun="pieces"
            findTheRest="Load some of these, and the rest will show up next."
          />
          <ul className="studio-list">
            {queue.data.items.map(({ piece, waitingSince, fitsNextLoad }) => (
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
        <button type="button" onClick={fillFromQueue} disabled={!queue.data?.items.length}>
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
