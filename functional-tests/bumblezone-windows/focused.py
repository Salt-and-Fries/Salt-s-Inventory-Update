"""Focused live checks of the three affected Bumblezone windows, not a full suite."""
from pathlib import Path
import sys,json,argparse
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'project-everything-ten'))
from send import send

class Check:
    def __init__(self,output):
        self.out=output;self.state={};self.folder=output/'bumblezone-checks';self.folder.mkdir(exist_ok=True)
        self.path=self.folder/'journal.json';self.results=json.loads(self.path.read_text()) if self.path.exists() else {'scope':'Bumblezone affected windows only','checks':[]}
    def call(self,kind,**args):
        r=send(self.out,kind,**args);self.state=r.get('client',{});return r
    def window(self,menu):
        w=next(w for w in self.state.get('allWindows',[]) if w.get('menuId')=='the_bumblezone:'+menu)
        assert w.get('template')=='com.salts_inventory_update.compat.bumblezone.BumblezoneWindows$Template',w
        return w
    def point(self,menu,x,y):
        w=self.window(menu);b=w['bounds'];p=w['composition'];brief=menu=='buzzing_briefcase'
        for r in reversed(p['regions']):
            s=r['source']
            if not(s['x']<=x<s['x']+s['width'] and s['y']<=y<s['y']+s['height']):continue
            return {'x':b['x']+(0 if brief else 7)+p['padding']+r['x']+x-s['x']-w.get('hostScrollX',0),
                    'y':b['y']+(23 if brief else 22)+p['padding']+r['y']+y-s['y']-w.get('hostScrollY',0)}
        raise AssertionError(('not mapped',menu,x,y))
    def tap(self,point,button=0):
        self.call('click',**point,button=button,ticks=1);self.call('release',**point,button=button,ticks=5)
    def native(self,menu,x,y):self.tap(self.point(menu,x,y))
    def snap(self,name):
        self.call('snapshot-server',ticks=1);d=json.loads((self.out/'server-state.json').read_text());(self.folder/(name+'.json')).write_text(json.dumps({'client':self.state,'server':d},indent=2));return d
    def capture(self,name):self.call('move',x=639,y=449,ticks=1);self.call('capture',capture=name,ticks=3)
    def passed(self,name,condition):
        self.results['checks'].append({'id':name,'passed':bool(condition)});self.path.write_text(json.dumps(self.results,indent=2));assert condition,name;print('PASS',name,flush=True)
    def open_block(self,block):
        self.call('close');self.call('fixture',block=block)
        if block=='the_bumblezone:crystalline_flower':self.cmd('setblock 8 64 8 minecraft:amethyst_block')
        self.call('command',command='tp @a 8.5 65 12.5 180 18',ticks=8);self.call('open-ray',ticks=12)
    def item_cell(self,index=9):
        inv=next(w for w in self.state['workspaceWindows'] if w['kind']=='INVENTORY')
        return next(c for c in inv['inventoryCells'] if c['inventorySlot']==index)
    def inventory(self):self.call('inventory',ticks=3)
    def cmd(self,s):self.call('command',command=s,ticks=4)
    def drag(self,menu,dx,dy):
        w=self.window(menu);info=next(v for v in self.state['workspaceWindows'] if v.get('kind')=='CONTAINER' and v['bounds']==w['bounds'])
        if info['locked']=='true':
            if 'LOCK' in info['controls']:self.tap(info['controls']['LOCK'])
            else:
                at=info['controls']['ELLIPSIS'];self.tap(at);self.tap({'x':at['x']-14,'y':at['y']+17})
        w=self.window(menu);b=w['bounds'];x=b['x']+25;y=b['y']+7
        self.call('click',x=x,y=y,button=0,ticks=1);self.call('drag',x=x+dx,y=y+dy,dx=dx,dy=dy,button=0,ticks=1);self.call('release',x=x+dx,y=y+dy,button=0,ticks=2)
        a=self.window(menu)['bounds'];return a['x']==b['x']+dx and a['y']==b['y']+dy
    def briefcase(self):
        self.call('close');self.call('fixture',block='minecraft:air')
        bees=[]
        for i in range(14):
            bees.append('{id:"minecraft:bee",UUID:[I;1,2,3,'+str(i+40)+'],Health:4.0f,Age:-12000,HasStung:1b,HasNectar:0b}')
        self.call('held',item='the_bumblezone:buzzing_briefcase',snbt='{BeesStored:['+','.join(bees)+']}')
        for i,item in enumerate(['minecraft:honey_bottle','the_bumblezone:bee_stinger','the_bumblezone:pollen_puff']):self.cmd('item replace entity @a inventory.'+str(i)+' with '+item+' 8')
        self.call('use-item',shift=True,ticks=12);w=self.window('buzzing_briefcase');self.passed('briefcase-live-template',w['bounds']['width']==382)
        self.capture('briefcase-populated')
        before=self.snap('briefcase-before')['inventory']
        # Original native cell: release, then health/stinger, then grow/pollen.
        for name,x,y,expected in [('heal',-6,73,'Health:6.0f'),('stinger',5,73,'HasStung:0b'),('grow',-6,84,'Age:0'),('pollen',5,84,'HasNectar:1b')]:
            self.native('buzzing_briefcase',x,y);d=self.snap('briefcase-'+name);case=d['inventory'][d['selected']]['item'];self.passed('briefcase-'+name,expected in case)
        self.native('buzzing_briefcase',244,105);d=self.snap('briefcase-last-row-release');case=d['inventory'][d['selected']]['item']
        self.passed('briefcase-last-row-release',case.count('id:"minecraft:bee"')==13)
        self.passed('briefcase-drag',self.drag('buzzing_briefcase',-20,35))
        self.capture('briefcase-after-actions')
        self.call('key',key=256,ticks=4);self.call('use-item',shift=True,ticks=10)
        self.passed('briefcase-close-reopen',self.window('buzzing_briefcase') is not None)
    def cocoon(self):
        self.open_block('the_bumblezone:honey_cocoon');self.cmd('gamemode survival @a');w=self.window('strict_9x2')
        self.passed('cocoon-18-owned-cells',len([s for s in w['slots'] if not s['player']])==18)
        self.inventory();self.cmd('item replace entity @a inventory.0 with minecraft:honey_bottle 8')
        self.tap(self.item_cell());self.native('strict_9x2',16,26)
        d=self.snap('cocoon-insert');m=next(m for m in d['menus'] if m['class'].endswith('StrictChestMenu'))
        self.passed('cocoon-native-slot-insert','honey_bottle' in m['slots'][0]['item'])
        self.native('strict_9x2',16,26);self.tap(self.item_cell());d=self.snap('cocoon-withdraw');m=next(m for m in d['menus'] if m['class'].endswith('StrictChestMenu'))
        self.passed('cocoon-native-slot-withdraw','honey_bottle' not in m['slots'][0]['item'])
        self.passed('cocoon-drag',self.drag('strict_9x2',20,25));self.capture('honey-cocoon-inventory')
        self.call('close');self.call('open-ray',ticks=10);self.passed('cocoon-close-reopen',self.window('strict_9x2') is not None)
    def flower(self):
        self.cmd('gamemode survival @a');self.cmd('xp add @a 100 levels')
        self.open_block('the_bumblezone:crystalline_flower');self.cmd('gamemode survival @a');w=self.window('crystalline_flower')
        self.passed('flower-three-owned-cells',len([s for s in w['slots'] if not s['player']])==3)
        self.inventory();self.cmd('item replace entity @a inventory.0 with minecraft:book 1')
        self.tap(self.item_cell());self.native('crystalline_flower',100,36)
        d=self.snap('flower-book');m=next(m for m in d['menus'] if m['class'].endswith('CrystallineFlowerMenu'))
        self.passed('flower-book-input','minecraft:book' in m['slots'][1]['item'])
        self.native('crystalline_flower',54,21);d=self.snap('flower-feed-xp');print('FLOWER_DATA',d['fixture'],flush=True)
        self.passed('flower-xp-tier-growth','tier:1,' not in d['fixture'] and d['fixture']!='none')
        self.native('crystalline_flower',95,55);d=self.snap('flower-select-enchantment')
        m=next(m for m in d['menus'] if m['class'].endswith('CrystallineFlowerMenu'))
        self.passed('flower-server-enchantment-selection','minecraft:enchanted_book' in m['slots'][2]['item'])
        self.capture('crystalline-flower-book')
        self.native('crystalline_flower',165,42);self.passed('flower-native-sort-click',self.window('crystalline_flower') is not None)
        self.passed('flower-drag',self.drag('crystalline_flower',-20,-25));self.capture('crystalline-flower-inventory')
        self.native('crystalline_flower',144,36);self.tap(self.item_cell());d=self.snap('flower-take-result')
        self.passed('flower-take-enchanted-result','minecraft:enchanted_book' in d['inventory'][9]['item'])

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('group',choices=['briefcase','cocoon','flower']);a=p.parse_args();c=Check(a.output);getattr(c,a.group)()
