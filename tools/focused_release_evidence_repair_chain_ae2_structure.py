"""Extend the exact prior eleven structural edges with the two scoped AE2 repairs."""
from pathlib import Path
import json
from focused_release_evidence import checked,reference
from focused_release_evidence_repair_chain_gallery_structure import structure as prior_structure,snapshot
from focused_release_evidence_repair_chain_ae2_delta import compile_inspector,inspect
PREPARATION={'file': 'output/release-readiness/AE2-repair-prefix-fourteen-stage-inputs-1074-01/manifest.json', 'sha256': '3f245897d25087c6487fbc9a1c154a9d90e949fb542ca5ce2e1517121cff64ae'}


def structure(root,folder):
    root=Path(root).resolve();folder=Path(folder).resolve();assert folder.is_relative_to(root)and not folder.exists();folder.mkdir(parents=True)
    graph=json.loads(checked(root,PREPARATION).read_text());previous=json.loads(checked(root,graph['previous_preparation']).read_text())
    assert len(graph['stages'])==14 and len(graph['edges'])==13 and graph['stages'][:-2]==previous['stages']and graph['edges'][:-2]==previous['edges']
    prior=prior_structure(root,folder/'prior-eleven-edges');last=[];stages=[]
    for number,edge in enumerate(graph['edges'][-2:],12):
        expected=('726','1019','ae2-native-payload-ownership')if number==12 else('1019','1063','ae2-native-pending-close')
        assert (edge['before'],edge['after'],edge['inspector_mode'])==expected
        old=next(v for v in graph['stages']if v['stage']==edge['before']);stage=next(v for v in graph['stages']if v['stage']==edge['after'])
        before_source=snapshot(root,old);source=snapshot(root,stage)
        a={n.replace('\\','/'):v for n,v in before_source['source_files'].items()};b={n.replace('\\','/'):v for n,v in source['source_files'].items()}
        changed={n for n in set(a)|set(b)if a.get(n)!=b.get(n)};expected_sources=set()
        for mc in ['1.21.1','26.1.2','26.2','26.3']:
            prefix='compatibility/ae2/versions/'+mc+'/neoforge/src/main/java/com/salts_inventory_update/compat/ae2/nativeview/'
            expected_sources.add(prefix+'AEWindow.java')
            if number==12:expected_sources.add(prefix+'NativePayloadQueue.java')
        assert changed==expected_sources=={v['file']for v in edge['production_sources']}
        for v in edge['production_sources']:assert a.get(v['file'])==v['before']and b[v['file']]==v['after']
        proof=json.loads(checked(root,edge['original_compiled_receipt']).read_text());checked(root,edge['native_qualification'])
        assert len(proof['records'])==4 and len(proof['archive_pairs'])==36
        assert edge['negative_guards']==proof['negative_guards']and len(edge['negative_guards'])==(6 if number==12 else 3)
        for v in edge['negative_guards']:assert v['refused']is True;checked(root,v['log'])
        compiled_sources=[proof['source']]+([]if number==12 else[proof['shared_source']]);assert edge['java_sources']==compiled_sources and edge['libraries']==proof['libraries']
        if number==12:assert edge['tested_queue_classes']==proof['tested_queue_classes']
        work=folder/(edge['before']+'-'+edge['after']);work.mkdir();tool=compile_inspector(root,work/'inspector',edge)
        left={(v['target'],v['family']):v for v in old['artifacts']};right={(v['target'],v['family']):v for v in stage['artifacts']};assert len(left)==len(right)==36
        source_artifacts={(v['minecraft']+'/'+v['loader'],v['family']):(Path(v['file']).as_posix(),v['sha256'])for v in source['artifacts']};pairs=[]
        for key,l in left.items():
            r=right[key];assert source_artifacts[key]==(r['distribution'],r['sha256']);first,last_path=checked(root,l['preserved']),checked(root,r['preserved']);log=None
            assert l['preserved']['sha256']==l['sha256']and r['preserved']['sha256']==r['sha256']
            if l['sha256']==r['sha256']:assert first.read_bytes()==last_path.read_bytes()
            else:
                assert key[1]=='compatibility'and key[0]in{'1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge'}
                original=next(v for v in proof['records']if v['target']==key[0]);assert original['before']['sha256']==l['sha256']and original['after']['sha256']==r['sha256']
                log=inspect(root,tool,edge,first,last_path,work/(key[0].replace('/','-')+'.log'))
            pairs.append(dict(target=key[0],family=key[1],before=l['preserved'],after=r['preserved'],scope='byte-identical'if log is None else'exact-original-reviewed-inspector',log=log))
        last.append(dict(before=edge['before'],after=edge['after'],mode=edge['inspector_mode'],exact_production_sources=edge['production_sources'],original_compiled_receipt=edge['original_compiled_receipt'],fresh_inspector_source=tool['source'],fresh_inspector_binary=tool['binary'],original_tool=dict(language='source-pinned-Java-ASM',sources=edge['java_sources'],libraries=edge['libraries'],tested_queue_classes=edge.get('tested_queue_classes',[])),rows=pairs))
        stages.append(dict(stage=stage['stage'],source_snapshot=stage['source_snapshot'],source_archive=stage['review_source_archive'],source_file_count=len(b),canonical_functional_QA_count=sum(n.startswith('functional-tests/')for n in b),artifacts=stage['artifacts']))
    return dict(status='fourteen-source-archives-thirteen-fresh-structural-edges-verified-no-native-retention',preparation=PREPARATION,stages=prior['stages']+stages,edges=prior['edges']+last,limits=prior['limits']+' Two new AE2edges retain exact scoped nativequeue/options bodies, unchanged other methods/resources/fields, and32whole archives per edge. Actual native settings and close/invalidation qualifiers remain separate and mandatory.')
