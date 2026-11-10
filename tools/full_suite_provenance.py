"""Immutable QA inputs and scoped runtime evidence for the complete suite.

The old five-file ledger omitted launcher files actually used by the wrapper.
These archives include repository QA sources/resources and launcher tools before
execution. Completed ordinary gameplay profiles additionally retain their real
runtime copies and QA binaries. This module assigns no gameplay acceptance.
"""
from pathlib import Path,PurePosixPath
import hashlib,importlib.util,json,os,re,zipfile
from urllib.parse import unquote,urlparse

SCHEMA='full-suite-qa-inputs-v2'
SKIP_DIRS={'build','output','__pycache__','.git','.gradle','node_modules','.cache','target','bin'}
EXTENSIONS={'.java','.py','.ps1','.gradle','.kts','.json','.toml','.mcmeta','.properties','.xml','.accesswidener','.txt','.fsh','.vsh','.glsl','.vert','.frag','.png'}
# Release reporting and standalone feature drivers do not launch the complete
# suite. Changing their reports must not invalidate unrelated gameplay input.
REPORT_ONLY_PREFIXES=('audit-release','check-release-helper','collect-release','record-release-candidate','activate-release-candidate',
    'build-release-coverage','prepare-public-release-matrix','reconcile-','seal-focused-release','focused_release_evidence',
    'native_publisher_index','native_selector_evidence','release_candidate_identity','preserve-retained-candidate',
    'audit-retained-','verify-retained-','archive-release-')


def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def relative(root,path):
    root=Path(root).resolve();p=Path(path);p=p if p.is_absolute() else root/p;p=p.resolve()
    assert p.is_relative_to(root) and p.is_file(),'QA evidence escapes or is missing: '+str(p)
    return p.relative_to(root).as_posix()


def sources(root,case):
    root=Path(root).resolve();found=set()
    for folder in ['functional-tests','controlify/functional-tests','controlify/model','tools','gradle']:
        start=root/folder
        if not start.exists():continue
        for parent,dirs,files in os.walk(start):
            dirs[:]=[d for d in dirs if d not in SKIP_DIRS and not(folder=='functional-tests' and Path(parent)==start and d in {'release-specialized','release-archive'})]
            for name in files:
                p=Path(parent)/name
                if p.suffix not in EXTENSIONS:continue
                if folder=='tools' and name.startswith(REPORT_ONLY_PREFIXES):continue
                found.add(relative(root,p))
    for name in ['settings.gradle.kts','build.gradle.kts','gradle.properties','compatibility/coverage-targets.json','controlify/targets.json',case['script'],'tools/run-full-validation.py','tools/full_suite_provenance.py']:
        if (root/name).is_file():found.add(relative(root,name))
    assert found,'No QA inputs selected'
    return {name:sha(root/name) for name in sorted(found)}


def digest_inputs(inputs):
    assert inputs and all(isinstance(k,str) and re.fullmatch('[a-f0-9]{64}',v) for k,v in inputs.items())
    return hashlib.sha256(json.dumps(inputs,sort_keys=True,separators=(',',':')).encode()).hexdigest()


def verify_bundle(root,ref,expected):
    root=Path(root).resolve();name=relative(root,ref['file']);path=root/name
    assert sha(path)==ref['sha256'],'Changed QA archive'
    assert ref['inputs_sha256']==digest_inputs(expected)
    with zipfile.ZipFile(path) as bundle:
        names=bundle.namelist();assert len(names)==len(set(names)) and set(names)==set(expected),'QA archive members differ'
        for name,value in expected.items():
            key=PurePosixPath(name);assert not key.is_absolute() and '..' not in key.parts and ':' not in name and '\\' not in name
            assert hashlib.sha256(bundle.read(name)).hexdigest()==value,('Changed archived QA input',name)
    return True


