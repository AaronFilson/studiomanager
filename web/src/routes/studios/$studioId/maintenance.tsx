import {
  archiveAssetMutation,
  type Asset,
  type AssetKind,
  createAssetMutation,
  createMaintenanceTaskMutation,
  type Duty,
  getStudioOptions,
  listAssetsOptions,
  listMaintenanceRecordsOptions,
  recordRepairMutation,
  setAssetAvailabilityMutation,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { useId, useState } from 'react';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { MaintenanceTaskRow } from '../../../components/MaintenanceTaskRow';
import { assetKindLabel, dutyLabel, formatDate, formatMoney } from '../../../labels';

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
      {assets.isPending && <p>Loading…</p>}
      {assets.isError && <ErrorMessage error={assets.error} />}
      {assets.data?.map((asset) => <AssetCard key={asset.id} studioId={studioId} asset={asset} />)}
      <AddAssetForm studioId={studioId} />
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
  const [note, setNote] = useState('');
  const [cost, setCost] = useState('');
  const repair = useMutation({
    ...recordRepairMutation(),
    onSuccess: async () => {
      setNote('');
      setCost('');
      await queryClient.invalidateQueries();
    },
  });

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
            repair.mutate({ path: { studioId, assetId: asset.id }, body: { note: note.trim(), ...(cost ? { cost } : {}) } })
          }
        >
          Log it
        </button>
      </div>
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
