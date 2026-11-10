"""Bind original whole-stage artifact identities through an explicitly verified repair chain. This class does not issue certificates or grant feature coverage."""
from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from focused_release_evidence import checked,reference,sha
from focused_release_evidence_core_log_consumer import subjects

ALLOWED_STAGES=('239','250','261','279','314','381','431','475','613','726','1019','1063','1159','1253','1266','1280')

class RepairConsumer:
    def __init__(self,root,base,graph,prefix_certificate,verification):
        self.root=Path(root).resolve();self.base=base;self.prefix_certificate=prefix_certificate
        self.verification=verification;self.certificate=prefix_certificate
        self.retention_kind='retained-explicit-reviewed-repair-chain'
        self.resolved=[];self.prior_proofs=[];self.stages={};self.locators={}
        self.distributions={};self.archived_stages={}
        for stage in graph['stages']:
            stage_id=stage['stage'];rows=stage['artifacts'];tuples={};artifacts={}
            assert len(rows)==36
            for row in rows:
                key=(row['target'],row['family']);assert key not in tuples
                distribution=Path(row['distribution']).as_posix()
                tuples[key]=(Path(distribution).name,row['sha256'])
                artifacts[distribution]=row['sha256']
                locator_key=(distribution,row['sha256'])
                locator=row['preserved']
                if locator_key in self.locators:assert self.locators[locator_key]['sha256']==locator['sha256']
                else:self.locators[locator_key]=locator
            assert len(tuples)==len(artifacts)==36
            self.archived_stages[stage_id]=tuples
            if stage_id in ALLOWED_STAGES:
                self.stages[stage_id]=tuples;self.distributions[stage_id]=artifacts
        assert set(self.stages)==set(ALLOWED_STAGES)

    def _stage_subjects(self,stage,target,families):
        return {f:self.stages[stage][target,f]for f in families}

    def _current(self,target,current):
        current=subjects(current,current)
        endpoint=self._stage_subjects('1280',target,current)
        assert self.base.bind(target,endpoint,current),'Current tuple is outside the exact1280/final transition'
        return current

    def prior_subjects(self,target,current):
        current=self._current(target,current)
        # The separate build13 Traveler-absent certificate ends at239, not381.
        return self._stage_subjects('239',target,current)

    def bind(self,target,recorded,current):
        current=self._current(target,current)
        try:original=subjects(recorded,current)
        except (AssertionError,KeyError,TypeError):return None
        direct=self.base.bind(target,original,current)
        if direct:return direct
        matches=[stage for stage in ALLOWED_STAGES if original==self._stage_subjects(stage,target,current)]
        if not matches:return None
        return dict(execution_kind=self.retention_kind,original_subjects=original,final_subjects=current,
            artifact_equivalence=self.prefix_certificate,repair_stage=matches[0],
            repair_chain_endpoint_subjects=self._stage_subjects('1280',target,current),
            final_transition=self.base.report())

    def artifact_path(self,ref):
        path=(self.root/ref['file']).resolve();assert path.is_relative_to(self.root)
        if path.is_file()and sha(path)==ref['sha256']:return path
        key=(Path(ref['file']).as_posix(),ref['sha256'])
        if key not in self.locators:return self.base.artifact_path(ref)
        original=self.locators[key];result=checked(self.root,original)
        row=dict(original_reference=ref,preserved_byte_locator=original,repair_chain=self.prefix_certificate)
        if row not in self.resolved:self.resolved.append(row)
        return result

    def fingerprints(self,recorded,current):
        assert set(recorded)==set(current),'Changed complete-suite artifact scope'
        if recorded==current:return self.base.fingerprints(recorded,current)
        # Match the ENTIRE original scope to one stage; never mix per-jar edges.
        matches=[s for s,d in self.distributions.items()if all(d.get(n)==v for n,v in recorded.items())]
        assert matches,'Original complete scope is outside every approved repair stage'
        endpoint={n:self.distributions['1280'][n]for n in current}
        final=self.base.fingerprints(endpoint,current)
        for name,digest in recorded.items():self.artifact_path(dict(file=name,sha256=digest))
        return dict(execution_kind=self.retention_kind,original_artifacts=dict(recorded),final_artifacts=dict(current),
            artifact_equivalence=self.prefix_certificate,repair_stage=matches[0],final_transition=final)

    def expected(self,target,record,current):
        binding=self.bind(target,record['subjects'],current);assert binding,'Unqualified recorded feature subjects'
        return binding['original_subjects']

    def coverage_path(self,out,prefix,generation,suffix='01'):
        out=Path(out);current=out/f'{prefix}-{generation}-{suffix}.json'
        if current.exists()or generation!='build15':return current
        previous=out/f'{prefix}-build14-{suffix}.json'
        if previous.exists():
            row=dict(requested_generation=generation,original_generation='build14',original_proof=reference(self.root,previous),
                repair_chain=self.prefix_certificate,final_transition=self.base.report())
            if row not in self.prior_proofs:self.prior_proofs.append(row)
            return previous
        return current

    def chained_retention(self,retained,current):
        assert retained['execution_kind']=='retained-original-scope'
        assert retained['equivalence']=='native-Traveler-absent-build13-to-build14'
        checked(self.root,retained['retained_evidence'])
        current=self._current(retained['target'],current)
        return dict(execution_kind='retained-original-scope-and-explicit-reviewed-repair-chain',
            original_subjects=retained['original_subjects'],
            intermediate_subjects=self._stage_subjects('239',retained['target'],current),
            repair_chain_endpoint_subjects=self._stage_subjects('1280',retained['target'],current),
            final_subjects=current,original_scope_equivalence=retained['equivalence'],
            original_retained_evidence=retained['retained_evidence'],artifact_equivalence=self.prefix_certificate,
            final_transition=self.base.report())

    def report(self):
        return dict(certificate=self.prefix_certificate,consumer_verification=self.verification,
            original_artifact_locators=self.resolved,original_generation_proofs=self.prior_proofs,
            final_transition=self.base.report(),
            scope='Original executions and feature counts remain unchanged. Whole-stage identities only; no fresh run or new native coverage is created.')
