import {
  deleteGlazeMutation,
  getGlazeOptions,
  getGlazeQueryKey,
  updateGlazeMutation,
  type GlazeRecipe,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router';
import { useState } from 'react';

import { ErrorMessage } from '../../../../components/ErrorMessage';
import { atmosphereLabel, GlazeForm, unitLabel } from '../../../../components/GlazeForm';

export const Route = createFileRoute('/studios/$studioId/glazes/$glazeId')({
  component: GlazePage,
});

function GlazePage() {
  const { studioId, glazeId } = Route.useParams();
  const path = { studioId, glazeId };
  const glaze = useQuery(getGlazeOptions({ path }));
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [editing, setEditing] = useState(false);
  const update = useMutation({
    ...updateGlazeMutation(),
    onSuccess: (saved) => {
      queryClient.setQueryData(getGlazeQueryKey({ path }), saved);
      setEditing(false);
    },
  });
  const remove = useMutation({
    ...deleteGlazeMutation(),
    onSuccess: () => {
      void queryClient.invalidateQueries();
      void navigate({ to: '/studios/$studioId/glazes', params: { studioId } });
    },
  });

  if (glaze.isError) {
    return <ErrorMessage error={glaze.error} />;
  }
  if (!glaze.data) {
    return <p>Loading recipe…</p>;
  }
  const g = glaze.data;

  return (
    <div className="stack">
      <p>
        <Link to="/studios/$studioId/glazes" params={{ studioId }}>
          Glaze book
        </Link>
      </p>
      <h1>{g.name}</h1>
      <p>{describe(g)}</p>
      {g.foodSurface && <p className="badge">Meant for food surfaces. Follow the studio&apos;s own food-safety rules.</p>}
      {editing ? (
        <>
          {update.isError && <ErrorMessage error={update.error} />}
          <GlazeForm
            initial={g}
            canShare={g.ownerKind === 'MEMBER'}
            submitLabel="Save changes"
            pending={update.isPending}
            onSubmit={(body) => update.mutate({ path, body: { ...body, version: g.version } })}
          />
          <div>
            <button type="button" onClick={() => setEditing(false)}>
              Cancel
            </button>
          </div>
        </>
      ) : (
        <>
          <RecipeTable glaze={g} />
          {g.notes && <p className="notes">{g.notes}</p>}
          {remove.isError && <ErrorMessage error={remove.error} />}
          {g.canEdit && (
            <div className="row">
              <button type="button" onClick={() => setEditing(true)}>
                Edit
              </button>
              <button type="button" className="danger" onClick={() => remove.mutate({ path })}>
                Delete
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function RecipeTable({ glaze }: { glaze: GlazeRecipe }) {
  return (
    <table>
      <caption>Recipe</caption>
      <thead>
        <tr>
          <th scope="col">Material</th>
          <th scope="col">Amount</th>
        </tr>
      </thead>
      <tbody>
        {glaze.lines.map((line, i) => (
          <tr key={i}>
            <td>
              {line.kind === 'ADDITION' && '+ '}
              {line.material}
            </td>
            <td>
              {line.amount} {unitLabel[line.unit]}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function describe(g: GlazeRecipe): string {
  const parts = [
    g.ownerKind === 'STUDIO' ? 'Studio glaze' : `${g.ownerName ?? 'A member'}'s recipe${g.sharedWithStudio ? ', shared' : ''}`,
    g.cone ? `cone ${g.cone}` : undefined,
    g.atmosphere ? atmosphereLabel[g.atmosphere].toLowerCase() : undefined,
    g.targetSpecificGravity ? `target SG ${g.targetSpecificGravity}` : undefined,
  ];
  return parts.filter(Boolean).join(' · ');
}
