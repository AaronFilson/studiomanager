export interface AuthSettings {
  clientId: string;
  /** The tenant's issuer, e.g. `https://<subdomain>.ciamlogin.com/<tenant-id>/v2.0`; discovery is read from it. */
  issuer: string;
  /** API scopes. Sign-in adds openid and offline_access (for the refresh token). */
  scopes: string[];
}

export interface AppConfig {
  /** Absolute API origin without a trailing slash. */
  apiBaseUrl: string;
  /** `null` when sign-in is not configured; `problems` says what to fix. */
  auth: AuthSettings | null;
  problems: string[];
}

export interface Env {
  EXPO_PUBLIC_ENTRA_CLIENT_ID?: string;
  EXPO_PUBLIC_ENTRA_ISSUER?: string;
  EXPO_PUBLIC_API_SCOPES?: string;
  EXPO_PUBLIC_API_BASE_URL?: string;
}

/**
 * Expo inlines `EXPO_PUBLIC_*` variables at build time, but only where each is named in full,
 * so they are listed one by one rather than read from `process.env` as a whole.
 */
/* eslint-disable @typescript-eslint/no-unsafe-assignment -- process.env is untyped in React Native,
   and a cast would stop Expo recognising the names it inlines */
export const buildEnv: Env = {
  EXPO_PUBLIC_ENTRA_CLIENT_ID: process.env.EXPO_PUBLIC_ENTRA_CLIENT_ID,
  EXPO_PUBLIC_ENTRA_ISSUER: process.env.EXPO_PUBLIC_ENTRA_ISSUER,
  EXPO_PUBLIC_API_SCOPES: process.env.EXPO_PUBLIC_API_SCOPES,
  EXPO_PUBLIC_API_BASE_URL: process.env.EXPO_PUBLIC_API_BASE_URL,
};
/* eslint-enable @typescript-eslint/no-unsafe-assignment */

export function readConfig(env: Env): AppConfig {
  const clientId = env.EXPO_PUBLIC_ENTRA_CLIENT_ID?.trim() ?? '';
  const issuer = (env.EXPO_PUBLIC_ENTRA_ISSUER?.trim() ?? '').replace(/\/+$/, '');
  const scopes = (env.EXPO_PUBLIC_API_SCOPES ?? '').split(/\s+/).filter(Boolean);
  const apiBaseUrl = (env.EXPO_PUBLIC_API_BASE_URL?.trim() ?? '').replace(/\/+$/, '');

  const problems: string[] = [];
  if (!clientId) problems.push('EXPO_PUBLIC_ENTRA_CLIENT_ID is not set.');
  if (!issuer) {
    problems.push('EXPO_PUBLIC_ENTRA_ISSUER is not set.');
  } else if (!/^https:\/\/[^/]+\/.+/.test(issuer)) {
    problems.push(`EXPO_PUBLIC_ENTRA_ISSUER is not an https issuer URL: "${issuer}".`);
  }
  if (scopes.length === 0) problems.push('EXPO_PUBLIC_API_SCOPES is not set.');
  // A phone can't reach "localhost" on the computer running the API, so there is no default
  if (!apiBaseUrl) problems.push('EXPO_PUBLIC_API_BASE_URL is not set.');

  if (problems.length > 0) {
    return { apiBaseUrl, auth: null, problems };
  }
  return { apiBaseUrl, auth: { clientId, issuer, scopes }, problems };
}
