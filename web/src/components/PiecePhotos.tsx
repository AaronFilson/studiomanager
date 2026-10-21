import {
  deletePhotoMutation,
  getPieceQueryKey,
  listPiecesQueryKey,
  type Piece,
  uploadPiecePhoto,
} from '@studiomanager/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { type ChangeEvent, useId } from 'react';

import { preparePhoto } from '../photos';
import { ErrorMessage } from './ErrorMessage';

const MAX_PHOTOS = 6;

/** A piece's photos, for its maker and staff to add to and remove from. */
export function PiecePhotos({ studioId, piece }: { studioId: string; piece: Piece }) {
  const id = useId();
  const queryClient = useQueryClient();
  const pieceKey = getPieceQueryKey({ path: { studioId, pieceId: piece.id } });
  const refreshList = () => queryClient.invalidateQueries({ queryKey: listPiecesQueryKey({ path: { studioId } }) });

  const add = useMutation({
    mutationFn: async (file: File) => uploadPiecePhoto(studioId, piece.id, await preparePhoto(file)),
    onSuccess: async (updated) => {
      queryClient.setQueryData(pieceKey, updated);
      await refreshList();
    },
  });
  const remove = useMutation({
    ...deletePhotoMutation(),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: pieceKey });
      await refreshList();
    },
  });

  function pick(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    // Cleared so picking the same file again after a failure still fires a change
    event.target.value = '';
    if (file) {
      add.mutate(file);
    }
  }

  return (
    <section className="stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Photos</h2>
      {piece.photos.length === 0 && <p>No photos yet. A photo of the piece and its mark helps find it on the shelf.</p>}
      {piece.photos.length > 0 && (
        <ul className="photos">
          {piece.photos.map((photo, index) => (
            <li key={photo.id}>
              <a href={photo.url} target="_blank" rel="noreferrer">
                <img
                  src={photo.thumbnailUrl}
                  alt={`Photo ${String(index + 1)} of piece ${String(piece.number)}`}
                  width={160}
                  height={160}
                />
              </a>
              <button
                type="button"
                disabled={remove.isPending}
                onClick={() => remove.mutate({ path: { studioId, pieceId: piece.id, photoId: photo.id } })}
              >
                Remove photo {index + 1}
              </button>
            </li>
          ))}
        </ul>
      )}
      {piece.photos.length < MAX_PHOTOS && (
        <div className="field">
          <label htmlFor={`${id}-file`}>Add a photo</label>
          <input id={`${id}-file`} type="file" accept="image/*" disabled={add.isPending} onChange={pick} />
        </div>
      )}
      {add.isPending && <p aria-live="polite">Uploading the photo…</p>}
      {add.isError && <ErrorMessage error={add.error} />}
      {remove.isError && <ErrorMessage error={remove.error} />}
    </section>
  );
}
