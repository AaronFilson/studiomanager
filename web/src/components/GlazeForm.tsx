import type {
  Atmosphere,
  GlazeAmountUnit,
  GlazeLine,
  GlazeRecipe,
  SaveGlazeRequest,
} from '@studiomanager/api-client';
import { useId, useState } from 'react';

const atmospheres: Atmosphere[] = ['OXIDATION', 'REDUCTION', 'NEUTRAL'];

export const atmosphereLabel: Record<Atmosphere, string> = {
  OXIDATION: 'Oxidation',
  REDUCTION: 'Reduction',
  NEUTRAL: 'Neutral',
};

export const unitLabel: Record<GlazeAmountUnit, string> = {
  PARTS: 'parts',
  PERCENT: '%',
  GRAMS: 'g',
};

const blankLine = (): GlazeLine => ({ kind: 'BASE', material: '', amount: '', unit: 'PARTS' });

/**
 * Adds or changes a recipe. Base lines are in parts; additions (colorants, opacifiers) are a
 * percent of the base, or fixed grams.
 */
export function GlazeForm({
  initial,
  canShare,
  submitLabel,
  pending,
  onSubmit,
}: {
  initial?: GlazeRecipe;
  canShare: boolean;
  submitLabel: string;
  pending: boolean;
  onSubmit: (body: SaveGlazeRequest) => void;
}) {
  const id = useId();
  const [name, setName] = useState(initial?.name ?? '');
  const [cone, setCone] = useState(initial?.cone ?? '');
  const [atmosphere, setAtmosphere] = useState<Atmosphere | ''>(initial?.atmosphere ?? '');
  const [foodSurface, setFoodSurface] = useState(initial?.foodSurface ?? false);
  const [targetSg, setTargetSg] = useState(initial?.targetSpecificGravity ?? '');
  const [shared, setShared] = useState(initial?.sharedWithStudio ?? false);
  const [notes, setNotes] = useState(initial?.notes ?? '');
  const [lines, setLines] = useState<GlazeLine[]>(initial?.lines ?? [blankLine()]);

  const setLine = (index: number, change: Partial<GlazeLine>) =>
    setLines(lines.map((line, i) => (i === index ? { ...line, ...change } : line)));

  return (
    <form
      className="card stack"
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit({
          name: name.trim(),
          lines: lines.map((l) => ({ ...l, material: l.material.trim(), amount: l.amount.trim() })),
          foodSurface,
          ...(cone.trim() ? { cone: cone.trim() } : {}),
          ...(atmosphere ? { atmosphere } : {}),
          ...(targetSg.trim() ? { targetSpecificGravity: targetSg.trim() } : {}),
          ...(notes.trim() ? { notes: notes.trim() } : {}),
          ...(canShare ? { sharedWithStudio: shared } : {}),
        });
      }}
    >
      <div className="field">
        <label htmlFor={`${id}-name`}>Name</label>
        <input id={`${id}-name`} required maxLength={120} value={name} onChange={(e) => setName(e.target.value)} />
      </div>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-cone`}>Cone</label>
          <input id={`${id}-cone`} maxLength={10} placeholder="6" value={cone} onChange={(e) => setCone(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-atmosphere`}>Atmosphere</label>
          <select
            id={`${id}-atmosphere`}
            value={atmosphere}
            onChange={(e) => setAtmosphere(e.target.value as Atmosphere | '')}
          >
            <option value="">Not set</option>
            {atmospheres.map((a) => (
              <option key={a} value={a}>
                {atmosphereLabel[a]}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-sg`}>Target specific gravity</label>
          <input
            id={`${id}-sg`}
            inputMode="decimal"
            pattern="[12](\.\d{1,3})?"
            placeholder="1.45"
            value={targetSg}
            onChange={(e) => setTargetSg(e.target.value)}
          />
        </div>
      </div>
      <label className="choice">
        <input type="checkbox" checked={foodSurface} onChange={(e) => setFoodSurface(e.target.checked)} />
        Meant for food surfaces
      </label>
      {canShare && (
        <label className="choice">
          <input type="checkbox" checked={shared} onChange={(e) => setShared(e.target.checked)} />
          Share with the studio
        </label>
      )}
      <table>
        <caption>Recipe</caption>
        <thead>
          <tr>
            <th scope="col">Material</th>
            <th scope="col">Amount</th>
            <th scope="col">Unit</th>
            <th scope="col" />
          </tr>
        </thead>
        <tbody>
          {lines.map((line, index) => (
            <tr key={index}>
              <td>
                <input
                  aria-label={`Material ${String(index + 1)}`}
                  required
                  maxLength={80}
                  value={line.material}
                  onChange={(e) => setLine(index, { material: e.target.value })}
                />
              </td>
              <td>
                <input
                  aria-label={`Amount ${String(index + 1)}`}
                  required
                  inputMode="decimal"
                  pattern="\d{1,9}(\.\d{1,4})?"
                  value={line.amount}
                  onChange={(e) => setLine(index, { amount: e.target.value })}
                />
              </td>
              <td>
                <select
                  aria-label={`Unit ${String(index + 1)}`}
                  value={line.kind === 'BASE' ? 'BASE' : line.unit}
                  onChange={(e) =>
                    setLine(
                      index,
                      e.target.value === 'BASE'
                        ? { kind: 'BASE', unit: 'PARTS' }
                        : { kind: 'ADDITION', unit: e.target.value as GlazeAmountUnit },
                    )
                  }
                >
                  <option value="BASE">parts (base)</option>
                  <option value="PERCENT">% addition</option>
                  <option value="GRAMS">g addition</option>
                </select>
              </td>
              <td>
                <button
                  type="button"
                  disabled={lines.length === 1}
                  onClick={() => setLines(lines.filter((_, i) => i !== index))}
                >
                  Remove
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div>
        <button type="button" disabled={lines.length >= 60} onClick={() => setLines([...lines, blankLine()])}>
          Add a line
        </button>
      </div>
      <div className="field">
        <label htmlFor={`${id}-notes`}>Notes</label>
        <textarea id={`${id}-notes`} maxLength={4000} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </div>
      <div>
        <button type="submit" className="primary" disabled={pending}>
          {submitLabel}
        </button>
      </div>
    </form>
  );
}
