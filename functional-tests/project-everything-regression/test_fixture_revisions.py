"""Changed publisher/runtime pins require complete matching reproduction."""
import unittest,json,tempfile
from pathlib import Path
from run_followups import revision_requires_retest
from prepare_recent_fixtures import write_fixture,is_publisher_input

class FixtureRevisionTests(unittest.TestCase):
    fixture={'regression_requires_full_retest':True,'artifacts':[{'filename':'publisher.jar','sha512':'new'}],
             'runtime':{'fabric_api':'0.116.8+1.21.1'}}
    artifacts=[{'family':'upstream','filename':'publisher.jar','sha512':'new'},
               {'family':'runtime-api','filename':'fabric-api-0.116.8+1.21.1.jar','sha512':'pinned-api'}]
    def test_complete_attempt_already_using_the_revision_needs_no_redundant_replay(self):
        self.assertFalse(revision_requires_retest(self.fixture,self.artifacts))
    def test_changed_publisher_or_api_still_requires_whole_replay(self):
        for field,value,index in [('sha512','old',0),('filename','fabric-api-0.116.5+1.21.1.jar',1),('sha512',None,1)]:
            rows=[dict(a) for a in self.artifacts];rows[index][field]=value
            self.assertTrue(revision_requires_retest(self.fixture,rows))
    def test_missing_artifact_evidence_cannot_certify_a_revision(self):
        self.assertTrue(revision_requires_retest(self.fixture,[]))
        self.assertTrue(revision_requires_retest(self.fixture,self.artifacts[1:]))
    def test_preparation_does_not_restore_an_incompatible_historical_pin(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'fixture.json'
            reviewed={'minecraft':'1.21.1','loader':'fabric','runtime':{'fabric_api':'new'},'regression_fixture_revision':{'reason':'native ABI requirement'}}
            path.write_text(json.dumps(reviewed));before=path.read_bytes()
            write_fixture(path,{'minecraft':'1.21.1','loader':'fabric','runtime':{'fabric_api':'old'}})
            self.assertEqual(before,path.read_bytes())
    def test_new_fixture_is_written_normally(self):
        with tempfile.TemporaryDirectory() as folder:
            path=Path(folder)/'fixture.json';data={'minecraft':'1.21.1','loader':'fabric','artifacts':[]}
            write_fixture(path,data);self.assertEqual(data,json.loads(path.read_text()))
    def test_observers_are_excluded_by_recorded_role_and_staged_filename(self):
        self.assertFalse(is_publisher_input({'family':'qa','file':'observer-srg.jar','filename':'native-ui-reference.jar'}))
        self.assertFalse(is_publisher_input({'file':'observer-srg.jar','filename':'native-ui-reference.jar'}))
        self.assertFalse(is_publisher_input({'family':'core','filename':'arbitrary-name.jar'}))
        self.assertTrue(is_publisher_input({'family':'upstream','filename':'reference-guide.jar'}))

if __name__=='__main__':unittest.main()
