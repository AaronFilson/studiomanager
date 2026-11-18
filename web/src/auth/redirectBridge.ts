import { broadcastResponseToMainFrame } from '@azure/msal-browser/redirect-bridge';

/*
 * The page a silent token refresh lands on (redirect.html). MSAL 5 refreshes in a hidden iframe
 * and waits for this page to hand the sign-in response back to the app over a BroadcastChannel;
 * the app itself doesn't load here.
 */
broadcastResponseToMainFrame().catch((error: unknown) => {
  console.error('Studio Manager: could not pass the sign-in response back to the app', error);
});
