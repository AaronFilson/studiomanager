import {
  addGlazeMixReadingMutation,
  getGlazeBatchOptions,
  getGlazeOptions,
  type GlazeMix,
  type GlazeRecipe,
  listGlazeMixesOptions,
  listGlazeMixesQueryKey,
  logGlazeMixMutation,
  useCan,
  type WeightUnit,
} from '@studiomanager/api-client';
import {
  analyzeRecipe,
  calculateSlurryAdjustment,
  checkLimits,
  CONE_BAND_LABELS,
  parseConeBand,
  type ConeBand,
} from '@studiomanager/glaze-chemistry';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';

import { atmosphereLabel, formatDateTime, glazeUnitLabel, weightUnitLabel } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, SectionTitle, Title } from '../ui';

const weightUnits: WeightUnit[] = ['GRAM', 'KILOGRAM', 'POUND', 'OUNCE'];
const coneBands: ConeBand[] = ['CONE_04', 'CONE_6', 'CONE_10'];
const decimal = /^\d{1,9}(\.\d{1,4})?$/;

function describe(g: GlazeRecipe): string {
  const parts = [
    g.ownerKind === 'STUDIO'
      ? 'Studio glaze'
      : `${g.ownerName ?? 'A member'}'s recipe${g.sharedWithStudio ? ', shared' : ''}`,
    g.cone ? `cone ${g.cone}` : undefined,
    g.atmosphere ? atmosphereLabel(g.atmosphere).toLowerCase() : undefined,
    g.targetSpecificGravity ? `target SG ${g.targetSpecificGravity}` : undefined,
  ];
  return parts.filter(Boolean).join(' · ');
}

/** Shows a glaze recipe in full, with dry-batch scaling, UMF chemistry, slurry SG helper, and studio mix log. */
export function GlazeDetail({ studioId, glazeId }: { studioId: string; glazeId: string }) {
  const glaze = useQuery(getGlazeOptions({ path: { studioId, glazeId } }));
  const permissions = useCan(studioId);

  if (glaze.isPending) return <Loading />;
  if (glaze.isError) return <ErrorText error={glaze.error} />;

  const g = glaze.data;
  const canLogMix = permissions.can('glazes.studio.edit');

  return (
    <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      <Title>{g.name}</Title>
      <Body muted>{describe(g)}</Body>
      {g.foodSurface && (
        <Badge>Meant for food surfaces. Follow the studio&apos;s own food-safety rules.</Badge>
      )}

      <Card>
        <SectionTitle>Recipe</SectionTitle>
        {g.lines.map((line, index) => (
          <View key={`${line.kind}-${line.material}-${String(index)}`} style={styles.lineRow}>
            <Body>
              {line.kind === 'ADDITION' ? '+ ' : ''}
              {line.material}
            </Body>
            <Body muted>
              {line.amount} {glazeUnitLabel(line.unit)}
            </Body>
          </View>
        ))}
      </Card>

      <BatchCalculator studioId={studioId} glazeId={glazeId} />

      <SlurrySgCard targetSpecificGravity={g.targetSpecificGravity} />

      <ChemistrySummaryCard glaze={g} />

      {g.ownerKind === 'STUDIO' && <StudioMixLogCard studioId={studioId} glaze={g} canEdit={canLogMix} />}

      {g.notes && (
        <Card>
          <SectionTitle>Notes</SectionTitle>
          <Body>{g.notes}</Body>
        </Card>
      )}
    </ScrollView>
  );
}

