"""Rebase the fork's common desktop additions onto AE2 19.2.18.

The installed AE2 owns the original methods. This emits verified instruction
edits, preserving its JSON client actions, recipe identifiers and Player opener.
Client rendering is deliberately a separate port and registration gate.
"""
import argparse
import copy
import importlib
import json
from pathlib import Path

g = importlib.import_module('generate-ae2-native-deltas')
t = importlib.import_module('transfer-ae2-native-deltas')
PLAYER = 'net/minecraft/world/entity/player/Player'
SERVER = 'net/minecraft/server/level/ServerPlayer'
BUILDER = 'appeng/menu/implementations/MenuTypeBuilder'
DISTRIBUTOR = 'net/neoforged/neoforge/network/PacketDistributor'
PAYLOAD = 'net/minecraft/network/protocol/common/custom/CustomPacketPayload'
SCREEN = 'appeng/client/gui/AEBaseScreen'
KEY_EVENT = 'com/salts_inventory_update/api/client/input/KeyEvent'
MOUSE_EVENT = 'com/salts_inventory_update/api/client/input/MouseButtonEvent'
INPUT_BRIDGE = 'com/salts_inventory_update/compat/ae2/nativeview/LegacyInput'
LISTENER = 'net/minecraft/client/gui/components/events/GuiEventListener'
GRAPHICS = 'com/salts_inventory_update/api/client/gui/GuiGraphicsExtractor'
POSE = GRAPHICS + '$PoseAdapter'
RAW_GRAPHICS = 'net/minecraft/client/gui/GuiGraphics'
RENDER_BRIDGE = 'com/salts_inventory_update/compat/ae2/nativeview/LegacyRendering'
PICK_BRIDGE = 'com/salts_inventory_update/compat/ae2/nativeview/LegacyPickBlock'
PICK_SCOPE = 'com/salts_inventory_update/compat/ae2/nativeview/LegacyPickScope'
INPUT_METHODS = {
    'keyPressed', 'desktopMouseClicked', 'withDesktopActions', 'currentDesktopContent', 'setDesktopNavigation',
    'desktopSlots', 'desktopSlotAt', 'desktopSlotVisible', 'desktopSemanticVisible',
    'desktopWantsTextInput', 'isDesktopHosted', 'closeCurrentMenu', 'cancelDesktopPointer',
}
NAVIGATION_CLASSES = {
    'appeng/client/gui/AESubScreen',
    'appeng/client/gui/implementations/AESubScreen',
    'appeng/client/gui/implementations/InterfaceScreen',
    'appeng/client/gui/implementations/PatternProviderScreen',
    'appeng/client/gui/widgets/SettingToggleButton',
    'appeng/client/gui/widgets/ServerSettingToggleButton',
    'appeng/client/gui/me/common/TerminalSettingsScreen',
}
MACHINE_SCREEN_CLASSES = {
    'appeng/client/gui/implementations/CellWorkbenchScreen',
    'appeng/client/gui/implementations/DriveScreen',
    'appeng/client/gui/implementations/MolecularAssemblerScreen',
    'appeng/client/gui/implementations/WirelessAccessPointScreen',
}
ENCODING_PANEL_CLASSES = {'appeng/client/gui/me/items/' + name for name in [
    'EncodingModePanel', 'CraftingEncodingPanel', 'ProcessingEncodingPanel',
    'SmithingTableEncodingPanel', 'StonecuttingEncodingPanel',
]}
FIELD_WIDGET_CLASSES = {
    'appeng/client/gui/widgets/AETextField',
    'appeng/client/gui/implementations/KeyTypeSelectionScreen$KeyTypeCheckboxes',
}
CRAFTING_CLASSES = {'appeng/client/gui/me/crafting/' + name for name in [
    'AbstractTableRenderer', 'CraftConfirmScreen', 'CraftConfirmTableRenderer',
    'CraftErrorScreen', 'CraftingCPUScreen', 'CraftingStatusScreen',
]} | {'appeng/client/gui/widgets/CPUSelectionList'}
TERMINAL_CLASSES = {
    'appeng/client/gui/me/common/MEStorageScreen',
    'appeng/client/gui/me/items/PatternEncodingTermScreen',
    'appeng/client/gui/me/patternaccess/PatternAccessTermScreen',
}


def replace(value, before, after):
    if isinstance(value, str): return value.replace(before, after)
    if isinstance(value, list): return [replace(x, before, after) for x in value]
    if isinstance(value, dict): return {k: replace(v, before, after) for k, v in value.items()}
    return value


def legacy_inventory(value, name):
    value = replace(value, 'net/minecraft/resources/Identifier', 'net/minecraft/resources/ResourceLocation')
    value = replace(value, 'appeng/client/InitScreens', 'appeng/init/client/InitScreens')
    value = replace(value, 'net/minecraft/world/inventory/ContainerInput', 'net/minecraft/world/inventory/ClickType')
    value = replace(value, 'net/minecraft/client/input/KeyEvent', KEY_EVENT)
    value = replace(value, 'net/minecraft/client/input/MouseButtonEvent', MOUSE_EVENT)
    value = replace(value, 'appeng/util/Icon', 'appeng/client/gui/Icon')
    value = replace(value, 'appeng/client/api/AEKeyRendering', 'appeng/api/client/AEKeyRendering')
    if name.endswith('/PatternEncodingTermScreen'):
        value = replace(value, 'lambda$handlePickBlock$', 'lambda$mouseClicked$')
    # The old builder accepts Player and checks the server side itself. Keep
    # that native contract, including the captured provider's Player field.
    if name.startswith(BUILDER): value = replace(value, SERVER, PLAYER)
    for method in value['methods']:
        instructions = method['instructions']
        for i in range(len(instructions) - 1, -1, -1):
            node = instructions[i]
            if name == 'appeng/core/network/InitNetwork' and node.get('owner') == 'net/neoforged/neoforge/network/registration/PayloadRegistrar' and node.get('name') == 'playToClient':
                if node['desc'] != '(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$Type;Lnet/minecraft/network/codec/StreamCodec;)Lnet/neoforged/neoforge/network/registration/PayloadRegistrar;' or instructions[i + 1] != {'type': 0, 'op': 87}:
                    raise ValueError('Unreviewed native client payload registration')
                node.update(op=184, owner=name, name='clientbound', desc='(Lnet/neoforged/neoforge/network/registration/PayloadRegistrar;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$Type;Lnet/minecraft/network/codec/StreamCodec;)V')
                del instructions[i + 1]
            if node.get('owner') == 'net/minecraft/client/gui/Gui' and node.get('name') in {'screen', 'setScreen'}:
                gui = next(j for j in range(i - 1, -1, -1) if instructions[j].get('owner') == 'net/minecraft/client/Minecraft' and instructions[j].get('name') == 'gui')
                del instructions[gui]
                node['owner'] = 'net/minecraft/client/Minecraft'
                if node['name'] == 'screen' and node['type'] == 5:
                    node.clear(); node.update(type=4, op=180, owner='net/minecraft/client/Minecraft', name='screen', desc='Lnet/minecraft/client/gui/screens/Screen;')
            if node.get('owner') == 'net/neoforged/neoforge/client/network/ClientPacketDistributor':
                node['owner'] = DISTRIBUTOR
                if node.get('name') == 'sendToServer' and node['desc'] == f'(L{PAYLOAD};)V':
                    node['desc'] = f'(L{PAYLOAD};[L{PAYLOAD};)V'
                    instructions[i:i] = [{'type': 0, 'op': 3}, {'type': 3, 'op': 189, 'desc': PAYLOAD}]
                    method['maxStack'] += 1
            if name.startswith(BUILDER) and node.get('owner') == PLAYER and node.get('name') == 'connection':
                # The original Player capture is retained; the opener's server
                # guard guarantees this cast, as in native AE2 19.
                node['owner'] = SERVER
                instructions[i:i] = [{'type': 3, 'op': 192, 'desc': SERVER}]
            if node.get('owner') == 'net/minecraft/world/inventory/Slot' and node.get('name') == 'getSlotIndex':
                node['name'] = 'getContainerSlot'
            if node.get('owner', '').startswith('appeng/client/gui/') and node.get('name') == 'getHoveredSlot':
                node['name'] = 'findSlot'
            if node.get('owner') == 'net/minecraft/client/Minecraft' and node.get('name') == 'hasShiftDown':
                node.update(op=184, owner=INPUT_BRIDGE, name='hasShiftDown', desc='(Lnet/minecraft/client/Minecraft;)Z', interface=False)
            if name == SCREEN and node.get('owner') == 'com/mojang/blaze3d/platform/InputConstants' and node.get('name') == 'isKeyDown' and node['desc'] == '(I)Z':
                key_index = next(j for j in range(i - 1, -1, -1) if instructions[j]['type'] not in [8, 14])
                if instructions[key_index].get('operand') != 44: raise ValueError('Unreviewed desktop key query')
                node['desc'] = '(JI)Z'
                instructions[key_index:key_index + 1] = [
                    {'type': 2, 'op': 25, 'var': 0},
                    {'type': 5, 'op': 182, 'owner': SCREEN, 'name': 'getMinecraft', 'desc': '()Lnet/minecraft/client/Minecraft;', 'interface': False},
                    {'type': 5, 'op': 182, 'owner': 'net/minecraft/client/Minecraft', 'name': 'getWindow', 'desc': '()Lcom/mojang/blaze3d/platform/Window;', 'interface': False},
                    {'type': 5, 'op': 182, 'owner': 'com/mojang/blaze3d/platform/Window', 'name': 'getWindow', 'desc': '()J', 'interface': False},
                    {'type': 1, 'op': 16, 'operand': 32}]
                method['maxStack'] += 2
    if name.endswith('/FillCraftingGridFromRecipePacket'):
        for index, method in enumerate(value['methods']):
            if method['name'] != 'decode': continue
            method = replace(method, 'net/minecraft/resources/ResourceKey', 'net/minecraft/resources/ResourceLocation')
            for i in range(len(method['instructions']) - 1, -1, -1):
                node = method['instructions'][i]
                if node.get('name') == 'readResourceKey':
                    if method['instructions'][i - 1].get('name') != 'RECIPE':
                        raise ValueError('Unreviewed recipe reader')
                    del method['instructions'][i - 1]
                    node['name'] = 'readResourceLocation'
                    node['desc'] = '()Lnet/minecraft/resources/ResourceLocation;'
            value['methods'][index] = method
    if name == SCREEN:
        value['methods'] = [primitive_mouse_method(m) if m['name'] in {'mouseReleased', 'mouseDragged'} else m for m in value['methods']]
    return value


