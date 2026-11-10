"""Retain prior exact native scopes and rederive both actual AE2 repair qualifiers."""
from pathlib import Path
import importlib.util,json,types
from focused_release_evidence import checked,reference,verify
from focused_release_evidence_repair_chain_gallery_qualifiers import qualify as prior_qualify
SETTINGS_READER={'file': 'build/release-readiness/AE2-resolved-reader-proposal-1046-01/reconcile-ae2-settings-release-evidence.py', 'sha256': 'aa22ef8b90c5b5fd5cfd4973b8329fcd3f36a6b1cd516fc2216fd8487f8a8c84'}
QUEUE_READER={'file': 'build/release-readiness/qualify_native_queue_stress_1067.py', 'sha256': 'eb50befb8e670aadf3315877b386de207216f36e6e139863c015b5a1116c160b'}


def qualify(root,graph,consumer):
    root=Path(root).resolve();previous=json.loads(checked(root,graph['previous_preparation']).read_text());prior=prior_qualify(root,previous,consumer)
    edge=graph['edges'][-2];campaign=checked(root,edge['native_qualification']);data=json.loads(campaign.read_text())
    assert data['status']=='all-four-affected-modern-native-settings-workflows-qualified'and(data['checks'],data['full_states'])==(80,72)and data['corrupted_full_state_sets_rejected']==40
    source=checked(root,SETTINGS_READER);reader=types.ModuleType('exact_native_AE2_settings');reader.__file__=str(root/'tools/reconcile-ae2-settings-release-evidence.py');exec(compile(source.read_text(),str(source),'exec'),reader.__dict__)
    targets={'1.21.1/neoforge','26.1.2/neoforge','26.2/neoforge','26.3/neoforge'};assert {v['target']for v in data['runs']}==targets;rows=[]
    for run in data['runs']:
        qualification=checked(root,run['strict_qualification']);actual=json.loads(qualification.read_text());expected=consumer._stage_subjects('1019',run['target'],{'core','compatibility'})
        seal=checked(root,run['seal']);records=[]
        for case,classes in [('ae2-priority',['appeng.client.gui.implementations.PriorityScreen']),('atlas-ae2-spatial-anchor',['appeng.menu.implementations.SpatialAnchorMenu']),('atlas-ae2-energy-level-emitter',['appeng.menu.implementations.EnergyLevelEmitterMenu'])]:
            v=reader.derive(root,seal,expected,case,classes);assert reader.verify(root,v,expected);records.append(v)
        assert records==actual['qualification']and len(actual['corrupted_full_state_sets_rejected'])==10
        rows.append(dict(target=run['target'],run=run['run'],seal=run['seal'],strict_qualification=run['strict_qualification'],checks=20,states=18))
    prior['receipts'].append(dict(kind='AE2-native-payload-acceptance-and-ordering',campaign=reference(root,campaign),runs=rows,reader=SETTINGS_READER,scope='Four actual native20check settings workflows on1019. Same-frame41/37and41/42input, native server/host persistence, actual server-backed overlay, moved input/reopen and typed foreign ownership. Does not claim the later discovered manager-close path; the next exact edge qualifies that correction.'))
    edge=graph['edges'][-1];campaign=checked(root,edge['native_qualification']);data=json.loads(campaign.read_text());assert data['status']=='four-actual-native-close-and-invalidation-workflows-qualified'and(data['checks'],data['full_states'],data['rejected_corrupted_full_state_sets'])==(48,44,32)and{v['target']for v in data['runs']}==targets
    path=checked(root,QUEUE_READER);sp=importlib.util.spec_from_file_location('exact_native_pending_close',path);reader=importlib.util.module_from_spec(sp);sp.loader.exec_module(reader);rows=[]
    for run in data['runs']:
        seal_path=checked(root,run['seal']);seal=json.loads(seal_path.read_text());expected=consumer._stage_subjects('1063',run['target'],{'core','compatibility'});verify(root,seal,expected)
        acceptance=checked(root,seal['acceptance']);folder=acceptance.parent;journal=json.loads(acceptance.read_text());assert journal['driver_sha256']==seal['archived_inputs']['build/release-readiness/ae2_queue_stress_features_1064.py']
        frames=[(p.name,json.loads(p.read_text()))for p in sorted(folder.glob('[0-9][0-9][0-9]-*.json'))];probes={p.stem:json.loads(p.read_text())for p in(folder/'transport-stress').glob('*.json')}
        assert len(seal['full_states'])==11
        for p in folder.rglob('*.json'):
            if p!=acceptance:assert reference(root,p)in seal['full_states']
        validation=reader.validate(journal,frames,probes);q=json.loads(checked(root,run['strict_qualification']).read_text());assert validation==q['validation']and len(q['rejected_corrupted_state_sets'])==8
        rows.append(dict(target=run['target'],run=run['run'],seal=run['seal'],strict_qualification=run['strict_qualification'],checks=12,states=11))
    prior['receipts'].append(dict(kind='AE2-native-pending-user-close-and-lifetime-invalidation',campaign=reference(root,campaign),runs=rows,reader=QUEUE_READER,native_failure=data['native_failure'],scope='Four actual12check native same-turn123/456bursts, publicUSER_CLOSE refusal whilepending, nativeEscape ACK deferral, actual source removal/recreation, empty old lifetime/no stale replay, exact typed ownership. Only four AEWindow.options bodies changed; no Core/public API change.'))
    return prior
