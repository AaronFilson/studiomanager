import { InteractionStatus, type IPublicClientApplication } from '@azure/msal-browser';
import { MsalProvider, useMsal } from '@azure/msal-react';
import { type ReactNode, useMemo } from 'react';

import { type Auth, AuthContext } from './auth';
import { acquireApiToken, currentAccount } from './msal';

interface Props {
  instance: IPublicClientApplication;
  scopes: string[];
  children: ReactNode;
}

/** Provides `Auth` backed by MSAL. MsalProvider initializes MSAL and finishes any sign-in redirect. */
export function MsalAuthProvider({ instance, scopes, children }: Props) {
  return (
    <MsalProvider instance={instance}>
      <MsalAuthBridge scopes={scopes}>{children}</MsalAuthBridge>
    </MsalProvider>
  );
}

function MsalAuthBridge({ scopes, children }: Omit<Props, 'instance'>) {
  const { instance, accounts, inProgress } = useMsal();

  const auth = useMemo<Auth>(() => {
    const account = currentAccount(instance, accounts);
    return {
      isAuthenticated: account !== null,
      user: account ? { name: account.name, username: account.username } : null,
      signIn: (returnTo = '/') =>
        instance.loginRedirect({ scopes, redirectStartPage: new URL(returnTo, window.location.origin).href }),
      signOut: () => instance.logoutRedirect({ account: account ?? undefined }),
      getAccessToken: () => acquireApiToken(instance, scopes),
    };
  }, [instance, accounts, scopes]);

  // Don't route until MSAL knows whether we're signed in, or route guards would bounce
  // a user who is halfway through the sign-in redirect.
  if (inProgress === InteractionStatus.Startup || inProgress === InteractionStatus.HandleRedirect) {
    return <p className="container">Signing you in…</p>;
  }
  return <AuthContext value={auth}>{children}</AuthContext>;
}
