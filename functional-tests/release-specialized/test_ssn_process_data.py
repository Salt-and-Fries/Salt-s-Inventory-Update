import copy,unittest
from ssn_process_features import changed_only,next_face,exact_ghost


class ProcessDataTests(unittest.TestCase):
    def test_native_face_order_wraps(self):
        self.assertEqual('UP',next_face('DOWN'));self.assertEqual('DOWN',next_face('EAST'))
    def test_exact_single_setting_and_client_agreement(self):
        before=dict(settings=dict(count=2,always=False,inputFace='NORTH'))
        after=dict(settings=dict(count=3,always=False,inputFace='NORTH'),client=dict(settings=dict(count=3,always=False,inputFace='NORTH')))
        self.assertTrue(changed_only(before,after,'count',3))
        changed=copy.deepcopy(after);changed['settings']['always']=True
        self.assertFalse(changed_only(before,changed,'count',3))
        changed=copy.deepcopy(after);changed['client']['settings']['count']=2
        self.assertFalse(changed_only(before,changed,'count',3))
    def test_wrong_metadata_or_extra_ghost_fails(self):
        item='{id:"minecraft:iron_ingot",count:16,components:{"minecraft:custom_data":{flag:1b}}}'
        ghost=item.replace('count:16','count:2');state=dict(settings=dict(getFilters=[ghost,'{}']))
        self.assertTrue(exact_ghost(state,'getFilters',item,2))
        state['settings']['getFilters'][0]=ghost.replace('flag:1b','flag:1')
        self.assertFalse(exact_ghost(state,'getFilters',item,2))
        state['settings']['getFilters']=[ghost,ghost]
        self.assertFalse(exact_ghost(state,'getFilters',item,2))


if __name__=='__main__':unittest.main()
