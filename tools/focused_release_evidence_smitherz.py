"""Recheck paid native linking/extraction and the exact legacy upstream limit."""
from pathlib import Path
from collections import Counter
import copy,hashlib,json
import nbtlib
from focused_release_evidence import checked,reference,verify as verify_sealed
from focused_release_evidence_reforge import stack,inv,item,empty_cursors
from sensor_feature_evidence import typed
from native_publisher_index import index,matches

SCHEMA='smitherz-native-paid-linking-extraction-and-explicit-upstream-limit-v1'
PAIRS={(t,c)for t in ['1.20.1/fabric','1.21.1/fabric']for c in ['smitherz-smither','smitherz-grinder']}
DRIVER='build/release-readiness/smither_native_contract_features_664.py'
QA_VARIANTS={'1.20.1/fabric': {'build/release-readiness/run_smither_native_contract_664.py': '2c967f1f166e32c0e7d2dd51de4de0d36f01c9294ba7ca7add6e20c29d83e76d', 'build/release-readiness/smither_visible_features_652.py': 'ff84d26aed5bb704a0bedf1099e3f5f7778239fd7795400934b501c446825a4e', 'build/release-readiness/smither_visible_observer_652.py': 'cb37538a04e3d2d6aa9bea3938c87d861d6d5f274f8b28d88d6aa570a329e764', 'build/release-readiness/prepare_smither_visible_652.py': '2cc0063edaa6b9639dec09fff7ae596d8abfdd7ef21076c0eee4e13084c8901d', 'build/release-readiness/smither_preserved_station_features_655.py': 'b5ffbfedbc90e9225e1107075ec3b981376ff7dcfafa1e9db276f2af41049f6e', 'build/release-readiness/prepare_smither_preserved_station_655.py': '2c2ff27c653bd19b606bf3ced0e28108c1716b4cdb733f09b26e29aff5b805b8', 'build/release-readiness/smither_native_contract_features_664.py': 'd3a55642cd3dc24c869afb0225ac6b941ce7875564797e7055dbe2d626597244', 'build/release-readiness/smither_native_contract_observer_664.py': '53a2eacb7914f4fb3810a3d48dcdae1db8850587e9b9dac8f45ca3c53c00333e', 'build/release-readiness/prepare_smither_native_contract_664.py': '8c282adf3e008496cb390655fb410700c91e2bf2ab73d95c84b7817ee003f9b3'}, '1.21.1/fabric': {'build/release-readiness/run_smither_registry_667.py': '907488ec231326464bf083c6f1657f80a76b739b7482c5639042a7d72617e854', 'build/release-readiness/smither_visible_features_652.py': 'ff84d26aed5bb704a0bedf1099e3f5f7778239fd7795400934b501c446825a4e', 'build/release-readiness/smither_visible_observer_652.py': 'cb37538a04e3d2d6aa9bea3938c87d861d6d5f274f8b28d88d6aa570a329e764', 'build/release-readiness/prepare_smither_visible_652.py': '2cc0063edaa6b9639dec09fff7ae596d8abfdd7ef21076c0eee4e13084c8901d', 'build/release-readiness/smither_preserved_station_features_655.py': 'b5ffbfedbc90e9225e1107075ec3b981376ff7dcfafa1e9db276f2af41049f6e', 'build/release-readiness/prepare_smither_preserved_station_655.py': '2c2ff27c653bd19b606bf3ced0e28108c1716b4cdb733f09b26e29aff5b805b8', 'build/release-readiness/smither_native_contract_features_664.py': 'd3a55642cd3dc24c869afb0225ac6b941ce7875564797e7055dbe2d626597244', 'build/release-readiness/smither_native_contract_observer_664.py': '53a2eacb7914f4fb3810a3d48dcdae1db8850587e9b9dac8f45ca3c53c00333e', 'build/release-readiness/prepare_smither_native_contract_664.py': '8c282adf3e008496cb390655fb410700c91e2bf2ab73d95c84b7817ee003f9b3', 'build/release-readiness/smither_registry_observer_667.py': '1cb87a9260fd57c9c0e728c9eec2dd1543c169f5751a6048792f383170c8e484', 'build/release-readiness/prepare_smither_registry_667.py': 'a42f511bf2c410038063fa81cc7097dcb2250e9395889d4dc9446a9b5253fd67'}}
CONTROL={'file':'output/release-readiness/smither-native-grinding-663-01/native-control.json','sha256':'7a2033a615759061819709096d52669bfdb7f7549883d8d95ea22af30b9a653e'}
LEGACY_SHA='940ba920d6fc013d35738317faaf84ddff69e2e94642c8ed7ac77f8c61195224'
PREFIX=['smither-initial','smither-seeded','three-sockets','foreign-hammer-refused','smither-moved']
SUFFIX=['linked-recovered','link-reopened','grinder-initial','grinder-tool','grinder-preview','grinder-cancelled','grinder-ready','grinder-output-picked','grinder-settled']

