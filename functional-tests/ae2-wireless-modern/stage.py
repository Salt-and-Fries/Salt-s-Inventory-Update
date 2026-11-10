"""Stage only the four tested extensions and their bundle/checksum entries."""
from pathlib import Path
import json, hashlib, shutil, zipfile, csv
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
WORK=ROOT/'build/ae2-wireless-all-targets'
OUT=ROOT/'output/ae2-wireless-modern'
OUT.mkdir(parents=True,exist_ok=True)
VERSIONS=['1.21.1','26.1.2','26.2','26.3']
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
core=json.loads((WORK/'core-before.json').read_text())
assert all(sha(ROOT/'dist/core'/n)==h for n,h in core.items()),'Core distribution changed'
report={'scope':'Focused affected AE2WTLib menus and native input only; not a full functional/gameplay suite',
        'core_unchanged':True,'core_artifacts':core,'targets':[],
        'coverage_limits':['Optional Curios terminal locators','Universal-terminal multi-type selection','Long-distance Quantum Bridge','Complete autocrafting CPU job','Interactive resizing']}
changed={}
for v in VERSIONS:
    run=ROOT/f'build/all-target-rollout/gameplay/{v}-neoforge-ae2-wireless-final-01'
    checks=json.loads((WORK/v/'checks.json').read_text())
    patterns=json.loads((WORK/v/'pattern-checks.json').read_text())
    assert len(checks)==35 and len(patterns)==8 and all(c['pass'] for c in checks+patterns)
    staged=json.loads((run/'staged-artifacts.json').read_text())
    runtime_result=json.loads((run/'results.json').read_text())
    assert runtime_result['failures']==0, 'Final runtime recorded API failures: '+v
    core_input=next(x for x in staged['artifacts'] if x['family']=='core')
    assert core_input['sha256']==core[core_input['filename']], 'Observer did not load the unchanged shipping Core'
    source=next(x for x in staged['artifacts'] if x['family']=='compatibility')
    jar=Path(source['file']);assert sha(jar)==source['sha256'],'Retest changed input: '+v
    with zipfile.ZipFile(jar) as z:
        assert z.testzip() is None
        names=z.namelist()
        assert not any('functionaltest/' in n or 'com/siu_reference/' in n for n in names)
        assert not any(n.startswith('appeng/') and n.endswith('.class') for n in names)
        assert not any(n.endswith('.png') and ('ae2wtlib' in n or '/wtlib/' in n) for n in names)
        docs=[n for n in names if n.startswith('assets/ae2/screens/desktop/wtlib/') and n.endswith('.json')]
        assert len(docs)==6
    target=ROOT/'dist/compatibility'/jar.name
    backup=WORK/'previous-distribution'/jar.name;backup.parent.mkdir(exist_ok=True)
    if not backup.exists():shutil.copyfile(target,backup)
    shutil.copyfile(jar,target)
    changed[f'{v}/neoforge/{jar.name}']=target.read_bytes()
    dest=OUT/v;dest.mkdir(exist_ok=True)
    for p in (WORK/v).glob('*'):
        if p.is_file() and p.suffix in ['.png','.json'] and p.name not in ['latest-observations.json']:
            shutil.copyfile(p,dest/p.name)
    # Screenshot writing is asynchronous. Copy completed originals after the
    # observer has exited, and validate each PNG before using it as evidence.
    for name in ['wireless-crafting','wireless-settings','magnet-filter','trash',
                 'pattern-encoding','encoding-processing','encoding-smithing',
                 'encoding-stonecutting','encoding-crafting','pattern-access',
                 'encoded-pattern','pattern-access-populated']:
        picture=run/'screenshots'/(name+'.png')
        with Image.open(picture) as im:im.verify()
        shutil.copyfile(picture,dest/picture.name)
    shutil.copyfile(run/'staged-artifacts.json',dest/'staged-artifacts.json')
    report['targets'].append({'minecraft':v,'loader':'neoforge','compatibility':jar.name,'sha256':sha(jar),
        'runtime':str(run.relative_to(ROOT)),'checks':checks+patterns,'publisher_inputs':staged['artifacts'],
        'packaging_checks':{'six_native_inherited_profiles':True,'no_publisher_classes_or_textures':True,'no_qa_classes':True,'tested_bytes_match_distribution':True}})
checksums=ROOT/'dist/compatibility/SHA256SUMS.txt'
checksums.write_text(''.join(f'{sha(p)}  {p.name}\n' for p in sorted((ROOT/'dist/compatibility').glob('*.jar'))),encoding='utf-8',newline='\n')
changed['checksums/compatibility/SHA256SUMS.txt']=checksums.read_bytes()
bundle=ROOT/'dist/salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.ae2wt.tmp')
with zipfile.ZipFile(bundle) as before,zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as after:
    old={i.filename:hashlib.sha256(before.read(i)).hexdigest() for i in before.infolist()}
    assert set(changed)<=set(old),'Unknown bundle member'
    for entry in before.infolist():after.writestr(entry,changed.get(entry.filename,before.read(entry)))
with zipfile.ZipFile(temporary) as z:
    assert z.testzip() is None
    assert all(hashlib.sha256(z.read(n)).hexdigest()==h for n,h in old.items() if n not in changed)
    assert all(z.read(n)==b for n,b in changed.items())
temporary.replace(bundle)
report['bundle_sha256']=sha(bundle);report['unrelated_bundle_members_unchanged']=True
availability=json.loads((WORK/'availability.json').read_text());(OUT/'availability.json').write_text(json.dumps(availability,indent=2))
with (OUT/'availability.csv').open('w',newline='') as stream:
    writer=csv.writer(stream);writer.writerow(['Minecraft','Loader','Official AE2WTLib','Status'])
    for r in availability:writer.writerow([r['minecraft'],r['loader'],r['version'] or '', 'Existing support' if r['minecraft']=='1.20.1' else 'Implemented and focused tested' if r['version'] else 'No official matching release'])
(OUT/'verification.json').write_text(json.dumps(report,indent=2))
print('Staged four extensions, preserved all other bundle members and all twelve Core JARs; 172 focused checks.')
