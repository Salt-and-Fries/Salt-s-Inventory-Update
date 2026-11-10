"""Qualify the original modifier sockets after a disclosed native block rename."""
from pathlib import Path
from collections import Counter
import argparse, datetime, hashlib, json, sys
from focused_release_evidence import checked, reference, verify as verify_sealed
from native_publisher_index import index, matches

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from subject_artifacts import current_subjects

TARGETS=['26.1.2/neoforge','26.2/fabric','26.2/neoforge','26.3/fabric','26.3/neoforge']
CASE='atlas-waystones-modifiers'
MENU='net.blay09.mods.waystones.menu.WaystoneModifierMenu'
SCREEN='net.blay09.mods.waystones.client.gui.screen.WaystoneModifierScreen'
OLD='waystones:waystone'
NEW='waystones:andesite_waystone'
HELPER='functional-tests/release-specialized/waystone_modifier_entry.py'
DRIVER='functional-tests/project-everything-regression/check_mod_interactions.py'
REQUIRED=Counter([
    'publisher child menu has the recorded semantic widget', 'publisher transition widget is active',
    'natural opening owns a real window', 'native layout callback succeeds',
    'ordinary menu entry leaves no pending world-use identity',
    'ordinary entry retains the publisher menu or its reviewed entity adapter',
    'owned menu remains present in the tested player mode',
    'source stack carries the regression data marker', 'fixture adds the exact native-capacity stack',
    'separate inventory supplies shared cursor', 'native input receives exact stack',
    'insertion conserves physical items', 'native insertion preserves stack data',
    'native input returns exact stack', 'round trip returns items once',
    'native withdrawal restores the full item metadata',
    *['additional native input accepts tagged stack']*2,
    *['additional input round trip conserves stack and data']*2,
])


def validate_case(target,report,helper_hash,driver_hash):
    assert target in TARGETS
    assert report['minecraft']+'/'+report['loader']==target
    assert report['test_driver_sha256']==driver_hash
    assert report['selection']==dict(only=[CASE],max_cases=None,mods=[])
    assert len(report['cases'])==1
    case=report['cases'][0]
    assert case['id']==CASE and case['status']=='passed'
    assert case['source']=='original-native-atlas'
    assert case['expected_native_menu']==MENU and case['expected_native_screen']==SCREEN
    assert type(case['expected_nonplayer_slot_count'])is int and case['expected_nonplayer_slot_count']==5
    assert case['native_menus']==[MENU] and case['native_screens']==[SCREEN]
    assert case['native_menu_types']==['waystones:waystone_modifiers']
    assert case['slot_access_contract']=='public-desktop-menu-bindings' and case['virtual_policy_query_contract']is True
    assert case['item_serialization_contract']=='native-item-codec'
    before,after=case['original_entry_actions'],case['actions']
    assert len(before)==len(after)==2
    assert before[0]['type']=='fixture' and before[0]['block']==OLD
    assert after[0]==dict(before[0],block=NEW)
    assert after[1]==before[1]
    button=after[1]
    assert button['type']=='regression-native-widget' and button['message']=='Manage Modifiers'
    assert button['parent_menu']=='net.blay09.mods.waystones.menu.WaystoneEditMenu'
    assert button['parent_screen']=='net.blay09.mods.waystones.client.gui.screen.WaystoneEditScreen'
    assert type(button['ordinal'])is int and button['ordinal']==0 and type(button['button'])is int and button['button']==0
    adaptation=case['registry_entry_adaptation']
    assert adaptation['previous_block']==OLD and adaptation['registered_block']==NEW
    assert adaptation['helper_sha256']==helper_hash
    assert type(case['session'])is int and case['session']>0
    assert type(case['slot'])is int and case['slot']==0
    assert case['sampled_input_slots']==[0,2,4] and all(type(v)is int for v in case['sampled_input_slots'])
    assert case['item']=='minecraft:diamond' and type(case['amount'])is int and case['amount']==7
    assert type(case['preexisting_tagged_count'])is int and case['preexisting_tagged_count']==0
    assert type(case['expected_tagged_count'])is int and case['expected_tagged_count']==7
    checks=report['checks']
    assert checks and all(v['case']==CASE and v.get('passed')is True for v in checks)
    names=Counter(v['check']for v in checks)
    assert not (REQUIRED-names), 'Original native entry, socket and full-data transfer checks are incomplete'
    # Repeated additional-slot checks are intentional, for the native middle
    # and last sockets. They must not be collapsed into a set or filled by an
    # unrelated case's assertions. Extra compound checks remain in the report.
    for name in REQUIRED:
        assert names[name]==REQUIRED[name], 'Unexpected multiplicity in the reviewed socket checks'
    return len(checks)


