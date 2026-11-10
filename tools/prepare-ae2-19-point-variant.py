"""Rebase SIU deltas onto Applied Botanics' published AE2 19.2.17 baseline.

Only integration deltas are packaged. Original point-release instructions and
textures remain in the publisher JAR. Overlapping changes fail for review.
"""
from pathlib import Path
import copy,importlib,json

g=importlib.import_module('generate-ae2-native-deltas')
t=importlib.import_module('transfer-ae2-native-deltas')
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'compatibility/ae2/versions/1.21.1/neoforge/src/main/resources/ae2-native-patches'
OUT=ROOT/'build/all-target-rollout/ae2-point-releases'
DEST=SOURCE.parent/'ae2-native-patches-19.2.17'

def main():
    manifest=json.loads((SOURCE/'manifest.json').read_text());variants=[]
    for entry in manifest['classes']:
        name=entry['class'];base=json.loads((OUT/'19.2.18'/(name+'.json')).read_text())
        native=json.loads((OUT/'19.2.17'/(name+'.json')).read_text())
        patch=json.loads((SOURCE/(name+'.json')).read_text())
        integrated=t.apply_to_inventory(base,patch)
        merged=copy.deepcopy(native)
        if 'interfaces' in patch:
            assert native['interfaces']==patch['interfaces']['original'],name
            merged['interfaces']+=patch['interfaces']['added']
        merged['fields']+=patch['fields']
        for change in patch['fieldChanges']:
            field=next(x for x in merged['fields'] if x['name']==change['name'])
            assert field['access']==change['originalAccess'],name
            for key in ['access','visibleAnnotations','invisibleAnnotations']:
                if key in change:field[key]=change[key]
        for change in patch['changes']:
            key=change['name'],change['desc']
            before=next(x for x in base['methods'] if (x['name'],x['desc'])==key)
            old=next(x for x in merged['methods'] if (x['name'],x['desc'])==key)
            after=next(x for x in integrated['methods'] if (x['name'],x['desc'])==(change.get('rename',key[0]),key[1]))
            merged['methods'][merged['methods'].index(old)]=t.merge_method(before,after,old)
        for removed in patch['removedMethods']:
            merged['methods']=[x for x in merged['methods'] if (x['name'],x['desc'])!=(removed['name'],removed['desc'])]
        merged['methods']+=patch['methods']
        delta=g.generate(native,merged)
        # Every class is retained in the build-only expected inventory, so the
        # verifier checks unchanged native methods as well as desktop hooks.
        expected=OUT/'19.2.17-desktop'/(name+'.json');expected.parent.mkdir(parents=True,exist_ok=True)
        expected.write_text(g.canonical(merged),encoding='utf-8')
        if delta!=patch:
            path=DEST/(name+'.json');path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(g.canonical(delta),encoding='utf-8');variants.append(name)
        # Verifier input uses a complete folder without changing shipping primary patches.
        path=OUT/'19.2.17-deltas'/(name+'.json');path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text(g.canonical(delta),encoding='utf-8')
    (OUT/'19.2.17-deltas/manifest.json').write_text(json.dumps(manifest,indent=2))
    (DEST/'variant.json').write_text(json.dumps({'nativeVersion':'19.2.17','classes':variants,'dependencyReference':'https://github.com/ramidzkh/Applied-Botanics/blob/1.6.0-alpha.3/gradle.properties'},indent=2))
    print('Prepared',len(variants),'point-release class deltas; no publisher classes or textures copied')

if __name__=='__main__':main()
