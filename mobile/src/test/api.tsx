import { configureApiClient, type Piece } from '@studiomanager/api-client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react-native';
import type { ReactElement } from 'react';

export const API = 'http://api.test';

export interface Call {
  method: string;
  path: string;
  /** The query string, with its leading `?`, or `''`. */
  query: string;
  body: unknown;
}

type Handler = (call: Call) => unknown;

/**
 * Answers the shared API client's requests from `routes` ("GET /api/v1/..." with `:param`
 * segments): a JSON body, a function of the call, or `null` for 204 No Content. An unmatched
 * request fails the test. Returns every call made, in order.
 */
export function stubApi(routes: Record<string, Handler | object | null>): Call[] {
  const calls: Call[] = [];
  const matchers = Object.entries(routes).map(([route, handler]) => {
    const [method, pattern = ''] = route.split(' ');
    const regex = new RegExp(`^${pattern.replace(/:[^/]+/g, '[^/]+')}$`);
    return { method, regex, handler };
  });
  globalThis.fetch = jest.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const request = input instanceof Request ? input : new Request(input, init);
    const url = new URL(request.url);
    const text = await request.text();
    const call: Call = {
      method: request.method,
      path: url.pathname,
      query: url.search,
      body: text ? (JSON.parse(text) as unknown) : undefined,
    };
    calls.push(call);
    const match = matchers.find((m) => m.method === call.method && m.regex.test(call.path));
    if (!match) {
      throw new Error(`Unexpected request: ${call.method} ${call.path}`);
    }
    const result = typeof match.handler === 'function' ? (match.handler as Handler)(call) : match.handler;
    if (result === null) return new Response(null, { status: 204 });
    return Response.json(result);
  }) as typeof fetch;
  configureApiClient({ baseUrl: API, getAccessToken: () => Promise.resolve('test-token') });
  return calls;
}

export async function renderWithQueries(ui: ReactElement) {
  // No retries, and no garbage-collection timers left running after a test ends
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: Infinity }, mutations: { retry: false, gcTime: Infinity } },
  });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

export function aPiece(overrides: Partial<Piece> = {}): Piece {
  return {
    id: 'p1',
    number: 42,
    makerAccountId: 'a1',
    makerName: 'Mo Maker',
    description: 'Lidded jar',
    size: {},
    stage: 'RECEIVED',
    fee: { amount: { amount: '12.00', currency: 'USD' }, scheduleVersion: 1, taxCategory: 'FIRING_SERVICE', breakdown: 'Flat' },
    receivedAt: '2026-10-19T15:00:00Z',
    overdue: false,
    nextStages: ['BISQUE_FIRED'],
    version: 3,
    timeline: [{ to: 'RECEIVED', actorName: 'Mo Maker', occurredAt: '2026-10-19T15:00:00Z' }],
    ...overrides,
  };
}
