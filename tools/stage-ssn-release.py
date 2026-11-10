"""Stage only the three tested SSN Compatibility artifacts and refresh their checksums."""
from pathlib import Path
import json,hashlib,zipfile,shutil,importlib.util
ROOT=Path(__file__).resolve().parents[1];DIST=ROOT/'dist';OUT=ROOT/'output/ssn-implemented'
TARGETS=[('1.20.1','forge','Forge','1.20.1-forge-siu-ssn-implemented-06',65),('1.21.1','neoforge','NeoForge','1.21.1-neoforge-integrated-ssn-implemented-04',83),('26.1.2','neoforge','NeoForge','26.1.2-neoforge-integrated-ssn-implemented-04',83)]
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
spec=importlib.util.spec_from_file_location('boundary',ROOT/'tools/verify-module-boundaries.py');boundary=importlib.util.module_from_spec(spec);spec.loader.exec_module(boundary)

def main():
    artifacts=[];replacements={};journals=[];images=[];before=ROOT/'build/ssn-implementation/dist-before';before.mkdir(parents=True,exist_ok=True)
    for mc,loader,label,run,minimum in TARGETS:
        folder=ROOT/'build/remaining-ui-audit/gameplay'/run
        journal=json.loads((folder/'ssn-checks/journal.json').read_text());assert len(journal['checks'])>=minimum and all(x['passed']for x in journal['checks']),(run,journal)
        name=f'salts_inventory_compat-{mc}-{label}-0.1.3.jar';jar=ROOT/f'compatibility/versions/{mc}/{loader}/build/libs'/name
        staged=json.loads((folder/'staged-artifacts.json').read_text())['artifacts'];record=next(x for x in staged if x['filename']==name)
        assert record['sha256']==sha(jar),'Build changed after live checks: '+name
        with zipfile.ZipFile(jar)as z:
            assert z.testzip()is None
            classes=[n for n in z.namelist()if n.startswith('com/salts_inventory_update/compat/ssn/')and n.endswith('.class')]
            assert len(classes)>=10
            assert not any(n.startswith(('com/siu_reference/','com/lothrazar/'))for n in z.namelist())
            for n in classes:
                refs=boundary.forbidden_references('\n'.join(boundary.utf8_constants(z.read(n))));assert not refs,(name,n,refs)
            if loader=='forge':
                manifest=z.read('META-INF/MANIFEST.MF').decode().replace('\r\n','\n').replace('\n ','')
                assert all(f'salts_inventory_compat.{n}.mixins.json'in manifest for n in ['ssn','ssn-transport'])
                assert any(n.startswith('META-INF/jarjar/mixinextras-forge-')for n in z.namelist())
            else:
                metadata=z.read('META-INF/neoforge.mods.toml').decode();assert all(f'salts_inventory_compat.{n}.mixins.json'in metadata for n in ['ssn','ssn-transport'])
        artifacts.append(dict(minecraft=mc,loader=label,screen_types=10 if mc=='1.20.1'else 13,jar=str((DIST/'compatibility'/name).relative_to(ROOT)),sha256=sha(jar),live_assertions=len(journal['checks']),run=run))
        journals.append(dict(target=mc+'/'+label,**journal));replacements[f'{mc}/{loader}/{name}']=jar.read_bytes()
        if not(before/name).exists():shutil.copy2(DIST/'compatibility'/name,before/name)
        for p in(folder/'screenshots').glob('ssn-*.png'):
            target=OUT/'screenshots'/mc/loader/p.name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,target);images.append(str(target.relative_to(ROOT)))
    geometry=json.loads((OUT/'geometry-verification.json').read_text());assert geometry['status']=='pass'and geometry['views']==36
    core={str(p.relative_to(DIST)):sha(p)for fam in ['core','controlify']for p in(DIST/fam).glob('*')if p.is_file()}
    changed={Path(a['jar']).name for a in artifacts};other={p.name:sha(p)for p in(DIST/'compatibility').glob('*.jar')if p.name not in changed}
    sums={p.name:sha(p)for p in(DIST/'compatibility').glob('*.jar')};sums.update({Path(a['jar']).name:a['sha256']for a in artifacts})
    checksum=''.join(f'{sums[n]}  {n}\n'for n in sorted(sums)).encode();replacements['checksums/compatibility/SHA256SUMS.txt']=checksum
    bundle=DIST/'salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.ssn-pending')
    with zipfile.ZipFile(bundle)as old:
        names=old.namelist();assert set(replacements)<=set(names)
        unchanged={n:hashlib.sha256(old.read(n)).hexdigest()for n in names if n not in replacements}
        with zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED)as new:
            for info in old.infolist():new.writestr(info,replacements.get(info.filename,old.read(info)))
    with zipfile.ZipFile(temporary)as z:
        assert z.testzip()is None and z.namelist()==names
        assert all(hashlib.sha256(z.read(n)).hexdigest()==v for n,v in unchanged.items())
        assert all(z.read(n)==v for n,v in replacements.items())
    for a in artifacts:
        name=Path(a['jar']).name;source=ROOT/f"compatibility/versions/{a['minecraft']}/{a['loader'].lower()}/build/libs"/name
        shutil.copy2(source,ROOT/a['jar'])
    (DIST/'compatibility/SHA256SUMS.txt').write_bytes(checksum);temporary.replace(bundle)
    assert core=={str(p.relative_to(DIST)):sha(p)for fam in ['core','controlify']for p in(DIST/fam).glob('*')if p.is_file()}
    assert other=={p.name:sha(p)for p in(DIST/'compatibility').glob('*.jar')if p.name not in changed}
    report=dict(mod='Simple Storage Network',release='1.13.2',targets=artifacts,live_assertions=sum(a['live_assertions']for a in artifacts),geometry=geometry,core_and_public_api_unchanged=True,other_bundle_entries_unchanged=True,bundle_sha256=sha(bundle),images=images,
        scope='Affected SSN menus only; original native handlers, actual integrated clients, GUI scales 1/2. Not the complete gameplay suite.',
        limits='Dedicated multiplayer, optional JEI/Curios, alternative resource packs and exhaustive automation/recipe/remote configurations were not exercised.',
        build='All three target assemblies and source coverage audit passed. Broader module-boundary audit remains blocked by pre-existing Core references in other integrations.')
    (OUT/'verification.json').write_text(json.dumps(report,indent=2));(OUT/'journals.json').write_text(json.dumps(journals,indent=2));print('Staged',len(artifacts),'Compatibility JARs;',report['live_assertions'],'focused live assertions; Core and other bundle entries unchanged')

if __name__=='__main__':main()
