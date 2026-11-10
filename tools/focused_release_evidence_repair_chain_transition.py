"""Produce and freshly revalidate the explicit repair-chain certificate. Original execution scope, assertions and latest-attempt validation remain separate."""
from pathlib import Path
import json,sys,uuid
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from focused_release_evidence import checked,reference
from focused_release_evidence_core_log_transition import production_unchanged
from focused_release_evidence_core_log_consumer import Consumer
from focused_release_evidence_repair_chain_consumer import RepairConsumer
from focused_release_evidence_repair_chain_structure import structure,PREPARATION
from focused_release_evidence_repair_chain_legacy_requester_qualifiers import qualify

SCHEMA='seventeen-exact-reviewed-production-repair-edges-239-to1280-v1'
STATUS='explicit-repair-chain-verified-no-new-execution-or-coverage-assigned'

def derive(root,folder,current_identity,base):
    root=Path(root).resolve();folder=Path(folder).resolve()
    assert folder.is_relative_to(root)and not folder.exists();folder.mkdir(parents=True)
    graph=json.loads(checked(root,PREPARATION).read_text())
    endpoint=next(v for v in graph['stages']if v['stage']=='1280')
    compiled=json.loads(checked(root,endpoint['source_snapshot']).read_text())
    current=json.loads(checked(root,current_identity['source_snapshot']).read_text())
    if base.certificate:
        final=json.loads(checked(root,base.certificate).read_text())
        before=json.loads(checked(root,final['before']['source_snapshot']).read_text())
        production_unchanged(compiled,before)
        # Base was independently verified by the existing final Core/REI verifier.
        for target in {v['target']for v in endpoint['artifacts']}:
            wanted={v['family']:(Path(v['distribution']).name,v['sha256'])for v in endpoint['artifacts']if v['target']==target}
            now={v['family']:(Path(v['file']).name,v['sha256'])for v in current['artifacts']if v['minecraft']+'/'+v['loader']==target}
            assert base.prior_subjects(target,now)==wanted,'Final correction does not start from exact1280 endpoint'
    else:production_unchanged(compiled,current)
    structural=structure(root,folder/'fresh-structure')
    # Specific native qualifiers retain their own original repair-stage subjects.
    native_consumer=RepairConsumer(root,Consumer(root),graph,PREPARATION,None)
    native=qualify(root,graph,native_consumer)
    return dict(schema=SCHEMA,status=STATUS,preparation=PREPARATION,
        compiled_endpoint_source=endpoint['source_snapshot'],accepted_native_stages=['239','250','261','279','314','381','431','475','613','726','1019','1063','1159','1253','1266','1280'],
        structural_intermediates_without_native_acceptance=['303','344','718','1013'],excluded_failed_patch_stages=['336','373','611'],
        structural=structural,specific_native_qualifiers=native,
        original_to_current_source_binding=dict(compiled_source=endpoint['source_snapshot'],current_source=current_identity['source_snapshot'],
            final_correction=base.certificate),
        retention_rule='A complete recorded subject tuple or complete artifact scope must match one approved stage. Original executions, source/runtime archives, assertions, timestamps, failures and native publisher pins remain unchanged. Each report still validates original completion, actual native scope, QA and latest-attempt precedence.',
        limits='Only the seventeen exact approved repair edges and their named native qualifiers. No additional feature, execution, full-suite pass or release approval is granted. New-method inspector allowances remain source-reviewed/native-qualified rather than independently exact-body-proven. The existing final Core/book/REI correction requires its separate strict verifier and completed native queues.')

def equivalent_reinspection(root,old,new):
    root=Path(root).resolve()
    for key in ['schema','status','preparation','compiled_endpoint_source','accepted_native_stages',
                'structural_intermediates_without_native_acceptance','excluded_failed_patch_stages','specific_native_qualifiers','retention_rule','limits']:
        assert old[key]==new[key],'Changed repair certificate field: '+key
    a,b=old['structural'],new['structural']
    assert a['status']==b['status']and a['preparation']==b['preparation']and a['stages']==b['stages']and a['limits']==b['limits']
    assert len(a['edges'])==len(b['edges'])==17
    for prior,fresh in zip(a['edges'],b['edges']):
        for key in ['before','after','mode','exact_production_sources','original_compiled_receipt','original_tool']:
            assert prior[key]==fresh[key],('Changed structural edge',key)
        for key in ['fresh_inspector_source']+([] if prior['mode']in ['analyzer-refresh','gallery-source-selection'] else ['fresh_inspector_binary']):
            assert checked(root,prior[key]).read_bytes()==checked(root,fresh[key]).read_bytes()
        assert len(prior['rows'])==len(fresh['rows'])==36
        for x,y in zip(prior['rows'],fresh['rows']):
            assert {k:v for k,v in x.items()if k!='log'}=={k:v for k,v in y.items()if k!='log'}
            if x['log']is None:assert y['log']is None
            else:assert checked(root,x['log']).read_bytes()==checked(root,y['log']).read_bytes()
    return True

def attach(root,generation,current_identity,base,certificate_path=None):
    root=Path(root).resolve()
    if generation not in ['build14','build15']:return base
    if certificate_path is None:
        pointer=root/'output/release-readiness/repair-prefix-current.json'
        if not pointer.exists():return base
        active=json.loads(pointer.read_text());assert active['status']=='explicit-repair-chain-active-no-new-coverage'
        path=checked(root,active['certificate'])
    else:path=Path(certificate_path)
    assert path.is_file()
    certificate=reference(root,path);original=json.loads(checked(root,certificate).read_text())
    assert original['schema']==SCHEMA and original['status']==STATUS
    expected_auditors={p.relative_to(root).as_posix()for p in (root/'tools').glob('focused_release_evidence_repair_chain*.py')}|{'tools/focused_release_evidence_core_log_consumer.py','tools/reconcile-release-repair-chain.py'}
    assert {v['file']for v in original['auditor_inputs']}==expected_auditors
    for ref in original['auditor_inputs']:checked(root,ref)
    folder=root/'build/release-readiness'/('repair-chain-consumer-'+uuid.uuid4().hex)
    actual=derive(root,folder,current_identity,base)
    equivalent_reinspection(root,original,actual)
    # The historical issue source is immutable. It may precede report-only edits.
    original_source=json.loads(checked(root,original['original_to_current_source_binding']['current_source']).read_text())
    compiled=json.loads(checked(root,original['compiled_endpoint_source']).read_text())
    if original['original_to_current_source_binding']['final_correction']is None:
        production_unchanged(compiled,original_source)
    verification=folder/'verification.json'
    verification.write_text(json.dumps(dict(status='repair-chain-consumer-freshly-revalidated-no-new-coverage',
        original_certificate=certificate,reinspection=actual),indent=2)+'\n')
    graph=json.loads(checked(root,PREPARATION).read_text())
    return RepairConsumer(root,base,graph,certificate,reference(root,verification))