def primitive_mouse_method(method):
    """Normalize the reference ABI, then merge its edits with native raw input."""
    method = copy.deepcopy(method)
    if not method['desc'].startswith(f'(L{MOUSE_EVENT};'): return method
    method['desc'] = method['desc'].replace(f'L{MOUSE_EVENT};', 'DDI', 1)
    code = method['instructions']
    getters = {'x': (24, 1), 'y': (24, 3), 'button': (21, 5)}
    consumed = set()
    replacements = {}
    for i, node in enumerate(code):
        if node.get('type') == 5 and node.get('owner') == MOUSE_EVENT and node.get('name') in getters:
            load = next(j for j in range(i - 1, -1, -1) if code[j]['type'] not in [8, 14])
            if code[load] != {'type': 2, 'op': 25, 'var': 1}: raise ValueError('Unreviewed mouse event receiver')
            op, var = getters[node['name']]
            replacements[load] = [{'type': 2, 'op': op, 'var': var}]
            consumed.add(i)
            if node['name'] == 'button':
                following = next((x for x in code[i + 1:] if x['type'] not in [8, 14]), None)
                if following and following['type'] == 0 and following['op'] in [4, 6]:
                    following['op'] = {4: 3, 6: 4}[following['op']]
    output = []
    for i, node in enumerate(code):
        if i in consumed: continue
        if i in replacements:
            output.extend(replacements[i]); continue
        if node['type'] in [2, 10]:
            if node['var'] >= 2: node['var'] += 4
            elif node['type'] == 2 and node['op'] == 25 and node['var'] == 1:
                output.extend([{'type': 2, 'op': 24, 'var': 1}, {'type': 2, 'op': 24, 'var': 3}, {'type': 2, 'op': 21, 'var': 5}]); continue
        if node['type'] == 14:
            if any(x.get('value') == MOUSE_EVENT for x in node['stack']): raise ValueError('Mouse event survives a branch on the operand stack')
            locals_ = node['locals']
            if len(locals_) > 1 and locals_[1].get('value') == MOUSE_EVENT:
                locals_[1:2] = [{'kind': 'Integer', 'value': 3}, {'kind': 'Integer', 'value': 3}, {'kind': 'Integer', 'value': 1}]
        if node['type'] == 5 and node.get('desc', '').startswith(f'(L{MOUSE_EVENT};'):
            node['desc'] = node['desc'].replace(f'L{MOUSE_EVENT};', 'DDI', 1)
        output.append(node)
    method['instructions'] = output
    method['maxLocals'] += 4
    method['maxStack'] += 4
    return method


def adapt_key_method(method):
    method = copy.deepcopy(method)
    for i, node in enumerate(method['instructions']):
        if node.get('owner') == KEY_EVENT and node.get('name') == 'key':
            following = next((x for x in method['instructions'][i + 1:] if x['type'] not in [8, 14]), None)
            if following and following.get('operand') == 43:
                following['operand'] = 258
                following['op'] = 17  # GLFW Tab, preserving the fork's focus traversal.
        if node.get('type') != 5: continue
        owner, name, desc = node['owner'], node['name'], node['desc']
        if owner == 'net/minecraft/client/gui/screens/inventory/AbstractContainerScreen' and name == 'keyPressed' and desc == f'(L{KEY_EVENT};)Z':
            if node['op'] != 183: raise ValueError('Unreviewed native superclass key dispatch')
            node.update(op=182, owner=SCREEN, name='siu$nativeKeyPressed')
        elif owner == LISTENER and name == 'keyPressed' and desc == f'(L{KEY_EVENT};)Z':
            node.update(op=184, owner=INPUT_BRIDGE, desc=f'(L{LISTENER};L{KEY_EVENT};)Z', interface=False)
        elif owner == 'com/mojang/blaze3d/platform/InputConstants' and name == 'getKey' and desc == f'(L{KEY_EVENT};)Lcom/mojang/blaze3d/platform/InputConstants$Key;':
            node.update(owner=INPUT_BRIDGE, name='key')
        elif owner == 'net/minecraft/client/KeyMapping' and name == 'matches' and desc == f'(L{KEY_EVENT};)Z':
            node.update(op=184, owner=INPUT_BRIDGE, desc=f'(Lnet/minecraft/client/KeyMapping;L{KEY_EVENT};)Z', interface=False)
    return method


def primitive_terminal_key_method(method):
    method = copy.deepcopy(method); method['desc'] = '(III)Z'
    code = method['instructions']; output = []; consumed = set()
    for i, node in enumerate(code):
        if i in consumed: continue
        if node == {'type': 2, 'op': 25, 'var': 1}:
            next_ = next(j for j in range(i + 1, len(code)) if code[j]['type'] not in [8, 14])
            call = code[next_]
            if call.get('owner') == KEY_EVENT and call.get('name') == 'key':
                output.append({'type': 2, 'op': 21, 'var': 1}); consumed.add(next_)
            elif call.get('name') == 'isCloseHotkey':
                output.extend([{'type': 2, 'op': 21, 'var': 1}, {'type': 2, 'op': 21, 'var': 2}]); call['desc'] = '(II)Z'
            elif call.get('name') == 'keyPressed':
                output.extend({'type': 2, 'op': 21, 'var': x} for x in [1, 2, 3]); call['desc'] = '(III)Z'
            else: raise ValueError('Unreviewed terminal key-event receiver')
            continue
        if node['type'] in [2, 10] and node['var'] >= 2: node['var'] += 2
        if node['type'] == 14 and len(node['locals']) > 1 and node['locals'][1].get('value') == KEY_EVENT:
            node['locals'][1:2] = [{'kind': 'Integer', 'value': 1}] * 3
        if node.get('operand') == 40:
            node.update(op=17, operand=257)  # Native GLFW Enter retains search-field focus behavior.
        output.append(node)
    method['instructions'] = output; method['maxLocals'] += 2; method['maxStack'] += 2
    return method


def terminal_key_bridge(owner):
    code = [{'type': 2, 'op': 25, 'var': 0}]
    for getter in ['key', 'scancode', 'modifiers']:
        code.extend([{'type': 2, 'op': 25, 'var': 1},
                     {'type': 5, 'op': 182, 'owner': KEY_EVENT, 'name': getter, 'desc': '()I', 'interface': False}])
    code.extend([{'type': 5, 'op': 182, 'owner': owner, 'name': 'keyPressed', 'desc': '(III)Z', 'interface': False}, {'type': 0, 'op': 172}])
    return {'name': 'keyPressed', 'desc': f'(L{KEY_EVENT};)Z', 'access': 1, 'exceptions': [],
            'maxStack': 4, 'maxLocals': 2, 'handlers': [], 'visibleAnnotations': [], 'invisibleAnnotations': [], 'instructions': code}


def native_super_key_bridge():
    bridge = terminal_key_bridge(SCREEN)
    bridge['name'] = 'siu$nativeKeyPressed'; bridge['access'] = 4
    call = next(x for x in bridge['instructions'] if x.get('name') == 'keyPressed')
    call.update(op=183, owner='net/minecraft/client/gui/screens/inventory/AbstractContainerScreen')
    return bridge


