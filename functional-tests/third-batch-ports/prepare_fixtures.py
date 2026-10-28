"""Resolve and hash-check Modrinth QA fixtures; never touches production artifacts."""
from pathlib import Path
import argparse
import hashlib
import json
import time
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
WORK = ROOT / 'build/third-batch-ports'
CACHE = ROOT / 'build/compatibility-coverage/upstream-fixtures/jars'
API = 'https://api.modrinth.com/v2/'
# The target development runtime already supplies Fabric API.
PROVIDED = {'P7dR8mSH'}

def fetch(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'SIU-focused-QA/1.0'})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=45) as response:
                return response.read()
        except Exception:
            if attempt == 2:
                raise
            time.sleep(2 ** attempt)

def api(path):
    return json.loads(fetch(API + path))

def prepare(target):
    mc, loader = target['minecraft'], target['loader']
    selected = {}
    missing, conflicts = [], []
    queue = [(entry['project_id'], entry['version_id'], 'requested:' + entry['project'])
             for entry in target['integrations']]
    while queue:
        project, version, origin = queue.pop(0)
        if not project or project in PROVIDED:
            continue
        if project in selected:
            if version and selected[project]['id'] != version:
                conflicts.append({'project': project, 'requested': version,
                                  'selected': selected[project]['id'], 'origin': origin})
            continue
        if version:
            item = api('version/' + version)
        else:
            query = urllib.parse.urlencode({'game_versions': json.dumps([mc]),
                                            'loaders': json.dumps([loader])})
            options = api('project/' + project + '/version?' + query)
            if not options:
                missing.append({'project': project, 'origin': origin})
                continue
            item = options[0]
        selected[project] = item
        for dep in item.get('dependencies', []):
            if dep['dependency_type'] == 'required':
                queue.append((dep.get('project_id'), dep.get('version_id'), item['id']))
    CACHE.mkdir(parents=True, exist_ok=True)
    artifacts = []
    for project, version in selected.items():
        primary = next((f for f in version['files'] if f.get('primary')), version['files'][0])
        expected = primary['hashes']['sha512']
        dest = CACHE / (expected[:16] + '-' + primary['filename'])
        if not dest.exists() or hashlib.sha512(dest.read_bytes()).hexdigest() != expected:
            data = fetch(primary['url'])
            if hashlib.sha512(data).hexdigest() != expected:
                raise ValueError('SHA-512 mismatch for ' + primary['filename'])
            dest.write_bytes(data)
        artifacts.append({'path': str(dest), 'filename': primary['filename'],
                          'coordinate': f"maven.modrinth:{project}:{version['id']}",
                          'sha512': expected})
    fixture = {'minecraft': mc, 'loader': loader, 'scenario': 'extension-empty',
               'artifacts': artifacts, 'missing_dependencies': missing,
               'version_conflicts': conflicts, 'provided_dependencies': sorted(PROVIDED)}
    output = WORK / f'fixture-{mc}-{loader}.json'
    output.write_text(json.dumps(fixture, indent=2), encoding='utf-8')
    print(f'{mc}/{loader}: {len(artifacts)} artifacts; missing={missing}; conflicts={conflicts}', flush=True)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--target', action='append', required=True, help='Minecraft/loader')
    args = parser.parse_args()
    targets = json.loads((WORK / 'availability.json').read_text(encoding='utf-8'))['targets']
    for name in args.target:
        mc, loader = name.split('/')
        target = next(t for t in targets if t['minecraft'] == mc and t['loader'] == loader)
        prepare(target)
