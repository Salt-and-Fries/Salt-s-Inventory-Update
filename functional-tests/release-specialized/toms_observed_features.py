"""Archive the original Tom workflows' existing observations, without extra input."""
from pathlib import Path
import hashlib, importlib.util, json

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'functional-tests/project-everything-regression/toms_release_workflows.py'
COUNTS = dict(terminal=17, configurator=11, link=11, vending=10)
SCHEMA = 'toms-original-observations-v1'


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def observed_class(base, group):
    """Decorate read/transport methods only; call each original exactly once."""
    assert group in COUNTS

    class Observed(base):
        def __init__(self, profile):
            super().__init__(profile)
            self.observation_dir = self.out / 'observations'
            self.observation_dir.mkdir()  # Refuse reuse of any prior attempt.
            self.observation_refs = []

        def archive(self, kind, request, result):
            number = len(self.observation_refs) + 1
            path = self.observation_dir / (str(number).zfill(5) + '-' + kind + '.json')
            record = dict(schema=SCHEMA, ordinal=number, group=group, kind=kind,
                          request=request, result=result)
            with path.open('x', encoding='utf-8') as stream:
                json.dump(record, stream, indent=2, allow_nan=False)
                stream.write('\n')
            self.observation_refs.append(dict(file=path.relative_to(self.out).as_posix(), sha256=sha(path)))
            index = self.observation_dir / 'index.json'
            index.write_text(json.dumps(dict(schema=SCHEMA, group=group,
                observations=self.observation_refs), indent=2, allow_nan=False) + '\n', encoding='utf-8')

        def call(self, *args, **kwargs):
            result = super().call(*args, **kwargs)
            self.archive('reply', dict(args=list(args), kwargs=kwargs), result)
            return result

        def state(self, name):
            result = super().state(name)
            self.archive('server-state', dict(name=name), result)
            return result

        def toms(self, session):
            result = super().toms(session)
            self.archive('native-state', dict(session=session), result)
            return result

    return Observed


def run(profile, mc, loader, *args, **kwargs):
    spec = importlib.util.spec_from_file_location('toms_observed_original_actions', SOURCE)
    original = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(original)
    groups = ['terminal', 'link', 'vending'] if mc == '1.20.1' else ['terminal', 'configurator', 'link', 'vending']
    profile = Path(profile)
    out = profile / 'toms-all-release'
    out.mkdir()
    checks, completed, observations = [], [], []

    def save(complete=False):
        record = dict(complete=complete, groups=completed, checks=checks, observation_indices=observations,
            retained_driver=dict(file=SOURCE.relative_to(ROOT).as_posix(), sha256=sha(SOURCE)),
            scope='Unchanged original Tom actions and assertions, with immediate archives of their existing replies and native/server observations. No added game calls, forced state, retries, arbitrary topology or multiplayer privacy claim.')
        (out / 'summary.json').write_text(json.dumps(record, indent=2, allow_nan=False) + '\n')

    save()
    for group in groups:
        suite = observed_class(original.Checks, group)(profile)
        assert not (suite.out / 'checks.json').exists(), 'Preserve each original workflow journal'
        getattr(suite, group)()
        data = json.loads((suite.out / 'checks.json').read_text())
        assert len(data['checks']) == COUNTS[group] and all(c['passed'] is True for c in data['checks'])
        index = suite.observation_dir / 'index.json'
        archived = json.loads(index.read_text())
        assert archived['observations'] == suite.observation_refs and suite.observation_refs
        for ref in suite.observation_refs:
            assert sha(suite.out / ref['file']) == ref['sha256']
        index_hash = sha(index)
        destination = out / group
        assert not destination.exists()
        suite.out.rename(destination)
        checks.extend(dict(group=group, **check) for check in data['checks'])
        observations.append(dict(group=group, file=(Path(group) / 'observations/index.json').as_posix(),
                                 sha256=index_hash, count=len(suite.observation_refs)))
        completed.append(group)
        save()
    save(True)
    return dict(cases=len(groups), failed=0, checks=len(checks), toms_all_features_checked=True,
                toms_existing_observations_archived=True, groups=groups)
