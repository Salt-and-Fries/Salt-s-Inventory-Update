"""Review real native Guard Villagers observations without replaying input.

This reader does not launch Minecraft or grant acceptance to missing runs.
It binds the original thirteen assertions to their full item/state witnesses,
the exact native classes, staged publisher and unchanged shipping subjects.
"""
from pathlib import Path
import hashlib,json
import nbtlib
from focused_release_evidence import checked,reference,verify as verify_sealed
from native_publisher_index import index,matches
from sensor_feature_evidence import typed

TARGETS=('1.20.1/fabric','1.20.1/forge','1.21.1/fabric','1.21.1/neoforge',
         '1.21.11/fabric','26.1.2/neoforge','26.2/fabric','26.2/neoforge','26.3/neoforge')
CASE='guard-villager'
DRIVER='functional-tests/release-specialized/guard_features.py'
LABELS=('initial','seeded','inserted','moved','follow-on','follow-off','patrol-on',
        'reopened','patrol-off','returned','foreign','foreign-refused')
NAMES=(
    'ordinary-native-guard-entry-has-six-real-equipment-cells',
    'both-original-native-behavior-buttons-retained',
    'all-six-guard-equipment-cells-preserve-their-full-stacks',
    'equipment-insertion-consumes-each-player-reference-once',
    'guard-window-moves-with-equipment-and-native-buttons',
    'native-follow-action-reaches-the-server-and-client',
    'native-follow-can-be-restored-through-moved-window',
    'native-patrol-action-reaches-the-server-and-position',
    'equipment-and-patrol-state-persist-through-ordinary-reopening',
    'native-patrol-can-be-restored-after-reopening',
    'every-native-equipment-stack-recovers-with-full-data',
    'native-helmet-slot-refuses-foreign-item-with-exact-cursor',
    'guard-source-invalidation-removes-its-session')
ITEMS=('iron_helmet','iron_chestplate','iron_leggings','iron_boots','shield','iron_sword')
SCHEMA='guard-original-equipment-and-behavior-observations-v1'
SCOPE=('Ordinary Hero-permitted native entry; six complete-data equipment transfers and recovery, '
       'native follow/patrol controls and restoration, moved input, ordinary reopening, refused helmet input and entity invalidation.')
LIMITS=('Exact staged publisher and named target only. Hero permission is a fixture prerequisite. '
        'No combat AI, patrol travel, arbitrary reputation/permission configuration, extra native execution or full-mod gameplay claim.')