def base_terminal_key_bridge():
    code = [{'type': 2, 'op': 25, 'var': 0},
            {'type': 5, 'op': 182, 'owner': SCREEN, 'name': 'isDesktopHosted', 'desc': '()Z', 'interface': False},
            {'type': 7, 'op': 153, 'label': 'native_terminal_key'},
            {'type': 2, 'op': 25, 'var': 0}, {'type': 3, 'op': 187, 'desc': KEY_EVENT}, {'type': 0, 'op': 89}]
    code.extend({'type': 2, 'op': 21, 'var': x} for x in [1, 2, 3])
    code.extend([{'type': 5, 'op': 183, 'owner': KEY_EVENT, 'name': '<init>', 'desc': '(III)V', 'interface': False},
                 # A special call reaches this base implementation exactly once,
                 # without redispatching into the terminal's public key bridge.
                 {'type': 5, 'op': 183, 'owner': SCREEN, 'name': 'keyPressed', 'desc': f'(L{KEY_EVENT};)Z', 'interface': False},
                 {'type': 0, 'op': 172}, {'type': 8, 'op': -1, 'label': 'native_terminal_key'},
                 {'type': 14, 'op': -1, 'frameType': -1,
                  'locals': [{'kind': 'string', 'value': SCREEN}] + [{'kind': 'Integer', 'value': 1}] * 3, 'stack': []},
                 {'type': 2, 'op': 25, 'var': 0}])
    code.extend({'type': 2, 'op': 21, 'var': x} for x in [1, 2, 3])
    code.extend([{'type': 5, 'op': 183, 'owner': SCREEN, 'name': 'keyPressed', 'desc': '(III)Z', 'interface': False}, {'type': 0, 'op': 172}])
    return {'name': 'siu$desktopKeyPressed', 'desc': '(III)Z', 'access': 4, 'exceptions': [],
            'maxStack': 6, 'maxLocals': 4, 'handlers': [], 'visibleAnnotations': [], 'invisibleAnnotations': [], 'instructions': code}


def scope_native_hotbar_transport(target):
    # This is the fork's sole hotbar edit: keep native slot lookup and swap
    # construction, and route only its final transport through the owner scope.
    method = next(m for m in target['methods'] if m['name'] == 'checkHotbarKeyPressed' and m['desc'] == '(II)Z')
    code = method['instructions']
    calls = [i for i, x in enumerate(code) if x.get('owner') == DISTRIBUTOR and x.get('name') == 'sendToServer']
    if len(calls) != 1: raise ValueError('Unreviewed native hotbar transport')
    i = calls[0]
    if code[i - 2:i] != [{'type': 0, 'op': 3}, {'type': 3, 'op': 189, 'desc': PAYLOAD}]:
        raise ValueError('Unreviewed native hotbar payload arguments')
    code[i].update(owner='com/salts_inventory_update/compat/ae2/nativeview/NativeMenuPacketScope', name='send',
                   desc='(Lappeng/core/network/ServerboundPacket;)V')
    del code[i - 2:i]


def pointer_slot_bridge():
    slot = 'net/minecraft/world/inventory/Slot'
    locals_ = [{'kind': 'string', 'value': SCREEN}, {'kind': 'Integer', 'value': 3},
               {'kind': 'Integer', 'value': 3}, {'kind': 'string', 'value': slot}]
    code = [{'type': 2, 'op': 25, 'var': 0},
            {'type': 5, 'op': 184, 'owner': PICK_SCOPE, 'name': 'slot', 'desc': f'(L{LISTENER};)L{slot};', 'interface': False},
            {'type': 2, 'op': 58, 'var': 5}, {'type': 2, 'op': 25, 'var': 5},
            {'type': 7, 'op': 198, 'label': 'pointer_lookup'}, {'type': 2, 'op': 25, 'var': 5}, {'type': 0, 'op': 176},
            {'type': 8, 'op': -1, 'label': 'pointer_lookup'}, {'type': 14, 'op': -1, 'frameType': -1, 'locals': locals_, 'stack': []},
            {'type': 2, 'op': 25, 'var': 0},
            {'type': 5, 'op': 182, 'owner': SCREEN, 'name': 'isDesktopHosted', 'desc': '()Z', 'interface': False},
            {'type': 7, 'op': 153, 'label': 'pointer_native'}]
    for method, label in [('desktopSlotAt', None), ('findSlot', 'pointer_native')]:
        if label:
            code.extend([{'type': 8, 'op': -1, 'label': label}, {'type': 14, 'op': -1, 'frameType': -1, 'locals': locals_, 'stack': []}])
        code.extend([{'type': 2, 'op': 25, 'var': 0}, {'type': 2, 'op': 24, 'var': 1}, {'type': 2, 'op': 24, 'var': 3},
                     {'type': 5, 'op': 182, 'owner': SCREEN, 'name': method, 'desc': f'(DD)L{slot};', 'interface': False}, {'type': 0, 'op': 176}])
    return {'name': 'siu$pointerSlotAt', 'desc': f'(DD)L{slot};', 'access': 4, 'exceptions': [],
            'maxStack': 5, 'maxLocals': 6, 'handlers': [], 'visibleAnnotations': [], 'invisibleAnnotations': [], 'instructions': code}


def native_pick_bridge(owner, method):
    return {'name': 'handlePickBlock', 'desc': '(Lnet/minecraft/world/inventory/Slot;)Z', 'access': 4,
            'exceptions': [], 'maxStack': 2, 'maxLocals': 2, 'handlers': [], 'visibleAnnotations': [], 'invisibleAnnotations': [],
            'instructions': [{'type': 2, 'op': 25, 'var': 0}, {'type': 2, 'op': 25, 'var': 1},
                {'type': 5, 'op': 184, 'owner': PICK_BRIDGE, 'name': method,
                 'desc': f'(L{owner};Lnet/minecraft/world/inventory/Slot;)Z', 'interface': False}, {'type': 0, 'op': 172}]}


def adapt_mouse_method(method):
    method = copy.deepcopy(method)
    code = method['instructions']
    constructors = [i for i, x in enumerate(code) if x.get('owner') == MOUSE_EVENT and x.get('name') == '<init>']
    if len(constructors) != 1: raise ValueError('Unreviewed desktop button normalization')
    end = constructors[0]
    start = max(i for i in range(end) if code[i].get('op') == 187 and code[i].get('desc') == MOUSE_EVENT)
    code[start:end + 1] = [
        {'type': 2, 'op': 25, 'var': 1},
        {'type': 5, 'op': 184, 'owner': INPUT_BRIDGE, 'name': 'leftButton',
         'desc': f'(L{MOUSE_EVENT};)L{MOUSE_EVENT};', 'interface': False}]
    for i, node in enumerate(code):
        if node.get('owner') == MOUSE_EVENT and node.get('name') == 'button':
            following = next((x for x in code[i + 1:] if x['type'] not in [8, 14]), None)
            if following and following['type'] == 0 and following['op'] in [4, 6]:
                following['op'] = {4: 3, 6: 4}[following['op']]
        if node.get('type') != 5: continue
        if node['owner'] == LISTENER and node['name'] == 'mouseClicked':
            node.update(op=184, owner=INPUT_BRIDGE, desc=f'(L{LISTENER};L{MOUSE_EVENT};Z)Z', interface=False)
        if node['owner'] == 'net/minecraft/client/Minecraft' and node['name'] == 'hasShiftDown':
            node.update(op=184, owner=INPUT_BRIDGE, name='hasShiftDown', desc='(Lnet/minecraft/client/Minecraft;)Z', interface=False)
    return method


def hotbar_bridge():
    # An overload calls the original protected AE2 method; it does not reproduce
    # AE2's hotbar selection or packet implementation.
    return {'name': 'checkHotbarKeyPressed', 'desc': f'(L{KEY_EVENT};)Z', 'access': 4,
            'exceptions': [], 'maxStack': 3, 'maxLocals': 2, 'handlers': [],
            'visibleAnnotations': [], 'invisibleAnnotations': [], 'instructions': [
                {'type': 2, 'op': 25, 'var': 0}, {'type': 2, 'op': 25, 'var': 1},
                {'type': 5, 'op': 182, 'owner': KEY_EVENT, 'name': 'key', 'desc': '()I', 'interface': False},
                {'type': 2, 'op': 25, 'var': 1},
                {'type': 5, 'op': 182, 'owner': KEY_EVENT, 'name': 'scancode', 'desc': '()I', 'interface': False},
                {'type': 5, 'op': 182, 'owner': SCREEN, 'name': 'checkHotbarKeyPressed', 'desc': '(II)Z', 'interface': False},
                {'type': 0, 'op': 172}]}


def mouse_click_bridge():
    return {'name': 'siu$desktopMouseClicked', 'desc': f'(L{MOUSE_EVENT};Z)Z', 'access': 1,
            'exceptions': [], 'maxStack': 3, 'maxLocals': 3, 'handlers': [],
            'visibleAnnotations': [], 'invisibleAnnotations': [], 'instructions': [
                {'type': 2, 'op': 25, 'var': 0}, {'type': 2, 'op': 25, 'var': 1}, {'type': 2, 'op': 21, 'var': 2},
                {'type': 5, 'op': 184, 'owner': INPUT_BRIDGE, 'name': 'mouseClicked', 'desc': f'(L{LISTENER};L{MOUSE_EVENT};Z)Z', 'interface': False},
                {'type': 0, 'op': 172}]}


