"""Stage the live-tested Tech Reborn target and preserve unrelated release bytes."""
from pathlib import Path
import json,hashlib,shutil,zipfile
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'output/tech-reborn-implementation'
sha=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()
read=lambda p:json.loads(Path(p).read_text())
def main():
    checks=read(OUT/'focused-checks.json');geometry=read(OUT/'geometry-checks.json')
    assert checks['status']=='passed' and all(c['passed']for c in checks['checks'])
    assert geometry['status']=='passed' and geometry['screen_types']==51
    name='salts_inventory_compat-1.20.1-Fabric-0.1.3.jar'
    jar=ROOT/'compatibility/versions/1.20.1/fabric/build/libs'/name
    run=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-tech-reborn-implemented-10'
    tested=next(r for r in read(run/'staged-artifacts.json')['artifacts']if r['family']=='compatibility')
    assert sha(jar)==tested['sha256'],'Built jar differs from tested release bytes'
    baseline={p.replace('\\','/'):d for p,d in read(OUT/'baseline.json').items()}
    mutable={'dist/compatibility/'+name,'dist/compatibility/SHA256SUMS.txt','dist/salts-inventory-all-latest.zip'}
    for p,d in baseline.items():
        if p not in mutable:assert sha(ROOT/p)==d,('Unrelated artifact changed',p)
    with zipfile.ZipFile(jar) as z:
        assert z.testzip()is None
        assert 'com/salts_inventory_update/compat/techreborn/TechRebornClient.class'in z.namelist()
        assert 'salts_inventory_compat.techreborn.mixins.json'in json.loads(z.read('fabric.mod.json'))['mixins']
        assert not any(p.startswith(('techreborn/','reborncore/','com/salts_inventory_update/api/','com/siu_reference/'))for p in z.namelist())
    sums={p.name:sha(p)for p in(ROOT/'dist/compatibility').glob('*.jar')};sums[name]=sha(jar)
    checksum=''.join(f'{sums[n]}  {n}\n'for n in sorted(sums)).encode()
    key='1.20.1/fabric/'+name;checkkey='checksums/compatibility/SHA256SUMS.txt'
    bundle=ROOT/'dist/salts-inventory-all-latest.zip';temp=bundle.with_suffix('.tech-reborn.tmp.zip')
    with zipfile.ZipFile(bundle)as old:
        assert key in old.namelist()and checkkey in old.namelist()
        untouched={i.filename:hashlib.sha256(old.read(i)).hexdigest()for i in old.infolist()if i.filename not in(key,checkkey)}
        with zipfile.ZipFile(temp,'w')as z:
            z.comment=old.comment
            for info in old.infolist():z.writestr(info,jar.read_bytes()if info.filename==key else checksum if info.filename==checkkey else old.read(info))
    with zipfile.ZipFile(temp)as z:
        assert z.testzip()is None and z.read(key)==jar.read_bytes()
        assert all(hashlib.sha256(z.read(p)).hexdigest()==d for p,d in untouched.items())
    shutil.copy2(jar,ROOT/'dist/compatibility'/name);(ROOT/'dist/compatibility/SHA256SUMS.txt').write_bytes(checksum);temp.replace(bundle)
    report=dict(status='passed',minecraft='1.20.1',loader='fabric',tech_reborn='5.8.14',machine_screens=51,complete_cells=geometry['complete_cells'],focused_checks=len(checks['checks']),jar_sha256=sha(jar),bundle_sha256=sha(bundle),core_and_api_unchanged=True,unrelated_jars_unchanged=True,unrelated_bundle_entries_preserved=True,tested_runtime=str(run.relative_to(ROOT)),limits='Focused window/action checks only. No complete gameplay suite, exhaustive recipes, resource-pack or controller testing. Broad repository build blocked by existing module-boundary violations in other integrations.')
    (OUT/'verification.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
if __name__=='__main__':main()
