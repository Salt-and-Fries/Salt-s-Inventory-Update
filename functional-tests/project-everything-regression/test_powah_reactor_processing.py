"""A processing fixture must prove conversion, not forgive missing stock."""
import copy,unittest
from powah_checks import prime_reactor_carbon


class Reactor:
    def __init__(self,consume=True,store_reserve=True,drift=False,assembled=True):
        self.stock=0;self.carried=0;self.carbon=0;self.consume=consume;self.store_reserve=store_reserve;self.drift=drift
        self.assembled=assembled;self.dispatched=[]
    def snapshot(self,item=None):
        return copy.deepcopy({'menus':[{'session':7,'type':'owmii.powah.inventory.ReactorMenu',
            'slots':[{'count':0},{'count':0},{'count':0}]}],
            'nativeHost':{'powahReactor':{'carbon':self.carbon,'carbonCapacity':16000,'fuel':0,'running':False,'assembled':self.assembled}},
            'stock':self.stock,'carried':self.carried})
    def act(self,kind,**kw):
        self.dispatched.append(kind)
        if kind=='regression-seed-player':self.stock+=kw['items'][0]['count']
        elif kind=='regression-player-click':self.stock-=1;self.carried=1
        elif kind=='regression-slot-click':
            self.carried=0
            if not self.consume:self.stock+=1
            if self.store_reserve:self.carbon=16000
        elif kind=='wait' and self.drift and self.carbon:self.carbon-=1
    def run(self):
        def expect(label,condition,**detail):
            if not condition:raise AssertionError(label)
        return prime_reactor_carbon({},self.snapshot(),self.act,expect,self.snapshot,
            lambda data,item:data['stock']+data['carried'],lambda data,item,count:data['carried']==count)


class ReactorProcessingTests(unittest.TestCase):
    def test_real_idle_reserve_allows_strict_following_transfers(self):
        self.assertEqual((7,2,'minecraft:oak_log',7),Reactor().run())
    def test_stock_that_was_not_converted_is_not_a_conversion_pass(self):
        with self.assertRaisesRegex(AssertionError,'exactly one'):Reactor(consume=False).run()
    def test_missing_fuel_without_a_real_reserve_is_not_a_pass(self):
        with self.assertRaisesRegex(AssertionError,'exactly one'):Reactor(store_reserve=False).run()
    def test_unexplained_idle_reserve_drift_is_not_ignored(self):
        with self.assertRaisesRegex(AssertionError,'stays stable'):Reactor(drift=True).run()
    def test_unfinished_multiblock_cannot_pass_or_receive_test_fuel(self):
        reactor=Reactor(assembled=False)
        with self.assertRaisesRegex(AssertionError,'multiblock assembly'):reactor.run()
        self.assertEqual([],reactor.dispatched)
    def test_missing_native_assembly_evidence_cannot_pass(self):
        reactor=Reactor(assembled=None)
        with self.assertRaisesRegex(AssertionError,'multiblock assembly'):reactor.run()
        self.assertEqual([],reactor.dispatched)


if __name__=='__main__':unittest.main()
