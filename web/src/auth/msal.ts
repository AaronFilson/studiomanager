import {
  type AccountInfo,
  type AuthenticationResult,
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
 * A token for the API: from the cache or a silent refresh, else by redirecting to sign
 * in again (consent changed, session expired). Returns `undefined` when signed out or
 * while that redirect is under way.
 */
export async function acquireApiToken(instance: IPublicClientApplication, scopes: string[]): Promise<string | undefined> {
  const account = currentAccount(instance);
  if (!account) {
    return undefined;
  }
  try {
    const result = await instance.acquireTokenSilent({ scopes, account });
    return result.accessToken;
  } catch (error) {
    if (error instanceof InteractionRequiredAuthError) {
      await instance.acquireTokenRedirect({ scopes, account, redirectStartPage: window.location.href });
      return undefined;
    }
    throw error;
  }
}
