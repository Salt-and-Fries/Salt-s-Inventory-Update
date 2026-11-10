"""Focused tray-height and raised-native-tab regressions, through live UI callbacks."""
from focused import Audit, ROOT, send
from pathlib import Path
import json, sys


class Polish(Audit):
    def call(self, kind, **args):
        result = super().call(kind, **args)
        self.reply.setdefault('server', {})
        return result

    def __init__(self, mc, loader, run):
        self.mc = mc
        self.loader = loader
        self.production = mc == '1.20.1' and loader == 'fabric'
        self.run = ROOT / (f'build/remaining-ui-audit/gameplay/{mc}-{loader}-siu-{run}' if self.production else f'build/all-target-rollout/gameplay/{mc}-{loader}-{run}')
        self.out = ROOT / f'output/extended-ae-modern-polish/{mc}-{loader}'
        self.out.mkdir(parents=True, exist_ok=True)
        self.state = {}
        self.checks = []
        self.records = []
        self.call('catalog', namespaces=['extendedae', 'expatternprovider'], ticks=1)
        self.pos = tuple(int(n.strip()) for n in json.loads((self.run/'catalog.json').read_text()).get('origin','8,65,8').split(','))
        if not self.production:
            self.call('dismiss-help',ticks=2)

    def namespace(self):
        return 'expatternprovider:' if self.mc == '1.20.1' and self.loader == 'forge' else 'extendedae:'

    def block(self, name):
        self.reset(self.namespace()+name)
        x, y, z = self.pos
        self.cmd(f'setblock {x} {y-1} {z} ae2:creative_energy_cell')
        self.call('open', ticks=16)

    def part(self, name):
        self.reset()
        x, y, z = self.pos
        self.cmd(f'setblock {x} {y} {z-1} minecraft:stone')
        self.cmd(f'setblock {x} {y-1} {z} ae2:creative_energy_cell')
        self.call('held', item='ae2:fluix_glass_cable')
        self.call('use-block-ray', ticks=10)
        self.call('held', item=self.namespace()+name)
        self.call('use-block-ray', ticks=10)
        self.call('held', item='minecraft:air')
        self.call('use-block-ray', ticks=16)

    def ae(self):
        return next(w for w in self.state['allWindows'] if w.get('definition','').endswith('.AEWindow'))

    def frame(self, name):
        w = self.ae()
        self.check(name+' frame observation', not w.get('aeFrameObserverError'))
        b = w['bounds']
        right = [r for r in w['nativeExternalBounds'] if r['x'] >= b['x']+b['width']-8]
        self.check(name+' has native upgrade tray', bool(right))
        # Native panel hit rectangles include transparent pixels below the artwork.
        margin = 4 if self.mc == '1.20.1' else 2
        self.check(name+' bottom aligns with full native tray', b['y']+b['height'] == max(r['y']+r['height']-margin for r in right))
        self.capture(name)

    def raised(self, name):
        w = self.ae()
        tabs = [t for t in w['nativeHeaderTabs'] if t['visible'] and t['x'] >= w['bounds']['x']+w['bounds']['width']/2]
        self.check(name+' retains a native raised tab', bool(tabs))
        for t in tabs:
            self.check(name+' '+t['id']+' matches original top anchor', t['y']-w['bounds']['y'] == t['styleTop'])
        return tabs[0]

    def priority(self, name):
        t = self.raised(name)
        self.capture(name)
        self.tap(t['x']+t['width']/2, t['y']+2)
        self.check(name+' exposed tab top is clickable', self.ae()['nativeScreen'].endswith('.PriorityScreen'))
        w = self.ae()
        tabs = [t for t in w['nativeWidgets'] if t['visible'] and t['type'].endswith('.TabButton')]
        self.check(name+' native return tab retained', bool(tabs))
        t = tabs[0]
        self.tap(t['x']+t['width']/2, t['y']+2)
        self.check(name+' native return navigation', '.extendedae.' in self.ae()['nativeScreen'])
        self.raised(name+' after return')

    def exercise(self):
        self.block('ex_io_port')
        self.frame('extended-io-port')
        self.part('mod_export_bus')
        self.frame('mod-export-bus')
        self.part('mod_storage_bus')
        self.priority('mod-storage-bus')
        self.block('ex_drive')
        self.priority('extended-drive')
        self.block('ex_pattern_provider')
        self.priority('extended-pattern-provider')
        self.call('wait', ticks=20)
        self.raised('stable repeated layout')
        # The excluded observation protocol has a separate parent-drag command.
        # Call it directly because the shared driver's "move" means cursor hover.
        from evidence import raw_send
        before = self.ae()['bounds']
        if self.production:
            workspace = next(w for w in self.state['workspaceWindows'] if w['bounds'] == before)
            if workspace.get('locked') == 'true':
                menu = workspace['controls']['ELLIPSIS']
                self.tap(menu['x'],menu['y'])
                self.tap(menu['x']-14,menu['y']+17)
                self.check('Unlock parent through normal SIU menu', next(w for w in self.state['workspaceWindows'] if w['bounds'] == before)['locked'] == 'false')
            x,y = before['x']+8,before['y']+5
            self.call('move',x=x,y=y,ticks=1)
            self.call('click',x=x,y=y,button=0,ticks=1)
            self.call('drag',x=x-15,y=y+12,button=0,dx=-15,dy=12,ticks=1)
            self.call('release',x=x-15,y=y+12,button=0,ticks=10)
        else:
            raw_send(self.run, 'move', dx=-15, dy=12, ticks=10)
        self.call('wait', ticks=5)
        after = self.ae()['bounds']
        self.check('Parent window moved through title drag', after['x'] != before['x'] or after['y'] != before['y'])
        self.priority('moved-pattern-provider')
        self.reset('ae2:drive')
        self.call('open', ticks=16)
        self.check('Existing AE2 Drive remains integrated', self.ae()['nativeScreen'].endswith('.DriveScreen'))
        self.call('close')


if __name__ == '__main__':
    a = Polish(*sys.argv[1:4])
    a.exercise()
