import {
  type AdminStudio,
  getInvoiceReadersOptions,
  getInvoiceReadersQueryKey,
  type InvoiceReader,
  type InvoiceReaderOverview,
  type InvoiceReaderSettings,
  listAdminStudiosOptions,
  listSettingChangesQueryKey,
  type PlatformReaderRule,
  type PlatformReaderScope,
  resetInvoiceReadersMutation,
  setInvoiceReadersMutation,
  type StudioReaderOrder,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useId, useState } from 'react';

import { readerLabels } from './CompareReaders';
import { ErrorMessage } from './ErrorMessage';

const scopeLabels: Record<PlatformReaderScope, string> = {
  NONE: 'No studios',
  CHOSEN: 'Chosen studios',
  ALL: 'Every studio that consents',
};

const allReaders: InvoiceReader[] = ['CLAUDE', 'GOOGLE_DOCUMENT_AI', 'AZURE_DOCUMENT_INTELLIGENCE'];

/**
 * The platform-paid invoice readers (docs/planning/20, "Invoice readers"): on or off, for which
 * studios, under which monthly page caps, with this month's pages and spend, and each studio's
 * reader order. A studio's owner still has to consent before its invoices go to Google or Azure.
 */
export function InvoiceReadersAdmin() {
  const id = useId();
  const overview = useQuery(getInvoiceReadersOptions());
  const studios = useQuery(listAdminStudiosOptions());
  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Invoice readers</h2>
      {overview.isPending && <p>Loading the readers…</p>}
      {overview.isError && <ErrorMessage error={overview.error} />}
      {studios.isError && <ErrorMessage error={studios.error} />}
      {overview.data && (
        <ReadersForm
          key={overview.data.monthStart + String(overview.data.saved)}
          overview={overview.data}
          studios={studios.data ?? []}
        />
      )}
    </section>
  );
}

function ReadersForm({ overview, studios }: { overview: InvoiceReaderOverview; studios: AdminStudio[] }) {
  const queryClient = useQueryClient();
  const [settings, setSettings] = useState<InvoiceReaderSettings>(overview.settings);
  const refresh = (after: InvoiceReaderOverview) => {
    queryClient.setQueryData(getInvoiceReadersQueryKey(), after);
    setSettings(after.settings);
    void queryClient.invalidateQueries({ queryKey: listSettingChangesQueryKey() });
  };
  const save = useMutation({ ...setInvoiceReadersMutation(), onSuccess: refresh });
  const reset = useMutation({ ...resetInvoiceReadersMutation(), onSuccess: refresh });

  const updateRule = (index: number, rule: PlatformReaderRule) =>
    setSettings({ ...settings, readers: settings.readers.map((old, at) => (at === index ? rule : old)) });
  const updateOrder = (index: number, order: StudioReaderOrder) =>
    setSettings({ ...settings, studioOrders: settings.studioOrders.map((old, at) => (at === index ? order : old)) });

  return (
    <form
      className="stack"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate({ body: settings });
      }}
    >
      <p>
        {overview.saved
          ? 'Changed here.'
          : 'The defaults: on for the demo studio only, so the platform pays nothing until a reader is turned on.'}{' '}
        A studio&apos;s owner must also consent in its AI key settings.
      </p>
      {settings.readers.map((rule, index) => (
        <ReaderRuleFields
          key={rule.reader}
          rule={rule}
          studios={studios}
          usage={overview.usage.find((use) => use.reader === rule.reader)}
          onChange={(changed) => updateRule(index, changed)}
        />
      ))}
      <fieldset className="stack">
        <legend>Reader order per studio</legend>
        <p>Studios not listed try Claude, then Google, then Azure. The first reader that may read, reads.</p>
        {settings.studioOrders.map((order, index) => (
          <OrderFields
            key={order.studioId + String(index)}
            order={order}
            studios={studios}
            onChange={(changed) => updateOrder(index, changed)}
            onRemove={() =>
              setSettings({
                ...settings,
                studioOrders: [...settings.studioOrders.slice(0, index), ...settings.studioOrders.slice(index + 1)],
              })
            }
          />
        ))}
        <div>
          <button
            type="button"
            disabled={studios.length === 0}
            onClick={() =>
              setSettings({
                ...settings,
                studioOrders: [...settings.studioOrders, { studioId: studios[0]?.id ?? '', readers: ['CLAUDE'] }],
              })
            }
          >
            Add a studio&apos;s order
          </button>
        </div>
      </fieldset>
      {save.isError && <ErrorMessage error={save.error} />}
      {reset.isError && <ErrorMessage error={reset.error} />}
      <div>
        <button type="submit" className="primary" disabled={save.isPending}>
          Save readers
        </button>{' '}
        {overview.saved && (
          <button type="button" disabled={reset.isPending} onClick={() => reset.mutate({})}>
            Back to defaults
          </button>
        )}
      </div>
    </form>
  );
}

