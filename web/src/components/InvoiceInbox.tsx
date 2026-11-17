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
import { type SubmitEvent, useEffect, useId, useRef, useState } from 'react';

import { CompareReaders, readerLabels } from './CompareReaders';
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

const waiting = (status: InvoiceStatus | undefined) => status === 'UPLOADING' || status === 'READING';

/**
 * The supplies inbox: upload a supplier's invoice, check the draft next to the file, and confirm it
 * into stock. Staff only. Nothing reaches stock until someone confirms. "Compare readers" is the
 * owner's, because it spends the studio's key and the platform's pages.
 */
export function InvoiceInbox({ studioId, canCompare }: { studioId: string; canCompare: boolean }) {
  const id = useId();
  const invoices = useQuery({
    ...listInvoicesOptions({ path: { studioId } }),
    // Keep the badges current while anything is still being read
    refetchInterval: (query) => (query.state.data?.items.some((invoice) => waiting(invoice.status)) ? 3000 : false),
  });
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
      {open && (
        <InvoiceReview
          key={open}
          studioId={studioId}
          invoiceId={open}
          canCompare={canCompare}
          onClosed={() => setOpen(null)}
        />
      )}
    </section>
  );
}

function UploadForm({ studioId, onUploaded }: { studioId: string; onUploaded: (invoiceId: string) => void }) {
  const id = useId();
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  // The whole sequence, the file's own upload included, so the button can't be pressed twice
  const [uploading, setUploading] = useState(false);
  const start = useMutation(startInvoiceUploadMutation());
  const complete = useMutation(completeInvoiceUploadMutation());
  const busy = uploading || start.isPending || complete.isPending;

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setFailure(null);
    const fileType = file ? fileTypes[file.type] : undefined;
    if (!file || !fileType) {
      setFailure('Choose a PDF, JPEG or PNG.');
      return;
    }
    setUploading(true);
    try {
      const upload = await start.mutateAsync({ path: { studioId }, body: { fileType } });
      if (file.size > upload.maxBytes) {
        setFailure(`The file is larger than ${String(Math.floor(upload.maxBytes / 1024 / 1024))} MB.`);
        return;
      }
      let stored = false;
      try {
        const put = await fetch(upload.uploadUrl, {
          method: 'PUT',
          headers: { 'x-ms-blob-type': 'BlockBlob', 'Content-Type': file.type },
          body: file,
        });
        stored = put.ok;
      } catch {
        stored = false;
      }
      if (!stored) {
        setFailure('The upload didn’t go through. Check the connection and try again.');
        return;
      }
      await complete.mutateAsync({ path: { studioId, invoiceId: upload.invoiceId } });
      setFile(null);
      onUploaded(upload.invoiceId);
    } catch {
      // The mutations show their own errors below
    } finally {
      setUploading(false);
      await queryClient.invalidateQueries({ queryKey: listInvoicesOptions({ path: { studioId } }).queryKey });
    }
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

function InvoiceReview({
  studioId,
  invoiceId,
  canCompare,
  onClosed,
}: {
  studioId: string;
  invoiceId: string;
  canCompare: boolean;
  onClosed: () => void;
}) {
  const queryClient = useQueryClient();
  const invoice = useQuery({
    ...getInvoiceOptions({ path: { studioId, invoiceId } }),
    refetchInterval: (query) => (waiting(query.state.data?.status) ? 2000 : false),
  });
  const supplies = useQuery(listSuppliesOptions({ path: { studioId } }));
  const discard = useMutation({
    ...discardInvoiceMutation(),
    onSuccess: async () => {
      await queryClient.invalidateQueries();
      onClosed();
    },
  });

  if (invoice.isError) {
    return <ErrorMessage error={invoice.error} />;
  }
  if (!invoice.data) {
    return <p>Loading the invoice…</p>;
  }
  if (waiting(invoice.data.status)) {
    // A failed upload stays "uploading" for good, so it can be thrown away from here
    return (
      <div className="row">
        <p aria-live="polite">
          {invoice.data.status === 'UPLOADING' ? 'Waiting for the file…' : 'Reading the invoice…'}
        </p>
        <button type="button" className="danger" onClick={() => discard.mutate({ path: { studioId, invoiceId } })}>
          Discard
        </button>
        {discard.isError && <ErrorMessage error={discard.error} />}
      </div>
    );
  }
  return (
    <div className="stack">
      <div className="invoice-review">
        <InvoiceFile invoice={invoice.data} />
        <DraftForm
          key={invoice.data.version}
          studioId={studioId}
          invoice={invoice.data}
          items={supplies.data ?? []}
          onClosed={onClosed}
        />
      </div>
      {canCompare && <CompareReaders studioId={studioId} invoiceId={invoiceId} />}
    </div>
  );
}

/** The invoice itself, beside its draft: a photo turned upright by hand if need be, or the PDF. */
function InvoiceFile({ invoice }: { invoice: Invoice }) {
  const [turns, setTurns] = useState(0);
  const url = invoice.fileUrl;
  if (!url) {
    return null;
  }
  return (
    <div className="stack">
      <div className="invoice-file">
        {invoice.fileType === 'PDF' ? (
          <iframe src={url} title="The invoice" />
        ) : (
          <TurnedImage url={url} turns={turns} />
        )}
      </div>
      <div className="row">
        {invoice.fileType !== 'PDF' && (
          <button type="button" onClick={() => setTurns((current) => (current + 1) % 4)}>
            Turn the photo
          </button>
        )}
        <a href={url} target="_blank" rel="noreferrer">
          Open the file
        </a>
      </div>
    </div>
  );
}

/** A photo turned by quarter turns: drawn onto a canvas, so the page around it makes room. */
function TurnedImage({ url, turns }: { url: string; turns: number }) {
  const canvas = useRef<HTMLCanvasElement>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    const image = new Image();
    image.onload = () => {
      const target = canvas.current;
      const context = target?.getContext('2d');
      if (!target || !context) {
        return;
      }
      const sideways = turns % 2 === 1;
      target.width = sideways ? image.naturalHeight : image.naturalWidth;
      target.height = sideways ? image.naturalWidth : image.naturalHeight;
      context.translate(target.width / 2, target.height / 2);
      context.rotate((turns * Math.PI) / 2);
      context.drawImage(image, -image.naturalWidth / 2, -image.naturalHeight / 2);
    };
    image.onerror = () => setFailed(true);
    image.src = url;
  }, [url, turns]);

  if (failed) {
    return <p className="warning">The photo couldn&apos;t be shown here. Open the file instead.</p>;
  }
  return <canvas ref={canvas} role="img" aria-label="The invoice" style={{ width: '100%', height: 'auto' }} />;
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

