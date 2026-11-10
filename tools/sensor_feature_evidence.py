"""Review existing native sensor feature evidence; never manufacture gameplay.

Only the listed targets and historical aliases are handled. Real passing
assertions, their original QA archive, typed native state witnesses and the
exact staged publisher must agree before a case receives a disposition.
"""
from pathlib import Path
from collections import Counter
import hashlib,json,sys
import nbtlib
from focused_release_evidence import checked,reference,verify as verify_sealed
from native_publisher_index import index,matches

MENU='ad_astra_giselle_addon.common.menu.RocketSensorMenu'
SCREEN='ad_astra_giselle_addon.client.screen.RocketSensorScreen'
DRIVER='functional-tests/release-specialized/sensor_features.py'
NATIVE_LIFETIME_DRIVER='build/release-readiness/sensor_native_lifetime_features_580.py'
NATIVE_LIFETIME_INPUTS={'build/release-readiness/sensor_native_lifetime_features_580.py': '9a699478fd55efc870b2153e4e18be2e531c5067f6d05d5eeea6ac7d0e2aed16', 'build/release-readiness/sensor_native_lifetime_observer_580.py': '083148b50bddffc1fee41cb0e3fccb96450fad86bc89930442134943d28f1f54', 'build/release-readiness/prepare_sensor_native_lifetime_580.py': 'cbe825633691e73d955b360c5243d583f06781759071dca06f09873cf1006fb1', 'build/release-readiness/run_sensor_native_lifetime_580.py': '566f219b8166c385ea6626478e02aceedacd8070dc3567129b9ce89978639cce', 'build/release-readiness/raw_source_removal_573.py': 'efe7c5aa7f7512847dbb97ea6c8c5f4d6e2dbce1004b9b66c2fd71f0fbe4be39', 'output/release-readiness/sensor-native-lifetime-577-01/native-control.json': 'f13a4c760e783b75ea8b8a0d03eefeaa807708509570b52f4667adea3645f73e', 'output/release-readiness/sensor-native-validity-contracts-578-01/manifest.json': '8a3d54e7b604317c2c6286acfa3396b159abdf479dfedff24c64cdcc34f3cb04'}
ORIGINAL_DRIVER_HASH='a707c4ce22d17f6865a711d6ca253836c66b7552311ec3f5a00c0ebb7165dca2'
NATIVE_LIFETIME_PINS={'1.20.1/fabric': {'publishers': ['18bdbbb84666d08725258196f25f7618b6193be96debabedd0a13183bff28c5d', '836d14396366fb370f432683aaecb85aa124bf3cff761e3fc224ecbce46a7aec'], 'bytecode': {'file': 'output/release-readiness/sensor-native-validity-contracts-578-01/1.20.1-fabric-primary-bytecode.txt', 'sha256': '5c5cd7c9ab57648471d7cb88e070922b80d2cdac69d9f9004e7be522310ebe7a'}}, '1.20.1/forge': {'publishers': ['d84dc1182b4c570d955df67b80703dc058ce944c7d9b0702e3577934c96d864c', 'f5638fe29542497b70e02744b74ed2cd4810c0edc48d176af31bfa32b44b47b3'], 'bytecode': {'file': 'output/release-readiness/sensor-native-validity-contracts-578-01/1.20.1-forge-primary-bytecode.txt', 'sha256': 'a67671000b06a80d3b3a735475ca35cb100df2ce0e88fbe753509efc19ea658f'}}, '1.21.1/fabric': {'publishers': ['3b6649cb1d8985e39efd1bf6ebfc3f5389072696ff7b612433828369fd1a45ad', '3ff6b11ca2fc915c2e6dd244ae0a04b904c4faefa525107585b7dd6b9d18637b'], 'bytecode': {'file': 'output/release-readiness/sensor-native-validity-contracts-578-01/1.21.1-fabric-primary-bytecode.txt', 'sha256': '0e0aa08d8bbc0ead39f8eb06a8df1aa08d0309f2ecccc8dd33eb6c9817cd252d'}}, '1.21.1/neoforge': {'publishers': ['8b50abe78976e7a85fd506b868ec896fe8d058ceb134e6cc5f5fe78f489e57bf', 'b8d1441e284db2e1e8ce5ffe739452633c90725312e476db8d8a0a4b64126856'], 'bytecode': {'file': 'output/release-readiness/sensor-native-validity-contracts-578-01/1.21.1-neoforge-primary-bytecode.txt', 'sha256': 'ff67f830338d3cb80fbb89b4dd4ae061f465ac2b1f4dfe485b9e0bd2a9395510'}}}
TARGETS=['1.20.1/fabric','1.20.1/forge','1.21.1/fabric','1.21.1/neoforge']
ALIASES={
 'ad-astra-giselle-addon-rocket-sensor':{'1.20.1/fabric','1.21.1/fabric'},
 'atlas-ad-astra-giselle-addon-rocket-sensor':{'1.20.1/fabric','1.21.1/fabric','1.21.1/neoforge'},
 'registry-ad_astra_giselle_addon-rocket_sensor':{'1.20.1/forge','1.21.1/neoforge'},
}
LABELS=['initial','inverted','found-selected','moved','inversion-restored','reopened']
REQUIRED=Counter([
 'original-native-zero-slot-sensor-opens','initial-native-settings-match-client-and-server',
 'inversion-click-changes-exact-native-boolean','native-list-click-selects-the-requested-mode',
 'sensor-moves-before-next-native-option-input','moved-checkbox-restores-only-inversion',
 'native-sensor-configuration-persists-through-ordinary-reopening',
 'settings-preserve-complete-inventory-and-empty-cursor','sensor-source-removal-closes-its-session',
])
LIFETIME_CHECKS=REQUIRED.copy()
del LIFETIME_CHECKS['sensor-source-removal-closes-its-session']
LIFETIME_CHECKS.update(['native-source-owner-matches-live-block-before-removal','native-always-valid-contract-matches-upstream-after-real-removal','ordinary-close-releases-removed-upstream-source-session'])
LIFETIME_SCOPE='Eight original settings assertions plus explicit live-owner, real block removal/native always-valid persistence, complete item/cursor conservation and ordinary-close cleanup. Automatic source-removal closure is not provided by this pinned upstream parent.'
LIFETIME_LIMITS='Pinned native Ad Astra parent unconditionally reports valid=true and leaves this zero-slot UI open after its block is removed, including without SIU. Close it normally. Exact1.20.1Fabric and1.21.1Fabric/NeoForge publishers with matching no-SIU controls only; no automatic removal-closure, rocket-flight or redstone-automation claim.'
SCHEMA='sensor-original-feature-observations-v1'
SCOPE='Native sensor inversion and FOUND-mode selection through original controls; exact server/client settings, moved input, complete physical-item/cursor conservation, ordinary reopening and source removal.'
LIMITS='Exact named target and native publisher only. No rocket-flight, redstone-circuit automation, general machinery, other upstream patch, or full-mod gameplay acceptance. Shared historical aliases add no gameplay executions.'

