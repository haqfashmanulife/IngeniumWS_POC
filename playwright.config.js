import { defineConfig } from '@playwright/test';

const rawUsername = process.env.BASIC_AUTH_USERNAME || '';
const password = process.env.BASIC_AUTH_PASSWORD || '';
const domain = process.env.BASIC_AUTH_DOMAIN || 'MFCGD';
const usernameForHttp = rawUsername.includes('\\') || rawUsername.includes('@')
  ? rawUsername
  : `${domain}\\${rawUsername}`;

export default defineConfig({
  timeout: 180000,

  expect: {
    timeout: 60000
  },

  use: {
    browserName: 'chromium',
    channel: 'msedge',
    headless: true,
    ignoreHTTPSErrors: true,

    // If the server falls back to Basic/NTLM-style username-password auth,
    // send the manager-suggested domain-qualified user, for example MFCGD\\haqfash.
    // For true SPNEGO/Kerberos, the kinit ticket from Jenkinsfile is the primary auth mechanism.
    httpCredentials: rawUsername && password ? {
      username: usernameForHttp,
      password
    } : undefined,

    launchOptions: {
      args: [
        '--auth-server-allowlist=azlapdnpingjp01.mfcgd.com',
        '--auth-negotiate-delegate-allowlist=azlapdnpingjp01.mfcgd.com',
        '--enable-auth-negotiate-port',
        '--no-sandbox',
        '--disable-dev-shm-usage'
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
