import { defineConfig } from '@playwright/test';

const basicAuthUsername = process.env.BASIC_AUTH_USERNAME;
const basicAuthPassword = process.env.BASIC_AUTH_PASSWORD;

if (!basicAuthUsername || !basicAuthPassword) {
  throw new Error('Missing BASIC_AUTH_USERNAME or BASIC_AUTH_PASSWORD. Configure Jenkins credential ID: ingenium-basic-auth');
}

export default defineConfig({
  timeout: 900000,

  expect: {
    timeout: 30000
  },

  use: {
    browserName: 'chromium',
    headless: true,
    ignoreHTTPSErrors: true,

    // Handles the browser-level Sign in popup for:
    // https://azlapdnpingjp01.mfcgd.com:9469
    // This is separate from Ingenium application login.
    httpCredentials: {
      username: basicAuthUsername,
      password: basicAuthPassword
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
