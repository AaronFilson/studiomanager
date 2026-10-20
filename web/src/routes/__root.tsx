import { createRootRouteWithContext, Link, Outlet } from '@tanstack/react-router';

import { useAuth } from '../auth/auth';
import type { RouterContext } from '../router';

export const Route = createRootRouteWithContext<RouterContext>()({
  component: RootLayout,
  notFoundComponent: NotFound,
});

function RootLayout() {
  const auth = useAuth();
  return (
    <>
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <header className="site-header">
        <div className="container header-row">
          <Link to="/" className="brand">
            Studio Manager
          </Link>
          {auth.user && (
            <div className="header-user">
              <span>{auth.user.name ?? auth.user.username}</span>
              <button type="button" onClick={() => void auth.signOut()}>
                Sign out
              </button>
            </div>
          )}
        </div>
      </header>
      <main id="main" className="container">
        <Outlet />
      </main>
    </>
  );
}

function NotFound() {
  return (
    <>
      <h1>Page not found</h1>
      <p>
        <Link to="/">Go to the home page</Link>
      </p>
    </>
  );
}
