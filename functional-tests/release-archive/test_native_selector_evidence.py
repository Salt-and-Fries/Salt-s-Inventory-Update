from pathlib import Path
import copy,hashlib,io,sys,tempfile,unittest,zipfile
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
from native_selector_evidence import CASE,MENU,CHECKS,validate_case,exact_module_alias


class NativeSelectorEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.case=dict(id=CASE,status='passed',polymer_browser_checked=True,mod_id='polymer-core',
            entry_native_player_menu=dict(**{'class':MENU}),native_screen_roundtrip=dict(menu=MENU),
            actions=[dict(type='command',command='execute as @p at @s run polymer creative')],
            native_browser_navigation=dict(handler=MENU,category_grid_sha256='a'*64,item_grid_sha256='b'*64,item_sample=['minecraft:oak_log','minecraft:oak_planks']))
        self.checks=[dict(case=CASE,check=name,passed=True)for name in sorted(CHECKS)]
    def test_exact_native_navigation_and_storage_guard_are_required(self):
        self.assertEqual(9,validate_case(self.case,self.checks))
    def test_entry_only_or_absent_publisher_is_never_accepted(self):
        for key,value in [('status','entry-and-policy-only'),('polymer_browser_checked',1),('mod_id','absent')]:
            case=copy.deepcopy(self.case);case[key]=value
            with self.assertRaises(AssertionError):validate_case(case,self.checks)
    def test_wrong_menu_or_missing_real_category_change_is_refused(self):
        for field,value in [('handler','invented.Menu'),('item_grid_sha256','a'*64),('item_sample',[])]:
            case=copy.deepcopy(self.case);case['native_browser_navigation'][field]=value
            with self.assertRaises(AssertionError):validate_case(case,self.checks)
    def test_failed_missing_or_duplicate_assertion_is_refused(self):
        for checks in [self.checks[:-1],self.checks+[self.checks[0]],[dict(v,passed=False)for v in self.checks]]:
            with self.assertRaises(AssertionError):validate_case(self.case,checks)
    def test_unrelated_case_assertions_cannot_fill_missing_native_checks(self):
        with self.assertRaises(AssertionError):validate_case(self.case,[dict(v,case='different-ui')for v in self.checks])
    def test_exact_embedded_module_alias_requires_complete_identical_bytes(self):
        with tempfile.TemporaryDirectory()as folder:
            path=Path(folder)/'native.jar'
            with zipfile.ZipFile(path,'w')as z:z.writestr(MENU.replace('.','/')+'.class',b'fixture')
            raw=path.read_bytes();modules={hashlib.sha256(raw).hexdigest()}
            self.assertTrue(exact_module_alias(path,hashlib.sha512(raw).hexdigest(),modules))
            with zipfile.ZipFile(path,'a')as z:z.writestr('different-resource.txt','changed native jar')
            self.assertFalse(exact_module_alias(path,hashlib.sha512(path.read_bytes()).hexdigest(),modules))
    def test_alias_cannot_hide_wrong_outer_hash_or_missing_handler(self):
        with tempfile.TemporaryDirectory()as folder:
            path=Path(folder)/'native.jar'
            with zipfile.ZipFile(path,'w')as z:z.writestr('unrelated.class',b'fixture')
            raw=path.read_bytes();modules={hashlib.sha256(raw).hexdigest()}
            self.assertFalse(exact_module_alias(path,hashlib.sha512(raw).hexdigest(),modules))
            with self.assertRaises(AssertionError):exact_module_alias(path,'0'*128,modules)


if __name__=='__main__':unittest.main()
