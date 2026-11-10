"""Re-derive native randomized merchant offers, paid output and exact physical ownership."""
from pathlib import Path
import hashlib,json
import nbtlib
from focused_release_evidence import checked,reference,verify as verify_sealed
from sensor_feature_evidence import typed
from native_publisher_index import index,matches

SCHEMA='native-red-merchant-four-row-paid-trade-and-owner-lifetime-v1'
CASE='own-supp-red-merchant'
MENU='net.mehvahdjukaar.supplementaries.common.inventories.RedMerchantMenu'
SCREEN='net.mehvahdjukaar.supplementaries.client.screens.RedMerchantScreen'
TARGETS={'1.20.1/fabric','1.20.1/forge','1.21.1/fabric','1.21.1/neoforge'}
PAIRS={(t,CASE)for t in TARGETS}
LABELS=['initial','scrolled-down','scrolled-top','selected','moved','foreign-fixture','foreign-payment','foreign-recovered','funded','ready','traded','settled','reopened','entity-removed']
DRIVER='functional-tests/release-specialized/red_merchant_features.py'
QA_VARIANTS={'1.20.1/fabric': {'build/release-readiness/run_red_merchant_static_734.py': '83c83315f985fcde5eb4791702c5e46c60013ecc8b1089dcc566fbbe4b1c8056', 'build/release-readiness/red_merchant_static_observer_732.py': 'a6fafdbdc596d8b066f527c7f0b68452f466920d23bb682e6a380c42da96b37b', 'build/release-readiness/prepare_red_merchant_static_raw_732.py': 'a60a90c1fa0a66497863cdd3b714d84d2959c6b263afcab38ea2425b07f82ab9', 'build/release-readiness/raw_static_fields_636.py': 'fe90803ab243dd0e095c6167e13433c0bdca3262ce66fc8ba49bd915da21c149', 'build/release-readiness/named_air_fixture_566.py': '0f799ff55e3a2a0da275ba5304800798dcee755ac37077a4e4704607f11c0430'}, '1.20.1/forge': {'build/release-readiness/run_red_merchant_static_734.py': '83c83315f985fcde5eb4791702c5e46c60013ecc8b1089dcc566fbbe4b1c8056', 'build/release-readiness/red_merchant_static_observer_732.py': 'a6fafdbdc596d8b066f527c7f0b68452f466920d23bb682e6a380c42da96b37b', 'build/release-readiness/prepare_red_merchant_static_raw_732.py': 'a60a90c1fa0a66497863cdd3b714d84d2959c6b263afcab38ea2425b07f82ab9', 'build/release-readiness/raw_static_fields_636.py': 'fe90803ab243dd0e095c6167e13433c0bdca3262ce66fc8ba49bd915da21c149', 'build/release-readiness/named_air_fixture_566.py': '0f799ff55e3a2a0da275ba5304800798dcee755ac37077a4e4704607f11c0430'}, '1.21.1/fabric': {'build/release-readiness/run_red_merchant_static_734.py': '83c83315f985fcde5eb4791702c5e46c60013ecc8b1089dcc566fbbe4b1c8056', 'build/release-readiness/red_merchant_static_observer_732.py': 'a6fafdbdc596d8b066f527c7f0b68452f466920d23bb682e6a380c42da96b37b', 'build/release-readiness/prepare_red_merchant_static_raw_732.py': 'a60a90c1fa0a66497863cdd3b714d84d2959c6b263afcab38ea2425b07f82ab9', 'build/release-readiness/raw_static_fields_636.py': 'fe90803ab243dd0e095c6167e13433c0bdca3262ce66fc8ba49bd915da21c149', 'build/release-readiness/named_air_fixture_566.py': '0f799ff55e3a2a0da275ba5304800798dcee755ac37077a4e4704607f11c0430'}, '1.21.1/neoforge': {'build/release-readiness/run_red_merchant_grounded_754.py': '78b61c28320bdf57f4198c7adae31d1eba1272c87a4b5ca438d1e06d609d0a74', 'build/release-readiness/red_merchant_static_observer_732.py': 'a6fafdbdc596d8b066f527c7f0b68452f466920d23bb682e6a380c42da96b37b', 'build/release-readiness/prepare_red_merchant_static_raw_732.py': 'a60a90c1fa0a66497863cdd3b714d84d2959c6b263afcab38ea2425b07f82ab9', 'build/release-readiness/raw_static_fields_636.py': 'fe90803ab243dd0e095c6167e13433c0bdca3262ce66fc8ba49bd915da21c149', 'build/release-readiness/named_air_fixture_566.py': '0f799ff55e3a2a0da275ba5304800798dcee755ac37077a4e4704607f11c0430', 'build/release-readiness/red_merchant_static_observer_741.py': '697ca5c8175cc9a31da330b556422955482247e7eeb960b38f8f2e310a480e2b', 'build/release-readiness/red_merchant_grounded_features_753.py': 'e679b1a749222e4bdbe56b00da91f0d2e5730a3d33c3c1ca551e332a7cb523d4', 'build/release-readiness/readiness-red-merchant-1211-neoforge-03/toms-observer/toms.init.gradle': 'f36a3637781f143e6dd8cd97410828b67fc9bbc312e78fb2d407dd34b6811484', 'build/release-readiness/readiness-red-merchant-1211-neoforge-03/toms-observer/java/com/salts_inventory_update/functionaltest/EverythingAuditQa.java': '7fda4a86a5601b1646907f636d7cafb1a2193fab7c1f4685bca501533d91978d', 'build/release-readiness/readiness-red-merchant-1211-neoforge-03/toms-observer/generated-metadata/resources/pack.mcmeta': 'f6d3f88cbc12aa4513806560d80852c42f60081133149515dadd88773d64514e', 'build/release-readiness/readiness-red-merchant-1211-neoforge-03/toms-observer/generated-metadata/resources/META-INF/neoforge.mods.toml': 'b934b45a453d81742f7c5f781801ebdc6e5f9fe9055c3fa5fa4194f7cfeab6c8', 'build/release-readiness/readiness-red-merchant-1211-neoforge-03/toms-observer/generated-metadata/java/com/salts_inventory_update/functionaltest/ChestCloseQaMod.java': '8786a886e1862c900902e1c73111c3842cf3cd4ffc1443032c6b292c58f31d42', 'build/release-readiness/readiness-red-merchant-1211-neoforge-03/toms-observer/artifact/chest-close-qa-1.21.1-neoforge.jar': 'e8841812df98e7851a6572797e287db37ce1088d5135ecedc21af33cb20cb370'}}

