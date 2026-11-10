"""Issue an explicit checked repair-chain report without granting UI coverage."""
from pathlib import Path
import argparse,json,sys
from focused_release_evidence import reference
from focused_release_evidence_core_log_consumer import Consumer
from focused_release_evidence_repair_chain_transition import derive
from release_candidate_identity import current
ROOT=Path(__file__).resolve().parents[1]
def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--id',required=True);args=parser.parse_args()
    assert args.id and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.id)
    certificate=ROOT/'output/release-readiness'/('repair-prefix-equivalence-'+args.id+'.json');assert not certificate.exists()
    candidate=current(ROOT,verify_worktree=True)
    result=derive(ROOT,ROOT/'build/release-readiness'/('repair-prefix-issuer-'+args.id),candidate,Consumer(ROOT))
    auditors=sorted((ROOT/'tools').glob('focused_release_evidence_repair_chain*.py'))
    auditors += [ROOT/'tools/focused_release_evidence_core_log_consumer.py',Path(__file__).resolve()]
    result['auditor_inputs']=[reference(ROOT,p)for p in auditors]
    certificate.write_text(json.dumps(result,indent=2)+'\n')
    pointer=ROOT/'output/release-readiness/repair-prefix-current.json'
    if pointer.exists():
        prior=certificate.with_suffix('.previous-pointer.json');assert not prior.exists();prior.write_bytes(pointer.read_bytes())
    temporary=pointer.with_suffix('.tmp');assert not temporary.exists()
    temporary.write_text(json.dumps(dict(status='explicit-repair-chain-active-no-new-coverage',certificate=reference(ROOT,certificate)),indent=2)+'\n');temporary.replace(pointer)
    print(result['status'])
if __name__=='__main__':main()
