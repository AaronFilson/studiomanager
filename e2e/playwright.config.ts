import { defineConfig, devices } from '@playwright/test';

// The deployed environment under test. deploy-dev.yml sets both from Terraform's outputs.
const webUrl = process.env.SMOKE_WEB_URL;
if (!webUrl) {
  throw new Error('Set SMOKE_WEB_URL to the web app, such as https://<name>.azurestaticapps.net');
}

export default defineConfig({
  testDir: 'tests',
  // The API may be waking from zero; its own requests retry inside the tests
  timeout: 120_000,
  retries: 1,
  workers: 1,
  forbidOnly: true,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: webUrl,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
