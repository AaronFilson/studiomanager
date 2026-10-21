import type { ParsedLocation } from '@tanstack/react-router';

import type { RouterContext } from '../router';

/** A route guard: sends signed-out visitors to sign in, then back to where they were going. */
export async function requireSignIn({ context, location }: { context: RouterContext; location: ParsedLocation }) {
  if (!context.auth.isAuthenticated) {
    await context.auth.signIn(location.href);
    // The browser is leaving for the sign-in page; keep this route pending until it does.
    await new Promise<never>(() => undefined);
  }
}
