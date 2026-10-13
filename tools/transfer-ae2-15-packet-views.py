"""Expose decoded AE2 15 packet data to the extension's lifetime-addressed views.

Native codecs, packet handlers and registration remain in the upstream JAR.
These read-only views are necessary because the older handlers target Minecraft's
single current screen, whereas desktop updates target one authenticated lifetime.
"""
import argparse
import copy
import importlib
import json
from pathlib import Path

g=importlib.import_module('generate-ae2-native-deltas')
menus=importlib.import_module('transfer-ae2-15-menu-deltas')
FIELDS={
    'FillCraftingGridFromRecipePacket':[],
    'MEInventoryUpdatePacket':['fullUpdate','entries'],
    'CraftConfirmPlanPacket':['plan'],
    'CraftingStatusPacket':['status'],
    'NetworkStatusPacket':['status'],
    'PatternAccessTerminalPacket':['fullUpdate','inventoryId','inventorySize','sortBy','group','slots'],
}


def view_contract(target, fields):
    result=copy.deepcopy(target);owner=result['class']
    if owner.endswith('/FillCraftingGridFromRecipePacket'):
        constructor=next(m for m in result['methods'] if m['name']=='<init>' and m['desc']=='(Lnet/minecraft/network/FriendlyByteBuf;)V')
        calls=[n for n in constructor['instructions'] if n.get('owner')=='net/minecraft/network/FriendlyByteBuf' and n.get('name')=='readInt']
        if len(calls)!=1:raise ValueError('Unreviewed native recipe grid size decoder')
        calls[0].update(op=184,owner='com/salts_inventory_update/compat/ae2/menu/LegacyRecipeBounds',
                        name='readSize',desc='(Lnet/minecraft/network/FriendlyByteBuf;)I',interface=False)
    for name in fields:
        field=next(f for f in result['fields'] if f['name']==name)
        desc=field['desc'];opcode=176 if desc.startswith(('L','[')) else 173 if desc=='J' else 172
        method_name='siu$'+name
        if any(m['name']==method_name for m in result['methods']):raise ValueError('Packet view already exists: '+method_name)
        result['methods'].append({'name':method_name,'desc':'()'+desc,'access':1,'signature':None,'exceptions':[],
            'handlers':[],'maxStack':2 if desc=='J' else 1,'maxLocals':1,'visibleAnnotations':[],'invisibleAnnotations':[],
            'instructions':[{'type':2,'op':25,'var':0},{'type':4,'op':180,'owner':owner,'name':name,'desc':desc},
                            {'type':0,'op':opcode}]})
    return result


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--loader',choices=['fabric','forge'],required=True)
    parser.add_argument('--namespace',choices=['named','runtime'],default='named')
    parser.add_argument('--output',type=Path,required=True)
    parser.add_argument('--minecraft-mappings',type=Path);parser.add_argument('--minecraft-jar',type=Path)
    args=parser.parse_args();inventories=g.ROOT/'build/ae2-port/patch-tools'
    named=inventories/f'1.20.1-{args.loader}-named';raw=inventories/f'1.20.1-{args.loader}'
    manifest_file=args.output/'manifest.json';manifest=json.loads(manifest_file.read_text())
    for simple,fields in FIELDS.items():
        owner='appeng/core/sync/packets/'+simple
        original=json.loads((named/(owner+'.json')).read_text());target=original;merged=view_contract(target,fields)
        if args.namespace=='runtime':
            target=json.loads((raw/(owner+'.json')).read_text())
            merged=menus.remap_runtime(merged,raw,named,original,args.minecraft_mappings,args.minecraft_jar)
        patch=g.generate(target,merged)
        output=args.output/(owner+'.json');output.parent.mkdir(parents=True,exist_ok=True)
        output.write_text(g.canonical(patch),encoding='utf-8')
        expected=inventories/f'1.20.1-{args.loader}-desktop-style-{args.namespace}'/(owner+'.json')
        expected.parent.mkdir(parents=True,exist_ok=True);expected.write_text(g.canonical(merged),encoding='utf-8')
        manifest['classes']=[e for e in manifest['classes'] if e['class']!=owner]
        manifest['classes'].append({'class':owner,'fields':0,'methods':len(fields),'changes':len(patch['changes']),
                                    'scope':'fork grid-size bound with native integer encoding' if not fields
                                    else 'decoded packet views; native codec and ordinary dispatch unchanged'})
        print('Transferred native packet views:',owner)
    manifest_file.write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')


if __name__=='__main__':main()
