import { calculateSlurryAdjustment } from '@studiomanager/glaze-chemistry';
import { useId, useMemo, useState } from 'react';

type AmountMode = 'volume' | 'mass';

/**
 * Bucket-side slurry specific gravity adjustment helper powered by `@studiomanager/glaze-chemistry`.
 */
export function SlurrySgHelper({ targetSpecificGravity }: { targetSpecificGravity?: string }) {
  const id = useId();
  const [measured, setMeasured] = useState('');
  const [target, setTarget] = useState(targetSpecificGravity ?? '1.45');
  const [amount, setAmount] = useState('1000');
  const [mode, setMode] = useState<AmountMode>('volume');

  const computed = useMemo(() => {
    const measuredSg = Number(measured);
    const targetSg = Number(target);
    const qty = Number(amount);
    if (!Number.isFinite(measuredSg) || !Number.isFinite(targetSg) || !Number.isFinite(qty)) {
      return null;
    }
    if (measuredSg <= 1.0 || measuredSg >= 2.6 || targetSg <= 1.0 || targetSg >= 2.6 || qty <= 0) {
      return null;
    }
    try {
      return calculateSlurryAdjustment(
        mode === 'volume'
          ? { measuredSg, targetSg, slurryVolumeMl: qty }
          : { measuredSg, targetSg, slurryMassG: qty },
      );
    } catch {
      return null;
    }
  }, [measured, target, amount, mode]);

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Slurry SG helper</h2>
      <p className="badge">
        Measure 100 mL of stirred glaze on a gram scale (e.g. 148 g = SG 1.48) to calculate the exact adjustment.
      </p>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-measured`}>Measured SG</label>
          <input
            id={`${id}-measured`}
            inputMode="decimal"
            placeholder="1.50"
            value={measured}
            onChange={(e) => setMeasured(e.target.value.trim())}
          />
        </div>
        <div className="field">
          <label htmlFor={`${id}-target`}>Target SG</label>
          <input
            id={`${id}-target`}
            inputMode="decimal"
            placeholder="1.45"
            value={target}
            onChange={(e) => setTarget(e.target.value.trim())}
          />
        </div>
      </div>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-amount`}>{mode === 'volume' ? 'Slurry volume (mL)' : 'Slurry mass (g)'}</label>
          <input
            id={`${id}-amount`}
            inputMode="decimal"
            placeholder="1000"
            value={amount}
            onChange={(e) => setAmount(e.target.value.trim())}
          />
        </div>
        <div className="field">
          <label htmlFor={`${id}-mode`}>Measured by</label>
          <select id={`${id}-mode`} value={mode} onChange={(e) => setMode(e.target.value as AmountMode)}>
            <option value="volume">Volume (mL)</option>
            <option value="mass">Mass (g)</option>
          </select>
        </div>
      </div>

      {measured.trim() !== '' && computed === null && (
        <p className="warning" role="status">
          Enter specific gravities between 1.01 and 2.59 (for example, 1.48 for 148 g per 100 mL) and a positive{' '}
          {mode === 'volume' ? 'volume' : 'mass'}.
        </p>
      )}

      {computed && (
        <div className="stack" aria-live="polite">
          {computed.action === 'on_target' && <p>Right on target specific gravity.</p>}
          {computed.action === 'add_water' && (
            <p>
              <strong>Too thick:</strong> add <strong>{computed.waterToAddG.toFixed(1)} g</strong> (mL) of water
              (resulting slurry ~{computed.resultingVolumeMl.toFixed(0)} mL).
            </p>
          )}
          {computed.action === 'thicken' && (
            <p>
              <strong>Too thin:</strong> let settle and decant{' '}
              <strong>{computed.waterToDecantG.toFixed(1)} g</strong> (mL) of water, or stir in{' '}
              <strong>{computed.dryGlazeToAddG.toFixed(1)} g</strong> of dry glaze mix.
            </p>
          )}
          <p className="badge">
            Estimated bucket contents: {computed.estimatedDrySolidsG.toFixed(0)} g dry solids +{' '}
            {computed.estimatedWaterG.toFixed(0)} g water.
          </p>
        </div>
      )}
    </section>
  );
}
