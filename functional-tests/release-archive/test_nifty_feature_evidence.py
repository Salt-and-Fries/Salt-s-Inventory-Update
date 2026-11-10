"""Offline rejection of corrupted cart evidence, not cart gameplay tests."""
from pathlib import Path
import copy,sys,unittest
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'tools'))
import focused_release_evidence_nifty as m
DRIVER='a'*64


def fixture(target='1.21.1/fabric'):
    def stack(identifier,count,marker,damage=None):
        data='{release_cart_marker:'+str(marker)+',typed:[B;2b,6b]}'
        if target.startswith('1.20.1/'):
            return '{id:"'+identifier+'",Count:'+str(count)+'b,tag:'+data[:-1]+(',Damage:'+str(damage)if damage is not None else'')+'}}'
        return '{id:"'+identifier+'",count:'+str(count)+',components:{"minecraft:custom_data":'+data+(',"minecraft:damage":'+str(damage)if damage is not None else'')+'}}'
    states={}
    for _,(kind,variant,size,identifier)in m.CASES.items():
        menu,screen=m.classes(variant)
        window=dict(menu=menu,nativeScreen=screen,sessionId=17,bounds=dict(x=12,y=24,width=100,height=100),
            slots=[dict(index=i,x=9+18*(i%3),y=19+18*(i//3),player=False)for i in range(size)],
            niftyInputRegions=[dict(interactive=True,x=16,y=50,source=dict(x=8,y=18,width=54,height=((size+2)//3)*18))])
        base=dict(client=dict(allWindows=[window]),server=dict(inventory=[dict(index=i,item='{}')for i in range(41)],
            menus=[dict(**{'class':menu},slots=[dict(slot=i,item='{}')for i in range(size)],carried='{}')]))
        frames=[copy.deepcopy(base)for _ in m.FRAMES[kind]]
        for i in range(size):
            value=stack(identifier,1 if kind=='plow'else i+1,42+i,1 if kind=='plow'else None)
            for n in range(len(frames)):
                if n in [1,2]:frames[n]['server']['menus'][0]['slots'][i]['item']=value
                else:frames[n]['server']['inventory'][9+i]['item']=value
        for n in range(2,len(frames)):frames[n]['client']['allWindows'][0]['sessionId']=18
        if kind=='seed_drill':
            diamond=stack('minecraft:diamond',3,99)
            frames[4]['server']['inventory'][20]['item']=diamond;frames[5]['server']['menus'][0]['carried']=diamond
        states[kind]=frames
    journal=dict(complete=True,cases=['plow','seed_drill'],driver_sha256=DRIVER,checks=[dict(name=n,passed=True)for n in m.NAMES])
    return journal,states


class NiftyEvidence(unittest.TestCase):
    def accepted(self,j,s):return m.validate('1.21.1/fabric',j,s,DRIVER)
    def rejected(self,change):
        j,s=fixture();change(j,s)
        with self.assertRaises((AssertionError,KeyError,ValueError,TypeError)):self.accepted(j,s)
    def test_all_three_native_targets_keep_original_fifteen_checks_and_ten_states(self):
        for target in m.TARGETS:
            j,s=fixture(target)
            self.assertEqual(m.validate(target,j,s,DRIVER),dict(assertions=15,states=10,native_variants=2,contract='native-cart-equipment-not-driving-or-agriculture'))
    def test_missing_duplicate_nonboolean_or_changed_driver_assertions_rejected(self):
        self.rejected(lambda j,s:j['checks'].pop())
        self.rejected(lambda j,s:j['checks'].append(copy.deepcopy(j['checks'][0])))
        self.rejected(lambda j,s:j['checks'][0].update(passed=1))
        self.rejected(lambda j,s:j.update(complete=1))
        self.rejected(lambda j,s:j.update(driver_sha256='b'*64))
    def test_native_variant_session_and_complete_cell_count_are_required(self):
        self.rejected(lambda j,s:s['plow'][0]['client']['allWindows'][0].update(nativeScreen='another.Screen'))
        self.rejected(lambda j,s:s['seed_drill'][1]['client']['allWindows'][0]['slots'].pop())
        self.rejected(lambda j,s:s['plow'][0]['client']['allWindows'][0].update(sessionId=True))
        self.rejected(lambda j,s:s['seed_drill'].pop())
    def test_complete_interactive_slot_rim_geometry_is_required(self):
        self.rejected(lambda j,s:s['plow'][0]['client']['allWindows'][0]['niftyInputRegions'][0]['source'].update(height=17))
        self.rejected(lambda j,s:s['plow'][0]['client']['allWindows'][0]['niftyInputRegions'][0].update(interactive=False))
        self.rejected(lambda j,s:s['plow'][1]['client']['allWindows'][0]['bounds'].update(x=13))
        self.rejected(lambda j,s:s['plow'][0]['client']['allWindows'][0]['slots'][0].update(index=True))
    def test_full_typed_equipment_and_player_consumption_are_required(self):
        self.rejected(lambda j,s:s['plow'][1]['server']['menus'][0]['slots'][0].update(item=s['plow'][0]['server']['inventory'][9]['item'].replace('[B;2b,6b]','[I;2,6]')))
        self.rejected(lambda j,s:s['seed_drill'][1]['server']['inventory'][9].update(item=s['seed_drill'][0]['server']['inventory'][9]['item']))
        self.rejected(lambda j,s:s['seed_drill'][2]['server']['inventory'][25].update(item='{id:"minecraft:stone",count:1}'))
    def test_reopening_and_full_item_recovery_cannot_be_inferred(self):
        self.rejected(lambda j,s:s['plow'][2]['server']['menus'][0]['slots'][2].update(item='{}'))
        self.rejected(lambda j,s:s['seed_drill'][3]['server']['inventory'][17].update(item='{}'))
        self.rejected(lambda j,s:s['plow'][3]['server']['menus'][0].update(carried=s['plow'][0]['server']['inventory'][9]['item']))
    def test_foreign_seed_refusal_keeps_exact_cursor_and_full_inventory(self):
        self.rejected(lambda j,s:s['seed_drill'][5]['server']['menus'][0].update(carried='{}'))
        self.rejected(lambda j,s:s['seed_drill'][5]['server']['menus'][0]['slots'][0].update(item=s['seed_drill'][4]['server']['inventory'][20]['item']))
        self.rejected(lambda j,s:s['seed_drill'][5]['server']['inventory'][20].update(item=s['seed_drill'][4]['server']['inventory'][20]['item']))
    def test_only_reviewed_native_target_and_case_aliases_are_accepted(self):
        for target in m.TARGETS:
            for case,(_,variant,_,_)in m.CASES.items():self.assertEqual(m.specification(target,case,[m.classes(variant)[1]])[1],variant)
        for target,case,classes in [('1.21.1/neoforge','niftycarts-plow',['other.Screen']),('1.21.1/fabric','another-case',['other.Screen']),('1.21.1/fabric','niftycarts-plow',['other.Screen'])]:
            with self.subTest(target=target,case=case):
                with self.assertRaises(AssertionError):m.specification(target,case,classes)


if __name__=='__main__':unittest.main(verbosity=2)
