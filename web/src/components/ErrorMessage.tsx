import { isApiError } from '@studiomanager/api-client';

export function errorText(error: unknown): string {
  if (isApiError(error)) {
    return error.detail ?? error.title ?? error.message;
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
