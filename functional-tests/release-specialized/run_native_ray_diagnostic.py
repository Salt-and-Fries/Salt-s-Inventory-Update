"""One original hamster entry with read-only ray tracing, not release acceptance."""
from pathlib import Path
import argparse,datetime,hashlib,json,msvcrt,sys,traceback

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from run_release_publishers import plan
from subject_artifacts import current_subjects
from check_mod_interactions import run as check
from run_toms_all import named
from native_ray_diagnostics import enhance


def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--run',required=True);a=p.parse_args()
    assert a.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-' for c in a.run)
    job=next(v for v in plan() if v['id']=='1211-neoforge-c1');target=job['target']
    out=ROOT/'output/release-readiness'/a.run;assert not out.exists();out.mkdir()
    files=[Path(__file__),Path(__file__).with_name('native_ray_diagnostics.py'),Path(__file__).with_name('native_observation_schema.py'),ROOT/job['manifest']]
    from resume_release_publishers import inputs_for
    inputs=inputs_for(target)|{str(v.relative_to(ROOT)):sha(v) for v in files}
    row=dict(status='running',target=target,subjects=current_subjects(target),qa_inputs=inputs,
        scope='Diagnostic original hamster entry; no input/assertion replacement, ray recalculation, screen/provider construction or release acceptance',started=datetime.datetime.now(datetime.timezone.utc).isoformat())
    def save():(out/'summary.json').write_text(json.dumps(row,indent=2)+'\n')
    save()
    with(ROOT/'build/project-everything-regression/runtime.lock').open('a+b')as lock:
        lock.seek(0);lock.write(b'0');lock.flush();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1)
        try:
            result=named(a.run,target,ROOT/job['manifest'],out,check=lambda profile,mc,loader:check(profile,mc,loader,only=('atlas-bb-spawn-hamster',)),enhance=enhance)
            row.update(status='diagnostic-input-passed',results=result)
        except BaseException as error:
            row.update(status='diagnostic-input-failed',error=repr(error),trace=traceback.format_exc());raise
        finally:
            row['finished']=datetime.datetime.now(datetime.timezone.utc).isoformat()
            row['qa_inputs_unchanged']=all(sha(ROOT/v)==h for v,h in inputs.items());row['subjects_unchanged']=current_subjects(target)==row['subjects']
            save();lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)


if __name__=='__main__':main()
