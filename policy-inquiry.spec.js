import { test, expect } from '@playwright/test';
import fs from 'fs';

test('FINAL Ingenium Policy Inquiry Flow Stable', async ({ page }) => {
  try {
    test.setTimeout(900000);

    const BASE_URL = process.env.APP_URL;
    const POLICY_ID = process.env.POLICY_ID;
    const MAJOR_POLICY_ID = process.env.MAJOR_POLICY_ID || POLICY_ID;
    const previousDateParts = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'Asia/Tokyo',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    }).formatToParts(new Date(Date.now() - 24 * 60 * 60 * 1000));
    const EFFECTIVE_DATE = `${previousDateParts.find(p => p.type === 'year').value}-${previousDateParts.find(p => p.type === 'month').value}-${previousDateParts.find(p => p.type === 'day').value}`;

    console.log('START TEST');
    console.log('BASE_URL:', BASE_URL);
    console.log('POLICY_ID:', POLICY_ID);
    console.log('MAJOR_POLICY_ID:', MAJOR_POLICY_ID);
    console.log('EFFECTIVE_DATE_PREVIOUS_DAY:', EFFECTIVE_DATE);

    expect(BASE_URL).toBeTruthy();
    expect(POLICY_ID).toBeTruthy();
    expect(MAJOR_POLICY_ID).toBeTruthy();

    async function findFrame(page, predicate, retries = 15, delay = 2000) {
      for (let i = 0; i < retries; i++) {
        for (const f of page.frames()) {
          try {
            if (await predicate(f)) return f;
          } catch {}
        }
        console.log(`Frame not ready (${i + 1}/${retries})`);
        await page.waitForTimeout(delay);
      }
      await page.screenshot({ path: 'screenshots/frame-error.png', fullPage: true });
      throw new Error('Frame not found');
    }

    async function safeClick(locator, retries = 5) {
      for (let i = 0; i < retries; i++) {
        try {
          await locator.first().waitFor({ state: 'visible', timeout: 5000 });
          await locator.first().click({ timeout: 5000 });
          return;
        } catch (e) {
          console.log(`Click retry ${i + 1}: ${e.message}`);
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
      await page.screenshot({ path: 'screenshots/click-error.png', fullPage: true });
      throw new Error('Failed to click element');
    }

    async function clickButtonFromAnyFrame(buttonName, screenName = '') {
      console.log(`Trying to click ${buttonName} ${screenName ? 'for ' + screenName : ''}`);
      const escaped = buttonName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const exactRegex = new RegExp(`^${escaped}$`, 'i');
      const selectorFactories = [
        (f) => f.getByRole('button', { name: exactRegex }),
        (f) => f.locator(`input[value="${buttonName}"]`),
        (f) => f.locator(`input[type="submit"][value="${buttonName}"]`),
        (f) => f.locator(`input[type="button"][value="${buttonName}"]`),
        (f) => f.locator(`input[type="image"][alt="${buttonName}"]`),
        (f) => f.locator(`img[alt="${buttonName}"]`),
        (f) => f.locator(`[title="${buttonName}"]`),
        (f) => f.locator('a').filter({ hasText: exactRegex }),
        (f) => f.locator(`text=/^${escaped}$/i`)
      ];

      for (let attempt = 0; attempt < 8; attempt++) {
        for (const f of page.frames()) {
          for (const makeLocator of selectorFactories) {
            try {
              const loc = makeLocator(f);
              if ((await loc.count()) > 0 && await loc.first().isVisible().catch(() => false)) {
                await loc.first().click({ timeout: 5000 });
                console.log(`Clicked ${buttonName} using locator in frame: ${f.url()}`);
                await page.waitForTimeout(5000);
                return;
              }
            } catch {}
          }
        }
        console.log(`${buttonName} not ready yet (${attempt + 1}/8)`);
        await page.waitForTimeout(1500);
      }

      if (/^OK$/i.test(buttonName)) {
        console.log('OK selector not found, using footer coordinate fallback');
        const vp = page.viewportSize();
        if (vp) {
          await page.mouse.click(Math.floor(vp.width / 2) - 25, Math.floor(vp.height) - 35);
          await page.waitForTimeout(5000);
          return;
        }
      }
      throw new Error(`Could not click ${buttonName}`);
    }

    async function clickOkFromAnyFrame(screenName = '') {
      await clickButtonFromAnyFrame('OK', screenName);
    }

    async function clickCancelFromAnyFrame(screenName = '') {
      await clickButtonFromAnyFrame('Cancel', screenName);
    }

    async function clickLeftMenu(menuText) {
      console.log(`Opening menu: ${menuText}`);
      const menuFrame = await findFrame(page, async (f) => {
        return await f.locator('a').filter({ hasText: menuText }).count() > 0;
      }, 15, 2000);
      await safeClick(menuFrame.locator('a').filter({ hasText: menuText }));
      await page.waitForTimeout(5000);
    }

    async function clickSubMenuUnderMainMenu(mainMenuTitle, subMenuText) {
      console.log(`Opening submenu under ${mainMenuTitle}: ${subMenuText}`);
      for (let directAttempt = 0; directAttempt < 3; directAttempt++) {
        for (const f of page.frames()) {
          try {
            const subMenu = f.locator('a').filter({ hasText: subMenuText });
            if ((await subMenu.count()) > 0 && await subMenu.first().isVisible().catch(() => false)) {
              await safeClick(subMenu);
              await page.waitForTimeout(5000);
              console.log(`Clicked visible submenu directly: ${subMenuText}`);
              return;
            }
          } catch {}
        }
        await page.waitForTimeout(1000);
      }

      console.log(`Submenu not visible yet. Opening main menu: ${mainMenuTitle}`);
      const mainMenuFrame = await findFrame(page, async (f) => {
        return await f.locator(`span[title="${mainMenuTitle}"]`).count() > 0 ||
               await f.locator('a, span, div').filter({ hasText: new RegExp(`^${mainMenuTitle}$`, 'i') }).count() > 0;
      }, 15, 2000);

      const titleLocator = mainMenuFrame.locator(`span[title="${mainMenuTitle}"]`);
      if ((await titleLocator.count()) > 0) {
        await safeClick(titleLocator);
      } else {
        await safeClick(mainMenuFrame.locator('a, span, div').filter({ hasText: new RegExp(`^${mainMenuTitle}$`, 'i') }));
      }
      await page.waitForTimeout(3000);

      const subMenuFrame = await findFrame(page, async (f) => {
        return await f.locator('a').filter({ hasText: subMenuText }).count() > 0;
      }, 15, 2000);
      await safeClick(subMenuFrame.locator('a').filter({ hasText: subMenuText }));
      await page.waitForTimeout(5000);
    }

    async function waitForScreenTitle(titleText) {
      console.log(`Waiting for screen title: ${titleText}`);
      await findFrame(page, async (f) => {
        return await f.locator(`text=${titleText}`).count() > 0;
      }, 15, 2000);
      console.log(`Screen title found: ${titleText}`);
    }

    async function findPolicyFormFrame(screenName) {
      console.log(`Finding policy form frame for: ${screenName}`);
      const formFrame = await findFrame(page, async (f) => {
        const hasPolicyLabel = await f.locator('text=/Policy\\s*Id/i').count() > 0;
        const visibleInputs = await f.locator('input:visible').count() > 0;
        return hasPolicyLabel && visibleInputs;
      }, 15, 2000);
      console.log(`Policy form frame found for: ${screenName}`);
      return formFrame;
    }

    async function fillPolicyIdOnScreen(screenName, policyId = POLICY_ID) {
      const formFrame = await findPolicyFormFrame(screenName);
      const policyInput = formFrame.locator('input:visible').first();
      await policyInput.waitFor({ state: 'visible', timeout: 10000 });
      await policyInput.click();
      await policyInput.fill(policyId, { timeout: 10000 });
      console.log(`Policy ID entered for ${screenName}: ${policyId}`);
      return formFrame;
    }

    async function fillPolicyIdAndEffectiveDate(screenName, policyId, effectiveDate) {
      const formFrame = await findPolicyFormFrame(screenName);
      const inputs = formFrame.locator('input:visible');
      await expect.poll(async () => await inputs.count(), { timeout: 10000 }).toBeGreaterThanOrEqual(3);

      // Screen order is Policy Id, Suffix, Effective Date.
      // Fill input #1 for Policy Id and input #3 for Effective Date.
      const policyInput = inputs.nth(0);
      const effectiveDateInput = inputs.nth(2);

      await policyInput.click();
      await policyInput.fill(policyId, { timeout: 10000 });
      await effectiveDateInput.click();
      await effectiveDateInput.fill(effectiveDate, { timeout: 10000 });

      const enteredPolicyId = await policyInput.inputValue().catch(() => '');
      const enteredEffectiveDate = await effectiveDateInput.inputValue().catch(() => '');
      console.log(`Policy ID and Effective Date entered for ${screenName}: ${enteredPolicyId}, ${enteredEffectiveDate}`);

      if (enteredEffectiveDate !== effectiveDate) {
        await page.screenshot({ path: `screenshots/effective-date-fill-warning-${policyId}.png`, fullPage: true });
        throw new Error(`Effective Date was not entered correctly for ${screenName}. Expected ${effectiveDate}, found ${enteredEffectiveDate}`);
      }

      return formFrame;
    }

    async function fillSingleVisibleInput(screenName, value) {
      console.log(`Filling single input for ${screenName}: ${value}`);
      const formFrame = await findFrame(page, async (f) => {
        return await f.locator('input:visible').count() > 0;
      }, 15, 2000);
      const input = formFrame.locator('input:visible').first();
      await input.waitFor({ state: 'visible', timeout: 10000 });
      await input.click();
      await input.fill(value, { timeout: 10000 });
      return formFrame;
    }

    async function getAllFrameText() {
      let text = '';
      for (const f of page.frames()) {
        try {
          text += '\n' + await f.locator('body').innerText({ timeout: 3000 });
        } catch {}
        try {
          const inputDump = await f.locator('input').evaluateAll((inputs) =>
            inputs.map((el, index) => `input[${index}] name=${el.name || ''} id=${el.id || ''} value=${el.value || ''}`).join('\n')
          );
          text += '\n' + inputDump;
        } catch {}
        try {
          const selectDump = await f.locator('select').evaluateAll((selects) =>
            selects.map((el, index) => `select[${index}] name=${el.name || ''} id=${el.id || ''} value=${el.value || ''}`).join('\n')
          );
          text += '\n' + selectDump;
        } catch {}
      }
      return text;
    }

    async function extractServiceAgentId() {
      const text = await getAllFrameText();
      fs.writeFileSync(`screenshots/policy-modification-update-page-text-${MAJOR_POLICY_ID}.txt`, text, 'utf8');

      const patterns = [
        /Servicing\s+Agent\s+(?:Id|ID|No|Number)?\s*[:\-]?\s*(\d{4,})/i,
        /Service\s+Agent\s+(?:Id|ID|No|Number)?\s*[:\-]?\s*(\d{4,})/i,
        /Agent\s+(?:Id|ID|No|Number)\s*[:\-]?\s*(\d{4,})/i,
        /Servicing\s+Agent[\s\S]{0,120}?(\d{4,})/i,
        /Service\s+Agent[\s\S]{0,120}?(\d{4,})/i
      ];

      for (const pattern of patterns) {
        const match = text.match(pattern);
        if (match?.[1]) {
          console.log(`Extracted Service Agent ID: ${match[1]}`);
          return match[1];
        }
      }

      const candidates = [...new Set((text.match(/\b\d{5,8}\b/g) || []))]
        .filter(v => v !== POLICY_ID && v !== MAJOR_POLICY_ID);
      fs.writeFileSync(`screenshots/service-agent-id-candidates-${MAJOR_POLICY_ID}.txt`, candidates.join('\n'), 'utf8');

      await page.screenshot({ path: `screenshots/service-agent-id-not-found-${MAJOR_POLICY_ID}.png`, fullPage: true });
      throw new Error(`Service Agent ID could not be extracted. Check policy-modification-update-page-text-${MAJOR_POLICY_ID}.txt and service-agent-id-candidates-${MAJOR_POLICY_ID}.txt`);
    }

    async function scrollAndCapture(prefix, count = 6, policyId = POLICY_ID) {
      console.log(`Capturing scroll screenshots: ${prefix}`);

      async function scrollState() {
        const states = [];
        for (const f of page.frames()) {
          try {
            const state = await f.evaluate(() => ({
              x: window.scrollX,
              y: window.scrollY,
              h: document.documentElement.scrollHeight || document.body.scrollHeight,
              wh: window.innerHeight
            }));
            states.push(JSON.stringify(state));
          } catch {}
        }
        return states.join('|');
      }

      for (const f of page.frames()) {
        try { await f.evaluate(() => window.scrollTo(0, 0)); } catch {}
      }
      await page.waitForTimeout(700);

      for (let i = 0; i < count; i++) {
        await page.screenshot({ path: `screenshots/${prefix}-${policyId}-${i + 1}.png`, fullPage: true });
        const before = await scrollState();
        for (const f of page.frames()) {
          try { await f.evaluate(() => window.scrollBy(0, window.innerHeight * 0.85)); } catch {}
        }
        await page.waitForTimeout(900);
        const after = await scrollState();
        if (i > 0 && before === after) {
          console.log(`Reached end of page for ${prefix} after ${i + 1} screenshot(s)`);
          break;
        }
      }
      console.log(`Completed screenshots for: ${prefix}`);
    }


    // STEP 1: Launch
    await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(5000);
    await page.screenshot({ path: 'screenshots/01-launch.png', fullPage: true });

    // STEP 2: English button
    const english = page.getByText('English Sign On');
    if (await english.isVisible().catch(() => false)) {
      await english.click();
      console.log('Clicked English Sign On');
    }
    await page.waitForTimeout(5000);

    // STEP 3: OK POPUP AFTER ENGLISH SIGN ON
    // U2 SSO uses browser-level authentication handled by httpCredentials.
    // After English Sign On, click OK and continue to the existing Ingenium flow.
    try {
      const popupFrame = await findFrame(page, async (f) => {
        return await f.getByRole('button', { name: /^OK$/i }).count() > 0;
      }, 8, 2000);
      await safeClick(popupFrame.getByRole('button', { name: /^OK$/i }));
      console.log('Clicked OK popup after English Sign On');
      await page.waitForTimeout(8000);
    } catch {
      console.log('No OK popup after English Sign On');
    }
    await page.screenshot({ path: 'screenshots/02-after-u2-sso-ok.png', fullPage: true });

    // STEP 4: APP FRAME
    const appFrame = await findFrame(page, async (f) => {
      return await f.locator('span[title="Policy Inquiry"]').count() > 0;
    });
    console.log('App frame ready');

    // STEP 7: EXISTING FLOW - POLICY INQUIRY ALL DETAILS
    await safeClick(appFrame.locator('span[title="Policy Inquiry"]'));
    await safeClick(appFrame.locator('a').filter({ hasText: 'Policy Inquiry - All Details' }));
    console.log('Navigation successful');
    await page.waitForTimeout(5000);

    // STEP 8: EXISTING FORM FRAME
    const formFrame = await findFrame(page, async (f) => await f.locator('input').count() > 0);
    await formFrame.locator('input').first().fill(POLICY_ID);
    console.log('Policy ID entered:', POLICY_ID);
    await page.waitForTimeout(3000);

    // STEP 9: EXISTING OK LOGIC
    try {
      const okFrame = await findFrame(page, async (f) => {
        return await f.getByRole('button', { name: 'OK' }).count() > 0;
      }, 5, 2000);
      await safeClick(okFrame.getByRole('button', { name: 'OK' }));
      console.log('Clicked OK after policy');
      await page.waitForTimeout(6000);
    } catch {
      console.log('OK not found in frame, fallback click');
      const vp = page.viewportSize();
      if (vp) await page.mouse.click(Math.floor(vp.width / 2), Math.floor(vp.height - 40));
    }

    // STEP 10: FINAL SCREENSHOT - EXISTING FLOW
    await page.screenshot({ path: `screenshots/policy-${POLICY_ID}.png`, fullPage: true });
    console.log(`Screenshot saved: screenshots/policy-${POLICY_ID}.png`);

    // STEP 11: INQUIRY COVERAGE VALUES
    await clickLeftMenu('Inquiry - Coverage Values');
    await waitForScreenTitle('Inquiry - Coverage Values');
    await fillPolicyIdOnScreen('Inquiry - Coverage Values');
    await page.screenshot({ path: `screenshots/coverage-values-before-ok-${POLICY_ID}.png`, fullPage: true });
    await clickOkFromAnyFrame('Inquiry - Coverage Values');
    await page.waitForTimeout(6000);
    await page.screenshot({ path: `screenshots/coverage-values-${POLICY_ID}.png`, fullPage: true });
    console.log('Coverage Values completed');

    // STEP 12: INQUIRY COVERAGE DETAILS
    await clickLeftMenu('Inquiry - Coverage Details');
    await waitForScreenTitle('Inquiry - Coverage Details');
    await fillPolicyIdOnScreen('Inquiry - Coverage Details');
    await page.screenshot({ path: `screenshots/coverage-details-before-ok-${POLICY_ID}.png`, fullPage: true });
    await clickOkFromAnyFrame('Inquiry - Coverage Details');
    await page.waitForTimeout(7000);
    await scrollAndCapture('coverage-details', 6);
    console.log('Coverage Details completed');

    // STEP 13: CALL CENTRE INFORMATION
    await clickLeftMenu('Inquiry - Call Centre Information');
    await waitForScreenTitle('Inquiry - Call Centre Information');
    await fillPolicyIdOnScreen('Inquiry - Call Centre Information');
    await page.screenshot({ path: `screenshots/call-centre-before-ok-${POLICY_ID}.png`, fullPage: true });
    await clickOkFromAnyFrame('Inquiry - Call Centre Information');
    await page.waitForTimeout(7000);
    await scrollAndCapture('call-centre', 6);
    console.log('Call Centre Information completed');

    // STEP 14: MAJOR POLICY CHANGE - COVERAGE RISK INQUIRY
    await clickSubMenuUnderMainMenu('Major Policy Change', 'Coverage Risk Inquiry');
    await waitForScreenTitle('Coverage Risk Inquiry');
    await fillPolicyIdOnScreen('Coverage Risk Inquiry', MAJOR_POLICY_ID);
    await page.screenshot({ path: `screenshots/coverage-risk-inquiry-before-ok-${MAJOR_POLICY_ID}.png`, fullPage: true });
    await clickOkFromAnyFrame('Coverage Risk Inquiry');
    await page.waitForTimeout(7000);
    await page.screenshot({ path: `screenshots/coverage-risk-inquiry-${MAJOR_POLICY_ID}.png`, fullPage: true });
    await scrollAndCapture('coverage-risk-inquiry', 8, MAJOR_POLICY_ID);
    console.log('Coverage Risk Inquiry completed');

    // STEP 15: MAJOR POLICY CHANGE - COVERAGE RISK UPDATE
    await clickSubMenuUnderMainMenu('Major Policy Change', 'Coverage Risk Update');
    await waitForScreenTitle('Coverage Risk Update');
    await fillPolicyIdOnScreen('Coverage Risk Update', MAJOR_POLICY_ID);
    await page.screenshot({ path: `screenshots/coverage-risk-update-before-ok-${MAJOR_POLICY_ID}.png`, fullPage: true });
    await clickOkFromAnyFrame('Coverage Risk Update');
    await page.waitForTimeout(7000);
    await page.screenshot({ path: `screenshots/coverage-risk-update-${MAJOR_POLICY_ID}.png`, fullPage: true });
    await scrollAndCapture('coverage-risk-update', 8, MAJOR_POLICY_ID);
    console.log('Coverage Risk Update completed');

    // STEP 16: SAME MAJOR POLICY CHANGE - POLICY MODIFICATION INQUIRY
    await clickSubMenuUnderMainMenu('Major Policy Change', 'Policy Modification Inquiry');
    await waitForScreenTitle('Policy Modification Inquiry');
    await fillPolicyIdAndEffectiveDate('Policy Modification Inquiry', MAJOR_POLICY_ID, EFFECTIVE_DATE);
    await page.screenshot({ path: `screenshots/policy-modification-inquiry-before-ok-${MAJOR_POLICY_ID}.png`, fullPage: true });
    await clickOkFromAnyFrame('Policy Modification Inquiry');
    await page.waitForTimeout(7000);
    await scrollAndCapture('policy-modification-inquiry', 8, MAJOR_POLICY_ID);
    await clickOkFromAnyFrame('Policy Modification Inquiry Result');
    console.log('Policy Modification Inquiry completed');

    // STEP 17: SAME MAJOR POLICY CHANGE - POLICY MODIFICATION UPDATE
    await clickSubMenuUnderMainMenu('Major Policy Change', 'Policy Modification Update');
    await waitForScreenTitle('Policy Modification Update');
    await fillPolicyIdAndEffectiveDate('Policy Modification Update', MAJOR_POLICY_ID, EFFECTIVE_DATE);
    await page.screenshot({ path: `screenshots/policy-modification-update-before-ok-${MAJOR_POLICY_ID}.png`, fullPage: true });
    await clickOkFromAnyFrame('Policy Modification Update');
    await page.waitForTimeout(7000);
    await scrollAndCapture('policy-modification-update-transaction', 8, MAJOR_POLICY_ID);

    const SERVICE_AGENT_ID = await extractServiceAgentId();
    console.log('SERVICE_AGENT_ID:', SERVICE_AGENT_ID);

    // Per the business flow, after the Policy Modification Update transaction
    // details screen, click OK first. The Confirm screen appears only after OK.
    await page.screenshot({ path: `screenshots/policy-modification-update-transaction-before-ok-${MAJOR_POLICY_ID}.png`, fullPage: true });
    await clickOkFromAnyFrame('Policy Modification Update Transaction Details');
    await page.waitForTimeout(7000);

    await page.screenshot({ path: `screenshots/policy-modification-update-confirm-screen-${MAJOR_POLICY_ID}.png`, fullPage: true });
    await clickButtonFromAnyFrame('Confirm', 'Policy Modification Update Confirm');
    await page.waitForTimeout(7000);
    await page.screenshot({ path: `screenshots/policy-modification-update-confirm-${MAJOR_POLICY_ID}.png`, fullPage: true });
    await scrollAndCapture('policy-modification-update-confirm', 4, MAJOR_POLICY_ID);
    console.log('Policy Modification Update completed');

    // STEP 18: AGENT - AGENT UPDATE
    await clickSubMenuUnderMainMenu('Agent', 'Agent Update');
    await waitForScreenTitle('Agent Update');
    await fillSingleVisibleInput('Agent Update', SERVICE_AGENT_ID);
    await page.screenshot({ path: `screenshots/agent-update-before-ok-${SERVICE_AGENT_ID}.png`, fullPage: true });
    await clickOkFromAnyFrame('Agent Update');
    await page.waitForTimeout(7000);
    await scrollAndCapture('agent-update', 8, SERVICE_AGENT_ID);
    await clickOkFromAnyFrame('Agent Update Result');
    console.log('Agent Update completed');

    // STEP 19: AGENT - AGENT LIST
    await clickSubMenuUnderMainMenu('Agent', 'Agent List');
    await waitForScreenTitle('Agent List');
    await fillSingleVisibleInput('Agent List', SERVICE_AGENT_ID);
    await page.screenshot({ path: `screenshots/agent-list-before-ok-${SERVICE_AGENT_ID}.png`, fullPage: true });
    await clickOkFromAnyFrame('Agent List');
    await page.waitForTimeout(7000);
    await page.screenshot({ path: `screenshots/agent-list-${SERVICE_AGENT_ID}.png`, fullPage: true });
    await clickCancelFromAnyFrame('Agent List Result');
    console.log('Agent List completed');

    // STEP 20: AGENT - AGENT INQUIRY
    await clickSubMenuUnderMainMenu('Agent', 'Agent Inquiry');
    await waitForScreenTitle('Agent Inquiry');
    await fillSingleVisibleInput('Agent Inquiry', SERVICE_AGENT_ID);
    await page.screenshot({ path: `screenshots/agent-inquiry-before-ok-${SERVICE_AGENT_ID}.png`, fullPage: true });
    await clickOkFromAnyFrame('Agent Inquiry');
    await page.waitForTimeout(7000);
    await scrollAndCapture('agent-inquiry', 8, SERVICE_AGENT_ID);
    await clickOkFromAnyFrame('Agent Inquiry Result');
    console.log('Agent Inquiry completed');

    console.log('ALL FLOWS COMPLETED SUCCESSFULLY');
  } catch (e) {
    console.error('TEST FAILURE:', e);
    try {
      await page.screenshot({ path: 'screenshots/failure-final.png', fullPage: true });
    } catch (screenshotError) {
      console.error('Could not capture failure screenshot:', screenshotError);
    }
    throw e;
  }
});
