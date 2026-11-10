"""Record exact upstream dependency declarations without granting gameplay support.

Reads locally retained publisher bytes only. It neither downloads mods nor
modifies fixtures, version pins, public jars or gameplay assertions.
"""
from pathlib import Path
import argparse,collections,csv,datetime,hashlib,json,re,sys,tomllib,zipfile

RELATIONS=('depends','recommends','suggests','breaks','conflicts')
BUILTINS={'minecraft','java','fabricloader','forge','neoforge','javafml','lowcodefml'}
FIELDS=['project','minecraft','loader','upstream_version','publisher_filename','upstream_sha512','owner_mod_id','dependency_id','relation','constraint_json','environment','side','ordering','declared_type','declared_mandatory','declared_required','descriptor','descriptor_sha256']
DOCS={'fabric':'https://docs.fabricmc.net/develop/loader/fabric-mod-json','neoforge':'https://docs.neoforged.net/docs/1.21.1/gettingstarted/modfiles/'}

def sha(path,kind='sha256'):return hashlib.new(kind,path.read_bytes()).hexdigest()
def ref(root,path):return dict(file=str(path.relative_to(root)),sha256=sha(path))

def descriptor(raw,loader):
    assert loader in ['fabric','forge','neoforge']
    if loader=='fabric':
        d=json.loads(raw,strict=False);assert isinstance(d,dict)and isinstance(d.get('id'),str)and d['id']
        environment=d.get('environment','*');assert environment in ['*','client','server']
        deps=[]
        for relation in RELATIONS:
            values=d.get(relation,{});assert isinstance(values,dict)
            for key,constraint in values.items():
                assert isinstance(key,str)and key
                assert isinstance(constraint,str)or(isinstance(constraint,list)and constraint and all(isinstance(v,str)for v in constraint))
                deps.append(dict(owner_mod_id=d['id'],dependency_id=key,relation=relation,constraint=constraint,environment=environment,side='',ordering='',declared_type='',declared_mandatory='',declared_required=''))
        return dict(mod_ids=[d['id']],environment=environment,loader_name='fabricloader',loader_constraint='',dependencies=deps)
    d=tomllib.loads(raw.decode('utf-8-sig'));mods=d['mods'];assert isinstance(mods,list)and mods
    ids=[m['modId']for m in mods];assert all(isinstance(v,str)and v for v in ids)and len(ids)==len(set(ids))
    deps=[]
    for owner,values in d.get('dependencies',{}).items():
        assert owner in ids,'Dependency belongs to an undeclared root mod'
        assert isinstance(values,list)
        for v in values:
            key=v['modId'];assert isinstance(key,str)and key
            for flag in ['mandatory','required']:
                if flag in v:assert type(v[flag])is bool
            if loader=='neoforge':
                declared=v.get('type','');assert isinstance(declared,str)
                relation=declared.lower()if declared else'required'
                assert relation in ['required','optional','incompatible','discouraged']
            else:
                assert type(v.get('mandatory'))is bool,'Forge dependency must declare mandatory'
                relation='required'if v['mandatory']else'optional';declared=v.get('type','')
            constraint=v.get('versionRange','');assert isinstance(constraint,str)
            deps.append(dict(owner_mod_id=owner,dependency_id=key,relation=relation,constraint=constraint,environment='',side=v.get('side',''),ordering=v.get('ordering',''),declared_type=declared,declared_mandatory=v.get('mandatory',''),declared_required=v.get('required','')))
    return dict(mod_ids=ids,environment='',loader_name=d.get('modLoader',''),loader_constraint=d.get('loaderVersion',''),dependencies=deps)

