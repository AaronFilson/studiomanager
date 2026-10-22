import {
  exportJournal,
  getClearingOptions,
  getStudioOptions,
  importPayoutMutation,
  listBalancesOptions,
  listMembersOptions,
  listPayoutsOptions,
  listStoredValuesOptions,
  type PayoutProvider,
  type Provider,
  recordPaymentMutation,
  sellStoredValueMutation,
  type StoredValueKind,
  type Studio,
  useStoredValueMutation,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { type SubmitEvent, useId, useState } from 'react';

import { ErrorMessage, errorText } from '../../../components/ErrorMessage';
import { formatDate, formatMoney, providerLabels, providers } from '../../../labels';

export const Route = createFileRoute('/studios/$studioId/money')({
  component: MoneyPage,
});

const payoutProviders: PayoutProvider[] = ['STRIPE', 'SQUARE', 'SHOPIFY', 'PAYPAL', 'OTHER'];
const kinds: StoredValueKind[] = ['FIRING_CARD', 'CLASS_PACK', 'GIFT_CARD', 'DEPOSIT', 'CREDIT'];
const kindLabels: Record<StoredValueKind, string> = {
  FIRING_CARD: 'Firing card',
  CLASS_PACK: 'Class pack',
  GIFT_CARD: 'Gift card',
  DEPOSIT: 'Deposit',
  CREDIT: 'Credit',
};

function MoneyPage() {
  const { studioId } = Route.useParams();
  const { data: studio } = useSuspenseQuery(getStudioOptions({ path: { studioId } }));
  const money = (amount: string) => formatMoney(amount, studio.currency);
  const balances = useQuery(listBalancesOptions({ path: { studioId } }));
  const storedValues = useQuery(listStoredValuesOptions({ path: { studioId } }));
  const clearing = useQuery(getClearingOptions({ path: { studioId } }));
  const payouts = useQuery(listPayoutsOptions({ path: { studioId } }));

  return (
    <div className="stack">
      <h1>Money</h1>

      <section className="card stack" aria-label="Balances owed">
        <h2>Who owes what</h2>
        {balances.isError && <ErrorMessage error={balances.error} />}
        {balances.isSuccess && balances.data.length === 0 && <p>Nobody owes anything.</p>}
        <ul className="studio-list">
          {balances.data?.map((b) => (
            <li key={b.memberId}>
              <span>{b.memberName ?? 'Unnamed member'}</span>
              <span>{Number(b.owed) < 0 ? `${money(String(-Number(b.owed)))} in credit` : money(b.owed)}</span>
            </li>
          ))}
        </ul>
        <PaymentForm studio={studio} />
      </section>

      <section className="card stack" aria-label="Prepaid balances">
        <h2>Prepaid balances</h2>
        <ul className="studio-list">
          {storedValues.data?.map((sv) => (
            <li key={sv.id}>
              <span>
                {kindLabels[sv.kind]}
                {sv.label && ` · ${sv.label}`}
                {sv.holderName && ` · ${sv.holderName}`}
                {sv.code && ` · ${sv.code}`}
                {sv.expiresOn && <span className="badge"> · expires {formatDate(`${sv.expiresOn}T12:00:00`)}</span>}
              </span>
              <span>{money(sv.remaining)} left</span>
            </li>
          ))}
        </ul>
        <SellForm studio={studio} />
        <UseForm studio={studio} />
      </section>

      <section className="card stack" aria-label="Payouts">
        <h2>Payouts and reconciliation</h2>
        {clearing.isSuccess && clearing.data.length === 0 && <p>Every processor has paid out everything recorded.</p>}
        <ul className="studio-list">
          {clearing.data?.map((c) => (
            <li key={c.provider}>
              <span>{providerLabels[c.provider]}: taken but not yet paid out</span>
              <span>{money(c.awaitingPayout)}</span>
            </li>
          ))}
        </ul>
        <PayoutForm studioId={studioId} />
        <ol className="timeline">
          {payouts.data?.map((p) => (
            <li key={p.id}>
              {providerLabels[p.provider]} {p.externalId} · {formatDate(`${p.arrivalOn}T12:00:00`)} · {money(p.gross)} −{' '}
              {money(p.refunds)} refunds − {money(p.fees)} fees = {money(p.net)}
            </li>
          ))}
        </ol>
      </section>

      <ExportForm studioId={studioId} />
    </div>
  );
}

function MemberSelect({ studioId, id, value, onChange }: { studioId: string; id: string; value: string; onChange: (v: string) => void }) {
  const members = useQuery(listMembersOptions({ path: { studioId } }));
  return (
    <select id={id} required value={value} onChange={(e) => onChange(e.target.value)}>
      <option value="">Choose…</option>
      {members.data?.map((m) => (
        <option key={m.accountId} value={m.accountId}>
          {m.displayName ?? m.accountId}
        </option>
      ))}
    </select>
  );
}

function PaymentForm({ studio }: { studio: Studio }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [member, setMember] = useState('');
  const [amount, setAmount] = useState('');
  const [provider, setProvider] = useState<Provider>('CASH');
  const pay = useMutation({
    ...recordPaymentMutation(),
    onSuccess: async () => {
      setAmount('');
      await queryClient.invalidateQueries();
    },
  });

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    pay.mutate({ path: { studioId: studio.id }, body: { memberId: member, amount, provider } });
  }

  return (
    <form className="row" onSubmit={submit} aria-label="Record a payment">
      <div className="field">
        <label htmlFor={`${id}-member`}>Payment from</label>
        <MemberSelect studioId={studio.id} id={`${id}-member`} value={member} onChange={setMember} />
      </div>
      <div className="field">
        <label htmlFor={`${id}-amount`}>Amount ({studio.currency})</label>
        <input id={`${id}-amount`} required inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value.trim())} />
      </div>
      <div className="field">
        <label htmlFor={`${id}-provider`}>Paid by</label>
        <select id={`${id}-provider`} value={provider} onChange={(e) => setProvider(e.target.value as Provider)}>
          {providers.map((p) => (
            <option key={p} value={p}>
              {providerLabels[p]}
            </option>
          ))}
        </select>
      </div>
      <button type="submit" className="primary" disabled={pay.isPending}>
        Record payment
      </button>
      {pay.isError && <ErrorMessage error={pay.error} />}
    </form>
  );
}