SECOND_NATIVE_LIFETIME_INPUTS={'build/release-readiness/sensor_native_lifetime_features_580.py': '9a699478fd55efc870b2153e4e18be2e531c5067f6d05d5eeea6ac7d0e2aed16', 'build/release-readiness/sensor_native_lifetime_observer_580.py': '083148b50bddffc1fee41cb0e3fccb96450fad86bc89930442134943d28f1f54', 'build/release-readiness/prepare_sensor_native_lifetime_580.py': 'cbe825633691e73d955b360c5243d583f06781759071dca06f09873cf1006fb1', 'build/release-readiness/raw_source_removal_573.py': 'efe7c5aa7f7512847dbb97ea6c8c5f4d6e2dbce1004b9b66c2fd71f0fbe4be39', 'output/release-readiness/sensor-native-validity-contracts-578-01/manifest.json': '8a3d54e7b604317c2c6286acfa3396b159abdf479dfedff24c64cdcc34f3cb04', 'build/release-readiness/run_sensor_native_lifetime_587.py': '3ed28b7f84d5649ba6f316a9144b749fee55cf9bd010b4e4ce10adb1e29ff0ef', 'output/release-readiness/sensor-native-lifetime-586-01/native-control.json': '50be88159089e630e88426a090ed9e1a296bf79e5e5fe22f55704918bac77735'}
NATIVE_LIFETIME_CONTROLS={'1.20.1/fabric': 'output/release-readiness/sensor-native-lifetime-577-01/native-control.json', '1.21.1/fabric': 'output/release-readiness/sensor-native-lifetime-586-01/native-control.json', '1.21.1/neoforge': 'output/release-readiness/sensor-native-lifetime-594-01/native-control.json'}

