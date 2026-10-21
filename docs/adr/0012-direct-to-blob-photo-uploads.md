# 12. Photos uploaded straight to Blob Storage with signed URLs

- **Status:** accepted
- **Date:** 2026-10-20

## Context

Studios tell pieces apart by the maker's mark and a photo (P1 item 4). Members and staff take
those photos on phones, at the shelf, over studio wifi. The images have to be stored, shown on
the shelf as thumbnails, and kept to each studio.

Sending the bytes through the API would tie up a request for every upload. The API scales to
zero on Container Apps, so it would also pay for the transfer and need a body-size limit and
streaming code of its own. Storage can take the upload directly.

## Decision

- **The client uploads straight to Blob Storage, with a signed URL the API hands out.**
  1. `POST …/pieces/{id}/photos` records a pending photo and returns two SAS URLs, one for the
     photo and one for the thumbnail. Each URL is scoped to one blob, expires after 15 minutes,
     and has only the **create** permission, so it can't read anything or replace an image once
     the API has checked it.
  2. The client `PUT`s each JPEG to its URL. No bearer token is involved.
  3. `POST …/photos/{photoId}/complete` checks that both blobs exist, are within the limits
     (8 MB and 512 KB) and start with the JPEG marker. Only then does the photo show on the piece.
- **The client shrinks the image and makes the thumbnail.** The web redraws it on a canvas; the
  phone uses `expo-image-manipulator`. Both produce a 1600-pixel photo and a 320-pixel
  thumbnail. Re-encoding also drops the camera's metadata, including the location. The server
  needs no image library, and only ever stores JPEG.
- **Blob names come from ids:** `{studio}/{piece}/{photo}/full.jpg` and `thumb.jpg`. A client
  never names a blob, so it can't reach another studio's.
- **Reading uses SAS URLs too,** with the read permission, a forced `image/jpeg` content type and
  `Cache-Control: private, max-age=3600`. They expire on the hour, one to two hours ahead, so
  every URL for a blob within the same hour is identical and the browser caches the image.
  Lists carry only each piece's first photo, which keeps 500-piece lists small.
- **Signing depends on where it runs** (`platform.storage.AzureBlobStore`):
  - On Azure, the account has shared keys switched off. The API asks Entra ID for a
    user-delegation key as its managed identity (Storage Blob Data Contributor), caches it for a
    day, and signs with that.
  - Locally and in tests there's no Entra ID, so it signs with Azurite's well-known account key.
    Azurite runs in `compose.yaml` and as a Testcontainer, so the whole flow is tested against a
    real storage API before Azure exists.
- **Abandoned uploads are swept.** Every hour `PhotoUploadSweep` removes photos still pending
  after a day, and their blobs. It runs with no tenant bound, so it goes through
  `app.take_abandoned_photo_uploads`, a SECURITY DEFINER function (ADR 10).

## Consequences

- The API never holds image bytes. An upload costs it two small requests.
- **The read URLs are bearer URLs.** Anyone given one can see the photo until it expires, at most
  two hours. That is the trade for caching and for `<img>` tags that need no token. Photos of
  pots aren't sensitive, and the location data is gone.
- The JPEG check reads only the first bytes, so it stops mistakes rather than a determined
  attacker. Since images are always served as `image/jpeg` from the storage domain, a disguised
  file can't run as script on the web app's origin.
- The web app's CSP allows `*.blob.core.windows.net` for images and uploads, and the storage
  account's CORS rule allows the web origin to `PUT` (`infra/modules/storage`).
- Deleting a photo deletes its blobs inside the database transaction, so a storage failure rolls
  the delete back. Blob soft delete keeps a deleted image for 7 days.
- Later photos, such as on maintenance records, can reuse `BlobStore` and the same flow.

## In an interview

"Phones upload straight to Blob Storage with a SAS URL that can only create one named blob for
fifteen minutes. The API then checks what landed before it shows it. In Azure there are no
storage keys at all: the API signs with a user-delegation key it gets as its managed identity.
Read URLs are rounded to the hour, so the browser can cache them. And the whole flow runs
against Azurite in the integration tests, including a test that the upload URL can't overwrite
a photo once it's checked."
