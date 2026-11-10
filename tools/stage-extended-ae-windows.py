"""Stage the two focused-tested 1.20.1 Compatibility jars; preserve Core/API and other targets."""
from pathlib import Path
import json,hashlib,shutil,zipfile
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'output/extended-ae-implementation'
read=lambda p:json.loads(p.read_text());sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 before=read(OUT/'dist-before.json');updates={};targets=[]
 checks={l:read(OUT/(l+'-functions.json'))for l in ['fabric','forge']}
 assert all(c['passed']for rows in checks.values()for c in rows)
 rims=read(OUT/'replace-slot-rim-check.json');assert len(rims)==2 and all(r['native_atlas_matches']for r in rims)
 captures=[]
 for p in OUT.glob('*-extended-ae-*.json'):
  if not any(n in p.name for n in ['impl-06','impl-09','impl-10','plain-10']):continue
  for r in read(p):
   path=ROOT/r['capture'];assert sha(path)==r['sha256']
   for w in r.get('allWindows',[]):
    if '.extendedae.'in w.get('nativeScreen',''):captures.append(dict(id=r['id'],loader=r['loader'],screen=w['nativeScreen'],path=r['capture'],sha256=r['sha256'],run=Path(r['capture']).parts[-3]))
 expected={r['screen'].split('.')[-1]for r in read(ROOT/'output/extended-ae-window-design/manifest.json')['pages']}
 observed={c['screen'].split('.')[-1]for c in captures};assert expected<=observed and len(expected)==31
 for loader,label,release,count in [('fabric','Fabric','1.20-1.0.2-fabric',25),('forge','Forge','1.20-1.4.18-forge',31)]:
  run=ROOT/f'build/remaining-ui-audit/gameplay/1.20.1-{loader}-siu-extended-ae-impl-10';name=f'salts_inventory_compat-1.20.1-{label}-0.1.3.jar';jar=ROOT/f'compatibility/versions/1.20.1/{loader}/build/libs'/name
  tested=next(a for a in read(run/'staged-artifacts.json')['artifacts']if a['filename']==name);assert tested['sha256']==sha(jar)
  errors=[]
  for line in (run/'launch.log').open(errors='replace'):
   if any(s in line for s in ['[desktop-api]','Window callback failed','Mixin apply for mod salts_inventory_compat failed']):errors.append(line[:250])
  assert not errors,errors[:3]
  with zipfile.ZipFile(jar)as z:
   assert z.testzip()is None
   assert 'salts_inventory_compat.extended-ae.mixins.json'in z.namelist()
   assert 'com/salts_inventory_update/compat/ae2/nativeview/ExtendedAePackets.class'in z.namelist()
   assert not any(n.startswith(('com/siu_reference/','com/github/glodblock/extendedae/','com/glodblock/github/extendedae/','com/salts_inventory_update/api/'))for n in z.namelist())
   for p in (ROOT/'compatibility/ae2/versions/1.20.1/common/src/main/resources/assets/ae2/screens/desktop').glob('*.json'):
    if p.stem in ['pattern_modifier','assembler_matrix','ex_crafting_terminal','ex_pattern_access_terminal']:assert json.loads(z.read('assets/ae2/screens/desktop/'+p.name))==read(p)
  updates['1.20.1/'+loader+'/'+name]=jar
  targets.append(dict(minecraft='1.20.1',loader=loader,extended_ae=release,audited_screen_types=count,focused_checks=len(checks[loader]),jar='dist/compatibility/'+name,sha256=sha(jar),tested_artifacts=str((run/'staged-artifacts.json').relative_to(ROOT))))
 changed={'dist/compatibility/'+p.name for p in updates.values()}
 for path,digest in before.items():
  if path not in changed:assert sha(ROOT/path)==digest,('Unrelated release changed',path)
 sums={p.name:sha(p)for p in (ROOT/'dist/compatibility').glob('*.jar')};sums.update({p.name:sha(p)for p in updates.values()});checksum=''.join(f'{sums[n]}  {n}\n'for n in sorted(sums)).encode()
 bundle=ROOT/'dist/salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.extended-ae.tmp.zip');checkpath='checksums/compatibility/SHA256SUMS.txt'
 with zipfile.ZipFile(bundle)as old:
  untouched={n:hashlib.sha256(old.read(n)).hexdigest()for n in old.namelist()if n not in updates and n!=checkpath};assert set(updates)<=set(old.namelist())
  with zipfile.ZipFile(temporary,'w')as z:
   z.comment=old.comment
   for info in old.infolist():z.writestr(info,updates[info.filename].read_bytes()if info.filename in updates else checksum if info.filename==checkpath else old.read(info))
 with zipfile.ZipFile(temporary)as z:
  assert z.testzip()is None
  assert all(hashlib.sha256(z.read(n)).hexdigest()==digest for n,digest in untouched.items())
  assert all(z.read(n)==p.read_bytes()for n,p in updates.items())
 for p in updates.values():shutil.copy2(p,ROOT/'dist/compatibility'/p.name)
 (ROOT/'dist/compatibility/SHA256SUMS.txt').write_bytes(checksum);temporary.replace(bundle)
 report=dict(status='pass',minecraft='1.20.1',targets=targets,union_screen_types=31,approved_mockup_views=40,capture_count=len(captures),captures=captures,
  focused_assertions=sum(len(v)for v in checks.values())+len(rims),checks=checks,slot_rims=rims,core_and_api_unchanged=True,other_release_jars_unchanged=True,unrelated_bundle_entries_preserved=True,bundle_sha256=sha(bundle),
  evidence_note='All 31 screen types were opened across iteration 06/09/10, including the conditional plain and AE2WTLib wireless variants. Iteration 10 is the exact final jar and reruns affected action-slot, provider, amount and movement paths. Earlier iteration captures document unchanged profiles; each capture retains its original run lineage.',
  limits='Focused affected-menu checks only, not a complete functional/gameplay suite. Native crafting output, Matrix withdrawal, provider transfer, amount navigation, machine pages, text actions, modifier actions, SIU movement/close and native inspectors were checked. Every recipe, multiplayer/dedicated servers, resource packs, controller input and arbitrary optional-mod combinations are not exhaustive. No later Minecraft versions were ported in this task.')
 (OUT/'verification.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:report[k]for k in ['status','union_screen_types','focused_assertions','core_and_api_unchanged']},indent=2))
if __name__=='__main__':main()
