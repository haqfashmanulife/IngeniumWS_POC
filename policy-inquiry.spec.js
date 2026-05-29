import { test, expect } from '@playwright/test';

test('✅ BULLETPROOF Ingenium Policy Inquiry Flow', async ({ page }) => {

  const BASE_URL   = process.env.APP_URL;
  const USERNAME   = process.env.APP_USERNAME;
  const PASSWORD   = process.env.APP_PASSWORD;
  const COMPANY    = process.env.COMPANY || 'Manulife';
  const POLICY_ID  = process.env.POLICY_ID;

  expect(POLICY_ID).toBeTruthy();

  // ======================================================
  // UTIL: SAFE FRAME FINDER (auto-retry + detach safe)
  // ======================================================
  async function findFrame(page, predicate, retries = 8, delay = 3000) {
    for (let i = 0; i < retries; i++) {
      for (const f of page.frames()) {
        try {
          if (await predicate(f)) {
            return f;
          }
        } catch {
          // frame detached → ignore
        }
      }
      console.log(`⏳ Frame not ready (attempt ${i + 1}/${retries})`);
      await page.waitForTimeout(delay);
    }
    throw new Error('❌ Frame not found');
  }

  // ======================================================
  // STEP 1: Launch
  // ======================================================
  await page.goto(BASE_URL, { waitUntil: 'networkidle' });

  await page.screenshot({ path: 'screenshots/01-launch.png', fullPage: true });

  // ======================================================
  // STEP 2: English button
  // ======================================================
  const english = page.getByText('English Sign On');

  if (await english.isVisible().catch(() => false)) {
    await english.click();
    console.log("✅ Clicked English Sign On");
  }

  await page.waitForTimeout(5000);

  // ======================================================
  // STEP 3: LOGIN FRAME
  // ======================================================
  const loginFrame = await findFrame(page, async (f) => {
    return await f.locator('input[type="password"]').count() > 0;
  });

  // ======================================================
  // STEP 4: LOGIN
  // ======================================================
  if (loginFrame) {
    console.log("✅ Login frame found");

    await loginFrame.locator('input[type="text"]').first().fill(USERNAME);
    await loginFrame.locator('input[type="password"]').fill(PASSWORD);

    await loginFrame.locator('select').selectOption({ label: COMPANY });

    await loginFrame.getByRole('button', { name: /submit/i }).click();

    console.log("✅ Login submitted");

    await page.waitForLoadState('networkidle').catch(() => {});
    await page.waitForTimeout(5000);
  }

  await page.screenshot({ path: 'screenshots/02-after-login.png', fullPage: true });

  // ======================================================
  // STEP 5: HANDLE OK POPUP (VERY ROBUST)
  // ======================================================
  try {
    const popupFrame = await findFrame(page, async (f) => {
      return await f.getByRole('button', { name: 'OK' }).count() > 0;
    }, 3, 2000);

    await popupFrame.getByRole('button', { name: 'OK' }).first().click();

    console.log("✅ Clicked OK popup");

    await page.waitForLoadState('networkidle').catch(() => {});
    await page.waitForTimeout(10000);
  } catch {
    console.log("✅ No popup detected");
  }

  // ======================================================
  // STEP 6: APP FRAME (stable detection)
  // ======================================================
  const appFrame = await findFrame(page, async (f) => {
    return await f.locator('span[title="Policy Inquiry"]').count() > 0;
  });

  console.log("✅ App frame ready");

  // ======================================================
  // UTIL: SAFE CLICK (retry + strict-safe)
  // ======================================================
  async function safeClick(locator, retries = 5) {
    for (let i = 0; i < retries; i++) {
      try {
        await locator.first().waitFor({ state: 'visible', timeout: 5000 });
        await locator.first().click();
        return;
      } catch (err) {
        console.log(`⚠️ Click retry ${i + 1}`);
        await page.waitForTimeout(2000);
      }
    }
    throw new Error("❌ Failed to click element");
  }

  // ======================================================
  // STEP 7: LEFT MENU NAVIGATION (FIXED ✅)
  // ======================================================
  await safeClick(appFrame.locator('span[title="Policy Inquiry"]'));

  await safeClick(
    appFrame.locator('a').filter({ hasText: 'Policy Inquiry - All Details' })
  );

  console.log("✅ Navigation successful");

  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(5000);

  // ======================================================
  // STEP 8: FORM FRAME (robust detection)
  // ======================================================
  const formFrame = await findFrame(page, async (f) => {
    return await f.locator('input').count() > 0;
  });

  // Fill policy input
  await formFrame.locator('input').first().fill(POLICY_ID);

  await safeClick(formFrame.getByRole('button', { name: 'OK' }));

  console.log("✅ Policy submitted:", POLICY_ID);

  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(5000);

  // ======================================================
  // STEP 9: FINAL SCREENSHOT
  // ======================================================
  const path = `screenshots/policy-${POLICY_ID}.png`;

  await page.screenshot({ path, fullPage: true });

  console.log(`✅ Screenshot saved: ${path}`);
});
