import { defineConfig } from '@playwright/test';

// Non-SSO configuration: use the bundled Playwright Chromium browser.
// Do not set channel: 'msedge' because Jenkins installs Chromium only.
export default defineConfig({
  timeout: 3600000,
  expect: {
    timeout: 60000
  },
  use: {
    browserName: 'chromium',
    headless: true,
    ignoreHTTPSErrors: true,
    viewport: {
      width: 1920,
      height: 1080
    },
    actionTimeout: 30000,
    navigationTimeout: 120000,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'off',
    launchOptions: {
      args: [
        '--no-sandbox',
        '--disable-dev-shm-usage'
      ]
    }
  },
  workers: 1,
  retries: 0,
  reporter: [
    ['list']
  ]
});
