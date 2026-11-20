/**
 * Ceramic oxide constants (molecular weights, Seger groups, and Appen thermal expansion factors).
 *
 * Modeled on Aaron Filson's MIT-licensed Glazecalc (https://github.com/AaronFilson/glazecalc,
 * lib/chemistry/oxides & lib/chemistry/expansion).
 */

export type OxideGroup = 'R2O' | 'RO' | 'R2O3' | 'RO2' | 'OTHER';

export const OXIDE_KEYS = [
  'Li2O',
  'Na2O',
  'K2O',
  'MgO',
  'CaO',
  'SrO',
  'BaO',
  'ZnO',
  'PbO',
  'Al2O3',
  'B2O3',
  'Fe2O3',
  'Cr2O3',
  'SiO2',
  'TiO2',
  'ZrO2',
  'SnO2',
  'P2O5',
  'MnO',
  'CoO',
  'CuO',
  'NiO',
  'F',
] as const;

export type OxideKey = (typeof OXIDE_KEYS)[number];

export interface OxideInfo {
  key: OxideKey;
  group: OxideGroup;
  molecularWeight: number;
  /** Appen linear thermal expansion factor (× 10^-7 / °C per mole fraction). */
  appenFactor: number;
}

export const OXIDES: Record<OxideKey, OxideInfo> = {
  // Alkali fluxes (R2O)
  Li2O: { key: 'Li2O', group: 'R2O', molecularWeight: 29.88, appenFactor: 270 },
  Na2O: { key: 'Na2O', group: 'R2O', molecularWeight: 61.98, appenFactor: 395 },
  K2O: { key: 'K2O', group: 'R2O', molecularWeight: 94.2, appenFactor: 465 },

  // Alkaline earth & divalent fluxes (RO)
  MgO: { key: 'MgO', group: 'RO', molecularWeight: 40.3, appenFactor: 60 },
  CaO: { key: 'CaO', group: 'RO', molecularWeight: 56.08, appenFactor: 130 },
  SrO: { key: 'SrO', group: 'RO', molecularWeight: 103.62, appenFactor: 160 },
  BaO: { key: 'BaO', group: 'RO', molecularWeight: 153.33, appenFactor: 200 },
  ZnO: { key: 'ZnO', group: 'RO', molecularWeight: 81.38, appenFactor: 50 },
  PbO: { key: 'PbO', group: 'RO', molecularWeight: 223.2, appenFactor: 130 },

  // Amphoteric / intermediates (R2O3)
  Al2O3: { key: 'Al2O3', group: 'R2O3', molecularWeight: 101.96, appenFactor: -30 },
  B2O3: { key: 'B2O3', group: 'R2O3', molecularWeight: 69.62, appenFactor: -50 },
  Fe2O3: { key: 'Fe2O3', group: 'R2O3', molecularWeight: 159.69, appenFactor: 55 },
  Cr2O3: { key: 'Cr2O3', group: 'R2O3', molecularWeight: 151.99, appenFactor: 51 },

  // Glass formers / opacifiers (RO2)
  SiO2: { key: 'SiO2', group: 'RO2', molecularWeight: 60.08, appenFactor: 38 },
  TiO2: { key: 'TiO2', group: 'RO2', molecularWeight: 79.87, appenFactor: 30 },
  ZrO2: { key: 'ZrO2', group: 'RO2', molecularWeight: 123.22, appenFactor: -60 },
  SnO2: { key: 'SnO2', group: 'RO2', molecularWeight: 150.71, appenFactor: 20 },

  // Other colorants / glass modifiers
  P2O5: { key: 'P2O5', group: 'OTHER', molecularWeight: 141.94, appenFactor: 140 },
  MnO: { key: 'MnO', group: 'OTHER', molecularWeight: 70.94, appenFactor: 105 },
  CoO: { key: 'CoO', group: 'OTHER', molecularWeight: 74.93, appenFactor: 50 },
  CuO: { key: 'CuO', group: 'OTHER', molecularWeight: 79.55, appenFactor: 30 },
  NiO: { key: 'NiO', group: 'OTHER', molecularWeight: 74.69, appenFactor: 50 },
  F: { key: 'F', group: 'OTHER', molecularWeight: 19.0, appenFactor: 0 },
};

const NORMALIZED_OXIDES: Record<string, OxideKey> = {
  li2o: 'Li2O',
  na2o: 'Na2O',
  k2o: 'K2O',
  mgo: 'MgO',
  cao: 'CaO',
  sro: 'SrO',
  bao: 'BaO',
  zno: 'ZnO',
  pbo: 'PbO',
  al2o3: 'Al2O3',
  b2o3: 'B2O3',
  fe2o3: 'Fe2O3',
  feo: 'Fe2O3',
  cr2o3: 'Cr2O3',
  sio2: 'SiO2',
  tio2: 'TiO2',
  zro2: 'ZrO2',
  sno2: 'SnO2',
  p2o5: 'P2O5',
  mno: 'MnO',
  mno2: 'MnO',
  coo: 'CoO',
  cuo: 'CuO',
  cu2o: 'CuO',
  nio: 'NiO',
  f: 'F',
};

/** Maps common oxide spellings (case-insensitive, e.g. `sio2`, `FeO`) to a canonical `OxideKey`. */
export function normalizeOxideKey(raw: string): OxideKey | undefined {
  const cleaned = raw.trim().replaceAll(/[^A-Za-z0-9]/g, '').toLowerCase();
  return NORMALIZED_OXIDES[cleaned];
}

export function isFluxGroup(group: OxideGroup): boolean {
  return group === 'R2O' || group === 'RO';
}
