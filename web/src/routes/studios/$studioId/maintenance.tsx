import {
  archiveAssetMutation,
  type Asset,
  type AssetKind,
  createAssetMutation,
  createMaintenanceTaskMutation,
  type Duty,
  exportMaintenanceRecords,
  getStudioOptions,
  listAssetsOptions,
  listMaintenanceRecordsOptions,
  listProblemReportsOptions,
  listSuggestedMaintenanceTasksOptions,
  listSuppliesOptions,
  type PartUse,
  recordRepairMutation,
  resolveProblemReportMutation,
  setAssetAvailabilityMutation,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { useId, useState } from 'react';

import { ErrorMessage, errorText } from '../../../components/ErrorMessage';
import { MaintenanceTaskRow } from '../../../components/MaintenanceTaskRow';
import { RecordPhotos } from '../../../components/RecordPhotos';
import { assetKindLabel, dutyLabel, formatDate, formatDateTime, formatMoney, scheduleText } from '../../../labels';

export const Route = createFileRoute('/studios/$studioId/maintenance')({
  component: MaintenancePage,
});

const addableKinds: AssetKind[] = [
  'WHEEL',
  'PUG_MILL',
  'SLAB_ROLLER',
  'EXTRUDER',
  'CLAY_TRAP',
  'SPRAY_BOOTH',
  'VENTILATION',
  'BUILDING',
  'SAFETY',
  'OTHER',
];
const duties: Duty[] = ['TECHNICIAN', 'MONITOR', 'WORK_EXCHANGE', 'VOLUNTEER_OFFICER', 'INSTRUCTOR'];

/** Everything the studio looks after, kilns first, each with its recurring tasks and history. */
function MaintenancePage() {
  const { studioId } = Route.useParams();
  const assets = useQuery(listAssetsOptions({ path: { studioId } }));

  return (
    <div className="stack">
      <h1>Maintenance</h1>
      <p className="badge">
        Kilns appear here by themselves. Add anything else that needs regular care: the pug mill, clay trap, spray booth
        filters, fire extinguishers, the roof.
      </p>
      <OpenProblems studioId={studioId} />
      {assets.isPending && <p>Loading…</p>}
      {assets.isError && <ErrorMessage error={assets.error} />}
      {assets.data?.map((asset) => <AssetCard key={asset.id} studioId={studioId} asset={asset} />)}
      <AddAssetForm studioId={studioId} />
      <HistoryExport studioId={studioId} />
    </div>
  );
}

function AssetCard({ studioId, asset }: { studioId: string; asset: Asset }) {
  const id = useId();
  const [showHistory, setShowHistory] = useState(false);
  return (
    // Kilns link here by kiln id, so the anchor is the kiln's when there is one
    <section className="card stack" id={asset.kilnId ?? asset.id} aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>{asset.name}</h2>
      <p className="badge">
        {assetKindLabel(asset.kind)}
        {asset.location && ` · ${asset.location}`}
        {asset.totalFirings !== undefined && ` · ${String(asset.totalFirings)} firings`}
        {!asset.available && <span className="warning"> · out of service: {asset.downReason}</span>}
      </p>
      {asset.tasks.length === 0 ? (
        <p>No recurring tasks yet.</p>
      ) : (
        <ul className="studio-list">
          {asset.tasks.map((task) => (
            <MaintenanceTaskRow key={task.id} studioId={studioId} task={task} />
          ))}
        </ul>
      )}
      <Suggestions studioId={studioId} asset={asset} />
      <AddTaskForm studioId={studioId} asset={asset} />
      <div className="row">
        <button type="button" onClick={() => setShowHistory((s) => !s)} aria-expanded={showHistory}>
          {showHistory ? 'Hide history' : 'History and repairs'}
        </button>
        {asset.kind !== 'KILN' && <AssetActions studioId={studioId} asset={asset} />}
      </div>
      {showHistory && <History studioId={studioId} asset={asset} />}
    </section>
  );
}

/** What members have reported and nobody has dealt with yet, oldest worries first in view. */
function OpenProblems({ studioId }: { studioId: string }) {
  const queryClient = useQueryClient();
  const reports = useQuery(listProblemReportsOptions({ path: { studioId }, query: { open: true } }));
  const resolve = useMutation({ ...resolveProblemReportMutation(), onSuccess: () => queryClient.invalidateQueries() });

  if (reports.isError) {
    return <ErrorMessage error={reports.error} />;
  }
  if (!reports.data?.length) {
    return null;
  }
  return (
    <section className="card stack" aria-label="Reported problems">
      <h2>Reported problems</h2>
      <ul className="studio-list">
        {reports.data.map((r) => (
          <li key={r.id}>
            <div>
              <strong>{r.assetName ?? 'Something else'}</strong>: {r.description}
              <div className="badge">
                {r.reportedByName ?? 'A member'} · {formatDateTime(r.reportedAt)}
              </div>
            </div>
            <button
              type="button"
              disabled={resolve.isPending}
              onClick={() => {
                const resolution = window.prompt('What was done? (optional)');
                if (resolution !== null) {
                  resolve.mutate({
                    path: { studioId, reportId: r.id },
                    body: { expectedVersion: r.version, ...(resolution.trim() ? { resolution: resolution.trim() } : {}) },
                  });
                }
              }}
            >
              Resolve
            </button>
          </li>
        ))}
      </ul>
      {resolve.isError && <ErrorMessage error={resolve.error} />}
    </section>
  );
}

/** Published schedules for this kind of asset (the kiln maker's, OSHA's…), each one click to add. */
function Suggestions({ studioId, asset }: { studioId: string; asset: Asset }) {
  const id = useId();
  const queryClient = useQueryClient();
  const suggestions = useQuery(listSuggestedMaintenanceTasksOptions({ path: { studioId, assetId: asset.id } }));
  const add = useMutation({ ...createMaintenanceTaskMutation(), onSuccess: () => queryClient.invalidateQueries() });

  if (!suggestions.data?.length) {
    return null;
  }
  return (
    <div className="stack">
      <h3 id={`${id}-title`}>Suggested tasks</h3>
      <ul className="studio-list" aria-labelledby={`${id}-title`}>
        {suggestions.data.map((s) => (
          <li key={s.task.title} className="row">
            <span>
              <strong>{s.task.title}</strong> · {scheduleText(s.task)}
              <span className="badge"> · {s.source}</span>
            </span>
            <button
              type="button"
              disabled={add.isPending}
              aria-label={`Add ${s.task.title}`}
              onClick={() => add.mutate({ path: { studioId, assetId: asset.id }, body: s.task })}
            >
              Add
            </button>
          </li>
        ))}
      </ul>
      {add.isError && <ErrorMessage error={add.error} />}
    </div>
  );
}

/** Every asset's records between two dates, for an insurer, a landlord or an audit. */
function HistoryExport({ studioId }: { studioId: string }) {
  const id = useId();
  const today = new Date().toISOString().slice(0, 10);
  const [from, setFrom] = useState(`${today.slice(0, 4)}-01-01`);
  const [to, setTo] = useState(today);
  const [error, setError] = useState<string>();

  async function download() {
    setError(undefined);
    try {
      const { data } = await exportMaintenanceRecords({ path: { studioId }, query: { from, to }, parseAs: 'text', throwOnError: true });
      const url = URL.createObjectURL(new Blob([data], { type: 'text/csv' }));
      const link = document.createElement('a');
      link.href = url;
      link.download = `maintenance-${from}-to-${to}.csv`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(errorText(e));
    }
  }

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Maintenance history</h2>
      <p className="badge">Everything done to every asset, as a spreadsheet: the record an insurer or landlord asks for.</p>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-from`}>From</label>
          <input id={`${id}-from`} type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-to`}>To</label>
          <input id={`${id}-to`} type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
        <button type="button" onClick={() => void download()}>
          Download CSV
        </button>
      </div>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </section>
  );
}

