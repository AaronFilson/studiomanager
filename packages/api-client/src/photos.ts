import {
  completePhotoUpload,
  completeRecordPhotoUpload,
  type Piece,
  type PhotoUpload,
  startPhotoUpload,
  startRecordPhotoUpload,
} from './generated';

/** A photo ready to upload: both images already shrunk and encoded as JPEG. */
export interface PhotoFiles {
  photo: Blob;
  thumbnail: Blob;
}

/**
 * Adds a photo to a piece: asks the API for upload URLs, puts both images straight into
 * storage, then asks the API to check them. Resolves to the piece with its new photo.
 *
 * The images never pass through the API. The upload URLs carry their own signature, so they
 * go without a bearer token.
 */
export async function uploadPiecePhoto(studioId: string, pieceId: string, files: PhotoFiles): Promise<Piece> {
  return uploadPhoto(
    files,
    async () => (await startPhotoUpload({ path: { studioId, pieceId }, throwOnError: true })).data,
    async (photoId) =>
      (await completePhotoUpload({ path: { studioId, pieceId, photoId }, throwOnError: true })).data,
  );
}

/** Adds a photo to a maintenance record (staff only), the same way as to a piece. */
export async function uploadRecordPhoto(studioId: string, recordId: string, files: PhotoFiles): Promise<void> {
  await uploadPhoto(
    files,
    async () => (await startRecordPhotoUpload({ path: { studioId, recordId }, throwOnError: true })).data,
    async (photoId) => {
      await completeRecordPhotoUpload({ path: { studioId, recordId, photoId }, throwOnError: true });
    },
  );
}

async function uploadPhoto<T>(
  files: PhotoFiles,
  start: () => Promise<PhotoUpload>,
  complete: (photoId: string) => Promise<T>,
): Promise<T> {
  const upload = await start();
  if (files.photo.size > upload.maxPhotoBytes || files.thumbnail.size > upload.maxThumbnailBytes) {
    throw new Error('This photo is too large to upload.');
  }
  await Promise.all([putImage(upload.photoUploadUrl, files.photo), putImage(upload.thumbnailUploadUrl, files.thumbnail)]);
  return complete(upload.photoId);
}

async function putImage(url: string, body: Blob): Promise<void> {
  const response = await fetch(url, {
    method: 'PUT',
    headers: { 'x-ms-blob-type': 'BlockBlob', 'Content-Type': 'image/jpeg' },
    body,
  });
  // 409 means an earlier try already created it: the URL can create the image but never replace it
  if (!response.ok && response.status !== 409) {
    throw new Error(`The photo didn't upload (status ${String(response.status)}). Try again.`);
  }
}