def graft_native_mouse_entry(target):
    """Route hosted clicks through native subclass handlers before base widgets.

    Native terminals clear searches and perform pick-block actions in their raw
    mouse override. Keeping that dispatch avoids bypassing those native actions.
    The existing ordinary base-screen method retains all its instruction objects.
    """
    method = next(m for m in target['methods'] if m['name'] == 'mouseClicked' and m['desc'] == '(DDI)Z')
    prefix = [
        {'type': 2, 'op': 25, 'var': 0},
        {'type': 5, 'op': 182, 'owner': SCREEN, 'name': 'isDesktopHosted', 'desc': '()Z', 'interface': False},
        {'type': 7, 'op': 153, 'label': 'native_mouse_body'},
        {'type': 2, 'op': 25, 'var': 0}, {'type': 3, 'op': 187, 'desc': MOUSE_EVENT}, {'type': 0, 'op': 89},
        {'type': 2, 'op': 24, 'var': 1}, {'type': 2, 'op': 24, 'var': 3}, {'type': 2, 'op': 21, 'var': 5},
        {'type': 5, 'op': 183, 'owner': MOUSE_EVENT, 'name': '<init>', 'desc': '(DDI)V', 'interface': False},
        {'type': 0, 'op': 3},
        {'type': 5, 'op': 182, 'owner': SCREEN, 'name': 'desktopMouseClicked', 'desc': f'(L{MOUSE_EVENT};Z)Z', 'interface': False},
        {'type': 0, 'op': 172},
        {'type': 8, 'op': -1, 'label': 'native_mouse_body'},
        {'type': 14, 'op': -1, 'frameType': -1,
         'locals': [{'kind': 'string', 'value': SCREEN}, {'kind': 'Integer', 'value': 3},
                    {'kind': 'Integer', 'value': 3}, {'kind': 'Integer', 'value': 1}], 'stack': []}]
    method['instructions'][0:0] = prefix
    method['maxStack'] = max(method['maxStack'], 9)


def pick_block_contract():
    # The fork's base-screen hook returns false, leaving its explicit CLONE
    # action below the hook in desktopMouseClicked. Native terminal-specific
    # overrides remain part of the pending screen port; activation stays off.
    return {'name': 'handlePickBlock', 'desc': '(Lnet/minecraft/world/inventory/Slot;)Z', 'access': 4,
            'exceptions': [], 'maxStack': 1, 'maxLocals': 2, 'handlers': [],
            'visibleAnnotations': [], 'invisibleAnnotations': [],
            'instructions': [{'type': 0, 'op': 3}, {'type': 0, 'op': 172}]}


def desktop_graphics_method(method):
    """Adapt newly added desktop code; native drawing remains in native methods."""
    method = replace(method, 'net/minecraft/client/gui/GuiGraphicsExtractor', GRAPHICS)
    code = method['instructions']
    remove = set()
    for i, node in enumerate(code):
        owner, name = node.get('owner'), node.get('name')
        if owner == GRAPHICS and name == 'pose': node['desc'] = f'()L{POSE};'
        elif owner == 'org/joml/Matrix3x2fStack':
            node['owner'] = POSE
            node['desc'] = node['desc'].split(')')[0] + ')V'
            following = next(j for j in range(i + 1, len(code)) if code[j]['type'] not in [8, 14])
            if code[following] != {'type': 0, 'op': 87}: raise ValueError('Unreviewed fluent pose use')
            remove.add(following)
        elif owner == 'net/minecraft/client/renderer/RenderPipelines':
            if name != 'GUI_TEXTURED': raise ValueError('Unreviewed desktop pipeline')
            node.clear(); node.update(type=0, op=1)
        elif owner == GRAPHICS:
            node['desc'] = node['desc'].replace('Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;', 'Ljava/lang/Object;')
        elif owner == 'appeng/client/gui/style/Blitter' and name == 'blit':
            node.update(op=184, owner=RENDER_BRIDGE, desc=f'(Lappeng/client/gui/style/Blitter;L{GRAPHICS};)V', interface=False)
        elif owner == 'appeng/client/gui/style/Blitter' and name == 'icon':
            node.update(owner=RENDER_BRIDGE)
        elif owner == 'appeng/client/gui/style/BackgroundGenerator' and name == 'draw':
            node.update(owner=RENDER_BRIDGE, name='background')
        elif owner == 'appeng/client/gui/me/common/StackSizeRenderer' and name == 'renderSizeLabel':
            node.update(owner=RENDER_BRIDGE, name='sizeLabel')
        elif owner == 'net/minecraft/nbt/CompoundTag' and name == 'getStringOr':
            node.update(op=184, owner='com/salts_inventory_update/compat/ae2/nativeview/LegacyPreferences', name='stringOr',
                        desc='(Lnet/minecraft/nbt/CompoundTag;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;', interface=False)
        elif name == 'extractRenderState' and owner in {'net/minecraft/client/gui/components/Renderable', 'net/minecraft/client/gui/components/AbstractWidget'}:
            node.update(op=184, owner=RENDER_BRIDGE, name='render', desc=f'(Lnet/minecraft/client/gui/components/Renderable;L{GRAPHICS};IIF)V', interface=False)
        elif owner == 'appeng/client/gui/ICompositeWidget' and name == 'drawBackgroundLayer':
            node.update(op=184, owner=RENDER_BRIDGE, name='compositeBackground', desc=f'(Lappeng/client/gui/ICompositeWidget;L{GRAPHICS};Lnet/minecraft/client/renderer/Rect2i;Lappeng/client/Point;)V', interface=False)
    method['instructions'] = [x for i, x in enumerate(code) if i not in remove]
    return method


def graphics_overload(owner, name, tail, native_name=None, native_tail=None, native_opcode=182):
    """An added overload delegates into the existing native implementation."""
    native_name = native_name or name
    native_tail = tail if native_tail is None else native_tail
    code = [{'type': 2, 'op': 25, 'var': 0}, {'type': 2, 'op': 25, 'var': 1},
            {'type': 5, 'op': 182, 'owner': GRAPHICS, 'name': 'unwrap', 'desc': f'()L{RAW_GRAPHICS};', 'interface': False}]
    # Parameters in these bridges use only references, int and float.
    types = []
    index = 0
    while index < len(tail):
        if tail[index] == 'L':
            end = tail.index(';', index) + 1; types.append(tail[index:end]); index = end
        else: types.append(tail[index]); index += 1
    native_count = len(types) if native_tail == tail else len(types) - 2
    for index, type_ in enumerate(types[:native_count], 2):
        code.append({'type': 2, 'op': 25 if type_.startswith('L') else 23 if type_ == 'F' else 21, 'var': index})
    code.extend([{'type': 5, 'op': native_opcode, 'owner': owner, 'name': native_name, 'desc': f'(L{RAW_GRAPHICS};{native_tail})V', 'interface': False}, {'type': 0, 'op': 177}])
    if owner == SCREEN and name == 'drawBG':
        locals_ = [{'kind': 'string', 'value': SCREEN}, {'kind': 'string', 'value': GRAPHICS}]
        locals_.extend({'kind': 'Integer', 'value': 2 if x == 'F' else 1} for x in types)
        code = [{'type': 2, 'op': 25, 'var': 0},
                {'type': 5, 'op': 182, 'owner': SCREEN, 'name': 'isDesktopHosted', 'desc': '()Z', 'interface': False},
                {'type': 7, 'op': 153, 'label': 'native'}, {'type': 2, 'op': 25, 'var': 0},
                {'type': 5, 'op': 182, 'owner': SCREEN, 'name': 'usesAeDesktopLayout', 'desc': '()Z', 'interface': False},
                {'type': 7, 'op': 154, 'label': 'native'}, {'type': 0, 'op': 177},
                {'type': 8, 'op': -1, 'label': 'native'},
                {'type': 14, 'op': -1, 'frameType': -1, 'locals': locals_, 'stack': []}] + code
    return {'name': name, 'desc': f'(L{GRAPHICS};{tail})V', 'access': 1, 'exceptions': [],
            'maxStack': len(types) + 2, 'maxLocals': len(types) + 2, 'handlers': [],
            'visibleAnnotations': [], 'invisibleAnnotations': [], 'instructions': code}


def screen_init_overload():
    return {'name': 'init', 'desc': '(II)V', 'access': 1, 'exceptions': [],
            'maxStack': 4, 'maxLocals': 3, 'handlers': [], 'visibleAnnotations': [], 'invisibleAnnotations': [],
            'instructions': [{'type': 2, 'op': 25, 'var': 0},
                {'type': 5, 'op': 184, 'owner': 'net/minecraft/client/Minecraft', 'name': 'getInstance', 'desc': '()Lnet/minecraft/client/Minecraft;', 'interface': False},
                {'type': 2, 'op': 21, 'var': 1}, {'type': 2, 'op': 21, 'var': 2},
                {'type': 5, 'op': 182, 'owner': SCREEN, 'name': 'init', 'desc': '(Lnet/minecraft/client/Minecraft;II)V', 'interface': False},
                {'type': 0, 'op': 177}]}


def native_graphics_method(method):
    return replace(method, 'net/minecraft/client/gui/GuiGraphicsExtractor', RAW_GRAPHICS)


