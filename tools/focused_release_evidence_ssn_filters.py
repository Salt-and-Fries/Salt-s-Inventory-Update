"""Re-derive native SSN filter ownership, settings and explicitly proven limits."""
from pathlib import Path
import hashlib,json
import nbtlib
from focused_release_evidence import checked,reference,verify as verify_sealed
from focused_release_evidence_reforge import stack,item,inv,empty_cursors
from native_publisher_index import index,matches

SCHEMA='native-SSN-ghost-filter-exact-settings-ownership-and-pinned-lifetime-v1'
TARGETS={'1.20.1/forge','1.21.1/neoforge','26.1.2/neoforge'}
PAIRS={(t,'registry-storagenetwork-'+c)for t in TARGETS for c in ['collector','filter_kabel']}
QA_VARIANTS={'1.20.1/forge': {'build/release-readiness/run_ssn_filter_native_client_679.py': '5d5b22907bd8dd17dad98670015004f378c0c0618cec57278c120a69e233daad', 'build/release-readiness/raw_source_removal_573.py': 'efe7c5aa7f7512847dbb97ea6c8c5f4d6e2dbce1004b9b66c2fd71f0fbe4be39', 'build/release-readiness/ssn_filter_native_client_features_679.py': '6bdd520824a339bf50d1bf137e96fc0c04d3dde36312cea6ec63b1afe2c91427', 'build/release-readiness/ssn_filter_native_client_observer_679.py': 'b0d3c881236a0b87e61aa115ed2de261c8b23cdac28abfd09da55a1d94a663db', 'build/release-readiness/prepare_ssn_filter_native_client_679.py': '728d8a99efd7c41940d49ae8f2a982222c8eded448a104d012c123c60c2d1d4d', 'build/release-readiness/ssn_filter_native_lifetime_observer_675.py': '4b8872dfffaa14866230e309eaa1b2b2b9a833183d2e71bb775ba698128b857d'}, '1.21.1/neoforge': {'build/release-readiness/run_ssn_filter_all_ghost_692.py': 'e5cff6e64464abc28d7a562e1946fbda2f323a62ade5234799aa5ff895c8c36e', 'build/release-readiness/raw_source_removal_573.py': 'efe7c5aa7f7512847dbb97ea6c8c5f4d6e2dbce1004b9b66c2fd71f0fbe4be39', 'build/release-readiness/ssn_filter_all_ghost_features_692.py': 'ca5be997405c970d47c47c71282098c5b4122b03221928c740fdc072c7703e91', 'build/release-readiness/ssn_filter_all_ghost_observer_692.py': 'd023b6ea7b6eac13c3c98633c2cff4c41642cce2f7dedaba279829557c75289e', 'build/release-readiness/prepare_ssn_filter_all_ghost_692.py': '5cd2b90dcf2faa691583b7e67be55118e6aa51097fa290ccd486697228e06b36', 'build/release-readiness/ssn_filter_native_lifetime_observer_675.py': '4b8872dfffaa14866230e309eaa1b2b2b9a833183d2e71bb775ba698128b857d', 'build/release-readiness/ssn_filter_native_client_observer_679.py': 'b0d3c881236a0b87e61aa115ed2de261c8b23cdac28abfd09da55a1d94a663db', 'build/release-readiness/ssn_named_region_observer_592.py': '71703a3dd8fdb972189a92dc2182bb98d8dfec4fc1567bc00202fefeb6581647', 'build/release-readiness/ssn_filter_all_native_observer_688.py': '3d14dc92f94797c9415d3e7347757322eb0039f59981b3f03a5c3b970aa18e49', 'build/release-readiness/readiness-ssn-filters-1211-neoforge-03/toms-observer/toms.init.gradle': '037f1c24729a5ed749b32921c022003feea8f17a48d141ed276f83a1de01121d', 'build/release-readiness/readiness-ssn-filters-1211-neoforge-03/toms-observer/java/com/salts_inventory_update/functionaltest/EverythingAuditQa.java': 'c4b43222cb2ecdb2dc7dc716995ce9f8ee7ced6c7bf1dbc1a4cb0ea18f628d55', 'build/release-readiness/readiness-ssn-filters-1211-neoforge-03/toms-observer/generated-metadata/resources/pack.mcmeta': 'f6d3f88cbc12aa4513806560d80852c42f60081133149515dadd88773d64514e', 'build/release-readiness/readiness-ssn-filters-1211-neoforge-03/toms-observer/generated-metadata/resources/META-INF/neoforge.mods.toml': 'b934b45a453d81742f7c5f781801ebdc6e5f9fe9055c3fa5fa4194f7cfeab6c8', 'build/release-readiness/readiness-ssn-filters-1211-neoforge-03/toms-observer/generated-metadata/java/com/salts_inventory_update/functionaltest/ChestCloseQaMod.java': '8786a886e1862c900902e1c73111c3842cf3cd4ffc1443032c6b292c58f31d42', 'build/release-readiness/readiness-ssn-filters-1211-neoforge-03/toms-observer/artifact/chest-close-qa-1.21.1-neoforge.jar': '2981d74840068c394eb8b29a63cd66e024c8b5c2ca9640ed3be0932f7369416a'}, '26.1.2/neoforge': {'build/release-readiness/run_ssn_filter_all_ghost_692.py': 'e5cff6e64464abc28d7a562e1946fbda2f323a62ade5234799aa5ff895c8c36e', 'build/release-readiness/raw_source_removal_573.py': 'efe7c5aa7f7512847dbb97ea6c8c5f4d6e2dbce1004b9b66c2fd71f0fbe4be39', 'build/release-readiness/ssn_filter_all_ghost_features_692.py': 'ca5be997405c970d47c47c71282098c5b4122b03221928c740fdc072c7703e91', 'build/release-readiness/ssn_filter_all_ghost_observer_692.py': 'd023b6ea7b6eac13c3c98633c2cff4c41642cce2f7dedaba279829557c75289e', 'build/release-readiness/prepare_ssn_filter_all_ghost_692.py': '5cd2b90dcf2faa691583b7e67be55118e6aa51097fa290ccd486697228e06b36', 'build/release-readiness/ssn_filter_native_lifetime_observer_675.py': '4b8872dfffaa14866230e309eaa1b2b2b9a833183d2e71bb775ba698128b857d', 'build/release-readiness/ssn_filter_native_client_observer_679.py': 'b0d3c881236a0b87e61aa115ed2de261c8b23cdac28abfd09da55a1d94a663db', 'build/release-readiness/ssn_named_region_observer_592.py': '71703a3dd8fdb972189a92dc2182bb98d8dfec4fc1567bc00202fefeb6581647', 'build/release-readiness/ssn_filter_all_native_observer_688.py': '3d14dc92f94797c9415d3e7347757322eb0039f59981b3f03a5c3b970aa18e49', 'build/release-readiness/readiness-ssn-filters-2612-neoforge-01/toms-observer/toms.init.gradle': '76bb96bde9dcdf5f1c6051c4e29ec5daf1f6af4c0ab110b4c23765a30aa666ea', 'build/release-readiness/readiness-ssn-filters-2612-neoforge-01/toms-observer/java/com/salts_inventory_update/functionaltest/EverythingAuditQa.java': '5d00030e5d0bc5c3ee63df825994a3a22848adce989b75f1ff5933cd25a0812d', 'build/release-readiness/readiness-ssn-filters-2612-neoforge-01/toms-observer/generated-metadata/resources/pack.mcmeta': '3e98634cc49357f5e77c5d5c0b4c474473c38a1e87a41beaa3b3c12e15c4c42c', 'build/release-readiness/readiness-ssn-filters-2612-neoforge-01/toms-observer/generated-metadata/resources/META-INF/neoforge.mods.toml': 'b934b45a453d81742f7c5f781801ebdc6e5f9fe9055c3fa5fa4194f7cfeab6c8', 'build/release-readiness/readiness-ssn-filters-2612-neoforge-01/toms-observer/generated-metadata/java/com/salts_inventory_update/functionaltest/ChestCloseQaMod.java': '8786a886e1862c900902e1c73111c3842cf3cd4ffc1443032c6b292c58f31d42', 'build/release-readiness/readiness-ssn-filters-2612-neoforge-01/toms-observer/artifact/chest-close-qa-26.1.2-neoforge.jar': '3c19dc28b3408a5f304674ae3f5b96cc32f2198a2f760e0dd823ce9846b0aabf'}}
CONTRACTS={'1.20.1/forge': {'lifetimes': [{'publisher_sha256': '1db6f5595e5edc255db6800de9e0e1aa8e01e10d79d1784d549d70eb2cf53012', 'native_control': {'file': 'output/release-readiness/ssn-native-filter-lifetime-672-01/native-control.json', 'sha256': '6695715a349500a20bd36697c59ca3df2ad316668d8e0a7ea5133445f68df624'}, 'limit': 'Both native SSN1.13.2filter menus remain open after real source removal, then close normally. Automatic removal closure is not claimed on this exact pin.'}], 'ghosts': [{'publisher_sha256': '1db6f5595e5edc255db6800de9e0e1aa8e01e10d79d1784d549d70eb2cf53012', 'native_control': {'file': 'output/release-readiness/ssn-native-collector-ghost-678-01/native-control.json', 'sha256': 'c477fef2c62181cbbc2b18bdba89a3e221a64998ffd638655be0dfd5a013e1ae'}, 'limit': 'Original SSN1.13.2 collector client stays blank after copy and reopening. Right-click with the original carried stack recopies one server ghost instead of incrementing it. Native client synchronization/count increment is not claimed on this pin; other target/filter expectations remain strict.'}]}, '1.21.1/neoforge': {'lifetimes': [{'native_control': {'file': 'output/release-readiness/ssn-native-lifetime-685-1211-01/native-control.json', 'sha256': '7a3df53b88e4d78f6e8f19eec5989255781a75a568ce5539e8c7362bd4f1e547'}, 'publisher_sha256': '1b5d1fa174406d1813882d25c1f523b2eb1b52805173427ae96ebdf4be81beac', 'limit': 'Both original filter menus on this exact staged native publisher persist after physical source removal, then close normally with every tagged item retained. Automatic source-removal closure is not claimed.'}], 'ghosts': [{'native_control': {'file': 'output/release-readiness/ssn-native-collector-695-1211-01/native-control.json', 'sha256': '4381b0c447f08a215fb813b6bb6df93b51f6f0fbba15068e4e1454a84e464157'}, 'publisher_sha256': '1b5d1fa174406d1813882d25c1f523b2eb1b52805173427ae96ebdf4be81beac', 'limit': 'Original Collector on this exact native publisher stays visually blank after copying/reopening; right-click with the same carried stack re-copies one server ghost. Client display/count synchronization is not claimed on this pin; full native server filter, typed item ownership, clear and ordinary close remain strictly checked.'}]}, '26.1.2/neoforge': {'lifetimes': [{'native_control': {'file': 'output/release-readiness/ssn-native-lifetime-689-2612-01/native-control.json', 'sha256': '397ce7601c3ea59cceb871ac32a189c3b5fab06c389c33ee01cd09983c702f1d'}, 'publisher_sha256': 'e2cc496e00f68a8f9633f7661be81813cb3468534df8a9ec232d23864f4373f2', 'limit': 'Both original filter menus on this exact staged native publisher persist after physical source removal, then close normally with every tagged item retained. Automatic source-removal closure is not claimed.'}], 'ghosts': [{'native_control': {'file': 'output/release-readiness/ssn-native-collector-695-2612-01/native-control.json', 'sha256': 'f18024c5fa8b25eb5fec4d39a6057db60e1700228f40946c4d46ac8afc8f9832'}, 'publisher_sha256': 'e2cc496e00f68a8f9633f7661be81813cb3468534df8a9ec232d23864f4373f2', 'limit': 'Original Collector on this exact native publisher stays visually blank after copying/reopening; right-click with the same carried stack re-copies one server ghost. Client display/count synchronization is not claimed on this pin; full native server filter, typed item ownership, clear and ordinary close remain strictly checked.'}]}}
LABELS=['initial','seeded','copied','incremented','physical-returned','moved']