NEO_NATIVE_LIFETIME_INPUTS={'build/release-readiness/sensor_native_lifetime_features_580.py': '9a699478fd55efc870b2153e4e18be2e531c5067f6d05d5eeea6ac7d0e2aed16', 'build/release-readiness/sensor_native_lifetime_observer_580.py': '083148b50bddffc1fee41cb0e3fccb96450fad86bc89930442134943d28f1f54', 'build/release-readiness/prepare_sensor_native_lifetime_580.py': 'cbe825633691e73d955b360c5243d583f06781759071dca06f09873cf1006fb1', 'build/release-readiness/run_sensor_native_named_595.py': '7d6f97d6e8afbd9733bbee6ac80d8f9c2b93de7c0a42694ea02a75bb5de3cddf', 'build/release-readiness/sensor_native_named_observer_595.py': '68485e24e97e453176654d6412061377af6146081d09e1bbb28c0c7ddd6edc95', 'build/release-readiness/raw_source_removal_573.py': 'efe7c5aa7f7512847dbb97ea6c8c5f4d6e2dbce1004b9b66c2fd71f0fbe4be39', 'output/release-readiness/sensor-native-lifetime-594-01/native-control.json': '8a2e629d95bb6c22046906cb2b937d1054e526eb0e22e1159ede49299a4406ba', 'output/release-readiness/sensor-native-validity-contracts-578-01/manifest.json': '8a3d54e7b604317c2c6286acfa3396b159abdf479dfedff24c64cdcc34f3cb04'}

def typed(value):
    if isinstance(value,dict):return type(value).__name__,tuple((k,typed(v))for k,v in sorted(value.items()))
    if isinstance(value,list):return type(value).__name__,tuple(typed(v)for v in value)
    return type(value).__name__,str(value)

def inventory(state):
    rows=state['inventory'];assert isinstance(rows,list)and len(rows)>=36
    assert [v['index']for v in rows]==list(range(len(rows)))
    assert all(type(v['index'])is int and isinstance(v['item'],str)for v in rows)
    parsed=[nbtlib.parse_nbt(v['item'])for v in rows]
    assert all(isinstance(v,nbtlib.Compound)for v in parsed)
    return tuple(typed(v)for v in parsed),parsed