def native_checkbox_method(method):
    method = native_graphics_method(method)
    if method['name'] == 'extractContents': method['name'] = 'renderWidget'
    code = []
    for node in method['instructions']:
        if node.get('owner') == RAW_GRAPHICS and node.get('name') == 'text':
            node['name'] = 'drawString'
            node['desc'] = node['desc'].removesuffix('V') + 'I'
            code.extend([node, {'type': 0, 'op': 87}])
        elif node.get('owner') == 'appeng/client/gui/style/Blitter' and node.get('name') == 'icon':
            node['owner'] = RENDER_BRIDGE; code.append(node)
        else: code.append(node)
    method['instructions'] = code
    return method


def native_textfield_method(method):
    method = native_graphics_method(method)
    method['name'] = 'renderWidget'
    for node in method['instructions']:
        if node.get('name') == 'extractWidgetRenderState': node['name'] = 'renderWidget'
    return method


def native_table_coordinates(method):
    # AE2 19 reuses its mouse parameters for relative coordinates; newer AE2
    # keeps two additional locals. Normalize that storage convention before
    # comparing integration edits, retaining native coordinate restoration.
    method = copy.deepcopy(method)
    for node in method['instructions']:
        if node['type'] in [2, 10]:
            if node['var'] in [6, 7]: node['var'] -= 4
            elif node['var'] >= 8: node['var'] -= 2
        if node['type'] == 14 and len(node['locals']) > 7:
            del node['locals'][6:8]
    method['maxLocals'] -= 2
    return method


def native_catalog_context(method):
    # AE2 19 caches ClientLevel before walking catalog rows. Keep its local
    # layout so the native row implementation can be rebased without changing
    # the meaning of its world-dependent slot construction.
    method = copy.deepcopy(method)
    for node in method['instructions']:
        if node['type'] in [2, 10] and node['var'] >= 7: node['var'] += 1
        if node['type'] == 14 and len(node['locals']) >= 7:
            node['locals'].insert(7, {'kind': 'string', 'value': 'net/minecraft/client/multiplayer/ClientLevel'})
    boundary = next(i + 1 for i, node in enumerate(method['instructions']) if node == {'type': 2, 'op': 54, 'var': 6})
    method['instructions'][boundary:boundary] = [
        {'type': 5, 'op': 184, 'owner': 'net/minecraft/client/Minecraft', 'name': 'getInstance', 'desc': '()Lnet/minecraft/client/Minecraft;', 'interface': False},
        {'type': 4, 'op': 180, 'owner': 'net/minecraft/client/Minecraft', 'name': 'level', 'desc': 'Lnet/minecraft/client/multiplayer/ClientLevel;'},
        {'type': 2, 'op': 58, 'var': 7}]
    method['maxLocals'] += 1
    return method


def native_legacy_render_method(method):
    """Translate only ABI conventions before rebasing rendering edits."""
    method = native_graphics_method(method)
    method = replace(method, 'org/joml/Matrix3x2fStack', 'com/mojang/blaze3d/vertex/PoseStack')
    code = method['instructions']; output = []; remove = set(); removed_stratum = False
    for i, node in enumerate(code):
        if i in remove: continue
        if node.get('owner') == RAW_GRAPHICS and node.get('name') == 'nextStratum':
            if not output or output[-1] != {'type': 2, 'op': 25, 'var': 1}:
                raise ValueError('Unreviewed native rendering stratum receiver')
            # This unchanged native operation has no AE2 19 equivalent. The
            # installed renderer retains its own original ordering; preserve
            # the branch labels around the removed modern ABI call.
            output.pop(); removed_stratum = True; continue
        if node.get('owner') == RAW_GRAPHICS and node.get('name') == 'text':
            node['name'] = 'drawString'; node['desc'] = node['desc'].removesuffix('V') + 'I'
            output.extend([node, {'type': 0, 'op': 87}]); continue
        if node.get('owner') == 'com/mojang/blaze3d/vertex/PoseStack':
            name = node.get('name')
            if name in ['pushMatrix', 'popMatrix', 'scale', 'translate']:
                following = next(j for j in range(i + 1, len(code)) if code[j]['type'] not in [8, 14])
                if code[following] != {'type': 0, 'op': 87}: raise ValueError('Unreviewed fluent rendering pose use')
                remove.add(following)
                if name in ['pushMatrix', 'popMatrix']:
                    node['name'] = {'pushMatrix': 'pushPose', 'popMatrix': 'popPose'}[name]; node['desc'] = '()V'
                elif name == 'scale' and node['desc'].startswith('(F)'):
                    # A merged conditional can leave different float values at
                    # its label. Duplicate that stack value rather than the
                    # constant from the last textual branch.
                    previous = code[i - 1]
                    output.append(copy.deepcopy(previous) if previous['type'] == 9 or (previous['type'] == 2 and previous['op'] == 23) else {'type': 0, 'op': 89})
                    output.append({'type': 0, 'op': 12}); node['desc'] = '(FFF)V'
                elif name in ['scale', 'translate'] and node['desc'].startswith('(FF)'):
                    output.append({'type': 0, 'op': 12 if name == 'scale' else 11}); node['desc'] = '(FFF)V'
                else: raise ValueError('Unreviewed native pose signature: ' + node['desc'])
        output.append(node)
    method['instructions'] = output; method['maxStack'] += 2
    if removed_stratum: method = coalesce_branch_labels(method)
    return method


def coalesce_branch_labels(method):
    # Removing an ABI-only call can leave two labels at the same byte offset.
    # Unify their references before matching native instructions, so a branch
    # destination cannot disappear as an apparently redundant native label.
    aliases = {}; output = []; current = None
    for node in method['instructions']:
        if node['type'] == 8:
            if current is None: current = node['label']
            else:
                aliases[node['label']] = current; continue
        elif node['type'] != 14: current = None
        output.append(node)
    def translate(value):
        if isinstance(value, list): return [translate(x) for x in value]
        if isinstance(value, dict):
            result = {}
            for key, item in value.items():
                if key in {'label', 'start', 'end', 'handler'} and isinstance(item, str): result[key] = aliases.get(item, item)
                elif key == 'labels': result[key] = [aliases.get(x, x) for x in item]
                else: result[key] = translate(item)
            return result
        return value
    method['instructions'] = translate(output); method['handlers'] = translate(method['handlers'])
    return method


def wrapped_graphics_overload(owner, name, tail):
    types = []
    index = 0
    while index < len(tail):
        if tail[index] == 'L':
            end = tail.index(';', index) + 1; types.append(tail[index:end]); index = end
        elif tail[index] in 'IF':
            types.append(tail[index]); index += 1
        else: raise ValueError('Unreviewed wrapped graphics signature: ' + tail)
    code = [{'type': 2, 'op': 25, 'var': 0}, {'type': 2, 'op': 25, 'var': 1},
            {'type': 5, 'op': 184, 'owner': GRAPHICS, 'name': 'wrap', 'desc': f'(L{RAW_GRAPHICS};)L{GRAPHICS};', 'interface': False}]
    code.extend({'type': 2, 'op': 25 if type_.startswith('L') else 23 if type_ == 'F' else 21, 'var': index}
                for index, type_ in enumerate(types, 2))
    code.extend([{'type': 5, 'op': 182, 'owner': owner, 'name': name, 'desc': f'(L{GRAPHICS};{tail})V', 'interface': False},
                 {'type': 0, 'op': 177}])
    return {'name': name, 'desc': f'(L{RAW_GRAPHICS};{tail})V', 'access': 1, 'exceptions': [],
            'maxStack': len(types) + 2, 'maxLocals': len(types) + 2, 'handlers': [],
            'visibleAnnotations': [], 'invisibleAnnotations': [], 'instructions': code}


def widget_sprite_overload(owner):
    # Read Minecraft's own default sprite catalog from inside its button
    # subclass. No texture or native button implementation is duplicated.
    code = [{'type': 2, 'op': 25, 'var': 1}, {'type': 0, 'op': 1},
            {'type': 4, 'op': 178, 'owner': 'net/minecraft/client/gui/components/AbstractButton', 'name': 'SPRITES', 'desc': 'Lnet/minecraft/client/gui/components/WidgetSprites;'},
            {'type': 2, 'op': 25, 'var': 0}, {'type': 4, 'op': 180, 'owner': owner, 'name': 'active', 'desc': 'Z'},
            {'type': 2, 'op': 25, 'var': 0}, {'type': 5, 'op': 182, 'owner': owner, 'name': 'isHoveredOrFocused', 'desc': '()Z', 'interface': False},
            {'type': 5, 'op': 182, 'owner': 'net/minecraft/client/gui/components/WidgetSprites', 'name': 'get', 'desc': '(ZZ)Lnet/minecraft/resources/ResourceLocation;', 'interface': False}]
    for name in ['getX', 'getY', 'getWidth', 'getHeight']:
        code.extend([{'type': 2, 'op': 25, 'var': 0}, {'type': 5, 'op': 182, 'owner': owner, 'name': name, 'desc': '()I', 'interface': False}])
    code.extend([{'type': 5, 'op': 182, 'owner': GRAPHICS, 'name': 'blitSprite', 'desc': '(Ljava/lang/Object;Lnet/minecraft/resources/ResourceLocation;IIII)V', 'interface': False}, {'type': 0, 'op': 177}])
    return {'name': 'extractDefaultSprite', 'desc': f'(L{GRAPHICS};)V', 'access': 4,
            'exceptions': [], 'maxStack': 7, 'maxLocals': 2, 'handlers': [], 'visibleAnnotations': [], 'invisibleAnnotations': [], 'instructions': code}


