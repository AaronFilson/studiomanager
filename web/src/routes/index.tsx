import { createFileRoute, redirect } from '@tanstack/react-router';

import { useAuth } from '../auth/auth';

export const Route = createFileRoute('/')({
  beforeLoad: ({ context }) => {
    if (context.auth.isAuthenticated) {
      throw redirect({ to: '/studios' });
    }
  },
  component: Landing,
});

function Landing() {
  const auth = useAuth();
  return (
    <section className="landing stack">
      <h1>The studio manager that knows what a kiln is.</h1>
      <p>
        Track every piece from greenware to pickup, charge firing fees that cover your real costs, and tell members
        the moment their work comes out of the kiln. Free for studios.
      </p>
      <div>
        <button type="button" className="primary" onClick={() => void auth.signIn('/studios')}>
          Sign in
        </button>
      </div>
    </section>
  );
}
