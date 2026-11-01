import {
  type ImportConsignmentRequest,
  importConsignmentMutation,
  type ImportMembersRequest,
  importMembersMutation,
  type ImportResult,
} from '@studiomanager/api-client';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createFileRoute } from '@tanstack/react-router';
import { useId, useState } from 'react';

import { ErrorMessage } from '../../../components/ErrorMessage';
import { guessColumn, parseCsv } from '../../../csv';

export const Route = createFileRoute('/studios/$studioId/import')({
  component: ImportPage,
});

type Kind = 'members' | 'consignment';

interface Field {
  key: string;
  label: string;
  required?: boolean;
  /** Header names this field is likely to be under, most likely first. */
  hints: string[];
}

const fields: Record<Kind, Field[]> = {
  members: [
    { key: 'email', label: 'Email', required: true, hints: ['email', 'email address', 'e-mail', 'mail'] },
    { key: 'balance', label: 'Balance owed', hints: ['balance', 'owed', 'owes', 'amount due', 'due'] },
    { key: 'balanceAsOf', label: 'Balance as of', hints: ['as of', 'balance date', 'date'] },
  ],
  consignment: [
    { key: 'artist', label: 'Artist', required: true, hints: ['artist', 'consignor', 'maker', 'name'] },
    { key: 'artistEmail', label: 'Artist email', hints: ['artist email', 'email'] },
    { key: 'description', label: 'Description', required: true, hints: ['description', 'item', 'title', 'product', 'what'] },
    { key: 'retailPrice', label: 'Retail price', required: true, hints: ['retail price', 'price', 'retail'] },
    { key: 'quantity', label: 'Quantity', hints: ['quantity', 'qty', 'count', 'stock', 'how many'] },
    { key: 'artistShare', label: "Artist's share", hints: ["artist's share", 'artist share', 'share', 'split', 'commission'] },
    { key: 'receivedOn', label: 'Received on', hints: ['received', 'date received', 'intake', 'date'] },
  ],
};

const kindLabels: Record<Kind, string> = { members: 'Members and balances', consignment: 'Consigned stock' };

const outcomeLabels: Record<ImportResult['rows'][number]['outcome'], string> = {
  CREATED: 'Add',
  UPDATED: 'Update',
  SKIPPED: 'Skip',
  ERROR: 'Fix',
};

/** Spreadsheet line numbers: the header is line 1, so the first data row is line 2. */
const FIRST_LINE = 2;

