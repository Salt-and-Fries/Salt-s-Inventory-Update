"""Summarize measured coverage without turning missing scenarios into passes."""
from pathlib import Path
from collections import Counter, defaultdict
import argparse, csv, hashlib, json, re, tomllib, zipfile
from subject_artifacts import current_subjects,subjects_match,subject_signature
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'output/project-everything-regression'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()

def complete_matrix(headless,targets,totals,reconciliation_errors):
    return (headless.get('status')=='passed' and not reconciliation_errors
            and totals['passed_cohorts']==totals['planned_cohorts']
            and not totals['failed_cohorts'] and not totals['failed_cases'] and not totals['failed_checks']
            and not totals.get('unverified_cases',0)
            and all(r['core_full_suite']=='passed' and r['core_full_suite_matches_current_jar'] and not r['missing_cohorts'] for r in targets))

def audit_status(matrix_complete,repository_checks):
    return ('complete-measured-matrix' if matrix_complete and all(r.get('status')=='passed' for r in repository_checks)
            else 'incomplete-or-needs-attention')

def qualify_case(case,subjects_current=True):
    if not subjects_current and case.get('status') in {'passed','entry-and-policy-only'}:
        return {**case,'status':'needs-observer-reproduction','original_status':case['status'],
                'verification_note':'The shipping Core or compatibility jar changed after this attempt. Require matching full-cohort evidence on the current subjects.'}
    if (case.get('status')=='passed' and case.get('coverage','').startswith('Reviewed native-only')
            and not case.get('native_screen_roundtrip')):
        return {**case,'status':'needs-observer-reproduction','original_status':case['status'],
                'verification_note':'Earlier native-screen proof did not assert that Escape changed the screen and that reopening returned to the exact screen class. Require that stronger reproduction.'}
    if (case.get('status')=='passed' and not case.get('virtual_policy_query_contract')
            and any(isinstance(v,str) and v.startswith('moze_intel.projecte.') for v in case.get('native_screens',[]))):
        return {**case,'status':'needs-observer-reproduction','original_status':case['status'],
                'verification_note':'The earlier observer queried ProjectE ghost-slot mayPlace, which can mutate its filter. Preserve the raw attempt but require a clean read-only-observer reproduction.'}
    return case

def interaction_pair(case):
    """Count measured source-to-storage interactions, never mere co-loading."""
    recorded_partner=case.get('cross_mod_partner')
    partner=recorded_partner if isinstance(recorded_partner,str) else recorded_partner.get('block','') if isinstance(recorded_partner,dict) else ''
    if case.get('status')!='passed' or ':' not in partner:return None
    source=case.get('mod_id')
    if not source:
        # Atlas histories can retain an earlier mod's setup before the actual
        # item/entity entry. The final native entry owns this interaction.
        # Keep addon item ownership even when its menu type belongs to AE2.
        for action in reversed(case.get('actions',[])):
            names=[str(action.get(k,'')) for k in ['block','item','entity']]
            names+=re.findall(r'\b[a-z0-9_.-]+:[a-z0-9_./-]+',str(action.get('command','')))
            source=next((v.split(':')[0] for v in names if ':' in v and not v.startswith(('minecraft:','salts_inventory_'))),None)
            if source:break
    if not source:
        source=next((v.split(':')[0] for v in case.get('menu_ids',[]) if isinstance(v,str) and ':' in v
                     and not v.startswith(('minecraft:','salts_inventory_'))),None)
    return (source,partner.split(':')[0]) if source else None

def publisher_artifact_signature(artifacts):
    # Old profiles may list one identical API file twice. New profiles retain
    # both declared roles on a single physical file; their binary identity is
    # unchanged. Different hashes or filenames still never reconcile.
    return sorted({(family,r.get('filename',''),r.get('sha512',r.get('sha256','')))
                   for r in artifacts for family in r.get('also_families',[r.get('family','')])
                   if family not in ['core','compatibility','qa']})

def select_attempts(attempts):
    """Retain history, but apply only replays following the latest whole run."""
    latest={};smoke=[];reproductions=[];active=defaultdict(list)
    for record in attempts:
        key=(record['target'],record['fixture'])
        if record.get('scope')=='smoke':
            smoke.append(record)
        elif record.get('scope')=='case-reproduction':
            reproductions.append(record)
            active[key].append(record)
        else:
            latest[key]=record
            active[key]=[]
    return latest,smoke,reproductions,dict(active)


