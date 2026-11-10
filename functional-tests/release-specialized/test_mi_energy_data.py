import copy,unittest
from mi_energy_features import seeded,synchronized


class NativeEnergyStatus(unittest.TestCase):
    def test_exact_server_and_client_status_required(self):
        state=dict(fixture='{storedEu:1000L}');client=dict(energy=[dict(eu=1000,maxEu=2048)])
        self.assertTrue(synchronized(state,client,1000))
        for changed in [dict(energy=[]),dict(energy=[dict(eu=True,maxEu=2048)]),dict(energy=[dict(eu=999,maxEu=2048)]),dict(energy=[dict(eu=1000,maxEu=999)])]:
            self.assertFalse(synchronized(state,changed,1000))
        self.assertFalse(synchronized(dict(fixture='{storedEu:1000}'),client,1000))

    def test_empty_or_wrong_typed_foreign_fixture_is_refused(self):
        for mc,item in [('1.20.1','{id:"minecraft:diamond",Count:7b,tag:{release_mi_status_marker:41,typed:[B;3b,6b]}}'),('1.21.1','{id:"minecraft:diamond",count:7,components:{"minecraft:custom_data":{release_mi_status_marker:41,typed:[B;3b,6b]}}}')]:
            data=dict(inventory=[dict(item='{}')for _ in range(13)]);data['inventory'][12]['item']=item
            self.assertTrue(seeded(data,mc));data['inventory'][12]['item']=item.replace('[B;3b,6b]','[3,6]')
            self.assertFalse(seeded(data,mc));data['inventory'][12]['item']='{}';self.assertFalse(seeded(data,mc))


if __name__=='__main__':unittest.main()
