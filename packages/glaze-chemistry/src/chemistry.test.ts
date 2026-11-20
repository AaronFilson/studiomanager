import { describe, expect, it } from 'vitest';

import {
  analyzeRecipe,
  calculateSlurryAdjustment,
  checkLimits,
  findMaterial,
  MATERIALS,
  normalizeOxideKey,
  parseConeBand,
} from './index';

describe('materials library & oxide normalization', () => {
  it('ships at least 55 built-in studio ceramic materials and resolves common aliases', () => {
    expect(MATERIALS.length).toBeGreaterThanOrEqual(55);
    expect(findMaterial('Custer Feldspar')?.name).toBe('Custer Feldspar');
    expect(findMaterial('custer')?.name).toBe('Custer Feldspar');
    expect(findMaterial('Minspar 200')?.name).toBe('Minspar 200');
    expect(findMaterial('Frit 3134')?.name).toBe('Ferro Frit 3134');
    expect(findMaterial('3124')?.name).toBe('Ferro Frit 3124');
    expect(findMaterial('EPK')?.name).toBe('EPK');
    expect(findMaterial('OM-4')?.name).toBe('OM4 Ball Clay');
    expect(findMaterial('Flint (325 mesh)')?.name).toBe('Silica');
    expect(findMaterial('Calcium Carbonate')?.name).toBe('Whiting');
    expect(findMaterial('RIO')?.name).toBe('Red Iron Oxide');
    expect(findMaterial('Unobtainium 9000')).toBeUndefined();
  });

  it('normalizes oxide spellings case-insensitively', () => {
    expect(normalizeOxideKey('sio2')).toBe('SiO2');
    expect(normalizeOxideKey('Al2O3')).toBe('Al2O3');
    expect(normalizeOxideKey('fe2o3')).toBe('Fe2O3');
    expect(normalizeOxideKey('nope')).toBeUndefined();
  });
});

describe('UMF, Stull surface, CTE & cone limit checks', () => {
  it('computes Leach 4321 (Cone 10 stoneware clear) accurately', () => {
    const result = analyzeRecipe(
      [
        { material: 'Custer Feldspar', amount: 40, kind: 'BASE', unit: 'PARTS' },
        { material: 'Silica', amount: 30, kind: 'BASE', unit: 'PARTS' },
        { material: 'Whiting', amount: 20, kind: 'BASE', unit: 'PARTS' },
        { material: 'EPK', amount: 10, kind: 'BASE', unit: 'PARTS' },
      ],
      { cone: '10' },
    );

    expect(result.valid).toBe(true);
    expect(result.unmatchedMaterials).toEqual([]);
    // R2O + RO fluxes normalize to 1.0
    expect(result.r2oTotal + result.roTotal).toBeCloseTo(1.0, 2);
    expect(result.r2oTotal).toBeGreaterThan(0.18);
    expect(result.r2oTotal).toBeLessThan(0.35);
    expect(result.umf.CaO).toBeGreaterThan(0.65);
    expect(result.umf.Al2O3).toBeGreaterThan(0.32);
    expect(result.umf.Al2O3).toBeLessThan(0.45);
    expect(result.umf.SiO2).toBeGreaterThan(3.5);
    expect(result.umf.SiO2).toBeLessThan(4.5);
    expect(result.siAlRatio).not.toBeNull();
    expect(result.siAlRatio ?? 0).toBeGreaterThan(9.0);
    expect(result.stull.surface).toBe('glossy');
    expect(result.ctePerC).not.toBeNull();
    expect(result.ctePerC ?? 0).toBeGreaterThan(5.0);
    expect(result.ctePerC ?? 0).toBeLessThan(7.2);
    expect(result.limits.band).toBe('CONE_10');
    expect(result.limits.outOfRange).toEqual([]);
  });

  it('prefers imported chemistry snapshots over the built-in materials library', () => {
    const result = analyzeRecipe([
      {
        material: 'Mystery Local Feldspar',
        amount: 50,
        kind: 'BASE',
        unit: 'PARTS',
        chemistry: {
          materialId: 'custom-1',
          analysis: { SiO2: 65, Al2O3: 18, K2O: 12, Na2O: 4 },
          loi: 1,
        },
      },
      { material: 'Whiting', amount: 20, kind: 'BASE', unit: 'PARTS' },
      { material: 'Silica', amount: 30, kind: 'BASE', unit: 'PARTS' },
    ]);

    expect(result.valid).toBe(true);
    expect(result.unmatchedMaterials).toEqual([]);
    expect(result.lineResolutions[0]?.source).toBe('snapshot');
    expect(result.lineResolutions[1]?.source).toBe('library');
    expect(result.r2oTotal + result.roTotal).toBeCloseTo(1.0, 2);
  });

  it('reports unmatched materials cleanly without failing the rest of the recipe', () => {
    const result = analyzeRecipe([
      { material: 'Custer Feldspar', amount: 45, kind: 'BASE', unit: 'PARTS' },
      { material: 'Whiting', amount: 25, kind: 'BASE', unit: 'PARTS' },
      { material: 'Silica', amount: 30, kind: 'BASE', unit: 'PARTS' },
      { material: 'Mason Stain 6600', amount: 4, kind: 'ADDITION', unit: 'PERCENT' },
      { material: 'Epsom Salts', amount: 2, kind: 'ADDITION', unit: 'GRAMS' },
    ]);

    expect(result.valid).toBe(true);
    expect(result.unmatchedMaterials).toEqual(['Mason Stain 6600']);
    expect(result.lineResolutions.find((r) => r.material === 'Epsom Salts')?.source).toBe('skipped_grams');
  });

  it('predicts matte surface for high-alumina, low Si:Al recipes and flags cone limit violations', () => {
    const result = analyzeRecipe(
      [
        { material: 'Nepheline Syenite', amount: 45, kind: 'BASE', unit: 'PARTS' },
        { material: 'Whiting', amount: 25, kind: 'BASE', unit: 'PARTS' },
        { material: 'EPK', amount: 30, kind: 'BASE', unit: 'PARTS' },
      ],
      { cone: '6' },
    );

    expect(result.valid).toBe(true);
    expect(result.stull.surface).toBe('matte');
    expect(result.limits.band).toBe('CONE_6');
    // Missing B2O3 and low SiO2 for Cone 6
    expect(result.limits.outOfRange.some((c) => c.oxide === 'SiO2' && c.status === 'low')).toBe(true);
    expect(result.limits.outOfRange.some((c) => c.oxide === 'B2O3' && c.status === 'low')).toBe(true);
  });

  it('parses cone strings into Cone 04, Cone 6, and Cone 10 bands', () => {
    expect(parseConeBand('04')).toBe('CONE_04');
    expect(parseConeBand('Cone 06')).toBe('CONE_04');
    expect(parseConeBand('5-6')).toBe('CONE_6');
    expect(parseConeBand('6')).toBe('CONE_6');
    expect(parseConeBand('10')).toBe('CONE_10');
    expect(parseConeBand('9-10')).toBe('CONE_10');
    expect(parseConeBand(null)).toBe('CONE_6');

    const report = checkLimits({ SiO2: 2.2, Al2O3: 0.18, B2O3: 0.3, Na2O: 0.2, CaO: 0.3 }, 'CONE_04');
    expect(report.outOfRange).toEqual([]);
  });
});

