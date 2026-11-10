"""Verify the explicit 1.20.1 Drive expectation correction and native atlas.

Historical y=4 failures stay failed. Later complete atlas runs must retain
all inputs and captures, the exact replacement, native slot geometry and
non-overlapping controls. Native runtime/Q A archives are measured separately.
"""
from pathlib import Path
import ast,importlib.util,json,re

from focused_release_evidence import checked, reference
from full_suite_provenance import (sources, digest_inputs, sha, bundle,
    ordinary_dependencies, loaded_code_source, runtime_subject)

KIND='legacy-AE2-Drive-native-safe-gutter-v1'
NATIVE_KIND='AE2-native-atlas-runtime-observation-v1'
MANIFEST=dict(file='output/release-readiness/ae2-drive-gutter-188-01/manifest.json',
    sha256='611a74a02121f9a62ea26b10936b7cb4b164d578d7597cb33319f8c95270cd59')
BASE='functional-tests/scripts/Run-Ae2ReleaseQa.ps1'
DRIVER='build/release-readiness/run-full-with-drive-gutter-189-01.py'
OBSERVER='tools/focused_release_evidence_ae2_drive_gutter.py'
PRIOR={OBSERVER:dict(file='output/release-readiness/atlas-runtime-layout-correction-206-01/observer-before.py',sha256='931cc475998935ae3a5308dc38f26c0728fc0ebfc7518ee610b9adb61ca9379c'),
       DRIVER:dict(file='output/release-readiness/atlas-runtime-layout-correction-206-01/driver-before.py',sha256='5c506c575ceae6edeaba2f52e342dfbf8b490578f61f7bd346c0c9abe0cbfaf0')}


def observer_versions(root,expected,recorded=None):
    """Retain only this named metadata correction; gameplay inputs stay exact."""
    if recorded is None:return expected
    assert set(recorded)==set(expected)
    changed={n for n in expected if expected[n]!=recorded[n]}
    if changed:
        assert changed<=set(PRIOR),'Native QA or launcher inputs changed'
        for n in changed:
            old=checked(root,PRIOR[n]);assert recorded[n]==PRIOR[n]['sha256']
            functions=lambda p:{n.name:ast.dump(n,include_attributes=False)for n in ast.parse(p.read_text()).body if isinstance(n,(ast.FunctionDef,ast.AsyncFunctionDef))}
            previous,current=functions(old),functions(root/n)
            required=['verify_drive_geometry']if n==OBSERVER else['plan','runtime','verify']
            assert all(previous[k]==current[k]for k in required),'Original gameplay assertions/actions changed'
            expected[n]=recorded[n]
    return expected


def expected_inputs(root,case,recorded=None):
    root=Path(root).resolve();m=json.loads(checked(root,MANIFEST).read_text())
    assert m['schema']==KIND and m['status']=='explicit-fixture-correction-prepared-not-qualified'
    assert case['qa_derivation']==dict(kind=KIND,base_script=BASE,manifest=MANIFEST)
    assert case['minecraft']=='1.20.1' and case['loader'] in {'fabric','forge'} and case['name']=='ae2-atlas'
    assert case['key']=='1.20.1:'+case['loader']+':ae2-atlas'
    assert case['script']==m['wrapper']['file'] and case['kinds']==['core','compatibility'] and case['gradle'] is False
    assert case['args']==['-Minecraft','1.20.1','-Loader',case['loader'],'-RunId','{runid}']
    original=checked(root,m['original_source']).read_text();derived=original
    for change in m['substitutions']:
        assert derived.count(change['before'])==1;derived=derived.replace(change['before'],change['after'])
    assert checked(root,m['derived_source']).read_text()==derived
    checked(root,m['original_failure']);checked(root,m['diagnostic']);checked(root,m['actual_bounds'])
    expected=sources(root,dict(case,script=BASE))
    assert len(expected)==1144 and digest_inputs(expected)==m['original_qa_inputs_sha256']
    for ref in [*m['derived_files'],m['preparer'],MANIFEST]:
        checked(root,ref);expected[ref['file']]=ref['sha256']
    expected[DRIVER]=sha(root/DRIVER)
    expected['tools/focused_release_evidence_ae2_drive_gutter.py']=sha(__file__)
    assert len(expected)==1151
    return dict(sorted(observer_versions(root,expected,recorded).items()))


def expected_native_inputs(root,case,recorded=None):
    root=Path(root).resolve()
    assert case['name'] in {'ae2-atlas','ae2-cpu-lifecycle'} and case['script']==BASE
    wanted=reference(root,root/OBSERVER)
    actual=case['qa_derivation']
    if recorded is not None and actual.get('observer')==dict(file=OBSERVER,sha256=PRIOR[OBSERVER]['sha256']):wanted=actual['observer']
    assert actual==dict(kind=NATIVE_KIND,base_script=BASE,observer=wanted)
    args=['-Minecraft',case['minecraft'],'-Loader',case['loader']]
    if case['name']=='ae2-cpu-lifecycle':args+=['-CpuLifecycle']
    assert case['args']==args+['-RunId','{runid}']
    expected=sources(root,case)
    assert len(expected)==1144 and digest_inputs(expected)=='57c834108359e803dece14e906db7add54bbc09fdd1ae2582acde106c9d04698'
    for name in [DRIVER,'tools/focused_release_evidence_ae2_drive_gutter.py']:expected[name]=sha(root/name)
    assert len(expected)==1146
    return dict(sorted(observer_versions(root,expected,recorded).items()))


