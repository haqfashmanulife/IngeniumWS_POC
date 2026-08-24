import { defineConfig } from '@playwright/test';

const rawUsername = process.env.BASIC_AUTH_USERNAME || '';
const password = process.env.BASIC_AUTH_PASSWORD || '';
const domain = process.env.BASIC_AUTH_DOMAIN || 'MFCGD';
const usernameForHttp = rawUsername.includes('\\') || rawUsername.includes('@')
  ? rawUsername
  : `${domain}\\${rawUsername}`;

const workerCount=Math.max(1,Math.min(Number(process.env.PLAYWRIGHT_WORKERS||'2'),10));
const spnegoAllowlist = '*mfcgd.com,azlapdnpingjp01.mfcgd.com';

export default defineConfig({
  timeout: 3600000,
  expect: { timeout: 60000 },
  use: {
    browserName: 'chromium',
    channel: 'msedge',
    headless: true,
    ignoreHTTPSErrors: true,
    httpCredentials: rawUsername && password ? {
      username: usernameForHttp,
      password
    } : undefined,
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36 Edg/151.0.4129.78',
    launchOptions: {
      args: [
        `--auth-server-allowlist=${spnegoAllowlist}`,
        `--auth-negotiate-delegate-allowlist=${spnegoAllowlist}`,
        `--auth-server-whitelist=${spnegoAllowlist}`,
        `--auth-negotiate-delegate-whitelist=${spnegoAllowlist}`,
        '--auth-schemes=basic,digest,ntlm,negotiate',
        '--enable-auth-negotiate-port',
        '--no-sandbox',
        '--disable-dev-shm-usage'
      ]
    },
    viewport: { width: 1920, height: 1080 },
    actionTimeout: 30000,
    navigationTimeout: 120000,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'off'
  },
  fullyParallel: true,
  workers: workerCount,
  retries: 0,
  reporter: [['list']]
});
