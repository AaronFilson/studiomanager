import { expect, test } from '@playwright/test';

// End-to-end and API contract checks for outbound webhooks (docs/planning/10-webhooks-plan.md).
// Safe to run against live dev environments (deploy-dev.yml):
// - Personal API tokens (SMOKE_TOKEN) are intentionally refused on /webhooks/* by TokenPolicy.
// - Staff webhook API tests require an interactive Entra ID JWT in SMOKE_STAFF_TOKEN (or a JWT
//   in SMOKE_TOKEN) plus SMOKE_API_URL and SMOKE_STUDIO_ID.
// - Live delivery creation/ping/log tests additionally require WEBHOOK_TEST_URL and always delete
//   any created endpoint in `finally`.
// - Optional UI flow runs when WEBHOOK_STORAGE_STATE (Playwright storageState JSON) is set.

const apiUrl = process.env.SMOKE_API_URL;
const studioId = process.env.SMOKE_STUDIO_ID;
const smokeToken = process.env.SMOKE_TOKEN;
const explicitStaffToken = process.env.SMOKE_STAFF_TOKEN;
const webhookTestUrl = process.env.WEBHOOK_TEST_URL;
const storageStatePath = process.env.WEBHOOK_STORAGE_STATE;

function isJwt(token: string): boolean {
  return token.split('.').length === 3;
}

const staffToken = explicitStaffToken ?? (smokeToken && isJwt(smokeToken) ? smokeToken : undefined);
const personalApiToken = smokeToken && !isJwt(smokeToken) ? smokeToken : undefined;

interface ProblemDetail {
  detail?: string;
  title?: string;
  status?: number;
}

interface WebhookEventTypeInfo {
  type: string;
  description: string;
  example: Record<string, unknown>;
}

interface WebhookEndpoint {
  id: string;
  url: string;
  description?: string;
  eventTypes: string[];
  enabled: boolean;
  consecutiveFailures: number;
  createdAt: string;
}

interface CreatedWebhookEndpoint {
  endpoint: WebhookEndpoint;
  secret: string;
}

interface WebhookAttempt {
  attemptedAt: string;
  statusCode?: number;
  error?: string;
  durationMs: number;
  responseExcerpt?: string;
}

interface WebhookDelivery {
  id: string;
  eventId: string;
  eventType: string;
  status: 'PENDING' | 'DELIVERED' | 'FAILED';
  attempts: number;
  createdAt: string;
  attemptLog: WebhookAttempt[];
}

interface WebhookDeliveryList {
  items: WebhookDelivery[];
  truncated: boolean;
  limit: number;
}

test('the webhooks deep link serves the app shell', async ({ page }) => {
  const targetStudio = studioId ?? '00000000-0000-0000-0000-000000000000';
  const response = await page.goto(`/studios/${targetStudio}/webhooks`);
  expect(response?.status()).toBe(200);
  await expect(page.locator('#root')).not.toBeEmpty();
});

test('personal API tokens are refused on studio webhooks (TokenPolicy)', async ({ request }) => {
  test.skip(
    !apiUrl || !personalApiToken || !studioId,
    'SMOKE_API_URL, personal API SMOKE_TOKEN, and SMOKE_STUDIO_ID not set',
  );
  const response = await request.get(`${apiUrl ?? ''}/api/v1/studios/${studioId ?? ''}/webhooks/endpoints`, {
    headers: { Authorization: `Bearer ${personalApiToken ?? ''}` },
  });
  expect(response.status()).toBe(403);
});

