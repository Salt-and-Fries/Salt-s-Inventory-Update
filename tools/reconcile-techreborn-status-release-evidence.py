"""Review sealed native Tech Reborn status workflows, without running gameplay.

The generic Reborn menu class alone cannot identify a machine. Require each
original screen, native block entity, named assertions and immediate states.
Missing live evidence or an untested publisher pin leaves coverage unresolved.
"""
from pathlib import Path
from collections import Counter
import argparse,datetime,hashlib,json,re,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from focused_release_evidence import checked,reference,verify as verify_sealed
from native_publisher_index import index,matches
from sensor_feature_evidence import inventory
from focused_release_evidence_ae2_variants import combine as bound_combine,expected_subjects

TARGETS=['1.20.1/fabric','1.21.1/fabric','26.1.2/fabric','26.2/fabric','26.3/fabric']
MENU='reborncore.common.screen.BuiltScreenHandler'
DRIVER='functional-tests/release-specialized/techreborn_status_features.py'
GROUPS={
 'elevator':('registry-techreborn-elevator','GuiElevator','machine.tier1.ElevatorBlockEntity',
             ['initial','moved','native-solar-charge','charge-stopped','reopened']),
 'launchpad':('registry-techreborn-launchpad','GuiLaunchpad','machine.tier2.LaunchpadBlockEntity',
              ['initial','moved','arrow-3','arrow-1','arrow-2','arrow-0','arrow-3','reopened']),
 'player_detector':('registry-techreborn-player_detector','GuiPlayerDetector','machine.tier1.PlayerDetectorBlockEntity',
                    ['initial','moved','arrow-3','arrow-1','arrow-2','arrow-0','arrow-3','reopened']),
 'basic_solar_panel':('machine-techreborn-basic_solar_panel','GuiSolar','generator.SolarPanelBlockEntity',
                      ['initial','moved','sunlight','blocked','stopped','sunlight-restored','reopened']),
}
COMMON=['original-native-zero-item-panel-opens','native-panel-moves-before-input',
        'native-controls-preserve-full-items-and-empty-cursors','source-removal-closes-the-original-native-session']
EXTRA={
 'elevator':['elevator-status-observes-native-adjacent-energy-generation','elevator-energy-bar-synchronizes-after-production-stops','native-elevator-energy-persists-on-ordinary-reopening'],
 'basic_solar_panel':['native-solar-panel-really-generates-energy','native-roof-obstruction-stops-generation-in-both-views','blocked-panel-energy-stays-stable-and-synchronizes','removing-native-obstruction-restores-real-generation','solar-status-persists-on-ordinary-reopening'],
}
for key in ['launchpad','player_detector']:
    EXTRA[key]=['all-four-native-selector-arrows-remain-present',
        *['native-arrow-'+str(n)+'-changes-exact-selection'for n in [3,1,2,0,3]],'native-selector-persists-on-ordinary-reopening']
REQUIRED=Counter((key,name)for key in GROUPS for name in COMMON+EXTRA[key])
SCHEMA='techreborn-native-status-observations-v1'
SCOPE='Four native zero-item machine panels: exact radius/launch selector arrows and clamping, moved input and persistence; changing solar generation/obstruction and elevator energy synchronization; full typed inventory/cursor preservation and original source-cleanup assertions.'
LIMITS='Named native publisher and target only. No player teleportation, launching, detector redstone automation, general machinery or complete Tech Reborn gameplay claim. Only the last source-removal snapshot persists; earlier cleanup is evidenced by its original passing assertion, not a retained per-machine snapshot.'

def integer(value,minimum=0):
    assert type(value)is int and value>=minimum
    return value

def specification(target,case,classes):
    assert target in TARGETS
    hits=[key for key,value in GROUPS.items()if value[0]==case];assert len(hits)==1
    assert classes==[MENU],'Unreviewed native status classification'
    return hits[0]

