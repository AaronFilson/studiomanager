import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(__dirname, '../..');
const DEFAULT_OUT = path.join(REPO_ROOT, 'api/src/test/resources/invoices/eval');

/** Deterministic Mulberry32 PRNG so repeated runs produce identical layouts and photo jitter. */
function mulberry32(seed) {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let t = Math.imul(state ^ (state >>> 15), 1 | state);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}

function formatUsd(value) {
  if (!value) return '';
  const n = Number(value);
  return n < 0 ? `-$${Math.abs(n).toFixed(2)}` : `$${n.toFixed(2)}`;
}

/**
 * Seven realistic ceramic-studio synthetic invoice templates (docs/planning/24).
 * Every vendor name is fictional and zero customer PII appears anywhere.
 * Each definition drives BOTH the HTML template rendered by Playwright AND exact expected.json.
 */
function buildCases(seed) {
  const rand = mulberry32(seed);
  const jitter = (maxDeg) => ((rand() * 2 - 1) * maxDeg).toFixed(2);

  return [
    {
      id: 'synth-printed-clay-invoice',
      template: 'printed-supplier',
      split: 'dev',
      tags: ['synthetic', 'printed', 'shipping-line', 'fee-line', 'three-decimal-price'],
      photoEffects: { tiltDeg: jitter(0.6), shadow: false, fold: false, rotation: 0 },
      expected: {
        documentType: 'INVOICE',
        vendorName: 'Alder Creek Ceramic Supply',
        invoiceNumber: 'AC-44108',
        orderNumber: 'PO-2026-089',
        invoiceDate: '2026-10-08',
        invoiceDatePrinted: '10/08/2026',
        invoiceDateYear: 'PRINTED',
        dueDate: '2026-11-07',
        subtotal: '419.50',
        shipping: '',
        tax: '24.82',
        total: '444.32',
        amountPaid: '',
        amountDue: '444.32',
        payments: [],
        crossedOut: [],
        otherPapers: '',
        notes: ['Freight and pallet fee are printed as lines among the items.'],
        rotation: '0',
        lines: [
          {
            description: 'B-Mix 5 Moist Clay 50 lb box',
            sku: 'AC-BM5',
            quantity: '6',
            unit: 'box',
            unitPrice: '33.50',
            lineTotal: '201.00',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
          {
            description: 'Custer Feldspar 50 lb bag',
            sku: 'AC-CF50',
            quantity: '100',
            unit: 'lb',
            unitPrice: '1.185',
            lineTotal: '118.50',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
          {
            description: 'Lift-gate freight delivery',
            sku: 'FRT-LG',
            quantity: '1',
            unit: '',
            unitPrice: '75.00',
            lineTotal: '75.00',
            kind: 'SHIPPING',
            serialNumbers: [],
            unclear: false,
          },
          {
            description: 'Wood pallet handling fee',
            sku: 'FEE-PAL',
            quantity: '1',
            unit: '',
            unitPrice: '25.00',
            lineTotal: '25.00',
            kind: 'FEE',
            serialNumbers: [],
            unclear: false,
          },
        ],
      },
    },
    {
      id: 'synth-quickbooks-glaze-invoice',
      template: 'quickbooks-square',
      split: 'test',
      tags: ['synthetic', 'quickbooks', 'discount-line', 'glaze-materials'],
      photoEffects: { tiltDeg: '0', shadow: false, fold: false, rotation: 0 },
      expected: {
        documentType: 'INVOICE',
        vendorName: 'Cedar Kiln & Supply Co.',
        invoiceNumber: '10842',
        orderNumber: 'EST-992',
        invoiceDate: '2026-10-12',
        invoiceDatePrinted: 'Oct 12, 2026',
        invoiceDateYear: 'PRINTED',
        dueDate: '2026-11-11',
        subtotal: '202.50',
        shipping: '',
        tax: '15.71',
        total: '218.21',
        amountPaid: '0.00',
        amountDue: '218.21',
        payments: [],
        crossedOut: [],
        otherPapers: '',
        notes: [],
        rotation: '0',
        lines: [
          {
            description: 'EPK Kaolin Dry Material',
            sku: 'CK-EPK',
            quantity: '50',
            unit: 'lb',
            unitPrice: '1.25',
            lineTotal: '62.50',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
          {
            description: 'Clear Zinc-Free Dipping Glaze',
            sku: 'CK-GLZ10',
            quantity: '4',
            unit: 'pint',
            unitPrice: '18.50',
            lineTotal: '74.00',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
          {
            description: 'Orton Self-Supporting Cones 06',
            sku: 'CK-C06',
            quantity: '4',
            unit: 'box',
            unitPrice: '22.00',
            lineTotal: '88.00',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
          {
            description: 'Studio trade discount (10%)',
            sku: '',
            quantity: '',
            unit: '',
            unitPrice: '',
            lineTotal: '-22.00',
            kind: 'DISCOUNT',
            serialNumbers: [],
            unclear: false,
          },
        ],
      },
    },
    {
      id: 'synth-online-kiln-parts',
      template: 'shopify-order',
      split: 'test',
      tags: ['synthetic', 'online-order', 'serial-numbers', 'shipping-header'],
      photoEffects: { tiltDeg: '0', shadow: false, fold: false, rotation: 0 },
      expected: {
        documentType: 'ORDER_FORM',
        vendorName: 'Hearthstone Ceramic Equipment',
        invoiceNumber: 'HCE-77319',
        orderNumber: 'WEB-77319',
        invoiceDate: '2026-10-15',
        invoiceDatePrinted: 'October 15, 2026',
        invoiceDateYear: 'PRINTED',
        dueDate: '',
        subtotal: '246.00',
        shipping: '28.50',
        tax: '19.68',
        total: '294.18',
        amountPaid: '294.18',
        amountDue: '0.00',
        payments: [
          { date: '2026-10-15', amount: '294.18', method: 'CARD', source: 'INVOICE' },
        ],
        crossedOut: [],
        otherPapers: '',
        notes: ['Web order paid in full by card.'],
        rotation: '0',
        lines: [
          {
            description: 'KM-1027 Kanthal Replacement Element Set',
            sku: 'HE-EL1027',
            quantity: '2',
            unit: 'set',
            unitPrice: '89.00',
            lineTotal: '178.00',
            kind: 'ITEM',
            serialNumbers: ['SN-SK240-8814'],
            unclear: false,
          },
          {
            description: 'Type K Heavy Duty Thermocouple 8 Gauge',
            sku: 'HE-TCK8',
            quantity: '2',
            unit: 'each',
            unitPrice: '34.00',
            lineTotal: '68.00',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
        ],
      },
    },
    {
      id: 'synth-handwritten-counter-order',
      template: 'handwritten-counter',
      split: 'test',
      tags: ['synthetic', 'handwritten', 'split-payments', 'crossed-out', 'rotated', 'no-unit-price'],
      photoEffects: { tiltDeg: jitter(1.4), shadow: true, fold: true, rotation: 90, receiptOverlay: true },
      expected: {
        documentType: 'ORDER_FORM',
        vendorName: 'Cascade Clay & Minerals',
        invoiceNumber: '',
        orderNumber: 'CTR-514',
        invoiceDate: '2026-10-18',
        invoiceDatePrinted: '10/18',
        invoiceDateYear: 'FROM_PAGE',
        dueDate: '',
        subtotal: '164.50',
        shipping: '',
        tax: '13.20',
        total: '95.00',
        amountPaid: '177.70',
        amountDue: '0.00',
        payments: [
          { date: '2026-10-18', amount: '95.00', method: 'CARD', source: 'SLIP' },
          { date: '2026-10-19', amount: '82.70', method: 'CARD', source: 'RECEIPT' },
        ],
        crossedOut: ['25 lb Redart Clay — 19.00'],
        otherPapers: 'Two till receipts and a card slip resting at the bottom',
        notes: [
          'Written total shows only the first card payment; two payments cover lines plus tax.',
          'Year taken from the dated card slip.',
        ],
        rotation: '90',
        lines: [
          {
            description: 'Hawthorne Bond Fireclay',
            sku: '',
            quantity: '100',
            unit: 'lb',
            unitPrice: '',
            lineTotal: '54.00',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
          {
            description: 'Dolomite 325 mesh',
            sku: '',
            quantity: '50',
            unit: 'lb',
            unitPrice: '',
            lineTotal: '42.50',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
          {
            description: 'Wax Resist Cotton Roll',
            sku: '',
            quantity: '2',
            unit: 'roll',
            unitPrice: '',
            lineTotal: '68.00',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
        ],
      },
    },
    {
      id: 'synth-thermal-till-receipt',
      template: 'thermal-receipt',
      split: 'dev',
      tags: ['synthetic', 'receipt', 'thermal'],
      photoEffects: { tiltDeg: jitter(1.2), shadow: true, fold: false, rotation: 0 },
      expected: {
        documentType: 'RECEIPT',
        vendorName: 'Milltown Pottery Mart',
        invoiceNumber: 'RCP-00419',
        orderNumber: '',
        invoiceDate: '2026-10-21',
        invoiceDatePrinted: '10/21/26',
        invoiceDateYear: 'PRINTED',
        dueDate: '',
        subtotal: '54.50',
        shipping: '',
        tax: '4.63',
        total: '59.13',
        amountPaid: '59.13',
        amountDue: '0.00',
        payments: [
          { date: '2026-10-21', amount: '59.13', method: 'CARD', source: 'RECEIPT' },
        ],
        crossedOut: [],
        otherPapers: '',
        notes: [],
        rotation: '0',
        lines: [
          {
            description: 'Large Yellow Elephant Ear Sponge',
            sku: 'SP-04',
            quantity: '6',
            unit: 'each',
            unitPrice: '4.25',
            lineTotal: '25.50',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
          {
            description: 'Stainless Rib Set #2',
            sku: 'RB-02',
            quantity: '2',
            unit: 'set',
            unitPrice: '14.50',
            lineTotal: '29.00',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
        ],
      },
    },
    {
      id: 'synth-packing-list',
      template: 'packing-list',
      split: 'dev',
      tags: ['synthetic', 'packing-list', 'show-through', 'no-prices'],
      photoEffects: { tiltDeg: jitter(0.8), shadow: false, fold: false, rotation: 0, showThrough: true },
      expected: {
        documentType: 'PACKING_LIST',
        vendorName: 'Willamette Clay Works',
        invoiceNumber: '',
        orderNumber: 'SO-3309',
        invoiceDate: '2026-10-23',
        invoiceDatePrinted: '2026-10-23',
        invoiceDateYear: 'PRINTED',
        dueDate: '',
        subtotal: '',
        shipping: '',
        tax: '',
        total: '',
        amountPaid: '',
        amountDue: '',
        payments: [],
        crossedOut: [],
        otherPapers: 'Faint invoice showing through from sheet underneath',
        notes: ['Packing list only; no prices printed.'],
        rotation: '0',
        lines: [
          {
            description: 'Speckled Buff Cone 5 Clay',
            sku: 'WC-SB5',
            quantity: '10',
            unit: 'bag',
            unitPrice: '',
            lineTotal: '',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
          {
            description: 'Porcelain Grolleg Cone 6',
            sku: 'WC-PG6',
            quantity: '4',
            unit: 'box',
            unitPrice: '',
            lineTotal: '',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
        ],
      },
    },
    {
      id: 'synth-credit-memo',
      template: 'credit-memo',
      split: 'test',
      tags: ['synthetic', 'credit-memo', 'negative-amounts'],
      photoEffects: { tiltDeg: '0', shadow: false, fold: false, rotation: 0 },
      expected: {
        documentType: 'CREDIT_MEMO',
        vendorName: 'Alder Creek Ceramic Supply',
        invoiceNumber: 'CM-44190',
        orderNumber: 'PO-2026-089',
        invoiceDate: '2026-10-26',
        invoiceDatePrinted: '10/26/2026',
        invoiceDateYear: 'PRINTED',
        dueDate: '',
        subtotal: '-87.00',
        shipping: '',
        tax: '-7.18',
        total: '-94.18',
        amountPaid: '',
        amountDue: '-94.18',
        payments: [],
        crossedOut: [],
        otherPapers: '',
        notes: ['Credit memo for two cracked half-shelves returned from PO-2026-089.'],
        rotation: '0',
        lines: [
          {
            description: '21 in Half Cordierite Kiln Shelf (damaged return)',
            sku: 'AC-KS21',
            quantity: '-2',
            unit: 'each',
            unitPrice: '48.50',
            lineTotal: '-97.00',
            kind: 'ITEM',
            serialNumbers: [],
            unclear: false,
          },
          {
            description: 'Return inspection fee',
            sku: 'FEE-RET',
            quantity: '1',
            unit: '',
            unitPrice: '10.00',
            lineTotal: '10.00',
            kind: 'FEE',
            serialNumbers: [],
            unclear: false,
          },
        ],
      },
    },
  ];
}

function renderHtml(spec) {
  const d = spec.expected;
  const fx = spec.photoEffects;
  const isThermal = spec.template === 'thermal-receipt';
  const isHandwritten = spec.template === 'handwritten-counter';
  const isPacking = spec.template === 'packing-list';
  const paperWidth = isThermal ? '340px' : '760px';
  const fontFamily = isThermal
    ? "'Courier New', monospace"
    : isHandwritten
      ? "'Comic Sans MS', 'Bradley Hand', 'Segoe Print', cursive"
      : "system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif";

  const rowsHtml = d.lines
    .map((line) => {
      if (isPacking) {
        return `<tr><td>${escapeHtml(line.sku)}</td><td>${escapeHtml(line.description)}</td><td>${escapeHtml(line.quantity)} ${escapeHtml(line.unit)}</td></tr>`;
      }
      if (isHandwritten) {
        return `<tr><td>${escapeHtml(line.quantity)} ${escapeHtml(line.unit)}</td><td>${escapeHtml(line.description)}</td><td class="num">${formatUsd(line.lineTotal)}</td></tr>`;
      }
      const serial = line.serialNumbers.length > 0 ? `<div class="sub">S/N: ${escapeHtml(line.serialNumbers.join(', '))}</div>` : '';
      return `<tr>
        <td>${escapeHtml(line.sku)}</td>
        <td>${escapeHtml(line.description)}${serial}</td>
        <td class="num">${escapeHtml(line.quantity)} ${escapeHtml(line.unit)}</td>
        <td class="num">${line.unitPrice ? `$${escapeHtml(line.unitPrice)}` : ''}</td>
        <td class="num">${formatUsd(line.lineTotal)}</td>
      </tr>`;
    })
    .join('\n');

  const crossedHtml = d.crossedOut
    .map((text) => `<tr class="struck"><td colspan="3">${escapeHtml(text)}</td></tr>`)
    .join('\n');

  const receiptOverlayHtml = fx.receiptOverlay
    ? `<div class="overlay-receipt">
         <strong>CARD SLIP &amp; TILL RECEIPTS</strong><br/>
         10/18/2026 CARD SALE $95.00<br/>
         10/19/2026 CARD SALE $82.70
       </div>`
    : '';

  const showThroughHtml = fx.showThrough
    ? `<div class="show-through" aria-hidden="true">INVOICE #99104 SUBTOTAL 842.00 DUE ON RECEIPT</div>`
    : '';

  const foldCss = fx.fold
    ? 'background-image: linear-gradient(180deg, transparent 48%, rgba(0,0,0,0.08) 50%, transparent 52%);'
    : '';

  const totalsHtml = isPacking
    ? '<p class="sub">PACKING LIST — NO PRICES</p>'
    : `<div class="totals">
         ${d.subtotal ? `<div>Subtotal: <strong>${formatUsd(d.subtotal)}</strong></div>` : ''}
         ${d.shipping ? `<div>Shipping: <strong>${formatUsd(d.shipping)}</strong></div>` : ''}
         ${d.tax ? `<div>Tax: <strong>${formatUsd(d.tax)}</strong></div>` : ''}
         ${d.total ? `<div class="grand">${d.documentType === 'CREDIT_MEMO' ? 'Credit Total' : 'Total'}: <strong>${formatUsd(d.total)}</strong></div>` : ''}
         ${d.amountPaid ? `<div>Amount Paid: ${formatUsd(d.amountPaid)}</div>` : ''}
         ${d.amountDue ? `<div>Amount Due: ${formatUsd(d.amountDue)}</div>` : ''}
       </div>`;

  return `<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      padding: 36px;
      background: ${fx.shadow ? '#c9c2b8' : '#f2efe9'};
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      font-family: ${fontFamily};
    }
    .sheet {
      position: relative;
      width: ${paperWidth};
      background: ${isHandwritten ? '#fffef0' : '#ffffff'};
      color: #1c1917;
      padding: ${isThermal ? '18px' : '32px'};
      border: 1px solid #d6d3d1;
      transform: rotate(${fx.rotation === 90 ? '-90' : fx.tiltDeg}deg);
      box-shadow: ${fx.shadow ? '0 14px 30px rgba(0,0,0,0.22)' : '0 2px 8px rgba(0,0,0,0.08)'};
      ${foldCss}
    }
    header { display: flex; justify-content: space-between; border-bottom: 2px solid #292524; padding-bottom: 12px; margin-bottom: 16px; }
    h1 { margin: 0; font-size: ${isThermal ? '16px' : '22px'}; }
    .meta { font-size: 13px; text-align: right; }
    table { width: 100%; border-collapse: collapse; font-size: ${isThermal ? '12px' : '14px'}; margin-top: 12px; }
    th, td { border-bottom: 1px solid #e7e5e4; padding: 6px 4px; text-align: left; }
    .num { text-align: right; white-space: nowrap; }
    .struck td { text-decoration: line-through; color: #78716c; }
    .totals { margin-top: 16px; text-align: right; font-size: 14px; line-height: 1.6; }
    .grand { font-size: 17px; border-top: 2px solid #292524; padding-top: 6px; margin-top: 4px; }
    .sub { font-size: 11px; color: #57534e; }
    .overlay-receipt {
      margin-top: 18px;
      padding: 10px 14px;
      background: #fafaf9;
      border: 1px dashed #78716c;
      font-family: 'Courier New', monospace;
      font-size: 12px;
      transform: rotate(2.5deg);
      box-shadow: 0 4px 10px rgba(0,0,0,0.14);
    }
    .show-through {
      position: absolute;
      inset: 40% 10% auto 10%;
      font-size: 20px;
      opacity: 0.08;
      transform: scaleX(-1);
      pointer-events: none;
    }
  </style>
</head>
<body>
  <article class="sheet">
    ${showThroughHtml}
    <header>
      <div>
        <h1>${escapeHtml(d.vendorName)}</h1>
        <div class="sub">${escapeHtml(d.documentType.replaceAll('_', ' '))}</div>
      </div>
      <div class="meta">
        ${d.invoiceNumber ? `<div>No: ${escapeHtml(d.invoiceNumber)}</div>` : ''}
        ${d.orderNumber ? `<div>Order/PO: ${escapeHtml(d.orderNumber)}</div>` : ''}
        ${d.invoiceDatePrinted ? `<div>Date: ${escapeHtml(d.invoiceDatePrinted)}</div>` : ''}
        ${d.dueDate ? `<div>Due: ${escapeHtml(d.dueDate)}</div>` : ''}
      </div>
    </header>
    <table>
      <thead>
        ${
          isPacking
            ? '<tr><th>SKU</th><th>Description</th><th>Qty Shipped</th></tr>'
            : isHandwritten
              ? '<tr><th>Qty</th><th>Item</th><th class="num">Amount</th></tr>'
              : '<tr><th>SKU</th><th>Description</th><th class="num">Qty</th><th class="num">Your Price</th><th class="num">Amount</th></tr>'
        }
      </thead>
      <tbody>
        ${rowsHtml}
        ${crossedHtml}
      </tbody>
    </table>
    ${totalsHtml}
    ${receiptOverlayHtml}
  </article>
</body>
</html>`;
}

async function renderPng(browser, html, outFile) {
  const page = await browser.newPage({ viewport: { width: 1000, height: 1200 }, deviceScaleFactor: 1 });
  try {
    await page.setContent(html, { waitUntil: 'load' });
    const sheet = page.locator('.sheet');
    await sheet.screenshot({ path: outFile });
  } finally {
    await page.close();
  }
}

async function main() {
  const args = process.argv.slice(2);
  let seed = 20261116;
  let outDir = DEFAULT_OUT;
  let renderImages = true;
  let writeRecorded = false;

  for (let i = 0; i < args.length; i++) {
    const arg = args[i];
    if (arg === '--seed' && args[i + 1]) {
      seed = Number(args[++i]);
    } else if (arg === '--out' && args[i + 1]) {
      outDir = path.resolve(args[++i]);
    } else if (arg === '--no-render') {
      renderImages = false;
    } else if (arg === '--with-recorded') {
      writeRecorded = true;
    }
  }

  const cases = buildCases(seed);
  let browser = null;
  if (renderImages) {
    try {
      const { chromium } = await import('@playwright/test');
      browser = await chromium.launch();
    } catch (error) {
      console.warn(`Playwright Chromium unavailable (${String(error)}); pass --no-render to skip image rendering.`);
      throw error;
    }
  }

  try {
    for (const spec of cases) {
      const folder = path.join(outDir, spec.id);
      await fs.mkdir(folder, { recursive: true });
      await fs.writeFile(path.join(folder, 'expected.json'), `${JSON.stringify(spec.expected, null, 2)}\n`, 'utf8');
      const caseMeta = {
        source: 'synthetic-generator',
        license: 'MIT',
        template: spec.template,
        seed,
        split: spec.split,
        tags: spec.tags,
      };
      await fs.writeFile(path.join(folder, 'case.json'), `${JSON.stringify(caseMeta, null, 2)}\n`, 'utf8');
      if (writeRecorded) {
        await fs.writeFile(path.join(folder, 'claude.json'), `${JSON.stringify(spec.expected, null, 2)}\n`, 'utf8');
      }
      if (browser) {
        await renderPng(browser, renderHtml(spec), path.join(folder, 'invoice.png'));
      }
      console.log(`Generated ${spec.id} (${spec.template}, split=${spec.split})`);
    }
  } finally {
    if (browser) {
      await browser.close();
    }
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
