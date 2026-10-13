"""Classify the fork's Java member additions separately from native AE2 code."""
import argparse
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reference', type=Path, required=True)
    parser.add_argument('--java-home', type=Path, required=True)
    args = parser.parse_args()
    reference = args.reference.resolve(strict=True)
    output = ROOT / 'build/ae2-port/members'
    output.mkdir(parents=True, exist_ok=True)
    java = args.java_home / 'bin/java.exe'
    javac = args.java_home / 'bin/javac.exe'
    subprocess.run([str(javac), '-d', str(output), str(ROOT / 'tools/JavaMembers.java')], check=True)
    inventory = json.loads((ROOT / 'build/ae2-port/integration-source-inventory.json').read_text())
    changes = []
    for entry in inventory['java_changes']:
        if entry['test_instrumentation']:
            continue
        path = entry['path']
        parsed = {}
        for side, commit in [('fork', inventory['reference_commit']), ('upstream', inventory['upstream_base'])]:
            if side == 'upstream' and entry['status'] == 'A':
                parsed[side] = []
                continue
            source = subprocess.check_output(['git', '--no-optional-locks', '-C', str(reference),
                                              'show', f'{commit}:{path}'])
            file = output / side / path
            file.parent.mkdir(parents=True, exist_ok=True)
            file.write_bytes(source)
            data = subprocess.check_output([str(java), '-cp', str(output), 'JavaMembers', str(file)])
            parsed[side] = json.loads(data)[0]['members']
        old = {member['key']: member for member in parsed['upstream']}
        new = {member['key']: member for member in parsed['fork']}
        def body(member):
            return member['source'].replace('\r\n', '\n')
        changes.append({'path': path,
                        'added': [member for key, member in new.items() if key not in old],
                        'modified': [member for key, member in new.items() if key in old and body(member) != body(old[key])],
                        'removed': [member for key, member in old.items() if key not in new]})
    target = output / 'integration-member-changes.json'
    target.write_text(json.dumps(changes, indent=2) + '\n', encoding='utf-8')
    print(f'Classified {len(changes)} Java files: ' + ', '.join(
        f'{sum(len(change[kind]) for change in changes)} {kind}' for kind in ['added', 'modified', 'removed']))
    print(target)


if __name__ == '__main__':
    main()
