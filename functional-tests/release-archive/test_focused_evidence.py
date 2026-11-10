"""Mutation controls for focused source/runtime/state evidence, not gameplay tests."""
from pathlib import Path
import copy,hashlib,json,sys,tempfile,unittest,zipfile

ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
from focused_release_evidence import collect,seal,verify,sha


class FocusedEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.addCleanup(self.temp.cleanup);self.root=Path(self.temp.name)
        self.run='synthetic-validation';self.profile=self.root/'build/profile';self.profile.mkdir(parents=True)
        self.folder=self.root/'output/release-readiness'/self.run;self.folder.mkdir(parents=True)
        self.runtime=self.root/'runtime/mods';self.runtime.mkdir(parents=True)
        stage=self.root/'build/stage';stage.mkdir();self.artifacts=[]
        for family,name in [('core','core.jar'),('compatibility','compatibility.jar'),('qa','qa.jar')]:
            file=stage/name;file.write_bytes(('distinct-'+family).encode());(self.runtime/name).write_bytes(file.read_bytes())
            self.artifacts.append(dict(family=family,filename=name,file=str(file),sha256=sha(file)))
        source=stage/'source/com/QA.java';source.parent.mkdir(parents=True);source.write_text('class QA {}',encoding='utf-8')
        self.driver=self.root/'functional-tests/driver.py';self.driver.parent.mkdir();self.driver.write_text('assert result\n',encoding='utf-8')
        self.expected={v['family']:(v['filename'],v['sha256'])for v in self.artifacts if v['family']!='qa'}
        self.staged=self.profile/'staged-artifacts.json';self.staged.write_text(json.dumps(dict(artifacts=self.artifacts)),encoding='utf-8')
        (self.profile/'launch.json').write_text(json.dumps(dict(cwd=str(self.runtime.parent))),encoding='utf-8')
        self.feature=self.profile/'feature';self.feature.mkdir();self.journal=self.feature/'summary.json'
        self.journal.write_text(json.dumps(dict(complete=True,cases=['synthetic'],checks=[dict(name='fixture-only',passed=True)],scope='Synthetic validator fixture only')),encoding='utf-8')
        self.state=self.feature/'full-state.json';self.state.write_text('{"inventory":[],"cursor":"{}"}',encoding='utf-8')
        self.summary=dict(status='passed',target='1.20.1/fabric',subjects=self.expected,
            qa_inputs={self.driver.relative_to(self.root).as_posix():sha(self.driver)},
            acceptance=dict(file=self.journal.relative_to(self.root).as_posix(),sha256=sha(self.journal)),
            results=[dict(status='passed',exit_code=0,target='1.20.1/fabric',profile=self.profile.relative_to(self.root).as_posix(),results=dict(cases=1,failed=0,synthetic_only=True))])
        self.save()
    def save(self):
        (self.folder/'summary.json').write_text(json.dumps(self.summary),encoding='utf-8')
    def proof(self):
        return json.loads(seal(self.root,self.run,self.expected).read_text(encoding='utf-8'))
    def test_valid_source_archive_survives_later_driver_edits_without_broadening(self):
        proof=self.proof();self.driver.write_text('changed for a later test\n',encoding='utf-8')
        self.assertTrue(verify(self.root,proof,self.expected))
        self.assertEqual('not-assigned-requires-native-case-review',proof['coverage_disposition'])
        self.assertTrue(any(v.endswith('.java')for v in proof['archived_inputs']))
    def test_failed_or_noninteger_exit_is_refused(self):
        self.summary['status']='failed';self.save()
        with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
        self.summary['status']='passed';self.summary['results'][0]['exit_code']=False;self.save()
        with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
    def test_wrong_or_duplicate_subject_is_refused(self):
        self.artifacts.append(copy.deepcopy(self.artifacts[0]));self.staged.write_text(json.dumps(dict(artifacts=self.artifacts)),encoding='utf-8')
        with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
        self.artifacts.pop();self.artifacts[0]['sha256']='0'*64;self.staged.write_text(json.dumps(dict(artifacts=self.artifacts)),encoding='utf-8')
        with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
    def test_changed_runtime_copy_is_refused(self):
        (self.runtime/'core.jar').write_bytes(b'wrong-runtime')
        with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
    def test_changed_qa_input_is_refused_before_archival(self):
        self.driver.write_text('changed\n',encoding='utf-8')
        with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
    def test_missing_generated_observer_is_refused(self):
        (self.root/'build/stage/source/com/QA.java').unlink()
        with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
    def test_incomplete_or_empty_assertion_journal_is_refused(self):
        for journal in [dict(complete=False,checks=[dict(passed=True)]),dict(complete=True,checks=[]),dict(complete=True,checks=[dict(passed=1)])]:
            self.journal.write_text(json.dumps(journal),encoding='utf-8');self.summary['acceptance']['sha256']=sha(self.journal);self.save()
            with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
    def test_changed_sealed_state_or_assertion_count_is_refused(self):
        proof=self.proof();changed=copy.deepcopy(proof);changed['assertions']=999
        with self.assertRaises(AssertionError):verify(self.root,changed,self.expected)
        self.state.write_text('{"inventory":["changed"]}',encoding='utf-8')
        with self.assertRaises(AssertionError):verify(self.root,proof,self.expected)
    def test_extra_archive_member_is_refused_even_after_reference_hash_refresh(self):
        proof=self.proof();archive=self.root/proof['qa_source_archive']['file']
        with zipfile.ZipFile(archive,'a')as z:z.writestr('unaccounted.java','extra')
        proof['qa_source_archive']['sha256']=sha(archive)
        with self.assertRaises(AssertionError):verify(self.root,proof,self.expected)
    def test_discovery_entry_only_remains_unassigned(self):
        new=self.profile/'mod-interaction-checks.json';new.write_text(json.dumps(dict(checks=[dict(passed=True)],cases=[dict(id='entry',status='entry-and-policy-only')])),encoding='utf-8')
        self.summary['acceptance']=dict(file=new.relative_to(self.root).as_posix(),sha256=sha(new));self.save()
        proof=self.proof();self.assertEqual('publisher-discovery-and-recorded-interactions',proof['scope'])
        self.assertEqual('not-assigned-requires-native-case-review',proof['coverage_disposition'])
    def test_reference_path_escape_is_refused(self):
        self.summary['acceptance']['file']='../outside.json';self.save()
        with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
    def test_recorded_native_runtime_copies_and_parent_snapshot_are_bound(self):
        parent=self.folder/'original-attempt.json';parent.write_text('{"status":"passed"}',encoding='utf-8')
        self.summary['supporting_evidence']=[dict(file=parent.relative_to(self.root).as_posix(),sha256=sha(parent))]
        self.summary['results'][0]['runtime_artifact_copies']=[dict(file=str(self.runtime/v['filename']),sha256=v['sha256'],family=v['family'])for v in self.artifacts]
        self.save();proof=self.proof();self.assertTrue(verify(self.root,proof,self.expected))
        parent.write_text('{"status":"failed"}',encoding='utf-8')
        with self.assertRaises(AssertionError):verify(self.root,proof,self.expected)
    def test_reported_assertion_count_cannot_exceed_actual_journal(self):
        self.summary['results'][0]['results']['assertions']=999;self.save()
        with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
    def test_declared_native_states_and_original_action_journal_are_bound(self):
        native=self.profile/'native-actions';native.mkdir();state=native/'server.json';state.write_text('{"native_energy":17}',encoding='utf-8')
        original=native/'actions.json';original.write_text('[{"check":"fixture-only","status":"pass"}]',encoding='utf-8')
        journal=json.loads(self.journal.read_text());journal.update(state_inputs=[dict(file=state.relative_to(self.root).as_posix(),sha256=sha(state))],
            original_journal=dict(file=original.relative_to(self.root).as_posix(),sha256=sha(original)))
        self.journal.write_text(json.dumps(journal),encoding='utf-8');self.summary['acceptance']['sha256']=sha(self.journal);self.save()
        proof=self.proof();self.assertTrue(verify(self.root,proof,self.expected))
        self.assertIn(journal['state_inputs'][0],proof['full_states']);self.assertIn(journal['original_journal'],proof['supporting_evidence'])
        original.write_text('changed native action journal',encoding='utf-8')
        with self.assertRaises(AssertionError):verify(self.root,proof,self.expected)
    def test_declared_native_state_cannot_escape_or_use_a_false_hash(self):
        for ref in [dict(file='../escape.json',sha256='0'*64),dict(file=self.state.relative_to(self.root).as_posix(),sha256='0'*64)]:
            journal=json.loads(self.journal.read_text());journal['state_inputs']=[ref]
            self.journal.write_text(json.dumps(journal),encoding='utf-8');self.summary['acceptance']['sha256']=sha(self.journal);self.save()
            with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
    def native_groups(self):
        feature=self.profile/'prominent-release';feature.mkdir();groups=[]
        for name in ['upgrade','armor','visuals','shop']:
            folder=self.profile/'focused-evidence'/name;folder.mkdir(parents=True)
            action=folder/'results.json';action.write_text(json.dumps([dict(name=name+'-'+str(i),passed=True)for i in range(5)]),encoding='utf-8')
            (folder/'actual-native-state.json').write_text(json.dumps(dict(group=name,fixture_only=True)),encoding='utf-8')
            groups.append(dict(group=name,checks=5,journal=action.relative_to(self.root).as_posix(),sha256=sha(action)))
        self.journal=feature/'summary.json';self.journal.write_text(json.dumps(dict(complete=True,groups=groups,scope='Synthetic grouped validator fixture only')),encoding='utf-8')
        self.summary['acceptance']=dict(file=self.journal.relative_to(self.root).as_posix(),sha256=sha(self.journal));self.summary['results'][0]['results'].update(cases=3,checks=20);self.save()
        return groups
    def test_native_group_archives_bind_all_original_checks_and_outside_summary_states(self):
        groups=self.native_groups();proof=self.proof();self.assertEqual(20,proof['assertions']);self.assertTrue(verify(self.root,proof,self.expected))
        for group in groups:self.assertIn(group['journal'],[v['file']for v in proof['full_states']])
        (self.profile/'focused-evidence/shop/actual-native-state.json').write_text('changed native state',encoding='utf-8')
        with self.assertRaises(AssertionError):verify(self.root,proof,self.expected)
    def test_native_group_missing_wrong_or_failed_journals_cannot_be_certified(self):
        self.native_groups();original=json.loads(self.journal.read_text())
        for change in ['missing-group','count','boolean-count','wrong-profile','bad-hash','unobserved']:
            journal=copy.deepcopy(original)
            if change=='missing-group':journal['groups'].pop()
            if change=='count':journal['groups'][0]['checks']=99
            if change=='boolean-count':journal['groups'][0]['checks']=True
            if change=='wrong-profile':journal['groups'][0]['journal']=journal['groups'][1]['journal'];journal['groups'][0]['sha256']=journal['groups'][1]['sha256']
            if change=='bad-hash':journal['groups'][0]['sha256']='0'*64
            if change=='unobserved':(self.profile/'focused-evidence/shop/actual-native-state.json').unlink()
            self.journal.write_text(json.dumps(journal),encoding='utf-8');self.summary['acceptance']['sha256']=sha(self.journal);self.save()
            with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
        (self.profile/'focused-evidence/shop/actual-native-state.json').write_text('{"fixture_only":true}',encoding='utf-8')
        action=self.root/original['groups'][0]['journal'];raw=json.loads(action.read_text());raw[0]['passed']=1;action.write_text(json.dumps(raw),encoding='utf-8');original['groups'][0]['sha256']=sha(action)
        self.journal.write_text(json.dumps(original),encoding='utf-8');self.summary['acceptance']['sha256']=sha(self.journal);self.save()
        with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
    def test_group_proof_cannot_drop_a_native_journal_or_its_observed_states(self):
        groups=self.native_groups();proof=self.proof();proof['full_states']=[v for v in proof['full_states']if v['file']!=groups[0]['journal']]
        with self.assertRaises(AssertionError):verify(self.root,proof,self.expected)
    def legacy_toms(self):
        destination=self.root/'build/remaining-ui-audit/gameplay'/('26.1.2-fabric-siu-'+self.run)
        destination.parent.mkdir(parents=True);self.profile.rename(destination);self.profile=destination
        self.staged=self.profile/'staged-artifacts.json';self.feature=self.profile/'toms-release';self.feature.mkdir()
        self.journal=self.feature/'checks.json';self.state=self.feature/'actual-state.json';self.state.write_text('{"inventory":[],"cursor":"{}"}')
        qa={}
        for name in ['functional-tests/release-specialized/toms_replay.py',
                     'functional-tests/release-specialized/run_toms_terminal.py',
                     'functional-tests/project-everything-regression/run_toms_release.py',
                     'functional-tests/project-everything-regression/toms_release_workflows.py']:
            path=self.root/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text('# synthetic archive fixture only\n');qa[str(Path(name))]=sha(path)
        driver=str(Path('functional-tests/project-everything-regression/toms_release_workflows.py'))
        self.journal.write_text(json.dumps(dict(scope='Synthetic old Tom format only',driver_sha256=qa[driver],
            checks=[dict(check='original-'+str(i),passed=True)for i in range(17)])))
        self.summary=dict(status='passed',target='26.1.2/fabric',workflow='terminal',
            fixture=str(Path('build/blocked-setup-recovery/fixtures/polymer-without-bumblezone.json')),
            profile=str(self.profile.relative_to(self.root)),started_at='2026-11-07T00:00:00+00:00',finished_at='2026-11-07T00:01:00+00:00',
            subjects=self.artifacts,qa_inputs={driver:qa[driver]},pid=123,checks_exit_code=0,exit_code=0)
        self.replay_qa={k:v for k,v in qa.items()if k!=driver};self.save_legacy_toms()
    def save_legacy_toms(self):
        self.save();summary=self.folder/'summary.json';self.receipt=self.folder/'replay-driver.json'
        self.receipt.write_text(json.dumps(dict(status='passed',qa_inputs=self.replay_qa,
            summary=dict(file=str(summary.relative_to(self.root)),sha256=sha(summary)),scope='Synthetic wrapper receipt only')))
    def test_frozen_toms_layout_seals_original_unmodified_journal_and_wrapper_inputs(self):
        self.legacy_toms();raw=self.journal.read_bytes();proof=self.proof()
        self.assertEqual('frozen-toms-native-workflow',proof['archival_layout']);self.assertEqual(17,proof['assertions'])
        self.assertEqual(raw,self.journal.read_bytes());self.assertIn(str(Path('functional-tests/release-specialized/toms_replay.py')).replace('\\','/'),proof['archived_inputs'])
        self.assertTrue(verify(self.root,proof,self.expected));self.assertEqual('not-assigned-requires-native-case-review',proof['coverage_disposition'])
    def test_frozen_toms_layout_rejects_unclean_unknown_or_wrong_profile_reports(self):
        self.legacy_toms();original=copy.deepcopy(self.summary)
        for key,value in [('exit_code',False),('checks_exit_code',1),('target','1.20.1/fabric'),('workflow','other'),('pid',False),('finished_at','2025-11-07T00:00:00+00:00'),('profile','build/profile')]:
            self.summary=copy.deepcopy(original);self.summary[key]=value;self.save_legacy_toms()
            with self.assertRaises((AssertionError,KeyError)):collect(self.root,self.run,self.expected)
        self.summary=copy.deepcopy(original);self.summary['invented']=True;self.save_legacy_toms()
        with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
    def test_frozen_toms_layout_binds_exact_count_driver_receipt_and_staging(self):
        self.legacy_toms();original=json.loads(self.journal.read_text())
        for change in ['count','boolean','driver','receipt','staging']:
            journal=copy.deepcopy(original);self.save_legacy_toms()
            if change=='count':journal['checks'].pop()
            if change=='boolean':journal['checks'][0]['passed']=1
            if change=='driver':journal['driver_sha256']='0'*64
            if change=='receipt':
                receipt=json.loads(self.receipt.read_text());receipt['summary']['sha256']='0'*64;self.receipt.write_text(json.dumps(receipt))
            if change=='staging':self.staged.write_text(json.dumps(dict(artifacts=self.artifacts[:-1])))
            self.journal.write_text(json.dumps(journal))
            with self.assertRaises(AssertionError):collect(self.root,self.run,self.expected)
        self.staged.write_text(json.dumps(dict(artifacts=self.artifacts)));self.journal.write_text(json.dumps(original));self.save_legacy_toms()
        proof=self.proof();proof['archival_layout']=None
        with self.assertRaises(AssertionError):verify(self.root,proof,self.expected)


if __name__=='__main__':unittest.main()
