"""Candidate identity includes actual build/test inputs without generated caches."""
from pathlib import Path
import importlib.util,tempfile,unittest
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('release_source_snapshot',ROOT/'tools/record-release-candidate.py')
snapshot=importlib.util.module_from_spec(spec);spec.loader.exec_module(snapshot)

class SourceSnapshot(unittest.TestCase):
    def test_build_helpers_assets_untracked_sources_and_fixtures_are_identified(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            files={'tools/build.py':b'helper','artwork/original.pdn':b'editable','controlify/sound.ogg':b'audio',
                'functional-tests/fixtures/player.dat':b'fixture','versions/a/New.java':b'untracked source',
                'tools/__pycache__/cache.pyc':b'cache','versions/a/build/derived.class':b'compiled',
                'functional-tests/results/run.log':b'mutable result'}
            for name in snapshot.ROOT_INPUTS:files[name]=b'root input'
            for name,data in files.items():
                path=root/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
            with patch.object(snapshot,'ROOT',root):first=snapshot.source_files()
            for name in ['tools/build.py','artwork/original.pdn','controlify/sound.ogg','functional-tests/fixtures/player.dat','versions/a/New.java']:
                self.assertIn(name,first)
            for name in ['tools/__pycache__/cache.pyc','versions/a/build/derived.class','functional-tests/results/run.log']:self.assertNotIn(name,first)
            (root/'tools/build.py').write_bytes(b'changed helper')
            with patch.object(snapshot,'ROOT',root):second=snapshot.source_files()
            self.assertNotEqual(first['tools/build.py'],second['tools/build.py'])

if __name__=='__main__':unittest.main()
