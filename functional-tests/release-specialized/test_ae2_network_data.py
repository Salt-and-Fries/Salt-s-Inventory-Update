"""Reject false status synchronization and fabricated/empty export acceptance."""
import copy,json,tempfile,unittest,zipfile
from pathlib import Path
from ae2_network_features import valid_status,topology,drive_count,exported_grid,DOUBLES


def example():
    values={k:dict(javaType='java.lang.Double',value=10.0)for k in DOUBLES}
    values['getChannelsUsed']=dict(javaType='java.lang.Integer',value=2)
    values['groups']=[dict(item='ae2:drive',display=dict(count=1,signature='{id:"ae2:drive",count:1}'),
        getCount=dict(javaType='java.lang.Integer',value=2),isMissingChannel=dict(javaType='java.lang.Boolean',value=False),
        getIdlePowerUsage=dict(javaType='java.lang.Double',value=1.0),getPowerGenerationCapacity=dict(javaType='java.lang.Double',value=0.0))]
    return values


class NativeNetworkData(unittest.TestCase):
    def test_actual_status_types_bounds_and_distinct_native_groups(self):
        p=example();self.assertTrue(valid_status(p));self.assertEqual(drive_count(p),2)
        for key,value in [('value',float('inf')),('value',float('nan')),('value',-1),('value',True),('javaType','java.lang.Float')]:
            bad=copy.deepcopy(p);bad['getAveragePowerUsage'][key]=value;self.assertFalse(valid_status(bad),(key,value))
        for key,value in [('value',2.0),('value',True),('value',-1),('javaType','java.lang.Long')]:
            bad=copy.deepcopy(p);bad['getChannelsUsed'][key]=value;self.assertFalse(valid_status(bad),(key,value))
        for change in ['no-groups','duplicate','count','boolean','stored','metadata']:
            bad=copy.deepcopy(p)
            if change=='no-groups':bad['groups']=[]
            if change=='duplicate':bad['groups'].append(copy.deepcopy(bad['groups'][0]))
            if change=='count':bad['groups'][0]['getCount']['value']=False
            if change=='boolean':bad['groups'][0]['isMissingChannel']['value']=0
            if change=='stored':bad['getStoredPower']['value']=11
            if change=='metadata':bad['groups'][0]['display']['count']=0
            self.assertFalse(valid_status(bad),change)
    def test_topology_is_exact_while_power_averages_can_change_over_ticks(self):
        original=example();later=copy.deepcopy(original);later['getAveragePowerUsage']['value']=7.5
        self.assertTrue(valid_status(later));self.assertEqual(topology(original),topology(later))
        for change in ['devices','channel','missing','ghost','idle']:
            bad=copy.deepcopy(original)
            if change=='devices':bad['groups'][0]['getCount']['value']=3
            if change=='channel':bad['getChannelsUsed']['value']=3
            if change=='missing':bad['groups'][0]['isMissingChannel']['value']=True
            if change=='ghost':bad['groups'][0]['display']['signature']='{id:"ae2:drive",count:1,components:{"minecraft:custom_data":{typed:[B;1b]}}}'
            if change=='idle':bad['groups'][0]['getIdlePowerUsage']['value']=2.0
            self.assertNotEqual(topology(original),topology(bad),change)
    def test_native_export_requires_exact_grid_and_populated_payload(self):
        with tempfile.TemporaryDirectory()as folder:
            path=Path(folder)/'native.zip';value=dict(id=9,disposed=False,nodes=[{}, {}, {}, {}],machines=[{}, {}, {}, {}])
            def write(data):
                with zipfile.ZipFile(path,'w')as archive:archive.writestr('grid.json',json.dumps(data))
            write(value);self.assertEqual(exported_grid(path,9)['grid_serial'],9)
            for key,data in [('id',10),('disposed',True),('nodes',[]),('machines',[])]:
                bad=copy.deepcopy(value);bad[key]=data;write(bad)
                with self.assertRaises(AssertionError,msg=key):exported_grid(path,9)
            write(value)
            with self.assertRaises(AssertionError):exported_grid(path,8)


if __name__=='__main__':unittest.main()
