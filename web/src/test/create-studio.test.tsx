import type { CreateStudioRequest, Me } from '@studiomanager/api-client';
import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const noStudios: Me = { account: { id: 'a1', displayName: 'Pat Potter' }, memberships: [] };

describe('creating a studio', () => {
  it('posts the form, including the browser time zone and the chosen country, and opens the new studio', async () => {
    const created = aStudio({ name: 'Clay Collective', unitSystem: 'METRIC' });
    let posted: CreateStudioRequest | undefined;
    server.use(
      http.get(`${API}/api/v1/me`, () => HttpResponse.json(noStudios)),
      http.post(`${API}/api/v1/studios`, async ({ request }) => {
        posted = (await request.json()) as CreateStudioRequest;
        return HttpResponse.json(created, { status: 201, headers: { Location: `/api/v1/studios/${created.id}` } });
      }),
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(created)),
    );

    const { router, user } = renderApp('/studios');
    expect(await screen.findByText(/not in any studios yet/)).toBeInTheDocument();

    await user.type(screen.getByLabelText('Studio name'), '  Clay Collective ');
    await user.click(screen.getByLabelText('Metric (cm, kg, °C)'));
    await user.selectOptions(screen.getByLabelText('Country'), 'GB');
    await user.click(screen.getByRole('button', { name: 'Create studio' }));

    expect(await screen.findByRole('heading', { name: 'Hello, Clay Collective' })).toBeInTheDocument();
    expect(posted).toEqual({
      name: 'Clay Collective',
      unitSystem: 'METRIC',
      timeZone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      country: 'GB',
    });
    await waitFor(() => expect(router.state.location.pathname).toBe(`/studios/${created.id}`));
  });

  it('shows the server explanation when the studio is rejected', async () => {
    server.use(
      http.get(`${API}/api/v1/me`, () => HttpResponse.json(noStudios)),
      http.post(`${API}/api/v1/studios`, () =>
        HttpResponse.json(
          { title: 'Bad Request', status: 400, detail: 'timeZone must be an IANA time zone ID' },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );

    const { router, user } = renderApp('/studios');
    const timeZone = await screen.findByLabelText('Time zone');

    await user.type(screen.getByLabelText('Studio name'), 'Clay Collective');
    await user.clear(timeZone);
    await user.type(timeZone, 'Mars/Olympus_Mons');
    await user.click(screen.getByRole('button', { name: 'Create studio' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('timeZone must be an IANA time zone ID');
    expect(router.state.location.pathname).toBe('/studios');
  });
});
