import { test, expect } from '@playwright/test';

test('U2 SSO smoke check - reach English Sign On page with Edge on Linux', async ({ page }) => {
  test.setTimeout(180000);

  const BASE_URL = process.env.APP_URL;
  const POLICY_ID = process.env.POLICY_ID || '8885442';
  const MAJOR_POLICY_ID = process.env.MAJOR_POLICY_ID || POLICY_ID;

  expect(BASE_URL).toBeTruthy();

  console.log('START U2 SSO SMOKE TEST');
  console.log('BASE_URL:', BASE_URL);
  console.log('POLICY_ID:', POLICY_ID);
  console.log('MAJOR_POLICY_ID:', MAJOR_POLICY_ID);
  console.log('BROWSER: Microsoft Edge on Linux');
  console.log('KRB_REALM:', process.env.KRB_REALM || 'MFCGD');

  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded', timeout: 120000 });
  await page.waitForTimeout(5000);
  await page.screenshot({ path: 'screenshots/01-launch.png', fullPage: true });

  const spnegoError = page.getByText('SPNEGO authentication is not supported on this client.');
  if (await spnegoError.isVisible().catch(() => false)) {
    await page.screenshot({ path: 'screenshots/spnego-not-supported.png', fullPage: true });
    throw new Error('SPNEGO authentication is not supported on this client. Kerberos ticket, Edge allowlist, or Linux SPNEGO setup is not accepted yet.');
  }

  const englishSignOn = page.getByText('English Sign On', { exact: true });
  await expect(englishSignOn).toBeVisible({ timeout: 60000 });
  console.log('English Sign On page visible');

  await page.screenshot({ path: 'screenshots/02-english-sign-on-visible.png', fullPage: true });

  console.log('U2 SSO smoke check completed successfully');
});
