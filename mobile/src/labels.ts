import type {
  AssetKind,
  Atmosphere,
  BookingKind,
  Duty,
  GlazeAmountUnit,
  KilnType,
  LengthUnit,
  MaintenanceTask,
  PieceStage,
  Provider,
  StoredValueKind,
  StudioRole,
  SupplyCategory,
  UnitSystem,
  WeightUnit,
} from '@studiomanager/api-client';

// The same wording as the web app (web/src/labels.ts), so members see one vocabulary
const stageLabels: Record<PieceStage, string> = {
  RECEIVED: 'Received',
  BISQUE_FIRED: 'Bisque fired',
  GLAZED: 'Glazed',
  READY: 'Ready for pickup',
  PICKED_UP: 'Picked up',
  UNCLAIMED: 'Unclaimed',
  DISPOSED: 'Disposed of',
};

const roleLabels: Record<StudioRole, string> = {
  OWNER: 'Owner',
  STAFF: 'Staff',
  MEMBER: 'Member',
};

export const allDuties: Duty[] = ['TECHNICIAN', 'MONITOR', 'WORK_EXCHANGE', 'VOLUNTEER_OFFICER', 'INSTRUCTOR'];

const dutyLabels: Record<Duty, string> = {
  TECHNICIAN: 'Technician',
  MONITOR: 'Studio monitor',
  WORK_EXCHANGE: 'Work exchange',
  VOLUNTEER_OFFICER: 'Volunteer officer',
  INSTRUCTOR: 'Instructor',
};

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

const kilnTypeLabels: Record<KilnType, string> = {
  ELECTRIC: 'Electric',
  GAS: 'Gas',
  WOOD: 'Wood',
  RAKU: 'Raku',
  OTHER: 'Other',
};

export const supplyCategories: SupplyCategory[] = [
  'CLAY',
  'GLAZE',
  'GLAZE_MATERIAL',
  'TOOL',
  'KILN_SUPPLY',
  'CLEANING',
  'OTHER',
];

const supplyCategoryLabels: Record<SupplyCategory, string> = {
  CLAY: 'Clay',
  GLAZE: 'Glaze',
  GLAZE_MATERIAL: 'Glaze material',
  TOOL: 'Tool',
  KILN_SUPPLY: 'Kiln supply',
  CLEANING: 'Cleaning',
  OTHER: 'Other',
};

const atmosphereLabels: Record<Atmosphere, string> = {
  OXIDATION: 'Oxidation',
  REDUCTION: 'Reduction',
  NEUTRAL: 'Neutral',
};

const glazeUnitLabels: Record<GlazeAmountUnit, string> = {
  PARTS: 'parts',
  PERCENT: '%',
  GRAMS: 'g',
};

export const staffBookingKinds: BookingKind[] = [
  'RESERVATION',
  'CLASS',
  'EVENT',
  'RENTAL',
  'FIRING',
  'MAINTENANCE',
  'CLOSURE',
];

const bookingKindLabels: Record<BookingKind, string> = {
  RESERVATION: 'Reservation',
  CLASS: 'Class',
  EVENT: 'Event',
  RENTAL: 'Rental',
  FIRING: 'Firing',
  MAINTENANCE: 'Maintenance',
  CLOSURE: 'Closure',
};

const storedValueKindLabels: Record<StoredValueKind, string> = {
  FIRING_CARD: 'Firing card',
  CLASS_PACK: 'Class pack',
  GIFT_CARD: 'Gift card',
  DEPOSIT: 'Deposit',
  CREDIT: 'Credit',
};

export const providers: Provider[] = ['CASH', 'SQUARE', 'STRIPE', 'SHOPIFY', 'PAYPAL', 'OTHER'];

export const providerLabels: Record<Provider, string> = {
  CASH: 'Cash or cheque',
  SQUARE: 'Square',
  STRIPE: 'Stripe',
  SHOPIFY: 'Shopify',
  PAYPAL: 'PayPal',
  OTHER: 'Other',
};

export const stageLabel = (stage: PieceStage): string => stageLabels[stage];
export const roleLabel = (role: StudioRole): string => roleLabels[role];
export const dutyLabel = (duty: Duty): string => dutyLabels[duty];
export const assetKindLabel = (kind: AssetKind): string => assetKindLabels[kind];
export const kilnTypeLabel = (kind: KilnType): string => kilnTypeLabels[kind];
export const supplyCategoryLabel = (category: SupplyCategory): string => supplyCategoryLabels[category];
export const atmosphereLabel = (atmosphere: Atmosphere): string => atmosphereLabels[atmosphere];
export const glazeUnitLabel = (unit: GlazeAmountUnit): string => glazeUnitLabels[unit];
export const bookingKindLabel = (kind: BookingKind): string => bookingKindLabels[kind];
export const storedValueKindLabel = (kind: StoredValueKind): string => storedValueKindLabels[kind];

/** The units a studio measures pieces in by default. */
export function defaultUnits(system: UnitSystem): { length: LengthUnit; weight: WeightUnit } {
  return system === 'US_CUSTOMARY' ? { length: 'INCH', weight: 'POUND' } : { length: 'CENTIMETRE', weight: 'KILOGRAM' };
}

export const lengthUnitLabel = (unit: LengthUnit): string => ({ MILLIMETRE: 'mm', CENTIMETRE: 'cm', INCH: 'in' })[unit];
export const weightUnitLabel = (unit: WeightUnit): string =>
  ({ GRAM: 'g', KILOGRAM: 'kg', POUND: 'lb', OUNCE: 'oz' })[unit];

export function formatMoney(amount: string, currency: string): string {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(Number(amount));
}

export function formatDate(iso: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(iso));
}

export function formatTime(iso: string): string {
  return new Intl.DateTimeFormat(undefined, { timeStyle: 'short' }).format(new Date(iso));
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso));
}

export function formatDuration(minutes: number): string {
  const sign = minutes < 0 ? '−' : '';
  const abs = Math.abs(minutes);
  return `${sign}${String(Math.floor(abs / 60))} h ${String(abs % 60).padStart(2, '0')} min`;
}

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

/** "is due", "is due soon (2 firings left)", "next due: by 4 Nov 2026" */
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
