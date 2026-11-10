"""Replay retained native forge/armor/shop actions on the final candidate."""
from pathlib import Path
import hashlib, importlib.util, json

ROOT = Path(__file__).resolve().parents[2]


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module); return module


forge = load('release_prominent_forge', ROOT / 'functional-tests/prominent-forge/check.py')
shop = load('release_prominent_shop', ROOT / 'functional-tests/prominent-shop/check.py')


class CurrentInput:
    def act(self, kind='capture', **kw):
        result = super().act('move' if kind == 'pointer' else kind, **kw)
        # The original superclass has already retained the raw response. Its
        # assertions use the previous observation name for the template class.
        for row in result.get('allWindows', []):
            if isinstance(row.get('template'), dict) and row.get('templateClass'):
                row['template'] = row['templateClass']
        return result


class CurrentForge(CurrentInput, forge.Check): pass
class CurrentShop(CurrentInput, shop.Shop): pass


def run(profile, mc, loader, *args, **kwargs):
    assert (mc, loader) == ('1.20.1', 'fabric')
    profile = Path(profile); folder = profile / 'prominent-release'; folder.mkdir()
    completed = []
    for phase in ['upgrade', 'armor', 'visuals', 'shop']:
        suite = (CurrentShop if phase == 'shop' else CurrentForge)(profile, phase)
        if phase == 'shop': suite.run_checks()
        else: getattr(suite, phase)()
        assert len(suite.results) >= 5 and all(v['passed'] for v in suite.results)
        completed.append(dict(group=phase, checks=len(suite.results),
            journal=str((suite.out / 'results.json').relative_to(ROOT)),
            sha256=hashlib.sha256((suite.out / 'results.json').read_bytes()).hexdigest()))
        (folder / 'summary.json').write_text(json.dumps(dict(complete=False, groups=completed), indent=2) + '\n')
        suite.act('close', ticks=5)
    scope = ('Native Prominent upgrade progression, armor selection/requirements/forging '
             'and all three shop transaction tabs with refusal, resources, synchronization, '
             'movement/scale and source lifetime. Operator fixtures grant native progression '
             'and materials; not campaign progression or every armor/trader offer.')
    summary = dict(complete=True, groups=completed, scope=scope)
    (folder / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    return dict(cases=3, failed=0, checks=sum(v['checks'] for v in completed), feature_groups=[v['group'] for v in completed], prominent_features_checked=True, scope=scope)
