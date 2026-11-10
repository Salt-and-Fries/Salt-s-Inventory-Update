"""Certify inventoried screenshot coverage, separately from functional testing."""
from pathlib import Path
import collections,csv,json
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'output/all-target-rollout'
plan=json.loads((OUT/'ui-evidence-plan.json').read_text());matrix=json.loads((OUT/'availability-and-registration.json').read_text())
groups=collections.defaultdict(list)
for row in plan:groups[(row['minecraft'],row['loader'],row['slug'])].append(row)
pending=[]
for row in matrix:
 candidates=groups[(row['minecraft'],row['loader'],row['slug'])]
 scoped=[c for c in candidates if not c.get('ui_scope','').startswith('appendix:')]
 missing=[c['screen'] for c in scoped if not c.get('captured_states')]
 row['inventoried_inventory_screen_classes']=len(scoped)
 row['pictured_inventory_screen_classes']=len(scoped)-len(missing)
 row['appendix_screen_classes']=len(candidates)-len(scoped)
 row['functional_test_scope']='Focused checks recorded separately; complete functional/gameplay suite not run.'
 if row.get('ui_scope','').startswith('appendix:'):
  row['inventory_ui_coverage']='appendix';row['runtime_status']='accounted for outside inventory-window scope'
 elif missing or not row.get('paired_states_captured'):
  row['inventory_ui_coverage']='pending'
  pending.append({'target':row['minecraft']+'/'+row['loader'],'slug':row['slug'],'missing_classes':missing,'paired_states':row.get('paired_states_captured',0)})
 else:
  row['inventory_ui_coverage']='complete';row['runtime_status']='inventory UI comparisons complete; focused checks only'
report={'scope':'Every inventoried in-scope screen plus separately recorded native UI variants, against exact publisher binaries. This is screenshot coverage, not a full functional-suite certificate.','project_targets':len(matrix),'complete':sum(r['inventory_ui_coverage']=='complete' for r in matrix),'appendix':sum(r['inventory_ui_coverage']=='appendix' for r in matrix),'pending':pending,'inventoried_screen_classes':sum(len(v) for v in groups.values()),'pictured_in_scope_classes':sum(r['pictured_inventory_screen_classes'] for r in matrix)}
(OUT/'final-ui-coverage.json').write_text(json.dumps(report,indent=2))
if pending:
 print(json.dumps(report,indent=2));raise SystemExit('Final UI coverage refused: real comparison evidence is still missing.')
(OUT/'availability-and-registration.json').write_text(json.dumps(matrix,indent=2))
with (OUT/'availability-and-registration.csv').open('w',newline='',encoding='utf-8-sig') as stream:
 writer=csv.DictWriter(stream,fieldnames=list(dict.fromkeys(k for r in matrix for k in r)));writer.writeheader();writer.writerows(matrix)
print(json.dumps(report,indent=2))
