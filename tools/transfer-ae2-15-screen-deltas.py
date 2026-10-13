"""Rebase desktop screen additions onto AE2 15 without replacing native renderer bodies."""
import argparse
import copy
import importlib
import json
from pathlib import Path

g = importlib.import_module('generate-ae2-native-deltas')
t = importlib.import_module('transfer-ae2-native-deltas')
menus = importlib.import_module('transfer-ae2-15-menu-deltas')
ROOT = g.ROOT
SCREEN = 'appeng/client/gui/AEBaseScreen'
FACTORIES = 'appeng/init/client/InitScreens'
CATALOG = 'appeng/client/gui/me/patternaccess/PatternAccessTermScreen'


def cpu_renderer_contract(target, native):
    """Branch into desktop drawing; retain the entire installed native body."""
    owner=target['class'];method=copy.deepcopy(native)
    frame={'type':14,'op':-1,'frameType':-1,'locals':[{'kind':'string','value':owner},
        {'kind':'string','value':'net/minecraft/client/gui/GuiGraphics'},
        {'kind':'string','value':'net/minecraft/client/renderer/Rect2i'},
        {'kind':'string','value':'appeng/client/Point'}],'stack':[]}
    prefix=[]
    def self_field(name,desc):
        prefix.extend([{'type':2,'op':25,'var':0},{'type':4,'op':180,'owner':owner,'name':name,'desc':desc}])
    self_field('desktop','Z');prefix.append({'type':7,'op':154,'label':'legacy_cpu_hosted'})
    self_field('desktopNarrow','Z');prefix.append({'type':7,'op':153,'label':'legacy_cpu_native'})
    prefix.extend([{'type':8,'op':-1,'label':'legacy_cpu_hosted'},copy.deepcopy(frame)])
    prefix.extend([{'type':2,'op':25,'var':i} for i in range(3)])
    self_field('bounds','Lnet/minecraft/client/renderer/Rect2i;')
    self_field('menu','Lappeng/menu/me/crafting/CraftingStatusMenu;')
    self_field('scrollbar','Lappeng/client/gui/widgets/Scrollbar;')
    self_field('textColor','Lappeng/client/gui/style/Color;');self_field('selectedColor','I')
    for dimension in ['Height','Width']:
        self_field('buttonBg','Lappeng/client/gui/style/Blitter;')
        prefix.append({'type':5,'op':182,'owner':'appeng/client/gui/style/Blitter','name':'getSrc'+dimension,'desc':'()I','interface':False})
    self_field('desktopNarrow','Z')
    prefix.extend([{'type':5,'op':184,'owner':'com/salts_inventory_update/compat/ae2/nativeview/LegacyCpuSelection',
        'name':'render','desc':f'(L{owner};Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/Rect2i;'
        'Lnet/minecraft/client/renderer/Rect2i;Lappeng/menu/me/crafting/CraftingStatusMenu;'
        'Lappeng/client/gui/widgets/Scrollbar;Lappeng/client/gui/style/Color;IIIZ)V','interface':False},
        {'type':0,'op':177},{'type':8,'op':-1,'label':'legacy_cpu_native'},copy.deepcopy(frame)])
    method['instructions']=prefix+method['instructions'];method['maxStack']=max(11,method['maxStack'])
    entry='Lappeng/menu/me/crafting/CraftingStatusMenu$CraftingCpuListEntry;'
    for name,native_name,result in [('siu$desktopCpuName','getCpuName','Lnet/minecraft/network/chat/Component;'),
                                    ('siu$desktopCpuStorage','formatStorage','Ljava/lang/String;')]:
        desc='('+entry+')'+result
        target['methods'].append({'name':name,'desc':desc,'access':1,'signature':None,'exceptions':[],
            'maxStack':2,'maxLocals':2,'handlers':[],'visibleAnnotations':[],'invisibleAnnotations':[],
            'instructions':[{'type':2,'op':25,'var':0},{'type':2,'op':25,'var':1},
                {'type':5,'op':183,'owner':owner,'name':native_name,'desc':desc,'interface':False},{'type':0,'op':176}]})
    return method


def toolbar_attachment_contract(target):
    owner=target['class']
    target['methods'].append({'name':'shouldAddToolbar','desc':'()Z','access':4,'signature':None,
        'exceptions':[],'maxStack':1,'maxLocals':1,'handlers':[],'visibleAnnotations':[],'invisibleAnnotations':[],
        'instructions':[{'type':0,'op':4},{'type':0,'op':172}]})
    constructor=next(m for m in target['methods'] if m['name']=='<init>');nodes=constructor['instructions']
    position=next(i for i,n in enumerate(nodes) if n.get('owner')=='appeng/client/gui/WidgetContainer' and n.get('name')=='add')
    if not any(n.get('constant',{}).get('value')=='verticalToolbar' for n in nodes[:position]):
        raise ValueError('Unknown native toolbar attachment')
    nodes[position:position]=[{'type':2,'op':25,'var':0},
        {'type':5,'op':182,'owner':owner,'name':'shouldAddToolbar','desc':'()Z','interface':False}]
    nodes[position+2].update(op=184,owner='com/salts_inventory_update/compat/ae2/nativeview/LegacyRendering',
        name='attachToolbar',desc='(Lappeng/client/gui/WidgetContainer;Ljava/lang/String;Lappeng/client/gui/ICompositeWidget;Z)V',interface=False)
    constructor['maxStack']=max(4,constructor['maxStack'])
    return target


