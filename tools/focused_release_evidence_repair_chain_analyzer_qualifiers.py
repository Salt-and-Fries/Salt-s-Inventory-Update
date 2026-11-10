"""Retain prior native qualifiers and require both original Analyzer workflows."""
from pathlib import Path
import json
from focused_release_evidence import checked,reference
from focused_release_evidence_repair_chain_mi_qualifiers import qualify as prior_qualify
from focused_release_evidence_analyzer import derive,CASE,SCREEN

def qualify(root,graph,consumer):
    root=Path(root).resolve();previous=json.loads(checked(root,graph['previous_preparation']).read_text());prior=prior_qualify(root,previous,consumer)
    campaign=root/'output/release-readiness/analyzer-refresh-native-620-01/manifest.json';data=json.loads(campaign.read_text());assert data['status']=='both-original-ten-check-analyzer-workflows-passed-and-rederived'and data['checks']==data['states']==20
    assert len(data['runs'])==2 and {r['target']for r in data['runs']}=={'1.21.1/fabric','1.21.1/neoforge'}
    expected_names={'1.21.1/fabric':'readiness-analyzer-1211-fabric-07','1.21.1/neoforge':'readiness-analyzer-1211-neoforge-01'};receipts=[]
    for run in data['runs']:
        assert run['status']=='passed'and run['run']==expected_names[run['target']];summary=json.loads(checked(root,run['summary']).read_text());assert summary['status']=='passed'and summary['results'][0]['exit_code']==0
        expected=consumer._stage_subjects('613',run['target'],{'core','compatibility'})
        native=derive(root,checked(root,run['archival']),expected,CASE,[SCREEN]);assert native['validation']['assertions']==native['validation']['states']==10 and native['validation']['native_slider_release_observed']is True
        assert native==json.loads(checked(root,run['native_validation']).read_text());receipts.append(dict(summary=run['summary'],seal=run['archival'],native_validation=run['native_validation'],checks=10,states=10))
    baseline=json.loads(checked(root,data['native_baseline']).read_text());assert baseline['status']=='native-refresh-confirmed-SIU-refresh-regression'and len(baseline['checks'])==6 and all(v['passed']is True for v in baseline['checks'])
    prior['receipts'].append(dict(kind='Bountiful-hosted-decree-refresh',campaign=reference(root,campaign),runs=receipts,native_baseline=data['native_baseline'],compiled_boundary=data['compiled_boundary'],scope='Both original ten-check workflows, exact native pool definitions and typed item conservation; native slider release observed. No bounty-generation or all-decree-distribution claim.'))
    return prior
