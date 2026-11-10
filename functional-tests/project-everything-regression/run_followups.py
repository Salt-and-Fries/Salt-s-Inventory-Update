"""Reproduce flagged cases with corrected fixtures, then cover missing publishers."""
from pathlib import Path
import argparse,json,msvcrt,subprocess,sys,time
from run_mod_matrix import FIXTURES,ROOT
from reconcile_results import identity
from subject_artifacts import current_subjects,subjects_match
OUT=ROOT/'output/project-everything-regression'

def revision_requires_retest(fixture,artifacts):
    """A complete attempt already using revised pins need not replay again."""
    if not fixture.get('regression_requires_full_retest'):return False
    actual={(a.get('filename'),a.get('sha512')) for a in artifacts if a.get('sha512')}
    expected={(a['filename'],a['sha512']) for a in fixture['artifacts']}
    if not expected or not expected.issubset(actual):return True
    api=fixture.get('runtime',{}).get('fabric_api')
    if api and not any(a.get('family')=='runtime-api' and a.get('filename')=='fabric-api-'+api+'.jar'
                       and a.get('sha512') for a in artifacts):return True
    return False

def main():
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--run',required=True);p.add_argument('--fill-run');p.add_argument('--plan-only',action='store_true')
    p.add_argument('--failed-cases-only',action='store_true',help='Replay only recorded failed cases on unchanged subjects; a changed subject still requires a whole cohort.')
    p.add_argument('--include-bound-slot-gaps',action='store_true',help='Also replay limited-barrel opening-only checks made before the observer used public custom slot bindings.')
    p.add_argument('--wait-for-runtime',action='store_true',help='Wait for the exclusive serial client lock instead of failing if another queue owns it.')
    a=p.parse_args()
    original=OUT/(a.source+'-integration.json');rows=[];report=OUT/(a.run+'-integration.json')
    assert not report.exists(),'Preserve evidence: choose a fresh run ID'
    jobs=[]
    for base in json.loads(original.read_text()):
        target=base['target'];fixture={'1201-fabric-regression-inventory-no-guard':'1201-fabric-inventory-batch-v2'}.get(base['fixture'],base['fixture'])
        child=next((r for r in base.get('results',[]) if r.get('target')==target),{})
        path=ROOT/child.get('profile','')/'mod-interaction-checks.json';only=[];whole=not path.exists() or bool(child.get('error'))
        fixture_data=json.loads((ROOT/'build/all-target-rollout/fixtures'/fixture/'fixture.json').read_text())
        staged=path.parent/'staged-artifacts.json'
        actual=json.loads(staged.read_text())['artifacts'] if staged.exists() else []
        whole|=revision_requires_retest(fixture_data,actual)
        whole|=not subjects_match(actual,current_subjects(target))
        if path.exists():
            checks=json.loads(path.read_text());ran={c['id'] for c in checks['cases']}
            whole|=not checks['cases']
            whole|=bool(set(checks.get('selected_case_ids',[]))-ran)
            only=[c['id'] for c in checks['cases'] if c['status']=='failed'
                  or any(isinstance(v,str) and v.startswith('moze_intel.projecte.') for v in c.get('native_screens',[])) and not c.get('virtual_policy_query_contract')
                  or any(v['type']=='open' for v in c.get('actions',[]))
                  or identity(c['id'])=='ae2-terminal' and not c.get('ae2_repository_checked')
                  or c['status']=='entry-and-policy-only' and any(v['type']=='regression-native-widget' for v in c.get('actions',[])) and not c.get('native_child_return_checked')
                  or c['status']=='entry-and-policy-only' and c['id'].startswith('sophisticated-limited-barrel')]
            only.extend(c['id'] for c in checks['cases'] if c['status']=='passed' and c.get('coverage','').startswith('Reviewed native-only') and not c.get('native_screen_roundtrip'))
            if not any(c.get('native_drag_partner_checked') for c in checks['cases']):
                candidate=next((c for c in checks['cases'] if c.get('cross_mod_partner') and c.get('amount',0)>=3),None)
                if candidate:only.append(candidate['id'])
            if not any(c.get('native_partner_reopen_checked') for c in checks['cases']):
                candidate=next((c for c in checks['cases'] if c.get('cross_mod_partner')),None)
                if candidate and candidate['id'] not in only:only.append(candidate['id'])
            profile=path.parent;staged=profile/'staged-artifacts.json';catalog=profile/'catalog.json'
            if staged.exists() and catalog.exists():
                hashes={r.get('sha256') for r in json.loads(staged.read_text())['artifacts']}
                registered=json.loads(catalog.read_text());known={identity(v) for v in ran}
                for entry in json.loads((OUT/'native-entry-cases.json').read_text())['cases']:
                    if (entry['minecraft'],entry['loader'])!=tuple(target.split('/')) or not hashes.intersection(entry['publisher_sha256']):continue
                    if any(a[k] not in registered['blocks' if k=='block' else 'items'] for a in entry['actions'] for k in ['item','block'] if k in a):continue
                    if identity(entry['id']) not in known:
                        only.append(entry['id']);known.add(identity(entry['id']))
                from collect_results import mod_ids
                from equipment_checks import cases as equipment_cases
                installed=set().union(*(mod_ids(Path(r['file'])) for r in json.loads(staged.read_text())['artifacts'] if r.get('file')))
                for entry in equipment_cases(installed,registered['items']):
                    if identity(entry['id']) not in known:only.append(entry['id']);known.add(identity(entry['id']))
                from entity_inventory_cases import cases as entity_inventory_cases
                entity_catalog=profile/'regression-catalog.json'
                entity_origin=json.loads(entity_catalog.read_text())['origin'] if entity_catalog.exists() else [8,65,8]
                gaps=json.loads((OUT/'native-entry-cases.json').read_text()).get('specialized_entry_gaps',[])
                for entry in entity_inventory_cases(*target.split('/'),installed,set(registered['items']),set(registered['menus']),
                        entity_origin,gaps,hashes):
                    if identity(entry['id']) not in known:only.append(entry['id']);known.add(identity(entry['id']))
            if a.failed_cases_only:only=[c['id'] for c in checks['cases'] if c['status']=='failed']
            if a.include_bound_slot_gaps:
                only.extend(c['id'] for c in checks['cases'] if c['status']=='entry-and-policy-only'
                    and c.get('slot_access_contract')!='public-desktop-menu-bindings'
                    and 'net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu' in c.get('native_menus',[])
                    and c['id'] not in only)
        if whole or only:jobs.append((target,fixture,[] if whole else only))
    priority=['regression-fill-1211-neoforge-create-and-ae2-v4','regression-fill-1201-forge-specialized-v2',
              'regression-fill-1201-fabric-remaining-inventory-tail-v1','regression-fill-1211-fabric-remaining-inventory-tail-v2',
              'regression-fill-12111-fabric-inventory-batch-v2',
              'regression-recent-20-1211-neoforge','regression-recent-21-2612-neoforge','regression-recent-06-1211-neoforge',
              'regression-26.1.2-neoforge-integrated-ssn-implemented-04','regression-recent-05-1201-forge','263-fabric-actions']
    jobs.sort(key=lambda job:priority.index(job[1]) if job[1] in priority else len(priority))
    (OUT/(a.run+'-plan.json')).write_text(json.dumps([{'target':target,'fixture':fixture,'only':only,
        'scope':'case-reproduction' if only else 'complete-discovery-pass'} for target,fixture,only in jobs],indent=2)+'\n')
    if a.plan_only:
        print(len(jobs),'cohorts require follow-up;',sum(not only for _,_,only in jobs),'require full replay')
        return
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0)
        while True:
            try:msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1);break
            except OSError:
                if not a.wait_for_runtime:raise
                time.sleep(2)
        for index,(target,fixture,only) in enumerate(jobs):
            name=a.run+'-j'+str(index);cohort=FIXTURES[target].index(fixture)
            production=target.endswith(('/fabric','/forge'));script='run_production_matrix.py' if production else 'run_mod_matrix.py'
            args=[sys.executable,str(Path(__file__).with_name(script)),'--run',name,'--target',target,'--cohort',str(cohort)]
            if production and fixture in ['1201-forge-themed-v1','1211-fabric-themed-v1']:args.append('--desktop-debug')
            for case in only:args+=['--only',case]
            log=ROOT/'build/project-everything-regression'/(name+'.log')
            row={'target':target,'fixture':fixture,'cohort':cohort,'scope':'case-reproduction' if only else 'complete-discovery-pass',
                 'only':only,'source_matrix':str(original.relative_to(ROOT)),'status':'running','log':str(log.relative_to(ROOT))};rows.append(row)
            report.write_text(json.dumps(rows,indent=2));print('RETRY',target,fixture,len(only) if only else 'whole cohort',flush=True)
            with log.open('w') as stream:result=subprocess.run(args,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT)
            row.update(status='passed' if result.returncode==0 else 'failed',exit_code=result.returncode)
            matrix=OUT/(name+('-production-mod-matrix.json' if production else '-mod-matrix.json'))
            if matrix.exists():row['results']=json.loads(matrix.read_text())
            report.write_text(json.dumps(rows,indent=2));print('RETRY END',target,row['status'],flush=True)
        msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)
    if a.fill_run:
        args=[sys.executable,str(Path(__file__).with_name('run_everything.py')),'--run',a.fill_run]
        for r in json.loads((OUT/'coverage-fill-fixtures.json').read_text()):args+=['--fixture',r['fixture']]
        with (ROOT/'build/project-everything-regression'/('fill-'+a.fill_run+'.log')).open('w') as stream:
            subprocess.run(args,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT)

if __name__=='__main__':main()
