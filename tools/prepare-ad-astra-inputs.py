"""Resolve unchanged publisher inputs for the four supported Ad Astra targets."""
from pathlib import Path
import hashlib,json,urllib.request,zipfile
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/ad-astra-implemented';CACHE=ROOT/'build/ad-astra-implementation/publishers'
OUT.mkdir(exist_ok=True);CACHE.mkdir(parents=True,exist_ok=True)
HEAD={'User-Agent':'SIU-Compatibility-Research/1.0'}
def api(path):
    with urllib.request.urlopen(urllib.request.Request('https://api.modrinth.com/v2/'+path,headers=HEAD),timeout=45) as r:return json.load(r)
def versions(project):
    p=OUT/(project+'-releases.json')
    if not p.exists():p.write_text(json.dumps(api('project/'+project+'/version'),indent=2),encoding='utf-8')
    return json.loads(p.read_text(encoding='utf-8'))
def resolve(mc,loader):
    selected={};records=[]
    def add(project,version=None):
        v=api('version/'+version) if version else next(v for v in versions(project) if mc in v['game_versions'] and loader in v['loaders'])
        project=v['project_id']
        if project in selected:return
        selected[project]=v
        f=next((f for f in v['files']if f.get('primary')),v['files'][0]);p=CACHE/f['filename']
        if not p.exists():
            with urllib.request.urlopen(urllib.request.Request(f['url'],headers=HEAD),timeout=60) as r:p.write_bytes(r.read())
        assert hashlib.sha512(p.read_bytes()).hexdigest()==f['hashes']['sha512']
        records.append(dict(coordinate='modrinth:'+project+':'+v['id'],version=v['version_number'],version_id=v['id'],
                            path=str(p.relative_to(ROOT)),filename=f['filename'],sha512=f['hashes']['sha512']))
        for dep in v['dependencies']:
            if dep['dependency_type']=='required':add(dep['project_id'],dep.get('version_id'))
    add('ad-astra');add('ad-astra-giselle-addon')
    if loader=='fabric':add('fabric-api')
    fabric_api=next((r['version']for r in records if r['filename'].startswith('fabric-api-')),None)
    for r in records:
        if r['filename'].startswith('fabric-api-'):r.pop('coordinate',None)
    fixture=dict(name='Ad Astra and Giselle native-window implementation',minecraft=mc,loader=loader,
                 runtime={'fabric_loader':'0.19.5','fabric_api':fabric_api,'provenance':'Original publisher releases'},artifacts=records)
    (OUT/f'fixture-{mc}-{loader}.json').write_text(json.dumps(fixture,indent=2),encoding='utf-8')
    return dict(minecraft=mc,loader=loader,artifacts=[dict(filename=r['filename'],version_id=r['version_id'])for r in records])
if __name__=='__main__':
    targets=[resolve(mc,loader)for mc,loader in [('1.20.1','fabric'),('1.20.1','forge'),('1.21.1','fabric'),('1.21.1','neoforge')]]
    (OUT/'publisher-targets.json').write_text(json.dumps(targets,indent=2),encoding='utf-8');print(json.dumps(targets,indent=2))
