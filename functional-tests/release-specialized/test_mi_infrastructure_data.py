import copy,unittest
from mi_infrastructure_features import tank_approach,tank_matches


class NativeTankInfrastructure(unittest.TestCase):
    def fixture(self,unit):
        capacity=27*64*unit
        return dict(shapeValid=True,fixture='{amount:'+str(unit)+'L,capacity:'+str(capacity)+'L}',client=dict(components=dict(tank=dict(fluid='minecraft:water',amount=unit,capacity=capacity))))

    def test_real_formation_and_exact_native_capacity_required(self):
        for unit in [81000,1000]:
            valid=self.fixture(unit);self.assertTrue(tank_matches(valid,unit,unit))
            changed=copy.deepcopy(valid);changed['shapeValid']=False;self.assertFalse(tank_matches(changed,unit,unit))
            changed=copy.deepcopy(valid);changed['client']['components']['tank']['capacity']-=1;self.assertFalse(tank_matches(changed,unit,unit))
            changed=copy.deepcopy(valid);changed['client']['components']['tank']['fluid']='minecraft:lava';self.assertFalse(tank_matches(changed,unit,unit))
            changed=copy.deepcopy(valid);changed['fixture']=changed['fixture'].replace(str(unit)+'L',str(unit),1);self.assertFalse(tank_matches(changed,unit,unit))

    def test_changed_or_boolean_native_display_amount_is_refused(self):
        valid=self.fixture(1000)
        for value in [True,999,1000.0]:
            changed=copy.deepcopy(valid);changed['client']['components']['tank']['amount']=value;self.assertFalse(tank_matches(changed,1000,1000))

    def test_native_blueprint_determines_unobstructed_exterior_approach(self):
        for low,high,expected in [(6,8,'tp @a 8.5 65 11.5 180 21'),(8,10,'tp @a 8.5 65 5.5 0 21')]:
            blueprint=[dict(x=x,y=65,z=z)for x in [7,8,9]for z in [low,high]if(x,z)!=(8,8)]
            self.assertEqual(tank_approach(blueprint),expected)
        with self.assertRaises(AssertionError):tank_approach([dict(x=x,z=z)for x in [7,9]for z in [7,9]])


if __name__=='__main__':unittest.main()
