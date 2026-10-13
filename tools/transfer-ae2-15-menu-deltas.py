"""Transfer fork-owned menu lifetime/slot/action additions onto unmodified AE2 15.

The native action serializer and ordinary transports remain intact. Runtime
member names are derived from paired raw/mapped upstream inventories, never
guessed from a newer Minecraft version.
"""
import argparse
import copy
import importlib
import json
import re
import struct
import zipfile
from pathlib import Path

g = importlib.import_module('generate-ae2-native-deltas')
t = importlib.import_module('transfer-ae2-native-deltas')
MENU = 'appeng/menu/AEBaseMenu'
PACKET = 'appeng/core/sync/BasePacket'
TRANSPORT = 'com/salts_inventory_update/compat/ae2/menu/DesktopNativeTransport'
BUILDER = 'appeng/menu/implementations/MenuTypeBuilder'
SCOPE = 'com/salts_inventory_update/compat/ae2/menu/DesktopMenuConstructionScope'
_runtime_mappers = {}


def runtime_mapping_result(inventory, raw_folder, original, mapped):
    if original is not None:
        raw = json.loads((raw_folder / (original['class'] + '.json')).read_text(encoding='utf-8'))
        proof = g.generate(raw, mapped(original))
        if any(proof[key] for key in ['fields', 'fieldChanges', 'methods', 'changes', 'removedMethods']) or 'interfaces' in proof:
            Path('build/ae2-port/runtime-mapping-error.json').write_text(json.dumps(proof,indent=2)+'\n',encoding='utf-8')
            raise ValueError('Runtime remapping changes unmodified native code: ' + original['class'])
    return mapped(inventory)


def replace(value, old, new):
    if isinstance(value, str): return value.replace(old, new)
    if isinstance(value, list): return [replace(x, old, new) for x in value]
    if isinstance(value, dict): return {k: replace(v, old, new) for k, v in value.items()}
    return value


def packet_types(value):
    for old in ['appeng/core/network/ServerboundPacket', 'appeng/core/network/ClientboundPacket']:
        value = replace(value, old, PACKET)
    return value


def adapt_added(method):
    method = packet_types(copy.deepcopy(method))
    nodes = method['instructions']
    for i in range(len(nodes) - 1, -1, -1):
        n = nodes[i]
        if n.get('owner') == 'net/neoforged/neoforge/network/PacketDistributor':
            assert nodes[i - 2]['op'] == 3 and nodes[i - 1].get('desc') == 'net/minecraft/network/protocol/common/custom/CustomPacketPayload'
            del nodes[i - 2:i]
            n.update(owner=TRANSPORT, name='sendDefault', desc=f'(L{PACKET};)V')
        elif n.get('owner') == PACKET and n.get('name') == 'type':
            assert nodes[i + 1].get('name') == 'id'
            del nodes[i + 1]
            n.update(op=184, owner='com/salts_inventory_update/compat/ae2/menu/DesktopMenuPackets', name='channel',
                     desc=f'(L{PACKET};)Lnet/minecraft/resources/ResourceLocation;', interface=False)
        elif n.get('owner') == MENU and n.get('name') == 'registryAccess':
            nodes[i:i + 1] = [{'type': 0, 'op': 87}, {'type': 0, 'op': 1}]
    return method


def route_client_action(method):
    result = copy.deepcopy(method)
    calls = 0
    for node in result['instructions']:
        if node.get('owner') == 'appeng/core/sync/network/NetworkHandler' and node.get('name') == 'instance':
            node.clear(); node.update(type=2, op=25, var=0)
        elif node.get('owner') == 'appeng/core/sync/network/NetworkHandler' and node.get('name') == 'sendToServer':
            node.update(op=182, owner=MENU, name='sendPacketToServer', interface=False)
            calls += 1
    if calls != 1: raise ValueError('Unreviewed native client action transport')
    return result


