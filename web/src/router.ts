import type { QueryClient } from '@tanstack/react-query';
import { createRouter, type RouterHistory } from '@tanstack/react-router';

import type { Auth } from './auth/auth';
import { routeTree } from './routeTree.gen';

export interface RouterContext {
  auth: Auth;
  queryClient: QueryClient;
}

export function createAppRouter(queryClient: QueryClient, history?: RouterHistory) {
  return createRouter({
    routeTree,
    history,
    // `auth` comes from <RouterProvider context={{ auth }}> on every render (see App).
    context: { queryClient } as RouterContext,
    defaultPreload: 'intent',
    scrollRestoration: true,
  });
}

export type AppRouter = ReturnType<typeof createAppRouter>;

declare module '@tanstack/react-router' {
  interface Register {
    router: AppRouter;
  }
}
