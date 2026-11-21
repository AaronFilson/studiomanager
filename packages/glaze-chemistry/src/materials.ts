/**
 * Built-in studio ceramic materials database (oxide weight-% analyses & LOI).
 *
 * Values come from standard ceramic manufacturer datasheets and reference tables, modeled on
 * Aaron Filson's MIT-licensed Glazecalc (https://github.com/AaronFilson/glazecalc).
 */

import type { OxideKey } from './oxides';

export type OxideAnalysis = Partial<Record<OxideKey, number>>;

export interface MaterialDefinition {
  name: string;
  aliases: readonly string[];
  /** Oxide percentages by weight (%). */
  analysis: OxideAnalysis;
  /** Loss on ignition (%). */
  loi: number;
}

export const MATERIALS: readonly MaterialDefinition[] = [
  // --- Feldspars & Feldspathoids ---
  {
    name: 'Custer Feldspar',
    aliases: ['custer', 'custer potash feldspar', 'potash feldspar', 'k-spar', 'kspar', 'orthoclase'],
    analysis: { SiO2: 68.5, Al2O3: 17.0, K2O: 10.0, Na2O: 3.0, CaO: 0.3, Fe2O3: 0.08 },
    loi: 0.3,
  },
  {
    name: 'Minspar 200',
    aliases: ['minspar', 'minspar 200 soda feldspar', 'soda feldspar', 'nc-4', 'nc4', 'nc 4', 'albite'],
    analysis: { SiO2: 67.6, Al2O3: 19.3, Na2O: 7.0, K2O: 4.1, CaO: 1.5, Fe2O3: 0.07 },
    loi: 0.4,
  },
  {
    name: 'G-200 Feldspar',
    aliases: ['g200', 'g-200', 'g 200', 'g-200 hp', 'g200 hp'],
    analysis: { SiO2: 66.5, Al2O3: 18.4, K2O: 10.7, Na2O: 3.1, CaO: 0.8, Fe2O3: 0.08 },
    loi: 0.3,
  },
  {
    name: 'G-200 EU',
    aliases: ['g200 eu', 'g-200eu', 'g200eu'],
    analysis: { SiO2: 66.8, Al2O3: 17.8, K2O: 11.4, Na2O: 3.2, CaO: 0.3, Fe2O3: 0.06 },
    loi: 0.3,
  },
  {
    name: 'Mahavir Feldspar',
    aliases: ['mahavir', 'mahavir potash feldspar'],
    analysis: { SiO2: 66.4, Al2O3: 18.3, K2O: 11.5, Na2O: 2.8, CaO: 0.2, Fe2O3: 0.06 },
    loi: 0.3,
  },
  {
    name: 'Kona F-4',
    aliases: ['kona f4', 'kona f 4', 'f-4', 'f4 feldspar', 'f-4 feldspar'],
    analysis: { SiO2: 66.8, Al2O3: 19.6, Na2O: 6.9, K2O: 4.8, CaO: 1.7, Fe2O3: 0.04 },
    loi: 0.2,
  },
  {
    name: 'Nepheline Syenite',
    aliases: ['neph sy', 'neph sye', 'nepheline syenite 270', 'minex 4', 'minex', 'syenite'],
    analysis: { SiO2: 60.7, Al2O3: 23.3, Na2O: 9.8, K2O: 4.6, CaO: 0.7, Fe2O3: 0.07 },
    loi: 0.7,
  },
  {
    name: 'Cornwall Stone',
    aliases: ['cornish stone', 'cornwall', 'cornish'],
    analysis: { SiO2: 72.6, Al2O3: 15.8, K2O: 4.3, Na2O: 3.2, CaO: 1.8, MgO: 0.1, Fe2O3: 0.2, F: 1.0 },
    loi: 1.2,
  },
  {
    name: 'Spodumene',
    aliases: ['australian spodumene', 'lithium feldspar'],
    analysis: { SiO2: 64.5, Al2O3: 26.8, Li2O: 7.2, Na2O: 0.3, K2O: 0.3, Fe2O3: 0.15 },
    loi: 0.5,
  },
  {
    name: 'Petalite',
    aliases: ['lithium petalite'],
    analysis: { SiO2: 77.0, Al2O3: 17.5, Li2O: 4.4, Na2O: 0.2, K2O: 0.2, Fe2O3: 0.05 },
    loi: 0.5,
  },

  // --- Frits & Borates ---
  {
    name: 'Ferro Frit 3134',
    aliases: ['frit 3134', '3134', 'ferro 3134'],
    analysis: { SiO2: 46.5, B2O3: 23.1, CaO: 20.1, Na2O: 10.3 },
    loi: 0,
  },
  {
    name: 'Ferro Frit 3124',
    aliases: ['frit 3124', '3124', 'ferro 3124'],
    analysis: { SiO2: 55.3, Al2O3: 9.9, B2O3: 13.8, CaO: 14.1, Na2O: 6.3, K2O: 0.7 },
    loi: 0,
  },
  {
    name: 'Ferro Frit 3110',
    aliases: ['frit 3110', '3110', 'ferro 3110'],
    analysis: { SiO2: 69.8, Al2O3: 3.7, B2O3: 2.6, CaO: 6.3, Na2O: 15.3, K2O: 2.3 },
    loi: 0,
  },
  {
    name: 'Ferro Frit 3195',
    aliases: ['frit 3195', '3195', 'ferro 3195'],
    analysis: { SiO2: 48.3, Al2O3: 12.0, B2O3: 22.6, CaO: 11.4, Na2O: 5.7 },
    loi: 0,
  },
  {
    name: 'Ferro Frit 3249',
    aliases: ['frit 3249', '3249', 'ferro 3249'],
    analysis: { SiO2: 42.1, Al2O3: 13.3, B2O3: 28.9, MgO: 12.2, CaO: 3.5 },
    loi: 0,
  },
  {
    name: 'Ferro Frit 3269',
    aliases: ['frit 3269', '3269', 'ferro 3269'],
    analysis: { SiO2: 51.23, Al2O3: 12.4, B2O3: 15.06, CaO: 0.41, K2O: 7.42, Na2O: 10.97, ZnO: 0.97 },
    loi: 0,
  },
  {
    name: 'Gerstley Borate',
    aliases: ['gerstley', 'gb', 'laguna borate'],
    analysis: { B2O3: 26.8, CaO: 20.5, SiO2: 14.4, Na2O: 4.5, MgO: 3.4, Al2O3: 1.5, K2O: 0.4, Fe2O3: 0.4 },
    loi: 27.5,
  },
  {
    name: 'Gillespie Borate',
    aliases: ['gillespie', 'gillespie borate substitute'],
    analysis: { B2O3: 24.4, CaO: 21.8, SiO2: 13.7, Na2O: 4.6, MgO: 3.4, Al2O3: 1.3, K2O: 0.4, Fe2O3: 0.3 },
    loi: 29.8,
  },
  {
    name: 'Colemanite',
    aliases: ['calcium borate'],
    analysis: { B2O3: 41.5, CaO: 27.0, SiO2: 5.0, Al2O3: 0.5, MgO: 1.5, Fe2O3: 0.2 },
    loi: 24.0,
  },
  {
    name: 'Ulexite',
    aliases: ['sodium calcium borate'],
    analysis: { B2O3: 38.0, CaO: 14.0, Na2O: 7.5, SiO2: 4.0, MgO: 1.0 },
    loi: 35.5,
  },

  // --- Clays ---
  {
    name: 'EPK',
    aliases: ['edgar plastic kaolin', 'epk kaolin', 'kaolin', 'china clay'],
    analysis: { SiO2: 45.7, Al2O3: 37.4, Fe2O3: 0.8, TiO2: 0.4, K2O: 0.3, Na2O: 0.1, CaO: 0.2, MgO: 0.1 },
    loi: 14.4,
  },
  {
    name: 'Grolleg Kaolin',
    aliases: ['grolleg', 'english china clay'],
    analysis: { SiO2: 47.7, Al2O3: 36.9, K2O: 1.85, Fe2O3: 0.72, MgO: 0.25, Na2O: 0.1, CaO: 0.08, TiO2: 0.04 },
    loi: 12.2,
  },
  {
    name: 'Tile 6 Kaolin',
    aliases: ['tile 6', 'tile-6', 'tile 6 clay'],
    analysis: { SiO2: 45.2, Al2O3: 38.5, TiO2: 1.4, Fe2O3: 0.5, K2O: 0.1, Na2O: 0.1, CaO: 0.1 },
    loi: 13.8,
  },
  {
    name: 'Calcined Kaolin',
    aliases: ['glomax', 'glomax ll', 'calcined epk', 'metakaolin', 'molochite'],
    analysis: { SiO2: 52.1, Al2O3: 44.4, TiO2: 1.7, Fe2O3: 0.5, K2O: 0.2, Na2O: 0.2, CaO: 0.1, MgO: 0.1 },
    loi: 0.5,
  },
  {
    name: 'OM4 Ball Clay',
    aliases: ['om4', 'om-4', 'om 4', 'ball clay', 'kentucky ball clay', 'old mine 4', 'old mine #4'],
    analysis: { SiO2: 53.6, Al2O3: 29.8, TiO2: 1.6, Fe2O3: 1.0, K2O: 1.0, MgO: 0.4, CaO: 0.3, Na2O: 0.2 },
    loi: 11.7,
  },
  {
    name: 'Goldart',
    aliases: ['goldart clay', 'goldart stoneware', 'cedar heights goldart'],
    analysis: { SiO2: 58.6, Al2O3: 25.0, K2O: 2.3, TiO2: 1.6, Fe2O3: 1.2, MgO: 0.6, CaO: 0.3, Na2O: 0.2 },
    loi: 9.8,
  },
  {
    name: 'Redart',
    aliases: ['redart clay', 'redart earthenware', 'cedar heights redart', 'red art'],
    analysis: { SiO2: 64.3, Al2O3: 16.4, Fe2O3: 7.0, K2O: 4.1, MgO: 1.6, TiO2: 1.1, Na2O: 0.4, CaO: 0.2 },
    loi: 4.9,
  },
  {
    name: 'Alberta Slip',
    aliases: ['albany slip', 'alberta slip clay'],
    analysis: { SiO2: 55.6, Al2O3: 13.0, CaO: 7.9, Fe2O3: 4.6, MgO: 2.9, K2O: 2.5, Na2O: 1.4, TiO2: 0.6 },
    loi: 11.3,
  },
  {
    name: 'Roasted Alberta Slip',
    aliases: ['calcined alberta slip'],
    analysis: { SiO2: 62.4, Al2O3: 14.6, CaO: 8.9, Fe2O3: 5.2, MgO: 3.3, K2O: 2.8, Na2O: 1.6, TiO2: 0.7 },
    loi: 0.5,
  },
  {
    name: 'Bentonite',
    aliases: ['wyoming bentonite', 'western bentonite', 'bentolite'],
    analysis: { SiO2: 63.0, Al2O3: 21.1, Fe2O3: 3.3, Na2O: 2.6, MgO: 2.5, CaO: 0.7, K2O: 0.4, TiO2: 0.2 },
    loi: 6.0,
  },
  {
    name: 'Veegum T',
    aliases: ['veegum', 'vee gum t', 'magnesium aluminum silicate'],
    analysis: { SiO2: 54.0, MgO: 23.0, CaO: 5.5, Al2O3: 2.0, Na2O: 1.5, Li2O: 1.0, K2O: 0.2, Fe2O3: 0.2 },
    loi: 12.6,
  },
  {
    name: 'Pyrophyllite',
    aliases: ['pyrotrol'],
    analysis: { SiO2: 75.5, Al2O3: 19.5, K2O: 0.6, Fe2O3: 0.5, TiO2: 0.2, Na2O: 0.1 },
    loi: 3.5,
  },

  // --- Fluxes, Carbonates & Ashes ---
  {
    name: 'Whiting',
    aliases: ['calcium carbonate', 'caco3', 'chalk', 'limestone', 'paris white'],
    analysis: { CaO: 56.0 },
    loi: 44.0,
  },
  {
    name: 'Wollastonite',
    aliases: ['vansil', 'vansil w-20', 'vansil w-30', 'calcium silicate'],
    analysis: { SiO2: 51.0, CaO: 47.0, Fe2O3: 0.4, MgO: 0.3, Al2O3: 0.3 },
    loi: 1.0,
  },
  {
    name: 'Dolomite',
    aliases: ['dolomite 200', 'calcium magnesium carbonate'],
    analysis: { CaO: 30.4, MgO: 21.9 },
    loi: 47.7,
  },
  {
    name: 'Talc',
    aliases: ['pioneer talc', 'nytal', 'magnesium silicate', 'steatite'],
    analysis: { SiO2: 62.0, MgO: 31.0, CaO: 0.8, Al2O3: 0.6, Fe2O3: 0.5 },
    loi: 5.1,
  },
  {
    name: 'Lithium Carbonate',
    aliases: ['li2co3', 'lithium carb'],
    analysis: { Li2O: 40.4 },
    loi: 59.6,
  },
  {
    name: 'Strontium Carbonate',
    aliases: ['srco3', 'strontium carb'],
    analysis: { SrO: 70.2 },
    loi: 29.8,
  },
  {
    name: 'Barium Carbonate',
    aliases: ['baco3', 'barium carb'],
    analysis: { BaO: 77.7 },
    loi: 22.3,
  },
  {
    name: 'Magnesium Carbonate',
    aliases: ['mgco3', 'mag carb', 'magnesium carb'],
    analysis: { MgO: 47.8 },
    loi: 52.2,
  },
  {
    name: 'Zinc Oxide',
    aliases: ['zno', 'zinc', 'calcined zinc oxide'],
    analysis: { ZnO: 100.0 },
    loi: 0,
  },
  {
    name: 'Bone Ash',
    aliases: ['tricalcium phosphate', 'calcium phosphate', 'synthetic bone ash'],
    analysis: { CaO: 54.2, P2O5: 45.8 },
    loi: 0,
  },
  {
    name: 'Wood Ash',
    aliases: ['washed wood ash', 'unwashed wood ash', 'hardwood ash', 'pine ash'],
    analysis: { CaO: 35.0, SiO2: 20.0, K2O: 5.5, MgO: 4.5, Al2O3: 3.5, P2O5: 3.5, Fe2O3: 1.5, Na2O: 1.0, MnO: 0.8 },
    loi: 24.7,
  },
  {
    name: 'Soda Ash',
    aliases: ['sodium carbonate', 'na2co3'],
    analysis: { Na2O: 58.5 },
    loi: 41.5,
  },
  {
    name: 'Pearl Ash',
    aliases: ['potassium carbonate', 'k2co3'],
    analysis: { K2O: 68.2 },
    loi: 31.8,
  },
  {
    name: 'Epsom Salts',
    aliases: ['epsom salt', 'magnesium sulfate', 'mgso4'],
    analysis: { MgO: 16.4 },
    loi: 83.6,
  },

  // --- Silica & Alumina ---
  {
    name: 'Silica',
    aliases: ['flint', 'quartz', 'silica 325', 'silica 200', '325 mesh silica', 'sio2', 'silicon dioxide'],
    analysis: { SiO2: 99.6, Al2O3: 0.2, Fe2O3: 0.03 },
    loi: 0.1,
  },
  {
    name: 'Alumina Hydrate',
    aliases: ['al(oh)3', 'aluminum hydroxide', 'hydrated alumina'],
    analysis: { Al2O3: 65.4 },
    loi: 34.6,
  },
  {
    name: 'Calcined Alumina',
    aliases: ['alumina', 'al2o3', 'aluminum oxide'],
    analysis: { Al2O3: 99.5, Na2O: 0.3 },
    loi: 0.2,
  },

  // --- Opacifiers & Colorants ---
  {
    name: 'Zircopax',
    aliases: ['zirconium silicate', 'superpax', 'ultrox', 'opax', 'zircopax plus'],
    analysis: { ZrO2: 65.0, SiO2: 34.0, TiO2: 0.3, Al2O3: 0.2, Fe2O3: 0.1 },
    loi: 0.4,
  },
  {
    name: 'Tin Oxide',
    aliases: ['sno2', 'stannic oxide', 'tin'],
    analysis: { SnO2: 100.0 },
    loi: 0,
  },
  {
    name: 'Titanium Dioxide',
    aliases: ['tio2', 'titanium', 'anatase', 'titanium white'],
    analysis: { TiO2: 99.0 },
    loi: 1.0,
  },
  {
    name: 'Rutile',
    aliases: ['dark rutile', 'light rutile', 'granular rutile', 'milled rutile'],
    analysis: { TiO2: 92.0, Fe2O3: 5.5, SiO2: 1.0, ZrO2: 0.5 },
    loi: 1.0,
  },
  {
    name: 'Red Iron Oxide',
    aliases: ['rio', 'red iron', 'fe2o3', 'ferric oxide', 'spanish red iron oxide', 'crocus martis'],
    analysis: { Fe2O3: 96.0, SiO2: 2.0 },
    loi: 2.0,
  },
  {
    name: 'Yellow Iron Oxide',
    aliases: ['yellow iron', 'yellow ochre', 'ochre'],
    analysis: { Fe2O3: 85.0, SiO2: 3.0 },
    loi: 12.0,
  },
  {
    name: 'Black Iron Oxide',
    aliases: ['black iron', 'fe3o4', 'magnetite'],
    analysis: { Fe2O3: 97.0 },
    loi: 3.0,
  },
  {
    name: 'Ilmenite',
    aliases: ['granular ilmenite', 'powdered ilmenite'],
    analysis: { TiO2: 53.0, Fe2O3: 45.0 },
    loi: 2.0,
  },
  {
    name: 'Cobalt Carbonate',
    aliases: ['cobalt carb', 'coco3'],
    analysis: { CoO: 63.0 },
    loi: 37.0,
  },
  {
    name: 'Cobalt Oxide',
    aliases: ['black cobalt oxide', 'coo', 'co3o4'],
    analysis: { CoO: 93.4 },
    loi: 6.6,
  },
  {
    name: 'Copper Carbonate',
    aliases: ['copper carb', 'cuco3', 'green copper carbonate'],
    analysis: { CuO: 71.9 },
    loi: 28.1,
  },
  {
    name: 'Copper Oxide',
    aliases: ['black copper oxide', 'cuo'],
    analysis: { CuO: 99.0 },
    loi: 1.0,
  },
  {
    name: 'Red Copper Oxide',
    aliases: ['cu2o', 'cuprous oxide'],
    analysis: { CuO: 111.2 },
    loi: 0,
  },
  {
    name: 'Chrome Oxide',
    aliases: ['chromium oxide', 'green chrome oxide', 'cr2o3'],
    analysis: { Cr2O3: 99.0 },
    loi: 1.0,
  },
  {
    name: 'Manganese Dioxide',
    aliases: ['mno2', 'manganese', 'black manganese'],
    analysis: { MnO: 81.6 },
    loi: 18.4,
  },
  {
    name: 'Manganese Carbonate',
    aliases: ['manganese carb', 'mnco3'],
    analysis: { MnO: 61.7 },
    loi: 38.3,
  },
  {
    name: 'Nickel Oxide',
    aliases: ['green nickel oxide', 'black nickel oxide', 'nio'],
    analysis: { NiO: 99.0 },
    loi: 1.0,
  },
];

