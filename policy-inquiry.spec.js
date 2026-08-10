import { test, expect } from '@playwright/test';
import fs from 'fs';

function parseCurlCookieJar(cookieJarPath) {
  if (!cookieJarPath || !fs.existsSync(cookieJarPath)) {
    console.log('No curl cookie jar found. COOKIE_JAR:', cookieJarPath || 'not-set');
    return [];
  }

  const lines = fs.readFileSync(cookieJarPath, 'utf8')
    .split('\n')
    .filter((line) => line.trim() && !line.startsWith('# Netscape') && !line.startsWith('# This file'));

  const cookies = [];
  for (const line of lines) {
    const httpOnly = line.startsWith('#HttpOnly_');
    const normalized = httpOnly ? line.replace('#HttpOnly_', '') : line;
    if (normalized.startsWith('#')) {
      continue;
    }

    const parts = normalized.split('\t');
    if (parts.length < 7) {
      continue;
    }

    const [domain, , path, secure, expires, name, ...valueParts] = parts;
    const value = valueParts.join('\t');
    if (!domain || !path || !name || !value) {
      continue;
    }

    cookies.push({
      domain,
      path,
      secure: secure.toUpperCase() === 'TRUE',
      expires: Number(expires) || -1,
      name,
      value,
      httpOnly,
      sameSite: 'Lax'
    });
  }

  return cookies;
}

async function loadCurlCookiesIntoContext(context) {
  const cookieJar = process.env.COOKIE_JAR;
  const cookies = parseCurlCookieJar(cookieJar);
  console.log('Cookies parsed from curl jar:', cookies.map((cookie) => cookie.name).join(', ') || 'none');

  if (cookies.length > 0) {
    await context.addCookies(cookies);
    console.log('Added curl SPNEGO cookies to Playwright context:', cookies.length);
  }
}

test('U2 SSO smoke check - reach English Sign On page with Edge on Linux', async ({ page, context }) => {
  test.setTimeout(180000);

  const BASE_URL = process.env.APP_URL;
  const POLICY_ID = process.env.POLICY_ID || '8885442';
  const MAJOR_POLICY_ID = process.env.MAJOR_POLICY_ID || POLICY_ID;

  expect(BASE_URL).toBeTruthy();

  const responseLines = [];
  page.on('response', async (response) => {
    const status = response.status();
    const url = response.url();
    if (status >= 300 || /ping|sso|ingenium|mfcgd/i.test(url)) {
      responseLines.push(`${status} ${url}`);
      console.log('RESPONSE:', status, url);
    }
  });

  console.log('START U2 SSO SMOKE TEST');
  console.log('BASE_URL:', BASE_URL);
  console.log('POLICY_ID:', POLICY_ID);
  console.log('MAJOR_POLICY_ID:', MAJOR_POLICY_ID);
  console.log('BROWSER: Microsoft Edge on Linux');
  console.log('KRB_REALM:', process.env.KRB_REALM || 'MFCGD.COM');
  console.log('KRB5CCNAME:', process.env.KRB5CCNAME || 'not-set');
  console.log('COOKIE_JAR:', process.env.COOKIE_JAR || 'not-set');

  await loadCurlCookiesIntoContext(context);

  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded', timeout: 120000 });
  await page.waitForTimeout(5000);
  await page.screenshot({ path: 'screenshots/01-launch.png', fullPage: true });

  const pageText = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '');
  fs.writeFileSync('screenshots/01-launch-text.txt', pageText, 'utf8');
  fs.writeFileSync('screenshots/response-log.txt', responseLines.join('\n'), 'utf8');

  const spnegoError = page.getByText('SPNEGO authentication is not supported on this client.');
  if (await spnegoError.isVisible().catch(() => false)) {
    await page.screenshot({ path: 'screenshots/spnego-not-supported.png', fullPage: true });
    throw new Error('SPNEGO page still displayed after cookie bridge. Check curl-authenticated.html, cookies.masked.txt, 01-launch-text.txt and response-log.txt.');
  }

  const englishSignOn = page.getByText('English Sign On', { exact: true });
  await expect(englishSignOn).toBeVisible({ timeout: 60000 });
  console.log('English Sign On page visible');

  await page.screenshot({ path: 'screenshots/02-english-sign-on-visible.png', fullPage: true });
  console.log('U2 SSO smoke check completed successfully');
});
