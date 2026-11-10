"""Re-use pinned fixtures from the later Project Everything implementations."""
from pathlib import Path
import hashlib, json
ROOT = Path(__file__).resolve().parents[2]

def is_publisher_input(record):
    """A prior audit's observer is never an upstream mod dependency."""
    name=record.get('filename',Path(record.get('file',record.get('path',''))).name).lower()
    return record.get('family') not in {'qa','core','compatibility'} and name!='native-ui-reference.jar' and not name.startswith('salts_inventory_')

def write_fixture(destination,prepared):
    if destination.exists():
        existing=json.loads(destination.read_text())
        if existing.get('regression_fixture_revision'):
            assert (existing['minecraft'],existing['loader'])==(prepared['minecraft'],prepared['loader'])
            return  # Do not restore a broken historical dependency pin.
    destination.write_text(json.dumps(prepared,indent=2)+'\n')

def platform(mc, loader):
    candidates = sorted((ROOT / 'build/all-target-rollout/fixtures').glob(mc.replace('.', '') + '-' + loader + '*inventory*/fixture.json'))
    return next((json.loads(p.read_text()).get('runtime', {}) for p in candidates if json.loads(p.read_text()).get('runtime')), {})

def main():
    paths = []
    for folder in ['build/enchanted-all-targets', 'build/kibe-all-targets', 'build/quark-all-targets',
                   'output/ad-astra-implemented', 'output/powah-all-targets']:
        paths.extend((ROOT / folder).glob('fixture-*.json'))
    paths.extend((ROOT / 'build/extended-ae-all-targets/fixtures').glob('*/fixture.json'))
    paths.extend((ROOT / 'build/tech-reborn-all-targets').glob('*/fixture.json'))
    paths.extend(ROOT / p for p in ['build/aether-ports/fixture-fabric.json', 'build/aether-ports/fixture-neoforge-final.json'])
    rows = []
    for index, path in enumerate(sorted(paths)):
        if not path.exists(): continue
        data = json.loads(path.read_text()); mc = data.get('minecraft'); loader = data.get('loader')
        if not mc or not loader: continue
        artifacts = []
        for original in data.get('artifacts', []):
            if not is_publisher_input(original): continue
            if 'path' not in original: continue
            file = ROOT / original['path']
            if not file.exists(): raise FileNotFoundError(file)
            digest = hashlib.sha512(file.read_bytes()).hexdigest()
            if original.get('sha512'): assert digest == original['sha512'], path
            artifacts.append({**original, 'path': str(file.resolve()), 'filename': original.get('filename', file.name),
                'coordinate': original.get('coordinate', 'exact-publisher-file:' + digest[:24]), 'sha512': digest})
        if not artifacts: continue
        name = f'regression-recent-{index:02d}-{mc.replace(".", "")}-{loader}'
        destination = ROOT / 'build/all-target-rollout/fixtures' / name / 'fixture.json'; destination.parent.mkdir(parents=True, exist_ok=True)
        prepared = {'minecraft': mc, 'loader': loader, 'scenario': 'extension-empty', 'artifacts': artifacts,
            'runtime': data.get('runtime', {}), 'regression_source_fixture': str(path.relative_to(ROOT)),
            'regression_scope': 'Original pinned publisher artifacts; no compatibility or observer classes are substituted for publisher menus.'}
        prepared['runtime'] = {**platform(mc, loader), **prepared['runtime']}
        write_fixture(destination,prepared)
        rows.append({'target': mc + '/' + loader, 'fixture': name, 'source': str(path.relative_to(ROOT)), 'publisher_artifacts': len(artifacts)})
    # The large 1.20.1 cohort also contains MI, Zenith, legacy Kibe/Powah and
    # embedded Prominent UI, which are not in the original inventory batch.
    for target, name in [('1.20.1/fabric', '1201-fabric-prominent-shop-tail-v1'), ('1.20.1/forge', '1201-forge-remaining-reviewed-tail-v1')]:
        if (ROOT / 'build/all-target-rollout/fixtures' / name / 'fixture.json').exists(): rows.append({'target': target, 'fixture': name, 'source': 'Previously pinned original audit cohort'})
    for mc, loader, profile in [
        ('1.21.1', 'neoforge', '1.21.1-neoforge-integrated-mi-refine03'),
        ('1.20.1', 'forge', '1.20.1-forge-siu-ssn-implemented-06'),
        ('1.21.1', 'neoforge', '1.21.1-neoforge-integrated-ssn-implemented-04'),
        ('26.1.2', 'neoforge', '26.1.2-neoforge-integrated-ssn-implemented-04')]:
        path = ROOT / 'build/remaining-ui-audit/gameplay' / profile / 'staged-artifacts.json'
        if not path.exists(): raise FileNotFoundError(path)
        artifacts = []
        for r in json.loads(path.read_text())['artifacts']:
            file = Path(r['file'])
            if not is_publisher_input(r): continue
            assert hashlib.sha256(file.read_bytes()).hexdigest() == r['sha256']
            digest = hashlib.sha512(file.read_bytes()).hexdigest()
            artifacts.append({'path': str(file), 'filename': r['filename'], 'sha512': digest, 'coordinate': 'exact-publisher-file:' + digest[:24]})
        name = 'regression-' + profile
        destination = ROOT / 'build/all-target-rollout/fixtures' / name / 'fixture.json'; destination.parent.mkdir(parents=True, exist_ok=True)
        write_fixture(destination,{'minecraft': mc, 'loader': loader, 'scenario': 'extension-empty', 'artifacts': artifacts,
            'runtime': platform(mc, loader), 'regression_source_fixture': str(path.relative_to(ROOT))})
        rows.append({'target': mc + '/' + loader, 'fixture': name, 'source': str(path.relative_to(ROOT)), 'publisher_artifacts': len(artifacts)})
    target = json.loads((ROOT / 'output/tech-reborn-all-targets/targets.json').read_text())[0]
    artifacts = []
    for key in ['techreborn_jar', 'reborncore_jar']:
        file = ROOT / target[key]; digest = hashlib.sha512(file.read_bytes()).hexdigest()
        artifacts.append({'path': str(file.resolve()), 'filename': file.name, 'sha512': digest, 'coordinate': 'exact-publisher-file:' + digest[:24]})
    name = 'regression-techreborn-current-1201-fabric'; destination = ROOT / 'build/all-target-rollout/fixtures' / name / 'fixture.json'; destination.parent.mkdir(parents=True, exist_ok=True)
    write_fixture(destination,{'minecraft': '1.20.1', 'loader': 'fabric', 'scenario': 'extension-empty', 'artifacts': artifacts, 'runtime': platform('1.20.1', 'fabric')})
    rows.append({'target': '1.20.1/fabric', 'fixture': name, 'source': 'output/tech-reborn-all-targets/targets.json', 'publisher_artifacts': len(artifacts)})
    out = ROOT / 'output/project-everything-regression/additional-fixtures.json'; out.write_text(json.dumps(rows, indent=2) + '\n')
    print('Prepared', len(rows), 'additional exact-publisher cohorts')

if __name__ == '__main__': main()
