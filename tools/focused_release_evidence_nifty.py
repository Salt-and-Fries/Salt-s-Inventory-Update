"""Review the original cart feature journals; never manufacture native runs.

The two cart variants share one real execution per target. Entry, movement
and entity invalidation retain their original executed assertions; no extra
pre-move or post-invalidation snapshot is claimed when it was not retained.
"""
from pathlib import Path
import hashlib,json
import nbtlib
from focused_release_evidence import checked,reference,verify as verify_sealed
from focused_release_evidence_guard import item,inventory
from native_publisher_index import index,matches
from sensor_feature_evidence import typed

TARGETS=('1.20.1/fabric','1.21.1/fabric','1.21.11/fabric')
CASES={'niftycarts-plow':('plow','Plow',3,'minecraft:iron_hoe'),
       'niftycarts-seed-drill':('seed_drill','SeedDrill',9,'minecraft:wheat_seeds')}
DRIVER='functional-tests/release-specialized/nifty_features.py'
SCHEMA='nifty-original-equipment-observations-v1'
PRIVATE_DRIVER='build/release-readiness/nifty_compact_features_510.py'
PRIVATE_INPUTS={'build/release-readiness/nifty_compact_features_510.py': '7bcf886b2a845f35c5850f6f05f8b760143bb06413abb5c0c37fc145802a6010', 'build/release-readiness/nifty_shared_cursor_observer_508.py': '42cb24664e6f4972d875cc54c3e017968e8d5975aa177f2456af440fd04bb99c', 'build/release-readiness/prepare_nifty_shared_cursor_508.py': '68ba76cc70386559cc7c8cbe358f0d2d10b20819db7088a7069579c2c4b95c5a', 'build/release-readiness/run_Nifty_compact_510_01.py': '05814dd655fc1ac4432ee14405d0eb3f3f2ef3a11de9fc5b6e762e5d035dfb1e'}
PRIVATE_DRIVER_SHA=PRIVATE_INPUTS[PRIVATE_DRIVER]
ORIGINAL_DRIVER_SHA='a549a470315e8409c9c425a90b3833271321c54dec988b867819bf72704ae076'
CROUCH_DRIVER='build/release-readiness/nifty_crouch_features_513.py'
CROUCH_INPUTS={'build/release-readiness/nifty_crouch_features_513.py': '374f202fbf163abc176e7c17532f386b26b17ed13f184dd439e99b20a1182ab7', 'build/release-readiness/nifty_shared_cursor_observer_508.py': '42cb24664e6f4972d875cc54c3e017968e8d5975aa177f2456af440fd04bb99c', 'build/release-readiness/prepare_nifty_shared_cursor_508.py': '68ba76cc70386559cc7c8cbe358f0d2d10b20819db7088a7069579c2c4b95c5a', 'build/release-readiness/run_Nifty_crouch_513_01.py': '2f2b06303d90f8e20a1188c8a1318accb48095fa3c0804ebe6df7281877e5c52'}
CROUCH_DRIVER_SHA=CROUCH_INPUTS[CROUCH_DRIVER]
REVIEWED_DRIVERS={PRIVATE_DRIVER_SHA:(('1.20.1/fabric','1.21.1/fabric'),PRIVATE_INPUTS),CROUCH_DRIVER_SHA:(('1.21.11/fabric',),CROUCH_INPUTS)}


COMMON=('ordinary-native-entry-has-complete-cells','cart-window-moves-before-native-slot-input',
        'every-native-slot-retains-its-designated-complete-stack','native-input-consumes-each-player-reference-once',
        'cart-equipment-persists-through-ordinary-reopening','all-native-cell-contents-return-once-with-complete-data')
NAMES=tuple('plow:'+name for name in (*COMMON,'cart-invalidation-closes-its-native-source'))+tuple(
    'seed_drill:'+name for name in (*COMMON,'native-seed-filter-refuses-foreign-item-with-full-cursor-data','cart-invalidation-closes-its-native-source'))
FRAMES={'plow':(('0002','seeded'),('0002','inserted'),('0004','reopened'),('0005','returned')),
        'seed_drill':(('0009','seeded'),('0009','inserted'),('0011','reopened'),('0012','returned'),
                      ('0013','foreign-before'),('0013','foreign-refused'))}
LIMITS=('Exact staged NiftyCarts publisher and target only. Two variants share one original execution; aliases do not add runs or assertions. '
        'Original entry, movement and invalidation assertions remain required; no unrecorded before-move or post-invalidation state is invented. '
        'No cart driving, crop production, agricultural automation or full-mod gameplay claim.')


def classes(variant):
    return ('net.jmb19905.niftycarts.container.'+variant+'Menu','net.jmb19905.niftycarts.client.screen.'+variant+'Screen')