function AddTaskForm({ studioId, asset }: { studioId: string; asset: Asset }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [days, setDays] = useState('');
  const [firings, setFirings] = useState('');
  const [duty, setDuty] = useState<Duty | ''>('');
  const [checklist, setChecklist] = useState('');
  const create = useMutation({
    ...createMaintenanceTaskMutation(),
    onSuccess: async () => {
      setOpen(false);
      setTitle('');
      setDays('');
      setFirings('');
      setDuty('');
      setChecklist('');
      await queryClient.invalidateQueries();
    },
  });

  if (!open) {
    return (
      <div>
        <button type="button" onClick={() => setOpen(true)}>
          Add a recurring task
        </button>
      </div>
    );
  }
  return (
    <form
      className="stack"
      aria-label={`New task for ${asset.name}`}
      onSubmit={(event) => {
        event.preventDefault();
        create.mutate({
          path: { studioId, assetId: asset.id },
          body: {
            title: title.trim(),
            checklist: checklist
              .split('\n')
              .map((line) => line.trim())
              .filter(Boolean),
            ...(days ? { everyDays: Number(days) } : {}),
            ...(firings ? { everyFirings: Number(firings) } : {}),
            ...(duty ? { assignedDuty: duty } : {}),
          },
        });
      }}
    >
      <div className="field">
        <label htmlFor={`${id}-title`}>Task</label>
        <input id={`${id}-title`} required maxLength={120} value={title} onChange={(e) => setTitle(e.target.value)} />
      </div>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-days`}>Every … days</label>
          <input id={`${id}-days`} type="number" min={1} max={3650} value={days} onChange={(e) => setDays(e.target.value)} />
        </div>
        {asset.kind === 'KILN' && (
          <div className="field">
            <label htmlFor={`${id}-firings`}>or every … firings</label>
            <input
              id={`${id}-firings`}
              type="number"
              min={1}
              max={10000}
              value={firings}
              onChange={(e) => setFirings(e.target.value)}
            />
          </div>
        )}
        <div className="field">
          <label htmlFor={`${id}-duty`}>Done by</label>
          <select id={`${id}-duty`} value={duty} onChange={(e) => setDuty(e.target.value as Duty | '')}>
            <option value="">Staff</option>
            {duties.map((d) => (
              <option key={d} value={d}>
                Any {dutyLabel(d).toLowerCase()}
              </option>
            ))}
          </select>
        </div>
      </div>
      <div className="field">
        <label htmlFor={`${id}-checklist`}>Checklist, one line each (optional)</label>
        <textarea id={`${id}-checklist`} rows={3} value={checklist} onChange={(e) => setChecklist(e.target.value)} />
      </div>
      {create.isError && <ErrorMessage error={create.error} />}
      <div className="row">
        <button type="submit" className="primary" disabled={create.isPending || (!days && !firings)}>
          Add task
        </button>
        <button type="button" onClick={() => setOpen(false)}>
          Cancel
        </button>
      </div>
    </form>
  );
}

function History({ studioId, asset }: { studioId: string; asset: Asset }) {
  const id = useId();
  const queryClient = useQueryClient();
  const records = useQuery(listMaintenanceRecordsOptions({ path: { studioId, assetId: asset.id } }));
  const studio = useQuery(getStudioOptions({ path: { studioId } }));
  const currency = studio.data?.currency ?? 'USD';
  const supplies = useQuery(listSuppliesOptions({ path: { studioId } }));
  const [note, setNote] = useState('');
  const [cost, setCost] = useState('');
  const [parts, setParts] = useState<PartUse[]>([]);
  const [partItem, setPartItem] = useState('');
  const [partQuantity, setPartQuantity] = useState('1');
  const repair = useMutation({
    ...recordRepairMutation(),
    onSuccess: async () => {
      setNote('');
      setCost('');
      setParts([]);
      await queryClient.invalidateQueries();
    },
  });
  const itemName = (itemId: string) => supplies.data?.find((s) => s.id === itemId)?.name ?? 'Part';

  return (
    <div className="stack">
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-note`}>Repair or other work</label>
          <input id={`${id}-note`} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-cost`}>Cost (optional)</label>
          <input id={`${id}-cost`} inputMode="decimal" value={cost} onChange={(e) => setCost(e.target.value.trim())} />
        </div>
        <button
          type="button"
          disabled={!note.trim() || repair.isPending}
          onClick={() =>
            repair.mutate({
              path: { studioId, assetId: asset.id },
              body: { note: note.trim(), ...(cost ? { cost } : {}), ...(parts.length ? { parts } : {}) },
            })
          }
        >
          Log it
        </button>
      </div>
      {!!supplies.data?.length && (
        <div className="row">
          <div className="field">
            <label htmlFor={`${id}-part`}>Part from supplies (optional)</label>
            <select id={`${id}-part`} value={partItem} onChange={(e) => setPartItem(e.target.value)}>
              <option value="">Choose…</option>
              {supplies.data
                .filter((s) => !parts.some((p) => p.itemId === s.id))
                .map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.onHand} {s.unit} on hand)
                  </option>
                ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor={`${id}-quantity`}>How many</label>
            <input
              id={`${id}-quantity`}
              inputMode="decimal"
              value={partQuantity}
              onChange={(e) => setPartQuantity(e.target.value.trim())}
            />
          </div>
          <button
            type="button"
            disabled={!partItem || !(Number(partQuantity) > 0)}
            onClick={() => {
              setParts((p) => [...p, { itemId: partItem, quantity: partQuantity }]);
              setPartItem('');
              setPartQuantity('1');
            }}
          >
            Add part
          </button>
        </div>
      )}
      {parts.length > 0 && (
        <ul aria-label="Parts used">
          {parts.map((p) => (
            <li key={p.itemId}>
              {p.quantity} × {itemName(p.itemId)}{' '}
              <button type="button" onClick={() => setParts((all) => all.filter((x) => x.itemId !== p.itemId))}>
                Remove
              </button>
            </li>
          ))}
        </ul>
      )}
      {repair.isError && <ErrorMessage error={repair.error} />}
      {records.isError && <ErrorMessage error={records.error} />}
      {records.isSuccess && records.data.length === 0 && <p>Nothing logged yet.</p>}
      <ol className="timeline" aria-label={`${asset.name} history`}>
        {records.data?.map((r) => (
          <li key={r.id}>
            <strong>{r.taskTitle ?? 'Repair'}</strong> · {formatDate(r.doneAt)}
            {r.doneByName && ` · ${r.doneByName}`}
            {r.atFiring !== undefined && ` · at firing ${String(r.atFiring)}`}
            {r.cost && ` · ${formatMoney(r.cost, currency)}`}
            {r.note && <div>{r.note}</div>}
            {r.checked.length > 0 && <div className="badge">Ticked: {r.checked.join('; ')}</div>}
            {r.parts.length > 0 && (
              <div className="badge">
                Parts: {r.parts.map((p) => `${p.quantity} ${p.unit} ${p.itemName}`.replace(/\s+/g, ' ')).join('; ')}
              </div>
            )}
            <RecordPhotos studioId={studioId} record={r} />
          </li>
        ))}
      </ol>
    </div>
  );
}

/** Out of service and back, or stop tracking. Kilns do both on the kiln itself. */
function AssetActions({ studioId, asset }: { studioId: string; asset: Asset }) {
  const queryClient = useQueryClient();
  const onSuccess = () => queryClient.invalidateQueries();
  const availability = useMutation({ ...setAssetAvailabilityMutation(), onSuccess });
  const archive = useMutation({ ...archiveAssetMutation(), onSuccess });

  return (
    <>
      {asset.available ? (
        <button
          type="button"
          onClick={() => {
            const reason = window.prompt(`Why is ${asset.name} out of service?`)?.trim();
            if (reason) {
              availability.mutate({
                path: { studioId, assetId: asset.id },
                body: { downReason: reason, expectedVersion: asset.version },
              });
            }
          }}
        >
          Out of service…
        </button>
      ) : (
        <button
          type="button"
          onClick={() =>
            availability.mutate({ path: { studioId, assetId: asset.id }, body: { expectedVersion: asset.version } })
          }
        >
          Back in service
        </button>
      )}
      <button
        type="button"
        onClick={() => {
          if (window.confirm(`Stop tracking ${asset.name}? Its history is kept.`)) {
            archive.mutate({ path: { studioId, assetId: asset.id } });
          }
        }}
      >
        Stop tracking
      </button>
      {(availability.error ?? archive.error) && <ErrorMessage error={availability.error ?? archive.error} />}
    </>
  );
}

function AddAssetForm({ studioId }: { studioId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [kind, setKind] = useState<AssetKind>('PUG_MILL');
  const [name, setName] = useState('');
  const [location, setLocation] = useState('');
  const create = useMutation({
    ...createAssetMutation(),
    onSuccess: async () => {
      setName('');
      setLocation('');
      await queryClient.invalidateQueries();
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
          body: { kind, name: name.trim(), ...(location.trim() ? { location: location.trim() } : {}) },
        });
      }}
    >
      <h2 id={`${id}-title`}>Add something to maintain</h2>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-kind`}>What</label>
          <select id={`${id}-kind`} value={kind} onChange={(e) => setKind(e.target.value as AssetKind)}>
            {addableKinds.map((k) => (
              <option key={k} value={k}>
                {assetKindLabel(k)}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-name`}>Name</label>
          <input id={`${id}-name`} required maxLength={80} value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-location`}>Where (optional)</label>
          <input id={`${id}-location`} maxLength={80} value={location} onChange={(e) => setLocation(e.target.value)} />
        </div>
      </div>
      {create.isError && <ErrorMessage error={create.error} />}
      <div>
        <button type="submit" className="primary" disabled={create.isPending}>
          Add
        </button>
      </div>
    </form>
  );
}
