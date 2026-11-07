import { expect, test } from '@playwright/test';

// Runs after every deploy to dev (deploy-dev.yml). It checks what a deploy can break without a
// person signing in: the web app loads and points at the right sign-in, the API answers the web
// app's origin, and, when a read token is configured, the API serves a studio through it.
// Signing in itself needs an interactive External ID login, so it isn't automated here.

const apiUrl = process.env.SMOKE_API_URL;
const token = process.env.SMOKE_TOKEN;
const studioId = process.env.SMOKE_STUDIO_ID;

test('the landing page loads without errors', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => {
    if (message.type() === 'error') {
      errors.push(message.text());
    }
  });

  await page.goto('/');

  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
  expect(errors).toEqual([]);
});

test('Sign in goes to the External ID authority', async ({ page }) => {
  await page.goto('/');
  // Stop at the identity provider: the request leaving the site is what's checked
  const leaving = page.waitForRequest((request) => new URL(request.url()).hostname.endsWith('.ciamlogin.com'));
  await page.getByRole('button', { name: 'Sign in' }).click();
  const request = await leaving;

  const url = new URL(request.url());
  expect(url.protocol).toBe('https:');
  expect(url.hostname.endsWith('.ciamlogin.com')).toBe(true);
});

test('a deep link serves the app, not a 404', async ({ page }) => {
  const response = await page.goto('/studios');
  expect(response?.status()).toBe(200);
  await expect(page.locator('#root')).not.toBeEmpty();
});

test('the API is ready and allows the web origin', async ({ request, baseURL }) => {
  test.skip(!apiUrl, 'SMOKE_API_URL not set');
  const origin = new URL(baseURL ?? '').origin;

  await expect
    .poll(async () => (await request.get(`${apiUrl ?? ''}/actuator/health/readiness`)).status(), {
      timeout: 90_000,
    })
    .toBe(200);

  const preflight = await request.fetch(`${apiUrl ?? ''}/api/v1/me`, {
    method: 'OPTIONS',
    headers: {
      Origin: origin,
      'Access-Control-Request-Method': 'GET',
      'Access-Control-Request-Headers': 'authorization',
    },
  });
  expect(preflight.headers()['access-control-allow-origin']).toBe(origin);
});

test('a read token reaches the studio', async ({ request }) => {
  test.skip(!apiUrl || !token || !studioId, 'SMOKE_TOKEN and SMOKE_STUDIO_ID not set');
  const headers = { Authorization: `Bearer ${token ?? ''}` };
  const base = `${apiUrl ?? ''}/api/v1/studios/${studioId ?? ''}`;

  const permissions = await request.get(`${base}/me/permissions`, { headers });
  expect(permissions.status()).toBe(200);
  const body = (await permissions.json()) as { permissions: string[] };
  expect(body.permissions).toContain('studio.read');

  const kilns = await request.get(`${base}/kilns`, { headers });
  expect(kilns.status()).toBe(200);
});
