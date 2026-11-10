"""Select only the four separately executed, sealed missing-publisher workflows."""
from pathlib import Path
import json
from focused_release_evidence import checked
def additional(root,family,target):
    assert family in{'stock','processing','crafting','red-merchant'}
    if target!='1.20.1/fabric':return []
    root=Path(root);path=root/'output/release-readiness/remaining-publisher-native-pins-retained-pair-1507-01/manifest.json'
    data=json.loads(path.read_text());assert data['status']=='passed-four-actual-publisher-checks-reconciliation-pending'
    assert len(data['jobs'])==len(data['runs'])==4
    assert {v['family']for v in data['jobs']}=={'stock','processing','crafting','red-merchant'}
    assert all(v['target']==target and v['status']=='passed'and v['exit_code']==0 and v['seal_exit_code']==0 for v in data['runs'])
    for row in data['runs']:checked(root,row['sealed'])
    found=[v for v in data['runs']if v['family']==family];assert len(found)==1
    return found
