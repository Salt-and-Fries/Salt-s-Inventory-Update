"""Reject invalid native blueprint allocation and fabricated active status."""
import copy,unittest
from mi_multiblock_features import select_hatches,active_status,HATCHES


class NativeMultiblockData(unittest.TestCase):
    def test_hatch_positions_are_distinct_and_native_allowed(self):
        blueprint=[dict(x=i,y=65,z=8,block='minecraft:bricks',allows=['modern_industrialization:'+k.lower()for k in HATCHES])for i in range(4)]
        selected=select_hatches(blueprint)
        self.assertEqual(len({v['x']for v in selected}),4)
        self.assertEqual([v['kind']for v in selected],HATCHES)
        self.assertTrue(all(v['block'].endswith(v['kind'].lower()+'_hatch')for v in selected))
        invalid=copy.deepcopy(blueprint)
        for v in invalid:v['allows'].remove('modern_industrialization:fluid_output')
        with self.assertRaises(AssertionError):select_hatches(invalid)
        with self.assertRaises(AssertionError):select_hatches(blueprint[:3])
    def test_both_native_sides_need_formed_active_progress(self):
        probe=dict(shapeValid=True,active=True,progress=.2,client=dict(shapeValid=True,active=True,progress=.18))
        self.assertTrue(active_status(probe))
        for field,value in [('shapeValid',False),('active',False),('progress',0),('progress',float('nan')),('progress',True)]:
            bad=copy.deepcopy(probe);bad[field]=value;self.assertFalse(active_status(bad))
            bad=copy.deepcopy(probe);bad['client'][field]=value;self.assertFalse(active_status(bad))


if __name__=='__main__':unittest.main()