LATCH_VARIANTS={'556': {'targets': ['1.20.1/fabric'], 'inputs': {'build/release-readiness/guard_native_latch_features_556.py': 'bc264e53ccf07f34ba700adfc0dc62ed5496c7f827ac679fae64d0a49516c5e3', 'build/release-readiness/guard_native_latch_observer_556.py': '64a928f260102ade3ff5a1778ed600945baf62037f8dfa0877f97a909eb36710', 'build/release-readiness/prepare_guard_native_latch_556.py': '54a38a3f0a83a7129f3c4713b0b511ec3009992949f2ec3cb74ca57b01346581', 'build/release-readiness/run_Guard_native_latch_556_01.py': 'cc8ecce0c0c1cd46a2e67eabd66e00c7df8bceb40491731be7a0b8582f97e800'}, 'driver': 'build/release-readiness/guard_native_latch_features_556.py'}, '562': {'targets': ['1.20.1/fabric', '1.20.1/forge', '1.21.1/fabric', '1.21.1/neoforge', '1.21.11/fabric', '26.1.2/neoforge', '26.2/fabric', '26.2/neoforge', '26.3/neoforge'], 'inputs': {'build/release-readiness/guard_native_latch_features_562.py': 'ee93da99e694389233f55ec37333ad87ac88f06fcbcfea93e032e3b9278a9fe5', 'build/release-readiness/guard_native_latch_observer_562.py': '14afe0b30dfcfd3012846c674334f1bc397cde70f9639cb98d1560f5bc60b5a3', 'build/release-readiness/prepare_guard_native_latch_562.py': '55c7d07ad7a324f2e7b5d6f3b010c0c639dd74ec026e8279ea54fed8e41cd9eb', 'build/release-readiness/run_Guard_native_latch_562_01.py': '11f387f7dfd84164a9bbf684f36d8b1eb6bfb39f24b2c70dc64aa8bc85c2d0c3'}, 'driver': 'build/release-readiness/guard_native_latch_features_562.py'}, '569': {'targets': ['1.21.1/neoforge', '26.1.2/neoforge', '26.2/neoforge', '26.3/neoforge'], 'inputs': {'build/release-readiness/guard_native_latch_features_562.py': 'ee93da99e694389233f55ec37333ad87ac88f06fcbcfea93e032e3b9278a9fe5', 'build/release-readiness/guard_native_latch_observer_562.py': '14afe0b30dfcfd3012846c674334f1bc397cde70f9639cb98d1560f5bc60b5a3', 'build/release-readiness/prepare_guard_native_latch_562.py': '55c7d07ad7a324f2e7b5d6f3b010c0c639dd74ec026e8279ea54fed8e41cd9eb', 'build/release-readiness/run_Guard_native_latch_562_01.py': '11f387f7dfd84164a9bbf684f36d8b1eb6bfb39f24b2c70dc64aa8bc85c2d0c3', 'build/release-readiness/guard_native_latch_features_569.py': '01c2ea602ca6d243779e9a9ef803d77fb137bb081b1e636e30d51dbc0bb70c38', 'build/release-readiness/run_guard_named_entry_569.py': 'fbdd7a8626ce31971bfb9478a466f9b4be665c92d07a820fdafd4e3eed2a0464', 'build/release-readiness/named_air_fixture_566.py': '0f799ff55e3a2a0da275ba5304800798dcee755ac37077a4e4704607f11c0430', 'build/release-readiness/run_guard_named_air_566.py': 'c7cf3b02621e6afa76827b63477951d486655f16f72a0f563a380ebc5a297b49'}, 'driver': 'build/release-readiness/guard_native_latch_features_569.py'}}
LATCH_PINS={'1.20.1/fabric': {'mode': 'direct', 'publisher': {'file': 'build/compatibility-coverage/upstream-fixtures/jars/afdb9cac841cb488-guardvillagers-2.0.9-1.20.1.jar', 'sha256': 'a18f60127cfc8f0f75867738255a98026ae53a3f4a02b5ce4901baaecfcab502'}}, '1.20.1/forge': {'mode': 'inverse', 'publisher': {'file': 'build/compatibility-coverage/upstream-fixtures/jars/8c4aa8a0b9ea3611-guardvillagers-1.20.1-1.6.19.jar', 'sha256': '4c48c88144bcd8d47820baa5ca0d0c1cab8984da4ec2ceeb7ed164c27d1266f3'}}, '1.21.1/fabric': {'mode': 'direct', 'publisher': {'file': 'build/compatibility-coverage/upstream-fixtures/jars/d0dfa35043a7f7f6-guardvillagers-2.1.2-1.21.1.jar', 'sha256': 'b7a2683564635186a37c1ab6def07cecab7505bd054357d0aa9bbc8f889345c8'}}, '1.21.1/neoforge': {'mode': 'inverse', 'publisher': {'file': 'build/compatibility-coverage/upstream-fixtures/jars/2b25eaace0b7ec4c-guardvillagers-2.4.12-1.21.1.jar', 'sha256': 'aab03d59216bd931a05434b2527e0d5b7641793bd0178a9958100579c4f11407'}}, '1.21.11/fabric': {'mode': 'direct', 'publisher': {'file': 'build/compatibility-coverage/upstream-fixtures/jars/83d7947d16df5302-guardvillagers-2.1.2-1.21.11.jar', 'sha256': 'bf9b9f06ec58bc6f8ce60a10c02a9cd71f353368e8effc46d38995732cf82232'}}, '26.1.2/neoforge': {'mode': 'direct', 'publisher': {'file': 'build/compatibility-coverage/upstream-fixtures/jars/550d5cb9ce79ea3f-guardvillagers-3.0.4-26.1.0.jar', 'sha256': 'e9031aaefd32de41c9134df5c0124905ec06d07d7a9e75515e13dad66a9b1964'}}, '26.2/fabric': {'mode': 'direct', 'publisher': {'file': 'build/compatibility-coverage/upstream-fixtures/jars/322c4e0cd2b2b6d9-guardvillagers-2.1.3-26.2.jar', 'sha256': '4cf5281a7de570424260bbb2a34c56d44be344f5deffe4a1bab096526a409a03'}}, '26.2/neoforge': {'mode': 'direct', 'publisher': {'file': 'build/compatibility-coverage/upstream-fixtures/jars/e29ed8769bedd972-guardvillagers-4.0.4-26.2.0.jar', 'sha256': '27bb2c18d9209a209b417761633b2986e349bd2b60dec50eb70629fccad25f16'}}, '26.3/neoforge': {'mode': 'direct', 'publisher': {'file': 'build/compatibility-coverage/upstream-fixtures/jars/3842d6e6a85ef47c-guardvillagers-5.0.0-26.3.0.jar', 'sha256': 'd17a772c71f709516c08270b40b497429aaf7b40e55f079a9017bec56058203a'}}}

