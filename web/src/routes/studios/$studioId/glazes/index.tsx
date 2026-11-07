import {
  createGlazeMutation,
  listGlazesOptions,
  type GlazeListFilter,
  type GlazeRecipe,
  useCan,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute, Link } from '@tanstack/react-router';
import { useState } from 'react';

import { ErrorMessage } from '../../../../components/ErrorMessage';
import { atmosphereLabel, GlazeForm } from '../../../../components/GlazeForm';

export const Route = createFileRoute('/studios/$studioId/glazes/')({
  component: GlazeBookPage,
});

const tabs: { filter: GlazeListFilter; label: string }[] = [
  { filter: 'STUDIO', label: 'Studio glazes' },
  { filter: 'MINE', label: 'My recipes' },
  { filter: 'SHARED', label: 'Shared by members' },
];

function GlazeBookPage() {
  const { studioId } = Route.useParams();
  const { can } = useCan(studioId);
  const staff = can('glazes.studio.edit');
  const [filter, setFilter] = useState<GlazeListFilter>('STUDIO');
  const [adding, setAdding] = useState(false);
  const glazes = useQuery(listGlazesOptions({ path: { studioId }, query: { owner: filter } }));
  const queryClient = useQueryClient();
  const create = useMutation({
    ...createGlazeMutation(),
    onSuccess: () => {
      setAdding(false);
      void queryClient.invalidateQueries();
    },
  });
  const canAdd = filter === 'MINE' || (filter === 'STUDIO' && staff);

  return (
    <div className="stack">
      <h1>Glaze book</h1>
      <p>
        The studio&apos;s bucket glazes, and your own recipes. Your recipes stay private unless you share them with the
        studio.
      </p>
      <div className="row" role="tablist" aria-label="Which recipes">
        {tabs.map((tab) => (
          <button
            key={tab.filter}
            type="button"
            role="tab"
            aria-selected={filter === tab.filter}
            className={filter === tab.filter ? 'primary' : undefined}
            onClick={() => {
              setFilter(tab.filter);
              setAdding(false);
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>
      {glazes.isError && <ErrorMessage error={glazes.error} />}
      {glazes.data?.length === 0 && <p>{emptyText(filter)}</p>}
      {glazes.data && glazes.data.length > 0 && <GlazeTable studioId={studioId} glazes={glazes.data} />}
      {canAdd && !adding && (
        <div>
          <button type="button" onClick={() => setAdding(true)}>
            {filter === 'STUDIO' ? 'Add a studio glaze' : 'Add a recipe'}
          </button>
        </div>
      )}
      {adding && (
        <>
          <h2>{filter === 'STUDIO' ? 'New studio glaze' : 'New recipe'}</h2>
          {create.isError && <ErrorMessage error={create.error} />}
          <GlazeForm
            canShare={filter === 'MINE'}
            submitLabel="Save"
            pending={create.isPending}
            onSubmit={(body) =>
              create.mutate({ path: { studioId }, body: { ...body, ownerKind: filter === 'STUDIO' ? 'STUDIO' : 'MEMBER' } })
            }
          />
        </>
      )}
    </div>
  );
}

function GlazeTable({ studioId, glazes }: { studioId: string; glazes: GlazeRecipe[] }) {
  return (
    <table>
      <thead>
        <tr>
          <th scope="col">Name</th>
          <th scope="col">Cone</th>
          <th scope="col">Atmosphere</th>
          <th scope="col">Food surface</th>
          <th scope="col">By</th>
        </tr>
      </thead>
      <tbody>
        {glazes.map((g) => (
          <tr key={g.id}>
            <td>
              <Link to="/studios/$studioId/glazes/$glazeId" params={{ studioId, glazeId: g.id }}>
                {g.name}
              </Link>
              {g.sharedWithStudio && <span className="badge"> shared</span>}
            </td>
            <td>{g.cone ?? ''}</td>
            <td>{g.atmosphere ? atmosphereLabel[g.atmosphere] : ''}</td>
            <td>{g.foodSurface ? 'Yes' : ''}</td>
            <td>{g.ownerKind === 'STUDIO' ? 'Studio' : (g.ownerName ?? '')}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function emptyText(filter: GlazeListFilter): string {
  switch (filter) {
    case 'STUDIO':
      return 'The studio has no glazes in the book yet.';
    case 'MINE':
      return 'You have no recipes yet.';
    case 'SHARED':
      return 'No one has shared a recipe yet.';
  }
}
