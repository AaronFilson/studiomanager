import {
  getPieceOptions,
  getPieceQueryKey,
  getStudioOptions,
  listPiecesQueryKey,
  movePieceMutation,
  type PieceSize,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute, Link } from '@tanstack/react-router';
import { useId, useState } from 'react';

import { ErrorMessage } from '../../../../components/ErrorMessage';
import {
  formatDate,
  formatDateTime,
  formatMoney,
  isStaff,
  lengthUnitLabel,
  stageLabel,
  weightUnitLabel,
} from '../../../../labels';

export const Route = createFileRoute('/studios/$studioId/pieces/$pieceId')({
  component: PiecePage,
});

function PiecePage() {
  const { studioId, pieceId } = Route.useParams();
  const { data: studio } = useSuspenseQuery(getStudioOptions({ path: { studioId } }));
  const piece = useQuery(getPieceOptions({ path: { studioId, pieceId } }));
  const queryClient = useQueryClient();
  const noteId = useId();
  const [note, setNote] = useState('');
  const move = useMutation({
    ...movePieceMutation(),
    onSuccess: async (moved) => {
      setNote('');
      queryClient.setQueryData(getPieceQueryKey({ path: { studioId, pieceId } }), moved);
      await queryClient.invalidateQueries({ queryKey: listPiecesQueryKey({ path: { studioId } }) });
    },
  });

  if (piece.isPending) {
    return <p>Loading piece…</p>;
  }
  if (piece.isError) {
    return <ErrorMessage error={piece.error} />;
  }
  const p = piece.data;

  return (
    <article className="stack">
      <p>
        <Link to="/studios/$studioId/pieces" params={{ studioId }}>
          ← All pieces
        </Link>
      </p>
      <h1>
        Piece {p.number}
        {p.description ? `: ${p.description}` : ''}
      </h1>
      <dl className="facts">
        <dt>Maker</dt>
        <dd>{p.makerName ?? 'Unnamed member'}</dd>
        <dt>Stage</dt>
        <dd>
          {stageLabel(p.stage)}
          {p.overdue && <span className="badge warning"> past its hold</span>}
        </dd>
        {p.holdUntil && (
          <>
            <dt>Held until</dt>
            <dd>{formatDate(p.holdUntil)}</dd>
          </>
        )}
        {p.makerMark && (
          <>
            <dt>Maker&apos;s mark</dt>
            <dd>{p.makerMark}</dd>
          </>
        )}
        {p.clayBody && (
          <>
            <dt>Clay body</dt>
            <dd>{p.clayBody}</dd>
          </>
        )}
        <dt>Size</dt>
        <dd>{sizeText(p.size)}</dd>
        <dt>Fee</dt>
        <dd>
          {formatMoney(p.fee.amount.amount, p.fee.amount.currency)} <span className="badge">({p.fee.breakdown})</span>
        </dd>
      </dl>

      {isStaff(studio.myRole) && p.nextStages.length > 0 && (
        <section className="card stack" aria-labelledby={`${noteId}-title`}>
          <h2 id={`${noteId}-title`}>Move this piece</h2>
          <div className="field">
            <label htmlFor={noteId}>Note (optional)</label>
            <input id={noteId} value={note} maxLength={500} onChange={(e) => setNote(e.target.value)} />
          </div>
          {move.isError && <ErrorMessage error={move.error} />}
          <div className="row">
            {p.nextStages.map((stage) => (
              <button
                key={stage}
                type="button"
                disabled={move.isPending}
                onClick={() =>
                  move.mutate({
                    path: { studioId, pieceId },
                    body: { to: stage, expectedVersion: p.version, ...(note.trim() ? { note: note.trim() } : {}) },
                  })
                }
              >
                {stageLabel(stage)}
              </button>
            ))}
          </div>
        </section>
      )}

      <section className="stack" aria-label="Timeline">
        <h2>Timeline</h2>
        <ol className="timeline">
          {p.timeline.map((event, index) => (
            <li key={index}>
              <strong>{stageLabel(event.to)}</strong> · {formatDateTime(event.occurredAt)}
              {event.actorName && ` · ${event.actorName}`}
              {!event.actorAccountId && ' · automatic'}
              {event.note && <div className="badge">{event.note}</div>}
            </li>
          ))}
        </ol>
      </section>
    </article>
  );
}

function sizeText(size: PieceSize): string {
  const parts: string[] = [];
  if (size.dimensions) {
    const { length, width, height, unit } = size.dimensions;
    parts.push(`${length} × ${width} × ${height} ${lengthUnitLabel(unit)}`);
  }
  if (size.weight) {
    parts.push(`${size.weight.value} ${weightUnitLabel(size.weight.unit)}`);
  }
  return parts.length > 0 ? parts.join(', ') : 'Not measured';
}
