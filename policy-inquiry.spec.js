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
    try {
      if (await f.locator('input[type="password"]').count() > 0) {
        loginFrame = f;
        break;
      }
    } catch {
      // Frame detached mid-scan — skip it.
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
  // Clicking OK triggers a navigation/reload that DETACHES the other
  // frames. So: stop scanning immediately after the click (break), guard
  // each frame access against detachment, then wait for the reload to
  // settle (~10s) before touching frames again.
  for (const f of page.frames()) {
    try {
      const okBtn = f.getByRole('button', { name: 'OK' });
      if (await okBtn.count() > 0) {
        await okBtn.first().click();
        console.log("✅ Clicked OK popup");
        break;
      }
    } catch {
      console.log("⚠️ Skipped detached frame during OK scan");
    }
  }

  // Page reloads after OK and takes ~10s to render the next view.
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(10000);

  // ======================================================
  // STEP 6: Find APP FRAME (retry until frames settle)
  // ======================================================
  let appFrame = null;

  for (let attempt = 0; attempt < 5 && !appFrame; attempt++) {
    for (const f of page.frames()) {
      try {
        if (await f.locator('text=Policy Inquiry').count() > 0) {
          appFrame = f;
          break;
        }
      } catch {
        // Detached frame — skip.
      }
    }
    if (!appFrame) {
      console.log(`⏳ App frame not ready, retry ${attempt + 1}/5`);
      await page.waitForTimeout(3000);
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

  // Menu navigation triggers another load; let it settle.
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(5000);

  // ======================================================
  // STEP 8: Enter Policy ID (retry until form frame settles)
  // ======================================================
  let formFrame = null;

  for (let attempt = 0; attempt < 5 && !formFrame; attempt++) {
    for (const f of page.frames()) {
      try {
        if (await f.locator('input').count() > 0) {
          formFrame = f;
          break;
        }
      } catch {
        // Detached frame — skip.
      }
    }
    if (!formFrame) {
      console.log(`⏳ Form frame not ready, retry ${attempt + 1}/5`);
      await page.waitForTimeout(3000);
    }
  }

  if (!formFrame) throw new Error("❌ Form frame not found");

  await formFrame.locator('input').first().fill(POLICY_ID);
  await formFrame.getByRole('button', { name: 'OK' }).click();

  console.log("✅ Policy submitted:", POLICY_ID);

  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(5000);

  // ======================================================
  // STEP 9: Screenshot
  // ======================================================
  const path = `screenshots/policy-${POLICY_ID}.png`;

  await page.screenshot({ path, fullPage: true });

  console.log(`✅ Screenshot saved: ${path}`);
});
