"""Focused live acceptance of the two native vending windows; not a gameplay suite."""
from pathlib import Path
import json,re,sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'project-everything-ten'))
from send import send

class Check:
    def __init__(self,out):
        self.out=Path(out);self.folder=self.out/'focused-checks';self.folder.mkdir(exist_ok=True);self.state={};self.results=[]
    def call(self,kind,**kw):self.state=send(self.out,kind,**kw)['client'];return self.state
    def window(self,kind='config'):return next(w for w in self.state['allWindows'] if w.get('menuId')=='toms_trading_network:vending_machine.menu_'+kind)
    def check(self,name,passed):
        self.results.append({'id':name,'passed':bool(passed)});(self.folder/'journal.json').write_text(json.dumps({'scope':'The two vending windows, ghost configuration, native popup, sides, name, native purchase, inventory and scaling only','checks':self.results},indent=2));assert passed,name;print('PASS',name,flush=True)
    def cmd(self,s):self.call('command',command=s,ticks=4)
    def click(self,x,y,kind='config',button=0):
        w=self.window(kind)['bounds'];x+=w['x']+8;y+=w['y']+24
        self.call('click',x=x,y=y,button=button,ticks=1);self.call('release',x=x,y=y,button=button,ticks=6)
    def key(self,key,modifiers=0):self.call('key',key=key,modifiers=modifiers,ticks=3)
    def snap(self,name):
        self.call('snapshot-server',ticks=2);s=json.loads((self.out/'server-state.json').read_text());(self.folder/(name+'.json')).write_text(json.dumps({'client':self.state,'server':s},indent=2));return s
    def menu(self,s,kind='config'):return next(m for m in s['menus'] if m['class']==self.window(kind)['menu'])
    @staticmethod
    def count(s):return int(re.search(r'Count:(-?\d+)b',s).group(1))
    def capture(self,name):self.call('capture',capture=name,ticks=8)
    def setup(self):
        self.call('close');self.call('fixture',block='toms_trading_network:vending_machine',ticks=8);self.cmd('gamemode survival @a');self.cmd('clear @a');self.cmd('data merge block 8 65 8 '+json.loads(Path(__file__).with_name('fixture.json').read_text())['block_nbt']);self.cmd('time set noon');self.cmd('gamerule doDaylightCycle false');self.call('open-ray',ticks=12);self.call('inventory',ticks=5)
    def inventory(self,index):
        w=next(w for w in self.state['workspaceWindows'] if w['kind']=='INVENTORY');p=next(p for p in w['inventoryCells'] if p['inventorySlot']==index);self.call('click',x=p['x'],y=p['y'],button=0,ticks=1);self.call('release',x=p['x'],y=p['y'],button=0,ticks=4)
    def run(self):
        self.setup();w=self.window()
        self.check('configuration-is-one-native-panel-with-inventory-cut',w['composition']['regions'][0]['source']=={'x':4,'y':4,'width':168,'height':114} and len(w['composition']['regions'])==1)
        self.check('complete-configuration-and-storage-cells-kept',sum(not s['player'] for s in w['slots'])==24 and w['bounds']['width']==184 and w['bounds']['height']==146)
        self.capture('vending-config-final')
        # Native EditBox and original receiver, through the session-scoped native channel.
        self.click(60,10);self.key(269)
        for _ in range(18):self.key(259)
        self.call('text',text='Corner Shop',ticks=8);s=self.snap('renamed')
        self.check('native-name-field-renames-the-real-machine','"text":"Corner Shop"' in s['fixture'])
        self.click(138,34);s=self.snap('top-side')
        self.check('native-side-button-updates-machine','inputSides:2' in s['fixture'])
        # The original right-click popup owns count editing and NBT matching.
        self.click(12,39,button=1);self.capture('vending-popup')
        self.click(16,43);self.key(259);self.call('text',text='4',ticks=3);self.key(257);s=self.snap('popup-count')
        self.check('original-popup-edits-ghost-count',self.count(self.menu(s)['slots'][0]['item'])==4)
        self.click(16,53);s=self.snap('popup-nbt')
        self.check('original-popup-toggles-NBT-matching','matchNBT:254' in s['fixture'])
        self.key(256)
        # Set the fixture's normal price again using the same popup control.
        self.click(12,39,button=1);self.click(16,43);self.key(259);self.call('text',text='2',ticks=3);self.key(257);self.key(256)
        self.cmd('item replace entity @a inventory.0 with minecraft:diamond 3');self.inventory(9);s=self.snap('carrying-diamonds')
        original=self.menu(s)['carried'];self.click(30,39);s=self.snap('ghost-copied')
        self.check('ghost-configuration-keeps-carried-items',self.menu(s)['carried']==original and 'minecraft:diamond' in self.menu(s)['slots'][1]['item'])
        self.inventory(9);self.click(30,39);s=self.snap('ghost-cleared');self.check('empty-cursor-clears-ghost-cell','Count:0b' in self.menu(s)['slots'][1]['item'])
        self.click(12,85);s=self.snap('stock-picked-up');self.check('stock-remains-real-inventory','minecraft:bread' in self.menu(s)['carried'] and self.count(self.menu(s)['carried'])==30)
        self.click(12,85)
        self.call('close');self.call('open-ray',ticks=8);self.call('inventory',ticks=5)
        self.capture('vending-config-final');self.check('configuration-persists-on-reopen','Corner Shop' in self.snap('reopened')['fixture'])
        self.click(141,13);w=self.window('trading')
        self.check('native-Trading-View-transitions-to-customer-window',w['bounds']['width']==184 and w['bounds']['height']==97 and w['composition']['regions'][0]['source']=={'x':4,'y':17,'width':168,'height':65})
        self.cmd('item replace entity @a inventory.0 with minecraft:emerald 12')
        self.click(78,53,'trading');self.key(269);self.key(259);self.call('text',text='2',ticks=4)
        before=self.snap('before-purchase');self.click(130,53,'trading');after=self.snap('after-purchase')
        self.check('native-Trade-button-pays-exact-price',self.count(after['inventory'][9]['item'])==8)
        self.check('native-Trade-button-delivers-batched-result',sum(self.count(i['item']) for i in after['inventory'] if 'minecraft:bread' in i['item'])==6)
        self.check('native-Trade-button-updates-real-stock','Count:24b' in after['fixture'] and 'Count:8b' in after['fixture'])
        self.click(12,26,'trading');after=self.snap('read-only-offer');self.check('customer-offer-cells-are-read-only','Count:0b' in self.menu(after,'trading')['carried'])
        self.cmd('item replace entity @a inventory.0 with minecraft:air');self.click(130,53,'trading');s=self.snap('insufficient-payment')
        self.check('insufficient-payment-leaves-stock-unchanged',s['fixture']==after['fixture'])
        self.capture('vending-trading-final')
        self.call('gui-scale',scale=3,width=1280,height=900,ticks=10);w=self.window('trading');self.check('GUI-scale-three-keeps-customer-window-complete',w['bounds']['x']+w['bounds']['width']<=self.state['guiWidth']);self.capture('vending-trading-scale-three')
        self.call('gui-scale',scale=2,width=1280,height=900,ticks=8)
        self.call('close');self.cmd('gamemode creative @a');self.call('open-ray',ticks=10)
        self.check('creative-vending-button-retained',any(r['id']=='creative-vending' for r in self.window()['composition']['regions']))
        self.click(154, -34);s=self.snap('creative-toggle');self.check('original-creative-mode-toggle-works','Creative:1b' in s['fixture'])
        self.capture('vending-creative')

if __name__=='__main__':Check(sys.argv[1]).run()
