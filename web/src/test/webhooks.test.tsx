import type { WebhookDelivery, WebhookEndpoint } from '@studiomanager/api-client';
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const endpoint: WebhookEndpoint = {
  id: 'e1',
  url: 'https://hooks.zapier.com/hooks/catch/1/abc',
  description: 'Zapier: ready email',
  eventTypes: ['pieces.ready'],
  enabled: true,
  consecutiveFailures: 0,
  createdAt: '2026-11-02T12:00:00Z',
};

const delivered: WebhookDelivery = {
  id: 'd1',
  eventId: 'evt_1',
  eventType: 'ping',
  status: 'DELIVERED',
  attempts: 1,
  createdAt: '2026-11-02T12:01:00Z',
  attemptLog: [{ attemptedAt: '2026-11-02T12:01:00Z', statusCode: 200, durationMs: 120 }],
};

describe('webhooks', () => {
  it('adds an endpoint and shows its secret once', async () => {
    const studio = aStudio();
    let sent: unknown;
    let endpoints: WebhookEndpoint[] = [];
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/webhooks/endpoints`, () => HttpResponse.json(endpoints)),
      http.get(`${API}/api/v1/studios/:studioId/webhooks/event-types`, () => HttpResponse.json([])),
      http.post(`${API}/api/v1/studios/:studioId/webhooks/endpoints`, async ({ request }) => {
        sent = await request.json();
        endpoints = [endpoint];
        return HttpResponse.json({ endpoint, secret: 'whsec_c2VjcmV0' }, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/webhooks`);
    await user.type(await screen.findByLabelText('URL'), endpoint.url);
    await user.type(screen.getByLabelText('Description (optional)'), 'Zapier: ready email');
    await user.click(screen.getByLabelText('Member joined'));
    await user.click(screen.getByRole('button', { name: 'Add endpoint' }));

    expect(await screen.findByText('whsec_c2VjcmV0')).toBeInTheDocument();
    expect(sent).toEqual({
      url: endpoint.url,
      description: 'Zapier: ready email',
      eventTypes: ['pieces.ready', 'member.joined'],
    });
    expect(await screen.findByRole('heading', { name: 'Zapier: ready email' })).toBeInTheDocument();
  });

  it('sends a test and says how it went', async () => {
    const studio = aStudio({ myRole: 'STAFF' });
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/webhooks/endpoints`, () => HttpResponse.json([endpoint])),
      http.get(`${API}/api/v1/studios/:studioId/webhooks/event-types`, () => HttpResponse.json([])),
      http.post(`${API}/api/v1/studios/:studioId/webhooks/endpoints/:endpointId/test`, () => HttpResponse.json(delivered)),
    );

    const { user } = renderApp(`/studios/${studio.id}/webhooks`);
    await user.click(await screen.findByRole('button', { name: 'Send test' }));

    expect(await screen.findByText('Test: Delivered (HTTP 200)')).toBeInTheDocument();
  });

  it('explains why an endpoint was turned off', async () => {
    const studio = aStudio();
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/webhooks/endpoints`, () =>
        HttpResponse.json([{ ...endpoint, enabled: false, disabledReason: 'It answered 410 Gone.' }]),
      ),
      http.get(`${API}/api/v1/studios/:studioId/webhooks/event-types`, () => HttpResponse.json([])),
    );

    renderApp(`/studios/${studio.id}/webhooks`);

    expect(await screen.findByRole('alert')).toHaveTextContent('Turned off: It answered 410 Gone.');
    expect(screen.getByRole('button', { name: 'Send test' })).toBeDisabled();
  });
});
