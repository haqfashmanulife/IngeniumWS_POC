import { test, expect } from '@playwright/test';

test('✅ FINAL Ingenium Policy.log("POLICY_ID:", POLICY_ID);test('✅ FINAL Ingenium Policy Inquiry Flow (Stable)', async ({ page }) => {

    expect(BASE_URL).toBeTruthy();
    expect(USERNAME).toBeTruthy();
    expect(PASSWORD).toBeTruthy();
    expect(POLICY_ID).toBeTruthy();

    // ======================================================
    // FRAME FINDER
    // ======================================================
    async function findFrame(page, predicate, retries = 15, delay = 2000) {
      for (let i = 0; i < retries; i++) {
        for (const f of page.frames()) {
          try {
            if (await predicate(f)) {
              return f;
            }
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
    // SAFE CLICK
    // ======================================================
    async function safeClick(locator, retries = 5) {
      for (let i = 0; i < retries; i++) {
        try {
          await locator.first().waitFor({
            state: 'visible',
            timeout: 5000
          });

          await locator.first().click({
            timeout: 5000
          });

          return;
        } catch (e) {
          console.log(`⚠️ Click retry ${i + 1}: ${e.message}`);

          try {
            const box = await locator.first().boundingBox();
            if (box) {
              await page.mouse.click(box.x + 5, box.y + 5);
              return;
            }
          } catch {}

          await page.waitForTimeout(2000);
        }
      }

      await page.screenshot({
        path: 'screenshots/click-error.png',
        fullPage: true
      });

      throw new Error('❌ Failed to click element');
    }

    // ======================================================
    // CLICK OK FROM ANY FRAME / FOOTER
    // Ingenium footer OK may be in a separate frame.
    // ======================================================
    async function clickOkFromAnyFrame(screenName = '') {
      console.log(`🔘 Trying to click OK ${screenName ? 'for ' + screenName : ''}`);

      const selectorFactories = [
        (f) => f.getByRole('button', { name: /^OK$/i }),
        (f) => f.locator('input[value="OK"]'),
        (f) => f.locator('input[type="submit"][value="OK"]'),
        (f) => f.locator('input[type="button"][value="OK"]'),
        (f) => f.locator('input[type="image"][alt="OK"]'),
        (f) => f.locator('img[alt="OK"]'),
        (f) => f.locator('[title="OK"]'),
        (f) => f.locator('a').filter({ hasText: /^OK$/i }),
        (f) => f.locator('text=/^OK$/')
      ];

      for (let attempt = 0; attempt < 8; attempt++) {
        for (const f of page.frames()) {
          for (const makeLocator of selectorFactories) {
            try {
              const loc = makeLocator(f);
              const count = await loc.count();

              if (count > 0) {
                const first = loc.first();

                if (await first.isVisible().catch(() => false)) {
                  await first.click({ timeout: 5000 });
                  console.log(`✅ Clicked OK using locator in frame: ${f.url()}`);
                  await page.waitForTimeout(4000);
                  return;
                }
              }
            } catch {}
          }
        }

        console.log(`⏳ OK not ready yet (${attempt + 1}/8)`);
        await page.waitForTimeout(1500);
      }

      // Final fallback: OK button is visually in bottom footer.
      console.log("⚠️ OK selector not found, using footer coordinate fallback");

      const vp = page.viewportSize();
      if (vp) {
        await page.mouse.click(
          Math.floor(vp.width / 2) - 25,
          Math.floor(vp.height) - 35
        );

        await page.waitForTimeout(5000);
        console.log("✅ Clicked OK using footer coordinate fallback");
        return;
      }

      throw new Error('❌ Could not click OK');
    }

    // ======================================================
    // CLICK LEFT MENU ITEM
    // ======================================================
    async function clickLeftMenu(menuText) {
      console.log(`📌 Opening menu: ${menuText}`);

      const menuFrame = await findFrame(page, async (f) => {
        return await f.locator('a').filter({ hasText: menuText }).count() > 0;
      }, 15, 2000);

      await safeClick(
        menuFrame.locator('a').filter({ hasText: menuText })
      );

      await page.waitForTimeout(5000);
    }

    // ======================================================
    // WAIT FOR SCREEN TITLE
    // ======================================================
    async function waitForScreenTitle(titleText) {
      console.log(`🔎 Waiting for screen title: ${titleText}`);

      await findFrame(page, async (f) => {
        return await f.locator(`text=${titleText}`).count() > 0;
      }, 15, 2000);

      console.log(`✅ Screen title found: ${titleText}`);
    }

    // ======================================================
    // FIND POLICY FORM FRAME
    // Important:
    // Screen title and input are often in different frames.
    // So we find the frame having visible Policy Id label + visible input.
    // ======================================================
    async function findPolicyFormFrame(screenName) {
      console.log(`🔎 Finding policy form frame for: ${screenName}`);

      const formFrame = await findFrame(page, async (f) => {
        const hasPolicyLabel =
          await f.locator('text=/Policy\\s*Id/i').count() > 0;

        const visibleInputs =
          await f.locator('input:visible').count() > 0;

        return hasPolicyLabel && visibleInputs;
      }, 15, 2000);

      console.log(`✅ Policy form frame found for: ${screenName}`);
      return formFrame;
    }

    // ======================================================
    // FILL POLICY ID ON CURRENT SCREEN
    // ======================================================
    async function fillPolicyIdOnScreen(screenName) {
      const formFrame = await findPolicyFormFrame(screenName);

      const policyInput = formFrame.locator('input:visible').first();

      await policyInput.waitFor({
        state: 'visible',
        timeout: 10000
      });

      await policyInput.click();
      await policyInput.fill(POLICY_ID, {
        timeout: 10000
      });

      console.log(`✅ Policy ID entered for ${screenName}: ${POLICY_ID}`);

      return formFrame;
    }

    // ======================================================
    // SCROLL ALL FRAMES AND CAPTURE SCREENSHOTS
    // ======================================================
    async function scrollAndCapture(prefix, count = 6) {
      console.log(`📸 Capturing scroll screenshots: ${prefix}`);

      for (const f of page.frames()) {
        try {
          await f.evaluate(() => window.scrollTo(0, 0));
        } catch {}
      }

      await page.waitForTimeout(1000);

      for (let i = 0; i < count; i++) {
        await page.screenshot({
          path: `screenshots/${prefix}-${POLICY_ID}-${i + 1}.png`,
          fullPage: true
        });

        for (const f of page.frames()) {
          try {
            await f.evaluate(() => {
              window.scrollBy(0, window.innerHeight * 0.85);
            });
          } catch {}
        }

        await page.waitForTimeout(1500);
      }

      console.log(`✅ Completed screenshots for: ${prefix}`);
    }

    // ======================================================
    // STEP 1: Launch
    // ======================================================
    await page.goto(BASE_URL, {
      waitUntil: 'domcontentloaded'
    });

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
    // STEP 7: EXISTING FLOW - POLICY INQUIRY ALL DETAILS
    // ======================================================
    await safeClick(appFrame.locator('span[title="Policy Inquiry"]'));

    await safeClick(
      appFrame.locator('a').filter({ hasText: 'Policy Inquiry - All Details' })
    );

    console.log("✅ Navigation successful");

    await page.waitForTimeout(5000);

    // ======================================================
    // STEP 8: FORM FRAME
    // Existing logic preserved.
    // ======================================================
    const formFrame = await findFrame(page, async (f) => {
      return await f.locator('input').count() > 0;
    });

    await formFrame.locator('input').first().fill(POLICY_ID);

    console.log("✅ Policy ID entered:", POLICY_ID);

    await page.waitForTimeout(3000);

    // ======================================================
    // STEP 9: EXISTING OK LOGIC
    // ======================================================
    try {
      const okFrame = await findFrame(page, async (f) => {
        return await f.getByRole('button', { name: 'OK' }).count() > 0;
      }, 5, 2000);

      await safeClick(okFrame.getByRole('button', { name: 'OK' }));

      console.log("✅ Clicked OK after policy");

      await page.waitForTimeout(6000);

    } catch {
      console.log("⚠️ OK not found in frame → fallback click");

      const vp = page.viewportSize();
      if (vp) {
        const x = Math.floor(vp.width / 2);
        const y = Math.floor(vp.height - 40);

        await page.mouse.click(x, y);

        console.log("✅ OK clicked via fallback");
      }
    }

    // ======================================================
    // STEP 10: FINAL SCREENSHOT - EXISTING FLOW
    // ======================================================
    await page.screenshot({
      path: `screenshots/policy-${POLICY_ID}.png`,
      fullPage: true
    });

    console.log(`✅ Screenshot saved: screenshots/policy-${POLICY_ID}.png`);

    // ======================================================
    // STEP 11: NEW FLOW - INQUIRY COVERAGE VALUES
    // ======================================================
    await clickLeftMenu('Inquiry - Coverage Values');

    await waitForScreenTitle('Inquiry - Coverage Values');

    await fillPolicyIdOnScreen('Inquiry - Coverage Values');

    await page.screenshot({
      path: `screenshots/coverage-values-before-ok-${POLICY_ID}.png`,
      fullPage: true
    });

    await clickOkFromAnyFrame('Inquiry - Coverage Values');

    await page.waitForTimeout(6000);

    await page.screenshot({
      path: `screenshots/coverage-values-${POLICY_ID}.png`,
      fullPage: true
    });

    console.log("✅ Coverage Values completed");

    // ======================================================
    // STEP 12: NEW FLOW - INQUIRY COVERAGE DETAILS
    // ======================================================
    await clickLeftMenu('Inquiry - Coverage Details');

    await waitForScreenTitle('Inquiry - Coverage Details');

    await fillPolicyIdOnScreen('Inquiry - Coverage Details');

    await page.screenshot({
      path: `screenshots/coverage-details-before-ok-${POLICY_ID}.png`,
      fullPage: true
    });

    await clickOkFromAnyFrame('Inquiry - Coverage Details');

    await page.waitForTimeout(7000);

    await scrollAndCapture('coverage-details', 6);

    console.log("✅ Coverage Details completed");

    // ======================================================
    // STEP 13: NEW FLOW - CALL CENTRE INFORMATION
    // ======================================================
    await clickLeftMenu('Inquiry - Call Centre Information');

    await waitForScreenTitle('Inquiry - Call Centre Information');

    await fillPolicyIdOnScreen('Inquiry - Call Centre Information');

    await page.screenshot({
      path: `screenshots/call-centre-before-ok-${POLICY_ID}.png`,
      fullPage: true
    });

    await clickOkFromAnyFrame('Inquiry - Call Centre Information');

    await page.waitForTimeout(7000);

    await scrollAndCapture('call-centre', 6);

    console.log("✅ Call Centre Information completed");

    console.log("✅ ALL FLOWS COMPLETED SUCCESSFULLY");

  } catch (e) {
    console.error("❌ TEST FAILURE:", e);

    try {
      await page.screenshot({
        path: 'screenshots/failure-final.png',
        fullPage: true
      });
    } catch (screenshotError) {
      console.error("❌ Could not capture failure screenshot:", screenshotError);
    }

    throw e;
  }
});


  try {
    const BASE_URL   = process.env.APP_URL;
    const USERNAME   = process.env.APP_USERNAME;
    const PASSWORD   = process.env.APP_PASSWORD;
    const COMPANY    = process.env.COMPANY || 'Manulife';
    const POLICY_ID  = process.env.POLICY_ID;

    console.log("🚀 START TEST");
    console.log("BASE_URL:", BASE_URL);
