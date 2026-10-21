import type { ProblemReport, ReportProblemRequest, ResolveProblemRequest } from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const report: ProblemReport = {
  id: 'p1',
  assetId: 'a1',
  assetName: 'Wheel 3',
  description: 'Grinds when it speeds up',
  reportedBy: 'm1',
  reportedByName: 'Mo Maker',
  reportedAt: '2026-10-21T15:00:00Z',
  open: true,
  version: 0,
};

describe('problem reports', () => {
  it('lets a member report a problem from the studio’s home page', async () => {
    const studio = aStudio({ myRole: 'MEMBER' });
    let sent: ReportProblemRequest | undefined;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/maintenance/reportable-assets`, () =>
        HttpResponse.json([{ id: 'a1', kind: 'WHEEL', name: 'Wheel 3', available: true }]),
      ),
      http.post(`${API}/api/v1/studios/:studioId/maintenance/problem-reports`, async ({ request }) => {
        sent = (await request.json()) as ReportProblemRequest;
        return HttpResponse.json(report, { status: 201 });
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}`);
    await user.click(await screen.findByRole('button', { name: 'Report a problem' }));
    await user.selectOptions(await screen.findByLabelText('With'), await screen.findByRole('option', { name: 'Wheel 3' }));
    await user.type(screen.getByLabelText('What’s wrong'), 'Grinds when it speeds up');
    await user.click(screen.getByRole('button', { name: 'Send report' }));

    await expect.poll(() => sent).toEqual({ assetId: 'a1', description: 'Grinds when it speeds up' });
    expect(await screen.findByText('Thanks, the staff have been told.')).toBeInTheDocument();
  });

  it('shows staff the open reports and resolves one', async () => {
    const studio = aStudio();
    let resolved: ResolveProblemRequest | undefined;
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.get(`${API}/api/v1/studios/:studioId/maintenance/assets`, () => HttpResponse.json([])),
      http.get(`${API}/api/v1/studios/:studioId/maintenance/problem-reports`, () => HttpResponse.json([report])),
      http.post(`${API}/api/v1/studios/:studioId/maintenance/problem-reports/:reportId/resolution`, async ({ request }) => {
        resolved = (await request.json()) as ResolveProblemRequest;
        return HttpResponse.json({ ...report, open: false, version: 1 });
      }),
    );
    vi.spyOn(window, 'prompt').mockReturnValue('New belt');

    const { user } = renderApp(`/studios/${studio.id}/maintenance`);
    const problems = await screen.findByRole('region', { name: 'Reported problems' });
    expect(within(problems).getByText(/Grinds when it speeds up/)).toBeInTheDocument();
    await user.click(within(problems).getByRole('button', { name: 'Resolve' }));

    await expect.poll(() => resolved).toEqual({ expectedVersion: 0, resolution: 'New belt' });
  });
});
