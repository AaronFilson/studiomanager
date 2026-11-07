import {
  type AccessChange,
  createCustomRoleMutation,
  deleteCustomRoleMutation,
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
  updateCustomRoleMutation,
  useCan,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { type SubmitEvent, useId, useState } from 'react';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { dutyLabel, formatDateTime, roleLabel } from '../../../labels';

/**
 * The owners' roles page: what each role may do, custom roles such as "Kiln tech", and every change
 * to who may do what (plan 21, R6 and R7).
 */
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
              key={`${role.id}:${role.name}:${role.permissions.join(',')}`}
              studioId={studioId}
              role={role}
              areas={catalogue.data.areas}
            />
          ) : (
            <OwnerCard key={role.id} role={role} />
          ),
        )}
      {catalogue.isSuccess && <NewRoleForm studioId={studioId} roles={catalogue.data.roles} />}
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

function useRefreshRoles(studioId: string) {
  const queryClient = useQueryClient();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: listRolesQueryKey({ path: { studioId } }) }),
      queryClient.invalidateQueries({ queryKey: listAccessChangesQueryKey({ path: { studioId } }) }),
      queryClient.invalidateQueries({ queryKey: getMyPermissionsQueryKey({ path: { studioId } }) }),
    ]);
}

/** A built-in STAFF or MEMBER role, or a custom role, which may also be renamed and deleted. */
function RoleForm({ studioId, role, areas }: { studioId: string; role: Role; areas: PermissionArea[] }) {
  const id = useId();
  const refresh = useRefreshRoles(studioId);
  const [chosen, setChosen] = useState(() => new Set(role.permissions));
  const [name, setName] = useState(role.name);
  const save = useMutation({ ...setRolePermissionsMutation(), onSuccess: refresh });
  const update = useMutation({ ...updateCustomRoleMutation(), onSuccess: refresh });
  const reset = useMutation({ ...resetRolePermissionsMutation(), onSuccess: refresh });
  const remove = useMutation({ ...deleteCustomRoleMutation(), onSuccess: refresh });

  const labels = labelsByKey(areas);
  const lost = role.permissions.filter((key) => !chosen.has(key));
  const gained = [...chosen].filter((key) => !role.permissions.includes(key));
  const renamed = role.custom && name.trim() !== role.name;
  const changed = lost.length > 0 || gained.length > 0 || renamed;
  const saving = save.isPending || update.isPending;

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
    const permissions = [...chosen].sort();
    if (role.custom) {
      update.mutate({ path: { studioId, roleId: role.id }, body: { name: name.trim(), permissions } });
    } else {
      save.mutate({ path: { studioId, role: role.role }, body: { permissions } });
    }
  }

  return (
    <form className="card stack" aria-labelledby={`${id}-title`} onSubmit={submit}>
      <h2 id={`${id}-title`}>{role.name}</h2>
      <p className="badge">
        {peopleText(role.memberCount)} with this role.
        {role.custom && ` A custom role; the rest of the system treats its people as ${roleLabel(role.role)}.`}
      </p>
      {role.custom && (
        <div className="field">
          <label htmlFor={`${id}-name`}>Name</label>
          <input id={`${id}-name`} required maxLength={60} value={name} onChange={(e) => setName(e.target.value)} />
        </div>
      )}
      <PermissionChoices areas={areas} chosen={chosen} onToggle={toggle} />
      {changed && lost.length > 0 && role.memberCount > 0 && (
        <p role="status" className="error">
          {peopleText(role.memberCount)} with this role lose: {lost.map((key) => labels.get(key) ?? key).join(', ')}.
        </p>
      )}
      {save.isError && <ErrorMessage error={save.error} />}
      {update.isError && <ErrorMessage error={update.error} />}
      {reset.isError && <ErrorMessage error={reset.error} />}
      {remove.isError && <ErrorMessage error={remove.error} />}
      {(save.isSuccess || update.isSuccess) && !changed && <p aria-live="polite">Saved. Changes apply within a minute.</p>}
      <div className="row">
        <button type="submit" className="primary" disabled={!changed || saving}>
          Save {role.name}
        </button>
        {!role.custom && (
          <button
            type="button"
            disabled={reset.isPending}
            onClick={() => reset.mutate({ path: { studioId, role: role.role } })}
          >
            Reset {role.name} to defaults
          </button>
        )}
        {role.custom && (
          <button
            type="button"
            disabled={remove.isPending || role.memberCount > 0}
            onClick={() => remove.mutate({ path: { studioId, roleId: role.id } })}
          >
            Delete {role.name}
          </button>
        )}
      </div>
      {role.custom && role.memberCount > 0 && (
        <p className="badge">To delete this role, give its {peopleText(role.memberCount)} another role first.</p>
      )}
    </form>
  );
}

