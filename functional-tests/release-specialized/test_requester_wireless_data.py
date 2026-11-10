"""Only reviewed native energy debits may change the complete source item."""
import copy,unittest
from requester_wireless_features import ENERGY,native_energy,power_matches,source_parts


def source(energy=ENERGY):return dict(count=1,signature='{id:"merequester:wireless_requester_terminal",count:1,components:{"ae2:stored_energy":'+str(energy)+'d,"minecraft:custom_data":{typed:[B;1b,7b]},"ae2:wireless_link_target":{dimension:"minecraft:overworld",pos:[I;11,65,8]}}}')


def power():return dict(creative=False,quantumConnected=False,sameSource=True,sourceSlot=0,stored=99995.0,storedJavaType='java.lang.Double',drain=0.5,drainJavaType='java.lang.Double',remainingEnergyTicks=7,accessPoint=[11,65,8],position=[12.5,66.0,11.5],source=source(99995.0))


class RequesterWirelessPower(unittest.TestCase):
    def test_native_energy_requires_exact_ten_tick_debits_and_never_an_increase(self):
        self.assertEqual(native_energy(source(99995.0),source(),ENERGY,5.0),99995.0)
        self.assertEqual(native_energy(source(99990.0),source(),99995.0,5.0),99990.0)
        for value,previous,unit in [(99994.75,ENERGY,5.0),(99995.0,99990.0,5.0),(100001.0,ENERGY,5.0),(99995.0,True,5.0),(99995.0,ENERGY,0.0),(99995.0,ENERGY,2)]:self.assertIsNone(native_energy(source(value),source(),previous,unit))
    def test_every_other_persistent_component_and_numeric_type_remains_exact(self):
        for change in ['metadata','position','dimension','component','count']:
            item=source(99995.0)
            if change=='metadata':item['signature']=item['signature'].replace('[B;','[')
            if change=='position':item['signature']=item['signature'].replace('11,65,8','12,65,8')
            if change=='dimension':item['signature']=item['signature'].replace('minecraft:overworld','minecraft:the_nether')
            if change=='component':item['signature']=item['signature'].replace('"minecraft:custom_data":','"unrelated:lost_data":')
            if change=='count':item['count']=2
            if change=='count':
                with self.assertRaises(AssertionError):native_energy(item,source(),ENERGY,5.0)
            else:self.assertIsNone(native_energy(item,source(),ENERGY,5.0),change)
        for bad in ['0.0d','-1.0d','NaNd','99995.0f']:
            item=source(99995.0);item['signature']=item['signature'].replace('99995.0d',bad)
            with self.assertRaises((AssertionError,ValueError)):source_parts(item)
    def test_actual_survival_host_power_and_source_identity_cannot_be_substituted(self):
        p=power();self.assertTrue(power_matches(p,source(),ENERGY,5.0,[11,65,8],[12.5,66.0,11.5]))
        for change in ['creative','quantum','source','slot','stored','java','drain','position','point','ticks','ticks-type']:
            bad=copy.deepcopy(p)
            if change=='creative':bad['creative']=True
            if change=='quantum':bad['quantumConnected']=True
            if change=='source':bad['sameSource']=False
            if change=='slot':bad['sourceSlot']=True
            if change=='stored':bad['stored']=99990.0
            if change=='java':bad['storedJavaType']='java.lang.Float'
            if change=='drain':bad['drain']=0.6
            if change=='position':bad['position'][0]+=1
            if change=='point':bad['accessPoint'][0]+=1
            if change=='ticks':bad['remainingEnergyTicks']=12
            if change=='ticks-type':bad['remainingEnergyTicks']=True
            self.assertFalse(power_matches(bad,source(),ENERGY,5.0,[11,65,8],[12.5,66.0,11.5]),change)
    def test_observation_gap_allows_later_legal_debit_but_not_rebased_history(self):
        p=power();self.assertTrue(power_matches(p,source(),ENERGY,5.0,[11,65,8],[12.5,66.0,11.5]))
        self.assertEqual(native_energy(source(99990.0),source(),ENERGY,5.0),99990.0)
        self.assertFalse(power_matches(p,source(),99990.0,5.0,[11,65,8],[12.5,66.0,11.5]))
        self.assertTrue(power_matches(p,source(),ENERGY,5.0,[11,65,8],[12.5,66.0,11.5]))


if __name__=='__main__':unittest.main()
