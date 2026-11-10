"""Retain original scoped repair qualifiers and require both native MI feature replays."""
from pathlib import Path
import json
from focused_release_evidence import checked,reference,sha,verify
from focused_release_evidence_core_log_consumer import Consumer,subjects
from focused_release_evidence_repair_chain_minecells_consumer import RepairConsumer as PriorConsumer
from focused_release_evidence_repair_chain_minecells_qualifiers import qualify as prior_qualify

def qualify(root,graph,consumer):
    root=Path(root).resolve();ready=root/'output/release-readiness'
    previous=json.loads(checked(root,graph['previous_preparation']).read_text())
    prior=prior_qualify(root,previous,PriorConsumer(root,Consumer(root),previous,graph['previous_preparation'],None))
    campaign=ready/'MI-sidebar-native-476-01/manifest.json';data=json.loads(campaign.read_text())
    assert data['status']=='both-supported-MI-native-workflows-passed-explicit-retention-pending'
    assert len(data['runs'])==2 and {r['target']for r in data['runs']}=={'1.20.1/fabric','1.21.1/neoforge'}
    expected_groups=['shape','tank','item-pipe','fluid-pipe','compact machine slots, lock and auto-eject','GUI scale and parent/tray movement with native input','native energy icon empty and charged']
    receipts=[]
    for run in data['runs']:
        assert run['status']=='passed'and run['exit_code']==run['seal_exit_code']==0
        path=checked(root,run['summary']);summary=json.loads(path.read_text())
        assert summary['status']=='passed'and summary['target']==run['target']and len(summary['results'])==1
        expected=consumer._stage_subjects('475',run['target'],{'core','compatibility'})
        assert subjects(summary['subjects'],expected)==expected
        row=summary['results'][0];assert row['status']=='passed'and row['exit_code']==0
        assert row['results']['cases']==1 and row['results']['checks']==7 and row['results']['mi_native_features_checked']
        seal=checked(root,run['archival']);verify(root,json.loads(seal.read_text()),expected)
        journal_path=checked(root,summary['acceptance']);journal=json.loads(journal_path.read_text())
        assert journal['complete']is True and [r['check']for r in journal['actions']]==expected_groups
        assert all(r['status']=='pass'for r in journal['actions'])and all(r['passed']is True for r in journal['checks'])
        receipts.append(dict(summary=reference(root,path),seal=reference(root,seal),checks=7,scope=summary['scope']))
    old=root/'build/modern-industrialization-implementation/1.20.1-fabric-siu-readiness-mi-1201-fabric-02-6/macerator-moved-client.json'
    new=root/'build/modern-industrialization-implementation/1.20.1-fabric-siu-readiness-mi-1201-fabric-03-6/macerator-moved-client.json'
    def tray(path):
        state=json.loads(path.read_text());w=state['allWindows'][0]
        values=[r['source']['width']for r in w['template']['regions']if r['id']=='native-shape-tray']
        assert len(values)<=1
        return values[0]if values else None
    expanded=new.with_name('shape-expanded-client.json')
    assert tray(old)==240 and tray(new)is None and tray(expanded)==89
    negative=ready/'MI-sidebar-inspector-negative-477-01/manifest.json';neg=json.loads(negative.read_text())
    assert neg['status']=='twenty-eight-incorrect-or-unrelated-compiled-mutations-refused'and len(neg['checks'])==28
    for case in neg['checks']:assert case['refused']is True and case['exit_code']!=0 and sha(root/case['log'])==case['log_sha256']
    prior['receipts'].append(dict(kind='MI-foreign-global-sidebar-layout-resolution',campaign=reference(root,campaign),runs=receipts,
        original_oversized_tray=reference(root,old),corrected_native_tray=reference(root,new),retained_expanded_tank_tray=reference(root,expanded),negative_inspector=reference(root,negative),
        scope='Both original seven-group native MI workflows only; exact foreign FTB sidebar exclusion, no FTB functionality or every MI recipe claim.'))
    return prior
