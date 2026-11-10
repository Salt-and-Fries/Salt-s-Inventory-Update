"""Verify every planned binary and account for available inventory publishers."""
from pathlib import Path
import hashlib,json,re,zipfile
from run_mod_matrix import ROOT,FIXTURES
from collect_results import mod_ids
from prepare_recent_fixtures import is_publisher_input
OUT=ROOT/'output/project-everything-regression'
cache={};identities={};planned={};rows=[];fabric_minimums={}
for target,fixtures in FIXTURES.items():
    installed=set()
    for fixture in fixtures:
        data=json.loads((ROOT/'build/all-target-rollout/fixtures'/fixture/'fixture.json').read_text());problems=[]
        for artifact in data['artifacts']:
            if not is_publisher_input(artifact):problems.append('Test observer or subject misclassified as publisher input: '+artifact['filename'])
            file=ROOT/artifact['path']
            if str(file) not in cache:
                cache[str(file)]=hashlib.sha512(file.read_bytes()).hexdigest();identities[str(file)]=mod_ids(file)
                with zipfile.ZipFile(file) as jar:
                    if 'fabric.mod.json' in jar.namelist():
                        metadata=json.loads(jar.read('fabric.mod.json'),strict=False)
                        minimum=metadata.get('depends',{}).get('fabric-api')
                        parsed=re.match(r'>=\s*(\d+\.\d+\.\d+)',minimum) if isinstance(minimum,str) else None
                        if parsed:fabric_minimums[str(file)]=(minimum,tuple(map(int,parsed[1].split('.'))))
            if cache[str(file)]!=artifact['sha512']:problems.append('Binary hash mismatch: '+str(file))
            installed.update(identities[str(file)])
        runtime=data.get('runtime',{})
        if target.endswith('/fabric') and not all(runtime.get(k) for k in ['fabric_loader','fabric_api']):problems.append('Missing pinned Fabric runtime')
        checked=[]
        if target.endswith('/fabric') and runtime.get('fabric_api'):
            selected=tuple(map(int,re.match(r'(\d+\.\d+\.\d+)',runtime['fabric_api'])[1].split('.')))
            for artifact in data['artifacts']:
                requirement=fabric_minimums.get(str(ROOT/artifact['path']))
                if not requirement:continue
                checked.append({'publisher':artifact['filename'],'minimum':requirement[0]})
                if selected<requirement[1]:problems.append(artifact['filename']+' requires Fabric API '+requirement[0]+'; pinned '+runtime['fabric_api'])
        rows.append({'target':target,'fixture':fixture,'status':'passed' if not problems else 'failed','problems':problems,'artifacts':len(data['artifacts']),'runtime':runtime,'checked_fabric_api_minimums':checked})
    planned[target]=installed
missing=[]
for row in json.loads((ROOT/'output/all-target-rollout/availability-and-registration.json').read_text()):
    if not row.get('upstream_available') or not row.get('container_screen_candidates'):continue
    target=row['minecraft']+'/'+row['loader'];ids=set(row['mod_ids'].replace(',',' ').replace(';',' ').split())
    if target in planned and not ids.intersection(planned[target]):missing.append({'target':target,'project':row['project'],'required_mod_ids':sorted(ids)})
(OUT/'planned-mod-coverage.json').write_text(json.dumps({'planned_mod_ids':{t:sorted(ids) for t,ids in planned.items()},'missing_inventory_candidates':missing},indent=2)+'\n')
result={'status':'passed' if not missing and all(r['status']=='passed' for r in rows) else 'needs-attention','fixtures':rows,'unique_fixture_files':len(cache),
        'scope':'Binary integrity and planned publisher coverage only; not functional test outcomes.'}
(OUT/'fixture-integrity-final.json').write_text(json.dumps(result,indent=2)+'\n')
print(len(rows),'cohorts;',len(cache),'distinct pinned fixture artifacts;',len(missing),'uncovered available inventory publishers;',result['status'])
