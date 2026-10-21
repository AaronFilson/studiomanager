import {
  type ConsignmentItem,
  createPayeeMutation,
  type DiscountsFrom,
  type EarningKind,
  exportNecReport,
  getNecReportOptions,
  getPayeeStatementOptions,
  getStudioOptions,
  listConsignmentItemsOptions,
  listConsignmentSalesOptions,
  listMembersOptions,
  listPayeesOptions,
  type Payee,
  type PaymentMethod,
  payPayeeMutation,
  type Provider,
  recordEarningMutation,
  refundConsignmentSaleMutation,
  returnConsignmentMutation,
  type SavePayeeRequest,
  sellConsignmentMutation,
  type Studio,
  takeInConsignmentMutation,
  updatePayeeMutation,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { useId, useState } from 'react';

import { ErrorMessage, errorText } from '../../../components/ErrorMessage';
import { formatDate, formatMoney, providerLabels, providers } from '../../../labels';

export const Route = createFileRoute('/studios/$studioId/payables')({
  component: PayablesPage,
});

const methods: PaymentMethod[] = ['CHECK', 'BANK_TRANSFER', 'CASH', 'PAYPAL', 'STRIPE', 'STUDIO_CREDIT'];
const methodLabels: Record<PaymentMethod, string> = {
  CHECK: 'Cheque',
  BANK_TRANSFER: 'Bank transfer',
  CASH: 'Cash',
  PAYPAL: 'PayPal',
  STRIPE: 'Stripe',
  STUDIO_CREDIT: 'Studio credit',
};
const earningKindLabels: Record<EarningKind, string> = {
  CLASS_SHARE: 'Share of a class',
  SERVICE: 'Fee for work',
  ADJUSTMENT: 'Adjustment',
};

/** "60" (percent, as typed) to "0.6" (the API's fraction), or undefined when blank. */
function toShare(percent: string): string | undefined {
  return percent.trim() ? String(Number((Number(percent) / 100).toFixed(4))) : undefined;
}

const percent = (share: string) => `${String(Number((Number(share) * 100).toFixed(2)))}%`;
const today = () => new Date().toISOString().slice(0, 10);

function PayablesPage() {
  const { studioId } = Route.useParams();
  const { data: studio } = useSuspenseQuery(getStudioOptions({ path: { studioId } }));
  const payees = useQuery(listPayeesOptions({ path: { studioId } }));
  const [selected, setSelected] = useState<string>();
  const money = (amount: string) => formatMoney(amount, studio.currency);
  const active = payees.data?.filter((p) => !p.archived) ?? [];
  const chosen = payees.data?.find((p) => p.id === selected);

  return (
    <div className="stack">
      <h1>Artists and instructors</h1>

      <section className="card stack" aria-label="Who the studio owes">
        <h2>Who the studio owes</h2>
        {payees.isError && <ErrorMessage error={payees.error} />}
        {payees.isSuccess && payees.data.length === 0 && (
          <p>No one yet. Add the artists whose work you sell and the instructors you share class takings with.</p>
        )}
        <ul className="studio-list">
          {payees.data?.map((p) => (
            <li key={p.id}>
              <span>
                <button type="button" className="link" onClick={() => setSelected(p.id)} aria-pressed={p.id === selected}>
                  {p.name}
                </button>
                {p.consignorShare && ` · ${percent(p.consignorShare)} to the artist`}
                {p.files1099 && !p.w9ReceivedOn && <span className="badge"> · W-9 missing</span>}
                {p.archived && <span className="badge"> · archived</span>}
              </span>
              <span>{Number(p.owed) < 0 ? `owes ${money(String(-Number(p.owed)))}` : money(p.owed)}</span>
            </li>
          ))}
        </ul>
        <PayeeForm studio={studio} key="new" />
      </section>

      {chosen && <PayeePanel key={chosen.id} studio={studio} payee={chosen} />}

      <ConsignmentSection studio={studio} payees={active} />

      {studio.country === 'US' && <NecSection studioId={studioId} money={money} />}
    </div>
  );
}

/** Adds a payee, or (given one) edits it. */
function PayeeForm({ studio, payee }: { studio: Studio; payee?: Payee }) {
  const id = useId();
  const queryClient = useQueryClient();
  const members = useQuery(listMembersOptions({ path: { studioId: studio.id } }));
  const [name, setName] = useState(payee?.name ?? '');
  const [email, setEmail] = useState(payee?.email ?? '');
  const [accountId, setAccountId] = useState(payee?.accountId ?? '');
  const [share, setShare] = useState(payee?.consignorShare ? String(Number(payee.consignorShare) * 100) : '');
  const [discountsFrom, setDiscountsFrom] = useState<DiscountsFrom>(payee?.discountsFrom ?? 'SHARED');
  const [files1099, setFiles1099] = useState(payee?.files1099 ?? studio.country === 'US');
  const [w9, setW9] = useState(payee?.w9ReceivedOn ?? '');
  const [archived, setArchived] = useState(payee?.archived ?? false);
  const onSuccess = async () => {
    if (!payee) {
      setName('');
      setEmail('');
      setAccountId('');
      setShare('');
      setW9('');
    }
    await queryClient.invalidateQueries();
  };
  const create = useMutation({ ...createPayeeMutation(), onSuccess });
  const update = useMutation({ ...updatePayeeMutation(), onSuccess });
  const saving = create.isPending || update.isPending;

  function body(): SavePayeeRequest {
    const consignorShare = toShare(share);
    return {
      name: name.trim(),
      files1099,
      discountsFrom,
      archived,
      ...(email.trim() ? { email: email.trim() } : {}),
      ...(accountId ? { accountId } : {}),
      ...(consignorShare ? { consignorShare } : {}),
      ...(w9 ? { w9ReceivedOn: w9 } : {}),
    };
  }

  return (
    <form
      className="stack"
      aria-label={payee ? 'Edit payee' : 'Add a payee'}
      onSubmit={(event) => {
        event.preventDefault();
        if (payee) {
          update.mutate({ path: { studioId: studio.id, payeeId: payee.id }, body: body() });
        } else {
          create.mutate({ path: { studioId: studio.id }, body: body() });
        }
      }}
    >
      <h3>{payee ? 'Details' : 'Add a payee'}</h3>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-name`}>Name</label>
          <input id={`${id}-name`} required maxLength={120} value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-email`}>Email (optional)</label>
          <input id={`${id}-email`} type="email" maxLength={254} value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-member`}>Also a member</label>
          <select id={`${id}-member`} value={accountId} onChange={(e) => setAccountId(e.target.value)}>
            <option value="">No</option>
            {members.data?.map((m) => (
              <option key={m.accountId} value={m.accountId}>
                {m.displayName ?? m.accountId}
              </option>
            ))}
          </select>
        </div>
      </div>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-share`}>Artist&apos;s share of consignment sales, % (optional)</label>
          <input id={`${id}-share`} inputMode="decimal" value={share} onChange={(e) => setShare(e.target.value.trim())} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-discounts`}>Discounts come out of</label>
          <select id={`${id}-discounts`} value={discountsFrom} onChange={(e) => setDiscountsFrom(e.target.value as DiscountsFrom)}>
            <option value="SHARED">Both shares</option>
            <option value="STUDIO">The studio&apos;s commission</option>
          </select>
        </div>
      </div>
      <div className="row">
        <label>
          <input type="checkbox" checked={files1099} onChange={(e) => setFiles1099(e.target.checked)} /> File a 1099-NEC for
          them
        </label>
        <div className="field">
          <label htmlFor={`${id}-w9`}>W-9 received</label>
          <input id={`${id}-w9`} type="date" max={today()} value={w9} onChange={(e) => setW9(e.target.value)} />
        </div>
        {payee && (
          <label>
            <input type="checkbox" checked={archived} onChange={(e) => setArchived(e.target.checked)} /> Archived
          </label>
        )}
      </div>
      <p className="badge">
        Payments for merchandise, such as consignment, don&apos;t need a 1099-NEC, but some studios file one anyway. Ask
        your accountant. Keep tax ids on the W-9s, not here.
      </p>
      {create.isError && <ErrorMessage error={create.error} />}
      {update.isError && <ErrorMessage error={update.error} />}
      <div>
        <button type="submit" disabled={saving}>
          {payee ? 'Save' : 'Add payee'}
        </button>
      </div>
    </form>
  );
}

function PayeePanel({ studio, payee }: { studio: Studio; payee: Payee }) {
  const money = (amount: string) => formatMoney(amount, studio.currency);
  const firstOfMonth = `${today().slice(0, 8)}01`;
  const [from, setFrom] = useState(firstOfMonth);
  const [to, setTo] = useState(today());
  const id = useId();
  const statement = useQuery(
    getPayeeStatementOptions({ path: { studioId: studio.id, payeeId: payee.id }, query: { from, to } }),
  );

  return (
    <section className="card stack" aria-label={payee.name}>
      <h2>{payee.name}</h2>
      <p>
        Owed now: <strong>{money(payee.owed)}</strong>
      </p>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-from`}>Statement from</label>
          <input id={`${id}-from`} type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-to`}>To</label>
          <input id={`${id}-to`} type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
      </div>
      {statement.isError && <ErrorMessage error={statement.error} />}
      {statement.data && (
        <ol className="timeline" aria-label="Statement">
          <li>Owed at the start: {money(statement.data.opening)}</li>
          {statement.data.lines.map((line, i) => (
            <li key={i}>
              {formatDate(`${line.date}T12:00:00`)} · {line.memo} · {money(line.amount)}
            </li>
          ))}
          <li>Owed at the end: {money(statement.data.closing)}</li>
        </ol>
      )}
      <PayForm studio={studio} payee={payee} />
      <EarningForm studio={studio} payee={payee} />
      <PayeeForm studio={studio} payee={payee} />
    </section>
  );
}

function PayForm({ studio, payee }: { studio: Studio; payee: Payee }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [amount, setAmount] = useState(Number(payee.owed) > 0 ? payee.owed : '');
  const [method, setMethod] = useState<PaymentMethod>('CHECK');
  const [reference, setReference] = useState('');
  const pay = useMutation({
    ...payPayeeMutation(),
    onSuccess: async () => {
      setAmount('');
      setReference('');
      await queryClient.invalidateQueries();
    },
  });

  return (
    <form
      className="stack"
      aria-label="Record a payment to them"
      onSubmit={(event) => {
        event.preventDefault();
        pay.mutate({
          path: { studioId: studio.id, payeeId: payee.id },
          body: { amount, method, ...(reference.trim() ? { reference: reference.trim() } : {}) },
        });
      }}
    >
      <h3>Pay</h3>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-amount`}>Amount ({studio.currency})</label>
          <input id={`${id}-amount`} required inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value.trim())} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-method`}>Paid by</label>
          <select id={`${id}-method`} value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)}>
            {methods
              .filter((m) => m !== 'STUDIO_CREDIT' || payee.accountId)
              .map((m) => (
                <option key={m} value={m}>
                  {methodLabels[m]}
                </option>
              ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-ref`}>Reference (optional)</label>
          <input id={`${id}-ref`} maxLength={80} value={reference} onChange={(e) => setReference(e.target.value)} />
        </div>
      </div>
      {(method === 'PAYPAL' || method === 'STRIPE') && (
        <p className="badge">The network reports this on a 1099-K, so it isn&apos;t counted towards a 1099-NEC.</p>
      )}
      {pay.isError && <ErrorMessage error={pay.error} />}
      <div>
        <button type="submit" className="primary" disabled={pay.isPending}>
          Record payment
        </button>
      </div>
    </form>
  );
}

