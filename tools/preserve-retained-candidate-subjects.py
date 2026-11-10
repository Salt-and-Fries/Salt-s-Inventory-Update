"""Keep exact retained SIU feature subjects outside disposable runtime caches."""
from pathlib import Path
import argparse,hashlib,json,shutil

ROOT=Path(__file__).resolve().parents[1]


def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--id',required=True);a=p.parse_args()
    assert a.id and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in a.id)
    folder=ROOT/'output/release-readiness'/('retained-subject-copies-'+a.id);assert not folder.exists();folder.mkdir()
    rows=[]
    for family in ['mi','powah']:
        audit=ROOT/'output/release-readiness'/('retained-'+family+'-baseline-build13-01.json')
        for row in json.loads(audit.read_text())['targets']:
            old=ROOT/row['old_archive'];assert sha(old)==row['baseline_sha256']
            target=folder/(row['baseline_sha256']+'-'+old.name)
            if not target.exists():shutil.copyfile(old,target)
            assert sha(target)==row['baseline_sha256']
            rows.append(dict(family=family,target=row['target'],sha256=row['baseline_sha256'],
                original=str(old),retained_copy=str(target.relative_to(ROOT)),
                audit=dict(file=str(audit.relative_to(ROOT)),sha256=sha(audit))))
    (folder/'manifest.json').write_text(json.dumps(dict(status='retained-bytes-preserved',subjects=rows,
        scope='Exact old SIU Compatibility subjects for local baseline/equivalence review. Not current candidate jars, not new gameplay and not staged to public dist.'),indent=2)+'\n')
    print('Preserved',len(rows),'retained subjects:',folder)


if __name__=='__main__':main()
