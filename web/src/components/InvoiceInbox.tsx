import {
  completeInvoiceUploadMutation,
  confirmInvoiceMutation,
  discardInvoiceMutation,
  getInvoiceOptions,
  type Invoice,
  type InvoiceFileType,
  type InvoiceLine,
  type InvoiceStatus,
  listInvoicesOptions,
  listSuppliesOptions,
  saveInvoiceDraftMutation,
  startInvoiceUploadMutation,
  type SupplyItem,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { type SubmitEvent, useId, useState } from 'react';

import { ErrorMessage } from './ErrorMessage';

const fileTypes: Record<string, InvoiceFileType> = {
  'application/pdf': 'PDF',
  'image/jpeg': 'JPEG',
  'image/png': 'PNG',
};

const statusLabels: Record<InvoiceStatus, string> = {
  UPLOADING: 'uploading',
  READING: 'being read',
  READY: 'ready to check',
  NEEDS_LOOK: 'needs a look',
  CONFIRMED: 'confirmed',
};

/**
 * The supplies inbox: upload a supplier's invoice, check the draft next to the file, and confirm it
 * into stock. Staff only. Nothing reaches stock until someone confirms.
 */
export function InvoiceInbox({ studioId }: { studioId: string }) {
  const id = useId();
  const invoices = useQuery(listInvoicesOptions({ path: { studioId } }));
  const [open, setOpen] = useState<string | null>(null);

  return (
    <section className="card stack" aria-labelledby={`${id}-title`}>
      <h2 id={`${id}-title`}>Invoices</h2>
      <p>Upload a supplier&apos;s invoice to have its lines read for you. Check them, then confirm to receive the stock.</p>
      <UploadForm studioId={studioId} onUploaded={setOpen} />
      {invoices.isError && <ErrorMessage error={invoices.error} />}
      {invoices.isSuccess && invoices.data.items.length > 0 && (
        <ul className="studio-list">
          {invoices.data.items.map((invoice) => (
            <li key={invoice.id}>
              <span>
                {invoice.vendorName ?? 'Unknown supplier'}
                {invoice.invoiceNumber && ` · ${invoice.invoiceNumber}`}
                {invoice.total && ` · ${invoice.total}`} <span className="badge">{statusLabels[invoice.status]}</span>
              </span>
              <button type="button" onClick={() => setOpen(invoice.id)}>
                Open
              </button>
            </li>
          ))}
        </ul>
      )}
      {open && <InvoiceReview key={open} studioId={studioId} invoiceId={open} onClosed={() => setOpen(null)} />}
    </section>
  );
}

function UploadForm({ studioId, onUploaded }: { studioId: string; onUploaded: (invoiceId: string) => void }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  const start = useMutation(startInvoiceUploadMutation());
  const complete = useMutation(completeInvoiceUploadMutation());
  const busy = start.isPending || complete.isPending;

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setFailure(null);
    const fileType = file ? fileTypes[file.type] : undefined;
    if (!file || !fileType) {
      setFailure('Choose a PDF, JPEG or PNG.');
      return;
    }
    const upload = await start.mutateAsync({ path: { studioId }, body: { fileType } });
    if (file.size > upload.maxBytes) {
      setFailure(`The file is larger than ${String(Math.floor(upload.maxBytes / 1024 / 1024))} MB.`);
      return;
    }
    const put = await fetch(upload.uploadUrl, {
      method: 'PUT',
      headers: { 'x-ms-blob-type': 'BlockBlob', 'Content-Type': file.type },
      body: file,
    });
    if (!put.ok) {
      setFailure('The upload didn’t go through. Try again.');
      return;
    }
    await complete.mutateAsync({ path: { studioId, invoiceId: upload.invoiceId } });
    await queryClient.invalidateQueries({ queryKey: listInvoicesOptions({ path: { studioId } }).queryKey });
    setFile(null);
    onUploaded(upload.invoiceId);
  }

  return (
    <form className="row" onSubmit={(event) => void submit(event)}>
      <div className="field">
        <label htmlFor={`${id}-file`}>Invoice file</label>
        <input
          id={`${id}-file`}
          type="file"
          accept="application/pdf,image/jpeg,image/png"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
        />
      </div>
      <div>
        <button type="submit" className="primary" disabled={busy || !file}>
          {busy ? 'Uploading…' : 'Upload invoice'}
        </button>
      </div>
      {failure && <p role="alert">{failure}</p>}
      {start.isError && <ErrorMessage error={start.error} />}
      {complete.isError && <ErrorMessage error={complete.error} />}
    </form>
  );
}

