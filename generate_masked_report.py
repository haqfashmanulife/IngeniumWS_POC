#!/usr/bin/env python3
from pathlib import Path
import argparse, base64, html, json, re
N={1:'Policy Inquiry - All Details',2:'Policy Inquiry - Inquiry Coverage Values',3:'Policy Inquiry - Inquiry Coverage Details',4:'Policy Inquiry - Inquiry Call Centre Information',5:'Agent - Agent Inquiry',6:'Client - Address List',7:'Client - Client Inquiry',8:'Client - Previous Name List',9:'Client Service - Client Inquiry General',10:'Client Service - Client Owner Summary',11:'Medical Claim Inquiry - Master Claim Inquiry',12:'Death Claims Inquiry - Death Master Claim Inquiry',13:'Disbursements - Firm Banking Entries',14:'Billing - Billing Activity Inquiry List by Policy',15:'Complex Policy Change - Movement Inquiry',16:'Policy History - APL History List',17:'Policy History - Change History List',18:'Policy History - Loan Detail List',19:'Policy Inquiry - Inquiry Coverage Premiums',20:'Policy Inquiry - Inquiry Loan APL APS Manual PS Judgment'}
def selected(raw):
 o=set()
 for t in raw.split(','):
  t=t.strip()
  if not t: continue
  if '-' in t:
   a,b=map(int,t.split('-',1));o.update(range(a,b+1))
  else:o.add(int(t))
 return sorted(o)
def e(v):return html.escape(str(v or ''))
def main():
 p=argparse.ArgumentParser();p.add_argument('--dest',required=True);p.add_argument('--region',required=True);p.add_argument('--url',required=True);p.add_argument('--screens',required=True);p.add_argument('--rc',required=True);a=p.parse_args()
 d=Path(a.dest);sel=selected(a.screens);summary=d/'screen-summary.json';data=json.loads(summary.read_text()) if summary.exists() else {};items=data.get('screens') or data.get('results') or []
 by={int(x['screenNo']):x for x in items if x.get('screenNo') is not None};passed=sum(x.get('status')=='PASSED' for x in by.values());nr=sum(x.get('status')=='NO_RECORDS' for x in by.values());failed=sum(x.get('status')=='FAILED' for x in by.values())
 for x in by.values(): x['input']='MASKED' if x.get('input') else ''
 summary.write_text(json.dumps({'counts':{'available':20,'selected':len(sel),'passed':passed,'noRecords':nr,'failed':failed,'skipped':20-len(sel)},'screens':list(by.values())},indent=2))
 imgs=sorted(d.glob('*.png'));group={i:[] for i in range(1,21)};diag=[]
 for x in imgs:
  m=re.match(r'screen-(\d+)-',x.name);group[int(m.group(1))].append(x) if m else diag.append(x)
 def badge(s):return f"<span class='b {e(s)}'>{e({'PASSED':'Passed','NO_RECORDS':'No records','FAILED':'Failed','SKIPPED':'Skipped'}.get(s,s))}</span>"
 def pic(x):return f"<h3>{e(x.name)}</h3><img src='data:image/png;base64,{base64.b64encode(x.read_bytes()).decode()}'/>"
 css="body{font-family:Arial;margin:24px;background:#f7f9fb}h1,h2{color:#163a5f}.box{background:white;border:1px solid #d9e2ec;border-radius:10px;padding:18px;margin:20px 0}.cards{display:flex;gap:12px}.c{padding:12px 18px;border-radius:9px;font-weight:bold}.blue{background:#e7f1fb}.green,.PASSED{background:#d1fae5;color:#065f46}.yellow,.NO_RECORDS{background:#fef3c7;color:#92400e}.red,.FAILED{background:#fee2e2;color:#991b1b}.SKIPPED{background:#e5e7eb}table{width:100%;border-collapse:collapse}th,td{border:1px solid #d9e2ec;padding:9px;text-align:left}th{background:#245780;color:white}.b{padding:4px 9px;border-radius:12px;font-weight:bold}img{max-width:100%;border:1px solid #bcccdc}"
 out=[f"<!doctype html><html><head><meta charset='utf-8'><style>{css}</style></head><body><h1>Ingenium {e(a.region.upper())} Screen Result Report</h1><p>APP URL: {e(a.url)} | Playwright exit code: {e(a.rc)}</p>","<div class='box'><h2>Execution Summary</h2><div class='cards'>",f"<div class='c blue'>Selected: {len(sel)} / 20</div><div class='c green'>Passed: {passed}</div><div class='c yellow'>No Records: {nr}</div><div class='c red'>Failed: {failed}</div><div class='c blue'>Skipped: {20-len(sel)}</div></div><table><tr><th>#</th><th>Screen</th><th>Input</th><th>Status</th><th>Details</th><th>Duration</th></tr>"]
 for n in range(1,21):
  x=by.get(n,{'name':N[n],'status':'SKIPPED','detail':'Not selected.','durationSeconds':''});out.append(f"<tr><td>{n:02}</td><td><a href='#s{n}'>{e(x.get('name'))}</a></td><td>{'MASKED' if n in sel else ''}</td><td>{badge(x.get('status'))}</td><td>{e(x.get('detail'))}</td><td>{e(x.get('durationSeconds'))}s</td></tr>")
 out+=['</table></div>']
 if diag:out+=['<div class="box"><h2>Diagnostics</h2>',*[pic(x) for x in diag],'</div>']
 for n in range(1,21):
  x=by.get(n,{'name':N[n],'status':'SKIPPED','detail':'Not selected.'});out+=[f"<div class='box' id='s{n}'><h2>Screen {n}: {e(x.get('name'))} - {badge(x.get('status'))}</h2><p><b>Input:</b> {'MASKED' if n in sel else ''}<br><b>Result:</b> {e(x.get('detail'))}</p>",*([pic(i) for i in group[n]] or ['<p>No screenshot captured.</p>']),'</div>']
 out.append('</body></html>');(d/'screenshot-report.html').write_text('\n'.join(out))
 (d/'screen-summary-email.txt').write_text(f"Ingenium {a.region.upper()} masked report\nSelected: {len(sel)} | Passed: {passed} | No Records: {nr} | Failed: {failed}\nOpen screenshot-report.html.")
 if not (d/'screenshot-report.html').stat().st_size:raise SystemExit('empty report')
main()
