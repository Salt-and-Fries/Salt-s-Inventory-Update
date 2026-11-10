"""Rederive both actual legacy Requester workflows after their distinct concrete-network repairs."""
from pathlib import Path
import json
from focused_release_evidence import checked,reference
from focused_release_evidence_repair_chain_layout_qualifiers import qualify as prior_qualify
from requester_native_release_evidence import derive

def qualify(root,graph,consumer):
    root=Path(root).resolve();previous=json.loads(checked(root,graph['previous_preparation']).read_text());prior=prior_qualify(root,previous,consumer)
    assert [(v['before'],v['after'])for v in graph['edges'][-2:]]==[('1253','1266'),('1266','1280')]
    path=checked(root,graph['legacy_native_workflows']);data=json.loads(path.read_text())
    assert data['status']=='two-legacy-native-Requester-workflows-qualified-after-loader-specific-scope-repairs'and data['checks']==64
    assert {(v['stage'],v['target'])for v in data['runs']}=={('1266','1.20.1/fabric'),('1280','1.20.1/forge')}
    rows=[]
    for entry in data['runs']:
        qualification=json.loads(checked(root,entry['qualification']).read_text());expected=consumer._stage_subjects(entry['stage'],entry['target'],{'core','compatibility'})
        actual=derive(root,checked(root,qualification['seal']),expected);assert actual==qualification and actual['checks']==32
        rows.append(dict(target=entry['target'],actual_stage=entry['stage'],seal=actual['seal'],checks=32,states=actual['states']))
    prior['receipts'].append(dict(kind='legacy-Requester-scoped-native-inventory-actions-on-both-concrete-loader-transports',campaign=reference(root,path),runs=rows,scope='Actual32checks per loader: complete native ghost/number/Submit/state/search/movement/reopen workflows, real12plank production from3logs and independent full typed player/cell-shell/provider/storage recovery. Fabric runs1266; Forge runs1280. No claim that intermediate1266Forge passed, and no wireless acceptance.'))
    return prior
