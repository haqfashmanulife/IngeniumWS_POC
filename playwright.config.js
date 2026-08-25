import { defineConfig } from '@playwright/test';

const requestedWorkers = Number(process.env.PLAYWRIGHT_WORKERS || '5');
const workerCount = Number.isFinite(requestedWorkers)
  ? Math.max(1, Math.min(10, requestedWorkers))
  : 10;

export default defineConfig({
  timeout: 3600000,
  expect: { timeout: 60000 },
  fullyParallel: true,
  workers: workerCount,
  retries: 0,
  reporter: [['list']],
  use: {
    browserName: 'chromium',
    headless: true,
    ignoreHTTPSErrors: true,
    viewport: { width: 1920, height: 1080 },
    actionTimeout: 30000,
    navigationTimeout: 120000,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'off',
    launchOptions: {
      args: ['--no-sandbox', '--disable-dev-shm-usage']
    }
  }
});
