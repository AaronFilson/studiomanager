import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, URL } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(__dirname, '../..');
const DEFAULT_OUT_DIR = path.join(REPO_ROOT, 'docs/screenshots');
const VIEWPORT = { width: 1440, height: 900 };

const LOADING_LABELS = [
  'Starting the studio server.',
  'Taking you to sign in…',
  'Loading studio…',
  'Loading today…',
  'Loading pieces…',
  'Loading kilns…',
  'Loading kiln…',
  'Loading firings…',
  'Loading the current firing…',
  'Loading the invoice…',
  'Loading the limits…',
  'Loading the readers…',
  'Loading the history…',
];

function printUsage() {
  console.log(`Capture deterministic 1440x900 light-mode README screenshots into docs/screenshots/.

Usage:
  node e2e/scripts/capture-screenshots.mjs \\
    --base-url <https://web-url> \\
    --studio-id <studio-uuid> \\
    --storage-state <path/to/storage-state.json>

Options:
  --base-url <url>        Web app origin (env: SCREENSHOT_BASE_URL or SMOKE_WEB_URL)
  --studio-id <uuid>      Demo studio UUID (env: SCREENSHOT_STUDIO_ID or SMOKE_STUDIO_ID;
                          if omitted, auto-discovers "Demo Clay Co." from /studios)
  --storage-state <file>  Playwright storageState JSON for an authenticated staff session
                          (env: SCREENSHOT_STORAGE_STATE)
  --kiln-id <uuid>        Optional kiln UUID for kiln-load.png (env: SCREENSHOT_KILN_ID;
                          if omitted, picks an active kiln from /today or first kiln on /kilns)
  --out <dir>             Output directory (default: docs/screenshots)
  --only <names>          Comma-separated subset of screenshot basenames (e.g. today,pieces)
  --skip-compare          Do not click "Compare readers" on invoice-compare-readers.png
  --full-page             Capture full scrollable page instead of 1440x900 viewport
  -h, --help              Show this help and exit
`);
}

function parseArgs(argv) {
  const options = {
    baseUrl: process.env.SCREENSHOT_BASE_URL ?? process.env.SMOKE_WEB_URL ?? '',
    studioId: process.env.SCREENSHOT_STUDIO_ID ?? process.env.SMOKE_STUDIO_ID ?? '',
    storageState: process.env.SCREENSHOT_STORAGE_STATE ?? '',
    kilnId: process.env.SCREENSHOT_KILN_ID ?? '',
    outDir: process.env.SCREENSHOT_OUT_DIR ? path.resolve(process.env.SCREENSHOT_OUT_DIR) : DEFAULT_OUT_DIR,
    only: null,
    skipCompare: false,
    fullPage: false,
    help: false,
  };

  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === '-h' || arg === '--help') {
      options.help = true;
    } else if (arg === '--base-url' && argv[i + 1]) {
      options.baseUrl = argv[++i];
    } else if (arg === '--studio-id' && argv[i + 1]) {
      options.studioId = argv[++i];
    } else if (arg === '--storage-state' && argv[i + 1]) {
      options.storageState = argv[++i];
    } else if (arg === '--kiln-id' && argv[i + 1]) {
      options.kilnId = argv[++i];
    } else if (arg === '--out' && argv[i + 1]) {
      options.outDir = path.resolve(argv[++i]);
    } else if (arg === '--only' && argv[i + 1]) {
      options.only = new Set(
        argv[++i]
          .split(',')
          .map((item) => item.trim().replace(/\.png$/i, ''))
          .filter(Boolean),
      );
    } else if (arg === '--skip-compare') {
      options.skipCompare = true;
    } else if (arg === '--full-page') {
      options.fullPage = true;
    } else {
      throw new Error(`Unknown or incomplete option: ${arg}`);
    }
  }

  return options;
}

function normalizeBaseUrl(raw) {
  const parsed = new URL(raw);
  return parsed.origin + parsed.pathname.replace(/\/+$/, '');
}

async function assertSignedIn(page, targetUrl) {
  const current = new URL(page.url());
  if (current.hostname.endsWith('.ciamlogin.com') || current.hostname.endsWith('.microsoftonline.com')) {
    throw new Error(
      `Redirected to sign-in (${current.hostname}) while visiting ${targetUrl}. Refresh your --storage-state session and retry.`,
    );
  }
  const signInButton = page.getByRole('button', { name: 'Sign in' });
  if (await signInButton.isVisible().catch(() => false)) {
    throw new Error(
      `Landing page "Sign in" button is visible at ${targetUrl}. Provide a valid authenticated --storage-state file.`,
    );
  }
}

