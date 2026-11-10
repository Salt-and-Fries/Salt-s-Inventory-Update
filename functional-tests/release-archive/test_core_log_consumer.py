"""Synthetic metadata corruption tests; no real equivalence is granted here."""
from pathlib import Path
import copy,json,sys,tempfile,unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
import focused_release_evidence_core_log_consumer as m
from focused_release_evidence import reference


class CoreLogConsumer(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.root=Path(self.tmp.name)
        self.targets=['game'+str(i)+'/fabric'for i in range(12)];self.rows=[]
        for target in self.targets:
            for family in ['core','compatibility','controlify']:
                name=target.replace('/','-')+'-'+family+'.jar'
                old=self.root/'preserved14'/family/name;new=self.root/'dist'/family/name
                old.parent.mkdir(parents=True,exist_ok=True);new.parent.mkdir(parents=True,exist_ok=True)
                old.write_bytes(('old:'+name).encode());new.write_bytes(('new:'+name if family=='core'else'old:'+name).encode())
                self.rows.append(dict(target=target,family=family,original=reference(self.root,old),final=reference(self.root,new)))
        self.cert=self.root/'synthetic-certificate.json';self.cert.write_text(json.dumps(dict(archive_pairs=self.rows)))
        self.consumer=m.Consumer(self.root,reference(self.root,self.cert),dict(file='synthetic-not-a-real-verification'))
        self.target=self.targets[0]

    def current(self,target=None):
        target=target or self.target
        return {v['family']:(Path(v['final']['file']).name,v['final']['sha256'])for v in self.rows if v['target']==target and v['family']in {'core','compatibility'}}

    def old(self,target=None):
        target=target or self.target
        return {v['family']:(Path(v['original']['file']).name,v['original']['sha256'])for v in self.rows if v['target']==target and v['family']in {'core','compatibility'}}

    def test_build14_is_strict_passthrough_without_loading_a_certificate(self):
        with patch.object(m,'derive',side_effect=AssertionError('must not run')):
            consumer=m.verified(self.root,'build14',{})
            self.assertIsNone(consumer.report())
            self.assertEqual(consumer.bind(self.target,self.current(),self.current())['execution_kind'],'fresh-current-artifact')
            self.assertIsNone(consumer.bind(self.target,self.old(),self.current()))

    def test_final_binding_keeps_original14_subjects_and_real15_is_fresh(self):
        before=self.old();saved=copy.deepcopy(before)
        binding=self.consumer.bind(self.target,before,self.current())
        self.assertEqual(binding['execution_kind'],'retained-core-log-equivalence')
        self.assertEqual(binding['original_subjects'],before);self.assertEqual(binding['final_subjects'],self.current())
        self.assertEqual(before,saved)
        self.assertEqual(self.consumer.bind(self.target,self.current(),self.current())['execution_kind'],'fresh-current-artifact')

    def test_unknown13_or_different_native_subjects_cannot_use_logger_equivalence(self):
        before=self.old();before['compatibility']=(before['compatibility'][0],'different13hash')
        self.assertIsNone(self.consumer.bind(self.target,before,self.current()))
        before=self.old();before['core']=(before['core'][0],'unreviewed-core')
        self.assertIsNone(self.consumer.bind(self.target,before,self.current()))
        current=self.current();current['core']=(current['core'][0],'changed-after-certificate')
        with self.assertRaises(AssertionError):self.consumer.bind(self.target,self.old(),current)

    def test_duplicate_missing_or_extra_shipping_subject_rows_are_rejected(self):
        rows=[dict(family=k,filename=v[0],sha256=v[1])for k,v in self.current().items()]
        self.assertEqual(m.subjects(rows,{'core','compatibility'}),self.current())
        for value in [rows+rows[:1],rows[:1],{'core':('a','b'),'qa':('c','d')}]:
            self.assertIsNone(self.consumer.bind(self.target,value,self.current()))

    def test_old_dist_locator_is_explicit_and_unknown_hash_or_path_is_rejected(self):
        pair=next(v for v in self.rows if v['target']==self.target and v['family']=='core')
        original_ref=dict(file=pair['final']['file'],sha256=pair['original']['sha256']);saved=copy.deepcopy(original_ref)
        self.assertEqual(self.consumer.artifact_path(original_ref),self.root/pair['original']['file'])
        self.assertEqual(original_ref,saved);self.assertEqual(len(self.consumer.resolved),1)
        self.consumer.artifact_path(original_ref);self.assertEqual(len(self.consumer.resolved),1)
        for ref in [dict(original_ref,sha256='unknown'),dict(original_ref,file='unrelated/other.jar')]:
            with self.assertRaises(AssertionError):self.consumer.artifact_path(ref)

    def test_modified_preserved_original_byte_is_not_accepted(self):
        pair=next(v for v in self.rows if v['target']==self.target and v['family']=='core')
        (self.root/pair['original']['file']).write_bytes(b'tampered')
        with self.assertRaises(AssertionError):self.consumer.artifact_path(dict(file=pair['final']['file'],sha256=pair['original']['sha256']))

    def test_full_suite_all36_fingerprints_preserve_scope_without_changing_inputs(self):
        before={v['final']['file']:v['original']['sha256']for v in self.rows};after={v['final']['file']:v['final']['sha256']for v in self.rows}
        original=copy.deepcopy(before);binding=self.consumer.fingerprints(before,after)
        self.assertEqual(binding['execution_kind'],'retained-core-log-equivalence');self.assertEqual(before,original)
        for value in [dict(before,unknown='hash'),{k:v for i,(k,v)in enumerate(before.items())if i},dict(before,**{next(iter(before)):'unknown'})]:
            with self.assertRaises(AssertionError):self.consumer.fingerprints(value,after)

    def test_current_generation_proof_takes_precedence_even_if_it_failed(self):
        out=self.root/'output';out.mkdir();old=out/'coverage-example-build14-01.json';old.write_text('{"status":"passed"}')
        self.assertEqual(self.consumer.coverage_path(out,'coverage-example','build15'),old)
        current=out/'coverage-example-build15-01.json';current.write_text('{"status":"failed"}')
        self.assertEqual(self.consumer.coverage_path(out,'coverage-example','build15'),current)
        self.assertEqual(m.Consumer(self.root).coverage_path(out,'coverage-example','build15'),current)

    def test_retained13_scope_and14_to15_link_stay_separate(self):
        evidence=self.root/'original13-retained.json';evidence.write_text('{"original":"scope"}')
        row=dict(target=self.target,execution_kind='retained-original-scope',original_subjects={'core':['original13','hash13']},
                 equivalence='native-Traveler-absent-build13-to-build14',retained_evidence=reference(self.root,evidence))
        saved=copy.deepcopy(row);binding=self.consumer.chained_retention(row,self.current())
        self.assertEqual(binding['original_subjects'],row['original_subjects'])
        self.assertEqual(binding['intermediate_subjects'],self.old());self.assertEqual(binding['final_subjects'],self.current())
        self.assertEqual(row,saved)
        with self.assertRaises(AssertionError):self.consumer.chained_retention(dict(row,equivalence='unreviewed-other-change'),self.current())

    def test_missing_or_uncompleted_real_certificate_never_falls_back_to_retention(self):
        with self.assertRaises(AssertionError):m.verified(self.root,'build15',{})
        path=self.root/'output/release-readiness/core-log-evidence-transition-final-01.json';path.parent.mkdir(parents=True)
        path.write_text(json.dumps(dict(schema=m.SCHEMA,status='running')))
        with patch.object(m,'derive',side_effect=AssertionError('native gates absent')):
            with self.assertRaises(AssertionError):m.verified(self.root,'build15',{})

    def queues(self):
        out=self.root/'output/release-readiness';out.mkdir(parents=True)
        for name in m.QUEUES:
            runs=[dict(run=name+'-run',target=self.target,status='passed')]
            if name=='final-installation-campaign-01':
                runs=[dict(run=name+'-'+mode,mode=mode,target='26.3/fabric',family='installation',status='passed',exit_code=0,checks=count)
                      for mode,count in [('client-only',22),('server-only',18),('protocol-mismatch',16)]]
            (out/(name+'.json')).write_text(json.dumps(dict(status='passed',runs=runs)))
        path=out/'readiness-final-publishers-01/publishers.json';path.parent.mkdir()
        campaign=dict(status='complete-mixed-publisher-qualification-needs-case-disposition',plan=[dict(id='job'+str(i))for i in range(65)],
                      attempts=[dict(job='job'+str(i),status='passed')for i in range(65)])
        path.write_text(json.dumps(campaign));return out,path,campaign

    def test_newer_native_or_publisher_failure_cannot_be_hidden_by_prior_generation(self):
        out,path,campaign=self.queues();self.assertEqual(len(m.completed_queues(self.root)),12)
        campaign['attempts'].append(dict(job='job0',status='running'));path.write_text(json.dumps(campaign))
        with self.assertRaises(AssertionError):m.completed_queues(self.root)
        campaign['attempts'].pop();path.write_text(json.dumps(campaign))
        name=m.QUEUES[0];p=out/(name+'.json');data=json.loads(p.read_text());data['runs'].append(dict(run='later-failed',target=self.target,status='failed'))
        p.write_text(json.dumps(data))
        with self.assertRaises(AssertionError):m.completed_queues(self.root)

    def test_failed_native_attempt_requires_explicit_matching_resolution_evidence(self):
        out,_,_=self.queues();name=m.QUEUES[0];p=out/(name+'.json');data=json.loads(p.read_text())
        evidence=self.root/'resolution.json';evidence.write_text('{"scope":"synthetic-resolution"}')
        data['runs'].insert(0,dict(run='earlier-failed',target=self.target,status='failed',resolved_by=data['runs'][0]['run'],resolved_evidence=reference(self.root,evidence)))
        p.write_text(json.dumps(data));self.assertEqual(len(m.completed_queues(self.root)),12)
        data['runs'][0]['target']='different/target';p.write_text(json.dumps(data))
        with self.assertRaises(AssertionError):m.completed_queues(self.root)

    def test_all_three_final_installation_modes_and_their_real_check_counts_are_required(self):
        out,_,_=self.queues();path=out/'final-installation-campaign-01.json';baseline=json.loads(path.read_text())
        for change in [lambda d:d['runs'].pop(),
                       lambda d:d['runs'][0].update(mode='other-mode'),
                       lambda d:d['runs'][0].update(status='running'),
                       lambda d:d['runs'][0].update(target='different/loader'),
                       lambda d:d['runs'][0].update(checks=21),
                       lambda d:d['runs'][0].update(exit_code=False)]:
            data=copy.deepcopy(baseline);change(data);path.write_text(json.dumps(data))
            with self.assertRaises(AssertionError):m.completed_queues(self.root)

    def test_combined_book_certificate_reports_original_runs_as_retained_not_fresh(self):
        self.cert.write_text(json.dumps(dict(schema=m.FINAL_SCHEMA, archive_pairs=self.rows)))
        consumer=m.Consumer(self.root,reference(self.root,self.cert))
        binding=consumer.bind(self.target,self.old(),self.current())
        self.assertEqual(binding['execution_kind'],'retained-final-Core-render-equivalence')
        self.assertEqual(binding['original_subjects'],self.old())
        self.assertEqual(consumer.bind(self.target,self.current(),self.current())['execution_kind'],'fresh-current-artifact')

    def test_combined_amendment_requires_the_extra_completed_native_book_queue(self):
        out,_,_=self.queues()
        with self.assertRaises(FileNotFoundError):m.completed_queues(self.root,require_book=True)
        path=out/'final-core-enchanting-book-01.json'
        path.write_text(json.dumps(dict(status='running',runs=[])))
        with self.assertRaises(AssertionError):m.completed_queues(self.root,require_book=True)


if __name__=='__main__':unittest.main(verbosity=2)