def classes(case):
    stem='Smither'if case=='smitherz-smither'else'Grinder'
    return 'net.smitherz.screen.'+stem+'ScreenHandler','net.smitherz.screen.'+stem+'Screen'
def without_gems(text):
    value=item(text);assert str(value['id'])=='minecraft:iron_sword'
    if 'Count'in value:value.get('tag',nbtlib.Compound()).pop('Gems',None)
    else:
        c=value['components']['smitherz:gems'];assert type(c)is nbtlib.Compound and int(c['size'])==3;c.pop('gems',None)
    return typed(value)
def embedded_gems(text):
    value=item(text)
    if not value:return []
    values=value.get('tag',nbtlib.Compound()).get('Gems',[])if 'Count'in value else value.get('components',nbtlib.Compound()).get('smitherz:gems',nbtlib.Compound()).get('gems',[])
    return [typed(v)for v in values]
def paid_tool(text,properties):
    value=item(text);key='Count'if'Count'in value else'count';count=int(value[key]);assert count==1
    if not properties['damageable']:return stack('{}')
    assert properties['damage']+1<properties['maxDamage']
    if key=='Count':value.setdefault('tag',nbtlib.Compound())['Damage']=nbtlib.Int(properties['damage']+1)
    else:value.setdefault('components',nbtlib.Compound())['minecraft:damage']=nbtlib.Int(properties['damage']+1)
    return typed(value)

