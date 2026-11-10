"""Resolve publisher releases for the disposable Create 1.20.1 test clients.

Original upstream bytes and their hashes are retained. This does not start a
client, change a user instance, or manufacture a menu/screen.
"""
from pathlib import Path
import concurrent.futures,hashlib,json,urllib.request,urllib.parse,zipfile

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/create-window-implementation'
OUT.mkdir(parents=True,exist_ok=True)
API='https://api.modrinth.com/v2/'
HEADERS={'User-Agent':'SIU-Create-Compatibility-Local-QA/1.0 (https://github.com/Salt-and-Fries/Salt-s-Inventory-Update)'}
PROJECTS={'create-fabric':'Xbc0uyRg','create-forge':'LNytGWDc','big-cannons':'GWp4jCJj','railways':'ZzjhlDgM','shuffle-filter':'gv5RRavC','aeronautics':'oWaK0Q19'}

def get(url):
    with urllib.request.urlopen(urllib.request.Request(url,headers=HEADERS),timeout=45) as r:return r.read()

def metadata(path):return json.loads(get(API+path))

def versions(project,loader):
    q=urllib.parse.urlencode({'game_versions':json.dumps(['1.20.1']),'loaders':json.dumps([loader])})
    return metadata('project/'+project+'/version?'+q)

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def download(v):
    f=next((f for f in v['files'] if f.get('primary')),v['files'][0]);path=OUT/'jars'/f['filename'];path.parent.mkdir(exist_ok=True)
    expected=f['hashes'].get('sha512');data=path.read_bytes() if path.exists() else get(f['url'])
    assert not expected or hashlib.sha512(data).hexdigest()==expected,f['filename']
    if not path.exists():path.write_bytes(data)
    modids=[]
    with zipfile.ZipFile(path) as z:
        if 'fabric.mod.json' in z.namelist():modids=[json.loads(z.read('fabric.mod.json'))['id']]
        elif 'META-INF/mods.toml' in z.namelist():
            import re
            modids=re.findall(r'^\s*modId\s*=\s*"([^"]+)"',z.read('META-INF/mods.toml').decode(),re.M)[:1]
    return dict(path=str(path.relative_to(ROOT)),filename=path.name,sha256=sha(path),sha512=hashlib.sha512(data).hexdigest(),
        project_id=v['project_id'],version_id=v['id'],version=v['version_number'],mod_ids=modids,url=f['url'])

def resolve(loader,roots):
    selected={};pending=list(roots);artifacts=[]
    while pending:
        v=pending.pop(0)
        if v['project_id'] in selected:continue
        selected[v['project_id']]=v
        artifacts.append(download(v))
        for d in v.get('dependencies',[]):
            if d['dependency_type']!='required' or d.get('project_id') in selected:continue
            if d.get('version_id'):dep=metadata('version/'+d['version_id'])
            elif d.get('project_id'):
                choices=versions(d['project_id'],loader)
                if not choices:raise ValueError('No matching dependency '+str(d))
                dep=choices[0]
            else:continue
            pending.append(dep)
    fixture=dict(name='create-window-implementation-'+loader,minecraft='1.20.1',loader=loader,pack_id='QbFcrWM5',
        scope='Create-family compatibility implementation and full affected-window feature tests',artifacts=artifacts)
    # Use the retained Fabric pack ID solely for its matching original Fabric API.
    if loader=='fabric':
        retained=json.loads((ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-native-prod-app02/staged-artifacts.json').read_text(encoding='utf-8'))
        files=json.loads((ROOT/'build/ui-modpack-audit-20261025/files.json').read_text(encoding='utf-8'))
        api=next(a for a in retained['artifacts'] if a['family']=='runtime-api')
        api_sha=hashlib.sha1(Path(api['file']).read_bytes()).hexdigest()
        fixture['pack_id']=files[api_sha]['packs'][0]
    (OUT/(loader+'-fixture.json')).write_text(json.dumps(fixture,indent=2)+'\n',encoding='utf-8')
    return fixture

def main():
    jobs=[(name,loader) for name in PROJECTS for loader in ['fabric','forge']]
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as ex:
        results=dict(zip(jobs,ex.map(lambda pair:versions(PROJECTS[pair[0]],pair[1]),jobs)))
    (OUT/'publisher-versions.json').write_text(json.dumps({n+':'+l:r for (n,l),r in results.items()},indent=2)+'\n',encoding='utf-8')
    for (n,l),rows in results.items():print(n,l,rows[0]['version_number'] if rows else 'NO 1.20.1 RELEASE',flush=True)
    matrix=[]
    for loader in ['fabric','forge']:
        roots=[]
        for name in ['create-'+loader,'big-cannons','railways','shuffle-filter']:
            choices=results[(name,loader)]
            if choices:roots.append(choices[0]);matrix.append(dict(mod=name,loader=loader,version=choices[0]['version_number'],version_id=choices[0]['id']))
        fixture=resolve(loader,roots)
        print(loader,'resolved',len(fixture['artifacts']),'artifacts',flush=True)
    (OUT/'target-matrix.json').write_text(json.dumps(matrix,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__':main()