function InvoiceReview({ studioId, invoiceId, onClosed }: { studioId: string; invoiceId: string; onClosed: () => void }) {
  const invoice = useQuery({
    ...getInvoiceOptions({ path: { studioId, invoiceId } }),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      return status === 'UPLOADING' || status === 'READING' ? 2000 : false;
    },
  });
  const supplies = useQuery(listSuppliesOptions({ path: { studioId } }));

  if (invoice.isError) {
    return <ErrorMessage error={invoice.error} />;
  }
  if (!invoice.data) {
    return <p>Loading the invoice…</p>;
  }
  if (invoice.data.status === 'UPLOADING' || invoice.data.status === 'READING') {
    return <p aria-live="polite">Reading the invoice…</p>;
  }
  return (
    <DraftForm
      key={invoice.data.version}
      studioId={studioId}
      invoice={invoice.data}
      items={supplies.data ?? []}
      onClosed={onClosed}
    />
  );
}

type EditableLine = Omit<InvoiceLine, 'matchSource'>;

function editable(line: InvoiceLine): EditableLine {
  return {
    description: line.description,
    sku: line.sku,
    quantity: line.quantity,
    unit: line.unit,
    unitPrice: line.unitPrice,
    lineTotal: line.lineTotal,
    supplyItemId: line.supplyItemId,
  };
}

