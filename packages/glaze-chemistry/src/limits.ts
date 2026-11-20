/**
 * Cone limit formula checks (Cone 04, Cone 6, Cone 10 bands).
 *
 * Ranges follow standard ceramic studio guidelines (Hesselberth & Roy / Currie / Digitalfire)
 * and Aaron Filson's MIT-licensed Glazecalc (lib/recipe/limits).
 */

import type { OxideKey } from './oxides';

export type ConeBand = 'CONE_04' | 'CONE_6' | 'CONE_10';

export interface OxideLimit {
  /** Oxide key, or `'R2O'` for combined alkalis (`Na2O + K2O`). */
  oxide: OxideKey | 'R2O';
  label: string;
  min: number;
  max: number;
}

export interface LimitCheck {
  oxide: OxideKey | 'R2O';
  label: string;
  actual: number;
  min: number;
  max: number;
  status: 'ok' | 'low' | 'high';
  message: string;
}

export interface LimitReport {
  band: ConeBand;
  bandLabel: string;
  checks: LimitCheck[];
  outOfRange: LimitCheck[];
}

export const CONE_BAND_LABELS: Record<ConeBand, string> = {
  CONE_04: 'Cone 04 (low fire)',
  CONE_6: 'Cone 6 (mid fire)',
  CONE_10: 'Cone 10 (high fire)',
};

export const CONE_LIMITS: Record<ConeBand, readonly OxideLimit[]> = {
  CONE_04: [
    { oxide: 'SiO2', label: 'SiO2', min: 1.5, max: 3.0 },
    { oxide: 'Al2O3', label: 'Al2O3', min: 0.1, max: 0.25 },
    { oxide: 'B2O3', label: 'B2O3', min: 0.15, max: 0.6 },
    { oxide: 'R2O', label: 'Na2O + K2O', min: 0.0, max: 0.5 },
    { oxide: 'CaO', label: 'CaO', min: 0.0, max: 0.5 },
    { oxide: 'MgO', label: 'MgO', min: 0.0, max: 0.15 },
    { oxide: 'ZnO', label: 'ZnO', min: 0.0, max: 0.3 },
    { oxide: 'Li2O', label: 'Li2O', min: 0.0, max: 0.25 },
  ],
  CONE_6: [
    { oxide: 'SiO2', label: 'SiO2', min: 2.5, max: 4.0 },
    { oxide: 'Al2O3', label: 'Al2O3', min: 0.25, max: 0.45 },
    { oxide: 'B2O3', label: 'B2O3', min: 0.1, max: 0.3 },
    { oxide: 'R2O', label: 'Na2O + K2O', min: 0.2, max: 0.5 },
    { oxide: 'CaO', label: 'CaO', min: 0.3, max: 0.7 },
    { oxide: 'MgO', label: 'MgO', min: 0.0, max: 0.3 },
    { oxide: 'ZnO', label: 'ZnO', min: 0.0, max: 0.3 },
    { oxide: 'Li2O', label: 'Li2O', min: 0.0, max: 0.15 },
    { oxide: 'BaO', label: 'BaO', min: 0.0, max: 0.3 },
    { oxide: 'SrO', label: 'SrO', min: 0.0, max: 0.3 },
  ],
  CONE_10: [
    { oxide: 'SiO2', label: 'SiO2', min: 3.0, max: 5.0 },
    { oxide: 'Al2O3', label: 'Al2O3', min: 0.3, max: 0.55 },
    { oxide: 'B2O3', label: 'B2O3', min: 0.0, max: 0.15 },
    { oxide: 'R2O', label: 'Na2O + K2O', min: 0.2, max: 0.45 },
    { oxide: 'CaO', label: 'CaO', min: 0.4, max: 0.8 },
    { oxide: 'MgO', label: 'MgO', min: 0.0, max: 0.35 },
    { oxide: 'ZnO', label: 'ZnO', min: 0.0, max: 0.2 },
    { oxide: 'Li2O', label: 'Li2O', min: 0.0, max: 0.15 },
    { oxide: 'BaO', label: 'BaO', min: 0.0, max: 0.3 },
    { oxide: 'SrO', label: 'SrO', min: 0.0, max: 0.3 },
  ],
};

/** Infers the nearest cone limit band from a free-text cone string such as `"04"`, `"6"`, `"5-6"` or `"10"`. */
export function parseConeBand(cone?: string | null): ConeBand {
  if (!cone) {
    return 'CONE_6';
  }
  const match = /\b(0\d{1,2}|\d{1,2})\b/.exec(cone.trim());
  if (!match?.[1]) {
    return 'CONE_6';
  }
  const token = match[1];
  // Leading zero cones (022..01) are low-fire
  if (token.startsWith('0')) {
    return 'CONE_04';
  }
  const num = Number(token);
  if (num <= 3) {
    return 'CONE_04';
  }
  if (num >= 8) {
    return 'CONE_10';
  }
  return 'CONE_6';
}

/** Checks a UMF against the typical oxide limit formula for `band`. */
export function checkLimits(umf: Partial<Record<OxideKey, number>>, band: ConeBand): LimitReport {
  const limits = CONE_LIMITS[band];
  const checks: LimitCheck[] = [];

  for (const limit of limits) {
    const actual =
      limit.oxide === 'R2O' ? (umf.Na2O ?? 0) + (umf.K2O ?? 0) : (umf[limit.oxide] ?? 0);

    // Skip optional zero-min oxides that aren't in the glaze at all
    if (limit.min === 0 && actual === 0) {
      continue;
    }

    let status: LimitCheck['status'] = 'ok';
    let message = `${limit.label} (${actual.toFixed(2)}) is within ${limit.min.toFixed(2)}–${limit.max.toFixed(2)}.`;
    if (actual < limit.min - 0.0005) {
      status = 'low';
      message = `${limit.label} (${actual.toFixed(2)}) is below the typical ${CONE_BAND_LABELS[band]} minimum of ${limit.min.toFixed(2)}.`;
    } else if (actual > limit.max + 0.0005) {
      status = 'high';
      message = `${limit.label} (${actual.toFixed(2)}) is above the typical ${CONE_BAND_LABELS[band]} maximum of ${limit.max.toFixed(2)}.`;
    }

    checks.push({
      oxide: limit.oxide,
      label: limit.label,
      actual,
      min: limit.min,
      max: limit.max,
      status,
      message,
    });
  }

  return {
    band,
    bandLabel: CONE_BAND_LABELS[band],
    checks,
    outOfRange: checks.filter((c) => c.status !== 'ok'),
  };
}
