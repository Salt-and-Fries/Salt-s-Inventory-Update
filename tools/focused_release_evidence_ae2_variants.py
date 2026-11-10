"""Keep distinct real publisher executions inside one native UI disposition."""
from pathlib import Path
import json


def additional_jobs(root,family,target):
    assert family in{'settings','quantum','network','pattern','cpu-controls'}
    if target!='1.20.1/fabric':return []
    path=Path(root)/'build/release-readiness/legacy-ae2-publisher-variants-plan-01.json'
    plan=json.loads(path.read_text());assert plan['sealed']is True and len(plan['jobs'])==5
    assert {v['family']for v in plan['jobs']}=={'settings','quantum','network','pattern','cpu-controls'}
    assert all(v['target']=='1.20.1/fabric'and v['publisher']=='appliedenergistics2-fabric-15.4.11.jar'for v in plan['jobs'])
    found=[v for v in plan['jobs']if v['family']==family];assert len(found)==1
    return found


SUBJECT_POLICY='independent-original-tuples-through-verified-consumer-v1'


def expected_subjects(record,workflow,expected,consumer=None,current=None):
    """A mixed aggregate never supplies an unverified substitute subject tuple."""
    if 'subject_binding_policy'not in record:return expected
    assert record['subject_binding_policy']==SUBJECT_POLICY
    assert consumer is not None and current is not None
    assert json.loads(json.dumps(expected))==record['subjects']
    return consumer.expected(record['target'],workflow,current)


def combine(workflows,consumer=None,current=None):
    assert workflows and len({v['sealed_evidence']['file']for v in workflows})==len(workflows),'Each publisher requires a distinct real execution'
    first=workflows[0]
    mixed=any(value['subjects']!=first['subjects']for value in workflows)
    if mixed:
        assert consumer is not None and current is not None,'Distinct original artifact tuples require independently verified retention'
        for value in workflows:
            assert json.loads(json.dumps(consumer.expected(value['target'],value,current)))==value['subjects']
    for workflow in workflows:
        assert not workflow.get('additional_workflows'),'Nested aggregates do not establish native executions'
        assert 'subject_binding_policy'not in workflow,'Expected original native workflow, not a prebound aggregate'
        assert all(workflow[k]==first[k]for k in['schema','target','case','disposition','original_native_classes','native_classes','scope','limits'])
    result=dict(first)
    if mixed:result['subject_binding_policy']=SUBJECT_POLICY
    if len(workflows)>1:
        publishers={}
        for workflow in workflows:
            for publisher in workflow['publishers']:
                digest=publisher['sha512']
                if digest in publishers:assert publishers[digest]==publisher
                publishers[digest]=publisher
        result['publishers']=[publishers[k]for k in sorted(publishers)]
        result['additional_workflows']=workflows[1:]
    return result
