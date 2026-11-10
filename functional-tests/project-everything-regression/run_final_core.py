"""Wait for the serial publisher sweep, then run Core on the final shipping jars."""
from pathlib import Path
import argparse,json,msvcrt,subprocess,sys,time
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'output/project-everything-regression'

def complete(name,minimum):
    path=OUT/(name+'-integration.json')
    if not path.exists():return False
    try:rows=json.loads(path.read_text())
    except ValueError:return False
    return len(rows)>=minimum and all(r['status']!='running' for r in rows)

def main():
    p=argparse.ArgumentParser();p.add_argument('--after',required=True);p.add_argument('--fill',required=True);p.add_argument('--run',required=True);a=p.parse_args()
    expected=len(json.loads((OUT/'coverage-fill-fixtures.json').read_text()))
    while not complete(a.after,1) or not complete(a.fill,expected):time.sleep(2)
    with (ROOT/'build/project-everything-regression/runtime.lock').open('a+b') as lock:
        while True:
            try:lock.seek(0);msvcrt.locking(lock.fileno(),msvcrt.LK_NBLCK,1);break
            except OSError:time.sleep(2)
        try:
            assert complete(a.after,1) and complete(a.fill,expected)
            result=subprocess.run([sys.executable,str(Path(__file__).with_name('run_core_matrix.py')),'--run',a.run],cwd=ROOT)
        finally:msvcrt.locking(lock.fileno(),msvcrt.LK_UNLCK,1)
    raise SystemExit(result.returncode)
if __name__=='__main__':main()
