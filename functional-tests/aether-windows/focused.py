"""Affected Aether windows only. Real slot clicks, native recipes and attachment lifetimes."""
from pathlib import Path
import json
import sys
import argparse

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send


class Check:
    def __init__(self,output):
        self.out=Path(output);self.folder=self.out/'aether-checks';self.folder.mkdir(exist_ok=True);self.state={}
        self.journal=self.folder/'journal.json';self.results=json.loads(self.journal.read_text()) if self.journal.exists() else {'scope':'Aether affected windows only','checks':[]}
    def call(self,kind='wait',**args):
        r=send(self.out,kind,**args);self.state=r.get('client',{});return r
    def cmd(self,text,ticks=4):return self.call('command',command=text,ticks=ticks)
    def passed(self,name,condition):
        self.results['checks'].append({'id':name,'passed':bool(condition)});self.journal.write_text(json.dumps(self.results,indent=2));assert condition,name;print('PASS',name,flush=True)
    def window(self,path):return next(w for w in self.state.get('allWindows',[]) if w.get('menuId')==path)
    def workspace(self,kind):return next(w for w in self.state['workspaceWindows'] if w['kind']==kind and w.get('persistentHidden')!='true')
    def info(self,w):return next(i for i in self.state['workspaceWindows'] if i['bounds']==w['bounds'])
    def point(self,path,x,y):
        w=self.window(path);b=w['bounds'];p=w['composition']
        for r in reversed(p['regions']):
            s=r['source']
            if s['x']<=x<s['x']+s['width'] and s['y']<=y<s['y']+s['height']:
                return dict(x=b['x']+7+p['padding']+r['x']+x-s['x'],y=b['y']+26+p['padding']+r['y']+y-s['y'])
        raise AssertionError((path,x,y))
    def tap(self,point,button=0):self.call('click',**point,button=button,ticks=1);self.call('release',**point,button=button,ticks=4)
    def slot(self,path,index):
        w=self.window(path)
        if path=='salts_inventory_compat:aether_equipment':
            cell=w['aetherCells'][index];b=w['bounds'];width=max(c['x']+20 for c in w['aetherCells']);self.tap(dict(x=b['x']+(b['width']-width)//2+1+cell['x']+8,y=b['y']+30+cell['y']+8))
        else:
            s=next(s for s in w['slots'] if s['index']==index);self.tap(self.point(path,s['x']+8,s['y']+8))
    def inventory(self,index=9):
        cell=next(c for c in self.workspace('INVENTORY')['inventoryCells'] if c['inventorySlot']==index);self.tap(dict(x=cell['x'],y=cell['y']))
    def insert(self,path,slot,item,count=1):
        self.cmd(f'item replace entity @a inventory.0 with {item} {count}');self.call('wait',ticks=8)
        for _ in range(5):
            cell=next(c for c in self.workspace('INVENTORY')['inventoryCells'] if c['inventorySlot']==9)
            if item in cell['item']:break
            self.call('wait',ticks=6)
        assert item in cell['item'],('Inventory snapshot did not arrive',item,cell)
        self.inventory();self.slot(path,slot)
    def snap(self,name):
        self.call('snapshot-server',ticks=1);d=json.loads((self.out/'server-state.json').read_text());self.call('aether-state',ticks=1);a=json.loads((self.out/'aether-state.json').read_text());(self.folder/(name+'.json')).write_text(json.dumps({'client':self.state,'server':d,'aether':a},indent=2));return d,a
    def capture(self,name):
        self.call('move',x=639,y=449,ticks=1);self.call('capture',capture=name,ticks=3);(self.folder/(name+'-state.json')).write_text(json.dumps(self.state,indent=2))
    def open(self,path,seeds=()):
        self.call('close');self.call('fixture',block='aether:'+path);self.cmd('gamemode survival @a');self.cmd('tp @a 8.5 65 12.5 180 18')
        # Seed before the menu opens, as a player arriving with ingredients.
        # This keeps commands out of a live native recipe-book inventory.
        for i,(item,count) in enumerate(seeds):self.cmd(f'item replace entity @a inventory.{i} with {item} {count}')
        self.call('open-ray',ticks=15)
        w=self.window('aether:'+path);self.passed(path+'-explicit-template',w.get('templateClass',w.get('template'))=='com.salts_inventory_update.compat.aether.AetherWindows$Template');self.call('inventory',ticks=5);self.capture('aether-'+path+'-empty')
    def close_reopen(self,path):
        w=self.window('aether:'+path);self.tap(self.info(w)['controls']['CLOSE']);self.passed(path+'-close-button',not any(w.get('menuId')=='aether:'+path for w in self.state.get('allWindows',[])))
        self.call('close');self.call('open-ray',ticks=12);self.passed(path+'-native-reopen',self.window('aether:'+path) is not None)
    def recipe(self,path,x):
        from PIL import Image
        # Core renders this native recipe book as a separate panel, rather
        # than another menu lifetime in workspaceWindows.
        self.tap(self.point('aether:'+path,x,44));self.capture('aether-'+path+'-recipes-open');b=self.window('aether:'+path)['bounds'];point=(2*(b['x']-15),2*(b['y']+35))
        color=Image.open(self.out/'screenshots'/('aether-'+path+'-recipes-open.png')).convert('RGB').getpixel(point)
        self.passed(path+'-recipe-book-opens',color==(55,55,55))
        self.tap(self.point('aether:'+path,x,44));self.capture('aether-'+path+'-recipes-closed');color=Image.open(self.out/'screenshots'/('aether-'+path+'-recipes-closed.png')).convert('RGB').getpixel(point)
        self.passed(path+'-recipe-book-closes',color!=(55,55,55))
    def machines(self):
        for path,input,fuel,result,wait in [('altar','aether:holystone','aether:ambrosium_shard','aether:healing_stone',520),('freezer','aether:cold_aercloud','aether:icestone','aether:blue_aercloud',420)]:
            self.open(path,[(input,1),(fuel,2)]);self.passed(path+'-three-native-slots',sum(not s['player'] for s in self.window('aether:'+path)['slots'])==3);self.recipe(path,30)
            self.inventory(9);self.slot('aether:'+path,0);self.inventory(10);self.slot('aether:'+path,1);self.call('wait',ticks=20);self.capture('aether-'+path+'-processing');d,a=self.snap(path+'-processing');self.passed(path+'-native-processing-started',('BurnTime:0' not in a['block']) and input in a['block'])
            self.call('wait',ticks=wait);d,a=self.snap(path+'-result');m=next(m for m in d['menus'] if m['class'].endswith(path.title()+'Menu'));self.passed(path+'-native-result',result in m['slots'][2]['item']);self.slot('aether:'+path,2);self.inventory(10);d,a=self.snap(path+'-withdraw');self.passed(path+'-result-to-separate-inventory',result in d['inventory'][10]['item']);self.close_reopen(path)
        self.open('incubator',[('aether:blue_moa_egg',1),('aether:ambrosium_torch',3)]);self.passed('incubator-two-native-slots',sum(not s['player'] for s in self.window('aether:incubator')['slots'])==2);self.recipe('incubator',47)
        self.inventory(9);self.slot('aether:incubator',0);self.inventory(10);self.slot('aether:incubator',1);self.call('wait',ticks=30);d,a=self.snap('incubator-processing');self.capture('aether-incubator-processing');self.passed('incubator-native-incubation-active',self.window('aether:incubator').get('nativeValues',{}).get('isIncubating') is True);self.close_reopen('incubator')
    def lore(self):
        self.call('close');self.call('held',item='aether:book_of_lore');self.call('use-item',ticks=15);w=self.window('aether:book_of_lore');self.passed('lore-explicit-template',w.get('templateClass',w.get('template'))=='com.salts_inventory_update.compat.aether.AetherWindows$Template');self.call('inventory',ticks=5);self.capture('aether-lore-empty')
        self.insert('aether:book_of_lore',0,'aether:zanite_shovel');self.call('wait',ticks=10);self.capture('aether-lore-item');w=self.window('aether:book_of_lore');self.passed('lore-reads-real-item',w['nativeValues'].get('pages',0)>1 and w['slots'][0]['item']=='aether:zanite_shovel')
        self.tap(self.point('aether:book_of_lore',231,179));self.passed('lore-next-native-page',self.window('aether:book_of_lore')['nativeValues']['currentPageNumber']>0);self.tap(self.point('aether:book_of_lore',24,179));self.passed('lore-previous-native-page',self.window('aether:book_of_lore')['nativeValues']['currentPageNumber']==0)
        self.tap(self.info(self.window('aether:book_of_lore'))['controls']['CLOSE']);d,a=self.snap('lore-closed');self.passed('lore-close-returns-inspected-item',any('aether:zanite_shovel' in e['item'] for e in d['inventory']))
    def cloud(self):
        c=self.workspace('CHARACTER')['content'];self.tap(dict(x=c['x']+33,y=c['y']+max(0,c['height']-12)+4))
    def equipment(self):
        self.call('close');self.call('fixture',block='minecraft:air');self.cmd('gamemode survival @a');self.call('inventory');self.call('aether-character',ticks=8);self.cloud();w=self.window('salts_inventory_compat:aether_equipment');self.passed('cloud-opens-only-eight-aether-slots',len(w['slots'])==8 and len(w['aetherCells'])==8)
        self.passed('equipment-owned-by-character',w['aetherOwner']==self.workspace('CHARACTER')['instance']);self.capture('aether-equipment-empty')
        self.cloud();self.passed('cloud-closes-equipment-panel',not any(w.get('menuId')=='salts_inventory_compat:aether_equipment' for w in self.state['allWindows']));self.cloud();self.passed('cloud-reopens-equipment-panel',self.window('salts_inventory_compat:aether_equipment') is not None)
        w=self.window('salts_inventory_compat:aether_equipment');i=next(i for i,c in enumerate(w['aetherCells']) if c['identifier']=='aether_cape');self.insert('salts_inventory_compat:aether_equipment',i,'aether:red_cape');d,a=self.snap('equipment-cape');self.passed('cape-reaches-real-wearable-inventory',any(v['name']=='aether_cape' and 'aether:red_cape' in v['item'] for v in a['equipment']))
        cell=w['aetherCells'][i];w=self.window('salts_inventory_compat:aether_equipment');b=w['bounds'];width=max(c['x']+20 for c in w['aetherCells']);point=dict(x=b['x']+(b['width']-width)//2+1+cell['x']+14,y=b['y']+30+cell['y']);old=next(v['render'] for v in a['equipment'] if v['name']=='aether_cape');self.tap(point);d,a=self.snap('equipment-hidden');self.passed('visibility-updates-native-backend',next(v['render'] for v in a['equipment'] if v['name']=='aether_cape')!=old);self.tap(point)
        self.capture('aether-equipment-cape');self.slot('salts_inventory_compat:aether_equipment',i);self.inventory(10);self.insert('salts_inventory_compat:aether_equipment',i,'minecraft:stone');d,a=self.snap('equipment-invalid');self.passed('native-equipment-restrictions-preserved',not any('minecraft:stone' in v['item'] for v in a['equipment']));self.inventory(9)
        w=self.window('salts_inventory_compat:aether_equipment');b=w['bounds'];self.tap(dict(x=b['x']-12 if b['x']>=22 else b['x']+12,y=b['y']+36 if b['x']>=22 else b['y']-12));self.passed('crown-opens-native-moa-skins',self.state['screen'].endswith('MoaSkinsScreen'));self.capture('aether-moa-skins-native');self.call('key',key=256,ticks=8);self.passed('skins-return-retains-equipment-window',any(w.get('menuId')=='salts_inventory_compat:aether_equipment' for w in self.state.get('allWindows',[])))
        self.capture('aether-equipment-final')
    def backpack(self):
        self.call('close');self.cmd('item replace entity @a armor.chest with quark:backpack');self.call('inventory',ticks=12);self.call('aether-character',ticks=5);self.call('wait',ticks=20);self.cloud()
        bag=self.window('salts_inventory_compat:quark_backpack');self.passed('backpack-retains-27-slots',len(bag['slots'])==27)
        i=self.workspace('INVENTORY');char=self.workspace('CHARACTER');eq=self.window('salts_inventory_compat:aether_equipment');self.passed('equipment-owner-is-character-with-backpack',eq['aetherOwner']==char['instance']);self.passed('backpack-no-independent-close','CLOSE' not in self.info(bag)['controls']);self.capture('aether-backpack-and-equipment')
        self.tap(char['controls']['CLOSE']);self.call('wait',ticks=10);self.passed('character-close-closes-equipment',not any(w.get('menuId')=='salts_inventory_compat:aether_equipment' for w in self.state['allWindows']));self.passed('character-close-keeps-inventory-backpack',self.window('salts_inventory_compat:quark_backpack') is not None)
        self.call('aether-character',ticks=8);self.cloud();i=self.workspace('INVENTORY');self.tap(i['controls']['CLOSE']);self.call('wait',ticks=10);self.passed('inventory-close-closes-backpack',not any(w.get('menuId')=='salts_inventory_compat:quark_backpack' for w in self.state['allWindows']));self.passed('inventory-close-keeps-aether-equipment',self.window('salts_inventory_compat:aether_equipment') is not None)
        self.call('inventory',ticks=25);self.passed('inventory-reopen-restores-backpack',self.window('salts_inventory_compat:quark_backpack') is not None);self.capture('aether-backpack-final')


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('phase',choices=['machines','lore','equipment','backpack']);a=p.parse_args();check=Check(a.output);getattr(check,a.phase)()