function EarningForm({ studio, payee }: { studio: Studio; payee: Payee }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [kind, setKind] = useState<EarningKind>('CLASS_SHARE');
  const [description, setDescription] = useState('');
  const [gross, setGross] = useState('');
  const [share, setShare] = useState('');
  const [amount, setAmount] = useState('');
  const record = useMutation({
    ...recordEarningMutation(),
    onSuccess: async () => {
      setDescription('');
      setGross('');
      setAmount('');
      await queryClient.invalidateQueries();
    },
  });
  const byShare = kind === 'CLASS_SHARE';

  return (
    <form
      className="stack"
      aria-label="Record what they earned"
      onSubmit={(event) => {
        event.preventDefault();
        record.mutate({
          path: { studioId: studio.id, payeeId: payee.id },
          body: {
            kind,
            description: description.trim(),
            ...(byShare ? { gross, share: toShare(share) ?? '' } : { amount }),
          },
        });
      }}
    >
      <h3>Record what they earned</h3>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-kind`}>For</label>
          <select id={`${id}-kind`} value={kind} onChange={(e) => setKind(e.target.value as EarningKind)}>
            {(Object.keys(earningKindLabels) as EarningKind[]).map((k) => (
              <option key={k} value={k}>
                {earningKindLabels[k]}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-desc`}>Description</label>
          <input id={`${id}-desc`} required maxLength={120} value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        {byShare ? (
          <>
            <div className="field">
              <label htmlFor={`${id}-gross`}>Class took</label>
              <input id={`${id}-gross`} required inputMode="decimal" value={gross} onChange={(e) => setGross(e.target.value.trim())} />
            </div>
            <div className="field">
              <label htmlFor={`${id}-share`}>Their share, %</label>
              <input id={`${id}-share`} required inputMode="decimal" value={share} onChange={(e) => setShare(e.target.value.trim())} />
            </div>
          </>
        ) : (
          <div className="field">
            <label htmlFor={`${id}-amount`}>Amount{kind === 'ADJUSTMENT' && ' (negative takes back)'}</label>
            <input id={`${id}-amount`} required inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value.trim())} />
          </div>
        )}
      </div>
      {record.isError && <ErrorMessage error={record.error} />}
      <div>
        <button type="submit" disabled={record.isPending}>
          Record
        </button>
      </div>
    </form>
  );
}

