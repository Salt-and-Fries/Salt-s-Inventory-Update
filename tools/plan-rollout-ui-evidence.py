"""Build a publisher-binary inventory for the all-target UI evidence pass.

Upstream availability and source registration are separate from successful
runtime verification. Screen inheritance is an inventory candidate, never a
claim that a window works or that every variant has been captured.
"""
from pathlib import Path
import csv, io, json, re, struct, zipfile

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/all-target-rollout'
OUT=ROOT/'output/all-target-rollout'
BASES={'net/minecraft/class_465','net/minecraft/client/gui/screens/inventory/AbstractContainerScreen'}

def header(data):
    stream=io.BytesIO(data)
    def u1():return int.from_bytes(stream.read(1),'big')
    def u2():return int.from_bytes(stream.read(2),'big')
    assert stream.read(4)==b'\xca\xfe\xba\xbe'
    stream.read(4);pool=[None]*u2();i=1
    while i<len(pool):
        tag=u1()
        if tag==1:pool[i]=stream.read(u2()).decode('utf-8',errors='replace')
        elif tag in (7,8,16,19,20):pool[i]=u2()
        elif tag in (3,4,9,10,11,12,17,18):stream.read(4)
        elif tag in (5,6):stream.read(8);i+=1
        elif tag==15:stream.read(3)
        else:raise ValueError(tag)
        i+=1
    def name(i):return pool[pool[i]] if i else None
    flags=u2();own=name(u2());parent=name(u2());interfaces=[name(u2()) for _ in range(u2())]
    return own,{'flags':flags,'parents':[p for p in [parent,*interfaces] if p]}

def index(path,cache):
    if cache.exists():return json.loads(cache.read_text())
    result={}
    def visit(archive):
        with zipfile.ZipFile(archive) as z:
            for entry in z.namelist():
                if entry.endswith('.class'):
                    name,info=header(z.read(entry));result[name]=info
                elif entry.startswith('META-INF/') and entry.endswith('.jar'):
                    visit(io.BytesIO(z.read(entry)))
    visit(path);cache.parent.mkdir(parents=True,exist_ok=True);cache.write_text(json.dumps(result))
    return result

def mod_ids(path):
    with zipfile.ZipFile(path) as archive:
        if 'fabric.mod.json' in archive.namelist():
            return {json.loads(archive.read('fabric.mod.json'),strict=False)['id']}
        descriptor=next((n for n in ['META-INF/neoforge.mods.toml','META-INF/mods.toml'] if n in archive.namelist()),None)
        if not descriptor:return set()
        text=archive.read(descriptor).decode(errors='replace');result=set()
        for section in re.split(r'\[\[mods\]\]',text)[1:]:
            found=re.search(r'modId\s*=\s*"([\w-]+)"',section)
            if found:result.add(found[1])
        return result

def minecraft_graph(mc,loader):
    """Include vanilla screen ancestors, e.g. InventoryScreen and ChestScreen.

    Publisher classes may inherit an intermediate Minecraft screen instead of
    AbstractContainerScreen directly. Namespace conversion uses Loom's mappings.
    """
    cache=Path.home()/'.gradle/caches/fabric-loom'/mc
    jar=cache/'minecraft-client.jar'
    if not jar.exists():raise FileNotFoundError('Missing actual Minecraft input: '+str(jar))
    raw=index(jar,WORK/'class-headers'/('minecraft-'+mc+'-official.json'))
    target='intermediary' if loader=='fabric' and not mc.startswith('26.') else 'named'
    mapping=next((p for p in cache.glob('loom.mappings.*2198*/mappings.tiny')),None)
    if mapping is None:return raw
    lines=mapping.read_text(encoding='utf-8').splitlines();names=lines[0].split('\t')[3:]
    if 'official' not in names or target not in names:return raw
    origin=names.index('official');dest=names.index(target)
    aliases={}
    for line in lines[1:]:
        if line.startswith('c\t'):
            fields=line.split('\t')[1:];aliases[fields[origin]]=fields[dest]
    return {aliases.get(n,n):{'flags':info['flags'],'parents':[aliases.get(p,p) for p in info['parents']]} for n,info in raw.items()}

