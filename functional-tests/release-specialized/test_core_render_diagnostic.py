"""Focused guard checks for the future real render-diagnostic smoke runs."""
from pathlib import Path
import json,tempfile,unittest,zipfile
from core_render_diagnostic import BASE,CONFIG,WANTED,OLD,NEW,script,init_source,prepare,validate_log,loaded_literal

ROOT=Path(__file__).resolve().parents[2]
ROW='client render container desktop=7 window=Chest menu=minecraft:generic_9x3 slots=27 api=vanilla resizable=false mouse=103,-17'


class CoreRenderDiagnostic(unittest.TestCase):
    def test_numeric_coordinates_and_ansi_timestamp_wrapping_are_observations(self):
        rows='[Render thread/INFO] [desktop-detail] '+ROW+'\n\x1b[32m[desktop-detail] '+ROW.replace('103,-17','-1,0')+'\x1b[0m'
        result=validate_log(rows);self.assertEqual(result['render_rows'],2)
        self.assertEqual((result['first']['mouseX'],result['first']['mouseY']),(103,-17))
        self.assertEqual((result['last']['mouseX'],result['last']['mouseY']),(-1,0))

    def test_original_false_argument_shift_and_obsolete_field_are_rejected(self):
        old=ROW.replace('resizable=false mouse=103,-17','resizable=false false=false mouse=false,103')
        with self.assertRaises(AssertionError):validate_log(old)

    def test_boolean_missing_or_unformatted_coordinates_are_rejected(self):
        for value in ['false,103','103','{},{}','103,NaN','103,17 extra','103,17.5']:
            with self.subTest(value=value):
                with self.assertRaises(AssertionError):validate_log(ROW.replace('103,-17',value))

    def test_no_actual_nonempty_rendering_is_not_acceptance(self):
        for value in ['', 'SIU_GAMEPLAY_SUMMARY status=PASS',ROW.replace('slots=27','slots=0')]:
            with self.assertRaises(AssertionError):validate_log(value)

    def test_every_row_must_be_correct_not_only_one_selected_sample(self):
        bad=ROW.replace('103,-17','false,103')
        with self.assertRaises(AssertionError):validate_log(ROW+'\n'+bad+'\n'+ROW)

    def test_original_wrapper_and_assertions_remain_exact_outside_two_declared_edits(self):
        base=(ROOT/BASE).read_text();before=(ROOT/BASE).read_bytes()
        with tempfile.TemporaryDirectory(dir=ROOT/'build/release-readiness')as directory:
            folder=Path(directory).resolve();self.assertTrue(folder.is_relative_to(ROOT/'build/release-readiness'))
            wrapper,init=prepare(ROOT,folder)
            generated=wrapper.read_text();self.assertEqual(generated,script(base,ROOT,init))
            restored=generated.replace("$repo='"+str(ROOT).replace("'","''")+"'","$repo=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path",1)
            restored=restored.replace("$argsList+=@('-I','"+init.relative_to(ROOT).as_posix()+"')\n",'',1)
            self.assertEqual(restored,base)
            self.assertIn('$report.failures -ne 0',generated)
            self.assertEqual(init.read_text(),init_source(ROOT,folder))
        self.assertEqual((ROOT/BASE).read_bytes(),before)

    def test_config_is_scoped_to_fresh_runtime_and_does_not_change_java_tests(self):
        text=init_source(ROOT,ROOT/'build/release-readiness/example-only')
        self.assertEqual(WANTED,{'enableMod':True,'enableDetailedConsoleLogs':True})
        self.assertIn(CONFIG,text);self.assertIn('config.exists()',text)
        self.assertIn('existingConfigOverwritten:false',text)
        self.assertNotIn('qa.java',text);self.assertNotIn('setDetailedConsoleLogs(',text)
        with self.assertRaises(AssertionError):script((ROOT/BASE).read_text(),ROOT,ROOT.parent/'outside.gradle')

    def test_loaded_class_literal_probe_rejects_obsolete_mixed_or_nonclass_members(self):
        # Literal-detector fixtures only; compiled class semantics are separately
        # checked by the strict ASM auditor and actual in-game execution.
        with tempfile.TemporaryDirectory(dir=ROOT/'build/release-readiness')as directory:
            folder=Path(directory).resolve();self.assertTrue(folder.is_relative_to(ROOT/'build/release-readiness'))
            for name,value,ok in [('new',b'\xca\xfe\xba\xbe'+NEW.encode(),True),('old',b'\xca\xfe\xba\xbe'+OLD.encode(),False),
                    ('mixed',b'\xca\xfe\xba\xbe'+NEW.encode()+OLD.encode(),False),('not-class',NEW.encode(),False)]:
                path=folder/(name+'.jar')
                with zipfile.ZipFile(path,'x')as jar:jar.writestr('com/salts_inventory_update/client/InventoryDesktopScreen.class',value)
                if ok:self.assertTrue(loaded_literal(path))
                else:
                    with self.assertRaises(AssertionError):loaded_literal(path)


if __name__=='__main__':unittest.main(verbosity=2)
