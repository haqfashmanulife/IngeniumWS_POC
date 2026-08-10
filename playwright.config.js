import { defineConfig } from '@playwright/test';

const basicAuthUsername = process.env.BASIC_AUTH_USERNAME;
const basicAuthPassword = process.env.BASIC_AUTH_PASSWORD;

export default defineConfig({
  timeout: 180000,

  expect: {
    timeout: 60000
  },

  use: {
    browserName: 'chromium',
    headless: true,
    ignoreHTTPSErrors: true,

    // If the endpoint falls back to Basic/Digest auth, Playwright can answer it.
    // If the endpoint uses Windows Integrated Authentication/SPNEGO, the Windows agent domain session handles it.
    httpCredentials: basicAuthUsername && basicAuthPassword ? {
      username: basicAuthUsername,
      password: basicAuthPassword
    } : undefined,

    launchOptions: {
      args: [
        '--auth-server-allowlist=azlapdnpingjp01.mfcgd.com',
        '--auth-negotiate-delegate-allowlist=azlapdnpingjp01.mfcgd.com',
        '--enable-auth-negotiate-port'
      ]
    },

    viewport: {
      width: 1920,
      height: 1080
    },

    actionTimeout: 30000,
    navigationTimeout: 120000,

    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'off'
  },

  workers: 1,
  retries: 0,
  reporter: [['list']]
});
