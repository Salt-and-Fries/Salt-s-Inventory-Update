"""Independently compiled negative controls for the additional book-only mode."""
from pathlib import Path
import importlib.util
import shutil
import subprocess
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('core_final_delta', ROOT / 'tools/reconcile-core-final-delta.py')
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)
OLD = 'client render container desktop={} window={} menu={} slots={} api={} resizable={} false={} mouse={},{}'
NEW = 'client render container desktop={} window={} menu={} slots={} api={} resizable={} mouse={},{}'
SOURCE = '''package com.salts_inventory_update.client;
import com.salts_inventory_update.debug.DesktopDebug;
public class InventoryDesktopScreen {
    private static final java.net.URI ENCHANTING_BOOK_TEXTURE=java.net.URI.create("BOOK");
    public String desktopId="desktop";
    public int unrelated() { return OTHER; }
    public void renderContainerWindow(int mouseX,int mouseY) {
        String window="window",menu="menu";
        DesktopDebug.detail("MESSAGE",desktopId,window,menu,27,"API",true,ARGUMENTS);
    }
}
'''


class CoreFinalDelta(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(dir=ROOT / 'build/release-readiness')
        cls.folder = Path(cls.temp.name)
        cls.tool = audit.prepare(cls.folder / 'inspector', Path('C:/Program Files/Java/jdk-26'))
        cls.old = cls.compile('old', OLD, 'false,false,mouseX,mouseY', audit.OLD_BOOK)
        cls.new = cls.compile('new', NEW, 'mouseX,mouseY', audit.NEW_BOOK)
        cls.logger = cls.compile('logger', NEW, 'mouseX,mouseY', audit.OLD_BOOK)
        cls.wrong = cls.compile('wrong', NEW, 'mouseX,mouseY', 'textures/wrong.png')
        cls.swapped = cls.compile('swapped', NEW, 'mouseY,mouseX', audit.NEW_BOOK)
        cls.other = cls.compile('other', NEW, 'mouseX,mouseY', audit.NEW_BOOK, other=8)
        cls.outside = cls.compile('outside', OLD, 'false,false,mouseX,mouseY', 'unrelated', outside=True)
        cls.duplicate = cls.compile('duplicate', OLD, 'false,false,mouseX,mouseY', audit.OLD_BOOK, outside=True)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    @classmethod
    def compile(cls, name, message, arguments, book, other=7, outside=False):
        folder = cls.folder / name
        java = folder / 'source/com/salts_inventory_update/client'
        java.mkdir(parents=True)
        debug = java.parent / 'debug'
        debug.mkdir()
        d = debug / 'DesktopDebug.java'
        d.write_text('package com.salts_inventory_update.debug; public final class DesktopDebug { public static void detail(String s,Object... args){} }')
        text = SOURCE.replace('"BOOK"', '"' + book + '"').replace('MESSAGE', message).replace('ARGUMENTS', arguments).replace('OTHER', str(other))
        if outside:
            text = text.replace('return ' + str(other), 'java.net.URI.create("' + audit.OLD_BOOK + '"); return ' + str(other))
        p = java / 'InventoryDesktopScreen.java'
        p.write_text(text)
        classes = folder / 'classes'
        classes.mkdir()
        result = subprocess.run([cls.tool['javac'], '--release', '17', '-g', '-d', str(classes), str(p), str(d)], capture_output=True, text=True)
        assert result.returncode == 0, result.stdout + result.stderr
        jar = folder / 'fixture.jar'
        with zipfile.ZipFile(jar, 'x', zipfile.ZIP_DEFLATED) as archive:
            for member in sorted(classes.rglob('*.class')):
                archive.write(member, member.relative_to(classes).as_posix())
            archive.writestr('layout.json', '{"unchanged":true}')
        return jar

    def accepted(self, before, after, mode, wanted):
        result = audit.execute(self.tool, 'audit-jar', before, after, mode)
        self.assertEqual(result.returncode == 0, wanted, result.stdout + result.stderr)

    def test_exact_book_and_logger_delta_requires_explicit_mode(self):
        self.accepted(self.old, self.new, audit.BOOK_MODE, True)
        self.accepted(self.old, self.new, 'core', False)

    def test_logger_only_mode_still_requires_only_original_delta(self):
        self.accepted(self.old, self.logger, 'core', True)
        self.accepted(self.old, self.logger, audit.BOOK_MODE, False)

    def test_wrong_texture_and_unchanged_old_class_are_rejected(self):
        self.accepted(self.old, self.wrong, audit.BOOK_MODE, False)
        self.accepted(self.old, self.old, audit.BOOK_MODE, False)

    def test_swapped_coordinates_and_unrelated_method_change_are_rejected(self):
        for changed in [self.swapped, self.other]:
            self.accepted(self.old, changed, audit.BOOK_MODE, False)

    def test_literal_outside_exact_initializer_or_duplicate_cannot_be_normalized(self):
        for before in [self.outside, self.duplicate]:
            self.accepted(before, self.new, audit.BOOK_MODE, False)

    def test_unrelated_resources_and_archive_members_remain_strict(self):
        for change in ['edit', 'add', 'remove', 'duplicate']:
            p = self.folder / ('resource-' + change + '.jar')
            import warnings
            with warnings.catch_warnings():
                warnings.simplefilter('ignore', UserWarning)
                with zipfile.ZipFile(self.new) as before, zipfile.ZipFile(p, 'x', zipfile.ZIP_DEFLATED) as after:
                    for item in before.infolist():
                        if item.filename == 'layout.json' and change == 'remove':
                            continue
                        value = before.read(item.filename)
                        if item.filename == 'layout.json' and change == 'edit': value = b'changed'
                        after.writestr(item, value)
                    if change in ['add', 'duplicate']:
                        after.writestr('layout.json' if change == 'duplicate' else 'extra.json', '{}')
            self.accepted(self.old, p, audit.BOOK_MODE, False)

    def test_noncore_archives_are_whole_byte_identical(self):
        self.accepted(self.old, self.old, 'unchanged', True)
        self.accepted(self.old, self.new, 'unchanged', False)

    def test_only_the_two_exact_targets_select_book_amendment(self):
        for target in ['1.20.1/fabric', '1.21.1/neoforge', '26.1.2/fabric', '26.2/neoforge', '26.3/neoforge']:
            self.assertEqual(audit.mode(target, 'core'), 'core')
        for target in audit.BOOK_TARGETS:
            self.assertEqual(audit.mode(target, 'core'), audit.BOOK_MODE)
            for family in ['compatibility', 'controlify']:
                self.assertEqual(audit.mode(target, family), 'unchanged')


if __name__ == '__main__':
    unittest.main()