def cut_frame_contract(target):
    """Add legacy-only drawing adapters; keep the installed native drawBG body."""
    extract = next(m for m in target['methods'] if m['name'] == 'extractDesktopContent')
    call = next(n for n in extract['instructions'] if n.get('name') == 'drawBG')
    call.update(op=184, owner='com/salts_inventory_update/compat/ae2/nativeview/LegacyFrameRendering',
                name='background', desc='(Lappeng/client/gui/AEBaseScreen;' + call['desc'][1:], interface=False)
    method = next(m for m in target['methods'] if m['name'] == 'drawDesktopBottomEdge')
    method.update(instructions=[{'type':2,'op':25,'var':0}, {'type':2,'op':25,'var':1},
        {'type':5,'op':184,'owner':'com/salts_inventory_update/compat/ae2/nativeview/LegacyFrameRendering',
         'name':'bottom','desc':'(Lappeng/client/gui/AEBaseScreen;Lcom/salts_inventory_update/api/client/gui/GuiGraphicsExtractor;)V',
         'interface':False}, {'type':0,'op':177}], handlers=[], maxStack=2)
    return target


def toolbox_inventory_contract(target):
    result=copy.deepcopy(target);owner=result['class']
    constructor=next(m for m in result['methods'] if m['name']=='<init>');nodes=constructor['instructions']
    position=next(i for i,n in enumerate(nodes) if n['op']==177)
    nodes[position:position]=[{'type':2,'op':25,'var':0},{'type':2,'op':25,'var':3},
        {'type':2,'op':25,'var':0},{'type':4,'op':180,'owner':owner,'name':'inv','desc':'Lappeng/util/inv/AppEngInternalInventory;'},
        {'type':5,'op':184,'owner':'com/salts_inventory_update/compat/ae2/menu/LegacyToolboxInventories',
         'name':'share','desc':'(Lnet/minecraft/world/item/ItemStack;Lappeng/util/inv/AppEngInternalInventory;)Lappeng/util/inv/AppEngInternalInventory;','interface':False},
        {'type':4,'op':181,'owner':owner,'name':'inv','desc':'Lappeng/util/inv/AppEngInternalInventory;'}]
    constructor['maxStack']=max(3,constructor['maxStack'])
    result['methods'].append({'name':'siu$isValid','desc':'()Z','access':1,'signature':None,'exceptions':[],
        'maxStack':1,'maxLocals':1,'handlers':[],'visibleAnnotations':[],'invisibleAnnotations':[],
        'instructions':[{'type':2,'op':25,'var':0},
            {'type':5,'op':182,'owner':owner,'name':'ensureItemStillInSlot','desc':'()Z','interface':False},
            {'type':0,'op':172}]})
    return result


def checkbox_contract(target):
    owner = target['class'];desc = '()Lappeng/client/gui/style/Blitter;'
    method = {'name':'siu$desktopIcon','desc':'(Z)Lappeng/client/gui/style/Blitter;','access':1,'signature':None,
              'exceptions':[],'maxStack':1,'maxLocals':2,'handlers':[],'visibleAnnotations':[],'invisibleAnnotations':[],'instructions':[]}
    code = method['instructions']
    frame = {'type':14,'op':-1,'frameType':-1,'locals':[{'kind':'string','value':owner},{'kind':'Integer','value':1}],'stack':[]}
    def check(name,label):
        code.extend([{'type':2,'op':25,'var':0},{'type':5,'op':182,'owner':owner,'name':name,'desc':'()Z','interface':False},{'type':7,'op':153,'label':label}])
    def field(name): code.extend([{'type':4,'op':178,'owner':owner,'name':name,'desc':desc[2:]},{'type':0,'op':176}])
    def label(name): code.extend([{'type':8,'op':-1,'label':name},copy.deepcopy(frame)])
    check('isRadio','square')
    for group,prefix in [('radio','RADIO_'),('square','')]:
        if group=='square':label('square')
        check('isSelected',group+'_unchecked')
        code.extend([{'type':2,'op':21,'var':1},{'type':7,'op':153,'label':group+'_checked_plain'}]);field(prefix+'CHECKED_FOCUS')
        label(group+'_checked_plain');field(prefix+'CHECKED')
        label(group+'_unchecked');code.extend([{'type':2,'op':21,'var':1},{'type':7,'op':153,'label':group+'_unchecked_plain'}]);field(prefix+'UNCHECKED_FOCUS')
        label(group+'_unchecked_plain');field(prefix+'UNCHECKED')
    target['methods'].append(method)
    target['methods'].append({'name':'siu$setHeight','desc':'(I)V','access':1,'signature':None,
        'exceptions':[],'maxStack':2,'maxLocals':2,'handlers':[],'visibleAnnotations':[],'invisibleAnnotations':[],
        'instructions':[{'type':2,'op':25,'var':0},{'type':2,'op':21,'var':1},
                        {'type':4,'op':181,'owner':owner,'name':'height','desc':'I'},{'type':0,'op':177}]})
    native = next(m for m in target['methods'] if m['name']=='renderWidget')
    prefix = [
        {'type':2,'op':25,'var':0},{'type':4,'op':180,'owner':owner,'name':'desktopHosted','desc':'Z'},
        {'type':7,'op':153,'label':'native_checkbox_body'}, {'type':2,'op':25,'var':0},
        {'type':2,'op':25,'var':0},{'type':4,'op':180,'owner':owner,'name':'style','desc':'Lappeng/client/gui/style/ScreenStyle;'},
        {'type':2,'op':25,'var':1},{'type':2,'op':21,'var':2},{'type':2,'op':21,'var':3},
        {'type':5,'op':184,'owner':'com/salts_inventory_update/compat/ae2/nativeview/LegacyCheckboxes','name':'render',
         'desc':f'(L{owner};Lappeng/client/gui/style/ScreenStyle;Lnet/minecraft/client/gui/GuiGraphics;II)V','interface':False},
        {'type':0,'op':177},{'type':8,'op':-1,'label':'native_checkbox_body'},
        {'type':14,'op':-1,'frameType':-1,'locals':[{'kind':'string','value':owner},{'kind':'string','value':'net/minecraft/client/gui/GuiGraphics'},
         {'kind':'Integer','value':1},{'kind':'Integer','value':1},{'kind':'Integer','value':2}],'stack':[]}]
    native['instructions'][0:0]=prefix;native['maxStack']=max(native['maxStack'],5)
    return target