def item(text):
    v=nbtlib.parse_nbt(text);assert isinstance(v,nbtlib.Compound);return v
def stack(text,count=None):
    v=item(text)
    if count is not None:
        key='Count'if'Count'in v else'count';v[key]=type(v[key])(count)
    return typed(v)
def inv(s):
    rows=s['inventory'];assert [v['index']for v in rows]==list(range(len(rows)))and len(rows)>=36
    return [stack(v['item'])for v in rows]
def empty_cursors(s):assert s['cursors']and all(not item(v['item'])for v in s['cursors'])
def plain_emerald(text):
    v=item(text)
    if not v:return False
    key='Count'if'Count'in v else'count'
    for k in ['tag','components','ForgeCaps']:
        if k in v and isinstance(v[k],nbtlib.Compound)and not v[k]:del v[k]
    return set(v)=={'id',key}and str(v['id'])=='minecraft:emerald'and 0<int(v[key])<=61
def offers(rows):
    assert [v['index']for v in rows]==list(range(len(rows)))and len(rows)>4
    result=[]
    for v in rows:
        assert set(v)=={'index','costA','costB','result','uses','maximum','outOfStock'}
        assert type(v['uses'])is int and type(v['maximum'])is int and type(v['outOfStock'])is bool and 0<=v['uses']<=v['maximum']
        result.append({k:stack(x)if k in ['costA','costB','result']else x for k,x in v.items()})
    return result