async function waitForSettled(page) {
  await page.waitForLoadState('networkidle', { timeout: 15_000 }).catch(() => undefined);

  for (const label of LOADING_LABELS) {
    const indicator = page.getByText(label, { exact: false });
    if ((await indicator.count()) > 0) {
      await indicator.first().waitFor({ state: 'hidden', timeout: 65_000 }).catch(() => undefined);
    }
  }

  await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => undefined);

  // Wait for fonts, visible images, and two animation frames so layouts are deterministic.
  await page
    .evaluate(async () => {
      const doc = globalThis.document;
      if (doc?.fonts?.ready) {
        await doc.fonts.ready;
      }
      const images = Array.from(doc?.querySelectorAll('img') ?? []);
      await Promise.all(
        images.map((img) => {
          img.loading = 'eager';
          if (img.complete) {
            return Promise.resolve();
          }
          return new Promise((resolve) => {
            img.addEventListener('load', resolve, { once: true });
            img.addEventListener('error', resolve, { once: true });
          });
        }),
      );
      if (typeof globalThis.requestAnimationFrame === 'function') {
        await new Promise((resolve) => globalThis.requestAnimationFrame(() => globalThis.requestAnimationFrame(resolve)));
      }
    })
    .catch(() => undefined);
}

async function openRoute(page, baseUrl, routePath) {
  const targetUrl = `${baseUrl}${routePath}`;
  await page.goto(targetUrl, { waitUntil: 'domcontentloaded', timeout: 60_000 });
  await waitForSettled(page);
  await assertSignedIn(page, targetUrl);
}

