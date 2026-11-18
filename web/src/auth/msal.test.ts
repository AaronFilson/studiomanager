import {
  type AccountInfo,
  AuthError,
  BrowserAuthErrorCodes,
  InteractionRequiredAuthError,
  type IPublicClientApplication,
} from '@azure/msal-browser';
import { describe, expect, it, vi } from 'vitest';

import { acquireApiToken, silentRedirectUri } from './msal';

const scopes = ['api://api-client-id/access_as_user'];
const account = { homeAccountId: 'home', username: 'potter@example.com' } as AccountInfo;

function fakeInstance(silent: () => Promise<{ accessToken: string }>) {
  const acquireTokenSilent = vi.fn(silent);
  const acquireTokenRedirect = vi.fn(() => Promise.resolve());
  const instance = {
    getActiveAccount: () => account,
    getAllAccounts: () => [account],
    acquireTokenSilent,
    acquireTokenRedirect,
  } as unknown as IPublicClientApplication;
  return { instance, acquireTokenSilent, acquireTokenRedirect };
}

describe('acquireApiToken', () => {
  it('refreshes silently through the redirect bridge page', async () => {
    const { instance, acquireTokenSilent } = fakeInstance(() => Promise.resolve({ accessToken: 'token' }));

    await expect(acquireApiToken(instance, scopes)).resolves.toBe('token');
    expect(acquireTokenSilent).toHaveBeenCalledWith({ scopes, account, redirectUri: silentRedirectUri() });
    expect(silentRedirectUri()).toBe(`${window.location.origin}/redirect.html`);
  });

  it('signs in again when the session needs interaction', async () => {
    const { instance, acquireTokenRedirect } = fakeInstance(() =>
      Promise.reject(new InteractionRequiredAuthError('login_required', '')),
    );

    await expect(acquireApiToken(instance, scopes)).resolves.toBeUndefined();
    expect(acquireTokenRedirect).toHaveBeenCalledOnce();
  });

  it('signs in again when the silent refresh times out', async () => {
    const { instance, acquireTokenRedirect } = fakeInstance(() =>
      Promise.reject(new AuthError(BrowserAuthErrorCodes.timedOut, '')),
    );

    await expect(acquireApiToken(instance, scopes)).resolves.toBeUndefined();
    expect(acquireTokenRedirect).toHaveBeenCalledOnce();
  });

  it('passes other failures on', async () => {
    const failure = new AuthError('network_error', '');
    const { instance, acquireTokenRedirect } = fakeInstance(() => Promise.reject(failure));

    await expect(acquireApiToken(instance, scopes)).rejects.toBe(failure);
    expect(acquireTokenRedirect).not.toHaveBeenCalled();
  });
});
