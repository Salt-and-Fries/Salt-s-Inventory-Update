"""Generate inert marker mixins for the verified AE2 15 instruction contracts."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'compatibility/ae2/versions/1.20.1/common/src/main/java/com/salts_inventory_update/compat/ae2/mixin/nativehooks'
RESOURCE=ROOT/'compatibility/ae2/versions/1.20.1/common/src/main/resources/salts_inventory_compat.ae2-native.mixins.json'


if __name__=='__main__':
    names=[]
    for loader in ['forge','fabric']:
        manifest=json.loads((ROOT/f'compatibility/ae2/versions/1.20.1/{loader}/src/main/resources/ae2-native-patches/manifest.json').read_text())
        names.append(sorted(e['class'] for e in manifest['classes']))
    all_names=sorted(set(names[0])|set(names[1]))
    config={'required':True,'minVersion':'0.8','package':'com.salts_inventory_update.compat.ae2.mixin',
            'plugin':'com.salts_inventory_update.compat.ae2.mixin.NativeDesktopPatchPlugin','mixins':[],'client':[],
            'injectors':{'defaultRequire':1}}
    SOURCE.mkdir(parents=True,exist_ok=True)
    for i,name in enumerate(all_names):
        path=ROOT/'build/ae2-port/patch-tools/1.20.1-forge-named'/(name+'.json')
        if not path.exists():path=ROOT/'build/ae2-port/patch-tools/1.20.1-fabric-named'/(name+'.json')
        native=json.loads(path.read_text())
        kind='interface' if native['access']&512 else 'abstract class'
        simple='NativeHook'+str(i)
        text='package com.salts_inventory_update.compat.ae2.mixin.nativehooks;\nimport org.spongepowered.asm.mixin.Mixin;\nimport org.spongepowered.asm.mixin.Pseudo;\n'
        text+=f'@Pseudo\n@Mixin(targets = "{name.replace("/",".")}", priority = 2000, remap = false)\npublic {kind} {simple} {{ }}\n'
        (SOURCE/(simple+'.java')).write_text(text,encoding='utf-8')
        side='client' if name.startswith(('appeng/client/','appeng/init/client/')) else 'mixins'
        config[side].append('nativehooks.'+simple)
    RESOURCE.write_text(json.dumps(config,indent=2)+'\n',encoding='utf-8')
    print('Generated',len(all_names),'optional verified native markers')