def validate(target,journal,states,driver_sha):
    assert target in TARGETS and set(states)==set(LABELS)
    assert journal['complete']is True and journal['cases']==['supplementaries-red-merchant']and journal['driver_sha256']==driver_sha
    initial=states['initial'];base=inv(initial);assert all(not item(v['item'])for v in initial['inventory'])and all(not item(v)for v in initial['cells'])
    native_offers=offers(initial['offers']);owner=initial['owner'];assert owner and initial['client']['scroll']==0
    selected=states['selected'];payable=[v for v in initial['offers']if not v['outOfStock']and plain_emerald(v['costA'])and not item(v['costB'])and item(v['result'])];assert payable
    offer=payable[0];chosen=offer['index'];assert selected['client']['selection']==chosen and all(not item(v)for v in selected['cells'])
    price=item(offer['costA']);cost=int(price['Count'if'Count'in price else'count']);assert 0<cost<=61
    assert states['scrolled-down']['client']['scroll']==1 and states['scrolled-top']['client']['scroll']==0
    session=initial['client']['session'];assert type(session)is int and session>0
    before,moved=selected['client']['bounds'],states['moved']['client']['bounds'];assert(moved['x'],moved['y'],moved['width'],moved['height'])==(before['x']+16,before['y']+12,before['width'],before['height'])
    foreign=states['foreign-fixture'];value=item(foreign['inventory'][10]['item']);key='Count'if'Count'in value else'count';assert str(value['id'])=='minecraft:diamond'and int(value[key])==3
    data=value['tag']if key=='Count'else value['components']['minecraft:custom_data'];assert type(data['release_red_merchant_marker'])is nbtlib.Int and int(data['release_red_merchant_marker'])==71 and type(data['typed'])is nbtlib.ByteArray and list(data['typed'])==[1,5]
    expected=list(base);expected[10]=stack(foreign['inventory'][10]['item']);assert inv(foreign)==expected
    refused=states['foreign-payment'];held=list(expected);held[10]=stack('{}');assert inv(refused)==held and not item(refused['cells'][1])and not item(refused['cells'][2])
    if foreign['foreignAccepted']:
        assert stack(refused['cells'][0])==expected[10];empty_cursors(refused);foreign_name='native-invalid-payment-is-not-a-trade'
    else:
        assert not item(refused['cells'][0]);carried=[stack(v['item'])for v in refused['cursors']if item(v['item'])];assert carried and all(v==expected[10]for v in carried);foreign_name='native-payment-slot-refusal-keeps-full-cursor'
    recovered=states['foreign-recovered'];assert inv(recovered)==expected and all(not item(v)for v in recovered['cells'])
    funded=states['funded'];expected[9]=stack(offer['costA'],cost+3);assert inv(funded)==expected
    ready=states['ready'];held=list(expected);held[9]=stack('{}');assert inv(ready)==held and stack(ready['cells'][0])==stack(offer['costA'],cost+3)and not item(ready['cells'][1])and stack(ready['cells'][2])==stack(offer['result'])
    traded=states['traded'];held[11]=stack(offer['result']);assert inv(traded)==held and stack(traded['cells'][0])==stack(offer['costA'],3)and not item(traded['cells'][1])
    assert not item(traded['cells'][2])or stack(traded['cells'][2])==stack(offer['result'])
    expected[9]=stack(offer['costA'],3);expected[11]=stack(offer['result'])
    for label in ['settled','reopened','entity-removed']:assert inv(states[label])==expected
    assert states['reopened']['client']['session']!=session and states['entity-removed']['menus']==0
    changed=[dict(v)for v in native_offers];changed[chosen]['uses']+=1;changed[chosen]['outOfStock']=changed[chosen]['uses']>=changed[chosen]['maximum']
    for label in LABELS[:-1]:
        s=states[label];c=s['client'];assert s['menus']==1 and s['owner']==owner and c['menu']==MENU and type(c['locked'])is bool
        expected_offers=changed if label in ['traded','settled','reopened']else native_offers
        assert offers(s['offers'])==offers(c['offers'])==expected_offers
        assert len(s['cells'])==3 and type(s['foreignAccepted'])is bool
        if label!='reopened':assert c['session']==session
        b=c['bounds'];assert set(b)=={'x','y','width','height'}and all(type(v)is int for v in b.values())and min(b['width'],b['height'])>0
        if label!='foreign-payment':empty_cursors(s)
        if label in LABELS[:5]:assert inv(s)==base and all(not item(v)for v in s['cells'])
        if label in ['foreign-fixture','foreign-recovered','funded','settled','reopened']:assert all(not item(v)for v in s['cells'])
    empty_cursors(states['entity-removed'])
    names=['native-red-merchant-enters-builtin-bridge-with-original-offers','native-offer-list-scrolls-one-entry-without-item-mutation','reverse-scroll-restores-first-four-offers','original-offers-provide-an-unmodified-payable-native-trade','actual-row-click-selects-requested-native-offer-with-empty-output','trading-window-moves-before-slot-interactions',foreign_name,'invalid-payment-recovers-exactly-once','actual-payment-unlocks-exact-native-result','result-pickup-charges-native-cost-and-increments-uses-once','full-inventory-ledger-has-only-paid-result-and-exact-change','same-native-trader-retains-uses-and-settled-items-on-reopening','entity-invalidation-closes-owned-trade-without-item-loss']
    assert journal['checks']==[dict(name=n,passed=True)for n in names]
    return dict(assertions=13,states=14,exact_randomized_native_trade=True,exact_typed_ownership=True)

