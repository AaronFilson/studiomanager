import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

describe('studio page', () => {
  it('says hello to the studio and shows its details', async () => {
    const studio = aStudio();
    let authorization: string | null = null;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, ({ request, params }) => {
        authorization = request.headers.get('Authorization');
        return params.studioId === studio.id ? HttpResponse.json(studio) : new HttpResponse(null, { status: 404 });
      }),
    );

    renderApp(`/studios/${studio.id}`);

    expect(await screen.findByRole('heading', { name: 'Hello, Clay Collective' })).toBeInTheDocument();
    expect(screen.getByText('Owner')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByText('Metric (cm, kg, °C)')).toBeInTheDocument();
    expect(screen.getByText('America/Chicago')).toBeInTheDocument();
    expect(authorization).toBe('Bearer test-token');
  });

  it('explains when the user is not a member of the studio', async () => {
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () =>
        HttpResponse.json(
          { title: 'Forbidden', status: 403, detail: 'Not a member of this studio' },
          { status: 403, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );

    renderApp(`/studios/${aStudio().id}`);

    expect(await screen.findByRole('alert')).toHaveTextContent("You don't have access to this studio");
    expect(screen.queryByRole('heading', { name: /Hello/ })).not.toBeInTheDocument();
  });

  it('treats an empty-bodied 403 the same way', async () => {
    server.use(http.get(`${API}/api/v1/studios/:studioId`, () => new HttpResponse(null, { status: 403 })));

    renderApp(`/studios/${aStudio().id}`);

    expect(await screen.findByRole('alert')).toHaveTextContent("You don't have access to this studio");
  });
});
