"""Stage the tested Zenith-enabled Fabric JAR; preserve every other bundle entry."""
from pathlib import Path
import hashlib,json,shutil,sys,zipfile
from xml.etree import ElementTree

ROOT=Path(__file__).resolve().parents[1]
DIST=ROOT/'dist';OUT=ROOT/'output/zenith-implemented'


def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    run=Path(sys.argv[1]);final=Path(sys.argv[2])if len(sys.argv)>2 else run
    journal=json.loads((run/'zenith-checks/journal.json').read_text())
    assert len(journal['checks'])>=55 and all(r['passed']for r in journal['checks'])
    rechecks=[]
    if final!=run:
        rechecks=json.loads((final/'zenith-checks/journal.json').read_text())['checks']
        assert len(rechecks)>=4 and all(r['passed']for r in rechecks)
        merged={r['id']:r for r in journal['checks']};merged.update({r['id']:r for r in rechecks})
        journal={**journal,'runs':[str(run),str(final)],'checks':list(merged.values()),'final_rechecks':rechecks}
    artwork=json.loads((run/'zenith-checks/artwork-verification.json').read_text());assert artwork['failures']==0
    unit=ROOT/'compatibility/versions/1.20.1/fabric/build/test-results/test/TEST-com.salts_inventory_update.compat.zenith.ZenithLayoutsTest.xml'
    result=ElementTree.parse(unit).getroot();assert result.attrib['tests']=='6'and result.attrib['failures']=='0'and result.attrib['errors']=='0'
    name='salts_inventory_compat-1.20.1-Fabric-0.1.3.jar'
    jar=ROOT/'compatibility/versions/1.20.1/fabric/build/libs'/name
    tested=next(r for r in json.loads((final/'staged-artifacts.json').read_text())['artifacts']if r['family']=='compatibility')
    assert tested['sha256']==sha(jar),'Build changed after the production checks'
    with zipfile.ZipFile(jar)as z:
        assert z.testzip()is None
        assert 'com/salts_inventory_update/compat/zenith/ZenithClient.class'in z.namelist()
        assert 'salts_inventory_compat.zenith.mixins.json'in json.loads(z.read('fabric.mod.json'))['mixins']
        assert not any(n.startswith(('com/siu_reference/','dev/shadowsoffire/'))for n in z.namelist())
    OUT.mkdir(parents=True,exist_ok=True)
    before=ROOT/'build/zenith-implementation/dist-before';before.mkdir(parents=True,exist_ok=True)
    current=DIST/'compatibility'/name
    if not(before/name).exists():shutil.copy2(current,before/name)
    core_before={str(p.relative_to(DIST)):sha(p)for family in ['core','controlify']for p in(DIST/family).iterdir()if p.is_file()}
    other_before={p.name:sha(p)for p in(DIST/'compatibility').glob('*.jar')if p.name!=name}
    sums={p.name:sha(p)for p in(DIST/'compatibility').glob('*.jar')};sums[name]=sha(jar)
    checksum=''.join(f'{sums[n]}  {n}\n'for n in sorted(sums)).encode()
    bundle=DIST/'salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.zenith-tmp')
    entry='1.20.1/fabric/'+name;check_entry='checksums/compatibility/SHA256SUMS.txt'
    with zipfile.ZipFile(bundle)as old:
        names=old.namelist();assert entry in names and check_entry in names
        untouched={n:hashlib.sha256(old.read(n)).hexdigest()for n in names if n not in [entry,check_entry]}
        with zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED)as new:
            for info in old.infolist():new.writestr(info,jar.read_bytes()if info.filename==entry else checksum if info.filename==check_entry else old.read(info))
    with zipfile.ZipFile(temporary)as z:
        assert z.testzip()is None and z.namelist()==names
        assert all(hashlib.sha256(z.read(n)).hexdigest()==s for n,s in untouched.items())
        assert hashlib.sha256(z.read(entry)).hexdigest()==sha(jar)
    shutil.copy2(jar,current);shutil.copy2(jar,OUT/name)
    (DIST/'compatibility/SHA256SUMS.txt').write_bytes(checksum);temporary.replace(bundle)
    assert core_before=={str(p.relative_to(DIST)):sha(p)for family in ['core','controlify']for p in(DIST/family).iterdir()if p.is_file()}
    assert other_before=={p.name:sha(p)for p in(DIST/'compatibility').glob('*.jar')if p.name!=name}
    (OUT/'journal.json').write_text(json.dumps(journal,indent=2))
    shutil.copy2(run/'zenith-checks/artwork-verification.json',OUT/'artwork-verification.json')
    images=[];screens={p.name:p for p in (run/'screenshots').glob('zenith-*.png')}
    if final!=run:screens.update({p.name:p for p in (final/'screenshots').glob('zenith-*.png')})
    for p in sorted(screens.values(),key=lambda p:p.name):
        target=OUT/p.name;shutil.copy2(p,target);images.append(dict(file=str(target.relative_to(ROOT)).replace('\\','/'),sha256=sha(target)))
    manifest=dict(minecraft='1.20.1',loader='Fabric',mod='Zenith Renewed 2.0.2',container_types=7,workstation_views=9,
        layout_tests=6,focused_live_assertions=len(journal['checks']),static_native_panel_pixels=artwork['pixels'],
        jar=str(current.relative_to(ROOT)).replace('\\','/'),jar_sha256=sha(jar),bundle_sha256=sha(bundle),
        tested_run=str(final),initial_affected_menu_run=str(run),final_rechecks=len(rechecks),core_and_public_api_unchanged=True,other_bundle_entries_unchanged=True,images=images,
        coverage='Nine workstation views; crafting, both libraries, salvaging, gem upgrades, both reforging tables, augmenting, enchanting and native information return, and representative SIU controls.',
        limits='Production Fabric client with integrated server at GUI scale 2. Dedicated multiplayer, other GUI scales and the complete functional/gameplay suite were not run.')
    (OUT/'verification.json').write_text(json.dumps(manifest,indent=2));print(json.dumps({k:v for k,v in manifest.items()if k!='images'},indent=2))


if __name__=='__main__':main()
