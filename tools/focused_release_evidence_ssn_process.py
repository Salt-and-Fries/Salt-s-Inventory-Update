"""Re-derive SSN process configuration; preserve exact original display limits."""
from pathlib import Path
import copy,hashlib,json
import nbtlib
from focused_release_evidence import checked,reference,verify as verify_sealed
from focused_release_evidence_reforge import stack,item,inv,empty_cursors
from native_publisher_index import index,matches

SCHEMA='native-SSN-process-configuration-and-pinned-native-redstone-display-v1'
CASE='registry-storagenetwork-process_kabel'
TARGETS={'1.21.1/neoforge','26.1.2/neoforge'}
PAIRS={(t,CASE)for t in TARGETS}
MENU='com.lothrazar.storagenetwork.block.cable.processing.ContainerCableProcess'
SCREEN='com.lothrazar.storagenetwork.block.cable.processing.ScreenCableProcess'
QA_VARIANTS={'1.21.1/neoforge': {'build/release-readiness/run_ssn_process_native_redstone_706.py': '464e55831aba412342a81a42f22d6d8e7db0c0256bb148796e841c02003990bd', 'build/release-readiness/raw_source_removal_573.py': 'efe7c5aa7f7512847dbb97ea6c8c5f4d6e2dbce1004b9b66c2fd71f0fbe4be39', 'build/release-readiness/ssn_process_native_contract_features_703.py': 'baab42fa79ad738f0c3363019456b18bc27e3b01cd60d474475e046c48fa9a1b', 'build/release-readiness/ssn_process_native_contract_observer_703.py': '7f38cfc7fc88c7039d2dcfac5727440ef532902d09170339182115f7192a683a', 'build/release-readiness/ssn_named_region_observer_592.py': '71703a3dd8fdb972189a92dc2182bb98d8dfec4fc1567bc00202fefeb6581647', 'build/release-readiness/ssn_process_native_redstone_features_706.py': '222dc795b0187868f3a703001ca68ff959a5557c2f5d460a4ed5bc689d3f1a93', 'build/release-readiness/readiness-ssn-process-1211-neoforge-03/toms-observer/toms.init.gradle': '6cf97b788998bf30058358e4cddca1d1f56f2ad974889da2b14dd2493d350c6f', 'build/release-readiness/readiness-ssn-process-1211-neoforge-03/toms-observer/java/com/salts_inventory_update/functionaltest/EverythingAuditQa.java': 'e37364bcb25f6d0af23c6e356967e7fe438acf38696ac73feed1b572190fd446', 'build/release-readiness/readiness-ssn-process-1211-neoforge-03/toms-observer/generated-metadata/resources/pack.mcmeta': 'f6d3f88cbc12aa4513806560d80852c42f60081133149515dadd88773d64514e', 'build/release-readiness/readiness-ssn-process-1211-neoforge-03/toms-observer/generated-metadata/resources/META-INF/neoforge.mods.toml': 'b934b45a453d81742f7c5f781801ebdc6e5f9fe9055c3fa5fa4194f7cfeab6c8', 'build/release-readiness/readiness-ssn-process-1211-neoforge-03/toms-observer/generated-metadata/java/com/salts_inventory_update/functionaltest/ChestCloseQaMod.java': '8786a886e1862c900902e1c73111c3842cf3cd4ffc1443032c6b292c58f31d42', 'build/release-readiness/readiness-ssn-process-1211-neoforge-03/toms-observer/artifact/chest-close-qa-1.21.1-neoforge.jar': '3ce952b9a8bc8814f3e8cabcdc5d6924040f90afebe23bc31afaee17fec9f72c'}, '26.1.2/neoforge': {'build/release-readiness/run_ssn_process_native_redstone_706.py': '464e55831aba412342a81a42f22d6d8e7db0c0256bb148796e841c02003990bd', 'build/release-readiness/raw_source_removal_573.py': 'efe7c5aa7f7512847dbb97ea6c8c5f4d6e2dbce1004b9b66c2fd71f0fbe4be39', 'build/release-readiness/ssn_process_native_contract_features_703.py': 'baab42fa79ad738f0c3363019456b18bc27e3b01cd60d474475e046c48fa9a1b', 'build/release-readiness/ssn_process_native_contract_observer_703.py': '7f38cfc7fc88c7039d2dcfac5727440ef532902d09170339182115f7192a683a', 'build/release-readiness/ssn_named_region_observer_592.py': '71703a3dd8fdb972189a92dc2182bb98d8dfec4fc1567bc00202fefeb6581647', 'build/release-readiness/ssn_process_native_redstone_features_706.py': '222dc795b0187868f3a703001ca68ff959a5557c2f5d460a4ed5bc689d3f1a93', 'build/release-readiness/readiness-ssn-process-2612-neoforge-01/toms-observer/toms.init.gradle': '6ea1edd63c36f563516f8705b498dc147aa639e25181546578d1051c5666b11c', 'build/release-readiness/readiness-ssn-process-2612-neoforge-01/toms-observer/java/com/salts_inventory_update/functionaltest/EverythingAuditQa.java': 'e7bc2f83d35e16f1b0c83f79b55989efa19d3a95be0f3e83f40f5b1e64b571a0', 'build/release-readiness/readiness-ssn-process-2612-neoforge-01/toms-observer/generated-metadata/resources/pack.mcmeta': '3e98634cc49357f5e77c5d5c0b4c474473c38a1e87a41beaa3b3c12e15c4c42c', 'build/release-readiness/readiness-ssn-process-2612-neoforge-01/toms-observer/generated-metadata/resources/META-INF/neoforge.mods.toml': 'b934b45a453d81742f7c5f781801ebdc6e5f9fe9055c3fa5fa4194f7cfeab6c8', 'build/release-readiness/readiness-ssn-process-2612-neoforge-01/toms-observer/generated-metadata/java/com/salts_inventory_update/functionaltest/ChestCloseQaMod.java': '8786a886e1862c900902e1c73111c3842cf3cd4ffc1443032c6b292c58f31d42', 'build/release-readiness/readiness-ssn-process-2612-neoforge-01/toms-observer/artifact/chest-close-qa-26.1.2-neoforge.jar': '5351a446f789ef4ace69c5e9d069fa81c967b388224b8d45ffd52889ae0e8e10'}}
CONTRACTS={'1.21.1/neoforge': {'lifetimes': [{'native_control': {'file': 'output/release-readiness/ssn-native-process-702-1211-01/native-control.json', 'sha256': '9095765e4b1144bf7f5d5abf79a3fe422b97cb4f85eef3189250ead707fdc275'}, 'publisher_sha256': '1b5d1fa174406d1813882d25c1f523b2eb1b52805173427ae96ebdf4be81beac', 'limit': 'This exact original process menu remains valid/open after source removal, then closes normally with every tagged item retained. Automatic source-removal closure is not claimed.'}], 'redstone': [{'native_control': {'file': 'output/release-readiness/ssn-native-process-redstone-705-1211-01/native-control.json', 'sha256': 'bfccbb649d8aadf591ec8a613bc401546287520c8c89614f9ac126c7f1001009'}, 'publisher_sha256': '1b5d1fa174406d1813882d25c1f523b2eb1b52805173427ae96ebdf4be81beac', 'native_client_after_reopen': False, 'limit': 'Native redstone click changes server while the original cached client capability/icon remains false. Exact observed client state after ordinary reopen is pinned separately. No immediate native redstone icon synchronization is claimed.'}]}, '26.1.2/neoforge': {'lifetimes': [{'native_control': {'file': 'output/release-readiness/ssn-native-process-702-2612-01/native-control.json', 'sha256': '6e494249fb243a1ce8bb04fb0b34c9c653cb8e6904052e28d1589fdc01ef4a10'}, 'publisher_sha256': 'e2cc496e00f68a8f9633f7661be81813cb3468534df8a9ec232d23864f4373f2', 'limit': 'This exact original process menu remains valid/open after source removal, then closes normally with every tagged item retained. Automatic source-removal closure is not claimed.'}], 'redstone': [{'native_control': {'file': 'output/release-readiness/ssn-native-process-redstone-705-2612-01/native-control.json', 'sha256': '939db36461aaedce59faa36e5b141fa6eedf1d1d5173388be57d7684d431946c'}, 'publisher_sha256': 'e2cc496e00f68a8f9633f7661be81813cb3468534df8a9ec232d23864f4373f2', 'native_client_after_reopen': False, 'limit': 'Native redstone click changes server while the original cached client capability/icon remains false. Exact observed client state after ordinary reopen is pinned separately. No immediate native redstone icon synchronization is claimed.'}]}}
FACES=['DOWN','UP','NORTH','SOUTH','WEST','EAST']
LABELS=['initial','seeded','getFilters-copied','getFilters-incremented','getFiltersOut-copied','getFiltersOut-incremented','ghost-recipe-configured','moved','btnPlus','btnMinus','btnAlways','btnRedstone','btnInputFace','btnOutputFace','reopened','cleared']

