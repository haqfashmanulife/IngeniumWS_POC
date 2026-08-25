const MASK_STYLE_ID = '__ingenium_privacy_blur_style__';
const MASK_CLASS = '__ingenium_sensitive_blur__';

async function installMaskInFrame(frame) {
  await frame.evaluate(({ styleId, maskClass }) => {
    document.getElementById(styleId)?.remove();
    const style = document.createElement('style');
    style.id = styleId;
    style.textContent = `.${maskClass}{filter:blur(7px)!important;-webkit-filter:blur(7px)!important;user-select:none!important;opacity:.82!important}input.${maskClass},textarea.${maskClass},select.${maskClass}{color:transparent!important;text-shadow:0 0 9px rgba(30,30,30,.95)!important;caret-color:transparent!important;background-color:rgba(210,210,210,.75)!important}`;
    (document.head || document.documentElement).appendChild(style);
    const isControl = (e) => e.closest('a,button,[role="button"],nav,.menu,.navigation') !== null;
    const mark = (e) => { if (e && !isControl(e)) e.classList.add(maskClass); };
    document.querySelectorAll('input:not([type="button"]):not([type="submit"]):not([type="reset"]):not([type="image"]):not([type="checkbox"]):not([type="radio"]),textarea,select').forEach(mark);
    document.querySelectorAll('.fieldValue,.field-value,.dataValue,.data-value,[data-value],[class*="value" i]').forEach(mark);
    const valuePattern = /(?:\d{2,}|@|¥|￥|\$|\b(?:JPY|USD|POL|CLM|AGT|CLI)\b)/i;
    document.querySelectorAll('td,th,span,div,p,label,font,b,strong').forEach((e) => {
      if (e.children.length !== 0 || isControl(e)) return;
      const text = (e.textContent || '').trim();
      if (text && valuePattern.test(text)) mark(e);
    });
    const sensitive = /owner|client|insured|beneficiary|agent|policy|claim|address|name|birth|gender|phone|email|premium|amount|currency|account|bank|status|date|location|country|product|coverage|loan|remittance|company|user id|batch number|processing date|application status/i;
    document.querySelectorAll('tr').forEach((row) => {
      const cells = [...row.querySelectorAll(':scope > th,:scope > td')];
      for (let i = 0; i < cells.length; i++) {
        if (!sensitive.test((cells[i].textContent || '').trim())) continue;
        if (cells[i + 1] && !cells[i + 1].querySelector('a,button,input[type="button"],input[type="submit"],input[type="image"]')) mark(cells[i + 1]);
      }
    });
  }, { styleId: MASK_STYLE_ID, maskClass: MASK_CLASS });
}

async function removeMaskFromFrame(frame) {
  await frame.evaluate(({ styleId, maskClass }) => {
    document.getElementById(styleId)?.remove();
    document.querySelectorAll(`.${maskClass}`).forEach((e) => e.classList.remove(maskClass));
  }, { styleId: MASK_STYLE_ID, maskClass: MASK_CLASS });
}

export async function captureMaskedScreenshot(page, options) {
  for (const frame of page.frames()) { try { await installMaskInFrame(frame); } catch {} }
  await page.waitForTimeout(300);
  try { await page.screenshot(options); }
  finally { for (const frame of page.frames()) { try { await removeMaskFromFrame(frame); } catch {} } }
}
export function maskedArtifactValue() { return 'MASKED'; }
export function maskSummaryInput(value) { return value == null || (Array.isArray(value) && !value.length) ? '' : 'MASKED'; }
