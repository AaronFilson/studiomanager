import { useQuery } from '@tanstack/react-query';

import { getMyPermissionsOptions } from './generated/@tanstack/react-query.gen';

/**
 * The permission keys the apps check (plan 21 §4). The API sends every key the caller holds; this
 * names the ones a screen shows or hides something by, so a typo doesn't compile.
 */
export type PermissionKey =
  | 'activity.read'
  | 'glazes.studio.edit'
  | 'imports.run'
  | 'kilns.manage'
  | 'maintenance.manage'
  | 'members.read'
  | 'money.payables.manage'
  | 'money.read'
  | 'notices.send'
  | 'owners.manage'
  | 'pieces.intake_for_others'
  | 'pieces.move'
  | 'proposals.review'
  | 'schedule.manage'
  | 'studio.ai_key.manage'
  | 'studio.export'
  | 'studio.settings.edit'
  | 'supplies.manage'
  | 'supplies.purchase.approve'
  | 'today.read'
  | 'webhooks.manage';

/** Answers whether the caller holds a permission; `false` until their permissions have loaded. */
export type Can = (permission: PermissionKey) => boolean;

/**
 * The caller's permissions in a studio, from `GET /studios/{studioId}/me/permissions`, cached like
 * the studio itself and refetched when the window regains focus.
 *
 * Hiding something the caller can't use is convenience only; the API checks every request.
 */
export function useCan(studioId: string): { can: Can; isPending: boolean; isError: boolean; error: unknown } {
  const permissions = useQuery(getMyPermissionsOptions({ path: { studioId } }));
  const held = new Set(permissions.data?.permissions ?? []);
  return {
    can: (permission) => held.has(permission),
    isPending: permissions.isPending,
    isError: permissions.isError,
    error: permissions.error,
  };
}
