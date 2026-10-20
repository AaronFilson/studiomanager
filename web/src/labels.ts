import type { StudioRole, UnitSystem } from '@studiomanager/api-client';

const roleLabels: Record<StudioRole, string> = {
  OWNER: 'Owner',
  STAFF: 'Staff',
  MEMBER: 'Member',
};

const unitSystemLabels: Record<UnitSystem, string> = {
  US_CUSTOMARY: 'US customary (in, lb, °F)',
  METRIC: 'Metric (cm, kg, °C)',
};

export const roleLabel = (role: StudioRole): string => roleLabels[role];
export const unitSystemLabel = (unitSystem: UnitSystem): string => unitSystemLabels[unitSystem];
