import { createKilnMutation, type KilnType, listKilnsOptions, listKilnsQueryKey } from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute, Link } from '@tanstack/react-router';
import { type SubmitEvent, useId, useState } from 'react';

import { ErrorMessage } from '../../../../components/ErrorMessage';

const kilnTypes: KilnType[] = ['ELECTRIC', 'GAS', 'WOOD', 'RAKU', 'OTHER'];
const kilnTypeLabels: Record<KilnType, string> = {
  ELECTRIC: 'Electric',
  GAS: 'Gas',
  WOOD: 'Wood',
  RAKU: 'Raku',
  OTHER: 'Other',
};

export const Route = createFileRoute('/studios/$studioId/kilns/')({
  component: KilnsPage,
});

function KilnsPage() {
  const { studioId } = Route.useParams();
  const kilns = useQuery(listKilnsOptions({ path: { studioId } }));

  return (
    <div className="stack">
      <h1>Kilns</h1>
      {kilns.isPending && <p>Loading kilns…</p>}
      {kilns.isError && <ErrorMessage error={kilns.error} />}
      {kilns.isSuccess && kilns.data.length === 0 && <p>No kilns yet. Add your first one below.</p>}
      {kilns.isSuccess && kilns.data.length > 0 && (
        <ul className="studio-list">
          {kilns.data.map((kiln) => (
            <li key={kiln.id}>
              <Link to="/studios/$studioId/kilns/$kilnId" params={{ studioId, kilnId: kiln.id }}>
                {kiln.name}
                {kiln.site && ` (${kiln.site})`}
              </Link>
              <span className="badge">
                {kilnTypeLabels[kiln.kind]} · {kiln.totalFirings} firings
                {!kiln.available && <span className="warning"> · out of service</span>}
              </span>
            </li>
          ))}
        </ul>
      )}
      <AddKilnForm studioId={studioId} />
    </div>
  );
}

function AddKilnForm({ studioId }: { studioId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [site, setSite] = useState('');
  const [kind, setKind] = useState<KilnType>('ELECTRIC');
  const [cap, setCap] = useState('');
  const create = useMutation({
    ...createKilnMutation(),
    onSuccess: async () => {
      setName('');
      setSite('');
      setCap('');
      await queryClient.invalidateQueries({ queryKey: listKilnsQueryKey({ path: { studioId } }) });
    },
  });

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    create.mutate({
      path: { studioId },
      body: {
        name: name.trim(),
        kind,
        ...(site.trim() ? { site: site.trim() } : {}),
        ...(cap ? { maxPiecesPerMember: Number(cap) } : {}),
      },
    });
  }

  return (
    <form className="card stack" onSubmit={submit} aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Add a kiln</h2>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-name`}>Name</label>
          <input id={`${id}-name`} required maxLength={60} value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-kind`}>Type</label>
          <select id={`${id}-kind`} value={kind} onChange={(e) => setKind(e.target.value as KilnType)}>
            {kilnTypes.map((type) => (
              <option key={type} value={type}>
                {kilnTypeLabels[type]}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-site`}>Site (optional)</label>
          <input id={`${id}-site`} maxLength={60} value={site} onChange={(e) => setSite(e.target.value)} />
        </div>
      </div>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-cap`}>Most pieces per member in one load</label>
          <input id={`${id}-cap`} type="number" min={1} max={500} value={cap} onChange={(e) => setCap(e.target.value)} />
        </div>
      </div>
      <p className="badge">Set up its servicing, such as elements and thermocouple, on the Maintenance page.</p>
      {create.isError && <ErrorMessage error={create.error} />}
      <div>
        <button type="submit" className="primary" disabled={create.isPending}>
          Add kiln
        </button>
      </div>
    </form>
  );
}
