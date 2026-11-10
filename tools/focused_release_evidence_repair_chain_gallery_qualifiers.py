"""Retain all prior native qualifiers and require both exact Gallery target workflows."""
from pathlib import Path
import json
from focused_release_evidence import checked,reference
from focused_release_evidence_repair_chain_analyzer_qualifiers import qualify as prior_qualify
from focused_release_evidence_gallery import derive,MENU

def qualify(root,graph,consumer):
    root=Path(root).resolve();previous=json.loads(checked(root,graph['previous_preparation']).read_text());prior=prior_qualify(root,previous,consumer)
    campaign=root/'output/release-readiness/gallery-native-qualified-731-01/manifest.json';data=json.loads(campaign.read_text());assert data['status']=='both-native-Gallery-workflows-passed-and-rederived'and(data['checks'],data['states'])==(36,30)
    assert len(data['runs'])==2 and len(data['negative_guards'])==20 and all(v['refused']for v in data['negative_guards'])
    names={'1.20.1/forge':('readiness-gallery-1201-forge-04','bf-gallery-painting-selector'),'1.21.1/neoforge':('readiness-gallery-1211-neoforge-01','atlas-bf-gallery-painting-selector')};receipts=[]
    assert {v['target']for v in data['runs']}==set(names)
    for run in data['runs']:
        name,case=names[run['target']];assert run['status']=='passed'and run['run']==name
        expected=consumer._stage_subjects('726',run['target'],{'core','compatibility'})
        native=derive(root,checked(root,run['archival']),expected,case,[MENU]);assert native==json.loads(checked(root,run['native_validation']).read_text())and native['validation']['assertions']==18 and native['validation']['states']==15
        receipts.append(dict(summary=run['summary'],seal=run['archival'],native_validation=run['native_validation'],checks=18,states=15))
    prior['receipts'].append(dict(kind='Gallery-held-source-and-window-addressed-native-selection',campaign=reference(root,campaign),runs=receipts,compiled_boundary=data['compiled_boundary'],source_binding_boundary=data['source_binding_boundary'],scope='Both actual original native Gallery target workflows. Complete held-source and unrelated typed ownership, real native search/scroll/moved selection and exact close/reopen variant. No painting-placement/resource-pack/localization or complete-suite claim.'))
    return prior