function BatchCalculator({ studioId, glazeId }: { studioId: string; glazeId: string }) {
  const [size, setSize] = useState('');
  const [unit, setUnit] = useState<WeightUnit>('KILOGRAM');
  const trimmed = size.trim();
  const valid = decimal.test(trimmed) && Number(trimmed) > 0;
  const batch = useQuery({
    ...getGlazeBatchOptions({ path: { studioId, glazeId }, query: { size: trimmed, unit } }),
    enabled: valid,
  });
  const unitText = weightUnitLabel(unit);

  return (
    <Card>
      <SectionTitle>Mix a batch</SectionTitle>
      <Field
        label="Dry batch size"
        value={size}
        onChangeText={setSize}
        keyboardType="decimal-pad"
        placeholder="10"
      />
      <View style={styles.rowWrap} accessibilityRole="radiogroup" accessibilityLabel="Batch unit">
        {weightUnits.map((u) => (
          <Button
            key={u}
            compact
            primary={unit === u}
            selected={unit === u}
            label={weightUnitLabel(u)}
            onPress={() => setUnit(u)}
          />
        ))}
      </View>

      {batch.isError && <ErrorText error={batch.error} />}
      {valid && batch.isPending && <Loading />}
      {valid && batch.data && (
        <View style={styles.group}>
          <Body muted>Weigh out</Body>
          {batch.data.lines.map((line, index) => (
            <View key={`${line.kind}-${line.material}-${String(index)}`} style={styles.lineRow}>
              <Body>
                {line.kind === 'ADDITION' ? '+ ' : ''}
                {line.material}
              </Body>
              <Body>
                {line.weight} {unitText}
              </Body>
            </View>
          ))}
          <View style={styles.totalRow}>
            <Body>Total</Body>
            <Body>
              {batch.data.total} {unitText}
            </Body>
          </View>
        </View>
      )}
    </Card>
  );
}

function SlurrySgCard({ targetSpecificGravity }: { targetSpecificGravity?: string }) {
  const [measured, setMeasured] = useState('');
  const [target, setTarget] = useState(targetSpecificGravity ?? '1.45');
  const [volumeMl, setVolumeMl] = useState('1000');

  const computed = useMemo(() => {
    const measuredSg = Number(measured.trim());
    const targetSg = Number(target.trim());
    const slurryVolumeMl = Number(volumeMl.trim());
    if (!Number.isFinite(measuredSg) || !Number.isFinite(targetSg) || !Number.isFinite(slurryVolumeMl)) {
      return null;
    }
    if (measuredSg <= 1.0 || measuredSg >= 2.6 || targetSg <= 1.0 || targetSg >= 2.6 || slurryVolumeMl <= 0) {
      return null;
    }
    try {
      return calculateSlurryAdjustment({ measuredSg, targetSg, slurryVolumeMl });
    } catch {
      return null;
    }
  }, [measured, target, volumeMl]);

  return (
    <Card>
      <SectionTitle>Slurry SG helper</SectionTitle>
      <Body muted>Weigh 100 mL of stirred glaze on a gram scale (e.g. 148 g = SG 1.48).</Body>
      <Field
        label="Measured SG"
        value={measured}
        onChangeText={setMeasured}
        keyboardType="decimal-pad"
        placeholder="1.50"
      />
      <Field
        label="Target SG"
        value={target}
        onChangeText={setTarget}
        keyboardType="decimal-pad"
        placeholder="1.45"
      />
      <Field
        label="Slurry volume (mL)"
        value={volumeMl}
        onChangeText={setVolumeMl}
        keyboardType="decimal-pad"
        placeholder="1000"
      />
      {computed && (
        <View style={styles.group}>
          {computed.action === 'on_target' && <Badge>Right on target specific gravity.</Badge>}
          {computed.action === 'add_water' && (
            <Body>
              Too thick: add {computed.waterToAddG.toFixed(1)} g (mL) of water (final ~
              {computed.resultingVolumeMl.toFixed(0)} mL).
            </Body>
          )}
          {computed.action === 'thicken' && (
            <Body>
              Too thin: decant {computed.waterToDecantG.toFixed(1)} g (mL) of water or stir in{' '}
              {computed.dryGlazeToAddG.toFixed(1)} g of dry glaze.
            </Body>
          )}
        </View>
      )}
    </Card>
  );
}

