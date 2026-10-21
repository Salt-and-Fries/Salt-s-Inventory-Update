"""Audit Core Mouse Tweaks packaging and optional pre-change artifact lineage."""
from pathlib import Path
import argparse, hashlib, json, zipfile

ROOT = Path(__file__).resolve().parents[1]
def sha(data): return hashlib.sha256(data).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline', type=Path, help='Optional pre-change artifacts.json')
    args = parser.parse_args()
    cores = sorted((ROOT/'dist/core').glob('*.jar'))
    assert len(cores) == 12, 'Expected twelve production Core JARs'
    results=[]
    for path in cores:
        with zipfile.ZipFile(path) as jar:
            names=jar.namelist()
            assert not any(n.startswith('yalter/') for n in names), (path,'embedded Mouse Tweaks')
            assert not any(n.startswith('com/salts_inventory_update/compat/') for n in names), (path,'embedded Compatibility')
            assert not any('functionaltest/' in n or 'MouseTweaksQaInputMixin' in n for n in names), (path,'QA leaked into production')
            config=json.loads(jar.read('salts_inventory_update.mixins.json'))
            assert config['plugin'].endswith('.MouseTweaksMixinPlugin')
            assert all('client.mousetweaks.'+n in config['client'] for n in ['MouseTweaksScreenMixin','MouseTweaksInputMixin'])
            if 'fabric.mod.json' in names:
                metadata=json.loads(jar.read('fabric.mod.json'))
                assert metadata['id']=='salts_inventory_update' and metadata['version']=='0.1.5'
                assert 'mousetweaks' not in metadata.get('depends',{})
            else:
                import tomllib
                name='META-INF/neoforge.mods.toml' if 'META-INF/neoforge.mods.toml' in names else 'META-INF/mods.toml'
                metadata=tomllib.loads(jar.read(name).decode())
                assert metadata['mods'][0]['modId']=='salts_inventory_update'
                assert metadata['mods'][0]['version']=='0.1.5'
                assert not any(d['modId']=='mousetweaks' for d in metadata.get('dependencies',{}).get('salts_inventory_update',[]))
            if args.baseline:
                old=args.baseline.parent/'core-jars'/path.name
                with zipfile.ZipFile(old) as before:
                    previous={n:sha(before.read(n)) for n in before.namelist() if n.startswith('com/salts_inventory_update/api/') and n.endswith('.class')}
                    current={n:sha(jar.read(n)) for n in names if n.startswith('com/salts_inventory_update/api/') and n.endswith('.class')}
                    assert previous==current, (path,'public API bytecode changed')
        release=ROOT/'build/release/0.1.5'/path.name
        assert release.read_bytes()==path.read_bytes()
        results.append(dict(file=str(path.relative_to(ROOT)),sha256=sha(path.read_bytes())))
    for family in ['core','compatibility','controlify']:
        folder=ROOT/'dist'/family
        for line in (folder/'SHA256SUMS.txt').read_text().splitlines():
            digest,name=line.split('  ',1)
            assert sha((folder/name).read_bytes())==digest, name
    all_jars=list((ROOT/'dist').glob('*/*.jar'))
    assert len(all_jars)==36
    with zipfile.ZipFile(ROOT/'dist/salts-inventory-all-latest.zip') as bundle:
        for path in all_jars:
            matches=[name for name in bundle.namelist() if name.rsplit('/',1)[-1]==path.name]
            assert len(matches)==1 and bundle.read(matches[0])==path.read_bytes(), path
    if args.baseline:
        baseline=json.loads(args.baseline.read_text())
        for family in ['compatibility','controlify']:
            files=list((ROOT/'dist'/family).glob('*.jar'))
            assert len(files)==12
            for path in files:
                assert baseline[str(path.relative_to(ROOT))]==sha(path.read_bytes()), (path,'companion artifact changed')
    output=ROOT/'build/mouse-tweaks/artifact-audit.json'; output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(dict(core=results,companionJars=24,bundleJars=36,publicApiUnchanged=bool(args.baseline)),indent=2)+'\n')
    print('Verified twelve optional Core integrations, production metadata, checksums and 36-JAR bundle.')
    if args.baseline: print('Public API bytecode and all 24 companion JARs are unchanged.')

if __name__=='__main__': main()
