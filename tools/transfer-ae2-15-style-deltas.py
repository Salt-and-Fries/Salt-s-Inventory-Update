"""Rebase only the fork's desktop style additions onto mapped, unmodified AE2 15.

Native fields and unchanged method instructions are retained. The output is
staged until the 1.20.1 window/menu consumer and runtime hooks are ported.
"""
import argparse
import copy
import importlib
import json
from pathlib import Path

g = importlib.import_module('generate-ae2-native-deltas')
t = importlib.import_module('transfer-ae2-native-deltas')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--loader', choices=['forge', 'fabric'], required=True)
    parser.add_argument('--namespace', choices=['named', 'runtime'], default='named')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    inventories = g.ROOT / 'build/ae2-port/patch-tools'
    source = g.ROOT / 'compatibility/ae2/versions/1.21.1/neoforge/src/main/resources/ae2-native-patches'
    native = inventories / (f'1.20.1-{args.loader}' + ('-named' if args.namespace == 'named' else ''))
    expected = inventories / f'1.20.1-{args.loader}-desktop-style-{args.namespace}'
    entries = []
    for short_name in ['ScreenStyle', 'StyleManager', 'TerminalStyle']:
        name = 'appeng/client/gui/style/' + short_name
        base = json.loads((inventories / '1.21.1-neoforge' / (name + '.json')).read_text())
        patch = json.loads((source / (name + '.json')).read_text())
        target = json.loads((native / (name + '.json')).read_text())
        integrated = t.apply_to_inventory(base, patch)
        merged = copy.deepcopy(target)
        assert not patch['fieldChanges'] and not patch['removedMethods'] and 'interfaces' not in patch
        for field in patch['fields']:
            if any(f['name'] == field['name'] for f in merged['fields']):
                raise ValueError('Desktop field collides with native AE2: ' + name + '.' + field['name'])
            merged['fields'].append(copy.deepcopy(field))
        for change in patch['changes']:
            key = change['name'], change['desc']
            before = next(m for m in base['methods'] if (m['name'], m['desc']) == key)
            after = next(m for m in integrated['methods'] if (m['name'], m['desc']) == key)
            old = next(m for m in merged['methods'] if (m['name'], m['desc']) == key)
            merged['methods'][merged['methods'].index(old)] = t.merge_method(before, after, old)
        for method in patch['methods']:
            if any((m['name'], m['desc']) == (method['name'], method['desc']) for m in merged['methods']):
                raise ValueError('Desktop method collides with native AE2: ' + name + '.' + method['name'])
            merged['methods'].append(copy.deepcopy(method))
        actual = g.generate(target, merged)
        output = args.output / (name + '.json')
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(g.canonical(actual), encoding='utf-8')
        evidence = expected / (name + '.json')
        evidence.parent.mkdir(parents=True, exist_ok=True)
        evidence.write_text(g.canonical(merged), encoding='utf-8')
        entries.append({'class': name, 'fields': len(actual['fields']), 'methods': len(actual['methods']),
                        'changes': len(actual['changes']), 'scope': 'desktop styles only'})
    manifest = json.loads((source / 'manifest.json').read_text())
    manifest.update(classes=entries, nativeTarget=f'1.20.1-{args.loader}', clientPortComplete=False,
                    nativeNamespace=args.namespace,
                    terminalKeyboardPortComplete=False, pointerPortComplete=False,
                    scope='staged desktop styles; screen/menu consumer and runtime hooks pending')
    (args.output / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    print(f'Transferred {len(entries)} desktop style deltas onto unmodified AE2 15 ({args.loader})')


if __name__ == '__main__':
    main()
