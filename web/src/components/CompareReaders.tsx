import { compareInvoiceReadersMutation, type ComparedRead, type InvoiceReader } from '@studiomanager/api-client';
import { useMutation } from '@tanstack/react-query';
import { useId } from 'react';

import { ErrorMessage } from './ErrorMessage';

export const readerLabels: Record<InvoiceReader, string> = {
  CLAUDE: 'Claude',
  GOOGLE_DOCUMENT_AI: 'Google Document AI',
  AZURE_DOCUMENT_INTELLIGENCE: 'Azure Document Intelligence',
};

const outcomeLabels: Record<ComparedRead['outcome'], string> = {
  READ: 'read',
  REFUSED_CREDENTIALS: 'sign-in refused',
  UNAVAILABLE: 'unavailable',
  UNREADABLE: 'couldn’t read it',
};

const rows: { label: string; value: (read: ComparedRead) => string | undefined }[] = [
  { label: 'Supplier', value: (read) => read.vendorName },
  { label: 'Invoice number', value: (read) => read.invoiceNumber },
  { label: 'Date', value: (read) => read.invoiceDate },
  { label: 'Subtotal', value: (read) => read.subtotal },
  { label: 'Shipping', value: (read) => read.shipping },
  { label: 'Tax', value: (read) => read.tax },
  { label: 'Total', value: (read) => read.total },
  { label: 'Lines', value: (read) => String(read.lines.length) },
  { label: 'Problems', value: (read) => (read.problems.length === 0 ? 'none' : read.problems.join(' ')) },
  { label: 'Time', value: (read) => `${(read.elapsedMs / 1000).toFixed(1)} s` },
  { label: 'Pages', value: (read) => (read.pages === undefined ? undefined : String(read.pages)) },
  { label: 'Cost', value: (read) => (read.costUsd === undefined ? undefined : `$${read.costUsd}`) },
];

/**
 * "Compare readers": reads this invoice with every reader the studio may use and shows what each
 * read, side by side, with its time and cost. Owner only; the invoice's own draft doesn't change.
 */
export function CompareReaders({ studioId, invoiceId }: { studioId: string; invoiceId: string }) {
  const id = useId();
  const compare = useMutation(compareInvoiceReadersMutation());
  const results = compare.data?.results;

  return (
    <section className="stack" aria-labelledby={`${id}-title`}>
      <h3 id={`${id}-title`}>Compare readers</h3>
      <p>Reads this invoice with every reader the studio may use, one after another. It can take a minute.</p>
      <div>
        <button
          type="button"
          disabled={compare.isPending}
          onClick={() => compare.mutate({ path: { studioId, invoiceId } })}
        >
          {compare.isPending ? 'Reading…' : 'Compare readers'}
        </button>
      </div>
      {compare.isError && <ErrorMessage error={compare.error} />}
      {results && results.length === 0 && (
        <p role="status">No reader may read for this studio. Add an Anthropic key, or ask the admin.</p>
      )}
      {results && results.length > 0 && (
        <table>
          <caption>What each reader read</caption>
          <thead>
            <tr>
              <th scope="col" />
              {results.map((read) => (
                <th key={read.reader} scope="col">
                  {readerLabels[read.reader]} <span className="badge">{outcomeLabels[read.outcome]}</span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.label}>
                <th scope="row">{row.label}</th>
                {results.map((read) => (
                  <td key={read.reader}>{row.value(read) ?? read.note ?? '—'}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