def validate(target,journal,states,driver_sha):
    assert target in TARGETS and journal['complete']is True and journal['cases']==['process_kabel']
    assert journal['driver_sha256']==driver_sha
    contract=CONTRACTS[target]
    assert journal['native_source_lifetime_controls']==contract['lifetimes']and journal['native_redstone_controls']==contract['redstone']
    assert set(states)==set(LABELS+['native-source-removed','ordinary-closed'])
    initial,seeded=[states[k]for k in LABELS[:2]]
    settings=copy.deepcopy(initial['settings'])
    assert set(settings)=={'getFilters','getFiltersOut','needsRedstone','count','always','inputFace','outputFace','status'}
    assert settings==dict(getFilters=['{}']*9,getFiltersOut=['{}']*9,needsRedstone=False,count=0,always=False,inputFace='UP',outputFace='DOWN',status='EXPORTING')
    assert all(not item(v['item'])for v in initial['inventory'])
    expected=inv(initial);originals={}
    for slot,id,marker in [(9,'minecraft:iron_ingot',51),(10,'minecraft:gold_ingot',52)]:
        text=seeded['inventory'][slot]['item'];v=item(text);assert str(v['id'])==id and type(v['count'])is nbtlib.Int and int(v['count'])==16
        data=v['components']['minecraft:custom_data'];assert type(data['release_ssn_process_marker'])is nbtlib.Int and int(data['release_ssn_process_marker'])==marker
        assert type(data['typed'])is nbtlib.ByteArray and list(data['typed'])==[2,9]
        expected[slot]=stack(text);originals[slot]=text
    assert inv(seeded)==expected and seeded['settings']==settings
    empty_cursors(initial);empty_cursors(seeded)
    for slot,key in [(9,'getFilters'),(10,'getFiltersOut')]:
        held=list(expected);held[slot]=stack('{}')
        for count,label in [(1,key+'-copied'),(2,key+'-incremented')]:
            state=states[label];want=copy.deepcopy(settings);want[key][0]=state['settings'][key][0]
            assert stack(want[key][0])==stack(originals[slot],count)and state['settings']==want
            assert inv(state)==held
            cursors=[stack(v['item'])for v in state['cursors']if item(v['item'])]
            assert cursors and all(v==stack(originals[slot])for v in cursors)
            settings=want
    configured,moved=states['ghost-recipe-configured'],states['moved']
    for state in [configured,moved]:assert state['settings']==settings and inv(state)==expected;empty_cursors(state)
    a,b=configured['client']['bounds'],moved['client']['bounds']
    assert(b['x'],b['y'])==(a['x']+16,a['y']+12)and(b['width'],b['height'])==(a['width'],a['height'])
    names=['original-process-menu-opens-with-synchronized-native-state','getFilters-copies-one-exact-native-ghost','getFilters-right-click-changes-only-count','getFiltersOut-copies-one-exact-native-ghost','getFiltersOut-right-click-changes-only-count','both-ghost-recipes-preserve-all-physical-items','process-window-moves-before-configuration-input']
    for button,key,value in [('btnPlus','count',1),('btnMinus','count',0),('btnAlways','always',True),('btnRedstone','needsRedstone',True),('btnInputFace','inputFace','NORTH'),('btnOutputFace','outputFace','UP')]:
        state=states[button];settings=copy.deepcopy(settings);settings[key]=value
        assert state['settings']==settings and inv(state)==expected;empty_cursors(state)
        assert moved['client']['buttons'][button]['active']is True and moved['client']['buttons'][button]['visible']is True
        names.append('btnRedstone-matches-proven-native-server-toggle-and-stale-client'if button=='btnRedstone'else button+'-changes-exact-native-setting-only')
    reopened,cleared=states['reopened'],states['cleared']
    assert reopened['settings']==settings and inv(reopened)==expected;empty_cursors(reopened)
    settings=copy.deepcopy(settings);settings['getFilters']=['{}']*9;settings['getFiltersOut']=['{}']*9
    assert cleared['settings']==settings and inv(cleared)==expected;empty_cursors(cleared)
    for label in LABELS:
        state=states[label];client=state['client'];assert client['menu']==MENU and client['screen']==SCREEN
        assert type(client['session'])is int and client['session']>0 and type(client['locked'])is bool
        want=copy.deepcopy(state['settings']);want['needsRedstone']=contract['redstone'][0]['native_client_after_reopen']if label in ['reopened','cleared']else False
        assert client['settings']==want
        bounds=client['bounds'];assert set(bounds)=={'x','y','width','height'}and all(type(v)is int for v in bounds.values())and min(bounds['width'],bounds['height'])>0
    assert len({states[label]['client']['session']for label in LABELS[:-2]})==1
    assert reopened['client']['session']!=initial['client']['session']
    life,closed=states['native-source-removed'],states['ordinary-closed']
    assert life['sourceAir']is True and life['blockEntityPresent']is False and life['allNativeValid']is True and life['nativeMenus']==[MENU]
    assert closed['sourceAir']is True and closed['blockEntityPresent']is False and closed['nativeMenus']==[]
    for state in [life,closed]:assert inv(state)==expected;empty_cursors(state)
    names+=['all-process-configuration-persists-on-reopening','empty-hand-clears-both-ghosts-without-changing-items','process-source-removal-preserves-proven-original-native-lifetime','ordinary-close-releases-retained-process-with-exact-items']
    assert journal['checks']==[dict(case='process_kabel',name=n,passed=True)for n in names]and len(names)==17
    return dict(assertions=17,numeric_states=16,lifetime_states=2,native_redstone_display_limit=True)