function ChemistrySummaryCard({ glaze }: { glaze: GlazeRecipe }) {
  const [coneBand, setConeBand] = useState<ConeBand>(() => parseConeBand(glaze.cone));

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

  return (
    <Card>
      <SectionTitle>Chemistry &amp; UMF</SectionTitle>
      {!analysis.valid ? (
        <Body muted>Not enough matched flux oxides to calculate a Unity Molecular Formula yet.</Body>
      ) : (
        <View style={styles.group}>
          <View style={styles.lineRow}>
            <Body muted>R2O : RO fluxes</Body>
            <Body>
              {analysis.r2oTotal.toFixed(2)} : {analysis.roTotal.toFixed(2)}
            </Body>
          </View>
          <View style={styles.lineRow}>
            <Body muted>SiO2 : Al2O3</Body>
            <Body>
              {analysis.siAlRatio !== null ? `${analysis.siAlRatio.toFixed(2)} · ` : ''}
              {analysis.stull.label}
            </Body>
          </View>
          <View style={styles.lineRow}>
            <Body muted>Thermal expansion</Body>
            <Body>
              {analysis.ctePerC !== null
                ? `${analysis.ctePerC.toFixed(2)} × 10⁻⁶ /°C (${analysis.expansionRating})`
                : 'Unknown'}
            </Body>
          </View>
          <View style={styles.lineRow}>
            <Body muted>LOI</Body>
            <Body>{analysis.loiPercent.toFixed(1)}%</Body>
          </View>
          <View style={styles.rowWrap} accessibilityRole="radiogroup" accessibilityLabel="Cone limit band">
            {coneBands.map((band) => (
              <Button
                key={band}
                compact
                primary={coneBand === band}
                selected={coneBand === band}
                label={CONE_BAND_LABELS[band]}
                onPress={() => setConeBand(band)}
              />
            ))}
          </View>
          {limits.outOfRange.length === 0 ? (
            <Badge>
              All {String(limits.checks.length)} oxides within {limits.bandLabel} limits.
            </Badge>
          ) : (
            limits.outOfRange.map((check) => (
              <Badge key={check.oxide} warning>
                {check.message}
              </Badge>
            ))
          )}
        </View>
      )}
      {analysis.unmatchedMaterials.length > 0 && (
        <Badge warning>Unmatched: {analysis.unmatchedMaterials.join(', ')}</Badge>
      )}
    </Card>
  );
}

function StudioMixLogCard({
  studioId,
  glaze,
  canEdit,
}: {
  studioId: string;
  glaze: GlazeRecipe;
  canEdit: boolean;
}) {
  const queryClient = useQueryClient();
  const mixesKey = listGlazeMixesQueryKey({ path: { studioId, glazeId: glaze.id } });
  const mixes = useQuery(listGlazeMixesOptions({ path: { studioId, glazeId: glaze.id } }));

  const [dryWeightG, setDryWeightG] = useState('');
  const [waterG, setWaterG] = useState('');
  const [initialSg, setInitialSg] = useState('');
  const [notes, setNotes] = useState('');

  const logMix = useMutation({
    ...logGlazeMixMutation(),
    onSuccess: () => {
      setDryWeightG('');
      setWaterG('');
      setInitialSg('');
      setNotes('');
      void queryClient.invalidateQueries({ queryKey: mixesKey });
    },
  });

  const dryTrimmed = dryWeightG.trim();
  const waterTrimmed = waterG.trim();
  const sgTrimmed = initialSg.trim();
  const dryValid = decimal.test(dryTrimmed) && Number(dryTrimmed) > 0;
  const waterValid = !waterTrimmed || (decimal.test(waterTrimmed) && Number(waterTrimmed) > 0);
  const sgValid = !sgTrimmed || (decimal.test(sgTrimmed) && Number(sgTrimmed) > 0);

  return (
    <Card>
      <SectionTitle>Mix log &amp; specific gravity</SectionTitle>
      {canEdit && (
        <View style={styles.group}>
          <Field
            label="Dry weight (g)"
            value={dryWeightG}
            onChangeText={setDryWeightG}
            keyboardType="decimal-pad"
            placeholder="10000"
          />
          <Field
            label="Water (g, optional)"
            value={waterG}
            onChangeText={setWaterG}
            keyboardType="decimal-pad"
            placeholder="7500"
          />
          <Field
            label="Initial SG (optional)"
            value={initialSg}
            onChangeText={setInitialSg}
            keyboardType="decimal-pad"
            placeholder={glaze.targetSpecificGravity ?? '1.45'}
          />
          <Field
            label="Mix notes (optional)"
            value={notes}
            onChangeText={setNotes}
            placeholder="Sieved 80 mesh, bucket #2"
          />
          {!waterValid && <Body>Enter water weight as a positive number, or leave blank.</Body>}
          {!sgValid && <Body>Enter specific gravity as a positive number, or leave blank.</Body>}
          {logMix.isError && <ErrorText error={logMix.error} />}
          <Button
            primary
            label={logMix.isPending ? 'Logging…' : 'Log mix'}
            disabled={logMix.isPending || !dryValid || !waterValid || !sgValid}
            onPress={() =>
              logMix.mutate({
                path: { studioId, glazeId: glaze.id },
                body: {
                  dryWeightG: dryTrimmed,
                  ...(waterTrimmed ? { waterG: waterTrimmed } : {}),
                  ...(sgTrimmed ? { specificGravity: sgTrimmed } : {}),
                  ...(notes.trim() ? { notes: notes.trim() } : {}),
                },
              })
            }
          />
        </View>
      )}

      {mixes.isPending && <Loading />}
      {mixes.isError && <ErrorText error={mixes.error} />}
      {mixes.isSuccess && mixes.data.length === 0 && <Body muted>No mixes logged for this bucket yet.</Body>}
      {mixes.data?.map((mix) => (
        <MixEntry
          key={mix.id}
          studioId={studioId}
          glazeId={glaze.id}
          mix={mix}
          canEdit={canEdit}
          onReadingAdded={() => {
            void queryClient.invalidateQueries({ queryKey: mixesKey });
          }}
        />
      ))}
    </Card>
  );
}

