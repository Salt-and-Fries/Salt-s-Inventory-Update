"""Carry reviewed integration edits onto another native AE2 ABI.

This is a three-way instruction merge, not a copy of native implementation.
Unchanged target instructions remain owned by the installed AE2 JAR. Any
overlapping upstream/integration edit fails closed for manual review.
"""
import argparse
import copy
import difflib
import importlib
import json
from pathlib import Path

g = importlib.import_module('generate-ae2-native-deltas')


def remap_pipeline(value):
    if isinstance(value, str):
        return value.replace('com/mojang/renderpearl/api/pipeline/RenderPipeline',
                             'com/mojang/blaze3d/pipeline/RenderPipeline')
    if isinstance(value, list): return [remap_pipeline(x) for x in value]
    if isinstance(value, dict): return {k: remap_pipeline(v) for k, v in value.items()}
    return value


def operations(before, after):
    return difflib.SequenceMatcher(None, [g.canonical(x) for x in before],
                                  [g.canonical(x) for x in after], autojunk=False).get_opcodes()


def merge_method(base, integration, target):
    old = base['instructions']
    integrated, translate_fork = g.align_labels(old, integration['instructions'])
    native, translate_target = g.align_labels(old, target['instructions'])
    # Both independent diffs allocate n0, n1, ... for newly introduced labels.
    # Native branch additions must not alias unrelated integration branches.
    original_labels = {x['label'] for x in old if x['type'] == 8}
    native_labels = {x['label']: 'native_' + x['label'] for x in native
                     if x['type'] == 8 and x['label'] not in original_labels}
    def namespace_native(value):
        if isinstance(value, str): return native_labels.get(value, value)
        if isinstance(value, list): return [namespace_native(x) for x in value]
        if isinstance(value, dict): return {k: namespace_native(v) for k, v in value.items()}
        return value
    native = namespace_native(native)
    edits = operations(old, integrated)
    # A boundary can have two positions around a new insertion. Native edits
    # must start after the insertion and end before the next insertion.
    starts, ends = {}, {}
    for op, a, b, c, d in edits:
        if op == 'equal':
            for offset in range(b-a+1):
                starts.setdefault(a+offset, c+offset)
                ends[a+offset] = c+offset
        elif op == 'insert':
            starts[a] = d
            ends[a] = c
    changes = []
    for op, a, b, c, d in operations(old, native):
        if op == 'equal': continue
        if any(kind != 'equal' and kind != 'insert' and max(a, x) < min(b, y)
               for kind, x, y, _, _ in edits):
            raise ValueError(f'Overlapping native/integration edit {base["name"]}: {a}:{b}')
        if a not in starts or b not in ends:
            raise ValueError(f'Unmapped native edit {base["name"]}: {a}:{b}')
        changes.append((starts[a], ends[b], native[c:d]))
    result = copy.deepcopy(integration)
    result['instructions'] = integrated
    for a, b, insert in reversed(changes): result['instructions'][a:b] = insert
    native_handlers = namespace_native(translate_target(target['handlers']))
    fork_handlers = translate_fork(integration['handlers'])
    if base['handlers'] == fork_handlers:
        result['handlers'] = native_handlers
    elif native_handlers == base['handlers'] or native_handlers == fork_handlers:
        result['handlers'] = fork_handlers
    else:
        raise ValueError('Overlapping exception-handler edits: ' + base['name'])
    result['maxStack'] = max(target['maxStack'], integration['maxStack'])
    result['maxLocals'] = max(target['maxLocals'], integration['maxLocals'])
    return result


def apply_to_inventory(base, patch):
    result = copy.deepcopy(base)
    if 'interfaces' in patch: result['interfaces'] = result.get('interfaces', []) + patch['interfaces']['added']
    result['fields'].extend(patch['fields'])
    for change in patch['fieldChanges']:
        field = next(x for x in result['fields'] if x['name'] == change['name'])
        field['access'] = change['access']
        for kind in ['visibleAnnotations','invisibleAnnotations']:
            if kind in change: field[kind] = change[kind]
    for change in patch['changes']:
        method = next(x for x in result['methods'] if (x['name'], x['desc']) == (change['name'], change['desc']))
        for edit in reversed(change['edits']):
            method['instructions'][edit['from']:edit['to']] = edit['insert']
        for key in ['handlers', 'maxStack', 'maxLocals', 'access', 'visibleAnnotations', 'invisibleAnnotations']:
            if key in change: method[key] = change[key]
        if 'rename' in change: method['name'] = change['rename']
    for removed in patch['removedMethods']:
        result['methods'] = [x for x in result['methods'] if (x['name'], x['desc']) != (removed['name'], removed['desc'])]
    result['methods'].extend(patch['methods'])
    return result