def validate(target,journal,states,removed,driver_hash):
    assert target in TARGETS
    assert journal['complete']is True and journal['cases']==list(GROUPS)
    assert journal['driver_sha256']==driver_hash
    assert Counter((v['case'],v['name'])for v in journal['checks'])==REQUIRED
    assert all(v.get('passed')is True for v in journal['checks'])
    assert set(states)==set(GROUPS)
    baseline,parsed=inventory(states['elevator'][0]);seed=parsed[9]
    assert seed.get('id')==nbtlib.String('minecraft:diamond')
    countkey='Count'if 'Count'in seed else'count'
    assert isinstance(seed[countkey],nbtlib.Byte if countkey=='Count'else nbtlib.Int)and int(seed[countkey])==3
    data=seed.get('tag')if'tag'in seed else seed.get('components',{}).get('minecraft:custom_data')
    assert isinstance(data,nbtlib.Compound)and isinstance(data.get('release_tr_status_marker'),nbtlib.Int)and int(data['release_tr_status_marker'])==29
    assert isinstance(data.get('typed'),nbtlib.ByteArray)and list(data['typed'])==[3,8]
    for key,(_,screen,entity,labels)in GROUPS.items():
        frames=states[key];assert isinstance(frames,list)and len(frames)==len(labels)
        for frame in frames:
            client=frame['client'];native=frame['native'];local=client['native']
            assert type(frame['physical_slots'])is int and frame['physical_slots']==0
            assert client['menu']==MENU and client['screen']=='techreborn.client.gui.'+screen
            assert native['type']==local['type']=='techreborn.blockentity.'+entity
            integer(client['session'],1)
            assert frame['position']==frames[0]['position']
            assert set(frame['position'])=={'x','y','z'}and all(type(v)is int for v in frame['position'].values())
            bounds=client['bounds'];assert set(bounds)=={'x','y','width','height'}and all(type(v)is int for v in bounds.values())
            assert bounds['width']>0 and bounds['height']>0
            for values in [native,local]:
                assert integer(values['energy'])<=integer(values['capacity'],1)
            assert inventory(frame)[0]==baseline,'Typed physical inventory changed during native status input'
            assert isinstance(frame['cursors'],list)and frame['cursors']
            assert all(isinstance(v.get('menu'),str)and isinstance(nbtlib.parse_nbt(v['item']),nbtlib.Compound)and not nbtlib.parse_nbt(v['item'])for v in frame['cursors'])
        assert all(v['client']['session']==frames[0]['client']['session']for v in frames[:-1])
        assert frames[0]['client']['bounds']!=frames[1]['client']['bounds']
        if key in ['launchpad','player_detector']:
            for frame in frames:
                native=frame['native'];local=frame['client']['native']
                minimum=integer(native['minimum']);maximum=integer(native['maximum'],minimum)
                assert minimum<=integer(native['selection'])<=maximum
                assert all(local[name]==native[name]and type(local[name])is int for name in ['minimum','maximum','selection'])
            buttons=frames[1]['client']['buttons'];assert len(buttons)==4
            assert all(b['visible']is True and b['active']is True for b in buttons)
            xs=[b['source_x']for b in buttons];assert xs==sorted(xs)and len(set(xs))==4
            previous=frames[1]['native'];bulk=previous['maximum']if key=='launchpad'else 16
            for frame,delta in zip(frames[2:-1],[-bulk,1,-1,bulk,-bulk]):
                assert frame['native']['selection']==max(previous['minimum'],min(previous['maximum'],previous['selection']+delta))
                previous=frame['native']
            assert frames[-1]['native']['selection']==previous['selection']
        elif key=='elevator':
            initial,moved,charged,stopped,reopened=frames
            assert charged['native']['energy']>initial['native']['energy']and 0<charged['client']['native']['energy']<=charged['native']['energy']
            energy=integer(stopped['native']['energy'],1)
            assert energy==stopped['client']['native']['energy']==reopened['native']['energy']==reopened['client']['native']['energy']
        else:
            initial,moved,sunlight,blocked,stopped,restored,reopened=frames
            for frame in frames:
                for values in [frame['native'],frame['client']['native']]:
                    assert type(values['generating'])is bool and type(values['rate'])is int and values['rate']>=0
            for frame in [sunlight,restored,reopened]:
                assert frame['native']['generating']is True and frame['client']['native']['generating']is True
            assert sunlight['native']['rate']>0 and sunlight['native']['energy']>initial['native']['energy']
            for frame in [blocked,stopped]:
                assert frame['native']['generating']is False and frame['client']['native']['generating']is False and frame['native']['rate']==0
            assert stopped['native']['energy']==blocked['native']['energy']==stopped['client']['native']['energy']
            assert restored['native']['energy']>stopped['native']['energy']
    assert isinstance(removed.get('menus'),list)
    assert not any(v.get('session')==states['basic_solar_panel'][-1]['client']['session']for v in removed['menus'])
    return dict(assertions=sum(REQUIRED.values()),states=sum(len(v)for v in states.values()),native_panels=4)

