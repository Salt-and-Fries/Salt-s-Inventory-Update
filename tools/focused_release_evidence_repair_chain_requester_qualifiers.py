"""Preserve prior native qualifiers and rederive the two actual owned Requester bursts."""
from pathlib import Path
import json
from focused_release_evidence import checked,reference
from focused_release_evidence_repair_chain_ae2_qualifiers import qualify as prior_qualify
from focused_release_evidence_repair_chain_requester_native import derive


def qualify(root,graph,consumer):
    root=Path(root).resolve();previous=json.loads(checked(root,graph['previous_preparation']).read_text());prior=prior_qualify(root,previous,consumer)
    edge=graph['edges'][-1];assert(edge['before'],edge['after'])==('1063','1159')
    path=checked(root,edge['native_qualification']);data=json.loads(path.read_text());assert data['status']=='actual-native-Requester-bursts-strictly-rederived'and(data['checks'],data['full_states'])==(24,32)
    assert {v['target']for v in data['runs']}=={'1.21.1/neoforge','26.1.2/neoforge'}
    guards=json.loads(checked(root,edge['negative_native_guards']).read_text());assert guards['status']=='all-eight-invalid-native-Requester-state-sets-rejected'and len(guards['guards'])==8 and all(v['rejected']is True for v in guards['guards'])
    failure=checked(root,edge['native_failure']);bad=json.loads(failure.read_text());frame=json.loads(checked(root,bad['actual_dropped_update']).read_text());assert(frame['requester']['requests'][0]['amount'],frame['requester']['requests'][0]['batch'])==(37,1)
    raw=json.loads(checked(root,bad['actual_burst']).read_text());assert[e['text']for e in raw['events']if e['type']=='text']==['37','8']
    rows=[]
    for record in data['runs']:
        expected=consumer._stage_subjects('1159',record['target'],{'core','compatibility'});actual=derive(root,checked(root,record['seal']),expected);assert actual==record
        rows.append(dict(target=record['target'],seal=record['seal'],checks=12,states=16))
    prior['receipts'].append(dict(kind='Requester-native-addon-owned-input-acceptance-and-persistence',campaign=reference(root,path),runs=rows,native_failure=reference(root,failure),negative_native_guards=edge['negative_native_guards'],scope='Two actual12check native Requester workflows: real37/8and41/9pointer/key/text bursts, owned pending lane, unavailable USER_CLOSE while pending, full typed ghost and physical resources, moved input and ordinary persistence. Only two exact addon sender method bodies changed; original32check terminal/production workflows remain separate.'))
    return prior