def validate_native_states(case,states):
    """Require actual source shape and exact full-data witnesses, not labels."""
    assert isinstance(states,list)and states
    signatures={cell['signature']for state in states for cell in state['inventory']
        if cell.get('item')==case['item']and type(cell.get('count'))is int and cell['count']==case['amount']
        and isinstance(cell.get('signature'),str)and 'siu_regression_marker'in cell['signature']}
    assert len(signatures)==1,'Require one actual complete seeded/recovered resource signature'
    signature=next(iter(signatures))
    menus=[menu for state in states for menu in state['menus']
        if menu.get('type')==MENU and menu.get('session')==case['session']and menu.get('valid')is True]
    assert menus,'The real native source must appear in a saved server snapshot'
    for menu in menus:
        assert all(type(cell.get('player'))is bool for cell in menu['slots'])
        slots=[cell for cell in menu['slots']if cell['player']is False]
        assert len(slots)==5 and [cell['index']for cell in slots]==[0,1,2,3,4]
        assert all(type(cell['index'])is int and cell.get('virtual')is False for cell in slots)
    for slot in case['sampled_input_slots']:
        assert any(any(cell['index']==slot and cell['player']is False and cell.get('item')==case['item']
            and type(cell.get('count'))is int and cell['count']==case['amount']and cell.get('signature')==signature
            for cell in menu['slots'])for menu in menus),('No full-data native insertion witness',slot)
    assert any(not any(menu.get('session')==case['session']for menu in state['menus'])
        and state['cursor'].get('item')==case['item']and type(state['cursor'].get('count'))is int
        and state['cursor']['count']==case['amount']and state['cursor'].get('signature')==signature
        for state in states),'Require the actual complete shared cursor after the original source closes'
    return dict(nonplayer_slots=5,sampled_slots=case['sampled_input_slots'],closed_cursor=True,states=len(states))


def native_states(root,profile,sealed_refs,case):
    folder=profile/'waystone-existing-observations'
    index_ref=reference(root,folder/'index.json')
    assert index_ref in sealed_refs
    index_data=json.loads((folder/'index.json').read_text(encoding='utf-8'))
    assert set(index_data)=={'schema','states'}and index_data['schema']=='waystone-existing-server-observations-v1'
    records=index_data['states'];assert records and len({r['file']for r in records})==len(records)
    assert {p.name for p in folder.iterdir()}=={'index.json',*[r['file']for r in records]},'Missing or unindexed native state archive'
    states=[]
    for ordinal,row in enumerate(records,1):
        assert set(row)=={'ordinal','file','sha256','command','args','kwargs'}
        assert type(row['ordinal'])is int and row['ordinal']==ordinal
        assert row['command']=='regression-state'and row['args']==[]and isinstance(row['kwargs'],dict)
        assert row['file']==str(ordinal).zfill(5)+'-server-state.json'
        path=folder/row['file'];ref=reference(root,path)
        assert ref['sha256']==row['sha256']and ref in sealed_refs
        states.append(json.loads(path.read_text(encoding='utf-8')))
    return index_ref,validate_native_states(case,states)


