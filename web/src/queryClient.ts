import { hasProblemCode, isApiError } from '@studiomanager/api-client';
import { QueryClient } from '@tanstack/react-query';

export function createQueryClient(): QueryClient {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        // Retrying a 4xx can't succeed; retry network errors and 5xx a couple of times.
        retry: (failureCount, error) => !(isApiError(error) && error.status < 500) && failureCount < 2,
      },
    },
  });
  // Someone else changed what this page shows. Its error says to reload and try again, so
  // fetch the current version now, and the next try uses it.
  queryClient.getMutationCache().subscribe((event) => {
    if (event.type === 'updated' && event.action.type === 'error' && hasProblemCode(event.action.error, 'stale-version')) {
      void queryClient.invalidateQueries();
    }
  });
  return queryClient;
}
