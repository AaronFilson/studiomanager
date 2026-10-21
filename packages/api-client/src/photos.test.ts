import { afterEach, describe, expect, it, vi } from 'vitest';

import { configureApiClient } from './index';
import { uploadPiecePhoto, uploadRecordPhoto } from './photos';

const studioId = '0b7f3f0e-6f1c-4c7a-9d55-1d2f2b8b4a10';
const pieceId = '5c1d2f2b-8b4a-4c7a-9d55-0b7f3f0e6f1c';
const photoId = '9d551d2f-2b8b-4a10-8c7a-0b7f3f0e6f1c';

const upload = {
  photoId,
  photoUploadUrl: 'https://blob.test/full.jpg?sig=a',
  thumbnailUploadUrl: 'https://blob.test/thumb.jpg?sig=b',
  expiresAt: '2026-10-20T15:15:00Z',
  maxPhotoBytes: 1000,
  maxThumbnailBytes: 100,
};

function routeFetch(putStatus = 201) {
  const calls: Request[] = [];
  const fetch = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const request = new Request(input, init);
    calls.push(request);
    if (request.url.startsWith('https://blob.test/')) {
      return Promise.resolve(new Response(null, { status: putStatus }));
    }
    if (request.url.endsWith('/photos')) {
      return Promise.resolve(Response.json(upload, { status: 201 }));
    }
    return Promise.resolve(Response.json({ id: pieceId, photos: [{ id: photoId }] }));
  });
  vi.stubGlobal('fetch', fetch);
  return calls;
}

describe('uploadPiecePhoto', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('puts both images straight into storage without the bearer token, then finishes', async () => {
    configureApiClient({ baseUrl: 'https://api.test', getAccessToken: () => Promise.resolve('token-123') });
    const calls = routeFetch();

    const piece = await uploadPiecePhoto(studioId, pieceId, {
      photo: new Blob(['full'], { type: 'image/jpeg' }),
      thumbnail: new Blob(['thumb'], { type: 'image/jpeg' }),
    });

    expect(piece.photos[0]?.id).toBe(photoId);
    const puts = calls.filter((c) => c.method === 'PUT');
    expect(puts.map((c) => c.url).sort()).toEqual([upload.photoUploadUrl, upload.thumbnailUploadUrl]);
    for (const put of puts) {
      expect(put.headers.get('x-ms-blob-type')).toBe('BlockBlob');
      expect(put.headers.get('Content-Type')).toBe('image/jpeg');
      expect(put.headers.has('Authorization')).toBe(false);
    }
    expect(calls.at(-1)?.url).toBe(
      `https://api.test/api/v1/studios/${studioId}/pieces/${pieceId}/photos/${photoId}/complete`,
    );
  });

  it('stops before finishing when storage refuses an image', async () => {
    configureApiClient({ baseUrl: 'https://api.test', getAccessToken: () => Promise.resolve('t') });
    const calls = routeFetch(403);

    await expect(
      uploadPiecePhoto(studioId, pieceId, { photo: new Blob(['full']), thumbnail: new Blob(['thumb']) }),
    ).rejects.toThrow("The photo didn't upload (status 403). Try again.");
    expect(calls.some((c) => c.url.endsWith('/complete'))).toBe(false);
  });

  it('adds a photo to a maintenance record the same way', async () => {
    configureApiClient({ baseUrl: 'https://api.test', getAccessToken: () => Promise.resolve('t') });
    const calls = routeFetch();
    const recordId = '1d2f2b8b-4a10-4c7a-9d55-0b7f3f0e6f1c';

    await uploadRecordPhoto(studioId, recordId, { photo: new Blob(['full']), thumbnail: new Blob(['thumb']) });

    expect(calls.filter((c) => c.method === 'PUT')).toHaveLength(2);
    expect(calls.at(-1)?.url).toBe(
      `https://api.test/api/v1/studios/${studioId}/maintenance/records/${recordId}/photos/${photoId}/complete`,
    );
  });

  it('refuses images over the limits the API sent', async () => {
    configureApiClient({ baseUrl: 'https://api.test', getAccessToken: () => Promise.resolve('t') });
    routeFetch();

    await expect(
      uploadPiecePhoto(studioId, pieceId, { photo: new Blob(['x'.repeat(1001)]), thumbnail: new Blob(['t']) }),
    ).rejects.toThrow('This photo is too large to upload.');
  });
});
