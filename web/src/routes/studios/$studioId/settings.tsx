import {
  type FeeRounding,
  type FeeRuleType,
  type FeeSchedule,
  type FeeUnit,
  getFeeScheduleOptions,
  getFeeScheduleQueryKey,
  getPiecePolicyOptions,
  getPiecePolicyQueryKey,
  getStudioOptions,
  listTaxRatesOptions,
  saveFeeScheduleMutation,
  savePiecePolicyMutation,
  setTaxRateMutation,
  type Studio,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { type SubmitEvent, useId, useState } from 'react';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { feeRuleTypeLabel, feeUnitLabel, roundingLabel } from '../../../labels';

export const Route = createFileRoute('/studios/$studioId/settings')({
  component: SettingsPage,
});

function SettingsPage() {
  const { studioId } = Route.useParams();
  const { data: studio } = useSuspenseQuery(getStudioOptions({ path: { studioId } }));
  const fees = useQuery(getFeeScheduleOptions({ path: { studioId } }));
  const policy = useQuery(getPiecePolicyOptions({ path: { studioId } }));

  return (
    <div className="stack">
      <h1>Settings</h1>
      {fees.isError && <ErrorMessage error={fees.error} />}
      {fees.isSuccess && <FeeForm key={fees.data.version} studio={studio} current={fees.data} />}
      {policy.isError && <ErrorMessage error={policy.error} />}
      {policy.isSuccess && <HoldForm key={policy.data.holdDays} studioId={studioId} holdDays={policy.data.holdDays} />}
      <TaxForm studioId={studioId} />
    </div>
  );
}

function TaxForm({ studioId }: { studioId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const rates = useQuery(listTaxRatesOptions({ path: { studioId } }));
  const [category, setCategory] = useState('FIRING_SERVICE');
  const [percent, setPercent] = useState('');
  const [from, setFrom] = useState(new Date().toISOString().slice(0, 10));
  const save = useMutation({ ...setTaxRateMutation(), onSuccess: () => queryClient.invalidateQueries() });

  return (
    <form
      className="card stack"
      aria-labelledby={`${id}-title`}
      onSubmit={(event) => {
        event.preventDefault();
        const rate = (Number(percent) / 100).toFixed(6).replace(/0+$/, '').replace(/\.$/, '');
        save.mutate({ path: { studioId }, body: { taxCategory: category.trim(), rate: rate === '' ? '0' : rate, effectiveFrom: from } });
      }}
    >
      <h2 id={`${id}-title`}>Sales tax</h2>
      <p className="badge">
        Rates change by date and by what is sold, so each category has its own rate from a given day. Fees use the
        category in the fee schedule.
      </p>
      <ul>
        {rates.data?.map((r) => (
          <li key={r.id}>
            {r.taxCategory}: {(Number(r.rate) * 100).toFixed(3).replace(/\.?0+$/, '')}% from {r.effectiveFrom}
          </li>
        ))}
      </ul>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-category`}>Category</label>
          <input id={`${id}-category`} required maxLength={40} value={category} onChange={(e) => setCategory(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-percent`}>Rate (%)</label>
          <input id={`${id}-percent`} required inputMode="decimal" value={percent} onChange={(e) => setPercent(e.target.value.trim())} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-from`}>From</label>
          <input id={`${id}-from`} type="date" required value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
      </div>
      {save.isError && <ErrorMessage error={save.error} />}
      <div>
        <button type="submit" className="primary" disabled={save.isPending}>
          Save rate
        </button>
      </div>
    </form>
  );
}

const unitsFor: Record<FeeRuleType, FeeUnit[]> = {
  PER_VOLUME: ['CUBIC_INCH', 'CUBIC_CENTIMETRE', 'LITRE'],
  PER_WEIGHT: ['POUND', 'KILOGRAM'],
  FLAT: [],
};

function FeeForm({ studio, current }: { studio: Studio; current: FeeSchedule }) {
  const id = useId();
  const queryClient = useQueryClient();
  const metric = studio.unitSystem === 'METRIC';
  const [ruleType, setRuleType] = useState<FeeRuleType>(current.version === 0 ? 'PER_VOLUME' : current.ruleType);
  const [unit, setUnit] = useState<FeeUnit | undefined>(
    current.unit ?? (current.version === 0 ? (metric ? 'LITRE' : 'CUBIC_INCH') : undefined),
  );
  const [rate, setRate] = useState(current.version === 0 ? '' : current.rate);
  const [rounding, setRounding] = useState<FeeRounding>(current.version === 0 ? 'WHOLE_UNIT' : current.rounding);
  const [minimum, setMinimum] = useState(current.minimum);
  const save = useMutation({
    ...saveFeeScheduleMutation(),
    onSuccess: (saved) => queryClient.setQueryData(getFeeScheduleQueryKey({ path: { studioId: studio.id } }), saved),
  });

  function changeType(type: FeeRuleType) {
    setRuleType(type);
    setUnit(unitsFor[type][0]);
  }

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    save.mutate({
      path: { studioId: studio.id },
      body: { ruleType, rate, rounding: ruleType === 'FLAT' ? 'NONE' : rounding, minimum, ...(unit && ruleType !== 'FLAT' ? { unit } : {}) },
    });
  }

  return (
    <form className="card stack" onSubmit={submit} aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Firing fees</h2>
      <p className="badge">
        {current.version === 0
          ? 'No fees set yet, so pieces fire free.'
          : `Version ${String(current.version)}. Saving creates a new version; pieces keep the fee they were priced with.`}
      </p>
      <fieldset className="field">
        <legend>Charge</legend>
        {(Object.keys(unitsFor) as FeeRuleType[]).map((type) => (
          <label key={type} className="choice">
            <input type="radio" name={`${id}-type`} checked={ruleType === type} onChange={() => changeType(type)} />
            {feeRuleTypeLabel(type)}
          </label>
        ))}
      </fieldset>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-rate`}>Rate ({studio.currency})</label>
          <input
            id={`${id}-rate`}
            required
            inputMode="decimal"
            pattern="\d{1,9}(\.\d{1,4})?"
            value={rate}
            onChange={(e) => setRate(e.target.value.trim())}
          />
        </div>
        {ruleType !== 'FLAT' && (
          <div className="field">
            <label htmlFor={`${id}-unit`}>Per</label>
            <select id={`${id}-unit`} value={unit} onChange={(e) => setUnit(e.target.value as FeeUnit)}>
              {unitsFor[ruleType].map((u) => (
                <option key={u} value={u}>
                  {feeUnitLabel(u)}
                </option>
              ))}
            </select>
          </div>
        )}
      </div>
      {ruleType !== 'FLAT' && (
        <div className="field">
          <label htmlFor={`${id}-rounding`}>{ruleType === 'PER_VOLUME' ? 'Each dimension' : 'The weight'}</label>
          <select id={`${id}-rounding`} value={rounding} onChange={(e) => setRounding(e.target.value as FeeRounding)}>
            {(['WHOLE_UNIT', 'HALF_UNIT', 'NONE'] as FeeRounding[]).map((r) => (
              <option key={r} value={r}>
                {roundingLabel(r)}
              </option>
            ))}
          </select>
        </div>
      )}
      <div className="field">
        <label htmlFor={`${id}-minimum`}>Minimum per piece ({studio.currency})</label>
        <input
          id={`${id}-minimum`}
          required
          inputMode="decimal"
          pattern="\d{1,9}(\.\d{1,2})?"
          value={minimum}
          onChange={(e) => setMinimum(e.target.value.trim())}
        />
      </div>
      {save.isError && <ErrorMessage error={save.error} />}
      {save.isSuccess && <p aria-live="polite">Saved as version {save.data.version}.</p>}
      <div>
        <button type="submit" className="primary" disabled={save.isPending}>
          Save fees
        </button>
      </div>
    </form>
  );
}

function HoldForm({ studioId, holdDays }: { studioId: string; holdDays: number }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [days, setDays] = useState(String(holdDays));
  const save = useMutation({
    ...savePiecePolicyMutation(),
    onSuccess: (saved) => queryClient.setQueryData(getPiecePolicyQueryKey({ path: { studioId } }), saved),
  });

  return (
    <form
      className="card stack"
      aria-labelledby={`${id}-title`}
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate({ path: { studioId }, body: { holdDays: Number(days) } });
      }}
    >
      <h2 id={`${id}-title`}>Pickup</h2>
      <div className="field">
        <label htmlFor={`${id}-days`}>Hold ready pieces for (days)</label>
        <input id={`${id}-days`} type="number" min={1} max={365} required value={days} onChange={(e) => setDays(e.target.value)} />
      </div>
      <p className="badge">After that, a piece is marked unclaimed. It can still be collected or disposed of.</p>
      {save.isError && <ErrorMessage error={save.error} />}
      {save.isSuccess && <p aria-live="polite">Saved.</p>}
      <div>
        <button type="submit" className="primary" disabled={save.isPending}>
          Save
        </button>
      </div>
    </form>
  );
}
