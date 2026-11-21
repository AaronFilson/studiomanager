/**
 * Unity Molecular Formula (Seger UMF), Stull surface prediction, and Appen thermal expansion.
 *
 * Modeled on Aaron Filson's MIT-licensed Glazecalc (https://github.com/AaronFilson/glazecalc,
 * lib/chemistry/umf & lib/chemistry/expansion).
 */

import { checkLimits, CONE_BAND_LABELS, parseConeBand, type ConeBand, type LimitReport } from './limits';
import { findMaterial, type OxideAnalysis } from './materials';
import { isFluxGroup, normalizeOxideWithFactor, OXIDE_KEYS, OXIDES, type OxideKey } from './oxides';

export interface ImportedChemistrySnapshot {
  materialId?: string | null;
  analysis?: Readonly<Record<string, number | string>> | null;
  loi?: number | string | null;
}

export interface RecipeChemistryLine {
  material: string;
  amount: number | string;
  kind?: 'BASE' | 'ADDITION';
  unit?: 'PARTS' | 'PERCENT' | 'GRAMS';
  chemistry?: ImportedChemistrySnapshot | null;
}

export interface AnalyzeRecipeOptions {
  /** Whether `ADDITION` lines count toward UMF and expansion. Defaults to `true` (except `GRAMS`). */
  includeAdditives?: boolean;
  /** Optional cone label (e.g. `"6"`, `"10"`, `"04"`) to pick the default cone limit band. */
  cone?: string | null;
  /** Explicit cone band override. */
  coneBand?: ConeBand;
}

export type StullSurface = 'underfired' | 'matte' | 'satin' | 'glossy' | 'high_silica_matte' | 'unknown';

export interface StullPrediction {
  siO2: number;
  al2O3: number;
  siAlRatio: number | null;
  surface: StullSurface;
  label: string;
}

export type ExpansionRating = 'low' | 'medium' | 'high' | 'unknown';

export interface LineResolution {
  material: string;
  source: 'snapshot' | 'library' | 'unmatched' | 'skipped_grams';
  matchedName?: string;
}

export interface GlazeAnalysis {
  /** Whether a UMF could be computed (requires positive flux moles). */
  valid: boolean;
  /** Full Seger UMF (normalized so total fluxes R2O + RO = 1.0). */
  umf: Partial<Record<OxideKey, number>>;
  /** Fired oxide composition by weight % (sums to 100%). */
  firedWeightPercent: Partial<Record<OxideKey, number>>;
  /** Alkali fluxes (Li2O, Na2O, K2O) in UMF. */
  r2o: Partial<Record<OxideKey, number>>;
  /** Alkaline-earth / divalent fluxes (MgO, CaO, SrO, BaO, ZnO, PbO) in UMF. */
  ro: Partial<Record<OxideKey, number>>;
  /** Amphoteric / R2O3 oxides (Al2O3, B2O3, Fe2O3, Cr2O3) in UMF. */
  r2o3: Partial<Record<OxideKey, number>>;
  /** Glass formers / RO2 oxides (SiO2, TiO2, ZrO2, SnO2) in UMF. */
  ro2: Partial<Record<OxideKey, number>>;
  /** Other oxides (P2O5, MnO, CoO, CuO, NiO, F) in UMF. */
  other: Partial<Record<OxideKey, number>>;
  /** Total R2O flux share (0..1). */
  r2oTotal: number;
  /** Total RO flux share (0..1). */
  roTotal: number;
  /** SiO2 : Al2O3 molar ratio, or null when Al2O3 is 0. */
  siAlRatio: number | null;
  /** Stull chart surface prediction. */
  stull: StullPrediction;
  /** Theoretical linear thermal expansion (Appen) in × 10^-7 / °C (stoneware ~60–72). */
  cte: number | null;
  /** Theoretical linear thermal expansion in × 10^-6 / °C (stoneware ~6.0–7.2). */
  ctePerC: number | null;
  expansionRating: ExpansionRating;
  /** Cone limit formula report. */
  limits: LimitReport;
  /** Recipe-weighted loss on ignition (%). */
  loiPercent: number;
  /** Per-line chemistry source resolution. */
  lineResolutions: LineResolution[];
  /** Deduplicated list of materials with no snapshot and no library match. */
  unmatchedMaterials: string[];
}

function parseSnapshotAnalysis(
  snapshot: ImportedChemistrySnapshot | null | undefined,
): { analysis: OxideAnalysis; loi: number } | undefined {
  if (!snapshot?.analysis) {
    return undefined;
  }
  const analysis: OxideAnalysis = {};
  let hasAny = false;
  for (const [rawKey, rawVal] of Object.entries(snapshot.analysis)) {
    const normalized = normalizeOxideWithFactor(rawKey);
    if (!normalized) {
      continue;
    }
    const num = typeof rawVal === 'number' ? rawVal : Number(rawVal);
    if (Number.isFinite(num) && num > 0) {
      analysis[normalized.key] = (analysis[normalized.key] ?? 0) + num * normalized.weightFactor;
      hasAny = true;
    }
  }
  if (!hasAny) {
    return undefined;
  }
  const rawLoi = snapshot.loi;
  const parsedLoi = rawLoi === null || rawLoi === undefined ? 0 : Number(rawLoi);
  const loi = Number.isFinite(parsedLoi) && parsedLoi >= 0 ? parsedLoi : 0;
  return { analysis, loi };
}

