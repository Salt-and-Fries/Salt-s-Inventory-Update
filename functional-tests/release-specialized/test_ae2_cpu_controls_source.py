"""Supplemental source transformation and native control observation guards."""
from pathlib import Path
import copy,unittest
import tempfile
from ae2_cpu_controls_source import BASE,TARGETS,enhance,preserves_base,validate_controls

ROOT=Path(__file__).resolve().parents[2]


def fixture(target='1.21.1/neoforge'):
    modern=not target.startswith('1.20.1/');cycles=[]
    for number in [1,2]:
        labels=(['fixture-held-suspended']if modern else[])+['initial-active','mode-1','mode-2','mode-3']+(['suspended','resumed']if modern else[])
        modes=(['ANY']if modern else[])+['ANY','PLAYER_ONLY','MACHINE_ONLY','ANY']+(['ANY','ANY']if modern else[])
        states=[dict(label=label,clientMode=mode,clientActive=True,clientSuspended=label in {'fixture-held-suspended','suspended'},
            server=dict(mode=mode,active=True,suspended=label in {'fixture-held-suspended','suspended'}))for label,mode in zip(labels,modes)]
        cycles.append(dict(cycle=number,modeTransitions=3,suspendAvailable=modern,
            hold='native-suspend-then-declared-assembler-removal'if modern else'original-legacy-assembler-removal',states=states))
    controls=dict(scope='Synthetic native-control observation fixture; no gameplay.',cycles=cycles)
    lifecycle=dict(nativeJobSubmitted=True,clientActiveStateReceived=True,cancelClickedThroughDesktop=True,
        clientIdleStateReceived=True,serverCancellationVerified=True,clientStatusFieldsAssignedByFixture=False,
        nativeJobsSubmitted=2,serverCancellationsVerified=2)
    log='\n'.join('CPU LIFECYCLE VERIFIED cycle='+str(i)+' output=256 acacia planks'for i in [1,2])
    return controls,lifecycle,log