def mod_ids(file):
    try:
        with zipfile.ZipFile(file) as jar:
            result=set()
            if 'fabric.mod.json' in jar.namelist():
                try:
                    # Gson accepts publisher descriptions containing literal
                    # newlines. Do not lose their mod identity in the report.
                    d=json.loads(jar.read('fabric.mod.json'),strict=False);result.update({d['id'],*d.get('provides',[])})
                except (ValueError,KeyError):pass
            for name in ['META-INF/neoforge.mods.toml','META-INF/mods.toml']:
                if name in jar.namelist():
                    raw=jar.read(name).decode('utf-8-sig')
                    try:result.update(r['modId'] for r in tomllib.loads(raw)['mods'])
                    except (ValueError,KeyError):result.update(re.findall(r'^\s*modId\s*=\s*[\"\']([a-z][a-z0-9_-]*)[\"\']',raw,re.M))
            return result
    except (OSError,KeyError,ValueError,zipfile.BadZipFile): pass
    return set()

def main():
    p=argparse.ArgumentParser();p.add_argument('--integration',action='append',default=[]);p.add_argument('--core',type=Path);a=p.parse_args()
    from run_mod_matrix import FIXTURES
    headless=json.loads((OUT/'headless-results-final.json').read_text())
    for r in headless['projects']:
        for file in r['xml_files']: assert sha(ROOT/file['file'])==file['sha256'],'JUnit evidence changed: '+file['file']
    core=json.loads(a.core.read_text()) if a.core else json.loads((OUT/'core-results.json').read_text())
    core_runs=core['runs'] if isinstance(core,dict) else core
    attempts=[]
    for name in a.integration:
        source=OUT/(name+'-integration.json');data=json.loads(source.read_text())
        for record in data:
            record['fixture']={'1201-fabric-regression-inventory-no-guard':'1201-fabric-inventory-batch-v2'}.get(record['fixture'],record['fixture'])
            attempts.append({**record,'matrix':str(source.relative_to(ROOT))})
    # Arguments are chronological. Keep the last attempt for each exact cohort,
    # retaining every earlier attempt separately for failure provenance.
    latest,smoke,reproductions,active_reproductions=select_attempts(attempts)
    from reconcile_results import merge,identity
    def publisher_signature(profile):
        artifacts=json.loads((profile/'staged-artifacts.json').read_text())['artifacts']
        return publisher_artifact_signature(artifacts)
    target_rows=[];coverage=defaultdict(lambda:Counter()); details=[];reconciliation_errors=[];resolutions=[];pairs=defaultdict(lambda:Counter());runtime_failures=[]
    for target,fixtures in FIXTURES.items():
        mc,loader=target.split('/');runs=[r for (t,_),r in latest.items() if t==target]
        expected_subjects=current_subjects(target)
        full=next((r for r in core_runs if r['minecraft']==mc and r['loader']==loader),{})
        filename=f'salts_inventory_update-{mc}-{"NeoForge" if loader=="neoforge" else loader.title()}-0.1.6.jar'
        current_sha=sha(ROOT/'dist/core'/filename)
        old_sha=full.get('shipping_sha256',full.get('staged',{}).get('sha256'))
        row={'target':target,'planned_cohorts':len(fixtures),'executed_cohorts':len(runs),'core_full_suite':full.get('status','not-run'),
             'core_full_suite_matches_current_jar':old_sha==current_sha,'current_core_sha256':current_sha,
             'passed_cohorts':0,'failed_cohorts':0,'running_cohorts':0,
             'checks':0,'failed_checks':0,'passed_cases':0,'entry_only':0,'failed_cases':0,'unverified_cases':0,'retired_invalid_cases':0,'missing_cohorts':[f for f in fixtures if (target,f) not in latest]}
        junit=[r for r in headless['projects'] if r['project'].replace('\\','/').endswith('/'+mc+'/fabric')]
        row['headless_tests']=sum(r['tests'] for r in junit)
        for run in runs:
            effective=[]
            for cohort in run.get('results',[]):
                profile=ROOT/cohort.get('profile','');report=profile/'mod-interaction-checks.json'
                staged=profile/'staged-artifacts.json';installed=set();artifacts=[]
                if staged.exists():
                    artifacts=json.loads(staged.read_text()).get('artifacts',[])
                    for artifact in artifacts:
                        file=Path(artifact['file']);installed.update(mod_ids(file))
                    for name in installed: coverage[(target,name)]['loaded_cohorts']+=1
                if cohort.get('error') or cohort.get('exit_code') not in (None,0) or not report.exists():
                    runtime_failures.append({'target':target,'fixture':run['fixture'],'profile':str(profile.relative_to(ROOT)),
                        'exit_code':cohort.get('exit_code'),'error':cohort.get('startup_failure') or cohort.get('error') or
                        ('No case report was produced.' if not report.exists() else 'Runtime exited unsuccessfully after testing.')})
                if not report.exists():effective.append('failed');continue
                result=json.loads(report.read_text())
                clean_reproductions=True
                source_complete=bool(result['cases']) and not cohort.get('error') and cohort.get('exit_code')==0 and cohort.get('results',{}).get('cases')==len(result['cases'])
                source_complete &= set(result.get('selected_case_ids',[])).issubset(c['id'] for c in result['cases'])
                for attempt in active_reproductions.get((target,run['fixture']),[]):
                    for replay in attempt.get('results',[]):
                        replay_profile=ROOT/replay.get('profile','');replay_report=replay_profile/'mod-interaction-checks.json'
                        if not replay_report.exists():continue
                        if replay.get('error') or replay.get('exit_code')!=0:
                            clean_reproductions=False
                            reconciliation_errors.append({'target':target,'fixture':run['fixture'],'error':'Reproduction runtime did not finish cleanly; its partial results were not merged.','report':str(replay_report.relative_to(ROOT))});continue
                        if publisher_signature(profile)!=publisher_signature(replay_profile):
                            clean_reproductions=False
                            reconciliation_errors.append({'target':target,'fixture':run['fixture'],'error':'Publisher/runtime binaries differ; targeted results were not merged.','report':str(replay_report.relative_to(ROOT))});continue
                        replay_artifacts=json.loads((replay_profile/'staged-artifacts.json').read_text())['artifacts']
                        if subject_signature(artifacts)!=subject_signature(replay_artifacts):
                            clean_reproductions=False
                            reconciliation_errors.append({'target':target,'fixture':run['fixture'],'error':'Shipping subjects differ; a full-cohort run is required instead of a targeted merge.','report':str(replay_report.relative_to(ROOT))});continue
                        replay_data=json.loads(replay_report.read_text())
                        measured={identity(c['id']) for c in replay_data['cases']}
                        rejected={identity(c['id']) for c in replay_data.get('rejected_reviewed_workflows',[])}
                        missing=[v for v in attempt['only'] if identity(v) not in measured|rejected]
                        if missing:
                            clean_reproductions=False
                            reconciliation_errors.append({'target':target,'fixture':run['fixture'],'error':'Requested case has no measurement or proven fixture rejection.','missing':missing,'report':str(replay_report.relative_to(ROOT))})
                        result=merge(result,replay_data,attempt['only'],str(replay_report.relative_to(ROOT)))
                resolutions.extend({'target':target,'fixture':run['fixture'],**r} for r in result.get('reconciliations',[]))
                result['cases']=[{**qualify_case(c,subjects_match(artifacts,expected_subjects)),
                                  'subjects_match_current_jars':subjects_match(artifacts,expected_subjects)} for c in result['cases']]
                row['checks']+=len(result['checks']);row['failed_checks']+=sum(not x['passed'] for x in result['checks'])
                counts=Counter(c['status'] for c in result['cases'])
                row['passed_cases']+=counts['passed'];row['entry_only']+=counts['entry-and-policy-only'];row['failed_cases']+=counts['failed']
                row['unverified_cases']+=counts['needs-observer-reproduction']
                row['retired_invalid_cases']+=counts['invalid-fixture-workflow']+counts['superseded-duplicate-entry']
                effective.append('passed' if source_complete and clean_reproductions and not counts['failed'] and not counts['running'] and not counts['needs-observer-reproduction'] and all(c['passed'] for c in result['checks']) else 'failed')
                for c in result['cases']:
                    owners={v.split(':')[0] for v in c.get('menu_ids',[]) if isinstance(v,str) and ':' in v}
                    if c.get('mod_id'):owners.add(c['mod_id'])
                    for action in c['actions']:
                        owners.update(v.split(':')[0] for k,v in action.items() if k in ['block','item'] and ':' in str(v))
                        owners.update(re.findall(r'\b([a-z0-9_.-]+):[a-z0-9_./-]+',str(action.get('command',''))))
                    owners.intersection_update(installed)
                    pair=interaction_pair(c)
                    if pair:
                        source,destination=pair
                        if source in installed and destination in installed|{'minecraft'}:
                            measured=pairs[(target,source,destination)];measured['passed_scenarios']+=1
                            for field in ['sort_partner_checked','native_drag_partner_checked','native_partner_reopen_checked']:
                                measured[field]+=bool(c.get(field))
                            if destination!='minecraft':owners.add(destination)
                    for name in owners-{'minecraft','salts_inventory_update','salts_inventory_compat'}:
                        coverage[(target,name)][c['status']]+=1
                    measured_report=c.get('reproduction_report') or c.get('previous_attempt',{}).get('reproduction_report') or str(report.relative_to(ROOT))
                    details.append({'target':target,'fixture':run['fixture'],'report':measured_report,
                                    'publisher_sha256':[a.get('sha256') for a in artifacts if a.get('family') not in ['core','compatibility','qa']],**c})
            run['effective_status']='running' if run['status']=='running' else 'passed' if effective and all(s=='passed' for s in effective) else 'failed'
            row['passed_cohorts']+=run['effective_status']=='passed';row['failed_cohorts']+=run['effective_status']!='passed'
            if run['effective_status']=='running':row['running_cohorts']+=1;row['failed_cohorts']-=1
        target_rows.append(row)
    with (OUT/'mod-coverage.csv').open('w',newline='',encoding='utf-8') as f:
        writer=csv.writer(f);writer.writerow(['target','mod_id','loaded_cohorts','passed_cases','entry_and_policy_only','failed_cases','needs_observer_reproduction'])
        for (target,name),count in sorted(coverage.items()):writer.writerow([target,name,count['loaded_cohorts'],count['passed'],count['entry-and-policy-only'],count['failed'],count['needs-observer-reproduction']])
    with (OUT/'interaction-pairs.csv').open('w',newline='',encoding='utf-8') as f:
        writer=csv.writer(f);writer.writerow(['target','source_mod','partner_mod','kind','passed_scenarios','sort_scenarios','drag_scenarios','populated_reopen_scenarios'])
        for (target,source,destination),count in sorted(pairs.items()):
            writer.writerow([target,source,destination,'mod-to-vanilla' if destination=='minecraft' else 'same-mod' if source==destination else 'cross-mod',
                count['passed_scenarios'],count['sort_partner_checked'],count['native_drag_partner_checked'],count['native_partner_reopen_checked']])
    with (OUT/'case-results.csv').open('w',newline='',encoding='utf-8') as f:
        columns=['target','fixture','id','status','coverage','item_serialization_contract','report']
        writer=csv.DictWriter(f,fieldnames=columns,extrasaction='ignore');writer.writeheader();writer.writerows(details)
    from entry_gap_coverage import classify
    atlas_gaps=json.loads((OUT/'native-entry-cases.json').read_text()).get('specialized_entry_gaps',[])
    entry_gap_rows=[classify(gap,details) for gap in atlas_gaps]
    with (OUT/'specialized-entry-coverage.csv').open('w',newline='',encoding='utf-8') as f:
        columns=['target','ui','screen','status','case','coverage','report']
        writer=csv.DictWriter(f,fieldnames=columns);writer.writeheader();writer.writerows(entry_gap_rows)
    unresolved=[c for c in details if c['status'] in ['failed','needs-observer-reproduction']]
    failure_text=['# Cases awaiting resolution','',
        'These cases remain flagged until a matching reproduction passes. Some flags identify test setup or observation problems; the original attempts remain preserved. This list is not a claim that every flag is an SIU defect.','',
        'See [changes and interpretation](changes-and-limits.md) for confirmed product defects, SIU-absent native controls and coverage limits.','',
        '| Target | Case | Latest measured problem |','|---|---|---|']
    for c in unresolved:
        problem=str(c.get('error',c.get('verification_note','Missing verification'))).replace('\n',' ').replace('|','\\|')
        if len(problem)>240:problem=problem[:237]+'...'
        failure_text.append(f"| {c['target']} | `{c['id']}` | {problem} |")
    if not unresolved:failure_text+=['','No unresolved case-level flags in the measured reports. Cohort startup, repository checks and untested coverage still appear in the main report.']
    if runtime_failures:
        failure_text+=['','## Startup and exit failures','','These runs do not become passing menu tests merely because no case could start.','',
            '| Target | Configuration | Runtime problem |','|---|---|---|']
        for failure in runtime_failures:
            problem=str(failure['error']).replace('\n',' ').replace('|','\\|')
            failure_text.append(f"| {failure['target']} | `{failure['fixture']}` | {problem} |")
    (OUT/'failures.md').write_text('\n'.join(failure_text)+'\n')
    totals={key:sum(r[key] for r in target_rows) for key in ['planned_cohorts','executed_cohorts','passed_cohorts','failed_cohorts','running_cohorts','checks','failed_checks','passed_cases','entry_only','failed_cases','unverified_cases','retired_invalid_cases']}
    matrix_complete=complete_matrix(headless,target_rows,totals,reconciliation_errors)
    repository_checks=[json.loads(p.read_text()) for p in [OUT/'source-boundary-audit.json'] if p.exists()]
    summary={'status':audit_status(matrix_complete,repository_checks),'measured_matrix_complete':matrix_complete,'additional_repository_checks':repository_checks,
             'headless':headless,'targets':target_rows,'totals':totals,'attempts':attempts,'platform_smoke_attempts':smoke,'case_reproductions':reproductions,
             'reconciliations':resolutions,'reconciliation_errors':reconciliation_errors,'runtime_failures':runtime_failures,'cases':details,
             'specialized_entry_coverage':dict(Counter(r['status'] for r in entry_gap_rows)),
             'coverage_limit':'A passing case proves its recorded assertions only. Entry-only interfaces, missing cohorts and unimplemented recipe/upgrade scenarios are not counted as full functional passes.'}
    (OUT/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    text=['# Automated functional regression results','',f"Status: **{summary['status']}**.",'',f"Headless: **{headless['tests']} tests, {headless['status']}**.",'',
          '| Minecraft / loader | Headless tests | Complete Core suite | Mixed cohorts run / planned | Live checks | Failed cases | Needs repeat | Entry only |',
          '|---|---:|---|---:|---:|---:|---:|---:|']
    for r in target_rows:
        core_label=r['core_full_suite']+(' (before later focused regression fixes)' if not r['core_full_suite_matches_current_jar'] else '')
        text.append(f"| {r['target']} | {r['headless_tests'] if r['target'].endswith('/fabric') else 'shared mapped tests'} | {core_label} | {r['executed_cohorts']} / {r['planned_cohorts']} | {r['checks']} | {r['failed_cases']} | {r['unverified_cases']} | {r['entry_only']} |")
    text+=['','The mixed-mod reports retain the exact block/item entry actions, native menu class, sampled slots, authoritative item/data assertions and each failure snapshot.',
           'See [changes and interpretation](changes-and-limits.md) for the tested fixes, native control comparisons and remaining product defects.',
           '',summary['coverage_limit'],'','See [unresolved cases](failures.md), [every measured case](case-results.csv), [summary.json](summary.json), [mod-coverage.csv](mod-coverage.csv), [measured interaction pairs](interaction-pairs.csv), [specialized child-entry coverage](specialized-entry-coverage.csv), and the [test instructions](../../functional-tests/project-everything-regression/README.md).','',
           'Specialized entries from the original UI atlas: '+', '.join(f'{n} {status}' for status,n in summary['specialized_entry_coverage'].items())+'. An entry or a passed return scenario does not certify every setting or upgrade.','']
    for check in repository_checks:
        if check['status']!='passed':text+=['',f"Additional repository check: **{check['status']}**; {check.get('count',0)} source-boundary violations. See [source-boundary-audit.json](source-boundary-audit.json). No rule was relaxed to hide this failure.",'']
    (OUT/'report.md').write_text('\n'.join(text))
    print(json.dumps(totals));print(summary['status'])

if __name__=='__main__': main()
