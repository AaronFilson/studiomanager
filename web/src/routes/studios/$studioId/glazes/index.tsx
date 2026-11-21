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
import { GlazeImportWizard } from '../../../../components/GlazeImportWizard';

export const Route = createFileRoute('/studios/$studioId/glazes/')({
  component: GlazeBookPage,
});

const tabs: { filter: GlazeListFilter; label: string }[] = [
  { filter: 'STUDIO', label: 'Studio glazes' },
  { filter: 'MINE', label: 'My recipes' },
  { filter: 'SHARED', label: 'Shared by members' },
];

function hasOAuthReturn(): boolean {
  const params = new URLSearchParams(window.location.search);
  const hasCode = Boolean(
    (params.get('glazecalc_code') ?? params.get('code')) &&
      (params.get('glazecalc_state') ?? params.get('state')),
  );
  return hasCode || Boolean(params.get('error'));
}

function GlazeBookPage() {
  const { studioId } = Route.useParams();
  const { can } = useCan(studioId);
  const staff = can('glazes.studio.edit');
  const [filter, setFilter] = useState<GlazeListFilter>('STUDIO');
  const [adding, setAdding] = useState(false);
  const [importing, setImporting] = useState(() => hasOAuthReturn());
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
      <div className="row">
        <div className="row" role="tablist" aria-label="Which recipes">
          {tabs.map((tab) => (
            <button
              key={tab.filter}
              type="button"
              role="tab"
              aria-selected={!importing && filter === tab.filter}
              className={!importing && filter === tab.filter ? 'primary' : undefined}
              onClick={() => {
                setFilter(tab.filter);
                setAdding(false);
                setImporting(false);
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>
        <button
          type="button"
          className={importing ? 'primary' : undefined}
          aria-pressed={importing}
          onClick={() => {
            setImporting(!importing);
            setAdding(false);
          }}
        >
          Import / Glazecalc
        </button>
      </div>

      {importing ? (
        <GlazeImportWizard
          key={studioId}
          studioId={studioId}
          onImported={() => {
            setFilter('MINE');
          }}
        />
      ) : (
        <>
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
                  create.mutate({
                    path: { studioId },
                    body: { ...body, ownerKind: filter === 'STUDIO' ? 'STUDIO' : 'MEMBER' },
                  })
                }
              />
            </>
          )}
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
