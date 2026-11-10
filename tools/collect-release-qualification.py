"""Current candidate dashboard; never promote opening-only or partial attempts."""
from pathlib import Path
import argparse, collections, csv, datetime, hashlib, importlib.util, json, re, sys

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'output/release-readiness'
sys.path.insert(0, str(ROOT/'functional-tests/project-everything-regression'))
from subject_artifacts import current_subjects, subjects_match
from release_candidate_identity import current as current_candidate
from focused_release_evidence import verify as verify_focused_evidence, checked
from focused_release_evidence_publisher_final_core import retained_for_campaign
from focused_release_evidence_session_registration import verified as verified_core_log_consumer
from focused_release_evidence_preserved_qa import verify_inputs as verify_measured_qa_inputs
from focused_release_evidence_toms_layout import verify_attempt as verify_toms_derived_attempt
from focused_release_evidence_controller_frames import KIND as CONTROLLER_FRAME_KIND, verify_attempt as verify_controller_frame_attempt
from focused_release_evidence_ae2_drive_gutter import KIND as AE2_DRIVE_KIND, NATIVE_KIND as AE2_ATLAS_NATIVE_KIND, verify_attempt as verify_ae2_atlas_attempt
from focused_release_evidence_ae2_modern_drive import KIND as AE2_MODERN_DRIVE_KIND, verify_attempt as verify_ae2_modern_drive_attempt
from focused_release_evidence_runtime_scope import phase_binding


def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def read(path): return json.loads(path.read_text())
def ref(path): return dict(file=str(path.relative_to(ROOT)), sha256=sha(path))


