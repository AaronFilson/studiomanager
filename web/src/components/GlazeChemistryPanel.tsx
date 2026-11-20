import { exportGlaze, type GlazeRecipe } from '@studiomanager/api-client';
import {
  analyzeRecipe,
  checkLimits,
  CONE_BAND_LABELS,
  parseConeBand,
  type ConeBand,
  type OxideKey,
} from '@studiomanager/glaze-chemistry';
import { useId, useMemo, useState } from 'react';

import { ErrorMessage } from './ErrorMessage';

const CONE_BANDS: ConeBand[] = ['CONE_04', 'CONE_6', 'CONE_10'];

function formatEntries(map: Partial<Record<OxideKey, number>>): [OxideKey, number][] {
  return (Object.entries(map) as [OxideKey, number | undefined][])
    .filter((entry): entry is [OxideKey, number] => entry[1] !== undefined && entry[1] > 0)
    .sort((a, b) => b[1] - a[1]);
}

function slugify(name: string): string {
  const slug = name
    .toLowerCase()
    .replaceAll(/[^a-z0-9]+/g, '-')
    .replaceAll(/^-|-$/g, '');
  return slug.length > 0 ? slug : 'glaze-recipe';
}

/**
 * Live glaze chemistry & Seger UMF analysis powered by `@studiomanager/glaze-chemistry`,
 * plus one-click export to Glazecalc (`glazecalc-export` v1 JSON).
 */
export function GlazeChemistryPanel({ studioId, glaze }: { studioId: string; glaze: GlazeRecipe }) {
  const id = useId();
  const [coneBand, setConeBand] = useState<ConeBand>(() => parseConeBand(glaze.cone));
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState<unknown>(null);

  const analysis = useMemo(
    () =>
      analyzeRecipe(
        glaze.lines.map((line) => ({
          material: line.material,
          amount: line.amount,
          kind: line.kind,
          unit: line.unit,
          chemistry: line.chemistry ?? null,
        })),
        { coneBand },
      ),
    [glaze.lines, coneBand],
  );

  const limits = useMemo(() => checkLimits(analysis.umf, coneBand), [analysis.umf, coneBand]);

  async function handleExport(): Promise<void> {
    setExporting(true);
    setExportError(null);
    try {
      const { data } = await exportGlaze({
        path: { studioId, glazeId: glaze.id },
        throwOnError: true,
      });
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      try {
        const link = document.createElement('a');
        link.href = url;
        link.download = `${slugify(glaze.name)}-glazecalc.json`;
        document.body.append(link);
        link.click();
        link.remove();
      } finally {
        URL.revokeObjectURL(url);
      }
    } catch (err: unknown) {
      setExportError(err);
    } finally {
      setExporting(false);
    }
  }

  const fluxEntries = [...formatEntries(analysis.r2o), ...formatEntries(analysis.ro)];
  const amphotericEntries = formatEntries(analysis.r2o3);
  const formerEntries = formatEntries(analysis.ro2);
  const otherEntries = formatEntries(analysis.other);

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <div className="row" style={{ justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 id={`${id}-title`}>Chemistry &amp; UMF analysis</h2>
        <div className="row">
          <button
            type="button"
            disabled={exporting}
            onClick={() => {
              void handleExport();
            }}
          >
            {exporting ? 'Exporting…' : 'Export for Glazecalc'}
          </button>
          <a href="https://glazecalcapp.com" target="_blank" rel="noreferrer">
            Open Glazecalc
          </a>
        </div>
      </div>

      {exportError !== null && <ErrorMessage error={exportError} />}

      {!analysis.valid ? (
        <p className="badge">
          Not enough matched flux oxides to calculate a Unity Molecular Formula yet.
        </p>
      ) : (
        <>
          <dl className="facts">
            <dt>R2O : RO fluxes</dt>
            <dd>
              {analysis.r2oTotal.toFixed(2)} : {analysis.roTotal.toFixed(2)}
            </dd>
            <dt>SiO2 : Al2O3 (Stull)</dt>
            <dd>
              {analysis.siAlRatio !== null ? `${analysis.siAlRatio.toFixed(2)} · ` : ''}
              {analysis.stull.label}
            </dd>
            <dt>Thermal expansion (CTE)</dt>
            <dd>
              {analysis.ctePerC !== null
                ? `${analysis.ctePerC.toFixed(2)} × 10⁻⁶ /°C (${analysis.expansionRating})`
                : 'Unknown'}
            </dd>
            <dt>Loss on ignition (LOI)</dt>
            <dd>{analysis.loiPercent.toFixed(1)}%</dd>
          </dl>

          <table className="table">
            <caption>Unity Molecular Formula (fluxes = 1.00)</caption>
            <thead>
              <tr>
                <th scope="col">Fluxes (R2O / RO)</th>
                <th scope="col">Amphoteric (R2O3)</th>
                <th scope="col">Glass formers (RO2)</th>
                {otherEntries.length > 0 && <th scope="col">Other</th>}
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  {fluxEntries.map(([ox, val]) => (
                    <div key={ox}>
                      <code>{ox}</code> {val.toFixed(3)}
                    </div>
                  ))}
                </td>
                <td>
                  {amphotericEntries.length === 0
                    ? '—'
                    : amphotericEntries.map(([ox, val]) => (
                        <div key={ox}>
                          <code>{ox}</code> {val.toFixed(3)}
                        </div>
                      ))}
                </td>
                <td>
                  {formerEntries.length === 0
                    ? '—'
                    : formerEntries.map(([ox, val]) => (
                        <div key={ox}>
                          <code>{ox}</code> {val.toFixed(3)}
                        </div>
                      ))}
                </td>
                {otherEntries.length > 0 && (
                  <td>
                    {otherEntries.map(([ox, val]) => (
                      <div key={ox}>
                        <code>{ox}</code> {val.toFixed(3)}
                      </div>
                    ))}
                  </td>
                )}
              </tr>
            </tbody>
          </table>

          <div className="stack">
            <div className="field" style={{ maxWidth: '16rem' }}>
              <label htmlFor={`${id}-cone-band`}>Cone limit formula</label>
              <select
                id={`${id}-cone-band`}
                value={coneBand}
                onChange={(e) => setConeBand(e.target.value as ConeBand)}
              >
                {CONE_BANDS.map((band) => (
                  <option key={band} value={band}>
                    {CONE_BAND_LABELS[band]}
                  </option>
                ))}
              </select>
            </div>
            {limits.outOfRange.length === 0 ? (
              <p className="badge">
                All {String(limits.checks.length)} checked oxides are within typical {limits.bandLabel} limits.
              </p>
            ) : (
              <ul className="timeline">
                {limits.outOfRange.map((check) => (
                  <li key={check.oxide} className="warning">
                    {check.message}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}

      {analysis.unmatchedMaterials.length > 0 && (
        <p className="badge warning">
          Unmatched materials (excluded from UMF): {analysis.unmatchedMaterials.join(', ')}.
        </p>
      )}
    </section>
  );
}
