"""Recheck original Decree Analyzer assertions and immediate native witnesses.

Expected pool IDs in the observer come from native decree definitions, not
the widgets being tested. The operator command is the publisher's entry path.
Native close-return permits any player cell; it is a full typed multiset.
"""
from pathlib import Path
from collections import Counter
import hashlib,json
import nbtlib
from focused_release_evidence import checked,reference,verify as verify_sealed
from focused_release_evidence_guard import item,inventory,empty_cursors
from native_publisher_index import index,matches
from sensor_feature_evidence import typed

TARGETS=('1.21.1/fabric','1.21.1/neoforge')
CASE='bountiful-decree-analyzer'
MENU='io.ejekta.bountiful.content.gui.AnalyzerScreenHandler'
SCREEN='io.ejekta.bountiful.client.AnalyzerScreen'
DRIVER='functional-tests/release-specialized/analyzer_features.py'
LABELS=('initial','decree-inserted','reward-mode','moved','objective-restored','resolution',
        'decree-recovered','diamond-refused','before-close','reopened')
NAMES=('operator-native-entry-is-an-original-analyzer-inventory','real-decree-transfers-once-with-full-typed-data',
       'decree-slot-refreshes-the-native-pool-analysis-without-a-forced-refresh',
       'mode-click-selects-reward-analysis-with-exact-native-pools','analyzer-window-moves-before-its-next-native-control',
       'moved-mode-control-restores-objective-analysis','native-resolution-slider-recomputes-its-pool-widgets',
       'decree-can-be-recovered-with-full-original-data','decree-slot-refuses-foreign-input-and-keeps-exact-cursor',
       'native-close-returns-decree-exactly-once-and-new-menu-is-empty')
PRIVATE_DRIVER='build/release-readiness/analyzer_pointer_features_617.py'
PRIVATE_INPUTS={'build/release-readiness/analyzer_player_entry_features_599.py': 'd12ed2fd0ae5ee6a7fe006469ef8dccaf58c6fbd7bc418b2b3115c4d0a73169a', 'build/release-readiness/analyzer_static_observer_601.py': 'da6d822761beae4ea049e0f3c6babe63dc5820a5742b06a06e69846614a541cc', 'build/release-readiness/analyzer_component_observer_603.py': '500b2a8bdf2eb10bbf73e703af699b36158c12b3f861ff988dc16ffa53720973', 'build/release-readiness/analyzer_pointer_features_617.py': '7f666c86adc89910a8ed6aab844ad2385d04f7f4a56dbc18a1ba3776c329fa0c', 'build/release-readiness/analyzer_drag_observer_617.py': '74f38417e9f4d28123f3da0c0bdbe880745d932b5563f200c3d9f0c49e6e006a', 'build/release-readiness/prepare_analyzer_pointer_617.py': 'fa222745ffdf282379fc3fd1dc5e2381ea3c2dee3a59d066b6f84095ec134ff3', 'build/release-readiness/run_analyzer_pointer_617.py': 'f2b46d5ea8c6fd94b76a1e2b78f16fc24ab3485ab4fd3e78d825a8512d337f47'}
SCHEMA='analyzer-original-decree-and-pool-observations-v1'
SCOPE=('Publisher operator-command entry, original decree input and independently computed native pool sets, reward/objective mode and resolution controls, '
       'moved input, full typed recovery/refusal and native close-return exactly once.')
LIMITS=('Exact staged Bountiful publisher and named target only. Operator permission is the native entry prerequisite. '
        'No bounty generation, all decree distributions, public-player command access or full-mod gameplay claim. '
        'The publisher may return the decree to a different empty player cell; no fixed return cell or retained final-close snapshot is invented.')


def specification(target,case,original):
    assert target in TARGETS and case==CASE and original==[SCREEN],'Unreviewed native Analyzer target/alias/class'


def pools(client):
    assert client['mode']in {'OBJ','REW'}and type(client['resolution'])is int and client['resolution']>0
    expected=client['expectedPools'];assert set(expected)=={'OBJ','REW'}
    assert all(isinstance(v,list)and all(isinstance(n,str)and n for n in v)and len(v)==len(set(v))for v in expected.values())
    actual=client['pools'];assert isinstance(actual,list)
    assert all(isinstance(v['id'],str)and v['id']and type(v['pixSize'])is int and v['pixSize']==client['resolution']for v in actual)
    ids=[v['id']for v in actual];assert len(ids)==len(set(ids))and sorted(ids)==sorted(expected[client['mode']])
    decree_ids=client['decreeIds'];assert isinstance(decree_ids,list)and all(isinstance(v,str)and v for v in decree_ids)
    assert decree_ids and expected[client['mode']],'Actual native decree and nonempty independent pool analysis required'


