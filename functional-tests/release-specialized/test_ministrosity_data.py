"""Negative controls for the companion's full cargo and inventory ledger."""
import copy,unittest
from ministrosity_features import Ministrosity


class CompanionData(unittest.TestCase):
    def test_typed_marker_and_count_changes_are_detected(self):
        before=dict(cells=[dict(item='{id:"minecraft:stone",Count:15b,tag:{marker:7b}}')])
        for old,new in [('15b','14b'),('marker:7b','marker:7'),('minecraft:stone','minecraft:cobblestone')]:
            after=copy.deepcopy(before);after['cells'][0]['item']=after['cells'][0]['item'].replace(old,new)
            self.assertNotEqual(Ministrosity.cargo(before),Ministrosity.cargo(after))

    def test_reopen_requires_original_cell_positions(self):
        cells=[dict(item='{id:"minecraft:stone",count:22}'),dict(item='{id:"minecraft:diamond",count:3}')]
        self.assertNotEqual(Ministrosity.cargo(dict(cells=cells)),Ministrosity.cargo(dict(cells=list(reversed(cells)))))

    def test_unrelated_player_data_cannot_change_during_cargo_operations(self):
        before=dict(inventory=[dict(item='{id:"minecraft:black_shulker_box",count:1,components:{"minecraft:custom_data":{typed:[B;1b,7b]}}}')])
        after=copy.deepcopy(before);after['inventory'][0]['item']=after['inventory'][0]['item'].replace('1b,7b','1b,8b')
        self.assertNotEqual(Ministrosity.inventory(before),Ministrosity.inventory(after))


if __name__=='__main__':unittest.main()
