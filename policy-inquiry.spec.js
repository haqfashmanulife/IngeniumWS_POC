import { test, expect } from '@playwright/test';

test('✅ FINAL Ingenium Policy Inquiry Flow (Stable)', async ({ page }) => {

  try {

    const BASE_URL   = process.env.APP_URL;
    const USERNAME   = process.env.APP_USERNAME;
    const PASSWORD   = process.env.APP_PASSWORD;
    const COMPANY    = process.env.COMPANY || 'Manulife';
    const POLICY_ID  = process.env.POLICY_ID;

    console.log("🚀 START TEST");
    console.log("BASE_URL:", BASE_URL);
    console.log("POLICY_ID:", POLICY_ID);

    expect(POLICY_ID).toBeTruthy();

    // ======================================================
    // FRAME FINDER (ROBUST + DEBUG)
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

      await page.screenshot({
        path: 'screenshots/frame-error.png',
        fullPage: true
      });

      throw new Error('❌ Frame not found');
    }

    // ======================================================
    // SAFE CLICK (STRONGER)
    // ======================================================
    async function safeClick(locator, retries = 5) {
      for (let i = 0; i < retries; i++) {
        try {
          await locator.first().waitFor({ state: 'visible', timeout: 5000 });
          await locator.first().click({ timeout: 5000 });
          return;
        } catch (e) {
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

      await locator.page().screenshot({
        path: 'screenshots/click-error.png',
        fullPage: true
      });

      throw new Error('❌ Failed to click element');
    }

    // ======================================================
    // STEP 1: Launch (FIXED)
    // ======================================================
    await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(5000);

    await page.screenshot({
      path: 'screenshots/01-launch.png',
      fullPage: true
    });

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

    await page.waitForTimeout(5000);

    await page.screenshot({
      path: 'screenshots/02-after-login.png',
      fullPage: true
    });

    // ======================================================
    // STEP 5: OK POPUP AFTER LOGIN
    // ======================================================
    try {
      const popupFrame = await findFrame(page, async (f) => {
        return await f.getByRole('button', { name: 'OK' }).count() > 0;
      }, 3, 2000);

      await safeClick(popupFrame.getByRole('button', { name: 'OK' }));

      console.log("✅ Clicked OK popup");

      await page.waitForTimeout(8000);
    } catch {
      console.log("✅ No popup");
    }

    // ======================================================
    // STEP 6: APP FRAME
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

    await page.waitForTimeout(5000);

    // ======================================================
    // STEP 8: FORM FRAME
    // ======================================================
    const formFrame = await findFrame(page, async (f) => {
      return await f.locator('input').count() > 0;
    });

    await formFrame.locator('input').first().fill(POLICY_ID);

    console.log("✅ Policy ID entered:", POLICY_ID);

    await page.waitForTimeout(3000);

    // ======================================================
    // STEP 9: OK CLICK
    // ======================================================
    try {
      const okFrame = await findFrame(page, async (f) => {
        return await f.getByRole('button', { name: 'OK' }).count() > 0;
      }, 5, 2000);

      await safeClick(okFrame.getByRole('button', { name: 'OK' }));

      console.log("✅ Clicked OK after policy");

      await page.waitForTimeout(6000);

    } catch {
      console.log("⚠️ OK not found → fallback click");

      const vp = page.viewportSize();
      if (vp) {
        await page.mouse.click(Math.floor(vp.width / 2), Math.floor(vp.height - 40));
        console.log("✅ Fallback OK click");
      }
    }

    // ======================================================
    // STEP 10: FINAL SCREENSHOT
    // ======================================================
    const path = `screenshots/policy-${POLICY_ID}.png`;

    await page.screenshot({
      path,
      fullPage: true
    });

    console.log(`✅ Screenshot saved: ${path}`);

  } catch (e) {

    console.error("❌ TEST FAILURE:", e);

    await page.screenshot({
      path: 'screenshots/failure-final.png',
      fullPage: true
    });

    throw e;
  }
});
