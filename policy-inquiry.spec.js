import { test, expect } from '@playwright/test';

test('✅ FINAL Ingenium Policy Inquiry Flow - ZERO FLAKE', async ({ page }) => {

  const BASE_URL   = process.env.APP_URL;
  const USERNAME   = process.env.APP_USERNAME;
  const PASSWORD   = process.env.APP_PASSWORD;
  const COMPANY    = process.env.COMPANY || 'Manulife';
  const POLICY_ID  = process.env.POLICY_ID;

  expect(POLICY_ID).toBeTruthy();

  // ======================================================
  // UTIL: FRAME FINDER (robust)
  // ======================================================
  async function findFrame(page, predicate, retries = 10, delay = 2000) {
    for (let i = 0; i < retries; i++) {
      for (const f of page.frames()) {
        try {
          if (await predicate(f)) return f;
        } catch {}
      }
      console.log(`⏳ Frame not ready (${i + 1}/${retries})`);
      await page.waitForTimeout(delay);
    }
    throw new Error('❌ Frame not found');
  }

  // ======================================================
  // UTIL: SAFE CLICK (force + fallback)
  // ======================================================
  async function safeClick(locator, retries = 5) {
    for (let i = 0; i < retries; i++) {
      try {
        await locator.first().waitFor({ state: 'visible', timeout: 5000 });

        await locator.first().scrollIntoViewIfNeeded();

        // Force click (important for legacy UI)
        await locator.first().click({ timeout: 5000, force: true });

        return;
      } catch {
        console.log(`⚠️ Click retry ${i + 1}`);

        try {
          const box = await locator.first().boundingBox();
          if (box) {
            await locator.page().mouse.click(box.x + 5, box.y + 5);
            return;
          }
        } catch {}

        await locator.page().waitForTimeout(2000);
      }
    }
    throw new Error('❌ Failed to click element');
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

  console.log("✅ Login frame found");

  // ======================================================
  // STEP 4: LOGIN
  // ======================================================
  await loginFrame.locator('input[type="text"]').first().fill(USERNAME);
  await loginFrame.locator('input[type="password"]').fill(PASSWORD);

  await loginFrame.locator('select').selectOption({ label: COMPANY });

  await safeClick(loginFrame.getByRole('button', { name: /submit/i }));

  console.log("✅ Login submitted");

  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(5000);

  await page.screenshot({ path: 'screenshots/02-after-login.png', fullPage: true });

  // ======================================================
  // STEP 5: OK POPUP
  // ======================================================
  try {
    const popupFrame = await findFrame(page, async (f) => {
      return await f.getByRole('button', { name: 'OK' }).count() > 0;
    }, 3, 2000);

    await safeClick(popupFrame.getByRole('button', { name: 'OK' }));

    console.log("✅ Clicked OK popup");

    await page.waitForLoadState('networkidle').catch(() => {});
    await page.waitForTimeout(8000);
  } catch {
    console.log("✅ No popup");
  }

  // ======================================================
  // STEP 6: APP FRAME (LEFT MENU)
  // ======================================================
  const appFrame = await findFrame(page, async (f) => {
    return await f.locator('span[title="Policy Inquiry"]').count() > 0;
  });

  console.log("✅ App frame ready");

  // ======================================================
  // STEP 7: NAVIGATION
  // ======================================================
  await safeClick(appFrame.locator('span[title="Policy Inquiry"]'));

  await safeClick(
    appFrame.locator('a').filter({ hasText: 'Policy Inquiry - All Details' })
  );

  console.log("✅ Navigation successful");

  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(5000);

  // ======================================================
  // STEP 8: FORM FRAME (Policy ID)
  // ======================================================
  const formFrame = await findFrame(page, async (f) => {
    return await f.locator('input').count() > 0;
  });

  await formFrame.locator('input').first().fill(POLICY_ID);

  console.log("✅ Policy ID entered:", POLICY_ID);

  // ======================================================
  // ✅ STEP 9: FINAL FIX - OK BUTTON (NO FRAME)
  // ======================================================
  const okButton = page.locator(
    'button:has-text("OK"), input[type="button"][value="OK"]'
  );

  await safeClick(okButton);

  console.log("✅ OK button clicked");

  // ======================================================
  // STEP 10: RESULT
  // ======================================================
  await page.waitForLoadState('networkidle').catch(() => {});
  await page.waitForTimeout(6000);

  // ======================================================
  // STEP 11: SCREENSHOT
  // ======================================================
  const path = `screenshots/policy-${POLICY_ID}.png`;

  await page.screenshot({ path, fullPage: true });

  console.log(`✅ Screenshot saved: ${path}`);
});
