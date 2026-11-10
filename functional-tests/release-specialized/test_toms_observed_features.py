"""Offline archive/transport tests; these establish no gameplay acceptance."""
from pathlib import Path
import json, tempfile, unittest
from toms_observed_features import observed_class, sha, SCHEMA


class Base:
    def __init__(self, profile):
        self.out = Path(profile) / 'toms-release'
        self.out.mkdir(exist_ok=True)
        self.calls = []
        self.reply = dict(client=dict(nativeTemplateMenus=[dict(type='ActualScreen', session=1)]))
        self.server = dict(cursor=dict(signature='{typed:1b}', count=7), menus=[dict(type='NativeMenu')])
        self.native = dict(menus=[dict(type='NativeMenu', valid=True, selected='owned-channel')])

    def call(self, *args, **kwargs):
        self.calls.append((args, kwargs))
        if kwargs.get('raise_original'):
            raise RuntimeError('original failure')
        return self.reply

    def state(self, name):
        self.call('regression-state', ticks=2)
        return self.server

    def toms(self, session):
        self.call('regression-toms-state', session=session, ticks=3)
        return self.native


class ObservationTests(unittest.TestCase):
    def test_each_original_call_result_and_timing_is_preserved_once(self):
        with tempfile.TemporaryDirectory() as folder:
            suite = observed_class(Base, 'terminal')(folder)
            self.assertIs(suite.call('wait', ticks=3), suite.reply)
            self.assertIs(suite.state('open'), suite.server)
            self.assertIs(suite.toms(8), suite.native)
            self.assertEqual(suite.calls, [(('wait',),dict(ticks=3)),
                (('regression-state',),dict(ticks=2)),(('regression-toms-state',),dict(session=8,ticks=3))])
            index = json.loads((suite.observation_dir / 'index.json').read_text())
            self.assertEqual(index['schema'], SCHEMA)
            self.assertEqual(len(index['observations']), 5)
            records = []
            for ref in index['observations']:
                path = suite.out / ref['file']
                self.assertEqual(sha(path), ref['sha256'])
                records.append(json.loads(path.read_text()))
            self.assertEqual([r['ordinal'] for r in records], [1,2,3,4,5])
            self.assertEqual([r['kind'] for r in records], ['reply','reply','server-state','reply','native-state'])
            self.assertEqual(records[2]['result'], suite.server)
            self.assertEqual(records[4]['result'], suite.native)
            suite.native['menus'][0]['selected'] = 'later'
            self.assertEqual(json.loads((suite.out / index['observations'][4]['file']).read_text())['result']['menus'][0]['selected'], 'owned-channel')

    def test_original_error_is_not_swallowed_or_retried(self):
        with tempfile.TemporaryDirectory() as folder:
            suite = observed_class(Base, 'link')(folder)
            with self.assertRaisesRegex(RuntimeError, 'original failure'):
                suite.call('test', raise_original=True, ticks=9)
            self.assertEqual(suite.calls, [(('test',),dict(raise_original=True,ticks=9))])
            self.assertEqual(suite.observation_refs, [])

    def test_refuses_reuse_overwrite_and_unknown_group(self):
        with tempfile.TemporaryDirectory() as folder:
            suite = observed_class(Base, 'vending')(folder)
            with self.assertRaises(FileExistsError):
                observed_class(Base, 'vending')(folder)
            (suite.observation_dir / '00001-reply.json').write_text('prior attempt')
            with self.assertRaises(FileExistsError):
                suite.call('wait', ticks=1)
            self.assertEqual((suite.observation_dir / '00001-reply.json').read_text(), 'prior attempt')
            with self.assertRaises(AssertionError):
                observed_class(Base, 'invented')


if __name__ == '__main__':
    unittest.main()
