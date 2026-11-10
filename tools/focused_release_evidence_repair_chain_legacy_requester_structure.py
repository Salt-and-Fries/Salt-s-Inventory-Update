"""Preserve fifteen prior edges and freshly inspect the two exact legacy loader repairs."""
from pathlib import Path
import json
from focused_release_evidence import checked,reference
from focused_release_evidence_repair_chain_layout_structure import structure as prior_structure
from focused_release_evidence_repair_chain_gallery_structure import snapshot
from focused_release_evidence_repair_chain_ae2_delta import compile_inspector,inspect
PREPARATION={'file':'output/release-readiness/Requester-loader-chain-preparation-1283-01/eighteen-stage-inputs.json','sha256':'caedc1ce40251006092435db8088ace97ae9e1335a4156fccea864ba24a3c1b9'}

def structure(root,folder):
    root=Path(root).resolve();folder=Path(folder).resolve();assert folder.is_relative_to(root)and not folder.exists();folder.mkdir(parents=True)
    graph=json.loads(checked(root,PREPARATION).read_text());previous=json.loads(checked(root,graph['previous_preparation']).read_text())
    assert len(graph['stages'])==18 and len(graph['edges'])==17 and graph['stages'][:-2]==previous['stages']and graph['edges'][:-2]==previous['edges']
    prior=prior_structure(root,folder/'prior-fifteen-edges')
    specs=[('1253','1266','legacy-Requester-native-sender-allowlist','RequesterLegacyScopeDelta_1266',{'1.20.1/fabric','1.20.1/forge'},1),('1266','1280','Forge-concrete-native-sender-bridge','ForgeRequesterBridgeDelta_1277',{'1.20.1/forge'},4)]
    stages=[];receipts=[]
    for offset,(first,last,mode,inspector,targets,source_count)in enumerate(specs):
        edge=graph['edges'][-2+offset];old=graph['stages'][-3+offset];stage=graph['stages'][-2+offset]
        assert(edge['before'],edge['after'],edge['inspector_mode'],edge['inspector_class'])==(first,last,mode,inspector)
        before=snapshot(root,old);after=snapshot(root,stage)
        a={k.replace('\\','/'):v for k,v in before['source_files'].items()};b={k.replace('\\','/'):v for k,v in after['source_files'].items()}
        changes={k for k in set(a)|set(b)if a.get(k)!=b.get(k)}
        sources={v['file'].replace('\\','/')for v in edge['production_sources']};assert changes==sources and len(sources)==source_count
        for v in edge['production_sources']:assert a.get(v['file'].replace('\\','/'))==v['before']and b[v['file'].replace('\\','/')]==v['after']
        proof=json.loads(checked(root,edge['original_compiled_receipt']).read_text());checked(root,edge['native_qualification'])
        assert proof['status']==('candidate-activated-two-exact-legacy-sender-guards-native-tests-pending'if offset==0 else'candidate-activated-one-exact-Forge-concrete-native-sender-bridge-native-tests-pending')
        assert edge['java_sources']==proof['java_sources']and edge['libraries']==proof['libraries']and len(proof['archive_pairs'])==36
        work=folder/(first+'-'+last);work.mkdir();tool=compile_inspector(root,work/'inspector',edge)
        left={(v['target'],v['family']):v for v in old['artifacts']};right={(v['target'],v['family']):v for v in stage['artifacts']};assert len(left)==len(right)==36
        actual={(v['minecraft']+'/'+v['loader'],v['family']):(Path(v['file']).as_posix(),v['sha256'])for v in after['artifacts']};pairs=[];changed=set()
        for key,l in left.items():
            r=right[key];assert actual[key]==(r['distribution'],r['sha256']);start,end=checked(root,l['preserved']),checked(root,r['preserved']);log=None
            assert l['preserved']['sha256']==l['sha256']and r['preserved']['sha256']==r['sha256']
            original=next(v for v in proof['archive_pairs']if(v['target'],v['family'])==key);assert original['before']['sha256']==l['sha256']and original['after']['sha256']==r['sha256']
            if l['sha256']==r['sha256']:assert start.read_bytes()==end.read_bytes()
            else:
                assert key[1]=='compatibility'and key[0]in targets;changed.add(key[0]);checked(root,original['log'])
                log=inspect(root,tool,edge,start,end,work/(key[0].replace('/','-')+'.log'))
            pairs.append(dict(target=key[0],family=key[1],before=l['preserved'],after=r['preserved'],scope='byte-identical'if log is None else'exact-original-reviewed-inspector',log=log))
        assert changed==targets
        receipts.append(dict(before=first,after=last,mode=mode,exact_production_sources=edge['production_sources'],original_compiled_receipt=edge['original_compiled_receipt'],fresh_inspector_source=tool['source'],fresh_inspector_binary=tool['binary'],original_tool=dict(language='source-pinned-Java-ASM',sources=edge['java_sources'],libraries=edge['libraries'],tested_queue_classes=[]),rows=pairs))
        stages.append(dict(stage=last,source_snapshot=stage['source_snapshot'],source_archive=stage['review_source_archive'],source_file_count=len(b),canonical_functional_QA_count=sum(k.startswith('functional-tests/')for k in b),artifacts=stage['artifacts']))
    return dict(status='eighteen-source-archives-seventeen-fresh-structural-edges-verified-no-native-retention',preparation=PREPARATION,stages=prior['stages']+stages,edges=prior['edges']+receipts,limits=prior['limits']+' Legacy Requester adds only the exact native class-prefix guard and a Forge-only identical sender bridge/config/metadata declaration. Actual Fabric32checks at1266 and Forge32checks at1280 qualify their final loader paths separately; no intermediate Forge pass is claimed.')