def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    key=specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('techreborn_status_features_checked')is True
    assert type(result['cases'])is int and result['cases']==4 and type(result['failed'])is int and result['failed']==0
    assert type(result['checks'])is int and result['checks']==sum(REQUIRED.values())
    profile=(root/row['profile']).resolve();journal_path=checked(root,sealed['acceptance'])
    assert journal_path==profile/'techreborn-status-release/summary.json'
    states={};refs=[];ordinal=0
    for machine,(_,_,_,labels)in GROUPS.items():
        states[machine]=[]
        for label in labels:
            ordinal+=1;path=journal_path.parent/(str(ordinal).zfill(3)+'-'+machine+'-'+label+'.json');ref=reference(root,path)
            assert ref in sealed['full_states'];refs.append(ref);states[machine].append(json.loads(path.read_text()))
    removal=profile/'regression-state.json';journal=json.loads(journal_path.read_text())
    validation=validate(target,journal,states,json.loads(removal.read_text()),sealed['archived_inputs'][DRIVER])
    wanted=[MENU,*['techreborn.client.gui.'+v[1]for v in GROUPS.values()],*['techreborn.blockentity.'+v[2]for v in GROUPS.values()]]
    staged=json.loads(checked(root,sealed['staged']).read_text())['artifacts'];publishers=[];found=set()
    for artifact in staged:
        if artifact['family']!='upstream' or not artifact['filename'].lower().startswith(('techreborn-','reborncore-')):continue
        path=Path(artifact['file']);path=path if path.is_absolute()else root/path;raw=path.read_bytes()
        assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert digest==artifact['sha512']
        locations=matches(index(path,digest,'fabric'),wanted)
        found.update(name[:-6].replace('/','.')for loc in locations for name in loc['classes'])
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert found==set(wanted)and any(v['filename'].lower().startswith('techreborn-')for v in publishers)
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',original_native_classes=original_classes,
        native_classes=[MENU,'techreborn.client.gui.'+GROUPS[key][1]],subjects={k:list(v)for k,v in expected.items()},
        sealed_evidence=reference(root,sealed_path),acceptance=reference(root,journal_path),native_states=refs,
        final_source_removal=reference(root,removal),validation=validation,publishers=publishers,scope=SCOPE,limits=LIMITS)

def verify(root,record,expected,consumer=None,current=None):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    workflows=[record,*record.get('additional_workflows',[])]
    actual=[]
    for workflow in workflows:
        checked(root,workflow['final_source_removal'])
        actual.append(derive(root,checked(root,workflow['sealed_evidence']),expected_subjects(record,workflow,expected,consumer,current),record['case'],record['original_native_classes']))
    assert combine(actual,consumer=consumer,current=current)==record
    return True

