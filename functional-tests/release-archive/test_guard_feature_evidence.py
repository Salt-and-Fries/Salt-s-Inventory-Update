"""Offline corruption checks for Guard reporting, never native gameplay passes."""
from pathlib import Path
import copy,sys,unittest
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
import focused_release_evidence_guard as m
DRIVER='a'*64


def fixture(target='1.21.1/neoforge'):
    menu,screen=m.native_classes(target)
    base=dict(following=False,patrolling=False,patrolPos='null',
        cells=[dict(index=i,item='{}')for i in range(6)],inventory=[dict(index=i,item='{}')for i in range(41)],
        cursors=[dict(menu=menu,item='{}')],
        client=dict(menu=menu,screen=screen,session=17,locked=False,following=False,patrolling=False,
            bounds=dict(x=12,y=24,width=160,height=80),
            buttons=[dict(follow=v,ordinal=i,type=screen+'$GuardGuiButton',message='',active=True,visible=True)for i,v in enumerate([False,True])]))
    states=[copy.deepcopy(base)for _ in m.LABELS]
    stacks=[]
    for i,name in enumerate(m.ITEMS):
        data='{release_guard_marker:'+str(40+i)+',typed:[B;2b,8b]}'
        if target.startswith('1.20.1/'):
            value='{id:"minecraft:'+name+'",Count:1b,tag:'+data[:-1]+',Damage:2}}'
        else:value='{id:"minecraft:'+name+'",count:1,components:{"minecraft:custom_data":'+data+',"minecraft:damage":2}}'
        stacks.append(value)
        for n in [1,9,10,11]:states[n]['inventory'][9+i]['item']=value
        for n in range(2,9):states[n]['cells'][i]['item']=value
    for n in range(3,12):states[n]['client']['bounds'].update(x=28,y=36)
    states[4]['following']=states[4]['client']['following']=True
    for n in [6,7]:
        states[n]['patrolling']=states[n]['client']['patrolling']=True;states[n]['patrolPos']='BlockPos{x=8,y=65,z=8}'
    for n in range(7,12):states[n]['client']['session']=18
    diamond='{id:"minecraft:diamond",'+('Count:3b'if target.startswith('1.20.1/')else'count:3')+'}'
    states[10]['inventory'][20]['item']=diamond;states[11]['cursors'][0]['item']=diamond
    journal=dict(complete=True,cases=[m.CASE],driver_sha256=DRIVER,checks=[dict(name=n,passed=True)for n in m.NAMES])
    removed=dict(menus=[dict(type='player.Menu',session=0)])
    return journal,states,removed


class GuardEvidence(unittest.TestCase):
    def accepted(self,j,s,r):return m.validate('1.21.1/neoforge',j,s,r,DRIVER)
    def rejected(self,change):
        j,s,r=fixture();change(j,s,r)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):self.accepted(j,s,r)
    def test_all_nine_reviewed_native_targets_and_typed_layouts(self):
        for target in m.TARGETS:
            with self.subTest(target=target):
                j,s,r=fixture(target)
                self.assertEqual(m.validate(target,j,s,r,DRIVER)['assertions'],13)
                self.assertEqual(m.specification(target,m.CASE,[m.native_classes(target)[1]]),m.native_classes(target))
    def test_missing_duplicate_or_nonboolean_original_assertions_rejected(self):
        self.rejected(lambda j,s,r:j['checks'].pop())
        self.rejected(lambda j,s,r:j['checks'].append(copy.deepcopy(j['checks'][0])))
        self.rejected(lambda j,s,r:j['checks'][0].update(passed=1))
        self.rejected(lambda j,s,r:j.update(complete=1))
        self.rejected(lambda j,s,r:j.update(driver_sha256='b'*64))
    def test_native_surface_and_complete_state_witnesses_required(self):
        self.rejected(lambda j,s,r:s.pop())
        self.rejected(lambda j,s,r:s[0]['client'].update(screen='another.Screen'))
        self.rejected(lambda j,s,r:s[0]['client'].update(menu='another.Menu'))
        self.rejected(lambda j,s,r:s[0]['client'].update(session=True))
    def test_full_equipment_metadata_and_player_consumption_required(self):
        self.rejected(lambda j,s,r:s[2]['cells'][0].update(item=s[2]['cells'][0]['item'].replace('[B;2b,8b]','[I;2,8]')))
        self.rejected(lambda j,s,r:s[2]['inventory'][9].update(item=s[1]['inventory'][9]['item']))
        self.rejected(lambda j,s,r:s[5]['inventory'][15].update(item='{id:"minecraft:stone",count:1}'))
        self.rejected(lambda j,s,r:s[0]['cells'][0].update(index=True))
    def test_behavior_must_change_on_server_and_restore(self):
        self.rejected(lambda j,s,r:s[4].update(following=False))
        def unchanged(j,s,r):s[4]['following']=s[4]['client']['following']=False
        self.rejected(unchanged)
        self.rejected(lambda j,s,r:s[7].update(patrolling=False))
        self.rejected(lambda j,s,r:s[6].update(patrolPos='null'))
        def unrestored(j,s,r):s[8]['patrolling']=s[8]['client']['patrolling']=True
        self.rejected(unrestored)
    def test_native_click_controls_and_moved_geometry_required(self):
        self.rejected(lambda j,s,r:s[3]['client']['buttons'][1].update(active=False))
        self.rejected(lambda j,s,r:s[5]['client']['buttons'][0].update(visible=False))
        self.rejected(lambda j,s,r:s[3]['client'].update(bounds=copy.deepcopy(s[2]['client']['bounds'])))
        self.rejected(lambda j,s,r:s[3]['client']['bounds'].update(x=True))
    def test_reopened_and_returned_items_must_preserve_full_data(self):
        self.rejected(lambda j,s,r:s[7]['cells'][2].update(item='{}'))
        self.rejected(lambda j,s,r:s[9]['inventory'][12].update(item='{}'))
        self.rejected(lambda j,s,r:s[8]['cursors'][0].update(item=s[1]['inventory'][9]['item']))
        self.rejected(lambda j,s,r:s[8].update(cursors=[]))
    def test_refusal_and_removed_source_cannot_be_inferred_from_green_flags(self):
        self.rejected(lambda j,s,r:s[11]['cells'][0].update(item=s[10]['inventory'][20]['item']))
        self.rejected(lambda j,s,r:s[11]['cursors'][0].update(item='{}'))
        self.rejected(lambda j,s,r:s[11]['cursors'][0].update(item='{id:"minecraft:diamond",count:2}'))
        self.rejected(lambda j,s,r:r['menus'].append(dict(type=m.native_classes('1.21.1/neoforge')[0],session=18)))
    def test_unreviewed_target_alias_class_and_case_rejected(self):
        for target,case,classes in [('26.3/fabric',m.CASE,['other.Screen']),('1.21.1/neoforge','another-case',[m.native_classes('1.21.1/neoforge')[1]]),('1.21.1/neoforge',m.CASE,['other.Screen'])]:
            with self.subTest(target=target,case=case):
                with self.assertRaises(AssertionError):m.specification(target,case,classes)


if __name__=='__main__':unittest.main(verbosity=2)
