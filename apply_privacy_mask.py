#!/usr/bin/env python3
from pathlib import Path
import argparse, re

p=argparse.ArgumentParser()
p.add_argument('spec')
a=p.parse_args()
path=Path(a.spec)
s=path.read_text(encoding='utf-8')

import_line="import { captureMaskedScreenshot, maskedArtifactValue, maskSummaryInput } from './privacy-mask.js';"
if import_line not in s:
    m=re.search(r"^import .*?;\s*$",s,re.M)
    if not m: raise SystemExit('No ES module import found in Playwright spec')
    s=s[:m.end()]+"\n"+import_line+s[m.end():]

# Every screenshot, including diagnostics and failures, is privacy masked.
s=s.replace('await page.screenshot({','await captureMaskedScreenshot(page, {')

# Do not expose IDs in screenshot filenames.
s=re.sub(r"screen\.values\.join\((['\"])\-\1\)","maskedArtifactValue()",s)
s=s.replace('${idValue}', '${maskedArtifactValue()}')

# Mask commonly used JSON summary input expressions without altering test inputs.
patterns=[
    r"input:\s*screen\.values\.join\([^)]*\)",
    r"input:\s*values\.join\([^)]*\)",
    r"input:\s*String\([^)]*\)",
]
for pat in patterns:
    s=re.sub(pat,"input: maskSummaryInput(screen?.values)",s)

# Safety marker allows Jenkins to prove the checked-out spec was transformed.
if 'PRIVACY_MASK_APPLIED' not in s:
    s="// PRIVACY_MASK_APPLIED\n"+s
path.write_text(s,encoding='utf-8')
print(f'Privacy masking applied to {path}')
