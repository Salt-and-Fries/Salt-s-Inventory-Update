"""Rebuild only the integration deltas against complete production symbol mappings."""
import importlib
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
menus=importlib.import_module('transfer-ae2-15-menu-deltas')
transfer=importlib.import_module('transfer-ae2-native-deltas')
generate=importlib.import_module('generate-ae2-native-deltas')
inventory=ROOT/'build/ae2-port/patch-tools'
resources=ROOT/'compatibility/ae2/versions/1.20.1/fabric/src/main/resources'
named=resources/'ae2-native-patches'
runtime=resources/'ae2-native-patches-runtime'
cache=Path.home()/'.gradle/caches'
mappings=cache/'fabric-loom/1.20.1/loom.mappings.1_20_1.layered+hash.2198-v2/mappings.tiny'
minecraft=next((cache/'fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/1.20.1-loom.mappings.1_20_1.layered+hash.2198-v2').glob('*.jar'))
manifest=json.loads((named/'manifest.json').read_text())
changed=[]
outputs=[]
for entry in manifest['classes']:
    relative=entry['class']+'.json'
    original=json.loads((inventory/'1.20.1-fabric-named'/relative).read_text())
    patch=json.loads((named/relative).read_text())
    # Remap integration instructions only. Original method guards and untouched
    # native instruction ranges remain byte-for-byte owned by the existing ABI.
    carrier=dict(class_=entry['class'])
    carrier={'class':entry['class'],'fields':patch['fields'],
        'methods':patch['methods']+[dict(c) for c in patch['changes']]}
    mapped=menus.remap_runtime(carrier,inventory/'1.20.1-fabric',inventory/'1.20.1-fabric-named',
        None,mappings,minecraft)
    target=runtime/relative
    result=json.loads(target.read_text())
    assert len(result['methods'])==len(patch['methods'])
    result['fields']=mapped['fields']
    result['methods']=mapped['methods'][:len(patch['methods'])]
    changes=mapped['methods'][len(patch['methods']):]
    assert len(changes)==len(result['changes'])
    for source,existing in zip(changes,result['changes']):
        assert (source['name'],source['desc'])==(existing['name'],existing['desc']), (relative,source['name'])
        assert [(e['from'],e['to'])for e in source['edits']]==[(e['from'],e['to'])for e in existing['edits']]
        existing['edits']=source['edits']
        for key in ['handlers','visibleAnnotations','invisibleAnnotations']:
            if key in source:existing[key]=source[key]
    text=generate.canonical(result)
    if target.read_text()!=text:
        changed.append(entry['class']);outputs.append((target,text))
for target,text in outputs:target.write_text(text,encoding='utf-8')
print(json.dumps({'changed_contracts':changed,'native_method_guards_preserved':len(manifest['classes'])},indent=2))

# The Forge contracts use a separate native mapping, but our public member
# names are stable in both environments. Restore only owned references from
# their matching named delta; leave native method guards and ranges untouched.
forge=ROOT/'compatibility/ae2/versions/1.20.1/forge/src/main/resources'
restored=[]
def owned_names(source,target):
    if isinstance(source,list) and isinstance(target,list):
        assert len(source)==len(target)
        for a,b in zip(source,target):owned_names(a,b)
    elif isinstance(source,dict) and isinstance(target,dict):
        if source.get('owner','').startswith('com/salts_inventory_update/') and 'name' in source:
            target['name']=source['name']
        for key in source.keys()&target.keys():
            if isinstance(source[key],(dict,list)):owned_names(source[key],target[key])
for source in (forge/'ae2-native-patches').rglob('*.json'):
    if source.name=='manifest.json':continue
    target=forge/'ae2-native-patches-runtime'/source.relative_to(forge/'ae2-native-patches')
    if not target.exists():continue
    a=json.loads(source.read_text());b=json.loads(target.read_text());before=generate.canonical(b)
    owned_names(a,b);after=generate.canonical(b)
    if after!=before:target.write_text(after,encoding='utf-8');restored.append(a['class'])
print(json.dumps({'forge_owned_api_contracts_restored':restored}))