def validate(target,journal,states,removed,driver_hash,native_lifetime=False):
    assert target in TARGETS
    assert journal['complete']is True and journal['cases']==['rocket-sensor']
    assert journal['driver_sha256']==driver_hash
    required=LIFETIME_CHECKS if native_lifetime else REQUIRED
    assert Counter(v['name']for v in journal['checks'])==required,'Missing, repeated or unrelated native feature assertion'
    assert all(v.get('passed')is True for v in journal['checks'])
    if native_lifetime:
        assert isinstance(states,list)and len(states)==7
        removed_source=states[-1];states=states[:-1]
    assert isinstance(states,list)and len(states)==len(LABELS)
    initial,changed,selected,moved,restored,reopened=states
    baseline,parsed=inventory(initial)
    seed=parsed[9];assert seed.get('id')==nbtlib.String('minecraft:diamond')
    countkey='Count'if 'Count'in seed else'count'
    counttype=nbtlib.Byte if countkey=='Count'else nbtlib.Int
    assert isinstance(seed.get(countkey),counttype)and int(seed[countkey])==3
    data=seed.get('tag') if 'tag'in seed else seed.get('components',{}).get('minecraft:custom_data')
    assert isinstance(data,nbtlib.Compound)and isinstance(data.get('release_sensor_marker'),nbtlib.Int)and int(data['release_sensor_marker'])==17
    assert isinstance(data.get('typed'),nbtlib.ByteArray)and list(data['typed'])==[2,5]
    for state in states:
        client=state['client']
        assert client['menu']==MENU and client['screen']==SCREEN
        assert type(client['session'])is int and client['session']>0
        assert type(state['inverted'])is bool and type(client['inverted'])is bool and state['inverted']==client['inverted']
        assert isinstance(state['sensingType'],str)and state['sensingType']==client['sensingType']
        bounds=client['bounds'];assert set(bounds)=={'x','y','width','height'}
        assert all(type(v)is int for v in bounds.values())and bounds['width']>0 and bounds['height']>0
        assert inventory(state)[0]==baseline,'Full typed physical inventory changed during native settings input'
        cursors=state['cursors'];assert isinstance(cursors,list)and cursors
        assert all(isinstance(v.get('menu'),str)and isinstance(nbtlib.parse_nbt(v['item']),nbtlib.Compound)and not nbtlib.parse_nbt(v['item'])for v in cursors)
    assert all(v['client']['session']==initial['client']['session']for v in states[:-1])
    assert changed['inverted']is not initial['inverted']and changed['sensingType']==initial['sensingType']
    choices=[v for v in changed['client']['choices']if isinstance(v.get('type'),str)and v['type'].endswith(':found')]
    assert len(choices)==1
    wanted=choices[0]['type'];assert wanted!=initial['sensingType']
    assert all(v['sensingType']==wanted for v in states[2:])
    assert selected['inverted']==changed['inverted']and moved['inverted']==changed['inverted']
    assert selected['client']['bounds']!=moved['client']['bounds']
    assert restored['inverted']==reopened['inverted']==initial['inverted']
    for state in [initial,moved]:
        button=state['client']['checkbox'];assert button['active']is True and button['visible']is True
    assert type(reopened['client']['session'])is int
    assert isinstance(removed.get('menus'),list)
    assert not any(v.get('session')==reopened['client']['session']for v in removed['menus'])
    if native_lifetime:
        for state in states:
            life=state['sourceLifetime']
            assert life==dict(block='ad_astra_giselle_addon:rocket_sensor',blockEntityMatchesOwner=True,ownerRemoved=False,nativeMenuValid=True)
        life=removed_source['sourceLifetime']
        assert life==dict(block='minecraft:air',blockEntityMatchesOwner=False,ownerRemoved=True,nativeMenuValid=True)
        assert removed_source['client']==reopened['client'],'Native source UI changed unexpectedly after world removal'
        assert removed_source['inverted']==reopened['inverted']and removed_source['sensingType']==reopened['sensingType']
        assert inventory(removed_source)[0]==baseline
        assert removed_source['cursors']and all(not nbtlib.parse_nbt(v['item'])for v in removed_source['cursors'])
    return dict(states=7 if native_lifetime else 6,assertions=sum(required.values()),native_settings_synchronized=True,
                typed_inventory_and_empty_cursors=True,source_removal_observed=True,
                automatic_source_removal_closure=not native_lifetime,ordinary_close_cleanup=native_lifetime)


def specification(target,case,classes):
    assert case in ALIASES and target in ALIASES[case],'Unreviewed native sensor alias or target'
    assert isinstance(classes,list)and classes and len(classes)==len(set(classes))
    assert set(classes)<={MENU,SCREEN},'Unreviewed original native classification'
    return tuple(classes)