def combine(workflows,consumer=None,current=None):
    return bound_combine(workflows,consumer=consumer,current=current)

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--generation',required=True);parser.add_argument('--plan-only',action='store_true');args=parser.parse_args()
    assert re.fullmatch(r'build[0-9]+',args.generation)
    out=ROOT/'output/release-readiness';backlog=json.loads((out/'coverage-backlog.json').read_text())
    plan=json.loads((ROOT/'build/release-readiness/focused-native-plan-02.json').read_text());jobs=[v for v in plan['jobs']if v['family']=='techreborn_status']
    assert len(jobs)==5 and {v['target']for v in jobs}==set(TARGETS)
    helper=ROOT/'build/release-readiness/release_queue_history_455.py';assert hashlib.sha256(helper.read_bytes()).hexdigest()=='fb00a5e47131d9ca30263154439a17f13ea3661eef2651febe9a9fa75fa3affc'
    import importlib.util
    spec=importlib.util.spec_from_file_location('original_native_status_history',helper);history=importlib.util.module_from_spec(spec);spec.loader.exec_module(history)
    resolved=history.completed(plan,json.loads((out/'focused-native-campaign-02.json').read_text()));assert len(resolved)==174
    jobs={v['target']:[v]for v in resolved if v['family']=='techreborn_status'};assert set(jobs)==set(TARGETS)
    extra=json.loads((ROOT/'build/release-readiness/post-remote-publisher-variants-plan-01.json').read_text())
    assert extra['sealed']is True and len(extra['jobs'])==1
    variant=extra['jobs'][0]
    assert variant['family']=='techreborn_status_latest'and variant['target']=='1.20.1/fabric'
    assert variant['run']=='readiness-techreborn-status-5815-1201-fabric-01'
    jobs[variant['target']].append(variant)
    rows=[r for r in backlog['records']if r['case']in{v[0]for v in GROUPS.values()}];assert len(rows)==20
    assert {(r['target'],r['case'])for r in rows}=={(t,v[0])for t in TARGETS for v in GROUPS.values()}
    if args.plan_only:
        for row in rows:print(row['target'],row['case'],'requires',','.join(v['run']for v in jobs[row['target']]))
        print('Plan only: no live evidence or accepted coverage. Every reviewed upstream pin still needs qualification.');return
    sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
    from subject_artifacts import current_subjects
    from release_candidate_identity import current
    from focused_release_evidence_core_log_consumer import verified
    candidate=current(ROOT,verify_worktree=True);consumer=verified(ROOT,args.generation,candidate);records=[]
    for row in rows:
        workflows=[]
        for job in jobs[row['target']]:
            sealed=out/('focused-evidence-'+job['run']+'.json')
            accepted=json.loads(sealed.read_text());expected=consumer.expected(row['target'],accepted,current_subjects(row['target']))
            workflows.append(derive(ROOT,sealed,expected,row['case'],row.get('historical_native_classes',row['native_classes'])))
        record=combine(workflows,consumer=consumer,current=current_subjects(row['target']))
        assert row['current_exact_pins']and {v['sha512']for v in row['current_exact_pins']}<={v['sha512']for v in record['publishers']},'Unqualified Tech Reborn/RebornCore publisher patch; preserve as unresolved'
        records.append(record)
    destination=out/f'coverage-techreborn-status-{args.generation}-01.json';assert not destination.exists()
    destination.write_text(json.dumps(dict(status='reviewed-native-techreborn-status-workflows-reconciled',created=datetime.datetime.now(datetime.timezone.utc).isoformat(),candidate_identity=candidate,artifact_equivalence=consumer.report(),auditor_inputs=[reference(ROOT,Path(__file__))],records=records,limits=LIMITS),indent=2)+'\n')
    print('Reconciled20named UI records from six actual four-panel workflows; repeated upstream pins are not additional distinct UI.')

if __name__=='__main__':main()
