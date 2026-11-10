import unittest
from fixture_isolation import commands,approach,water_walkway

class FixtureIsolationTests(unittest.TestCase):
    def test_dry_walkway_stays_outside_the_entire_native_water_pool(self):
        for origin in ([8,65,8],[12,-60,10]):
            x,y,z=origin;words=water_walkway(origin)[0].split()
            self.assertEqual('fill',words[0]);self.assertEqual('minecraft:stone',words[-1])
            low=list(map(int,words[1:4]));high=list(map(int,words[4:7]))
            self.assertGreater(low[2],z+1)
            self.assertEqual(y,low[1]);self.assertEqual(y,high[1])
            tp=water_walkway(origin)[1].split();pos=list(map(float,tp[2:5]))
            self.assertTrue(all(a<=v<=b+1 for a,v,b in zip(low,pos,high)))
            self.assertEqual(y+1,pos[1])
            self.assertLess(sum((v-c-.5)**2 for v,c in zip(pos,origin)),16)
    def test_clearance_includes_old_water_and_player_approach_with_a_floor(self):
        self.assertEqual(['fill -2 65 -2 18 73 18 minecraft:air',
                          'fill -2 64 -2 18 64 18 minecraft:stone'],commands([8,65,8]))
    def test_translated_negative_origin_is_used_without_fixed_world_coordinates(self):
        self.assertEqual('fill -14 -1998 -17 6 -1990 3 minecraft:air',commands([-4,-1998,-7])[0])
    def test_both_operations_remain_within_the_native_fill_limit(self):
        for command in commands([8,65,8]):
            words=command.split();a=list(map(int,words[1:4]));b=list(map(int,words[4:7]));volume=1
            for start,end in zip(a,b):volume*=end-start+1
            self.assertLessEqual(volume,32768)
    def test_cleanup_has_no_inventory_menu_or_entity_mutation(self):
        self.assertTrue(all(c.startswith('fill ') and c.endswith(('minecraft:air','minecraft:stone')) for c in commands([8,65,8])))
    def test_native_block_approach_uses_actual_negative_height_origin(self):
        self.assertEqual('tp @a 10.5 -59 10.5 -90 46',approach([12,-60,10]))
    def test_native_approach_remains_within_ordinary_block_reach(self):
        origin=[12,-60,10];words=approach(origin).split();position=list(map(float,words[2:5]))
        self.assertLess(sum((p-b-.5)**2 for p,b in zip(position,origin)),16)

if __name__=='__main__':unittest.main()