test('lists webhook event types with versioned example payloads', async ({ request }) => {
  test.skip(
    !apiUrl || !staffToken || !studioId,
    'SMOKE_API_URL, staff JWT (SMOKE_STAFF_TOKEN), and SMOKE_STUDIO_ID not set',
  );
  const headers = { Authorization: `Bearer ${staffToken ?? ''}` };
  const base = `${apiUrl ?? ''}/api/v1/studios/${studioId ?? ''}`;

  const response = await request.get(`${base}/webhooks/event-types`, { headers });
  expect(response.status()).toBe(200);
  const types = (await response.json()) as WebhookEventTypeInfo[];

  const names = types.map((entry) => entry.type);
  expect(names).toEqual(
    expect.arrayContaining(['pieces.ready', 'fee.charged', 'member.joined', 'access.changed', 'glaze.mixed', 'ping']),
  );
  for (const entry of types) {
    expect(entry.description.length).toBeGreaterThan(0);
    expect(entry.example['type']).toBe(entry.type);
    expect(entry.example['version']).toBe(1);
    expect(entry.example['studio']).toBeTruthy();
    expect(entry.example['data']).toBeTruthy();
  }
});

test('rejects private, loopback, and non-HTTPS webhook URLs (SSRF guard)', async ({ request }) => {
  test.skip(
    !apiUrl || !staffToken || !studioId,
    'SMOKE_API_URL, staff JWT (SMOKE_STAFF_TOKEN), and SMOKE_STUDIO_ID not set',
  );
  const headers = {
    Authorization: `Bearer ${staffToken ?? ''}`,
    'Content-Type': 'application/json',
  };
  const base = `${apiUrl ?? ''}/api/v1/studios/${studioId ?? ''}`;

  const unsafeUrls = [
    'https://127.0.0.1/hook',
    'https://169.254.169.254/latest/meta-data/',
    'https://10.0.0.1/hook',
    'http://hooks.example.com/hook',
    'https://hooks.example.com:9443/hook',
  ];

  for (const unsafeUrl of unsafeUrls) {
    const response = await request.post(`${base}/webhooks/endpoints`, {
      headers,
      data: {
        url: unsafeUrl,
        description: 'Playwright SSRF guard check',
        eventTypes: ['pieces.ready'],
      },
    });
    if (response.status() === 201) {
      const leaked = (await response.json()) as CreatedWebhookEndpoint;
      await request.delete(`${base}/webhooks/endpoints/${leaked.endpoint.id}`, { headers });
    }
    expect(response.status(), `Expected 400 Bad Request for ${unsafeUrl}`).toBe(400);
    const problem = (await response.json()) as ProblemDetail;
    expect(problem.detail ?? problem.title ?? '').toMatch(/\S/);
  }
});