function ConsignmentSection({ studio, payees }: { studio: Studio; payees: Payee[] }) {
  const money = (amount: string) => formatMoney(amount, studio.currency);
  const queryClient = useQueryClient();
  const items = useQuery(listConsignmentItemsOptions({ path: { studioId: studio.id } }));
  const sales = useQuery(listConsignmentSalesOptions({ path: { studioId: studio.id } }));
  const refund = useMutation({ ...refundConsignmentSaleMutation(), onSuccess: () => queryClient.invalidateQueries() });

  return (
    <section className="card stack" aria-label="Consigned work">
      <h2>Consigned work</h2>
      {items.isError && <ErrorMessage error={items.error} />}
      {items.isSuccess && items.data.length === 0 && <p>Nothing on the shelf.</p>}
      <ul className="studio-list">
        {items.data?.map((item) => (
          <li key={item.id}>
            <span>
              {item.description} · {item.payeeName} · {money(item.retailPrice)} · {item.inStock} in stock
            </span>
            <SellForm studio={studio} item={item} />
          </li>
        ))}
      </ul>
      <TakeInForm studio={studio} payees={payees} />
      <h3>Recent sales</h3>
      {refund.isError && <ErrorMessage error={refund.error} />}
      <ol className="timeline">
        {sales.data?.map((sale) => (
          <li key={sale.id}>
            {formatDate(`${sale.soldOn}T12:00:00`)} · {sale.quantity} × {sale.description} ·{' '}
            {money(sale.price)} ({money(sale.consignorAmount)} to the artist)
            {sale.refundedOn ? (
              <span className="badge"> · refunded</span>
            ) : (
              <>
                {' '}
                <button
                  type="button"
                  disabled={refund.isPending}
                  onClick={() => refund.mutate({ path: { studioId: studio.id, saleId: sale.id } })}
                >
                  Refund
                </button>
              </>
            )}
          </li>
        ))}
      </ol>
    </section>
  );
}