def main():
    lock=json.loads((WORK/'availability.json').read_text());OUT.mkdir(parents=True,exist_ok=True)
    old_rows=json.loads((OUT/'availability-and-registration.json').read_text()) if (OUT/'availability-and-registration.json').exists() else []
    old_screens=json.loads((OUT/'ui-evidence-plan.json').read_text()) if (OUT/'ui-evidence-plan.json').exists() else []
    previous={(r['minecraft'],r['loader'],r['project_id'],r['upstream_sha512']):r for r in old_rows}
    previous_screens={(r['minecraft'],r['loader'],r['slug'],r['version'],r['screen']):r for r in old_screens}
    rows=[];screens=[]
    for target in lock['targets']:
        mc,loader=target['minecraft'],target['loader'];key=mc+'/'+loader
        registered=set();source=ROOT/f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat/CompatibilityBootstrap.java'
        if source.exists():registered.update(re.findall(r'initialize\("([^\"]+)"',source.read_text()))
        catalog=ROOT/f'compatibility/versions/{mc}/common/src/main/resources/salts_inventory_compat/native-windows/project-everything.json'
        profiles=json.loads(catalog.read_text()) if catalog.exists() else []
        registered.update(m for p in profiles for m in p['mods'])
        graph=minecraft_graph(mc,loader);native={};metadata_ids={};seen=set()
        for slug,p in lock['projects'].items():
            release=p['targets'].get(key)
            if not release:continue
            binary=WORK/'upstream'/(release['sha512'][:16]+'-'+release['filename'])
            data=index(binary,WORK/'class-headers'/(release['sha512']+'.json'))
            graph.update(data);native[slug]=data;metadata_ids[slug]=mod_ids(binary)
        # Publisher screens can inherit an inventory base from a required mod
        # that has no standalone SIU project row (e.g. Deep Aether's Combiner).
        # Include original fixture dependency ancestors without replacing the
        # exact selected Minecraft or publisher classes already indexed above.
        dependencies={}
        for fixture_file in sorted((WORK/'fixtures').glob('*/fixture.json')):
            fixture=json.loads(fixture_file.read_text())
            if (fixture.get('minecraft'),fixture.get('loader'))!=(mc,loader):continue
            for artifact in fixture.get('artifacts',[]):
                digest=artifact.get('sha512');path=Path(artifact['path'])
                if digest and path.is_file():dependencies.setdefault(digest,path)
        for digest,path in dependencies.items():
            for name,info in index(path,WORK/'class-headers'/(digest+'.json')).items():
                graph.setdefault(name,info)
        def inventory(name,ancestors=frozenset()):
            if name in BASES:return True
            if name in ancestors:return False
            return any(inventory(p,ancestors|{name}) for p in graph.get(name,{}).get('parents',[]))
        for slug,p in lock['projects'].items():
            release=p['targets'].get(key)
            if not release or p['project_id'] in seen:continue
            seen.add(p['project_id']);data=native[slug]
            candidates=sorted(n.replace('/','.') for n,d in data.items() if not d['flags']&0x0600 and inventory(n))
            ids=set(p['mod_ids'])|metadata_ids[slug];source_bound=bool(ids&registered)
            if ids&{'createbigcannons','railways'}:source_bound='create' in registered
            if ids&{'sophisticatedbackpacks','sophisticatedstorage'}:
                source_bound='sophisticatedcore' in registered or (ROOT/f'compatibility/versions/{mc}/{loader}/src/main/java/com/salts_inventory_update/compat/sophisticated').exists()
            if 'curios' in ids:source_bound=(ROOT/f'compatibility/versions/{mc}/{loader}/src/main/java/com/salts_inventory_update/compat/curios').exists()
            if ids&{'appbot','megacells','merequester'}:source_bound=mc in ['1.20.1','1.21.1','26.1.2'] and (mc=='1.20.1' or loader=='neoforge')
            if 'ae2' in ids:source_bound=(ROOT/f'compatibility/ae2/versions/{mc}/{loader}').exists() or mc=='1.20.1'
            route='compatibility registration' if source_bound else 'needs source review'
            if 'visualworkbench' in ids:
                source_bound=True;route='Core vanilla crafting integration'
            if slug=='create-alloyed' and mc=='1.20.1' and not candidates:
                source_bound='create' in registered;route='Inherited Create workstations; this release adds materials and recipes, no separate container screen'
            if 'modmenu' in ids:
                source_bound=(ROOT/'core-integrations/modmenu/src/main/java').exists()
                route='Core Mod Menu entry; full-screen settings'
            if 'jei' in ids:
                source_bound=(ROOT/f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat/jei/SaltsJeiPlugin.java').exists()
                route='JEI plugin entry'
            if 'roughlyenoughitems' in ids or slug=='rei':
                source_bound=(ROOT/f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat/rei/SaltsReiClientPlugin.java').exists()
                route='REI plugin entry'
            if 'sophisticatedcore' in ids and not source_bound:
                source_bound=(ROOT/f'compatibility/versions/{mc}/{loader}/src/main/java/com/salts_inventory_update/compat/sophisticated/SophisticatedCompatBootstrap.java').exists()
                route='loader-specific Sophisticated bootstrap'
            row={'minecraft':mc,'loader':loader,'project':p['title'],'project_id':p['project_id'],'slug':slug,'mod_ids':','.join(sorted(ids)),
                 'upstream_version':release['version'],'upstream_available':True,'source_registration':source_bound,
                 'source_registration_route':route,
                 'container_screen_candidates':len(candidates),'runtime_status':'pending focused validation',
                 'release_url':release['release_url'],'upstream_sha512':release['sha512']}
            old=previous.get((mc,loader,p['project_id'],release['sha512']),{})
            for field in ['runtime_status','paired_states_captured','verification_notes','ui_scope','scope_notes']:
                if field in old:row[field]=old[field]
            rows.append(row)
            for screen in candidates:
                profiles_for_screen=[x['id'] for x in profiles if screen in [x['screen'],*x.get('screens',[])]]
                screen_row={'minecraft':mc,'loader':loader,'project':p['title'],'slug':slug,'version':release['version'],'screen':screen,
                            'profiles':profiles_for_screen,'native_capture':None,'siu_capture':None,'status':'candidate; real opening and variant review required'}
                old=previous_screens.get((mc,loader,slug,release['version'],screen),{})
                for field in ['native_capture','siu_capture','captured_states','status','ui_scope','scope_notes']:
                    if field in old:screen_row[field]=old[field]
                screens.append(screen_row)
        print(key,len(seen),'publisher projects;',sum(r['container_screen_candidates'] for r in rows if r['minecraft']==mc and r['loader']==loader),'container screen candidates',flush=True)
    (OUT/'availability-and-registration.json').write_text(json.dumps(rows,indent=2))
    (OUT/'ui-evidence-plan.json').write_text(json.dumps(screens,indent=2))
    with (OUT/'availability-and-registration.csv').open('w',newline='',encoding='utf-8-sig') as f:
        w=csv.DictWriter(f,fieldnames=list(dict.fromkeys(k for r in rows for k in r)));w.writeheader();w.writerows(rows)
    print('Rows',len(rows),'screen candidates',len(screens))

if __name__=='__main__':main()
