import { act, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ServerWaking } from '../components/ServerWaking';
import { resetWakeState, wakeApi } from '../wakeApi';

describe('wakeApi', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    resetWakeState();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('asks the public health endpoint, without credentials, and is awake on any answer', async () => {
    const fetchFn = vi.fn().mockResolvedValue(new Response(null));
    await expect(wakeApi('https://api.example/', { fetchFn })).resolves.toBe('awake');
    expect(fetchFn).toHaveBeenCalledWith('https://api.example/actuator/health/liveness', {
      mode: 'no-cors',
      cache: 'no-store',
      credentials: 'omit',
    });
  });

  it('retries a network error, and gives up at the deadline', async () => {
    const fetchFn = vi.fn().mockRejectedValue(new TypeError('Failed to fetch'));
    const result = wakeApi('https://api.example', { fetchFn, giveUpAfterMs: 10_000, retryEveryMs: 3_000 });
    await vi.advanceTimersByTimeAsync(12_000);
    await expect(result).resolves.toBe('failed');
    expect(fetchFn.mock.calls.length).toBeGreaterThan(1);
  });

  it('shows the notice only once waking has taken a moment, and hides it when awake', async () => {
    let answer: (response: Response) => void = () => {};
    const fetchFn = vi.fn(() => new Promise<Response>((resolve) => (answer = resolve)));
    render(<ServerWaking />);
    const woken = wakeApi('https://api.example', { fetchFn });

    await act(() => vi.advanceTimersByTimeAsync(1_000));
    expect(screen.queryByRole('status')).toBeNull();

    await act(() => vi.advanceTimersByTimeAsync(1_500));
    expect(screen.getByRole('status')).toHaveTextContent('Starting the studio server');

    await act(async () => {
      answer(new Response(null));
      await woken;
    });
    expect(screen.queryByRole('status')).toBeNull();
  });
});
