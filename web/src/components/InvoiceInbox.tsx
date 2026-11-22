import {
  completeInvoiceUploadMutation,
  confirmInvoiceMutation,
  createSupplyMutation,
  discardInvoiceMutation,
  exportInvoiceEval,
  getInvoiceOptions,
  type Invoice,
  type InvoiceDocumentType,
  type InvoiceFileType,
  type InvoiceLine,
  type InvoiceLineKind,
  type InvoicePayment,
  type InvoiceStatus,
  listInvoicesOptions,
  listSuppliesOptions,
  listSuppliesQueryKey,
  saveInvoiceDraftMutation,
  startInvoiceUploadMutation,
  type SupplyCategory,
  type SupplyItem,
  useCan,
} from '@studiomanager/api-client';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { type KeyboardEvent, type SubmitEvent, useEffect, useId, useRef, useState } from 'react';

import { CompareReaders, documentTypeLabels, readerLabels } from './CompareReaders';
import { ErrorMessage, errorText } from './ErrorMessage';

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

const documentTypes: InvoiceDocumentType[] = [
  'INVOICE',
  'ORDER_FORM',
  'RECEIPT',
  'PACKING_LIST',
  'CREDIT_MEMO',
  'STATEMENT',
  'QUOTE',
  'OTHER',
];

const lineKinds: InvoiceLineKind[] = ['ITEM', 'SHIPPING', 'FEE', 'DISCOUNT', 'DEPOSIT', 'OTHER'];

const lineKindLabels: Record<InvoiceLineKind, string> = {
  ITEM: 'Item',
  SHIPPING: 'Shipping',
  FEE: 'Fee',
  DISCOUNT: 'Discount',
  DEPOSIT: 'Deposit',
  OTHER: 'Other',
};

const supplyCategories: SupplyCategory[] = [
  'CLAY',
  'GLAZE',
  'GLAZE_MATERIAL',
  'TOOL',
  'KILN_SUPPLY',
  'CLEANING',
  'OTHER',
];

const supplyCategoryLabels: Record<SupplyCategory, string> = {
  CLAY: 'Clay',
  GLAZE: 'Glaze',
  GLAZE_MATERIAL: 'Glaze material',
  TOOL: 'Tool',
  KILN_SUPPLY: 'Kiln supply',
  CLEANING: 'Cleaning',
  OTHER: 'Other',
};

const waiting = (status: InvoiceStatus | undefined) => status === 'UPLOADING' || status === 'READING';

/** Guesses a supply category from an invoice line's description to prefill "+ New item". */
function inferSupplyCategory(description: string): SupplyCategory {
  const lower = description.toLowerCase();
  if (
    /\b(feldspar|kaolin|silica|oxide|carbonate|whiting|dolomite|talc|epk|nepheline|gerstley|frit|bentonite|zircon|rutile|cobalt|copper|iron|tin|zinc|spodumene|wollastonite|fireclay|ball clay|custer)\b/.test(
      lower,
    )
  ) {
    return 'GLAZE_MATERIAL';
  }
  if (/\b(clay|b-mix|porcelain|stoneware|buff|terracotta|earthenware|grog|redart)\b/.test(lower)) {
    return 'CLAY';
  }
  if (/\b(glaze|underglaze|slip|engobe|celadon|shino|tenmoku|stain|wax resist)\b/.test(lower)) {
    return 'GLAZE';
  }
  if (/\b(kiln|shelf|shelves|post|stilt|cone|pyrometric|wash|element|thermocouple|brick|peep)\b/.test(lower)) {
    return 'KILN_SUPPLY';
  }
  if (/\b(sponge|rib|wire|needle|trim|brush|chamois|bat|cutter|loop|caliper|bucket|sieve|tool)\b/.test(lower)) {
    return 'TOOL';
  }
  if (/\b(soap|mop|towel|cleaner|wipe|detergent|bleach)\b/.test(lower)) {
    return 'CLEANING';
  }
  return 'OTHER';
}