def graft_widget_branch(name, target, reference, raw_patch):
    change = next(x for x in raw_patch['changes'] if x['name'].startswith('extract'))
    insertion = change['edits'][0]
    if insertion['from'] != insertion['to']: raise ValueError('Widget edit replaces native code: ' + name)
    source = next(x for x in reference['methods'] if (x['name'], x['desc']) == (change['name'], change['desc']))
    native = next(x for x in target['methods'] if x['name'] == 'renderWidget' and x['desc'] == f'(L{RAW_GRAPHICS};IIF)V')
    position = insertion['from']
    normalized = native_graphics_method(legacy_inventory({'methods': [copy.deepcopy(source)]}, name)['methods'][0])
    if [g.shape(x) for x in normalized['instructions'][:position]] != [g.shape(x) for x in native['instructions'][:position]]:
        raise ValueError('Widget integration boundary differs: ' + name)
    fragment = {**source, 'instructions': copy.deepcopy(insertion['insert'])}
    fragment = legacy_inventory({'methods': [fragment]}, name)['methods'][0]
    fragment = desktop_graphics_method(fragment)
    prefix = fragment['instructions']
    # Its raw parameter remains a native GuiGraphics. Wrap it only where the
    # added hosted branch consumes the supported public graphics contract.
    output = []
    for node in prefix:
        if node['type'] == 14:
            node['locals'] = replace(node['locals'], GRAPHICS, RAW_GRAPHICS)
        output.append(node)
        if node == {'type': 2, 'op': 25, 'var': 1}:
            output.append({'type': 5, 'op': 184, 'owner': GRAPHICS, 'name': 'wrap', 'desc': f'(L{RAW_GRAPHICS};)L{GRAPHICS};', 'interface': False})
    defined = {x['label'] for x in output if x['type'] == 8}
    external = {x['label'] for x in output if x['type'] == 7} - defined
    if len(external) != 1: raise ValueError('Unreviewed widget branch exits: ' + name)
    external_label = next(iter(external))
    output = replace(output, external_label, 'native_widget_body')
    locals_ = [{'kind': 'string', 'value': name}, {'kind': 'string', 'value': RAW_GRAPHICS},
               {'kind': 'Integer', 'value': 1}, {'kind': 'Integer', 'value': 1}, {'kind': 'Integer', 'value': 2}]
    if name.endswith('/IconButton'):
        locals_.extend([{'kind': 'string', 'value': 'appeng/client/gui/Icon'}, {'kind': 'string', 'value': 'net/minecraft/world/item/Item'}])
    output.extend([{'type': 8, 'op': -1, 'label': 'native_widget_body'},
                   {'type': 14, 'op': -1, 'frameType': -1, 'locals': locals_, 'stack': []}])
    native['instructions'][position:position] = output
    native['maxStack'] = max(native['maxStack'], change['maxStack'] + 1, 10)
    native['maxLocals'] = max(native['maxLocals'], change['maxLocals'])


def graft_title_clipping(target, reference, raw_patch, method_name):
    change = next(x for x in raw_patch['changes'] if x['name'] == method_name)
    before = next(x for x in reference['methods'] if x['name'] == method_name)
    native = next(x for x in target['methods'] if x['name'] == method_name and RAW_GRAPHICS in x['desc'])
    boundary = max(e['to'] for e in change['edits'])
    normalized = native_graphics_method(before)
    if [g.shape(x) for x in normalized['instructions'][:boundary]] != [g.shape(x) for x in native['instructions'][:boundary]]:
        raise ValueError('Native title insertion boundary differs')
    for edit in reversed(change['edits']):
        fragment = {**before, 'instructions': copy.deepcopy(edit['insert'])}
        fragment = native_graphics_method(legacy_inventory({'methods': [fragment]}, SCREEN)['methods'][0])
        native['instructions'][edit['from']:edit['to']] = fragment['instructions']
    native['maxStack'] = max(native['maxStack'], change['maxStack'])
    native['maxLocals'] = max(native['maxLocals'], change['maxLocals'])