def validate(target,journal,states,driver_sha):
    assert target in ['1.20.1/fabric','1.21.1/fabric']
    assert journal['complete']is True and journal['cases']==['smitherz-smither','smitherz-grinder']and journal['driver_sha256']==driver_sha
    attempts=sorted(int(k.split('-')[1])for k in states if k.startswith('attempt-')and k.endswith('-ready'))
    assert attempts==list(range(1,len(attempts)+1))and 1<=len(attempts)<=3
    labels=PREFIX+[f'attempt-{n}-{suffix}'for n in attempts for suffix in ['ready','outcome','settled']]+SUFFIX
    assert list(states)==labels
    for label,s in states.items():
        kind='smitherz-grinder'if label.startswith('grinder-')else'smitherz-smither';menu,screen=classes(kind);c=s['client']
        assert c['menu']==menu and c['screen']==screen and type(c['session'])is int and c['session']>0 and type(c['locked'])is bool
        inv(s);assert isinstance(s['worldDrops'],list)
        for value in s['worldDrops']:item(value)
        assert [v['index']for v in s['cells']]==list(range(len(s['cells'])))
        assert embedded_gems(s['cells'][0]['item'])==[stack(v)for v in s['gems']]
        if kind=='smitherz-smither':
            assert len(c['buttons'])==1 and c['renderedConsumerReady']is s['nativeReady']
            assert c['buttons'][0]['disabled']is(not s['nativeReady'])and c['renderedActionSource']==dict(x=148,y=18,width=18,height=18)
        else:assert len(s['cells'])==3
        if label!='foreign-hammer-refused':empty_cursors(s)
    initial,seeded,equipment,refused,moved=[states[k]for k in PREFIX]
    assert initial['client']['buttons'][0]['disabled']and all(not item(v['item'])for v in initial['cells'])
    expected=inv(seeded);gear=seeded['inventory'][9]['item'];diamond=seeded['inventory'][17]['item']
    fixtures=[(9,'minecraft:iron_sword',81),(17,'minecraft:diamond',89)]
    for n in range(3):fixtures.extend([(10+2*n,'smitherz:smither_hammer_4',82+2*n),(11+2*n,'smitherz:strength_1_gem'if target=='1.20.1/fabric'else'smitherz:gem',83+2*n)])
    for slot,id,marker in fixtures:
        value=item(seeded['inventory'][slot]['item']);assert str(value['id'])==id
        key='Count'if'Count'in value else'count';assert type(value[key])is(nbtlib.Byte if key=='Count'else nbtlib.Int)and int(value[key])==1
        data=value['tag']if key=='Count'else value['components']['minecraft:custom_data']
        assert type(data['release_smither_marker'])is nbtlib.Int and int(data['release_smither_marker'])==marker
        assert type(data['typed'])is nbtlib.ByteArray and list(data['typed'])==[5,9]
    assert sum(v['gemSlot']and v['active']for v in equipment['cells'])==3 and stack(equipment['cells'][0]['item'])==stack(gear)
    expected[9]=stack('{}');assert inv(equipment)==expected
    assert not item(refused['cells'][1]['item'])
    held=[stack(v['item'])for v in refused['cursors']if item(v['item'])];assert held and all(v==stack(diamond)for v in held)
    old,new=equipment['client']['bounds'],moved['client']['bounds'];assert(new['x'],new['y'])==(old['x']+18,old['y']+12)and(new['width'],new['height'])==(old['width'],old['height'])
    assert inv(moved)==expected
    names=['original-linking-tab-and-disabled-empty-action','native-equipment-enables-three-original-gem-sockets','native-hammer-cell-refuses-foreign-diamond-with-exact-cursor','linking-window-moves-before-native-action']
    linked=chosen=None
    for n in attempts:
        ready,outcome,settled=[states[f'attempt-{n}-{suffix}']for suffix in ['ready','outcome','settled']]
        tool_slot,gem_slot=8+2*n,9+2*n
        assert not ready['client']['buttons'][0]['disabled']and not ready['cells'][2]['linked']
        assert stack(ready['cells'][1]['item'])==stack(seeded['inventory'][tool_slot]['item'])and stack(ready['cells'][2]['item'])==stack(seeded['inventory'][gem_slot]['item'])
        ready_inventory=list(expected);ready_inventory[tool_slot]=stack('{}');ready_inventory[gem_slot]=stack('{}')
        assert inv(ready)==ready_inventory and inv(outcome)==ready_inventory
        assert stack(outcome['cells'][1]['item'])==paid_tool(ready['cells'][1]['item'],ready['tool'])
        assert without_gems(outcome['cells'][0]['item'])==without_gems(ready['cells'][0]['item'])and len(outcome['gems'])in [0,1]
        names += [f'attempt-{n}-actual-native-prerequisites-enable-action',f'attempt-{n}-charges-the-exact-native-tool-cost',f'attempt-{n}-preserves-equipment-and-non-gem-data']
        if outcome['gems']:
            assert n==attempts[-1]and stack(outcome['gems'][0])==stack(ready['cells'][2]['item'])and outcome['cells'][2]['linked']
            linked=outcome['cells'][0]['item'];chosen=outcome['gems'][0];names+=['successful-native-link-owns-exactly-the-original-gem']
        else:
            assert not item(outcome['cells'][2]['item'])and not outcome['cells'][2]['linked'];names+=['failed-native-roll-consumes-the-gem-without-projecting-a-duplicate']
        expected[tool_slot]=stack(outcome['cells'][1]['item']);expected[gem_slot]=stack('{}');assert inv(settled)==expected
        names+=[f'attempt-{n}-settles-all-other-player-items-and-cursors-exactly']
    assert linked is not None;expected[9]=stack(linked)
    for label in ['linked-recovered','link-reopened']:
        s=states[label];assert inv(s)==expected and all(not item(v['item'])for v in s['cells'])
    assert inv(states['grinder-initial'])==expected
    seeded=states['grinder-tool'];expected=inv(seeded)
    preview=states['grinder-preview'];assert without_gems(preview['cells'][2]['item'])==without_gems(linked)and len(preview['pendingReturnedGems'])==1 and stack(preview['pendingReturnedGems'][0])==stack(chosen)
    cancelled=states['grinder-cancelled'];assert inv(cancelled)==expected and all(not item(v['item'])for v in cancelled['cells'])
    ready,picked,settled=[states[k]for k in ['grinder-ready','grinder-output-picked','grinder-settled']]
    assert stack(picked['inventory'][9]['item'])==stack(ready['cells'][2]['item'])and stack(picked['cells'][1]['item'])==paid_tool(ready['cells'][1]['item'],ready['tool'])and not item(picked['cells'][0]['item'])
    expected[9]=stack(ready['cells'][2]['item']);expected[16]=stack(picked['cells'][1]['item'])
    legacy=target=='1.20.1/fabric'
    if legacy:
        limits=journal['native_upstream_limits'];assert len(limits)==1 and limits[0]['publisher_sha256']==LEGACY_SHA and limits[0]['native_control']==CONTROL
        assert inv(settled)==expected and not settled['pendingReturnedGems']and not any(stack(v)==stack(chosen)for v in settled['worldDrops'])
        last='legacy-grinding-matches-proven-native-nonreturn-with-exact-other-ownership'
    else:
        assert journal['native_upstream_limits']==[]
        bag=Counter(expected);bag[stack('{}')]-=1;bag[stack(chosen)]+=1;assert Counter(inv(settled))==bag
        last='native-grinding-returns-the-exact-guaranteed-gem-once'
    assert not any(stack(v)==stack(chosen)for v in settled['worldDrops']), 'Extracted gem duplicated as a world drop'
    assert all(not item(v['item'])for v in settled['cells'])
    names+=['at-least-one-real-link-completes-within-three-distinct-paid-attempts','linked-equipment-recovers-once-with-no-physical-projection-duplicate','linked-full-item-persists-through-reopening','original-grinding-tab-keeps-three-native-cells','native-strength-one-extraction-previews-full-equipment-and-original-gem','cancelled-grinding-keeps-all-original-ingredients-without-output','grinding-takes-the-exact-preview-and-charges-the-native-tool',last]
    assert [v['name']for v in journal['checks']]==names and all(v['passed']is True for v in journal['checks'])
    return dict(assertions=len(names),states=len(states),paid_native_attempts=len(attempts),legacy_native_gem_nonreturn_limit=legacy)