function SellForm({ studio, item }: { studio: Studio; item: ConsignmentItem }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [price, setPrice] = useState(item.retailPrice);
  const [provider, setProvider] = useState<Provider>('CASH');
  const sell = useMutation({ ...sellConsignmentMutation(), onSuccess: () => queryClient.invalidateQueries() });
  const giveBack = useMutation({ ...returnConsignmentMutation(), onSuccess: () => queryClient.invalidateQueries() });

  return (
    <form
      className="row"
      aria-label={`Sell ${item.description}`}
      onSubmit={(event) => {
        event.preventDefault();
        sell.mutate({ path: { studioId: studio.id, itemId: item.id }, body: { quantity: 1, price, provider } });
      }}
    >
      <div className="field">
        <label htmlFor={`${id}-price`}>Sold for</label>
        <input id={`${id}-price`} required inputMode="decimal" value={price} onChange={(e) => setPrice(e.target.value.trim())} />
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
      <button type="submit" disabled={sell.isPending}>
        Sell one
      </button>
      <button
        type="button"
        disabled={giveBack.isPending}
        onClick={() =>
          giveBack.mutate({ path: { studioId: studio.id, itemId: item.id }, body: { quantity: item.inStock } })
        }
      >
        Return to artist
      </button>
      {sell.isError && <ErrorMessage error={sell.error} />}
      {giveBack.isError && <ErrorMessage error={giveBack.error} />}
    </form>
  );
}

