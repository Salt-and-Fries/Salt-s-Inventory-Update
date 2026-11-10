"""Refresh exact upstream availability for the implemented SIU integration set."""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
from datetime import datetime,timezone
import json,re,urllib.request,time

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/all-target-rollout';OUT.mkdir(parents=True,exist_ok=True)
CACHE=OUT/'metadata';CACHE.mkdir(exist_ok=True)
VERSIONS=['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']
TARGETS=[(v,l) for v in VERSIONS for l in ['fabric','forge' if v=='1.20.1' else 'neoforge']]
HEADERS={'User-Agent':'SIU-Compatibility-Rollout/1.0 (https://github.com/Salt-and-Fries/Salt-s-Inventory-Update)'}

def request(endpoint):
    path=CACHE/(endpoint.replace('/','-')+'.json')
    if path.exists():return json.loads(path.read_text(encoding='utf-8'))
    for attempt in range(4):
        try:
            with urllib.request.urlopen(urllib.request.Request('https://api.modrinth.com/v2/'+endpoint,headers=HEADERS),timeout=35) as response:data=json.load(response)
            path.write_text(json.dumps(data,ensure_ascii=False),encoding='utf-8');return data
        except Exception:
            if attempt==3:raise
            time.sleep(attempt+1)

def main():
    projects={};mod_projects={}
    for filename in ['coverage-targets.json','native-windows-1.20.1.json','native-windows-all-targets.json','native-windows-third-batch-1.20.1.json','third-batch-all-targets.json']:
        data=json.loads((ROOT/'compatibility'/filename).read_text(encoding='utf-8'))
        for target in data.get('targets',[]):
            rows=target.get('integrations',{})
            for key,row in rows.items() if isinstance(rows,dict) else [(r.get('project'),r) for r in rows]:
                project=row.get('project') or key
                if not project:continue
                projects.setdefault(project,{'project':project,'mod_ids':set(),'evidence':[]})
                if row.get('mod_id'):projects[project]['mod_ids'].add(row['mod_id'])
                projects[project]['evidence'].append(filename)
    profiles=json.loads((ROOT/'compatibility/versions/1.20.1/common/src/main/resources/salts_inventory_compat/native-windows/project-everything.json').read_text())
    bootstrap=(ROOT/'compatibility/versions/1.20.1/fabric/src/main/java/com/salts_inventory_update/compat/CompatibilityBootstrap.java').read_text()
    mods=set(re.findall(r'initialize\("([^"]+)"',bootstrap))|{m for p in profiles for m in p['mods']}
    # Integrations registered through another mod's builders, or add-on menu
    # catalogs, do not appear as separate bootstrap invocations.
    mods.update(['appbot','createbigcannons','railways','visualworkbench','curios'])
    inventory=json.loads((ROOT/'output/remaining-ui-audit/inventory.json').read_text())['entries']
    aliases={'create':['create','create-fabric'],'farmersdelight':['farmers-delight','farmers-delight-refabricated'],
             'graveyard':['the-graveyard-fabric','the-graveyard-forge'], 'sophisticatedcore':['sophisticated-core','sophisticated-core-(unofficial-fabric-port)'],
             'sophisticatedbackpacks':['sophisticated-backpacks','sophisticated-backpacks-(unoffical-fabric-port)'],
             'sophisticatedstorage':['sophisticated-storage','sophisticated-storage-(unofficial-fabric-port)'],
             'ae2':['ae2'],'emi':['emi'],'raised':['raised'],'toms_storage':['toms-storage'],
             'trinkets':['trinkets'],'universal-graves':['universal-graves'],
             'the_bumblezone':['the-bumblezone','the-bumblezone-fabric'],
             'appbot':['applied-botanics'], 'createbigcannons':['create-big-cannons'],
             'railways':['create-steam-n-rails','create-steam-n-rails-1.21.1'],
             'visualworkbench':['visual-workbench'], 'itemfilters':['item-filters']}
    aliases['curios']=['curios']
    # The early internal integration key predates the publisher's mod ID.
    aliases['miners_delight']=['miners-delight']
    for mod in sorted(mods):
        entries=[e for e in inventory if mod in e['mod_ids']]
        names=aliases.get(mod,[])+[e['source_url'].split('/mod/',1)[1].split('/')[0] for e in entries if '/mod/' in (e['source_url'] or '')]
        for project in names:
            projects.setdefault(project,{'project':project,'mod_ids':set(),'evidence':[]})
            projects[project]['mod_ids'].add(mod)
            projects[project]['evidence'].extend(['inventory:'+str(e['entry']) for e in entries])
        mod_projects[mod]=sorted(set(names))
    results={};errors={}
    def fetch(project):
        meta=request('project/'+project);versions=request('project/'+project+'/version')
        exact={}
        for mc,loader in TARGETS:
            matching=[v for v in versions if mc in v['game_versions'] and loader in v['loaders'] and v.get('status','listed')=='listed' and any(f['filename'].endswith('.jar') for f in v['files'])]
            matching.sort(key=lambda v:v['date_published'],reverse=True)
            if matching:
                v=matching[0];f=next((f for f in v['files'] if f.get('primary') and f['filename'].endswith('.jar')),next(f for f in v['files'] if f['filename'].endswith('.jar')))
                exact[mc+'/'+loader]={'version_id':v['id'],'version':v['version_number'],'date':v['date_published'],'filename':f['filename'],'sha512':f['hashes']['sha512'],'sha1':f['hashes']['sha1'],'download':f['url'],'release_url':f'https://modrinth.com/mod/{meta["slug"]}/version/{v["id"]}','dependencies':v['dependencies']}
        row=projects[project]
        return {'project':project,'project_id':meta['id'],'slug':meta['slug'],'title':meta['title'],'mod_ids':sorted(row['mod_ids']),'evidence':sorted(set(row['evidence'])),'targets':exact}
    with ThreadPoolExecutor(max_workers=4) as pool:
        pending={pool.submit(fetch,p):p for p in projects}
        for f in as_completed(pending):
            p=pending[f]
            try:results[p]=f.result();print(p,len(results[p]['targets']),flush=True)
            except Exception as e:errors[p]=str(e);print('ERROR',p,str(e),flush=True)
            (OUT/'availability-progress.json').write_text(json.dumps({'projects':results,'errors':errors},indent=2),encoding='utf-8')
    manifest={'checked_at':datetime.now(timezone.utc).isoformat(),'source':'https://api.modrinth.com/v2','targets':[{'minecraft':m,'loader':l} for m,l in TARGETS],
              'scope':'All integration families already implemented in this repository; exact upstream game and loader declarations, not inferred cross-loader support.',
              'mod_projects':mod_projects,'projects':dict(sorted(results.items())),'errors':errors}
    supplemental=OUT/'publisher-supplemental-inputs.json'
    if supplemental.exists():
        for record in json.loads(supplemental.read_text()):
            manifest['projects'][record['project']]['targets'][record['target']]=record['release']
        manifest['supplemental_source_manifest']=str(supplemental.relative_to(ROOT))
    embedded=OUT/'publisher-embedded-inputs.json'
    if embedded.exists():
        for record in json.loads(embedded.read_text()):
            manifest['projects'][record['project']]=record
            for mod in record['mod_ids']:
                manifest['mod_projects'][mod]=sorted(set(manifest['mod_projects'].get(mod,[])+[record['project']]))
        manifest['embedded_source_manifest']=str(embedded.relative_to(ROOT))
    manifest['unmapped_integration_mod_ids']=[mod for mod,names in manifest['mod_projects'].items() if not names]
    (OUT/'availability.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False),encoding='utf-8')
    print('Finished',len(results),'projects;',sum(len(v['targets']) for v in results.values()),'exact releases;',len(errors),'errors',flush=True)

if __name__=='__main__':main()
