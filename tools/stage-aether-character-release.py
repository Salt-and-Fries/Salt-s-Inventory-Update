"""Stage only the two verified Character-mode Compatibility builds."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile
import xml.etree.ElementTree as ET
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/aether-character-mode'
DIST=ROOT/'dist'
BEFORE=ROOT/'build/aether-implementation/character-dist-before'
RUNS={loader:ROOT/'build/remaining-ui-audit/gameplay'/name for loader,name in {
    'fabric':'1.20.1-fabric-integrated-aether-character-03',
    'forge':'1.20.1-forge-siu-aether-character-02',
}.items()}
NEGATIVE={
    'fabric':ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-integrated-aether-character-negative-01',
    'forge':ROOT/'build/remaining-ui-audit/gameplay/1.20.1-forge-siu-aether-character-negative-01',
}


def digest(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main():
    OUT.mkdir(parents=True,exist_ok=True);BEFORE.mkdir(parents=True,exist_ok=True)
    unit=ROOT/'compatibility/versions/1.20.1/fabric/build/test-results/test/TEST-com.salts_inventory_update.compat.aether.AetherCharacterLayoutTest.xml'
    tests=ET.parse(unit).getroot();assert all(tests.attrib[k]==v for k,v in {'tests':'2','failures':'0','errors':'0'}.items())
    shutil.copy2(unit,OUT/'layout-tests.xml')
    counts={};negative_counts={};artwork={};upstream={};replacements={};artifacts=[]
    for loader,label,expected in [('fabric','Fabric',34),('forge','Forge',39)]:
        run=RUNS[loader];journal=json.loads((run/'aether-checks/journal.json').read_text());checks=journal['checks']
        assert len(checks)==len({c['id']for c in checks})==expected,(loader,len(checks),expected)
        assert all(c['passed']for c in checks),loader
        counts[loader]=len(checks)
        negative=json.loads((NEGATIVE[loader]/'aether-checks/journal.json').read_text())['checks']
        assert len(negative)==5 and all(c['passed']for c in negative),loader
        negative_counts[loader]=len(negative)
        negative_artifacts=json.loads((NEGATIVE[loader]/'staged-artifacts.json').read_text())
        assert not any(a.get('filename','').startswith('aether-')for a in negative_artifacts['artifacts'])
        art=json.loads((run/'aether-checks/character-artwork.json').read_text());assert art['slots']==18 and art['failures']==0;artwork[loader]=art
        for name in ['journal.json','character-artwork.json']:shutil.copy2(run/'aether-checks'/name,OUT/f'{loader}-{name}')
        shutil.copy2(NEGATIVE[loader]/'aether-checks/journal.json',OUT/f'{loader}-without-aether-journal.json')
        shutil.copy2(NEGATIVE[loader]/'staged-artifacts.json',OUT/f'{loader}-without-aether-artifacts.json')
        staged=json.loads((run/'staged-artifacts.json').read_text());shutil.copy2(run/'staged-artifacts.json',OUT/f'{loader}-tested-artifacts.json')
        upstream[loader]=[a for a in staged['artifacts']if a['family']=='upstream']
        name=f'salts_inventory_compat-1.20.1-{label}-0.1.3.jar';source=ROOT/f'compatibility/versions/1.20.1/{loader}/build/libs'/name
        assert next(a for a in staged['artifacts']if a['family']=='compatibility')['sha256']==digest(source)
        assert next(a for a in negative_artifacts['artifacts']if a['family']=='compatibility')['sha256']==digest(source)
        with zipfile.ZipFile(source)as jar:
            names=jar.namelist()
            assert all('com/salts_inventory_update/compat/aether/'+c+'.class'in names for c in ['AetherCharacterLayout','AetherCharacterBridge','AetherEquipmentClient','AetherEquipmentMenu'])
            config=json.loads(jar.read('salts_inventory_compat.reviewed-native.mixins.json'))
            for c in ['AetherCharacterDesktopMixin','AetherCharacterSlotsMixin','AetherCharacterBoundsMixin']:
                assert c in config['client'] and 'com/salts_inventory_update/compat/nativewindows/mixin/'+c+'.class'in names
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
    bundle=DIST/'salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.aether-character-tmp');checksum_entry='checksums/compatibility/SHA256SUMS.txt'
    with zipfile.ZipFile(bundle)as old:
        names=old.namelist();assert set(replacements).issubset(names)and checksum_entry in names
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
        'normal-character':('fabric','character-normal'),
        'aether-character-fabric':('fabric','character-final-arranged'),
        'aether-character-forge-and-backpack':('forge','character-backpack-arranged'),
        'accessories-exclusive-panel':('fabric','character-general-panel'),
        'native-slot-artwork-fabric':('fabric','character-aether-empty'),
        'native-slot-artwork-forge':('forge','character-aether-empty'),
    }
    images=[]
    for name,(loader,capture)in selected.items():
        source=RUNS[loader]/'screenshots'/(capture+'.png');target=OUT/(name+'.png');shutil.copy2(source,target)
        assert digest(source)==digest(target)
        images.append(dict(file=str(target.relative_to(ROOT)),source=str(source.relative_to(ROOT)),sha256=digest(target),size=list(Image.open(target).size)))
    manifest=dict(scope='Aether Character mode / Minecraft 1.20.1 Fabric and Forge',
        runtime='Production clients, integrated servers, GUI scale 2',live_assertions=counts,without_aether_assertions=negative_counts,
        total_live_assertions=sum(counts.values())+sum(negative_counts.values()),layout_tests=2,
        artwork={k:{n:v[n]for n in ['slots','channel_tolerance','failures']}for k,v in artwork.items()},
        aether_equipment_presentation='Existing Character window; no separate Aether window',
        general_wearables='Accessories and Curios panels are mutually exclusive with Aether Character mode',
        quark_backpack_parent='Inventory',core_and_api_unchanged=True,controlify_unchanged=True,
        unrelated_bundle_entries_preserved=True,bundle_sha256=digest(bundle),artifacts=artifacts,images=images,upstream=upstream,
        fabric_runtime_note='Accessories beta48 needs Sodium rendering API when rendering the cape. Final profile includes unmodified Sodium 0.5.13 and Indium 1.0.36. These are test-profile dependencies, not bundled or newly required by SIU.',
        limits='Focused affected-menu checks only. No complete gameplay suite, dedicated multiplayer, other GUI scales, arbitrary equipment datapacks, supporter-only customization or full Moa hatch cycle. Raw native keyboard events were not part of the checks. Unchanged workstations and Lore were not rerun.')
    (OUT/'verification.json').write_text(json.dumps(manifest,indent=2))
    print(json.dumps({k:v for k,v in manifest.items()if k not in ['images','upstream']},indent=2))


if __name__=='__main__':main()