function PermissionChoices({
  areas,
  chosen,
  onToggle,
}: {
  areas: PermissionArea[];
  chosen: Set<string>;
  onToggle: (key: string, on: boolean) => void;
}) {
  return areas.map((area) => (
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
              onChange={(event) => onToggle(permission.key, event.target.checked)}
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
  ));
}

/**
 * Make a custom role: a name and the built-in role it sits at, starting from what that role holds
 * now. Its permissions are then edited in its own form.
 */
function NewRoleForm({ studioId, roles }: { studioId: string; roles: Role[] }) {
  const id = useId();
  const refresh = useRefreshRoles(studioId);
  const [name, setName] = useState('');
  const [base, setBase] = useState<StudioRole>('MEMBER');
  const create = useMutation({
    ...createCustomRoleMutation(),
    onSuccess: async () => {
      setName('');
      await refresh();
    },
  });
  const startingFrom = roles.find((role) => !role.custom && role.role === base)?.permissions ?? [];

  return (
    <form
      className="card stack"
      aria-labelledby={`${id}-title`}
      onSubmit={(event) => {
        event.preventDefault();
        create.mutate({ path: { studioId }, body: { name: name.trim(), base, permissions: startingFrom } });
      }}
    >
      <h2 id={`${id}-title`}>Make a role</h2>
      <p className="badge">
        For a job such as a kiln tech or the board. It starts with what its built-in role may do; change that below
        once it&apos;s made. Owner-only permissions stay with owners.
      </p>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-name`}>Role name</label>
          <input id={`${id}-name`} required maxLength={60} value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-base`}>Treated as</label>
          <select id={`${id}-base`} value={base} onChange={(e) => setBase(e.target.value as StudioRole)}>
            <option value="MEMBER">{roleLabel('MEMBER')}</option>
            <option value="STAFF">{roleLabel('STAFF')}</option>
          </select>
        </div>
      </div>
      {create.isError && <ErrorMessage error={create.error} />}
      <div>
        <button type="submit" className="primary" disabled={create.isPending}>
          Make role
        </button>
      </div>
    </form>
  );
}

function History({ studioId, areas, roles }: { studioId: string; areas: PermissionArea[]; roles: Role[] }) {
  const id = useId();
  const history = useQuery(listAccessChangesOptions({ path: { studioId } }));
  const labels = labelsByKey(areas);
  const roleNames = new Map(roles.map((role) => [role.key, role.name]));

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

const builtInRoles: readonly string[] = ['OWNER', 'STAFF', 'MEMBER'];

/** A role as the log names it: a built-in role by name, a custom role by key. */
function loggedRole(value: string, roleNames: Map<string, string>): string {
  return builtInRoles.includes(value) ? roleLabel(value as StudioRole) : (roleNames.get(value) ?? value);
}

function describeChange(change: AccessChange, labels: Map<string, string>, roleNames: Map<string, string>): string {
  const named = (keys: string[]) => keys.map((key) => labels.get(key) ?? key).join(', ');
  const parts = (gave: string, tookAway: string) =>
    [change.added.length > 0 && `${gave} ${named(change.added)}`, change.removed.length > 0 && `${tookAway} ${named(change.removed)}`]
      .filter(Boolean)
      .join('; ');
  const member = change.targetName ?? 'a former member';
  const role = roleNames.get(change.target) ?? change.target;
  switch (change.kind) {
    case 'ROLE_PERMISSIONS':
      return `changed ${role}: ${parts('gave', 'took away')}`;
    case 'MEMBER_DUTIES':
      return `changed ${member}'s duties: ${[
        change.added.length > 0 && `added ${change.added.map((duty) => dutyLabel(duty as Duty)).join(', ')}`,
        change.removed.length > 0 && `removed ${change.removed.map((duty) => dutyLabel(duty as Duty)).join(', ')}`,
      ]
        .filter(Boolean)
        .join('; ')}`;
    case 'MEMBER_ROLE':
      return `made ${member} ${change.added.map((value) => loggedRole(value, roleNames)).join(', ')}`;
    case 'BILLING_CONTACT':
      return 'changed the billing contact';
    case 'ROLE_CREATED':
      return `made the role ${change.added.join(', ')}`;
    case 'ROLE_RENAMED':
      return `renamed the role ${change.removed.join(', ')} to ${change.added.join(', ')}`;
    case 'ROLE_DELETED':
      return `deleted the role ${change.removed.join(', ')}`;
    case 'STUDIO_EXPORTED':
      return "downloaded the studio's data";
  }
}

function labelsByKey(areas: PermissionArea[]): Map<string, string> {
  return new Map(areas.flatMap((area) => area.permissions.map((permission) => [permission.key, permission.label])));
}

function peopleText(count: number): string {
  return count === 1 ? '1 person' : `${String(count)} people`;
}
