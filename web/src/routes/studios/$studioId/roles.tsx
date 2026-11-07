import {
  type AccessChange,
  type Duty,
  getMyPermissionsQueryKey,
  listAccessChangesOptions,
  listAccessChangesQueryKey,
  listRolesOptions,
  listRolesQueryKey,
  type PermissionArea,
  type PermissionInfo,
  resetRolePermissionsMutation,
  type Role,
  type StudioRole,
  setRolePermissionsMutation,
  useCan,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { type SubmitEvent, useId, useState } from 'react';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { dutyLabel, formatDateTime, roleLabel } from '../../../labels';

/** The owners' roles page: what each role may do, and every change to who may do what (plan 21, R6). */
export const Route = createFileRoute('/studios/$studioId/roles')({
  component: RolesPage,
});

function RolesPage() {
  const { studioId } = Route.useParams();
  const { can } = useCan(studioId);
  const allowed = can('studio.roles.manage');
  const catalogue = useQuery({ ...listRolesOptions({ path: { studioId } }), enabled: allowed });

  if (!allowed) {
    return (
      <div className="stack">
        <h1>Roles</h1>
        <p role="alert" className="error">
          Only the studio&apos;s owners can see and change what each role may do.
        </p>
      </div>
    );
  }

  return (
    <div className="stack">
      <h1>Roles</h1>
      <p className="badge">
        Choose what each role may do. Owners always hold every permission. A person&apos;s duties can add a few more.
        Changes apply within a minute.
      </p>
      {catalogue.isPending && <p>Loading roles…</p>}
      {catalogue.isError && <ErrorMessage error={catalogue.error} />}
      {catalogue.isSuccess &&
        catalogue.data.roles.map((role) =>
          role.editable ? (
            <RoleForm
              key={`${role.role}:${role.permissions.join(',')}`}
              studioId={studioId}
              role={role}
              areas={catalogue.data.areas}
            />
          ) : (
            <OwnerCard key={role.role} role={role} />
          ),
        )}
      {catalogue.isSuccess && <History studioId={studioId} areas={catalogue.data.areas} roles={catalogue.data.roles} />}
    </div>
  );
}

function OwnerCard({ role }: { role: Role }) {
  const id = useId();
  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>{role.name}</h2>
      <p className="badge">
        {peopleText(role.memberCount)}. Owners hold every permission, including the ones only owners can have. This
        can&apos;t be changed.
      </p>
    </section>
  );
}

function lockReason(permission: PermissionInfo): string | undefined {
  switch (permission.lock) {
    case 'OWNER_ONLY':
      return 'Owners only';
    case 'FLOOR':
      return 'Everyone always has this, for their own things';
    case undefined:
      return undefined;
  }
}

function RoleForm({ studioId, role, areas }: { studioId: string; role: Role; areas: PermissionArea[] }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState(() => new Set(role.permissions));
  const refresh = () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: listRolesQueryKey({ path: { studioId } }) }),
      queryClient.invalidateQueries({ queryKey: listAccessChangesQueryKey({ path: { studioId } }) }),
      queryClient.invalidateQueries({ queryKey: getMyPermissionsQueryKey({ path: { studioId } }) }),
    ]);
  const save = useMutation({ ...setRolePermissionsMutation(), onSuccess: refresh });
  const reset = useMutation({ ...resetRolePermissionsMutation(), onSuccess: refresh });

  const labels = labelsByKey(areas);
  const lost = role.permissions.filter((key) => !chosen.has(key));
  const gained = [...chosen].filter((key) => !role.permissions.includes(key));
  const changed = lost.length > 0 || gained.length > 0;

  function toggle(key: string, on: boolean) {
    setChosen((current) => {
      const next = new Set(current);
      if (on) {
        next.add(key);
      } else {
        next.delete(key);
      }
      return next;
    });
  }

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    save.mutate({ path: { studioId, role: role.role }, body: { permissions: [...chosen].sort() } });
  }

  return (
    <form className="card stack" aria-labelledby={`${id}-title`} onSubmit={submit}>
      <h2 id={`${id}-title`}>{role.name}</h2>
      <p className="badge">{peopleText(role.memberCount)} with this role.</p>
      {areas.map((area) => (
        <fieldset key={area.area} className="field">
          <legend>{area.label}</legend>
          {area.permissions.map((permission) => {
            const reason = lockReason(permission);
            return (
              <label key={permission.key} className="choice">
                <input
                  type="checkbox"
                  checked={permission.lock === 'FLOOR' || (permission.lock === undefined && chosen.has(permission.key))}
                  disabled={reason !== undefined}
                  onChange={(event) => toggle(permission.key, event.target.checked)}
                />
                {permission.label}
                <span className="badge">
                  {' '}
                  {permission.description}
                  {reason && ` (${reason})`}
                </span>
              </label>
            );
          })}
        </fieldset>
      ))}
      {changed && lost.length > 0 && role.memberCount > 0 && (
        <p role="status" className="error">
          {peopleText(role.memberCount)} with this role lose: {lost.map((key) => labels.get(key) ?? key).join(', ')}.
        </p>
      )}
      {save.isError && <ErrorMessage error={save.error} />}
      {reset.isError && <ErrorMessage error={reset.error} />}
      {save.isSuccess && !changed && <p aria-live="polite">Saved. Changes apply within a minute.</p>}
      <div className="row">
        <button type="submit" className="primary" disabled={!changed || save.isPending}>
          Save {role.name}
        </button>
        <button
          type="button"
          disabled={reset.isPending}
          onClick={() => reset.mutate({ path: { studioId, role: role.role } })}
        >
          Reset {role.name} to defaults
        </button>
      </div>
    </form>
  );
}

