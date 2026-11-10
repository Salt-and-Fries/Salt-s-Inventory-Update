"""Focused live checks for the AE2-backed Applied Botanics portable mana cell."""
from pathlib import Path
import json,re,sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'project-everything-ten'))
from send import send

class AppBotCheck:
    def __init__(self,output,attempt=None):
        self.out=Path(output);self.state={};self.menu_id='ae2:portable_mana_cell';self.folder=self.out/'appbot-checks'
        if attempt:self.folder=self.folder/attempt
        self.folder.mkdir(parents=True,exist_ok=True)
        self.path=self.folder/'journal.json'
        self.results=json.loads(self.path.read_text()) if self.path.exists() else {'scope':'Applied Botanics portable mana window and shared AE2 cursor callback only','checks':[]}
    def call(self,kind,**args):
        r=send(self.out,kind,**args);self.state=r.get('client',{});return r
    def passed(self,name,condition):
        self.results['checks'].append({'id':name,'passed':bool(condition)});self.path.write_text(json.dumps(self.results,indent=2));assert condition,name;print('PASS',name,flush=True)
    def window(self):return next(w for w in self.state['allWindows'] if w.get('menuId')==self.menu_id)
    def tap(self,x,y,button=0):
        self.call('click',x=x,y=y,button=button,ticks=1);self.call('release',x=x,y=y,button=button,ticks=6)
    def native(self,x,y,button=0):
        b=self.window()['bounds'];self.tap(b['x']+x,b['y']+y,button)
    def cell(self,index=9):
        inv=next(w for w in self.state['workspaceWindows'] if w['kind']=='INVENTORY')
        return next(c for c in inv['inventoryCells'] if c['inventorySlot']==index)
    def inventory_tap(self,index=9):
        at=self.cell(index);self.tap(at['x'],at['y'])
    def snap(self,name):
        self.call('snapshot-server',ticks=1);d=json.loads((self.out/'server-state.json').read_text());(self.folder/(name+'.json')).write_text(json.dumps({'client':self.state,'server':d},indent=2));return d
    def cmd(self,text):self.call('command',command=text,ticks=10)
    def capture(self,name):self.call('capture',capture=name,ticks=5)
    def start(self,tier='1k'):
        self.call('close');self.call('fixture',block='minecraft:air');self.cmd('gamemode survival @a')
        self.call('held',item='appbot:portable_mana_storage_cell_'+tier,snbt='{internalCurrentPower:20000.0d,amount:250000L}')
        self.call('use-item',shift=False,ticks=15)
        w=self.window();self.passed(tier+'-uses-existing-AE2-window',w['definition']=='com.salts_inventory_update.compat.ae2.nativeview.AEWindow')
        self.passed(tier+'-native-mana-entry',w['nativeValues']['repositorySize']==1 and w['nativeValues']['repositoryEntries'][0]['getStoredAmount']=='250000')
    @staticmethod
    def amount(item,key):
        m=re.search(r'(?<![a-zA-Z])'+key+r':(\d+)',item);return int(m[1]) if m else 0
    def mana(self):
        self.start();self.call('inventory')
        self.cmd('item replace entity @a inventory.0 with botania:mana_tablet{mana:100000}')
        self.call('wait',ticks=10);self.inventory_tap()
        d=self.snap('tablet-picked-up')
        self.passed('tablet-pickup-from-SIU-inventory','botania:mana_tablet' in d['menus'][0]['carried'])
        self.native(15,40)
        d=self.snap('filled-carried-tablet');carried=d['menus'][0]['carried']
        self.passed('native-mana-fill-retains-carried-tablet','botania:mana_tablet' in carried and self.amount(carried,'mana')==350000)
        self.passed('mana-withdrawal-conserved',self.amount(d['inventory'][0]['item'],'amount')==0)
        self.inventory_tap();d=self.snap('filled-tablet-returned')
        self.passed('filled-tablet-returns-to-SIU-inventory',self.amount(d['inventory'][9]['item'],'mana')==350000)
        self.inventory_tap();self.native(15,40,1)
        d=self.snap('emptied-carried-tablet');carried=d['menus'][0]['carried']
        self.passed('native-mana-deposit-retains-tablet','botania:mana_tablet' in carried and self.amount(carried,'mana')==0)
        self.passed('mana-deposit-conserved',self.amount(d['inventory'][0]['item'],'amount')==350000)
        self.inventory_tap();d=self.snap('empty-tablet-returned')
        self.passed('empty-tablet-returns-to-SIU-inventory','botania:mana_tablet' in d['inventory'][9]['item'])
        self.capture('appbot-mana-roundtrip')

    def controls(self):
        self.call('wait',ticks=5)
        self.native(50,23,1);self.call('text',text='no-such-mana-entry',ticks=10)
        self.passed('native-search-filters-mana',self.window()['nativeValues']['repositorySize']==0)
        self.native(50,23,1);self.call('text',text='mana',ticks=10)
        self.passed('native-search-finds-mana',self.window()['nativeValues']['repositorySize']==1)
        self.native(50,23,1)
        before=self.window()['nativeValues']['sortDirection'];self.native(-11,33)
        self.passed('native-sort-direction',self.window()['nativeValues']['sortDirection']!=before)
        before=self.window()['nativeValues']['sortOrder'];self.native(-11,11)
        self.passed('native-sort-order',self.window()['nativeValues']['sortOrder']!=before)
        selected_sort=(self.window()['nativeValues']['sortDirection'],self.window()['nativeValues']['sortOrder'])
        self.cmd('item replace entity @a inventory.1 with ae2:energy_card')
        self.inventory_tap(10);self.snap('upgrade-picked-up');self.native(213,16)
        d=self.snap('upgrade-inserted');menu=next(m for m in d['menus'] if m['class']=='appeng.menu.me.common.MEStorageMenu')
        self.passed('native-upgrade-insertion','ae2:energy_card' in menu['slots'][0]['item'])
        self.native(213,16);self.inventory_tap(10)
        d=self.snap('upgrade-removed')
        self.passed('native-upgrade-withdrawal','ae2:energy_card' in d['inventory'][10]['item'])
        self.native(-11,55)
        self.passed('native-settings-subview',self.window()['nativeScreen'].endswith('.TerminalSettingsScreen'))
        self.capture('appbot-native-settings')
        back=next(w for w in self.window()['nativeWidgets'] if w['type'].endswith('.TabButton') and w['visible'])
        self.tap(back['x']+back['width']//2,back['y']+back['height']//2)
        self.passed('native-settings-return-tab',self.window()['nativeScreen']=='appeng.client.gui.me.common.MEStorageScreen')
        self.passed('settings-preserve-mana-entry',self.window()['nativeValues']['repositoryEntries'][0]['getStoredAmount']=='350000')
        w=self.window();b=w['bounds'];info=next(v for v in self.state['workspaceWindows'] if v['kind']=='CONTAINER')
        if info['locked']=='true':self.tap(**info['controls']['LOCK'])
        self.call('click',x=b['x']+25,y=b['y']+7,button=0,ticks=1)
        self.call('drag',x=b['x']+5,y=b['y']+27,dx=-20,dy=20,button=0,ticks=1)
        self.call('release',x=b['x']+5,y=b['y']+27,button=0,ticks=3)
        moved=self.window()['bounds'];self.passed('AE2-window-title-drag',moved['x']==b['x']-20 and moved['y']==b['y']+20)
        b=moved;x=b['x']+b['width']-2;y=b['y']+b['height']-2
        self.call('click',x=x,y=y,button=0,ticks=1);self.call('drag',x=x+18,y=y+18,dx=18,dy=18,button=0,ticks=1);self.call('release',x=x+18,y=y+18,button=0,ticks=4)
        sized=self.window()['bounds'];self.passed('AE2-window-row-resize',sized['width']==b['width'] and sized['height']==b['height']+18)
        self.capture('appbot-resized')
        self.call('close');self.call('use-item',shift=False,ticks=15)
        self.passed('close-reopen-retains-mana',self.window()['nativeValues']['repositoryEntries'][0]['getStoredAmount']=='350000')
        self.passed('close-reopen-retains-native-sort',(self.window()['nativeValues']['sortDirection'],self.window()['nativeValues']['sortOrder'])==selected_sort)

    def tiers(self):
        for tier in ['1k','4k','16k','64k','256k']:self.start(tier)

    def fluid(self):
        self.menu_id='ae2:portable_fluid_cell'
        self.call('close');self.call('fixture',block='minecraft:air');self.cmd('gamemode survival @a')
        self.call('held',item='ae2:portable_fluid_cell_1k',snbt='{internalCurrentPower:20000.0d}')
        self.call('use-item',shift=False,ticks=15);self.call('inventory')
        self.passed('shared-cursor-regression-ordinary-AE2-window',self.window()['definition']=='com.salts_inventory_update.compat.ae2.nativeview.AEWindow')
        self.cmd('item replace entity @a inventory.0 with minecraft:water_bucket')
        self.inventory_tap();self.snap('water-bucket-picked-up');self.native(15,40,1)
        d=self.snap('water-deposit');self.passed('ordinary-AE2-water-deposit-retains-bucket','id:"minecraft:bucket"' in d['menus'][0]['carried'] and self.window()['nativeValues']['repositorySize']==1)
        self.inventory_tap();self.snap('bucket-returned');self.inventory_tap();self.snap('bucket-picked-up');self.native(15,40)
        d=self.snap('water-withdrawal');self.passed('ordinary-AE2-water-withdrawal-retains-filled-bucket','minecraft:water_bucket' in d['menus'][0]['carried'] and self.window()['nativeValues']['repositorySize']==0)
        self.inventory_tap();d=self.snap('water-bucket-returned');self.passed('ordinary-AE2-bucket-roundtrip-conserved','minecraft:water_bucket' in d['inventory'][9]['item'])

if __name__=='__main__':
    c=AppBotCheck(sys.argv[1]);getattr(c,sys.argv[2] if len(sys.argv)>2 else 'mana')()
