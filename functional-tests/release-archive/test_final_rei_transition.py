"""Keep the combined final mode narrow without weakening old Core-only modes."""
from pathlib import Path
import copy,sys,unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
from focused_release_evidence_core_log_transition import sources,rei_smokes,REI_PLAN


class FinalReiTransition(unittest.TestCase):
    def fixture(self):
        versions=['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']
        core=[dict(source=dict(file=f'versions/{mc}/fabric/src/main/java/com/salts_inventory_update/client/InventoryDesktopScreen.java',sha256='a'*64),prepared_source=dict(file='prepared-core/'+mc,sha256='b'*64))for mc in versions]
        rei=[]
        for mc in versions:
            for loader in ['fabric','forge'if mc=='1.20.1'else'neoforge']:
                name='SaltsReiClientPlugin'if loader=='fabric'else'SaltsReiForgeClientPlugin'
                rei.append(dict(kind='existing-plugin-one-public-registration-method',source=dict(file=f'compatibility/versions/{mc}/{loader}/src/main/java/com/salts_inventory_update/compat/rei/{name}.java',sha256='c'*64),prepared_source=dict(file=f'prepared-rei/{mc}/{loader}',sha256='d'*64)))
            rei.append(dict(kind='new-exact-screen-policy',source=dict(file=f'compatibility/versions/{mc}/fabric/src/main/java/com/salts_inventory_update/compat/rei/WorkspaceReiOverlayPolicy.java',sha256=None),prepared_source=dict(file=f'prepared-policy/{mc}',sha256='e'*64)))
        before={r['source']['file']:r['source']['sha256']for r in core+rei if r['source']['sha256']is not None}
        after={r['source']['file']:r['prepared_source']['sha256']for r in core+rei}
        before['tools/unchanged.py']=after['tools/unchanged.py']='f'*64
        return dict(source_files=before),dict(source_files=after),dict(rows=core),dict(schema='exact-REI-controller-workspace-overlay-policy-v1',rows=rei)
    def test_exact_twenty_four_sources(self):
        self.assertEqual(24,len(sources(*self.fixture())))
    def test_old_core_only_mode_still_rejects_rei(self):
        a,b,c,d=self.fixture()
        with self.assertRaises(AssertionError):sources(a,b,c)
    def test_rejects_extra_source_change(self):
        a,b,c,d=self.fixture();b['source_files']['tools/unchanged.py']='0'*64
        with self.assertRaises(AssertionError):sources(a,b,c,d)
    def test_missing_policy_is_not_equivalent(self):
        a,b,c,d=self.fixture();d['rows'].pop()
        with self.assertRaises(AssertionError):sources(a,b,c,d)
    def test_existing_policy_cannot_replace_new_class_contract(self):
        a,b,c,d=self.fixture();r=next(v for v in d['rows']if v['kind']=='new-exact-screen-policy');a['source_files'][r['source']['file']]='a'*64
        with self.assertRaises(AssertionError):sources(a,b,c,d)
    def test_wrong_native_plugin_name(self):
        a,b,c,d=self.fixture();r=d['rows'][0];old=r['source']['file'];new=old.replace('SaltsReiClientPlugin','OtherClientPlugin');r['source']['file']=new;a['source_files'][new]=a['source_files'].pop(old);b['source_files'][new]=b['source_files'].pop(old)
        with self.assertRaises(AssertionError):sources(a,b,c,d)
    def test_wrong_prepared_bytes(self):
        a,b,c,d=self.fixture();d['rows'][0]['prepared_source']['sha256']='0'*64
        with self.assertRaises(AssertionError):sources(a,b,c,d)
    def test_both_original_native_plans_and_receipts_required(self):
        q=dict(status='passed',plan=copy.deepcopy(REI_PLAN),runs=[dict(**p,status='passed',exit_code=0,summary={'file':p['run']},acceptance={'file':'receipt-'+p['run']})for p in REI_PLAN])
        self.assertEqual(2,len(rei_smokes(q)))
        for mutation in ['missing','duplicate','failed','boolean','other-driver','receipt','other-plan']:
            value=copy.deepcopy(q)
            if mutation=='missing':value['runs'].pop()
            if mutation=='duplicate':value['runs'][1]=value['runs'][0]
            if mutation=='failed':value['runs'][0]['status']='failed'
            if mutation=='boolean':value['runs'][0]['exit_code']=False
            if mutation=='other-driver':value['runs'][0]['driver']='other.py'
            if mutation=='receipt':value['runs'][0].pop('acceptance')
            if mutation=='other-plan':value['plan'][0]['run']='another-run'
            with self.subTest(mutation=mutation),self.assertRaises(AssertionError):rei_smokes(value)


if __name__=='__main__':unittest.main()
