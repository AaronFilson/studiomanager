import { type CappedList, cappedNote } from '@studiomanager/api-client';

/**
 * Says so when a list was cut at its limit, and how to find what isn't shown. Renders nothing
 * when the whole list came back.
 */
export function CappedNote({
  list,
  order = 'newest',
  noun,
  findTheRest,
}: {
  list: CappedList | undefined;
  order?: string;
  noun: string;
  findTheRest: string;
}) {
  const note = cappedNote(list, { order, noun }, findTheRest);
  if (note === undefined) {
    return null;
  }
  return (
    <p role="status" className="badge warning">
      {note}
    </p>
  );
}