function predictStull(siO2: number, al2O3: number): StullPrediction {
  if (siO2 <= 0 && al2O3 <= 0) {
    return { siO2: 0, al2O3: 0, siAlRatio: null, surface: 'unknown', label: 'Unknown' };
  }
  if (siO2 < 0.5) {
    return {
      siO2: round3(siO2),
      al2O3: round3(al2O3),
      siAlRatio: al2O3 > 0.0001 ? round2(siO2 / al2O3) : null,
      surface: 'underfired',
      label: 'Insufficient silica (underfired / non-glassy)',
    };
  }
  if (al2O3 <= 0.0001) {
    return {
      siO2: round3(siO2),
      al2O3: 0,
      siAlRatio: null,
      surface: 'high_silica_matte',
      label: 'Very low alumina (fluid / devitrifying)',
    };
  }
  const ratio = siO2 / al2O3;
  if (al2O3 > 0.75 || ratio < 4.0) {
    return {
      siO2: round3(siO2),
      al2O3: round3(al2O3),
      siAlRatio: round2(ratio),
      surface: 'underfired',
      label: 'Underfired / dry alumina matte',
    };
  }
  if (ratio < 6.0) {
    return {
      siO2: round3(siO2),
      al2O3: round3(al2O3),
      siAlRatio: round2(ratio),
      surface: 'matte',
      label: 'Matte',
    };
  }
  if (ratio < 7.5) {
    return {
      siO2: round3(siO2),
      al2O3: round3(al2O3),
      siAlRatio: round2(ratio),
      surface: 'satin',
      label: 'Satin / semi-matte',
    };
  }
  if (ratio <= 12.0) {
    return {
      siO2: round3(siO2),
      al2O3: round3(al2O3),
      siAlRatio: round2(ratio),
      surface: 'glossy',
      label: 'Glossy',
    };
  }
  return {
    siO2: round3(siO2),
    al2O3: round3(al2O3),
    siAlRatio: round2(ratio),
    surface: 'high_silica_matte',
    label: 'High-silica matte',
  };
}

function computeAppenCte(moles: Partial<Record<OxideKey, number>>): number | null {
  let totalMoles = 0;
  for (const key of OXIDE_KEYS) {
    totalMoles += moles[key] ?? 0;
  }
  if (totalMoles <= 0) {
    return null;
  }

  const siO2MolPct = ((moles.SiO2 ?? 0) / totalMoles) * 100;
  // Appen's composition-dependent factor for SiO2 above 67 mol%
  const siO2Factor = siO2MolPct > 67 ? Math.max(5, 38 - (siO2MolPct - 67)) : 38;

  let cte = 0;
  for (const key of OXIDE_KEYS) {
    const m = moles[key] ?? 0;
    if (m <= 0) {
      continue;
    }
    const fraction = m / totalMoles;
    const factor = key === 'SiO2' ? siO2Factor : OXIDES[key].appenFactor;
    cte += fraction * factor;
  }
  return round1(cte);
}

function rateExpansion(cte: number | null): ExpansionRating {
  if (cte === null) {
    return 'unknown';
  }
  if (cte < 60) {
    return 'low';
  }
  if (cte <= 72) {
    return 'medium';
  }
  return 'high';
}

/**
 * Analyzes a glaze recipe into its Unity Molecular Formula (Seger UMF), Stull surface prediction,
 * Appen thermal expansion (CTE), and cone limit formula checks.
 */
