"""Negative controls for the selector's no-physical-movement assertions."""
import copy, unittest
from wands_features import Tools


class ToolSelectorData(unittest.TestCase):
    def test_empty_native_cursor_is_accepted_but_real_stack_is_rejected(self):
        self.assertTrue(Tools.empty_cursors(dict(cursors=[dict(item='{}')])) )
        self.assertFalse(Tools.empty_cursors(dict(cursors=[dict(item='{id:"minecraft:diamond_pickaxe",Count:1b}')])) )

    def test_changes_to_tool_wear_or_typed_data_are_not_selection(self):
        value=dict(inventory=[dict(item='{id:"minecraft:diamond_pickaxe",Count:1b,tag:{Damage:2,marker:7b}}')])
        for old,new in [('Damage:2','Damage:3'),('marker:7b','marker:7'),('Count:1b','Count:2b')]:
            other=copy.deepcopy(value);other['inventory'][0]['item']=other['inventory'][0]['item'].replace(old,new)
            with self.subTest(change=(old,new)):self.assertNotEqual(Tools.inventory(value),Tools.inventory(other))

    def test_modern_custom_data_and_components_remain_in_the_oracle(self):
        value=dict(inventory=[dict(item='{id:"minecraft:diamond_shovel",count:1,components:{"minecraft:damage":2,"minecraft:custom_data":{typed:[B;3b,7b]}}}')])
        other=copy.deepcopy(value);other['inventory'][0]['item']=other['inventory'][0]['item'].replace('3b,7b','3b,8b')
        self.assertNotEqual(Tools.inventory(value),Tools.inventory(other))


if __name__=='__main__':unittest.main()
