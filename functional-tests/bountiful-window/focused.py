"""Focused live acceptance for Bountiful's board window, not a gameplay suite."""
from pathlib import Path
import json,sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'project-everything-ten'))
from send import send

class Check:
    def __init__(self,out):
        self.out=Path(out);self.folder=self.out/'bountiful-checks';self.folder.mkdir(exist_ok=True);self.state={};self.results=[]
    def call(self,kind,**args):self.state=send(self.out,kind,**args)['client'];return self.state
    def window(self):return next(w for w in self.state['allWindows'] if w.get('menuId')=='bountiful:board')
    def info(self):return self.window()['nativeValues']
    def check(self,name,passed):
        self.results.append({'id':name,'passed':bool(passed)});(self.folder/'journal.json').write_text(json.dumps({'scope':'Bountiful board geometry, selection, scrolling, bounty pickup, decree cells, reputation display and reopening only','checks':self.results},indent=2))
        assert passed,name;print('PASS',name,flush=True)
    def cmd(self,text):self.call('command',command=text,ticks=6)
    def point(self,x,y):b=self.window()['bounds'];return b['x']+6+x,b['y']+16+y
    def tap(self,x,y):self.call('click',x=x,y=y,button=0,ticks=1);self.call('release',x=x,y=y,button=0,ticks=6)
    def click(self,x,y):self.tap(*self.point(x,y))
    def inventory(self,index):
        if not any(w['kind']=='INVENTORY' for w in self.state['workspaceWindows']):self.call('inventory',ticks=5)
        w=next(w for w in self.state['workspaceWindows'] if w['kind']=='INVENTORY');p=next(p for p in w['inventoryCells'] if p['inventorySlot']==index);self.tap(p['x'],p['y'])
    def grid(self,index):self.click(190+index%7*18,24+index//7*18)
    def snap(self,name):
        self.call('snapshot-server',ticks=2);s=json.loads((self.out/'server-state.json').read_text());(self.folder/(name+'.json')).write_text(json.dumps({'client':self.state,'server':s},indent=2));return s
    def menu(self,s):return next(m for m in s['menus'] if m['class']==self.window()['menu'])
    def capture(self,name):self.call('move',x=639,y=449,ticks=30);self.call('capture',capture=name,ticks=8);(self.folder/(name+'-client.json')).write_text(json.dumps(self.state,indent=2))
    def setup(self):
        self.call('close');self.call('fixture',block='bountiful:bountyboard',ticks=8);self.cmd('gamemode survival @a')
        f=json.loads(Path(__file__).with_name('fixture.json').read_text());self.cmd('data merge block 8 65 8 '+f['block_nbt'])
        self.call('open-ray',ticks=20);self.call('inventory',ticks=8)
    def run(self):
        self.setup();b=self.window()['bounds']
        self.check('compact-348-by-120-window',b['width']==348 and b['height']==120)
        self.check('four-original-bounty-preview-rows',self.info()['lastBountyRow']-self.info()['firstBountyRow']==3)
        self.check('all-21-board-and-three-decree-cells-retained',len([s for s in self.window()['slots'] if not s['player']])==25 and self.window()['slots'][21]['x']<0)
        before=list(self.info()['bountyIndices']);self.check('native-generated-offers-load',len(before)>=8)
        self.capture('bountiful-overview')
        self.click(35,24);s=self.snap('first-selected');index=before[0]
        self.check('first-preview-selects-native-board-index',self.info()['selectedBounty']==index)
        self.check('server-selected-alias-has-exact-bounty-data',self.menu(s)['slots'][21]['item']==self.menu(s)['slots'][index]['item'])
        self.click(35,84);self.check('fourth-preview-selects-correct-bounty',self.info()['selectedBounty']==self.info()['bountyIndices'][3])
        self.capture('bountiful-selected')
        self.call('close');self.call('open-ray',ticks=20)
        self.check('selection-alone-does-not-take-any-bounties',all(i in self.info()['bountyIndices'] for i in before))
        x,y=self.point(80,45)
        for _ in range(30):
            if self.info()['firstBountyRow']>=len(self.info()['bountyIndices'])-4:break
            self.call('scroll',x=x,y=y,amount=-1,ticks=2)
        self.check('wheel-reaches-final-four-bounties',self.info()['firstBountyRow']==len(self.info()['bountyIndices'])-4)
        last=self.info()['bountyIndices'][-1];self.click(35,84)
        self.check('last-visible-preview-selects-last-board-bounty',self.info()['selectedBounty']==last)
        self.capture('bountiful-last-page')
        x,y=self.point(166,80);self.call('click',x=x,y=y,button=0,ticks=1)
        nx,ny=self.point(166,15);self.call('drag',x=nx,y=ny,dx=0,dy=ny-y,button=0,ticks=3);self.call('release',x=nx,y=ny,button=0,ticks=5)
        self.check('native-scrollbar-thumb-drags-toward-top',self.info()['firstBountyRow']<len(self.info()['bountyIndices'])-4)
        s=self.snap('before-taking');original=self.menu(s)['slots'][last]['item']
        self.grid(last);s=self.snap('taken-bounty')
        self.check('board-cell-pickup-preserves-full-bounty',self.menu(s)['carried']==original)
        self.inventory(9);s=self.snap('bounty-stored')
        self.check('bounty-enters-separate-player-inventory',s['inventory'][9]['item']==original)
        self.call('close');self.call('open-ray',ticks=20)
        self.check('taken-bounty-stays-masked-on-reopen',last not in self.info()['bountyIndices'])
        s=self.snap('decree-before');decree=self.menu(s)['slots'][22]['item']
        self.click(326,24);self.inventory(10);s=self.snap('decree-withdrawn')
        self.check('native-decree-withdrawal-preserves-data',s['inventory'][10]['item']==decree and 'Count:0b' in self.menu(s)['slots'][22]['item'])
        self.inventory(10);self.click(326,42);s=self.snap('decree-reinserted')
        self.check('second-decree-cell-accepts-native-decree',self.menu(s)['slots'][23]['item']==decree)
        self.cmd('item replace entity @a inventory.2 with minecraft:cobblestone 3');self.inventory(11);self.click(326,60);s=self.snap('non-decree-rejected')
        self.check('decree-cell-rejects-ordinary-items','minecraft:cobblestone' in self.menu(s)['carried'] and 'Count:0b' in self.menu(s)['slots'][24]['item'])
        self.inventory(11)
        # Fixture-only reputation change, exercising the same native data slot.
        value=json.dumps({'14ea713f-e12d-3d8b-b7b6-baf52d52d598':12},separators=(',',':'))
        self.cmd("data modify block 8 65 8 completed set value '"+value+"'")
        self.check('live-reputation-data-reaches-native-bar',self.info()['completed']==12)
        self.capture('bountiful-reputation')
        self.call('move',x=self.point(180,86)[0],y=self.point(180,86)[1],ticks=8);self.call('capture',capture='bountiful-reputation-tooltip',ticks=5)
        self.call('gui-scale',scale=3,width=1280,height=900,ticks=12)
        self.check('GUI-scale-three-keeps-complete-window',self.window()['bounds']['width']==348 and self.window()['bounds']['height']==120 and self.window()['bounds']['x']+348<=self.state['guiWidth'])
        self.capture('bountiful-scale-three');self.call('gui-scale',scale=2,width=1280,height=900,ticks=10)
        self.capture('bountiful-final')

if __name__=='__main__':Check(sys.argv[1]).run()
