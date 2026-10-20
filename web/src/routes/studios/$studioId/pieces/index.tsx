import {
  getStudioOptions,
  intakePieceMutation,
  listMembersOptions,
  listPiecesOptions,
  listPiecesQueryKey,
  type LengthUnit,
  type Piece,
  type PieceSize,
  type PieceStage,
  quoteFeeMutation,
  type Studio,
  type WeightUnit,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute, Link } from '@tanstack/react-router';
import { type SubmitEvent, useId, useState } from 'react';

import { ErrorMessage } from '../../../../components/ErrorMessage';
import { formatDate, formatMoney, isStaff, stageLabel } from '../../../../labels';

const stages: PieceStage[] = ['RECEIVED', 'BISQUE_FIRED', 'GLAZED', 'READY', 'UNCLAIMED', 'PICKED_UP', 'DISPOSED'];

export const Route = createFileRoute('/studios/$studioId/pieces/')({
  component: PiecesPage,
});

function PiecesPage() {
  const { studioId } = Route.useParams();
  const { data: studio } = useSuspenseQuery(getStudioOptions({ path: { studioId } }));
  const [stage, setStage] = useState<PieceStage | ''>('');
  const pieces = useQuery(listPiecesOptions({ path: { studioId }, query: stage ? { stage } : undefined }));
  const staff = isStaff(studio.myRole);

  return (
    <div className="stack">
      <h1>{staff ? 'Pieces' : 'My pieces'}</h1>
      <IntakeForm studio={studio} />
      <div className="field">
        <label htmlFor="stage-filter">Show</label>
        <select id="stage-filter" value={stage} onChange={(e) => setStage(e.target.value as PieceStage | '')}>
          <option value="">Every stage</option>
          {stages.map((s) => (
            <option key={s} value={s}>
              {stageLabel(s)}
            </option>
          ))}
        </select>
      </div>
      {pieces.isPending && <p>Loading pieces…</p>}
      {pieces.isError && <ErrorMessage error={pieces.error} />}
      {pieces.isSuccess && <PieceTable pieces={pieces.data} studioId={studioId} showMaker={staff} />}
    </div>
  );
}

