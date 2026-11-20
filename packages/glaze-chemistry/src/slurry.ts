/**
 * Slurry specific gravity adjustment calculator.
 *
 * Uses conservation of mass and volume (and Brongniart's formula for dry solids vs. water) to
 * compute the exact water to add (when thick) or water to decant / dry glaze to add (when thin).
 * Modeled on Aaron Filson's MIT-licensed Glazecalc (lib/recipe/slurry).
 */

export interface SlurryAdjustmentInput {
  /** Measured specific gravity of the slurry (g/mL, > 1.0). */
  measuredSg: number;
  /** Target specific gravity (g/mL, > 1.0). */
  targetSg: number;
  /** Current slurry volume in mL (provide either volumeMl or massG). */
  slurryVolumeMl?: number;
  /** Current slurry total mass in g (provide either volumeMl or massG). */
  slurryMassG?: number;
  /** Assumed dry glaze specific gravity (g/mL). Typical stoneware/porcelain dry blend is ~2.60. */
  dryMaterialSg?: number;
}

export type SlurryAction = 'on_target' | 'add_water' | 'thicken';

export interface SlurryAdjustmentResult {
  action: SlurryAction;
  slurryVolumeMl: number;
  slurryMassG: number;
  estimatedDrySolidsG: number;
  estimatedWaterG: number;
  /** Grams (= mL) of water to add when `measuredSg > targetSg`; 0 otherwise. */
  waterToAddG: number;
  /** Grams (= mL) of water to decant after settling when `measuredSg < targetSg`; 0 otherwise. */
  waterToDecantG: number;
  /** Grams of dry glaze mix to add when `measuredSg < targetSg`; 0 otherwise. */
  dryGlazeToAddG: number;
  /** Resulting slurry volume (mL) after adding water (or adding dry glaze). */
  resultingVolumeMl: number;
}

const DEFAULT_DRY_SG = 2.6;
const TARGET_EPSILON = 0.0005;

export function calculateSlurryAdjustment(input: SlurryAdjustmentInput): SlurryAdjustmentResult {
  const drySg = input.dryMaterialSg ?? DEFAULT_DRY_SG;
  if (!(drySg > 1.5 && drySg <= 6.0)) {
    throw new RangeError('Dry material specific gravity must be between 1.5 and 6.0.');
  }
  if (!(input.measuredSg > 1.0 && input.measuredSg < drySg)) {
    throw new RangeError(`Measured specific gravity must be between 1.0 and ${drySg.toFixed(2)}.`);
  }
  if (!(input.targetSg > 1.0 && input.targetSg < drySg)) {
    throw new RangeError(`Target specific gravity must be between 1.0 and ${drySg.toFixed(2)}.`);
  }

  let volumeMl: number;
  let massG: number;
  if (input.slurryVolumeMl !== undefined) {
    if (!(input.slurryVolumeMl > 0)) {
      throw new RangeError('Slurry volume must be greater than 0.');
    }
    volumeMl = input.slurryVolumeMl;
    massG = volumeMl * input.measuredSg;
  } else if (input.slurryMassG !== undefined) {
    if (!(input.slurryMassG > 0)) {
      throw new RangeError('Slurry mass must be greater than 0.');
    }
    massG = input.slurryMassG;
    volumeMl = massG / input.measuredSg;
  } else {
    throw new RangeError('Provide either slurryVolumeMl or slurryMassG.');
  }

  // Brongniart's formula: dry solids in slurry of volume V and SG S
  const estimatedDrySolidsG = volumeMl * (input.measuredSg - 1) * (drySg / (drySg - 1));
  const estimatedWaterG = massG - estimatedDrySolidsG;

  const diff = input.measuredSg - input.targetSg;
  if (Math.abs(diff) <= TARGET_EPSILON) {
    return {
      action: 'on_target',
      slurryVolumeMl: round1(volumeMl),
      slurryMassG: round1(massG),
      estimatedDrySolidsG: round1(estimatedDrySolidsG),
      estimatedWaterG: round1(estimatedWaterG),
      waterToAddG: 0,
      waterToDecantG: 0,
      dryGlazeToAddG: 0,
      resultingVolumeMl: round1(volumeMl),
    };
  }

  if (diff > 0) {
    // Too thick: add water
    const waterToAddG = (volumeMl * (input.measuredSg - input.targetSg)) / (input.targetSg - 1);
    return {
      action: 'add_water',
      slurryVolumeMl: round1(volumeMl),
      slurryMassG: round1(massG),
      estimatedDrySolidsG: round1(estimatedDrySolidsG),
      estimatedWaterG: round1(estimatedWaterG),
      waterToAddG: round1(waterToAddG),
      waterToDecantG: 0,
      dryGlazeToAddG: 0,
      resultingVolumeMl: round1(volumeMl + waterToAddG),
    };
  }

  // Too thin: decant water OR add dry glaze mix
  const waterToDecantG = (volumeMl * (input.targetSg - input.measuredSg)) / (input.targetSg - 1);
  const dryGlazeToAddG =
    (volumeMl * (input.targetSg - input.measuredSg)) / (1 - input.targetSg / drySg);

  return {
    action: 'thicken',
    slurryVolumeMl: round1(volumeMl),
    slurryMassG: round1(massG),
    estimatedDrySolidsG: round1(estimatedDrySolidsG),
    estimatedWaterG: round1(estimatedWaterG),
    waterToAddG: 0,
    waterToDecantG: round1(waterToDecantG),
    dryGlazeToAddG: round1(dryGlazeToAddG),
    resultingVolumeMl: round1(volumeMl + dryGlazeToAddG / drySg),
  };
}

function round1(value: number): number {
  return Math.round(value * 10) / 10;
}
