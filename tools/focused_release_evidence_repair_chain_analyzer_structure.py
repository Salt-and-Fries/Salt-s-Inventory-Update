"""Revalidate the prior nine repairs and only the exact hosted-analyzer addition."""
from pathlib import Path
import hashlib,json,shutil,zipfile
from focused_release_evidence import checked,reference,sha
from focused_release_evidence_core_log_transition import PRODUCTION,ROOT_BUILD
from focused_release_evidence_repair_chain_mi_structure import structure as prior_structure
from focused_release_evidence_analyzer_delta import inspect
PREPARATION={'file': 'output/release-readiness/repair-prefix-eleven-stage-inputs-616-01/manifest.json', 'sha256': '665c0ce5873c93d73ca5c75974143c8c75b2142d468b86630624299e449d9d76'}

def structure(root,folder):
    root=Path(root).resolve();folder=Path(folder).resolve();assert folder.is_relative_to(root)and not folder.exists();folder.mkdir(parents=True)
    prior=prior_structure(root,folder/'prior-nine-edges');graph=json.loads(checked(root,PREPARATION).read_text());previous_graph=json.loads(checked(root,graph['previous_preparation']).read_text())
    assert len(graph['stages'])==11 and len(graph['edges'])==10
    assert graph['stages'][:-1]==previous_graph['stages']and graph['edges'][:-1]==previous_graph['edges']
    stage,edge=graph['stages'][-1],graph['edges'][-1];assert stage['stage']=='613'and(edge['before'],edge['after'],edge['inspector_mode'])==('475','613','analyzer-refresh')
    snapshot=json.loads(checked(root,stage['source_snapshot']).read_text());files=snapshot['source_files'];digest=lambda raw:hashlib.sha256(raw).hexdigest()
    tree=digest(json.dumps(files,sort_keys=True,separators=(',',':')).encode());assert tree==snapshot['source_tree_sha256']==stage['source_tree_sha256']and snapshot['review_source_archive']==stage['review_source_archive']
    with zipfile.ZipFile(checked(root,stage['review_source_archive']))as z:
        names=z.namelist();assert len(names)==len(set(names))and set(names)==set(files)|{'CANDIDATE-SOURCE-IDENTITY.json'}
        assert json.loads(z.read('CANDIDATE-SOURCE-IDENTITY.json'))==dict(source_tree_sha256=tree,source_files=files)
        for name,value in files.items():assert digest(z.read(name))==value,name
    oldsource=json.loads(checked(root,graph['stages'][-2]['source_snapshot']).read_text())
    assert {n:d for n,d in files.items()if n.startswith('functional-tests/')}=={n:d for n,d in oldsource['source_files'].items()if n.startswith('functional-tests/')}
    selected=lambda d:{n:v for n,v in d.items()if n.startswith(PRODUCTION)or n in ROOT_BUILD}
    old,new=selected(oldsource['source_files']),selected(files);changed={n for n in set(old)|set(new)if old.get(n)!=new.get(n)}
    expected={'compatibility/versions/1.21.1/fabric/src/main/java/com/salts_inventory_update/compat/nativewindows/mixin/BountifulAnalyzerRefreshMixin.java','compatibility/versions/1.21.1/common/src/main/resources/salts_inventory_compat.reviewed-native.mixins.json'}
    assert changed==expected=={v['file']for v in edge['production_sources']}
    for v in edge['production_sources']:assert old.get(v['file'])==v['before']and new[v['file']]==v['after']
    inspector=checked(root,edge['reviewed_inspector_python']);canonical=Path(__file__).with_name('focused_release_evidence_analyzer_delta.py');assert inspector.read_bytes()==canonical.read_bytes()
    work=folder/'475-613';work.mkdir();fresh=work/'analyzer_delta.py';shutil.copy2(inspector,fresh)
    checked(root,edge['original_compiled_receipt']);pairs=[];rows={(r['target'],r['family']):r for r in stage['artifacts']};before={(r['target'],r['family']):r for r in graph['stages'][-2]['artifacts']}
    candidate={(v['minecraft']+'/'+v['loader'],v['family']):(Path(v['file']).as_posix(),v['sha256'])for v in snapshot['artifacts']};assert len(rows)==len(stage['artifacts'])==len(candidate)==36
    for key,left in before.items():
        right=rows[key];assert candidate[key]==(right['distribution'],right['sha256'])and right['preserved']['sha256']==right['sha256']
        a,b=checked(root,left['preserved']),checked(root,right['preserved']);log=None
        if left['sha256']==right['sha256']:assert a.read_bytes()==b.read_bytes()
        else:
            assert key[0]in ['1.21.1/fabric','1.21.1/neoforge']and key[1]=='compatibility';loader=key[0].split('/')[1]
            result=inspect(a,b,edge['reviewed_class_sha256'][loader]);p=work/(loader+'.json');p.write_text(json.dumps(result,indent=2,sort_keys=True)+'\n');log=reference(root,p)
        pairs.append(dict(target=key[0],family=key[1],before=left['preserved'],after=right['preserved'],scope='byte-identical'if log is None else'exact-original-reviewed-inspector',log=log))
    last=dict(before='475',after='613',mode='analyzer-refresh',exact_production_sources=edge['production_sources'],original_compiled_receipt=edge['original_compiled_receipt'],fresh_inspector_source=reference(root,fresh),original_tool=dict(language='python',reviewed_class_sha256=edge['reviewed_class_sha256']),rows=pairs)
    return dict(status='eleven-source-archives-ten-fresh-structural-edges-verified-no-native-retention',preparation=PREPARATION,
        stages=prior['stages']+[dict(stage='613',source_snapshot=stage['source_snapshot'],source_archive=stage['review_source_archive'],source_file_count=len(files),canonical_functional_QA_count=sum(n.startswith('functional-tests/')for n in files),artifacts=stage['artifacts'])],edges=prior['edges']+[last],limits=prior['limits']+' Edge475to613adds only the two source-pinned/reviewed compiled analyzer classes and exact client registrations; every pre-existing member and34complete archives are byte-identical. Original failed611is excluded; native qualification remains separate.')
