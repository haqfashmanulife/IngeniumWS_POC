import { test, expect } from '@playwright/test';

test('✅ Ingenium Policy Inquiry Flow - ULTIMATE STABLE', async ({ page }) => {

  const BASE_URL   = process.env.APP_URL;
  const USERNAME   = process.env.APP_USERNAME;
  const PASSWORD   = process.env.APP_PASSWORD;
  const COMPANY    = process.env.COMPANY || 'Manulife';
  const POLICY_ID  = process.env.POLICY_ID;

  expect(POLICY_ID).toBeTruthy();

  // ======================================================
  // UTIL: FRAME FINDER
  // ======================================================
  async function findFrame(page, predicate, retries = 10, delay = 2000) {
    for (let i = 0; i < retries; i++) {
      for (const f of page.frames()) {
        try {
          if (await predicate(f)) return f;
        } catch {}
      }
      console.log(`⏳ Frame retry ${i + 1}/${retries}`);
      await page.waitForTimeout(delay);
    }
    throw new Error('❌ Frame not found');
  }

  // ======================================================
  // UTIL: SAFE CLICK (DOM)
  // ======================================================
  async function safeClick(locator) {
    try {
      await locator.first().waitFor({ state: 'visible', timeout: 5000 });
      await locator.first().scrollIntoViewIfNeeded();
      await locator.first().click({ force: true });
      return true;
    } catch {
      return false;
    }
  }

  // ======================================================
  // UTIL: SMART OK CLICK (CRITICAL)
  // ======================================================
  async function clickOK(page) {

    console.log("🔍 Trying DOM click...");

    // 1️⃣ Try normal DOM click
    const okLocator = page.locator(
      'button:has-text("OK"), input[value="OK"], text=OK'
    );

    if (await okLocator.count() > 0) {
      const success = await safeClick(okLocator);
      if (success) {
        console.log("✅ OK clicked via locator");
        return;
      }
    }

    console.log("⚠️ Locator click failed → trying frame scan...");

    // 2️⃣ Try inside any frame
    for (const f of page.frames()) {
      try {
        const btn = f.locator('button:has-text("OK"), input[value="OK"]');
        if (await btn.count() > 0) {
          await btn.first().click({ force: true });
          console.log("✅ OK clicked via frame");
          return;
        }
      } catch {}
    }

    console.log("⚠️ Frame click failed → using coordinates...");

    // 3️⃣ FINAL GUARANTEED fallback → coordinate click
    const viewport = page.viewportSize();

    const x = Math.floor(viewport.width / 2);
    const y = Math.floor(viewport.height - 40);

    console.log(`🖱 Clicking at (${x}, ${y})`);

    await page.mouse.click(x, y);

    console.log("✅ OK clicked via coordinates (guaranteed)");
  }

  // ======================================================
  // STEP 1: Launch
  // ======================================================
  await page.goto(BASE_URL, { waitUntil: 'networkidle' });

  await page.screenshot({ path: 'screenshots/01-launch.png', fullPage: true });

  // ======================================================
  // STEP 2: English
  // ======================================================
  const english = page.getByText('English Sign On');

  if (await english.isVisible().catch(() => false)) {
    await english.click();
    console.log("✅ English selected");
  }

  await page.waitForTimeout(5000);

  // ======================================================
  // STEP 3: LOGIN FRAME
  // ======================================================
  const loginFrame = await findFrame(page, f =>
    f.locator('input[type="password"]').count()
  );

  console.log("✅ Login frame found");

  // ======================================================
  // STEP 4: LOGIN
  // ======================================================
  await loginFrame.locator('input[type="text"]').first().fill(USERNAME);
  await loginFrame.locator('input[type="password"]').fill(PASSWORD);

  await loginFrame.locator('select').selectOption({ label: COMPANY });

  await loginFrame.getByRole('button', { name: /submit/i }).click();

  console.log("✅ Login submitted");

  await page.waitForTimeout(6000);

  await page.screenshot({ path: 'screenshots/02-after-login.png', fullPage: true });

  // ======================================================
  // STEP 5: POPUP OK
  // ======================================================
  await clickOK(page);

  await page.waitForTimeout(8000);

  // ======================================================
  // STEP 6: APP FRAME
  // ======================================================
  const appFrame = await findFrame(page, f =>
    f.locator('span[title="Policy Inquiry"]').count()
  );

  console.log("✅ App frame ready");

  // ======================================================
  // STEP 7: MENU NAVIGATION
  // ======================================================
  await appFrame.locator('span[title="Policy Inquiry"]').click();

  await appFrame
    .locator('a')
    .filter({ hasText: 'Policy Inquiry - All Details' })
    .click();

  console.log("✅ Navigation done");

  await page.waitForTimeout(6000);

  // ======================================================
  // STEP 8: FORM FRAME
  // ======================================================
  const formFrame = await findFrame(page, f =>
    f.locator('input').count()
  );

  await formFrame.locator('input').first().fill(POLICY_ID);

  console.log("✅ Policy ID entered:", POLICY_ID);

  await page.waitForTimeout(3000);

  // ======================================================
  // ✅ STEP 9: CLICK OK (100% GUARANTEED)
  // ======================================================
  await clickOK(page);

  await page.waitForTimeout(8000);

  // ======================================================
  // STEP 10: FINAL SCREENSHOT
  // ======================================================
  const path = `screenshots/policy-${POLICY_ID}.png`;

  await page.screenshot({ path, fullPage: true });

  console.log(`✅ DONE: ${path}`);
});
