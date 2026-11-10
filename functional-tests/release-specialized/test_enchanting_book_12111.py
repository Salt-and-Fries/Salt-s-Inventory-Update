"""Reject missing/wrong-phase book observations; these are offline guard tests."""
from pathlib import Path
import copy
import unittest
import enchanting_book_12111 as book

ROOT = Path(__file__).resolve().parents[2]


class EnchantingBook12111(unittest.TestCase):
    def report(self):
        rows = []
        for phase in book.PHASES:
            if phase == 'book-after-reload':
                rows += [dict(name='desktop-ready-after-resource-reload', status='PASS'), dict(name='resource-pack-reload', status='PASS')]
            rows += [dict(name=phase + '-book-' + n, status='PASS') for n in [
                'version-correct-identifier', 'resource-available', 'native-resource-hash',
                'textured-model-visible', 'not-missing-texture-rectangle']]
            rows.append(dict(name=phase + '-book-resource-observed', status='INFO', details=book.RESOURCE + ' sha256=' + book.NATIVE_SHA))
            rows.append(dict(name=phase + '-framebuffer', status='CAPTURED', path='fixture.png'))
        return dict(minecraft='1.21.11', loader='fabric', scenario='core', failures=0, results=rows)

    def test_only_additions_to_original_harness_restore_every_original_byte(self):
        path = ROOT / book.BASE_JAVA
        before = path.read_bytes()
        text = book.derive_java(path.read_text())
        for old, new in reversed(book.substitutions()):
            self.assertEqual(text.count(new), 1)
            text = text.replace(new, old)
        self.assertEqual(text, path.read_text())
        self.assertEqual(before, path.read_bytes())
        additions = book.METHOD
        self.assertNotIn('STEPS.remove', additions)
        self.assertNotIn('clickAt(', additions)
        self.assertNotIn('server(', additions)

    def test_both_phases_have_independent_resource_render_and_framebuffer_receipts(self):
        self.assertTrue(book.verify_report(self.report()))

    def test_missing_failed_or_duplicated_observation_cannot_pass(self):
        baseline = self.report()
        for row in baseline['results']:
            for mode in ['missing', 'failed', 'duplicate']:
                altered = copy.deepcopy(baseline)
                if mode == 'missing': altered['results'] = [v for v in altered['results'] if v['name'] != row['name']]
                if mode == 'failed': next(v for v in altered['results'] if v['name'] == row['name'])['status'] = 'FAIL'
                if mode == 'duplicate': altered['results'].append(copy.deepcopy(row))
                with self.subTest(name=row['name'], mode=mode), self.assertRaises(AssertionError):
                    book.verify_report(altered)

    def test_before_and_after_cannot_both_be_captured_before_reload(self):
        data = self.report()
        data['results'] = [v for v in data['results'] if not v['name'].endswith('resource-pack-reload') and v['name'] != 'desktop-ready-after-resource-reload'] + [
            dict(name='resource-pack-reload', status='PASS'), dict(name='desktop-ready-after-resource-reload', status='PASS')]
        with self.assertRaises(AssertionError): book.verify_report(data)

    def test_wrong_native_asset_identity_or_scope_is_rejected(self):
        for field, value in [('minecraft', '26.1.2'), ('loader', 'forge'), ('scenario', 'toms'), ('failures', 1), ('completeCoreFeatureSuite', True)]:
            data = self.report()
            data[field] = value
            with self.subTest(field=field), self.assertRaises(AssertionError): book.verify_report(data)
        data = self.report()
        next(v for v in data['results'] if v['status'] == 'INFO')['details'] += 'wrong'
        with self.assertRaises(AssertionError): book.verify_report(data)

    def test_missing_resource_warning_is_not_hidden_by_success_summary(self):
        self.assertTrue(book.verify_log('SIU_GAMEPLAY_SUMMARY status=PASS'))
        for path in ['textures/entity/enchantment/enchanting_table_book.png', 'textures/entity/enchanting_table_book.png']:
            with self.assertRaises(AssertionError):
                book.verify_log('Missing resource minecraft:' + path + '\nSIU_GAMEPLAY_SUMMARY status=PASS')

    def test_compile_override_is_limited_to_two_targets_and_qa_source_set(self):
        text = book.init_source(ROOT, ROOT / 'build/release-readiness/fixture-book')
        self.assertIn("mc!='1.21.11'", text)
        self.assertIn("['fabric','neoforge']", text)
        self.assertIn("'gameplayQa':'chestCloseQa'", text)
        self.assertIn('it.canonicalPath!=original.canonicalPath', text)
        self.assertNotIn('sourceSets.main', text)
        self.assertNotIn('src/main/java', text.replace(book.BASE_JAVA, 'CANONICAL_QA'))
        with self.assertRaises(AssertionError): book.init_source(ROOT, ROOT.parent / 'escape')


if __name__ == '__main__':
    unittest.main()
