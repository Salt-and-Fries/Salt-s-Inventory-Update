"""Exact Fabric RuntimeModRemapper lineage for packaged native CPU QA subjects."""
from pathlib import Path
import importlib.util,inspect,json,re

_TOOLS={}


def _write_once(path,text):
    if path.exists():assert path.read_text()==text,'Retained lineage input changed: '+str(path)
    else:path.write_text(text)


def derive(root,folder,subjects,copies,loaded):
    root=Path(root).resolve();folder=Path(folder).resolve();assert folder.is_relative_to(root)
    assert set(subjects)=={'core','compatibility'}and set(loaded)=={'coreCodeSource','compatibilityCodeSource'}
    report_path=folder/'results.json';report=json.loads(report_path.read_text())
    assert report['loader']=='fabric'and report['scenario']=='ae2-atlas'and report['failures']==0
    game=report['minecraft'];assert game=='1.20.1','Review the actual CPU Fabric target before extending this lineage contract'
    runtime=Path(report['runDirectory']).resolve();assert runtime.is_relative_to(root)
    source=root/'tools/verify-controlify-focused-runtime.py'
    key=str(source)
    if key not in _TOOLS:
        spec=importlib.util.spec_from_file_location('native_cpu_fabric_lineage',source)
        tool=importlib.util.module_from_spec(spec);spec.loader.exec_module(tool);_TOOLS[key]=tool
    tool=_TOOLS[key];assert tool.ROOT.resolve()==root
    versions=set(re.findall('Loading Minecraft '+re.escape(game)+r' with Fabric Loader ([^\s]+)',(runtime/'logs/debug.log').read_text()))
    assert len(versions)==1;version=versions.pop();mods=tool.loaded_mods(report,'native-cpu-controls')
    assert mods['fabricloader']==version
    loaders=list((Path.home()/'.gradle/caches/modules-2/files-2.1/net.fabricmc/fabric-loader'/version).glob('*/fabric-loader-'+version+'.jar'))
    assert len(loaders)==1 and loaders[0].is_file()
    proof=folder/'fabric-native-lineage';proof.mkdir(exist_ok=True)
    compiler_inputs=root/f'controlify/versions/{game}/fabric/build/controlify-compile-classpath.txt'
    paths=[Path(v).resolve()for v in compiler_inputs.read_text().splitlines()if v]
    old=[p for p in paths if p.name.startswith('fabric-loader-')and p.suffix=='.jar'];assert len(old)==1
    paths=[loaders[0]if p==old[0]else p for p in paths]
    cp=proof/'native-proof-compile-classpath.txt';_write_once(cp,'\n'.join(map(str,paths))+'\n')
    original=inspect.getsource(tool.fabric_named_lineage)
    old='target = ROOT / f"controlify/versions/{game}/fabric"';assert original.count(old)==1
    text=original.replace(old,'target = ROOT / f"versions/{game}/fabric"',1)
    old='compile_classpath = target / "build/controlify-compile-classpath.txt"';assert text.count(old)==1
    text=text.replace(old,'compile_classpath = ROOT / '+repr(cp.relative_to(root).as_posix()),1)
    code=proof/'derived-verifier.py';_write_once(code,text)
    namespace={};exec(compile(text,str(code),'exec'),tool.__dict__,namespace)
    staged={p.name:p for p in(runtime/'mods').glob('*.jar')}
    mapping,lineage=namespace['fabric_named_lineage'](runtime,game,staged,mods,report_path)
    assert len(copies)==len(subjects)
    for role,code_key in [('core','coreCodeSource'),('compatibility','compatibilityCodeSource')]:
        name,digest=subjects[role];original=runtime/'mods'/name
        assert tool.runtime.digest(original)==digest
        records=[v for v in copies if Path(v['file']).name==name];assert len(records)==1
        assert records[0]['sha256']==records[0]['subject_sha256']==digest
        assert (root/records[0]['file']).resolve()==original.resolve()
        assert mapping[name].resolve()==(root/loaded[code_key]['file']).resolve()
        assert tool.runtime.digest(mapping[name])==loaded[code_key]['sha256']
        matches=[v for v in lineage['artifacts']if v['original']==name]
        assert len(matches)==1 and matches[0]['original_sha256']==digest
        assert matches[0]['mapped_sha256']==loaded[code_key]['sha256']
    from focused_release_evidence import reference
    return dict(schema='ae2-cpu-exact-native-fabric-remap-v1',
        report=reference(root,report_path),helper=reference(root,Path(__file__)),
        original_verifier=reference(root,source),derived_verifier=reference(root,code),
        java_helper=reference(root,root/'tools/VerifyFabricRuntimeRemap.java'),
        compile_classpath=reference(root,cp),lineage=lineage,
        scope='Exact packaged Core/Compatibility inputs are linked to actual loaded files by reproducing native Fabric processing and comparing every archive member. This is artifact provenance for the recorded CPU execution, not another gameplay run.')
