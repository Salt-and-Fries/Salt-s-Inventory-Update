import json,tempfile,unittest
from pathlib import Path
from display_fixture import configure

class DisplayFixtureTests(unittest.TestCase):
    def test_only_display_options_change_and_duplicate_keys_are_removed(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);p=root/'options.txt'
            p.write_text('guiScale:2\nkey_key.attack:key.mouse.left\nmaxFps:120\ninactivityFpsLimit:"afk"\nmaxFps:10\n')
            proof=configure(root,root);text=p.read_text()
            self.assertIn('guiScale:2\nkey_key.attack:key.mouse.left\n',text)
            self.assertEqual(1,text.count('maxFps:'));self.assertIn('maxFps:60',text)
            self.assertIn('inactivityFpsLimit:"minimized"',text)
            self.assertNotEqual(proof['before_options_sha256'],proof['options_sha256'])
    def test_missing_options_uses_display_only_defaults(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);configure(root,root)
            self.assertEqual({'maxFps','inactivityFpsLimit','pauseOnLostFocus'},
                {line.split(':',1)[0] for line in (root/'options.txt').read_text().splitlines()})
    def test_repeated_preparation_does_not_duplicate_settings(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder);first=configure(root,root);second=configure(root,root)
            self.assertEqual(first['options_sha256'],second['options_sha256'])
            self.assertEqual(second,json.loads((root/'qa-display-settings.json').read_text()))
    def test_invalid_profile_cannot_create_a_runtime_folder(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder)
            with self.assertRaises(AssertionError):configure(root/'missing',root)
            self.assertFalse((root/'missing').exists())

if __name__=='__main__':unittest.main()
