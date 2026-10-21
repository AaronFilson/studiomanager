import { listSuppliesOptions, type PartUse } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { useId, useState } from 'react';

/**
 * Spare parts taken from supplies for a piece of work, such as a new element. Logging the work
 * takes them out of stock. Shows nothing if the studio keeps no supplies.
 */
export function PartsPicker({
  studioId,
  parts,
  onChange,
}: {
  studioId: string;
  parts: PartUse[];
  onChange: (parts: PartUse[]) => void;
}) {
  const id = useId();
  const supplies = useQuery(listSuppliesOptions({ path: { studioId } }));
  const [item, setItem] = useState('');
  const [quantity, setQuantity] = useState('1');
  const nameOf = (itemId: string) => supplies.data?.find((s) => s.id === itemId)?.name ?? 'Part';

  if (!supplies.data?.length) {
    return null;
  }
  return (
    <>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-part`}>Part from supplies (optional)</label>
          <select id={`${id}-part`} value={item} onChange={(e) => setItem(e.target.value)}>
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
          <input id={`${id}-quantity`} inputMode="decimal" value={quantity} onChange={(e) => setQuantity(e.target.value.trim())} />
        </div>
        <button
          type="button"
          disabled={!item || !(Number(quantity) > 0)}
          onClick={() => {
            onChange([...parts, { itemId: item, quantity }]);
            setItem('');
            setQuantity('1');
          }}
        >
          Add part
        </button>
      </div>
      {parts.length > 0 && (
        <ul aria-label="Parts used">
          {parts.map((p) => (
            <li key={p.itemId}>
              {p.quantity} × {nameOf(p.itemId)}{' '}
              <button type="button" onClick={() => onChange(parts.filter((x) => x.itemId !== p.itemId))}>
                Remove
              </button>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
