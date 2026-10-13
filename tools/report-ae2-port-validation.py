"""Record executed test reports, artifacts, and read-only fork integrity evidence."""
import argparse
import hashlib
import json
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    baseline = json.loads((ROOT / 'build/ae2-port/reference-before.json').read_text())
    reference = Path(baseline['path'])
    output = args.output.resolve()
    assert output.is_relative_to(ROOT) and not output.is_relative_to(reference)
    git = lambda *args: subprocess.check_output(['git', '-C', str(reference), *args], text=True).strip()
    changed = [name for name, digest in baseline['tracked_files'].items()
               if not (reference / name).is_file()
               or hashlib.sha256((reference / name).read_bytes()).hexdigest() != digest]
    result = {'reference': {'head': git('rev-parse', 'HEAD'), 'status': git('status', '--porcelain'),
                            'trackedFiles': len(baseline['tracked_files']), 'changedFiles': changed},
              'junit': {}, 'gameplay': {}, 'artifacts': {}}
    assert result['reference']['head'] == baseline['head'] and not result['reference']['status'] and not changed
    reference_jar = reference / 'build/libs/appliedenergistics2-0.0.0-api5-rectangular-toolbar-r1.jar'
    result['reference']['jarSha256'] = hashlib.sha256(reference_jar.read_bytes()).hexdigest()
    assert result['reference']['jarSha256'] == '31042bf006a7226283494d8ad8684daef52ab246917b317e324e8fda7a669872'
    suites = [ET.parse(path).getroot() for parent in ['common', 'versions', 'compatibility']
              for path in (ROOT / parent).rglob('TEST-*.xml') if '/test-results/test/' in path.as_posix()]
    result['junit'] = {key: sum(int(suite.get(key, '0')) for suite in suites)
                       for key in ['tests', 'failures', 'errors', 'skipped']}
    result['junit']['suiteFiles'] = len(suites)
    # Reports are evidence for their recorded scope, not broad integration completion.
    for path in (ROOT / 'build/gameplay-qa').rglob('results.json'):
        if path.parent.parent.name not in ['ae2', 'ae2-atlas', 'core', 'extension-empty']:
            continue
        report = json.loads(path.read_text())
        rows = report['results']
        result['gameplay'][path.relative_to(ROOT).as_posix()] = {
            'records': len(rows), 'failures': sum(row['status'] == 'FAIL' for row in rows)}
    for kind in ['core', 'compatibility']:
        result['artifacts'][kind] = {path.name: hashlib.sha256(path.read_bytes()).hexdigest()
                                     for path in sorted((ROOT / 'dist' / kind).glob('*.jar'))}
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({'referenceUnchanged': True, 'junit': result['junit'],
                      'productionJars': {kind: len(values) for kind, values in result['artifacts'].items()}}))

if __name__ == '__main__':
    main()
