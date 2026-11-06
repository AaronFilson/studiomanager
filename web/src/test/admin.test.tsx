import type { RateLimitOverview, SettingChange } from '@studiomanager/api-client';
import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { renderApp } from './renderApp';
import { API, server } from './server';

const defaults = {
  mcp: { burst: 30, refillTokens: 1, refillPeriodSeconds: 10, dailyCap: 500 },
  token: { burst: 200, refillTokens: 2, refillPeriodSeconds: 1, dailyCap: 5000 },
};

function overview(overrides: Partial<RateLimitOverview> = {}): RateLimitOverview {
  return { now: '2026-11-06T15:07:00Z', defaults, normal: defaults, ...overrides };
}

const demoBoost = {
  startsAt: '2026-11-06T15:00:00Z',
  endsAt: '2026-11-06T16:30:00Z',
  unlimitedMinutes: 30,
  easingMinutes: 60,
  steps: [
    { kind: 'UNLIMITED' as const, startsAt: '2026-11-06T15:00:00Z', endsAt: '2026-11-06T15:30:00Z' },
    { kind: 'EASING' as const, startsAt: '2026-11-06T15:30:00Z', endsAt: '2026-11-06T15:50:00Z', multiplier: 8 },
    { kind: 'EASING' as const, startsAt: '2026-11-06T15:50:00Z', endsAt: '2026-11-06T16:10:00Z', multiplier: 4 },
    { kind: 'EASING' as const, startsAt: '2026-11-06T16:10:00Z', endsAt: '2026-11-06T16:30:00Z', multiplier: 2 },
  ],
};

function asAdmin(platformAdmin = true) {
  return http.get(`${API}/api/v1/me`, () => HttpResponse.json({ account: { id: 'a1' }, memberships: [], platformAdmin }));
}

describe('admin', () => {
  it('shows the Admin link only to platform admins', async () => {
    server.use(asAdmin(false));
    renderApp('/studios');
    expect(await screen.findByText(/not in any studios yet/)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Admin' })).not.toBeInTheDocument();
  });

  it('turns away someone without the role', async () => {
    server.use(asAdmin(false));
    renderApp('/admin');
    expect(await screen.findByText(/for the people who run Studio Manager/)).toBeInTheDocument();
  });

  it('starts the demo boost and says what applies now', async () => {
    let sent: unknown;
    let current = overview();
    const changes: SettingChange[] = [];
    server.use(
      asAdmin(),
      http.get(`${API}/api/v1/admin/rate-limits`, () => HttpResponse.json(current)),
      http.get(`${API}/api/v1/admin/setting-changes`, () => HttpResponse.json(changes)),
      http.put(`${API}/api/v1/admin/rate-limits/boost`, async ({ request }) => {
        sent = await request.json();
        current = overview({ boost: demoBoost });
        changes.push({
          key: 'rate-limit.boost',
          version: 1,
          newValue: { unlimitedMinutes: 30, easingMinutes: 60 },
          changedById: 'a1',
          changedByName: 'Ada Admin',
          changedAt: '2026-11-06T15:00:00Z',
        });
        return HttpResponse.json(current);
      }),
    );

    const { user } = renderApp('/admin');
    expect(await screen.findByRole('link', { name: 'Admin' })).toBeInTheDocument();
    expect(await screen.findByText('Normal limits apply.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /^Demo: 30 minutes unlimited/ }));
    await user.click(screen.getByRole('button', { name: 'Start boost' }));

    expect(await screen.findByText(/Unlimited in every studio for 23 more minutes, then easing to normal by/)).toBeInTheDocument();
    expect(sent).toEqual({ unlimitedMinutes: 30, easingMinutes: 60 });
    expect(await screen.findByText('Ada Admin')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'End now' })).toBeInTheDocument();
  });

  it('ends a boost now', async () => {
    let ended = false;
    server.use(
      asAdmin(),
      http.get(`${API}/api/v1/admin/rate-limits`, () => HttpResponse.json(overview({ boost: demoBoost }))),
      http.get(`${API}/api/v1/admin/setting-changes`, () => HttpResponse.json([])),
      http.delete(`${API}/api/v1/admin/rate-limits/boost`, () => {
        ended = true;
        return HttpResponse.json(overview());
      }),
    );

    const { user } = renderApp('/admin');
    await user.click(await screen.findByRole('button', { name: 'End now' }));

    expect(await screen.findByText('Normal limits apply.')).toBeInTheDocument();
    expect(ended).toBe(true);
    expect(screen.queryByRole('button', { name: 'End now' })).not.toBeInTheDocument();
  });

  it('saves new normal limits and goes back to the defaults', async () => {
    let sent: unknown;
    let current = overview();
    server.use(
      asAdmin(),
      http.get(`${API}/api/v1/admin/rate-limits`, () => HttpResponse.json(current)),
      http.get(`${API}/api/v1/admin/setting-changes`, () => HttpResponse.json([])),
      http.put(`${API}/api/v1/admin/rate-limits/normal`, async ({ request }) => {
        sent = await request.json();
        const saved = sent as RateLimitOverview['normal'];
        current = overview({ normal: saved, override: saved });
        return HttpResponse.json(current);
      }),
      http.delete(`${API}/api/v1/admin/rate-limits/normal`, () => {
        current = overview();
        return HttpResponse.json(current);
      }),
    );

    const { user } = renderApp('/admin');
    const burst = await screen.findByLabelText('MCP tool calls: Burst');
    await user.clear(burst);
    await user.type(burst, '60');
    await user.click(screen.getByRole('button', { name: 'Save limits' }));

    expect(await screen.findByText(/overriding the configured defaults/)).toBeInTheDocument();
    expect(sent).toEqual({ ...defaults, mcp: { ...defaults.mcp, burst: 60 } });
    await user.click(screen.getByRole('button', { name: 'Back to defaults' }));
    expect(await screen.findByText('The configured defaults.')).toBeInTheDocument();
    expect(screen.getByLabelText('MCP tool calls: Burst')).toHaveValue(30);
  });
});
