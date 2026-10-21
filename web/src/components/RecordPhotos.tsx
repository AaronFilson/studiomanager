import { type MaintenanceRecord, uploadRecordPhoto } from '@studiomanager/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { type ChangeEvent, useId } from 'react';

import { preparePhoto } from '../photos';
import { ErrorMessage } from './ErrorMessage';

const MAX_PHOTOS = 4;

/**
 * A maintenance record's photos: the worn element, the cleaned trap. They're evidence, so they
 * can be added but not removed.
 */
export function RecordPhotos({ studioId, record }: { studioId: string; record: MaintenanceRecord }) {
  const id = useId();
  const queryClient = useQueryClient();
  const add = useMutation({
    mutationFn: async (file: File) => uploadRecordPhoto(studioId, record.id, await preparePhoto(file)),
    onSuccess: () => queryClient.invalidateQueries(),
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
    <div className="stack">
      {record.photos.length > 0 && (
        <ul className="photos">
          {record.photos.map((photo, index) => (
            <li key={photo.id}>
              <a href={photo.url} target="_blank" rel="noreferrer">
                <img src={photo.thumbnailUrl} alt={`Photo ${String(index + 1)} of this work`} width={96} height={96} />
              </a>
            </li>
          ))}
        </ul>
      )}
      {record.photos.length < MAX_PHOTOS && (
        <div className="field">
          <label htmlFor={`${id}-file`}>Add a photo of this work</label>
          <input id={`${id}-file`} type="file" accept="image/*" disabled={add.isPending} onChange={pick} />
        </div>
      )}
      {add.isPending && <p aria-live="polite">Uploading the photo…</p>}
      {add.isError && <ErrorMessage error={add.error} />}
    </div>
  );
}