def adapt(value):
    value = menus.replace(value, 'appeng/client/gui/widgets/AE2Button', 'net/minecraft/client/gui/components/Button')
    value = menus.replace(value, 'net/minecraft/network/RegistryFriendlyByteBuf', 'net/minecraft/network/FriendlyByteBuf')
    value = menus.replace(value, 'appeng/menu/locator/MenuHostLocator', 'appeng/menu/locator/MenuLocator')
    value = menus.replace(value, 'appeng/menu/locator/ItemMenuHostLocator', 'appeng/menu/locator/MenuItemLocator')
    for family in ['serverbound', 'clientbound', 'bidirectional']:
        value = menus.replace(value, 'appeng/core/network/' + family + '/', 'appeng/core/sync/packets/')
    value = menus.packet_types(value)
    if 'methods' in value:
        for method in value['methods']:
            nodes = method['instructions']
            if value.get('class')==CATALOG and method['name']=='slotClicked' and any(n.get('desc')=='appeng/core/sync/packets/QuickMovePatternPacket' for n in nodes):
                # The visible-container quick-move packet is a later native
                # AE2 feature. Retain AE2 15's super dispatch for player slots;
                # its existing pattern-slot action still gains desktop routing.
                boundary=next(i for i,n in enumerate(nodes) if n['op']==177)
                call=next(i for i,n in enumerate(nodes) if n.get('owner')=='appeng/client/gui/AEBaseScreen' and n.get('name')=='slotClicked')
                if [n['op'] for n in nodes[call-5:call]]!=[25,25,21,21,25]:raise ValueError('Unknown catalog slot super dispatch')
                method['instructions']=nodes[:boundary+3]+nodes[call-5:]
                nodes=method['instructions']
            if value.get('class')==CATALOG and method['name']=='drawFG':
                if any(n.get('name')=='renderLinkStatus' for n in nodes):
                    end=max(i for i,n in enumerate(nodes) if n.get('op')==132)
                    if [n['type'] for n in nodes[end+1:end+4]]!=[7,8,14]:
                        raise ValueError('Unknown catalog foreground loop exit')
                    # End after the row loop, as the native AE2 15 renderer does.
                    # Drop the unavailable newer link-status tail and its guard.
                    method['instructions']=nodes[:end+4]+[{'type':0,'op':177},{'type':8,'op':-1,'label':'legacy_catalog_fg_end'}]
                    nodes=method['instructions']
            if value.get('class','').endswith('/CraftingCPUScreen') and method['name']=='updateBeforeRender':
                # Suspension was added by a later upstream AE2 release. AE2 15
                # has only the cancel control, whose desktop state is retained.
                for i in range(len(nodes)-1,-1,-1):
                    if nodes[i].get('name')=='suspend' and nodes[i].get('op')==180:
                        expected=[25,180,25,180,180,181]
                        if [n['op'] for n in nodes[i-1:i+5]]!=expected:raise ValueError('Unknown suspension control assignment')
                        del nodes[i-1:i+5]
            if value.get('class','').endswith('/CPUSelectionList') and method['name'] in ['hitTestCpu','drawBackgroundLayer']:
                # AE2 15's ordinary CPU hit area starts at x + 9. Keep the
                # fork's desktop/narrow offsets (4/1), adapting only fallback.
                for node in nodes:
                    if node.get('increment')==-8 and node.get('var')==2:node['increment']=-9
                    if node.get('increment')==8 and node.get('var')==4:node['increment']=9
                    if node.get('operand')==8:node['operand']=9
            if value.get('class','').endswith('EncodingPanel') and method['name'] in ['drawBackgroundLayer','drawModeBackground']:
                for i,node in enumerate(nodes):
                    if node.get('operand')==8 and i+1<len(nodes) and nodes[i+1].get('op')==96:node['operand']=9
                    if node.get('operand')==165 and i+1<len(nodes) and nodes[i+1].get('op')==100:node['operand']=164
            if value.get('class','').endswith('/MEStorageScreen') and method['name']=='drawFG':
                end=next(i for i,n in enumerate(nodes) if n.get('owner')=='appeng/client/gui/me/common/StackSizeRenderer' and n.get('name')=='renderSizeLabel')
                # AE2 15 ends here: its ordinary renderer has no later
                # upstream link-status feature. Keep the shared title skip
                # destination while removing that unavailable source-only tail.
                method['instructions']=nodes[:end+3]+[{'type':0,'op':177},{'type':8,'op':-1,'label':'legacy_storage_fg_end'}]
                nodes=method['instructions']
            if value.get('class','').endswith('/MEStorageScreen') and method['name'] in ['desktopSurfaceSegments','drawDesktopBottomEdge']:
                # The plain terminal footer is 97 pixels in AE2 15. It still
                # occupies the full terminal width; only crafting/encoding step in.
                for node in nodes:
                    if node.get('operand') == 99: node['operand'] = 97
            if method['name']=='mouseScrolled' and method['desc']=='(DDDD)Z':
                for i in range(len(nodes)-1,-1,-1):
                    if nodes[i].get('op')==24 and nodes[i].get('var')==5:
                        if not any(n.get('name')=='mouseScrolled' for n in nodes[i+1:i+4]):raise ValueError('Horizontal scroll participates in native logic')
                        del nodes[i]
                for node in nodes:
                    if node['type'] in [2,10] and node['var']>=7:node['var']-=2
                    if node['type']==14:del node['locals'][3]
                    if node.get('name')=='mouseScrolled' and node.get('desc')=='(DDDD)Z':node['desc']='(DDD)Z'
                method['desc']='(DDD)Z';method['maxLocals']-=2
            if value.get('class') == CATALOG:
                # Native AE2 15's ordinary catalog inventory footer is two
                # pixels shorter. Desktop defaults use that version's sizing too.
                for node in nodes:
                    if node.get('operand') == 116: node['operand'] = 114
                    if method['name'] in ['init','desktopDefaultContentSize'] and node.get('operand')==99:node['operand']=97
                if method['name']=='init':
                    maximum=next((i for i,n in enumerate(nodes) if n.get('owner')=='java/lang/Math' and n.get('name')=='max'),None)
                    if maximum is not None:
                        if any(n.get('name')=='isDesktopHosted' for n in nodes):
                            # The fork requires two desktop rows. Ordinary AE2
                            # 15 does not apply this clamp, so retain its sizing.
                            nodes[maximum:maximum]=[{'type':2,'op':25,'var':0},
                                {'type':5,'op':182,'owner':CATALOG,'name':'isDesktopHosted','desc':'()Z','interface':False}]
                            nodes[maximum+2].update(owner='com/salts_inventory_update/compat/ae2/nativeview/LegacyCatalogRows',
                                                    name='limit',desc='(IIZ)I')
                        else:
                            del nodes[maximum]
                            if nodes[2]['op']!=5:raise ValueError('Unknown catalog native minimum row clamp')
                            del nodes[2]
            for i in range(len(nodes) - 1, -1, -1):
                n = nodes[i]
                if n.get('owner') == 'net/neoforged/neoforge/network/PacketDistributor' and n.get('name') == 'sendToServer':
                    assert nodes[i - 2]['op'] == 3 and nodes[i - 1]['op'] == 189
                    del nodes[i - 2:i]
                    n.update(owner=menus.TRANSPORT, name='sendDefault', desc=f'(L{menus.PACKET};)V')
                elif n.get('owner') == 'appeng/core/sync/network/NetworkHandler' and n.get('name') == 'sendToServer':
                    start = next(j for j in range(i - 1, -1, -1) if nodes[j].get('owner') == 'appeng/core/sync/network/NetworkHandler' and nodes[j].get('name') == 'instance')
                    del nodes[start]
                    n.update(op=184, owner=menus.TRANSPORT, name='sendDefault', interface=False)
                elif n.get('owner') == 'net/minecraft/client/gui/components/Button' and n.get('name') == 'setDesktop':
                    n.update(op=184, owner='com/salts_inventory_update/compat/ae2/nativeview/LegacyButtons',
                             desc='(Lnet/minecraft/client/gui/components/Button;Z)V', interface=False)
                elif n.get('name') == 'renderButtonText' and n.get('owner','').startswith('appeng/client/gui/widgets/') and n.get('desc','').endswith('Lnet/minecraft/client/gui/Font;III)V'):
                    n.update(op=184, owner='com/salts_inventory_update/compat/ae2/nativeview/LegacyButtons',
                             desc='(Lnet/minecraft/client/gui/components/Button;' + n['desc'][1:], interface=False)
                elif n.get('owner') == 'appeng/client/gui/style/Blitter' and n.get('name') == 'guiSprite':
                    n.update(owner='com/salts_inventory_update/compat/ae2/nativeview/LegacyToolbarBacking', name='sprite')
                elif n.get('owner') == 'appeng/client/gui/widgets/AECheckbox' and n.get('name') == 'setHeight' and n.get('desc') == '(I)V':
                    n['name']='siu$setHeight'
                elif n.get('owner') in ['appeng/client/gui/widgets/TabButton','appeng/client/gui/widgets/IconButton'] and n.get('name') == 'setHeight' and n.get('desc') == '(I)V':
                    n.clear(); n.update(type=4,op=181,owner=value['class'],name='height',desc='I')
                elif n.get('owner') == 'net/minecraft/client/KeyMapping' and n.get('name') == 'isActiveAndMatches':
                    n.update(op=184,owner='com/salts_inventory_update/compat/ae2/nativeview/LegacyKeyBinding',
                             desc='(Lnet/minecraft/client/KeyMapping;Lcom/mojang/blaze3d/platform/InputConstants$Key;)Z',interface=False)
                elif n.get('owner') == 'net/minecraft/resources/ResourceLocation' and n.get('name') == 'withDefaultNamespace':
                    n.update(owner='com/salts_inventory_update/compat/ae2/nativeview/LegacyIdentifiers')
            if value.get('class','').endswith('/VerticalButtonBar') and method['name'] == 'backingPiece':
                method['instructions'] = ([{'type':2,'op':25,'var':0},{'type':2,'op':25,'var':1}] +
                    [{'type':2,'op':21,'var':i} for i in range(2,10)] +
                    [{'type':5,'op':184,'owner':'com/salts_inventory_update/compat/ae2/nativeview/LegacyToolbarBacking',
                      'name':'piece','desc':method['desc'],'interface':False},{'type':0,'op':177}])
                method['handlers'] = []; method['maxStack'] = 10; method['maxLocals'] = 10
    return value