def archival_context(root,path,evidence):
    """Keep the two named sealing sidecars separate from native executions."""
    if not path.name.endswith('-retained-sealing-context.json'):
        assert evidence['status']=='completed-scoped-evidence-sealed','Unknown focused evidence schema'
        return None
    allowed={'focused-evidence-readiness-powah-modern-1211-neoforge-06-retained-sealing-context.json',
             'focused-evidence-readiness-powah-modern-2612-neoforge-01-retained-sealing-context.json'}
    assert path.name in allowed and evidence['status']=='original-run-sealed-with-explicit-final16-binding'
    for key in ['native_summary','seal','original_source_archive','collector']:
        checked(root,evidence[key])
    seal=read(checked(root,evidence['seal']))
    assert seal['status']=='completed-scoped-evidence-sealed'and seal['summary']==evidence['native_summary']
    assert path.name=='focused-evidence-'+seal['run']+'-retained-sealing-context.json'
    assert seal['subjects']==evidence['artifact_binding']['original_subjects']
    checked(root,evidence['artifact_binding']['artifact_equivalence'])
    for key in ['native_original_candidate','archival_candidate']:
        candidate=evidence[key];source=read(checked(root,candidate['source_snapshot']))
        checked(root,candidate['source_archive'])
        assert source['source_tree_sha256']==candidate['source_tree_sha256']
    return dict(evidence=ref(path),seal=evidence['seal'],scope=evidence['scope'],counts_as_native_execution=False)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--generation',default='build13')
    args=parser.parse_args();assert re.fullmatch(r'build[0-9]+',args.generation)
    candidate=current_candidate(ROOT,verify_worktree=True)
    consumer=verified_core_log_consumer(ROOT,args.generation,candidate)
    integrity = OUT/'candidate-integrity.json'
    artifacts = read(integrity)['artifacts']
    assert read(integrity)['status'] == 'passed' and len(artifacts) == 36
    for row in artifacts: assert sha(ROOT/row['file']) == row['sha256']
    junit = OUT/f'junit-{args.generation}-01/summary.json'
    units = read(junit)
    assert units['status'] == 'passed'
    for project in units['projects']:
        for file in project['xml_files']: assert sha(ROOT/file['file']) == file['sha256']
    for row in units['artifacts']: assert sha(ROOT/row['file']) == row['sha256']

    path = ROOT/'tools/run-full-validation.py'
    spec = importlib.util.spec_from_file_location('validation_plan', path)
    full = importlib.util.module_from_spec(spec); spec.loader.exec_module(full)
    phases = []
    for name in full.PHASES:
        result = ROOT/'build/full-validation-20261023/readiness-final-20261106'/name/'results.json'
        plan = full.plan(name, 'readiness-final-20261106')
        rows = read(result)['cases'] if result.exists() else []
        assert {v['key'] for v in rows}.issubset(v['key'] for v in plan)
        accepted = [];artifact_bindings={}
        for case in rows:
            if case['status'] != 'PASS': continue
            last = case['attempts'][-1]
            artifact_bindings[case['key']]=phase_binding(case,last,consumer.fingerprints(case['artifacts'],full.fingerprints(case)))
            assert last['exit_code'] == 0 and not last.get('input_changed_during_profile')
            if consumer.certificate and args.generation == 'build16':
                from focused_release_evidence_full_phase_history import verify_phase
                artifact_bindings[case['key']]['qa_source_history']=verify_phase(ROOT,case,last,full)
            else:
                if case.get('qa_derivation',{}).get('kind') == CONTROLLER_FRAME_KIND:
                    assert name == 'controller'
                    verify_controller_frame_attempt(ROOT,last,case,full.verify_qa_attempt)
                elif case.get('qa_derivation',{}).get('kind') == AE2_MODERN_DRIVE_KIND:
                    assert name == 'ae2-atlas'
                    verify_ae2_modern_drive_attempt(ROOT,last,case,full.verify_qa_attempt)
                elif case.get('qa_derivation',{}).get('kind') in {AE2_DRIVE_KIND, AE2_ATLAS_NATIVE_KIND}:
                    assert name == 'ae2-atlas'
                    verify_ae2_atlas_attempt(ROOT,last,case,full.verify_qa_attempt)
                else:
                    assert last['qa_inputs'] == full.qa_fingerprints(case), ('Changed QA inputs', name, case['key'])
                    verify_toms_derived_attempt(ROOT,last,case,full.verify_qa_attempt)
            assert (ROOT/last['log']).exists()
            accepted.append(case['key'])
        phases.append(dict(phase=name, planned=len(plan), accepted=len(accepted),
            artifact_bindings=artifact_bindings,
            running=[v['key'] for v in rows if v['status'] == 'RUNNING'],
            failed=[v['key'] for v in rows if v['status'] == 'FAIL'],
            evidence=ref(result) if result.exists() else None))

    publisher = OUT/'readiness-final-publishers-01/publishers.json'
    campaign = read(publisher)
    accepted_jobs, cases, checks, preserved = [], [], 0, []
    historical_publisher_jobs=[]
    for job in campaign['plan']:
        attempts = [v for v in campaign['attempts'] if v['job'] == job['id']]
        if not attempts: continue
        preserved.extend(dict(job=job['id'], run=v['run'], status=v['status'], log=v['log']) for v in attempts)
        attempt = attempts[-1]
        if attempt['status'] != 'passed': continue
        assert attempt['exit_code'] == 0 and attempt['qa_inputs_unchanged'] and attempt['subjects_unchanged']
        binding=consumer.bind(job['target'],attempt['subjects'],current_subjects(job['target']))
        if binding is None:
            historical_publisher_jobs.append(dict(job=job['id'],run=attempt['run'],reason='Different production bytes; fresh qualification pending',subjects=attempt['subjects']))
            continue
        expected_subjects=binding['original_subjects']
        binding_metadata={'artifact_binding':binding}if binding['artifact_equivalence']else{}
        verify_measured_qa_inputs(ROOT, attempt['qa_inputs'])
        accepted_reports = []
        for result in attempt['results']:
            assert result['status'] == 'passed' and result['exit_code'] == 0
            profile = ROOT/result['profile']; report = profile/'mod-interaction-checks.json'
            staged = profile/'staged-artifacts.json'
            assert subjects_match(read(staged)['artifacts'],expected_subjects)
            data = read(report)
            assert data['cases'] and all(v['passed'] for v in data['checks'])
            assert not any(v['status'] in ['running','failed','needs-observer-reproduction'] for v in data['cases'])
            checks += len(data['checks'])
            for case in data['cases']:
                cases.append(dict(target=job['target'], fixture=job['original_fixture'],
                    publisher_job=job['id'], execution_kind=binding['execution_kind'], evidence=str(report.relative_to(ROOT)),
                    report_sha256=sha(report), **binding_metadata,**case))
            accepted_reports.append(ref(report))
        assert accepted_reports
        accepted_jobs.append(dict(job=job['id'], run=attempt['run'], execution_kind=binding['execution_kind'], reports=accepted_reports,**binding_metadata))

    retained_jobs=retained_for_campaign(ROOT,campaign,
        lambda target:consumer.prior_subjects(target,current_subjects(target)),
        artifact_locator=consumer.artifact_path if consumer.certificate else None)
    for retained in retained_jobs:
        assert retained['job']not in {v['job']for v in accepted_jobs},'Fresh and retained execution counted twice'
        job=next(v for v in campaign['plan']if v['id']==retained['job'])
        report=ROOT/retained['report']['file'];assert sha(report)==retained['report']['sha256']
        data=read(report);checks+=len(data['checks'])
        chain=consumer.chained_retention(retained,current_subjects(job['target']))
        kind=chain['execution_kind']if chain else retained['execution_kind']
        binding_metadata={'artifact_binding':chain}if chain else{}
        for case in data['cases']:
            cases.append(dict(target=job['target'],fixture=job['original_fixture'],publisher_job=job['id'],
                evidence=str(report.relative_to(ROOT)),report_sha256=sha(report),execution_kind=kind,
                original_run=retained['original_run'],original_subjects=retained['original_subjects'],
                retained_evidence=retained['retained_evidence'],equivalence=retained['equivalence'],**binding_metadata,**case))
        accepted_jobs.append(dict(job=job['id'],run=retained['original_run'],execution_kind=kind,
            reports=[retained['report']],original_subjects=retained['original_subjects'],retained_evidence=retained['retained_evidence'],**binding_metadata))
    retained_ids={v['job']for v in retained_jobs}
    historical_publisher_jobs=[v for v in historical_publisher_jobs if v['job']not in retained_ids]

    backlog = OUT/'coverage-backlog.json'; gaps = read(backlog)
    assert gaps['candidate_identity']==candidate,'Refresh the coverage backlog for this exact activated source first'
    for row in gaps['records']:
        if row['disposition']not in ['functional-pass','native-only-qualified','read-only-status-pass']:continue
        assert row['current_acceptance']and row['current_exact_pins'],'Accepted case lacks native evidence or pins'
        for acceptance in row['current_acceptance']:
            assert sha(ROOT/acceptance['report'])==acceptance['report_sha256']
            if acceptance.get('reviewed_evidence'):
                assert sha(ROOT/acceptance['reviewed_evidence'])==acceptance['reviewed_evidence_sha256']
    selector_proof=consumer.coverage_path(OUT,'coverage-native-selectors',args.generation,'02')
    if selector_proof.exists():
        from native_selector_evidence import verify as verify_selector
        selectors=read(selector_proof);assert selectors['status']=='reviewed-native-selector-acceptances-reconciled'
        for row in selectors['records']:verify_selector(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])))
    analyzer_proof=consumer.coverage_path(OUT,'coverage-analyzer',args.generation,'01')
    if analyzer_proof.exists():
        from focused_release_evidence_analyzer import CASE as ANALYZER_CASE,TARGETS as ANALYZER_TARGETS,verify as verify_analyzer
        proof=read(analyzer_proof);assert proof['status']=='reviewed-native-analyzer-feature-workflows-reconciled'
        assert len(proof['records'])==2 and {(v['target'],v['case'])for v in proof['records']}=={(target,ANALYZER_CASE)for target in ANALYZER_TARGETS}
        for row in proof['records']:verify_analyzer(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])))
    nifty_proof=consumer.coverage_path(OUT,'coverage-nifty',args.generation,'01')
    if nifty_proof.exists():
        from focused_release_evidence_nifty import CASES as NIFTY_CASES,TARGETS as NIFTY_TARGETS,verify as verify_nifty
        proof=read(nifty_proof);assert proof['status']=='reviewed-native-nifty-feature-workflows-reconciled'
        assert proof['unique_executions']==3 and len(proof['records'])==6
        assert {(v['target'],v['case'])for v in proof['records']}=={(target,case)for target in NIFTY_TARGETS for case in NIFTY_CASES}
        for row in proof['records']:verify_nifty(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])))
    guard_proof=consumer.coverage_path(OUT,'coverage-guard',args.generation,'01')
    if guard_proof.exists():
        from focused_release_evidence_guard import TARGETS as GUARD_TARGETS,CASE as GUARD_CASE,verify as verify_guard
        proof=read(guard_proof);assert proof['status']=='reviewed-native-guard-feature-workflows-reconciled'
        assert len(proof['records'])==9 and {(v['target'],v['case'])for v in proof['records']}=={(target,GUARD_CASE)for target in GUARD_TARGETS}
        for row in proof['records']:verify_guard(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])))
    pet_proof=consumer.coverage_path(OUT,'coverage-pet-cargo',args.generation,'01')
    if pet_proof.exists():
        from focused_release_evidence_pet_cargo import PAIRS as PET_PAIRS,verify as verify_pet
        proof=read(pet_proof);assert proof['status']=='reviewed-native-pet-cargo-workflows-reconciled'
        assert len(proof['records'])==5 and {(v['target'],v['case'])for v in proof['records']}==PET_PAIRS
        for row in proof['records']:verify_pet(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])))
    sensor_proof=consumer.coverage_path(OUT,'coverage-sensors',args.generation,'01')
    if sensor_proof.exists():
        from sensor_feature_evidence import verify as verify_sensor
        sensors=read(sensor_proof);assert sensors['status']=='reviewed-native-sensor-feature-workflows-reconciled'
        for row in sensors['records']:verify_sensor(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])))
    mi_energy_proof=consumer.coverage_path(OUT,'coverage-mi-energy',args.generation,'01')
    if mi_energy_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_mi_energy_status',ROOT/'tools/reconcile-mi-energy-release-evidence.py')
        energy=importlib.util.module_from_spec(spec);spec.loader.exec_module(energy)
        proof=read(mi_energy_proof);assert proof['status']=='reviewed-native-mi-energy-status-workflows-reconciled'
        assert len(proof['records'])==20 and {(v['target'],v['case'])for v in proof['records']}=={(target,energy.PREFIX+hatch)for target in energy.TARGETS for hatch in energy.HATCHES}
        for row in proof['records']:energy.verify(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])))
    mi_storage_proof=consumer.coverage_path(OUT,'coverage-mi-storage',args.generation,'01')
    if mi_storage_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_mi_storage',ROOT/'tools/reconcile-mi-storage-release-evidence.py')
        storage=importlib.util.module_from_spec(spec);spec.loader.exec_module(storage)
        proof=read(mi_storage_proof);assert proof['status']=='reviewed-native-mi-storage-workflows-reconciled'
        assert len(proof['records'])==34 and {(v['target'],v['case'])for v in proof['records']}=={(target,case)for target in storage.TARGETS for case in storage.CASES}
        for row in proof['records']:storage.verify(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])))
    mi_infra_proof=consumer.coverage_path(OUT,'coverage-mi-infrastructure',args.generation,'01')
    if mi_infra_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_mi_infrastructure',ROOT/'tools/reconcile-mi-infrastructure-release-evidence.py')
        infra=importlib.util.module_from_spec(spec);spec.loader.exec_module(infra)
        proof=read(mi_infra_proof);assert proof['status']=='reviewed-native-mi-infrastructure-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,case)for target in infra.TARGETS for case in infra.CASES}
        for row in proof['records']:infra.verify(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])))
    mi_multi_proof=consumer.coverage_path(OUT,'coverage-mi-multiblock',args.generation,'01')
    if mi_multi_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_mi_multiblocks',ROOT/'tools/reconcile-mi-multiblock-release-evidence.py')
        multi=importlib.util.module_from_spec(spec);spec.loader.exec_module(multi)
        proof=read(mi_multi_proof);assert proof['status']=='reviewed-native-mi-multiblock-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,case)for target in multi.TARGETS for case in multi.CASES}
        for row in proof['records']:multi.verify(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])))
    ae2_settings_proof=consumer.coverage_path(OUT,'coverage-ae2-settings',args.generation,'01')
    if ae2_settings_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_ae2_settings',ROOT/'tools/reconcile-ae2-settings-release-evidence.py')
        settings=importlib.util.module_from_spec(spec);spec.loader.exec_module(settings)
        proof=read(ae2_settings_proof);assert proof['status']=='reviewed-native-ae2-settings-workflows-reconciled'
        assert len(proof['records'])==13 and len({(v['target'],v['case'])for v in proof['records']})==13
        assert all(sum(v['case']==case for v in proof['records'])==(1 if case=='ae2-priority'else 6)for case in settings.ALIASES)
        for row in proof['records']:settings.verify(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])),consumer=consumer,current=current_subjects(row['target']))
    ae2_quantum_proof=consumer.coverage_path(OUT,'coverage-ae2-quantum',args.generation,'01')
    if ae2_quantum_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_ae2_quantum',ROOT/'tools/reconcile-ae2-quantum-release-evidence.py')
        quantum=importlib.util.module_from_spec(spec);spec.loader.exec_module(quantum)
        proof=read(ae2_quantum_proof);assert proof['status']=='reviewed-native-ae2-quantum-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,quantum.CASE)for target in quantum.TARGETS}
        for row in proof['records']:quantum.verify(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])),consumer=consumer,current=current_subjects(row['target']))
    ae2_network_proof=consumer.coverage_path(OUT,'coverage-ae2-network',args.generation,'01')
    if ae2_network_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_ae2_network',ROOT/'tools/reconcile-ae2-network-release-evidence.py')
        network=importlib.util.module_from_spec(spec);spec.loader.exec_module(network)
        proof=read(ae2_network_proof);assert proof['status']=='reviewed-native-ae2-network-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,network.CASE)for target in network.TARGETS}
        for row in proof['records']:network.verify(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])),consumer=consumer,current=current_subjects(row['target']))
    ae2_stock_proof=consumer.coverage_path(OUT,'coverage-ae2-stock',args.generation,'02')
    if ae2_stock_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_ae2_stock',ROOT/'tools/reconcile-ae2-stock-release-evidence.py')
        stock=importlib.util.module_from_spec(spec);spec.loader.exec_module(stock)
        proof=read(ae2_stock_proof);assert proof['status']=='reviewed-native-ae2-stock-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,stock.CASE)for target in stock.TARGETS}
        for row in proof['records']:stock.verify(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])),consumer=consumer,current=current_subjects(row['target']))
    ae2_processing_proof=consumer.coverage_path(OUT,'coverage-ae2-processing',args.generation,'02')
    if ae2_processing_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_ae2_processing',ROOT/'tools/reconcile-ae2-processing-release-evidence.py')
        processing=importlib.util.module_from_spec(spec);spec.loader.exec_module(processing)
        proof=read(ae2_processing_proof);assert proof['status']=='reviewed-native-ae2-processing-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,processing.CASE)for target in processing.TARGETS}
        for row in proof['records']:processing.verify(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])),consumer=consumer,current=current_subjects(row['target']))
    ae2_pattern_proof=consumer.coverage_path(OUT,'coverage-ae2-pattern',args.generation,'01')
    if ae2_pattern_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_ae2_pattern',ROOT/'tools/reconcile-ae2-pattern-release-evidence.py')
        pattern=importlib.util.module_from_spec(spec);spec.loader.exec_module(pattern)
        proof=read(ae2_pattern_proof);assert proof['status']=='reviewed-native-ae2-pattern-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,pattern.CASE)for target in pattern.TARGETS}
        for row in proof['records']:pattern.verify(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])),consumer=consumer,current=current_subjects(row['target']))
    ae2_crafting_proof=consumer.coverage_path(OUT,'coverage-ae2-crafting',args.generation,'02')
    if ae2_crafting_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_ae2_crafting',ROOT/'tools/reconcile-ae2-crafting-release-evidence.py')
        crafting=importlib.util.module_from_spec(spec);spec.loader.exec_module(crafting)
        proof=read(ae2_crafting_proof);assert proof['status']=='reviewed-native-ae2-crafting-workflows-reconciled'
        assert proof['unique_executions']==7 and len(proof['records'])==18
        assert {(v['target'],v['case'])for v in proof['records']}=={(target,case)for target in crafting.TARGETS for case in crafting.ALIASES}
        for row in proof['records']:crafting.verify(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])),consumer=consumer,current=current_subjects(row['target']))
    status_proof=consumer.coverage_path(OUT,'coverage-techreborn-status',args.generation,'01')
    if status_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_techreborn_status',ROOT/'tools/reconcile-techreborn-status-release-evidence.py')
        status=importlib.util.module_from_spec(spec);spec.loader.exec_module(status)
        proof=read(status_proof);assert proof['status']=='reviewed-native-techreborn-status-workflows-reconciled'
        for row in proof['records']:status.verify(ROOT,row,consumer.expected(row['target'],row,current_subjects(row['target'])),consumer=consumer,current=current_subjects(row['target']))
    ae2_cpu_proof=consumer.coverage_path(OUT,'coverage-ae2-cpu',args.generation,'01')
    if ae2_cpu_proof.exists():
        spec=importlib.util.spec_from_file_location('qualification_ae2_cpu',ROOT/'tools/reconcile-ae2-cpu-release-evidence.py')
        cpu=importlib.util.module_from_spec(spec);spec.loader.exec_module(cpu)
        proof=read(ae2_cpu_proof);assert proof['status']=='reviewed-native-ae2-cpu-workflows-reconciled'
        assert proof['unique_executions']==10 and len(proof['records'])==7
        assert {(v['target'],v['case'])for v in proof['records']}=={*[(t,cpu.CPU_CASE)for t in cpu.TARGETS],('1.21.1/neoforge',cpu.STATUS_ALIAS)}
        for record in proof['records']:cpu.verify(ROOT,record,consumer.expected(record['target'],record,current_subjects(record['target'])))
    focused_queues=[];historical_focused=[]
    for name in ['film-after-matrix-01','specialized-after-film-01','focused-tail-campaign-01','focused-native-campaign-02','post-remote-publisher-variants-01','supplemental-ae2-cpu-controls-01','CPU-exact15412-native-1479-01/manifest','supplemental-ae2-cpu-status-01','legacy-ae2-publisher-variants-01','final-core-diagnostics-01','supplemental-modern-powah-01','final-smallships-paging-01']:
        path=OUT/(name+'.json')
        if not path.exists():continue
        queue=read(path);completed=[]
        for run in queue.get('runs',[]):
            if run['status']!='passed':continue
            path=OUT/run['run']/'summary.json';summary=read(path)
            assert summary['status']=='passed'
            subjects=summary['subjects']
            expected_current=current_subjects(run['target'])
            if name=='final-core-diagnostics-01':expected_current={'core':expected_current['core']}
            binding=consumer.bind(run['target'],subjects,expected_current)
            if binding is None:
                historical_focused.append(dict(run=run['run'],target=run['target'],summary=ref(path),reason='Different production bytes; no current acceptance'))
                continue
            expected_original=binding['original_subjects']
            if name=='final-core-diagnostics-01':
                if args.generation=='build16':
                    assert binding['execution_kind']=='retained-exact-session-registration-and-prior-Core-REI-corrections'
                    assert binding['artifact_equivalence']['file']=='output/release-readiness/session-registration-evidence-build16-01.json'
                else:assert binding['execution_kind']=='fresh-current-artifact','Final corrected logger needs an actual final-Core run'
            from focused_release_evidence_session_registration import verify_scoped_qa_inputs
            retained_qa_inputs=verify_scoped_qa_inputs(ROOT,summary)
            if summary.get('acceptance'):
                assert sha(ROOT/summary['acceptance']['file'])==summary['acceptance']['sha256']
            if name in{'supplemental-ae2-cpu-controls-01','CPU-exact15412-native-1479-01/manifest'}:
                sys.path.insert(0,str(ROOT/'functional-tests/release-specialized'))
                from run_ae2_cpu_controls import verify_run as verify_cpu_controls
                verify_cpu_controls(ROOT,summary,expected_original)
            if name=='final-core-diagnostics-01':
                sys.path.insert(0,str(ROOT/'functional-tests/release-specialized'))
                from run_core_render_diagnostic import verify_run as verify_render_diagnostic
                verify_render_diagnostic(ROOT,summary,expected_original)
            if name=='legacy-ae2-publisher-variants-01'and run['family']=='cpu-controls':
                sys.path.insert(0,str(ROOT/'functional-tests/release-specialized'))
                from run_ae2_cpu_controls_legacy_variant import verify_run as verify_legacy_cpu_controls
                verify_legacy_cpu_controls(ROOT,summary,expected_original)
            completed.append(dict(run=run['run'],target=run['target'],summary=ref(path),artifact_binding=binding,retained_original_qa_inputs=retained_qa_inputs))
        focused_queues.append(dict(queue=name,status=queue['status'],evidence=ref(OUT/(name+'.json')),
            passed_runs=completed,runs=queue.get('runs',[]),
            scope='Named focused launcher results only; per-feature backlog reconciliation remains separate.'))
    native_plan=ROOT/'build/release-readiness/focused-native-plan-02.json'
    native_plan_state=read(native_plan)if native_plan.exists()else None
    extra_plan=ROOT/'build/release-readiness/publisher-extra-plan-01.json'
    sealed_scoped=[];historical_sealed_scoped=[];retained_sealing_contexts=[]
    for path in sorted(OUT.glob('focused-evidence-*.json')):
        evidence=read(path)
        context=archival_context(ROOT,path,evidence)
        if context:
            retained_sealing_contexts.append(context)
            continue
        binding=consumer.bind(evidence['target'],evidence['subjects'],current_subjects(evidence['target']))
        if binding is None:
            historical_sealed_scoped.append(dict(run=evidence['run'],target=evidence['target'],evidence=ref(path),reason='Preserved archive on earlier production bytes; no current qualification assigned'))
            continue
        verify_focused_evidence(ROOT,evidence,binding['original_subjects'])
        sealed_scoped.append(dict(run=evidence['run'],target=evidence['target'],evidence=ref(path),
            scope=evidence['scope'],assertions=evidence['assertions'],coverage_disposition=evidence['coverage_disposition'],artifact_binding=binding))
    original_features=[]
    original_proof=consumer.coverage_path(OUT,'coverage-original-features',args.generation,'01')
    if original_proof.exists():
        from focused_release_evidence_original_features import verify_record as verify_original_feature
        proof=read(original_proof);assert proof['status']=='original-completed-native-features-reconciled'
        assert len(proof['records'])==88 and proof['unique_executions']==30
        for row in proof['records']:
            verify_original_feature(ROOT,row,consumer,current_subjects(row['target']))
            original_features.append(dict(target=row['target'],case=row['case'],disposition=row['disposition'],evidence=ref(original_proof),scope=row['scope'],limits=row['limits']))
    disposition = collections.Counter(v['disposition'] for v in gaps['records'])
    counts = collections.Counter(v['status'] for v in cases)
    record = dict(status='qualification-evidence-collected-not-release-approval', generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        candidate_identity=candidate,
        artifact_equivalence=consumer.report(),
        artifacts=artifacts, integrity=ref(integrity), generation=args.generation,junit=dict(evidence=ref(junit), tests=units['tests'], scope=units['scope']),
        complete_phases=phases, publisher_campaign=ref(publisher), publisher_jobs=accepted_jobs,
        retained_sealing_contexts=retained_sealing_contexts,
        publisher_plan_count=len(campaign['plan']), publisher_cases=cases, publisher_checks=checks,
        publisher_execution_kinds=dict(collections.Counter(v['execution_kind']for v in accepted_jobs)),
        publisher_case_statuses=dict(counts), preserved_attempts=preserved,
        coverage_dispositions=dict(disposition), coverage_backlog=ref(backlog),
        focused_queues=focused_queues,original_completed_native_features=original_features,
        sealed_scoped_evidence=sealed_scoped,historical_sealed_scoped_evidence=historical_sealed_scoped,
        historical_publisher_jobs=historical_publisher_jobs,historical_focused_runs=historical_focused,
        native_tail_plan=dict(evidence=ref(native_plan),planned=len(native_plan_state['jobs']),sealed=native_plan_state['sealed'])if native_plan_state else None,
        additional_publisher_fixtures=dict(evidence=ref(extra_plan),planned=len(read(extra_plan)['jobs']),
            scope='Additional actual external publishers; separate from the original65-job campaign.')if extra_plan.exists()else None,
        limits='Each passing row proves only its recorded assertions. Entry/policy-only, missing native features and partial failed attempts are not functional acceptances. Final code/evidence review and release acceptance are separate; this aggregation does not authorize publication.',
        remote_installation_scope='docs/release/INSTALLATION_ACCEPTANCE.md')
    (OUT/'qualification-current.json').write_text(json.dumps(record, indent=2)+'\n')
    fields = ['target','fixture','publisher_job','execution_kind','original_run','id','status','coverage','evidence','report_sha256']
    with (OUT/'case-results-current.csv').open('w', newline='', encoding='utf-8') as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, extrasaction='ignore'); writer.writeheader(); writer.writerows(cases)
    text = ['# Current release qualification', '',
        '**Collected qualification evidence. Final acceptance and independent review are recorded separately.**', '',
        ('Current source: ['+Path(candidate['source_snapshot']['file']).name+']('+Path(candidate['source_snapshot']['file']).name+'), tree `'+candidate['source_tree_sha256']+'`.'if candidate else 'Current source snapshot has not been activated; the artifact-level results below remain separately scoped.'),'',
        f'Candidate: Core 0.1.6 / Compatibility 0.1.3 / Controlify 0.1.1, normal {args.generation}. '
        'All 36 current artifact hashes are recorded in [the machine-readable report](qualification-current.json).', '',
        f"Archived unit/contract checks: **{units['tests']} passed**, six mapped source families. These are separate from live gameplay.", '',
        '| Complete qualification phase | Qualified fresh/retained cases | Planned |', '| --- | ---: | ---: |']
    for phase in phases: text.append(f"| {phase['phase']} | {phase['accepted']} | {phase['planned']} |")
    text += ['', 'The startup phase is the original twelve-target project-runtime initialization matrix. '
        'Its distribution hashes are identity guards, rather than observations of loaded packaged jars. '
        'Packaged Core gameplay and dedicated-server results retain their separate evidence.', '']
    kind_counts=collections.Counter(v['execution_kind']for v in accepted_jobs)
    text += ['', f"Publisher discovery: **{len(accepted_jobs)}/{len(campaign['plan'])} qualified profiles**: "+', '.join(f'{name}: {count}'for name,count in sorted(kind_counts.items()))+f'; {len(cases)} measured cases and {checks} recorded checks from those original executions.', '',
        'Case statuses: '+', '.join(f'{k}: {v}' for k,v in sorted(counts.items()))+'.', '',
        'Opening/policy-only results do not establish feature acceptance on their own. A generic transfer pass does not certify every specialized setting or recipe. '
        'The [explicit backlog](coverage-backlog.csv) retains the native class, exact pins, prerequisites, required acceptance and evidence for each such gap.', '',
        'Coverage dispositions: '+', '.join(f'{k}: {v}' for k,v in sorted(disposition.items()))+'.', '',
        'Additional focused queues: '+', '.join(f"{v['queue']}: {v['status']} ({len(v['passed_runs'])} clean named runs)"for v in focused_queues)+'. '
        +(f"The native tail currently plans {len(native_plan_state['jobs'])} jobs and is {'sealed'if native_plan_state['sealed']else'unsealed'}. "if native_plan_state else'')+
        'Three additional publisher fixtures cover the external modern AE2WTLib binaries and legacy Forge Powah. '
        'Prepared scenarios and successful source compilation are not live gameplay passes.','',
        f"Sealed scoped source/runtime archives: **{len(sealed_scoped)}**. These preserve existing assertions and do not add gameplay cases or assign backlog coverage.",'',
        f"Earlier artifact evidence remains preserved: {len(historical_publisher_jobs)} publisher jobs and {len(historical_sealed_scoped)} sealed scoped archives await requalification or explicit equivalence review. None is counted as a current pass.",'',
        'See [the current case table](case-results-current.csv), [the checklist](../../RELEASE_READINESS.md), '
        '[installation/multiplayer evidence and runtime limits](../../docs/release/INSTALLATION_ACCEPTANCE.md), '
        '[the preserved historical audit](historical-audit/report.md), and [focused AE2 results](../../docs/markdown/ae2-requester-missed-click-fix.md).', '',
        'Historical failures and unsuccessful native-JVM/setup attempts remain preserved. This dashboard accepts clean exact-subject attempts and separately labeled historical scopes with explicit validated equivalence; no fresh run is invented. '
        'Known upstream combinations are partitioned explicitly, retaining every publisher binary elsewhere in that target. No runtime case limit is applied in the publisher campaign.', '']
    (OUT/'report.md').write_text('\n'.join(text), encoding='utf-8')
    print('Current evidence:', len(accepted_jobs), '/', len(campaign['plan']), 'publisher jobs;', len(cases), 'cases;', checks, 'checks;', dict(disposition))


if __name__ == '__main__': main()