test('creates a webhook endpoint, sends a test ping, reads deliveries, and cleans up', async ({ request }) => {
  test.skip(
    !apiUrl || !staffToken || !studioId || !webhookTestUrl,
    'SMOKE_API_URL, staff JWT (SMOKE_STAFF_TOKEN), SMOKE_STUDIO_ID, and WEBHOOK_TEST_URL not set',
  );
  const headers = {
    Authorization: `Bearer ${staffToken ?? ''}`,
    'Content-Type': 'application/json',
  };
  const base = `${apiUrl ?? ''}/api/v1/studios/${studioId ?? ''}`;
  let createdId: string | undefined;

  try {
    const createResponse = await request.post(`${base}/webhooks/endpoints`, {
      headers,
      data: {
        url: webhookTestUrl ?? '',
        description: 'Playwright e2e webhook test (auto-deleted)',
        eventTypes: ['pieces.ready'],
      },
    });
    expect(createResponse.status()).toBe(201);
    const created = (await createResponse.json()) as CreatedWebhookEndpoint;
    createdId = created.endpoint.id;
    expect(created.secret.startsWith('whsec_')).toBe(true);
    expect(created.endpoint.enabled).toBe(true);
    expect(created.endpoint.eventTypes).toEqual(['pieces.ready']);

    // Listing endpoints must include the created one and never expose the signing secret again.
    const listResponse = await request.get(`${base}/webhooks/endpoints`, { headers });
    expect(listResponse.status()).toBe(200);
    const listRaw = await listResponse.text();
    expect(listRaw).not.toContain(created.secret);
    const endpoints = JSON.parse(listRaw) as WebhookEndpoint[];
    expect(endpoints.some((ep) => ep.id === createdId)).toBe(true);

    // Send a synchronous ping delivery and confirm it appears in the delivery log.
    const pingResponse = await request.post(`${base}/webhooks/endpoints/${createdId}/test`, { headers });
    expect(pingResponse.status()).toBe(200);
    const pingDelivery = (await pingResponse.json()) as WebhookDelivery;
    expect(pingDelivery.eventType).toBe('ping');
    expect(pingDelivery.eventId.startsWith('evt_')).toBe(true);
    expect(pingDelivery.attempts).toBeGreaterThanOrEqual(1);
    expect(pingDelivery.attemptLog.length).toBeGreaterThanOrEqual(1);

    const deliveriesResponse = await request.get(`${base}/webhooks/endpoints/${createdId}/deliveries`, { headers });
    expect(deliveriesResponse.status()).toBe(200);
    const deliveries = (await deliveriesResponse.json()) as WebhookDeliveryList;
    expect(deliveries.items.some((item) => item.id === pingDelivery.id)).toBe(true);

    // Turning off an endpoint succeeds before cleanup.
    const disableResponse = await request.patch(`${base}/webhooks/endpoints/${createdId}`, {
      headers,
      data: { enabled: false },
    });
    expect(disableResponse.status()).toBe(200);
    const disabled = (await disableResponse.json()) as WebhookEndpoint;
    expect(disabled.enabled).toBe(false);
  } finally {
    if (createdId) {
      const deleteResponse = await request.delete(`${base}/webhooks/endpoints/${createdId}`, { headers });
      expect([204, 404]).toContain(deleteResponse.status());
    }
  }
});

test('webhooks UI adds an endpoint, sends a test ping, inspects the log, and deletes it', async ({ browser }) => {
  test.skip(
    !storageStatePath || !studioId || !webhookTestUrl,
    'WEBHOOK_STORAGE_STATE, SMOKE_STUDIO_ID, and WEBHOOK_TEST_URL not set',
  );
  const context = await browser.newContext({ storageState: storageStatePath ?? '' });
  const page = await context.newPage();
  const label = `Playwright UI webhook ${String(Date.now())}`;
  let createdCardVisible = false;

  try {
    await page.goto(`/studios/${studioId ?? ''}/webhooks`);
    await expect(page.getByRole('heading', { level: 1, name: 'Webhooks' })).toBeVisible({ timeout: 60_000 });

    const addForm = page.locator('form', { has: page.getByRole('heading', { name: 'Add an endpoint' }) });
    await addForm.getByLabel('URL').fill(webhookTestUrl ?? '');
    await addForm.getByLabel('Description (optional)').fill(label);
    await addForm.getByRole('button', { name: 'Add endpoint' }).click();

    const secretCard = page.locator('section[aria-label="Signing secret"]');
    await expect(secretCard).toBeVisible();
    await expect(secretCard.locator('code')).toContainText(/^whsec_/);
    await secretCard.getByRole('button', { name: 'Done' }).click();

    const card = page.locator('section.card', { has: page.getByRole('heading', { name: label }) });
    await expect(card).toBeVisible();
    createdCardVisible = true;

    await card.getByRole('button', { name: 'Send test' }).click();
    await expect(card.getByText(/^Test:/)).toBeVisible({ timeout: 30_000 });

    await card.getByRole('button', { name: 'Delivery log' }).click();
    await expect(card.getByRole('table')).toBeVisible();
    await expect(card.locator('tbody tr').first()).toContainText('Test');
  } finally {
    if (createdCardVisible) {
      const card = page.locator('section.card', { has: page.getByRole('heading', { name: label }) });
      if (await card.isVisible().catch(() => false)) {
        await card.getByRole('button', { name: 'Delete' }).click();
        await expect(card).toBeHidden({ timeout: 15_000 }).catch(() => undefined);
      }
    }
    await context.close();
  }
});
