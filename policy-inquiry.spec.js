import { test, expect } from '@playwright/test';
import fs from 'fs';

test('Ingenium Policy Inquiry - All Details', async ({ page }) => {

  // ---------- Inputs from Jenkins ----------
  const BASE_URL   = process.env.APP_URL;
  const USERNAME   = process.env.APP_USERNAME;
  const PASSWORD   = process.env.APP_PASSWORD;
  const COMPANY    = process.env.COMPANY || 'Manulife';
  const POLICY_ID  = process.env.POLICY_ID;

  // ---------- Safety checks ----------
  expect(POLICY_ID).toBeTruthy();

  // ---------- Step 1: Launch app ----------
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });

  // ---------- Step 2: English Sign On ----------
  await page.getByRole('link', { name: 'English Sign On' }).click();

  // ---------- Step 3: Login ----------
  await page.fill('input[type="text"]', USERNAME);
  await page.fill('input[type="password"]', PASSWORD);
  await page.selectOption('select', { label: COMPANY });
  await page.getByRole('button', { name: /submit/i }).click();

  // ---------- Step 4: Post-login OK ----------
  await expect(page.locator('text=User ID')).toBeVisible();
  await page.getByRole('button', { name: 'OK' }).click();

  // ---------- Step 5: Menu → Policy Inquiry → All Details ----------
  await page.locator('text=Policy Inquiry').click();
  await page.locator('text=Policy Inquiry - All Details').click();

  // ---------- Step 6: Enter Policy ID ----------
  const policyInput = page.locator('input').first();
  await policyInput.fill(POLICY_ID);

  // ---------- Step 7: Submit ----------
  await page.getByRole('button', { name: 'OK' }).click();

  // ---------- Step 8: Validate & Screenshot ----------
  await page.waitForLoadState('networkidle');

  const screenshotPath = `screenshots/policy-${POLICY_ID}.png`;
  await page.screenshot({ path: screenshotPath, fullPage: true });

  console.log(`Screenshot saved: ${screenshotPath}`);
});
