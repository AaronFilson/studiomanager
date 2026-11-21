/**
 * Pure-TypeScript ceramic glaze chemistry engine for Studio Manager (web & mobile).
 *
 * Modeled on Aaron Filson's MIT-licensed Glazecalc (https://github.com/AaronFilson/glazecalc).
 */

export {
  isFluxGroup,
  normalizeOxideKey,
  normalizeOxideWithFactor,
  OXIDE_KEYS,
  OXIDES,
  type NormalizedOxideEntry,
  type OxideGroup,
  type OxideInfo,
  type OxideKey,
} from './oxides';
export { findMaterial, MATERIALS, type MaterialDefinition, type OxideAnalysis } from './materials';
export {
  checkLimits,
  CONE_BAND_LABELS,
  CONE_LIMITS,
  parseConeBand,
  type ConeBand,
  type LimitCheck,
  type LimitReport,
  type OxideLimit,
} from './limits';
export {
  calculateSlurryAdjustment,
  type SlurryAction,
  type SlurryAdjustmentInput,
  type SlurryAdjustmentResult,
} from './slurry';
export {
  analyzeRecipe,
  type AnalyzeRecipeOptions,
  type ExpansionRating,
  type GlazeAnalysis,
  type ImportedChemistrySnapshot,
  type LineResolution,
  type RecipeChemistryLine,
  type StullPrediction,
  type StullSurface,
} from './umf';
