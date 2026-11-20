import {
  getGlazeBatchOptions,
  getGlazeOptions,
  type GlazeRecipe,
  type WeightUnit,
} from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';

import { atmosphereLabel, glazeUnitLabel, weightUnitLabel } from '../labels';
import { Badge, Body, Button, Card, ErrorText, Field, Loading, SectionTitle, Title } from '../ui';

const weightUnits: WeightUnit[] = ['GRAM', 'KILOGRAM', 'POUND', 'OUNCE'];
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

/** Shows a glaze recipe in full, with an interactive dry-batch weight calculator. */
export function GlazeDetail({ studioId, glazeId }: { studioId: string; glazeId: string }) {
  const glaze = useQuery(getGlazeOptions({ path: { studioId, glazeId } }));

  if (glaze.isPending) return <Loading />;
  if (glaze.isError) return <ErrorText error={glaze.error} />;

  const g = glaze.data;

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
          <Button key={u} compact primary={unit === u} label={weightUnitLabel(u)} onPress={() => setUnit(u)} />
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
  group: { gap: 6, marginTop: 4 },
});
