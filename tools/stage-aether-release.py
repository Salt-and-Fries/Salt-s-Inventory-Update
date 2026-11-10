"""Stage the two Aether-enabled 1.20.1 Compatibility JARs after focused QA."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile
import xml.etree.ElementTree as ET
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/aether-implemented'
DIST=ROOT/'dist'
BEFORE=ROOT/'build/aether-implementation/dist-before'
RUNS={loader:ROOT/'build/remaining-ui-audit/gameplay'/name for loader,name in {
    'fabric':'1.20.1-fabric-integrated-aether-07',
    'forge':'1.20.1-forge-siu-aether-07',
}.items()}


def digest(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    OUT.mkdir(parents=True,exist_ok=True);BEFORE.mkdir(parents=True,exist_ok=True)
    unit=ROOT/'compatibility/versions/1.20.1/fabric/build/test-results/test/TEST-com.salts_inventory_update.compat.aether.AetherLayoutsTest.xml'
    tests=ET.parse(unit).getroot();assert tests.attrib['tests']=='4' and tests.attrib['failures']=='0' and tests.attrib['errors']=='0'
    shutil.copy2(unit,OUT/'layout-tests.xml')
    counts={};artwork={};upstream={};replacements={};artifacts=[];rechecks={}
    for loader,label,expected in [('fabric','Fabric',44),('forge','Forge',53)]:
        run=RUNS[loader];journal=json.loads((run/'aether-checks/journal.json').read_text())
        # Preserve the entire attempt history. Corrected drag probes raise and
        # unlock the intended parent, then move beyond its edge snap zone.
        latest={c['id']:c for c in journal['checks']}
        assert all(c['passed'] for c in latest.values()),loader
        assert len(latest)==expected,(loader,len(latest),expected)
        rechecks[loader]=dict(attempts=len(journal['checks']),superseded_failures=[c['id']for c in journal['checks']if not c['passed']],reason='Drag probes corrected to raise and unlock the intended parent, avoid covered title bars and edge snapping, and dispatch within the input callback. Final rechecks passed; full attempt history retained in loader journal.')
        counts[loader]=expected
        art=json.loads((run/'aether-checks/artwork.json').read_text());assert art['slots']==17 and art['failures']==0;artwork[loader]=art
        for name in ['journal.json','artwork.json']:shutil.copy2(run/'aether-checks'/name,OUT/f'{loader}-{name}')
        staged=json.loads((run/'staged-artifacts.json').read_text());shutil.copy2(run/'staged-artifacts.json',OUT/f'{loader}-tested-artifacts.json')
        upstream[loader]=[a for a in staged['artifacts']if a['family']=='upstream']
        name=f'salts_inventory_compat-1.20.1-{label}-0.1.3.jar';source=ROOT/f'compatibility/versions/1.20.1/{loader}/build/libs'/name
        assert next(a for a in staged['artifacts']if a['family']=='compatibility')['sha256']==digest(source)
        with zipfile.ZipFile(source)as jar:
            names=jar.namelist();assert all('com/salts_inventory_update/compat/aether/'+c+'.class'in names for c in ['AetherWindows','AetherEquipment','AetherEquipmentMenu','AetherEquipmentClient','AetherEquipmentPanel'])
            assert not any(n.startswith(('com/siu_reference/','com/aetherteam/','io/wispforest/','top/theillusivec4/'))for n in names)
            if loader=='forge':assert not any('/aether/fabric/'in n for n in names)
        target=DIST/'compatibility'/name
        if not (BEFORE/name).exists():shutil.copy2(target,BEFORE/name)
        replacements[f'1.20.1/{loader}/{name}']=source
        artifacts.append(dict(minecraft='1.20.1',loader=loader,file=str(target.relative_to(ROOT)),sha256=digest(source)))
    core_before={p.name:digest(p)for p in (DIST/'core').iterdir()if p.is_file()}
    control_before={p.name:digest(p)for p in (DIST/'controlify').iterdir()if p.is_file()}
    sums={p.name:digest(p)for p in (DIST/'compatibility').glob('*.jar')};sums.update({p.name:digest(p)for p in replacements.values()})
    checksum=''.join(f'{sums[n]}  {n}\n'for n in sorted(sums)).encode()
    bundle=DIST/'salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.aether-tmp');checksum_entry='checksums/compatibility/SHA256SUMS.txt'
    with zipfile.ZipFile(bundle)as old:
        names=old.namelist();assert set(replacements).issubset(names)
        untouched={n:hashlib.sha256(old.read(n)).hexdigest()for n in names if n not in replacements and n!=checksum_entry}
        with zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED)as new:
            for info in old.infolist():
                data=replacements[info.filename].read_bytes()if info.filename in replacements else checksum if info.filename==checksum_entry else old.read(info)
                new.writestr(info,data)
    with zipfile.ZipFile(temporary)as z:
        assert z.testzip()is None and z.namelist()==names
        assert all(hashlib.sha256(z.read(n)).hexdigest()==s for n,s in untouched.items())
        assert all(hashlib.sha256(z.read(n)).hexdigest()==digest(p)for n,p in replacements.items())
    for source in replacements.values():shutil.copy2(source,DIST/'compatibility'/source.name)
    (DIST/'compatibility/SHA256SUMS.txt').write_bytes(checksum);temporary.replace(bundle)
    assert core_before=={p.name:digest(p)for p in (DIST/'core').iterdir()if p.is_file()}
    assert control_before=={p.name:digest(p)for p in (DIST/'controlify').iterdir()if p.is_file()}
    selected={
        'altar':('forge','aether-altar-processing'),
        'freezer':('fabric','aether-freezer-processing'),
        'incubator':('forge','aether-incubator-processing'),
        'book-of-lore':('forge','aether-lore-item'),
        'accessories-fabric':('fabric','aether-equipment-empty'),
        'accessories-forge':('forge','aether-equipment-empty'),
        'separate-attachments':('forge','aether-attachments-arranged'),
        'moa-skins-native':('forge','aether-moa-skins-native'),
    }
    images=[]
    for name,(loader,capture)in selected.items():
        source=RUNS[loader]/'screenshots'/(capture+'.png');target=OUT/(name+'.png');shutil.copy2(source,target)
        assert digest(source)==digest(target)
        images.append(dict(file=str(target.relative_to(ROOT)),source=str(source.relative_to(ROOT)),sha256=digest(target),size=list(Image.open(target).size)))
    manifest=dict(scope='The Aether / Minecraft 1.20.1 Fabric and Forge',
        live_runtime='Production clients, integrated servers, GUI scale 2',live_assertions=counts,total_live_assertions=sum(counts.values()),layout_tests=4,rechecks=rechecks,
        artwork={k:{n:v[n]for n in ['slots','logical_pixels','failures']}for k,v in artwork.items()},equipment_rgb_tolerance=1,
        equipment_parent='Character',quark_backpack_parent='Inventory',core_and_api_unchanged=True,controlify_unchanged=True,
        unrelated_bundle_entries_preserved=True,bundle_sha256=digest(bundle),artifacts=artifacts,images=images,upstream=upstream,
        fabric_runtime_note='Accessories beta48 needs Sodium rendering API when rendering the cape. Final profile includes unmodified Sodium 0.5.13 and Indium 1.0.36. These are test-profile dependencies, not bundled or newly required by SIU.',
        limits='Focused affected-menu checks only. No complete gameplay suite, dedicated multiplayer, other GUI scales, arbitrary equipment datapacks, supporter-only customization or full Moa hatch cycle. Native raw keyboard events were not part of the checks.')
    (OUT/'verification.json').write_text(json.dumps(manifest,indent=2));print(json.dumps({k:v for k,v in manifest.items()if k not in ['images','upstream']},indent=2))


if __name__=='__main__':main()
