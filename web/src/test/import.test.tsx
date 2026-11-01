import type { ImportMembersRequest, ImportResult } from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, server } from './server';

const studio = aStudio();

describe('import from a spreadsheet', () => {
  it('guesses the columns, checks the file, then imports it', async () => {
    const requests: ImportMembersRequest[] = [];
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.post(`${API}/api/v1/studios/:studioId/imports/members`, async ({ request }) => {
        const body = (await request.json()) as ImportMembersRequest;
        requests.push(body);
        const result: ImportResult = {
          committed: !body.dryRun,
          rows: [
            { line: 2, outcome: 'CREATED', message: 'Invite emailed.' },
            { line: 3, outcome: 'SKIPPED', message: 'Already a member.' },
          ],
        };
        return HttpResponse.json(result);
      }),
    );

    const { user } = renderApp(`/studios/${studio.id}/import`);
    const file = new File(['Name,Email Address,Balance Due\nMo,mo@example.test,$12.50\nNia,nia@example.test,\n'], 'members.csv', {
      type: 'text/csv',
    });
    await user.upload(await screen.findByLabelText('CSV file'), file);

    const columns = await screen.findByRole('region', { name: 'Match the columns' });
    expect(within(columns).getByLabelText('Email')).toHaveDisplayValue('Email Address');
    expect(within(columns).getByLabelText('Balance owed (optional)')).toHaveDisplayValue('Balance Due');
    expect(within(columns).getByRole('button', { name: 'Import 2 rows' })).toBeDisabled();

    await user.click(within(columns).getByRole('button', { name: 'Check the file' }));
    const result = await screen.findByRole('region', { name: 'Result' });
    expect(within(result).getByText('Already a member.')).toBeInTheDocument();
    expect(requests[0]).toEqual({
      dryRun: true,
      rows: [
        { line: 2, email: 'mo@example.test', balance: '$12.50' },
        { line: 3, email: 'nia@example.test' },
      ],
    });

    await user.click(within(columns).getByRole('button', { name: 'Import 2 rows' }));
    expect(await screen.findByRole('heading', { name: 'Imported' })).toBeInTheDocument();
    expect(requests[1]?.dryRun).toBe(false);
  });

  it('holds back the import while any row needs fixing', async () => {
    server.use(
      http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
      http.post(`${API}/api/v1/studios/:studioId/imports/members`, () =>
        HttpResponse.json({
          committed: false,
          rows: [{ line: 2, outcome: 'ERROR', message: "'mo at home' isn't an email address." }],
        } satisfies ImportResult),
      ),
    );

    const { user } = renderApp(`/studios/${studio.id}/import`);
    await user.upload(
      await screen.findByLabelText('CSV file'),
      new File(['email\nmo at home\n'], 'members.csv', { type: 'text/csv' }),
    );
    await user.click(await screen.findByRole('button', { name: 'Check the file' }));

    expect(await screen.findByText(/1 row needs fixing/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Import 1 row' })).toBeDisabled();
  });
});
