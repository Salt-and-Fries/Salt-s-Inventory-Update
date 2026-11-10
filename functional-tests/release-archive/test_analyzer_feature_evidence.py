"""Offline mutation tests for Analyzer evidence, not native gameplay passes."""
from pathlib import Path
import copy,sys,unittest
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
import focused_release_evidence_analyzer as m
DRIVER='a'*64


def fixture():
    decree='{id:"bountiful:decree",count:1,components:{"bountiful:decree_data":{ids:["builder"]},"minecraft:custom_data":{release_analyzer_marker:47,typed:[B;2b,9b]}}}'
    diamond='{id:"minecraft:diamond",count:3,components:{"minecraft:custom_data":{release_analyzer_marker:48}}}'
    base=dict(decree='{}',inventory=[dict(index=i,item=decree if i==9 else diamond if i==10 else'{}')for i in range(41)],
        cursors=[dict(menu=m.MENU,item='{}')],
        client=dict(menu=m.MENU,screen=m.SCREEN,session=17,locked=False,decree='{}',mode='OBJ',resolution=4,
            pools=[],expectedPools={'OBJ':[],'REW':[]},decreeIds=[],bounds=dict(x=12,y=24,width=180,height=110)))
    states=[copy.deepcopy(base)for _ in m.LABELS]
    for n in [1,2,3,4,5,8]:
        s=states[n];s['decree']=s['client']['decree']=decree;s['inventory'][9]['item']='{}'
        s['client']['decreeIds']=['builder'];s['client']['expectedPools']={'OBJ':['objective:a','objective:b'],'REW':['reward:a']}
        s['client']['mode']='REW'if n in [2,3]else'OBJ';s['client']['resolution']=8 if n in [5,8]else 4
        s['client']['pools']=[dict(id=p,pixSize=s['client']['resolution'])for p in s['client']['expectedPools'][s['client']['mode']]]
    for n in range(3,9):states[n]['client']['bounds'].update(x=28,y=36)
    states[7]['inventory'][10]['item']='{}';states[7]['cursors'][0]['item']=diamond
    states[9]['inventory'][9]['item']='{}';states[9]['inventory'][11]['item']=decree;states[9]['client']['session']=18
    journal=dict(complete=True,cases=[m.CASE],driver_sha256=DRIVER,checks=[dict(name=n,passed=True)for n in m.NAMES])
    return journal,states


class AnalyzerEvidence(unittest.TestCase):
    def accepted(self,j,s):return m.validate('1.21.1/neoforge',j,s,DRIVER)
    def rejected(self,change):
        j,s=fixture();change(j,s)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):self.accepted(j,s)
    def test_both_exact_native_targets_and_arbitrary_return_cell_are_valid(self):
        for target in m.TARGETS:
            j,s=fixture();self.assertEqual(m.validate(target,j,s,DRIVER),dict(assertions=10,states=10,native_variants=1,contract='native-decree-analysis-not-bounty-generation'))
            self.assertEqual(m.inventory(s[0])[9],m.inventory(s[9])[11])
    def test_missing_duplicate_nonboolean_or_changed_original_checks_are_rejected(self):
        self.rejected(lambda j,s:j['checks'].pop())
        self.rejected(lambda j,s:j['checks'].append(copy.deepcopy(j['checks'][0])))
        self.rejected(lambda j,s:j['checks'][0].update(passed=1))
        self.rejected(lambda j,s:j.update(complete=1))
        self.rejected(lambda j,s:j.update(driver_sha256='b'*64))
    def test_exact_native_menu_screen_sessions_and_all_frames_are_required(self):
        self.rejected(lambda j,s:s.pop())
        self.rejected(lambda j,s:s[0]['client'].update(menu='another.Menu'))
        self.rejected(lambda j,s:s[0]['client'].update(screen='another.Screen'))
        self.rejected(lambda j,s:s[0]['client'].update(session=True))
    def test_pool_refresh_requires_independent_nonempty_exact_current_mode_set(self):
        self.rejected(lambda j,s:s[1]['client'].update(pools=[]))
        self.rejected(lambda j,s:s[2]['client']['pools'].append(copy.deepcopy(s[2]['client']['pools'][0])))
        self.rejected(lambda j,s:s[1]['client'].update(pools=[dict(id='stale:pool',pixSize=4)]))
        self.rejected(lambda j,s:s[1]['client'].update(decreeIds=[]))
    def test_mode_moved_input_and_native_integer_resolution_are_required(self):
        self.rejected(lambda j,s:s[3]['client'].update(bounds=copy.deepcopy(s[2]['client']['bounds'])))
        self.rejected(lambda j,s:s[2]['client'].update(mode='OBJ'))
        self.rejected(lambda j,s:s[5]['client'].update(resolution=4))
        self.rejected(lambda j,s:s[5]['client']['pools'][0].update(pixSize=4))
        self.rejected(lambda j,s:s[5]['client'].update(resolution=8.0))
    def test_decree_typed_data_player_consumption_and_client_server_sync_required(self):
        self.rejected(lambda j,s:s[1].update(decree=s[1]['decree'].replace('[B;2b,9b]','[I;2,9]')))
        self.rejected(lambda j,s:s[1]['inventory'][9].update(item=s[0]['inventory'][9]['item']))
        self.rejected(lambda j,s:s[5]['inventory'][15].update(item='{id:"minecraft:stone",count:1}'))
        self.rejected(lambda j,s:s[6]['client'].update(decree=s[0]['inventory'][9]['item']))
    def test_foreign_refusal_and_native_close_return_cannot_lose_or_duplicate_items(self):
        self.rejected(lambda j,s:s[7]['cursors'][0].update(item='{}'))
        self.rejected(lambda j,s:s[7]['inventory'][10].update(item=s[0]['inventory'][10]['item']))
        self.rejected(lambda j,s:s[9]['inventory'][11].update(item='{}'))
        self.rejected(lambda j,s:s[9]['inventory'][12].update(item=s[0]['inventory'][9]['item']))
    def test_unreviewed_target_alias_and_original_class_are_rejected(self):
        for target,case,classes in [('1.20.1/forge',m.CASE,[m.SCREEN]),('1.21.1/fabric','another-case',[m.SCREEN]),('1.21.1/fabric',m.CASE,['another.Screen'])]:
            with self.subTest(target=target,case=case):
                with self.assertRaises(AssertionError):m.specification(target,case,classes)


if __name__=='__main__':unittest.main(verbosity=2)
