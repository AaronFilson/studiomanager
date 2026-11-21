import { hasProblemCode, isApiError } from '@studiomanager/api-client';
import { QueryClient, useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';

/** Retries network and server failures twice; a 4xx won't change by asking again. */
export function createQueryClient(): QueryClient {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: (failures, error) => !(isApiError(error) && error.status < 500) && failures < 2,
        staleTime: 30_000,
      },
    },
  });
  // Someone else changed what this screen shows. Fetch the current version, so trying again works.
  queryClient.getMutationCache().subscribe((event) => {
    if (event.type === 'updated' && event.action.type === 'error' && hasProblemCode(event.action.error, 'stale-version')) {
      void queryClient.invalidateQueries();
    }
  });
  return queryClient;
}

/** Clears cached studio data whenever auth transitions to signedOut, protecting shared devices. */
export function useClearCacheOnSignOut(status: 'starting' | 'signedOut' | 'signedIn'): void {
  const queryClient = useQueryClient();
  const previousStatus = useRef(status);

  useEffect(() => {
    if (status === 'signedOut' && previousStatus.current !== 'signedOut') {
      void queryClient.cancelQueries();
      queryClient.clear();
    }
    previousStatus.current = status;
  }, [status, queryClient]);
}
