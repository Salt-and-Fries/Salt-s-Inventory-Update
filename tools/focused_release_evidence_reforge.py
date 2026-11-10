"""Recheck actual native reforge costs/quality/typed recovery, without replay."""
from pathlib import Path
import hashlib,json,math,zipfile
import nbtlib
from focused_release_evidence import checked,reference,verify as verify_sealed
from native_publisher_index import index,matches
from sensor_feature_evidence import typed

SCHEMA='native-reforge-exact-cost-quality-and-recovery-v1'
PAIRS={('1.20.1/fabric','tieredz-reforge'):'tiered',('1.21.1/fabric','tieredz-reforge'):'tiered',('1.20.1/fabric','tierify-reforge'):'tierify'}
LABELS=('initial','seeded','foreign-refused','missing-addition','ready','moved','forged','recovered','reopened')
NAMES=('original-libz-tab-opens-the-native-three-cell-reforge','native-addition-slot-refuses-foreign-item-with-full-cursor','incomplete-native-prerequisites-disable-reforge','actual-native-materials-enable-reforge-with-no-forced-readiness','reforge-window-moves-before-the-native-action','native-reforge-consumes-one-of-each-actual-material','native-random-quality-is-registered-and-preserves-all-other-data','reforge-does-not-touch-other-player-items-or-cursor','exact-reforged-equipment-and-remainders-recover-once','native-reopening-preserves-the-exact-reforged-result','first-libz-tab-returns-to-original-anvil')
DRIVER='build/release-readiness/reforge_quality_features_641.py'
DRIVER_SHA='fca6da3ccc4a363073e26a4dddf1f6e6b232cbb48172780307b713ad0f6ac244'
ORIGINAL='functional-tests/release-specialized/reforge_features.py'
ORIGINAL_SHA='1d18b0c79a496505f6e1fdeec629c6648e21cd3a8623b720ad095772f20a5e5c'
QA_VARIANTS={('1.20.1/fabric', 'tieredz-reforge'): {'build/release-readiness/run_reforge_quality_642.py': 'fe0f077e624197c367eb88cbff2ca88a0c90d76ba73015c1cfae455be6dc57e3', 'build/release-readiness/raw_static_fields_636.py': 'fe90803ab243dd0e095c6167e13433c0bdca3262ce66fc8ba49bd915da21c149', 'build/release-readiness/reforge_static_observer_636.py': 'bdc3c13e1d09016a5efed11f6b3f2811acdc6ece88144cd2f83031faedec9359', 'build/release-readiness/prepare_reforge_static_636.py': '3648fa701a8dc3abfbbed1d60ffa233756d68210459ff9958c62c0367a1e255b', 'build/release-readiness/run_tierify_static_636.py': '5f4adb09794b3fcb4a7b09d0614c289efb541834ae6db45b8b0cf85b09dcccfd', 'build/release-readiness/reforge_visible_features_639.py': 'e2f58d8fe043590cffb1102239adb93b4ad65605cfc78b42a258dc7d4de47024', 'build/release-readiness/reforge_visible_observer_639.py': 'edac9eaa945416a7520e25029ec129beb315c1b00584cad44bec09b15f9860a0', 'build/release-readiness/prepare_reforge_visible_raw_639.py': '2676dc2043908db9e51e45ef7abc104371b6db595d8ce176da61bb0b15e3b3e3', 'build/release-readiness/run_tierify_visible_639.py': '3a2d86be28f2e720cc3d0cbacaaac71413acb281bd327d328b0d4e4fd603b2b1', 'build/release-readiness/reforge_quality_features_641.py': 'fca6da3ccc4a363073e26a4dddf1f6e6b232cbb48172780307b713ad0f6ac244', 'build/release-readiness/reforge_quality_observer_641.py': '9b9f8afa8382d67e7ff650d3f5713b4ca018fc6ec3e6b0663d56fadf3c18fdd1', 'build/release-readiness/prepare_reforge_quality_raw_641.py': '571d6c6523e2c97b846c5f8d39eca39119a5c9549949a861964140a5004fcd44', 'build/release-readiness/run_tierify_quality_641.py': '3096ca9c2374fc22256ae0057afbe12ade3e4da4772627ed46498515506ce96b', 'build/release-readiness/reforge_quality_observer_642.py': 'f3f1b3a4c835fef74c0ba9e1db308208be5780f40ac62e01c65994ecf6eaf70c', 'build/release-readiness/prepare_reforge_quality_raw_642.py': '33804c0780b5ed7fed1480f5dbdeea5ebdae856b10b3cf12d10e6ce71b73a449', 'build/release-readiness/run_tierify_quality_642.py': '088d748cb849325df50b12c59fed2ff134460ecbf6c5dd8699a103e3ee9f9d68'}, ('1.21.1/fabric', 'tieredz-reforge'): {'build/release-readiness/run_reforge_exact_645.py': '9b9dbc842bf64c0b163abcfd73f962c9c459e51dd37c1c3cd419f5a874cee193', 'build/release-readiness/raw_static_fields_636.py': 'fe90803ab243dd0e095c6167e13433c0bdca3262ce66fc8ba49bd915da21c149', 'build/release-readiness/reforge_static_observer_636.py': 'bdc3c13e1d09016a5efed11f6b3f2811acdc6ece88144cd2f83031faedec9359', 'build/release-readiness/prepare_reforge_static_636.py': '3648fa701a8dc3abfbbed1d60ffa233756d68210459ff9958c62c0367a1e255b', 'build/release-readiness/run_tierify_static_636.py': '5f4adb09794b3fcb4a7b09d0614c289efb541834ae6db45b8b0cf85b09dcccfd', 'build/release-readiness/reforge_visible_features_639.py': 'e2f58d8fe043590cffb1102239adb93b4ad65605cfc78b42a258dc7d4de47024', 'build/release-readiness/reforge_visible_observer_639.py': 'edac9eaa945416a7520e25029ec129beb315c1b00584cad44bec09b15f9860a0', 'build/release-readiness/prepare_reforge_visible_raw_639.py': '2676dc2043908db9e51e45ef7abc104371b6db595d8ce176da61bb0b15e3b3e3', 'build/release-readiness/run_tierify_visible_639.py': '3a2d86be28f2e720cc3d0cbacaaac71413acb281bd327d328b0d4e4fd603b2b1', 'build/release-readiness/reforge_quality_features_641.py': 'fca6da3ccc4a363073e26a4dddf1f6e6b232cbb48172780307b713ad0f6ac244', 'build/release-readiness/reforge_quality_observer_641.py': '9b9f8afa8382d67e7ff650d3f5713b4ca018fc6ec3e6b0663d56fadf3c18fdd1', 'build/release-readiness/prepare_reforge_quality_raw_641.py': '571d6c6523e2c97b846c5f8d39eca39119a5c9549949a861964140a5004fcd44', 'build/release-readiness/run_tierify_quality_641.py': '3096ca9c2374fc22256ae0057afbe12ade3e4da4772627ed46498515506ce96b', 'build/release-readiness/reforge_quality_observer_642.py': 'f3f1b3a4c835fef74c0ba9e1db308208be5780f40ac62e01c65994ecf6eaf70c', 'build/release-readiness/prepare_reforge_quality_raw_642.py': '33804c0780b5ed7fed1480f5dbdeea5ebdae856b10b3cf12d10e6ce71b73a449', 'build/release-readiness/run_tierify_quality_642.py': '088d748cb849325df50b12c59fed2ff134460ecbf6c5dd8699a103e3ee9f9d68', 'build/release-readiness/reforge_exact_observer_645.py': '74a7e2fba7c593bb47eb5f6f02e43697c2611a896134df251b9c0e6350aeb074', 'build/release-readiness/prepare_reforge_exact_raw_645.py': '23388b6410da35c2ad1ac17329eda16c06c5766da042523c08aa77474a5e5db2', 'build/release-readiness/run_tierify_exact_645.py': '0ff13f7336853b9d340516dd0e2976bbe52f160773f34ff26a42687ec6f745ea'}, ('1.20.1/fabric', 'tierify-reforge'): {'build/release-readiness/run_tierify_publisher_647.py': 'a885a8883d490327b5b1c388ebe28d415c256770c0489c23486a311ce6be80a5', 'build/release-readiness/raw_static_fields_636.py': 'fe90803ab243dd0e095c6167e13433c0bdca3262ce66fc8ba49bd915da21c149', 'build/release-readiness/reforge_static_observer_636.py': 'bdc3c13e1d09016a5efed11f6b3f2811acdc6ece88144cd2f83031faedec9359', 'build/release-readiness/prepare_reforge_static_636.py': '3648fa701a8dc3abfbbed1d60ffa233756d68210459ff9958c62c0367a1e255b', 'build/release-readiness/run_tierify_static_636.py': '5f4adb09794b3fcb4a7b09d0614c289efb541834ae6db45b8b0cf85b09dcccfd', 'build/release-readiness/reforge_visible_features_639.py': 'e2f58d8fe043590cffb1102239adb93b4ad65605cfc78b42a258dc7d4de47024', 'build/release-readiness/reforge_visible_observer_639.py': 'edac9eaa945416a7520e25029ec129beb315c1b00584cad44bec09b15f9860a0', 'build/release-readiness/prepare_reforge_visible_raw_639.py': '2676dc2043908db9e51e45ef7abc104371b6db595d8ce176da61bb0b15e3b3e3', 'build/release-readiness/run_tierify_visible_639.py': '3a2d86be28f2e720cc3d0cbacaaac71413acb281bd327d328b0d4e4fd603b2b1', 'build/release-readiness/reforge_quality_features_641.py': 'fca6da3ccc4a363073e26a4dddf1f6e6b232cbb48172780307b713ad0f6ac244', 'build/release-readiness/reforge_quality_observer_641.py': '9b9f8afa8382d67e7ff650d3f5713b4ca018fc6ec3e6b0663d56fadf3c18fdd1', 'build/release-readiness/prepare_reforge_quality_raw_641.py': '571d6c6523e2c97b846c5f8d39eca39119a5c9549949a861964140a5004fcd44', 'build/release-readiness/run_tierify_quality_641.py': '3096ca9c2374fc22256ae0057afbe12ade3e4da4772627ed46498515506ce96b', 'build/release-readiness/reforge_quality_observer_642.py': 'f3f1b3a4c835fef74c0ba9e1db308208be5780f40ac62e01c65994ecf6eaf70c', 'build/release-readiness/prepare_reforge_quality_raw_642.py': '33804c0780b5ed7fed1480f5dbdeea5ebdae856b10b3cf12d10e6ce71b73a449', 'build/release-readiness/run_tierify_quality_642.py': '088d748cb849325df50b12c59fed2ff134460ecbf6c5dd8699a103e3ee9f9d68', 'build/release-readiness/reforge_exact_observer_645.py': '74a7e2fba7c593bb47eb5f6f02e43697c2611a896134df251b9c0e6350aeb074', 'build/release-readiness/prepare_reforge_exact_raw_645.py': '23388b6410da35c2ad1ac17329eda16c06c5766da042523c08aa77474a5e5db2', 'build/release-readiness/run_tierify_exact_645.py': '0ff13f7336853b9d340516dd0e2976bbe52f160773f34ff26a42687ec6f745ea', 'build/release-readiness/tierify_native_observer_647.py': '544cb3a061f5f1b4ce538508b357b4bcb55ad4f4ea972914cc252546bcd97df7', 'build/release-readiness/prepare_tierify_native_raw_647.py': '30d0f46d236438ffc053572cd59d384b9194a8163b8cd41522559889c04e695d', 'build/release-readiness/run_tierify_native_647.py': 'bc8aecf602930e0b96e4338ec6515b5c08e044006e5ffe0fb79ecdcd1a5ab3ac'}}
SCOPE='Original LibZ tab entry, real prerequisites and foreign-input refusal, visible moved SIU action, original random registered quality, exact material costs and full unrelated item data, recovery/reopening and original return tab.'
LIMITS='Three exact native publisher/target workflows only. No predetermined quality, all RNG distributions, crafting recipe, LevelZ integration or complete-mod gameplay claim. The final return-to-anvil assertion comes from the original executed journal; no additional post-tab full state is invented.'

