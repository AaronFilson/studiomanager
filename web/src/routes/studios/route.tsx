import { createFileRoute } from '@tanstack/react-router';

/** Everything under /studios needs a signed-in user. */
export const Route = createFileRoute('/studios')({
  beforeLoad: async ({ context, location }) => {
    if (!context.auth.isAuthenticated) {
      await context.auth.signIn(location.href);
      // The browser is leaving for the sign-in page; keep this route pending until it does.
      await new Promise<never>(() => undefined);
    }
  },
  pendingComponent: () => <p>Taking you to sign in…</p>,
});
