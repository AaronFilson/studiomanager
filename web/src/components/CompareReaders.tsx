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
  SKIPPED: 'didn’t run',
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
 * "Compare readers": reads this invoice with every reader, side by side, with its time and cost. A
 * reader that may not read for the studio, or that failed, says why once, at the top of its column.
 * Owner only; the invoice's own draft doesn't change.
 */
export function CompareReaders({ studioId, invoiceId }: { studioId: string; invoiceId: string }) {
  const id = useId();
  const compare = useMutation(compareInvoiceReadersMutation());
  const results = compare.data?.results;
  const read = results?.filter((result) => result.outcome === 'READ') ?? [];
  const longest = Math.max(0, ...read.map((result) => result.lines.length));

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
      {results && results.length > 0 && (
        <div className="invoice-lines">
          <table className="table">
            <caption>What each reader read</caption>
            <thead>
              <tr>
                <th scope="col" />
                {results.map((result) => (
                  <th key={result.reader} scope="col">
                    {readerLabels[result.reader]} <span className="badge">{outcomeLabels[result.outcome]}</span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {results.some((result) => result.note) && (
                <tr>
                  <th scope="row">Why not</th>
                  {results.map((result) => (
                    <td key={result.reader} className={result.note ? 'warning' : undefined}>
                      {result.note ?? '—'}
                    </td>
                  ))}
                </tr>
              )}
              {rows.map((row) => (
                <tr key={row.label}>
                  <th scope="row">{row.label}</th>
                  {results.map((result) => (
                    <td key={result.reader}>{result.outcome === 'READ' ? (row.value(result) ?? '—') : '—'}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {read.length > 0 && longest > 0 && (
        <div className="invoice-lines">
          <table className="table">
            <caption>Lines, as each reader read them (quantity × unit price = total)</caption>
            <thead>
              <tr>
                <th scope="col">#</th>
                {read.map((result) => (
                  <th key={result.reader} scope="col">
                    {readerLabels[result.reader]}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {Array.from({ length: longest }, (_, index) => {
                const cells = read.map((result) => lineText(result, index));
                // Only the numbers count: each reader spells descriptions its own way
                const differ = new Set(read.map((result) => lineNumbers(result, index))).size > 1;
                return (
                  <tr key={index}>
                    <th scope="row">{String(index + 1)}</th>
                    {cells.map((cell, at) => (
                      <td key={read[at]?.reader} className={differ ? 'warning' : undefined}>
                        {cell}
                      </td>
                    ))}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function lineNumbers(result: ComparedRead, index: number): string {
  const line = result.lines[index];
  const amount = (value: string | undefined) => (value === undefined ? '' : String(Number(value)));
  return line ? [amount(line.quantity), amount(line.unitPrice), amount(line.lineTotal)].join('|') : '';
}

function lineText(result: ComparedRead, index: number): string {
  const line = result.lines[index];
  if (!line) {
    return '—';
  }
  const quantity = line.quantity ?? '?';
  const price = line.unitPrice ?? '?';
  return `${line.description}: ${quantity} × ${price} = ${line.lineTotal ?? '?'}`;
}