function SellForm({ studio }: { studio: Studio }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [kind, setKind] = useState<StoredValueKind>('FIRING_CARD');
  const [holder, setHolder] = useState('');
  const [label, setLabel] = useState('');
  const [amount, setAmount] = useState('');
  const [provider, setProvider] = useState<Provider>('CASH');
  const sell = useMutation({
    ...sellStoredValueMutation(),
    onSuccess: async () => {
      setAmount('');
      setLabel('');
      await queryClient.invalidateQueries();
    },
  });

  return (
    <form
      className="stack"
      aria-label="Sell prepaid value"
      onSubmit={(event) => {
        event.preventDefault();
        sell.mutate({
          path: { studioId: studio.id },
          body: {
            kind,
            amount,
            provider,
            ...(kind !== 'GIFT_CARD' || holder ? { holder } : {}),
            ...(label.trim() ? { label: label.trim() } : {}),
          },
        });
      }}
    >
      <h3>Sell</h3>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-kind`}>What</label>
          <select id={`${id}-kind`} value={kind} onChange={(e) => setKind(e.target.value as StoredValueKind)}>
            {kinds.map((k) => (
              <option key={k} value={k}>
                {kindLabels[k]}
              </option>
            ))}
          </select>
        </div>
        {kind !== 'GIFT_CARD' && (
          <div className="field">
            <label htmlFor={`${id}-holder`}>For</label>
            <MemberSelect studioId={studio.id} id={`${id}-holder`} value={holder} onChange={setHolder} />
          </div>
        )}
        <div className="field">
          <label htmlFor={`${id}-label`}>Label (optional)</label>
          <input id={`${id}-label`} maxLength={80} value={label} onChange={(e) => setLabel(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-amount`}>Amount</label>
          <input id={`${id}-amount`} required inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value.trim())} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-provider`}>Paid by</label>
          <select id={`${id}-provider`} value={provider} onChange={(e) => setProvider(e.target.value as Provider)}>
            {providers.map((p) => (
              <option key={p} value={p}>
                {providerLabels[p]}
              </option>
            ))}
          </select>
        </div>
      </div>
      {sell.isError && <ErrorMessage error={sell.error} />}
      {sell.data?.code && <p aria-live="polite">Gift card code: <strong>{sell.data.code}</strong></p>}
      <div>
        <button type="submit" disabled={sell.isPending}>
          Sell
        </button>
      </div>
    </form>
  );
}

function UseForm({ studio }: { studio: Studio }) {
  const id = useId();
  const queryClient = useQueryClient();
  const storedValues = useQuery(listStoredValuesOptions({ path: { studioId: studio.id } }));
  const [balance, setBalance] = useState('');
  const [member, setMember] = useState('');
  const [amount, setAmount] = useState('');
  const use = useMutation({
    ...useStoredValueMutation(),
    onSuccess: async () => {
      setAmount('');
      await queryClient.invalidateQueries();
    },
  });

  return (
    <form
      className="stack"
      aria-label="Use prepaid value"
      onSubmit={(event) => {
        event.preventDefault();
        use.mutate({ path: { studioId: studio.id, storedValueId: balance }, body: { memberId: member, amount } });
      }}
    >
      <h3>Use towards a balance</h3>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-balance`}>From</label>
          <select id={`${id}-balance`} required value={balance} onChange={(e) => setBalance(e.target.value)}>
            <option value="">Choose…</option>
            {storedValues.data?.map((sv) => (
              <option key={sv.id} value={sv.id}>
                {kindLabels[sv.kind]} {sv.holderName ?? sv.code ?? ''} ({sv.remaining})
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-member`}>Pays for</label>
          <MemberSelect studioId={studio.id} id={`${id}-member`} value={member} onChange={setMember} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-amount`}>Amount</label>
          <input id={`${id}-amount`} required inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value.trim())} />
        </div>
      </div>
      {use.isError && <ErrorMessage error={use.error} />}
      <div>
        <button type="submit" disabled={use.isPending}>
          Use
        </button>
      </div>
    </form>
  );
}

