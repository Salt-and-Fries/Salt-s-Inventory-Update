"""The analyzer oracle must reject missing, stale and duplicated pool widgets."""
import unittest
from analyzer_features import expected_pools,Analyzer


class AnalyzerData(unittest.TestCase):
    def test_exact_current_mode_pool_set_is_required(self):
        client=dict(mode='OBJ',expectedPools={'OBJ':['a','b'],'REW':['c']},pools=[dict(id='b'),dict(id='a')])
        self.assertTrue(expected_pools(client))
        for bad in [[],[dict(id='a')],[dict(id='a'),dict(id='a')],[dict(id='c')]]:
            self.assertFalse(expected_pools({**client,'pools':bad}))

    def test_empty_decree_analysis_is_not_acceptance(self):
        self.assertFalse(expected_pools(dict(mode='OBJ',expectedPools={'OBJ':[]},pools=[])))

    def test_full_decree_component_and_numeric_types_are_preserved(self):
        before=dict(inventory=[dict(item='{id:"bountiful:decree",count:1,components:{"bountiful:decree_data":{ids:["a"]},"minecraft:custom_data":{marker:7b}}}')])
        for old,new in [('marker:7b','marker:7'),('ids:["a"]','ids:["b"]'),('count:1','count:2')]:
            after=dict(inventory=[dict(item=before['inventory'][0]['item'].replace(old,new))])
            self.assertNotEqual(Analyzer.inventory(before),Analyzer.inventory(after))


if __name__=='__main__':unittest.main()
