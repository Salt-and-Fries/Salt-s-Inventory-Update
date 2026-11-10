"""Stage focused-tested modern ports, preserving every other release artifact."""
from pathlib import Path
import json,hashlib,shutil,zipfile,sys

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/extended-ae-all-targets'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
read=lambda p:json.loads(p.read_text())

def main():
    run=sys.argv[1]
    updates={};targets=[];all_checks={};captures=[]
    before={Path(k).as_posix():v for k,v in read(OUT/'dist-before.json').items()}
    publishers=read(OUT/'targets.json')
    for mc in ['1.21.1','26.1.2']:
        release=next(r for r in publishers if r['minecraft']==mc)
        folder=OUT/mc/run;checks=read(folder/'checks.json')
        assert checks and all(r['passed']for r in checks),checks
        required={'Native mod filter reaches server and synchronizes','Configuration numeric entry reaches server',
            'Renamer native confirm closes its window','Native clear restores blank pattern',
            'Output-side initial native host selection synchronized','Output-side edit reaches native machine',
            'Native PAT search clear restores rows','Native PAT provider-ID withdrawal',
            'Assembler Matrix native withdrawal reaches cursor','Native amount save returns to bus',
            'Wireless PAT connects to powered native access point','Existing AE2 Drive still uses native AEWindow'}
        assert required<={r['name']for r in checks},('Missing required focused checks',mc,required-{r['name']for r in checks})
        all_checks[mc]=checks
        evidence=ROOT/f'build/all-target-rollout/gameplay/{mc}-neoforge-{run}'
        name=f'salts_inventory_compat-{mc}-NeoForge-0.1.3.jar'
        jar=ROOT/f'compatibility/versions/{mc}/neoforge/build/libs'/name
        tested=next(a for a in read(evidence/'staged-artifacts.json')['artifacts']if a['filename']==name)
        assert sha(jar)==tested['sha256'],('Jar differs from live-tested artifact',mc)
        errors=[]
        for line in (ROOT/f'build/extended-ae-all-targets/{mc}-{run}.log').open(errors='replace'):
            if any(s in line for s in ['[desktop-api]','Window callback failed','Mixin apply for mod salts_inventory_compat failed','server mutation failed','server custom-mutation failed']):errors.append(line[:220])
        assert not errors,errors[:4]
        with zipfile.ZipFile(jar)as z:
            assert z.testzip()is None
            config=read(ROOT/f'compatibility/ae2/versions/{mc}/neoforge/src/main/resources/salts_inventory_compat.extended-ae.mixins.json')
            assert json.loads(z.read('salts_inventory_compat.extended-ae.mixins.json'))==config
            for cls in ['ExtendedAeLayout','ExtendedAePackets']:
                entry=f'com/salts_inventory_update/compat/ae2/nativeview/{cls}.class'
                assert entry in z.namelist()
                assert int.from_bytes(z.read(entry)[6:8],'big')<=(65 if mc=='1.21.1'else 69),('Wrong Java bytecode target',mc,cls)
            assert not any(n.startswith(('appeng/','com/glodblock/github/extendedae/','com/glodblock/github/glodium/','com/salts_inventory_update/api/','com/salts_inventory_update/functionaltest/'))for n in z.namelist())
            profiles=ROOT/f'compatibility/ae2/versions/{mc}/neoforge/src/main/resources/assets/ae2/screens/desktop'
            with zipfile.ZipFile(ROOT/release['jar'])as publisher:
                addon_profiles=[file for file in profiles.glob('*.json')if 'assets/ae2/screens/'+file.name in publisher.namelist()]
                assert len(addon_profiles)==release['screen_documents']-1
                for file in addon_profiles:
                    assert json.loads(z.read('assets/ae2/screens/desktop/'+file.name))==read(file)
            # Modern native artwork remains supplied by the installed AE2/EAE
            # releases. No vintage or replacement raster assets are added here.
            with zipfile.ZipFile(ROOT/'dist/compatibility'/name)as old:
                png={n:hashlib.sha256(z.read(n)).hexdigest()for n in z.namelist()if n.endswith('.png')}
                assert png=={n:hashlib.sha256(old.read(n)).hexdigest()for n in old.namelist()if n.endswith('.png')}
        updates[f'{mc}/neoforge/{name}']=jar
        targets.append(dict(minecraft=mc,loader='neoforge',extended_ae=release['release'],ui='modern',jar='dist/compatibility/'+name,sha256=sha(jar),focused_checks=len(checks),desktop_profiles=len(addon_profiles),publisher_sha512=release['sha512'],source_commit=release['source_commit'],tested_artifacts=(evidence/'staged-artifacts.json').relative_to(ROOT).as_posix()))
        for row in read(folder/'captures.json'):
            p=Path(row['capture']);captures.append(dict(minecraft=mc,name=row['name'],path=p.relative_to(ROOT).as_posix(),sha256=sha(p),run=run))
        views={w['nativeScreen']for row in read(folder/'captures.json')for w in row['state']['allWindows']if '.extendedae.'in w.get('nativeScreen','')}
        assert len(views)==(32 if mc=='1.21.1'else 31),('Missing native screen coverage',mc,len(views))
        targets[-1]['live_screen_classes']=len(views)

    legacy=read(ROOT/'output/extended-ae-implementation/verification.json')
    for target in legacy['targets']:
        jar=ROOT/target['jar'];assert sha(jar)==target['sha256']
        targets.append(dict(minecraft='1.20.1',loader=target['loader'],extended_ae=target['extended_ae'],ui='vintage',jar=target['jar'],sha256=sha(jar),verification='output/extended-ae-implementation/verification.json',reused_approved_artifact=True))
    changed={'dist/compatibility/'+p.name for p in updates.values()}
    mutable=changed|{'dist/compatibility/SHA256SUMS.txt','dist/salts-inventory-all-latest.zip'}
    for path,digest in before.items():
        if path not in mutable:assert sha(ROOT/path)==digest,('Unrelated artifact changed',path)
    sums={p.name:sha(p)for p in(ROOT/'dist/compatibility').glob('*.jar')}
    sums.update({p.name:sha(p)for p in updates.values()})
    checksum=''.join(f'{sums[n]}  {n}\n'for n in sorted(sums)).encode()
    bundle=ROOT/'dist/salts-inventory-all-latest.zip';temp=bundle.with_suffix('.extended-ae.tmp.zip');checkpath='checksums/compatibility/SHA256SUMS.txt'
    with zipfile.ZipFile(bundle)as old:
        assert set(updates)<=set(old.namelist())
        untouched={n:hashlib.sha256(old.read(n)).hexdigest()for n in old.namelist()if n not in updates and n!=checkpath}
        with zipfile.ZipFile(temp,'w')as z:
            z.comment=old.comment
            for info in old.infolist():z.writestr(info,updates[info.filename].read_bytes()if info.filename in updates else checksum if info.filename==checkpath else old.read(info))
    with zipfile.ZipFile(temp)as z:
        assert z.testzip()is None
        assert all(hashlib.sha256(z.read(n)).hexdigest()==digest for n,digest in untouched.items())
        assert all(z.read(n)==p.read_bytes()for n,p in updates.items())
    for p in updates.values():shutil.copy2(p,ROOT/'dist/compatibility'/p.name)
    (ROOT/'dist/compatibility/SHA256SUMS.txt').write_bytes(checksum);temp.replace(bundle)
    report=dict(status='pass',targets=targets,checks=all_checks,focused_assertions=sum(len(v)for v in all_checks.values()),captures=captures,
        core_and_api_unchanged=True,other_target_jars_unchanged=True,legacy_approved_jars_preserved=True,unrelated_bundle_entries_preserved=True,bundle_sha256=sha(bundle),
        modern_artwork='Desktop documents include each installed release\'s native documents and use native drawing. Raster assets are unchanged from the prior modern jars.',
        limits='Focused affected-menu checks only; not a complete functional/gameplay suite. Every recipe, multiplayer/dedicated server, controller, resource pack and arbitrary optional-mod combination was not exhaustively tested. Modern conditional AppliedE layouts were checked against native documents, not a live AppliedE stack. Earlier layout captures retain their original run lineage.')
    (OUT/'verification.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps({k:report[k]for k in ['status','focused_assertions','core_and_api_unchanged','other_target_jars_unchanged']},indent=2))

if __name__=='__main__':main()
