"""Final full-discovery publisher campaign, with explicit native-conflict partitions.

No case limits or mod selections. Failed attempts remain immutable. Resume starts
a new disposable runtime for that job; it never resends a failed menu input.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,msvcrt,subprocess,sys
from run_mod_matrix import FIXTURES,ROOT
from subject_artifacts import current_subjects

PARTITIONS={
    '1211-fabric-themed-v1':'charm-themed-without-aether.json',
    'regression-fill-1211-fabric-remaining-inventory-tail-v2':'charm-tail-without-aether.json',
    'regression-fill-2612-fabric-inventory-batch':'polymer-without-bumblezone.json',
}

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def plan():
    jobs=[]
    for target,fixtures in FIXTURES.items():
        for cohort,name in enumerate(fixtures):
            original=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
            actual=ROOT/'build/blocked-setup-recovery/fixtures'/PARTITIONS[name] if name in PARTITIONS else original
            jobs.append(dict(id=target.replace('/','-').replace('.','')+'-c'+str(cohort),target=target,cohort=cohort,
                original_fixture=name,manifest=str(actual.relative_to(ROOT)),manifest_sha256=digest(actual),
                original_manifest=str(original.relative_to(ROOT)),original_manifest_sha256=digest(original),
                partition=name in PARTITIONS))
    name='regression-fill-2612-fabric-inventory-batch';target='26.1.2/fabric'
    original=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
    actual=ROOT/'build/blocked-setup-recovery/fixtures/bumblezone-without-polymer-v2.json'
    jobs.append(dict(id='2612-fabric-bumblezone-partition',target=target,cohort=FIXTURES[target].index(name),
        original_fixture=name,manifest=str(actual.relative_to(ROOT)),manifest_sha256=digest(actual),
        original_manifest=str(original.relative_to(ROOT)),original_manifest_sha256=digest(original),partition=True))
    name='1211-fabric-themed-v1';target='1.21.1/fabric'
    original=ROOT/'build/all-target-rollout/fixtures'/name/'fixture.json'
    actual=ROOT/'build/blocked-setup-recovery/fixtures/aether-themed-without-charm.json'
    jobs.append(dict(id='1211-fabric-aether-partition',target=target,cohort=FIXTURES[target].index(name),
        original_fixture=name,manifest=str(actual.relative_to(ROOT)),manifest_sha256=digest(actual),
        original_manifest=str(original.relative_to(ROOT)),original_manifest_sha256=digest(original),partition=True))
    # Repartitioning must not silently drop an exact publisher release. One
    # dependency-complete job elsewhere in the same target must retain it.
    for target in FIXTURES:
        required=set();present=set()
        for row in jobs:
            if row['target']!=target:continue
            for key,result in [('original_manifest',required),('manifest',present)]:
                result.update((a['filename'],a['sha512']) for a in json.loads((ROOT/row[key]).read_text())['artifacts'])
        absent=required-present
        assert not absent,(target,'Partition would lose exact upstream binaries',sorted(absent))
    return jobs

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--run',required=True);p.add_argument('--resume',action='store_true');p.add_argument('--plan-only',action='store_true');a=p.parse_args()
    assert a.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in a.run)
    folder=ROOT/'output/release-readiness'/a.run;path=folder/'publishers.json';jobs=plan()
    if path.exists():
        assert a.resume,'Choose a fresh ID, or explicitly resume without erasing failed attempts'
        record=json.loads(path.read_text());assert record['plan']==jobs,'Pinned plan changed; choose a new campaign ID'
    else:
        assert not a.resume;folder.mkdir(parents=True,exist_ok=True)
        record=dict(status='planned',scope='Complete discovered publisher-menu cases in every exact cohort; per-case coverage dispositions remain required.',
            plan=jobs,attempts=[],started=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save():path.write_text(json.dumps(record,indent=2)+'\n')
    save()
    if a.plan_only:print('Planned',len(jobs),'jobs; all original exact publisher binaries retained by target');return
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            record['status']='running';save()
            for job in jobs:
                previous=[v for v in record['attempts'] if v['job']==job['id']]
                if previous and previous[-1]['status']=='passed':
                    old=previous[-1]
                    assert old['subjects']=={k:list(v) for k,v in current_subjects(job['target']).items()},'Previously passed subject changed; choose a fresh campaign'
                    assert all(digest(ROOT/q)==sha for q,sha in old['qa_inputs'].items()),'Previously passed QA input changed; choose a fresh campaign'
                    continue
                name=a.run+'-'+job['id']+'-a'+str(len(previous)+1)
                for key in ['manifest','original_manifest']:assert digest(ROOT/job[key])==job[key+'_sha256']
                production=job['target'].endswith(('/fabric','/forge'))
                script='run_production_matrix.py' if production else 'run_mod_matrix.py'
                args=[sys.executable,str(Path(__file__).with_name(script)),'--run',name,'--target',job['target'],'--cohort',str(job['cohort'])]
                if job['partition']:
                    assert production;args+=['--fixture-manifest',str(ROOT/job['manifest'])]
                log=ROOT/'build/release-readiness'/(name+'.log');assert not log.exists()
                inputs={str(q.relative_to(ROOT)):digest(q) for q in Path(__file__).parent.iterdir() if q.is_file() and q.suffix in {'.py','.txt','.inc','.java','.gradle'}}
                attempt=dict(job=job['id'],run=name,status='running',arguments=args[1:],log=str(log.relative_to(ROOT)),
                    subjects=current_subjects(job['target']),qa_inputs=inputs,started=datetime.datetime.now(datetime.timezone.utc).isoformat())
                record['attempts'].append(attempt);save();print('START',name,flush=True)
                with log.open('x') as stream:completed=subprocess.run(args,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT)
                suffix='-production-mod-matrix.json' if production else '-mod-matrix.json'
                matrix=ROOT/'output/project-everything-regression'/(name+suffix)
                if matrix.exists():attempt['results']=json.loads(matrix.read_text())
                unchanged=all(digest(ROOT/q)==sha for q,sha in inputs.items())
                same_subjects=current_subjects(job['target'])==attempt['subjects']
                attempt.update(status='passed' if completed.returncode==0 and unchanged and same_subjects else 'failed',exit_code=completed.returncode,
                    qa_inputs_unchanged=unchanged,subjects_unchanged=same_subjects,finished=datetime.datetime.now(datetime.timezone.utc).isoformat())
                save();print(attempt['status'],name,flush=True)
                if attempt['status']!='passed':record['status']='failed';save();raise RuntimeError('Preserved failed publisher job '+job['id'])
            record['status']='complete-discovery-results-need-case-disposition';save()
        finally:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)

if __name__=='__main__':main()