export function analyzeRecipe(
  lines: readonly RecipeChemistryLine[],
  options: AnalyzeRecipeOptions = {},
): GlazeAnalysis {
  const includeAdditives = options.includeAdditives ?? true;
  const coneBand = options.coneBand ?? parseConeBand(options.cone);

  const numericLines = lines
    .map((line) => ({
      ...line,
      numericAmount: typeof line.amount === 'number' ? line.amount : Number(line.amount),
      kind: line.kind ?? 'BASE',
      unit: line.unit ?? 'PARTS',
    }))
    .filter((line) => Number.isFinite(line.numericAmount) && line.numericAmount > 0);

  let baseSum = numericLines
    .filter((l) => l.kind === 'BASE' && l.unit !== 'GRAMS')
    .reduce((sum, l) => sum + l.numericAmount, 0);
  if (baseSum <= 0) {
    baseSum = numericLines
      .filter((l) => l.unit !== 'GRAMS')
      .reduce((sum, l) => sum + l.numericAmount, 0);
  }

  const oxideWeights: Partial<Record<OxideKey, number>> = {};
  const lineResolutions: LineResolution[] = [];
  const unmatchedSet = new Set<string>();
  let totalParts = 0;
  let totalLoiWeight = 0;

  for (const line of numericLines) {
    if (line.kind === 'ADDITION' && !includeAdditives) {
      continue;
    }
    if (line.unit === 'GRAMS') {
      lineResolutions.push({ material: line.material, source: 'skipped_grams' });
      continue;
    }

    const parts =
      line.kind === 'ADDITION' && line.unit === 'PERCENT'
        ? line.numericAmount
        : baseSum > 0
          ? (line.numericAmount / baseSum) * 100
          : 0;
    if (parts <= 0) {
      continue;
    }

    const fromSnapshot = parseSnapshotAnalysis(line.chemistry);
    let analysis: OxideAnalysis;
    let loi: number;

    if (fromSnapshot) {
      analysis = fromSnapshot.analysis;
      loi = fromSnapshot.loi;
      lineResolutions.push({ material: line.material, source: 'snapshot' });
    } else {
      const mat = findMaterial(line.material);
      if (mat) {
        analysis = mat.analysis;
        loi = mat.loi;
        lineResolutions.push({ material: line.material, source: 'library', matchedName: mat.name });
      } else {
        lineResolutions.push({ material: line.material, source: 'unmatched' });
        unmatchedSet.add(line.material.trim());
        continue;
      }
    }

    totalParts += parts;
    totalLoiWeight += (parts * loi) / 100;
    for (const key of OXIDE_KEYS) {
      const pct = analysis[key];
      if (pct !== undefined && pct > 0) {
        oxideWeights[key] = (oxideWeights[key] ?? 0) + (parts * pct) / 100;
      }
    }
  }

  // Fired weight percentages & raw moles
  let totalFiredWeight = 0;
  const moles: Partial<Record<OxideKey, number>> = {};
  let fluxMoles = 0;

  for (const key of OXIDE_KEYS) {
    const wt = oxideWeights[key] ?? 0;
    if (wt <= 0) {
      continue;
    }
    totalFiredWeight += wt;
    const m = wt / OXIDES[key].molecularWeight;
    moles[key] = m;
    if (isFluxGroup(OXIDES[key].group)) {
      fluxMoles += m;
    }
  }

  const firedWeightPercent: Partial<Record<OxideKey, number>> = {};
  if (totalFiredWeight > 0) {
    for (const key of OXIDE_KEYS) {
      const wt = oxideWeights[key] ?? 0;
      if (wt > 0) {
        firedWeightPercent[key] = round2((wt / totalFiredWeight) * 100);
      }
    }
  }

  const umf: Partial<Record<OxideKey, number>> = {};
  const r2o: Partial<Record<OxideKey, number>> = {};
  const ro: Partial<Record<OxideKey, number>> = {};
  const r2o3: Partial<Record<OxideKey, number>> = {};
  const ro2: Partial<Record<OxideKey, number>> = {};
  const other: Partial<Record<OxideKey, number>> = {};

  let r2oTotal = 0;
  let roTotal = 0;

  if (fluxMoles > 0) {
    for (const key of OXIDE_KEYS) {
      const m = moles[key] ?? 0;
      if (m <= 0) {
        continue;
      }
      const value = m / fluxMoles;
      const rounded = round4(value);
      if (rounded <= 0) {
        continue;
      }
      umf[key] = rounded;
      switch (OXIDES[key].group) {
        case 'R2O':
          r2o[key] = rounded;
          r2oTotal += value;
          break;
        case 'RO':
          ro[key] = rounded;
          roTotal += value;
          break;
        case 'R2O3':
          r2o3[key] = rounded;
          break;
        case 'RO2':
          ro2[key] = rounded;
          break;
        case 'OTHER':
          other[key] = rounded;
          break;
      }
    }
  }

  const valid = fluxMoles > 0;
  const siO2 = umf.SiO2 ?? 0;
  const al2O3 = umf.Al2O3 ?? 0;
  const stull = predictStull(siO2, al2O3);
  const cte = computeAppenCte(moles);
  const ctePerC = cte === null ? null : round2(cte / 10);
  const limits: LimitReport = valid
    ? checkLimits(umf, coneBand)
    : { band: coneBand, bandLabel: CONE_BAND_LABELS[coneBand], checks: [], outOfRange: [] };
  const loiPercent = totalParts > 0 ? round1((totalLoiWeight / totalParts) * 100) : 0;

  return {
    valid,
    umf,
    firedWeightPercent,
    r2o,
    ro,
    r2o3,
    ro2,
    other,
    r2oTotal: round3(r2oTotal),
    roTotal: round3(roTotal),
    siAlRatio: stull.siAlRatio,
    stull,
    cte,
    ctePerC,
    expansionRating: rateExpansion(cte),
    limits,
    loiPercent,
    lineResolutions,
    unmatchedMaterials: [...unmatchedSet],
  };
}

function round1(v: number): number {
  return Math.round(v * 10) / 10;
}

function round2(v: number): number {
  return Math.round(v * 100) / 100;
}

function round3(v: number): number {
  return Math.round(v * 1000) / 1000;
}

function round4(v: number): number {
  return Math.round(v * 10000) / 10000;
}
