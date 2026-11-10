"""Merge explicit case reproductions without treating them as a full cohort."""
from copy import deepcopy
import re

def identity(label):
    value=re.sub(r'^(?:(?:atlas|registry|machine|item)-)+','',label).replace('_','-')
    return {'ae2-me-chest':'ae2-chest','tradingpost-trading-post':'trading-post',
        'crabbersdelight-crab-trap':'crabbers-delight-crab-trap',
        'toms-storage-inventory-configurator':'toms-config','toms-storage-item-filter':'toms-item-filter',
        'toms-storage-tag-item-filter':'toms-tag-filter','toms-tag-item-filter':'toms-tag-filter'}.get(value,value)

def merge(base,reproduction,requested,proof):
    result=deepcopy(base);requested=set(requested);requested_keys={identity(name) for name in requested};used=set();resolutions=[]
    for index,old in enumerate(result['cases']):
        # A selected registry/short name can reproduce an earlier atlas name.
        # Use the same existing identity rule for selection and measurement;
        # otherwise the measured alias is found but its old case is skipped.
        if identity(old['id']) not in requested_keys:continue
        new=next((r for r in reproduction['cases'] if r['id']==old['id']),None)
        if new is None:new=next((r for r in reproduction['cases'] if identity(r['id'])==identity(old['id'])),None)
        rejection=next((r for r in reproduction.get('rejected_reviewed_workflows',[]) if identity(r['id'])==identity(old['id'])),None)
        if new is None and rejection is None:continue
        previous={'id':old['id'],'status':old['status'],'error':old.get('error'),'reproduction_report':proof}
        result['checks']=[c for c in result['checks'] if c['case']!=old['id']]
        if new is not None and new['id'] not in used:
            used.add(new['id']);updated=deepcopy(new);updated['id']=old['id'];updated['reproduction_case_id']=new['id']
            updated['previous_attempt']=previous;result['cases'][index]=updated
            result['checks'].extend({**c,'case':old['id']} for c in reproduction['checks'] if c['case']==new['id'])
        else:
            result['cases'][index]={**old,'status':'invalid-fixture-workflow' if new is None else 'superseded-duplicate-entry',
                'previous_attempt':previous,'rejection':rejection,'coverage':'Not counted as a functional pass.'}
        resolutions.append({**previous,'latest_status':result['cases'][index]['status']})
    known={identity(c['id']) for c in result['cases']}
    for name in sorted(requested):
        if identity(name) in known:continue
        new=next((r for r in reproduction['cases'] if identity(r['id'])==identity(name)),None)
        if new is None:continue
        known.add(identity(name));updated=deepcopy(new);updated['id']=name;updated['reproduction_case_id']=new['id'];updated['reproduction_report']=proof
        result['cases'].append(updated)
        result['checks'].extend({**c,'case':name} for c in reproduction['checks'] if c['case']==new['id'])
        resolutions.append({'id':name,'status':'previously-uncovered','latest_status':new['status'],'reproduction_report':proof})
    result.setdefault('reconciliations',[]).extend(resolutions)
    return result
