import { configureApiClient, type Studio } from '@studiomanager/api-client';
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
