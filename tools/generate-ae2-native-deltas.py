"""Extract integration-only instruction deltas; never distribute a native AE2 class."""
import argparse
import copy
import difflib
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'build/ae2-port/patch-tools/upstream'
FORK = ROOT / 'build/ae2-port/patch-tools/fork'
OUTPUT = ROOT / 'build/ae2-port/native-deltas'
PREFIX = 'com/salts_inventory_update/compat/ae2/'
MAPPINGS = {
    'appeng/client/gui/DesktopChrome': PREFIX + 'nativeview/NativeDesktopChrome',
    'appeng/client/gui/DesktopContentActions': PREFIX + 'nativeview/NativeContentActions',
    'appeng/client/gui/DesktopContentRegistry': PREFIX + 'nativeview/NativeContentRegistry',
    'appeng/client/gui/DesktopSlotPresentation': PREFIX + 'client/DesktopSlotPresentation',
    'appeng/client/gui/me/networktool/ToolboxCompanionScreen': PREFIX + 'nativeview/ToolboxCompanionScreen',
    'appeng/client/gui/style/DesktopFrameEdges': PREFIX + 'nativeview/style/DesktopFrameEdges',
    'appeng/client/gui/style/DesktopLayoutStyle': PREFIX + 'nativeview/style/DesktopLayoutStyle',
    'appeng/client/integration/salts/AEManagementControls': PREFIX + 'nativeview/AEManagementControls',
    'appeng/client/integration/salts/AEWindow': PREFIX + 'nativeview/AEWindow',
    'appeng/client/integration/salts/SaltsClientIntegration': PREFIX + 'nativeview/AE2ClientIntegration',
    'appeng/core/network/clientbound/DesktopMenuUpdatePacket': PREFIX + 'menu/DesktopMenuUpdatePacket',
    'appeng/core/network/clientbound/MenuOpeningDataPacket': PREFIX + 'menu/MenuOpeningDataPacket',
    'appeng/integration/modules/salts/SaltsCommonIntegration': PREFIX + 'server/AE2ServerIntegration',
    'appeng/menu/DesktopMenuPackets': PREFIX + 'menu/DesktopMenuPackets',
    'appeng/menu/MenuOpeningData': PREFIX + 'menu/MenuOpeningData',
    'appeng/menu/MenuPacketScope': PREFIX + 'nativeview/NativeMenuPacketScope',
}


def canonical(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':'))


def session_cpu_state(inventory):
    """Desktop fields use the public session channel, preserving vanilla GuiSync."""
    if inventory.get('class')=='appeng/menu/me/crafting/CraftingCPUMenu':
        for field in inventory['fields']:
            if field['name'] in ['cpuStatusReady','cpuAvailable','hasActiveJob','cpuStatusSerial']:
                for kind in ['visibleAnnotations','invisibleAnnotations']:
                    field[kind]=[a for a in field.get(kind,[]) if a['desc']!='Lappeng/menu/guisync/GuiSync;']
    return inventory


def remap(value):
    if isinstance(value, str):
        for source, target in MAPPINGS.items():
            value = value.replace(source, target).replace(source.replace('/', '.'), target.replace('/', '.'))
        return value
    if isinstance(value, list): return [remap(item) for item in value]
    if isinstance(value, dict): return {key: remap(item) for key, item in value.items()}
    return value


def shape(instruction):
    out = copy.deepcopy(instruction)
    if 'label' in out: out['label'] = '*'
    if 'labels' in out: out['labels'] = ['*'] * len(out['labels'])
    if 'default' in out: out['default'] = '*'
    # Compiler numbers synthetic callbacks globally; compare their original role.
    def simplify(value):
        if isinstance(value, str): return re.sub(r'(lambda\$[^$]+\$)\d+', r'\1*', value)
        if isinstance(value, list): return [simplify(item) for item in value]
        if isinstance(value, dict): return {key:simplify(item) for key,item in value.items()}
        return value
    return canonical(simplify(out))


