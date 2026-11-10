"""Reject incomplete or overlapping native Drive geometry receipts."""
from pathlib import Path
import copy, sys, unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[2]/'tools'))
from focused_release_evidence_ae2_drive_gutter import verify_drive_geometry


class DriveGutterTests(unittest.TestCase):
    def setUp(self):
        self.window=[244,77,176,112]
        self.controls=[dict(kind=k,x=x,y=79,width=11,height=11)for k,x in zip(
            ['FOCUS','PIN','LOCK','LINK','CLOSE'],[324,338,352,366,380])]
        self.geometry={f'slot/STORAGE/{i}':[x,y]for i,(x,y)in enumerate(
            ((x,y)for x in [71,89]for y in [14,32,50,68,86]))}
    def test_native_geometry(self):
        self.assertTrue(verify_drive_geometry(self.controls,self.geometry,self.window))
    def reject(self):
        with self.assertRaises(AssertionError):verify_drive_geometry(self.controls,self.geometry,self.window)
    def test_old_four_pixel_literal_overlaps(self):
        for c in self.controls:c['y']+=2
        self.reject()
    def test_missing_control(self):
        self.controls.pop();self.reject()
    def test_wrong_control_order(self):
        self.controls.reverse();self.reject()
    def test_control_clipped(self):
        self.controls[-1]['x']=self.window[0]+self.window[2]-5;self.reject()
    def test_changed_control_size(self):
        self.controls[0]['height']=12;self.reject()
    def test_missing_native_cell(self):
        self.geometry.pop(next(iter(self.geometry)));self.reject()
    def test_grid_moved_to_make_test_pass(self):
        for v in self.geometry.values():v[1]+=2
        self.reject()
    def test_noninteger_bounds(self):
        self.controls[0]['y']=79.0;self.reject()


if __name__=='__main__':unittest.main()