function initialTurns(rotation: number | undefined): number {
  if (rotation === 90 || rotation === 180 || rotation === 270) {
    return rotation / 90;
  }
  return 0;
}

/**
 * The supplies inbox: upload a supplier's invoice, check the draft next to the file, and confirm it
 * into stock. Staff only. Nothing reaches stock until someone confirms. "Compare readers" is the
 * owner's, because it spends the studio's key and the platform's pages.
 */
export function InvoiceInbox({ studioId, canCompare }: { studioId: string; canCompare: boolean }) {
  const id = useId();
  const invoices = useQuery({
    ...listInvoicesOptions({ path: { studioId } }),
    // Keep the badges current while anything is being read. Not for "uploading": an abandoned upload
    // stays that way, and polling for it would keep the API awake for good
    refetchInterval: (query) =>
      query.state.data?.items.some((invoice) => invoice.status === 'READING') ? 3000 : false,
  });
  const [openSelection, setOpenSelection] = useState<{ studioId: string; invoiceId: string } | null>(null);
  const open = openSelection?.studioId === studioId ? openSelection.invoiceId : null;
  const setOpen = (invoiceId: string | null) => setOpenSelection(invoiceId ? { studioId, invoiceId } : null);

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
  const fileInput = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [failure, setFailure] = useState<string | null>(null);
  // The whole sequence, the file's own upload included, so the button can't be pressed twice
  const [uploading, setUploading] = useState(false);
  const start = useMutation(startInvoiceUploadMutation());
  const complete = useMutation(completeInvoiceUploadMutation());
  const abandon = useMutation(discardInvoiceMutation());
  const busy = uploading || start.isPending || complete.isPending;
  // A file that never arrives leaves nothing behind in the list
  const throwAway = (invoiceId: string) => abandon.mutateAsync({ path: { studioId, invoiceId } }).catch(() => undefined);

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    setFailure(null);
    start.reset();
    complete.reset();
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
        await throwAway(upload.invoiceId);
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
        await throwAway(upload.invoiceId);
        return;
      }
      await complete.mutateAsync({ path: { studioId, invoiceId: upload.invoiceId } });
      setFile(null);
      if (fileInput.current) {
        fileInput.current.value = '';
      }
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
          ref={fileInput}
          id={`${id}-file`}
          type="file"
          accept="application/pdf,image/jpeg,image/png"
          onChange={(e) => {
            setFailure(null);
            start.reset();
            complete.reset();
            setFile(e.target.files?.[0] ?? null);
          }}
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
    refetchOnWindowFocus: (query) => waiting(query.state.data?.status),
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
    // A failed upload stays "uploading" for good, so it can be thrown away from here. Not while it's
    // being read: the reader's pages are recorded against the invoice when it finishes
    return (
      <div className="row">
        <p aria-live="polite">
          {invoice.data.status === 'UPLOADING' ? 'Waiting for the file…' : 'Reading the invoice…'}
        </p>
        {invoice.data.status === 'UPLOADING' && (
          <button
            type="button"
            className="danger"
            disabled={discard.isPending}
            onClick={() => discard.mutate({ path: { studioId, invoiceId } })}
          >
            {discard.isPending ? 'Discarding…' : 'Discard'}
          </button>
        )}
        {discard.isError && <ErrorMessage error={discard.error} />}
      </div>
    );
  }
  return (
    <div className="stack">
      <div className="invoice-review">
        <InvoiceFile key={`${invoice.data.id}-${String(invoice.data.rotation ?? 0)}`} invoice={invoice.data} />
        <DraftForm
          key={invoice.data.id}
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

/** The invoice itself, beside its draft: a photo starts upright from `rotation` and can be turned, or the PDF. */
function InvoiceFile({ invoice }: { invoice: Invoice }) {
  const [turns, setTurns] = useState(() => initialTurns(invoice.rotation));
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
  const [loaded, setLoaded] = useState<{ url: string; image: HTMLImageElement } | null>(null);
  const [failedUrl, setFailedUrl] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const image = new Image();
    image.onload = () => {
      if (!cancelled) {
        setFailedUrl((current) => (current === url ? null : current));
        setLoaded({ url, image });
      }
    };
    image.onerror = () => {
      if (!cancelled) {
        setFailedUrl(url);
      }
    };
    image.src = url;
    return () => {
      cancelled = true;
      image.onload = null;
      image.onerror = null;
    };
  }, [url]);

  useEffect(() => {
    if (!loaded || loaded.url !== url) {
      return;
    }
    const target = canvas.current;
    const context = target?.getContext('2d');
    if (!target || !context) {
      return;
    }
    const { image } = loaded;
    const sideways = turns % 2 === 1;
    target.width = sideways ? image.naturalHeight : image.naturalWidth;
    target.height = sideways ? image.naturalWidth : image.naturalHeight;
    context.setTransform(1, 0, 0, 1, 0, 0);
    context.clearRect(0, 0, target.width, target.height);
    context.translate(target.width / 2, target.height / 2);
    context.rotate((turns * Math.PI) / 2);
    context.drawImage(image, -image.naturalWidth / 2, -image.naturalHeight / 2);
  }, [loaded, url, turns]);

  if (failedUrl === url) {
    return <p className="warning">The photo couldn&apos;t be shown here. Open the file instead.</p>;
  }
  return (
    <canvas
      ref={canvas}
      role="img"
      aria-label="The invoice"
      data-rotation={String((turns * 90) % 360)}
      style={{ width: '100%', height: 'auto' }}
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
    kind: line.kind ?? 'ITEM',
    serialNumbers: line.serialNumbers,
    unclear: line.unclear,
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

/** The word a unit counts in: "box" for "50 lb box" or "boxes", "roll" for "Rolls". */
function countingWord(unit: string | undefined): string {
  const words = (unit ?? '').toLowerCase().match(/[a-z]+/g) ?? [];
  const last = words.at(-1) ?? '';
  if (/(x|ch|sh|ss)es$/.test(last)) {
    return last.slice(0, -2);
  }
  return last.length > 2 && last.endsWith('s') && !last.endsWith('ss') ? last.slice(0, -1) : last;
}

/**
 * What confirming adds to the item, in the item's own unit, and a warning when the invoice counts
 * in something else ("2 bag" into an item counted in "50 lb box"), since the quantity is received
 * as it stands.
 */
function ReceivedAs({ line, item }: { line: EditableLine; item: SupplyItem | undefined }) {
  if (!item) {
    return null;
  }
  const quantity = line.quantity && line.quantity.trim() ? line.quantity.trim() : '?';
  const invoiceWord = countingWord(line.unit);
  const differs = invoiceWord !== '' && invoiceWord !== countingWord(item.unit);
  return differs ? (
    <p className="error">
      The invoice counts in {line.unit}; {item.name} is counted in {item.unit}. Put the quantity in {item.unit} and
      change the unit to match before confirming.
    </p>
  ) : (
    <p className="muted">
      Receives {quantity} {item.unit}
    </p>
  );
}

function InlineNewItemForm({
  studioId,
  lineNumber,
  line,
  vendorName,
  onCreated,
  onCancel,
}: {
  studioId: string;
  lineNumber: string;
  line: EditableLine;
  vendorName: string;
  onCreated: (item: SupplyItem) => void;
  onCancel: () => void;
}) {
  const id = useId();
  const queryClient = useQueryClient();
  const [name, setName] = useState(() => line.description.trim().slice(0, 80));
  const [category, setCategory] = useState<SupplyCategory>(() => inferSupplyCategory(line.description));
  const [unit, setUnit] = useState(() => (line.unit ?? '').trim().slice(0, 30) || 'each');
  const [supplier, setSupplier] = useState(() => vendorName.trim().slice(0, 80));
  const [validation, setValidation] = useState<string>();
  const create = useMutation(createSupplyMutation());

  async function handleCreate() {
    const trimmedName = name.trim();
    const trimmedUnit = unit.trim();
    if (!trimmedName || !trimmedUnit) {
      setValidation('Enter a name and unit.');
      return;
    }
    setValidation(undefined);
    try {
      const created = await create.mutateAsync({
        path: { studioId },
        body: {
          name: trimmedName,
          category,
          unit: trimmedUnit,
          ...(supplier.trim() ? { supplier: supplier.trim() } : {}),
        },
      });
      const suppliesKey = listSuppliesQueryKey({ path: { studioId } });
      queryClient.setQueryData<SupplyItem[]>(suppliesKey, (existing) => {
        const current = existing ?? [];
        return current.some((item) => item.id === created.id) ? current : [...current, created];
      });
      void queryClient.invalidateQueries({ queryKey: suppliesKey });
      onCreated(created);
    } catch {
      // Mutation error is shown below
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Enter' && event.target instanceof HTMLInputElement) {
      event.preventDefault();
      event.stopPropagation();
      if (!create.isPending) {
        void handleCreate();
      }
    }
  }

  return (
    <div
      className="card stack"
      role="group"
      aria-label={`New supply item for line ${lineNumber}`}
      onKeyDown={handleKeyDown}
    >
      <strong>New supply item</strong>
      <div className="field">
        <label htmlFor={`${id}-name`}>Item name</label>
        <input id={`${id}-name`} maxLength={80} value={name} onChange={(e) => setName(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor={`${id}-category`}>Category</label>
        <select id={`${id}-category`} value={category} onChange={(e) => setCategory(e.target.value as SupplyCategory)}>
          {supplyCategories.map((c) => (
            <option key={c} value={c}>
              {supplyCategoryLabels[c]}
            </option>
          ))}
        </select>
      </div>
      <div className="field">
        <label htmlFor={`${id}-unit`}>Unit</label>
        <input id={`${id}-unit`} maxLength={30} value={unit} onChange={(e) => setUnit(e.target.value)} />
      </div>
      <div className="field">
        <label htmlFor={`${id}-supplier`}>Supplier</label>
        <input id={`${id}-supplier`} maxLength={80} value={supplier} onChange={(e) => setSupplier(e.target.value)} />
      </div>
      {validation && <p role="alert" className="error">{validation}</p>}
      {create.isError && <ErrorMessage error={create.error} />}
      <div className="row">
        <button type="button" className="primary" disabled={create.isPending} onClick={() => void handleCreate()}>
          {create.isPending ? 'Creating…' : 'Create item'}
        </button>
        <button type="button" disabled={create.isPending} onClick={onCancel}>
          Cancel
        </button>
      </div>
    </div>
  );
}

const headerFields = [
  { key: 'invoiceDate', label: 'Date', type: 'date' },
  { key: 'dueDate', label: 'Due date', type: 'date' },
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
  const [documentType, setDocumentType] = useState<InvoiceDocumentType | ''>(invoice.documentType ?? '');
  const [vendorName, setVendorName] = useState(invoice.vendorName ?? '');
  const [invoiceNumber, setInvoiceNumber] = useState(invoice.invoiceNumber ?? '');
  const [orderNumber, setOrderNumber] = useState(invoice.orderNumber ?? '');
  const [header, setHeader] = useState<Record<HeaderKey, string>>({
    invoiceDate: invoice.invoiceDate ?? '',
    dueDate: invoice.dueDate ?? '',
    subtotal: invoice.subtotal ?? '',
    shipping: invoice.shipping ?? '',
    tax: invoice.tax ?? '',
    total: invoice.total ?? '',
  });
  const [lines, setLines] = useState<EditableLine[]>(invoice.lines.map(editable));
  const [creatingForLine, setCreatingForLine] = useState<number | null>(null);
  const [recordPayable, setRecordPayable] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState<string>();
  const { can } = useCan(studioId);
  const canRecordPayable = can('money.payables.manage');
  // The version on the server. Saving or confirming raises it without remounting the form, so a
  // confirm that then fails ("line 2 needs a quantity") can be fixed and retried without a 409
  const [version, setVersion] = useState(invoice.version);
  const effectiveVersion = Math.max(version, invoice.version);
  const refresh = () => queryClient.invalidateQueries();
  const save = useMutation({
    ...saveInvoiceDraftMutation(),
    onSuccess: (saved) => {
      setVersion(saved.version);
      return refresh();
    },
  });
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
  const busy = save.isPending || saveFirst.isPending || confirm.isPending || discard.isPending;
  const change = (index: number, patch: Partial<EditableLine>) =>
    setLines((current) => current.map((line, i) => (i === index ? { ...line, ...patch } : line)));
  const optional = (value: string | undefined) => (value && value.trim() ? value.trim() : undefined);
  const path = { studioId, invoiceId: invoice.id };

  const draft = () => ({
    ...(documentType ? { documentType } : {}),
    vendorName: optional(vendorName),
    invoiceNumber: optional(invoiceNumber),
    orderNumber: optional(orderNumber),
    invoiceDate: optional(header.invoiceDate),
    invoiceDatePrinted: optional(invoice.invoiceDatePrinted),
    ...(invoice.invoiceDateYear ? { invoiceDateYear: invoice.invoiceDateYear } : {}),
    dueDate: optional(header.dueDate),
    subtotal: optional(header.subtotal),
    shipping: optional(header.shipping),
    tax: optional(header.tax),
    total: optional(header.total),
    amountPaid: optional(invoice.amountPaid),
    amountDue: optional(invoice.amountDue),
    ...(invoice.payments && invoice.payments.length > 0 ? { payments: invoice.payments } : {}),
    ...(invoice.crossedOut && invoice.crossedOut.length > 0 ? { crossedOut: invoice.crossedOut } : {}),
    otherPapers: optional(invoice.otherPapers),
    ...(invoice.notes && invoice.notes.length > 0 ? { notes: invoice.notes } : {}),
    ...(invoice.rotation !== undefined ? { rotation: invoice.rotation } : {}),
    lines: lines.map((line) => ({
      description: line.description,
      sku: optional(line.sku),
      quantity: optional(line.quantity),
      unit: optional(line.unit),
      unitPrice: optional(line.unitPrice),
      lineTotal: optional(line.lineTotal),
      ...(line.kind ? { kind: line.kind } : {}),
      ...(line.serialNumbers && line.serialNumbers.length > 0 ? { serialNumbers: line.serialNumbers } : {}),
      ...(line.unclear ? { unclear: true } : {}),
      supplyItemId: optional(line.supplyItemId),
    })),
    expectedVersion: effectiveVersion,
  });

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    save.mutate({ path, body: draft() });
  }

  async function saveAndConfirm() {
    try {
      const saved = await saveFirst.mutateAsync({ path, body: draft() });
      setVersion(saved.version);
      confirm.mutate({
        path,
        body: {
          expectedVersion: saved.version,
          ...(canRecordPayable && recordPayable ? { recordPayable: true } : {}),
        },
      });
    } catch {
      await refresh();
    }
  }

  async function downloadEvalCase() {
    setExportError(undefined);
    setExporting(true);
    try {
      const { data, response } = await exportInvoiceEval({ path, parseAs: 'blob', throwOnError: true });
      const url = URL.createObjectURL(data);
      try {
        const link = document.createElement('a');
        link.href = url;
        link.download =
          /filename="([^"]+)"/.exec(response.headers.get('Content-Disposition') ?? '')?.[1] ??
          `invoice-eval-${invoice.id}.zip`;
        document.body.append(link);
        link.click();
        link.remove();
      } finally {
        window.setTimeout(() => {
          URL.revokeObjectURL(url);
        }, 1000);
      }
    } catch (e) {
      setExportError(errorText(e));
    } finally {
      setExporting(false);
    }
  }

  return (
    <form className="stack" onSubmit={submit} aria-labelledby={`${id}-title`}>
      <h3 id={`${id}-title`}>
        {invoice.vendorName ?? 'Invoice'} <span className="badge">{statusLabels[invoice.status]}</span>
        {invoice.documentType && (
          <>
            {' '}
            <span className="badge">{documentTypeLabels[invoice.documentType]}</span>
          </>
        )}
      </h3>
      {invoice.reader && <p className="badge">Read by {readerLabels[invoice.reader]}</p>}
      {invoice.readNote && <p className="warning">{invoice.readNote}</p>}
      {invoice.payableAmount && invoice.payableAmount !== invoice.total && (
        <p className="badge">
          Payable: <strong>{invoice.payableAmount}</strong> (from payments; written total is {invoice.total ?? 'blank'})
        </p>
      )}
      {invoice.explanations && invoice.explanations.length > 0 && (
        <ul aria-label="Payment notes">
          {invoice.explanations.map((explanation, index) => (
            <li key={`${String(index)}-${explanation}`}>{explanation}</li>
          ))}
        </ul>
      )}
      {invoice.problems.length > 0 && (
        <ul aria-label="Problems">
          {invoice.problems.map((problem, index) => (
            <li key={`${String(index)}-${problem}`}>{problem}</li>
          ))}
        </ul>
      )}
      {invoice.notes && invoice.notes.length > 0 && (
        <section aria-label="Reader notes">
          <strong>Reader notes</strong>
          <ul>
            {invoice.notes.map((note, index) => (
              <li key={`${String(index)}-${note}`}>&ldquo;{note}&rdquo;</li>
            ))}
          </ul>
        </section>
      )}
      {invoice.crossedOut && invoice.crossedOut.length > 0 && (
        <section aria-label="Crossed-out rows">
          <strong>Crossed-out rows left out</strong>
          <ul>
            {invoice.crossedOut.map((row, index) => (
              <li key={`${String(index)}-${row}`}>{row}</li>
            ))}
          </ul>
        </section>
      )}
      {invoice.otherPapers && (
        <p className="badge">
          <strong>Other papers in photo:</strong> {invoice.otherPapers}
        </p>
      )}
      {invoice.payments && invoice.payments.length > 0 && (
        <section aria-label="Payments">
          <strong>Payments</strong>
          <ul>
            {invoice.payments.map((payment: InvoicePayment, index: number) => (
              <li key={`${payment.amount}-${payment.date ?? ''}-${String(index)}`}>
                {payment.amount}
                {payment.date && ` on ${payment.date}`}
                {payment.method && ` (${payment.method.toLowerCase()})`}
                {payment.source && ` — from ${payment.source.toLowerCase()}`}
              </li>
            ))}
          </ul>
        </section>
      )}
      <div className="row">
        <div className="field">
          <label htmlFor={`${id}-doctype`}>Document type</label>
          <select
            id={`${id}-doctype`}
            disabled={confirmed}
            value={documentType}
            onChange={(e) => setDocumentType(e.target.value as InvoiceDocumentType | '')}
          >
            <option value="">Unspecified</option>
            {documentTypes.map((type) => (
              <option key={type} value={type}>
                {documentTypeLabels[type]}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor={`${id}-vendor`}>Supplier</label>
          <input id={`${id}-vendor`} maxLength={120} disabled={confirmed} value={vendorName} onChange={(e) => setVendorName(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-number`}>Invoice number</label>
          <input id={`${id}-number`} maxLength={60} disabled={confirmed} value={invoiceNumber} onChange={(e) => setInvoiceNumber(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor={`${id}-order`}>Order / PO</label>
          <input id={`${id}-order`} maxLength={60} disabled={confirmed} value={orderNumber} onChange={(e) => setOrderNumber(e.target.value)} />
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
            {field.key === 'invoiceDate' && invoice.invoiceDateYear === 'ASSUMED' && (
              <span className="warning">Year assumed — check against the invoice</span>
            )}
            {field.key === 'invoiceDate' && invoice.invoiceDateYear === 'FROM_PAGE' && (
              <span className="badge">Year taken from another paper in photo</span>
            )}
          </div>
        ))}
      </div>
      <div className="invoice-lines">
        <table className="table">
          <thead>
            <tr>
              <th scope="col">Line</th>
              <th scope="col">Kind</th>
              <th scope="col">Quantity</th>
              <th scope="col">Unit</th>
              <th scope="col">Unit price</th>
              <th scope="col">Total</th>
              <th scope="col">Receive into</th>
              {!confirmed && <th scope="col" />}
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
                    {line.unclear && <span className="warning">Unclear — check photo</span>}
                    {line.serialNumbers && line.serialNumbers.length > 0 && (
                      <span className="badge">S/N: {line.serialNumbers.join(', ')}</span>
                    )}
                  </td>
                  <td>
                    <select
                      aria-label={`Line ${n} kind`}
                      disabled={confirmed}
                      value={line.kind ?? 'ITEM'}
                      onChange={(e) => change(index, { kind: e.target.value as InvoiceLineKind })}
                    >
                      {lineKinds.map((kind) => (
                        <option key={kind} value={kind}>
                          {lineKindLabels[kind]}
                        </option>
                      ))}
                    </select>
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
                    <ReceivedAs line={line} item={items.find((item) => item.id === line.supplyItemId)} />
                    {!confirmed && !line.supplyItemId && creatingForLine !== index && (
                      <div>
                        <button
                          type="button"
                          className="link"
                          aria-label={`+ New item for line ${n}`}
                          onClick={() => setCreatingForLine(index)}
                        >
                          + New item
                        </button>
                      </div>
                    )}
                    {!confirmed && creatingForLine === index && (
                      <InlineNewItemForm
                        studioId={studioId}
                        lineNumber={n}
                        line={line}
                        vendorName={vendorName}
                        onCreated={(created) => {
                          change(index, { supplyItemId: created.id });
                          setCreatingForLine(null);
                        }}
                        onCancel={() => setCreatingForLine(null)}
                      />
                    )}
                  </td>
                  {!confirmed && (
                    <td>
                      <button
                        type="button"
                        aria-label={`Remove line ${n}`}
                        onClick={() => {
                          setLines((current) => current.filter((_, i) => i !== index));
                          setCreatingForLine((cur) => (cur === null || cur === index ? null : cur > index ? cur - 1 : cur));
                        }}
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
      {!confirmed && canRecordPayable && (
        <label className="choice">
          <input
            type="checkbox"
            checked={recordPayable}
            disabled={busy}
            onChange={(e) => setRecordPayable(e.target.checked)}
          />
          Also record this invoice as a supplier bill in the ledger (accounts payable)
        </label>
      )}
      {!confirmed && (
        <div className="row">
          <button type="button" onClick={() => setLines((current) => [...current, { description: '', kind: 'ITEM' }])}>
            Add a line
          </button>
          <button type="submit" disabled={busy}>
            Save changes
          </button>
          <button type="button" className="primary" disabled={busy} onClick={() => void saveAndConfirm()}>
            Confirm and receive stock
          </button>
          <button type="button" className="danger" disabled={busy} onClick={() => discard.mutate({ path })}>
            {discard.isPending ? 'Discarding…' : 'Discard'}
          </button>
        </div>
      )}
      {confirmed && (
        <div className="row">
          {invoice.ledgerEntryId && <span className="badge">Recorded as a supplier bill in the ledger</span>}
          <button type="button" disabled={exporting} onClick={() => void downloadEvalCase()}>
            {exporting ? 'Preparing…' : 'Download eval case'}
          </button>
        </div>
      )}
      {exportError && (
        <p role="alert" className="error">
          {exportError}
        </p>
      )}
      {[save, saveFirst, confirm, discard].map((m, i) => m.isError && <ErrorMessage key={i} error={m.error} />)}
    </form>
  );
}
