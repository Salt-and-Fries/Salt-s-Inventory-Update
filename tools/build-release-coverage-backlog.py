"""One current, explicit backlog; historical opening-only flags never become passes."""
from pathlib import Path
import argparse,collections,csv,hashlib,json,re,sys,zipfile

ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'output/release-readiness'
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from run_mod_matrix import FIXTURES
from reconcile_results import identity
from subject_artifacts import current_subjects,subjects_match
from release_candidate_identity import current as current_candidate
from native_publisher_index import index as publisher_index,matches as publisher_matches
from focused_release_evidence_publisher_final_core import retained_for_campaign
from focused_release_evidence_session_registration import verified as verified_core_log_consumer


def matching_observations(observations,target,classes,case=None):
    """Attach all clean-profile observations without granting feature acceptance."""
    wanted=set(classes)
    return [entry for entry in observations if entry['target']==target
        and(case is None or identity(entry['case']['id'])==identity(case))
        and wanted.intersection(entry['case'].get('native_menus',[])+entry['case'].get('native_screens',[]))]


def latest_clean_publisher_attempts(campaign):
    """A later failed/running replay supersedes the older clean profile."""
    jobs=[row['id']for row in campaign['plan']]
    assert len(jobs)==len(set(jobs)), 'Duplicate publisher job'
    latest={}
    for attempt in campaign['attempts']:
        assert attempt['job']in jobs, 'Attempt outside the frozen publisher plan'
        latest[attempt['job']]=attempt
    return [latest[job]for job in jobs if job in latest and latest[job]['status']=='passed']


def current_discovery_gaps(records, observations):
    """Keep every unqualified discovery result visible, including fixture gaps."""
    known={}
    for record in records:
        known.setdefault((record['target'],identity(record['case'])),set()).update(record['native_classes'])
    result=[];seen=set()
    for entry in observations:
        case=entry['case']
        if case['status']=='passed':continue
        classes=set(case.get('native_menus',[])+case.get('native_screens',[]))
        key=(entry['target'],identity(case['id']))
        if key in known and known[key].intersection(classes):continue
        signature=(*key,tuple(sorted(classes)))
        if signature not in seen:result.append(entry);seen.add(signature)
    return result


def discovery_class_evidence(case):
    """Locate fixture pins without turning a declared class into an observation."""
    observed=sorted(set(case.get('native_menus',[])+case.get('native_screens',[])))
    declared=[]
    for key in ['expected_native_menu','expected_native_screen']:
        value=case.get(key)
        if value is None:continue
        assert isinstance(value,str)and re.fullmatch(r'[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)+',value),'Invalid declared native class in '+case['id']
        declared.append(value)
    declared=sorted(set(declared))
    return dict(observed=observed,declared=declared,pin_classes=observed or declared,
        pin_basis='observed-native-class'if observed else'declared-entry-contract-not-observed'if declared else'no-native-class-observed-or-declared')