def align_labels(original, integrated):
    old = [(index, instruction) for index, instruction in enumerate(original) if instruction['type'] not in [8, 14]]
    new = [(index, instruction) for index, instruction in enumerate(integrated) if instruction['type'] not in [8, 14]]
    matches = difflib.SequenceMatcher(None, [shape(value) for _,value in old], [shape(value) for _,value in new], autojunk=False)
    positions = {len(integrated): len(original)}
    for block in matches.get_matching_blocks():
        for offset in range(block.size): positions[new[block.b+offset][0]] = old[block.a+offset][0]
    old_targets = {}
    for index, instruction in enumerate(original):
        if instruction['type'] != 8: continue
        target = next((position for position in range(index+1,len(original)) if original[position]['type'] not in [8,14]), len(original))
        old_targets.setdefault(target, instruction['label'])
    labels = {}
    used = set()
    for index, instruction in enumerate(integrated):
        if instruction['type'] != 8: continue
        target = next((position for position in range(index+1,len(integrated)) if integrated[position]['type'] not in [8,14]), len(integrated))
        equivalent = old_targets.get(positions.get(target))
        if equivalent is not None and equivalent not in used:
            labels[instruction['label']] = equivalent; used.add(equivalent)
        else: labels[instruction['label']] = 'n' + str(len(labels))
    def translate(value):
        if isinstance(value, str): return labels.get(value, value)
        if isinstance(value, list): return [translate(item) for item in value]
        if isinstance(value, dict): return {key:translate(item) for key,item in value.items()}
        return value
    return translate(integrated), translate


def generate(original, integrated):
    patch = {'class': original['class'], 'fields':[], 'fieldChanges':[], 'methods':[], 'changes':[], 'removedMethods':[]}
    if original.get('superName') != integrated.get('superName'):
        raise ValueError('Changed native superclass: ' + original['class'])
    if original.get('interfaces', []) != integrated.get('interfaces', []):
        before, after = original.get('interfaces', []), integrated.get('interfaces', [])
        if any(x not in after for x in before): raise ValueError('Removed native interface: ' + original['class'])
        patch['interfaces'] = {'original': before, 'added': remap([x for x in after if x not in before])}
    fields = {field['name']:field for field in original['fields']}
    for field in integrated['fields']:
        if field['name'] not in fields: patch['fields'].append(remap(field))
        elif field != fields[field['name']]:
            before = fields[field['name']]
            if any(field.get(key) != before.get(key) for key in ['name','desc','signature','value']):
                raise ValueError('Changed native field definition: '+original['class']+'.'+field['name'])
            patch['fieldChanges'].append({'name':field['name'],'desc':field['desc'],'originalAccess':before['access'],'access':field['access'],
                                         'visibleAnnotations':remap(field.get('visibleAnnotations',[])),
                                         'invisibleAnnotations':remap(field.get('invisibleAnnotations',[]))})
    old = {(method['name'],method['desc']):method for method in original['methods']}
    remaining = set(old)
    for method in integrated['methods']:
        validate_labels(method)
        key = method['name'],method['desc']
        source_key = key
        if key not in old and method['name'].startswith('lambda$'):
            stem = method['name'].rsplit('$',1)[0]
            candidates = [candidate for candidate in remaining if candidate[0].rsplit('$',1)[0] == stem and candidate[1] == method['desc']]
            if candidates:
                def score(candidate):
                    return difflib.SequenceMatcher(None,[shape(value) for value in old[candidate]['instructions']], [shape(value) for value in method['instructions']],autojunk=False).ratio()
                source_key = max(candidates,key=score)
        if source_key not in old:
            patch['methods'].append(remap(method)); continue
        remaining.discard(source_key)
        before = old[source_key]
        after = remap(method)
        instructions, translate = align_labels(before['instructions'], after['instructions'])
        if (instructions == before['instructions'] and source_key == key and before['handlers'] == after['handlers'] and before['access'] == after['access']
                and all(before.get(kind,[]) == after.get(kind,[]) for kind in ['visibleAnnotations','invisibleAnnotations'])): continue
        edits = []
        matcher = difflib.SequenceMatcher(None,[canonical(value) for value in before['instructions']], [canonical(value) for value in instructions],autojunk=False)
        for operation,start,end,new_start,new_end in matcher.get_opcodes():
            if operation != 'equal': edits.append({'from':start,'to':end,'insert':instructions[new_start:new_end]})
        change = {'name':source_key[0],'desc':source_key[1],
                  'original':hashlib.sha256(canonical(before['instructions']).encode()).hexdigest(),
                  'edits':edits,'handlers':translate(after['handlers']),'maxStack':after['maxStack'],'maxLocals':after['maxLocals'],'access':after['access'],
                  'visibleAnnotations':after.get('visibleAnnotations',[]),'invisibleAnnotations':after.get('invisibleAnnotations',[])}
        if source_key != key: change['rename'] = key[0]
        patch['changes'].append(change)
    for key in sorted(remaining):
        patch['removedMethods'].append({'name':key[0],'desc':key[1],
            'original':hashlib.sha256(canonical(old[key]['instructions']).encode()).hexdigest()})
    return patch


