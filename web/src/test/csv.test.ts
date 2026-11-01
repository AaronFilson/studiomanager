import { describe, expect, it } from 'vitest';

import { guessColumn, parseCsv } from '../csv';

describe('parseCsv', () => {
  it('reads quoted fields, doubled quotes, line breaks in quotes and CRLF', () => {
    const text = '﻿Name,Note\r\n"Ruiz, Ana","She said ""hi""\nthen left"\r\n\r\nBen,\r\n';
    expect(parseCsv(text)).toEqual([
      ['Name', 'Note'],
      ['Ruiz, Ana', 'She said "hi"\nthen left'],
      ['Ben', ''],
    ]);
  });

  it('reads semicolon files from European Excel', () => {
    expect(parseCsv('Email;Balance\nmo@example.test;12,50')).toEqual([
      ['Email', 'Balance'],
      ['mo@example.test', '12,50'],
    ]);
  });
});

describe('guessColumn', () => {
  it('prefers an exact header, then one containing a hint', () => {
    expect(guessColumn(['Member name', 'E-mail', 'Email address'], ['email', 'email address', 'e-mail'])).toBe(2);
    expect(guessColumn(['Name', 'Balance due ($)'], ['balance'])).toBe(1);
    expect(guessColumn(['Name'], ['balance'])).toBe(-1);
  });
});