def classes(case):
    if case.endswith('filter_kabel'):
        return ('com.lothrazar.storagenetwork.block.cable.linkfilter.ContainerCableFilter',
                'com.lothrazar.storagenetwork.block.cable.linkfilter.ScreenCableFilter')
    assert case.endswith('collector')
    return ('com.lothrazar.storagenetwork.block.collection.ContainerCollectionFilter',
            'com.lothrazar.storagenetwork.block.collection.ScreenCollectionFilter')

def exact_ghosts(state,original,count):
    ghosts=state['settings']['ghosts'];assert len(ghosts)==18
    assert stack(ghosts[0])==stack(original,count)
    assert all(not item(v)for v in ghosts[1:])

def validate(target,journal,states,driver_sha):
    assert target in TARGETS and journal['complete']is True
    assert journal['cases']==['filter_kabel','collector']and journal['driver_sha256']==driver_sha
    contract=CONTRACTS[target]
    assert journal['native_source_lifetime_controls']==contract['lifetimes']
    assert journal['native_collector_ghost_controls']==contract['ghosts']
    names=[];expected_keys=[]
    for path in ['filter_kabel','collector']:
        case='registry-storagenetwork-'+path;menu,screen=classes(case)
        labels=LABELS+(['priority-plus','priority-minus','allowlist']if path=='filter_kabel'else[])+['reopened','cleared']
        expected_keys += [path+'-'+label for label in labels]
        rows=[states[path+'-'+label]for label in labels]
        initial,seeded,copied,incremented,returned,moved=rows[:6]
        blank=bool(contract['ghosts'])and path=='collector'
        for state in rows:
            client=state['client'];assert client['menu']==menu and client['screen']==screen
            assert type(client['session'])is int and client['session']>0
            assert type(client['locked'])is bool
            bounds=client['bounds'];assert set(bounds)=={'x','y','width','height'}
            assert all(type(v)is int for v in bounds.values())and min(bounds['width'],bounds['height'])>0
            inv(state)
            if blank:
                assert set(client['settings'])=={'ghosts'}and len(client['settings']['ghosts'])==18
                assert all(not item(v)for v in client['settings']['ghosts'])
                assert client['renderedGhosts']==client['settings']['ghosts']
            else:assert client['settings']==state['settings']
        assert all(not item(v)for v in initial['settings']['ghosts'])
        assert all(not item(v['item'])for v in initial['inventory'])
        original=seeded['inventory'][9]['item'];value=item(original)
        key='Count'if'Count'in value else'count'
        assert type(value[key])is(nbtlib.Byte if key=='Count'else nbtlib.Int)and int(value[key])==16
        assert str(value['id'])=='minecraft:diamond'
        data=value['tag']if key=='Count'else value['components']['minecraft:custom_data']
        assert type(data['release_ssn_filter_marker'])is nbtlib.Int and int(data['release_ssn_filter_marker'])==41
        assert type(data['typed'])is nbtlib.ByteArray and list(data['typed'])==[3,6]
        expected=inv(initial);expected[9]=stack(original);assert inv(seeded)==expected
        empty_cursors(initial);empty_cursors(seeded)
        count=16 if path=='filter_kabel'else 1
        exact_ghosts(copied,original,count);exact_ghosts(incremented,original,count if blank else count+1)
        held=list(expected);held[9]=stack('{}')
        for state in [copied,incremented]:
            assert inv(state)==held
            nonempty=[stack(v['item'])for v in state['cursors']if item(v['item'])]
            assert nonempty and all(v==stack(original)for v in nonempty)
        for state in [returned,moved,*rows[6:]]:
            assert inv(state)==expected;empty_cursors(state)
        a,b=returned['client']['bounds'],moved['client']['bounds']
        assert(b['x'],b['y'])==(a['x']+16,a['y']+12)
        assert(b['width'],b['height'])==(a['width'],a['height'])
        assert returned['settings']==incremented['settings']==moved['settings']
        prefix=['original-native-zero-inventory-filter-opens','native-filter-copies-exact-data-and-native-count',
                'ghost-copy-keeps-full-physical-cursor',
                'right-click-recopies-one-ghost-matching-proven-native-blank-client'if blank else'right-click-increments-ghost-count-only',
                'filter-does-not-consume-or-duplicate-physical-item','filter-window-moves-before-native-input']
        if path=='filter_kabel':
            plus,minus,toggle=rows[6:9]
            assert set(initial['settings'])=={'ghosts','priority','allowList'}
            want=dict(moved['settings']);want['priority']=initial['settings']['priority']+1
            assert plus['settings']==want
            want['priority']=initial['settings']['priority'];assert minus['settings']==want
            assert type(want['allowList'])is bool;want['allowList']=not want['allowList']
            assert toggle['settings']==want
            for name in ['btnPlus','btnMinus','btnAllowIgn']:
                assert moved['client']['buttons'][name]['visible']is True and moved['client']['buttons'][name]['active']is True
            prefix+=['priority-increases-by-exactly-one','priority-minus-restores-initial-value','allowlist-click-toggles-only-native-flag']
        else:want=moved['settings']
        reopened,cleared=rows[-2:]
        assert reopened['settings']==want
        assert reopened['client']['session']!=initial['client']['session']
        assert len({s['client']['session']for s in rows[:-2]})==1
        empty=dict(want);empty['ghosts']=['{}']*18;assert cleared['settings']==empty
        prefix+=['all-native-settings-persist-through-ordinary-reopening','empty-hand-click-clears-only-ghost']
        life=states[path+'-native-source-removed'];closed=states[path+'-ordinary-closed']
        assert life['sourceAir']is True and life['blockEntityPresent']is False and life['allNativeValid']is True
        assert life['nativeMenus']==[menu]and inv(life)==expected;empty_cursors(life)
        assert closed['sourceAir']is True and closed['blockEntityPresent']is False
        assert closed['nativeMenus']==[]and inv(closed)==expected;empty_cursors(closed)
        expected_keys += [path+'-native-source-removed',path+'-ordinary-closed']
        prefix+=['source-removal-preserves-proven-native-filter-lifetime','ordinary-close-releases-retained-native-filter-with-exact-items']
        names += [dict(case=path,name=name,passed=True)for name in prefix]
    assert set(states)==set(expected_keys)
    assert journal['checks']==names and len(names)==23
    return dict(assertions=23,numeric_states=19,lifetime_states=4,collector_native_blank_client_limit=bool(contract['ghosts']))