RED_MERCHANT_3143_QA={'build/release-readiness/named_air_fixture_566.py': '0f799ff55e3a2a0da275ba5304800798dcee755ac37077a4e4704607f11c0430', 'build/release-readiness/prepare_red_merchant_static_raw_732.py': 'a60a90c1fa0a66497863cdd3b714d84d2959c6b263afcab38ea2425b07f82ab9', 'build/release-readiness/raw_static_fields_636.py': 'fe90803ab243dd0e095c6167e13433c0bdca3262ce66fc8ba49bd915da21c149', 'build/release-readiness/red_merchant_grounded_features_753.py': 'e679b1a749222e4bdbe56b00da91f0d2e5730a3d33c3c1ca551e332a7cb523d4', 'build/release-readiness/red_merchant_static_observer_732.py': 'a6fafdbdc596d8b066f527c7f0b68452f466920d23bb682e6a380c42da96b37b', 'build/release-readiness/red_merchant_static_observer_741.py': '697ca5c8175cc9a31da330b556422955482247e7eeb960b38f8f2e310a480e2b', 'build/release-readiness/run_red_merchant_3143_1491.py': '5ae38acc28569ad5f83b2c8984a9f78c77b5aba27417df794437945bcd45dd1e', 'build/release-readiness/run_red_merchant_3143_minimal_1495.py': '33335f5cb5b573ccaf2c4a0bec6dbb7f5b5e9a6c350e56cdd2212ef4bffdf903', 'build/release-readiness/run_red_merchant_grounded_754.py': '78b61c28320bdf57f4198c7adae31d1eba1272c87a4b5ca438d1e06d609d0a74', 'build/release-readiness/supplementaries-3143-minimal-fixture-1495.json': '864f1276ac88ec4c72afa38a6d7e62fb51435dccbae550c3730166b045f7201c'}

def verify_qa_inputs(target,inputs):
    if 'build/release-readiness/run_red_merchant_3143_minimal_1495.py' in inputs:
        assert target=='1.20.1/fabric' and inputs==RED_MERCHANT_3143_QA
    else:
        assert inputs==QA_VARIANTS[target]
    return True


def combine_records(workflows,consumer=None,current=None):
    from focused_release_evidence_ae2_variants import combine
    assert all('disposition'not in v for v in workflows)
    result=combine([dict(v,disposition='functional-pass')for v in workflows],consumer=consumer,current=current)
    result.pop('disposition')
    for row in result.get('additional_workflows',[]):row.pop('disposition')
    return result


def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target'];assert(target,case)in PAIRS and original_classes==[SCREEN]
    verify_sealed(root,sealed,expected);qa={k:v for k,v in sealed['archived_inputs'].items()if k.startswith('build/release-readiness/')};verify_qa_inputs(target,qa)
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];r=row['results'];assert summary['status']=='passed'and row['exit_code']==0 and r['red_merchant_features_checked']is True and r['checks']==13 and r['cases']==1 and r['failed']==0
    journal_path=checked(root,sealed['acceptance']);assert journal_path==(root/row['profile']).resolve()/'red-merchant-release/summary.json';journal=json.loads(journal_path.read_text());states={};refs=[];files=sorted(journal_path.parent.glob('[0-9][0-9][0-9]-*.json'));assert len(files)==14
    for i,(label,path)in enumerate(zip(LABELS,files),1):
        assert path.name==f'{i:03}-{label}.json';ref=reference(root,path);assert ref in sealed['full_states'];refs.append(ref);states[label]=json.loads(path.read_text())
    validation=validate(target,journal,states,sealed['archived_inputs'][DRIVER]);publishers=[]
    for artifact in json.loads(checked(root,sealed['staged']).read_text())['artifacts']:
        if artifact['family']!='upstream'or not artifact['filename'].lower().startswith('supplementaries-'):continue
        p=Path(artifact['file']);p=p if p.is_absolute()else root/p;raw=p.read_bytes();assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if'sha512'in artifact:assert digest==artifact['sha512']
        locations=matches(index(p,digest,target.split('/')[1]),[MENU,SCREEN]);assert locations
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1
    return dict(schema=SCHEMA,target=target,case=case,subjects=sealed['subjects'],original_native_classes=original_classes,native_classes=[MENU],sealed_evidence=reference(root,Path(sealed_path)),acceptance=reference(root,journal_path),full_states=refs,publishers=publishers,validation=validation,scope='Native randomized Red Merchant offers through the builtin four-row window: scroll, selection, refusal, exact payment/output/uses, moved input, owner reopening and invalidation.',limits='Four exact publisher targets and one actually paid native offer each. Not every trade, restocking or progression.')
def verify(root,record,expected,consumer=None,current=None):
    from focused_release_evidence_ae2_variants import expected_subjects
    values=[derive(root,checked(root,v['sealed_evidence']),expected_subjects(record,v,expected,consumer,current),record['case'],record['original_native_classes'])for v in[record,*record.get('additional_workflows',[])]]
    assert record==combine_records(values,consumer=consumer,current=current)
    return record
