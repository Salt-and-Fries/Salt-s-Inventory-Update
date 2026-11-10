"""Record final focused evidence and optionally stage the tested artifact."""
from pathlib import Path
import argparse, hashlib, json, os, shutil, zipfile

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/kibe-implemented'
RUNS=['1.20.1-fabric-siu-kibe-final-02','1.20.1-fabric-siu-kibe-source-probe-01']
NAME='salts_inventory_compat-1.20.1-Fabric-0.1.3.jar'
BUILT=ROOT/'compatibility/versions/1.20.1/fabric/build/libs'/NAME
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main(publish):
    journals=[json.loads((OUT/f'journal-{run}.json').read_text(encoding='utf-8'))for run in RUNS]
    checks=[c for j in journals for c in j['checks']]
    assert len(checks)==184 and all(c['passed']for c in checks)
    assert len(journals[0]['captures'])==22
    manifest=json.loads((ROOT/'output/kibe-window-design/manifest.json').read_text(encoding='utf-8'))
    assert sum(len(c['slots'])for c in manifest['cases'])==375
    staged=[]
    for run in RUNS:
        d=json.loads((ROOT/'build/remaining-ui-audit/gameplay'/run/'staged-artifacts.json').read_text(encoding='utf-8'))
        compat=next(a for a in d['artifacts']if a['family']=='compatibility')
        assert compat['sha256']==sha(BUILT)
        for a in d['artifacts']:
            assert sha(Path(a['file']))==a['sha256'],a['file']
        staged.append(dict(run=run,artifacts=d['artifacts']))
    with zipfile.ZipFile(BUILT)as z:
        names=z.namelist();mod=json.loads(z.read('fabric.mod.json'))
        assert 'salts_inventory_compat.kibe.mixins.json'in mod['mixins']
        assert all(any(n.endswith('/'+c+'.class')for n in names)for c in ['KibeWindows','KibeClient','KibeChrome','KibeCoolerTickMixin'])
        assert not any('NativeUiReference'in n or 'kibe-windows'in n or n.startswith('com/salts_inventory_update/api/')for n in names)
    distribution={}
    if publish:
        target=ROOT/'dist/compatibility'/NAME
        shutil.copy2(BUILT,target)
        sums=target.parent/'SHA256SUMS.txt';lines=sums.read_text(encoding='utf-8').splitlines()
        assert sum(line.endswith('  '+NAME)for line in lines)==1
        sums.write_text('\n'.join(sha(target)+'  '+NAME if line.endswith('  '+NAME)else line for line in lines)+'\n',encoding='utf-8')
        bundle=ROOT/'dist/salts-inventory-all-latest.zip'
        replacements={'1.20.1/fabric/'+NAME:target.read_bytes(),'checksums/compatibility/SHA256SUMS.txt':sums.read_bytes()}
        temporary=bundle.with_suffix('.kibe.tmp.zip')
        with zipfile.ZipFile(bundle)as old:
            before={n:hashlib.sha256(old.read(n)).hexdigest()for n in old.namelist()}
            assert set(replacements)<=set(before)
            with zipfile.ZipFile(temporary,'w')as new:
                new.comment=old.comment
                for info in old.infolist():new.writestr(info,replacements.get(info.filename,old.read(info.filename)))
        with zipfile.ZipFile(temporary)as z:
            assert z.testzip()is None
            assert set(z.namelist())==set(before)
            changed=[n for n in before if hashlib.sha256(z.read(n)).hexdigest()!=before[n]]
            assert set(changed)<=set(replacements)
            for n,data in replacements.items():assert z.read(n)==data
        os.replace(temporary,bundle)
        assert sha(target)==sha(BUILT)
        distribution=dict(jar=str(target.relative_to(ROOT)),sha256=sha(target),bundle=str(bundle.relative_to(ROOT)),bundle_sha256=sha(bundle),changed_bundle_entries=changed)
    captures=[]
    for j in journals:
        for c in j['captures']:
            path=Path(c['screenshot']);assert path.exists()
            captures.append(dict(**c,sha256=sha(path)))
    extras=[]
    run=ROOT/'build/remaining-ui-audit/gameplay'/RUNS[1]
    for name in ['kibe-pocket-crafting-current','kibe-native-chunk-loader','kibe-cooler-management-menu','kibe-vacuum-tank-tooltip','kibe-big-torch-tooltip']:
        path=run/'screenshots'/(name+'.png');assert path.exists()
        extras.append(dict(screenshot=str(path.relative_to(ROOT)),sha256=sha(path)))
    result=dict(baseline=manifest['baseline'],upstream_release=manifest['release'],upstream_source=manifest['source'],
        status='passed',scope='Focused Kibe windows; not a complete functional or gameplay suite',
        check_count=len(checks),failed=0,screen_types=13,custom_views=22,native_slot_envelopes=375,
        inherited='Pocket Crafting Table uses existing vanilla crafting integration',native_screen='Chunk Loader',
        core_and_public_api_changed=False,build_log='build/kibe-implementation/build-final.log',
        journals=[str((OUT/f'journal-{r}.json').relative_to(ROOT))for r in RUNS],
        artifacts=staged,distribution=distribution,gallery='output/kibe-implemented/all-windows.png',captures=captures,extra_captures=extras,
        limits=['Dedicated multiplayer not exercised','World automation and redstone configurations not exhaustive; no Wither spawned',
                'All generator tiers visually mapped; actual generation checked for MK1 of each family',
                'Alternate resource packs, translations, controllers and optional-mod combinations not exhaustive',
                'Storage membership reviewed in code; sorting permutations not exhaustively exercised'],
        iteration_notes=['An observation-only capture-name validation error was fixed by using hyphenated names; generator checks and captures then passed.',
                         'The normal SIU held-item source lease was checked through the existing public server snapshot; raw vanilla inventory packets are not used to simulate desktop input.'],
        checks=checks)
    (OUT/'verification.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps(dict(checks=len(checks),failed=0,distribution=distribution),indent=2))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--publish',action='store_true');main(p.parse_args().publish)