def latch_contract(sealed,target):
    archived=sealed['archived_inputs']
    names=[key for key,value in LATCH_VARIANTS.items()if value['driver']in archived]
    if not names:return None
    assert names in [['556'],['562'],['562','569']],'Unexpected private Guard variant combination'
    variant=LATCH_VARIANTS[names[-1]];assert target in variant['targets']
    assert all(archived.get(path)==digest for path,digest in variant['inputs'].items()),'Unreviewed private Guard source'
    assert archived.get(DRIVER)=='efadd66e3b1efe86846ef98823342453b971ec12f7c6596eeeeae643f7afab70'
    return dict(mode=LATCH_PINS[target]['mode'],driver=variant['driver'],publisher=LATCH_PINS[target]['publisher']['sha256'])


def validate_latch(target,states,contract):
    inverse=contract['mode']=='inverse';index=6 if inverse else 8
    assert len(states)==13
    extra=states[index];prior=states[index-1]
    assert prior['patrolling']is (not inverse) and prior['client']['nativePatrolLatch']is False
    assert extra['patrolling']is (not inverse) and extra['client']['patrolling']is (not inverse)
    assert extra['client']['nativePatrolLatch']is True
    for key in ['screen','menu','session','bounds','buttons','locked','following']:
        assert extra['client'][key]==prior['client'][key]
    assert extra['following']==prior['following']
    assert inventory(extra)==inventory(prior)and cells(extra)==cells(prior)
    empty_cursors(extra)
    base=states[:index]+states[index+1:]
    expected=[False]*6+([False,False,True,True,True,True]if inverse else[True,False,False,False,False,False])
    assert len(expected)==12
    for state,wanted in zip(base,expected):
        assert type(state['client']['nativePatrolLatch'])is bool and state['client']['nativePatrolLatch']is wanted
    return base


def native_classes(target):
    assert target in TARGETS,'Unreviewed Guard target'
    if target.endswith('/fabric'):
        return ('dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler',
                'dev.sterner.guardvillagers.client.screen.GuardVillagerScreen')
    return ('tallestegg.guardvillagers.'+('entities.'if target=='1.20.1/forge'else'common.entities.')+'GuardContainer',
            'tallestegg.guardvillagers.client.gui.GuardInventoryScreen')


def specification(target,case,classes):
    menu,screen=native_classes(target)
    assert case==CASE and classes==[screen],'Unreviewed historical Guard alias/class'
    return menu,screen


def item(text):
    assert isinstance(text,str)
    value=nbtlib.parse_nbt(text)
    assert isinstance(value,nbtlib.Compound)
    return value


def inventory(value):
    rows=value['inventory'];assert isinstance(rows,list)and len(rows)>=36
    assert all(type(v['index'])is int and v['index']==i for i,v in enumerate(rows))
    return tuple(typed(item(v['item']))for v in rows)


def cells(value):
    rows=value['cells'];assert isinstance(rows,list)and len(rows)==6
    assert all(type(v['index'])is int and v['index']>=0 for v in rows)
    assert len({v['index']for v in rows})==6
    return tuple(typed(item(v['item']))for v in rows)


def empty_cursors(value):
    rows=value['cursors'];assert isinstance(rows,list)and rows
    assert all(isinstance(v['menu'],str)and not item(v['item'])for v in rows),'Missing or occupied original cursor'


