"""Reject missing rendering proof without relaxing the original assertions."""
import copy
from pathlib import Path
import sys
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
from focused_release_evidence_controller_frames import STAGES, verify_frame_observations


class ControllerFrameObservationTests(unittest.TestCase):
    def report(self):
        names=[*STAGES,*(v+'-completed-native-frame' for v in STAGES),
               'tabs-rendered-after-transition','disabled-animation-tabs-rendered']
        return dict(failures=0, results=[dict(name=name,status='PASS') for name in names],
            qaNativeFrameObservations=[dict(stage=name,startCompletedFrame=80,observedCompletedFrame=81,
                waitTicks=3,tabFrames=12,elapsedMs=450) for name in STAGES])

    def test_real_completed_frame_and_original_assertions(self):
        self.assertTrue(verify_frame_observations(self.report()))

    def test_catch_up_ticks_do_not_count_as_a_frame(self):
        report=self.report()
        report['qaNativeFrameObservations'][1].update(observedCompletedFrame=80,waitTicks=100)
        with self.assertRaises(AssertionError): verify_frame_observations(report)

    def test_original_rendering_assertion_cannot_be_removed(self):
        report=self.report()
        report['results']=[r for r in report['results'] if r['name']!='disabled-animation-tabs-rendered']
        with self.assertRaises(AssertionError): verify_frame_observations(report)

    def test_original_failure_cannot_be_relabelled(self):
        report=self.report();report['results'][-1]['status']='FAIL'
        with self.assertRaises(AssertionError): verify_frame_observations(report)

    def test_both_rendering_stages_are_required(self):
        report=self.report();report['qaNativeFrameObservations'].pop()
        with self.assertRaises(AssertionError): verify_frame_observations(report)

    def test_unknown_extra_stage_is_rejected(self):
        report=self.report();report['qaNativeFrameObservations'].append(copy.deepcopy(report['qaNativeFrameObservations'][0]))
        with self.assertRaises(AssertionError): verify_frame_observations(report)

    def test_deadline_cannot_be_silently_extended(self):
        report=self.report();report['qaNativeFrameObservations'][0]['elapsedMs']=45_001
        with self.assertRaises(AssertionError): verify_frame_observations(report)

    def test_boolean_is_not_a_native_frame_count(self):
        report=self.report();report['qaNativeFrameObservations'][0]['observedCompletedFrame']=True
        with self.assertRaises(AssertionError): verify_frame_observations(report)


if __name__=='__main__': unittest.main()
