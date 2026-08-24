import { test, expect } from '@playwright/test';
import fs from 'fs';

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

function parseCurlCookieJar(cookieJarPath) {
  if (!cookieJarPath || !fs.existsSync(cookieJarPath)) {
    console.log('No curl cookie jar found. COOKIE_JAR:', cookieJarPath || 'not-set');
    return [];
  }

  const lines = fs.readFileSync(cookieJarPath, 'utf8')
    .split('\n')
    .filter((line) => line.trim() && !line.startsWith('# Netscape') && !line.startsWith('# This file'));

  const cookies = [];
  for (const line of lines) {
    const httpOnly = line.startsWith('#HttpOnly_');
    const normalized = httpOnly ? line.replace('#HttpOnly_', '') : line;
    if (normalized.startsWith('#')) continue;
    const parts = normalized.split('\t');
    if (parts.length < 7) continue;
    const [domain, , path, secure, expires, name, ...valueParts] = parts;
    const value = valueParts.join('\t');
    if (!domain || !path || !name || !value) continue;
    cookies.push({
      domain,
      path,
      secure: secure.toUpperCase() === 'TRUE',
      expires: Number(expires) || -1,
      name,
      value,
      httpOnly,
      sameSite: 'Lax'
    });
  }
  return cookies;
}

async function loadCurlCookiesIntoContext(context, cookieJar) {
  cookieJar = cookieJar || process.env.COOKIE_JAR;
  const cookies = parseCurlCookieJar(cookieJar);
  console.log('Cookies parsed from curl jar:', cookies.map((cookie) => cookie.name).join(', ') || 'none');
  if (cookies.length > 0) {
    await context.addCookies(cookies);
    console.log('Added curl SPNEGO cookies to Playwright context:', cookies.length);
  }
}

