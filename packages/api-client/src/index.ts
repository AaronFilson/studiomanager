import { client } from './generated/client.gen';
import { toApiError } from './errors';

export * from './generated';
export * from './generated/@tanstack/react-query.gen';
export { client } from './generated/client.gen';
export { ApiError, isApiError } from './errors';
export { type PhotoFiles, uploadPiecePhoto } from './photos';

export interface ApiClientConfig {
  /** Absolute API origin, e.g. `https://api.example.com`. For the same origin, pass `window.location.origin`. */
  baseUrl: string;
  /** Returns a bearer token for the API, or `undefined` when signed out. */
  getAccessToken: () => Promise<string | undefined>;
}

let errorInterceptor: number | undefined;

/**
 * Points the shared client at the API and wires authentication and error handling.
 * Safe to call more than once (e.g. React StrictMode, tests); the last call wins.
 *
 * The token is attached through the client's `auth` hook, which the generated SDK
 * applies to every operation the spec marks with `bearerAuth`.
 */
export function configureApiClient({ baseUrl, getAccessToken }: ApiClientConfig): void {
  client.setConfig({
    baseUrl,
    auth: () => getAccessToken(),
  });
  if (errorInterceptor !== undefined) {
    client.interceptors.error.eject(errorInterceptor);
  }
  errorInterceptor = client.interceptors.error.use((error, response) => toApiError(error, response));
}
