import copy,unittest
from rei_workspace_policy_acceptance import REQUIRED,WARNING,verify_observation


class ReiWorkspaceAcceptanceTests(unittest.TestCase):
    def setUp(self):
        self.report=dict(scope='mod-compat',failures=0,results=[dict(status='PASS',name=n)for n in sorted(REQUIRED)]+[
            dict(status='CAPTURED',name='compat-browser-controls'),dict(status='CAPTURED',name='compat-browser-recipe-virtual-slots')])
        self.log='native com.salts_inventory_update.controlify.RadialInventoryScreen active';self.config='{ overlayVisible: true }'
    def verify(self):return verify_observation(self.report,self.log,self.config)
    def test_accepts_complete_observation(self):self.assertTrue(self.verify()['global_REI_overlay_enabled'])
    def reject(self):
        with self.assertRaises(AssertionError):self.verify()
    def test_repeated_warning_is_not_ignored(self):self.log+='\n[WARN] '+WARNING;self.reject()
    def test_global_overlay_disabled(self):self.config='{ overlayVisible: false }';self.reject()
    def test_missing_overlay_setting(self):self.config='{}';self.reject()
    def test_workspace_never_observed(self):self.log='native ordinary screen';self.reject()
    def test_missing_native_render_check(self):self.report['results'].pop(0);self.reject()
    def test_missing_framebuffer_capture(self):self.report['results'].pop();self.reject()
    def test_native_failure_cannot_be_relabelled(self):self.report['failures']=1;self.reject()
    def test_failed_row_with_zero_failure_counter(self):self.report['results'].append(dict(status='FAIL',name='other'));self.reject()


if __name__=='__main__':unittest.main()
