import { test, expect } from '@playwright/test';
import fs from 'fs';

test('U2 SSO smoke check - reach English Sign On page with Edge on Linux', async ({ page }) => {
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

  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded', timeout: 120000 });
  await page.waitForTimeout(5000);
  await page.screenshot({ path: 'screenshots/01-launch.png', fullPage: true });

  const pageText = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '');
  fs.writeFileSync('screenshots/01-launch-text.txt', pageText, 'utf8');
  fs.writeFileSync('screenshots/response-log.txt', responseLines.join('\n'), 'utf8');

  const spnegoError = page.getByText('SPNEGO authentication is not supported on this client.');
  if (await spnegoError.isVisible().catch(() => false)) {
    await page.screenshot({ path: 'screenshots/spnego-not-supported.png', fullPage: true });
    throw new Error('SPNEGO authentication is still not accepted by the SSO endpoint. Kerberos ticket exists, so check Edge allowlist/user-agent/SSO browser policy. See 01-launch-text.txt and response-log.txt.');
  }

  const englishSignOn = page.getByText('English Sign On', { exact: true });
  await expect(englishSignOn).toBeVisible({ timeout: 60000 });
  console.log('English Sign On page visible');

  await page.screenshot({ path: 'screenshots/02-english-sign-on-visible.png', fullPage: true });
  console.log('U2 SSO smoke check completed successfully');
});
