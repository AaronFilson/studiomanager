import {
  deleteGlazePhotoMutation,
  getGlazeQueryKey,
  type GlazeRecipe,
  uploadGlazePhoto,
} from '@studiomanager/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { type ChangeEvent, useId } from 'react';

import { preparePhoto } from '../photos';
import { ErrorMessage } from './ErrorMessage';

const MAX_PHOTOS = 6;

/** Test-tile photos attached to a glaze recipe (direct-to-blob upload, ADR 12). */
export function GlazePhotos({ studioId, glaze }: { studioId: string; glaze: GlazeRecipe }) {
  const id = useId();
  const queryClient = useQueryClient();
  const photos = glaze.photos ?? [];

  const add = useMutation({
    mutationFn: async ({
      studioId: targetStudioId,
      glazeId: targetGlazeId,
      file,
    }: {
      studioId: string;
      glazeId: string;
      file: File;
    }) => uploadGlazePhoto(targetStudioId, targetGlazeId, await preparePhoto(file)),
    onSuccess: (updated, variables) => {
      queryClient.setQueryData(
        getGlazeQueryKey({ path: { studioId: variables.studioId, glazeId: updated.id } }),
        updated,
      );
    },
  });

  const remove = useMutation({
    ...deleteGlazePhotoMutation(),
    onSuccess: (_data, variables) => {
      void queryClient.invalidateQueries({
        queryKey: getGlazeQueryKey({
          path: { studioId: variables.path.studioId, glazeId: variables.path.glazeId },
        }),
      });
    },
  });

  function pick(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (file) {
      add.mutate({ studioId, glazeId: glaze.id, file });
    }
  }

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Test-tile photos</h2>
      {photos.length === 0 && (
        <p className="badge">No test-tile photos yet. Add photos on different clay bodies or thicknesses.</p>
      )}
      {photos.length > 0 && (
        <ul className="photos">
          {photos.map((photo, index) => (
            <li key={photo.id}>
              <a href={photo.url} target="_blank" rel="noreferrer">
                <img
                  src={photo.thumbnailUrl}
                  alt={`Test tile ${String(index + 1)} of ${glaze.name}`}
                  width={160}
                  height={160}
                />
              </a>
              {glaze.canEdit && (
                <button
                  type="button"
                  disabled={remove.isPending}
                  onClick={() => remove.mutate({ path: { studioId, glazeId: glaze.id, photoId: photo.id } })}
                >
                  Remove photo {String(index + 1)}
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
      {glaze.canEdit && photos.length < MAX_PHOTOS && (
        <div className="field">
          <label htmlFor={`${id}-file`}>Add a test-tile photo</label>
          <input id={`${id}-file`} type="file" accept="image/*" disabled={add.isPending} onChange={pick} />
        </div>
      )}
      {add.isPending && <p aria-live="polite">Uploading the photo…</p>}
      {add.isError && <ErrorMessage error={add.error} />}
      {remove.isError && <ErrorMessage error={remove.error} />}
    </section>
  );
}
