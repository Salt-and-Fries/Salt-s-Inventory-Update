"""Keep native ghost configuration, identity and physical buffer checks strict."""
import copy,unittest
from requester_features import config,matches,no_buffered_items,synchronized


def item():return dict(count=1,signature='{id:"minecraft:stone",count:1,components:{"minecraft:custom_data":{typed:[B;3b,8b]}}}')


def request(index=0,key=True,enabled=False,amount=39,batch=9):
    return dict(empty=False,index=index,enabled=enabled,key=dict(empty=False,item=item())if key else dict(empty=True),amount=amount,amountJavaType='java.lang.Long',batch=batch,batchJavaType='java.lang.Long',status='IDLE')


def native():return dict(requests=[request(),request(1,False,True,0,1)],buffers=[])


class NativeRequesterData(unittest.TestCase):
    def test_request_key_flags_numbers_and_other_ghosts_require_exact_native_data(self):
        good=native();self.assertTrue(matches(good,item(),39,9,False))
        for change in ['metadata','amount','batch','enabled','extra','key-flag','number-type','java']:
            bad=copy.deepcopy(good)
            if change=='metadata':bad['requests'][0]['key']['item']['signature']=item()['signature'].replace('[B;3b,8b]','[3b,8b]')
            if change=='amount':bad['requests'][0]['amount']=38
            if change=='batch':bad['requests'][0]['batch']=8
            if change=='enabled':bad['requests'][0]['enabled']=True
            if change=='extra':bad['requests'][1]=request(1)
            if change=='key-flag':bad['requests'][0]['key']['empty']=0
            if change=='number-type':bad['requests'][0]['amount']=39.0
            if change=='java':bad['requests'][0]['batchJavaType']='java.lang.Integer'
            if change in ['key-flag','number-type','java']:
                with self.assertRaises(AssertionError,msg=change):matches(bad,item(),39,9,False)
            else:self.assertFalse(matches(bad,item(),39,9,False),change)
    def test_terminal_reference_and_server_tracker_must_refer_to_same_actual_manager(self):
        n=native();c=dict(references=[dict(id=7,requests=copy.deepcopy(n['requests']))]);s=dict(trackers=[dict(id=7,sameBackingManager=True,requests=copy.deepcopy(n['requests']),cache=copy.deepcopy(n['requests']))]);self.assertTrue(synchronized(n,c,s))
        for change in ['id','identity','host','cache','client','duplicate','index']:
            a=copy.deepcopy(c);b=copy.deepcopy(s);d=copy.deepcopy(n)
            if change=='id':b['trackers'][0]['id']=8
            if change=='identity':a['references'][0]['id']=7.0
            if change=='host':b['trackers'][0]['sameBackingManager']=False
            if change=='cache':b['trackers'][0]['cache'][0]['amount']=40
            if change=='client':a['references'][0]['requests'][0]['key']['item']['signature']=item()['signature'].replace('8b','9b')
            if change=='duplicate':a['references'].append(copy.deepcopy(a['references'][0]))
            if change=='index':d['requests'][1]['index']=3
            self.assertFalse(synchronized(d,a,b),change)
    def test_only_native_zero_physical_buffers_and_pending_amounts_are_settled(self):
        value=dict(buffers=[dict(empty=True),dict(empty=False,bufferAmount=dict(value=0,javaType='java.lang.Long'),pendingAmount=dict(value=0,javaType='java.lang.Long'))]);self.assertTrue(no_buffered_items(value))
        for change in ['buffer','pending','float','bool','java','empty']:
            bad=copy.deepcopy(value)
            if change=='buffer':bad['buffers'][1]['bufferAmount']['value']=1
            if change=='pending':bad['buffers'][1]['pendingAmount']['value']=1
            if change=='float':bad['buffers'][1]['bufferAmount']['value']=0.0
            if change=='bool':bad['buffers'][1]['pendingAmount']['value']=False
            if change=='java':bad['buffers'][1]['pendingAmount']['javaType']='java.lang.Integer'
            if change=='empty':bad['buffers'][0]['empty']=1
            self.assertFalse(no_buffered_items(bad),change)
    def test_transient_statuses_do_not_substitute_for_config_or_manufacture_identity(self):
        n=native();c=dict(references=[dict(id=7,requests=copy.deepcopy(n['requests']))]);s=dict(trackers=[dict(id=7,sameBackingManager=True,requests=copy.deepcopy(n['requests']),cache=copy.deepcopy(n['requests']))])
        c['references'][0]['requests'][0]['status']='MISSING';self.assertTrue(synchronized(n,c,s))
        c['references'][0]['requests'][0]['amount']=40;self.assertFalse(synchronized(n,c,s))
        for bad in [request(enabled=1),request(amount=True),request(batch=0)]:
            with self.assertRaises(AssertionError):config(bad)


if __name__=='__main__':unittest.main()
