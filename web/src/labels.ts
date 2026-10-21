import type {
  Duty,
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
