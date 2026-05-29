import { test, expect } from '@playwright/test';

test('✅ Ingenium Policy Inquiry Flow - FINAL FIXED', async ({ page }) => {

  const BASE_URL   = process.env.APP_URL;
  const USERNAME   = process.env.APP_USERNAME;
  const PASSWORD   = process.env.APP_PASSWORD;
  const COMPANY    = process.env.COMPANY || 'Manulife';
  const POLICY_ID  = process.env.POLICY_ID;

  expect(POLICY_ID).toBeTruthy();

  // ======================================================
  // FRAME FINDER
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
  async function safeClick(locator, retries = 5) {
    for (let i = 0; i < retries; i++) {
      try {
        await locator.first().waitFor({ state: 'visible', timeout: 5000 });
        await locator.first().scrollIntoViewIfNeeded();
        await locator.first().click({ force: true });
        return;
      } catch {
        console.log(`⚠️ Retry click ${i + 1}`);
        await page.waitForTimeout(2000);
      }
    }
    return false;
  }

  // ======================================================
  // SMART OK CLICK (FIXED)
  // ======================================================
  async function clickOK(page) {

    console.log("🔍 Trying OK click...");

    const okLocator =
      page.locator('button:has-text("OK"), input[value="OK"]')
          .or(page.locator('text=OK'));

    try {
      const success = await safeClick(okLocator);
      if (success) {
        console.log("✅ OK clicked via locator");
        return;
      }
    } catch {}

    console.log("⚠️ Locator failed → fallback to coordinates");

    const vp = page.viewportSize();

    const x = Math.floor(vp.width / 2);
    const y = Math.floor(vp.height - 40);

    await page.mouse.click(x, y);

    console.log("✅ OK clicked via coordinates");
  }

  // ======================================================
  // STEP 1
  // ======================================================
  await page.goto(BASE_URL, { waitUntil: 'networkidle' });

  await page.screenshot({ path: 'screenshots/01-launch.png', fullPage: true });

  // ======================================================
  // STEP 2
  // ======================================================
  const english = page.getByText('English Sign On');

  if (await english.isVisible().catch(() => false)) {
    await english.click();
    console.log("✅ English selected");
  }

  await page.waitForTimeout(5000);

  // ======================================================
  // STEP 3
  // ======================================================
  const loginFrame = await findFrame(page, f =>
    f.locator('input[type="password"]').count()
  );

  console.log("✅ Login frame found");

  // ======================================================
  // STEP 4
  // ======================================================
  await loginFrame.locator('input[type="text"]').first().fill(USERNAME);
  await loginFrame.locator('input[type="password"]').fill(PASSWORD);
  await loginFrame.locator('select').selectOption({ label: COMPANY });

  await loginFrame.getByRole('button', { name: /submit/i }).click();

  await page.waitForTimeout(6000);

  // ======================================================
  // STEP 5
  // ======================================================
  await clickOK(page);

  await page.waitForTimeout(8000);

  // ======================================================
  // STEP 6
  // ======================================================
  const appFrame = await findFrame(page, f =>
    f.locator('span[title="Policy Inquiry"]').count()
  );

  await appFrame.locator('span[title="Policy Inquiry"]').click();

  await appFrame.locator('a')
    .filter({ hasText: 'Policy Inquiry - All Details' })
    .click();

  console.log("✅ Navigation successful");

  await page.waitForTimeout(6000);

  // ======================================================
  // STEP 7
  // ======================================================
  const formFrame = await findFrame(page, f =>
    f.locator('input').count()
  );

  await formFrame.locator('input').first().fill(POLICY_ID);

  console.log("✅ Policy entered:", POLICY_ID);

  await page.waitForTimeout(3000);

  // ======================================================
  // STEP 8 (FINAL OK)
  // ======================================================
  await clickOK(page);

  await page.waitForTimeout(8000);

  // ======================================================
  // STEP 9
  // ======================================================
  await page.screenshot({
    path: `screenshots/policy-${POLICY_ID}.png`,
    fullPage: true
  });

  console.log("✅ SUCCESS");
});