def old_client_action(method):
    """Only redirect the transport; AE2 19 retains all native JSON encoding."""
    result = copy.deepcopy(method)
    code = result['instructions']
    positions = [i for i, x in enumerate(code) if x.get('owner') == DISTRIBUTOR and x.get('name') == 'sendToServer']
    if len(positions) != 1: raise ValueError('Unexpected legacy client action transport')
    i = positions[0]
    if code[i - 3:i] != [{'type': 2, 'op': 25, 'var': 5}, {'type': 0, 'op': 3}, {'type': 3, 'op': 189, 'desc': PAYLOAD}]:
        raise ValueError('Unexpected legacy client action dispatch arguments')
    code[i - 3:i + 1] = [
        {'type': 2, 'op': 25, 'var': 0}, {'type': 2, 'op': 25, 'var': 5},
        {'type': 5, 'op': 182, 'owner': 'appeng/menu/AEBaseMenu', 'name': 'sendPacketToServer',
         'desc': '(Lappeng/core/network/ServerboundPacket;)V', 'interface': False}]
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--include-style', action='store_true', help='Also transfer the three renderer-independent desktop style contracts')
    parser.add_argument('--include-factories', action='store_true', help='Also transfer native screen factory registration and the catalog default-size helper')
    parser.add_argument('--include-input', action='store_true', help='Transfer hosted keyboard/callback lifetimes and composite pointer cancellation')
    parser.add_argument('--include-layout', action='store_true', help='Transfer all added base-screen and widget desktop layout/rendering methods')
    parser.add_argument('--include-controls', action='store_true', help='Graft hosted button, tab and progress rendering branches onto native renderers')
    parser.add_argument('--include-navigation', action='store_true', help='Transfer hosted sub-screen lifetimes and scoped settings/back-button actions')
    parser.add_argument('--include-machines', action='store_true', help='Transfer drive, cell workbench, assembler and wireless desktop surfaces')
    parser.add_argument('--include-panels', action='store_true', help='Transfer native pattern-encoding panel desktop edits')
    parser.add_argument('--include-fields', action='store_true', help='Transfer text-field contours and persistent key-type checkbox layouts')
    parser.add_argument('--include-network', action='store_true', help='Register added desktop packets through AE2 19 native clientbound registration')
    parser.add_argument('--include-crafting', action='store_true', help='Transfer crafting tables, CPU selection and confirmation subviews')
    parser.add_argument('--include-status', action='store_true', help='Transfer desktop network-status machine grids and native frame edges')
    parser.add_argument('--include-terminal-layout', action='store_true', help='Transfer terminal layout, scrolling and virtual slots; keyboard activation remains staged')
    parser.add_argument('--include-terminal-input', action='store_true', help='Transfer native terminal search focus and hosted keyboard dispatch')
    parser.add_argument('--include-pointer', action='store_true', help='Adapt virtual-slot lookup and delegate pick-block to native mouse implementations')
    parser.add_argument('--write-hooks', action='store_true', help='Write native marker mixins using each target class/interface kind')
    args = parser.parse_args()
    if args.include_controls and not (args.include_layout and args.include_input):
        parser.error('--include-controls requires --include-layout and --include-input')
    if args.include_navigation and not (args.include_factories and args.include_layout and args.include_input):
        parser.error('--include-navigation requires --include-factories, --include-layout and --include-input')
    if args.include_machines and not args.include_layout:
        parser.error('--include-machines requires --include-layout')
    if args.include_panels and not args.include_layout:
        parser.error('--include-panels requires --include-layout')
    if args.include_fields and not args.include_controls:
        parser.error('--include-fields requires --include-controls')
    if args.include_crafting and not (args.include_layout and args.include_navigation):
        parser.error('--include-crafting requires --include-layout and --include-navigation')
    if args.include_status and not args.include_layout:
        parser.error('--include-status requires --include-layout')
    if args.include_terminal_layout and not (args.include_layout and args.include_factories):
        parser.error('--include-terminal-layout requires --include-layout and --include-factories')
    if args.include_terminal_input and not args.include_terminal_layout:
        parser.error('--include-terminal-input requires --include-terminal-layout')
    if args.include_pointer and not args.include_terminal_input:
        parser.error('--include-pointer requires --include-terminal-input')
    source = g.ROOT / 'compatibility/ae2/versions/26.3/neoforge/src/main/resources/ae2-native-patches'
    native = g.ROOT / 'build/ae2-port/patch-tools/1.21.1-neoforge'
    expected = g.ROOT / 'build/ae2-port/patch-tools/1.21.1-neoforge-desktop-common'
    manifest = json.loads((source / 'manifest.json').read_text())
    emitted = []
    for entry in manifest['classes']:
        name = entry['class']
        style = name in {'appeng/client/gui/style/ScreenStyle', 'appeng/client/gui/style/StyleManager', 'appeng/client/gui/style/TerminalStyle'}
        factory = name == 'appeng/client/InitScreens'
        catalog = name == 'appeng/client/gui/me/patternaccess/PatternAccessTermScreen'
        layout_widget = name in {'appeng/client/gui/widgets/' + x for x in ['AE2Button', 'AECheckbox', 'IconButton', 'ProgressBar', 'Scrollbar', 'TabButton', 'VerticalButtonBar']}
        input_contract = name in {SCREEN, 'appeng/client/gui/WidgetContainer', 'appeng/client/gui/ICompositeWidget', 'appeng/client/gui/widgets/Scrollbar'} or (args.include_layout and layout_widget)
        navigation = args.include_navigation and name in NAVIGATION_CLASSES
        machine = args.include_machines and name in MACHINE_SCREEN_CLASSES
        panel = args.include_panels and name in ENCODING_PANEL_CLASSES
        field_widget = args.include_fields and name in FIELD_WIDGET_CLASSES
        crafting = args.include_crafting and name in CRAFTING_CLASSES
        status_screen = args.include_status and name == 'appeng/client/gui/me/networktool/NetworkStatusScreen'
        terminal = args.include_terminal_layout and name in TERMINAL_CLASSES
        if (name.startswith('appeng/client/') and not ((args.include_style and style) or (args.include_factories and (factory or catalog)) or (args.include_input and input_contract) or navigation or machine or panel or field_widget or crafting or status_screen or terminal)) or (name.endswith('/InitNetwork') and not args.include_network): continue
        raw_base = json.loads((g.BASE / (name + '.json')).read_text())
        raw_patch = json.loads((source / (name + '.json')).read_text())
        if catalog and not terminal:
            # The default geometry is independent of the screen/input port.
            # Carry only that new helper until the rest of this screen is ported.
            raw_patch = {**raw_patch, 'fields': [], 'fieldChanges': [], 'changes': [], 'removedMethods': [],
                         'methods': [x for x in raw_patch['methods'] if x['name'] == 'desktopDefaultContentSize']}
        if terminal and not args.include_terminal_input:
            raw_patch = {**raw_patch, 'changes': [m for m in raw_patch['changes'] if m['name'] != 'keyPressed']}
        if input_contract:
            wanted = INPUT_METHODS if name == SCREEN else {'cancelPointer'}
            raw_patch = {**raw_patch, 'fieldChanges': [], 'removedMethods': [],
                         'fields': raw_patch['fields'] if name == SCREEN else [],
                         'changes': [x for x in raw_patch['changes'] if name == SCREEN and x['name'] in {'<init>', '<clinit>', 'slotClicked', 'mouseReleased', 'mouseDragged'}],
                         'methods': [x for x in raw_patch['methods'] if x['name'] in wanted]}
            if args.include_layout and name in {SCREEN, 'appeng/client/gui/WidgetContainer'}:
                full = json.loads((source / (name + '.json')).read_text())
                keep = {'<init>', '<clinit>', 'getBounds', 'getInventorySlots', 'repositionSlots', 'slotClicked', 'mouseReleased', 'mouseDragged'} if name == SCREEN else {'<init>', 'populateScreen', 'onMouseDown', 'onMouseUp', 'onMouseDrag', 'onMouseWheel', 'updateBeforeRender', 'drawBackgroundLayer', 'drawForegroundLayer'}
                if args.include_navigation and name == SCREEN: keep.add('switchToScreen')
                raw_patch = {**full, 'removedMethods': [], 'changes': [x for x in full['changes'] if x['name'] in keep]}
            if args.include_layout and layout_widget:
                full = json.loads((source / (name + '.json')).read_text())
                raw_patch = {**full, 'removedMethods': [], 'changes': [x for x in full['changes'] if x['name'] in {'<init>', 'updateBeforeRender', 'drawBackgroundLayer', 'drawForegroundLayer'}]}
                if args.include_controls and name.endswith('/AECheckbox'):
                    raw_patch['changes'] = full['changes']
        # Apply before adapting native signatures so the original hashes still
        # identify precisely the reviewed additions from the reference fork.
        raw_integrated = t.apply_to_inventory(raw_base, raw_patch)
        base = legacy_inventory(raw_base, name)
        integrated = legacy_inventory(raw_integrated, name)
        if args.include_layout and (name in {'appeng/client/gui/WidgetContainer', 'appeng/client/gui/widgets/VerticalButtonBar', 'appeng/client/gui/widgets/Scrollbar'} or panel):
            for inventory in [base, integrated]:
                inventory['methods'] = [native_graphics_method(m) if m['name'] in {'drawBackgroundLayer', 'drawForegroundLayer', 'drawRecipes'} else m for m in inventory['methods']]
        if args.include_controls and name.endswith('/AECheckbox'):
            for inventory in [base, integrated]:
                inventory['methods'] = [native_checkbox_method(m) if m['name'] == 'extractContents' else m for m in inventory['methods']]
        if field_widget and name.endswith('/AETextField'):
            for inventory in [base, integrated]:
                inventory['methods'] = [native_textfield_method(m) if m['name'] == 'extractWidgetRenderState' else m for m in inventory['methods']]
        if crafting:
            for inventory in [base, integrated]:
                inventory['methods'] = [native_legacy_render_method(m) if m['name'] in {'render', 'drawFG', 'drawBackgroundLayer'} else m for m in inventory['methods']]
                if name.endswith('/AbstractTableRenderer'):
                    inventory['methods'] = [native_table_coordinates(m) if m['name'] == 'render' else m for m in inventory['methods']]
        if status_screen:
            for inventory in [base, integrated]:
                inventory['methods'] = [native_legacy_render_method(m) if m['name'] in {'drawFG', 'drawMachineCount'} else m for m in inventory['methods']]
        if terminal:
            for inventory in [base, integrated]:
                inventory['methods'] = [native_legacy_render_method(m) if m['name'] in {'drawFG', 'drawBG', 'renderLinkStatus', 'renderPinnedRowDecorations'} else m for m in inventory['methods']]
                if catalog:
                    inventory['methods'] = [native_catalog_context(m) if m['name'] == 'drawFG' else m for m in inventory['methods']]
                if args.include_terminal_input and name.endswith('/MEStorageScreen'):
                    inventory['methods'] = [primitive_terminal_key_method(m) if m['name'] == 'keyPressed' else m for m in inventory['methods']]
        if name == SCREEN:
            integrated['methods'] = [adapt_key_method(x) if x['name'] == 'keyPressed' else adapt_mouse_method(x) if x['name'] == 'desktopMouseClicked' else x for x in integrated['methods']]
        patch = g.generate(base, integrated)
        if args.include_layout and (name in {SCREEN, 'appeng/client/gui/WidgetContainer'} or layout_widget or machine or panel or crafting or status_screen or terminal):
            patch['methods'] = [desktop_graphics_method(m) for m in patch['methods']]
        native_name = base['class']
        target = json.loads((native / (native_name + '.json')).read_text())
        merged = copy.deepcopy(target)
        lambda_renames = {}
        if 'interfaces' in patch:
            if merged.get('interfaces', []) != patch['interfaces']['original']: raise ValueError('Native interfaces differ: ' + name)
            merged['interfaces'] = merged.get('interfaces', []) + patch['interfaces']['added']
        merged['fields'].extend(patch['fields'])
        for change in patch['fieldChanges']:
            field = next(x for x in merged['fields'] if x['name'] == change['name'])
            if field['access'] != change['originalAccess']: raise ValueError('Native field access differs: ' + name)
            for key in ['access', 'visibleAnnotations', 'invisibleAnnotations']:
                if key in change: field[key] = change[key]
        for change in patch['changes']:
            key = change['name'], change['desc']
            if name == 'appeng/menu/AEBaseMenu' and change['name'] == 'sendClientAction':
                old = next(x for x in merged['methods'] if (x['name'], x['desc']) == ('sendClientAction', '(Ljava/lang/String;Ljava/lang/Object;)V'))
                result = old_client_action(old)
            else:
                before = next(x for x in base['methods'] if (x['name'], x['desc']) == key)
                old = next((x for x in merged['methods'] if (x['name'], x['desc']) == key), None)
                if old is None and key[0].startswith('lambda$'):
                    stem = key[0].rsplit('$', 1)[0]
                    candidates = [x for x in merged['methods'] if x['name'].rsplit('$', 1)[0] == stem and x['desc'] == key[1]]
                    if len(candidates) == 1: old = candidates[0]
                if old is None:
                    raise ValueError('Native method signature requires an adapter: ' + name + '.' + key[0] + key[1])
                after = next(x for x in integrated['methods'] if (x['name'], x['desc']) == (change.get('rename', key[0]), key[1]))
                try: result = t.merge_method(before, after, old)
                except ValueError as error: raise ValueError(name + ': ' + str(error)) from error
                if old['name'].startswith('lambda$'):
                    lambda_renames[(after['name'], after['desc'])] = old['name']
                    result['name'] = old['name']
            merged['methods'][merged['methods'].index(old)] = result
        for removed in patch['removedMethods']:
            merged['methods'] = [x for x in merged['methods'] if (x['name'], x['desc']) != (removed['name'], removed['desc'])]
        for method in patch['methods']:
            if any((x['name'], x['desc']) == (method['name'], method['desc']) for x in merged['methods']):
                raise ValueError('Added integration method collides: ' + name + '.' + method['name'])
            merged['methods'].append(method)
        if lambda_renames:
            def native_callback_names(value):
                if isinstance(value, list): return [native_callback_names(x) for x in value]
                if isinstance(value, dict):
                    value = {k: native_callback_names(v) for k, v in value.items()}
                    if value.get('owner') == native_name:
                        replacement = lambda_renames.get((value.get('name'), value.get('desc')))
                        if replacement is not None: value['name'] = replacement
                    return value
                return value
            merged['methods'] = native_callback_names(merged['methods'])
        if name == SCREEN and args.include_input:
            merged['methods'].append(hotbar_bridge())
            merged['methods'].append(pick_block_contract())
            merged['methods'].append(mouse_click_bridge())
            merged['methods'].append(native_super_key_bridge())
            graft_native_mouse_entry(merged)
            if args.include_terminal_input:
                merged['methods'].append(base_terminal_key_bridge())
                scope_native_hotbar_transport(merged)
            if args.include_pointer: merged['methods'].append(pointer_slot_bridge())
        if args.include_layout and name == SCREEN:
            merged['methods'].append(screen_init_overload())
            for method in [graphics_overload(SCREEN, 'drawBG', 'IIIIF'),
                           # The fork's added foreground overrides invoke super.
                           # Call this original base method directly, so the raw
                           # subclass bridge cannot dispatch back into itself.
                           graphics_overload(SCREEN, 'drawFG', 'IIII', native_opcode=183),
                           graphics_overload(SCREEN, 'drawOptionalSlotBackground', 'Lappeng/menu/slot/IOptionalSlot;Z'),
                           graphics_overload(SCREEN, 'drawText', 'Lappeng/client/gui/style/Text;Lappeng/client/gui/TextOverride;'),
                           graphics_overload(SCREEN, 'renderTooltips', 'II'),
                           graphics_overload(SCREEN, 'extractLabels', 'II', 'renderLabels'),
                           graphics_overload(SCREEN, 'extractSlot', 'Lnet/minecraft/world/inventory/Slot;II', 'renderSlot', 'Lnet/minecraft/world/inventory/Slot;')]:
                merged['methods'].append(method)
        if args.include_layout and name == 'appeng/client/gui/WidgetContainer':
            merged['methods'].append(graphics_overload(name, 'drawBackgroundLayer', 'Lnet/minecraft/client/renderer/Rect2i;Lappeng/client/Point;'))
        if args.include_layout and name == 'appeng/client/gui/widgets/VerticalButtonBar':
            merged['methods'].append(wrapped_graphics_overload(name, 'drawAeDesktopBacking', 'Lnet/minecraft/client/renderer/Rect2i;'))
        if panel and name.endswith('/EncodingModePanel'):
            merged['methods'].append(wrapped_graphics_overload(name, 'drawModeBackground', 'Lnet/minecraft/client/renderer/Rect2i;Lappeng/client/gui/style/Blitter;'))
        if machine and any(m['name'] == 'drawFG' for m in patch['methods']):
            # Native renderLabels calls the raw foreground signature. Preserve
            # that path while dispatching to the fork's new desktop override.
            merged['methods'].append(wrapped_graphics_overload(name, 'drawFG', 'IIII'))
        if crafting and name.endswith('/CraftingCPUScreen'):
            merged['methods'].append(wrapped_graphics_overload(name, 'drawDesktopTableOutline', 'III'))
        if terminal and not name.endswith('/PatternEncodingTermScreen'):
            merged['methods'].append(graphics_overload(name, 'renderLinkStatus', 'Lappeng/api/storage/ILinkStatus;'))
        if terminal and args.include_terminal_input and name.endswith('/MEStorageScreen'):
            merged['methods'].append(terminal_key_bridge(name))
            key_method = next(m for m in merged['methods'] if m['name'] == 'keyPressed' and m['desc'] == '(III)Z')
            calls = [x for x in key_method['instructions'] if x.get('owner') == SCREEN and x.get('name') == 'keyPressed']
            if len(calls) != 1: raise ValueError('Unreviewed terminal superclass keyboard dispatch')
            calls[0]['name'] = 'siu$desktopKeyPressed'
        if args.include_pointer and name in {'appeng/client/gui/me/common/MEStorageScreen', 'appeng/client/gui/me/items/PatternEncodingTermScreen'}:
            merged['methods'].append(native_pick_bridge(name, 'pattern' if name.endswith('/PatternEncodingTermScreen') else 'storage'))
            mouse = next(m for m in merged['methods'] if m['name'] == 'mouseClicked' and m['desc'] == '(DDI)Z')
            for node in mouse['instructions']:
                if node.get('name') == 'findSlot': node['name'] = 'siu$pointerSlotAt'
                if node.get('owner') == 'net/minecraft/client/KeyMapping' and node.get('name') == 'matchesMouse':
                    node.update(op=184, owner=PICK_SCOPE, name='matches', desc='(Lnet/minecraft/client/KeyMapping;I)Z', interface=False)
        if args.include_controls and name in {'appeng/client/gui/widgets/' + x for x in ['AE2Button', 'IconButton', 'ProgressBar', 'TabButton']}:
            graft_widget_branch(name, merged, raw_base, json.loads((source / (name + '.json')).read_text()))
            if name.endswith('/AE2Button'):
                merged['methods'].extend([widget_sprite_overload(name), graphics_overload(name, 'renderButtonText', 'Lnet/minecraft/client/gui/Font;III')])
        if args.include_controls and name == SCREEN:
            for method_name in ['drawText', 'renderTooltips', 'drawTooltip']:
                graft_title_clipping(merged, raw_base, json.loads((source / (name + '.json')).read_text()), method_name)
        output = args.output / (native_name + '.json')
        output.parent.mkdir(parents=True, exist_ok=True)
        g.session_cpu_state(merged)
        actual_patch = g.generate(target, merged)
        output.write_text(g.canonical(actual_patch), encoding='utf-8')
        evidence = expected / (native_name + '.json')
        evidence.parent.mkdir(parents=True, exist_ok=True)
        evidence.write_text(g.canonical(merged), encoding='utf-8')
        scope = 'base desktop layout/rendering and input' if args.include_layout and name in {SCREEN, 'appeng/client/gui/WidgetContainer'} else 'hosted keyboard and input lifetimes' if input_contract else 'default size only' if catalog else 'full desktop delta'
        emitted.append({'class': native_name, 'fields': len(actual_patch['fields']), 'methods': len(actual_patch['methods']), 'changes': len(actual_patch['changes']), 'scope': scope})
    manifest['classes'] = emitted
    manifest['nativeTarget'] = '1.21.1-neoforge'
    manifest['clientPortComplete'] = False
    manifest['terminalKeyboardPortComplete'] = args.include_terminal_input
    manifest['pointerPortComplete'] = args.include_pointer
    manifest['scope'] = 'staged contracts; screen and input hooks must be verified before activation'
    (args.output / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    if args.write_hooks:
        main = g.ROOT / 'compatibility/ae2/versions/1.21.1/neoforge/src/main'
        folder = main / 'java/com/salts_inventory_update/compat/ae2/mixin/nativehooks'
        folder.mkdir(parents=True, exist_ok=True)
        config = {'required': True, 'minVersion': '0.8', 'package': 'com.salts_inventory_update.compat.ae2.mixin',
                  'plugin': 'com.salts_inventory_update.compat.ae2.mixin.NativeDesktopPatchPlugin', 'mixins': [], 'client': [], 'injectors': {'defaultRequire': 1}}
        for index, entry in enumerate(emitted):
            name = 'NativeHook' + str(index); owner = entry['class']
            metadata = json.loads((native / (owner + '.json')).read_text())
            kind = 'interface' if metadata['access'] & 512 else 'abstract class'
            (folder / (name + '.java')).write_text('package com.salts_inventory_update.compat.ae2.mixin.nativehooks;\n'
                'import org.spongepowered.asm.mixin.Mixin;\nimport org.spongepowered.asm.mixin.Pseudo;\n@Pseudo\n'
                '@Mixin(targets = "' + owner.replace('/', '.') + '", priority = 2000, remap = false)\npublic ' + kind + ' ' + name + ' { }\n')
            config['client' if owner.startswith(('appeng/client/', 'appeng/init/client/')) else 'mixins'].append('nativehooks.' + name)
        (main / 'resources/salts_inventory_compat.ae2-native.mixins.json').write_text(json.dumps(config, indent=2) + '\n')
    print(f'Transferred {len(emitted)} desktop deltas, retaining native AE2 19 behavior')


if __name__ == '__main__': main()
