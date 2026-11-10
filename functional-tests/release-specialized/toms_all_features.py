"""Retain the strict native Tom workflow conditions on each target's publisher."""
from pathlib import Path
import hashlib, importlib.util, json

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'functional-tests/project-everything-regression/toms_release_workflows.py'


def run(profile, mc, loader, *args, **kw):
    spec = importlib.util.spec_from_file_location('toms_all_native_actions', SOURCE)
    original = importlib.util.module_from_spec(spec); spec.loader.exec_module(original)
    # Native 1.7.1 has no inventory-configurator item/menu. Its historical
    # misclassified row actually pictured the vending owner, exercised below.
    groups = ['terminal', 'link', 'vending'] if mc == '1.20.1' else ['terminal', 'configurator', 'link', 'vending']
    profile = Path(profile); out = profile / 'toms-all-release'; out.mkdir()
    checks = []; completed = []
    def save(complete=False):
        (out / 'summary.json').write_text(json.dumps(dict(complete=complete, groups=completed, checks=checks,
            retained_driver=dict(file=str(SOURCE.relative_to(ROOT)), sha256=hashlib.sha256(SOURCE.read_bytes()).hexdigest()),
            scope='Real connected Tom storage network, owned native channels and owner/customer vending; configurator only on native releases that include it. Moved controls, exact typed resource ledgers, refused action and persistence. No arbitrary network topology or multiplayer channel privacy claim.'), indent=2) + '\n')
    for group in groups:
        suite = original.Checks(profile)
        assert not (suite.out / 'checks.json').exists(), 'Preserve one distinct journal per native workflow'
        getattr(suite, group)()
        journal = suite.out / 'checks.json'; data = json.loads(journal.read_text())
        assert data['checks'] and all(c['passed'] for c in data['checks'])
        destination = out / group; assert not destination.exists(); suite.out.rename(destination)
        checks.extend(dict(group=group, **c) for c in data['checks'])
        completed.append(group); save()
    save(True)
    return dict(cases=len(groups), failed=0, checks=len(checks), toms_all_features_checked=True, groups=groups)