def specification(target,case,original):
    assert target in TARGETS and case in CASES,'Unreviewed Nifty target/alias'
    kind,variant,size,identifier=CASES[case]
    assert original==[classes(variant)[1]],'Unreviewed native cart classification'
    return kind,variant,size,identifier


def native(frame,kind,variant,size):
    assert set(frame)=={'client','server'}
    menu,screen=classes(variant);server=frame['server'];inventory(server)
    rows=server['menus'];assert isinstance(rows,list)and rows
    assert all(isinstance(v['class'],str)and isinstance(v['slots'],list)for v in rows)
    matches=[v for v in rows if v['class']==menu];assert len(matches)==1
    current=matches[0];assert len(current['slots'])>=size
    assert all(type(v['slot'])is int and v['slot']==i and isinstance(v['item'],str)for i,v in enumerate(current['slots']))
    windows=[v for v in frame['client']['allWindows']if v.get('menu')==menu];assert len(windows)==1
    window=windows[0];assert window['nativeScreen']==screen and type(window['sessionId'])is int and window['sessionId']>0
    bounds=window['bounds'];assert set(bounds)=={'x','y','width','height'}
    assert all(type(v)is int for v in bounds.values())and bounds['width']>0 and bounds['height']>0
    slots=window['slots'];assert all(type(v['player'])is bool for v in slots)
    cells=[v for v in slots if not v['player']];assert len(cells)==size
    assert sorted(v['index']for v in cells)==list(range(size))
    regions=window['niftyInputRegions'];assert isinstance(regions,list)and regions
    for cell in cells:
        assert all(type(cell[k])is int for k in ['index','x','y'])
        x,y=cell['x']-1,cell['y']-1
        covering=[]
        for region in regions:
            assert type(region['interactive'])is bool and all(type(region[k])is int for k in ['x','y'])
            source=region['source'];assert all(type(source[k])is int for k in ['x','y','width','height'])and source['width']>0 and source['height']>0
            if region['interactive']and source['x']<=x and source['y']<=y and x+18<=source['x']+source['width']and y+18<=source['y']+source['height']:
                covering.append(region)
        assert len(covering)==1,'Native cart slot rim is clipped, duplicated or not reachable'
    return current,window


def empty_cursors(server):
    assert server['menus']and all(not item(v['carried'])for v in server['menus'])


def seed(target,text,identifier,count,marker,damage=None):
    value=item(text);assert value['id']==nbtlib.String(identifier)
    key='Count'if 'Count'in value else'count'
    if key in value:
        assert isinstance(value[key],nbtlib.Byte if key=='Count'else nbtlib.Int)and int(value[key])==count
    else:assert not target.startswith('1.20.1/')and count==1
    data=value['tag']if 'tag'in value else value['components']['minecraft:custom_data']
    assert isinstance(data['release_cart_marker'],nbtlib.Int)and int(data['release_cart_marker'])==marker
    assert isinstance(data['typed'],nbtlib.ByteArray)and list(data['typed'])==[2,6]
    if damage is not None:
        actual=data['Damage']if 'tag'in value else value['components']['minecraft:damage']
        assert isinstance(actual,nbtlib.Int)and int(actual)==damage


def validate(target,journal,states,driver_hash):
    if driver_hash in REVIEWED_DRIVERS:assert target in REVIEWED_DRIVERS[driver_hash][0],'Unreviewed driver/target pair'
    shared=driver_hash in REVIEWED_DRIVERS and target!='1.20.1/fabric'
    if shared:assert target in ['1.21.1/fabric','1.21.11/fabric'],'Reviewed shared cursor applies only to modern native carts'
    def cursors_empty(server):
        empty_cursors(server)
        if shared:assert isinstance(server['sharedCursor'],str)and not item(server['sharedCursor'])
    assert target in TARGETS and journal['complete']is True and journal['cases']==['plow','seed_drill']
    assert journal['driver_sha256']==driver_hash and [v['name']for v in journal['checks']]==list(NAMES)
    assert len(NAMES)==15 and all(v.get('passed')is True for v in journal['checks'])
    assert set(states)==set(FRAMES)
    empty=typed(nbtlib.Compound())
    for case,(kind,variant,size,identifier)in CASES.items():
        frames=states[kind];assert len(frames)==len(FRAMES[kind])
        observed=[native(f,kind,variant,size)for f in frames]
        before,inserted,reopened,returned=frames[:4]
        baseline=inventory(before['server']);expected_cells=tuple(baseline[9+i]for i in range(size))
        for i in range(size):seed(target,before['server']['inventory'][9+i]['item'],identifier,1 if kind=='plow'else i+1,42+i,1 if kind=='plow'else None)
        getcells=lambda pair:tuple(typed(item(v['item']))for v in pair[0]['slots'][:size])
        assert getcells(observed[0])==(empty,)*size
        cursors_empty(before['server'])
        for n in [1,2]:
            assert getcells(observed[n])==expected_cells
            assert inventory(frames[n]['server'])==tuple(empty if i in range(9,9+size)else v for i,v in enumerate(baseline))
            cursors_empty(frames[n]['server'])
        assert getcells(observed[3])==(empty,)*size and inventory(returned['server'])==baseline
        cursors_empty(returned['server'])
        assert observed[0][1]['sessionId']==observed[1][1]['sessionId']
        assert observed[0][1]['bounds']==observed[1][1]['bounds'],'Geometry changed during ordinary slot transfers'
        if kind=='seed_drill':
            foreign,denied=frames[4:];original=inventory(foreign['server'])
            assert len(original)==len(baseline)and all(v==baseline[i]for i,v in enumerate(original)if i!=20)
            diamond=foreign['server']['inventory'][20]['item'];seed(target,diamond,'minecraft:diamond',3,99)
            assert getcells(observed[4])==getcells(observed[5])==(empty,)*size
            cursors_empty(foreign['server'])
            assert inventory(denied['server'])==tuple(empty if i==20 else v for i,v in enumerate(original))
            assert typed(item(denied['server']['sharedCursor'] if shared else observed[5][0]['carried']))==typed(item(diamond))
            if shared:assert not item(observed[5][0]['carried']),'Detached cart cursor must remain empty under modern shared ownership'
            held=[item(v['carried'])for v in denied['server']['menus']]
            assert held and all(not v or typed(v)==typed(item(diamond))for v in held)
    return dict(assertions=15,states=10,native_variants=2,contract='native-cart-equipment-not-driving-or-agriculture')