def builder_contract(target, source_patch, capture_menu=False):
    result = copy.deepcopy(target)
    result['interfaces'].append('com/salts_inventory_update/compat/ae2/menu/NativeDesktopBuilder')
    writer = next(m for m in source_patch['methods'] if m['name'] == 'writeDesktopOpeningData')
    writer = replace(writer, 'net/minecraft/network/RegistryFriendlyByteBuf', 'net/minecraft/network/FriendlyByteBuf')
    writer = replace(writer, 'appeng/menu/locator/MenuHostLocator', 'appeng/menu/locator/MenuLocator')
    for node in writer['instructions']:
        if node.get('owner') == 'net/minecraft/network/FriendlyByteBuf' and node.get('name') == 'writeBoolean':
            node['desc'] = '(Z)Lio/netty/buffer/ByteBuf;'
    result['methods'].append(writer)
    bridge = copy.deepcopy(next(m for m in source_patch['methods'] if m['name'] == 'createDesktopMenu'))
    bridge['desc'] = '(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/FriendlyByteBuf;)Lappeng/menu/AEBaseMenu;'
    bridge['maxStack'], bridge['maxLocals'] = 4, 7
    bridge['instructions'] = [
        {'type': 5, 'op': 184, 'owner': SCOPE, 'name': 'begin', 'desc': '()I', 'interface': False},
        {'type': 2, 'op': 54, 'var': 4}, {'type': 8, 'op': -1, 'label': 'construct_start'},
        {'type': 2, 'op': 25, 'var': 0}, {'type': 2, 'op': 21, 'var': 1},
        {'type': 2, 'op': 25, 'var': 2}, {'type': 2, 'op': 25, 'var': 3},
        {'type': 5, 'op': 183, 'owner': BUILDER, 'name': 'fromNetwork', 'desc': bridge['desc'], 'interface': False},
        {'type': 2, 'op': 58, 'var': 5}, {'type': 8, 'op': -1, 'label': 'construct_end'},
        {'type': 2, 'op': 21, 'var': 4},
        {'type': 5, 'op': 184, 'owner': SCOPE, 'name': 'restore', 'desc': '(I)V', 'interface': False},
        {'type': 2, 'op': 25, 'var': 5}, {'type': 0, 'op': 176},
        {'type': 8, 'op': -1, 'label': 'construct_failed'},
        {'type': 14, 'op': -1, 'frameType': -1, 'locals': [{'kind': 'string', 'value': BUILDER}, {'kind': 'Integer', 'value': 1},
            {'kind': 'string', 'value': 'net/minecraft/world/entity/player/Inventory'},
            {'kind': 'string', 'value': 'net/minecraft/network/FriendlyByteBuf'}, {'kind': 'Integer', 'value': 1}],
            'stack': [{'kind': 'string', 'value': 'java/lang/Throwable'}]},
        {'type': 2, 'op': 58, 'var': 6}, {'type': 2, 'op': 21, 'var': 4},
        {'type': 5, 'op': 184, 'owner': SCOPE, 'name': 'restore', 'desc': '(I)V', 'interface': False},
        {'type': 2, 'op': 25, 'var': 6}, {'type': 0, 'op': 191}]
    bridge['handlers'] = [{'start': 'construct_start', 'end': 'construct_end', 'handler': 'construct_failed', 'type': None}]
    result['methods'].append(bridge)
    native = next(m for m in result['methods'] if m['name'] == 'fromNetwork')
    nodes = native['instructions']
    start = next(i for i, n in enumerate(nodes) if n.get('owner') == 'net/minecraft/client/Minecraft' and n.get('name') == 'getInstance')
    closed = next(i for i, n in enumerate(nodes) if n.get('owner') == 'net/minecraft/client/multiplayer/ClientPacketListener' and n.get('name') == 'send')
    end_label = next(n['label'] for n in nodes[closed + 1:] if n['type'] == 8)
    connection_var = next(n['var'] for n in nodes[start:] if n['type'] == 2 and n['op'] == 58)
    nodes[start:start] = [{'type': 0, 'op': 1}, {'type': 2, 'op': 58, 'var': connection_var},
        {'type': 5, 'op': 184, 'owner': SCOPE, 'name': 'detached', 'desc': '()Z', 'interface': False},
        {'type': 7, 'op': 154, 'label': end_label}]
    if capture_menu:
        factory=next((m for m in result['methods'] if m['name']=='lambda$open$1'),None)
        # Forge uses this lambda; Fabric delegates to HandlerFactory.createMenu.
        if factory is not None:
            capture_created_menu(factory)
            opener=next(m for m in result['methods'] if m['name']=='open')
            calls=[n for n in opener['instructions'] if n.get('owner')=='net/minecraftforge/network/NetworkHooks' and n.get('name')=='openScreen']
            if len(calls)!=1:raise ValueError('Unreviewed native Forge menu opener')
            calls[0]['owner']='com/salts_inventory_update/compat/ae2/menu/LegacyNativeOpen'
    return result


