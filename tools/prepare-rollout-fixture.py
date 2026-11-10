"""Prepare an isolated, exact-release runtime using the rollout availability lock."""
from pathlib import Path
import argparse,importlib.util,json,urllib.request
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/all-target-rollout'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--minecraft',required=True);parser.add_argument('--loader',required=True);parser.add_argument('--project',action='append',default=[]);parser.add_argument('--project-version',action='append',default=[],help='Explicit publisher version number, project=version');parser.add_argument('--dependency-project',action='append',default=[]);parser.add_argument('--dependency-version',action='append',default=[],help='Publisher library baseline, dependency-project=version');parser.add_argument('--name',required=True);args=parser.parse_args()
    selected_versions=dict(value.split('=',1) for value in args.project_version)
    selected_dependencies=dict(value.split('=',1) for value in args.dependency_version)
    if set(selected_dependencies)-set(args.dependency_project):raise ValueError('Explicit dependency versions require an included dependency project')
    if set(selected_versions)-set(args.project):raise ValueError('Explicit versions require an included project')
    lock=json.loads((OUT/'availability.json').read_text());key=args.minecraft+'/'+args.loader
    projects={};versions={};project_aliases={};maven_versions={};maven_coordinates={};maven_releases={}
    pins=OUT/'publisher-dependency-pins.json';dependency_pins=json.loads(pins.read_text()) if pins.exists() else []
    for name,p in lock['projects'].items():
        project_aliases[p['project_id']]=name
        for v in json.loads((OUT/'metadata'/('project-'+name+'-version.json')).read_text()):versions[v['id']]=v
        release=p['targets'].get(key)
        if release and release.get('source_kind')=='official-publisher-file':
            baseline=versions[release['dependency_baseline_version_id']]
            identifier='publisher-file-'+release['sha512'][:24]
            version={**baseline,'id':identifier,'version_number':release['version'],
                'files':[{'primary':True,'filename':release['filename'],'url':release['download'],'hashes':{'sha512':release['sha512'],'sha1':release['sha1']}}],
                'source_kind':'official-publisher-file'}
            versions[identifier]=version;maven_versions[name]=version
            maven_releases[name]={**release,'version_id':identifier}
        if release and release.get('source_kind')=='official-maven-snapshot':
            release=next((pin['release'] for pin in dependency_pins if pin['project']==name and pin['target']==key and pin['dependent'] in args.project),release)
            dependencies=[]
            for dependency in ['patchouli','curios']:
                meta=OUT/'metadata'/('project-'+dependency+'.json')
                if not meta.exists():
                    with urllib.request.urlopen(urllib.request.Request('https://api.modrinth.com/v2/project/'+dependency,headers={'User-Agent':'SIU-Compatibility-Rollout/1.0'}),timeout=40) as response:meta.write_text(json.dumps(json.load(response)))
                dependencies.append({'project_id':json.loads(meta.read_text())['id'],'version_id':None,'dependency_type':'required'})
            identifier='maven:'+release['coordinate']
            version={'id':identifier,'project_id':p['project_id'],'version_number':release['version'],'game_versions':[args.minecraft],'loaders':[args.loader],
                'files':[{'primary':True,'filename':release['filename'],'url':release['download'],'hashes':{'sha512':release['sha512'],'sha1':release['sha1']}}],
                'dependencies':dependencies,'version_type':'alpha','source_kind':'official-maven-snapshot'}
            versions[identifier]=version;maven_versions[name]=version;maven_coordinates[release['sha512']]=release['coordinate']
            maven_releases[name]=release
    # Explicit dependencies come first so a stale embedded copy cannot win the
    # preparer's expanded-library deduplication over the selected native library.
    for name in args.dependency_project:
        with urllib.request.urlopen(urllib.request.Request('https://api.modrinth.com/v2/project/'+name+'/version',headers={'User-Agent':'SIU-Compatibility-Rollout/1.0'}),timeout=40) as response:releases=json.load(response)
        matching=[v for v in releases if args.minecraft in v['game_versions'] and args.loader in v['loaders']]
        if name in selected_dependencies:
            matching=[v for v in matching if v['version_number']==selected_dependencies[name]]
            if len(matching)!=1:raise ValueError('Dependency baseline must resolve unambiguously: '+name+'='+selected_dependencies[name])
        if name=='resourceful-lib' and args.minecraft=='26.1.2':
            # The newest Bumblezone requires 4.0.2. Modrinth tags that library
            # only as 26.1, while its original Fabric descriptor declares
            # minecraft >=1.21.11; it is the publisher's 26.1 patch-line build.
            matching=[v for v in releases if v['version_number']=='4.0.2' and args.loader in v['loaders']]
        release=next((v for v in matching if v.get('version_type')=='release'),next((v for v in matching if v.get('version_type')=='beta'),matching[0]))
        versions[release['id']]=release;primary=next((f for f in release['files'] if f.get('primary')),release['files'][0])
        projects[name]={'available':True,'version_id':release['id'],'version':release['version_number'],'sha512':primary['hashes']['sha512']}
    for name in args.project:
        release=maven_releases.get(name,lock['projects'][name]['targets'].get(key))
        if not release:raise ValueError('No exact release: '+key+'/'+name)
        release=next((pin['release'] for pin in dependency_pins if pin['project']==name and pin['target']==key and pin['dependent'] in args.project),release)
        if name in selected_versions:
            matches=[v for v in versions.values() if v['project_id']==lock['projects'][name]['project_id'] and v['version_number']==selected_versions[name] and args.minecraft in v['game_versions'] and args.loader in v['loaders']]
            if len(matches)!=1:raise ValueError('Publisher version must resolve unambiguously: '+name+'='+selected_versions[name])
            v=matches[0];primary=next((f for f in v['files'] if f.get('primary')),v['files'][0])
            release={'version_id':v['id'],'version':v['version_number'],'sha512':primary['hashes']['sha512'],'pin_reason':'Explicit upstream dependency baseline'}
        projects[name]={**release,'available':True}
        if name in maven_versions:projects[name]['version_id']=maven_versions[name]['id']
    spec=importlib.util.spec_from_file_location('qa',ROOT/'tools/prepare-compatibility-qa.py');helper=importlib.util.module_from_spec(spec);spec.loader.exec_module(helper)
    target=next(t for t in json.loads((ROOT/'compatibility/coverage-targets.json').read_text())['targets'] if t['minecraft']==args.minecraft and t['loader']==args.loader)
    inputs=OUT/'fixtures'/args.name/'inputs-root';(inputs/'compatibility').mkdir(parents=True,exist_ok=True)
    (inputs/'compatibility/coverage-targets.json').write_text(json.dumps({'targets':[{**target,'integrations':projects}]}))
    helper.ROOT=inputs
    native={r['download']:OUT/'upstream'/(r['sha512'][:16]+'-'+r['filename']) for p in lock['projects'].values() for r in p['targets'].values()}
    native.update({r['download']:OUT/'upstream'/(r['sha512'][:16]+'-'+r['filename']) for r in maven_releases.values()})
    network=urllib.request.urlopen
    def request(url,*a,**kw):
        address=url.full_url if isinstance(url,urllib.request.Request) else url
        if address in native and native[address].exists():return native[address].open('rb')
        return network(url,*a,**kw)
    helper.urllib.request.urlopen=request
    def get_json(endpoint):
        if endpoint.startswith('version/') and endpoint[8:] in versions:return versions[endpoint[8:]]
        parts=endpoint.split('/')
        if len(parts)>=2 and parts[0]=='project':
            slug=project_aliases.get(parts[1],parts[1]);cache=OUT/'metadata'/('project-'+slug+('-version' if len(parts)==3 else '')+'.json')
            if cache.exists():
                data=json.loads(cache.read_text())
                if len(parts)==3 and slug in maven_versions:return [maven_versions[slug],*data]
                return data
        with network(urllib.request.Request('https://api.modrinth.com/v2/'+endpoint,headers=helper.HEADERS),timeout=40) as response:return json.load(response)
    helper.get_json=get_json
    fixture=OUT/'fixtures'/args.name/'fixture.json';helper.prepare(args.minecraft,args.loader,'extension-empty',fixture,projects.keys())
    data=json.loads(fixture.read_text());data['rollout_exact_projects']=projects
    if 'resourceful-lib' in args.dependency_project and args.minecraft=='26.1.2':
        data['publisher_library_note']='Resourceful Lib 4.0.2 is tagged 26.1 upstream; its original metadata permits this patch release. Required by the exact Bumblezone release; library ABI/runtime validation remains focused.'
    for artifact in data['artifacts']:
        if artifact['sha512'] in maven_coordinates:
            artifact['coordinate']=maven_coordinates[artifact['sha512']];artifact['source_kind']='official-maven-snapshot'
    if maven_versions and args.minecraft=='1.21.1' and args.loader=='neoforge':data.setdefault('runtime',{})['neoforge']='21.1.262'
    # Exact publisher dependency floors used by the October 2026 audit inputs.
    # These affect disposable QA runtimes only, never release compile baselines.
    if args.minecraft=='26.3' and args.loader=='fabric':data.setdefault('runtime',{})['fabric_api']='0.163.0+26.3'
    if args.minecraft=='26.3' and args.loader=='neoforge' and 'revampedwolf' in args.project:data.setdefault('runtime',{})['neoforge']='26.3.0.99'
    fixture.write_text(json.dumps(data,indent=2));print(fixture)

if __name__=='__main__':main()
