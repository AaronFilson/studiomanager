import { isApiError } from '@studiomanager/api-client';
import { QueryClient } from '@tanstack/react-query';

/** Retries network and server failures twice; a 4xx won't change by asking again. */
export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: (failures, error) => !(isApiError(error) && error.status < 500) && failures < 2,
        staleTime: 30_000,
      },
    },
  });
}