function ReaderRuleFields({
  rule,
  studios,
  usage,
  onChange,
}: {
  rule: PlatformReaderRule;
  studios: AdminStudio[];
  usage: InvoiceReaderOverview['usage'][number] | undefined;
  onChange: (rule: PlatformReaderRule) => void;
}) {
  const id = useId();
  const label = readerLabels[rule.reader];
  return (
    <fieldset className="stack">
      <legend>{label}</legend>
      <p role="status">
        This month: {String(usage?.pages ?? 0)} pages in {String(usage?.reads ?? 0)} reads, about $
        {usage?.costUsd ?? '0.0000'}.
      </p>
      <div className="field">
        <label>
          <input type="checkbox" checked={rule.enabled} onChange={(e) => onChange({ ...rule, enabled: e.target.checked })} />{' '}
          On
        </label>
      </div>
      <div className="field">
        <label htmlFor={`${id}-scope`}>Studios {label} reads for</label>
        <select
          id={`${id}-scope`}
          value={rule.scope}
          onChange={(e) => onChange({ ...rule, scope: e.target.value as PlatformReaderScope })}
        >
          {(Object.keys(scopeLabels) as PlatformReaderScope[]).map((scope) => (
            <option key={scope} value={scope}>
              {scopeLabels[scope]}
            </option>
          ))}
        </select>
      </div>
      {rule.scope === 'CHOSEN' && (
        <fieldset>
          <legend>Chosen studios for {label}</legend>
          {studios.map((studio) => (
            <label key={studio.id}>
              <input
                type="checkbox"
                checked={rule.studioIds.includes(studio.id)}
                onChange={(e) =>
                  onChange({
                    ...rule,
                    studioIds: e.target.checked
                      ? [...rule.studioIds, studio.id]
                      : rule.studioIds.filter((chosen) => chosen !== studio.id),
                  })
                }
              />{' '}
              {studio.name}
            </label>
          ))}
        </fieldset>
      )}
      <div className="field">
        <label htmlFor={`${id}-studio-pages`}>Pages per studio per month</label>
        <input
          id={`${id}-studio-pages`}
          type="number"
          min={0}
          max={1000000}
          required
          value={rule.studioMonthlyPages}
          onChange={(e) => onChange({ ...rule, studioMonthlyPages: Number(e.target.value) })}
        />
      </div>
      <div className="field">
        <label htmlFor={`${id}-platform-pages`}>Pages for every studio together per month</label>
        <input
          id={`${id}-platform-pages`}
          type="number"
          min={0}
          max={1000000}
          required
          value={rule.platformMonthlyPages}
          onChange={(e) => onChange({ ...rule, platformMonthlyPages: Number(e.target.value) })}
        />
      </div>
      {usage && usage.studios.length > 0 && (
        <table>
          <caption>{label} this month, by studio</caption>
          <thead>
            <tr>
              <th scope="col">Studio</th>
              <th scope="col">Reads</th>
              <th scope="col">Pages</th>
              <th scope="col">Spend</th>
            </tr>
          </thead>
          <tbody>
            {usage.studios.map((use) => (
              <tr key={use.studioId}>
                <td>{use.studioName ?? use.studioId}</td>
                <td>{String(use.reads)}</td>
                <td>{String(use.pages)}</td>
                <td>${use.costUsd}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </fieldset>
  );
}

function OrderFields({
  order,
  studios,
  onChange,
  onRemove,
}: {
  order: StudioReaderOrder;
  studios: AdminStudio[];
  onChange: (order: StudioReaderOrder) => void;
  onRemove: () => void;
}) {
  const id = useId();
  const setPosition = (position: number, value: string) => {
    const chosen = [...order.readers];
    if (value === '') {
      chosen.splice(position);
    } else {
      chosen[position] = value as InvoiceReader;
    }
    onChange({ ...order, readers: chosen.filter((reader, at) => chosen.indexOf(reader) === at) });
  };
  return (
    <div className="row">
      <div className="field">
        <label htmlFor={`${id}-studio`}>Studio</label>
        <select id={`${id}-studio`} value={order.studioId} onChange={(e) => onChange({ ...order, studioId: e.target.value })}>
          {studios.map((studio) => (
            <option key={studio.id} value={studio.id}>
              {studio.name}
            </option>
          ))}
        </select>
      </div>
      {[0, 1, 2].map((position) => (
        <div className="field" key={position}>
          <label htmlFor={`${id}-reader-${String(position)}`}>{position === 0 ? 'First' : 'Then'}</label>
          <select
            id={`${id}-reader-${String(position)}`}
            value={order.readers[position] ?? ''}
            disabled={position > order.readers.length}
            onChange={(e) => setPosition(position, e.target.value)}
          >
            {position > 0 && <option value="">Nothing else</option>}
            {allReaders.map((reader) => (
              <option key={reader} value={reader}>
                {readerLabels[reader]}
              </option>
            ))}
          </select>
        </div>
      ))}
      <div>
        <button type="button" onClick={onRemove}>
          Remove
        </button>
      </div>
    </div>
  );
}