function DraftForm({
  studioId,
  invoice,
  items,
  onClosed,
}: {
  studioId: string;
  invoice: Invoice;
  items: SupplyItem[];
  onClosed: () => void;
}) {
  const id = useId();
  const queryClient = useQueryClient();
  const [vendorName, setVendorName] = useState(invoice.vendorName ?? '');
  const [invoiceNumber, setInvoiceNumber] = useState(invoice.invoiceNumber ?? '');
  const [total, setTotal] = useState(invoice.total ?? '');
  const [lines, setLines] = useState<EditableLine[]>(invoice.lines.map(editable));
  const refresh = () => queryClient.invalidateQueries();
  const save = useMutation({ ...saveInvoiceDraftMutation(), onSuccess: refresh });
  const confirm = useMutation({ ...confirmInvoiceMutation(), onSuccess: refresh });
  const discard = useMutation({
    ...discardInvoiceMutation(),
    onSuccess: async () => {
      await refresh();
      onClosed();
    },
  });
  const confirmed = invoice.status === 'CONFIRMED';
  const change = (index: number, patch: Partial<EditableLine>) =>
    setLines((current) => current.map((line, i) => (i === index ? { ...line, ...patch } : line)));
  const optional = (value: string | undefined) => (value && value.trim() ? value.trim() : undefined);

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    save.mutate({
      path: { studioId, invoiceId: invoice.id },
      body: {
        vendorName: optional(vendorName),
        invoiceNumber: optional(invoiceNumber),
        invoiceDate: invoice.invoiceDate,
        subtotal: invoice.subtotal,
        shipping: invoice.shipping,
        tax: invoice.tax,
        total: optional(total),
        lines: lines.map((line) => ({
          description: line.description,
          sku: optional(line.sku),
          quantity: optional(line.quantity),
          unit: optional(line.unit),
          unitPrice: optional(line.unitPrice),
          lineTotal: optional(line.lineTotal),
          supplyItemId: optional(line.supplyItemId),
        })),
        expectedVersion: invoice.version,
      },
    });
  }

  return (
    <form className="stack" onSubmit={submit} aria-labelledby={`${id}-title`}>
      <h3 id={`${id}-title`}>
        {invoice.vendorName ?? 'Invoice'} <span className="badge">{statusLabels[invoice.status]}</span>
      </h3>
      {invoice.fileUrl && (
        <p>
          <a href={invoice.fileUrl} target="_blank" rel="noreferrer">
            Open the file
          </a>
          {invoice.reader === 'CLAUDE' && <span className="badge"> · read by Claude</span>}
        </p>
      )}
      {invoice.readNote && <p className="warning">{invoice.readNote}</p>}
      {invoice.problems.length > 0 && (
        <ul aria-label="Problems">
          {invoice.problems.map((problem) => (
            <li key={problem}>{problem}</li>
          ))}
        </ul>
      )}
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-vendor`}>Supplier</label>
          <input id={`${id}-vendor`} maxLength={120} disabled={confirmed} value={vendorName} onChange={(e) => setVendorName(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-number`}>Invoice number</label>
          <input id={`${id}-number`} maxLength={60} disabled={confirmed} value={invoiceNumber} onChange={(e) => setInvoiceNumber(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-total`}>Total</label>
          <input id={`${id}-total`} inputMode="decimal" disabled={confirmed} value={total} onChange={(e) => setTotal(e.target.value.trim())} />
        </div>
      </div>
      <table>
        <thead>
          <tr>
            <th>Line</th>
            <th>Quantity</th>
            <th>Unit price</th>
            <th>Total</th>
            <th>Receive into</th>
          </tr>
        </thead>
        <tbody>
          {lines.map((line, index) => (
            <tr key={index}>
              <td>
                <input
                  aria-label={`Line ${String(index + 1)} description`}
                  required
                  maxLength={200}
                  disabled={confirmed}
                  value={line.description}
                  onChange={(e) => change(index, { description: e.target.value })}
                />
              </td>
              <td>
                <input
                  aria-label={`Line ${String(index + 1)} quantity`}
                  inputMode="decimal"
                  disabled={confirmed}
                  value={line.quantity ?? ''}
                  onChange={(e) => change(index, { quantity: e.target.value.trim() })}
                />
              </td>
              <td>
                <input
                  aria-label={`Line ${String(index + 1)} unit price`}
                  inputMode="decimal"
                  disabled={confirmed}
                  value={line.unitPrice ?? ''}
                  onChange={(e) => change(index, { unitPrice: e.target.value.trim() })}
                />
              </td>
              <td>
                <input
                  aria-label={`Line ${String(index + 1)} total`}
                  inputMode="decimal"
                  disabled={confirmed}
                  value={line.lineTotal ?? ''}
                  onChange={(e) => change(index, { lineTotal: e.target.value.trim() })}
                />
              </td>
              <td>
                <select
                  aria-label={`Line ${String(index + 1)} item`}
                  disabled={confirmed}
                  value={line.supplyItemId ?? ''}
                  onChange={(e) => change(index, { supplyItemId: e.target.value || undefined })}
                >
                  <option value="">Not stock</option>
                  {items.map((item) => (
                    <option key={item.id} value={item.id}>
                      {item.name}
                    </option>
                  ))}
                </select>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {!confirmed && (
        <div className="row">
          <button type="button" onClick={() => setLines((current) => [...current, { description: '' }])}>
            Add a line
          </button>
          <button type="submit" disabled={save.isPending}>
            Save changes
          </button>
          <button
            type="button"
            className="primary"
            disabled={confirm.isPending}
            onClick={() => confirm.mutate({ path: { studioId, invoiceId: invoice.id }, body: { expectedVersion: invoice.version } })}
          >
            Confirm and receive stock
          </button>
          <button type="button" className="danger" onClick={() => discard.mutate({ path: { studioId, invoiceId: invoice.id } })}>
            Discard
          </button>
        </div>
      )}
      {[save, confirm, discard].map((m, i) => m.isError && <ErrorMessage key={i} error={m.error} />)}
    </form>
  );
}