def verify_drive_geometry(controls,geometry,window):
    assert len(controls)==5 and [c['kind']for c in controls]==['FOCUS','PIN','LOCK','LINK','CLOSE']
    slots=[v for k,v in geometry.items() if k.startswith('slot/')]
    assert len(slots)==10 and sorted(set((v[0],v[1])for v in slots))==[(x,y)for x in [71,89]for y in [14,32,50,68,86]]
    x,y,w,h=window
    for c in controls:
        assert all(type(c[k]) is int for k in ['x','y','width','height'])
        assert c['y']-y==2 and c['width']==c['height']==11
        assert x<=c['x'] and y<=c['y'] and c['x']+11<=x+w and c['y']+11<=y+h
        for sx,sy in slots:
            rx,ry=x+sx-1,y+sy-1
            assert not(c['x']<rx+18 and c['x']+11>rx and c['y']<ry+18 and c['y']+11>ry),'Control covers native rim'
    return True


def atlas_subject(root,folder,runtime,name,expected,loader):
    if (runtime/'mods'/Path(name).name).is_file():return runtime_subject(root,folder,runtime,name,expected,loader)
    actual=runtime/'qa-launch-mods'/Path(name).name
    assert actual.is_file()and loader in {'forge','neoforge'}
    if sha(actual)==expected:return dict(file=actual.relative_to(root).as_posix(),sha256=expected,subject_sha256=expected,mode='exact-packaged-copy')
    assert loader=='forge'
    lineage=folder/'forge-remap-lineage.json';data=json.loads(lineage.read_text());original=runtime/'original-mods'/Path(name).name
    assert data['schema_version']==1 and data['operation']=='SRG_TO_MCP'and sha(original)==expected
    rows=[v for v in data['artifacts']if Path(v['original']).resolve()==original.resolve()and Path(v['mapped']).resolve()==(runtime/'mods'/Path(name).name).resolve()]
    assert len(rows)==1 and rows[0]['original_sha256']==expected and rows[0]['mapped_sha256']==sha(actual)
    for k in ['installer_tools','mappings']:assert sha(data[k])==data[k+'_sha256']
    spec=importlib.util.spec_from_file_location('atlas_original_resource_verifier',root/'tools/remap-controlify-forge-qa.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);module.verify_resources(original,actual)
    return dict(file=actual.relative_to(root).as_posix(),sha256=sha(actual),subject_sha256=expected,mode='declared-Forge-SRG_TO_MCP-QA-copy',
        original=reference(root,original),lineage=reference(root,lineage),mapped_before_group=str(runtime/'mods'/Path(name).name),
        transformation=dict(operation=data['operation'],**{k:data[k]for k in ['installer_tools','installer_tools_sha256','mappings','mappings_sha256']}))


def atlas_group(root,folder,runtime,copies,loaded):
    path=folder/'qa-runtime-group.json';data=json.loads(path.read_text())
    assert data['purpose']=='AE2 observation and metadata overlay; no duplicated production classes'and len(data['archives'])==2
    classes=[];qa=[];archives=[]
    for row in data['archives']:
        p=Path(row['file']).resolve();assert p.is_relative_to(runtime)and sha(p)==row['sha256']
        with __import__('zipfile').ZipFile(p)as z:
            names=z.namelist();assert len(names)==len(set(names))and z.testzip()is None
            classes.append({n for n in names if n.endswith('.class')})
            if 'com/salts_inventory_update/compat/ae2/qa/DesktopAtlasQa.class'in names:
                assert 'com/salts_inventory_update/functionaltest/InWorldGameplayHarness.class'in names;qa.append(reference(root,p))
        archives.append(reference(root,p))
    assert len(qa)==1 and not(classes[0]&classes[1])
    implementation=next(v for v in copies if '/salts_inventory_compat-'in v['file'])
    assert implementation['file']in {v['file']for v in archives}
    assert loaded['compatibilityCodeSource']['file']in {v['file']for v in archives}
    return dict(receipt=reference(root,path),archives=archives,QA_archive=qa[0],duplicate_production_classes=0,
        compatibility_source_scope='Observed FML group protection domain; production classes reside in the separately verified Compatibility archive, with no duplicate classes in the observation archive.')


def native_evidence(root,case,entry):
    """Use the existing full-suite runtime contracts with the atlas's real path."""
    root=Path(root).resolve();mc,loader=case['minecraft'],case['loader']
    folder=root/'build/gameplay-qa/packaged-native'/(mc+'-'+loader+'-'+entry['run_id'])
    report=folder/'results.json';data=json.loads(report.read_text())
    assert data['failures']==0 and data['scenario']=='ae2-atlas' and data['minecraft']==mc and data['loader']==loader
    assert data['results'] and not any(v['status'] in {'FAIL','FAILED','ERROR'}for v in data['results'])
    runtime=Path(data['runDirectory']).resolve();assert runtime.is_relative_to(root)
    dependencies=ordinary_dependencies(root,folder,runtime,case,'ae2-atlas')
    loaded={k:loaded_code_source(root,runtime,data[k])for k in ['coreCodeSource','compatibilityCodeSource']}
    copies=[atlas_subject(root,folder,runtime,n,digest,loader)for n,digest in case['artifacts'].items()]
    binaries={}
    if loader=='fabric':
        hits=re.findall(r'^PACKAGED_QA_PROJECT_CLASSPATH=\[(.*)\]\s*$',(folder/'launch.log').read_text(errors='replace'),re.MULTILINE)
        assert len(hits)<=1
        paths=[Path(v.strip()).resolve()for v in hits[0].split(',')]if hits else[root/f'versions/{mc}/{loader}/build/classes/java/chestCloseQa',root/f'versions/{mc}/{loader}/build/resources/chestCloseQa']
        for p in paths:
            assert p.is_relative_to(root)and p.is_dir()
            for f in p.rglob('*'):
                if f.is_file():binaries[f.relative_to(root).as_posix()]=sha(f)
        assert any(n.endswith('/DesktopAtlasQa.class')for n in binaries)
    else:
        group=atlas_group(root,folder,runtime,copies,loaded);qa=checked(root,group['QA_archive']);binaries[qa.relative_to(root).as_posix()]=sha(qa)
    files=[p for p in (folder/'atlas').iterdir()if p.is_file()]
    atlas={p.relative_to(root).as_posix():sha(p)for p in files}
    result=dict(scope='Actual complete native AE2 atlas, production copies and post-run observation binaries',
        report=reference(root,report),runtime_copies=copies,qa_binaries=binaries,
        qa_binary_archive=bundle(root,binaries,'qa-binaries'),dependency_evidence=dependencies,
        loaded_code_sources=loaded,atlas_files=atlas,atlas_archive=bundle(root,atlas,'qa-binaries'))
    result['qa_binary_measurement']='Post-run declared Gradle QA outputs, not a prelaunch/native-VM binary checksum observation'
    if loader!='fabric':result['FML_group']=group
    return result


def verify_attempt(root,entry,case,original_verify):
    root=Path(root).resolve()
    legacy=case['qa_derivation']['kind']==KIND
    assert entry['qa_inputs']==(expected_inputs(root,case,entry['qa_inputs'])if legacy else expected_native_inputs(root,case,entry['qa_inputs']))
    original_verify(root,entry,case)
    runtime=entry['runtime_evidence'];assert 'report'in runtime and 'atlas_archive'in runtime
    folder=root/'build/gameplay-qa/packaged-native'/(case['minecraft']+'-'+case['loader']+'-'+entry['run_id'])
    assert runtime['report']['file']==(folder/'results.json').relative_to(root).as_posix()
    from full_suite_provenance import verify_bundle
    verify_bundle(root,runtime['atlas_archive'],runtime['atlas_files'])
    if case['loader']!='fabric':
        report=json.loads(checked(root,runtime['report']).read_text());actual=Path(report['runDirectory']).resolve()
        assert runtime['FML_group']==atlas_group(root,folder,actual,runtime['runtime_copies'],runtime['loaded_code_sources'])
    if not legacy:return True
    completed=(folder/'atlas/completed.txt').read_text();match=re.fullmatch(r'Registered containers: (\d+)\s*',completed)
    assert match and int(match[1])>20,'Focused Drive observation cannot qualify a complete atlas'
    candidates=list((folder/'atlas').glob('*-me-drive.controls.json'));assert len(candidates)==1
    controls=json.loads(candidates[0].read_text());geometry=json.loads(candidates[0].with_name(candidates[0].name.replace('.controls.json','.geometry.json')).read_text())
    # Geometry/capture metadata uses native window coordinates; the gutter is
    # independently checked again from the captured control bounds.
    slots=[v for k,v in geometry.items()if k.startswith('slot/')]
    assert slots
    # The captured toolbar/slot geometry is local; the native main window is
    # independently recorded by the QA manifest's screen coordinates.
    rows=(folder/'atlas/manifest.csv').read_text().splitlines();header=rows[0].split(',')
    rows=[dict(zip(header,line.split(',')))for line in rows[1:]if ',me-drive,' in line]
    assert len(rows)==1, 'Missing original native Drive capture row'
    row=rows[0]
    verify_drive_geometry(controls,geometry,[int(row[k])for k in ['x','y','width','height']])
    return True
