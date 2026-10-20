import type { Me } from '@studiomanager/api-client';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp, signedOut } from './renderApp';
import { API, server } from './server';

describe('authentication', () => {
  it('sends signed-out visitors to sign in, then back to the page they asked for', async () => {
    const auth = signedOut();
    const path = `/studios/${aStudio().id}`;
    const requested: string[] = [];
    server.events.on('request:start', ({ request }) => {
      requested.push(request.url);
    });

    renderApp(path, auth);

    await waitFor(() => expect(auth.signIn).toHaveBeenCalledWith(path));
    expect(requested).toEqual([]);
  });

  it('offers sign-in on the landing page', async () => {
    const auth = signedOut();
    const { user } = renderApp('/', auth);

    await user.click(await screen.findByRole('button', { name: 'Sign in' }));

    expect(auth.signIn).toHaveBeenCalledWith('/studios');
  });

  it('takes signed-in users from the landing page to their studios', async () => {
    const me: Me = {
      account: { id: 'a1' },
      memberships: [{ studio: { id: aStudio().id, name: 'Clay Collective' }, role: 'STAFF' }],
    };
    server.use(http.get(`${API}/api/v1/me`, () => HttpResponse.json(me)));

    const { router } = renderApp('/');

    expect(await screen.findByRole('link', { name: 'Clay Collective' })).toBeInTheDocument();
    expect(screen.getByText('Staff')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/studios');
  });
});
