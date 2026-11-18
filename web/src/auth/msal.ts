import {
  type AccountInfo,
  AuthError,
  type AuthenticationResult,
  BrowserAuthErrorCodes,
  EventType,
  InteractionRequiredAuthError,
  type IPublicClientApplication,
  PublicClientApplication,
} from '@azure/msal-browser';

import type { AuthSettings } from '../config';

/**
 * MSAL for Microsoft Entra External ID. Sign-in uses the redirect flow with the
 * authorization code grant and PKCE (MSAL does PKCE automatically).
 */
export function createMsalInstance(settings: AuthSettings): PublicClientApplication {
  const appRoot = `${window.location.origin}/`;
  const instance = new PublicClientApplication({
    auth: {
      clientId: settings.clientId,
      authority: settings.authority,
      knownAuthorities: [settings.knownAuthority],
      // One registered redirect URI per origin; MSAL then returns to the page that
      // started sign-in (`redirectStartPage`).
      redirectUri: appRoot,
      postLogoutRedirectUri: appRoot,
    },
  });

  instance.addEventCallback((event) => {
    if (event.eventType === EventType.LOGIN_SUCCESS || event.eventType === EventType.ACQUIRE_TOKEN_SUCCESS) {
      const { account } = event.payload as AuthenticationResult;
      instance.setActiveAccount(account);
    }
  });

  return instance;
}

export function currentAccount(instance: IPublicClientApplication, accounts?: AccountInfo[]): AccountInfo | null {
  return instance.getActiveAccount() ?? (accounts ?? instance.getAllAccounts())[0] ?? null;
}

/**
 * Where a silent refresh's hidden iframe returns: a blank page that hands the response back
 * to the app (redirect.html, src/auth/redirectBridge.ts). MSAL 5 needs this page; the app
 * root can't do it. It's registered as a redirect URI next to the origin root.
 */
export function silentRedirectUri(): string {
  return `${window.location.origin}/redirect.html`;
}

/**
 * A token for the API: from the cache or a silent refresh, else by redirecting to sign
 * in again (consent changed, session expired, or the silent refresh timed out). Returns
 * `undefined` when signed out or while that redirect is under way.
 */
export async function acquireApiToken(instance: IPublicClientApplication, scopes: string[]): Promise<string | undefined> {
  const account = currentAccount(instance);
  if (!account) {
    return undefined;
  }
  try {
    const result = await instance.acquireTokenSilent({ scopes, account, redirectUri: silentRedirectUri() });
    return result.accessToken;
  } catch (error) {
    if (needsInteraction(error)) {
      await instance.acquireTokenRedirect({ scopes, account, redirectStartPage: window.location.href });
      return undefined;
    }
    throw error;
  }
}

/**
 * Whether to sign in again. A silent refresh that times out (the iframe never got a response,
 * for example when the browser blocks it) can't succeed by retrying, so it goes to sign-in too.
 */
function needsInteraction(error: unknown): boolean {
  return (
    error instanceof InteractionRequiredAuthError ||
    (error instanceof AuthError && error.errorCode === BrowserAuthErrorCodes.timedOut)
  );
}