def derive(root,sealed_path,expected,case,original_classes):
    root=Path(root).resolve();sealed_path=Path(sealed_path)
    sealed=json.loads(sealed_path.read_text(encoding='utf-8'));target=sealed['target']
    specification(target,case,original_classes);verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text(encoding='utf-8'))
    row=summary['results'][0];result=row['results']
    assert result.get('sensor_features_checked')is True
    assert type(result['cases'])is int and result['cases']==1 and type(result['failed'])is int and result['failed']==0
    native_lifetime=NATIVE_LIFETIME_DRIVER in sealed['archived_inputs']
    driver_hash=sealed['archived_inputs'][DRIVER]
    if native_lifetime:
        assert target in NATIVE_LIFETIME_CONTROLS,'Only exact independently controlled publishers'
        assert driver_hash==ORIGINAL_DRIVER_HASH
        inputs=NATIVE_LIFETIME_INPUTS if target=='1.20.1/fabric'else NEO_NATIVE_LIFETIME_INPUTS if target=='1.21.1/neoforge'else SECOND_NATIVE_LIFETIME_INPUTS
        assert all(sealed['archived_inputs'].get(path)==digest for path,digest in inputs.items()),'Unreviewed native lifetime fixture source'
        ref=NATIVE_LIFETIME_PINS[target]['bytecode'];assert sealed['archived_inputs'].get(ref['file'])==ref['sha256']
        driver_hash=sealed['archived_inputs'][NATIVE_LIFETIME_DRIVER]
        control=json.loads((root/NATIVE_LIFETIME_CONTROLS[target]).read_text())
        expected_status='native-only-NeoForge-sensor-source-removal-persistence-confirmed'if target=='1.21.1/neoforge'else'native-only-sensor-source-removal-persistence-confirmed'
        assert control['status']==expected_status and len(control['checks'])==(6 if target=='1.21.1/neoforge'else 7)and all(v['passed']is True for v in control['checks'])
        for observation in control['states']:checked(root,observation['evidence'])
    elif 'build/release-readiness/sensor_seeded_features_574.py' in sealed['archived_inputs']:
        assert driver_hash==ORIGINAL_DRIVER_HASH
        assert all(sealed['archived_inputs'].get(path)==digest for path,digest in {'build/release-readiness/sensor_seeded_features_574.py': 'a2dee7a2a18903cbee298f20e3c5af85ed256502038eef6f85def73c2e897692', 'build/release-readiness/run_sensor_raw_removal_574.py': '312774acaef18aeb638c9dbc626a220c47399d2fd37b617196277f21027227fb', 'build/release-readiness/raw_source_removal_573.py': 'efe7c5aa7f7512847dbb97ea6c8c5f4d6e2dbce1004b9b66c2fd71f0fbe4be39'}.items()),'Unreviewed post-fixture sensor seed/cleanup adapter'
        driver_hash=sealed['archived_inputs']['build/release-readiness/sensor_seeded_features_574.py']
    assert type(result['checks'])is int and result['checks']==sum((LIFETIME_CHECKS if native_lifetime else REQUIRED).values())
    profile=(root/row['profile']).resolve();journal_path=checked(root,sealed['acceptance'])
    assert journal_path==profile/'sensor-release/summary.json'
    journal=json.loads(journal_path.read_text(encoding='utf-8'))
    state_refs=[];states=[]
    labels=LABELS+(['source-removed']if native_lifetime else[])
    assert {p.name for p in journal_path.parent.glob('[0-9][0-9][0-9]-*.json')}=={f'{n:03}-{label}.json'for n,label in enumerate(labels,1)}
    for n,label in enumerate(labels,1):
        path=journal_path.parent/(str(n).zfill(3)+'-'+label+'.json');r=reference(root,path)
        assert r in sealed['full_states'],'Native setting witness missing from the original sealed state journal'
        state_refs.append(r);states.append(json.loads(path.read_text(encoding='utf-8')))
    removal_path=profile/'regression-state.json';removal_ref=reference(root,removal_path)
    removed=json.loads(removal_path.read_text(encoding='utf-8'))
    validated=validate(target,journal,states,removed,driver_hash,native_lifetime)
    staged=json.loads(checked(root,sealed['staged']).read_text(encoding='utf-8'))['artifacts'];publishers=[]
    for artifact in staged:
        if artifact['family']!='upstream' or 'giselle'not in artifact['filename'].lower():continue
        path=Path(artifact['file']);path=path if path.is_absolute()else root/path
        raw=path.read_bytes();assert hashlib.sha256(raw).hexdigest()==artifact['sha256']
        digest=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert artifact['sha512']==digest
        locations=matches(index(path,digest,target.split('/')[1]),[MENU,SCREEN])
        found={name[:-6].replace('/','.')for loc in locations for name in loc['classes']}
        assert found=={MENU,SCREEN},'Require both original native classes in the actual staged publisher'
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=digest,class_locations=locations))
    assert len(publishers)==1,'Require exactly one native Giselle publisher'
    if native_lifetime:assert set(NATIVE_LIFETIME_PINS[target]['publishers'])<={v['sha256']for v in staged if v['family']=='upstream'},'Wrong native parent/addon pins'
    return dict(schema=SCHEMA,target=target,case=case,disposition='functional-pass',
        original_native_classes=original_classes,native_classes=[MENU,SCREEN],
        subjects={k:list(v)for k,v in expected.items()},sealed_evidence=reference(root,sealed_path),
        acceptance=reference(root,journal_path),native_states=state_refs,source_removal=removal_ref,
        validation=validated,publishers=publishers,scope=LIFETIME_SCOPE if native_lifetime else SCOPE,limits=LIFETIME_LIMITS if native_lifetime else LIMITS)

def verify(root,record,expected):
    assert record['schema']==SCHEMA and record['disposition']=='functional-pass'
    checked(root,record['source_removal'])
    actual=derive(root,checked(root,record['sealed_evidence']),expected,record['case'],record['original_native_classes'])
    assert actual==record,'Changed or over-broad sensor feature acceptance'
    return True