async function findFrame(page, predicate, retries = 20, delay = 1500) {
  for (let i = 0; i < retries; i++) {
    for (const frame of page.frames()) {
      try {
        if (await predicate(frame)) return frame;
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

function escapeRegExp(text) {
  return String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

async function domClick(locator, label) {
  const first = locator.first();
  await first.evaluate((element) => {
    if (typeof window.clickedMenu === 'function') return window.clickedMenu(element);
    element.click();
    return true;
  });
  console.log(`Clicked ${label} using DOM onclick fallback`);
}

async function clickFirstAvailable(page, locators, label) {
  let lastError;
  for (const locator of locators) {
    try {
      const count = await locator.count();
      if (count === 0) continue;
      for (let i = 0; i < count; i++) {
        const item = locator.nth(i);
        if (await item.isVisible().catch(() => false)) {
          await safeClick(page, item, label);
          return;
        }
      }
      await domClick(locator, label);
      await page.waitForTimeout(5000);
      return;
    } catch (error) {
      lastError = error;
      console.log(`Locator strategy failed for ${label}: ${error.message}`);
    }
  }
  await page.screenshot({ path: `${SCREENSHOT_DIR}/click-unavailable-${sanitizeName(label)}.png`, fullPage: true });
  throw lastError || new Error(`Locator not found for ${label}`);
}

async function clickMenuPath(page, appFrame, mainMenu, subMenu, subMenuAliases = []) {
  console.log(`Navigating menu path: ${mainMenu} -> ${subMenu}`);
  const mainCandidates = [
    appFrame.locator(`span[title="${mainMenu}"]`),
    appFrame.locator('span').filter({ hasText: new RegExp(`^${escapeRegExp(mainMenu)}$`, 'i') }),
    appFrame.locator('a').filter({ hasText: new RegExp(escapeRegExp(mainMenu), 'i') }),
    appFrame.getByText(mainMenu, { exact: true })
  ];
  await clickFirstAvailable(page, mainCandidates, mainMenu);
  await page.waitForTimeout(1500);

  const labels = [subMenu, ...subMenuAliases].filter(Boolean);
  const subCandidates = [];
  for (const label of labels) {
    subCandidates.push(appFrame.locator('a').filter({ hasText: new RegExp(escapeRegExp(label), 'i') }));
    subCandidates.push(appFrame.locator('span').filter({ hasText: new RegExp(escapeRegExp(label), 'i') }));
    subCandidates.push(appFrame.getByText(label, { exact: true }));
  }
  if (/loan quote/i.test(subMenu) || /manual ps/i.test(subMenu)) {
    subCandidates.push(appFrame.locator('a').filter({ hasText: /Loan\/APL\/APS/i }));
    subCandidates.push(appFrame.locator('a').filter({ hasText: /Manual PS Judgment/i }));
    subCandidates.push(appFrame.locator('span').filter({ hasText: /Loan\/APL\/APS/i }));
    subCandidates.push(appFrame.locator('span').filter({ hasText: /Manual PS Judgment/i }));
  }
  await clickFirstAvailable(page, subCandidates, subMenu);
  await page.waitForTimeout(5000);
}

async function findAppFrame(page) {
  return await findFrame(page, async (frame) => {
    const markers = ['Policy Inquiry', 'Agent', 'Client', 'Billing', 'Disbursements', 'Medical Claim Inquiry'];
    for (const marker of markers) {
      if (await frame.locator('span,a').filter({ hasText: marker }).count() > 0) return true;
    }
    return false;
  }, 25, 2000);
}

async function fillVisibleInputs(page, screenName, values) {
  const inputSelector = 'input:visible:not([type="image"]):not([type="submit"]):not([type="button"]):not([type="reset"]):not([type="checkbox"]):not([type="radio"]):not([type="hidden"])';
  const frame = await findFrame(page, async (f) => {
    const items=f.locator(inputSelector); let editable=0;
    for(let i=0;i<await items.count();i++) if(await items.nth(i).isEditable().catch(()=>false)) editable++;
    return editable>=values.length;
  },30,1500);
  const items=frame.locator(inputSelector), inputs=[];
  for(let i=0;i<await items.count();i++) if(await items.nth(i).isEditable().catch(()=>false)) inputs.push(items.nth(i));
  if(inputs.length<values.length) throw new Error(`Expected ${values.length} editable inputs for ${screenName}, found ${inputs.length}`);
  for(let i=0;i<values.length;i++){ await inputs[i].fill(String(values[i]),{timeout:10000}); console.log(`Filled input ${i+1} for ${screenName}`); }
}

async function scrollAndCapture(page, screen, idValue, count = 5) {
  const prefix = `screen-${String(screen.screenNo).padStart(2, '0')}-${sanitizeName(screen.name)}`;
  console.log(`Capturing screenshots for ${screen.name}`);
  for (const frame of page.frames()) {
    try { await frame.evaluate(() => window.scrollTo(0, 0)); } catch {}
  }
  await page.waitForTimeout(1000);
  for (let i = 0; i < count; i++) {
    await page.screenshot({ path: `${SCREENSHOT_DIR}/${prefix}-${idValue}-${i + 1}.png`, fullPage: true });
    for (const frame of page.frames()) {
      try { await frame.evaluate(() => window.scrollBy(0, window.innerHeight * 0.85)); } catch {}
    }
    await page.waitForTimeout(1200);
  }
}

async function runInquiryScreen(page, appFrame, screen) {
  console.log(`========== Running screen: ${screen.name} ==========`);
  await clickMenuPath(page, appFrame, screen.mainMenu, screen.subMenu, screen.subMenuAliases || []);
  await fillVisibleInputs(page, screen.name, screen.values);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/screen-${String(screen.screenNo).padStart(2, '0')}-${sanitizeName(screen.name)}-before-ok.png`, fullPage: true });
  await clickOkFromAnyFrame(page, screen.name);
  await page.waitForTimeout(screen.waitAfterOkMs || 7000);
  await scrollAndCapture(page, screen, screen.values.join('-'), screen.captureCount || 5);
  console.log(`========== Completed screen: ${screen.name} ==========`);
}

async function optionalCredentialLogin(page, workerTag) {
  // U2 SSO behavior:
  // 1. Browser/domain authentication is handled by Playwright httpCredentials and/or curl SPNEGO cookie bridge.
  // 2. Click English Sign On.
  // 3. Ingenium shows Sign-On Connect with User Status = Connected.
  // 4. Click OK. Do not use GOCC / ingenium application credentials for U2 SSO.
  const english = page.getByText('English Sign On', { exact: true });
  if (await english.isVisible().catch(() => false)) {
    await english.click();
    console.log('Clicked English Sign On');
    await page.waitForTimeout(5000);
  } else {
    console.log('English Sign On link not visible. Continuing to Sign-On Connect or app frame.');
  }

  await page.screenshot({ path: `${SCREENSHOT_DIR}/${workerTag}-03-after-english-sign-on.png`, fullPage: true });
  const signOnText = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '');
  fs.writeFileSync(`${SCREENSHOT_DIR}/${workerTag}-03-after-english-sign-on-text.txt`, signOnText, 'utf8');

  const connectVisible = page.getByText(/Sign-On Connect|User Status|Connected/i);
  if (await connectVisible.first().isVisible().catch(() => false)) {
    console.log('Sign-On Connect page visible. Clicking OK to enter Ingenium application.');
    await clickOkFromAnyFrame(page, 'Sign-On Connect');
    await page.waitForTimeout(7000);
    await page.screenshot({ path: `${SCREENSHOT_DIR}/${workerTag}-04-after-signon-connect-ok.png`, fullPage: true });
    return;
  }

  try {
    await clickOkFromAnyFrame(page, 'possible Sign-On Connect');
    await page.waitForTimeout(7000);
    await page.screenshot({ path: `${SCREENSHOT_DIR}/${workerTag}-04-after-possible-signon-ok.png`, fullPage: true });
  } catch {
    console.log('No Sign-On Connect OK button found. Continuing to app frame detection.');
  }
}


function parseSelectedScreens(raw){const out=new Set();for(const t of String(raw||'1-20').split(',').map(x=>x.trim()).filter(Boolean)){const m=t.match(/^(\d+)\s*-\s*(\d+)$/);if(m){for(let n=+m[1];n<=+m[2];n++)out.add(n)}else if(/^\d+$/.test(t))out.add(+t);else throw new Error(`Invalid screen: ${t}`)}const a=[...out].sort((x,y)=>x-y);if(!a.length||a.some(n=>n<1||n>20))throw new Error('Screens must be 1-20');return a}
function chunk(a,n=2){const r=[];for(let i=0;i<a.length;i+=n)r.push(a.slice(i,i+n));return r}
const SELECTED=parseSelectedScreens(process.env.SELECTED_SCREENS), GROUPS=chunk(SELECTED,2);
console.log(`Selected SSO screens: ${SELECTED.join(', ')}`); console.log(`Parallel browser groups: ${GROUPS.map(x=>`[${x}]`).join(' ')}`);
test.describe.configure({mode:'parallel'});
for(const [groupIndex,assigned] of GROUPS.entries()){
 test(`U2 SSO browser ${groupIndex+1} - screens ${assigned.join(', ')}`,async({page,context},testInfo)=>{
  test.setTimeout(3600000);ensureScreenshotDir();const BASE_URL=process.env.APP_URL;
  const AGT_ID=process.env.AGT_ID,CLI_ID=process.env.CLI_ID,WL_POL_ID=process.env.WL_POL_ID||process.env.POLICY_ID,FIRM_BANKING_POL_ID=process.env.FIRM_BANKING_POL_ID,DEATH_CLM_ID=process.env.DEATH_CLM_ID,MED_CLM_ID=process.env.MED_CLM_ID,REMITTANCE_DATE=process.env.REMITTANCE_DATE,APL_POLICY_ID=process.env.APL_POLICY_ID,CHANGE_HIST_POLICY_ID=process.env.CHANGE_HIST_POLICY_ID,LOAN_DETAIL_POLICY_ID=process.env.LOAN_DETAIL_POLICY_ID;
  const workerTag=`worker-${testInfo.parallelIndex}-group-${groupIndex+1}`;await loadCurlCookiesIntoContext(context,`${process.env.COOKIE_JAR_PREFIX}-${groupIndex+1}.txt`);await page.goto(BASE_URL,{waitUntil:'domcontentloaded',timeout:120000});await page.waitForTimeout(5000);await optionalCredentialLogin(page,workerTag);const appFrame=await findAppFrame(page);
  const screens=[
    { screenNo: 1, name: 'Policy Inquiry - All Details', mainMenu: 'Policy Inquiry', subMenu: 'Policy Inquiry - All Details', values: [WL_POL_ID], captureCount: 5 },
    { screenNo: 2, name: 'Policy Inquiry - Inquiry Coverage Values', mainMenu: 'Policy Inquiry', subMenu: 'Inquiry - Coverage Values', values: [WL_POL_ID], captureCount: 5 },
    { screenNo: 3, name: 'Policy Inquiry - Inquiry Coverage Details', mainMenu: 'Policy Inquiry', subMenu: 'Inquiry - Coverage Details', values: [WL_POL_ID], captureCount: 6 },
    { screenNo: 4, name: 'Policy Inquiry - Inquiry Call Centre Information', mainMenu: 'Policy Inquiry', subMenu: 'Inquiry - Call Centre Information', values: [WL_POL_ID], captureCount: 6 },
    { screenNo: 5, name: 'Agent - Agent Inquiry', mainMenu: 'Agent', subMenu: 'Agent Inquiry', values: [AGT_ID], captureCount: 5 },
    { screenNo: 6, name: 'Client - Address List', mainMenu: 'Client', subMenu: 'Address List', values: [CLI_ID], captureCount: 5 },
    { screenNo: 7, name: 'Client - Client Inquiry', mainMenu: 'Client', subMenu: 'Client Inquiry', values: [CLI_ID], captureCount: 5 },
    { screenNo: 8, name: 'Client - Previous Name List', mainMenu: 'Client', subMenu: 'Previous Name List', values: [CLI_ID], captureCount: 5 },
    { screenNo: 9, name: 'Client Service - Client Inquiry General', mainMenu: 'Client Service', subMenu: 'Client Inquiry - General', values: [CLI_ID], captureCount: 5 },
    { screenNo: 10, name: 'Client Service - Client Owner Summary', mainMenu: 'Client Service', subMenu: 'Client Owner Summary', values: [CLI_ID], captureCount: 5 },
    { screenNo: 11, name: 'Medical Claim Inquiry - Master Claim Inquiry', mainMenu: 'Medical Claim Inquiry', subMenu: 'Master Claim Inquiry', values: [MED_CLM_ID], captureCount: 5 },
    { screenNo: 12, name: 'Death Claims Inquiry - Death Master Claim Inquiry', mainMenu: 'Death Claims Inquiry', subMenu: 'Death Master Claim Inquiry', values: [DEATH_CLM_ID], captureCount: 5 },
    { screenNo: 13, name: 'Disbursements - Firm Banking Entries', mainMenu: 'Disbursements', subMenu: 'Firm Banking Entries', values: [REMITTANCE_DATE, FIRM_BANKING_POL_ID], captureCount: 5 },
    { screenNo: 14, name: 'Billing - Billing Activity Inquiry List by Policy', mainMenu: 'Billing', subMenu: 'Billing Activity List', values: [WL_POL_ID], captureCount: 5 },
    { screenNo: 15, name: 'Complex Policy Change - Movement Inquiry', mainMenu: 'Complex Policy Change', subMenu: 'Movement Inquiry', values: [WL_POL_ID], captureCount: 5 },
    { screenNo: 16, name: 'Policy History - APL History List', mainMenu: 'Policy History', subMenu: 'APL History', values: [APL_POLICY_ID], captureCount: 5 },
    { screenNo: 17, name: 'Policy History - Change History List', mainMenu: 'Policy History', subMenu: 'Change History List', values: [CHANGE_HIST_POLICY_ID], captureCount: 6 },
    { screenNo: 18, name: 'Policy History - Loan Detail List', mainMenu: 'Policy History', subMenu: 'Loan Detail List', values: [LOAN_DETAIL_POLICY_ID], captureCount: 6 },
    { screenNo: 19, name: 'Policy Inquiry - Inquiry Coverage Premiums', mainMenu: 'Policy Inquiry', subMenu: 'Inquiry - Coverage Premiums', values: [WL_POL_ID], captureCount: 6 },
    { screenNo: 20, name: 'Policy Inquiry - Inquiry Loan APL APS Manual PS Judgment', mainMenu: 'Policy Inquiry', subMenu: 'Inquiry-Loan/APL/APS/Manual PS Judgment', subMenuAliases: ['Inquiry - Loan/APL/APS/Manual PS Judgment', 'Inquiry-Loan / APL / APS / Manual PS Judgment', 'Policy Loan Quote, APL or APS Judgment', 'Manual PS Judgment'], values: [WL_POL_ID], captureCount: 6 }
  ];const results=[];
  for(const screen of screens.filter(x=>assigned.includes(x.screenNo))){const started=Date.now();try{await runInquiryScreen(page,appFrame,screen);results.push({screenNo:screen.screenNo,name:screen.name,input:screen.values.join(', '),status:'PASSED',detail:'Completed',durationSeconds:Math.round((Date.now()-started)/1000)})}catch(e){results.push({screenNo:screen.screenNo,name:screen.name,input:screen.values.join(', '),status:'FAILED',detail:String(e?.message||e),durationSeconds:Math.round((Date.now()-started)/1000)})}}
  fs.writeFileSync(`${SCREENSHOT_DIR}/screen-summary-${workerTag}.json`,JSON.stringify({results},null,2));expect(results.filter(x=>x.status==='FAILED')).toEqual([])
 })
}