async function resolveStudioId(page, baseUrl, explicitStudioId) {
  if (explicitStudioId) {
    return explicitStudioId;
  }
  await openRoute(page, baseUrl, '/studios');
  const demoLink = page.getByRole('link', { name: /Demo Clay Co\./i }).first();
  let href = null;
  if (await demoLink.isVisible().catch(() => false)) {
    href = await demoLink.getAttribute('href');
  } else {
    const anyStudio = page.locator('a[href^="/studios/"]').first();
    if (await anyStudio.isVisible().catch(() => false)) {
      href = await anyStudio.getAttribute('href');
    }
  }
  const match = /^\/studios\/([^/?#]+)/.exec(href ?? '');
  if (!match?.[1]) {
    throw new Error('Could not discover a studio ID from /studios. Pass --studio-id <uuid> explicitly.');
  }
  console.log(`Discovered studio ID: ${match[1]}`);
  return match[1];
}

async function resolveKilnId(page, baseUrl, studioId, explicitKilnId) {
  if (explicitKilnId) {
    return explicitKilnId;
  }
  // Prefer a kiln that is currently loading or firing on /today so kiln-load.png shows a loaded firing.
  await openRoute(page, baseUrl, `/studios/${studioId}/today`);
  const activeItem = page
    .locator('section[aria-label="Kilns"] li')
    .filter({ hasText: /\b(loading|firing)\b/i })
    .first();
  if (await activeItem.isVisible().catch(() => false)) {
    const href = await activeItem.locator('a').first().getAttribute('href');
    const match = /\/kilns\/([^/?#]+)/.exec(href ?? '');
    if (match?.[1]) {
      console.log(`Selected active kiln from /today: ${match[1]}`);
      return match[1];
    }
  }

  // Fallback to the first kiln listed on /studios/{id}/kilns.
  await openRoute(page, baseUrl, `/studios/${studioId}/kilns`);
  const firstKiln = page.locator('ul.studio-list a[href*="/kilns/"]').first();
  const href = (await firstKiln.isVisible().catch(() => false)) ? await firstKiln.getAttribute('href') : null;
  const match = /\/kilns\/([^/?#]+)/.exec(href ?? '');
  if (!match?.[1]) {
    throw new Error('Could not discover a kiln ID for kiln-load.png. Pass --kiln-id <uuid> explicitly.');
  }
  console.log(`Selected kiln from /kilns: ${match[1]}`);
  return match[1];
}

async function prepareInvoiceCompare(page, skipCompare) {
  const invoicesSection = page.locator('section', { has: page.getByRole('heading', { level: 2, name: 'Invoices' }) });
  if (!(await invoicesSection.isVisible().catch(() => false))) {
    console.warn('  warning: Invoices section not visible on /supplies (requires invoices.manage permission).');
    return;
  }
  await invoicesSection.scrollIntoViewIfNeeded();
  const openButton = invoicesSection.getByRole('button', { name: 'Open' }).first();
  if (!(await openButton.isVisible().catch(() => false))) {
    console.warn('  warning: No invoices found in the supplies inbox; capturing empty inbox.');
    return;
  }

  await openButton.click();
  await waitForSettled(page);

  const compareSection = page.locator('section', {
    has: page.getByRole('heading', { level: 3, name: 'Compare readers' }),
  });
  if (!(await compareSection.isVisible().catch(() => false))) {
    console.warn('  warning: "Compare readers" section not visible (requires owner studio.ai_key.manage permission).');
    return;
  }

  if (!skipCompare) {
    const compareButton = compareSection.getByRole('button', { name: 'Compare readers' });
    if (await compareButton.isEnabled().catch(() => false)) {
      await compareButton.click();
      const readingButton = compareSection.getByRole('button', { name: 'Reading…' });
      await readingButton.waitFor({ state: 'hidden', timeout: 75_000 }).catch(() => undefined);
      await waitForSettled(page);
    }
  }

  await compareSection.scrollIntoViewIfNeeded();
}

async function captureScreen(page, outDir, fileName, fullPage) {
  const outPath = path.join(outDir, fileName);
  await page.screenshot({ path: outPath, fullPage, animations: 'disabled' });
  console.log(`Saved ${path.relative(REPO_ROOT, outPath)}`);
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    printUsage();
    return;
  }
  if (!options.baseUrl) {
    printUsage();
    throw new Error('Missing --base-url (or SCREENSHOT_BASE_URL / SMOKE_WEB_URL).');
  }
  if (!options.storageState) {
    printUsage();
    throw new Error('Missing --storage-state (or SCREENSHOT_STORAGE_STATE) pointing to a signed-in Playwright session.');
  }

  const baseUrl = normalizeBaseUrl(options.baseUrl);
  const storageStatePath = path.resolve(options.storageState);
  await fs.access(storageStatePath);
  await fs.mkdir(options.outDir, { recursive: true });

  const { chromium } = await import('@playwright/test');
  const browser = await chromium.launch({ headless: true });

  try {
    const context = await browser.newContext({
      viewport: VIEWPORT,
      deviceScaleFactor: 1,
      colorScheme: 'light',
      reducedMotion: 'reduce',
      storageState: storageStatePath,
    });
    const page = await context.newPage();

    const studioId = await resolveStudioId(page, baseUrl, options.studioId);
    const shouldCapture = (key) => !options.only || options.only.has(key);

    if (shouldCapture('today')) {
      await openRoute(page, baseUrl, `/studios/${studioId}/today`);
      await page.getByRole('heading', { level: 1, name: /^Today,/ }).waitFor({ state: 'visible', timeout: 30_000 });
      await captureScreen(page, options.outDir, 'today.png', options.fullPage);
    }

    if (shouldCapture('pieces')) {
      await openRoute(page, baseUrl, `/studios/${studioId}/pieces`);
      await page.getByRole('heading', { level: 1, name: /^(Pieces|My pieces)$/ }).waitFor({ state: 'visible', timeout: 30_000 });
      await captureScreen(page, options.outDir, 'pieces.png', options.fullPage);
    }

    if (shouldCapture('kiln-load')) {
      const kilnId = await resolveKilnId(page, baseUrl, studioId, options.kilnId);
      await openRoute(page, baseUrl, `/studios/${studioId}/kilns/${kilnId}`);
      await page.getByRole('heading', { level: 1 }).waitFor({ state: 'visible', timeout: 30_000 });
      await captureScreen(page, options.outDir, 'kiln-load.png', options.fullPage);
    }

    if (shouldCapture('invoice-compare-readers')) {
      await openRoute(page, baseUrl, `/studios/${studioId}/supplies`);
      await page.getByRole('heading', { level: 1, name: 'Clay and supplies' }).waitFor({ state: 'visible', timeout: 30_000 });
      await prepareInvoiceCompare(page, options.skipCompare);
      await captureScreen(page, options.outDir, 'invoice-compare-readers.png', options.fullPage);
    }

    if (shouldCapture('admin-limits')) {
      await openRoute(page, baseUrl, '/admin');
      await page.getByRole('heading', { level: 1, name: 'Admin' }).waitFor({ state: 'visible', timeout: 30_000 });
      await captureScreen(page, options.outDir, 'admin-limits.png', options.fullPage);
    }

    if (shouldCapture('assistant-suggestions')) {
      await openRoute(page, baseUrl, `/studios/${studioId}/suggestions`);
      await page.getByRole('heading', { level: 1, name: 'Assistant suggestions' }).waitFor({ state: 'visible', timeout: 30_000 });
      await captureScreen(page, options.outDir, 'assistant-suggestions.png', options.fullPage);
    }

    await context.close();
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
});
