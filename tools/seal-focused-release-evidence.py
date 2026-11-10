"""Preserve one completed scoped run; never assign or broaden its coverage."""
from pathlib import Path
import argparse,sys
from focused_release_evidence import seal
from release_candidate_identity import current

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from subject_artifacts import current_subjects


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--run',required=True);args=parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.run)
    candidate=current(ROOT,verify_worktree=True);assert candidate,'Activate the current source/artifact identity first'
    import json
    summary=ROOT/'output/release-readiness'/args.run/'summary.json';target=json.loads(summary.read_text(encoding='utf-8'))['target']
    result=seal(ROOT,args.run,current_subjects(target),candidate_identity=candidate)
    print('Sealed scoped evidence:',result)


if __name__=='__main__':main()