def transfer(name, base, integrated, patch, target):
    merged = copy.deepcopy(target)
    if 'interfaces' in patch:
        if merged['interfaces'] != patch['interfaces']['original']: raise ValueError('Native interfaces differ: ' + name)
        merged['interfaces'].extend(patch['interfaces']['added'])
    if patch['removedMethods']: raise ValueError('Unreviewed desktop removal contract: ' + name)
    for change in patch['fieldChanges']:
        reviewed = (name.endswith('/MEStorageScreen') and change['name']=='style' and change['access']==2) or \
                   (name.endswith('/AbstractTableRenderer') and change['name'] in ['x','y','rows'] and change['access']==2) or \
                   (name.endswith('/CraftingCPUScreen') and change['name']=='table' and change['access']==20)
        if not (reviewed and change['originalAccess']==18):
            raise ValueError('Unreviewed desktop field contract: ' + name)
        field=next(f for f in merged['fields'] if f['name']==change['name'])
        if field['access']!=change['originalAccess'] or field['desc']!=change['desc']:raise ValueError('Native style field differs: '+name)
        field['access']=change['access']
    for f in patch['fields']:
        if any(x['name'] == f['name'] for x in merged['fields']): raise ValueError('Desktop field collides: ' + name + '.' + f['name'])
        merged['fields'].append(copy.deepcopy(f))
    for change in patch['changes']:
        key = change['name'], change['desc']
        before = next(m for m in base['methods'] if (m['name'], m['desc']) == key)
        after = next(m for m in integrated['methods'] if (m['name'], m['desc']) == key)
        old = next((m for m in merged['methods'] if (m['name'], m['desc']) == key), None)
        if name.endswith('/CPUSelectionList') and key[0]=='drawBackgroundLayer' and old is not None:
            method=cpu_renderer_contract(merged,old)
            merged['methods'][merged['methods'].index(old)]=method
            continue
        if name.endswith('/FakeSlot') and key[0]=='setFilterTo' and old is not None:
            # The older bytecode forwards the freshly constructed packet
            # directly. Retain that native construction, adding only the fork's
            # owning-menu routing and its scoped fallback.
            call=next(i for i,n in enumerate(old['instructions']) if n.get('name')=='sendDefault')
            if [g.shape(n) for n in old['instructions'][:call]]!=[g.shape(n) for n in before['instructions'][:call]]:
                raise ValueError('Native ghost filter packet construction differs')
            merged['methods'][merged['methods'].index(old)]=copy.deepcopy(after)
            continue
        if name.rsplit('/',1)[-1] in ['TabButton','IconButton','ProgressBar'] and key[0] == 'renderWidget' and old is not None:
            insertion = change['edits'][0]
            if insertion['from'] != insertion['to']: raise ValueError('Widget branch replaces native instructions: ' + name)
            position = insertion['from']
            boundary = 8 if name.endswith('/IconButton') else position
            if [g.shape(n) for n in before['instructions'][:boundary]] != [g.shape(n) for n in old['instructions'][:boundary]]:
                raise ValueError('Widget branch insertion boundary differs: ' + name)
            prefix = copy.deepcopy(insertion['insert'])
            defined = {n['label'] for n in prefix if n['type'] == 8}
            external = {n['label'] for n in prefix if n['type'] == 7} - defined
            if len(external) != 1: raise ValueError('Widget branch has unexpected exits: ' + name)
            prefix = menus.replace(prefix, next(iter(external)), 'native20_widget_body')
            if name.endswith('/IconButton'):
                # AE2 15 exposes icon blitters, without the later native item
                # overlay API. Its original blitter setup remains in place.
                position = boundary
                prefix = [{'type': 0, 'op': 1}, {'type': 2, 'op': 58, 'var': 6}] + prefix
            locals_ = [{'kind': 'string', 'value': name}, {'kind': 'string', 'value': 'net/minecraft/client/gui/GuiGraphics'},
                {'kind': 'Integer', 'value': 1}, {'kind': 'Integer', 'value': 1}, {'kind': 'Integer', 'value': 2}]
            if name.endswith('/IconButton'):
                locals_ += [{'kind': 'string', 'value': 'appeng/client/gui/Icon'}, {'kind': 'string', 'value': 'net/minecraft/world/item/Item'}]
            prefix += [{'type': 8, 'op': -1, 'label': 'native20_widget_body'},
                {'type': 14, 'op': -1, 'frameType': -1, 'locals': locals_, 'stack': []}]
            old['instructions'][position:position] = prefix
            old['maxStack'] = max(old['maxStack'], change['maxStack'])
            old['maxLocals'] = max(old['maxLocals'], change['maxLocals'])
            continue
        if old is None and name.endswith('/VerticalButtonBar') and key[0] == 'drawBackgroundLayer':
            # AE2 15 inherits the empty composite background method. Keep that
            # ordinary behavior and add only the fork's hosted drawing branch.
            method = copy.deepcopy(after)
            start = next(i for i, n in enumerate(method['instructions']) if n.get('owner') == name and n.get('name') == 'aeDesktop')
            assert method['instructions'][start - 1].get('var') == 0
            end = next(i for i in range(start + 1, len(method['instructions'])) if method['instructions'][i]['op'] == 177)
            resume = next(n for n in method['instructions'][end + 1:] if n['type'] == 8)
            method['instructions'] = method['instructions'][:start - 1] + [resume,
                {'type': 14, 'op': -1, 'frameType': -1, 'locals': [{'kind': 'string', 'value': name},
                    {'kind': 'string', 'value': 'net/minecraft/client/gui/GuiGraphics'},
                    {'kind': 'string', 'value': 'net/minecraft/client/renderer/Rect2i'},
                    {'kind': 'string', 'value': 'appeng/client/Point'}], 'stack': []}, {'type': 0, 'op': 177}]
            merged['methods'].append(method)
            continue
        if old is None: raise ValueError('Native signature requires an adapter: ' + name + '.' + str(key))
        try: method = t.merge_method(before, after, old)
        except ValueError as error: raise ValueError(name + '.' + key[0] + ': ' + str(error)) from error
        merged['methods'][merged['methods'].index(old)] = method
    for method in patch['methods']:
        if any((x['name'], x['desc']) == (method['name'], method['desc']) for x in merged['methods']):
            raise ValueError('Added method collides: ' + name + '.' + method['name'])
        merged['methods'].append(copy.deepcopy(method))
    return merged


