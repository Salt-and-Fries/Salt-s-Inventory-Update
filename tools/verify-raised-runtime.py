"""Verify focused HUD gameplay evidence, shipping-artifact lineage and explicit coverage scope."""
from pathlib import Path
import argparse
import collections
import hashlib
import json
import struct

ROOT = Path(__file__).resolve().parents[1]


def verify(folder, artifacts):
    folder = Path(folder).resolve()
    report = json.loads((folder / 'results.json').read_text(encoding='utf-8-sig'))
    assert report['scope'] == 'raised-hud-interoperability', folder
    assert report['failures'] == 0, folder
    assert not [r for r in report['results'] if r['status'] == 'FAIL'], folder
    mods = report['loadedMods']
    assert mods['salts_inventory_update'] == '0.1.5'
    assert 'salts_inventory_controlify' not in mods
    if report['scenario'] == 'core':
        assert 'salts_inventory_compat' not in mods
    else:
        assert mods['salts_inventory_compat'] == '0.1.2'
    if 'raised' in mods:
        assert mods['raised'] == '6.0.2'
    passed = {r['name'] for r in report['results'] if r['status'] == 'PASS'}
    required = {'integrated-server-connected', 'authenticated-desktop-ready', 'raised-presence',
                'hotbar-pickup-real-cursor', 'hud-to-window-server', 'offhand-return',
                'raised-drag-conservation', 'custom-scale-input', 'hidden-slot-no-input',
                'window-hosted-no-detached-target', 'hud-cleanup-no-session'}
    required.update(f'{state}-{check}-{index}' for state in ['default', 'live', 'gui-scale', 'zero']
                    for check in ['geometry', 'hit', 'world-ownership', 'mouse-tweaks-target'] for index in range(10))
    if 'raised' in mods:
        required.add('old-hud-location-inactive')
    if 'mousetweaks' in mods:
        required.add('raised-wheel-conservation')
    assert required <= passed, (folder, sorted(required - passed))

    captures = sorted((folder / 'screenshots').glob('*.png'))
    assert len(captures) >= 8, folder
    for capture in captures:
        data = capture.read_bytes()
        assert data[:8] == b'\x89PNG\r\n\x1a\n' and min(struct.unpack_from('>II', data, 16)) > 0

    mode = 'development classes'
    if report['loader'] == 'fabric':
        mode = 'packaged production JARs'
        assert '.jar' in report['coreCodeSource']
        if report['scenario'] != 'core':
            assert '.jar' in report['compatibilityCodeSource']
        log = (folder / 'launch.log').read_text(encoding='utf-8', errors='replace')
        for kind in ['core'] + ([] if report['scenario'] == 'core' else ['compatibility']):
            jar = next(a for a in artifacts if a['artifact'].startswith('dist/' + kind + '/')
                       and f'-{report["minecraft"]}-Fabric-' in a['artifact'])
            assert 'PACKAGED_QA_ARTIFACT_SHA256=' + jar['sha256'] in log, (folder, jar)
    counts = dict(collections.Counter(result['status'] for result in report['results']))
    return {'minecraft': report['minecraft'], 'loader': report['loader'], 'mode': mode,
            'raised': mods.get('raised'), 'compatibility': mods.get('salts_inventory_compat'),
            'mouse_tweaks': mods.get('mousetweaks'), 'counts': counts,
            'report': str(folder / 'results.json'), 'captures': len(captures)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('folders', nargs='+', type=Path)
    args = parser.parse_args()
    artifacts = json.loads((ROOT / 'build/raised-qa/artifacts.json').read_text())
    for artifact in artifacts:
        artifact['artifact'] = Path(artifact['artifact']).as_posix()
        path = ROOT / artifact['artifact']
        assert hashlib.sha256(path.read_bytes()).hexdigest() == artifact['sha256'], path
    results = [verify(folder, artifacts) for folder in args.folders]
    output = ROOT / 'build/raised-qa/verified-runtime.json'
    output.write_text(json.dumps({'scope': 'focused HUD checks; not a complete functional suite',
                                 'profiles': results, 'artifacts': artifacts}, indent=2) + '\n', encoding='utf-8')
    for row in results:
        print(f'{row["minecraft"]} {row["loader"]}: {row["counts"]}, {row["mode"]}')
    print(f'Verified {len(results)} focused profiles and unchanged shipping hashes for all 24 Core/Compatibility JARs')


if __name__ == '__main__':
    main()
