import { useSyncExternalStore } from 'react';

/**
 * Wakes the API as soon as the page loads (plan 23). The API scales to zero, and a cold start takes
 * most of a minute, so asking for its health before sign-in finishes overlaps the two waits.
 *
 * The request is `no-cors`: the health endpoint is public, and any answer at all, even an opaque
 * one, means a replica is up. Container Apps holds a request to a scaled-to-zero app until a
 * replica is ready, so one request usually covers the whole start. A network error is retried.
 */

/** `slow` is `waking` that has taken longer than `slowAfterMs`, so a warm API never shows a notice. */
export type WakeState = 'idle' | 'waking' | 'slow' | 'awake' | 'failed';

let state: WakeState = 'idle';
const listeners = new Set<() => void>();

function setState(next: WakeState) {
  state = next;
  for (const listener of listeners) {
    listener();
  }
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

export function useWakeState(): WakeState {
  return useSyncExternalStore(subscribe, () => state);
}

export interface WakeOptions {
  /** Gives up after this long; the page still works, its own requests just wait. */
  giveUpAfterMs?: number;
  retryEveryMs?: number;
  slowAfterMs?: number;
  fetchFn?: typeof fetch;
}

export async function wakeApi(baseUrl: string, options: WakeOptions = {}): Promise<WakeState> {
  const { giveUpAfterMs = 180_000, retryEveryMs = 3_000, slowAfterMs = 2_000, fetchFn = fetch } = options;
  const url = `${baseUrl.replace(/\/$/, '')}/actuator/health/liveness`;
  const deadline = Date.now() + giveUpAfterMs;
  setState('waking');
  const slowTimer = setTimeout(() => {
    if (state === 'waking') {
      setState('slow');
    }
  }, slowAfterMs);
  try {
    while (Date.now() < deadline) {
      try {
        await fetchFn(url, { mode: 'no-cors', cache: 'no-store', credentials: 'omit' });
        setState('awake');
        return state;
      } catch {
        await new Promise((resolve) => setTimeout(resolve, retryEveryMs));
      }
    }
    setState('failed');
    return state;
  } finally {
    clearTimeout(slowTimer);
  }
}

/** For tests: back to a page that never asked. */
export function resetWakeState() {
  setState('idle');
}
