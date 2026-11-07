import type {
  AccessChange,
  CreateCustomRoleRequest,
  Role,
  RoleCatalogue,
  SetRolePermissionsRequest,
  UpdateCustomRoleRequest,
} from '@studiomanager/api-client';
import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { aStudio, renderApp } from './renderApp';
import { API, capped, server } from './server';

const studio = aStudio();

const catalogue: RoleCatalogue = {
  areas: [
    {
      area: 'MEMBERS',
      label: 'Members',
      permissions: [{ key: 'members.read', label: 'See members', description: 'See everyone in the studio.' }],
    },
    {
      area: 'MONEY',
      label: 'Money',
      permissions: [
        { key: 'money.own.read', label: 'See own balance', description: 'See their own balance.', lock: 'FLOOR' },
        { key: 'money.refund_and_adjust', label: 'Refund and adjust', description: 'Refund payments.' },
      ],
    },
    {
      area: 'OWNERS',
      label: 'Owners',
      permissions: [{ key: 'owners.manage', label: 'Manage owners', description: 'Add co-owners.', lock: 'OWNER_ONLY' }],
    },
  ],
  roles: [
    {
      id: 'r-owner',
      key: 'owner',
      role: 'OWNER',
      name: 'Owner',
      permissions: ['members.read', 'money.own.read', 'money.refund_and_adjust', 'owners.manage'],
      editable: false,
      custom: false,
      memberCount: 1,
    },
    {
      id: 'r-staff',
      key: 'staff',
      role: 'STAFF',
      name: 'Staff',
      permissions: ['members.read', 'money.own.read', 'money.refund_and_adjust'],
      editable: true,
      custom: false,
      memberCount: 3,
    },
    {
      id: 'r-member',
      key: 'member',
      role: 'MEMBER',
      name: 'Member',
      permissions: ['money.own.read'],
      editable: true,
      custom: false,
      memberCount: 12,
    },
  ],
};

const kilnTech: Role = {
  id: 'r-kiln',
  key: 'kiln_tech',
  role: 'MEMBER',
  name: 'Kiln tech',
  permissions: ['members.read', 'money.own.read'],
  editable: true,
  custom: true,
  memberCount: 0,
};

const history: AccessChange[] = [
  {
    id: 'c1',
    at: '2026-11-07T12:00:00Z',
    actorAccountId: 'a1',
    actorName: 'Mara',
    viaToken: false,
    kind: 'ROLE_PERMISSIONS',
    target: 'staff',
    added: [],
    removed: ['money.refund_and_adjust'],
  },
  {
    id: 'c2',
    at: '2026-11-06T12:00:00Z',
    actorAccountId: 'a1',
    actorName: 'Mara',
    viaToken: false,
    kind: 'MEMBER_DUTIES',
    target: 'a2',
    targetName: 'Jo',
    added: ['MONITOR'],
    removed: [],
  },
];

function serveRoles(calls: string[], served: RoleCatalogue = catalogue) {
  server.use(
    http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(studio)),
    http.get(`${API}/api/v1/studios/:studioId/roles`, () => HttpResponse.json(served)),
    http.post(`${API}/api/v1/studios/:studioId/roles/custom`, async ({ request }) => {
      const body = (await request.json()) as CreateCustomRoleRequest;
      calls.push(`create ${body.name} ${body.base} ${body.permissions.join(',')}`);
      return HttpResponse.json({ ...kilnTech, name: body.name }, { status: 201 });
    }),
    http.put(`${API}/api/v1/studios/:studioId/roles/custom/:roleId`, async ({ params, request }) => {
      const body = (await request.json()) as UpdateCustomRoleRequest;
      calls.push(`update ${String(params.roleId)} ${body.name} ${body.permissions.join(',')}`);
      return HttpResponse.json({ ...kilnTech, name: body.name, permissions: body.permissions });
    }),
    http.delete(`${API}/api/v1/studios/:studioId/roles/custom/:roleId`, ({ params }) => {
      calls.push(`delete ${String(params.roleId)}`);
      return new HttpResponse(null, { status: 204 });
    }),
    http.get(`${API}/api/v1/studios/:studioId/roles/history`, () => HttpResponse.json(capped(history, false, 200))),
    http.put(`${API}/api/v1/studios/:studioId/roles/:role/permissions`, async ({ params, request }) => {
      const body = (await request.json()) as SetRolePermissionsRequest;
      calls.push(`set ${String(params.role)} ${body.permissions.join(',')}`);
      return HttpResponse.json({ ...catalogue.roles[1], permissions: body.permissions });
    }),
    http.post(`${API}/api/v1/studios/:studioId/roles/:role/reset`, ({ params }) => {
      calls.push(`reset ${String(params.role)}`);
      return HttpResponse.json(catalogue.roles[1]);
    }),
  );
}