/** The unit price the line's amount and quantity imply, when the invoice prints only the amount. */
function impliedUnitPrice(line: EditableLine): string | undefined {
  const quantity = Number(line.quantity);
  const total = Number(line.lineTotal);
  if (!line.quantity || !line.lineTotal || !Number.isFinite(quantity) || !Number.isFinite(total) || quantity === 0) {
    return undefined;
  }
  return `≈ ${String(Math.round((total / quantity) * 10000) / 10000)}`;
}

const headerFields = [
  { key: 'invoiceDate', label: 'Date', type: 'date' },
  { key: 'subtotal', label: 'Subtotal', type: 'decimal' },
  { key: 'shipping', label: 'Shipping', type: 'decimal' },
  { key: 'tax', label: 'Tax', type: 'decimal' },
  { key: 'total', label: 'Total', type: 'decimal' },
] as const;

type HeaderKey = (typeof headerFields)[number]['key'];

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
  const [header, setHeader] = useState<Record<HeaderKey, string>>({
    invoiceDate: invoice.invoiceDate ?? '',
    subtotal: invoice.subtotal ?? '',
    shipping: invoice.shipping ?? '',
    tax: invoice.tax ?? '',
    total: invoice.total ?? '',
  });
  const [lines, setLines] = useState<EditableLine[]>(invoice.lines.map(editable));
  const refresh = () => queryClient.invalidateQueries();
  const save = useMutation({ ...saveInvoiceDraftMutation(), onSuccess: refresh });
  // Confirming saves what's on screen first, without refreshing in between, then confirms that
  const saveFirst = useMutation(saveInvoiceDraftMutation());
  const confirm = useMutation({ ...confirmInvoiceMutation(), onSuccess: refresh });
  const discard = useMutation({
    ...discardInvoiceMutation(),
    onSuccess: async () => {
      await refresh();
      onClosed();
    },
  });
  const confirmed = invoice.status === 'CONFIRMED';
  const busy = save.isPending || saveFirst.isPending || confirm.isPending;
  const change = (index: number, patch: Partial<EditableLine>) =>
    setLines((current) => current.map((line, i) => (i === index ? { ...line, ...patch } : line)));
  const optional = (value: string | undefined) => (value && value.trim() ? value.trim() : undefined);
  const path = { studioId, invoiceId: invoice.id };

  const draft = () => ({
    vendorName: optional(vendorName),
    invoiceNumber: optional(invoiceNumber),
    invoiceDate: optional(header.invoiceDate),
    subtotal: optional(header.subtotal),
    shipping: optional(header.shipping),
    tax: optional(header.tax),
    total: optional(header.total),
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
  });

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    save.mutate({ path, body: draft() });
  }

  async function saveAndConfirm() {
    try {
      const saved = await saveFirst.mutateAsync({ path, body: draft() });
      confirm.mutate({ path, body: { expectedVersion: saved.version } });
    } catch {
      await refresh();
    }
  }

  return (
    <form className="stack" onSubmit={submit} aria-labelledby={`${id}-title`}>
      <h3 id={`${id}-title`}>
        {invoice.vendorName ?? 'Invoice'} <span className="badge">{statusLabels[invoice.status]}</span>
      </h3>
      {invoice.reader && <p className="badge">Read by {readerLabels[invoice.reader]}</p>}
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
      </div>
      <div className="row">
        {headerFields.map((field) => (
          <div className="field" key={field.key}>
            <label htmlFor={`${id}-${field.key}`}>{field.label}</label>
            <input
              id={`${id}-${field.key}`}
              type={field.type === 'date' ? 'date' : 'text'}
              inputMode={field.type === 'date' ? undefined : 'decimal'}
              disabled={confirmed}
              value={header[field.key]}
              onChange={(e) => setHeader((current) => ({ ...current, [field.key]: e.target.value.trim() }))}
            />
          </div>
        ))}
      </div>
      <div className="invoice-lines">
        <table className="table">
          <thead>
            <tr>
              <th>Line</th>
              <th>Quantity</th>
              <th>Unit</th>
              <th>Unit price</th>
              <th>Total</th>
              <th>Receive into</th>
              {!confirmed && <th />}
            </tr>
          </thead>
          <tbody>
            {lines.map((line, index) => {
              const n = String(index + 1);
              return (
                <tr key={index}>
                  <td>
                    <input
                      aria-label={`Line ${n} description`}
                      required
                      maxLength={200}
                      disabled={confirmed}
                      value={line.description}
                      onChange={(e) => change(index, { description: e.target.value })}
                    />
                  </td>
                  <td>
                    <input
                      aria-label={`Line ${n} quantity`}
                      inputMode="decimal"
                      disabled={confirmed}
                      value={line.quantity ?? ''}
                      onChange={(e) => change(index, { quantity: e.target.value.trim() })}
                    />
                  </td>
                  <td>
                    <input
                      aria-label={`Line ${n} unit`}
                      maxLength={30}
                      disabled={confirmed}
                      value={line.unit ?? ''}
                      onChange={(e) => change(index, { unit: e.target.value })}
                    />
                  </td>
                  <td>
                    <input
                      aria-label={`Line ${n} unit price`}
                      inputMode="decimal"
                      disabled={confirmed}
                      placeholder={impliedUnitPrice(line)}
                      title={line.unitPrice ? undefined : 'Not printed on the invoice; the grey figure is the amount divided by the quantity.'}
                      value={line.unitPrice ?? ''}
                      onChange={(e) => change(index, { unitPrice: e.target.value.trim() })}
                    />
                  </td>
                  <td>
                    <input
                      aria-label={`Line ${n} total`}
                      inputMode="decimal"
                      disabled={confirmed}
                      value={line.lineTotal ?? ''}
                      onChange={(e) => change(index, { lineTotal: e.target.value.trim() })}
                    />
                  </td>
                  <td>
                    <select
                      aria-label={`Line ${n} item`}
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
                  {!confirmed && (
                    <td>
                      <button
                        type="button"
                        aria-label={`Remove line ${n}`}
                        onClick={() => setLines((current) => current.filter((_, i) => i !== index))}
                      >
                        Remove
                      </button>
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {!confirmed && (
        <div className="row">
          <button type="button" onClick={() => setLines((current) => [...current, { description: '' }])}>
            Add a line
          </button>
          <button type="submit" disabled={busy}>
            Save changes
          </button>
          <button type="button" className="primary" disabled={busy} onClick={() => void saveAndConfirm()}>
            Confirm and receive stock
          </button>
          <button type="button" className="danger" onClick={() => discard.mutate({ path })}>
            Discard
          </button>
        </div>
      )}
      {[save, saveFirst, confirm, discard].map((m, i) => m.isError && <ErrorMessage key={i} error={m.error} />)}
    </form>
  );
}
