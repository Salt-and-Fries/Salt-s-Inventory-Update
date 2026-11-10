"""Named exact-build Tom workflow replay through the existing serial launcher."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))


def main(workflow):
    assert workflow in ['terminal','configurator','link','vending']
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run',required=True);parser.add_argument('--target',choices=['26.1.2/fabric'],required=True)
    parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert args.run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.run)
    launcher_path=ROOT/'functional-tests/project-everything-regression/run_toms_release.py'
    inputs=[Path(__file__),Path(__file__).with_name('run_toms_'+workflow+'.py'),launcher_path]
    expected={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest()for p in inputs}
    if args.plan_only:
        print(json.dumps(dict(target=args.target,workflow=workflow,launcher=str(launcher_path.relative_to(ROOT)),
            fixture='build/blocked-setup-recovery/fixtures/polymer-without-bumblezone.json',
            scope='Named real Tom workflow replay only; final subjects and frozen observer input hashes recorded by the original launcher'),indent=2));return
    spec=importlib.util.spec_from_file_location('release_toms_replay_launcher',launcher_path)
    launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher)
    previous=sys.argv;sys.argv=['run_toms_release.py','--run',args.run,'--only',workflow]
    try:launcher.main()
    finally:sys.argv=previous
    assert expected=={str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest()for p in inputs}
    output=ROOT/'output/release-readiness'/args.run
    summary=output/'summary.json';data=json.loads(summary.read_text())
    assert data['status']=='passed'and data['workflow']==workflow and data['target']==args.target
    receipt=output/'replay-driver.json';assert not receipt.exists()
    receipt.write_text(json.dumps(dict(status='passed',qa_inputs=expected,
        summary=dict(file=str(summary.relative_to(ROOT)),sha256=hashlib.sha256(summary.read_bytes()).hexdigest()),
        scope='Wrapper identity for a named current-artifact workflow. Acceptance checks remain in the original launcher/scenario journals.'),indent=2)+'\n')