def specification(target,case,original):
    kind=PAIRS[target,case]
    screen='elocindev.tierify.screen.client.ReforgeScreen'if kind=='tierify'else'draylar.tiered.reforge.ReforgeScreen'
    menu='elocindev.tierify.screen.ReforgeScreenHandler'if kind=='tierify'else'draylar.tiered.reforge.ReforgeScreenHandler'
    assert original==[screen]
    return kind,menu,screen

def item(text):return nbtlib.parse_nbt(text)
def inv(state):
    rows=state['inventory'];assert [r['index']for r in rows]==list(range(len(rows)))and len(rows)>=36
    return [typed(item(r['item']))for r in rows]
def stack(text,count=None):
    value=item(text)
    if count is not None:
        key='Count'if 'Count'in value else'count';value[key]=type(value[key])(count)
    return typed(value)
def empty_cursors(state):
    assert state['cursors']and all(not item(v['item'])for v in state['cursors'])
def unrelated(text,kind):
    value=item(text);assert str(value['id'])=='minecraft:diamond_sword'
    if 'Count'in value:
        tag=value.get('tag',nbtlib.Compound());tag.pop('Tiered',None);tag.pop('durable',None)
        if kind=='tierify':tag.pop('itemborders_colors',None)
    else:value.get('components',nbtlib.Compound()).pop('tiered:tier',None)
    return typed(value)