def reviewed_driver(target,journal,archived_inputs):
    original=archived_inputs[DRIVER]
    actual=journal['driver_sha256']
    if actual in REVIEWED_DRIVERS:
        targets,pins=REVIEWED_DRIVERS[actual]
        assert target in targets,'Unreviewed feature driver/target pair'
        assert original==ORIGINAL_DRIVER_SHA,'Original native action source changed'
        assert all(archived_inputs.get(name)==digest for name,digest in pins.items()),'Unreviewed cursor observer/transport source'
        return actual
    assert journal['driver_sha256']==original,'Unknown changed feature driver'
    return original


def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    kind,variant,_,_=specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('nifty_features_checked')is True
    assert all(type(result[k])is int and result[k]==v for k,v in [('cases',2),('failed',0),('checks',15)])
    profile=(root/row['profile']).resolve();journal_path=checked(root,sealed['acceptance'])
    assert journal_path==profile/'nifty-release/summary.json'
    expected_files={f'{n}-{k}-{label}.json'for k,rows in FRAMES.items()for n,label in rows}
    assert {v.name for v in journal_path.parent.glob('[0-9][0-9][0-9][0-9]-*.json')}==expected_files
    states={};refs=[]
    for k,labels in FRAMES.items():
        states[k]=[]
        for n,label in labels:
            path=journal_path.parent/f'{n}-{k}-{label}.json';ref=reference(root,path);assert ref in sealed['full_states']
            refs.append(ref);states[k].append(json.loads(path.read_text()))
    journal=json.loads(journal_path.read_text());driver_hash=reviewed_driver(target,journal,sealed['archived_inputs'])
    validation=validate(target,journal,states,driver_hash)
    wanted=classes(variant);publishers=[]
    for artifact in json.loads(checked(root,sealed['staged']).read_text())['artifacts']:
        if artifact['family']!='upstream'or not artifact['filename'].lower().startswith('niftycarts-'):continue
        path=Path(artifact['file']);path=path if path.is_absolute()else root/path;raw=path.read_bytes()
        assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(path,digest,target.split('/')[1]),wanted)
        if not locations:continue
        assert {v[:-6].replace('/','.')for loc in locations for v in loc['classes']}==set(wanted),'Incomplete original cart publisher'
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1,'Exactly one original NiftyCarts publisher required'
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',original_native_classes=original_classes,
        **({'qa_observation_variant':dict(feature_source=dict(file=PRIVATE_DRIVER if driver_hash==PRIVATE_DRIVER_SHA else CROUCH_DRIVER,sha256=driver_hash),
            compact_management_popup=True,authoritative_shared_cursor=not target.startswith('1.20.1/'),held_crouch_prerequisite=driver_hash==CROUCH_DRIVER_SHA)}if driver_hash in REVIEWED_DRIVERS else{}),
        native_classes=list(wanted),subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),
        acceptance=reference(root,journal_path),native_states=refs,validation=validation,publishers=publishers,
        scope=('Original '+kind+' native equipment UI, every designated cell, full typed transfer/return, native reopening and applicable seed-filter refusal. '
               'Original entry/movement/invalidation assertions are retained from the shared two-cart execution.'),limits=LIMITS)


def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    assert derive(root,checked(root,record['sealed_evidence']),expected,record['case'],record['original_native_classes'])==record
    return True
