"""Package the scoped tab fix; preserve all unrelated release artifacts."""
from pathlib import Path
import json,hashlib,shutil,zipfile
from PIL import Image
ROOT=Path(__file__).resolve().parents[2];WORK=ROOT/'build/ae2-tab-join-fix';OUT=ROOT/'output/ae2-tab-join-fix';OUT.mkdir(parents=True,exist_ok=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
core=json.loads((WORK/'core-before.json').read_text());assert all(sha(ROOT/'dist/core'/n)==h for n,h in core.items())
report={'scope':'Focused native tab style, placement, return navigation and upstream-atlas pixel checks; not a full gameplay suite','core_unchanged':True,'core_artifacts':core,'targets':[]}
changed={}
targets=[('1.20.1','fabric'),('1.20.1','forge')]+[(v,'neoforge') for v in ['1.21.1','26.1.2','26.2','26.3']]
for v,loader in targets:
    key=v if loader=='neoforge' else v+'-'+loader
    run=ROOT/(f'build/all-target-rollout/gameplay/{v}-neoforge-ae2-tab-join-01' if loader=='neoforge' else f'build/remaining-ui-audit/gameplay/{v}-{loader}-'+('integrated' if loader=='fabric' else 'siu')+'-tab-join-fix-01')
    staged=json.loads((run/'staged-artifacts.json').read_text())
    checks=list({c['name']:c for c in json.loads((WORK/key/'checks.json').read_text())}.values())
    assert all(c['pass'] for c in checks)
    assert len(checks)==(23 if loader=='neoforge' else 11 if loader=='fabric' else 14)
    if loader=='neoforge':assert json.loads((run/'results.json').read_text())['failures']==0
    source=next(a for a in staged['artifacts'] if a['family']=='compatibility');jar=Path(source['file'])
    assert sha(jar)==source['sha256'],'Tested input changed: '+key
    core_input=next(a for a in staged['artifacts'] if a['family']=='core');assert core_input['sha256']==core[core_input['filename']]
    # Staging is byte-for-byte from the original release inputs used in game.
    destination=ROOT/'dist/compatibility'/jar.name
    backup=WORK/'previous-distribution'/jar.name;backup.parent.mkdir(exist_ok=True)
    if not backup.exists():shutil.copyfile(destination,backup)
    with zipfile.ZipFile(jar) as z:
        assert z.testzip() is None
        assert 'com/salts_inventory_update/compat/ae2/nativeview/NativeTabJoins.class' in z.namelist()
        assert not any('functionaltest/' in n or 'com/siu_reference/' in n for n in z.namelist())
    shutil.copyfile(jar,destination);changed[f'{v}/{loader}/{jar.name}']=destination.read_bytes()
    folder=OUT/key;folder.mkdir(exist_ok=True)
    names=['trash','magnet-filter','terminal-settings']
    if loader!='fabric':names.append('wireless-settings')
    if loader=='neoforge':names.append('priority')
    for name in names:
        meta=WORK/key/(name+'.json');screenshot=run/'screenshots'/(name+'.png')
        with Image.open(screenshot) as image:image.verify()
        shutil.copyfile(screenshot,folder/(name+'-full.png'));shutil.copyfile(meta,folder/meta.name)
    shutil.copyfile(run/'staged-artifacts.json',folder/'staged-artifacts.json')
    pixels=json.loads((WORK/key/'pixel-checks.json').read_text()) if loader=='neoforge' else []
    if pixels:assert len(pixels)==5 and all(c['pass'] for c in pixels)
    report['targets'].append({'minecraft':v,'loader':loader,'jar':jar.name,'sha256':sha(jar),'runtime':str(run.relative_to(ROOT)),'checks':checks,'pixel_checks':pixels,'release_inputs':staged['artifacts']})
    (folder/'checks.json').write_text(json.dumps(checks,indent=2))
# Check both legacy runtime namespaces and every modern shared widget path.
contracts=[]
for p in (ROOT/'compatibility/ae2/versions').glob('**/src/main/resources/ae2-native-patches*/appeng/client/gui/WidgetContainer.json'):
    d=json.loads(p.read_text());m=next(m for m in d['methods'] if m['name']=='drawDesktopAttachments');nodes=m['instructions']
    join=next(i for i,n in enumerate(nodes) if n.get('owner','').endswith('/NativeTabJoins') and n.get('name')=='draw')
    draw=[i for i,n in enumerate(nodes) if n.get('name') in ['render','extractRenderState']]
    assert len(draw)==2 and draw[0]<join<draw[1]
    assert not any(n.get('name')=='fill' for n in nodes)
    contracts.append({'patch':str(p.relative_to(ROOT)),'join_precedes_generic_icons':True,'no_post_icon_mask':True})
assert len(contracts)==8;report['patch_contracts']=contracts
checksums=ROOT/'dist/compatibility/SHA256SUMS.txt'
checksums.write_text(''.join(f'{sha(p)}  {p.name}\n' for p in sorted((ROOT/'dist/compatibility').glob('*.jar'))),encoding='utf-8',newline='\n')
changed['checksums/compatibility/SHA256SUMS.txt']=checksums.read_bytes()
bundle=ROOT/'dist/salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.tabs.tmp')
with zipfile.ZipFile(bundle) as before,zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as after:
    old={e.filename:hashlib.sha256(before.read(e)).hexdigest() for e in before.infolist()};assert set(changed)<=set(old)
    for entry in before.infolist():after.writestr(entry,changed.get(entry.filename,before.read(entry)))
with zipfile.ZipFile(temporary) as z:
    assert z.testzip() is None
    assert all(hashlib.sha256(z.read(n)).hexdigest()==h for n,h in old.items() if n not in changed)
temporary.replace(bundle)
report['unchanged_other_bundle_members']=True;report['bundle_sha256']=sha(bundle)
report['coverage_limits']=['No complete functional/gameplay suite','Generic seam order checked in all eight patch namespaces; native return views exercised in game']
report['pixel_negative_control']=json.loads((WORK/'pixel-verification.json').read_text())['pre_fix_negative_control']
(OUT/'verification.json').write_text(json.dumps(report,indent=2))
# Presentation crop only; keep its complete original beside it.
data=json.loads((OUT/'26.3/trash.json').read_text())['state'];window=next(w for w in data.get('allWindows',data.get('windows',[])) if w.get('definition','').endswith('.AEWindow'));b=window['bounds']
image=Image.open(OUT/'26.3/trash-full.png');scale=image.width/data['guiWidth']
image.crop(tuple(int(v*scale) for v in [b['x']-3,b['y']-8,b['x']+b['width']+3,b['y']+b['height']+3])).save(OUT/'fixed-trash.png')
print('Staged six tested Compatibility JARs; 117 unique focused checks, 20 pixel comparisons, eight patch contracts; Core unchanged.')