def bundle(root,inputs,folder='qa-inputs'):
    root=Path(root).resolve();digest=digest_inputs(inputs)
    assert folder in {'qa-inputs','qa-binaries'}
    for name,expected in inputs.items():
        assert relative(root,name)==name,'QA archive input is not canonical'
        assert sha(root/name)==expected,'Current input differs before archive publication'
    output=root/'output/release-readiness/full-suite-provenance'/folder/(digest+'.zip');output.parent.mkdir(parents=True,exist_ok=True)
    if not output.exists():
        temporary=output.with_suffix('.tmp');assert not temporary.exists(),'Inspect interrupted archive before retrying'
        with zipfile.ZipFile(temporary,'x',zipfile.ZIP_DEFLATED) as z:
            for name,expected in sorted(inputs.items()):
                path=root/relative(root,name);raw=path.read_bytes();assert hashlib.sha256(raw).hexdigest()==expected,'Input changed during archive'
                info=zipfile.ZipInfo(name,date_time=(1980,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,raw)
        temporary.replace(output)
    ref=dict(file=relative(root,output),sha256=sha(output),inputs_sha256=digest)
    verify_bundle(root,ref,inputs);return ref


def runtime_subject(root,folder,runtime,name,expected,loader):
    root=Path(root).resolve();folder=Path(folder);runtime=Path(runtime);copied=runtime/'mods'/Path(name).name
    if sha(copied)==expected:return dict(file=relative(root,copied),sha256=expected,subject_sha256=expected,mode='exact-packaged-copy')
    assert loader=='forge','Actual complete-suite runtime copy differs'
    lineage=folder/'forge-remap-lineage.json';data=json.loads(lineage.read_text())
    assert data['schema_version']==1 and data['operation']=='SRG_TO_MCP'
    matches=[v for v in data['artifacts'] if Path(v['mapped']).resolve()==copied.resolve()]
    assert len(matches)==1,'Exact Forge mapped copy must have one lineage row'
    row=matches[0];original=Path(row['original']).resolve()
    assert original== (runtime/'original-mods'/copied.name).resolve(),'Forge source must be the preserved original copy'
    assert row['original_sha256']==expected and sha(original)==expected,'Forge remap source differs from shipping subject'
    assert row['mapped_sha256']==sha(copied),'Forge mapped bytes changed'
    for key in ['installer_tools','mappings']:assert sha(data[key])==data[key+'_sha256'],('Forge remap tool/input changed',key)
    spec=importlib.util.spec_from_file_location('full_suite_forge_resource_contract',root/'tools/remap-controlify-forge-qa.py')
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);module.verify_resources(original,copied)
    return dict(file=relative(root,copied),sha256=sha(copied),subject_sha256=expected,mode='declared-Forge-SRG_TO_MCP-QA-copy',
        original=dict(file=relative(root,original),sha256=expected),lineage=dict(file=relative(root,lineage),sha256=sha(lineage)),
        transformation=dict(operation=data['operation'],**{k:data[k] for k in ['installer_tools','installer_tools_sha256','mappings','mappings_sha256']}))


def ordinary_report(root,case,entry):
    args=case['args'];scenario=args[args.index('-Scenario')+1] if '-Scenario' in args else 'core'
    mc=case['minecraft'];loader=case['loader']
    folder=Path(root)/'build/gameplay-qa'/('packaged-'+scenario if loader=='fabric' else 'packaged-native')/(mc+'-'+loader+'-'+entry['run_id'])
    return folder,scenario


def loaded_code_source(root,runtime,value):
    """Retain the file actually named by the loader, including normal remaps."""
    assert isinstance(value,str) and value,'Missing actual loaded code source'
    location=value[4:] if value.startswith('jar:') else value
    parsed=urlparse(location);assert parsed.scheme in {'file','union'},'Unexpected loaded code-source protocol'
    location=unquote(parsed.path).split('!/',1)[0].split('#',1)[0]
    if os.name=='nt' and re.match(r'^/[A-Za-z]:/',location):location=location[1:]
    path=Path(location).resolve()
    assert path.is_relative_to(Path(runtime).resolve()) and path.suffix=='.jar','Loaded implementation is outside the isolated runtime'
    return dict(url=value,file=relative(root,path),sha256=sha(path),
        scope='Actual loader code source; development-loader processing may differ from its separately retained packaged input')


