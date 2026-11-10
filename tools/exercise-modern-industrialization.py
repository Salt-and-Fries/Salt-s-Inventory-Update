"""Focused real-input MI actions; fixture commands only seed ordinary world/items."""
from pathlib import Path
import sys,json,argparse
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'functional-tests/all-target-rollout'))
from evidence import send
sys.path.insert(0,str(ROOT/'tools'))
from importlib.machinery import SourceFileLoader
geometry=SourceFileLoader('mi_geometry',str(ROOT/'tools/check-modern-industrialization-windows.py')).load_module()
class Ui:
    def __init__(self,run):
        self.run=ROOT/'build/remaining-ui-audit/gameplay'/run
        self.out=ROOT/'build/modern-industrialization-implementation'/run;self.out.mkdir(parents=True,exist_ok=True)
    def send(self,action,**kw):return send(self.run,action,**kw)['client']
    def state(self):return self.send('wait',ticks=1)
    def window(self):return geometry.windows(self.state())[0]
    def click(self,x,y):self.send('click',x=x,y=y,ticks=2);return self.send('release',x=x,y=y,ticks=5)
    def native(self,x,y):
        w=self.window();b=w['bounds'];a=next(a for a in w['template']['regions'] if (q:=a['source'])['x']<=x<q['x']+q['width'] and q['y']<=y<q['y']+q['height'])
        return self.click(b['x']+(0 if a['exterior'] else 6)+a['x']+x-a['source']['x'],b['y']+(0 if a['exterior'] else 22)+a['y']+y-a['source']['y'])
    def inv(self,index):
        s=self.state();w=next(w for w in s['workspaceWindows'] if w['kind']=='INVENTORY');c=next(c for c in w['inventoryCells'] if c['inventorySlot']==index);return self.click(c['x'],c['y'])
    def save(self,tag):
        s=self.state();(self.out/(tag+'-client.json')).write_text(json.dumps(s,indent=2))
        self.send('snapshot-server',ticks=3);d=json.loads((self.run/'server-state.json').read_text());(self.out/(tag+'-server.json')).write_text(json.dumps(d,indent=2))
        self.send('capture',capture=tag,ticks=1);return s,d
    def pipe(self,fluid=False):
        # Same ordinary placement workflow as the approved native references;
        # switch to Survival before testing any inventory/UI transaction.
        self.send('command',command='gamemode creative @a',ticks=3)
        self.send('fixture',block='minecraft:air',ticks=3)
        self.send('command',command='setblock 8 65 7 '+('modern_industrialization:steel_tank' if fluid else 'minecraft:chest'),ticks=3)
        self.send('held',item='modern_industrialization:'+('fluid_pipe' if fluid else 'item_pipe'),count=8,ticks=3)
        self.send('sneak',down=True,ticks=2)
        for pos in ['8.5 65 12.5 180 11','8.9 65 12.5 180 11']:
            self.send('command',command='tp @a '+pos,ticks=5);self.send('use-block-ray',ticks=5)
        self.send('sneak',down=False,ticks=2);self.send('held',item='minecraft:air',ticks=2)
        self.send('command',command='tp @a 10.5 65 8.12 90 31',ticks=7);s=self.send('use-block-ray',ticks=10);geometry.check(s)
        self.send('command',command='gamemode survival @a',ticks=3)
    def seed(self,item,count=1):
        self.send('command',command=f'item replace entity @a container.9 with {item} {count}',ticks=3);self.send('inventory',ticks=4);self.inv(9)
    def block(self,block):
        self.send('fixture',block='modern_industrialization:'+block,ticks=3);self.send('command',command='tp @a 8.5 65 12.5 180 16',ticks=7);s=self.send('open' if block=='forge_hammer' else 'open-ray',ticks=8);geometry.check(s)
    def execute(self,case):
        self.send('close',ticks=2);self.send('command',command='gamemode survival @a',ticks=2)
        if case=='fluid-pipe':
            self.pipe(True);self.seed('minecraft:water_bucket');self.native(80,28);self.inv(9)
            self.native(101,48);self.native(158,32);self.save('fluid-pipe-actions')
            self.send('close',ticks=3);self.send('use-block-ray',ticks=8);s,d=self.save('fluid-pipe-reopened');assert 'water_bucket' in str(next(i for i in d['inventory'] if i['index']==9))
        elif case=='item-pipe':
            self.pipe();self.seed('minecraft:stone',8);self.native(24,26);self.inv(9)
            self.native(103,78);self.native(158,54);self.native(158,32);self.save('item-pipe-actions')
            self.send('close',ticks=3);self.send('use-block-ray',ticks=8);s,d=self.save('item-pipe-reopened');assert 'whitelist:0b' in d['fixture'] and 'insertPriority:1' in d['fixture'] and 'minecraft:stone' in d['fixture']
        elif case=='tank':
            self.block('configurable_tank');w=self.window();cell=next(s for s in w['slots'] if not s['player']);self.seed('minecraft:water_bucket');self.native(cell['x']+8,cell['y']+8);self.inv(9)
            s,d=self.save('tank-bucket-transfer');assert 'minecraft:water' in d['fixture'];assert 'id:"minecraft:bucket"' in str(next(i for i in d['inventory'] if i['index']==9))
        elif case=='shape':
            self.block('large_tank');self.native(-14,27);self.send('wait',ticks=6);s,d=self.save('shape-expanded');geometry.check(s)
            w=self.window();tray=next(r for r in w['template']['regions'] if r['id']=='native-shape-tray');assert tray['source']['width']>32
            buttons=[a for a in w['nativeWidgets'] if a.get('visible') and a['width']==12 and a['x']-w['nativeBounds']['x']<0]
            button=max(buttons,key=lambda a:a['x']);self.native(button['x']-w['nativeBounds']['x']+6,button['y']-w['nativeBounds']['y']+6)
            self.send('wait',ticks=8);s,d=self.save('shape-changed');geometry.check(s)
        else:raise ValueError(case)
        print(self.run.name,case,'PASS',flush=True)
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('run');p.add_argument('cases',nargs='+');a=p.parse_args();ui=Ui(a.run)
    for case in a.cases:ui.execute(case)
