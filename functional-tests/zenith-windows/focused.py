"""Focused production checks for the seven newly integrated Zenith menus."""
from pathlib import Path
import json,sys

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send


class Check:
    def __init__(self,run):
        self.run=Path(run);self.folder=self.run/'zenith-checks';self.folder.mkdir(exist_ok=True)
        self.path=self.folder/'journal.json';self.state={}
        self.results=json.loads(self.path.read_text())if self.path.exists()else {'scope':'Zenith affected menus only; not the full gameplay suite','checks':[]}
    def call(self,kind,**args):
        r=send(self.run,kind,**args);self.state=r['client'];return self.state
    def cmd(self,command):self.call('command',command=command,ticks=4)
    def passed(self,name,condition):
        self.results['checks'].append({'id':name,'passed':bool(condition)});self.path.write_text(json.dumps(self.results,indent=2))
        assert condition,name
        print('PASS',name,flush=True)
    def window(self,path):
        return next(w for w in self.state.get('allWindows',[])if w.get('menuId')=='zenith:'+path)
    def point(self,path,x,y):
        w=self.window(path);b=w['bounds'];p=w['composition']
        for r in reversed(p['regions']):
            s=r['source']
            if s['x']<=x<s['x']+s['width']and s['y']<=y<s['y']+s['height']:
                return dict(x=b['x']+(0 if r.get('exterior')else 7)+p['padding']+r['x']+x-s['x'],y=b['y']+(0 if r.get('exterior')else 26)+p['padding']+r['y']+y-s['y'])
        raise AssertionError((path,x,y))
    def tap(self,point,button=0):
        self.call('move',**point,ticks=2)
        self.call('click',**point,button=button,ticks=1);self.call('release',**point,button=button,ticks=5)
    def slot(self,path,index):
        s=next(s for s in self.window(path)['slots']if s['index']==index);self.tap(self.point(path,s['x']+8,s['y']+8))
    def inventory(self,index=9):
        w=next(w for w in self.state['workspaceWindows']if w['kind']=='INVENTORY')
        self.tap(next(dict(x=c['x'],y=c['y'])for c in w['inventoryCells']if c['inventorySlot']==index))
    def insert(self,path,index,item,count=1):
        self.cmd(f'item replace entity @a inventory.0 with {item} {count}')
        self.inventory();self.slot(path,index)
    def snap(self,name):
        self.call('snapshot-server',ticks=3);d=json.loads((self.run/'server-state.json').read_text());(self.folder/(name+'.json')).write_text(json.dumps(d,indent=2));return d
    def capture(self,name):
        name=name.replace('_','-')
        self.call('move',x=2,y=2,ticks=2);self.call('capture',capture='zenith-'+name,ticks=3)
        (self.folder/(name+'-client.json')).write_text(json.dumps(self.state,indent=2))
    def open(self,block,path):
        self.call('close');self.call('fixture',block=block,ticks=8);self.cmd('gamemode survival @a');self.cmd('experience set @a 50 levels')
        self.call('open',ticks=14);self.window(path)
        if not any(w['kind']=='INVENTORY'for w in self.state.get('workspaceWindows',[])):self.call('inventory',ticks=5)
        return self.window(path)
    def geometry(self):
        specs=[('gem_cutting_table','gem_cutting',4,190,126),('salvaging_table','salvage',18,190,120),('reforging_table','reforging',6,190,212),('simple_reforging_table','reforging',6,190,212),('augmenting_table','augmenting',2,190,168),('library','library',3,190,176),('ender_library','library',3,190,176),('minecraft:fletching_table','fletching',4,176,93),('minecraft:enchanting_table','enchanting_table',2,178,126)]
        for block,path,count,width,height in specs:
            self.open(block if ':'in block else 'zenith:'+block,path);w=self.window(path)
            self.passed(block+'-approved-window',w['template']=='com.salts_inventory_update.compat.zenith.ZenithClient$Template'and w['bounds']['width']==width and w['bounds']['height']==height)
            self.passed(block+'-native-slot-count',len([s for s in w['slots']if not s['player']])==count)
            self.capture(block.split(':')[-1]+'-empty')
    def fletching(self):
        self.open('minecraft:fletching_table','fletching')
        for slot,item in [(1,'minecraft:flint'),(2,'minecraft:stick'),(3,'minecraft:feather')]:self.insert('fletching',slot,item,16)
        d=self.snap('fletching-ready');m=next(m for m in d['menus']if m['class'].endswith('FletchingContainer'))
        self.passed('fletching-native-six-arrow-result','minecraft:arrow' in m['slots'][0]['item']and 'Count:6b'in m['slots'][0]['item'])
        self.capture('fletching-active');self.slot('fletching',0);self.inventory(10);d=self.snap('fletching-retrieved')
        self.passed('fletching-output-to-separate-inventory','minecraft:arrow'in d['inventory'][10]['item'])
    def libraries(self,block='library'):
        self.open('zenith:'+block,'library')
        for ench in ['sharpness','protection','unbreaking','efficiency','mending','fortune']:
            self.insert('library',0,'minecraft:enchanted_book{StoredEnchantments:[{id:"minecraft:'+ench+'",lvl:3s}]}')
        v=self.window('library')['nativeValues'];self.passed(block+'-six-deposits-synchronized',v['storedEnchantments']==6 and v['libraryRows']==6)
        self.capture(block+'-populated')
        self.call('scroll',**self.point('library',75,80),amount=-1,ticks=5)
        self.passed(block+'-native-list-scroll',self.window('library')['nativeValues']['startIndex']>0)
        self.tap(self.point('library',65,21));self.call('text',text='sharp',ticks=5)
        self.passed(block+'-native-search',self.window('library')['nativeValues']['libraryRows']==1)
        self.tap(self.point('library',65,38));d=self.snap(block+'-extraction');m=next(m for m in d['menus']if m['class'].endswith('EnchLibraryContainer'))
        self.passed(block+'-native-book-extraction','minecraft:enchanted_book'in m['slots'][1]['item'])
        self.slot('library',1);self.inventory(10);d=self.snap(block+'-book-withdrawn');self.passed(block+'-book-to-separate-inventory','minecraft:enchanted_book'in d['inventory'][10]['item'])
        self.tap(self.point('library',65,21),1);self.passed(block+'-right-click-search-clear',self.window('library')['nativeValues']['search']=='')
    def enchant(self):
        self.call('close');self.cmd('fill 6 65 6 10 66 10 minecraft:bookshelf')
        self.cmd('fill 7 65 7 9 66 9 minecraft:air')
        self.open('minecraft:enchanting_table','enchanting_table');self.insert('enchanting_table',0,'minecraft:diamond_sword');self.insert('enchanting_table',1,'minecraft:lapis_lazuli',64)
        v=self.window('enchanting_table')['nativeValues']
        self.passed('enchanting-stats-and-three-clues-synchronized',v['eterna']>0 and len(v['costs'])==3 and all(x>0 for x in v['costs']) and all(x>=0 for x in v['enchantClue']) and v['receivedClues']!='{}')
        self.capture('enchanting-active');w=self.window('enchanting_table');b=w['bounds']
        self.tap(dict(x=b['x']+155,y=b['y']-8))
        self.passed('enchanting-native-fullscreen-information',self.state['screen']=='dev.shadowsoffire.apotheosis.ench.table.EnchantingInfoScreen')
        self.capture('enchanting-information');self.call('key',key=256,ticks=6)
        self.passed('enchanting-information-returns-to-same-window',self.window('enchanting_table')['bounds']==b)
        self.tap(self.point('enchanting_table',95,23));d=self.snap('enchanted-result');m=next(m for m in d['menus']if m['class'].endswith('ApothEnchantmentMenu'))
        self.passed('enchanting-native-enchantment-applied','Enchantments:'in m['slots'][0]['item'])
        self.capture('enchanting-complete')
    def reforging(self,block='reforging_table'):
        self.open('zenith:'+block,'reforging')
        for slot,item in [(0,'minecraft:diamond_sword'),(1,'zenith:common_material'),(2,'zenith:sigil_of_rebirth')]:self.insert('reforging',slot,item,1 if slot==0 else 64)
        d=self.snap(block+'-ready');m=next(m for m in d['menus']if m['class'].endswith('ReforgingMenu'))
        self.passed(block+'-three-native-choices',all('minecraft:diamond_sword'in s['item']and 'affix_data'in s['item']for s in m['slots'][3:6]))
        self.capture(block+'-active');self.slot('reforging',5);self.inventory(10);d=self.snap(block+'-result')
        self.passed(block+'-choice-to-separate-inventory','affix_data'in d['inventory'][10]['item'])
        m=next(m for m in d['menus']if m['class'].endswith('ReforgingMenu'))
        self.passed(block+'-native-costs-consumed','Count:61b'in m['slots'][1]['item'])
    def affixed_sword(self,path,slot=0):
        self.call('held',item='minecraft:diamond_sword');self.cmd('execute as @a run zenith lootify zenith:rare')
        self.cmd('item replace entity @a inventory.0 from entity @p weapon.mainhand')
        self.inventory();self.slot(path,slot)
    def salvage(self):
        self.open('zenith:salvaging_table','salvage');self.affixed_sword('salvage')
        self.tap(self.point('salvage',107,43));d=self.snap('salvage-results');m=next(m for m in d['menus']if m['class'].endswith('SalvagingMenu'))
        self.passed('salvage-native-input-consumed','minecraft:air'in m['slots'][0]['item'])
        self.passed('salvage-native-material-produced',any('zenith:rare_material'in s['item']for s in m['slots'][12:18]))
        self.capture('salvage-produced');self.slot('salvage',12);self.inventory(10);d=self.snap('salvage-withdrawn')
        self.passed('salvage-output-to-separate-inventory','zenith:rare_material'in d['inventory'][10]['item'])
    def gems(self):
        self.open('zenith:gem_cutting_table','gem_cutting')
        gem='zenith:gem{gem:"zenith:core/combatant",affix_data:{rarity:"zenith:common"}}'
        for slot,item,count in [(0,gem,1),(1,'zenith:gem_dust',16),(2,gem,1),(3,'zenith:common_material',16)]:self.insert('gem_cutting',slot,item,count)
        self.capture('gem-cutting-ready');self.tap(self.point('gem_cutting',144,53));d=self.snap('gem-upgraded');m=next(m for m in d['menus']if m['class'].endswith('GemCuttingMenu'))
        self.passed('gem-upgrades-in-place-to-uncommon','zenith:uncommon'in m['slots'][0]['item'])
        self.passed('gem-native-material-costs','Count:15b'in m['slots'][1]['item']and 'minecraft:air'in m['slots'][2]['item']and 'Count:13b'in m['slots'][3]['item'])
        self.capture('gem-cutting-upgraded');self.slot('gem_cutting',0);self.inventory(10);d=self.snap('gem-withdrawn')
        self.passed('gem-result-to-separate-inventory','zenith:uncommon'in d['inventory'][10]['item'])
    def augment(self):
        self.open('zenith:augmenting_table','augmenting');self.affixed_sword('augmenting');self.insert('augmenting',1,'zenith:sigil_of_enhancement',16)
        self.passed('augment-native-affixes-loaded',self.window('augmenting')['nativeValues']['affixes']>=2)
        self.tap(self.point('augmenting',85,23));self.passed('augment-dropdown-opens',self.window('augmenting')['nativeValues']['dropdownOpen'])
        self.capture('augmenting-selector');self.tap(self.point('augmenting',85,53));self.passed('augment-dropdown-selects-affix',not self.window('augmenting')['nativeValues']['dropdownOpen'])
        d=self.snap('augment-before');m=next(m for m in d['menus']if m['class'].endswith('AugmentingMenu'));before=m['slots'][0]['item']
        self.tap(self.point('augmenting',74,117));d=self.snap('augment-upgrade');m=next(m for m in d['menus']if m['class'].endswith('AugmentingMenu'))
        self.passed('augment-native-upgrade-cost-and-item-change','Count:14b'in m['slots'][1]['item']and m['slots'][0]['item']!=before)
        self.capture('augmenting-upgraded');before=m['slots'][0]['item'];self.tap(self.point('augmenting',126,117));d=self.snap('augment-reroll');m=next(m for m in d['menus']if m['class'].endswith('AugmentingMenu'))
        self.passed('augment-native-reroll-cost-and-item-change','Count:13b'in m['slots'][1]['item']and m['slots'][0]['item']!=before)
        self.capture('augmenting-rerolled')
    def management(self):
        self.open('zenith:gem_cutting_table','gem_cutting')
        def row():
            b=self.window('gem_cutting')['bounds']
            return next(w for w in self.state['workspaceWindows']if w['kind']=='CONTAINER'and w['bounds']==b)
        r=row();self.passed('processing-window-no-sort-controls',not any('SORT'in key for key in r['controls']))
        original=r['pinMode'];self.tap(r['controls']['PIN']);self.passed('window-pin-control',row()['pinMode']!=original)
        for i in range(3):
            if row()['pinMode']==original:break
            self.tap(row()['controls']['PIN'])
        self.passed('window-pin-cycle-restores-state',row()['pinMode']==original)
        r=row()
        if r['locked']=='true':self.tap(r['controls']['LOCK'])
        self.passed('window-unlock-control',row()['locked']=='false')
        b=self.window('gem_cutting')['bounds'];p=dict(x=b['x']+20,y=b['y']+9)
        self.call('click',**p,button=0,ticks=1);self.call('drag',x=p['x']-24,y=p['y']+12,dx=-24,dy=12,button=0,ticks=1);self.call('release',x=p['x']-24,y=p['y']+12,button=0,ticks=3)
        after=self.window('gem_cutting')['bounds'];self.passed('window-title-drag',after['x']==b['x']-24 and after['y']==b['y']+12)
        self.insert('gem_cutting',1,'zenith:gem_dust',4);d=self.snap('moved-window-input');m=next(m for m in d['menus']if m['class'].endswith('GemCuttingMenu'))
        self.passed('moved-window-slot-input-mapping','Count:4b'in m['slots'][1]['item'])
        self.tap(row()['controls']['FOCUS']);self.passed('window-focus-control',row()['focused']=='true')
        self.capture('management-moved');self.tap(row()['controls']['CLOSE'])
        self.passed('window-close-control',not any(w.get('menuId')=='zenith:gem_cutting'for w in self.state.get('allWindows',[])))
        self.call('close');self.call('open',ticks=12);self.passed('window-native-reopen',self.window('gem_cutting')['template']=='com.salts_inventory_update.compat.zenith.ZenithClient$Template')


if __name__=='__main__':
    q=Check(sys.argv[1]);getattr(q,sys.argv[2])(*sys.argv[3:])
