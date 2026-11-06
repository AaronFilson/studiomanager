import {
  deleteGlazeMutation,
  getGlazeBatchOptions,
  getGlazeOptions,
  getGlazeQueryKey,
  updateGlazeMutation,
  type GlazeRecipe,
  type WeightUnit,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute, Link, useNavigate } from '@tanstack/react-router';
import { useId, useState } from 'react';

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
      // A changed recipe makes a different batch
      void queryClient.invalidateQueries();
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
          <BatchPanel studioId={studioId} glazeId={glazeId} />
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

const weightUnits: { unit: WeightUnit; label: string }[] = [
  { unit: 'GRAM', label: 'g' },
  { unit: 'KILOGRAM', label: 'kg' },
  { unit: 'POUND', label: 'lb' },
  { unit: 'OUNCE', label: 'oz' },
];

/** Weigh out a batch: the server scales the recipe, so web and phone agree to the gram. */
function BatchPanel({ studioId, glazeId }: { studioId: string; glazeId: string }) {
  const id = useId();
  const [size, setSize] = useState('');
  const [unit, setUnit] = useState<WeightUnit>('KILOGRAM');
  const valid = /^\d{1,9}(\.\d{1,4})?$/.test(size) && Number(size) > 0;
  const batch = useQuery({
    ...getGlazeBatchOptions({ path: { studioId, glazeId }, query: { size, unit } }),
    enabled: valid,
  });
  const label = weightUnits.find((u) => u.unit === unit)?.label ?? '';

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Mix a batch</h2>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-size`}>Dry batch size</label>
          <input
            id={`${id}-size`}
            inputMode="decimal"
            placeholder="10"
            value={size}
            onChange={(e) => setSize(e.target.value.trim())}
          />
        </div>
        <div className="field">
          <label htmlFor={`${id}-unit`}>Unit</label>
          <select id={`${id}-unit`} value={unit} onChange={(e) => setUnit(e.target.value as WeightUnit)}>
            {weightUnits.map((u) => (
              <option key={u.unit} value={u.unit}>
                {u.label}
              </option>
            ))}
          </select>
        </div>
      </div>
      {batch.isError && <ErrorMessage error={batch.error} />}
      {valid && batch.data && (
        <table>
          <caption>Weigh out</caption>
          <tbody>
            {batch.data.lines.map((line, i) => (
              <tr key={i}>
                <td>
                  {line.kind === 'ADDITION' && '+ '}
                  {line.material}
                </td>
                <td>
                  {line.weight} {label}
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr>
              <th scope="row">Total</th>
              <td>
                {batch.data.total} {label}
              </td>
            </tr>
          </tfoot>
        </table>
      )}
    </section>
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
