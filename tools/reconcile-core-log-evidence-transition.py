"""Seal the explicitly scoped final Core render equivalence after real gates."""
from pathlib import Path
import argparse, datetime, json, re
from focused_release_evidence import reference
from focused_release_evidence_core_log_transition import derive
from release_candidate_identity import current

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--id', required=True)
    parser.add_argument('--plan-only', action='store_true')
    args = parser.parse_args()
    assert re.fullmatch('[a-z0-9-]+', args.id)
    out = ROOT / 'output/release-readiness'
    if args.plan_only:
        print(json.dumps(dict(status='plan-only-no-equivalence-granted', requirements=[
            'Completed normal15build/36artifactABI/metadata gates and immutable before/after source archives',
            'Old Core-only modes: exactly six prepared logger/book sources and no other source transition',
            'All12real final-Core detailed-render smoke summaries/receipts/loaded code sources revalidated',
            'Fresh independent compilation of the strict ASM inspector and reinspection of all36real archive pairs',
            'Old Core-only modes: exactly12Corelogger deltas and24whole-byte-identical Compatibility/Controlify jars',
            'If the explicit1.21.11book amendment applies: exactly its two field-initializer identifier changes, both real book resource/render/reload workflows, and the additional strict compiled inspector',
            'Combined REI mode only: the same Core corrections plus exactly18 named REI sources,12 exact registration/policy archive deltas,12 byte-identical Controlify jars,84 independently executed pinned-API policy checks and both real final native REI/browser workflows; no other scope is allowed',
            'No uncompiled production change after normal15; all old execution/QA/publisher/feature scope checks remain separate'],
            coverage_disposition='Not assigned; no gameplay execution, assertion or release approval created'), indent=2))
        return
    destination = out / ('core-log-evidence-transition-' + args.id + '.json')
    assert not destination.exists(), 'Preserve prior certificates and failures'
    folder = ROOT / 'build/release-readiness' / ('core-log-evidence-transition-' + args.id)
    record = derive(ROOT, folder, reference(ROOT, out / 'final-core-log-build15-01.json'),
                    reference(ROOT, out / 'final-core-diagnostics-01.json'), current(ROOT, verify_worktree=True))
    record['created'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    record['auditor_inputs'] = [reference(ROOT, Path(__file__)), reference(ROOT, ROOT / 'tools/focused_release_evidence_core_log_transition.py')]
    destination.write_text(json.dumps(record, indent=2) + '\n')
    print(record['status'], destination)


if __name__ == '__main__':
    main()