def adapt_added_method(method, target):
    """Translate only newly added integration code to the older Minecraft ABI."""
    method = copy.deepcopy(method)
    instructions = method['instructions']
    for index in range(len(instructions)-1, -1, -1):
        instruction = instructions[index]
        if instruction.get('owner') == 'net/minecraft/client/input/MouseButtonEvent' and instruction.get('name') == 'button':
            following = next((x for x in instructions[index+1:] if x['type'] not in [8,14]), None)
            if following and following['type'] == 0 and following['op'] in [4,6]:
                # SDL left/right 1/3 became GLFW left/right 0/1.
                following['op'] = {4:3, 6:4}[following['op']]
        if instruction.get('owner') == 'net/minecraft/client/input/KeyEvent' and instruction.get('name') == 'key':
            following = next((x for x in instructions[index+1:] if x['type'] not in [8,14]), None)
            if following and following.get('operand') == 43:
                following['operand'] = 258; following['op'] = 17  # GLFW Tab
        if instruction.get('owner') == 'com/mojang/blaze3d/platform/InputConstants' and instruction.get('name') == 'isKeyDown' and instruction['desc'] == '(I)Z':
            key_index = next(i for i in range(index-1,-1,-1) if instructions[i]['type'] not in [8,14])
            if instructions[key_index].get('operand') != 44: raise ValueError('Unreviewed SDL key in integration method')
            instruction['desc'] = '(Lcom/mojang/blaze3d/platform/Window;I)Z'
            instructions[key_index:key_index+1] = [
                {'type':5,'op':184,'owner':'net/minecraft/client/Minecraft','name':'getInstance','desc':'()Lnet/minecraft/client/Minecraft;','interface':False},
                {'type':5,'op':182,'owner':'net/minecraft/client/Minecraft','name':'getWindow','desc':'()Lcom/mojang/blaze3d/platform/Window;','interface':False},
                {'type':1,'op':16,'operand':32}]
            method['maxStack'] += 1
        if target == '26.1.2-neoforge' and instruction.get('owner') == 'net/minecraft/client/gui/Gui' and instruction.get('name') == 'screen':
            gui_index = next(i for i in range(index-1,-1,-1) if instructions[i]['type'] not in [8,14])
            if instructions[gui_index].get('name') != 'gui': raise ValueError('Unexpected native GUI screen access')
            instructions.pop(gui_index)
            instruction.clear(); instruction.update({'type':4,'op':180,'owner':'net/minecraft/client/Minecraft','name':'screen','desc':'Lnet/minecraft/client/gui/screens/Screen;'})
    return method


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--target', required=True, choices=['26.2-neoforge', '26.1.2-neoforge'])
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    source = g.ROOT / 'compatibility/ae2/versions/26.3/neoforge/src/main/resources/ae2-native-patches'
    native = g.ROOT / 'build/ae2-port/patch-tools' / args.target
    manifest = json.loads((source/'manifest.json').read_text())
    changed_native = 0
    expected = g.ROOT / 'build/ae2-port/patch-tools' / (args.target+'-desktop')
    for entry in manifest['classes']:
        name = entry['class']
        base = remap_pipeline(json.loads((g.BASE/(name+'.json')).read_text()))
        patch = remap_pipeline(json.loads((source/(name+'.json')).read_text()))
        target = json.loads((native/(name+'.json')).read_text())
        merged = copy.deepcopy(target)
        integrated = apply_to_inventory(base, patch)
        if 'interfaces' in patch:
            if merged.get('interfaces', []) != patch['interfaces']['original']: raise ValueError('Native interfaces differ: ' + name)
            merged['interfaces'] = merged.get('interfaces', []) + patch['interfaces']['added']
        merged['fields'].extend(patch['fields'])
        for change in patch['fieldChanges']:
            field = next(x for x in merged['fields'] if x['name'] == change['name'])
            if field['access'] != change['originalAccess']: raise ValueError('Native field access differs: '+name)
            field['access'] = change['access']
            for kind in ['visibleAnnotations','invisibleAnnotations']:
                if kind in change: field[kind] = change[kind]
        for change in patch['changes']:
            key = change['name'], change['desc']
            before = next(x for x in base['methods'] if (x['name'], x['desc']) == key)
            old = next(x for x in merged['methods'] if (x['name'], x['desc']) == key)
            after = next(x for x in integrated['methods'] if (x['name'], x['desc']) == (change.get('rename', key[0]), key[1]))
            result = merge_method(before, after, old)
            if before['instructions'] != old['instructions']: changed_native += 1
            merged['methods'][merged['methods'].index(old)] = result
        for removed in patch['removedMethods']:
            merged['methods'] = [x for x in merged['methods'] if (x['name'], x['desc']) != (removed['name'], removed['desc'])]
        for method in patch['methods']:
            method = adapt_added_method(method, args.target)
            if any((x['name'],x['desc']) == (method['name'],method['desc']) for x in merged['methods']):
                raise ValueError('Added method collides with native method: '+name+'.'+method['name'])
            merged['methods'].append(method)
        output = args.output/(name+'.json'); output.parent.mkdir(parents=True, exist_ok=True)
        g.session_cpu_state(merged)
        output.write_text(g.canonical(g.generate(target, merged)), encoding='utf-8')
        evidence = expected/(name+'.json'); evidence.parent.mkdir(parents=True, exist_ok=True)
        evidence.write_text(g.canonical(merged), encoding='utf-8')
    manifest['nativeTarget'] = args.target
    manifest['nativeDifferencesPreserved'] = changed_native
    (args.output/'manifest.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
    print(f'{args.target}: transferred {len(manifest["classes"])} integration deltas; preserved {changed_native} changed native methods')


if __name__ == '__main__': main()
