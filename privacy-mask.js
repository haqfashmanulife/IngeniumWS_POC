// Screenshot-only privacy masking for Ingenium.
// Sensitive values are blurred for capture and the page is restored immediately afterward.

const MASK_STYLE_ID = '__ingenium_privacy_blur_style__';
const MASK_CLASS = '__ingenium_sensitive_blur__';

async function installMaskInFrame(frame) {
  await frame.evaluate(({ styleId, maskClass }) => {
    document.getElementById(styleId)?.remove();

    const style = document.createElement('style');
    style.id = styleId;
    style.textContent = `
      .${maskClass} {
        filter: blur(7px) !important;
        -webkit-filter: blur(7px) !important;
        user-select: none !important;
        opacity: 0.82 !important;
      }

      input.${maskClass},
      textarea.${maskClass},
      select.${maskClass} {
        color: transparent !important;
        text-shadow: 0 0 9px rgba(30, 30, 30, 0.95) !important;
        caret-color: transparent !important;
        background-color: rgba(210, 210, 210, 0.75) !important;
      }
    `;
    (document.head || document.documentElement).appendChild(style);

    const isControl = (element) =>
      element.closest('a, button, [role="button"], nav, .menu, .navigation') !== null;

    const mark = (element) => {
      if (!element || isControl(element)) return;
      element.classList.add(maskClass);
    };

    // Always blur values entered in application fields.
    document.querySelectorAll(
      'input:not([type="button"]):not([type="submit"]):not([type="reset"]):not([type="image"]):not([type="checkbox"]):not([type="radio"]), textarea, select'
    ).forEach(mark);

    // Common Ingenium value containers. Do not blanket-mask every table cell.
    document.querySelectorAll(
      '.fieldValue, .field-value, .dataValue, .data-value, [data-value], [class*="value" i]'
    ).forEach(mark);

    // Blur leaf values containing identifiers, dates, amounts, e-mail addresses, phone data or codes.
    const valuePattern = /(?:\d{2,}|@|¥|￥|\$|\b(?:JPY|USD|POL|CLM|AGT|CLI)\b)/i;
    document.querySelectorAll('td, th, span, div, p, label, font, b, strong').forEach((element) => {
      if (element.children.length !== 0 || isControl(element)) return;
      const text = (element.textContent || '').trim();
      if (text && valuePattern.test(text)) mark(element);
    });

    // Blur value cells following sensitive labels, including text-only values such as names/status/country.
    const sensitiveLabel = /owner|client|insured|beneficiary|agent|policy|claim|address|name|birth|gender|phone|email|premium|amount|currency|account|bank|status|date|location|country|product|coverage|loan|remittance|company|user id|batch number|processing date|application status/i;

    document.querySelectorAll('tr').forEach((row) => {
      const cells = [...row.querySelectorAll(':scope > th, :scope > td')];
      for (let index = 0; index < cells.length; index++) {
        const label = (cells[index].textContent || '').trim();
        if (!sensitiveLabel.test(label)) continue;
        const next = cells[index + 1];
        if (next && !next.querySelector('a, button, input[type="button"], input[type="submit"], input[type="image"]')) {
          mark(next);
        }
      }
    });

    // Support label/value layouts that are not table rows.
    document.querySelectorAll('label, span, div, font, b, strong').forEach((labelElement) => {
      const label = (labelElement.textContent || '').trim();
      if (!label || !sensitiveLabel.test(label) || isControl(labelElement)) return;

      const targetId = labelElement.getAttribute('for');
      if (targetId) mark(document.getElementById(targetId));

      let sibling = labelElement.nextElementSibling;
      while (sibling && ['BR'].includes(sibling.tagName)) sibling = sibling.nextElementSibling;
      if (sibling && !isControl(sibling)) mark(sibling);
    });
  }, { styleId: MASK_STYLE_ID, maskClass: MASK_CLASS });
}

async function removeMaskFromFrame(frame) {
  await frame.evaluate(({ styleId, maskClass }) => {
    document.getElementById(styleId)?.remove();
    document.querySelectorAll(`.${maskClass}`).forEach((element) => {
      element.classList.remove(maskClass);
    });
  }, { styleId: MASK_STYLE_ID, maskClass: MASK_CLASS });
}

export async function captureMaskedScreenshot(page, options) {
  for (const frame of page.frames()) {
    try {
      await installMaskInFrame(frame);
    } catch {}
  }

  await page.waitForTimeout(300);

  try {
    await page.screenshot(options);
  } finally {
    for (const frame of page.frames()) {
      try {
        await removeMaskFromFrame(frame);
      } catch {}
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