def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target'];assert(target,case)in PAIRS and original_classes==[MENU]
    verify_sealed(root,sealed,expected);qa={k:v for k,v in sealed['archived_inputs'].items()if k.startswith('build/release-readiness/')};assert qa==QA_VARIANTS[target]
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results'];assert result['ssn_process_features_checked']is True and result['cases']==1 and result['failed']==0 and result['checks']==17
    journal_path=checked(root,sealed['acceptance']);assert journal_path==(root/row['profile']).resolve()/'ssn-process-release/summary.json'
    journal=json.loads(journal_path.read_text());states={};refs=[];files=sorted(journal_path.parent.glob('[0-9][0-9][0-9]-*.json'));assert len(files)==16
    for n,path in enumerate(files,1):
        assert path.name==f'{n:03}-'+LABELS[n-1]+'.json';ref=reference(root,path);assert ref in sealed['full_states'];refs.append(ref);states[LABELS[n-1]]=json.loads(path.read_text())
    for label in ['native-source-removed','ordinary-closed']:
        path=journal_path.parent/(label+'.json');ref=reference(root,path);assert ref in sealed['full_states'];refs.append(ref);states[label]=json.loads(path.read_text())
    driver='build/release-readiness/ssn_process_native_redstone_features_706.py';validation=validate(target,journal,states,sealed['archived_inputs'][driver])
    publishers=[]
    for artifact in json.loads(checked(root,sealed['staged']).read_text())['artifacts']:
        if artifact['family']!='upstream'or not artifact['filename'].lower().startswith(('simplestoragenetwork-','storagenetwork-')):continue
        p=Path(artifact['file']);p=p if p.is_absolute()else root/p;raw=p.read_bytes();assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(p,digest,target.split('/')[1]),[MENU,SCREEN]);assert locations
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1
    for kind in ['lifetimes','redstone']:
        for pin in CONTRACTS[target][kind]:
            assert pin['publisher_sha256']==publishers[0]['sha256'];control=json.loads(checked(root,pin['native_control']).read_text());assert all(v['passed']is True for v in control['checks'])
            if kind=='lifetimes':assert control['status']=='native-process-source-persistence-contract-confirmed'and len(control['checks'])==8
            else:assert control['status']=='native-process-redstone-sync-behavior-measured'and len(control['checks'])==11 and control['native_redstone_sync_after_reopen']is pin['native_client_after_reopen']
            staged=json.loads(checked(root,control['staged']).read_text());assert staged['siu_loaded']is False and not any('salts_inventory'in v['filename'].lower()for v in staged['artifacts'])
            for ref in control['inputs']+[v.get('evidence',v)for v in control['states']]:checked(root,ref)
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',original_native_classes=original_classes,native_classes=[MENU,SCREEN],subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,journal_path),native_states=refs,validation=validation,publishers=publishers,native_upstream_limits=CONTRACTS[target],scope='Original native input/output ghost recipes, count/always/redstone/face controls, exact typed item and shared-cursor ownership, moved input, reopening, clear and ordinary close.',limits='Original stale redstone client/icon state and source-removal persistence are separately reproduced with SIU absent on each exact publisher pin. No formed processing network, reset-during-production or adjacent-machine filter import acceptance.')

def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    assert derive(root,checked(root,record['sealed_evidence']),expected,record['case'],record['original_native_classes'])==record
    return True
