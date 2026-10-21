import {
  exchangeCodeAsync,
  makeRedirectUri,
  refreshAsync,
  TokenError,
  type TokenResponse,
  useAuthRequest,
  useAutoDiscovery,
} from 'expo-auth-session';
import * as SecureStore from 'expo-secure-store';
import * as WebBrowser from 'expo-web-browser';
import { createContext, type ReactNode, use, useEffect, useMemo, useRef, useState } from 'react';

import type { AuthSettings } from '../config';
import { RefreshRejected, Session, type Tokens } from './session';

// Completes the sign-in when the browser hands control back to the app
WebBrowser.maybeCompleteAuthSession();

const REFRESH_TOKEN_KEY = 'studiomanager.refreshToken';

/** studiomanager://auth, registered on the mobile app in infra/identity. */
export const redirectUri = makeRedirectUri({ scheme: 'studiomanager', path: 'auth' });

export type AuthStatus = 'starting' | 'signedOut' | 'signedIn';

export interface Auth {
  status: AuthStatus;
  /** False until the identity provider's discovery document has loaded. */
  ready: boolean;
  signIn: () => Promise<void>;
  signOut: () => Promise<void>;
  getAccessToken: () => Promise<string | undefined>;
  error: string | null;
}

export const AuthContext = createContext<Auth | null>(null);

export function useAuth(): Auth {
  const auth = use(AuthContext);
  if (!auth) throw new Error('useAuth needs an AuthProvider');
  return auth;
}

function toTokens(response: TokenResponse): Tokens {
  const lifetimeSeconds = response.expiresIn ?? 300;
  return {
    accessToken: response.accessToken,
    expiresAt: (response.issuedAt + lifetimeSeconds) * 1000,
    refreshToken: response.refreshToken,
  };
}

/**
 * Sign-in with Microsoft Entra External ID: the authorization code flow with PKCE in the system
 * browser, then tokens refreshed in the background. Only the refresh token is stored, in the
 * device's keychain or keystore.
 */
export function AuthProvider({ settings, children }: { settings: AuthSettings; children: ReactNode }) {
  const discovery = useAutoDiscovery(settings.issuer);
  const scopes = useMemo(() => [...settings.scopes, 'openid', 'offline_access'], [settings.scopes]);
  const [request, , promptAsync] = useAuthRequest({ clientId: settings.clientId, scopes, redirectUri }, discovery);
  const [status, setStatus] = useState<AuthStatus>('starting');
  const [error, setError] = useState<string | null>(null);

  // One session for the app's lifetime; discovery arrives later, so refreshes read it from a ref
  const discoveryRef = useRef(discovery);
  useEffect(() => {
    discoveryRef.current = discovery;
  }, [discovery]);
  const [session] = useState(
    () =>
      new Session({
        store: {
          get: () => SecureStore.getItemAsync(REFRESH_TOKEN_KEY),
          set: (token) => SecureStore.setItemAsync(REFRESH_TOKEN_KEY, token),
          clear: () => SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY),
        },
        refresh: async (refreshToken) => {
          const current = discoveryRef.current;
          if (!current) throw new Error('Sign-in is still starting.');
          try {
            return toTokens(await refreshAsync({ clientId: settings.clientId, refreshToken, scopes }, current));
          } catch (e) {
            if (e instanceof TokenError) throw new RefreshRejected(e.code);
            throw e;
          }
        },
      }),
  );

  useEffect(() => {
    let cancelled = false;
    void session.restore().then((restored) => {
      if (!cancelled) setStatus(restored ? 'signedIn' : 'signedOut');
    });
    const unsubscribe = session.onChange((signedIn) => setStatus(signedIn ? 'signedIn' : 'signedOut'));
    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, [session]);

  const auth = useMemo<Auth>(
    () => ({
      status,
      ready: discovery !== null && request !== null,
      error,
      getAccessToken: () => session.getAccessToken(),
      signOut: () => session.signOut(),
      signIn: async () => {
        if (!discovery || !request) return;
        setError(null);
        const result = await promptAsync();
        if (result.type !== 'success') {
          if (result.type === 'error') setError(result.error?.message ?? 'Sign-in failed.');
          return;
        }
        const tokens = await exchangeCodeAsync(
          {
            clientId: settings.clientId,
            code: result.params.code ?? '',
            redirectUri,
            extraParams: { code_verifier: request.codeVerifier ?? '' },
          },
          discovery,
        );
        await session.signedIn(toTokens(tokens));
      },
    }),
    [status, discovery, request, error, session, promptAsync, settings.clientId],
  );

  return <AuthContext value={auth}>{children}</AuthContext>;
}
