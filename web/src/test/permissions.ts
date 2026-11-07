import type { MyPermissions, PermissionKey, Role, StudioRole } from '@studiomanager/api-client';

/**
 * The default grants of the keys the app checks, as the API's `Permission.defaultsFor` gives them
 * (plan 21 §4). Tests serve these for the role their studio fixture names, so each test keeps
 * saying only "a member" or "staff".
 */
const staffDefaults: PermissionKey[] = [
  'activity.read',
  'glazes.studio.edit',
  'imports.run',
  'kilns.manage',
  'maintenance.manage',
  'members.invite',
  'members.read',
  'members.role.assign',
  'money.payables.manage',
  'money.read',
  'notices.send',
  'pieces.intake_for_others',
  'pieces.move',
  'proposals.review',
  'schedule.manage',
  'studio.settings.edit',
  'supplies.manage',
  'today.read',
  'webhooks.manage',
];

const ownerOnly: PermissionKey[] = [
  'owners.manage',
  'studio.ai_key.manage',
  'studio.export',
  'studio.roles.manage',
  'supplies.purchase.approve',
];

const roleNames: Record<StudioRole, string> = { OWNER: 'Owner', STAFF: 'Staff', MEMBER: 'Member' };

function keysFor(role: StudioRole): PermissionKey[] {
  switch (role) {
    case 'OWNER':
      return [...staffDefaults, ...ownerOnly];
    case 'STAFF':
      return staffDefaults;
    case 'MEMBER':
      return [];
  }
}

/** The built-in roles as the roles API lists them, for pickers. */
export const systemRoles: Role[] = (['OWNER', 'STAFF', 'MEMBER'] as const).map((role) => ({
  id: `r-${role.toLowerCase()}`,
  key: role.toLowerCase(),
  role,
  name: roleNames[role],
  permissions: keysFor(role),
  editable: role !== 'OWNER',
  custom: false,
  memberCount: 0,
}));

/** The roles someone in `role` may give: those holding nothing more than they do. */
export function assignableFor(role: StudioRole): Role[] {
  const held = new Set<string>(keysFor(role));
  return systemRoles.filter((candidate) => candidate.permissions.every((key) => held.has(key)));
}

export function permissionsFor(role: StudioRole): MyPermissions {
  return { role, roleName: roleNames[role], permissions: keysFor(role), duties: [] };
}
