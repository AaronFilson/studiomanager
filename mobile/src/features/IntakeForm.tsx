import {
  intakePieceMutation,
  listMembersOptions,
  type Member,
  type Piece,
  type PieceSize,
  type UnitSystem,
  uploadPiecePhoto,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Image, ScrollView, StyleSheet, View } from 'react-native';

import { defaultUnits, formatMoney, lengthUnitLabel, weightUnitLabel } from '../labels';
import { pickPhoto, type PreparedPhoto } from '../photos';
import { Body, Button, Card, ErrorText, Field, Title } from '../ui';

const decimal = /^\d{1,9}(\.\d{1,4})?$/;

/**
 * Takes in a piece at the shelf. Members drop off their own; staff can name the maker. Fill in
 * the measurements the studio prices by: the bounding box, the weight, or neither for a flat fee.
 * A photo taken here is added once the piece is in.
 */
export function IntakeForm({
  studioId,
  unitSystem,
  staff,
  onDone,
}: {
  studioId: string;
  unitSystem: UnitSystem;
  staff: boolean;
  onDone?: (piece: Piece) => void;
}) {
  const queryClient = useQueryClient();
  const units = defaultUnits(unitSystem);
  const [maker, setMaker] = useState<Member | null>(null);
  const [description, setDescription] = useState('');
  const [clayBody, setClayBody] = useState('');
  const [makerMark, setMakerMark] = useState('');
  const [length, setLength] = useState('');
  const [width, setWidth] = useState('');
  const [height, setHeight] = useState('');
  const [weight, setWeight] = useState('');
  const [photo, setPhoto] = useState<PreparedPhoto | null>(null);
  const [photoError, setPhotoError] = useState<unknown>(null);
  const camera = useMutation({
    mutationFn: () => pickPhoto('camera'),
    onSuccess: (taken) => {
      if (taken) setPhoto(taken);
    },
  });
  const intake = useMutation({
    ...intakePieceMutation(),
    onSuccess: async (piece) => {
      setDescription('');
      setLength('');
      setWidth('');
      setHeight('');
      setWeight('');
      setPhotoError(null);
      // The piece is in either way; a photo that fails can be added again from the piece
      if (photo) {
        try {
          await uploadPiecePhoto(studioId, piece.id, photo);
          setPhoto(null);
        } catch (error) {
          setPhotoError(error);
        }
      }
      await queryClient.invalidateQueries();
      onDone?.(piece);
    },
  });

  const box = [length, width, height];
  const boxStarted = box.some(Boolean);
  const boxValid = !boxStarted || box.every((v) => decimal.test(v));
  const weightValid = !weight || decimal.test(weight);

  function submit() {
    const size: PieceSize = {};
    if (boxStarted) size.dimensions = { length, width, height, unit: units.length };
    if (weight) size.weight = { value: weight, unit: units.weight };
    intake.mutate({
      path: { studioId },
      body: {
        size,
        ...(maker ? { makerAccountId: maker.accountId } : {}),
        ...(description.trim() ? { description: description.trim() } : {}),
        ...(clayBody.trim() ? { clayBody: clayBody.trim() } : {}),
        ...(makerMark.trim() ? { makerMark: makerMark.trim() } : {}),
      },
    });
  }

  return (
    <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      {staff && <MakerPicker studioId={studioId} maker={maker} onPick={setMaker} />}
      <Field label="What is it?" value={description} onChangeText={setDescription} maxLength={200} />
      <View style={styles.row}>
        <Field label="Clay" value={clayBody} onChangeText={setClayBody} maxLength={80} />
        <Field label="Maker's mark" value={makerMark} onChangeText={setMakerMark} maxLength={80} />
      </View>
      <Body muted>Size, in {lengthUnitLabel(units.length)}</Body>
      <View style={styles.row}>
        <Field label="Length" value={length} onChangeText={setLength} keyboardType="decimal-pad" />
        <Field label="Width" value={width} onChangeText={setWidth} keyboardType="decimal-pad" />
        <Field label="Height" value={height} onChangeText={setHeight} keyboardType="decimal-pad" />
      </View>
      <Field
        label={`Weight, in ${weightUnitLabel(units.weight)}`}
        value={weight}
        onChangeText={setWeight}
        keyboardType="decimal-pad"
      />
      <View style={styles.row}>
        {photo && (
          <Image source={{ uri: photo.previewUri }} style={styles.preview} accessibilityLabel="The photo to add" />
        )}
        <Button
          label={photo ? 'Take it again' : 'Take a photo'}
          disabled={camera.isPending}
          onPress={() => camera.mutate()}
        />
      </View>
      {camera.isError && <ErrorText error={camera.error} />}
      {!boxValid && <Body>Enter all three measurements as numbers, or none.</Body>}
      {!weightValid && <Body>Enter the weight as a number.</Body>}
      {intake.isError && <ErrorText error={intake.error} />}
      <Button primary label="Take in piece" disabled={intake.isPending || !boxValid || !weightValid} onPress={submit} />
      {intake.data && (
        <Card>
          <Title>Piece {intake.data.number}</Title>
          <Body>Write {intake.data.number} on its label.</Body>
          <Body muted>
            Firing fee {formatMoney(intake.data.fee.amount.amount, intake.data.fee.amount.currency)}
          </Body>
          {photoError !== null && (
            <>
              <Body>The piece is in, but its photo didn&apos;t upload. Add it from the piece.</Body>
              <ErrorText error={photoError} />
            </>
          )}
        </Card>
      )}
    </ScrollView>
  );
}

/** Staff pick whose piece it is by typing part of a name. Empty means their own. */
function MakerPicker({
  studioId,
  maker,
  onPick,
}: {
  studioId: string;
  maker: Member | null;
  onPick: (member: Member | null) => void;
}) {
  const [search, setSearch] = useState('');
  const members = useQuery(listMembersOptions({ path: { studioId } }));
  const term = search.trim().toLowerCase();
  const matches = term
    ? (members.data ?? []).filter((m) => m.displayName?.toLowerCase().includes(term)).slice(0, 5)
    : [];

  if (maker) {
    return (
      <View style={styles.row}>
        <Body>Maker: {maker.displayName ?? 'Unnamed member'}</Body>
        <Button label="Change" onPress={() => onPick(null)} />
      </View>
    );
  }
  return (
    <View style={styles.picker}>
      <Field label="Whose piece? (blank for yours)" value={search} onChangeText={setSearch} autoCorrect={false} />
      {matches.map((m) => (
        <Button
          key={m.accountId}
          label={m.displayName ?? 'Unnamed member'}
          onPress={() => {
            onPick(m);
            setSearch('');
          }}
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  content: { gap: 12, paddingBottom: 32 },
  row: { flexDirection: 'row', gap: 8, alignItems: 'flex-end' },
  picker: { gap: 6 },
  preview: { width: 64, height: 64, borderRadius: 8 },
});
