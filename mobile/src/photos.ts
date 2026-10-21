import type { PhotoFiles } from '@studiomanager/api-client';
import { ImageManipulator, SaveFormat } from 'expo-image-manipulator';
import * as ImagePicker from 'expo-image-picker';

/** Long edges, as on the web: enough to tell pieces apart on a shelf, small enough for studio wifi. */
const PHOTO_EDGE = 1600;
const THUMBNAIL_EDGE = 320;

/** A photo ready to upload, and a local preview of it. */
export interface PreparedPhoto extends PhotoFiles {
  previewUri: string;
}

/**
 * Takes a photo with the camera, or picks one from the library, and shrinks it into a photo and
 * a thumbnail, both JPEG. Re-encoding drops the metadata the camera wrote, including where the
 * picture was taken. Resolves to null if the person cancels.
 */
export async function pickPhoto(source: 'camera' | 'library'): Promise<PreparedPhoto | null> {
  if (source === 'camera') {
    const permission = await ImagePicker.requestCameraPermissionsAsync();
    if (!permission.granted) {
      throw new Error('Allow camera access in Settings to photograph pieces.');
    }
  }
  const options: ImagePicker.ImagePickerOptions = { mediaTypes: ['images'], quality: 1, exif: false };
  const result =
    source === 'camera' ? await ImagePicker.launchCameraAsync(options) : await ImagePicker.launchImageLibraryAsync(options);
  const picked = result.canceled ? undefined : result.assets[0];
  if (!picked) {
    return null;
  }
  const [photo, thumbnail] = await Promise.all([
    shrink(picked, PHOTO_EDGE, 0.85),
    shrink(picked, THUMBNAIL_EDGE, 0.8),
  ]);
  return { photo: await toBlob(photo), thumbnail: await toBlob(thumbnail), previewUri: thumbnail };
}

async function shrink(image: ImagePicker.ImagePickerAsset, maxEdge: number, compress: number): Promise<string> {
  const context = ImageManipulator.manipulate(image.uri);
  if (image.width >= image.height) {
    context.resize({ width: Math.min(maxEdge, image.width) });
  } else {
    context.resize({ height: Math.min(maxEdge, image.height) });
  }
  const rendered = await context.renderAsync();
  const saved = await rendered.saveAsync({ format: SaveFormat.JPEG, compress });
  return saved.uri;
}

async function toBlob(uri: string): Promise<Blob> {
  const response = await fetch(uri);
  return response.blob();
}
