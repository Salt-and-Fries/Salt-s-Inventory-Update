"""Read-only inventory of published 1.20.1 Create addon release UI classes.

Downloads publisher-hosted JARs for static inspection; never executes them.
This inventories declarations and hierarchy, not working SIU compatibility.
"""
from pathlib import Path
from urllib.parse import urlencode
import concurrent.futures,hashlib,io,json,re,struct,time,tomllib,urllib.request,zipfile

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'build/create-addon-coverage-audit'
UA={'User-Agent':'SaltsInventoryUpdate-compatibility-audit/1.0'}
def fetch(url):
    for attempt in range(3):
        try:
            with urllib.request.urlopen(urllib.request.Request(url,headers=UA),timeout=60) as r:return r.read()
        except Exception:
            if attempt==2:raise
            time.sleep(2+attempt*3)
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def write(p,v):p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')

def parent(data):
    """Only names and superclass; no disassembly or execution."""
    p=8;n=struct.unpack_from('>H',data,p)[0];p+=2;cp=[None]*n;i=1
    while i<n:
        tag=data[p];p+=1
        if tag==1:
            size=struct.unpack_from('>H',data,p)[0];p+=2;cp[i]=data[p:p+size].decode('utf-8','replace');p+=size
        elif tag==7:cp[i]=struct.unpack_from('>H',data,p)[0];p+=2
        elif tag in [3,4,9,10,11,12,17,18]:p+=4
        elif tag in [5,6]:p+=8;i+=1
        elif tag in [8,16,19,20]:p+=2
        elif tag==15:p+=3
        else:raise ValueError(('class tag',tag))
        i+=1
    _,this,superclass=struct.unpack_from('>HHH',data,p)
    return cp[cp[this]],cp[cp[superclass]] if superclass else ''

INVENTORY=['AbstractContainerScreen','AbstractSimiContainerScreen','AbstractFilterScreen','AEBaseScreen','AbstractContainerMenu','GhostItemMenu','AbstractSimiContainerMenu','class_465','class_1703']
def inspect(path):
    graph={};assets=[];errors=[];metadata={}
    with zipfile.ZipFile(path) as z:
        if 'fabric.mod.json' in z.namelist():
            f=json.loads(z.read('fabric.mod.json'),strict=False);metadata['fabric']={'id':f.get('id'),'create':f.get('depends',{}).get('create'),'depends':f.get('depends',{})}
        for name in ['META-INF/mods.toml','META-INF/neoforge.mods.toml']:
            if name in z.namelist():
                try:
                    t=tomllib.loads(z.read(name).decode('utf-8'));deps=[d for ds in t.get('dependencies',{}).values() for d in ds if d.get('modId')=='create'];metadata[name]={'mods':t.get('mods',[]),'create':deps}
                except Exception as e:metadata[name]={'error':str(e)}
        for name in z.namelist():
            if name.endswith('.class') and not name.startswith(('META-INF/','com/google/','org/','net/minecraft/')):
                try:
                    a,b=parent(z.read(name));graph[a]=b
                except Exception as e:errors.append([name,str(e)])
            if '/textures/gui/' in name and name.endswith('.png'):assets.append(name)
    def chain(n):
        result=[];seen=set()
        while n and n not in seen:
            seen.add(n);result.append(n);n=graph.get(n,'')
        return result
    screens=[];menus=[]
    for n,b in graph.items():
        leaf=n.rsplit('/',1)[-1]
        if '$' in leaf or leaf.startswith(('Accessor','Mixin','IExtended','IGhost')):continue
        lineage=chain(n);is_inventory=any(any(t in q for t in INVENTORY) for q in lineage)
        if leaf.endswith(('Screen','Gui')):screens.append({'class':n,'parent':b,'lineage':lineage,'container_hierarchy':is_inventory})
        if leaf.endswith(('Menu','Container','ScreenHandler')) and (is_inventory or 'Menu' in b or 'ScreenHandler' in b):menus.append({'class':n,'parent':b,'lineage':lineage})
    return {'screens':screens,'menus':menus,'gui_assets':assets,'parse_errors':errors,'declared_metadata':metadata}

def project(proj):
    slug=proj['slug'];cache=OUT/'versions'/f'{slug}.json';cache.parent.mkdir(exist_ok=True)
    try:
        if not cache.exists():
            url='https://api.modrinth.com/v2/project/'+proj['id']+'/version?'+urlencode({'game_versions':json.dumps(['1.20.1'])})
            cache.write_bytes(fetch(url))
        versions=read(cache);chosen={}
        for loader in ['fabric','forge']:
            matches=[v for v in versions if loader in v['loaders']]
            eligible=[v for v in matches if v['version_type']!='alpha'] or matches
            if eligible:chosen[loader]=max(eligible,key=lambda v:v['date_published'])
        releases=[]
        for ident in dict.fromkeys(v['id'] for v in chosen.values()):
            v=next(x for x in versions if x['id']==ident);f=next((x for x in v['files'] if x.get('primary')),v['files'][0])
            path=OUT/'jars'/f'{slug}-{ident}.jar';path.parent.mkdir(exist_ok=True)
            if not path.exists():path.write_bytes(fetch(f['url']))
            digest=hashlib.sha512(path.read_bytes()).hexdigest();assert digest==f['hashes']['sha512'],slug
            releases.append({'version_id':ident,'version':v['version_number'],'date_published':v['date_published'],'loaders':v['loaders'],'version_type':v['version_type'],'dependencies':v['dependencies'],'changelog':v['changelog'],
                'file':str(path.relative_to(ROOT)),'url':f['url'],'sha512':digest,**inspect(path)})
        return {'slug':slug,'name':proj['title'],'downloads':proj['downloads'],'project_url':'https://modrinth.com/mod/'+slug,'source_url':proj.get('source_url'),'description':proj['description'],'releases':releases}
    except Exception as e:return {'slug':slug,'name':proj['title'],'error':str(e),'releases':[]}

def main():
    projects=read(OUT/'selected-projects.json');results=[]
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
        for row in executor.map(project,projects):
            results.append(row);write(OUT/'release-ui-scan.json',results)
            containers=[s['class'].rsplit('/',1)[-1] for r in row['releases'] for s in r['screens'] if s['container_hierarchy']]
            controls=[s['class'].rsplit('/',1)[-1] for r in row['releases'] for s in r['screens'] if not s['container_hierarchy']]
            menus=[s['class'].rsplit('/',1)[-1] for r in row['releases'] for s in r['menus']]
            print(json.dumps({'slug':row['slug'],'container_screens':sorted(set(containers)),'other_screens':sorted(set(controls)),'menus':sorted(set(menus)),'error':row.get('error')}),flush=True)
    print('FINISHED',len(results),flush=True)

if __name__=='__main__':main()