function normalizeMaterialKey(name: string): string {
  return name
    .toLowerCase()
    .replaceAll(/[()[\],.;:/_-]+/g, ' ')
    .replaceAll(/\s+/g, ' ')
    .trim();
}

const MATERIAL_INDEX = new Map<string, MaterialDefinition>();

for (const material of MATERIALS) {
  MATERIAL_INDEX.set(normalizeMaterialKey(material.name), material);
  for (const alias of material.aliases) {
    MATERIAL_INDEX.set(normalizeMaterialKey(alias), material);
  }
}

/**
 * Looks up a studio ceramic material by canonical name or common alias (case-insensitive,
 * forgiving punctuation and mesh suffixes like "(325 mesh)").
 */
export function findMaterial(name: string): MaterialDefinition | undefined {
  const key = normalizeMaterialKey(name);
  if (!key) {
    return undefined;
  }
  const direct = MATERIAL_INDEX.get(key);
  if (direct) {
    return direct;
  }
  // Retry stripping common mesh annotations such as "325 mesh" or "200 mesh"
  const withoutMesh = key.replace(/\b\d+\s*mesh\b/g, '').replaceAll(/\s+/g, ' ').trim();
  if (withoutMesh && withoutMesh !== key) {
    return MATERIAL_INDEX.get(withoutMesh);
  }
  return undefined;
}