def locate(root,pins):
    paths=collections.defaultdict(list);sources=[]
    def add(digest,path,filename,origin):
        if not digest or not path:return
        p=Path(path);p=p if p.is_absolute()else root/p
        if p.is_file():paths[digest].append((p,filename,origin))
    for manifest in sorted((root/'build/all-target-rollout/fixtures').glob('*/fixture.json')):
        sources.append(ref(root,manifest));data=json.loads(manifest.read_text())
        for a in data.get('artifacts',[]):add(a.get('sha512'),a.get('path',a.get('file')),a.get('filename',''),str(manifest.relative_to(root)))
    supplement=root/'output/release-readiness/public-support-supplement.json';sources.append(ref(root,supplement))
    for r in json.loads(supplement.read_text())['rows']:add(r['upstream_sha512'],r['publisher_file'],r['publisher_filename'],'support-supplement')
    native=root/'output/release-readiness/native-only-contracts-02.json';sources.append(ref(root,native))
    for r in json.loads(native.read_text())['records']:
        a=r['publisher'];add(a['sha512'],a['path'],a['filename'],'native-pin-contract')
    result={}
    for pin in pins:
        digest=pin['upstream_sha512'];assert re.fullmatch('[a-f0-9]{128}',digest)
        if digest in result:continue
        candidates=list(paths[digest])
        for folder in ['build/compatibility-coverage/upstream-fixtures/jars','build/all-target-rollout/upstream']:
            candidates += [(p,p.name[17:],'sha512-prefixed-cache')for p in sorted((root/folder).glob(digest[:16]+'-*.jar'))]
        assert candidates,('Exact publisher bytes missing',pin['project'],pin['minecraft'],pin['loader'])
        selected=next(((p,n,o)for p,n,o in candidates if sha(p,'sha512')==digest),None)
        assert selected,('No exact publisher hash match',pin['project'],digest)
        result[digest]=selected
    return result,sources

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--id',required=True)
    p.add_argument('--root',type=Path,default=Path(__file__).resolve().parents[1]);p.add_argument('--destination',type=Path)
    a=p.parse_args();assert re.fullmatch('[a-z0-9-]+',a.id)
    root=a.root.resolve();dest=a.destination.resolve()if a.destination else root/'docs/release'
    assert dest.is_relative_to(root)
    out=root/'output/release-readiness'/('upstream-dependencies-'+a.id+'.json');assert not out.exists()
    csv_source=root/'docs/release/compatibility-pins.csv';native_source=root/'docs/release/native-screen-pins.csv'
    pins=[r for r in csv.DictReader(csv_source.open(encoding='utf-8'))if r['upstream_sha512']]
    pins += list(csv.DictReader(native_source.open(encoding='utf-8')))
    assert pins and len({(r['project'],r['minecraft'],r['loader'],r['upstream_sha512'])for r in pins})==len(pins)
    paths,source_inputs=locate(root,pins);rows=[];records=[];descriptors={}
    for pin in pins:
        path,filename,origin=paths[pin['upstream_sha512']];loader=pin['loader']
        with zipfile.ZipFile(path)as jar:
            names=['fabric.mod.json']if loader=='fabric'else['META-INF/neoforge.mods.toml','META-INF/mods.toml']if loader=='neoforge'else['META-INF/mods.toml']
            name=next((n for n in names if n in jar.namelist()),None);assert name,('Missing native descriptor',filename,loader)
            raw=jar.read(name);data=descriptor(raw,loader)
        digest=hashlib.sha256(raw).hexdigest();archive_key=pin['upstream_sha512']+'/'+name
        assert archive_key not in descriptors or descriptors[archive_key]==raw;descriptors[archive_key]=raw
        entry=dict(**{k:pin[k]for k in ['project','minecraft','loader','upstream_version','upstream_sha512']},publisher_filename=filename,descriptor=name,descriptor_sha256=digest)
        for dependency in data['dependencies']:
            dep=dict(dependency);constraint=dep.pop('constraint');rows.append(dict(entry,**dep,constraint_json=json.dumps(constraint,separators=(',',':'))))
        if not data['dependencies']:
            rows.append(dict(entry,owner_mod_id=','.join(data['mod_ids']),dependency_id='',relation='none-declared',constraint_json='',environment=data['environment'],side='',ordering='',declared_type='',declared_mandatory='',declared_required=''))
        records.append(dict(entry,publisher_file=str(path.relative_to(root)),publisher_sha256=sha(path),path_source=origin,mod_ids=data['mod_ids'],environment=data['environment'],loader_name=data['loader_name'],loader_constraint=data['loader_constraint'],dependencies=data['dependencies']))
    assert len(records)==len(pins)and rows
    edge_count=sum(len(v['dependencies'])for v in records)
    dest.mkdir(parents=True,exist_ok=True);csv_dest=dest/'upstream-dependencies.csv'
    with csv_dest.open('w',newline='',encoding='utf-8')as stream:
        w=csv.DictWriter(stream,fieldnames=FIELDS);w.writeheader();w.writerows(rows)
    csv_rows=list(csv.DictReader(csv_dest.open(encoding='utf-8')));assert len(csv_rows)==len(rows)
    assert {(v['project'],v['minecraft'],v['loader'],v['upstream_sha512'])for v in csv_rows}=={(v['project'],v['minecraft'],v['loader'],v['upstream_sha512'])for v in pins},'Every reviewed pin must have descriptor dependency rows'
    # Human addon chains link only dependency IDs that have another recorded
    # root publisher on the same exact target. The full table includes libraries.
    providers=collections.defaultdict(set)
    for r in records:
        for mid in r['mod_ids']:providers[(r['minecraft'],r['loader'],mid)].add(r['project'])
    chains=collections.defaultdict(list)
    for r in records:
        deps=sorted({d['dependency_id']for d in r['dependencies']if d['relation']in ['depends','required']and d['dependency_id']not in r['mod_ids']and(r['minecraft'],r['loader'],d['dependency_id'])in providers})
        if deps:chains[(r['project'],tuple(deps))].append(r['minecraft']+' '+r['loader']+' / '+r['upstream_version'])
    text=['# Pinned upstream dependencies','',
        'This guide records declarations from the exact publisher jars in [the reviewed support table](compatibility-pins.md), including its six intentionally native entries. It does not add upstream support or establish gameplay qualification.','',
        'Install the upstream mods and their matching libraries separately from SIU. The [complete dependency table](upstream-dependencies.csv) preserves each publisher hash, owning mod ID, version constraint, environment/side, ordering and original required/optional fields. Version alternatives remain JSON arrays. Blank fields preserve an omitted declaration.','',
        'These declarations are not a list of separate downloads: a publisher may bundle libraries, and a mod manager can supply transitive dependencies. This inventory does not resolve the complete dependency graph, validate every range against installed libraries, or override publisher constraints. Optional addons remain optional for SIU. A `none-declared` row records an empty descriptor, not proof that a mod is dependency-free.','',
        'Fabric `depends` and Forge `mandatory=true` declare requirements. Fabric recommendations, suggestions and conflicts retain their distinct original categories. NeoForge uses its dependency `type`, defaulting to `required` when omitted; legacy or extra flags are preserved in separate columns. See the [Fabric descriptor reference]('+DOCS['fabric']+') and [NeoForge 1.21.1 descriptor reference]('+DOCS['neoforge']+').','',
        '## Declared addon chains','',
        'These are required base mods that also have a pin in this inventory on the same target. Library requirements and exact ranges/sides remain in the CSV. This table does not certify that every listed optional combination works together.','',
        '| Addon / dependent mod | Required recorded base mod IDs | Exact target / publisher release |','| --- | --- | --- |']
    for (project,deps),targets in sorted(chains.items(),key=lambda v:(v[0][0].casefold(),v[0][1])):
        text.append('| '+project.replace('|','\\|')+' | '+', '.join('`'+d+'`'for d in deps)+' | '+'; '.join(targets).replace('|','\\|')+' |')
    text+=['',f'Inspected {len(records)} exact mod/target pins and {edge_count} declared dependency edges. This is metadata documentation, separate from the current release gameplay campaign.','']
    md_dest=dest/'UPSTREAM_DEPENDENCIES.md';md_dest.write_text('\n'.join(text),encoding='utf-8')
    archive=out.with_suffix('.descriptors.zip');assert not archive.exists()
    with zipfile.ZipFile(archive,'x',zipfile.ZIP_DEFLATED)as z:
        for name,raw in sorted(descriptors.items()):z.writestr(name,raw)
    with zipfile.ZipFile(archive)as z:
        assert len(z.namelist())==len(set(z.namelist()))and z.testzip()is None
        assert all(z.read(k)==v for k,v in descriptors.items())
    result=dict(status='exact-publisher-dependency-declarations-inspected-not-gameplay',generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),pins=len(records),dependencies=edge_count,csv_rows=len(rows),records=records,
        source_inputs=[ref(root,csv_source),ref(root,native_source),*source_inputs],auditor=ref(root,Path(__file__).resolve()),
        descriptor_archive=ref(root,archive),descriptor_members={k:hashlib.sha256(v).hexdigest()for k,v in descriptors.items()},
        outputs=[ref(root,csv_dest),ref(root,md_dest)],reference_docs=DOCS,
        limits='Exact root-descriptor declarations only. Embedded library resolution, complete dependency solving, native recipe availability and gameplay acceptance are not established. Public support pins, fixtures, original assertions and all shipping artifacts are unchanged.')
    out.write_text(json.dumps(result,indent=2)+'\n');print('Inspected',len(records),'pins,',edge_count,'native dependency edges; metadata only, no gameplay acceptance.')

if __name__=='__main__':main()

