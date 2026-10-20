import { configureApiClient, type Piece, type Studio } from '@studiomanager/api-client';
import { createMemoryHistory } from '@tanstack/react-router';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';

import { App } from '../App';
import { type Auth, AuthContext } from '../auth/auth';
import { createQueryClient } from '../queryClient';
import { createAppRouter } from '../router';
import { API } from './server';

export function fakeAuth(overrides: Partial<Auth> = {}): Auth {
  return {
    isAuthenticated: true,
    user: { name: 'Pat Potter', username: 'pat@example.com' },
    signIn: vi.fn(() => Promise.resolve()),
    signOut: vi.fn(() => Promise.resolve()),
    getAccessToken: vi.fn(() => Promise.resolve('test-token')),
    ...overrides,
  };
}

export const signedOut = (): Auth =>
  fakeAuth({ isAuthenticated: false, user: null, getAccessToken: vi.fn(() => Promise.resolve(undefined)) });

export function aStudio(overrides: Partial<Studio> = {}): Studio {
  return {
    id: '0b7f3f0e-6f1c-4c7a-9d55-1d2f2b8b4a10',
    name: 'Clay Collective',
    unitSystem: 'METRIC',
    timeZone: 'America/Chicago',
    country: 'US',
    currency: 'USD',
    myRole: 'OWNER',
    memberCount: 12,
    createdAt: '2026-10-19T12:00:00Z',
    ...overrides,
  };
}

export function aPiece(overrides: Partial<Piece> = {}): Piece {
  return {
    id: '7c1d4a52-90b3-4f0e-8a51-2e7f5c6d9b01',
    number: 42,
    makerAccountId: 'a1',
    makerName: 'Mo Maker',
    description: 'Lidded jar',
    size: { dimensions: { length: '12', width: '10', height: '8', unit: 'INCH' } },
    stage: 'RECEIVED',
    fee: {
      amount: { amount: '48.00', currency: 'USD' },
      scheduleVersion: 1,
      taxCategory: 'FIRING_SERVICE',
      breakdown: '12 × 10 × 8 in → 960 in³ × 0.05 = 48.00',
    },
    receivedAt: '2026-10-19T15:00:00Z',
    overdue: false,
    nextStages: ['BISQUE_FIRED', 'GLAZED'],
    version: 0,
    timeline: [{ to: 'RECEIVED', actorAccountId: 'a1', actorName: 'Mo Maker', occurredAt: '2026-10-19T15:00:00Z' }],
    ...overrides,
  };
}

/** Renders the real app (routes, query client, API client) at `path`, with a fake signed-in user by default. */
export function renderApp(path: string, auth: Auth = fakeAuth()) {
  configureApiClient({ baseUrl: API, getAccessToken: () => auth.getAccessToken() });
  const queryClient = createQueryClient();
  const router = createAppRouter(queryClient, createMemoryHistory({ initialEntries: [path] }));
  const user = userEvent.setup();

  render(
    <AuthContext value={auth}>
      <App router={router} queryClient={queryClient} />
    </AuthContext>,
  );
  return { auth, router, user };
}
