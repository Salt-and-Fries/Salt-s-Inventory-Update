"""Compare exported class/member ABI against a preserved production distribution.

Additive default methods are permitted. Removed members, changed descriptors,
static/instance changes and tighter visibility are rejected. Consumer example
compilation remains a separate source-compatibility gate.
"""
import argparse
import hashlib
import json
import struct
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREFIX = 'com/salts_inventory_update/api/'


def declarations(data):
    assert data[:4] == b'\xca\xfe\xba\xbe'
    position = 8

    def u2():
        nonlocal position
        value = struct.unpack_from('>H', data, position)[0]; position += 2
        return value

    pool = [None] * u2()
    index = 1
    while index < len(pool):
        tag = data[position]; position += 1
        if tag == 1:
            size = u2(); pool[index] = data[position:position + size].decode('utf-8', errors='replace'); position += size
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): position += 4
        elif tag in (5, 6): position += 8; index += 1
        elif tag in (7, 8, 16, 19, 20): pool[index] = u2()
        elif tag == 15: position += 3
        else: raise AssertionError(f'Unknown constant tag {tag}')
        index += 1
    access, this, parent = u2(), u2(), u2()
    interfaces = [pool[pool[u2()]] for _ in range(u2())]
    members = {}
    for kind in ('field', 'method'):
        for _ in range(u2()):
            flags, name, descriptor = u2(), pool[u2()], pool[u2()]
            for _ in range(u2()):
                u2(); size = struct.unpack_from('>I', data, position)[0]; position += 4 + size
            if flags & 0x0005:
                members[(kind, name, descriptor)] = flags
    return dict(access=access, parent=pool[pool[parent]] if parent else None,
                interfaces=interfaces, members=members)


def compare(before, after, name):
    errors = []
    if before['parent'] != after['parent']: errors.append(f'{name}: changed superclass')
    if not set(before['interfaces']) <= set(after['interfaces']): errors.append(f'{name}: removed interface')
    for member, old in before['members'].items():
        new = after['members'].get(member)
        if new is None: errors.append(f'{name}: missing {member}')
        elif ((old & 1 and not new & 1) or (old & 8) != (new & 8)
              or (not old & 16 and new & 16) or (not old & 1024 and new & 1024)):
            errors.append(f'{name}: incompatible access/static/final/abstract {member}')
    return errors


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--baseline', type=Path, required=True)
    parser.add_argument('--candidate', type=Path, default=ROOT / 'dist')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    errors, artifacts = [], []
    for baseline in sorted(args.baseline.rglob('*.jar')):
        candidate = args.candidate / baseline.relative_to(args.baseline)
        assert candidate.exists(), f'Missing {candidate}'
        with zipfile.ZipFile(baseline) as old, zipfile.ZipFile(candidate) as new:
            exported = [n for n in old.namelist() if n.startswith(PREFIX) and n.endswith('.class')
                        and declarations(old.read(n))['access'] & 1]
            for name in exported:
                if name not in new.namelist(): errors.append(f'{candidate.name}: removed {name}'); continue
                errors += compare(declarations(old.read(name)), declarations(new.read(name)), candidate.name + '!' + name)
        artifacts.append(dict(file=str(candidate), exported_classes=len(exported),
            baseline_sha256=hashlib.sha256(baseline.read_bytes()).hexdigest(),
            candidate_sha256=hashlib.sha256(candidate.read_bytes()).hexdigest()))
    assert len(artifacts) == 36, f'Expected all 36 module artifacts, got {len(artifacts)}'
    result = dict(status='failed' if errors else 'passed', artifacts=artifacts, errors=errors,
                  scope='Existing public/protected binary members; examples separately verify source compatibility.')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    assert not errors, '\n'.join(errors)
    print('Public ABI preserved in all 36 module artifacts; additive API contracts permitted.')


if __name__ == '__main__': main()