def capture_created_menu(factory):
    code=factory['instructions'];returns=[i for i,n in enumerate(code) if n['op']==176]
    if len(returns)!=1:raise ValueError('Unreviewed native server menu factory return')
    code[returns[0]:returns[0]]=[{'type':0,'op':89},{'type':3,'op':192,'desc':MENU},
        {'type':5,'op':184,'owner':'appeng/menu/MenuOpener','name':'menuCreated','desc':f'(L{MENU};)V','interface':False}]
    factory['maxStack']=max(2,factory['maxStack'])


def class_parents(data):
    cp, pos, index = [None] * struct.unpack_from('>H',data,8)[0], 10, 1
    while index < len(cp):
        tag = data[pos]; pos += 1
        if tag == 1:
            length = struct.unpack_from('>H',data,pos)[0];pos += 2;cp[index] = data[pos:pos+length].decode('utf-8',errors='replace');pos += length
        elif tag == 7: cp[index] = struct.unpack_from('>H',data,pos)[0];pos += 2
        elif tag in [3,4,9,10,11,12,17,18]: pos += 4
        elif tag in [5,6]: pos += 8;index += 1
        elif tag in [8,16,19,20]: pos += 2
        elif tag == 15: pos += 3
        else: raise ValueError('Unknown class-file constant tag: ' + str(tag))
        index += 1
    access, own, parent, count = struct.unpack_from('>HHHH',data,pos);pos += 8
    parents = ([cp[cp[parent]]] if parent else []) + [cp[cp[struct.unpack_from('>H',data,pos+2*i)[0]]] for i in range(count)]
    return cp[cp[own]], parents


