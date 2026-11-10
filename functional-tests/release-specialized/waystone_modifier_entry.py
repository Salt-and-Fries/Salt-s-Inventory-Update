"""Disclosed exact-target entry correction; native assertions stay unchanged."""
from pathlib import Path
import copy,hashlib,json,sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
TARGETS=['26.1.2/neoforge','26.2/fabric','26.2/neoforge','26.3/fabric','26.3/neoforge']
CASE='atlas-waystones-modifiers'
MENU='net.blay09.mods.waystones.menu.WaystoneModifierMenu'
SCREEN='net.blay09.mods.waystones.client.gui.screen.WaystoneModifierScreen'
OLD='waystones:waystone';NEW='waystones:andesite_waystone'


def adapt(case,mc,loader,blocks):
    assert mc+'/'+loader in TARGETS and case['id']==CASE
    assert case['expected_native_menu']==MENU and case['expected_native_screen']==SCREEN
    assert case['expected_nonplayer_slot_count']==5
    assert OLD not in blocks and NEW in blocks,'Require the actual native renamed registry'
    actions=copy.deepcopy(case['actions'])
    replacements=[a for a in actions if a.get('type')=='fixture'and a.get('block')==OLD]
    assert len(replacements)==1 and len(actions)==2
    assert actions[1]['type']=='regression-native-widget'and actions[1]['message']=='Manage Modifiers'
    assert actions[1]['parent_menu']=='net.blay09.mods.waystones.menu.WaystoneEditMenu'
    replacements[0]['block']=NEW
    return actions,dict(scope='Exact '+mc+'/'+loader+' modifier entry only; original slot/transfer assertions unchanged',
        previous_block=OLD,registered_block=NEW,helper_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
        verification='Normal registered block placement and original Manage Modifiers button must reach the original five-slot publisher menu. No menu/provider construction or registry substitution during input.')


def run(profile,mc,loader,*args,**kwargs):
    import registry_entry_adaptations as registry
    import check_mod_interactions as original
    from waystone_observation_archive import ExistingStateArchive
    previous=registry.adapt
    previous_send=original.send
    archive=ExistingStateArchive(profile,previous_send)
    def reviewed(case,m,l,blocks):
        if case['id']==CASE and(m,l)==(mc,loader):return adapt(case,m,l,blocks)
        return previous(case,m,l,blocks)
    registry.adapt=reviewed
    original.send=archive
    try:result=original.run(Path(profile),mc,loader,only=(CASE,))
    finally:
        registry.adapt=previous
        original.send=previous_send
    archive.finish()
    data=json.loads((Path(profile)/'mod-interaction-checks.json').read_text())
    assert data['selection']==dict(only=[CASE],max_cases=None,mods=[])
    assert len(data['cases'])==1 and data['cases'][0]['id']==CASE
    case=data['cases'][0]
    assert case['status']=='passed'and case['native_menus']==[MENU]and case['native_screens']==[SCREEN]
    assert case['registry_entry_adaptation']['registered_block']==NEW and case['original_entry_actions'][0]['block']==OLD
    assert data['checks']and all(v['passed']is True for v in data['checks'])
    assert result['cases']==1 and result['passed']==1 and result['failed']==0 and result['entry_only']==0
    return dict(**result,waystone_modifier_entry_checked=True,waystone_existing_states_archived=True)