function MixEntry({
  studioId,
  glazeId,
  mix,
  canEdit,
  onReadingAdded,
}: {
  studioId: string;
  glazeId: string;
  mix: GlazeMix;
  canEdit: boolean;
  onReadingAdded: () => void;
}) {
  const [sg, setSg] = useState('');
  const [note, setNote] = useState('');
  const addReading = useMutation({
    ...addGlazeMixReadingMutation(),
    onSuccess: () => {
      setSg('');
      setNote('');
      onReadingAdded();
    },
  });

  const sgTrimmed = sg.trim();
  const sgValid = decimal.test(sgTrimmed) && Number(sgTrimmed) > 0;

  return (
    <View style={styles.mixEntry}>
      <Body>
        {mix.dryWeightG} g dry{mix.waterG ? ` · ${mix.waterG} g water` : ''} · {mix.mixedByName ?? 'Staff'} ·{' '}
        {formatDateTime(mix.mixedAt)}
      </Body>
      {mix.notes && <Body muted>{mix.notes}</Body>}
      {mix.readings.map((r) => (
        <View key={r.id} style={styles.readingRow}>
          <Body>
            SG {r.specificGravity} · {formatDateTime(r.readAt)}
            {r.note ? ` — ${r.note}` : ''}
          </Body>
          {r.offTarget && <Badge warning>{r.warning ?? 'Off target specific gravity.'}</Badge>}
        </View>
      ))}
      {canEdit && (
        <View style={styles.group}>
          <Field
            label="New SG reading"
            accessibilityLabel={`New SG reading for mix ${mix.id}`}
            value={sg}
            onChangeText={setSg}
            keyboardType="decimal-pad"
            placeholder="1.45"
          />
          <Field
            label="Reading note"
            accessibilityLabel={`Reading note for mix ${mix.id}`}
            value={note}
            onChangeText={setNote}
            placeholder="Added 200 mL water"
          />
          {addReading.isError && <ErrorText error={addReading.error} />}
          <Button
            compact
            label={addReading.isPending ? 'Saving…' : 'Add reading'}
            disabled={addReading.isPending || !sgValid}
            onPress={() =>
              addReading.mutate({
                path: { studioId, glazeId, mixId: mix.id },
                body: {
                  specificGravity: sgTrimmed,
                  ...(note.trim() ? { note: note.trim() } : {}),
                },
              })
            }
          />
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  lineRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 8, paddingVertical: 2 },
  totalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 8,
    paddingTop: 8,
    marginTop: 4,
  },
  rowWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  group: { gap: 8, marginTop: 4 },
  mixEntry: { gap: 6, paddingTop: 10 },
  readingRow: { gap: 4 },
});
