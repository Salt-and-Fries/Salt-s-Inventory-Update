"""Recheck actual native Wands tool selections; no physical-transfer claim."""
from pathlib import Path
import hashlib,json
import nbtlib
from focused_release_evidence import checked,reference,verify as verify_sealed
from native_publisher_index import index,matches
from sensor_feature_evidence import typed

CASES=('design-wands-tools-request','wands-tools')
TARGETS=('1.20.1/fabric','1.20.1/forge','1.21.1/fabric','1.21.1/neoforge','1.21.11/fabric','1.21.11/neoforge','26.1.2/fabric','26.1.2/neoforge','26.2/fabric','26.2/neoforge')
DRIVER='functional-tests/release-specialized/wands_features.py'
DRIVER_HASH='0ffd5749f820e39d758799a0497d10373ccfb06f336fcbeb0df2d8d3bcc5921a'
LABELS=('initial','baseline','hotbar-selected','main-selected','right-click','moved','hotbar-deselected','reopened','cleared')
NAMES=('original-native-selector-screen-and-menu','native-preferences-can-be-cleared-without-item-movement',
 'native-hotbar-tool-selection-reaches-server-and-client','native-main-inventory-tool-selection-reaches-server-and-client',
 'native-right-click-is-nondestructive-no-toggle','selector-window-moves-before-deselection','moved-selector-deselects-only-clicked-source',
 'native-selection-persists-through-key-request-reopening','all-full-item-data-and-cursors-preserved-through-selection',
 'second-source-deselection-remains-independent-and-conservative')
SCOPE='Original native tool-source selector: independent hotbar/main selection and deselection, synchronized server/client preferences, nondestructive right click, moved input, reopening and complete physical inventory/cursor conservation.'
LIMITS='Ten exact target/publisher executions; historical aliases may share one execution. Not owned storage, physical item transfer, OS shortcut delivery or world mining/building.'

def classes(target):
    assert target in TARGETS
    return ['net.nicguzzo.wands.menues.'+('WandMenu'if target.split('/')[0]in ['1.20.1','1.21.1','1.21.11']else'WandToolsMenu'),'net.nicguzzo.wands.client.screens.WandToolScreen']

def item(text):
    value=nbtlib.parse_nbt(text);assert isinstance(value,nbtlib.Compound);return value

def inventory(state):
    rows=state['inventory'];assert len(rows)>=36 and all(v['index']==i and type(v['index'])is int for i,v in enumerate(rows))
    return tuple(typed(item(v['item']))for v in rows)

def validate(target,journal,states):
    menu,screen=classes(target)
    assert journal['complete']is True and journal['cases']==['native-tools-selector']and journal['driver_sha256']==DRIVER_HASH
    assert [v['name']for v in journal['checks']]==list(NAMES)and all(v['passed']is True for v in journal['checks'])
    assert len(states)==9
    first=states[0];baseline=inventory(first)
    expected=[None,[],[1],[1,9],[1,9],[1,9],[9],[9],[]]
    for n,(state,selection)in enumerate(zip(states,expected)):
        client=state['client'];assert client['menu']==menu and client['screen']==screen
        assert type(client['session'])is int and client['session']>0 and type(client['locked'])is bool
        for values in [state['selected'],client['selected']]:
            assert isinstance(values,list)and len(set(values))==len(values)and all(type(v)is int and 0<=v<36 for v in values)
        assert state['selected']==client['selected']
        if selection is not None:assert set(state['selected'])==set(selection)
        assert inventory(state)==baseline,'Physical item data changed in a native configuration selector'
        assert state['cursors']and all(not item(v['item'])for v in state['cursors'])
        bounds=client['bounds'];assert set(bounds)=={'x','y','width','height'}and all(type(v)is int for v in bounds.values())
        assert bounds['width']>0 and bounds['height']>0
        assert client['session']==states[0 if n<7 else 7]['client']['session']
    for slot,id_,count,marker,wear in [(1,'diamond_pickaxe',1,17,True),(9,'diamond_shovel',1,19,True),(10,'cobblestone',7,23,False)]:
        value=item(first['inventory'][slot]['item']);assert value['id']==nbtlib.String('minecraft:'+id_)
        key='Count'if 'Count'in value else'count'
        if key in value:assert isinstance(value[key],nbtlib.Byte if key=='Count'else nbtlib.Int)and int(value[key])==count
        else:assert count==1 and not target.startswith('1.20.1/')
        data=value['tag']if 'tag'in value else value['components']['minecraft:custom_data']
        assert isinstance(data['release_wand_marker'],nbtlib.Int)and int(data['release_wand_marker'])==marker
        assert isinstance(data['typed'],nbtlib.ByteArray)and list(data['typed'])==[3,7]
        if wear:
            damage=data['Damage']if 'tag'in value else value['components']['minecraft:damage']
            assert isinstance(damage,nbtlib.Int)and int(damage)==2
    assert item(first['inventory'][0]['item'])['id']==nbtlib.String('wands:diamond_wand')
    before,after=states[3]['client']['bounds'],states[5]['client']['bounds']
    assert (after['x'],after['y'])==(before['x']+16,before['y']+12)
    assert (after['width'],after['height'])==(before['width'],before['height'])
    return dict(assertions=10,states=9,native_variants=1,physical_item_movement=False)

def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text(encoding='utf-8'));target=sealed['target'];native=classes(target)
    assert case in CASES and original_classes==[native[0]if case==CASES[0]else native[1]]
    verify_sealed(root,sealed,expected)
    assert sealed['archived_inputs'][DRIVER]==DRIVER_HASH
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('wands_features_checked')is True
    assert all(type(result[k])is int and result[k]==v for k,v in [('cases',1),('failed',0),('checks',10)])
    profile=root/row['profile'];journal=checked(root,sealed['acceptance']);assert journal==profile/'wands-release/summary.json'
    names={f'{i:03}-{label}.json'for i,label in enumerate(LABELS,1)}
    assert {p.name for p in journal.parent.glob('[0-9][0-9][0-9]-*.json')}==names
    refs=[];states=[]
    for i,label in enumerate(LABELS,1):
        p=journal.parent/f'{i:03}-{label}.json';ref=reference(root,p);assert ref in sealed['full_states'];refs.append(ref);states.append(json.loads(p.read_text()))
    validation=validate(target,json.loads(journal.read_text()),states)
    publishers=[]
    for artifact in json.loads(checked(root,sealed['staged']).read_text())['artifacts']:
        if artifact['family']!='upstream':continue
        p=Path(artifact['file']);p=p if p.is_absolute()else root/p;raw=p.read_bytes()
        assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(p,digest,target.split('/')[1]),native)
        if locations:
            assert {v[:-6].replace('/','.')for loc in locations for v in loc['classes']}==set(native)
            publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1
    return dict(schema='wands-native-tool-selector-observations-v1',target=target,case=case,disposition='functional-pass',
        original_native_classes=original_classes,native_classes=native,subjects={k:list(v)for k,v in expected.items()},
        sealed_evidence=reference(root,sealed_path),acceptance=reference(root,journal),native_states=refs,validation=validation,publishers=publishers,scope=SCOPE,limits=LIMITS)

def verify(root,record,expected):
    assert derive(root,checked(root,record['sealed_evidence']),expected,record['case'],record['original_native_classes'])==record
    return True