def ordinary_dependencies(root,folder,runtime,case,scenario):
    root=Path(root).resolve();manifest=Path(folder)/'upstream-fixtures.json';data=json.loads(manifest.read_text(encoding='utf-8'))
    assert (data['minecraft'],data['loader'],data['scenario'])==(case['minecraft'],case['loader'],scenario)
    upstream=[]
    for row in data['artifacts']:
        source=root/relative(root,row['path']);raw=source.read_bytes()
        assert hashlib.sha512(raw).hexdigest()==row['sha512'],'Changed pinned upstream dependency'
        record=dict(file=relative(root,source),sha256=hashlib.sha256(raw).hexdigest(),sha512=row['sha512'],filename=row['filename'],coordinate=row.get('coordinate'))
        if row.get('coordinate'):
            record['runtime_copy']=runtime_subject(root,folder,runtime,row['filename'],record['sha256'],case['loader'])
        upstream.append(record)
    mods={relative(root,p):sha(p) for p in sorted((Path(runtime)/'mods').glob('*.jar'))}
    return dict(manifest=dict(file=relative(root,manifest),sha256=sha(manifest)),
        upstream=upstream,runtime_mod_archives=mods,
        scope='Exact generated upstream pins and staged mods; loader/platform libraries remain in the retained launch log and loaded-mod report')


def ordinary_gameplay_evidence(root,case,entry):
    """Known wrapper: preserve the actual report, runtime mods and QA output."""
    root=Path(root).resolve();result=dict(scope='QA-source archive and outer process log; inner scope remains in the named runner evidence')
    if case['script']!='functional-tests/scripts/Run-ReleaseGameplayQa.ps1':return result
    mc=case['minecraft'];loader=case['loader'];folder,scenario=ordinary_report(root,case,entry)
    report=folder/'results.json';data=json.loads(report.read_text(encoding='utf-8'))
    assert data['failures']==0 and data['minecraft']==mc and data['loader']==loader and data['scenario']==scenario
    if case['name']=='complete-core':assert data['completeCoreFeatureSuite'] is True
    assert data['results'] and any(v.get('status')=='PASS' for v in data['results'])
    assert not any(v.get('status') in {'FAIL','FAILED','ERROR'} for v in data['results'])
    runtime=Path(data['runDirectory']).resolve();assert runtime.is_relative_to(root),'Unexpected ordinary gameplay runtime'
    dependencies=ordinary_dependencies(root,folder,runtime,case,scenario)
    loaded={key:loaded_code_source(root,runtime,data[key]) for key in ['coreCodeSource','compatibilityCodeSource'] if key in data}
    assert 'coreCodeSource' in loaded
    if any('/compatibility/' in name for name in case['artifacts']):assert 'compatibilityCodeSource' in loaded
    copies=[]
    for name,expected in case['artifacts'].items():
        copies.append(runtime_subject(root,folder,runtime,name,expected,loader))
    binaries={}
    if loader=='fabric':
        text=(folder/'launch.log').read_text(encoding='utf-8',errors='replace')
        hits=re.findall(r'^PACKAGED_QA_PROJECT_CLASSPATH=\[(.*)\]\s*$',text,re.MULTILINE)
        assert len(hits)==1,'Actual Fabric QA classpath was not recorded'
        paths=[Path(v.strip()).resolve() for v in hits[0].split(',')]
        assert len(paths)>=2 and all(p.is_relative_to(root) and p.is_dir() for p in paths)
        for path in paths:
            for parent,dirs,files in os.walk(path):
                for name in files:
                    p=Path(parent)/name;binaries[relative(root,p)]=sha(p)
        assert any(p.endswith('/InWorldGameplayHarness.class') for p in binaries),'Actual QA harness binary is absent'
    else:
        archives=list((runtime/'mods').glob('chest-close-qa-*.jar'))
        assert len(archives)==1,'Expected the actual complete-suite excluded QA jar'
        binaries[relative(root,archives[0])]=sha(archives[0])
        with zipfile.ZipFile(archives[0]) as z:assert 'com/salts_inventory_update/functionaltest/InWorldGameplayHarness.class' in z.namelist()
    assert binaries
    result=dict(scope='Actual ordinary gameplay report, production runtime copies and excluded QA binaries',
        report=dict(file=relative(root,report),sha256=sha(report)),runtime_copies=copies,
        qa_binaries=binaries,qa_binary_archive=bundle(root,binaries,'qa-binaries'),
        dependency_evidence=dependencies,loaded_code_sources=loaded)
    return result