def factories(base, integrated, patch, target):
    merged = copy.deepcopy(target)
    merged['fields'].extend(copy.deepcopy(patch['fields']))
    merged['methods'].extend(copy.deepcopy(patch['methods']))
    register = next(m for m in merged['methods'] if m['name'] == 'register')
    register['instructions'][0:0] = [
        {'type': 4, 'op': 178, 'owner': FACTORIES, 'name': 'DESKTOP_FACTORIES', 'desc': 'Ljava/util/Map;'},
        {'type': 2, 'op': 25, 'var': 0}, {'type': 2, 'op': 25, 'var': 1},
        {'type': 5, 'op': 185, 'owner': 'java/util/Map', 'name': 'put', 'desc': '(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;', 'interface': True},
        {'type': 0, 'op': 87}]
    register['maxStack'] = max(3, register['maxStack'])
    changed = next(c for c in patch['changes'] if c['name'] == '<clinit>')
    before = next(m for m in base['methods'] if m['name'] == '<clinit>')
    after = next(m for m in integrated['methods'] if m['name'] == '<clinit>')
    old = next(m for m in merged['methods'] if m['name'] == '<clinit>')
    merged['methods'][merged['methods'].index(old)] = t.merge_method(before, after, old)
    return merged


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--loader', choices=['fabric', 'forge'], required=True)
    parser.add_argument('--namespace', choices=['named', 'runtime'], default='named')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--minecraft-mappings', type=Path)
    parser.add_argument('--minecraft-jar', type=Path)
    parser.add_argument('--include-specialized', action='store_true')
    parser.add_argument('--include-catalog', action='store_true')
    parser.add_argument('--catalog-only', action='store_true', help='Regenerate only the catalog without touching other staged contracts')
    parser.add_argument('--classes', nargs='+', help='Regenerate only the specified native screen contracts')
    parser.add_argument('--include-common', action='store_true')
    parser.add_argument('--defer-cpu-renderer', action='store_true',
                        help='Stage other contracts while retaining AE2 15 CPU drawing pending its separate adapter')
    args = parser.parse_args()
    if args.catalog_only: args.include_catalog = True
    inventories = ROOT / 'build/ae2-port/patch-tools'
    source = ROOT / 'compatibility/ae2/versions/1.21.1/neoforge/src/main/resources/ae2-native-patches'
    named_folder = inventories / f'1.20.1-{args.loader}-named'
    manifest_file = args.output / 'manifest.json'; manifest = json.loads(manifest_file.read_text(encoding='utf-8'))
    # Generated contracts need renewed runtime review before release activation.
    manifest['clientPortComplete'] = False
    manifest['runtimeValidationPending'] = True
    names = [SCREEN, 'appeng/client/gui/WidgetContainer', 'appeng/client/gui/ICompositeWidget',
             'appeng/client/gui/widgets/Scrollbar', FACTORIES, CATALOG]
    names += ['appeng/client/gui/widgets/' + n for n in ['VerticalButtonBar','TabButton','IconButton','AECheckbox','ProgressBar']]
    if args.include_specialized:
        catalog=json.loads((source/'manifest.json').read_text(encoding='utf-8'))
        candidates=[e['class'] for e in catalog['classes'] if e['class'].startswith('appeng/client/')]
        missing=[]
        for name in candidates:
            if name in names: continue
            if (named_folder/(name+'.json')).exists(): names.append(name)
            else: missing.append(name)
        manifest['specializedNativeAdaptersPending']=missing
        if missing: print('Native classes requiring alternate-version adapters:', ', '.join(missing))
        # The specialized crafting screens consume these fork-added synced
        # fields, so transfer their producer in the same staging operation.
        names.append('appeng/menu/me/crafting/CraftingCPUMenu')
    if args.include_common:
        catalog=json.loads((source/'manifest.json').read_text(encoding='utf-8'))
        for entry in catalog['classes']:
            name=entry['class']
            if name in names or name in [menus.MENU,menus.BUILDER] or name.startswith(('appeng/client/','appeng/init/client/')):continue
            if (named_folder/(name+'.json')).exists():names.append(name)
    if args.catalog_only: names = [CATALOG]
    if args.classes: names = args.classes
    for name in names:
        raw_base = json.loads((inventories / '1.21.1-neoforge' / (name + '.json')).read_text(encoding='utf-8'))
        raw_patch = json.loads((source / (name + '.json')).read_text(encoding='utf-8'))
        base = adapt(raw_base)
        integrated = adapt(t.apply_to_inventory(raw_base, raw_patch))
        patch = g.generate(base, integrated)
        if name.endswith('/CPUSelectionList') and args.defer_cpu_renderer:
            patch['changes'] = [m for m in patch['changes'] if m['name'] != 'drawBackgroundLayer']
            manifest['cpuRendererPortPending'] = True
        elif name.endswith('/CPUSelectionList'):manifest['cpuRendererPortPending'] = False
        if (name.endswith('/MEStorageScreen') or name==CATALOG) and not (named_folder/'appeng/api/storage/ILinkStatus.json').exists():
            # This alignment hook belongs to a later upstream AE2 feature.
            # Native AE2 15 has no link-status API or renderer to reposition.
            patch['changes']=[m for m in patch['changes'] if m['name']!='renderLinkStatus']
            patch['methods']=[m for m in patch['methods'] if m['name']!='renderLinkStatus']
            hints=next(m for m in patch['methods'] if m['name']=='drawDesktopBodyOverlayHints')
            hints.update(instructions=[{'type':0,'op':177}],handlers=[],maxStack=0)
            manifest['nativeLinkStatusUnavailable']=True
        if name.endswith('/AECheckbox'):
            # Preserve the old focused-radio renderer for ordinary AE2 views;
            # hosted rendering uses the fork's explicit geometry and labels.
            patch['changes'] = []
        original_target = json.loads((named_folder / (name + '.json')).read_text(encoding='utf-8'))
        target = adapt(copy.deepcopy(original_target))
        if name == CATALOG and not args.include_catalog:
            patch = {**patch, 'fields': [], 'fieldChanges': [], 'changes': [], 'removedMethods': [],
                     'methods': [m for m in patch['methods'] if m['name'] == 'desktopDefaultContentSize']}
            if args.include_specialized: manifest['catalogLayoutAndInputPending'] = True
        elif name == CATALOG:
            manifest['catalogLayoutAndInputPending'] = False
            manifest['nativeVisibleContainerQuickMoveUnavailable'] = True
        merged = (toolbox_inventory_contract(target) if name.endswith('/NetworkToolMenuHost')
                  else factories(base, integrated, patch, target) if name == FACTORIES else transfer(name, base, integrated, patch, target))
        if name==SCREEN:
            merged=toolbar_attachment_contract(merged)
            merged=cut_frame_contract(merged)
        if name.endswith('/AECheckbox'): merged = checkbox_contract(merged)
        if name.endswith('/StyleManager'):
            method = next(m for m in merged['methods'] if m['name'] == 'loadIndependentStyleDoc')
            position = next(i for i,n in enumerate(method['instructions']) if n.get('owner') == 'appeng/client/gui/style/ScreenStyle' and n.get('name') == 'GSON')
            method['instructions'][position:position] = [
                {'type':2,'op':25,'var':1},
                {'type':5,'op':184,'owner':'com/salts_inventory_update/compat/ae2/nativeview/LegacyDesktopStyle',
                 'name':'prepare','desc':'(Lcom/google/gson/JsonObject;)V','interface':False}]
        g.session_cpu_state(merged)
        if args.namespace == 'runtime':
            original_named = original_target
            target = json.loads((inventories / f'1.20.1-{args.loader}' / (name + '.json')).read_text(encoding='utf-8'))
            merged = menus.remap_runtime(merged, inventories / f'1.20.1-{args.loader}', named_folder, original_named, args.minecraft_mappings, args.minecraft_jar)
        else:
            target = original_target
        try: actual = g.generate(target, merged)
        except ValueError:
            (ROOT/'build/ae2-port/screen-merge-error.json').write_text(json.dumps(merged,indent=2)+'\n',encoding='utf-8')
            raise
        output = args.output / (name + '.json'); output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(g.canonical(actual), encoding='utf-8')
        expected = inventories / f'1.20.1-{args.loader}-desktop-style-{args.namespace}' / (name + '.json')
        expected.parent.mkdir(parents=True, exist_ok=True); expected.write_text(g.canonical(merged), encoding='utf-8')
        manifest['classes'] = [e for e in manifest['classes'] if e['class'] != name]
        scope = ('hosted checkbox rendering and label/tooltip contract' if name.endswith('/AECheckbox')
                 else 'desktop catalog layout and native slot-action routing' if name == CATALOG and args.include_catalog
                 else 'catalog sizing only' if name == CATALOG else 'crafting status synchronization' if name.endswith('/CraftingCPUMenu')
                 else 'desktop screen layout/input contract')
        manifest['classes'].append({'class': name, 'fields': len(actual['fields']), 'methods': len(actual['methods']),
                                    'changes': len(actual['changes']), 'scope': scope})
        manifest['scope'] = 'staged style/menu/screen contracts; specialized views and runtime activation pending'
        manifest_file.write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
        print('Transferred desktop screen contract:', name)
    if args.include_common:
        name=menus.BUILDER
        original=json.loads((named_folder/(name+'.json')).read_text(encoding='utf-8'));target=original
        source_patch=json.loads((source/(name+'.json')).read_text(encoding='utf-8'))
        merged=menus.builder_contract(original,source_patch,capture_menu=True)
        if args.namespace=='runtime':
            target=json.loads((inventories/f'1.20.1-{args.loader}'/(name+'.json')).read_text(encoding='utf-8'))
            merged=menus.remap_runtime(merged,inventories/f'1.20.1-{args.loader}',named_folder,original,args.minecraft_mappings,args.minecraft_jar)
        actual=g.generate(target,merged)
        (args.output/(name+'.json')).write_text(g.canonical(actual),encoding='utf-8')
        expected=inventories/f'1.20.1-{args.loader}-desktop-style-{args.namespace}'/(name+'.json')
        expected.write_text(g.canonical(merged),encoding='utf-8')
        manifest['classes']=[e for e in manifest['classes'] if e['class']!=name]
        manifest['classes'].append({'class':name,'fields':len(actual['fields']),'methods':len(actual['methods']),
            'changes':len(actual['changes']),'scope':'native opening/factory bridges and server menu capture'})
        manifest_file.write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
        print('Transferred native server menu creation capture:',name)
        if args.loader=='fabric':
            name=menus.BUILDER+'$HandlerFactory'
            original=json.loads((named_folder/(name+'.json')).read_text(encoding='utf-8'));target=original
            merged=copy.deepcopy(original);factory=next(m for m in merged['methods'] if m['name']=='createMenu')
            menus.capture_created_menu(factory)
            if args.namespace=='runtime':
                target=json.loads((inventories/'1.20.1-fabric'/(name+'.json')).read_text(encoding='utf-8'))
                merged=menus.remap_runtime(merged,inventories/'1.20.1-fabric',named_folder,original,args.minecraft_mappings,args.minecraft_jar)
            actual=g.generate(target,merged)
            (args.output/(name+'.json')).write_text(g.canonical(actual),encoding='utf-8')
            expected=inventories/f'1.20.1-fabric-desktop-style-{args.namespace}'/(name+'.json')
            expected.write_text(g.canonical(merged),encoding='utf-8')
            manifest['classes']=[e for e in manifest['classes'] if e['class']!=name]
            manifest['classes'].append({'class':name,'fields':0,'methods':0,'changes':1,
                'scope':'capture the native Fabric menu after creation and locator assignment'})
            manifest_file.write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
            print('Transferred native Fabric handler factory capture:',name)


if __name__ == '__main__': main()