def validate(target,journal,states,removed,driver_hash):
    menu,screen=native_classes(target)
    assert journal['complete']is True and journal['cases']==[CASE]
    assert journal['driver_sha256']==driver_hash
    assert [v['name']for v in journal['checks']]==list(NAMES),'Missing, repeated or reordered original assertion'
    assert all(v.get('passed')is True for v in journal['checks'])
    assert isinstance(states,list)and len(states)==len(LABELS)
    for state in states:
        client=state['client']
        assert client['menu']==menu and client['screen']==screen
        assert type(client['session'])is int and client['session']>0
        assert type(client['locked'])is bool
        for key in ('following','patrolling'):
            assert type(state[key])is bool and type(client[key])is bool and client[key]==state[key]
        bounds=client['bounds'];assert set(bounds)=={'x','y','width','height'}
        assert all(type(v)is int for v in bounds.values())and bounds['width']>0 and bounds['height']>0
        buttons=client['buttons'];assert isinstance(buttons,list)and len(buttons)==2
        assert all(type(b['follow'])is bool and type(b['ordinal'])is int and b['ordinal']>=0
                   and b['type'].endswith('$GuardGuiButton')and isinstance(b['message'],str)
                   and type(b['active'])is bool and b['visible']is True for b in buttons)
        assert {b['follow']for b in buttons}=={False,True}and len({b['ordinal']for b in buttons})==2
        assert isinstance(state['patrolPos'],str)
        inventory(state);cells(state)
        assert [v['index']for v in state['cells']]==[v['index']for v in states[0]['cells']]
    initial,seeded,inserted,moved,follow,unfollow,patrol,reopened,restored,returned,foreign,denied=states
    for state,follow_button in [(moved,True),(follow,True),(unfollow,False),(reopened,False)]:
        assert next(b for b in state['client']['buttons']if b['follow']==follow_button)['active']is True
    assert all(not item(v['item'])for v in initial['cells'])
    baseline=inventory(initial);seed=inventory(seeded)
    assert len(baseline)==len(seed)and all(baseline[i]==seed[i]for i in range(len(seed))if i not in range(9,15))
    for i,name in enumerate(ITEMS):
        value=item(seeded['inventory'][9+i]['item'])
        assert value['id']==nbtlib.String('minecraft:'+name)
        key='Count'if 'Count'in value else'count'
        if key in value:
            assert isinstance(value[key],nbtlib.Byte if key=='Count'else nbtlib.Int)and int(value[key])==1
        else:assert target!='1.20.1/fabric'and target!='1.20.1/forge','Legacy Count must be present'
        data=value['tag']if 'tag'in value else value['components']['minecraft:custom_data']
        assert isinstance(data['release_guard_marker'],nbtlib.Int)and int(data['release_guard_marker'])==40+i
        assert isinstance(data['typed'],nbtlib.ByteArray)and list(data['typed'])==[2,8]
        damage=data['Damage']if 'tag'in value else value['components']['minecraft:damage']
        assert isinstance(damage,nbtlib.Int)and int(damage)==2
    empty=typed(nbtlib.Compound())
    carried=tuple(seed[i]for i in range(9,15))
    inserted_inventory=tuple(empty if i in range(9,15)else v for i,v in enumerate(seed))
    for state in states[:2]:
        assert cells(state)==(empty,)*6
        empty_cursors(state)
    for state in states[2:9]:
        assert cells(state)==carried and inventory(state)==inserted_inventory,'Native equipment/full inventory changed during controls or reopening'
        empty_cursors(state)
    assert all(v['client']['session']==initial['client']['session']for v in states[:7])
    assert all(v['client']['session']==reopened['client']['session']for v in states[7:])
    before,after=inserted['client']['bounds'],moved['client']['bounds']
    assert (after['x'],after['y'])==(before['x']+16,before['y']+12)
    assert (after['width'],after['height'])==(before['width'],before['height'])
    assert follow['following']is not moved['following']and unfollow['following']==moved['following']
    assert patrol['patrolling']is not unfollow['patrolling']and patrol['patrolPos']!='null'
    assert reopened['patrolling']==patrol['patrolling']and reopened['following']==unfollow['following']
    assert restored['patrolling']==unfollow['patrolling']and restored['following']==unfollow['following']
    assert inventory(returned)==seed and cells(returned)==(empty,)*6;empty_cursors(returned)
    foreign_inventory=inventory(foreign);assert len(foreign_inventory)==len(seed)
    assert all(v==seed[i]for i,v in enumerate(foreign_inventory)if i!=20)
    diamond=item(foreign['inventory'][20]['item']);assert diamond['id']==nbtlib.String('minecraft:diamond')
    key='Count'if 'Count'in diamond else'count'
    assert isinstance(diamond[key],nbtlib.Byte if key=='Count'else nbtlib.Int)and int(diamond[key])==3
    assert cells(foreign)==cells(denied)==(empty,)*6;empty_cursors(foreign)
    assert inventory(denied)==tuple(empty if i==20 else v for i,v in enumerate(foreign_inventory))
    held=[item(v['item'])for v in denied['cursors']]
    assert held and all(isinstance(v['menu'],str)for v in denied['cursors'])
    nonempty=[v for v in held if v];assert nonempty and all(typed(v)==typed(diamond)for v in nonempty)
    assert isinstance(removed['menus'],list)
    assert not any(v.get('type')==menu or v.get('session')==reopened['client']['session']for v in removed['menus'])
    return dict(assertions=13,states=12,native_variants=1,contract='native-equipment-and-behavior-controls-not-combat-or-travel')


