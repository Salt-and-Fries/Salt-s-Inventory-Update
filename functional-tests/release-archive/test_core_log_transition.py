"""Reject broader source/artifact changes and missing diagnostic targets."""
from pathlib import Path
import copy, subprocess, sys, unittest
sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'tools'))
from focused_release_evidence_core_log_transition import sources, production_unchanged, ledger, smokes, book_smokes


class CoreLogTransition(unittest.TestCase):
    def test_real_verifiers_load_from_a_clean_producer_process(self):
        root = Path(__file__).resolve().parents[2]
        code = 'import sys;from pathlib import Path;sys.path.insert(0,sys.argv[1]);from focused_release_evidence_core_log_transition import load;m=load(Path(sys.argv[2]));assert callable(m.verify_run)'
        for name in ['run_core_render_diagnostic.py', 'run_core_enchanting_book.py']:
            result = subprocess.run([sys.executable, '-c', code, str(root / 'tools'), str(root / 'functional-tests/release-specialized' / name)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)

    def source_fixture(self):
        rows = [dict(source=dict(file='versions/' + str(i) + '/fabric/src/main/java/com/salts_inventory_update/client/InventoryDesktopScreen.java', sha256='a' * 64),
                     prepared_source=dict(file='prepared/' + str(i), sha256='b' * 64)) for i in range(6)]
        old = {r['source']['file']: 'a' * 64 for r in rows}
        new = {r['source']['file']: 'b' * 64 for r in rows}
        old['tools/report.py'] = new['tools/report.py'] = 'c' * 64
        return dict(source_files=old), dict(source_files=new), dict(rows=rows)

    def test_only_six_exact_prepared_source_changes_are_allowed(self):
        before, after, prepared = self.source_fixture()
        self.assertEqual(6, len(sources(before, after, prepared)))
        for kind in ['extra', 'removed', 'wrong-before', 'wrong-after', 'duplicate']:
            a, b, c = copy.deepcopy((before, after, prepared))
            if kind == 'extra': b['source_files']['tools/report.py'] = 'd' * 64
            if kind == 'removed': b['source_files'].pop('tools/report.py')
            if kind == 'wrong-before': c['rows'][0]['source']['sha256'] = 'd' * 64
            if kind == 'wrong-after': c['rows'][0]['prepared_source']['sha256'] = 'd' * 64
            if kind == 'duplicate': c['rows'][-1] = c['rows'][0]
            with self.assertRaises(AssertionError, msg=kind): sources(a, b, c)

    def test_later_docs_may_change_but_no_uncompiled_production_change(self):
        compiled = dict(source_files={'versions/game/A.java': 'a', 'compatibility/art.png': 'b', 'docs/release.md': 'c', 'compatibility/CHANGELOG.md': 'old notes'}, artifacts=[1])
        current = copy.deepcopy(compiled)
        current['source_files']['docs/release.md'] = 'd'
        current['source_files']['compatibility/CHANGELOG.md'] = 'new release notes'
        production_unchanged(compiled, current)
        for name in ['versions/game/A.java', 'compatibility/art.png', 'gradle.properties', 'controlify/new.java',
                     'compatibility/src/main/resources/CHANGELOG.md', 'compatibility/other.md', 'compatibility/build.gradle.kts']:
            value = copy.deepcopy(current)
            value['source_files'][name] = 'changed'
            with self.assertRaises(AssertionError): production_unchanged(compiled, value)
        current['artifacts'] = [2]
        with self.assertRaises(AssertionError): production_unchanged(compiled, current)

    def test_all_twelve_target_and_three_family_archive_pairs_are_required(self):
        targets = ['game' + str(i) + '/fabric' for i in range(12)]
        rows = [dict(minecraft=t.split('/')[0], loader='fabric', family=f, file=t + '/' + f + '.jar', sha256='a' * 64)
                for t in targets for f in ['core', 'compatibility', 'controlify']]
        self.assertEqual(36, len(ledger(rows, targets)))
        for kind in ['missing', 'duplicate', 'family', 'hash', 'filename']:
            value = copy.deepcopy(rows)
            if kind == 'missing': value.pop()
            if kind == 'duplicate': value[-1] = value[0]
            if kind == 'family': value[-1]['family'] = 'qa'
            if kind == 'hash': value[-1]['sha256'] = 'not-a-hash'
            if kind == 'filename': value[-1]['file'] = value[0]['file']
            with self.assertRaises(AssertionError, msg=kind): ledger(value, targets)

    def test_real_diagnostic_receipts_for_every_target_no_bool_exit_or_old_failure(self):
        targets = ['game' + str(i) + '/fabric' for i in range(12)]
        queue = dict(status='passed', runs=[dict(target=t, status='passed', exit_code=0, summary={'file': t}, acceptance={'file': t}) for t in targets])
        self.assertEqual(12, len(smokes(queue, targets)))
        for kind in ['missing', 'duplicate', 'failed', 'running', 'boolean', 'receipt']:
            value = copy.deepcopy(queue)
            if kind == 'missing': value['runs'].pop()
            if kind == 'duplicate': value['runs'][-1] = value['runs'][0]
            if kind == 'failed': value['runs'][-1]['status'] = 'failed'
            if kind == 'running': value['status'] = 'running'
            if kind == 'boolean': value['runs'][-1]['exit_code'] = False
            if kind == 'receipt': value['runs'][-1].pop('acceptance')
            with self.assertRaises(AssertionError, msg=kind): smokes(value, targets)

    def test_both_book_targets_need_their_actual_named_workflow_receipts(self):
        plan = [dict(target='1.21.11/' + loader, driver='book.py', run='book-' + loader) for loader in ['fabric', 'neoforge']]
        queue = dict(status='passed', plan=plan, runs=[dict(**job, status='passed', exit_code=0,
            summary={'file': job['run']}, acceptance={'file': job['run'] + '-acceptance'}) for job in plan])
        self.assertEqual(set(book_smokes(queue, dict(book_workflows=plan))), {'1.21.11/fabric', '1.21.11/neoforge'})
        for kind in ['missing', 'duplicate', 'running', 'bool-exit', 'driver', 'receipt', 'wrong-plan']:
            value = copy.deepcopy(queue)
            if kind == 'missing': value['runs'].pop()
            if kind == 'duplicate': value['runs'][-1] = value['runs'][0]
            if kind == 'running': value['runs'][-1]['status'] = 'running'
            if kind == 'bool-exit': value['runs'][-1]['exit_code'] = False
            if kind == 'driver': value['runs'][-1]['driver'] = 'other.py'
            if kind == 'receipt': value['runs'][-1].pop('acceptance')
            if kind == 'wrong-plan': value['plan'][-1]['target'] = '26.1.2/neoforge'
            with self.subTest(kind=kind), self.assertRaises(AssertionError): book_smokes(value, dict(book_workflows=plan))


if __name__ == '__main__':
    unittest.main()