def acceptance(case,menus):
    text=(case+' '+' '.join(menus)).lower()
    if 'briefcase' in text:return 'Capture a real world bee; all native care controls, exact resource use/refusal, release/recapture and persistence.', 'bumblezone_checks.py'
    if 'blockgenerator' in text:return 'Observe real generation, withdraw native output, preserve the withdrawn stack, refuse foreign inputs and close/reopen. Do not mistake continuing production for loss/duplication.', 'kibe_checks.py'
    if 'toms' in text or 'storagemod' in text or 'com.tom.trading' in text:return 'Use a real connected network or owner-configured vending offer; native search/configuration/purchase, authoritative server effect, refusal and persistence.', 'toms_release_workflows.py'
    if 'smither' in text or 'grinder' in text or 'reforge' in text:return 'Open the real second LibZ tab from smithing/grindstone/anvil; use publisher-accepted equipment, gem/hammer/base, action prerequisites, exact recovery and return to the first tab.', 'gear_inventory_cases.py; focused-smitherz-1211.py'
    if 'guard' in text:return 'Ordinary Hero-of-the-Village permitted entity use; native equipment transfers/refusal, source close and independent inventory.', 'entity_inventory_cases.py'
    if 'nifty' in text:return 'Ordinary crouched cart use; actual native hoe/seed cells, quantities, metadata, refusal and close/reopen.', 'nifty_inventory_cases.py'
    if 'blue-skies-arcs' in text or 'blue_skies' in text and 'arcs' in text:return 'Use the actual Character Arcs attachment and original publisher request to open its original native screen/menu. Check all four real player-capability cells, same backing owner, full item data, wrong/duplicate-item refusal, moved input, reopening and recovery. This is not a noninventory selector or a fabricated equipment surface.', 'release-specialized/run_blue_arcs.py'
    if 'portable-cell-workbench' in text or 'portablecellworkbench' in text:return 'Use the original handheld MEGA workbench item; do not require a nonexistent block. Check real cell/upgrade ownership inside the source item, full typed ghost partitions, original toolbar controls, source protection, moved input and recovery. Also qualify the separate conditional compression-card control with an actual bulk cell, single native partition and both mouse directions/wraps; do not infer bulk storage throughput.', 'release-specialized/run_mega_workbench.py; release-specialized/run_mega_bulk.py'
    if 'ae2-terminal-settings' in text or 'terminalsettingsscreen' in text:return 'Open the original local Terminal Settings child from a real terminal. Observe actual native client-config booleans and applicable checkbox/radio callbacks, browser availability, moved input, original Back and reopening while preserving all owned items. Do not mistake local preferences for server settings or browser-search effects.', 'release-specialized/run_ae2_preferences.py'
    if 'merequester' in text:
        if 'wireless' in text:return 'Establish the exact optional AE2WTLib dependency and native wireless host. Verify real shared Requester controls/backing manager and applicable physical slots/source protections, or an evidence-backed absent-in-this-cohort disposition. A block/terminal workflow alone is not wireless feature acceptance.', 'release-specialized/run_requester.py (block/terminal only); dependency-complete wireless publisher/click evidence requires separate review'
        return 'Use original Requester block/network-terminal widgets and real server manager. Check exact typed ghost keys, amounts/batches/state/Submit/search, moved input, persistence, native Missing and genuine recipe production with independent physical/buffer conservation.', 'release-specialized/run_requester.py'
    if any(v in text for v in ['accessories','curios','aetheraccess']):return 'Use the approved public-API equipment surface. Check actual equipment/cosmetic ownership, launcher placement, exclusive switching, lifecycle and focused controller paths; native Screen construction is not required for an approved replacement.', 'equipment_checks.py; run_boundary_regressions.py'
    if 'create' in text or 'fuze' in text:return 'Replay the corresponding native Create feature group: real ghost/configuration/action controls and server/item persistence, with required stock/blueprint/schedule prerequisites. Compare the native layout and populated states.', 'create-window-overhaul/suite.py'
    if 'industrialization' in text:return 'Use the native machine subtype: actual item/fluid/energy/shape prerequisites as applicable, output/consumption or read-only status observation, controls, persistence and pop-out/slot padding.', 'tools/exercise-modern-industrialization.py'
    if 'techreborn' in text and any(v in text for v in ['elevator','launchpad','player_detector','basic_solar_panel']):return 'Use the original zero-item machine panel: exact native radius/launch selector arrows and clamping, moved input and persistence; observe real solar generation/obstruction or elevator energy synchronization, preserve full physical items and close the original source. Teleportation/launching/automation are separate gameplay scope.', 'release-specialized/run_techreborn_status.py'
    if any(v in text for v in ['appeng.','merequester','ae2-']):return 'Enter the native AE2 parent/child workflow using a powered network/valid cell or crafting job where required. Verify real settings, repository/recipe effects, cancellation, cursor ownership and return; use the native vintage/modern layout.', 'run-full-validation.py --phase ae2-atlas; ae2_repository_checks.py'
    if 'powah' in text:return 'Use the native clicked face/channel, bound card or charged energy item as applicable; exercise face/redstone/channel controls and authoritative persistence, or observe actual live status on a display-only panel.', 'release-specialized/run_powah_legacy.py (1.20.1); release-specialized/run_powah_modern.py (1.21.1/26.1.2 NeoForge); powah-windows/focused.py (other panels)'
    if 'exposure' in text:return 'Rename a real native film through text focus and confirm; verify item-name persistence, cancel/no-change and source lifetime.', 'original native atlas actions plus named film-renaming scenario'
    if 'mowzie' in text or 'sculptor' in text or 'umvuth' in text:return 'Ordinary entity interaction; pay the actual native offer or test prerequisite, verify refusal/consumption/result and source lifetime. Challenge gameplay is a separate scope.', 'all-target-rollout/focused-mowzie-*.py'
    if 'prominent' in text:return 'Use the original forge/selection/shop entry and native material/payment prerequisites; verify action, quantity/progression, refusal and persistence.', 'prominent-forge/check.py; prominent-shop/check.py'
    return 'Establish the publisher-native prerequisites from the retained original entry; test the actual mutable controls/transfers/output or explicitly prove a read-only/native-screen contract. Opening/policy checks alone are insufficient.', 'retained native capture and corresponding focused feature driver'

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--generation',default='build13')
    args=parser.parse_args();assert re.fullmatch(r'build[0-9]+',args.generation)
    candidate=current_candidate(ROOT,verify_worktree=True)
    consumer=verified_core_log_consumer(ROOT,args.generation,candidate)
    artifacts=collections.defaultdict(list);contents={}
    for target,fixtures in FIXTURES.items():
        for fixture in fixtures:
            path=ROOT/'build/all-target-rollout/fixtures'/fixture/'fixture.json'
            for row in json.loads(path.read_text())['artifacts']:artifacts[target].append((fixture,row))
    def pins(target,classes):
        found=[];seen=set()
        if not classes:return found
        for fixture,row in artifacts[target]:
            path=(ROOT/row['path']).resolve()
            loader=target.split('/')[1];cache_key=(path,loader)
            if cache_key not in contents:
                contents[cache_key]=publisher_index(path,row['sha512'],loader)
            assert contents[cache_key]['sha512']==row['sha512'],'One native path has conflicting exact pins'
            locations=publisher_matches(contents[cache_key],classes)
            if locations:
                key=(fixture,row['filename'],row['sha512'])
                if key not in seen:found.append(dict(fixture=fixture,filename=row['filename'],sha512=row['sha512'],path=row['path'],class_locations=locations));seen.add(key)
        return found
    accepted=[];current_observations=[]
    campaign=OUT/'readiness-final-publishers-01/publishers.json'
    if campaign.exists():
        # The live writer can replace this journal between profile completions.
        # Resolve attempts and their immutable plan from one parsed snapshot.
        campaign_snapshot=json.loads(campaign.read_text())
        for attempt in latest_clean_publisher_attempts(campaign_snapshot):
            job=next(v for v in campaign_snapshot['plan'] if v['id']==attempt['job'])
            for result in attempt.get('results',[]):
                profile=ROOT/result['profile'];report=profile/'mod-interaction-checks.json'
                if result.get('exit_code')!=0 or result.get('status')!='passed' or not report.exists():continue
                staged=json.loads((profile/'staged-artifacts.json').read_text())['artifacts']
                binding=consumer.bind(job['target'],attempt['subjects'],current_subjects(job['target']))
                if binding is None or not subjects_match(staged,binding['original_subjects']):continue
                metadata={'artifact_binding':binding}if binding['artifact_equivalence']else{}
                data=json.loads(report.read_text())
                for case in data['cases']:
                    current_observations.append(dict(target=job['target'],case=case,report=str(report.relative_to(ROOT)),report_sha256=hashlib.sha256(report.read_bytes()).hexdigest(),**metadata))
                    if case['status']=='passed' and all(c['passed'] for c in data['checks'] if c['case']==case['id']):
                        accepted.append(dict(target=job['target'],case=case,report=str(report.relative_to(ROOT)),report_sha256=hashlib.sha256(report.read_bytes()).hexdigest(),**metadata))
        for retained in retained_for_campaign(ROOT,campaign_snapshot,
                lambda target:consumer.prior_subjects(target,current_subjects(target)),
                artifact_locator=consumer.artifact_path if consumer.certificate else None):
            report=ROOT/retained['report']['file'];assert hashlib.sha256(report.read_bytes()).hexdigest()==retained['report']['sha256']
            data=json.loads(report.read_text())
            chain=consumer.chained_retention(retained,current_subjects(retained['target']))
            metadata={'artifact_binding':chain}if chain else{}
            for case in data['cases']:
                entry=dict(target=retained['target'],case=case,report=str(report.relative_to(ROOT)),
                    report_sha256=retained['report']['sha256'],execution_kind=chain['execution_kind']if chain else retained['execution_kind'],
                    original_run=retained['original_run'],original_subjects=retained['original_subjects'],
                    retained_evidence=retained['retained_evidence'],equivalence=retained['equivalence'],**metadata)
                current_observations.append(entry)
                if case['status']=='passed'and all(c['passed']for c in data['checks']if c['case']==case['id']):accepted.append(entry)
    records=[]
    old=json.loads((OUT/'entry-only-backlog.json').read_text())
    for group in old['groups']:
        for target in group['targets']:
            criterion,driver=acceptance(group['case'],group['menus']);matches=[]
            for entry in accepted:
                if entry['target']==target and identity(entry['case']['id'])==identity(group['case']) and set(group['menus']).intersection(entry['case'].get('native_menus',[])):
                    matches.append(entry)
            qualified=[v for v in matches if any(v['case'].get(flag) for flag in ['native_bee_care_checked','native_output_generation_checked','native_recipe_checked','native_disposal_checked'])]
            records.append(dict(kind='historical-opening-only',target=target,case=group['case'],native_classes=group['menus'],
                disposition='functional-pass' if qualified else 'unresolved-coverage-blocker',confirmed_product_bug=False,
                current_exact_pins=pins(target,group['menus']),historical_evidence=group['evidence'],
                reproduction='Original normal entry actions are in the named historical case; establish the additional prerequisites below before measuring its mutable features.',
                required_work=criterion,focused_driver=driver,current_acceptance=qualified,
                current_scoped_evidence=matching_observations(current_observations,target,group['menus'],group['case']),
                review_note='A matching generic transfer/return pass is useful scoped evidence, but does not automatically certify the specialized controls/prerequisites requested for this historical opening-only UI.'))
    for gap in json.loads((OUT/'specialized-entry-inventory.json').read_text())['entries']:
        target=gap['minecraft']+'/'+gap['loader'];criterion,driver=acceptance(gap['ui'],[gap['screen']])
        matches=[entry for entry in accepted if entry['target']==target and gap['screen'] in entry['case'].get('native_screens',[])]
        qualified=[v for v in matches if any(v['case'].get(flag) for flag in ['native_bee_care_checked','native_output_generation_checked','native_recipe_checked','native_disposal_checked'])]
        records.append(dict(kind='specialized-entry',target=target,case=gap['ui'],native_classes=[gap['screen']],
            disposition='functional-pass' if qualified else 'unresolved-coverage-blocker',confirmed_product_bug=False,
            current_exact_pins=gap['current_exact_cohorts'],captured_publisher_sha256=gap['publisher_sha256'],
            reproduction=gap['reason']+' Use the retained SIU-absent capture and a semantic native transition, never a forced provider.',
            required_work=criterion,focused_driver=driver,current_acceptance=qualified,
            current_scoped_evidence=matching_observations(current_observations,target,[gap['screen']]),
            review_note='Native entry/transfer evidence remains scoped until the specialized feature or approved replacement-surface contract is explicitly reconciled.'))
    for entry in current_discovery_gaps(records,current_observations):
        case=entry['case'];class_evidence=discovery_class_evidence(case);classes=class_evidence['observed']
        criterion,driver=acceptance(case['id'],class_evidence['pin_classes'])
        if case['status']!='entry-and-policy-only':
            criterion='Resolve the exact native entry/registry prerequisites, or record evidence-backed native-only/unavailable scope on this pinned publisher. A rejected or unexecuted fixture is not a functional pass. '+criterion
        records.append(dict(kind='current-discovery-opening-only'if case['status']=='entry-and-policy-only'else'current-discovery-unqualified-fixture',
            discovery_status=case['status'],target=entry['target'],case=case['id'],native_classes=classes,
            expected_native_classes=class_evidence['declared'],publisher_pin_basis=class_evidence['pin_basis'],
            disposition='unresolved-coverage-blocker',confirmed_product_bug=False,
            current_exact_pins=pins(entry['target'],class_evidence['pin_classes']),current_acceptance=[],current_scoped_evidence=[entry],
            reproduction='Replay the exact clean current publisher report entry actions, then establish the native mutable/read-only prerequisites below.',
            required_work=criterion,focused_driver=driver,
            review_note='Newly discovered unqualified case absent from the historical backlog, or using a different/unknown native class. Entry, rejected fixtures and other incomplete results require an explicit disposition.'))
    # Named standalone workflows are deliberately separate from discovery's
    # generic transfer flags. Seal their original QA sources and exact native
    # entry before a reviewed row can close. Missing/pending proofs do nothing.
    standalone=consumer.coverage_path(OUT,'coverage-standalone',args.generation,'02')
    if standalone.exists():
        proof=json.loads(standalone.read_text())
        assert proof['status']=='named-current-artifact-acceptances-reconciled'
        def checked(reference):
            path=ROOT/reference['file']
            assert hashlib.sha256(path.read_bytes()).hexdigest()==reference['sha256'],reference['file']
            return path
        with zipfile.ZipFile(checked(proof['qa_source_archive']))as archive:
            assert set(archive.namelist())==set(proof['qa_inputs'])
            for file,wanted in proof['qa_inputs'].items():assert hashlib.sha256(archive.read(file)).hexdigest()==wanted,file
        for reviewed in proof['records']:
            assert {k:tuple(v)for k,v in reviewed['subjects'].items()}==consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target']))
            for key in ['summary','staged','acceptance','native_entry']:checked(reviewed[key])
            for reference in reviewed['full_state_inputs']:checked(reference)
            summary=json.loads(checked(reviewed['summary']).read_text())
            assert summary['status']=='passed'and summary['exit_code']==0 and summary['checks_exit_code']==0
            assert subjects_match(summary['subjects'],consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            journal=json.loads(checked(reviewed['acceptance']).read_text())
            assert len(journal['checks'])==reviewed['checks']and all(v['passed']for v in journal['checks'])
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1,reviewed['case'];row=hits[0]
            actual_pins=pins(reviewed['target'],reviewed['native_classes'])
            assert actual_pins
            assert {v['sha512']for v in actual_pins}<={v['sha512']for v in reviewed['publishers']},'Unqualified publisher patch in named acceptance'
            if row['native_classes']!=reviewed['native_classes']:
                assert reviewed['corrected_historical_classification'],reviewed['case']
                row['historical_native_classes']=row['native_classes']
                row['classification_correction']=reviewed['classification_note']
                row['native_classes']=reviewed['native_classes'];row['current_exact_pins']=actual_pins
            row.update(disposition='functional-pass',current_acceptance=[dict(
                report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                kind='named-standalone-functional-workflow',reviewed_evidence=str(standalone.relative_to(ROOT)),
                reviewed_evidence_sha256=hashlib.sha256(standalone.read_bytes()).hexdigest(),
                scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Named current-artifact functional workflow accepted for the exact native publisher after QA-source and full-state sealing; other targets remain separate.')
    waystone_proof=consumer.coverage_path(OUT,'coverage-waystone',args.generation,'01')
    if waystone_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_waystone_workflows',ROOT/'tools/reconcile-waystone-release-evidence.py')
        waystone=importlib.util.module_from_spec(spec);spec.loader.exec_module(waystone)
        proof=json.loads(waystone_proof.read_text());assert proof['status']=='reviewed-waystone-modifier-workflows-reconciled'
        assert len({v['target']for v in proof['records']})==len(proof['records'])
        for reviewed in proof['records']:
            waystone.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==waystone.CASE]
            assert len(hits)==1,reviewed['target'];row=hits[0]
            assert row['kind']=='current-discovery-unqualified-fixture'and not row['native_classes'],'Only the reviewed absent-registry fixture gap may be replaced'
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified Waystones publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=[],native_classes=reviewed['native_classes'],
                current_exact_pins=actual,publisher_pin_basis='reviewed-sealed-native-entry-class',classification_correction=reviewed['classification_note'],
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-native-modifier-entry-and-socket-workflow',reviewed_evidence=str(waystone_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(waystone_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Original failed fixture observations remain retained. Normal renamed-block placement and native child-button entry qualified the original socket transfers on the exact current publisher. Teleport effects and selector UI are outside this inventory scope.')
    sensor_proof=consumer.coverage_path(OUT,'coverage-sensors',args.generation,'01')
    if sensor_proof.exists():
        from sensor_feature_evidence import verify as verify_sensor
        proof=json.loads(sensor_proof.read_text());assert proof['status']=='reviewed-native-sensor-feature-workflows-reconciled'
        assert len({(v['target'],v['case'])for v in proof['records']})==len(proof['records'])
        for reviewed in proof['records']:
            verify_sensor(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1,reviewed['case'];row=hits[0]
            original=row.get('historical_native_classes',row['native_classes'])
            assert original==reviewed['original_native_classes'],'Unreviewed historical sensor classification'
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified native sensor publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-original-native-sensor-feature-workflow',reviewed_evidence=str(sensor_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(sensor_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Explicit native sensor alias qualified from nine original control/persistence/source checks and six complete typed observations on the exact native publisher. Shared aliases add no gameplay executions; rocket flight and redstone automation remain outside this inventory UI acceptance.')
    mi_energy_proof=consumer.coverage_path(OUT,'coverage-mi-energy',args.generation,'01')
    if mi_energy_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_mi_energy_status',ROOT/'tools/reconcile-mi-energy-release-evidence.py')
        energy=importlib.util.module_from_spec(spec);spec.loader.exec_module(energy)
        proof=json.loads(mi_energy_proof.read_text());assert proof['status']=='reviewed-native-mi-energy-status-workflows-reconciled'
        assert len(proof['records'])==20 and {(v['target'],v['case'])for v in proof['records']}=={(target,energy.PREFIX+hatch)for target in energy.TARGETS for hatch in energy.HATCHES}
        for reviewed in proof['records']:
            energy.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified MI energy publisher patch'
            row.update(disposition=energy.STATUS,historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-native-mi-read-only-energy-status',reviewed_evidence=str(mi_energy_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(mi_energy_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Read-only energy UI qualified from its exact native block ID, complete component geometry and empty/charged/reopened client-server observations. Declared charge does not prove power generation, transmission, multiblock formation or recipes.')
    mi_storage_proof=consumer.coverage_path(OUT,'coverage-mi-storage',args.generation,'01')
    if mi_storage_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_mi_storage',ROOT/'tools/reconcile-mi-storage-release-evidence.py')
        storage=importlib.util.module_from_spec(spec);spec.loader.exec_module(storage)
        proof=json.loads(mi_storage_proof.read_text());assert proof['status']=='reviewed-native-mi-storage-workflows-reconciled'
        assert len(proof['records'])==34 and {(v['target'],v['case'])for v in proof['records']}=={(target,case)for target in storage.TARGETS for case in storage.CASES}
        for reviewed in proof['records']:
            storage.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified MI storage publisher patch'
            row.update(disposition=storage.STATUS,historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-native-mi-storage-transfers-and-controls',reviewed_evidence=str(mi_storage_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(mi_storage_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Exact native storage variant qualified from first/last bucket or typed item pickup/refusal/reopening and original native-provided lock/eject controls, option restoration and full physical ledgers. Declared stored resources do not prove production, multiblock formation or adjacent-container automation.')
    mi_infra_proof=consumer.coverage_path(OUT,'coverage-mi-infrastructure',args.generation,'01')
    if mi_infra_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_mi_infrastructure',ROOT/'tools/reconcile-mi-infrastructure-release-evidence.py')
        infra=importlib.util.module_from_spec(spec);spec.loader.exec_module(infra)
        proof=json.loads(mi_infra_proof.read_text());assert proof['status']=='reviewed-native-mi-infrastructure-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,case)for target in infra.TARGETS for case in infra.CASES}
        for reviewed in proof['records']:
            infra.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified MI infrastructure publisher patch'
            row.update(disposition=reviewed['disposition'],historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-native-mi-pump-or-formed-tank',reviewed_evidence=str(mi_infra_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(mi_infra_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Native pump cycles retain actual steam consumption/generated water and native withdrawal/refusal/reopening. LargeTank separately retains genuine formation/capacity and declared populated gauge status; its read-only disposition does not claim tank production or fluid transfers.')
    mi_multi_proof=consumer.coverage_path(OUT,'coverage-mi-multiblock',args.generation,'01')
    if mi_multi_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_mi_multiblocks',ROOT/'tools/reconcile-mi-multiblock-release-evidence.py')
        multi=importlib.util.module_from_spec(spec);spec.loader.exec_module(multi)
        proof=json.loads(mi_multi_proof.read_text());assert proof['status']=='reviewed-native-mi-multiblock-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,case)for target in multi.TARGETS for case in multi.CASES}
        for reviewed in proof['records']:
            multi.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified MI multiblock publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-native-mi-formed-controller-workflow',reviewed_evidence=str(mi_multi_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(mi_multi_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Exact original controller workflow retains native blueprint/allowed hatches, shape tray, formed/active/invalidation/reopen states and typed item/hatch ledgers. Coke/blast qualify their guaranteed recipe output; quarry qualifies active status and invalidation only, without claiming completed probabilistic output.')
    ae2_settings_proof=consumer.coverage_path(OUT,'coverage-ae2-settings',args.generation,'01')
    if ae2_settings_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_ae2_settings',ROOT/'tools/reconcile-ae2-settings-release-evidence.py')
        settings=importlib.util.module_from_spec(spec);spec.loader.exec_module(settings)
        proof=json.loads(ae2_settings_proof.read_text());assert proof['status']=='reviewed-native-ae2-settings-workflows-reconciled'
        assert len(proof['records'])==13 and len({(v['target'],v['case'])for v in proof['records']})==13
        assert all(sum(v['case']==case for v in proof['records'])==(1 if case=='ae2-priority'else 6)for case in settings.ALIASES)
        for reviewed in proof['records']:
            settings.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])),consumer=consumer,current=current_subjects(reviewed['target']))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified AE2 settings publisher patch'
            row.update(disposition=reviewed['disposition'],historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-original-ae2-settings-or-local-overlay',reviewed_evidence=str(ae2_settings_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(ae2_settings_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Exact original priority/emitter numeric and mode controls preserve native backing values, movement and reopening with complete typed item/cursor ledgers. Spatial Anchor has real populated statistics and a local-only overlay toggle; read-only status acceptance does not claim a server overlay setting or chunk-loading endurance.')
    ae2_quantum_proof=consumer.coverage_path(OUT,'coverage-ae2-quantum',args.generation,'01')
    if ae2_quantum_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_ae2_quantum',ROOT/'tools/reconcile-ae2-quantum-release-evidence.py')
        quantum=importlib.util.module_from_spec(spec);spec.loader.exec_module(quantum)
        proof=json.loads(ae2_quantum_proof.read_text());assert proof['status']=='reviewed-native-ae2-quantum-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,quantum.CASE)for target in quantum.TARGETS}
        for reviewed in proof['records']:
            quantum.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])),consumer=consumer,current=current_subjects(reviewed['target']))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified AE2 quantum publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-original-ae2-quantum-slot-and-source-lifetime',reviewed_evidence=str(ae2_quantum_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(ae2_quantum_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='One genuine formed native chamber qualifies nonzero-frequency slot pickup/insertion/refusal/recovery, moved input, ordinary reopening, complete typed inventory/cursor/physical conservation and empty-source invalidation. It does not qualify two-endpoint network transport or arbitrary dimensions.')
    ae2_network_proof=consumer.coverage_path(OUT,'coverage-ae2-network',args.generation,'01')
    if ae2_network_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_ae2_network',ROOT/'tools/reconcile-ae2-network-release-evidence.py')
        network=importlib.util.module_from_spec(spec);spec.loader.exec_module(network)
        proof=json.loads(ae2_network_proof.read_text());assert proof['status']=='reviewed-native-ae2-network-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,network.CASE)for target in network.TARGETS}
        for reviewed in proof['records']:
            network.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])),consumer=consumer,current=current_subjects(reviewed['target']))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified AE2 status publisher patch'
            row.update(disposition='read-only-status-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-original-ae2-network-status-and-export',reviewed_evidence=str(ae2_network_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(ae2_network_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Actual native populated packet/backend topology retains two/three/two drives, local units, moved controls and reopening with full typed item preservation. Modern native Export Grid requires its exact populated ZIP; legacy correctly has no such button. Read-only status acceptance is not complete grid or item-transfer gameplay.')
    ae2_stock_proof=consumer.coverage_path(OUT,'coverage-ae2-stock',args.generation,'02')
    if ae2_stock_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_ae2_stock',ROOT/'tools/reconcile-ae2-stock-release-evidence.py')
        stock=importlib.util.module_from_spec(spec);spec.loader.exec_module(stock)
        proof=json.loads(ae2_stock_proof.read_text());assert proof['status']=='reviewed-native-ae2-stock-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,stock.CASE)for target in stock.TARGETS}
        for reviewed in proof['records']:
            stock.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])),consumer=consumer,current=current_subjects(reviewed['target']))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified AE2 stock publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-original-ae2-ghost-stock-child',reviewed_evidence=str(ae2_stock_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(ae2_stock_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Actual native Interface ghost selection and separate SetStockAmountMenu retain initial3, native Set37/41, moved input, parent/child reopening and ghost clear with full typed owned items. Inaccessible native display copies are decoded/accounted explicitly; actual storage stays empty. This does not qualify positive network stocking or crafting.')
    ae2_processing_proof=consumer.coverage_path(OUT,'coverage-ae2-processing',args.generation,'02')
    if ae2_processing_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_ae2_processing',ROOT/'tools/reconcile-ae2-processing-release-evidence.py')
        processing=importlib.util.module_from_spec(spec);spec.loader.exec_module(processing)
        proof=json.loads(ae2_processing_proof.read_text());assert proof['status']=='reviewed-native-ae2-processing-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,processing.CASE)for target in processing.TARGETS}
        for reviewed in proof['records']:
            processing.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])),consumer=consumer,current=current_subjects(reviewed['target']))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified AE2 processing publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-original-ae2-local-processing-child-and-encoding',reviewed_evidence=str(ae2_processing_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(ae2_processing_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Original local child keeps the actual parent menu/session. Native Set37/output5, cancel/back, movement, reopen and zero/ghost clear retain full typed keys and owned items. Original Encode consumes a real blank, its native decoded complete input/output are verified, and the encoded item returns to the player. No downstream machine production or fluid-encoding claim.')
    ae2_pattern_proof=consumer.coverage_path(OUT,'coverage-ae2-pattern',args.generation,'01')
    if ae2_pattern_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_ae2_pattern',ROOT/'tools/reconcile-ae2-pattern-release-evidence.py')
        pattern=importlib.util.module_from_spec(spec);spec.loader.exec_module(pattern)
        proof=json.loads(ae2_pattern_proof.read_text());assert proof['status']=='reviewed-native-ae2-pattern-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}=={(target,pattern.CASE)for target in pattern.TARGETS}
        for reviewed in proof['records']:
            pattern.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])),consumer=consumer,current=current_subjects(reviewed['target']))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified AE2 pattern-access publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-original-ae2-provider-remote-pattern-inputs',reviewed_evidence=str(ae2_pattern_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(ae2_pattern_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='One declared native-encoded player ingredient enters the actual provider through its restricted UI slot. Real remote records, decoded-output/impossible search, refusal/recovery/return, moved input, native shown-provider filter and reopening retain exact complete physical inventory/cursor data. No downstream processing output, exhaustive provider topology or large-list scrolling claim.')
    ae2_crafting_proof=consumer.coverage_path(OUT,'coverage-ae2-crafting',args.generation,'02')
    if ae2_crafting_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_ae2_crafting',ROOT/'tools/reconcile-ae2-crafting-release-evidence.py')
        crafting=importlib.util.module_from_spec(spec);spec.loader.exec_module(crafting)
        proof=json.loads(ae2_crafting_proof.read_text());assert proof['status']=='reviewed-native-ae2-crafting-workflows-reconciled'
        assert proof['unique_executions']==7 and len(proof['records'])==18
        assert {(v['target'],v['case'])for v in proof['records']}=={(target,case)for target in crafting.TARGETS for case in crafting.ALIASES}
        for reviewed in proof['records']:
            crafting.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])),consumer=consumer,current=current_subjects(reviewed['target']))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified native crafting publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-original-ae2-real-crafting-error-replan-retry',reviewed_evidence=str(ae2_crafting_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(ae2_crafting_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Three native screen aliases share one real execution per target. Native amount/plan/CPU selection, moved confirmation and Cancel preserve all items. Actual Start consumes three logs for twelve planks; two real ingredient withdrawals produce genuine local errors on CraftConfirmMenu. Native Replan preserves resources and Retry consumes two logs for eight more planks. Exact recovery and full typed item/cursor/mirror ledgers; no synthetic CraftErrorMenu or additional gameplay counts.')
    cpu_proof=consumer.coverage_path(OUT,'coverage-ae2-cpu',args.generation,'01')
    if cpu_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_ae2_cpu',ROOT/'tools/reconcile-ae2-cpu-release-evidence.py')
        cpu=importlib.util.module_from_spec(spec);spec.loader.exec_module(cpu)
        proof=json.loads(cpu_proof.read_text());assert proof['status']=='reviewed-native-ae2-cpu-workflows-reconciled'
        assert proof['unique_executions']==10 and len(proof['records'])==7
        assert {(v['target'],v['case'])for v in proof['records']}=={*[(t,cpu.CPU_CASE)for t in cpu.TARGETS],('1.21.1/neoforge',cpu.STATUS_ALIAS)}
        for reviewed in proof['records']:
            cpu.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified native CPU publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=v['acceptance']['file'],report_sha256=v['acceptance']['sha256'],
                    kind='reviewed-genuine-cpu-job-controls'if reviewed['case']==cpu.CPU_CASE else'reviewed-idle-cpu-selector',
                    reviewed_evidence=str(cpu_proof.relative_to(ROOT)),reviewed_evidence_sha256=hashlib.sha256(cpu_proof.read_bytes()).hexdigest(),scope=v['scope'],limits=v['limits'])
                    for v in[reviewed,*reviewed.get('additional_workflows',[])]],
                review_note='Real submitted-job scheduling cycles and desktop Cancel twice per execution, plus applicable native Suspend/Resume, remain distinct from the separate nine-idle-CPU scrolling/selection workflow. Both recorded legacy patch pins retain separate native executions; exact15.4.12 controls are also verified on Forge. Compiled native-typed lifecycle source, executed binaries, actual jobs and status observations retain their stated class-evidence basis; no completed-crafting-output or extra assertion claim.')
    additional_proof=consumer.coverage_path(OUT,'coverage-additional-native',args.generation,'01')
    if additional_proof.exists():
        import additional_native_release_evidence as additional
        from focused_release_evidence import checked
        proof=json.loads(additional_proof.read_text());assert proof['status']=='reviewed-additional-native-workflows-reconciled'
        assert proof['unique_executions']==15 and proof['checks']==354 and proof['original_backlog_records']==13 and len(proof['records'])==13
        qualified=proof['workflow_qualifications'];assert len(qualified)==15 and len({(v['family'],v['qualification']['target'])for v in qualified})==15
        for value in qualified:
            q=value['qualification'];expected=consumer.expected(q['target'],q,current_subjects(q['target']))
            assert additional.workflow(ROOT,value['family'],checked(ROOT,q['seal']),expected)==value
        for reviewed in proof['records']:
            additional.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']];assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual and {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']}
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],kind='reviewed-original-additional-native-'+reviewed['family'],
                    reviewed_evidence=str(additional_proof.relative_to(ROOT)),reviewed_evidence_sha256=hashlib.sha256(additional_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Native full-state derivation retains all original case identities, complete typed resources, actual callbacks and stated limits. Fifteen unique executions cover thirteen original backlog rows; no duplicate gameplay count. The MEGA workbench includes separate ordinary fuzzy-card and native bulk-compression workflows.')
    status_proof=consumer.coverage_path(OUT,'coverage-techreborn-status',args.generation,'01')
    if status_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_techreborn_status',ROOT/'tools/reconcile-techreborn-status-release-evidence.py')
        status=importlib.util.module_from_spec(spec);spec.loader.exec_module(status)
        proof=json.loads(status_proof.read_text());assert proof['status']=='reviewed-native-techreborn-status-workflows-reconciled'
        assert len({(v['target'],v['case'])for v in proof['records']})==len(proof['records'])
        for reviewed in proof['records']:
            status.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])),consumer=consumer,current=current_subjects(reviewed['target']))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes'])
            assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified Tech Reborn/RebornCore publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=workflow['acceptance']['file'],report_sha256=workflow['acceptance']['sha256'],
                    kind='reviewed-native-techreborn-status-workflow',reviewed_evidence=str(status_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(status_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])for workflow in[reviewed,*reviewed.get('additional_workflows',[])]],
                review_note='Four original zero-item panels are identified by screen and native entity, not their shared generic menu. Exact selector and energy/status states qualify only the staged publisher; teleportation, launching and detector automation remain outside this UI workflow.')
    powah_proof=consumer.coverage_path(OUT,'coverage-powah',args.generation,'01')
    if powah_proof.exists():
        from powah_native_release_evidence import TARGETS as POWAH_TARGETS,derive as derive_powah
        from additional_native_release_evidence import publishers as powah_publishers
        proof=json.loads(powah_proof.read_text());assert proof['status']=='reviewed-native-powah-workflows-reconciled'
        assert proof['unique_executions']==4 and proof['assertions']==332 and set(proof['workflows'])==set(POWAH_TARGETS)
        assert len(proof['records'])==13 and len({(v['target'],v['case'])for v in proof['records']})==13
        for target,q in proof['workflows'].items():
            assert derive_powah(ROOT,ROOT/q['seal']['file'],consumer.expected(target,q,current_subjects(target)))==q
        assert proof['states']==sum(q['validation']['states']for q in proof['workflows'].values())
        for reviewed in proof['records']:
            target=reviewed['target'];q=proof['workflows'][reviewed['workflow_target']];assert target==q['target']
            case=reviewed['case'];assert case.startswith('registry-powah-')and case.endswith('_starter')
            name=case[len('registry-powah-'):-len('_starter')]
            assert reviewed['native_classes']==q['validation']['native_classes'][name]
            assert reviewed['validation']==next(v for v in q['validation']['cases']if v['case']==name)
            assert reviewed['acceptance']==q['acceptance']and reviewed['sealed_evidence']==q['seal']and reviewed['subjects']==q['subjects']
            assert reviewed['scope']==q['scope']and reviewed['limits']==q['limits']
            assert powah_publishers(ROOT,ROOT/q['seal']['file'],target,reviewed['native_classes'])==reviewed['publishers']
            hits=[r for r in records if r['target']==target and r['case']==case];assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']==[reviewed['native_classes'][0]]
            actual=pins(target,reviewed['native_classes']);assert actual and {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']}
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],kind='reviewed-native-powah-controls-and-resource-workflow',
                    reviewed_evidence=str(powah_proof.relative_to(ROOT)),reviewed_evidence_sha256=hashlib.sha256(powah_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Thirteen historical UI aliases share four actual publisher executions with332originalassertions. Original side/redstone controls, full typed card ownership and actual solar behavior retain exact scope; the separate1.21.1discharger additionally proves seven physical cells and balanced native energy. No arbitrary power-network or transmitter-charging claim.')
    toms_proof=consumer.coverage_path(OUT,'coverage-toms',args.generation,'01')
    if toms_proof.exists():
        import importlib.util
        spec=importlib.util.spec_from_file_location('reviewed_toms_workflows',ROOT/'tools/reconcile-toms-release-evidence.py')
        toms=importlib.util.module_from_spec(spec);spec.loader.exec_module(toms)
        proof=json.loads(toms_proof.read_text());assert proof['status']=='reviewed-toms-native-workflows-reconciled'
        assert len({(v['target'],v['case'])for v in proof['records']})==len(proof['records'])
        for reviewed in proof['records']:
            toms.verify(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1,reviewed['case'];row=hits[0]
            original=row.get('historical_native_classes',row['native_classes'])
            assert original==reviewed['original_native_classes'],'Unreviewed Tom historical classification'
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified Tom publisher patch'
            if reviewed['corrected_historical_classification']:
                row.update(historical_native_classes=original,native_classes=reviewed['native_classes'],
                    classification_correction=reviewed['classification_note'])
            else:assert row['native_classes']==reviewed['native_classes']
            row.update(disposition='functional-pass',current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-original-toms-feature-workflow',reviewed_evidence=str(toms_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(toms_proof.read_bytes()).hexdigest(),
                    scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Explicit Tom UI alias qualified from sealed original native feature assertions and their immediate observations. Shared aliases do not add gameplay executions; every native publisher pin must match.')
    analyzer_proof=consumer.coverage_path(OUT,'coverage-analyzer',args.generation,'01')
    if analyzer_proof.exists():
        from focused_release_evidence_analyzer import CASE as ANALYZER_CASE,TARGETS as ANALYZER_TARGETS,verify as verify_analyzer
        proof=json.loads(analyzer_proof.read_text());assert proof['status']=='reviewed-native-analyzer-feature-workflows-reconciled'
        assert len(proof['records'])==2 and {(v['target'],v['case'])for v in proof['records']}=={(target,ANALYZER_CASE)for target in ANALYZER_TARGETS}
        for reviewed in proof['records']:
            verify_analyzer(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0];original=row.get('historical_native_classes',row['native_classes'])
            assert original==reviewed['original_native_classes'],'Unreviewed historical Analyzer classification'
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified native Bountiful publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-native-decree-analysis-workflow',reviewed_evidence=str(analyzer_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(analyzer_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Original operator-command Analyzer entry,10 native assertions and10 retained observations qualify exact decree/pool controls and complete-data recovery/refusal. Native close-return allows another empty player cell; bounty generation and all decree distributions remain separate.')
    nifty_proof=consumer.coverage_path(OUT,'coverage-nifty',args.generation,'01')
    if nifty_proof.exists():
        from focused_release_evidence_nifty import CASES as NIFTY_CASES,TARGETS as NIFTY_TARGETS,verify as verify_nifty
        proof=json.loads(nifty_proof.read_text());assert proof['status']=='reviewed-native-nifty-feature-workflows-reconciled'
        assert proof['unique_executions']==3 and len(proof['records'])==6
        assert {(v['target'],v['case'])for v in proof['records']}=={(target,case)for target in NIFTY_TARGETS for case in NIFTY_CASES}
        for reviewed in proof['records']:
            verify_nifty(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0];original=row.get('historical_native_classes',row['native_classes'])
            assert original==reviewed['original_native_classes'],'Unreviewed historical cart classification'
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified native NiftyCarts publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-native-cart-equipment-workflow',reviewed_evidence=str(nifty_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(nifty_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Two cart UI records share one original15-assertion native execution per target. Ten retained full states recheck every designated cell, original native rims, typed physical items, reopening and applicable seed refusal. Original movement/entry/invalidation assertions remain distinct from saved frames; no driving/agricultural claim.')
    guard_proof=consumer.coverage_path(OUT,'coverage-guard',args.generation,'01')
    if guard_proof.exists():
        from focused_release_evidence_guard import verify as verify_guard
        proof=json.loads(guard_proof.read_text());assert proof['status']=='reviewed-native-guard-feature-workflows-reconciled'
        assert len(proof['records'])==9 and len({v['target']for v in proof['records']})==9
        for reviewed in proof['records']:
            verify_guard(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes'])
            assert original==reviewed['original_native_classes'],'Unreviewed historical Guard classification'
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified native Guard publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-native-guard-equipment-and-behavior-workflow',reviewed_evidence=str(guard_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(guard_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Original six equipment cells, thirteen native assertions and all original full-state observations plus the explicitly modeled native-latch observation on adapted targets qualify only the staged publisher and named target. Hero permission remains a fixture prerequisite; combat AI and patrol travel are outside this UI contract.')
    wands_proof=consumer.coverage_path(OUT,'coverage-wands',args.generation,'01')
    if wands_proof.exists():
        from focused_release_evidence_wands import verify as verify_wands
        proof=json.loads(wands_proof.read_text());assert proof['status']=='reviewed-native-wands-tool-selector-workflows-reconciled'
        assert len(proof['records'])==8 and len(proof['all_target_executions'])==10
        assert len({v['target']for v in proof['all_target_executions']})==10
        for reviewed in proof['all_target_executions']:
            verify_wands(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
        for reviewed in proof['records']:
            verify_wands(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']}
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],kind='reviewed-native-tool-source-selection',
                    reviewed_evidence=str(wands_proof.relative_to(ROOT)),reviewed_evidence_sha256=hashlib.sha256(wands_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Ten original assertions and nine full-state observations qualify native tool-source selection/deselection, synchronization, moved input, reopening and typed item/cursor conservation. This is configuration, not owned storage or a physical-transfer test.')
    pet_proof=consumer.coverage_path(OUT,'coverage-pet-cargo',args.generation,'01')
    if pet_proof.exists():
        from focused_release_evidence_pet_cargo import PAIRS as PET_PAIRS,verify as verify_pet
        proof=json.loads(pet_proof.read_text());assert proof['status']=='reviewed-native-pet-cargo-workflows-reconciled'
        assert len(proof['records'])==5 and {(v['target'],v['case'])for v in proof['records']}==PET_PAIRS
        for reviewed in proof['records']:
            verify_pet(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes'])
            assert original==reviewed['original_native_classes'],'Unreviewed pet inventory classification'
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified native pet publisher patch'
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='reviewed-native-pet-cargo-and-equipment-workflow',reviewed_evidence=str(pet_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(pet_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Original ten or eleven assertions and eleven full-state observations qualify the exact staged publisher only. All fifteen cargo cells, native refusal, typed conservation, moved input, owner reopening and range invalidation are retained; no pet flight/combat claim.')
    reforge_proof=consumer.coverage_path(OUT,'coverage-reforge',args.generation,'01')
    if reforge_proof.exists():
        from focused_release_evidence_reforge import PAIRS as REFORGE_PAIRS,verify as verify_reforge
        proof=json.loads(reforge_proof.read_text());assert proof['status']=='reviewed-native-reforge-feature-workflows-reconciled'
        assert len(proof['records'])==3 and {(v['target'],v['case'])for v in proof['records']}==set(REFORGE_PAIRS)
        for reviewed in proof['records']:
            verify_reforge(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']}
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],kind='reviewed-native-reforge-cost-quality-and-recovery',reviewed_evidence=str(reforge_proof.relative_to(ROOT)),reviewed_evidence_sha256=hashlib.sha256(reforge_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Eleven original assertions and nine full-state witnesses qualify actual native random quality, exact resource costs/derived durability and typed recovery, through the visible moved action. Every exact publisher pin remains required; no deterministic roll or complete-mod claim.')
    smither_proof=consumer.coverage_path(OUT,'coverage-smitherz',args.generation,'01')
    if smither_proof.exists():
        from focused_release_evidence_smitherz import PAIRS as SMITHER_PAIRS,verify as verify_smither
        proof=json.loads(smither_proof.read_text());assert proof['status']=='reviewed-native-smitherz-feature-workflows-reconciled'
        assert len(proof['records'])==4 and {(v['target'],v['case'])for v in proof['records']}==SMITHER_PAIRS
        for reviewed in proof['records']:
            verify_smither(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual and {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']}
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,native_upstream_limits=reviewed['native_upstream_limits'],
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],kind='reviewed-native-linking-and-grinding-with-exact-upstream-disposition',reviewed_evidence=str(smither_proof.relative_to(ROOT)),reviewed_evidence_sha256=hashlib.sha256(smither_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Original paid random linking, exact material/virtual-socket ownership, moved action, cancellation and recovery qualified through two native executions. Exact1.0.4gem nonreturn remains an independently confirmed upstream limit;1.2.0passes strict gem return. Four aliases do not create four gameplay runs.')
    ssn_proof=consumer.coverage_path(OUT,'coverage-ssn-filters',args.generation,'01')
    if ssn_proof.exists():
        from focused_release_evidence_ssn_filters import PAIRS as SSN_FILTER_PAIRS,verify as verify_ssn_filter
        proof=json.loads(ssn_proof.read_text());assert proof['status']=='reviewed-native-ssn-filter-workflows-reconciled'
        assert len(proof['records'])==6 and {(v['target'],v['case'])for v in proof['records']}==SSN_FILTER_PAIRS
        for reviewed in proof['records']:
            verify_ssn_filter(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']];assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual and {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']}
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,native_upstream_limits=reviewed['native_upstream_limits'],
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],kind='reviewed-native-ghost-filters-and-exact-upstream-dispositions',reviewed_evidence=str(ssn_proof.relative_to(ROOT)),reviewed_evidence_sha256=hashlib.sha256(ssn_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Six aliases share three native target executions with exact full item/ghost/cursor data, native priority and allowlist controls, moved input, persistence and ordinary close. Each staged1.13.2publisher has independent no-SIU lifetime and Collector display/count controls; no network or processing-automation acceptance.')
    process_proof=consumer.coverage_path(OUT,'coverage-ssn-process',args.generation,'01')
    if process_proof.exists():
        from focused_release_evidence_ssn_process import PAIRS as SSN_PROCESS_PAIRS,verify as verify_ssn_process
        proof=json.loads(process_proof.read_text());assert proof['status']=='reviewed-native-ssn-process-workflows-reconciled'
        assert len(proof['records'])==2 and {(v['target'],v['case'])for v in proof['records']}==SSN_PROCESS_PAIRS
        for reviewed in proof['records']:
            verify_ssn_process(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']];assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual and {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']}
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,native_upstream_limits=reviewed['native_upstream_limits'],
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],kind='reviewed-native-process-configuration-and-pinned-display-limits',reviewed_evidence=str(process_proof.relative_to(ROOT)),reviewed_evidence_sha256=hashlib.sha256(process_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Seventeen native assertions and eighteen full-state witnesses per target qualify ghost/configuration controls, exact typed ownership, moved input, native lifetime and ordinary close. Stale redstone client/icon state is independently reproduced with SIU absent on each pin; no immediate icon sync, formed network, production reset or adjacent import acceptance.')
    gallery_proof=consumer.coverage_path(OUT,'coverage-gallery',args.generation,'01')
    if gallery_proof.exists():
        from focused_release_evidence_gallery import PAIRS as GALLERY_PAIRS,MENU as GALLERY_MENU,verify as verify_gallery
        proof=json.loads(gallery_proof.read_text());assert proof['status']=='reviewed-native-gallery-workflows-reconciled'
        assert len(proof['records'])==2 and {(v['target'],v['case'])for v in proof['records']}==GALLERY_PAIRS
        for reviewed in proof['records']:
            verify_gallery(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']];assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==[GALLERY_MENU]
            actual=pins(reviewed['target'],[GALLERY_MENU]);assert actual and {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']}
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=[GALLERY_MENU],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],kind='reviewed-native-painting-selector-and-protected-source',reviewed_evidence=str(gallery_proof.relative_to(ROOT)),reviewed_evidence_sha256=hashlib.sha256(gallery_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Original native search, no-match/clear, one-row scrolling, moved selection, unrelated item transfer, full held-source protection, exact variant persistence and source invalidation:18assertions and15full states on each exact publisher. Not every painting placement, resource pack or localization.')
    merchant_proof=consumer.coverage_path(OUT,'coverage-red-merchant',args.generation,'02')
    if merchant_proof.exists():
        from focused_release_evidence_red_merchant import PAIRS as MERCHANT_PAIRS,verify as verify_merchant
        proof=json.loads(merchant_proof.read_text());assert proof['status']=='reviewed-native-red-merchant-workflows-reconciled'
        assert len(proof['records'])==4 and {(v['target'],v['case'])for v in proof['records']}==MERCHANT_PAIRS
        for reviewed in proof['records']:
            verify_merchant(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])),consumer=consumer,current=current_subjects(reviewed['target']))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']];assert len(hits)==1;row=hits[0]
            original=row.get('historical_native_classes',row['native_classes']);assert original==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual and {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']}
            row.update(disposition='functional-pass',historical_native_classes=original,native_classes=reviewed['native_classes'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],kind='reviewed-native-randomized-merchant-paid-trade',reviewed_evidence=str(merchant_proof.relative_to(ROOT)),reviewed_evidence_sha256=hashlib.sha256(merchant_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Four native targets each pass13original assertions and14full-state witnesses: native randomized offers, four-row scrolling/selection, invalid payment recovery, actual paid output and use count, exact tagged ownership, moved slots, same-owner reopening and invalidation. No fabricated trade, every-trade/restocking/progression or full gameplay suite claim.')
    selector_proof=consumer.coverage_path(OUT,'coverage-native-selectors',args.generation,'02')
    if selector_proof.exists():
        from native_selector_evidence import verify as verify_selector
        proof=json.loads(selector_proof.read_text());assert proof['status']=='reviewed-native-selector-acceptances-reconciled'
        for reviewed in proof['records']:
            verify_selector(ROOT,reviewed,consumer.expected(reviewed['target'],reviewed,current_subjects(reviewed['target'])))
            hits=[r for r in records if r['target']==reviewed['target']and r['case']==reviewed['case']]
            assert len(hits)==1,reviewed['case'];row=hits[0]
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']+reviewed['exact_module_aliases']},'Unqualified native selector publisher patch'
            row.update(disposition=reviewed['disposition'],historical_native_classes=row['native_classes'],
                native_classes=reviewed['native_classes'],current_exact_pins=actual,classification_correction=reviewed['classification_note'],
                current_acceptance=[dict(report=reviewed['report']['file'],report_sha256=reviewed['report']['sha256'],
                    kind='reviewed-native-selector-workflow',reviewed_evidence=str(selector_proof.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(selector_proof.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note='Applicable exact publisher native-selector workflow qualified from sealed current-artifact evidence. Other-cohort absent-publisher attempts remain retained as fixture observations.')
    originals=consumer.coverage_path(OUT,'coverage-original-features',args.generation,'01')
    if originals.exists():
        from focused_release_evidence_original_features import verify_record as verify_original_feature
        proof=json.loads(originals.read_text());assert proof['status']=='original-completed-native-features-reconciled'
        assert len(proof['records'])==88 and proof['unique_executions']==30
        for reviewed in proof['records']:
            verify_original_feature(ROOT,reviewed,consumer,current_subjects(reviewed['target']))
            hits=[r for r in records if(r['target'],r['case'])==(reviewed['target'],reviewed['case'])];assert len(hits)==1
            row=hits[0];assert row['native_classes']==reviewed['original_native_classes']
            actual=pins(reviewed['target'],reviewed['native_classes']);assert actual
            from accessories_current_pin_evidence import select_current_pin
            selection=select_current_pin(ROOT,reviewed,actual)
            if selection:
                row.update(historical_catalogue_publisher_candidates=selection['historical_catalogue_publisher_candidates'],
                    publisher_scope_selection=dict(evidence=selection['evidence'],scope=selection['scope']))
                actual=selection['pins']
            assert {v['sha512']for v in actual}<={v['sha512']for v in reviewed['publishers']},'Unqualified publisher in completed native feature'
            row.update(disposition=reviewed['disposition'],current_exact_pins=actual,
                current_acceptance=[dict(report=reviewed['acceptance']['file'],report_sha256=reviewed['acceptance']['sha256'],
                    kind='original-completed-native-feature-workflow',reviewed_evidence=str(originals.relative_to(ROOT)),
                    reviewed_evidence_sha256=hashlib.sha256(originals.read_bytes()).hexdigest(),scope=reviewed['scope'],limits=reviewed['limits'])],
                review_note=reviewed['limits'])
    if consumer.certificate:
        proof_cache={}
        for row in records:
            if row['disposition']not in {'functional-pass','native-only-qualified','read-only-status-pass'}:continue
            for accepted_proof in row['current_acceptance']:
                if 'artifact_binding'in accepted_proof:continue
                assert accepted_proof.get('reviewed_evidence'),'Accepted original execution lacks an explicit subject binding'
                name=accepted_proof['reviewed_evidence']
                if name not in proof_cache:proof_cache[name]=json.loads((ROOT/name).read_text())
                matches=[v for v in proof_cache[name]['records']if v['target']==row['target']and v['case']==row['case']]
                assert len(matches)==1,'Ambiguous original feature subject binding'
                binding=consumer.bind(row['target'],matches[0]['subjects'],current_subjects(row['target']))
                assert binding;accepted_proof['artifact_binding']=binding
    value=dict(status='coverage-dispositions-recorded-not-release-approval',candidate=candidate['source_snapshot']['file']if candidate else 'Unactivated source snapshot; artifact qualification remains pending',
        candidate_identity=candidate,
        artifact_equivalence=consumer.report(),
        scope='Explicit dispositions for all historical opening-only groups/targets and specialized-entry records. Other final gates remain in RELEASE_READINESS.md.',
        acceptance_rule='Only clean current-artifact publisher runs or explicitly verified unchanged-scope historical equivalences with matching case identity/native class are promoted. Original subjects and retained evidence stay labeled separately. Entry-only cases remain unqualified. Named standalone acceptances and approved replacement surfaces require explicit reconciliation; absence of an original Screen is not upstream unavailability.',
        counts=dict(collections.Counter(r['disposition'] for r in records)),records=records)
    (OUT/'coverage-backlog.json').write_text(json.dumps(value,indent=2)+'\n')
    with (OUT/'coverage-backlog.csv').open('w',newline='') as stream:
        writer=csv.writer(stream);writer.writerow(['target','case','kind','disposition','native_classes','expected_native_classes','publisher_pin_basis','exact_publisher_pins','required_work','focused_driver','evidence'])
        for r in records:writer.writerow([r['target'],r['case'],r['kind'],r['disposition'],'; '.join(r['native_classes']),
            '; '.join(r.get('expected_native_classes',[])),r.get('publisher_pin_basis','recorded-native-class'),
            '; '.join(sorted({p['filename']+' SHA512:'+p['sha512'] for p in r['current_exact_pins']})),r['required_work'],r['focused_driver'],
            '; '.join(v['report'] for v in r['current_acceptance']) or 'Pending current-artifact functional acceptance'])
    print(len(records),'explicit coverage records;',value['counts'])

if __name__=='__main__':main()
