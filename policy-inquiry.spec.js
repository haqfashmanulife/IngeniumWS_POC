import { test, expect } from '@playwright/test';

test('U2 SSO smoke check - reach English Sign On page', async ({ page }) => {
  test.setTimeout(180000);

  const BASE_URL = process.env.APP_URL;
  expect(BASE_URL).toBeTruthy();

  console.log('START U2 SSO SMOKE TEST');
  console.log('BASE_URL:', BASE_URL);

  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded', timeout: 120000 });
  await page.waitForTimeout(5000);
  await page.screenshot({ path: 'screenshots/01-launch.png', fullPage: true });

  const englishSignOn = page.getByText('English Sign On', { exact: true });
  await expect(englishSignOn).toBeVisible({ timeout: 60000 });
  console.log('English Sign On page visible');

  await page.screenshot({ path: 'screenshots/02-english-sign-on-visible.png', fullPage: true });

  console.log('U2 SSO smoke check completed successfully');
});
