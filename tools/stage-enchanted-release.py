"""Stage only the two Enchanted-enabled 1.20.1 compatibility builds."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/enchanted-implemented'
DIST=ROOT/'dist'
BEFORE=ROOT/'build/enchanted-implementation/dist-before'
RUNS={n:ROOT/f'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-enchanted-final-{n:02d}' for n in [2,3]}


def digest(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()


def main():
    OUT.mkdir(parents=True,exist_ok=True);BEFORE.mkdir(parents=True,exist_ok=True)
    unit=ROOT/'compatibility/versions/1.20.1/fabric/build/test-results/test/TEST-com.salts_inventory_update.compat.enchanted.EnchantedLayoutsTest.xml'
    import xml.etree.ElementTree as ET
    result=ET.parse(unit).getroot();assert result.attrib['tests']=='5' and result.attrib['failures']=='0'
    checks={}
    for n,run in RUNS.items():
        d=json.loads((run/'enchanted-checks/journal.json').read_text())
        assert all(r['passed'] for r in d['checks'])
        checks.update({r['id']:r for r in d['checks']})
        for name in ['journal.json','artwork-verification.json','distillery-label-verification.json']:
            p=run/'enchanted-checks'/name
            if p.exists():shutil.copy2(p,OUT/f'run-{n:02d}-{name}')
    assert len(checks)==33
    a=json.loads((RUNS[2]/'enchanted-checks/artwork-verification.json').read_text());assert a['slots']==20 and a['failures']==0
    a3=json.loads((RUNS[3]/'enchanted-checks/artwork-verification.json').read_text());assert a3['slots']==7 and a3['failures']==0
    replacements={};artifacts=[]
    for loader,label in [('fabric','Fabric'),('forge','Forge')]:
        name=f'salts_inventory_compat-1.20.1-{label}-0.1.3.jar'
        source=ROOT/f'compatibility/versions/1.20.1/{loader}/build/libs'/name
        with zipfile.ZipFile(source) as jar:
            assert 'com/salts_inventory_update/compat/enchanted/EnchantedWindows.class' in jar.namelist()
            assert not any(n.startswith(('com/siu_reference/','net/favouriteless/enchanted/')) for n in jar.namelist())
        target=DIST/'compatibility'/name
        if not (BEFORE/name).exists():shutil.copy2(target,BEFORE/name)
        replacements[f'1.20.1/{loader}/{name}']=source
        artifacts.append(dict(minecraft='1.20.1',loader=loader,file=str(target.relative_to(ROOT)),sha256=digest(source)))
    core_before={p.name:digest(p) for p in (DIST/'core').iterdir() if p.is_file()}
    control_before={p.name:digest(p) for p in (DIST/'controlify').iterdir() if p.is_file()}
    sums={p.name:digest(p) for p in (DIST/'compatibility').glob('*.jar')}
    sums.update({p.name:digest(p) for p in replacements.values()})
    checksum=''.join(f'{sums[name]}  {name}\n' for name in sorted(sums)).encode()
    bundle=DIST/'salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.enchanted-tmp')
    checksum_entry='checksums/compatibility/SHA256SUMS.txt'
    with zipfile.ZipFile(bundle) as old:
        names=old.namelist();assert set(replacements).issubset(names)
        untouched={n:hashlib.sha256(old.read(n)).hexdigest() for n in names if n not in replacements and n!=checksum_entry}
        with zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED) as new:
            for info in old.infolist():
                data=replacements[info.filename].read_bytes() if info.filename in replacements else checksum if info.filename==checksum_entry else old.read(info)
                new.writestr(info,data)
    with zipfile.ZipFile(temporary) as archive:
        assert archive.testzip() is None and archive.namelist()==names
        assert all(hashlib.sha256(archive.read(n)).hexdigest()==s for n,s in untouched.items())
        for n,p in replacements.items():assert hashlib.sha256(archive.read(n)).hexdigest()==digest(p)
    for p in replacements.values():shutil.copy2(p,DIST/'compatibility'/p.name)
    (DIST/'compatibility/SHA256SUMS.txt').write_bytes(checksum);temporary.replace(bundle)
    assert core_before=={p.name:digest(p) for p in (DIST/'core').iterdir() if p.is_file()}
    assert control_before=={p.name:digest(p) for p in (DIST/'controlify').iterdir() if p.is_file()}
    # Every delivered PNG remains an unmodified game screenshot. The one
    # Distillery image from before its label fix is intentionally superseded.
    selected={
        'oven-processing':(2,'enchanted-oven-processing'),
        'poppet-shelf':(2,'enchanted-shelf-filled'),
        'spinning-wheel':(2,'enchanted-wheel-processing'),
        'altar':(3,'enchanted-altar-final'),
        'distillery-four-outputs':(3,'enchanted-distillery-four-outputs'),
        'all-five-windows':(3,'enchanted-five-windows-fullscreen'),
    }
    images=[]
    for name,(n,capture) in selected.items():
        source=RUNS[n]/'screenshots'/(capture+'.png');target=OUT/(name+'.png');shutil.copy2(source,target)
        assert digest(source)==digest(target)
        images.append(dict(file=str(target.relative_to(ROOT)),source=str(source.relative_to(ROOT)),sha256=digest(target),size=list(Image.open(target).size)))
    staged=json.loads((RUNS[3]/'staged-artifacts.json').read_text())
    tested=next(r for r in staged['artifacts'] if r['family']=='compatibility')
    assert tested['sha256']==artifacts[0]['sha256']
    manifest=dict(scope='Enchanted 3.1.14 / Minecraft 1.20.1 Fabric and Forge',
        live_runtime='Fabric, integrated server, GUI scale 2',live_assertions=len(checks),layout_tests=5,
        unique_slot_artwork_checks=20,slot_pixels_checked=6480,final_distillery_slot_recheck=7,
        final_distillery_label_pixels=261,core_and_api_unchanged=True,controlify_unchanged=True,
        unrelated_bundle_entries_preserved=True,bundle_sha256=digest(bundle),artifacts=artifacts,images=images,
        final_rechecks='The lower-left Distillery label cleanup was followed by 13 live Altar/Distillery assertions and a seven-slot pixel recheck. All five windows were captured together on the final build.',
        limits='Forge compiled/reobfuscated and native constructor/owner symbols checked; Forge gameplay, dedicated multiplayer, other GUI scales and full gameplay suite not run.')
    (OUT/'verification.json').write_text(json.dumps(manifest,indent=2))
    print(json.dumps({k:v for k,v in manifest.items() if k not in ['images']},indent=2))


if __name__=='__main__':main()
