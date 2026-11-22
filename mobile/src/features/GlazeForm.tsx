import type {
  Atmosphere,
  GlazeAmountUnit,
  GlazeLine,
  GlazeRecipe,
  SaveGlazeRequest,
} from '@studiomanager/api-client';
import { useRef, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';

import { atmosphereLabel, glazeUnitLabel } from '../labels';
import { Body, Button, Card, ErrorText, Field, SectionTitle } from '../ui';

const atmospheres: Atmosphere[] = ['OXIDATION', 'REDUCTION', 'NEUTRAL'];
const units: GlazeAmountUnit[] = ['PARTS', 'PERCENT', 'GRAMS'];
const decimal = /^\d{1,9}(\.\d{1,4})?$/;
const specificGravityPattern = /^[12](\.\d{1,3})?$/;
const MAX_LINES = 60;

interface DraftLine extends GlazeLine {
  rowId: number;
}

function makeLine(rowId: number, line?: GlazeLine): DraftLine {
  return line ? { ...line, rowId } : { rowId, kind: 'BASE', material: '', amount: '', unit: 'PARTS' };
}

function isValidSg(value: string): boolean {
  if (!value) return true;
  if (!specificGravityPattern.test(value)) return false;
  const parsed = Number(value);
  return parsed >= 1 && parsed <= 2.5;
}

/**
 * Shared create/edit form for studio and personal glaze recipes.
 * Base lines use parts; additions (colorants, opacifiers) use percent, grams, or parts.
 */
export function GlazeForm({
  initial,
  canShare,
  submitLabel,
  pending,
  error,
  onSubmit,
  onCancel,
}: {
  initial?: GlazeRecipe;
  canShare: boolean;
  submitLabel: string;
  pending: boolean;
  error?: unknown;
  onSubmit: (body: SaveGlazeRequest) => void;
  onCancel: () => void;
}) {
  const nextRowId = useRef(initial?.lines.length ?? 1);
  const [name, setName] = useState(initial?.name ?? '');
  const [cone, setCone] = useState(initial?.cone ?? '');
  const [atmosphere, setAtmosphere] = useState<Atmosphere | ''>(initial?.atmosphere ?? '');
  const [targetSg, setTargetSg] = useState(initial?.targetSpecificGravity ?? '');
  const [foodSurface, setFoodSurface] = useState(initial?.foodSurface ?? false);
  const [shared, setShared] = useState(initial?.sharedWithStudio ?? false);
  const [notes, setNotes] = useState(initial?.notes ?? '');
  const [lines, setLines] = useState<DraftLine[]>(() =>
    initial && initial.lines.length > 0
      ? initial.lines.map((line, index) => makeLine(index, line))
      : [makeLine(0)],
  );

  const setLine = (rowId: number, change: Partial<GlazeLine>) =>
    setLines((current) =>
      current.map((line) => {
        if (line.rowId !== rowId) return line;
        const next: DraftLine = { ...line, ...change };
        if (next.chemistry === undefined) {
          delete next.chemistry;
        }
        if (next.amountText === undefined) {
          delete next.amountText;
        }
        return next;
      }),
    );

  const addLine = () => {
    const id = nextRowId.current;
    nextRowId.current += 1;
    setLines((current) => [...current, makeLine(id)]);
  };

  const removeLine = (rowId: number) => {
    setLines((current) => (current.length > 1 ? current.filter((line) => line.rowId !== rowId) : current));
  };

  const sgTrimmed = targetSg.trim();
  const sgValid = isValidSg(sgTrimmed);
  const linesValid =
    lines.length > 0 &&
    lines.every((line) => {
      const amount = line.amount.trim();
      return line.material.trim().length > 0 && decimal.test(amount) && Number(amount) > 0;
    });
  const canSubmit = name.trim().length > 0 && sgValid && linesValid;

  const handleSubmit = () => {
    const cleanLines: GlazeLine[] = lines.map((line) => ({
      kind: line.kind,
      material: line.material.trim(),
      amount: line.amount.trim(),
      unit: line.unit,
      ...(line.amountText !== undefined ? { amountText: line.amountText } : {}),
      ...(line.chemistry !== undefined ? { chemistry: line.chemistry } : {}),
    }));
    onSubmit({
      name: name.trim(),
      lines: cleanLines,
      foodSurface,
      ...(cone.trim() ? { cone: cone.trim() } : {}),
      ...(atmosphere ? { atmosphere } : {}),
      ...(sgTrimmed ? { targetSpecificGravity: sgTrimmed } : {}),
      ...(notes.trim() ? { notes: notes.trim() } : {}),
      ...(canShare ? { sharedWithStudio: shared } : {}),
    });
  };

  return (
    <Card>
      <Field label="Name" value={name} onChangeText={setName} maxLength={120} placeholder="Floating Blue" />

      <Field label="Cone" value={cone} onChangeText={setCone} maxLength={10} placeholder="6" />

      <View style={styles.group}>
        <Body muted>Atmosphere</Body>
        <View style={styles.rowWrap} accessibilityRole="radiogroup" accessibilityLabel="Atmosphere">
          {atmospheres.map((a) => (
            <Button
              key={a}
              compact
              primary={atmosphere === a}
              selected={atmosphere === a}
              label={atmosphereLabel(a)}
              onPress={() => setAtmosphere((current) => (current === a ? '' : a))}
            />
          ))}
        </View>
      </View>

      <Field
        label="Target specific gravity"
        value={targetSg}
        onChangeText={setTargetSg}
        keyboardType="decimal-pad"
        placeholder="1.45"
      />
      {!sgValid && <Body>Enter a specific gravity between 1 and 2.5 (for example 1.45), or leave blank.</Body>}

      <Pressable
        accessibilityRole="checkbox"
        accessibilityState={{ checked: foodSurface }}
        accessibilityLabel="Meant for food surfaces"
        onPress={() => setFoodSurface((v) => !v)}
        style={styles.checkbox}
      >
        <Body>{foodSurface ? '☑' : '☐'} Meant for food surfaces</Body>
      </Pressable>

      {canShare && (
        <Pressable
          accessibilityRole="checkbox"
          accessibilityState={{ checked: shared }}
          accessibilityLabel="Share with the studio"
          onPress={() => setShared((v) => !v)}
          style={styles.checkbox}
        >
          <Body>{shared ? '☑' : '☐'} Share with the studio</Body>
        </Pressable>
      )}

      <View style={styles.group}>
        <SectionTitle>Recipe lines</SectionTitle>
        {lines.map((line, index) => {
          const lineNumber = String(index + 1);
          return (
            <View key={line.rowId} style={styles.lineCard}>
              <Body muted>Line {lineNumber}</Body>
              <View style={styles.rowWrap} accessibilityRole="radiogroup" accessibilityLabel={`Line ${lineNumber} kind`}>
                <Button
                  compact
                  primary={line.kind === 'BASE'}
                  selected={line.kind === 'BASE'}
                  label="Base"
                  accessibilityLabel={`Base for line ${lineNumber}`}
                  onPress={() => setLine(line.rowId, { kind: 'BASE', unit: 'PARTS' })}
                />
                <Button
                  compact
                  primary={line.kind === 'ADDITION'}
                  selected={line.kind === 'ADDITION'}
                  label="Addition"
                  accessibilityLabel={`Addition for line ${lineNumber}`}
                  onPress={() =>
                    setLine(line.rowId, {
                      kind: 'ADDITION',
                      unit: line.kind === 'BASE' ? 'PERCENT' : line.unit,
                    })
                  }
                />
              </View>
              <Field
                label="Material"
                accessibilityLabel={`Material ${lineNumber}`}
                value={line.material}
                onChangeText={(material) => setLine(line.rowId, { material, chemistry: undefined })}
                maxLength={80}
                placeholder="Nepheline Syenite"
              />
              <Field
                label="Amount"
                accessibilityLabel={`Amount ${lineNumber}`}
                value={line.amount}
                onChangeText={(amount) => setLine(line.rowId, { amount, amountText: undefined })}
                keyboardType="decimal-pad"
                placeholder="40"
              />
              <View style={styles.rowWrap} accessibilityRole="radiogroup" accessibilityLabel={`Line ${lineNumber} unit`}>
                {units.map((u) => (
                  <Button
                    key={u}
                    compact
                    primary={line.unit === u}
                    selected={line.unit === u}
                    label={glazeUnitLabel(u)}
                    accessibilityLabel={`${glazeUnitLabel(u)} for line ${lineNumber}`}
                    onPress={() =>
                      setLine(
                        line.rowId,
                        u === 'PARTS' ? { unit: 'PARTS' } : { kind: 'ADDITION', unit: u },
                      )
                    }
                  />
                ))}
                <Button
                  compact
                  label="Remove"
                  accessibilityLabel={`Remove line ${lineNumber}`}
                  disabled={lines.length === 1}
                  onPress={() => removeLine(line.rowId)}
                />
              </View>
            </View>
          );
        })}
        <Button compact label="Add a line" disabled={lines.length >= MAX_LINES} onPress={addLine} />
      </View>

      <Field
        label="Notes"
        value={notes}
        onChangeText={setNotes}
        maxLength={4000}
        multiline
        placeholder="Dip thickness, firing notes, clay body"
      />

      {error !== undefined && error !== null && <ErrorText error={error} />}

      <View style={styles.rowWrap}>
        <Button
          primary
          label={pending ? 'Saving…' : submitLabel}
          disabled={pending || !canSubmit}
          onPress={handleSubmit}
        />
        <Button label="Cancel" disabled={pending} onPress={onCancel} />
      </View>
    </Card>
  );
}

const styles = StyleSheet.create({
  group: { gap: 8, marginTop: 4 },
  rowWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  checkbox: { paddingVertical: 4 },
  lineCard: { gap: 6, paddingVertical: 8 },
});