def remap_runtime(inventory, raw_folder, named_folder, original=None, minecraft_mappings=None, minecraft_jar=None):
    # A screen transfer maps dozens of classes against the same inputs. Reuse
    # only the mapping tables; each class still proves native round-trip identity.
    def fingerprint(path):
        if path is None:return None
        path=Path(path).resolve()
        files=sorted(path.rglob('*.json')) if path.is_dir() else [path]
        return str(path),tuple((str(p),p.stat().st_size,p.stat().st_mtime_ns) for p in files)
    key=tuple(fingerprint(p) for p in [raw_folder,named_folder,minecraft_mappings,minecraft_jar])
    if key in _runtime_mappers:
        return runtime_mapping_result(inventory,raw_folder,original,_runtime_mappers[key])
    classes, members, handle_tags, dynamic_names = {}, {}, {}, {}
    dynamic_interfaces = {}
    hierarchy = {}
    fallback = {}
    def class_strings(a, b):
        if isinstance(a, str) and isinstance(b, str):
            if a.startswith('net/minecraft/') and b.startswith('net/minecraft/'): classes[b] = a
            old = re.findall(r'L(net/minecraft/[^;]+);', a)
            new = re.findall(r'L(net/minecraft/[^;]+);', b)
            if len(old) == len(new):
                for x, y in zip(old, new): classes[y] = x
        elif isinstance(a, list) and isinstance(b, list) and len(a) == len(b):
            for x, y in zip(a, b): class_strings(x, y)
        elif isinstance(a, dict) and isinstance(b, dict):
            if a.get('kind') == b.get('kind') == 'handle':
                handle_tags[(b['owner'], b['name'], b['desc'], b['tag'])] = a['tag']
            if a.get('type') == b.get('type') == 6:
                dynamic_names[(b['name'], b['desc'])] = a['name']
                dynamic_interfaces.setdefault((b['name'], b['desc'].split(')')[-1]), set()).add(a['name'])
            for k in a.keys() & b.keys(): class_strings(a[k], b[k])
    def member(owner, a, b):
        if not all(k in a and k in b for k in ['name', 'desc']): return
        members[(owner, b['name'], b['desc'])] = (a['name'], a['desc'])
        if re.fullmatch(r'(?:m_\d+_|f_\d+_|method_\d+|field_\d+)', a['name']):
            fallback.setdefault((b['name'], b['desc']), set()).add((a['name'], a['desc']))
    for p in named_folder.rglob('*.json'):
        q = raw_folder / p.relative_to(named_folder)
        if not q.exists(): continue
        raw, named = json.loads(q.read_text(encoding='utf-8')), json.loads(p.read_text(encoding='utf-8'))
        hierarchy[named['class']] = ([named['superName']] if named.get('superName') else []) + named.get('interfaces',[])
        class_strings(raw, named)
        for group in ['fields', 'methods']:
            if len(raw[group]) != len(named[group]): raise ValueError('Mapped member count differs: ' + raw['class'])
            for a, b in zip(raw[group], named[group]):
                member(named['class'], a, b)
                if group == 'methods':
                    if len(a['instructions']) != len(b['instructions']): raise ValueError('Mapped instruction count differs: ' + raw['class'] + '.' + a['name'])
                    for x, y in zip(a['instructions'], b['instructions']):
                        if 'owner' in x and 'owner' in y: member(y['owner'], x, y)
    if minecraft_mappings is not None:
        lines = Path(minecraft_mappings).read_text(encoding='utf-8').splitlines()
        if lines[0].startswith('tiny\t2\t'):
            namespaces = lines[0].split('\t')[3:]; raw_index = namespaces.index('intermediary'); named_index = namespaces.index('named')
            official_named, official_raw = {}, {}
            for line in lines[1:]:
                parts = line.split('\t')
                if parts[0] == 'c':
                    names = parts[1:]; official_named[names[0]] = names[named_index]; official_raw[names[0]] = names[raw_index]
                    classes[names[named_index]] = names[raw_index]
            def descriptor(v, names): return re.sub(r'L([^;]+);', lambda m: 'L' + names.get(m[1], m[1]) + ';', v)
            owner = None
            for line in lines[1:]:
                parts = line.split('\t')
                if parts[0] == 'c': owner = parts[1 + named_index]
                elif len(parts) > 4 and parts[0] == '' and parts[1] in ['m','f']:
                    desc, names = parts[2], parts[3:]
                    members[(owner, names[named_index], descriptor(desc, official_named))] = (names[raw_index], descriptor(desc, official_raw))
        else:
            for line in lines:
                parts = line.split()
                if not parts: continue
                if parts[0] == 'CL:': classes[parts[2]] = parts[1]
                elif parts[0] == 'MD:':
                    raw_owner, raw_name = parts[1].rsplit('/',1); owner, name = parts[3].rsplit('/',1)
                    members[(owner,name,parts[4])] = (raw_name,parts[2])
                elif parts[0] == 'FD:' and '#' in parts:
                    raw_owner, raw_name = parts[1].rsplit('/',1); owner, name = parts[2].rsplit('/',1)
                    members[(owner,name,parts[5])] = (raw_name,parts[4])
    if minecraft_jar is not None:
        with zipfile.ZipFile(minecraft_jar) as jar:
            for name in jar.namelist():
                if name.endswith('.class'):
                    owner, parents = class_parents(jar.read(name));hierarchy[owner] = parents
    def inherited(owner,name,desc,seen=None):
        seen = set() if seen is None else seen
        if owner in seen: return None
        seen.add(owner)
        pair = members.get((owner,name,desc))
        if pair is not None: return pair
        for parent in hierarchy.get(owner,[]):
            pair = inherited(parent,name,desc,seen)
            if pair is not None: return pair
        # The named-symbol gate has already validated these inherited JDK
        # methods; their names stay stable in every Minecraft namespace.
        if owner.startswith('java/'): return (name,string(desc))
        return None
    def string(v):
        if v in classes: return classes[v]
        return re.sub(r'L(net/minecraft/[^;]+);', lambda m: 'L' + classes.get(m[1], m[1]) + ';', v)
    def value(v):
        if isinstance(v, str): return string(v)
        if isinstance(v, list): return [value(x) for x in v]
        if not isinstance(v, dict): return v
        result = {k: value(x) for k, x in v.items()}
        if v.get('type') == 6:
            name = dynamic_names.get((v['name'], v['desc']))
            candidates = dynamic_interfaces.get((v['name'], v['desc'].split(')')[-1]), set())
            if name is not None: result['name'] = name
            elif len(candidates) == 1: result['name'] = next(iter(candidates))
        if all(k in v for k in ['owner', 'name', 'desc']):
            pair = inherited(v['owner'], v['name'], v['desc'])
            if pair is None:
                candidates = fallback.get((v['name'], v['desc']), set())
                if len(candidates) == 1: pair = next(iter(candidates))
                elif v['owner'].startswith('net/minecraft/') and v['name'] != '<init>':
                    raise ValueError('Runtime member mapping is ambiguous or absent: ' + str(v))
            if pair is not None: result['name'], result['desc'] = pair
            if v.get('kind') == 'handle':
                result['tag'] = handle_tags.get((v['owner'], v['name'], v['desc'], v['tag']), v['tag'])
        return result
    def mapped(source):
        result = value(source)
        for group in ['fields', 'methods']:
            for old, new in zip(source[group], result[group]):
                pair = members.get((source['class'], old['name'], old['desc']))
                if pair is not None: new['name'], new['desc'] = pair
        return result
    _runtime_mappers[key]=mapped
    return runtime_mapping_result(inventory,raw_folder,original,mapped)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--loader', choices=['fabric', 'forge'], required=True)
    parser.add_argument('--namespace', choices=['named', 'runtime'], default='named')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--menu-only', action='store_true', help='Keep separately staged native builder capture unchanged')
    args = parser.parse_args()
    inventories = g.ROOT / 'build/ae2-port/patch-tools'
    source = g.ROOT / 'compatibility/ae2/versions/1.21.1/neoforge/src/main/resources/ae2-native-patches'
    base = json.loads((inventories / '1.21.1-neoforge' / (MENU + '.json')).read_text(encoding='utf-8'))
    patch = json.loads((source / (MENU + '.json')).read_text(encoding='utf-8'))
    integrated = t.apply_to_inventory(base, patch)
    target = json.loads((inventories / f'1.20.1-{args.loader}-named' / (MENU + '.json')).read_text(encoding='utf-8'))
    merged = copy.deepcopy(target)
    assert not patch['fieldChanges'] and not patch['removedMethods']
    merged['fields'].extend(copy.deepcopy(patch['fields']))
    for change in patch['changes']:
        name, desc = change['name'], packet_types(change['desc'])
        old = next(m for m in merged['methods'] if (m['name'], m['desc']) == (name, desc))
        if name == 'sendPacketToClient':
            result = copy.deepcopy(old)
            prefix = [
                {'type': 2, 'op': 25, 'var': 0},
                {'type': 4, 'op': 180, 'owner': MENU, 'name': 'desktopSyncId', 'desc': 'Ljava/util/UUID;'},
                {'type': 7, 'op': 198, 'label': 'desktop_native_transport'},
                {'type': 2, 'op': 25, 'var': 0}, {'type': 2, 'op': 25, 'var': 0},
                {'type': 4, 'op': 180, 'owner': MENU, 'name': 'desktopSyncId', 'desc': 'Ljava/util/UUID;'},
                {'type': 2, 'op': 25, 'var': 1},
                {'type': 5, 'op': 184, 'owner': TRANSPORT, 'name': 'sendUpdate', 'desc': f'(L{MENU};Ljava/util/UUID;L{PACKET};)V', 'interface': False},
                {'type': 0, 'op': 177}, {'type': 8, 'op': -1, 'label': 'desktop_native_transport'},
                {'type': 14, 'op': -1, 'frameType': -1, 'locals': [{'kind': 'string', 'value': MENU}, {'kind': 'string', 'value': PACKET}], 'stack': []}]
            result['instructions'] = prefix + result['instructions']
            result['maxStack'] = max(3, result['maxStack'])
        elif name == 'sendClientAction':
            result = route_client_action(old)
        else:
            before = next(m for m in base['methods'] if (m['name'], m['desc']) == (name, change['desc']))
            after = next(m for m in integrated['methods'] if (m['name'], m['desc']) == (name, change['desc']))
            result = t.merge_method(before, after, old)
        merged['methods'][merged['methods'].index(old)] = result
    merged['methods'].extend(adapt_added(m) for m in patch['methods'])
    if args.namespace == 'runtime':
        original_named = target
        target = json.loads((inventories / f'1.20.1-{args.loader}' / (MENU + '.json')).read_text(encoding='utf-8'))
        merged = remap_runtime(merged, inventories / f'1.20.1-{args.loader}', inventories / f'1.20.1-{args.loader}-named', original_named)
    actual = g.generate(target, merged)
    output = args.output / (MENU + '.json'); output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(g.canonical(actual), encoding='utf-8')
    expected = inventories / f'1.20.1-{args.loader}-desktop-style-{args.namespace}' / (MENU + '.json')
    expected.parent.mkdir(parents=True, exist_ok=True); expected.write_text(g.canonical(merged), encoding='utf-8')
    manifest_file = args.output / 'manifest.json'; manifest = json.loads(manifest_file.read_text(encoding='utf-8'))
    manifest['classes'] = [e for e in manifest['classes'] if e['class'] != MENU]
    manifest['classes'].append({'class': MENU, 'fields': len(actual['fields']), 'methods': len(actual['methods']), 'changes': len(actual['changes']), 'scope': 'menu lifetimes, detached slots and native action transport'})
    if not args.menu_only:
        manifest['scope'] = 'staged style/menu contracts; screen consumer and runtime activation pending'
    manifest_file.write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    print(f'Transferred native AE2 15 menu lifetime/action contract ({args.loader}, {args.namespace})')
    if args.menu_only: return
    named_folder = inventories / f'1.20.1-{args.loader}-named'
    target = json.loads((named_folder / (BUILDER + '.json')).read_text(encoding='utf-8'))
    builder_patch = json.loads((source / (BUILDER + '.json')).read_text(encoding='utf-8'))
    merged = builder_contract(target, builder_patch)
    if args.namespace == 'runtime':
        original_named = target
        target = json.loads((inventories / f'1.20.1-{args.loader}' / (BUILDER + '.json')).read_text(encoding='utf-8'))
        merged = remap_runtime(merged, inventories / f'1.20.1-{args.loader}', named_folder, original_named)
    actual = g.generate(target, merged)
    output = args.output / (BUILDER + '.json'); output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(g.canonical(actual), encoding='utf-8')
    expected = inventories / f'1.20.1-{args.loader}-desktop-style-{args.namespace}' / (BUILDER + '.json')
    expected.parent.mkdir(parents=True, exist_ok=True); expected.write_text(g.canonical(merged), encoding='utf-8')
    manifest['classes'] = [e for e in manifest['classes'] if e['class'] != BUILDER]
    manifest['classes'].append({'class': BUILDER, 'fields': 0, 'methods': 2, 'changes': 1, 'scope': 'native opening serializer/factory bridge and detached close guard'})
    manifest_file.write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    print(f'Transferred native AE2 15 detached builder bridge ({args.loader}, {args.namespace})')


if __name__ == '__main__': main()