class CpuControlsSource(unittest.TestCase):
    def test_additive_separate_copy_preserves_every_original_lifecycle_statement(self):
        base=(ROOT/BASE).read_text()
        for mc in {v.split('/')[0]for v in TARGETS}:
            result=enhance(base,mc);self.assertTrue(preserves_base(base,result,mc))
            self.assertIn('verifyAdditionalControls(mc, screen, origin, output, click)',result)
            self.assertEqual('job.cpu().craftingLogic.isJobSuspended()'in result,mc!='1.20.1')
            self.assertEqual('value.isSuspended()'in result,mc!='1.20.1')
        self.assertEqual((ROOT/BASE).read_text(),base)

    def test_changed_original_assertion_or_unknown_anchor_rejected(self):
        base=(ROOT/BASE).read_text();result=enhance(base,'26.3')
        for old,new in [('Client Cancel did not cancel the server job','ignored cancellation'),('completedCycles < 2','completedCycles < 1')]:
            with self.assertRaises(AssertionError):preserves_base(base,result.replace(old,new),'26.3')
        with self.assertRaises(AssertionError):enhance(base.replace('    private static Job job;',''),'26.3')

    def test_archived_crlf_source_and_generated_copy_roundtrip(self):
        base=(ROOT/BASE).read_text();enhanced=enhance(base,'26.3')
        self.assertTrue(preserves_base(base.replace('\n','\r\n'),enhanced.replace('\n','\r\n'),'26.3'))
        self.assertEqual(enhance(base.replace('\n','\r\n'),'26.3'),enhanced)

    def test_all_targets_require_real_two_cycle_synchronized_control_observations(self):
        for target in TARGETS:
            checks=validate_controls(target,*fixture(target))
            self.assertEqual(len(checks),13 if target.startswith('1.20.1/')else 19)
            self.assertTrue(all(v['passed']is True for v in checks))

    def test_mode_duplicate_loss_type_and_server_disagreement_rejected(self):
        for mode in ['repeat','server','missing','bool-count','persist']:
            controls,lifecycle,log=fixture()
            if mode=='repeat':controls['cycles'][0]['states'][3]['clientMode']='PLAYER_ONLY';controls['cycles'][0]['states'][3]['server']['mode']='PLAYER_ONLY'
            if mode=='server':controls['cycles'][0]['states'][2]['server']['mode']='ANY'
            if mode=='missing':controls['cycles'][0]['states'].pop(1)
            if mode=='bool-count':controls['cycles'][0]['modeTransitions']=True
            if mode=='persist':controls['cycles'][1]['states'][0]['clientMode']='PLAYER_ONLY'
            with self.assertRaises(AssertionError,msg=mode):validate_controls('1.21.1/neoforge',controls,lifecycle,log)

    def test_suspend_resume_job_lifetime_and_legacy_capabilities_rejected(self):
        for mode in ['client','server','lost-job','resume','legacy-extra']:
            target='1.20.1/fabric'if mode=='legacy-extra'else'26.3/neoforge'
            controls,lifecycle,log=fixture(target)
            if mode=='client':controls['cycles'][0]['states'][5]['clientSuspended']=False
            if mode=='server':controls['cycles'][0]['states'][5]['server']['suspended']=False
            if mode=='lost-job':controls['cycles'][1]['states'][5]['server']['active']=False
            if mode=='resume':controls['cycles'][1]['states'][6]['server']['suspended']=True
            if mode=='legacy-extra':controls['cycles'][0]['suspendAvailable']=True
            with self.assertRaises(AssertionError,msg=mode):validate_controls(target,controls,lifecycle,log)

    def test_failed_cancellation_assigned_status_or_fabricated_cycle_count_rejected(self):
        for mode in ['cancel','assigned','bool','missing-log','duplicate-log']:
            controls,lifecycle,log=fixture()
            if mode=='cancel':lifecycle['serverCancellationVerified']=False
            if mode=='assigned':lifecycle['clientStatusFieldsAssignedByFixture']=True
            if mode=='bool':lifecycle['nativeJobsSubmitted']=True
            if mode=='missing-log':log=log.splitlines()[0]
            if mode=='duplicate-log':log+='\n'+log
            with self.assertRaises(AssertionError,msg=mode):validate_controls('1.21.1/neoforge',controls,lifecycle,log)

    def test_extra_fixture_reads_state_and_clicks_widgets_without_setting_native_status(self):
        base=(ROOT/BASE).read_text();result=enhance(base,'26.3')
        for forbidden in ['setJobSuspended(','.cpuStatusReady =','.cpuAvailable =','.hasActiveJob =','setSchedulingMode(']:
            self.assertNotIn(forbidden,result)
        self.assertIn('click.accept(widget.getX()',result)
        self.assertIn('job.cpu().getConfigManager().getSetting',result)

    def test_original_modern_suspended_hold_requires_native_resume_and_world_restore(self):
        controls,lifecycle,log=fixture()
        for mode in ['missing-hold','wrong-hold','already-resumed','resume-not-received']:
            changed=copy.deepcopy(controls)
            if mode=='missing-hold':changed['cycles'][0]['states'].pop(0)
            if mode=='wrong-hold':changed['cycles'][0]['hold']='original-legacy-assembler-removal'
            if mode=='already-resumed':changed['cycles'][0]['states'][0]['server']['suspended']=False
            if mode=='resume-not-received':changed['cycles'][0]['states'][1]['clientSuspended']=True
            with self.assertRaises(AssertionError,msg=mode):validate_controls('1.21.1/neoforge',changed,lifecycle,log)
        result=enhance((ROOT/BASE).read_text(),'26.3')
        self.assertIn('prepareControlsHold(mc, origin)',result)
        self.assertIn('release.run();\n                releaseControlsHold(mc, origin);',result)
        self.assertIn('level.setBlock(pos, controlsAssemblerState, 3)',result)

    def test_preparation_isolates_generated_sources_and_preserves_original_wrapper(self):
        from run_ae2_cpu_controls import prepare
        wrapper=ROOT/'functional-tests/scripts/Run-Ae2ReleaseQa.ps1';before=wrapper.read_bytes()
        with tempfile.TemporaryDirectory(dir=ROOT/'build/release-readiness')as temporary:
            folder=Path(temporary);prepared=prepare(folder,'1.20.1')
            java=list((folder/'generated-shared').rglob('NativeCpuLifecycleQa.java'));self.assertEqual(len(java),1)
            self.assertTrue(preserves_base((ROOT/BASE).read_text(),java[0].read_text(),'1.20.1'))
            self.assertIn('qa.java.setSrcDirs', (folder/'cpu-controls.init.gradle').read_text())
            self.assertIn('functional-tests/ae2-release-qa.init.gradle',prepared['script'].read_text())
            self.assertIn('cpu-controls.init.gradle',prepared['script'].read_text())
        self.assertEqual(wrapper.read_bytes(),before)


if __name__=='__main__':unittest.main(verbosity=2)
