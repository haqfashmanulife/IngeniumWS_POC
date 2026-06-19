import { test, expect } from '@playwright/test';

test('✅ FINAL Ingenium Policy Inquiry Flow (Stable)', async ({ page }) => {

  try {
    const BASE_URL   = process.env.APP_URL;
    const USERNAME   = process.env.APP_USERNAME;
    const PASSWORD   = process.env.APP_PASSWORD;
    const COMPANY    = process.env.COMPANY || 'Manulife';
    const POLICY_ID  = process.env.POLICY_ID;

    console.log("🚀 START TEST");
    expect(POLICY_ID).toBeTruthy();

    // ======================================================
    // FRAME FINDER (IMPROVED)
    // ======================================================
    async function findFrame(page, predicate, retries = 10) {
      for (let i = 0; i < retries; i++) {
        for (const f of page.frames()) {
          try {
            if (await predicate(f)) return f;
          } catch {}
        }
        await page.waitForTimeout(2000);
      }
      throw new Error('❌ Frame not found');
    }

    // ======================================================
    // SAFE CLICK
    // ======================================================
    async function safeClick(locator) {
      await locator.first().waitFor({ state: 'visible', timeout: 5000 });
      await locator.first().click();
    }

    // ======================================================
    // STEP 1: Launch
    // ======================================================
    await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(5000);

    // ======================================================
    // STEP 2: English
    // ======================================================
    const english = page.getByText('English Sign On');
    if (await english.isVisible().catch(() => false)) {
      await english.click();
    }

    await page.waitForTimeout(5000);

    // ======================================================
    // STEP 3: LOGIN FRAME
    // ======================================================
    const loginFrame = await findFrame(page, async (f) =>
      await f.locator('input[type="password"]').count() > 0
    );

    await loginFrame.locator('input[type="text"]').first().fill(USERNAME);
    await loginFrame.locator('input[type="password"]').fill(PASSWORD);
    await loginFrame.locator('select').selectOption({ label: COMPANY });

    await safeClick(loginFrame.getByRole('button', { name: /submit/i }));

    await page.waitForTimeout(5000);

    // ======================================================
    // STEP 5: POPUP OK
    // ======================================================
    try {
      const popup = await findFrame(page, async (f) =>
        await f.getByRole('button', { name: 'OK' }).count() > 0
      , 3);

      await safeClick(popup.getByRole('button', { name: 'OK' }));
      await page.waitForTimeout(8000);
    } catch {}

    // ======================================================
    // STEP 6: APP FRAME
    // ======================================================
    const appFrame = await findFrame(page, async (f) =>
      await f.locator('span[title="Policy Inquiry"]').count() > 0
    );

    // ======================================================
    // STEP 7: NAVIGATION
    // ======================================================
    await safeClick(appFrame.locator('span[title="Policy Inquiry"]'));
    await safeClick(appFrame.locator('a').filter({ hasText: 'Policy Inquiry - All Details' }));

    await page.waitForTimeout(5000);

    // ======================================================
    // STEP 8: FORM FRAME
    // ======================================================
    const formFrame = await findFrame(page, async (f) =>
      await f.locator('input').count() > 0
    );

    await formFrame.locator('input').first().fill(POLICY_ID);

    // ✅ SAME LOGIC — FIND OK FRAME
    const okFrame = await findFrame(page, async (f) =>
      await f.getByRole('button', { name: 'OK' }).count() > 0
    );

    await safeClick(okFrame.getByRole('button', { name: 'OK' }));

    await page.waitForTimeout(6000);

    await page.screenshot({
      path: `screenshots/policy-${POLICY_ID}.png`,
      fullPage: true
    });

    // ======================================================
    // ✅ STEP 11: Coverage Values (FIXED)
    // ======================================================
    await safeClick(appFrame.locator('a').filter({ hasText: 'Inquiry - Coverage Values' }));

    const cvFrame = await findFrame(page, async (f) =>
      await f.locator('text=Inquiry - Coverage Values').count() > 0
    );

    await cvFrame.locator('input').first().fill(POLICY_ID);

    // ✅ KEY FIX: CLICK INSIDE SAME FRAME
    await safeClick(cvFrame.getByRole('button', { name: 'OK' }));

    await page.waitForTimeout(5000);

    await page.screenshot({
      path: `screenshots/coverage-values-${POLICY_ID}.png`,
      fullPage: true
    });

    // ======================================================
    // ✅ STEP 12: Coverage Details (FIXED)
    // ======================================================
    await safeClick(appFrame.locator('a').filter({ hasText: 'Inquiry - Coverage Details' }));

    const cdFrame = await findFrame(page, async (f) =>
      await f.locator('text=Inquiry - Coverage Details').count() > 0
    );

    await cdFrame.locator('input').first().fill(POLICY_ID);

    await safeClick(cdFrame.getByRole('button', { name: 'OK' }));

    await page.waitForTimeout(6000);

    for (let i = 0; i < 5; i++) {
      await page.mouse.wheel(0, 2000);
      await page.screenshot({
        path: `screenshots/coverage-details-${POLICY_ID}-${i + 1}.png`
      });
      await page.waitForTimeout(1500);
    }

    // ======================================================
    // ✅ STEP 13: Call Centre Info (FIXED)
    // ======================================================
    await safeClick(appFrame.locator('a').filter({ hasText: 'Inquiry - Call Centre Information' }));

    const ccFrame = await findFrame(page, async (f) =>
      await f.locator('text=Inquiry - Call Centre Information').count() > 0
    );

    await ccFrame.locator('input').first().fill(POLICY_ID);

    await safeClick(ccFrame.getByRole('button', { name: 'OK' }));

    await page.waitForTimeout(6000);

    for (let i = 0; i < 5; i++) {
      await page.mouse.wheel(0, 2000);
      await page.screenshot({
        path: `screenshots/call-centre-${POLICY_ID}-${i + 1}.png`
      });
      await page.waitForTimeout(1500);
    }

    console.log("✅ ALL FLOWS SUCCESS");

  } catch (e) {
    console.error("❌ TEST FAILURE:", e);

    await page.screenshot({
      path: 'screenshots/failure-final.png',
      fullPage: true
    });

    throw e;
  }
});
