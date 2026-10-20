import { createFileRoute } from '@tanstack/react-router';

import { requireSignIn } from '../../auth/requireSignIn';

/** Everything under /studios needs a signed-in user. */
export const Route = createFileRoute('/studios')({
  beforeLoad: requireSignIn,
  pendingComponent: () => <p>Taking you to sign in…</p>,
});
