import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react-native';

import { useClearCacheOnSignOut } from './queryClient';

function Probe({ status }: { status: 'starting' | 'signedOut' | 'signedIn' }) {
  useClearCacheOnSignOut(status);
  return null;
}

describe('useClearCacheOnSignOut', () => {
  it('clears cached queries when auth transitions to signedOut', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false, gcTime: Infinity } },
    });
    queryClient.setQueryData(['me'], { account: { id: 'user-a' } });

    const { rerender } = await render(
      <QueryClientProvider client={queryClient}>
        <Probe status="signedIn" />
      </QueryClientProvider>,
    );
    expect(queryClient.getQueryData(['me'])).toEqual({ account: { id: 'user-a' } });

    await rerender(
      <QueryClientProvider client={queryClient}>
        <Probe status="signedOut" />
      </QueryClientProvider>,
    );
    expect(queryClient.getQueryData(['me'])).toBeUndefined();
    queryClient.clear();
  });
});
