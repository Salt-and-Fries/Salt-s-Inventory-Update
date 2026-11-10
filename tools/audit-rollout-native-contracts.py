"""Inspect optional mixin targets and reviewed screen names in publisher binaries.

This is an ABI check, not a gameplay or visual test. Missing required methods
remain explicit failures; class presence alone is never called compatibility.
"""
from pathlib import Path
import json,re,zipfile,io
from controlify_classfile import read_class
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/all-target-rollout'

def main():
    availability=json.loads((OUT/'availability.json').read_text())
    results={}
    for target in availability['targets']:
        version,loader=target['minecraft'],target['loader'];key=version+'/'+loader
        jars={};classes={};mods=set()
        def index(z,origin):
            for name in z.namelist():
                if name.endswith('.class'):classes.setdefault(name[:-6].replace('/','.'),[]).append((origin,name))
                elif name.endswith('.jar') and name.startswith('META-INF/'):
                    data=z.read(name);nested=zipfile.ZipFile(io.BytesIO(data));nested_origin=origin+'!'+name
                    jars[nested_origin]=nested;index(nested,nested_origin)
        for project in availability['projects'].values():
            release=project['targets'].get(key)
            if not release:continue
            mods.update(project['mod_ids'])
            path=OUT/'upstream'/(release['sha512'][:16]+'-'+release['filename'])
            if not path.exists():continue
            origin=str(path.relative_to(ROOT))
            if origin in jars:continue
            z=zipfile.ZipFile(path);jars[origin]=z;index(z,origin)
        source=ROOT/f'compatibility/versions/{version}/fabric/src/main/java/com/salts_inventory_update/compat'
        checks=[]
        for path in source.rglob('*Mixin.java'):
            text=path.read_text();m=re.search(r'@Mixin\(targets\s*=\s*(\{[^}]+\}|"[^"]+")',text,re.S)
            if not m:continue
            names=re.findall(r'"([\w.$]+)"',m[1]);methods=[]
            for match in re.finditer(r'@(?:Inject|Redirect|ModifyVariable|ModifyArg|ModifyExpressionValue|ModifyReturnValue|WrapOperation)\(\s*method\s*=\s*(\{[^}]+\}|"[^"]+")',text,re.S):
                methods.append(re.findall(r'"([^"(]+)(?:\([^"\n]*)?"',match[1]))
            for name in names:
                inputs=classes.get(name,[])
                if not inputs:continue # Optional owner absent; no mixin should apply.
                for origin,entry in inputs[:1]:
                    cf=read_class(jars[origin].read(entry));actual={m[0] for m in cf.methods}
                    for alternatives in methods:
                        found=sorted(actual.intersection(alternatives))
                        checks.append({'mixin':str(path.relative_to(ROOT)),'target':name,'expected_methods':alternatives,'found':found,'status':'present' if found else 'guarded-feature-absent' if path.name=='LimitlessSynchronizerMixin.java' and 'setActualSynchronizer' not in actual else 'missing-native-method','input':origin})
        profiles=[]
        catalog=ROOT/f'compatibility/versions/{version}/common/src/main/resources/salts_inventory_compat/native-windows/project-everything.json'
        if catalog.exists():
            for p in json.loads(catalog.read_text()):
                if not mods.intersection(p['mods']):continue
                name=p['screen'];found=classes.get(name,[]) or next((classes.get(alias,[]) for alias in p.get('screens',[]) if alias in classes),[])
                alternatives=[c for c in classes if c.rsplit('.',1)[-1]==name.rsplit('.',1)[-1]] if not found else []
                profiles.append({'id':p['id'],'mods':p['mods'],'screen':name,'status':'present' if found else 'minecraft-owned' if name.startswith('net.minecraft.') else 'unique-relocation' if len(alternatives)==1 else 'missing-native-class','alternatives':alternatives})
        results[key]={'mixin_checks':checks,'reviewed_profiles':profiles,'indexed_classes':len(classes),'native_jars':len(jars)}
        for z in jars.values():z.close()
        failures=[c for c in checks if c['status']!='present'];missing=[p for p in profiles if p['status']not in ['present','minecraft-owned']]
        print(key,len(checks),'mixin methods,',len(failures),'missing;',len(profiles),'profiles,',len(missing),'need review',flush=True)
        (OUT/'native-contracts-progress.json').write_text(json.dumps(results,indent=2))
    (OUT/'native-contracts.json').write_text(json.dumps(results,indent=2))

if __name__=='__main__':main()
