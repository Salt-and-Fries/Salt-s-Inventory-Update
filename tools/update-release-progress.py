"""Refresh the active work journal from named results, without promoting unfinished work."""
from pathlib import Path
import datetime
import json

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'output/release-readiness'

def main():
    path=OUT/'work-state.json';state=json.loads(path.read_text())
    state['updated_at']=datetime.datetime.now(datetime.timezone.utc).isoformat()
    state['candidate']='output/release-readiness/candidate-build13-local-source-04.json'
    state['phase']='current-candidate-followups'
    queue=json.loads((OUT/'build13-followups.json').read_text());queue=queue if isinstance(queue,list) else [queue]
    state['active_runtime']=next((r['id'] for r in queue if r['status']=='running'),None)
    state['focused_queue']='output/release-readiness/build13-followups.json'
    state['steps']['2'].update(
        terminal='output/release-readiness/readiness-toms-terminal-02/summary.json (passed all 17 named checks on normal build13; full native connected network, components, refresh and source close)',
        vending='output/release-readiness/readiness-build13-vending-01/summary.json (passed on normal build13)',
        link='output/release-readiness/readiness-build13-toms-link-01/summary.json (passed 11 named checks on normal build13)',
        configurator='output/release-readiness/readiness-build13-toms-configurator-01/summary.json (passed 11 named checks on normal build13)',
        drivers='functional-tests/project-everything-regression/toms_release_workflows.py; all four native workflows passed, 49 named focused checks on exact normal build13 subjects')
    tom_runs=['readiness-toms-terminal-02','readiness-build13-vending-01','readiness-build13-toms-link-01','readiness-build13-toms-configurator-01']
    tom_complete=all((OUT/run/'summary.json').exists() and json.loads((OUT/run/'summary.json').read_text())['status']=='passed' for run in tom_runs)
    if tom_complete:state['steps']['2']['status']='complete-focused-26.1.2-fabric'
    state['steps']['2']['new_finding']=dict(id='toms-polymer-client-codec-context',
        reproduction='output/release-readiness/readiness-toms-terminal-01/summary.json',
        cause='Polymer item codec requires the optional Fabric connection PacketContext when encoding the client withdrawal outside native packet serialization.',
        fix='All six Compatibility source families supply the actual client connection around terminal-action encoding; no public API or packet-format change.',
        focused='build/release-readiness/toms-packet-context-unit-01.log',
        green='output/release-readiness/readiness-toms-terminal-02/summary.json',status='fixed-and-focused-pass')
    state['steps']['3']['new_finding'].update(
        additional_candidate='Latest immutable revision reservation, intermediate cursor suppression and native client revision consumption passed 324 focused checks; current-byte cross-target replay in build13-followups.',
        status='Build13 six-target replay passed 1250 focused assertions. Current-byte 26.2 ordinary and the earlier byte-identical-Core 3-trial delayed proof are recorded separately; final exact-byte delayed replay remains queued.')
    final_delayed=ROOT/'output/ae2-click-investigation/readiness-build13-262-delayed-01/run.json'
    if final_delayed.exists():
        delayed=json.loads(final_delayed.read_text())
        if delayed.get('exit_code')==0 and len(delayed.get('trials',[]))==2 and all(r['passed'] and not r['failed'] and not r['entry_only'] and not r.get('portable_lifecycle_missing') for r in delayed['trials']):
            state['steps']['3'].update(status='complete-focused-six-ae2-targets-build13',final_delayed=str(final_delayed.relative_to(ROOT)),live_assertions=1380)
            state['steps']['3']['new_finding']['status']='All six exact-build13 targets passed ordinary and bounded-delay/jitter source/cursor checks: 1380 focused assertions, plus324 validator/publication/gesture tests.'
    state['steps']['4']['source_boundary']='passes: 2063 compatibility classes; no exclusions added'
    state['steps']['7']['normal_build']='build/release-readiness/source-build-13.log (passed all460 tasks and all36 normal jars)'
    state['steps']['8']['integrity']='All36 normal build13 jars passed candidate-integrity-08; final full gameplay remains pending.'
    if not any(r['log'].endswith('source-build-13.log') for r in state['build_attempts']):
        state['build_attempts'].append(dict(log='build/release-readiness/source-build-13.log',status='passed',finding='All36 normal jars after optional Tom client codec context fix; unchanged public ABI and compiler/source/bytecode/archive gates.'))
    state['independent_multiplayer']=dict(
        accepted='output/release-readiness/readiness-multiplayer-modern-13/manifest.json',
        scope='Actual dedicated26.3Fabric server and separate client JVMs: concurrent withdrawal/output, sorting, full typed metadata, owner isolation, native disconnect drop, reconnect/pickup, source invalidation and clean stop.',
        execution_limit='The complete remote campaign used the explicitly recorded HotSpot TieredStopAtLevel=1 diagnostic setting after native JVM access violations in raw-launch profiles. Ordinary current-artifact Core suites and portable-host runs remain separate. Default-compilation multiplayer attempts07/08/11/14 retain native process failures, including a client without SIU before connection. Do not describe this diagnostic campaign as a default-JIT multiplayer pass.',
        native_disconnect='Vanilla26.3 saves a dropped carried stack rather than inserting it in offline Inventory. Attempt12 incorrectly required saved Inventory only; exact native item/drop recovery passed in13.',
        recorder='Atomic observation publication now waits only for transient Windows reader locks; it never resends input. Actual Windows lock/release/permanent-failure test passed in atomic-observation-unit-01.log.')
    state['installation_checks']={}
    for run in ['readiness-install-client-only-02','readiness-install-server-only-04','readiness-install-protocol-04']:
        report=OUT/run/'manifest.json'
        if report.exists():
            data=json.loads(report.read_text());state['installation_checks'][run]=dict(status=data['status'],mode=data.get('mode'),evidence=str(report.relative_to(ROOT)))
    state['steps']['8']['provenance']='output/release-readiness/provenance-build13-01.json (passed all36 exact archives, pixel origins, source notices and audio attribution)'
    state['publisher_campaign_plan']='output/release-readiness/readiness-final-publishers-01/publishers.json (65 planned full-discovery jobs, every original exact publisher binary retained by target; live acceptance pending)'
    core=ROOT/'build/full-validation-20261023/readiness-final-20261106/core/results.json'
    if core.exists():
        core_rows=json.loads(core.read_text())['cases']
        state['phase']='final-complete-core-qualification'
        state['active_runtime']=next((r['key'] for r in core_rows if r['status']=='RUNNING'),None)
        state['steps']['7']['final_frozen_candidate']='Normal build13; complete Core phase under readiness-final-20261106. Every case retains exact input fingerprints. Remaining phases/publisher qualification are separate.'
        state['current_complete_core_counts']={name:sum(r['status']==name for r in core_rows) for name in ['PASS','RUNNING','PENDING','FAIL']}
    remainder=OUT/'remainder-campaign-04.json'
    if remainder.exists():
        later=json.loads(remainder.read_text());active=next((r for r in later if r['status']=='running'),None)
        state['serial_remainder']='output/release-readiness/remainder-campaign-04.json'
        if active:state['phase']='final-'+active['name'];state['active_runtime']=active['name']
        state['current_remainder_counts']={name:sum(r['status']==name for r in later) for name in ['passed','running','failed']}
    path.write_text(json.dumps(state,indent=2)+'\n')
    checklist=ROOT/'RELEASE_READINESS.md';text=checklist.read_text()
    text=text.replace('The latest AE2 work verified the **24 Core/Compatibility jars** and their entries/checksums in the combined archive. Controlify\'s twelve existing archive entries were preserved. A fresh, unified verification of all **36 final release jars** is still required.',
        'The normal build 13 and integrity pass 08 verify all **36 jars** together, their existing public ABI, archive contents and checksums. Final current-artifact gameplay, dedicated-server and independent-review acceptance remains pending.')
    text=text.replace('source-build-10.log','source-build-13.log').replace('Build 10','Build 13').replace('build 10','build 13').replace('integrity pass 06','integrity pass 08').replace('Integrity pass 06','Integrity pass 08').replace('2,057 compatibility classes','2,063 compatibility classes')
    if tom_complete:
        begin=text.index('## 2. Finish the four specific Tom-family workflows');end=text.index('## 3.',begin)
        section=text[begin:end].replace('- [ ]','- [x]')
        section=section.replace('The recovery run on **26.1.2 Fabric** still records these four cases as opening/policy-only.',
            'The recovery run originally recorded four **26.1.2 Fabric** cases as opening/policy-only. Fresh normal-build13 workflows now pass 49 named focused checks across these four interfaces.')
        section=section.replace('Source: `remaining_opening_and_policy_only_cases` in [setup recovery](output/blocked-setup-recovery/summary.json).',
            'Current evidence: [terminal](output/release-readiness/readiness-toms-terminal-02/summary.json), [configurator](output/release-readiness/readiness-build13-toms-configurator-01/summary.json), [link](output/release-readiness/readiness-build13-toms-link-01/summary.json), [customer](output/release-readiness/readiness-build13-vending-01/summary.json). The exact counts and all failed setup/functional attempts are preserved. These are focused workflows on the named target; the wider all-target campaign remains separate. Historical source: `remaining_opening_and_policy_only_cases` in [setup recovery](output/blocked-setup-recovery/summary.json).')
        text=text[:begin]+section+text[end:]
    checklist.write_text(text)
    print('Updated in-progress work journal and current normal-build references; final coverage items remain open.')

if __name__=='__main__':main()