describe('roles', () => {
  it('shows each role grouped by area, with locked permissions disabled and why', async () => {
    serveRoles([]);

    renderApp(`/studios/${studio.id}/roles`);

    const staff = await screen.findByRole('form', { name: 'Staff' });
    expect(within(staff).getByRole('checkbox', { name: /See members/ })).toBeChecked();
    expect(within(staff).getByRole('checkbox', { name: /See own balance/ })).toBeDisabled();
    expect(within(staff).getByRole('checkbox', { name: /See own balance/ })).toBeChecked();
    const owners = within(staff).getByRole('checkbox', { name: /Manage owners/ });
    expect(owners).toBeDisabled();
    expect(owners).not.toBeChecked();
    expect(within(staff).getByText(/Owners only/)).toBeInTheDocument();
    expect(within(staff).getByRole('group', { name: 'Money' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: 'Owner' })).toHaveTextContent("can't be changed");
    expect(screen.queryByRole('form', { name: 'Owner' })).not.toBeInTheDocument();
  });

  it('warns who loses what before saving, then saves the new set', async () => {
    const calls: string[] = [];
    serveRoles(calls);

    const { user } = renderApp(`/studios/${studio.id}/roles`);
    const staff = await screen.findByRole('form', { name: 'Staff' });
    expect(within(staff).getByRole('button', { name: 'Save Staff' })).toBeDisabled();
    await user.click(within(staff).getByRole('checkbox', { name: /Refund and adjust/ }));

    expect(within(staff).getByRole('status')).toHaveTextContent('3 people with this role lose: Refund and adjust.');
    await user.click(within(staff).getByRole('button', { name: 'Save Staff' }));

    await expect.poll(() => calls).toEqual(['set STAFF members.read,money.own.read']);
  });

  it('resets a role to its defaults', async () => {
    const calls: string[] = [];
    serveRoles(calls);

    const { user } = renderApp(`/studios/${studio.id}/roles`);
    const member = await screen.findByRole('form', { name: 'Member' });
    await user.click(within(member).getByRole('button', { name: 'Reset Member to defaults' }));

    await expect.poll(() => calls).toEqual(['reset MEMBER']);
  });

  it('lists the access history with labels and names', async () => {
    serveRoles([]);

    renderApp(`/studios/${studio.id}/roles`);

    const log = await screen.findByRole('region', { name: 'Access history' });
    expect(await within(log).findByText(/Mara changed Staff: took away Refund and adjust/)).toBeInTheDocument();
    expect(within(log).getByText(/Mara changed Jo's duties: added/)).toBeInTheDocument();
  });

  it('makes a custom role starting from its built-in role', async () => {
    const calls: string[] = [];
    serveRoles(calls);

    const { user } = renderApp(`/studios/${studio.id}/roles`);
    const form = await screen.findByRole('form', { name: 'Make a role' });
    await user.type(within(form).getByLabelText('Role name'), 'Kiln tech');
    await user.selectOptions(within(form).getByLabelText('Treated as'), 'STAFF');
    await user.click(within(form).getByRole('button', { name: 'Make role' }));

    await expect
      .poll(() => calls)
      .toEqual(['create Kiln tech STAFF members.read,money.own.read,money.refund_and_adjust']);
  });

  it('renames, edits and deletes a custom role, with no reset', async () => {
    const calls: string[] = [];
    serveRoles(calls, { ...catalogue, roles: [...catalogue.roles, kilnTech] });

    const { user } = renderApp(`/studios/${studio.id}/roles`);
    const form = await screen.findByRole('form', { name: 'Kiln tech' });
    expect(within(form).queryByRole('button', { name: /Reset/ })).not.toBeInTheDocument();
    const name = within(form).getByLabelText('Name');
    await user.clear(name);
    await user.type(name, 'Kiln crew');
    await user.click(within(form).getByRole('checkbox', { name: /See members/ }));
    await user.click(within(form).getByRole('button', { name: 'Save Kiln tech' }));
    await user.click(within(form).getByRole('button', { name: 'Delete Kiln tech' }));

    await expect.poll(() => calls).toEqual(['update r-kiln Kiln crew money.own.read', 'delete r-kiln']);
  });

  it("won't delete a custom role people hold", async () => {
    serveRoles([], { ...catalogue, roles: [...catalogue.roles, { ...kilnTech, memberCount: 2 }] });

    renderApp(`/studios/${studio.id}/roles`);

    const form = await screen.findByRole('form', { name: 'Kiln tech' });
    expect(within(form).getByRole('button', { name: 'Delete Kiln tech' })).toBeDisabled();
    expect(within(form).getByText(/give its 2 people another role first/)).toBeInTheDocument();
  });

  it('offers the page and its link only to owners', async () => {
    serveRoles([]);
    server.use(http.get(`${API}/api/v1/studios/:studioId`, () => HttpResponse.json(aStudio({ myRole: 'STAFF' }))));

    renderApp(`/studios/${studio.id}/roles`);

    expect(await screen.findByText(/Only the studio's owners/)).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Roles' })).not.toBeInTheDocument();
    expect(screen.queryByRole('form', { name: 'Staff' })).not.toBeInTheDocument();
  });
});