def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target'];assert(target,case)in PAIRS
    menu,screen=classes(case);assert original_classes==[screen];verify_sealed(root,sealed,expected)
    qa={k:v for k,v in sealed['archived_inputs'].items()if k.startswith('build/release-readiness/')};assert qa==QA_VARIANTS[target]
    driver_sha=sealed['archived_inputs'][DRIVER]
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result.get('smitherz_features_checked')is True and result['cases']==2 and result['failed']==0
    journal_path=checked(root,sealed['acceptance']);profile=(root/row['profile']).resolve();assert journal_path==profile/'smitherz-release/summary.json'
    journal=json.loads(journal_path.read_text());states={};refs=[]
    for n,path in enumerate(sorted(journal_path.parent.glob('[0-9][0-9][0-9]-*.json')),1):
        assert path.name.startswith(f'{n:03}-');ref=reference(root,path);assert ref in sealed['full_states'];refs.append(ref);states[path.name[4:-5]]=json.loads(path.read_text())
    validation=validate(target,journal,states,driver_sha);assert result['checks']==validation['assertions']
    publishers=[]
    for a in json.loads(checked(root,sealed['staged']).read_text())['artifacts']:
        if a['family']!='upstream'or not a['filename'].startswith('smitherz-'):continue
        p=Path(a['file']);p=p if p.is_absolute()else root/p;raw=p.read_bytes();assert hashlib.sha256(raw).hexdigest()==a['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in a:assert digest==a['sha512']
        locations=matches(index(p,digest,target.split('/')[1]),[menu,screen]);assert locations
        publishers.append(dict(filename=a['filename'],sha256=a['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1
    if target=='1.20.1/fabric':
        native=json.loads(checked(root,CONTROL).read_text());assert native['status']=='native-legacy-gem-nonreturn-confirmed-upstream'and len(native['checks'])==11 and all(v['passed']is True for v in native['checks'])and publishers[0]['sha256']==LEGACY_SHA
        for ref in native['states']+native['inputs']:checked(root,ref)
        staged=json.loads(checked(root,native['staged']).read_text());assert staged['siu_loaded']is False and not any(v['family']in ['core','compatibility','controlify']for v in staged['artifacts'])
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',original_native_classes=original_classes,native_classes=[menu,screen],subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,journal_path),native_states=refs,validation=validation,publishers=publishers,native_upstream_limits=journal['native_upstream_limits']if case=='smitherz-grinder'else[],scope='Original native linking/grinding through actual moved SIU controls, distinct paid random attempts, exact socket/tool/gem ownership, cancellation and full recovery; target-specific native result behavior.',limits='Two native target executions shared across four UI aliases, not four gameplay runs. Original1.0.4guaranteed-gem-return assertion remains an explicitly proven upstream exception;1.2.0must pass strict exact return. No modified RNG, all-gem distributions or full-mod gameplay claim.')

def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    assert derive(root,checked(root,record['sealed_evidence']),expected,record['case'],record['original_native_classes'])==record
    return True
