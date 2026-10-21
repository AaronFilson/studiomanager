import { deletePhotoMutation, type Piece, uploadPiecePhoto } from '@studiomanager/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Image, StyleSheet, View } from 'react-native';

import { pickPhoto } from '../photos';
import { Body, Button, ErrorText, Title } from '../ui';

const MAX_PHOTOS = 6;

/** A piece's photos, with the camera and library to add more, for its maker and staff. */
export function PiecePhotos({ studioId, piece }: { studioId: string; piece: Piece }) {
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: async (source: 'camera' | 'library') => {
      const photo = await pickPhoto(source);
      return photo ? uploadPiecePhoto(studioId, piece.id, photo) : null;
    },
    onSuccess: (updated) => (updated ? queryClient.invalidateQueries() : undefined),
  });
  const remove = useMutation({
    ...deletePhotoMutation(),
    onSuccess: () => queryClient.invalidateQueries(),
  });

  return (
    <View style={styles.section}>
      <Title>Photos</Title>
      {piece.photos.length === 0 && <Body muted>No photos yet. A photo of the piece and its mark helps find it.</Body>}
      <View style={styles.grid}>
        {piece.photos.map((photo, index) => (
          <View key={photo.id} style={styles.photo}>
            <Image
              source={{ uri: photo.thumbnailUrl }}
              style={styles.image}
              accessibilityLabel={`Photo ${String(index + 1)} of piece ${String(piece.number)}`}
            />
            <Button
              label={`Remove photo ${String(index + 1)}`}
              disabled={remove.isPending}
              onPress={() => remove.mutate({ path: { studioId, pieceId: piece.id, photoId: photo.id } })}
            />
          </View>
        ))}
      </View>
      {piece.photos.length < MAX_PHOTOS && (
        <View style={styles.actions}>
          <Button label="Take a photo" disabled={add.isPending} onPress={() => add.mutate('camera')} />
          <Button label="Choose a photo" disabled={add.isPending} onPress={() => add.mutate('library')} />
        </View>
      )}
      {add.isPending && <Body muted>Uploading the photo…</Body>}
      {add.isError && <ErrorText error={add.error} />}
      {remove.isError && <ErrorText error={remove.error} />}
    </View>
  );
}

const styles = StyleSheet.create({
  section: { gap: 8 },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  photo: { width: 150, gap: 6 },
  image: { width: 150, height: 150, borderRadius: 10 },
  actions: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
});
