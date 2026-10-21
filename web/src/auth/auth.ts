import { createContext, use } from 'react';

/**
 * What the app needs from authentication. MSAL implements it in production
 * (MsalAuthProvider); tests provide a plain object, so nothing else imports MSAL.
 */
export interface Auth {
  isAuthenticated: boolean;
  user: { name?: string; username: string } | null;
  /** Starts the sign-in redirect; after sign-in the browser returns to `returnTo` (a path). */
  signIn: (returnTo?: string) => Promise<void>;
  signOut: () => Promise<void>;
  /** A token for the Studio Manager API, or `undefined` when signed out. */
  getAccessToken: () => Promise<string | undefined>;
}

export const AuthContext = createContext<Auth | null>(null);

export function useAuth(): Auth {
  const auth = use(AuthContext);
  if (!auth) {
    throw new Error('useAuth must be used inside an auth provider');
  }
  return auth;
}