describe('slurry specific gravity adjustment calculator', () => {
  it('computes exact water to add when slurry is too thick', () => {
    const adj = calculateSlurryAdjustment({
      measuredSg: 1.5,
      targetSg: 1.45,
      slurryVolumeMl: 1000,
    });
    expect(adj.action).toBe('add_water');
    // (1000 * 1.50 + 111.1) / (1000 + 111.1) = 1.450
    expect(adj.waterToAddG).toBeCloseTo(111.1, 1);
    expect(adj.waterToDecantG).toBe(0);
    expect(adj.dryGlazeToAddG).toBe(0);
    const resultingSg = (1000 * 1.5 + adj.waterToAddG) / (1000 + adj.waterToAddG);
    expect(resultingSg).toBeCloseTo(1.45, 3);
  });

  it('computes water to decant and dry glaze to add when slurry is too thin', () => {
    const adj = calculateSlurryAdjustment({
      measuredSg: 1.4,
      targetSg: 1.45,
      slurryMassG: 1400, // 1000 mL at SG 1.40
    });
    expect(adj.action).toBe('thicken');
    expect(adj.slurryVolumeMl).toBeCloseTo(1000, 1);
    expect(adj.waterToAddG).toBe(0);
    expect(adj.waterToDecantG).toBeCloseTo(111.1, 1);
    expect(adj.dryGlazeToAddG).toBeCloseTo(113.0, 0);
    const sgAfterDecant = (1400 - adj.waterToDecantG) / (1000 - adj.waterToDecantG);
    expect(sgAfterDecant).toBeCloseTo(1.45, 3);
    const sgAfterDry = (1400 + adj.dryGlazeToAddG) / (1000 + adj.dryGlazeToAddG / 2.6);
    expect(sgAfterDry).toBeCloseTo(1.45, 3);
  });

  it('reports on_target when measured SG matches target SG', () => {
    const adj = calculateSlurryAdjustment({
      measuredSg: 1.45,
      targetSg: 1.45,
      slurryVolumeMl: 5000,
    });
    expect(adj.action).toBe('on_target');
    expect(adj.waterToAddG).toBe(0);
    expect(adj.waterToDecantG).toBe(0);
    expect(adj.dryGlazeToAddG).toBe(0);
  });

  it('rejects impossible specific gravities or non-positive amounts', () => {
    expect(() =>
      calculateSlurryAdjustment({ measuredSg: 1.0, targetSg: 1.45, slurryVolumeMl: 1000 }),
    ).toThrow(RangeError);
    expect(() =>
      calculateSlurryAdjustment({ measuredSg: 1.5, targetSg: 1.45, slurryVolumeMl: 0 }),
    ).toThrow(RangeError);
  });
});