def verify_attempt(root,entry,case=None):
    assert entry['qa_provenance_schema']==SCHEMA and type(entry['exit_code']) is int and entry['exit_code']==0
    verify_bundle(root,entry['qa_source_archive'],entry['qa_inputs'])
    assert sha(Path(root)/entry['log'])==entry['log_sha256'],'Changed complete-suite process log'
    runtime=entry['runtime_evidence']
    if case and case['script']=='functional-tests/scripts/Run-ReleaseGameplayQa.ps1':
        folder,scenario=ordinary_report(root,case,entry)
        assert runtime['report']['file']==relative(root,folder/'results.json')
        copies=runtime['runtime_copies'];actual={(Path(v['file']).name,v['subject_sha256']) for v in copies}
        expected={(Path(k).name,v) for k,v in case['artifacts'].items()}
        assert len(copies)==len(actual) and actual==expected,'Actual runtime subjects do not match the complete-suite case'
        dependencies=runtime['dependency_evidence']
        assert dependencies['manifest']['file']==relative(root,folder/'upstream-fixtures.json')
    if 'report' in runtime:
        assert sha(Path(root)/runtime['report']['file'])==runtime['report']['sha256']
        # The immutable binary archive remains valid if a later Gradle profile
        # legitimately reuses its shared compile-output directory.
        verify_bundle(root,runtime['qa_binary_archive'],runtime['qa_binaries'])
        for copy in runtime['runtime_copies']:
            assert sha(Path(root)/copy['file'])==copy['sha256']
            if copy['mode']=='exact-packaged-copy':assert copy['subject_sha256']==copy['sha256']
            else:
                assert copy['mode']=='declared-Forge-SRG_TO_MCP-QA-copy'
                for key in ['original','lineage']:assert sha(Path(root)/copy[key]['file'])==copy[key]['sha256']
                assert copy['original']['sha256']==copy['subject_sha256']
                transform=copy['transformation'];assert transform['operation']=='SRG_TO_MCP'
                for key in ['installer_tools','mappings']:assert sha(transform[key])==transform[key+'_sha256']
        dependencies=runtime.get('dependency_evidence')
        if dependencies:
            assert sha(Path(root)/dependencies['manifest']['file'])==dependencies['manifest']['sha256']
            for path,expected in dependencies['runtime_mod_archives'].items():assert sha(Path(root)/path)==expected
            for row in dependencies['upstream']:
                raw=(Path(root)/row['file']).read_bytes();assert hashlib.sha256(raw).hexdigest()==row['sha256'] and hashlib.sha512(raw).hexdigest()==row['sha512']
                if row.get('runtime_copy'):
                    copy=row['runtime_copy'];assert sha(Path(root)/copy['file'])==copy['sha256']
                    assert copy['subject_sha256']==row['sha256']
                    if copy['mode']=='declared-Forge-SRG_TO_MCP-QA-copy':
                        for key in ['original','lineage']:assert sha(Path(root)/copy[key]['file'])==copy[key]['sha256']
            for source in runtime['loaded_code_sources'].values():assert sha(Path(root)/source['file'])==source['sha256']
    return True
