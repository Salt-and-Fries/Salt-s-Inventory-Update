"""The extra publisher run must use its own existing exact cohort and pins."""
from pathlib import Path
import hashlib,importlib.util,json,sys,unittest
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/release-specialized'))
import run_techreborn_status as m

class FixtureSelection(unittest.TestCase):
    def test_default_preserves_original_5814_cohort(self):
        cohort,name,path=m.selection('1.20.1/fabric')
        self.assertEqual(name,'1201-fabric-prominent-shop-tail-v1')
        self.assertEqual(m.FIXTURES['1.20.1/fabric'][cohort],name)
        artifacts=json.loads(path.read_text())['artifacts']
        self.assertEqual([v['filename']for v in artifacts if v['filename'].lower().startswith('techreborn-')],['TechReborn-5.8.14.jar'])
    def test_explicit_5815_uses_reviewed_index_and_actual_native_bytes(self):
        cohort,name,path=m.selection('1.20.1/fabric','regression-techreborn-current-1201-fabric')
        self.assertEqual(m.FIXTURES['1.20.1/fabric'][cohort],name)
        pins=[v for v in json.loads(path.read_text())['artifacts']if v['filename'].lower().startswith(('techreborn-','reborncore-'))]
        self.assertEqual({v['filename']for v in pins},{'TechReborn-5.8.15.jar','RebornCore-5.8.15.jar'})
        expected={'TechReborn-5.8.15.jar':'bc4feb57728ba2bb2332b91f907e7cdfd13d0eb6d1c19f38c51268ecf8bc1d5c8a8e552181d92bf8ef22f5e536d7a3d0575167c6e2536cac51bdb0597c1628a5','RebornCore-5.8.15.jar':'74eb4f9614a2d756dc4724789563ebaabed25ff018c324c9e7b375b005e085a161b7a2895e0a1168f43219eaa17254c7f6fb712eb347b86387f5cda3131a3bcf'}
        for pin in pins:
            raw=Path(pin['path']).read_bytes()
            self.assertEqual(hashlib.sha512(raw).hexdigest(),expected[pin['filename']])
    def test_other_target_unknown_fixture_or_nontech_fixture_is_refused(self):
        for target,name in [('1.20.1/forge',None),('1.21.1/fabric','regression-techreborn-current-1201-fabric'),('1.20.1/fabric','unknown'),('1.20.1/fabric','1201-fabric-inventory-batch-v2')]:
            with self.subTest(target=target,name=name):
                with self.assertRaises(AssertionError):m.selection(target,name)

if __name__=='__main__':unittest.main(verbosity=2)
