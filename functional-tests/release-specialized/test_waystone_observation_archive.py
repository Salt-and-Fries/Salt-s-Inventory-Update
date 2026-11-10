"""Offline original-call and immutable snapshot controls, not gameplay tests."""
from pathlib import Path
import json, tempfile, unittest
from waystone_observation_archive import ExistingStateArchive,SCHEMA,sha


class ArchiveTests(unittest.TestCase):
    def test_archives_existing_snapshots_and_preserves_each_original_call_once(self):
        with tempfile.TemporaryDirectory()as folder:
            profile=Path(folder);calls=[];reply=dict(sequence=1,client={})
            def original(p,kind,**kwargs):
                calls.append((p,kind,kwargs))
                if kind=='regression-state':
                    (profile/'regression-state.json').write_text(json.dumps(dict(menus=[dict(type='NativeMenu',slots=[])],cursor=dict(count=len(calls),signature='{typed:1b}'))))
                return reply
            archived=ExistingStateArchive(profile,original)
            self.assertIs(archived(profile,'wait',ticks=2),reply)
            self.assertIs(archived(profile,'regression-state',ticks=4),reply)
            old=(archived.out/'00001-server-state.json').read_bytes()
            self.assertIs(archived(profile,'regression-state',ticks=4),reply)
            self.assertEqual(calls,[(profile,'wait',dict(ticks=2)),(profile,'regression-state',dict(ticks=4)),(profile,'regression-state',dict(ticks=4))])
            self.assertEqual(archived.finish(),2)
            self.assertEqual((archived.out/'00001-server-state.json').read_bytes(),old)
            index=json.loads((archived.out/'index.json').read_text())
            self.assertEqual(index['schema'],SCHEMA)
            self.assertEqual([v['ordinal']for v in index['states']],[1,2])
            for record in index['states']:self.assertEqual(sha(archived.out/record['file']),record['sha256'])

    def test_original_error_propagates_without_another_call_or_fake_snapshot(self):
        with tempfile.TemporaryDirectory()as folder:
            profile=Path(folder);calls=[]
            def original(*args,**kwargs):
                calls.append((args,kwargs));raise RuntimeError('original failure')
            archived=ExistingStateArchive(profile,original)
            with self.assertRaisesRegex(RuntimeError,'original failure'):archived(profile,'regression-state',ticks=3)
            self.assertEqual(len(calls),1)
            self.assertEqual(list(archived.out.iterdir()),[])
            with self.assertRaises(AssertionError):archived.finish()

    def test_old_attempts_wrong_profiles_and_changed_snapshots_are_refused(self):
        with tempfile.TemporaryDirectory()as folder:
            profile=Path(folder);calls=[]
            def original(*args,**kwargs):
                calls.append(1);(profile/'regression-state.json').write_text('{}');return {}
            archived=ExistingStateArchive(profile,original)
            with self.assertRaises(FileExistsError):ExistingStateArchive(profile,original)
            with self.assertRaises(AssertionError):archived(profile/'other','regression-state',ticks=3)
            self.assertEqual(calls,[])
            archived(profile,'regression-state',ticks=3)
            (archived.out/'00001-server-state.json').write_text('{"changed":true}')
            with self.assertRaises(AssertionError):archived.finish()

    def test_final_index_cannot_drop_an_existing_native_observation(self):
        with tempfile.TemporaryDirectory()as folder:
            profile=Path(folder)
            def original(*args,**kwargs):
                (profile/'regression-state.json').write_text('{}');return {}
            archived=ExistingStateArchive(profile,original)
            archived(profile,'regression-state',ticks=3)
            (archived.out/'index.json').write_text(json.dumps(dict(schema=SCHEMA,states=[])))
            with self.assertRaises(AssertionError):archived.finish()


if __name__=='__main__':unittest.main()