def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed=json.loads(Path(sealed_path).read_text());target=sealed['target']
    assert(target,case)in PAIRS
    menu,screen=classes(case);assert original_classes==[menu]
    verify_sealed(root,sealed,expected)
    qa={k:v for k,v in sealed['archived_inputs'].items()if k.startswith('build/release-readiness/')}
    assert qa==QA_VARIANTS[target]
    summary=json.loads(checked(root,sealed['summary']).read_text());row=summary['results'][0];result=row['results']
    assert result['ssn_filters_features_checked']is True and result['cases']==2 and result['failed']==0 and result['checks']==23
    journal_path=checked(root,sealed['acceptance']);assert journal_path==(root/row['profile']).resolve()/'ssn-filters-release/summary.json'
    journal=json.loads(journal_path.read_text());states={};refs=[]
    files=sorted(journal_path.parent.glob('[0-9][0-9][0-9]-*.json'))
    assert len(files)==19
    for n,path in enumerate(files,1):
        assert path.name.startswith(f'{n:03}-');ref=reference(root,path);assert ref in sealed['full_states']
        states[path.name[4:-5]]=json.loads(path.read_text());refs.append(ref)
    for name in ['filter_kabel','collector']:
        for suffix in ['native-source-removed','ordinary-closed']:
            path=journal_path.parent/(name+'-'+suffix+'.json');ref=reference(root,path);assert ref in sealed['full_states']
            states[name+'-'+suffix]=json.loads(path.read_text());refs.append(ref)
    drivers=[k for k in qa if k.endswith('_features_679.py')or k.endswith('_features_692.py')]
    assert len(drivers)==1
    validation=validate(target,journal,states,sealed['archived_inputs'][drivers[0]])
    publishers=[]
    for artifact in json.loads(checked(root,sealed['staged']).read_text())['artifacts']:
        if artifact['family']!='upstream'or not artifact['filename'].lower().startswith(('simplestoragenetwork-','storagenetwork-')):continue
        p=Path(artifact['file']);p=p if p.is_absolute()else root/p;raw=p.read_bytes();assert hashlib.sha256(raw).hexdigest()==artifact['sha256'];digest=hashlib.sha512(raw).hexdigest()
        if'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(p,digest,target.split('/')[1]),[menu,screen]);assert locations
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1
    for pin in CONTRACTS[target]['lifetimes']+CONTRACTS[target]['ghosts']:
        assert pin['publisher_sha256']==publishers[0]['sha256'];control=json.loads(checked(root,pin['native_control']).read_text())
        assert all(v['passed']is True for v in control['checks'])
        if pin in CONTRACTS[target]['lifetimes']:assert control['status']=='two-native-filter-source-persistence-contracts-confirmed'and len(control['checks'])==14
        else:assert control['status']in ['native-collector-ghost-client-divergence-confirmed','native-collector-ghost-behavior-measured']and len(control['checks'])==10 and control['native_client_sync_matches']is False
        staged=json.loads(checked(root,control['staged']).read_text())
        assert staged['siu_loaded']is False and not any('salts_inventory'in v['filename'].lower()for v in staged['artifacts'])
        for ref in control['inputs']+[v.get('evidence',v)for v in control['states']]:checked(root,ref)
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',original_native_classes=original_classes,native_classes=[menu,screen],subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),acceptance=reference(root,journal_path),native_states=refs,validation=validation,publishers=publishers,native_upstream_limits=CONTRACTS[target],scope='Original native ghost/configuration controls, exact typed item/cursor conservation, moved input, reopening and source lifetime. Two UI aliases share one target execution.',limits='Native source-removal persistence and separately proven Collector display/count behavior remain explicit upstream limits on their exact pins. No network-routing, automation or full-mod gameplay claim.')

def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    assert derive(root,checked(root,record['sealed_evidence']),expected,record['case'],record['original_native_classes'])==record
    return True
