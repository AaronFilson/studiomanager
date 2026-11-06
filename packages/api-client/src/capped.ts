/** The shape every capped list in the API shares: the rows, and whether any were left out. */
export interface CappedList {
  truncated: boolean;
  limit: number;
}

/**
 * What to tell people when a list was cut at its limit, or `undefined` when nothing was.
 *
 * @param list the list response
 * @param shown what is listed, for example "newest 2,000 pieces" reads from `newest` and `pieces`
 * @param findTheRest how to reach what isn't shown: a filter to narrow by, or the studio export
 */
export function cappedNote(
  list: CappedList | undefined,
  shown: { order: string; noun: string },
  findTheRest: string,
): string | undefined {
  if (!list?.truncated) {
    return undefined;
  }
  return `Showing the ${shown.order} ${list.limit.toLocaleString('en-US')} ${shown.noun}. ${findTheRest}`;
}

/** The hint for lists with no filter: the studio export has every row. */
export const FROM_THE_EXPORT = 'The studio export (Settings) has them all.';