def derive(root,sealed_path,expected):
    root=Path(root).resolve()
    sealed=json.loads(Path(sealed_path).read_text(encoding='utf-8'))
    assert sealed['target']in TARGETS
    verify_sealed(root,sealed,expected)
    summary=json.loads(checked(root,sealed['summary']).read_text(encoding='utf-8'))
    result=summary['results'][0]['results']
    assert result['waystone_modifier_entry_checked']is True
    assert result['waystone_existing_states_archived']is True
    assert type(result['cases'])is int and result['cases']==1 and type(result['passed'])is int and result['passed']==1
    assert type(result['failed'])is int and result['failed']==0 and type(result['entry_only'])is int and result['entry_only']==0
    report_path=checked(root,sealed['acceptance'])
    assert report_path.name=='mod-interaction-checks.json'
    report=json.loads(report_path.read_text(encoding='utf-8'))
    count=validate_case(sealed['target'],report,sealed['archived_inputs'][HELPER],sealed['archived_inputs'][DRIVER])
    state_index,state_validation=native_states(root,report_path.parent,sealed['full_states'],report['cases'][0])
    staging=json.loads(checked(root,sealed['staged']).read_text(encoding='utf-8'))['artifacts']
    publishers=[]
    for artifact in staging:
        if artifact['family']!='upstream' or not artifact['filename'].startswith('waystones-'):
            continue
        path=Path(artifact['file']);path=path if path.is_absolute()else root/path
        raw=path.read_bytes();assert hashlib.sha256(raw).hexdigest()==artifact['sha256']
        sha512=hashlib.sha512(raw).hexdigest()
        if 'sha512'in artifact:assert artifact['sha512']==sha512
        locations=matches(index(path,sha512,sealed['target'].split('/')[1]),[MENU,SCREEN])
        found={name[:-6].replace('/','.')for loc in locations for name in loc['classes']}
        assert found=={MENU,SCREEN}, 'Both original native classes must exist in this exact staged publisher'
        publishers.append(dict(filename=artifact['filename'],sha256=artifact['sha256'],sha512=sha512,class_locations=locations))
    assert len(publishers)==1, 'Require exactly one actual native Waystones publisher'
    return dict(target=sealed['target'],case=CASE,disposition='functional-pass',native_classes=[MENU,SCREEN],
        sealed_evidence=reference(root,sealed_path),acceptance=reference(root,report_path),checks=count,
        native_state_index=state_index,native_state_validation=state_validation,
        publishers=publishers,subjects={k:list(v)for k,v in expected.items()},
        classification_note='The historical fixture requested the removed waystones:waystone block. A disclosed setup-only correction uses the registered andesite_waystone, then the original Manage Modifiers button reaches the original native five-slot UI. No SIU product defect or unavailable native interface is inferred from the old fixture failure.',
        scope='Normal parent/child entry, native slot policy, exact tagged seven-item insertion/withdrawal and conservation through first/middle/last modifier sockets; any additional original compound checks are retained in the journal.',
        limits='Exact target and native publisher only. This checks the inventory/socket interface, not every modifier effect, teleport cost, destination selection or arbitrary upstream patch.')


def verify(root,record,expected):
    path=checked(root,record['sealed_evidence'])
    assert derive(root,path,expected)==record
    return True


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--id',required=True)
    parser.add_argument('--run',required=True,action='append')
    args=parser.parse_args()
    assert args.id and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in args.id)
    output=ROOT/'output/release-readiness'/('coverage-waystone-'+args.id+'.json')
    assert not output.exists()
    records=[]
    for run in args.run:
        assert run and all(c in 'abcdefghijklmnopqrstuvwxyz0123456789-'for c in run)
        path=ROOT/'output/release-readiness'/('focused-evidence-'+run+'.json')
        target=json.loads(path.read_text())['target']
        records.append(derive(ROOT,path,current_subjects(target)))
    assert records and len({v['target']for v in records})==len(records)
    output.write_text(json.dumps(dict(status='reviewed-waystone-modifier-workflows-reconciled',
        generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),records=records,
        limits='Reconciliation of existing sealed native runs. No new gameplay, skipped assertion or unrelated UI acceptance.'),indent=2)+'\n')
    print(len(records),'actual native modifier workflows reconciled')


if __name__=='__main__':main()
