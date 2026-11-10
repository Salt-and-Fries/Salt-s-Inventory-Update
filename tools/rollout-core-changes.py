"""Three-way merge reviewed 1.20.1 Core changes onto existing version ports.

Never replaces version-specific code wholesale. Conflicting proposals stay in
build/all-target-rollout/core-conflicts for review; original bytes are backed up.
"""
from pathlib import Path
import subprocess,json,hashlib

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/all-target-rollout'
BASE='versions/1.20.1/'
VERSIONS=['1.21.1','1.21.11','26.1.2','26.2','26.3']

def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
def main():
    changed=git('diff','--name-only','--','versions/1.20.1').decode().splitlines()
    added=git('ls-files','--others','--exclude-standard','--','versions/1.20.1').decode().splitlines()
    report=[]
    for source_name in changed+added:
        if '/tests/' in source_name:continue
        source=ROOT/source_name
        if not source.is_file():continue
        relative=source_name[len(BASE):]
        current=source.read_bytes().replace(b'\r\n',b'\n')
        old=git('show','HEAD:'+source_name).replace(b'\r\n',b'\n') if source_name in changed else b''
        for version in VERSIONS:
            name=f'versions/{version}/{relative}';target=ROOT/name
            backup=WORK/'baseline'/name
            if not backup.exists():
                backup.parent.mkdir(parents=True,exist_ok=True)
                backup.write_bytes(target.read_bytes() if target.exists() else b'')
            row={'source':source_name,'target':name}
            if not old:
                if target.exists():row['status']='existing-new-file-review'
                else:
                    target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(current);row['status']='new-file'
            elif not target.exists():row['status']='missing-target-review'
            else:
                folder=WORK/'merge-inputs'/version/relative;folder.mkdir(parents=True,exist_ok=True)
                ours=folder/'target';base=folder/'base';theirs=folder/'reviewed'
                ours.write_bytes(target.read_bytes().replace(b'\r\n',b'\n'));base.write_bytes(old);theirs.write_bytes(current)
                merge=subprocess.run(['git','merge-file','-p','--diff3',str(ours),str(base),str(theirs)],cwd=ROOT,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
                if merge.returncode==0:
                    target.write_bytes(merge.stdout);row['status']='merged'
                elif merge.returncode>0 and merge.returncode<128:
                    conflict=WORK/'core-conflicts'/version/relative;conflict.parent.mkdir(parents=True,exist_ok=True);conflict.write_bytes(merge.stdout)
                    row.update(status='conflict-review',conflicts=merge.stdout.count(b'<<<<<<<'),proposal=str(conflict.relative_to(ROOT)))
                else:raise RuntimeError(merge.stderr.decode())
            report.append(row)
    (WORK/'core-merge-report.json').write_text(json.dumps(report,indent=2))
    from collections import Counter
    print(json.dumps(Counter(r['status'] for r in report),indent=2))
    for row in report:
        if 'review' in row['status']:print(row['target'],row.get('conflicts',''),row['status'])

if __name__=='__main__':main()
