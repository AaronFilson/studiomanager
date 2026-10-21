import type {
  LengthUnit,
  MaintenanceTask,
  PieceStage,
  StudioRole,
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

export const stageLabel = (stage: PieceStage): string => stageLabels[stage];

export const isStaff = (role: StudioRole): boolean => role === 'OWNER' || role === 'STAFF';

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

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso));
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