function PieceTable({ pieces, studioId, showMaker }: { pieces: Piece[]; studioId: string; showMaker: boolean }) {
  if (pieces.length === 0) {
    return <p>No pieces yet.</p>;
  }
  return (
    <table className="table">
      <thead>
        <tr>
          <th scope="col">#</th>
          {showMaker && <th scope="col">Maker</th>}
          <th scope="col">Piece</th>
          <th scope="col">Stage</th>
          <th scope="col">Fee</th>
        </tr>
      </thead>
      <tbody>
        {pieces.map((piece) => (
          <tr key={piece.id}>
            <td>
              <Link to="/studios/$studioId/pieces/$pieceId" params={{ studioId, pieceId: piece.id }}>
                {piece.number}
              </Link>
            </td>
            {showMaker && <td>{piece.makerName ?? 'Unnamed member'}</td>}
            <td>{piece.description ?? piece.makerMark ?? '—'}</td>
            <td>
              {stageLabel(piece.stage)}
              {piece.overdue && <span className="badge warning"> past hold</span>}
              {piece.stage === 'READY' && piece.holdUntil && !piece.overdue && (
                <span className="badge"> until {formatDate(piece.holdUntil)}</span>
              )}
            </td>
            <td>{formatMoney(piece.fee.amount.amount, piece.fee.amount.currency)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function IntakeForm({ studio }: { studio: Studio }) {
  const id = useId();
  const queryClient = useQueryClient();
  const staff = isStaff(studio.myRole);
  const metric = studio.unitSystem === 'METRIC';
  const lengthUnit: LengthUnit = metric ? 'CENTIMETRE' : 'INCH';
  const weightUnit: WeightUnit = metric ? 'KILOGRAM' : 'POUND';
  const members = useQuery({ ...listMembersOptions({ path: { studioId: studio.id } }), enabled: staff });

  const [maker, setMaker] = useState('');
  const [description, setDescription] = useState('');
  const [makerMark, setMakerMark] = useState('');
  const [clayBody, setClayBody] = useState('');
  const [length, setLength] = useState('');
  const [width, setWidth] = useState('');
  const [height, setHeight] = useState('');
  const [weight, setWeight] = useState('');

  const size: PieceSize = {
    ...(length && width && height ? { dimensions: { length, width, height, unit: lengthUnit } } : {}),
    ...(weight ? { weight: { value: weight, unit: weightUnit } } : {}),
  };
  const quote = useMutation(quoteFeeMutation());
  const intake = useMutation({
    ...intakePieceMutation(),
    onSuccess: async () => {
      setDescription('');
      setMakerMark('');
      setLength('');
      setWidth('');
      setHeight('');
      setWeight('');
      quote.reset();
      await queryClient.invalidateQueries({ queryKey: listPiecesQueryKey({ path: { studioId: studio.id } }) });
    },
  });

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    intake.mutate({
      path: { studioId: studio.id },
      body: {
        ...(maker ? { makerAccountId: maker } : {}),
        ...(description.trim() ? { description: description.trim() } : {}),
        ...(makerMark.trim() ? { makerMark: makerMark.trim() } : {}),
        ...(clayBody.trim() ? { clayBody: clayBody.trim() } : {}),
        size,
      },
    });
  }

  const unit = metric ? 'cm' : 'in';
  return (
    <form className="card stack" onSubmit={submit} aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Drop off a piece</h2>
      {staff && (
        <div className="field">
          <label htmlFor={`${id}-maker`}>Maker</label>
          <select id={`${id}-maker`} value={maker} onChange={(e) => setMaker(e.target.value)}>
            <option value="">Me</option>
            {members.data?.map((member) => (
              <option key={member.accountId} value={member.accountId}>
                {member.displayName ?? member.accountId}
              </option>
            ))}
          </select>
        </div>
      )}
      <div className="field">
        <label htmlFor={`${id}-description`}>Description</label>
        <input id={`${id}-description`} value={description} maxLength={200} onChange={(e) => setDescription(e.target.value)} />
      </div>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-mark`}>Maker&apos;s mark</label>
          <input id={`${id}-mark`} value={makerMark} maxLength={80} onChange={(e) => setMakerMark(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-clay`}>Clay body</label>
          <input id={`${id}-clay`} value={clayBody} maxLength={80} onChange={(e) => setClayBody(e.target.value)} />
        </div>
      </div>
      <fieldset className="field">
        <legend>Size ({unit})</legend>
        <div className="row">
          <NumberInput label={`Length (${unit})`} value={length} onChange={setLength} />
          <NumberInput label={`Width (${unit})`} value={width} onChange={setWidth} />
          <NumberInput label={`Height (${unit})`} value={height} onChange={setHeight} />
          <NumberInput label={`Weight (${metric ? 'kg' : 'lb'})`} value={weight} onChange={setWeight} />
        </div>
      </fieldset>
      {quote.isSuccess && (
        <p aria-live="polite">
          Fee: <strong>{formatMoney(quote.data.amount.amount, quote.data.amount.currency)}</strong>{' '}
          <span className="badge">({quote.data.breakdown})</span>
        </p>
      )}
      {quote.isError && <ErrorMessage error={quote.error} />}
      {intake.isError && <ErrorMessage error={intake.error} />}
      {intake.isSuccess && <p aria-live="polite">Piece {intake.data.number} is in.</p>}
      <div className="row">
        <button type="button" onClick={() => quote.mutate({ path: { studioId: studio.id }, body: size })}>
          Work out the fee
        </button>
        <button type="submit" className="primary" disabled={intake.isPending}>
          {intake.isPending ? 'Saving…' : 'Drop off'}
        </button>
      </div>
    </form>
  );
}

function NumberInput({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  const id = useId();
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input
        id={id}
        inputMode="decimal"
        pattern="\d{1,9}(\.\d{1,4})?"
        value={value}
        onChange={(e) => onChange(e.target.value.trim())}
      />
    </div>
  );
}
