"""Point local qualification at an exact recorded source; never publish or qualify it."""
from pathlib import Path
import argparse,datetime,json
from release_candidate_identity import sha,validate

ROOT=Path(__file__).resolve().parents[1]


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--id',required=True);args=parser.parse_args()
    assert args.id and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.id)
    folder=ROOT/'output/release-readiness';source=folder/('candidate-'+args.id+'.json');integrity=folder/'candidate-integrity.json'
    value=json.loads(source.read_text());assert value.get('review_source_archive'),'An immutable review-source archive is required'
    pointer=dict(status='recorded-qualification-pending',activated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        source_snapshot=dict(file=str(source.relative_to(ROOT)),sha256=sha(source)),source_tree_sha256=value['source_tree_sha256'],
        artifact_integrity=dict(file=str(integrity.relative_to(ROOT)),sha256=sha(integrity)),
        note='Local current-candidate pointer only. This does not establish gameplay acceptance, approval, a commit, or publication.')
    validate(ROOT,pointer,verify_worktree=True,verify_archive=True)
    path=folder/'candidate-current.json'
    if path.exists():
        # Prior immutable source snapshots remain authoritative historical inputs.
        # Keep each replaced pointer as well, rather than losing its activation.
        history=folder/'candidate-pointer-history';history.mkdir(exist_ok=True)
        archive=history/(sha(path)+'.json')
        if archive.exists():assert archive.read_bytes()==path.read_bytes()
        else:archive.write_bytes(path.read_bytes())
    temporary=path.with_suffix('.tmp');temporary.write_text(json.dumps(pointer,indent=2)+'\n',encoding='utf-8');temporary.replace(path)
    print('Activated local candidate',args.id,pointer['source_tree_sha256'],'(qualification pending)')


if __name__=='__main__':main()
