export interface AuthSettings {
  clientId: string;
  /** e.g. `https://<subdomain>.ciamlogin.com/` */
  authority: string;
  /** The authority's host. MSAL only trusts non-Microsoft-cloud authorities listed here. */
  knownAuthority: string;
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
  VITE_ENTRA_CLIENT_ID?: string;
  VITE_ENTRA_AUTHORITY?: string;
  VITE_API_SCOPES?: string;
  VITE_API_BASE_URL?: string;
}

/** Reads build-time settings. `origin` is the page origin, used when no API origin is set. */
export function readConfig(env: Env, origin: string): AppConfig {
  const clientId = env.VITE_ENTRA_CLIENT_ID?.trim() ?? '';
  const authority = env.VITE_ENTRA_AUTHORITY?.trim() ?? '';
  const scopes = (env.VITE_API_SCOPES ?? '').split(/\s+/).filter(Boolean);
  const apiBaseUrl = (env.VITE_API_BASE_URL?.trim() || origin).replace(/\/+$/, '');

  const problems: string[] = [];
  if (!clientId) {
    problems.push('VITE_ENTRA_CLIENT_ID is not set.');
  }
  let knownAuthority = '';
  if (!authority) {
    problems.push('VITE_ENTRA_AUTHORITY is not set.');
  } else {
    try {
      knownAuthority = new URL(authority).host;
    } catch {
      problems.push(`VITE_ENTRA_AUTHORITY is not a URL: "${authority}".`);
    }
  }
  if (scopes.length === 0) {
    problems.push('VITE_API_SCOPES is not set.');
  }

  if (problems.length > 0) {
    return { apiBaseUrl, auth: null, problems };
  }
  return { apiBaseUrl, auth: { clientId, authority, knownAuthority, scopes }, problems };
}