function History({ studioId, areas, roles }: { studioId: string; areas: PermissionArea[]; roles: Role[] }) {
  const id = useId();
  const history = useQuery(listAccessChangesOptions({ path: { studioId } }));
  const labels = labelsByKey(areas);
  const roleNames = new Map(roles.map((role) => [role.role.toLowerCase(), role.name]));

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Access history</h2>
      {history.isPending && <p>Loading history…</p>}
      {history.isError && <ErrorMessage error={history.error} />}
      {history.isSuccess && history.data.items.length === 0 && <p>No changes yet.</p>}
      {history.isSuccess && history.data.items.length > 0 && (
        <ul>
          {history.data.items.map((change) => (
            <li key={change.id}>
              {formatDateTime(change.at)}: {change.actorName ?? 'A former member'}{' '}
              {describeChange(change, labels, roleNames)}
              {change.viaToken && ' (with an API token)'}
            </li>
          ))}
        </ul>
      )}
      {history.isSuccess && history.data.truncated && (
        <p className="badge">Showing the newest {history.data.limit}. The studio export has them all.</p>
      )}
    </section>
  );
}

function describeChange(change: AccessChange, labels: Map<string, string>, roleNames: Map<string, string>): string {
  const named = (keys: string[]) => keys.map((key) => labels.get(key) ?? key).join(', ');
  const parts = (gave: string, tookAway: string) =>
    [change.added.length > 0 && `${gave} ${named(change.added)}`, change.removed.length > 0 && `${tookAway} ${named(change.removed)}`]
      .filter(Boolean)
      .join('; ');
  const member = change.targetName ?? 'a former member';
  switch (change.kind) {
    case 'ROLE_PERMISSIONS':
      return `changed ${roleNames.get(change.target) ?? change.target}: ${parts('gave', 'took away')}`;
    case 'MEMBER_DUTIES':
      return `changed ${member}'s duties: ${[
        change.added.length > 0 && `added ${change.added.map((duty) => dutyLabel(duty as Duty)).join(', ')}`,
        change.removed.length > 0 && `removed ${change.removed.map((duty) => dutyLabel(duty as Duty)).join(', ')}`,
      ]
        .filter(Boolean)
        .join('; ')}`;
    case 'MEMBER_ROLE':
      return `made ${member} ${change.added.map((role) => roleLabel(role as StudioRole)).join(', ')}`;
    case 'BILLING_CONTACT':
      return 'changed the billing contact';
  }
}

function labelsByKey(areas: PermissionArea[]): Map<string, string> {
  return new Map(areas.flatMap((area) => area.permissions.map((permission) => [permission.key, permission.label])));
}

function peopleText(count: number): string {
  return count === 1 ? '1 person' : `${String(count)} people`;
}
