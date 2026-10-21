import type { Resource } from '@studiomanager/api-client';
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const roller: Resource = {
  id: 'r1',
  name: 'Slab roller',
  kind: 'EQUIPMENT',
  capacity: 1,
  assetId: 'a1',
  downReason: 'Cable snapped',
  version: 0,
};

describe('calendar', () => {
  it('tells a member when the equipment they picked is out of service', async () => {
    const studio = aStudio({ myRole: 'MEMBER' });
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/bookings`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/resources`, () => HttpResponse.json([roller])),
    );

    renderApp(`/studios/${studio.id}/calendar`);

    expect(await screen.findByRole('option', { name: 'Slab roller (out of service)' })).toBeInTheDocument();
    expect(screen.getByText(/Slab roller is out of service: Cable snapped\. It can’t be reserved/)).toBeInTheDocument();
  });
});
