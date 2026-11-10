"""Stage the two validated modern Enchanted ports and record all target coverage."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/enchanted-all-targets'
OUT=ROOT/'output/enchanted-all-targets'
DIST=ROOT/'dist'
digest=lambda p:hashlib.sha256(Path(p).read_bytes()).hexdigest()


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    availability=json.loads((WORK/'availability.json').read_text())
    before=json.loads((WORK/'core-companions-before.json').read_text())
    assert all(digest(ROOT/p)==sha for p,sha in before.items())
    unit=ET.parse(ROOT/'compatibility/versions/1.21.1/fabric/build/test-results/test/TEST-com.salts_inventory_update.compat.enchanted.EnchantedLayoutsTest.xml').getroot()
    assert unit.attrib['tests']=='5' and unit.attrib['failures']=='0'
    replacements={};rows=[];live=[]
    for loader,label in [('fabric','Fabric'),('neoforge','NeoForge')]:
        run=ROOT/f'build/remaining-ui-audit/gameplay/1.21.1-{loader}-integrated-enchanted-modern-01'
        journal=json.loads((run/'enchanted-checks/journal.json').read_text())
        assert len(journal['checks'])==33 and all(c['passed'] for c in journal['checks'])
        art=json.loads((run/'enchanted-checks/artwork-verification.json').read_text())
        assert art['slots']==20 and art['pixels']==6480 and art['failures']==0
        name=f'salts_inventory_compat-1.21.1-{label}-0.1.3.jar'
        source=ROOT/f'compatibility/versions/1.21.1/{loader}/build/libs'/name
        target=DIST/'compatibility'/name
        staged=json.loads((run/'staged-artifacts.json').read_text())
        tested=next(a for a in staged['artifacts'] if a['filename']==name)
        assert tested['sha256']==digest(source)
        with zipfile.ZipFile(source) as jar:
            assert jar.testzip() is None
            assert 'com/salts_inventory_update/compat/enchanted/EnchantedWindows.class' in jar.namelist()
            assert not any(n.startswith(('com/siu_reference/','net/favouriteless/enchanted/')) for n in jar.namelist())
        backup=WORK/'dist-before'/name;backup.parent.mkdir(exist_ok=True)
        if not backup.exists():shutil.copy2(target,backup)
        replacements[f'1.21.1/{loader}/{name}']=source
        for item in ['journal.json','artwork-verification.json']:
            shutil.copy2(run/'enchanted-checks'/item,OUT/(loader+'-'+item))
        screen=run/'screenshots/enchanted-five-windows-fullscreen.png'
        shutil.copy2(screen,OUT/(loader+'-all-five-windows.png'))
        live.append(dict(minecraft='1.21.1',loader=loader,assertions=33,slot_pixels=6480,run=str(run.relative_to(ROOT)),
                         screenshot=str((OUT/(loader+'-all-five-windows.png')).relative_to(ROOT)),tested_jar_sha256=digest(source)))
    legacy=json.loads((ROOT/'output/enchanted-implemented/verification.json').read_text())
    assert all(digest(ROOT/a['file'])==a['sha256'] for a in legacy['artifacts'])
    for t in availability['targets']:
        row={k:t[k] for k in ['minecraft','loader','available']}
        if t['available']:
            label={'fabric':'Fabric','forge':'Forge','neoforge':'NeoForge'}[t['loader']]
            name=f"salts_inventory_compat-{t['minecraft']}-{label}-0.1.3.jar"
            path=DIST/'compatibility'/name
            source=replacements.get(f"{t['minecraft']}/{t['loader']}/{name}",path)
            row.update(release=t['release']['version_number'],version_id=t['release']['id'],windows=5,
                       jar=str(path.relative_to(ROOT)),sha256=digest(source),source_url='https://modrinth.com/mod/enchanted-witchcraft/version/'+t['release']['id'],
                       coverage='build verified; prior 1.20.1 coverage retained' if t['loader']=='forge' else 'focused live checks passed')
        else:row.update(windows=0,coverage='No official publisher release for this SIU target; no port added')
        rows.append(row)
    assert sum(r['available'] for r in rows)==4
    sums={p.name:digest(p) for p in (DIST/'compatibility').glob('*.jar')}
    sums.update({p.name:digest(p) for p in replacements.values()})
    checksum=''.join(f'{sums[name]}  {name}\n' for name in sorted(sums)).encode()
    bundle=DIST/'salts-inventory-all-latest.zip';temporary=bundle.with_suffix('.enchanted-ports-tmp')
    checksums='checksums/compatibility/SHA256SUMS.txt'
    with zipfile.ZipFile(bundle) as old:
        names=old.namelist();assert set(replacements).issubset(names)
        untouched={n:hashlib.sha256(old.read(n)).hexdigest() for n in names if n not in replacements and n!=checksums}
        with zipfile.ZipFile(temporary,'w',zipfile.ZIP_DEFLATED) as new:
            for info in old.infolist():
                data=replacements[info.filename].read_bytes() if info.filename in replacements else checksum if info.filename==checksums else old.read(info)
                new.writestr(info,data)
    with zipfile.ZipFile(temporary) as archive:
        assert archive.testzip() is None and archive.namelist()==names
        assert all(hashlib.sha256(archive.read(n)).hexdigest()==sha for n,sha in untouched.items())
        for n,p in replacements.items():assert hashlib.sha256(archive.read(n)).hexdigest()==digest(p)
    for p in replacements.values():shutil.copy2(p,DIST/'compatibility'/p.name)
    (DIST/'compatibility/SHA256SUMS.txt').write_bytes(checksum);temporary.replace(bundle)
    assert all(digest(ROOT/p)==sha for p,sha in before.items())
    assert all(digest(ROOT/a['file'])==a['sha256'] for a in legacy['artifacts'])
    manifest=dict(checked_at='2026-11-02',available_targets=4,updated_jars=2,targets=rows,live=live,
        geometry_tests=5,core_and_api_unchanged=True,legacy_jars_unchanged=True,other_bundle_entries_preserved=True,
        bundle_sha256=digest(bundle),limits='Focused Enchanted checks only; no complete gameplay suite, dedicated multiplayer or exhaustive GUI-scale checks. Prior 1.20.1 Forge coverage is build-level, not a new Forge gameplay test.')
    (OUT/'verification.json').write_text(json.dumps(manifest,indent=2))
    table=['# Enchanted: Witchcraft - SIU target coverage','','Checked November 2, 2026 against official publisher releases.','',
           '| Minecraft | Loader | Enchanted | SIU windows / coverage |','| --- | --- | --- | --- |']
    for r in rows:
        table.append(f"| {r['minecraft']} | {r['loader']} | {r.get('release','No release')} | "+('5 / '+r['coverage'] if r['available'] else 'Not applicable')+' |')
    table.extend(['','Only two new 1.21.1 Compatibility JARs changed. Existing 1.20.1 builds and all Core/API distributions remain unchanged.',
                  'Both new loaders passed 33 focused live assertions and 6,480 slot-artwork pixel comparisons. Five layout tests passed.',
                  'Screenshots, journals and exact hashes are saved alongside this table. This is not a complete gameplay test.',''])
    (OUT/'compatibility-table.md').write_text('\n'.join(table))
    print('Staged 1.21.1 Fabric/NeoForge; all four applicable targets covered, 66 focused modern live assertions passed.')


if __name__=='__main__':main()
