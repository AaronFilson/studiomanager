import { describe, expect, it } from 'vitest';

import { cappedNote, FROM_THE_EXPORT } from './capped';

describe('cappedNote', () => {
  it('says nothing when the whole list came back', () => {
    expect(cappedNote({ truncated: false, limit: 2000 }, { order: 'newest', noun: 'pieces' }, 'Filter.')).toBeUndefined();
  });

  it('says nothing before the list has loaded', () => {
    expect(cappedNote(undefined, { order: 'newest', noun: 'pieces' }, 'Filter.')).toBeUndefined();
  });

  it('names the limit and how to find the rest', () => {
    expect(
      cappedNote(
        { truncated: true, limit: 2000 },
        { order: 'newest', noun: 'pieces' },
        'Filter by stage or member to find older ones.',
      ),
    ).toBe('Showing the newest 2,000 pieces. Filter by stage or member to find older ones.');
  });

  it('points at the export for lists with no filter', () => {
    expect(cappedNote({ truncated: true, limit: 1000 }, { order: 'newest', noun: 'payouts' }, FROM_THE_EXPORT)).toBe(
      `Showing the newest 1,000 payouts. ${FROM_THE_EXPORT}`,
    );
  });
});