function PayoutForm({ studioId }: { studioId: string }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [provider, setProvider] = useState<PayoutProvider>('STRIPE');
  const [externalId, setExternalId] = useState('');
  const [arrivalOn, setArrivalOn] = useState('');
  const [gross, setGross] = useState('');
  const [refunds, setRefunds] = useState('0');
  const [fees, setFees] = useState('0');
  const [adjustments, setAdjustments] = useState('0');
  const net = (Number(gross || 0) - Number(refunds || 0) - Number(fees || 0) + Number(adjustments || 0)).toFixed(2);
  const importPayout = useMutation({
    ...importPayoutMutation(),
    onSuccess: async () => {
      setExternalId('');
      setGross('');
      await queryClient.invalidateQueries();
    },
  });

  return (
    <form
      className="stack"
      aria-label="Import a payout"
      onSubmit={(event) => {
        event.preventDefault();
        importPayout.mutate({
          path: { studioId },
          body: { provider, externalId: externalId.trim(), arrivalOn, gross, refunds, fees, adjustments, net },
        });
      }}
    >
      <h3>Record a payout</h3>
      <p className="badge">From the processor&apos;s payout report. The net must match what reached the bank.</p>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-provider`}>From</label>
          <select id={`${id}-provider`} value={provider} onChange={(e) => setProvider(e.target.value as PayoutProvider)}>
            {payoutProviders.map((p) => (
              <option key={p} value={p}>
                {providerLabels[p]}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-ext`}>Payout id</label>
          <input id={`${id}-ext`} required maxLength={120} value={externalId} onChange={(e) => setExternalId(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-date`}>Arrived</label>
          <input id={`${id}-date`} type="date" required value={arrivalOn} onChange={(e) => setArrivalOn(e.target.value)} />
        </div>
      </div>
      <div className="row">
        <DecimalField label="Gross" value={gross} onChange={setGross} />
        <DecimalField label="Refunds" value={refunds} onChange={setRefunds} />
        <DecimalField label="Fees" value={fees} onChange={setFees} />
        <DecimalField label="Adjustments" value={adjustments} onChange={setAdjustments} />
      </div>
      <p>Net: {net}</p>
      {importPayout.isError && <ErrorMessage error={importPayout.error} />}
      <div>
        <button type="submit" disabled={importPayout.isPending}>
          Record payout
        </button>
      </div>
    </form>
  );
}

function DecimalField({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  const id = useId();
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} inputMode="decimal" value={value} onChange={(e) => onChange(e.target.value.trim())} />
    </div>
  );
}

function ExportForm({ studioId }: { studioId: string }) {
  const id = useId();
  const firstOfMonth = new Date();
  firstOfMonth.setDate(1);
  const [from, setFrom] = useState(firstOfMonth.toISOString().slice(0, 10));
  const [to, setTo] = useState(new Date().toISOString().slice(0, 10));
  const [error, setError] = useState<string>();

  async function download() {
    setError(undefined);
    try {
      const { data } = await exportJournal({ path: { studioId }, query: { from, to }, parseAs: 'text', throwOnError: true });
      const url = URL.createObjectURL(new Blob([data], { type: 'text/csv' }));
      const link = document.createElement('a');
      link.href = url;
      link.download = `studio-journal-${from}-to-${to}.csv`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(errorText(e));
    }
  }

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Export for your bookkeeper</h2>
      <p className="badge">One balanced journal per day, ready to import into QuickBooks Online or Xero.</p>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-from`}>From</label>
          <input id={`${id}-from`} type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-to`}>To</label>
          <input id={`${id}-to`} type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
        <button type="button" onClick={() => void download()}>
          Download CSV
        </button>
      </div>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </section>
  );
}
