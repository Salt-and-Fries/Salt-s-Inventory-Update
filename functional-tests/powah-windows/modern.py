"""Focused modern Powah checks, including the newly added native tank action."""
from pathlib import Path
import sys
from focused import Check


class Modern(Check):
    def tanks(self,block='magmator',path='magmator',bucket='lava_bucket',fluid='lava'):
        self.open(block,'starter'if block=='reactor'else'basic')
        prefix='modern-tank'if block=='magmator'else'modern-'+block+'-tank'
        self.tap(self.point(path,182,38))
        self.passed(prefix+'-native-redstone-stop',self.native_state(path)['getRedstoneMode']=='ON')
        self.call('inventory',ticks=3)
        self.cmd('item replace entity @a inventory.0 with minecraft:'+bucket)
        self.inventory();self.tap(self.point(path,164,35))
        d=self.native_state(path,'tank-ui-filled')
        self.passed(prefix+'-left-click-uses-native-fill','minecraft:'+fluid in d['nbt'],d['nbt'])
        self.inventory(10);d=self.snapshot('tank-empty-bucket-returned')
        self.passed(prefix+'-fill-returns-empty-bucket','minecraft:bucket'in d['inventory'][10]['item'],d['inventory'][10])
        self.capture(block+'-tank-ui-filled',path)
        self.inventory(10);self.tap(self.point(path,164,35),1)
        self.inventory(11);d=self.snapshot('tank-lava-bucket-returned')
        self.passed(prefix+'-right-click-drains-to-cursor','minecraft:'+bucket in d['inventory'][11]['item'],d['inventory'][11])
        self.passed(prefix+'-drain-empties-original-tank','minecraft:'+fluid not in self.machine(d,path)['state']['nbt'])
        self.capture(block+'-tank-ui-drained',path)
    def presentation(self):
        self.cmd('fill -32 64 -32 32 64 32 minecraft:stone')
        super().presentation()
    def modern(self):
        self.geometry();self.settings();self.cable();self.channels();self.transfers()
        self.generation();self.reactor();self.fluids();self.quick();self.tanks();self.controls();self.presentation()


if __name__=='__main__':
    q=Modern(Path(sys.argv[1]));getattr(q,sys.argv[2]if len(sys.argv)>2 else'modern')()
