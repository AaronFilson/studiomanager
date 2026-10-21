import type {
  AssetKind,
  Duty,
  MaintenanceTask,
  FeeRounding,
  FeeRuleType,
  FeeUnit,
  LengthUnit,
  PieceStage,
  StudioRole,
  UnitSystem,
  WeightUnit,
} from '@studiomanager/api-client';

const roleLabels: Record<StudioRole, string> = {
  OWNER: 'Owner',
  STAFF: 'Staff',
  MEMBER: 'Member',
};

const unitSystemLabels: Record<UnitSystem, string> = {
  US_CUSTOMARY: 'US customary (in, lb, °F)',
  METRIC: 'Metric (cm, kg, °C)',
};

const stageLabels: Record<PieceStage, string> = {
  RECEIVED: 'Received',
  BISQUE_FIRED: 'Bisque fired',
  GLAZED: 'Glazed',
  READY: 'Ready for pickup',
  PICKED_UP: 'Picked up',
  UNCLAIMED: 'Unclaimed',
  DISPOSED: 'Disposed of',
};

const feeRuleTypeLabels: Record<FeeRuleType, string> = {
  PER_VOLUME: 'By volume',
  PER_WEIGHT: 'By weight',
  FLAT: 'Flat fee per piece',
};

const feeUnitLabels: Record<FeeUnit, string> = {
  CUBIC_INCH: 'per cubic inch',
  CUBIC_CENTIMETRE: 'per cubic centimetre',
  LITRE: 'per litre',
  POUND: 'per pound',
  KILOGRAM: 'per kilogram',
};

const roundingLabels: Record<FeeRounding, string> = {
  NONE: 'No rounding',
  HALF_UNIT: 'Round up to the next half unit',
  WHOLE_UNIT: 'Round up to the next whole unit',
};

const lengthUnitLabels: Record<LengthUnit, string> = { MILLIMETRE: 'mm', CENTIMETRE: 'cm', INCH: 'in' };
const weightUnitLabels: Record<WeightUnit, string> = { GRAM: 'g', KILOGRAM: 'kg', POUND: 'lb', OUNCE: 'oz' };

export const roleLabel = (role: StudioRole): string => roleLabels[role];
export const unitSystemLabel = (unitSystem: UnitSystem): string => unitSystemLabels[unitSystem];
export const stageLabel = (stage: PieceStage): string => stageLabels[stage];
export const feeRuleTypeLabel = (type: FeeRuleType): string => feeRuleTypeLabels[type];
export const feeUnitLabel = (unit: FeeUnit): string => feeUnitLabels[unit];
export const roundingLabel = (rounding: FeeRounding): string => roundingLabels[rounding];
export const lengthUnitLabel = (unit: LengthUnit): string => lengthUnitLabels[unit];
export const weightUnitLabel = (unit: WeightUnit): string => weightUnitLabels[unit];

const dutyLabels: Record<Duty, string> = {
  TECHNICIAN: 'Technician',
  MONITOR: 'Studio monitor',
  WORK_EXCHANGE: 'Work exchange',
  VOLUNTEER_OFFICER: 'Volunteer officer',
  INSTRUCTOR: 'Instructor',
};

export const dutyLabel = (duty: Duty): string => dutyLabels[duty];

export const isStaff = (role: StudioRole): boolean => role === 'OWNER' || role === 'STAFF';

const assetKindLabels: Record<AssetKind, string> = {
  KILN: 'Kiln',
  WHEEL: 'Wheel',
  PUG_MILL: 'Pug mill',
  SLAB_ROLLER: 'Slab roller',
  EXTRUDER: 'Extruder',
  CLAY_TRAP: 'Clay trap',
  SPRAY_BOOTH: 'Spray booth',
  VENTILATION: 'Ventilation',
  BUILDING: 'Building',
  SAFETY: 'Safety equipment',
  OTHER: 'Other',
};

export const assetKindLabel = (kind: AssetKind): string => assetKindLabels[kind];

/** "every 30 days or 10 firings, whichever comes first" */
export function scheduleText(task: Pick<MaintenanceTask, 'everyDays' | 'everyFirings'>): string {
  const days = task.everyDays ? `${String(task.everyDays)} ${task.everyDays === 1 ? 'day' : 'days'}` : null;
  const firings = task.everyFirings
    ? task.everyFirings === 1
      ? 'firing'
      : `${String(task.everyFirings)} firings`
    : null;
  if (days && firings) return `every ${days} or ${firings}, whichever comes first`;
  return `every ${days ?? firings ?? ''}`;
}

/** "is due", "is due soon (2 firings left)", "next due 4 Nov 2026" */
export function dueText(task: Pick<MaintenanceTask, 'status' | 'dueAt' | 'firingsLeft'>): string {
  const firings =
    task.firingsLeft !== undefined && task.firingsLeft > 0
      ? `${String(task.firingsLeft)} ${task.firingsLeft === 1 ? 'firing' : 'firings'} left`
      : null;
  const date = task.dueAt ? `by ${formatDate(task.dueAt)}` : null;
  const when = [firings, date].filter(Boolean).join(', ');
  switch (task.status) {
    case 'DUE':
      return 'is due';
    case 'DUE_SOON':
      return when ? `is due soon (${when})` : 'is due soon';
    case 'OK':
      return when ? `next due: ${when}` : '';
  }
}

export function formatMoney(amount: string, currency: string): string {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(Number(amount));
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso));
}

export function formatTime(iso: string): string {
  return new Intl.DateTimeFormat(undefined, { timeStyle: 'short' }).format(new Date(iso));
}

export function formatDate(iso: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(iso));
}
