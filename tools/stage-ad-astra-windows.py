"""Stage only the four applicable, focused-tested Compatibility jars."""
from pathlib import Path
import hashlib,json,shutil,zipfile

ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'output/ad-astra-implemented'
read=lambda p:json.loads(p.read_text(encoding='utf-8'))
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
rel=lambda p:p.relative_to(ROOT).as_posix()
CASES=[('1.20.1','fabric','Fabric','08','1.15.21','6.19'),('1.20.1','forge','Forge','05','1.15.21','6.20'),
       ('1.21.1','fabric','Fabric','06','1.16.26','8.2'),('1.21.1','neoforge','NeoForge','02','1.16.26','8.1')]

def main():
 before=read(OUT/'dist-before.json');targets=[];updates={}
 for mc,loader,label,n,ad,giselle in CASES:
  run=f'{mc}-{loader}-'+('integrated-'if loader=='neoforge'else'siu-')+'ad-astra-native-'+n
  folder=OUT/run;rp=ROOT/'build/remaining-ui-audit/gameplay'/run;journal=read(folder/'journal.json')
  assert len(journal['checks'])==89 and all(c['passed']for c in journal['checks']),run
  ids=[s['id']for s in read(ROOT/f'compatibility/versions/{mc}/common/src/main/resources/salts_inventory_compat/ad-astra/native-windows.json')]
  assert set(ids)<=set(c['id']for c in journal['captures'])
  log=rp/'launch.log'if (rp/'launch.log').exists()else ROOT/'build/ad-astra-implementation/neoforge.log'
  assert not any(phrase in log.read_text(errors='replace')for phrase in ['native template callback failed','Mixin apply for mod salts_inventory_compat failed'])
  name=f'salts_inventory_compat-{mc}-{label}-0.1.3.jar';built=ROOT/f'compatibility/versions/{mc}/{loader}/build/libs'/name
  staged=read(rp/'staged-artifacts.json')
  for a in staged['artifacts']:assert sha(Path(a['file']))==a['sha256'],a['filename']
  tested=next(a for a in staged['artifacts']if a['filename']==name);assert tested['sha256']==sha(built)
  with zipfile.ZipFile(built)as z:
   assert z.testzip()is None
   assert 'com/salts_inventory_update/compat/adastra/AdAstraClient.class'in z.namelist()
   assert 'salts_inventory_compat/ad-astra/native-windows.json'in z.namelist()
   assert not any(n.startswith(('com/siu_reference/','earth/terrarium/adastra/','ad_astra_giselle_addon/','com/salts_inventory_update/api/'))for n in z.namelist())
  captures=[]
  for cap in journal['captures']:
   p=ROOT/cap['path'];assert p.is_file();captures.append(dict(id=cap['id'],path=rel(p),sha256=sha(p)))
  targets.append(dict(minecraft=mc,loader=loader,ad_astra=ad,giselle=giselle,run=run,assertions=len(journal['checks']),screen_types=18,
    native_nonplayer_slot_envelopes=107,jar='dist/compatibility/'+name,sha256=sha(built),journal=rel(folder/'journal.json'),
    staged_artifacts=rel(rp/'staged-artifacts.json'),captures=captures))
  updates[f'{mc}/{loader}/{name}']=built
 expected={'dist/compatibility/'+p.name for p in updates.values()}
 for path,digest in before.items():
  if path not in expected:assert sha(ROOT/path)==digest,('Unrelated release changed',path)
 sums={p.name:sha(p)for p in (ROOT/'dist/compatibility').glob('*.jar')};sums.update({p.name:sha(p)for p in updates.values()})
 checksum=''.join(f'{sums[n]}  {n}\n'for n in sorted(sums)).encode()
 bundle=ROOT/'dist/salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.ad-astra.tmp.zip');checkpath='checksums/compatibility/SHA256SUMS.txt'
 with zipfile.ZipFile(bundle)as old:
  untouched={n:hashlib.sha256(old.read(n)).hexdigest()for n in old.namelist()if n not in updates and n!=checkpath}
  assert set(updates)<=set(old.namelist())
  with zipfile.ZipFile(temporary,'w')as z:
   z.comment=old.comment
   for info in old.infolist():z.writestr(info,updates[info.filename].read_bytes()if info.filename in updates else checksum if info.filename==checkpath else old.read(info))
 with zipfile.ZipFile(temporary)as z:
  assert z.testzip()is None
  assert all(hashlib.sha256(z.read(n)).hexdigest()==digest for n,digest in untouched.items())
  assert all(z.read(n)==p.read_bytes()for n,p in updates.items())
 for p in updates.values():shutil.copy2(p,ROOT/'dist/compatibility'/p.name)
 (ROOT/'dist/compatibility/SHA256SUMS.txt').write_bytes(checksum);temporary.replace(bundle)
 limits='Focused checks of the changed windows only; no complete gameplay suite. Dedicated-server multiplayer, every machine recipe and world automation setup, all resource packs, controller input and optional mod combinations are not exhaustive.'
 report=dict(status='pass',checked_at='2026-11-03',targets=targets,focused_assertions=sum(t['assertions']for t in targets),
   core_and_api_unchanged=True,unrelated_release_jars_unchanged=True,other_bundle_entries_preserved=True,
   bundle_sha256=sha(bundle),bundle_entries_updated=list(updates)+[checkpath],limits=limits,
   approved_design='output/ad-astra-window-design/manifest.json',publisher_artwork='output/ad-astra-implemented/publisher-artwork-comparison.json',
   fixture_note='The excluded observer fills fixture ingredients, then invokes the original NASA update method to invalidate its recipe cache. Crafting, transfer, native actions and SIU control checks use actual input events. Vehicle fixtures spawn above the floor; no production checks are bypassed.')
 (OUT/'verification.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
 rows=['# Ad Astra and Giselle Addon - SIU target coverage','','Checked November 3, 2026 against publisher release metadata.','',
  '| Minecraft | Loader | Ad Astra | Giselle Addon | Inventory screen types | Focused checks |','| --- | --- | --- | --- | ---: | ---: |']
 rows += [f"| {t['minecraft']} | {t['loader']} | {t['ad_astra']} | {t['giselle']} | 14 + 4 | {t['assertions']} |"for t in targets]
 rows+=['','These are every official release target overlapping SIU\'s supported version/loader set. No publisher release exists for the later SIU targets. The 1.21.1 port uses Common Storage Lib; the 1.20.1 port uses Botarium.',
  '', 'Original native frames, machinery, monitors, slot art, charging and machine toolbars are retained. Gear configuration uses an attached matching drawer. Player inventory stays separate. The Planets navigation UI is explicitly kept native.',
  '', 'Machine actions resolve only a live menu session belonging to the requesting player, with original world/block identity, distance and still-valid checks. Vehicle sorting includes only cargo, never refueling or take-only recovery ports.',
  '', limits,'','Sources: [Ad Astra publisher versions](https://modrinth.com/mod/ad-astra/versions), [Giselle Addon publisher versions](https://modrinth.com/mod/ad-astra-giselle-addon/versions).']
 (OUT/'compatibility-table.md').write_text('\n'.join(rows)+'\n',encoding='utf-8')
 print(json.dumps(dict(status='pass',jars=4,assertions=report['focused_assertions'],bundle_sha256=sha(bundle)),indent=2))
if __name__=='__main__':main()
