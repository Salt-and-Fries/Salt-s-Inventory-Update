"""Retain the original six ship-page assertions and their real observer traffic."""
from pathlib import Path
import hashlib, json, runpy, sys

ROOT = Path(__file__).resolve().parents[2]
ORIGINAL = ROOT / 'functional-tests/all-target-rollout/focused-smallships-paging.py'
CHECKS = [
    'normal-opening-has-complete-unclipped-regions',
    'real-player-stack-enters-first-page-cargo',
    'next-page-replaces-one-window-and-shows-different-cargo',
    'previous-page-restores-the-exact-stored-stack',
    'paging-with-held-stack-keeps-seven-on-shared-cursor',
    'cargo-round-trip-returns-seven-with-empty-cursor',
]


def derive(original, minecraft):
    """Translate only the two legacy public world-position prerequisites."""
    assert original.count('8.5 65') == 2
    if minecraft == '1.20.1':
        return original
    assert minecraft == '1.21.1' and '8.5 -60' not in original
    changed = original.replace('8.5 65', '8.5 -60')
    assert changed.replace('8.5 -60', '8.5 65') == original
    return changed


def run(profile, minecraft, loader, only=None, max_cases=None, mods=None, **options):
    assert (minecraft, loader) in {('1.20.1', 'fabric'), ('1.20.1', 'forge'),
                                    ('1.21.1', 'fabric'), ('1.21.1', 'neoforge')}
    assert not only and max_cases is None and not mods and not any(options.values()), 'No hidden case/feature filters'
    profile = Path(profile)
    folder = profile / 'smallships-paging-release'
    folder.mkdir()
    original = ORIGINAL.read_text()
    derived = folder / 'retained-driver.py'
    derived.write_text(derive(original, minecraft))
    old_path, old_argv = list(sys.path), sys.argv
    sys.path.insert(0, str(ORIGINAL.parent))
    import evidence
    assert Path(evidence.__file__).resolve() == ORIGINAL.with_name('evidence.py').resolve()
    original_send = evidence.send
    calls = []

    def observed_send(path, kind, **kwargs):
        assert Path(path) == profile
        response = original_send(path, kind, **kwargs)
        index = len(calls)
        capture = folder / ('response-' + str(index).zfill(3) + '.json')
        capture.write_text(json.dumps(response, indent=2) + '\n')
        row = dict(index=index, command=kind, arguments=kwargs,
                   response=dict(file=str(capture.relative_to(ROOT)),
                                 sha256=hashlib.sha256(capture.read_bytes()).hexdigest()))
        server = profile / 'server-state.json'
        if server.exists():
            copied = folder / ('server-after-' + str(index).zfill(3) + '.json')
            copied.write_bytes(server.read_bytes())
            row['latest_server_file'] = dict(file=str(copied.relative_to(ROOT)),
                sha256=hashlib.sha256(copied.read_bytes()).hexdigest(),
                scope='Existing file after this original action; only snapshot-server requests establish a fresh server observation.')
        calls.append(row)
        (folder / 'original-actions.json').write_text(json.dumps(calls, indent=2) + '\n')
        return response

    evidence.send = observed_send
    sys.argv = [str(derived), str(profile)]
    try:
        runpy.run_path(str(derived), run_name='__main__')
    finally:
        evidence.send = original_send
        sys.path[:] = old_path
        sys.argv = old_argv
    assert ORIGINAL.read_text() == original
    assert derived.read_text() == derive(original, minecraft)
    accepted_path = profile / 'smallships-cargo-paging-focused-checks.json'
    accepted = json.loads(accepted_path.read_text())
    assert [c['check'] for c in accepted['checks']] == CHECKS
    assert all(c['passed'] is True for c in accepted['checks'])
    summary = dict(complete=True, checks=accepted['checks'], original_scope=accepted['scope'],
        original=dict(file=str(ORIGINAL.relative_to(ROOT)), sha256=hashlib.sha256(ORIGINAL.read_bytes()).hexdigest()),
        derived=dict(file=str(derived.relative_to(ROOT)), sha256=hashlib.sha256(derived.read_bytes()).hexdigest()),
        fixture_derivation='Unchanged on 1.20.1; only two public summon/teleport Y literals move from 65 to the actual flat-world ground -60 on 1.21.1. All UI clicks, original six assertions, quantities and native cursor predicates remain unchanged.',
        original_journal=dict(file=str(accepted_path.relative_to(ROOT)), sha256=hashlib.sha256(accepted_path.read_bytes()).hexdigest()),
        original_actions=calls,
        scope='One native Galleon cargo/page workflow: first cargo deposit, next/previous replacement, shared held cursor and exact recovery. Brigg/Galley tagged three-cell transfer results remain separate original publisher cases; no combat/sailing or every cargo-page claim.')
    (folder / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    return dict(cases=1, failed=0, checks=6, smallships_paging_checked=True, scope=summary['scope'])