function TakeInForm({ studio, payees }: { studio: Studio; payees: Payee[] }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [payeeId, setPayeeId] = useState('');
  const [description, setDescription] = useState('');
  const [retailPrice, setRetailPrice] = useState('');
  const [quantity, setQuantity] = useState('1');
  const [share, setShare] = useState('');
  const payee = payees.find((p) => p.id === payeeId);
  const takeIn = useMutation({
    ...takeInConsignmentMutation(),
    onSuccess: async () => {
      setDescription('');
      setRetailPrice('');
      setQuantity('1');
      await queryClient.invalidateQueries();
    },
  });

  return (
    <form
      className="stack"
      aria-label="Take in work"
      onSubmit={(event) => {
        event.preventDefault();
        const consignorShare = toShare(share);
        takeIn.mutate({
          path: { studioId: studio.id },
          body: {
            payeeId,
            description: description.trim(),
            retailPrice,
            quantity: Number(quantity),
            ...(consignorShare ? { consignorShare } : {}),
          },
        });
      }}
    >
      <h3>Take in work</h3>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-payee`}>Artist</label>
          <select id={`${id}-payee`} required value={payeeId} onChange={(e) => setPayeeId(e.target.value)}>
            <option value="">Choose…</option>
            {payees.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-desc`}>What</label>
          <input id={`${id}-desc`} required maxLength={120} value={description} onChange={(e) => setDescription(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-price`}>Retail price</label>
          <input id={`${id}-price`} required inputMode="decimal" value={retailPrice} onChange={(e) => setRetailPrice(e.target.value.trim())} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-qty`}>How many</label>
          <input id={`${id}-qty`} required type="number" min={1} max={10000} value={quantity} onChange={(e) => setQuantity(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-share`}>Artist&apos;s share, %</label>
          <input
            id={`${id}-share`}
            inputMode="decimal"
            placeholder={payee?.consignorShare ? String(Number(payee.consignorShare) * 100) : ''}
            value={share}
            onChange={(e) => setShare(e.target.value.trim())}
          />
        </div>
      </div>
      {takeIn.isError && <ErrorMessage error={takeIn.error} />}
      <div>
        <button type="submit" disabled={takeIn.isPending}>
          Take in
        </button>
      </div>
    </form>
  );
}

function NecSection({ studioId, money }: { studioId: string; money: (amount: string) => string }) {
  const id = useId();
  const thisYear = new Date().getFullYear();
  const [year, setYear] = useState(thisYear);
  const [error, setError] = useState<string>();
  const report = useQuery(getNecReportOptions({ path: { studioId }, query: { year } }));

  async function download() {
    setError(undefined);
    try {
      const { data } = await exportNecReport({ path: { studioId }, query: { year }, parseAs: 'text', throwOnError: true });
      const url = URL.createObjectURL(new Blob([data], { type: 'text/csv' }));
      const link = document.createElement('a');
      link.href = url;
      link.download = `1099-nec-${String(year)}.csv`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError(errorText(e));
    }
  }

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>1099-NEC</h2>
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-year`}>Payments made in</label>
          <select id={`${id}-year`} value={year} onChange={(e) => setYear(Number(e.target.value))}>
            {[thisYear, thisYear - 1, thisYear - 2].map((y) => (
              <option key={y} value={y}>
                {y}
              </option>
            ))}
          </select>
        </div>
        <button type="button" onClick={() => void download()}>
          Download CSV
        </button>
      </div>
      {report.data && (
        <p className="badge">
          A form is due for each payee paid {money(report.data.threshold)} or more by cheque, bank transfer, cash or studio
          credit. Card and PayPal or Stripe payments are on the network&apos;s 1099-K instead.
        </p>
      )}
      {report.isError && <ErrorMessage error={report.error} />}
      {report.isSuccess && report.data.payees.length === 0 && <p>No payments to report.</p>}
      <ul className="studio-list">
        {report.data?.payees.map((line) => (
          <li key={line.payeeId}>
            <span>
              {line.name}
              {!line.w9ReceivedOn && <span className="badge"> · W-9 missing</span>}
              {line.meetsThreshold && <span className="badge"> · form needed</span>}
            </span>
            <span>{money(line.reportable)}</span>
          </li>
        ))}
      </ul>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </section>
  );
}
