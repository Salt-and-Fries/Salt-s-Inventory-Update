"""Final report lookup must preserve the frozen publisher verification policy."""
from pathlib import Path
import ast
import contextlib
import copy
import inspect
import json
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools'))
import focused_release_evidence_publisher_final_core as adapter
import focused_release_evidence_publisher_retention as original
from focused_release_evidence import reference


class FinalPublisherAdapter(unittest.TestCase):
    def test_current_generation_delegates_to_the_unmodified_policy(self):
        campaign = {'scope': 'original'}
        subjects = lambda target: {'original': target}
        with patch.object(original, 'retained_for_campaign', return_value=['original-result']) as call:
            self.assertEqual(adapter.retained_for_campaign('root', campaign, subjects, pointer='pointer'),
                             ['original-result'])
            call.assert_called_once_with('root', campaign, subjects, pointer='pointer')

    def test_verification_has_only_the_explicit_artifact_lookup_extension(self):
        before = ast.parse(inspect.getsource(original.verify_equivalence)).body[0]
        after = ast.parse(inspect.getsource(adapter.verify_equivalence)).body[0]
        self.assertEqual(after.args.kwonlyargs[-1].arg, 'artifact_locator')
        after.args.kwonlyargs.pop()
        after.args.kw_defaults.pop()
        old_loop = next(v for v in before.body if isinstance(v, ast.For) and ast.unparse(v.target) == 'delta')
        new_loop = next(v for v in after.body if isinstance(v, ast.For) and ast.unparse(v.target) == 'delta')
        expected = ast.parse('''
before = checked(root, delta['before']) if artifact_locator is None else artifact_locator(delta['before'])
after = checked(root, delta['after']) if artifact_locator is None else artifact_locator(delta['after'])
archive_pair(before, after, delta['family'])
''').body
        self.assertEqual(ast.dump(ast.Module(body=new_loop.body, type_ignores=[])),
                         ast.dump(ast.Module(body=expected, type_ignores=[])))
        new_loop.body = copy.deepcopy(old_loop.body)
        routing_call = next(v for v in ast.walk(after) if isinstance(v, ast.Call)
                            and isinstance(v.func, ast.Name) and v.func.id == 'routing')
        self.assertEqual(len(routing_call.keywords), 1)
        self.assertEqual(routing_call.keywords[0].arg, 'artifact_locator')
        self.assertEqual(ast.unparse(routing_call.keywords[0].value), 'artifact_locator')
        routing_call.keywords.clear()
        self.assertEqual(ast.dump(after), ast.dump(before))

    def test_routing_uses_frozen_code_without_changing_original_checked(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            old = root / 'old.jar';old.write_bytes(b'original')
            new = root / 'new.jar';new.write_bytes(b'new')
            text = root / 'disassembly.txt';text.write_bytes(b'unchanged')
            row = dict(before=reference(root, old), after=reference(root, new))
            refs = [row['before'], row['after'], reference(root, text)]
            saved_checked = original.checked
            # Exercise the cloned frozen function's actual global lookup.
            def frozen(owner, record, delta):
                return [checked(owner, ref).read_bytes() for ref in delta]
            namespace = dict(frozen.__globals__);namespace['checked'] = saved_checked
            import types
            frozen = types.FunctionType(frozen.__code__, namespace, frozen.__name__)
            resolved = []
            def locate(ref):
                resolved.append(ref);return root / ref['file']
            with patch.object(original, 'routing', frozen):
                self.assertEqual(adapter.routing(root, row, refs, artifact_locator=locate),
                                 [b'original', b'new', b'unchanged'])
            self.assertEqual(resolved, [row['before'], row['after']])
            self.assertIs(original.checked, saved_checked)
            corrupt = root / 'corrupt.jar';corrupt.write_bytes(b'corrupt')
            with patch.object(original, 'routing', frozen):
                with self.assertRaisesRegex(AssertionError, 'hash mismatch'):
                    adapter.routing(root, row, refs, artifact_locator=lambda ref: corrupt)
                def unknown(ref):raise AssertionError('Unknown preserved archive')
                with self.assertRaisesRegex(AssertionError, 'Unknown preserved'):
                    adapter.routing(root, row, refs, artifact_locator=unknown)


    def test_enumeration_preserves_every_original_latest_attempt_check(self):
        before = ast.parse(inspect.getsource(original.retained_for_campaign)).body[0]
        after = ast.parse(inspect.getsource(adapter.retained_for_campaign)).body[0]
        after.args.kwonlyargs.pop()
        after.args.kw_defaults.pop()
        self.assertIsInstance(after.body[1], ast.If)
        self.assertEqual(ast.unparse(after.body[1].test), 'artifact_locator is None and (not archived_qa_active(root))')
        after.body.pop(1)
        calls = [v for v in ast.walk(after) if isinstance(v, ast.Call)
                 and isinstance(v.func, ast.Name) and v.func.id == 'verify_equivalence']
        self.assertEqual(len(calls), 1)
        self.assertEqual(calls[0].keywords[-1].arg, 'artifact_locator')
        calls[0].keywords.pop()
        self.assertEqual(ast.dump(after), ast.dump(before))

    def test_history_preserves_original_checks_except_explicit_recorded_source_read(self):
        before = ast.parse(inspect.getsource(original.history)).body[0]
        after = ast.parse(inspect.getsource(adapter.history)).body[0]
        insertion = next(v for v in after.body if isinstance(v, ast.Assign)
                         and ast.unparse(v.targets[0]) == 'measured_sources')
        self.assertEqual(ast.unparse(insertion.value), 'MeasuredSources(root)')
        after.body.remove(insertion)
        old_loop = next(v for v in before.body if isinstance(v, ast.For)
                        and ast.unparse(v.target) == '(names, destination)')
        new_loop = next(v for v in after.body if isinstance(v, ast.For)
                        and ast.unparse(v.target) == '(names, destination)')
        expected = ast.parse('''
for name, digest in names.items():
    path = (root / name).resolve()
    assert path.is_relative_to(root), ('Measured source outside workspace', name)
    raw = measured_sources.read(name, digest)
    key = path.relative_to(root).as_posix()
    inputs[key] = raw
    destination.append(dict(file=key, sha256=digest))
''').body
        self.assertEqual(ast.dump(ast.Module(body=new_loop.body, type_ignores=[])),
                         ast.dump(ast.Module(body=expected, type_ignores=[])))
        new_loop.body = copy.deepcopy(old_loop.body)
        self.assertEqual(ast.dump(after), ast.dump(before))

    def fixture(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)

        def write(name, data):
            path = root / name
            path.write_text(json.dumps(data))
            return reference(root, path)

        target = 'test/fabric'
        plan = [dict(id='job-' + str(i), target=target) for i in range(65)]
        attempt = dict(job='job-0', run='old-native-run', status='passed')
        campaign = dict(plan=plan, attempts=[attempt])
        rows = []
        for family in ['core', 'compatibility', 'controlify']:
            refs = {}
            for side in ['before', 'after']:
                path = root / (side + '-' + family + '.jar')
                path.write_bytes((side + family).encode())
                refs[side] = reference(root, path)
            rows.append(dict(target=target, family=family, **refs))
        log = write('native-log.json', {'native': 'recorded'})
        history = dict(job='job-0', target=target, original_run=attempt['run'],
                       acceptance=write('acceptance.json', {'recorded': True}),
                       log=log, runtime_copies=[], measured={'assertions': 7})
        scope = dict(records=[dict(job='job-0', target=target, original_attempt=attempt['run'],
                                  traveler_absent=True, traveler_native_definitions=[], log=log, runtime_mods=[])])
        record = dict(status='retained-unchanged-scope-not-a-new-execution', history=history,
                      original_campaign=write('campaign.json', campaign),
                      delta_ledger=write('deltas.json', dict(artifacts=rows)),
                      runtime_scope_audit=write('scope.json', scope),
                      engine_semantics=write('engine.json', {}),
                      absence_routing=write('routing.json', dict(rows=[dict(target=target)])),
                      coverage_limit='Original recorded assertions only; entry-only and unresolved features remain unqualified.')
        subjects = {v['family']: [Path(v['after']['file']).name, v['after']['sha256']]
                    for v in rows if v['family'] in {'core', 'compatibility'}}
        return root, campaign, record, subjects, rows

    def test_preserved_lookup_keeps_all_original_references_and_subjects(self):
        root, campaign, record, subjects, rows = self.fixture()
        saved = copy.deepcopy(record)
        resolved = []

        def locate(ref):
            resolved.append(copy.deepcopy(ref))
            return root / ref['file']

        with contextlib.ExitStack() as stack:
            pairs = stack.enter_context(patch.object(adapter, 'archive_pair'))
            stack.enter_context(patch.object(adapter, 'history', return_value=(record['history'], None)))
            for name in ['verify_historical_archive', 'engine_semantics', 'routing']:
                stack.enter_context(patch.object(adapter, name))
            result = adapter.verify_equivalence(root, record, campaign, subjects, artifact_locator=locate)
        self.assertEqual(record, saved)
        self.assertEqual(resolved, [v[side] for v in rows for side in ['before', 'after']])
        self.assertEqual(pairs.call_count, 3)
        self.assertEqual(result['original_run'], 'old-native-run')
        self.assertEqual(result['measured'], {'assertions': 7})

    def test_unknown_locator_and_newer_failed_run_cannot_inherit_a_pass(self):
        root, campaign, record, subjects, rows = self.fixture()
        def unknown(ref):
            raise AssertionError('Unknown preserved artifact')
        with self.assertRaisesRegex(AssertionError, 'Unknown preserved artifact'):
            adapter.verify_equivalence(root, record, campaign, subjects, artifact_locator=unknown)
        campaign['attempts'].append(dict(job='job-0', run='new-failed-run', status='failed'))
        with self.assertRaisesRegex(AssertionError, 'newer failed attempt'):
            adapter.verify_equivalence(root, record, campaign, subjects, artifact_locator=unknown)


if __name__ == '__main__':
    unittest.main(verbosity=2)
