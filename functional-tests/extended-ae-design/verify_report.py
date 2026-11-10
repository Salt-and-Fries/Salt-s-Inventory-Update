"""Focused design-evidence checks, not a functional gameplay suite."""
from pathlib import Path
import json,hashlib
import pdfplumber
from PIL import Image
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'output/extended-ae-window-design'
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
m=json.loads((OUT/'manifest.json').read_text());v=json.loads((OUT/'verification.json').read_text())
types={p['screen'].split('.')[-1]for p in m['pages']};native={p['native']['screen'].split('.')[-1]for p in m['read_only']}
coverage={};lineages={}
mode_icons_checked=0
for loader in ['fabric','forge']:
 rows=json.loads((OUT/(loader+'-all-screen-types.json')).read_text())
 expected={r['screen'].split('/')[-1]for r in rows if (r['screen'].split('/')[-1].startswith('Gui')and '$'not in r['screen']and '/widget/'not in r['screen'])or r['screen'].endswith('/SetAmount')};expected.discard('GuiPattern')
 assert not expected-types-native,(loader,expected-types-native)
 coverage[loader]={'all_concrete_screens':len(expected),'inventory_configuration_types':len(expected-native),'read_only_inspectors':len(expected&native),'missing':[]}
for p in m['pages']:
 file=ROOT/p['native_capture'];assert sha(file)==p['native_sha256']
 staging=file.parent.parent/'staged-artifacts.json';s=json.loads(staging.read_text());assert s['siu_loaded']is False
 assert not any('salts_inventory' in a.get('filename','').lower()for a in s['artifacts'])
 lineages[str(staging.relative_to(ROOT))]=sha(staging)
 assert p['native_work_preserved_at_capture_resolution']
 if p.get('kind')=='crafting':
  rows=json.loads((OUT/'forge-extra-captures.json').read_text());r=next(r for r in rows if r['id']==p['id']);b=r['native']['bounds'];original=Image.open(file).convert('RGBA');mock=Image.open(ROOT/p['mockup']).convert('RGBA');ox,oy=p['root_origin'];dy=p['native_work_offset'][1]
  for w in r['native']['widgets']:
   if w['message']not in ['Crafting','Stonecutter','Smithing','Anvil']:continue
   sx=w['x']+2;sy=w['y']+2;dx=ox+w['x']-b['x']+2;desty=oy+w['y']-b['y']+2+dy
   a=original.crop((sx*2,sy*2,(sx+16)*2,(sy+16)*2));z=mock.crop((dx*2,desty*2,(dx+16)*2,(desty+16)*2));assert a.tobytes()==z.tobytes(),(p['id'],w['message'],'native mode icon changed');mode_icons_checked+=1
for p in m['read_only']:assert sha(ROOT/p['capture'])==p['sha256']
pdf=ROOT/'output/pdf/siu-extended-ae-ui-window-mockups.pdf';issues=[]
with pdfplumber.open(pdf)as f:
 assert len(f.pages)==49
 for i,p in enumerate(f.pages,1):
  for ch in p.chars:
   if ch['x0']<24 or ch['x1']>p.width-24 or ch['top']<16 or ch['bottom']>p.height-16:issues.append([i,ch['text']])
 assert not issues,issues
 for p in m['pages']:
  assert any(p['title']in page.extract_text()for page in f.pages)
rasters=sorted((OUT/'review/pdf').glob('page-*.png'));assert len(rasters)==49
v.update(screen_class_coverage=coverage,native_mode_icons_identical=mode_icons_checked,capture_lineages=lineages,pdf_text_bounds_pass=True,pdf_sha256=sha(pdf),rendered_pages={p.name:sha(p)for p in rasters},visual_pdf_review='All 49 pages inspected; updated pages re-rendered and re-inspected. Native references, all 40 mockups, complete auxiliary rims, badges, mode tabs and text layout reviewed.',focused_checks_only=True,gameplay_suite_run=False,coverage_limits=['Design only; no production implementation or interaction parity claim.','Idle, sample-item and selected mode states; not every recipe, upgrade combination or amount.','Read-only inspectors opened by original mod packet; raw GLFW shortcut was not exercised.','Select Amount reference is the previously captured original Fabric 1.0.2 UI.'])
(OUT/'verification.json').write_text(json.dumps(v,indent=2));print(json.dumps({'pdf_pages':49,'mockups':len(m['pages']),'inventory_types':len(types),'coverage':coverage,'pdf_sha256':v['pdf_sha256']},indent=2))
