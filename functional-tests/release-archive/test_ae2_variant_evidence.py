"""Distinct publisher pins need distinct fully validated workflow evidence."""
import copy,sys,unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
from focused_release_evidence_ae2_variants import combine,expected_subjects,SUBJECT_POLICY


def workflow(identity='one',pin='a'):
    return dict(schema='native-v1',target='1.20.1/fabric',case='menu',disposition='functional-pass',original_native_classes=['native.Menu'],
        native_classes=['native.Menu'],subjects={'core':['name.jar','digest']},scope='native control only',limits='no broader claim',
        sealed_evidence={'file':identity,'sha256':identity},acceptance={'file':identity+'-acceptance','sha256':identity},
        publishers=[{'filename':pin+'.jar','sha512':pin,'sha256':pin}],validation={'assertions':12})


class ApprovedSubjects:
    """Structural fixture only; real callers use the independently verified consumer."""
    def expected(self,target,record,current):
        assert target=='1.20.1/fabric'and current=={'core':['name.jar','current']}
        assert record['subjects']in[{'core':['name.jar','digest']},{'core':['name.jar','new-digest']}]
        return {k:tuple(v)for k,v in record['subjects'].items()}


class PublisherEvidenceTests(unittest.TestCase):
    def test_single_keeps_original_record_and_counts(self):
        value=workflow();self.assertEqual(combine([value]),value)

    def test_distinct_actual_workflows_preserve_refs_and_original_counts(self):
        first=workflow();second=workflow('two','b');value=combine([first,second])
        self.assertEqual(value['validation'],first['validation']);self.assertEqual(value['acceptance'],first['acceptance'])
        self.assertEqual(value['additional_workflows'],[second]);self.assertEqual(len(value['publishers']),2)
        self.assertNotIn('additional_workflows',first)

    def test_same_execution_cannot_qualify_second_pin(self):
        with self.assertRaises(AssertionError):combine([workflow(),workflow(pin='b')])

    def test_different_subject_or_contract_rejected(self):
        for key in['schema','target','case','disposition','original_native_classes','native_classes','subjects','scope','limits']:
            second=workflow('two','b');second[key]='changed'
            with self.assertRaises(AssertionError):combine([workflow(),second])

    def test_nested_execution_or_conflicting_publisher_rejected(self):
        second=workflow('two','b');second['additional_workflows']=[workflow('third','c')]
        with self.assertRaises(AssertionError):combine([workflow(),second])

    def mixed(self):
        first=workflow();second=workflow('two','b');second['subjects']={'core':['name.jar','new-digest']}
        return first,second,ApprovedSubjects(),{'core':['name.jar','current']}

    def test_distinct_approved_original_tuples_are_preserved_without_relabeling(self):
        first,second,consumer,current=self.mixed();before=copy.deepcopy((first,second))
        value=combine([first,second],consumer=consumer,current=current)
        self.assertEqual(value['subject_binding_policy'],SUBJECT_POLICY)
        self.assertEqual(value['subjects'],first['subjects'])
        self.assertEqual(value['additional_workflows'][0]['subjects'],second['subjects'])
        self.assertEqual(value['validation']['assertions'],12)
        self.assertEqual((first,second),before)

    def test_mixed_tuples_require_every_original_and_current_binding(self):
        first,second,consumer,current=self.mixed()
        with self.assertRaises(AssertionError):combine([first,second])
        with self.assertRaises(AssertionError):combine([first,second],consumer=consumer)
        with self.assertRaises(AssertionError):combine([first,second],consumer=consumer,current={'core':['name.jar','other']})
        second['subjects']['core'][1]='unapproved'
        with self.assertRaises(AssertionError):combine([first,second],consumer=consumer,current=current)

    def test_mixed_readback_rechecks_policy_baseline_and_actual_original(self):
        first,second,consumer,current=self.mixed();value=combine([first,second],consumer=consumer,current=current)
        self.assertEqual(expected_subjects(value,second,first['subjects'],consumer,current),{'core':('name.jar','new-digest')})
        with self.assertRaises(AssertionError):expected_subjects(value,second,first['subjects'])
        with self.assertRaises(AssertionError):expected_subjects(value,second,second['subjects'],consumer,current)
        wrong=copy.deepcopy(value);wrong['subject_binding_policy']='unreviewed'
        with self.assertRaises(AssertionError):expected_subjects(wrong,second,first['subjects'],consumer,current)

    def test_approved_artifacts_do_not_allow_changed_native_contract_or_duplicate_execution(self):
        for key in ['schema','target','case','disposition','original_native_classes','native_classes','scope','limits']:
            first,second,consumer,current=self.mixed();second[key]='changed'
            with self.assertRaises(AssertionError,msg=key):combine([first,second],consumer=consumer,current=current)
        first,second,consumer,current=self.mixed();second['sealed_evidence']=first['sealed_evidence']
        with self.assertRaises(AssertionError):combine([first,second],consumer=consumer,current=current)
        second=workflow('two');second['publishers'][0]['sha256']='changed'
        with self.assertRaises(AssertionError):combine([workflow(),second])


if __name__=='__main__':unittest.main()
