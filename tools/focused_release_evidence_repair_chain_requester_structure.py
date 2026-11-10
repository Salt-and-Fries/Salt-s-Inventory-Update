"""Reinspect the thirteen prior edges and the exact two-method Requester repair."""
from pathlib import Path
import json
from focused_release_evidence import checked,reference
from focused_release_evidence_repair_chain_ae2_structure import structure as prior_structure
from focused_release_evidence_repair_chain_gallery_structure import snapshot
from focused_release_evidence_repair_chain_ae2_delta import compile_inspector,inspect
PREPARATION={'file': 'output/release-readiness/Requester-repair-prefix-fifteen-stage-inputs-1174-01/manifest.json', 'sha256': '25a576b67d2925f4584c37d0bb839338697d83ca6766b5bb0589e30c9960a101'}


def structure(root,folder):
    root=Path(root).resolve();folder=Path(folder).resolve();assert folder.is_relative_to(root)and not folder.exists();folder.mkdir(parents=True)
    graph=json.loads(checked(root,PREPARATION).read_text());previous=json.loads(checked(root,graph['previous_preparation']).read_text())
    assert len(graph['stages'])==15 and len(graph['edges'])==14 and graph['stages'][:-1]==previous['stages']and graph['edges'][:-1]==previous['edges']
    prior=prior_structure(root,folder/'prior-thirteen-edges');edge=graph['edges'][-1]
    assert(edge['before'],edge['after'],edge['inspector_mode'],edge['inspector_class'])==('1063','1159','requester-native-addon-payload-ownership','RequesterPayloadDelta')
    old=graph['stages'][-2];stage=graph['stages'][-1];before=snapshot(root,old);source=snapshot(root,stage)
    a={k.replace('\\','/'):v for k,v in before['source_files'].items()};b={k.replace('\\','/'):v for k,v in source['source_files'].items()}
    changed={k for k in set(a)|set(b)if a.get(k)!=b.get(k)}
    expected={'compatibility/ae2/versions/'+mc+'/neoforge/src/main/java/com/salts_inventory_update/compat/ae2/nativeview/AEWindow.java'for mc in ['1.21.1','26.1.2']}
    assert changed==expected=={v['file']for v in edge['production_sources']}
    for row in edge['production_sources']:assert a[row['file']]==row['before']and b[row['file']]==row['after']
    proof=json.loads(checked(root,edge['original_compiled_receipt']).read_text());checked(root,edge['native_qualification']);checked(root,edge['native_failure']);checked(root,edge['negative_native_guards'])
    assert proof['status']=='two-exact-addon-enqueue-archive-deltas-confirmed-native-tests-pending'and len(proof['records'])==2 and len(proof['archive_pairs'])==36
    assert edge['java_sources']==[proof['source'],proof['shared_source']]and edge['libraries']==proof['libraries']
    assert edge['negative_guards']==proof['negative_guards']and len(edge['negative_guards'])==3
    for guard in edge['negative_guards']:assert guard['refused']is True;checked(root,guard['log'])
    work=folder/'1063-1159';work.mkdir();tool=compile_inspector(root,work/'inspector',edge)
    left={(v['target'],v['family']):v for v in old['artifacts']};right={(v['target'],v['family']):v for v in stage['artifacts']};assert len(left)==len(right)==36
    current={(v['minecraft']+'/'+v['loader'],v['family']):(Path(v['file']).as_posix(),v['sha256'])for v in source['artifacts']};pairs=[];count=0
    for key,l in left.items():
        r=right[key];assert current[key]==(r['distribution'],r['sha256']);first,last=checked(root,l['preserved']),checked(root,r['preserved']);log=None
        assert l['preserved']['sha256']==l['sha256']and r['preserved']['sha256']==r['sha256']
        if l['sha256']==r['sha256']:assert first.read_bytes()==last.read_bytes()
        else:
            assert key[1]=='compatibility'and key[0]in{'1.21.1/neoforge','26.1.2/neoforge'}
            match=next(v for v in proof['records']if v['target']==key[0]);assert match['before']['sha256']==l['sha256']and match['after']['sha256']==r['sha256']
            log=inspect(root,tool,edge,first,last,work/(key[0].replace('/','-')+'.log'));count+=1
        pairs.append(dict(target=key[0],family=key[1],before=l['preserved'],after=r['preserved'],scope='byte-identical'if log is None else'exact-original-reviewed-inspector',log=log))
    assert count==2
    receipt=dict(before='1063',after='1159',mode=edge['inspector_mode'],exact_production_sources=edge['production_sources'],original_compiled_receipt=edge['original_compiled_receipt'],fresh_inspector_source=tool['source'],fresh_inspector_binary=tool['binary'],original_tool=dict(language='source-pinned-Java-ASM',sources=edge['java_sources'],libraries=edge['libraries'],tested_queue_classes=[]),rows=pairs)
    added=dict(stage=stage['stage'],source_snapshot=stage['source_snapshot'],source_archive=stage['review_source_archive'],source_file_count=len(b),canonical_functional_QA_count=sum(k.startswith('functional-tests/')for k in b),artifacts=stage['artifacts'])
    return dict(status='fifteen-source-archives-fourteen-fresh-structural-edges-verified-no-native-retention',preparation=PREPARATION,stages=prior['stages']+[added],edges=prior['edges']+[receipt],limits=prior['limits']+' Requester edge changes exactly two addon sender bodies; every other method/resource and34whole archives remain exact. Native rapid-input qualifiers are separate and mandatory.')
