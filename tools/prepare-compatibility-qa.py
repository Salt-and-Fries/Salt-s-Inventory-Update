"""Prepare unmodified, hash-checked upstream releases and their required QA dependencies."""
from pathlib import Path
import argparse, hashlib, io, json, re, urllib.request, zipfile
ROOT=Path(__file__).resolve().parents[1]
CACHE=ROOT/'build/compatibility-coverage/upstream-fixtures'
HEADERS={'User-Agent':'SaltsInventoryCompatibility/coverage-qa (https://github.com/Salt-and-Fries/Salt-s-Inventory-Update)'}
PROVIDED={'P7dR8mSH'}  # Fabric API is supplied by the selected project's normal runtime.

def get_json(endpoint):
    path=CACHE/'metadata'/(endpoint.replace('/','-')+'.json')
    if path.exists(): return json.loads(path.read_text())
    with urllib.request.urlopen(urllib.request.Request('https://api.modrinth.com/v2/'+endpoint,headers=HEADERS),timeout=60) as response: data=json.load(response)
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2)+'\n')
    return data

def prepare(mc,loader,scenario,output,extra_projects=()):
    lock=json.loads((ROOT/'compatibility/coverage-targets.json').read_text())
    target=next(t for t in lock['targets'] if t['minecraft']==mc and t['loader']==loader)
    names={'browser-jei':['jei'],'browser-rei':['rei'],'browser-emi':['emi'],'toms':['toms-storage'],
           'sophisticated':['sophisticated-core','sophisticated-backpacks','sophisticated-storage'],
           'ae2':['ae2'],'ae2-atlas':['ae2'],'extension-empty':[],'core':[]}[scenario]
    if scenario=='browser-rei' and loader=='fabric': names+=['modmenu']
    names+=list(extra_projects)
    selected={}
    queue=[]
    for name in names:
        record=target['integrations'].get(name)
        if record is None:
            version=next((v for v in get_json('project/'+name+'/version') if mc in v['game_versions'] and loader in v['loaders']),None)
            if version is None: raise ValueError(f'No upstream release for {mc}/{loader}/{name}')
        else:
            if not record['available']: raise ValueError(f'No upstream release for {mc}/{loader}/{name}')
            version=get_json('version/'+record['version_id'])
        primary=next((f for f in version['files'] if f['primary']),version['files'][0])
        if record is not None: assert primary['hashes']['sha512']==record['sha512'],f'Upstream primary file changed: {name}'
        selected[version['project_id']]=version;queue.append(version)
    while queue:
        version=queue.pop(0)
        for dep in version['dependencies']:
            if dep['dependency_type']!='required' or dep['project_id'] in PROVIDED or dep['project_id'] in selected:continue
            if dep['version_id']: required=get_json('version/'+dep['version_id'])
            elif dep['project_id']:
                releases=get_json('project/'+dep['project_id']+'/version')
                required=next((v for v in releases if mc in v['game_versions'] and loader in v['loaders']),None)
                if required is None: raise ValueError(f'No required dependency {dep["project_id"]} for {mc}/{loader}')
            else: raise ValueError(f'Unresolvable required upstream file dependency: {dep}')
            selected[required['project_id']]=required;queue.append(required)
    artifacts=[];seen={};seen_mods=set();pending=[]
    def register(data,filename,coordinate=None):
        digest=hashlib.sha512(data).hexdigest()
        if digest in seen:return
        meta=None
        if loader=='fabric':
            with zipfile.ZipFile(io.BytesIO(data)) as archive:
                if 'fabric.mod.json' in archive.namelist(): meta=json.loads(archive.read('fabric.mod.json'))
            if meta and meta['id'] in seen_mods:return
            if meta:seen_mods.add(meta['id'])
        seen[digest]=True
        path=CACHE/'jars'/(digest[:16]+'-'+Path(filename).name)
        path.parent.mkdir(parents=True,exist_ok=True)
        if not path.exists():path.write_bytes(data)
        assert hashlib.sha512(path.read_bytes()).hexdigest()==digest,path
        artifacts.append({'path':str(path),'filename':filename,'coordinate':coordinate,'sha512':digest})
        if loader=='fabric':
            with zipfile.ZipFile(io.BytesIO(data)) as archive:
                if 'fabric.mod.json' in archive.namelist():
                    meta=json.loads(archive.read('fabric.mod.json'))
                    for nested in meta.get('jars',[]):pending.append((archive.read(nested['file']),Path(nested['file']).name))
    for project,version in selected.items():
        primary=next((f for f in version['files'] if f['primary']),version['files'][0])
        cache=CACHE/'downloads'/primary['hashes']['sha512']
        if not cache.exists():
            with urllib.request.urlopen(urllib.request.Request(primary['url'],headers=HEADERS),timeout=60) as response:data=response.read()
            assert hashlib.sha512(data).hexdigest()==primary['hashes']['sha512'],primary['filename']
            cache.parent.mkdir(parents=True,exist_ok=True);cache.write_bytes(data)
        data=cache.read_bytes()
        assert hashlib.sha512(data).hexdigest()==primary['hashes']['sha512'],f'Cached upstream file changed: {primary["filename"]}'
        register(data,primary['filename'],f'maven.modrinth:{project}:{version["id"]}')
    while pending:
        data,filename=pending.pop(0)
        register(data,filename)
    output.parent.mkdir(parents=True,exist_ok=True)
    report={'minecraft':mc,'loader':loader,'scenario':scenario,'artifacts':artifacts,'runtime':target.get('qa_runtime',{}),
            'versions':[{'project_id':project,'version_id':v['id'],'version':v['version_number']} for project,v in selected.items()]}
    output.write_text(json.dumps(report,indent=2)+'\n')
    print(f'Prepared {len(selected)} pinned upstream mods and {len(artifacts)} native/library artifacts: {output}')

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft',required=True)
    parser.add_argument('--loader',required=True,choices=['fabric','forge','neoforge'])
    parser.add_argument('--scenario',required=True)
    parser.add_argument('--output',required=True,type=Path)
    parser.add_argument('--extra-project',action='append',default=[],help='Additional official Modrinth project for this isolated QA fixture')
    args=parser.parse_args();prepare(args.minecraft,args.loader,args.scenario,args.output.resolve(),args.extra_project)
