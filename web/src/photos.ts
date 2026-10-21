import type { PhotoFiles } from '@studiomanager/api-client';

/** Long edges: enough to tell pieces apart on a shelf, small enough to upload on studio wifi. */
const PHOTO_EDGE = 1600;
const THUMBNAIL_EDGE = 320;

/**
 * Shrinks a picked or taken image into a photo and a thumbnail, both JPEG. Redrawing on a canvas
 * also drops the metadata the camera wrote, including where the picture was taken.
 */
export async function preparePhoto(file: Blob): Promise<PhotoFiles> {
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  } catch {
    throw new Error("This image couldn't be read. Try a JPEG or PNG.");
  }
  try {
    const [photo, thumbnail] = await Promise.all([toJpeg(bitmap, PHOTO_EDGE, 0.85), toJpeg(bitmap, THUMBNAIL_EDGE, 0.8)]);
    return { photo, thumbnail };
  } finally {
    bitmap.close();
  }
}

function toJpeg(bitmap: ImageBitmap, maxEdge: number, quality: number): Promise<Blob> {
  const scale = Math.min(1, maxEdge / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.round(bitmap.width * scale));
  canvas.height = Math.max(1, Math.round(bitmap.height * scale));
  const context = canvas.getContext('2d');
  if (!context) {
    return Promise.reject(new Error("This browser can't prepare photos."));
  }
  context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob);
        } else {
          reject(new Error("This image couldn't be read. Try a JPEG or PNG."));
        }
      },
      'image/jpeg',
      quality,
    );
  });
}