def validate_labels(method):
    definitions = [x['label'] for x in method['instructions'] if x['type'] == 8]
    if len(definitions) != len(set(definitions)):
        raise ValueError('Duplicate branch label in ' + method['name'])
    references = set()
    def frame_labels(value):
        if isinstance(value, dict):
            if 'label' in value: references.add(value['label'])
            for x in value.values(): frame_labels(x)
        elif isinstance(value, list):
            for x in value: frame_labels(x)
    for node in method['instructions']:
        if node['type'] == 7: references.add(node['label'])
        elif node['type'] in [11, 12]:
            references.add(node['default']); references.update(node['labels'])
        elif node['type'] == 14:
            frame_labels(node['locals']); frame_labels(node['stack'])
    for handler in method['handlers']:
        references.update(handler[x] for x in ['start', 'end', 'handler'])
    missing = references - set(definitions)
    if missing: raise ValueError('Undefined branch labels in ' + method['name'] + ': ' + ', '.join(sorted(missing)))


def main():
    OUTPUT.mkdir(parents=True,exist_ok=True)
    inventory = json.loads((ROOT/'build/ae2-port/integration-source-inventory.json').read_text())
    summary = []
    for entry in inventory['java_changes']:
        if entry['test_instrumentation'] or entry['status'] != 'M' or '/datagen/' in entry['path'] or entry['path'].endswith('/AppEngClient.java'): continue
        relative = entry['path'].split('/java/',1)[1].removesuffix('.java')
        candidates = [FORK/(relative+'.json'), *sorted((FORK/relative).parent.glob(Path(relative).name+'$*.json'))]
        for candidate in candidates:
            native = BASE/candidate.relative_to(FORK)
            if not native.exists():
                summary.append({'class':candidate.relative_to(FORK).as_posix(),'newClass':True}); continue
            patch = generate(json.loads(native.read_text()),session_cpu_state(json.loads(candidate.read_text())))
            if not any(patch[kind] for kind in ['fields','fieldChanges','methods','changes','removedMethods']) and 'interfaces' not in patch: continue
            target = OUTPUT/candidate.relative_to(FORK); target.parent.mkdir(parents=True,exist_ok=True)
            target.write_text(canonical(patch),encoding='utf-8')
            summary.append({'class':patch['class'],'fields':len(patch['fields']),'methods':len(patch['methods']),'changes':len(patch['changes'])})
    (OUTPUT/'manifest.json').write_text(json.dumps({'mappings':MAPPINGS,'classes':summary},indent=2)+'\n',encoding='utf-8')
    print('Extracted',len(summary),'native integration deltas')
    print(json.dumps(summary,indent=2))


if __name__ == '__main__': main()