def validate(target,case,original,journal,states,definitions):
    kind,menu,screen=specification(target,case,original)
    assert journal['complete']is True and journal['cases']==[kind+'-reforge']and journal['driver_sha256']==DRIVER_SHA
    assert [v['name']for v in journal['checks']]==list(NAMES)and all(v['passed']is True for v in journal['checks'])
    assert len(states)==9
    for s in states:
        c=s['client'];assert c['menu']==menu and c['screen']==screen and type(c['session'])is int and c['session']>0
        assert type(s['ready'])is bool and c['renderedConsumerReady']is s['ready']
        assert len(c['buttons'])==1 and c['buttons'][0]['disabled']is(not s['ready'])
        assert c['buttons'][0]['type']=='com.salts_inventory_update.compat.nativewindows.TabbedGearTemplate'
        assert c['renderedActionSource']==dict(x=79,y=56,width=18,height=18)
        assert [r['index']for r in s['cells']]==[0,1,2];inv(s)
        b=c['bounds'];assert set(b)=={'x','y','width','height'}and all(type(v)is int for v in b.values())and min(b['width'],b['height'])>0
        assert isinstance(c['locked'],bool)and set(s['knownQualities'])==set(definitions)
    initial,seeded,refused,partial,ready,moved,forged,recovered,reopened=states
    assert all(not item(v['item'])for v in initial['cells'])and not initial['ready'];empty_cursors(initial)
    for slot,id,count,marker in [(9,'minecraft:diamond',3,71),(10,'minecraft:diamond_sword',1,72),(11,'tiered:limestone_chunk'if kind=='tierify'else'minecraft:amethyst_shard',3,73),(12,'minecraft:dirt',2,74)]:
        v=item(seeded['inventory'][slot]['item']);assert str(v['id'])==id
        key='Count'if 'Count'in v else'count';assert type(v[key])is(nbtlib.Byte if key=='Count'else nbtlib.Int)and int(v[key])==count
        data=v['tag']if key=='Count'else v['components']['minecraft:custom_data']
        assert type(data['release_reforge_marker'])is nbtlib.Int and int(data['release_reforge_marker'])==marker
        assert type(data['typed'])is nbtlib.ByteArray and list(data['typed'])==[4,8]
    assert all(not item(v['item'])for v in refused['cells'])
    cursor=[stack(v['item'])for v in refused['cursors']if item(v['item'])]
    assert cursor and all(v==stack(seeded['inventory'][12]['item'])for v in cursor)
    assert not partial['ready']and not item(partial['cells'][2]['item'])
    assert stack(partial['cells'][0]['item'])==stack(seeded['inventory'][9]['item'])and stack(partial['cells'][1]['item'])==stack(seeded['inventory'][10]['item'])
    assert ready['ready']and moved['ready']and stack(ready['cells'][2]['item'])==stack(seeded['inventory'][11]['item'])
    for s in [partial,ready,moved,forged,recovered,reopened]:empty_cursors(s)
    old,new=ready['client']['bounds'],moved['client']['bounds']
    assert(new['x'],new['y'])==(old['x']+18,old['y']+12)and(new['width'],new['height'])==(old['width'],old['height'])
    assert [stack(v['item'])for v in moved['cells']]==[stack(v['item'])for v in ready['cells']]
    assert inv(moved)==inv(ready)
    for i in [0,2]:assert stack(forged['cells'][i]['item'])==stack(ready['cells'][i]['item'],2)
    quality=forged['quality'];assert quality and quality in definitions
    assert unrelated(forged['cells'][1]['item'],kind)==unrelated(ready['cells'][1]['item'],kind)
    value=item(forged['cells'][1]['item'])
    if 'Count'in value:
        assert str(value['tag']['Tiered']['Tier'])==quality
        durable=[a['modifier']['amount']for a in definitions[quality]['attributes']if a['type']=='tiered:generic.durable']
        expected={}if not durable else dict(durable=math.floor(float(durable[0])*100+.5)/100)
        assert forged['expectedQualityFields']==expected
        if expected:
            amount=expected['durable'];want=nbtlib.Int(round(amount))if abs(amount)%1<1.e-4 else nbtlib.Double(amount)
            assert typed(value['tag']['durable'])==typed(want)
        else:assert 'durable'not in value['tag']
    else:
        component=value['components']['tiered:tier'];assert str(component['tier'])==quality
        durable=[a['modifier']for a in definitions[quality]['attributes']if a['type']=='tiered:generic.durable']
        factor=-1 if not durable else math.floor(float(durable[0]['amount'])*100+.5)/100
        operation=0 if not durable else {'ADDITION':0,'ADD_VALUE':0,'MULTIPLY_BASE':1,'ADD_MULTIPLIED_BASE':1,'MULTIPLY_TOTAL':2,'ADD_MULTIPLIED_TOTAL':2}[durable[0]['operation']]
        if 'durable_factor'in component:assert typed(component['durable_factor'])==typed(nbtlib.Float(factor))
        else:assert factor==-1
        if 'operation'in component:assert typed(component['operation'])==typed(nbtlib.Int(operation))
        else:assert operation==0
    assert inv(forged)==inv(ready)
    expected=inv(seeded)
    for i,target in [(0,9),(1,10),(2,11)]:expected[target]=stack(forged['cells'][i]['item'])
    for s in [recovered,reopened]:assert inv(s)==expected and all(not item(v['item'])for v in s['cells'])
    assert len({s['client']['session']for s in states[:-1]})==1 and reopened['client']['session']!=initial['client']['session']
    return dict(assertions=11,states=9,quality=quality,contract='original-random-quality-and-exact-derived-durability-not-predetermined-RNG')

