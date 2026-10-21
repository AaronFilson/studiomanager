import type { FeeSchedule, Studio } from '@studiomanager/api-client';
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const noFees: FeeSchedule = { version: 0, ruleType: 'FLAT', rate: '0', rounding: 'NONE', minimum: '0', taxCategory: 'FIRING_SERVICE' };

function settingsHandlers(studio: Studio) {
  return [
    http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
    http.get(`${API}/api/v1/studios/:studioId/fee-schedule`, () => HttpResponse.json(noFees)),
    http.get(`${API}/api/v1/studios/:studioId/piece-policy`, () => HttpResponse.json({ holdDays: 30 })),
    http.get(`${API}/api/v1/studios/:studioId/tax-rates`, () => HttpResponse.json([])),
  ];
}

describe('settings', () => {
  it('lets the owner download all of the studio data', async () => {
    const studio = aStudio();
    server.use(
      ...settingsHandlers(studio),
      http.get(`${API}/api/v1/studios/:studioId/export`, () =>
        HttpResponse.arrayBuffer(new Uint8Array([0x50, 0x4b, 0x05, 0x06]).buffer, {
          headers: {
            'Content-Type': 'application/zip',
            'Content-Disposition': 'attachment; filename="studio-export-2026-10-21.zip"',
          },
        }),
      ),
    );
    // jsdom has no object URLs, and can't follow a download link
    URL.createObjectURL = vi.fn(() => 'blob:export');
    const revoke = vi.fn();
    URL.revokeObjectURL = revoke;
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    const { user } = renderApp(`/studios/${studio.id}/settings`);
    await user.click(await screen.findByRole('button', { name: 'Download all data' }));

    await vi.waitFor(() => expect(click).toHaveBeenCalledOnce());
    const link = click.mock.contexts[0] as HTMLAnchorElement;
    expect(link.download).toBe('studio-export-2026-10-21.zip');
    expect(link.href).toBe('blob:export');
    expect(revoke).toHaveBeenCalledWith('blob:export');
  });

  it('offers the download to the owner only', async () => {
    const studio = aStudio({ myRole: 'STAFF' });
    server.use(...settingsHandlers(studio));

    renderApp(`/studios/${studio.id}/settings`);

    expect(await screen.findByRole('heading', { name: 'Firing fees' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Download all data' })).not.toBeInTheDocument();
  });
});
