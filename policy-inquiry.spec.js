import { test, expect } from '@playwright/test';
import fs from 'fs';
import { execSync } from 'child_process';

const SCREENSHOT_DIR = 'screenshots';

function ensureScreenshotDir() {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

function sanitizeName(value) {
  return String(value || 'screen')
    .replace(/[^a-z0-9]+/gi, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase();
}

function parseDb2Value(output) {
  return String(output || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .filter((line) => !/^SQL/i.test(line))
    .filter((line) => !/^[-=]+$/.test(line))
    .map((line) => line.split(/\s+/)[0])
    .find(Boolean);
}

function db2Value(envName, query) {
  const fromEnv = process.env[envName];
  if (fromEnv && fromEnv.trim()) {
    console.log(`Using ${envName} from environment: ${fromEnv}`);
    return fromEnv.trim();
  }

  console.log(`Environment variable ${envName} not set. Trying DB2 query from Node runtime.`);
  try {
    const command = `db2 -x "${query.replace(/"/g, '\\"')}"`;
    const output = execSync(command, {
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe']
    });
    const value = parseDb2Value(output);
    if (!value) {
      throw new Error(`DB2 query returned no usable value for ${envName}`);
    }
    console.log(`Fetched ${envName} from DB2: ${value}`);
    return value;
  } catch (error) {
    throw new Error(`Missing ${envName}. Set it in Jenkins from DB2 before running Playwright. Query: ${query}. Error: ${error.message}`);
  }
}

function yesterdayDateYYYYMMDD() {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  return d.toISOString().slice(0, 10);
}

async function findFrame(page, predicate, retries = 20, delay = 1500) {
  for (let i = 0; i < retries; i++) {
    for (const frame of page.frames()) {
      try {
        if (await predicate(frame)) {
          return frame;
        }
      } catch {}
    }
    console.log(`Frame not ready (${i + 1}/${retries})`);
    await page.waitForTimeout(delay);
  }
  await page.screenshot({ path: `${SCREENSHOT_DIR}/frame-not-found.png`, fullPage: true });
  throw new Error('Frame not found for requested predicate');
}

async function safeClick(page, locator, label = 'element', retries = 6) {
  for (let i = 0; i < retries; i++) {
    try {
      const first = locator.first();
      await first.waitFor({ state: 'visible', timeout: 5000 });
      await first.click({ timeout: 5000 });
      console.log(`Clicked ${label}`);
      return;
    } catch (error) {
      console.log(`Click retry ${i + 1}/${retries} for ${label}: ${error.message}`);
      try {
        const box = await locator.first().boundingBox();
        if (box) {
          await page.mouse.click(box.x + Math.min(10, box.width / 2), box.y + Math.min(10, box.height / 2));
          console.log(`Clicked ${label} using coordinate fallback`);
          return;
        }
      } catch {}
      await page.waitForTimeout(1500);
    }
  }
  await page.screenshot({ path: `${SCREENSHOT_DIR}/click-failed-${sanitizeName(label)}.png`, fullPage: true });
  throw new Error(`Failed to click ${label}`);
}

async function clickOkFromAnyFrame(page, screenName = '') {
  console.log(`Trying to click OK ${screenName ? 'for ' + screenName : ''}`);
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

  for (let attempt = 0; attempt < 10; attempt++) {
    for (const frame of page.frames()) {
      for (const makeLocator of selectorFactories) {
        try {
          const loc = makeLocator(frame);
          if ((await loc.count()) > 0 && await loc.first().isVisible().catch(() => false)) {
            await loc.first().click({ timeout: 5000 });
            console.log(`Clicked OK in frame: ${frame.url()}`);
            await page.waitForTimeout(5000);
            return;
          }
        } catch {}
      }
    }
    await page.waitForTimeout(1000);
  }

  console.log('OK selector not found. Using footer coordinate fallback.');
  const vp = page.viewportSize();
  if (vp) {
    await page.mouse.click(Math.floor(vp.width / 2) - 25, Math.floor(vp.height) - 35);
    await page.waitForTimeout(5000);
    return;
  }
  throw new Error('Could not click OK');
}

async function clickMenuPath(page, appFrame, mainMenu, subMenu) {
  console.log(`Navigating menu path: ${mainMenu} -> ${subMenu}`);

  const mainCandidates = [
    appFrame.locator(`span[title="${mainMenu}"]`),
    appFrame.locator('span').filter({ hasText: new RegExp(`^${escapeRegExp(mainMenu)}$`, 'i') }),
    appFrame.locator('a').filter({ hasText: new RegExp(escapeRegExp(mainMenu), 'i') }),
    appFrame.getByText(mainMenu, { exact: true })
  ];
  await clickFirstAvailable(page, mainCandidates, mainMenu);
  await page.waitForTimeout(1500);

  const subCandidates = [
    appFrame.locator('a').filter({ hasText: new RegExp(escapeRegExp(subMenu), 'i') }),
    appFrame.locator('span').filter({ hasText: new RegExp(escapeRegExp(subMenu), 'i') }),
    appFrame.getByText(subMenu, { exact: true })
  ];
  await clickFirstAvailable(page, subCandidates, subMenu);
  await page.waitForTimeout(5000);
}

async function clickFirstAvailable(page, locators, label) {
  let lastError;
  for (const locator of locators) {
    try {
      if ((await locator.count()) > 0) {
        await safeClick(page, locator, label);
        return;
      }
    } catch (error) {
      lastError = error;
    }
  }
  throw lastError || new Error(`Locator not found for ${label}`);
}

function escapeRegExp(text) {
  return String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

async function findAppFrame(page) {
  return await findFrame(page, async (frame) => {
    const markers = [
      'Policy Inquiry',
      'Agent',
      'Client',
      'Billing',
      'Disbursements',
      'Medical Claim Inquiry'
    ];
    for (const marker of markers) {
      if (await frame.locator('span,a').filter({ hasText: marker }).count() > 0) {
        return true;
      }
    }
    return false;
  }, 25, 2000);
}

async function fillVisibleInputs(page, screenName, values) {
  const frame = await findFrame(page, async (f) => {
    return await f.locator('input:visible').count() >= values.length;
  }, 20, 1500);

  const inputs = frame.locator('input:visible');
  const count = await inputs.count();
  console.log(`Found ${count} visible input(s) for ${screenName}. Filling ${values.length} value(s).`);

  for (let i = 0; i < values.length; i++) {
    const input = inputs.nth(i);
    await input.waitFor({ state: 'visible', timeout: 10000 });
    await input.click({ timeout: 5000 });
    await input.fill(String(values[i]), { timeout: 10000 });
    console.log(`Filled input ${i + 1} for ${screenName} with value ${values[i]}`);
  }
}

async function scrollAndCapture(page, prefix, idValue, count = 5) {
  console.log(`Capturing screenshots for ${prefix}`);
  for (const frame of page.frames()) {
    try { await frame.evaluate(() => window.scrollTo(0, 0)); } catch {}
  }
  await page.waitForTimeout(1000);

  for (let i = 0; i < count; i++) {
    await page.screenshot({
      path: `${SCREENSHOT_DIR}/${sanitizeName(prefix)}-${idValue}-${i + 1}.png`,
      fullPage: true
    });
    for (const frame of page.frames()) {
      try { await frame.evaluate(() => window.scrollBy(0, window.innerHeight * 0.85)); } catch {}
    }
    await page.waitForTimeout(1200);
  }
}

async function runInquiryScreen(page, appFrame, screen) {
  console.log(`========== Running screen: ${screen.name} ==========`);
  await clickMenuPath(page, appFrame, screen.mainMenu, screen.subMenu);
  await fillVisibleInputs(page, screen.name, screen.values);
  await page.screenshot({
    path: `${SCREENSHOT_DIR}/${sanitizeName(screen.name)}-before-ok.png`,
    fullPage: true
  });
  await clickOkFromAnyFrame(page, screen.name);
  await page.waitForTimeout(screen.waitAfterOkMs || 7000);
  await scrollAndCapture(page, screen.name, screen.values.join('-'), screen.captureCount || 5);
  console.log(`========== Completed screen: ${screen.name} ==========`);
}

test('Ingenium extended multi-screen smoke flow', async ({ page }) => {
  ensureScreenshotDir();

  const BASE_URL = process.env.APP_URL;
  const USERNAME = process.env.APP_USERNAME;
  const PASSWORD = process.env.APP_PASSWORD;
  const COMPANY = process.env.COMPANY || 'Manulife';

  expect(BASE_URL).toBeTruthy();
  expect(USERNAME).toBeTruthy();
  expect(PASSWORD).toBeTruthy();

  const AGT_ID = db2Value('AGT_ID', "SELECT AGT_ID FROM TAG WHERE CO_ID='CP' LIMIT 1");
  const CLI_ID = db2Value('CLI_ID', "SELECT CLI_ID FROM TCLI WHERE CO_ID='CP' LIMIT 1");
  const WL_POL_ID = db2Value('WL_POL_ID', "SELECT POL_ID FROM TPOL WHERE CO_ID='CP' AND PROD_APP_TYP_CD='WL' AND POL_CSTAT_CD='1' LIMIT 1");
  const FIRM_BANKING_POL_ID = db2Value('FIRM_BANKING_POL_ID', "SELECT POL_ID FROM TFBNK WHERE CO_ID='CP' LIMIT 1");
  const DEATH_CLM_ID = db2Value('DEATH_CLM_ID', "SELECT CLM_ID FROM TDCLM WHERE CO_ID='CP' AND CLM_STAT_CD='C' LIMIT 1");
  const MED_CLM_ID = db2Value('MED_CLM_ID', "SELECT CLM_ID FROM TCLBD WHERE CO_ID='CP' LIMIT 1");
  const REMITTANCE_DATE = process.env.REMITTANCE_DATE || yesterdayDateYYYYMMDD();

  console.log('START EXTENDED INGENIUM SCREEN TEST');
  console.log('BASE_URL:', BASE_URL);
  console.log('AGT_ID:', AGT_ID);
  console.log('CLI_ID:', CLI_ID);
  console.log('WL_POL_ID:', WL_POL_ID);
  console.log('FIRM_BANKING_POL_ID:', FIRM_BANKING_POL_ID);
  console.log('DEATH_CLM_ID:', DEATH_CLM_ID);
  console.log('MED_CLM_ID:', MED_CLM_ID);
  console.log('REMITTANCE_DATE:', REMITTANCE_DATE);

  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded', timeout: 120000 });
  await page.waitForTimeout(5000);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/01-launch.png`, fullPage: true });

  const english = page.getByText('English Sign On');
  if (await english.isVisible().catch(() => false)) {
    await english.click();
    console.log('Clicked English Sign On');
  }
  await page.waitForTimeout(5000);

  const loginFrame = await findFrame(page, async (f) => await f.locator('input[type="password"]').count() > 0);
  await loginFrame.locator('input[type="text"]').first().fill(USERNAME);
  await loginFrame.locator('input[type="password"]').fill(PASSWORD);
  await loginFrame.locator('select').selectOption({ label: COMPANY });
  await safeClick(page, loginFrame.getByRole('button', { name: /submit/i }), 'Submit Login');
  await page.waitForTimeout(6000);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/02-after-login.png`, fullPage: true });

  try {
    await clickOkFromAnyFrame(page, 'post-login popup');
  } catch {
    console.log('No post-login popup OK found. Continuing.');
  }

  const appFrame = await findAppFrame(page);
  console.log('Application menu frame ready');

  const screens = [
    { name: 'Agent - Agent Inquiry', mainMenu: 'Agent', subMenu: 'Agent Inquiry', values: [AGT_ID], captureCount: 5 },
    { name: 'Client - Address List', mainMenu: 'Client', subMenu: 'Address List', values: [CLI_ID], captureCount: 5 },
    { name: 'Client - Client Inquiry', mainMenu: 'Client', subMenu: 'Client Inquiry', values: [CLI_ID], captureCount: 5 },
    { name: 'Client - Previous Name List', mainMenu: 'Client', subMenu: 'Previous Name List', values: [CLI_ID], captureCount: 5 },
    { name: 'Client Service - Client Inquiry General', mainMenu: 'Client Service', subMenu: 'Client Inquiry - General', values: [CLI_ID], captureCount: 5 },
    { name: 'Client Service - Client Owner Summary', mainMenu: 'Client Service', subMenu: 'Client Owner Summary', values: [CLI_ID], captureCount: 5 },
    { name: 'Medical Claim Inquiry - Master Claim Inquiry', mainMenu: 'Medical Claim Inquiry', subMenu: 'Master Claim Inquiry', values: [MED_CLM_ID], captureCount: 5 },
    { name: 'Death Claims Inquiry - Death Master Claim Inquiry', mainMenu: 'Death Claims Inquiry', subMenu: 'Death Master Claim Inquiry', values: [DEATH_CLM_ID], captureCount: 5 },
    { name: 'Disbursements - Firm Banking Entries', mainMenu: 'Disbursements', subMenu: 'Firm Banking Entries', values: [REMITTANCE_DATE, FIRM_BANKING_POL_ID], captureCount: 5 },
    { name: 'Billing - Billing Activity Inquiry List by Policy', mainMenu: 'Billing', subMenu: 'Billing Activity List', values: [WL_POL_ID], captureCount: 5 },
    { name: 'Complex Policy Change - Movement Inquiry', mainMenu: 'Complex Policy Change', subMenu: 'Movement Inquiry', values: [WL_POL_ID], captureCount: 5 }
  ];

  for (const screen of screens) {
    await runInquiryScreen(page, appFrame, screen);
  }

  console.log('ALL EXTENDED INGENIUM SCREENS COMPLETED SUCCESSFULLY');
});
