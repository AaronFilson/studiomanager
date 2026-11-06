import { getMeOptions } from '@studiomanager/api-client';
import { useQuery } from '@tanstack/react-query';
import { createRootRouteWithContext, Link, Outlet } from '@tanstack/react-router';

import { useAuth } from '../auth/auth';
import type { RouterContext } from '../router';

export const Route = createRootRouteWithContext<RouterContext>()({
  component: RootLayout,
  notFoundComponent: NotFound,
});

function RootLayout() {
  const auth = useAuth();
  // Only people with the Platform.Admin app role see the admin page's link
  const me = useQuery({ ...getMeOptions(), enabled: auth.isAuthenticated });
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
              {me.data?.platformAdmin && <Link to="/admin">Admin</Link>}
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