def validate(target,journal,states,driver_hash):
    specification(target,CASE,[SCREEN])
    assert journal['complete']is True and journal['cases']==[CASE]and journal['driver_sha256']==driver_hash
    assert [v['name']for v in journal['checks']]==list(NAMES)and all(v.get('passed')is True for v in journal['checks'])
    assert isinstance(states,list)and len(states)==len(LABELS)
    for state in states:
        client=state['client'];assert client['menu']==MENU and client['screen']==SCREEN
        assert type(client['session'])is int and client['session']>0 and type(client['locked'])is bool
        bounds=client['bounds'];assert set(bounds)=={'x','y','width','height'}
        assert all(type(v)is int for v in bounds.values())and bounds['width']>0 and bounds['height']>0
        assert client['mode']in {'OBJ','REW'}and type(client['resolution'])is int and client['resolution']>0
        assert typed(item(state['decree']))==typed(item(client['decree'])),'Client/server decree differs'
        inventory(state)
        assert isinstance(state['cursors'],list)and state['cursors']and all(isinstance(v['menu'],str)for v in state['cursors'])
    initial,inserted,reward,moved,objective,resolution,recovered,refused,before_close,reopened=states
    baseline=inventory(initial);empty=typed(nbtlib.Compound())
    decree=initial['inventory'][9]['item'];value=item(decree)
    assert value['id']==nbtlib.String('bountiful:decree')
    if 'count'in value:assert isinstance(value['count'],nbtlib.Int)and int(value['count'])==1
    data=value['components']['minecraft:custom_data'];assert isinstance(data['release_analyzer_marker'],nbtlib.Int)and int(data['release_analyzer_marker'])==47
    assert isinstance(data['typed'],nbtlib.ByteArray)and list(data['typed'])==[2,9]
    diamond=initial['inventory'][10]['item'];other=item(diamond)
    assert other['id']==nbtlib.String('minecraft:diamond')and isinstance(other['count'],nbtlib.Int)and int(other['count'])==3
    other_data=other['components']['minecraft:custom_data'];assert isinstance(other_data['release_analyzer_marker'],nbtlib.Int)and int(other_data['release_analyzer_marker'])==48
    assert not item(initial['decree'])and initial['client']['mode']=='OBJ';empty_cursors(initial)
    active=[inserted,reward,moved,objective,resolution,before_close]
    for state in active:
        assert typed(item(state['decree']))==typed(value)
        assert inventory(state)==tuple(empty if i==9 else v for i,v in enumerate(baseline))
        empty_cursors(state);pools(state['client'])
        assert state['client']['decreeIds']==inserted['client']['decreeIds']
        assert state['client']['expectedPools']==inserted['client']['expectedPools']
    assert inserted['client']['mode']=='OBJ'and reward['client']['mode']==moved['client']['mode']=='REW'
    assert objective['client']['mode']==resolution['client']['mode']=='OBJ'
    old,new=reward['client']['bounds'],moved['client']['bounds']
    assert(new['x'],new['y'])==(old['x']+16,old['y']+12)and(new['width'],new['height'])==(old['width'],old['height'])
    assert resolution['client']['resolution']>objective['client']['resolution']
    assert not item(recovered['decree'])and inventory(recovered)==baseline;empty_cursors(recovered)
    assert not item(refused['decree'])and inventory(refused)==tuple(empty if i==10 else v for i,v in enumerate(baseline))
    held=[item(v['item'])for v in refused['cursors']];assert any(v for v in held)and all(not v or typed(v)==typed(other)for v in held)
    assert not item(reopened['decree'])and Counter(inventory(reopened))==Counter(baseline);empty_cursors(reopened)
    assert all(v['client']['session']==initial['client']['session']for v in states[:-1])
    return dict(assertions=10,states=10,native_variants=1,contract='native-decree-analysis-not-bounty-generation')


def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('analyzer_features_checked')is True
    assert all(type(result[k])is int and result[k]==v for k,v in [('cases',1),('failed',0),('checks',10)])
    profile=(root/row['profile']).resolve();journal_path=checked(root,sealed['acceptance']);assert journal_path==profile/'analyzer-release/summary.json'
    expected_files={f'{n:03}-{label}.json'for n,label in enumerate(LABELS,1)}
    assert {v.name for v in journal_path.parent.glob('[0-9][0-9][0-9]-*.json')}==expected_files
    refs=[];states=[]
    for n,label in enumerate(LABELS,1):
        path=journal_path.parent/f'{n:03}-{label}.json';ref=reference(root,path);assert ref in sealed['full_states']
        refs.append(ref);states.append(json.loads(path.read_text()))
    selected=DRIVER
    if PRIVATE_DRIVER in sealed['archived_inputs']:
        for name,digest in PRIVATE_INPUTS.items():
            assert sealed['archived_inputs'].get(name)==digest,'Unreviewed private Analyzer QA input: '+name
        selected=PRIVATE_DRIVER
    validation=validate(target,json.loads(journal_path.read_text()),states,sealed['archived_inputs'][selected])
    if selected==PRIVATE_DRIVER:
        assert all(s['client']['resolutionDragging']is False for s in states),'A native slider latch remained held after mouse release'
        validation['native_slider_release_observed']=True
    publishers=[]
    for artifact in json.loads(checked(root,sealed['staged']).read_text())['artifacts']:
        if artifact['family']!='upstream'or not artifact['filename'].lower().startswith('bountiful-'):continue
        path=Path(artifact['file']);path=path if path.is_absolute()else root/path;raw=path.read_bytes()
        assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(path,digest,target.split('/')[1]),[MENU,SCREEN]);assert locations
        assert {v[:-6].replace('/','.')for loc in locations for v in loc['classes']}=={MENU,SCREEN}
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1,'Exactly one original Bountiful publisher required'
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',original_native_classes=original_classes,
        native_classes=[MENU,SCREEN],subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),
        acceptance=reference(root,journal_path),native_states=refs,validation=validation,publishers=publishers,scope=SCOPE,limits=LIMITS)


def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    assert derive(root,checked(root,record['sealed_evidence']),expected,record['case'],record['original_native_classes'])==record
    return True
