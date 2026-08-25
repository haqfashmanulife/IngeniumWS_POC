// Privacy-first screenshot masking for Ingenium.
// This changes screenshot rendering only; normal page processing is restored afterward.

const MASK_STYLE_ID = '__ingenium_privacy_mask_style__';
const MASK_CLASS = '__ingenium_sensitive_value__';

async function installMaskInFrame(frame) {
  await frame.evaluate(({ styleId, maskClass }) => {
    document.getElementById(styleId)?.remove();

    const style = document.createElement('style');
    style.id = styleId;
    style.textContent = `
      .${maskClass},
      .${maskClass} *,
      input:not([type="button"]):not([type="submit"]):not([type="reset"]):not([type="image"]),
      textarea,
      select,
      option {
        color: transparent !important;
        text-shadow: none !important;
        caret-color: transparent !important;
        background-image: none !important;
      }
      .${maskClass} {
        position: relative !important;
        filter: none !important;
      }
      .${maskClass}::after,
      input:not([type="button"]):not([type="submit"]):not([type="reset"]):not([type="image"])::after,
      textarea::after,
      select::after {
        content: "████████" !important;
        color: #111 !important;
        background: #111 !important;
        border-radius: 2px !important;
        position: absolute !important;
        inset: 1px !important;
        z-index: 2147483647 !important;
      }
    `;
    (document.head || document.documentElement).appendChild(style);

    const isInteractiveLabel = (element) =>
      element.closest('a,button,[role="button"],nav,.menu,.navigation') !== null;

    const mark = (element) => {
      if (!element || isInteractiveLabel(element)) return;
      element.classList.add(maskClass);
    };

    // All entered identifiers and selected values.
    document.querySelectorAll(
      'input:not([type="button"]):not([type="submit"]):not([type="reset"]):not([type="image"]), textarea, select'
    ).forEach(mark);

    // Legacy Ingenium result pages are table/Form based. Mask data cells while preserving links and buttons.
    document.querySelectorAll('td, th[scope="row"], .fieldValue, .field-value, .value, .data, [data-value]').forEach((element) => {
      if (element.querySelector('a,button,input[type="button"],input[type="submit"],input[type="image"]')) return;
      mark(element);
    });

    // Mask leaf text nodes that look like IDs, dates, money, e-mail, phone, codes, or returned values.
    const valuePattern = /(?:\d|@|¥|￥|\$|\b(?:JPY|USD|POL|CLM|AGT|CLI)\b)/i;
    document.querySelectorAll('span,div,p,label,font,b,strong').forEach((element) => {
      if (element.children.length !== 0 || isInteractiveLabel(element)) return;
      const text = (element.textContent || '').trim();
      if (text && valuePattern.test(text)) mark(element);
    });

    // Mask common sensitive fields even when returned values contain no digits.
    const sensitiveLabels = /owner|client|insured|beneficiary|agent|policy|claim|address|name|birth|gender|phone|email|premium|amount|currency|account|bank|status|date|location|country|product|coverage|loan|remittance/i;
    document.querySelectorAll('tr').forEach((row) => {
      const cells = [...row.querySelectorAll(':scope > th, :scope > td')];
      for (let i = 0; i < cells.length; i++) {
        const labelText = (cells[i].textContent || '').trim();
        if (!sensitiveLabels.test(labelText)) continue;
        // Mask following value cell and alternating value cells in the same row.
        if (cells[i + 1]) mark(cells[i + 1]);
        for (let j = i + 1; j < cells.length; j += 2) mark(cells[j]);
      }
    });
  }, { styleId: MASK_STYLE_ID, maskClass: MASK_CLASS });
}

async function removeMaskFromFrame(frame) {
  await frame.evaluate(({ styleId, maskClass }) => {
    document.getElementById(styleId)?.remove();
    document.querySelectorAll(`.${maskClass}`).forEach((element) => element.classList.remove(maskClass));
  }, { styleId: MASK_STYLE_ID, maskClass: MASK_CLASS });
}

export async function captureMaskedScreenshot(page, options) {
  for (const frame of page.frames()) {
    try { await installMaskInFrame(frame); } catch {}
  }

  // Allow masking styles to render before capture.
  await page.waitForTimeout(250);

  try {
    await page.screenshot(options);
  } finally {
    for (const frame of page.frames()) {
      try { await removeMaskFromFrame(frame); } catch {}
    }
  }
}

export function maskedArtifactValue() {
  return 'MASKED';
}

export function maskSummaryInput(value) {
  if (value === undefined || value === null) return '';
  if (Array.isArray(value) && value.length === 0) return '';
  return 'MASKED';
}