def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text(encoding='utf-8'));target=sealed['target']
    classes=specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text(encoding='utf-8'));row=summary['results'][0];result=row['results']
    assert result.get('guard_features_checked')is True
    assert all(type(result[k])is int and result[k]==v for k,v in [('cases',1),('failed',0),('checks',13)])
    profile=(root/row['profile']).resolve();journal_path=checked(root,sealed['acceptance'])
    assert journal_path==profile/'guard-release/summary.json'
    contract=latch_contract(sealed,target)
    labels=list(LABELS)
    if contract:labels.insert(6 if contract['mode']=='inverse'else 8,'native-latch-first-press')
    state_refs=[];states=[]
    assert {v.name for v in journal_path.parent.glob('[0-9][0-9][0-9]-*.json')}=={f'{n:03}-{label}.json'for n,label in enumerate(labels,1)}
    for n,label in enumerate(labels,1):
        path=journal_path.parent/f'{n:03}-{label}.json';ref=reference(root,path)
        assert ref in sealed['full_states'];state_refs.append(ref);states.append(json.loads(path.read_text(encoding='utf-8')))
    removal_path=profile/'regression-state.json';removed=reference(root,removal_path)
    base=validate_latch(target,states,contract)if contract else states
    validation=validate(target,json.loads(journal_path.read_text(encoding='utf-8')),base,
                        json.loads(removal_path.read_text(encoding='utf-8')),sealed['archived_inputs'][contract['driver']if contract else DRIVER])
    if contract:validation.update(states=13,native_latch_mode=contract['mode'],original_assertions=13,extra_native_effect_and_full_data_conservation=True)
    publishers=[]
    for artifact in json.loads(checked(root,sealed['staged']).read_text(encoding='utf-8'))['artifacts']:
        if artifact['family']!='upstream':continue
        path=Path(artifact['file']);path=path if path.is_absolute()else root/path;raw=path.read_bytes()
        assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(path,digest,target.split('/')[1]),classes)
        if not locations:continue
        assert {v[:-6].replace('/','.')for loc in locations for v in loc['classes']}==set(classes),'Incomplete native Guard publisher'
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1,'Exactly one original native Guard publisher required'
    if contract:assert publishers[0]['sha256']==contract['publisher'],'Wrong native latch publisher pin'
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',original_native_classes=original_classes,
                native_classes=list(classes),subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),
                acceptance=reference(root,journal_path),native_states=state_refs,source_removal=removed,
                validation=validation,publishers=publishers,scope=SCOPE,limits=LIMITS)


def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    checked(root,record['source_removal'])
    assert derive(root,checked(root,record['sealed_evidence']),expected,record['case'],record['original_native_classes'])==record
    return True
