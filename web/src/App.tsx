import { type QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from '@tanstack/react-router';

import { useAuth } from './auth/auth';
import type { AppRouter } from './router';

interface Props {
  router: AppRouter;
  queryClient: QueryClient;
}

export function App({ router, queryClient }: Props) {
  const auth = useAuth();
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} context={{ auth }} />
    </QueryClientProvider>
  );
}
