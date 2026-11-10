"""A pass on a superseded jar is not proof of the current release."""
import unittest
from subject_artifacts import subjects_match,subject_signature

class SubjectArtifactTests(unittest.TestCase):
    expected={'core':('core.jar','new-core'),'compatibility':('compat.jar','new-compat')}
    def rows(self):return [{'family':f,'filename':v[0],'sha256':v[1]} for f,v in self.expected.items()]
    def test_both_exact_subjects_are_required(self):
        self.assertTrue(subjects_match(self.rows(),self.expected))
        self.assertFalse(subjects_match(self.rows()[:1],self.expected))
        self.assertFalse(subjects_match([],self.expected))
    def test_changed_core_or_compatibility_requires_a_fresh_cohort(self):
        for index in range(2):
            rows=self.rows();rows[index]['sha256']='old'
            self.assertFalse(subjects_match(rows,self.expected))
    def test_observer_and_publisher_changes_are_not_subject_identity(self):
        rows=self.rows()+[{'family':'qa','filename':'observer.jar','sha256':'qa'}, {'family':'upstream','filename':'publisher.jar','sha256':'mod'}]
        self.assertTrue(subjects_match(rows,self.expected))
        self.assertEqual(self.expected,subject_signature(rows))

if __name__=='__main__':unittest.main()
