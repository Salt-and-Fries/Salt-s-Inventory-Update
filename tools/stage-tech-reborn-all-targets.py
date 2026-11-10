"""Verify live-tested Tech Reborn ports and stage only their release artifacts."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'output/tech-reborn-all-targets'
RUNS = {
    '1.20.1': '1.20.1-fabric-integrated-tech-reborn-latest-01',
    '1.21.1': '1.21.1-fabric-siu-tech-reborn-port-05',
    '26.1.2': '26.1.2-fabric-siu-tech-reborn-final-01',
    '26.2': '26.2-fabric-siu-tech-reborn-final-02',
    '26.3': '26.3-fabric-siu-tech-reborn-final-02',
}
HEAT_RUNS = dict(RUNS, **{'1.21.1': '1.21.1-fabric-siu-tech-reborn-heat-01'})
sha = lambda data: hashlib.sha256(data).hexdigest()


def load(path):
    return json.loads(path.read_text())


def entries(path, prefix):
    with zipfile.ZipFile(path) as jar:
        return {n: jar.read(n) for n in jar.namelist() if n.startswith(prefix)}


def main():
    before = load(OUT / 'dist-before.json')
    updates, records, coverage = {}, [], []
    for mc, run in RUNS.items():
        result = load(OUT / run / 'checks.json')
        heat_run = HEAT_RUNS[mc]
        heat = load(OUT / heat_run / 'heat-checks.json')
        assert result['status'] == heat['status'] == 'passed', mc
        assert all(c['passed'] for c in result['checks'] + heat['checks']), mc
        profile = ROOT / 'build/remaining-ui-audit/gameplay' / run
        staged = load(profile / 'staged-artifacts.json')['artifacts']
        name = f'salts_inventory_compat-{mc}-Fabric-0.1.3.jar'
        path = ROOT / (f'dist/compatibility/{name}' if mc == '1.20.1' else
                       f'compatibility/versions/{mc}/fabric/build/libs/{name}')
        data = path.read_bytes()
        assert sha(data) == next(a['sha256'] for a in staged if a['family'] == 'compatibility'), mc
        with zipfile.ZipFile(path) as jar:
            assert jar.testzip() is None
            assert not any(n.startswith(('com/siu_reference/', 'techreborn/', 'reborncore/',
                                         'com/salts_inventory_update/api/')) for n in jar.namelist())
            config = load_json_bytes(jar.read('salts_inventory_compat.techreborn.mixins.json'))
            assert config['required'] and config['client']
        if mc != '1.20.1':
            updates[f'dist/compatibility/{name}'] = data
        else:
            assert sha(data) == before[f'dist/compatibility/{name}']
        geom = load(OUT / run / 'geometry.json') if mc != '1.20.1' else None
        nuclear = None
        if mc.startswith('26.'):
            nuclear = load(OUT / run / 'nuclear-checks.json')
            assert nuclear['status'] == 'passed' and all(c['passed'] for c in nuclear['checks'])
        coverage.append({'minecraft': mc, 'loader': 'fabric', 'run': run, 'heat_run': heat_run,
                         'checks': len(result['checks']), 'heat_checks': len(heat['checks']),
                         'nuclear_checks': len(nuclear['checks']) if nuclear else 0,
                         'geometry': geom, 'compatibility_sha256': sha(data)})
        records.append({'family': 'compatibility', 'minecraft': mc, 'loader': 'fabric',
                        'file': 'dist/compatibility/' + name, 'sha256': sha(data),
                        'updated': mc != '1.20.1'})

    api_prefix = 'com/salts_inventory_update/api/'
    renderer_prefix = 'com/salts_inventory_update/client/automatic/NativeFrameBuffer'
    api_count = {}
    for mc in ['26.2', '26.3']:
        renderers = []
        for loader in ['Fabric', 'NeoForge']:
            name = f'salts_inventory_update-{mc}-{loader}-0.1.6.jar'
            old = ROOT / 'dist/core' / name
            new = ROOT / f'versions/{mc}/{loader.lower()}/build/libs' / name
            a, b = entries(old, api_prefix), entries(new, api_prefix)
            assert a and a == b, f'{mc} {loader} public API bytes changed'
            api_count[f'{mc}/{loader}'] = len(a)
            renderers.append(entries(new, renderer_prefix))
            with zipfile.ZipFile(new) as jar:
                assert jar.testzip() is None
                with zipfile.ZipFile(old) as previous:
                    assert set(previous.namelist()) == set(jar.namelist())
                    changed = [n for n in jar.namelist() if previous.read(n) != jar.read(n)]
                    assert changed and all(n.startswith(renderer_prefix) for n in changed), changed
            data = new.read_bytes()
            if loader == 'Fabric':
                staged = load(ROOT / 'build/remaining-ui-audit/gameplay' / RUNS[mc] / 'staged-artifacts.json')['artifacts']
                assert sha(data) == next(a['sha256'] for a in staged if a['family'] == 'core')
            updates['dist/core/' + name] = data
            records.append({'family': 'core', 'minecraft': mc, 'loader': loader.lower(),
                            'file': 'dist/core/' + name, 'sha256': sha(data), 'updated': True})
        assert renderers[0] and renderers[0] == renderers[1], f'{mc} renderer loader mismatch'

    checksums = {}
    for family in ['core', 'compatibility']:
        sums = {p.name: sha(updates.get(p.relative_to(ROOT).as_posix(), p.read_bytes()))
                for p in (ROOT / 'dist' / family).glob('*.jar')}
        data = ''.join(f'{sums[name]}  {name}\n' for name in sorted(sums)).encode()
        updates[f'dist/{family}/SHA256SUMS.txt'] = data
        checksums[f'checksums/{family}/SHA256SUMS.txt'] = data
    mutable = {*updates, 'dist/salts-inventory-all-latest.zip'}
    for path, expected in before.items():
        assert sha((ROOT / path).read_bytes()) == expected, 'Release baseline drift: ' + path

    replacement = dict(checksums)
    for record in records:
        if record['updated']:
            replacement[f"{record['minecraft']}/{record['loader']}/{Path(record['file']).name}"] = updates[record['file']]
    bundle = ROOT / 'dist/salts-inventory-all-latest.zip'
    temp = bundle.with_suffix('.tech-reborn.tmp.zip')
    with zipfile.ZipFile(bundle) as old:
        assert set(replacement).issubset(old.namelist())
        unchanged = {i.filename: sha(old.read(i)) for i in old.infolist() if i.filename not in replacement}
        with zipfile.ZipFile(temp, 'w') as new:
            new.comment = old.comment
            for info in old.infolist():
                new.writestr(info, replacement.get(info.filename, old.read(info)))
    with zipfile.ZipFile(temp) as new:
        assert new.testzip() is None
        assert all(new.read(n) == d for n, d in replacement.items())
        assert all(sha(new.read(n)) == expected for n, expected in unchanged.items())
    for path, data in updates.items():
        (ROOT / path).write_bytes(data)
    temp.replace(bundle)
    for path, expected in before.items():
        if path not in mutable:
            assert sha((ROOT / path).read_bytes()) == expected, path
    report = {'status': 'passed', 'artifacts': records, 'coverage': coverage,
              'public_api_byte_identical': True, 'public_api_entries': api_count,
              'core_26_2_and_26_3_renderer_loader_parity': True,
              'core_changes_confined_to_native_frame_buffer': True,
              'unrelated_dist_artifacts_preserved': True,
              'unchanged_bundle_entries_verified': len(unchanged),
              'bundle_sha256': sha(bundle.read_bytes()),
              'limitations': ['Focused UI/actions only; no complete gameplay suite.',
                              '26.2/26.3 NeoForge Core was built and checked for API/renderer parity; Tech Reborn has no official NeoForge target, so no Tech Reborn NeoForge runtime was tested.',
                              'The pinned 6.0.5 publisher resources log missing reborncore:padded recipe serializers. Window/action checks passed; those recipes were not certified.']}
    (OUT / 'verification.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))


def load_json_bytes(data):
    return json.loads(data)


if __name__ == '__main__':
    main()
