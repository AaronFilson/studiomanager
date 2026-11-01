/**
 * Reads CSV as Google Sheets, Excel, Forms and Airtable export it (RFC 4180): commas, quoted
 * fields with "" for a quote, and line breaks inside quotes. A byte-order mark is dropped, CRLF
 * and LF both end a row, and blank lines are skipped. Excel in some countries writes semicolons,
 * so the separator is whichever of the two the header row uses more.
 */
export function parseCsv(text: string): string[][] {
  const source = text.startsWith('﻿') ? text.slice(1) : text;
  const separator = guessSeparator(source);
  const rows: string[][] = [];
  let row: string[] = [];
  let field = '';
  let quoted = false;

  for (let i = 0; i < source.length; i++) {
    const c = source.charAt(i);
    if (quoted) {
      if (c === '"') {
        if (source[i + 1] === '"') {
          field += '"';
          i++;
        } else {
          quoted = false;
        }
      } else {
        field += c;
      }
    } else if (c === '"' && field === '') {
      quoted = true;
    } else if (c === separator) {
      row.push(field);
      field = '';
    } else if (c === '\n' || c === '\r') {
      if (c === '\r' && source[i + 1] === '\n') i++;
      row.push(field);
      field = '';
      if (row.some((cell) => cell.trim() !== '')) rows.push(row);
      row = [];
    } else {
      field += c;
    }
  }
  row.push(field);
  if (row.some((cell) => cell.trim() !== '')) rows.push(row);
  return rows;
}

function guessSeparator(text: string): ',' | ';' {
  const end = text.search(/\r|\n/);
  const header = end === -1 ? text : text.slice(0, end);
  const count = (c: string) => header.split(c).length - 1;
  return count(';') > count(',') ? ';' : ',';
}

/**
 * Picks the column whose header best matches one of {@code hints}: an exact match first, then a
 * header containing a hint. Returns -1 when nothing matches, so the field starts unmapped.
 */
export function guessColumn(headers: string[], hints: string[]): number {
  const normal = headers.map((h) => h.trim().toLowerCase());
  for (const hint of hints) {
    const exact = normal.indexOf(hint);
    if (exact !== -1) return exact;
  }
  for (const hint of hints) {
    const partial = normal.findIndex((h) => h.includes(hint));
    if (partial !== -1) return partial;
  }
  return -1;
}