def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    kind,menu,screen=specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    assert sealed['archived_inputs'][ORIGINAL]==ORIGINAL_SHA and sealed['archived_inputs'][DRIVER]==DRIVER_SHA
    actual={k:v for k,v in sealed['archived_inputs'].items()if k.startswith('build/release-readiness/')}
    assert actual==QA_VARIANTS[target,case], 'Unreviewed private observer/feature/runner variant'
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('reforge_features_checked')is True and summary['publisher_kind']==kind
    assert all(type(result[k])is int and result[k]==v for k,v in [('cases',1),('failed',0),('checks',11)])
    profile=(root/row['profile']).resolve();journal_path=checked(root,sealed['acceptance']);assert journal_path==profile/'reforge-release/summary.json'
    files={f'{n:03}-{label}.json'for n,label in enumerate(LABELS,1)};assert {p.name for p in journal_path.parent.glob('[0-9][0-9][0-9]-*.json')}==files
    refs=[];states=[]
    for n,label in enumerate(LABELS,1):
        path=journal_path.parent/f'{n:03}-{label}.json';ref=reference(root,path);assert ref in sealed['full_states'];refs.append(ref);states.append(json.loads(path.read_text()))
    publishers=[];definitions={}
    for artifact in json.loads(checked(root,sealed['staged']).read_text())['artifacts']:
        if artifact['family']!='upstream'or not artifact['filename'].lower().startswith(kind+'-'):continue
        path=Path(artifact['file']);path=path if path.is_absolute()else root/path;raw=path.read_bytes()
        assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(path,digest,target.split('/')[1]),[menu,screen]);assert locations
        with zipfile.ZipFile(path)as jar:
            for name in jar.namelist():
                if name.startswith('data/tiered/item_attributes/')and name.endswith('.json'):
                    d=json.loads(jar.read(name));assert d['id']not in definitions;definitions[d['id']]=d
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1 and definitions
    validation=validate(target,case,original_classes,json.loads(journal_path.read_text()),states,definitions)
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',original_native_classes=original_classes,native_classes=[menu,screen],subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,journal_path),native_states=refs,validation=validation,publishers=publishers,scope=SCOPE,limits=LIMITS)

def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    assert derive(root,checked(root,record['sealed_evidence']),expected,record['case'],record['original_native_classes'])==record
    return True