function ImportPage() {
  const { studioId } = Route.useParams();
  const id = useId();
  const queryClient = useQueryClient();
  const [kind, setKind] = useState<Kind>('members');
  const [fileName, setFileName] = useState<string>();
  const [table, setTable] = useState<string[][]>([]);
  const [mapping, setMapping] = useState<Record<string, number>>({});
  const [checked, setChecked] = useState<ImportResult>();
  const headers = table[0] ?? [];
  const rows = table.slice(1);
  const members = useMutation({ ...importMembersMutation() });
  const consignment = useMutation({ ...importConsignmentMutation() });
  const running = members.isPending || consignment.isPending;
  const error = members.error ?? consignment.error;
  const missing = fields[kind].filter((f) => f.required && (mapping[f.key] ?? -1) < 0);

  function guessMapping(forKind: Kind, header: string[]) {
    // Each column goes to at most one field, the first that claims it
    const taken = new Set<number>();
    const next: Record<string, number> = {};
    for (const field of fields[forKind]) {
      const column = guessColumn(
        header.map((h, i) => (taken.has(i) ? '' : h)),
        field.hints,
      );
      next[field.key] = column;
      if (column >= 0) taken.add(column);
    }
    setMapping(next);
  }

  async function load(file: File) {
    const parsed = parseCsv(await file.text());
    setFileName(file.name);
    setTable(parsed);
    setChecked(undefined);
    guessMapping(kind, parsed[0] ?? []);
  }

  function body(dryRun: boolean) {
    const mapped = rows.map((cells, i) => {
      const row: Record<string, string | number> = { line: i + FIRST_LINE };
      for (const field of fields[kind]) {
        const column = mapping[field.key] ?? -1;
        const value = column >= 0 ? cells[column]?.trim() : undefined;
        if (value) row[field.key] = value;
      }
      return row;
    });
    return { dryRun, rows: mapped };
  }

  async function run(dryRun: boolean) {
    const options = { path: { studioId } };
    const result =
      kind === 'members'
        ? await members.mutateAsync({ ...options, body: body(dryRun) as ImportMembersRequest })
        : await consignment.mutateAsync({ ...options, body: body(dryRun) as ImportConsignmentRequest });
    setChecked(result);
    if (result.committed) await queryClient.invalidateQueries();
  }

  const errors = checked?.rows.filter((r) => r.outcome === 'ERROR').length ?? 0;

  return (
    <div className="stack">
      <h1>Import from a spreadsheet</h1>
      <section className="card stack" aria-label="Choose a file">
        <p>
          Export your list as CSV from Google Sheets, Excel or Airtable (File › Download › CSV). The first row should be the
          column names. Nothing is saved until you check the file and then import it, and an import with any errors keeps
          nothing.
        </p>
        <div className="row">
          <div className="field">
            <label htmlFor={`${id}-kind`}>What&apos;s in it</label>
            <select
              id={`${id}-kind`}
              value={kind}
              onChange={(e) => {
                const next = e.target.value as Kind;
                setKind(next);
                setChecked(undefined);
                guessMapping(next, headers);
              }}
            >
              {(Object.keys(kindLabels) as Kind[]).map((k) => (
                <option key={k} value={k}>
                  {kindLabels[k]}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor={`${id}-file`}>CSV file</label>
            <input
              id={`${id}-file`}
              type="file"
              accept=".csv,text/csv"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) void load(file);
              }}
            />
          </div>
        </div>
        {kind === 'members' && (
          <p className="badge">
            Each new address gets an emailed invite to join as a member. A balance is what they owed the studio; write a
            credit as negative or in parentheses. It&apos;s added when they join, or now if they&apos;re already a member.
          </p>
        )}
        {kind === 'consignment' && (
          <p className="badge">
            Each row is one item. Artists are matched by email, then by name; anyone new is added with the share on their
            first row as their terms.
          </p>
        )}
      </section>

      {fileName && headers.length === 0 && <p className="error">That file is empty.</p>}

      {headers.length > 0 && (
        <section className="card stack" aria-label="Match the columns">
          <h2>Match the columns</h2>
          <p>
            {fileName}: {rows.length} {rows.length === 1 ? 'row' : 'rows'}.
          </p>
          <div className="row">
            {fields[kind].map((field) => (
              <div className="field" key={field.key}>
                <label htmlFor={`${id}-${field.key}`}>
                  {field.label}
                  {field.required ? '' : ' (optional)'}
                </label>
                <select
                  id={`${id}-${field.key}`}
                  value={mapping[field.key] ?? -1}
                  onChange={(e) => {
                    setMapping({ ...mapping, [field.key]: Number(e.target.value) });
                    setChecked(undefined);
                  }}
                >
                  <option value={-1}>Not in this file</option>
                  {headers.map((h, i) => (
                    <option key={i} value={i}>
                      {h || `Column ${String(i + 1)}`}
                    </option>
                  ))}
                </select>
              </div>
            ))}
          </div>
          {missing.length > 0 && <p className="badge warning">Choose a column for {missing.map((f) => f.label).join(', ')}.</p>}
          {error && <ErrorMessage error={error} />}
          <div className="row">
            <button type="button" disabled={running || missing.length > 0 || rows.length === 0} onClick={() => void run(true)}>
              Check the file
            </button>
            <button
              type="button"
              className="primary"
              disabled={running || !checked || checked.committed || errors > 0}
              onClick={() => void run(false)}
            >
              Import {rows.length} {rows.length === 1 ? 'row' : 'rows'}
            </button>
          </div>
        </section>
      )}

      {checked && (
        <section className="card stack" aria-label="Result" aria-live="polite">
          <h2>{checked.committed ? 'Imported' : 'Check'}</h2>
          <p>
            {checked.committed
              ? 'Done. Each row below says what happened.'
              : errors > 0
                ? `${String(errors)} ${errors === 1 ? 'row needs' : 'rows need'} fixing. Correct the file and choose it again; nothing has been saved.`
                : 'Nothing is saved yet. If this looks right, import it.'}
          </p>
          <table>
            <thead>
              <tr>
                <th scope="col">Line</th>
                <th scope="col">Action</th>
                <th scope="col">Detail</th>
              </tr>
            </thead>
            <tbody>
              {checked.rows.map((row) => (
                <tr key={row.line} className={row.outcome === 'ERROR' ? 'error' : undefined}>
                  <td>{row.line}</td>
                  <td>{outcomeLabels[row.outcome]}</td>
                  <td>{row.message}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </div>
  );
}
