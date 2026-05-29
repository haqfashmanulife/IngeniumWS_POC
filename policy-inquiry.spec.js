import { test, expect } from '@playwright/test';

test('✅ STABLE Ingenium Policy Inquiry Flow', async ({ page }) => {

  const BASE_URL   = process.env.APP_URL;
  const USERNAME   = process.env.APP_USERNAME;
  const PASSWORD   = process.env.APP_PASSWORD;
  const COMPANY    = process.env.COMPANY || 'Manulife';
  const POLICY_ID  = process.env.POLICY_ID;

  expect(POLICY_ID).toBeTruthy();

  // ======================================================
  // STEP 1: Launch App
  // ======================================================
  await page.goto(BASE_URL, { waitUntil: 'networkidle' });
  console.log("✅ URL:", page.url());

  await page.screenshot({ path: 'screenshots/01-launch.png', fullPage: true });

  // ======================================================
  // STEP 2: Click English Sign On (if present)
  // ======================================================
  const english = page.getByText('English Sign On');

  if (await english.isVisible().catch(() => false)) {
    await english.click();
    console.log("✅ Clicked English Sign On");
  }

  await page.waitForTimeout(5000);

  // ======================================================
  // STEP 3: Find LOGIN FRAME
  // ======================================================
  let loginFrame = null;

  for (const f of page.frames()) {
    if (await f.locator('input[type="password"]').count() > 0) {
      loginFrame = f;
      break;
    }
  }

  // ======================================================
  // STEP 4: Login (if needed)
  // ======================================================
  if (loginFrame) {
    console.log("✅ Login frame found");

    await loginFrame.locator('input[type="text"]').first().fill(USERNAME);
    await loginFrame.locator('input[type="password"]').fill(PASSWORD);
    await loginFrame.locator('select').selectOption({ label: COMPANY });

    await loginFrame.getByRole('button', { name: /submit/i }).click();

    console.log("✅ Login submitted");
    await page.waitForTimeout(5000);
  } else {
    console.log("✅ Already logged in");
  }

  await page.screenshot({ path: 'screenshots/02-after-login.png', fullPage: true });

  // ======================================================
  // STEP 5: Handle OK popup
  // ======================================================
  for (const f of page.frames()) {
    if (await f.getByRole('button', { name: 'OK' }).count() > 0) {
      try {
        await f.getByRole('button', { name: 'OK' }).click();
        console.log("✅ Clicked OK popup");
      } catch {}
    }
  }

  await page.waitForTimeout(3000);

  // ======================================================
  // STEP 6: Find APP FRAME
  // ======================================================
  let appFrame = null;

  for (const f of page.frames()) {
    if (await f.locator('text=Policy Inquiry').count() > 0) {
      appFrame = f;
      break;
    }
  }

  if (!appFrame) throw new Error("❌ App frame not found");

  console.log("✅ App frame ready");

  // ======================================================
  // STEP 7: Navigate Menu
  // ======================================================
  await appFrame.locator('text=Policy Inquiry').click();
  await appFrame.locator('text=Policy Inquiry - All Details').click();

  console.log("✅ Navigation successful");

  await page.waitForTimeout(5000);

  // ======================================================
  // STEP 8: Enter Policy ID
  // ======================================================
  let formFrame = null;

  for (const f of page.frames()) {
    if (await f.locator('input').count() > 0) {
      formFrame = f;
      break;
    }
  }

  if (!formFrame) throw new Error("❌ Form frame not found");

  await formFrame.locator('input').first().fill(POLICY_ID);
  await formFrame.getByRole('button', { name: 'OK' }).click();

  console.log("✅ Policy submitted:", POLICY_ID);

  await page.waitForLoadState('networkidle');

  // ======================================================
  // STEP 9: Screenshot
  // ======================================================
  const path = `screenshots/policy-${POLICY_ID}.png`;

  await page.screenshot({ path, fullPage: true });

  console.log(`✅ Screenshot saved: ${path}`);
});
