import { isApiError } from '@studiomanager/api-client';

export function errorText(error: unknown): string {
  if (isApiError(error)) {
    return error.detail ?? error.title ?? error.message;
  }
  // Our own errors carry a message for people, such as a photo that couldn't be read. A failed
  // fetch is a TypeError, whose message ("Failed to fetch") isn't.
  if (error instanceof Error && !(error instanceof TypeError)) {
    return error.message;
  }
  return 'Could not reach Studio Manager. Check your connection and try again.';
}

export function ErrorMessage({ error }: { error: unknown }) {
  return (
    <p role="alert" className="error">
      {errorText(error)}
    </p>
  );
}
