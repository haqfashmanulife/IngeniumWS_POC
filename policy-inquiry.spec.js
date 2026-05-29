import { test, expect } from '@playwright/test';
import fs from 'fs';

test('Ingenium Policy Inquiry - Stable Flow', async ({ page }) => {

  const BASE_URL   = process.env.APP_URL;
  const USERNAME   = process.env.APP_USERNAME;
  const PASSWORD   = process.env.APP_PASSWORD;
  const COMPANY    = process.env.COMPANY || 'Manulife';
  const POLICY_ID  = process.env.POLICY_ID;

  expect(POLICY_ID).toBeTruthy();

  // ======================================================
  // ✅ STEP 1: Launch App
  // ======================================================
  await page.goto(BASE_URL, { waitUntil: 'networkidle' });

  await page.screenshot({ path: 'screenshots/01-launch.png', fullPage: true });
  console.log("URL after launch:", page.url());

  // ======================================================
  // ✅ STEP 2: Click English Sign On (if exists)
  // ======================================================
  const englishLink = page.getByText('English Sign On');

  if (await englishLink.isVisible().catch(() => false)) {
    await englishLink.click();
    console.log("Clicked English Sign On");
  } else {
    console.log("English Sign On NOT found → skipping");
  }

  await page.waitForTimeout(5000);

  // ======================================================
  // ✅ STEP 3: Find LOGIN FRAME dynamically
  // ======================================================
  let loginFrame = null;

  for (const frame of page.frames()) {
    if (await frame.locator('input[type="password"]').count() > 0) {
      loginFrame = frame;
      break;
    }
  }

  // ======================================================
  // ✅ STEP 4: Perform LOGIN (only if needed)
  // ======================================================
  if (loginFrame) {

    console.log("Login frame detected");

    await loginFrame.locator('input[type="text"]').first().fill(USERNAME);
    await loginFrame.locator('input[type="password"]').fill(PASSWORD);
    await loginFrame.locator('select').selectOption({ label: COMPANY });

    await loginFrame.getByRole('button', { name: /submit/i }).click();

    console.log("Login submitted");

    await page.waitForTimeout(5000);

  } else {
    console.log("✅ Already logged in (no login form found)");
  }

  await page.screenshot({ path: 'screenshots/02-after-login.png', fullPage: true });

  // ======================================================
  // ✅ STEP 5: Handle POST-LOGIN OK popup
  // ======================================================
  for (const frame of page.frames()) {
    if (await frame.getByRole('button', { name: 'OK' }).count() > 0) {
      try {
        await frame.getByRole('button', { name: 'OK' }).click();
        console.log("Clicked OK popup");
      } catch (e) {}
    }
  }

  await page.waitForTimeout(3000);

  // ======================================================
  // ✅ STEP 6: Find MAIN APP FRAME (menu present)
  // ======================================================
  let appFrame = null;

  for (const frame of page.frames()) {
    if (await frame.locator('text=Policy Inquiry').count() > 0) {
      appFrame = frame;
      break;
    }
  }

  if (!appFrame) {
    throw new Error("❌ App frame not found (menu missing)");
  }

  console.log("App frame detected");

  // ======================================================
  // ✅ STEP 7: Navigate Menu
  // ======================================================
  await appFrame.locator('text=Policy Inquiry').click();
  await appFrame.locator('text=Policy Inquiry - All Details').click();

  console.log("Navigated to Policy Inquiry");

  await page.waitForTimeout(5000);

  // ======================================================
  // ✅ STEP 8: Enter POLICY ID
  // ======================================================
  let formFrame = null;

  for (const frame of page.frames()) {
    if (await frame.locator('input').count() > 0) {
      formFrame = frame;
      break;
    }
  }

  if (!formFrame) {
    throw new Error("❌ Policy input frame not found");
  }

  await formFrame.locator('input').first().fill(POLICY_ID);

  await formFrame.getByRole('button', { name: 'OK' }).click();

  console.log("Policy ID submitted:", POLICY_ID);

  await page.waitForLoadState('networkidle');

  // ======================================================
  // ✅ STEP 9: FINAL SCREENSHOT
  // ======================================================
  const screenshotPath = `screenshots/policy-${POLICY_ID}.png`;

  await page.screenshot({
    path: screenshotPath,
    fullPage: true
  });

  console.log(`✅ Screenshot saved: ${screenshotPath}`);

});
