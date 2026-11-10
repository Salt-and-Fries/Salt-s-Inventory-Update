"""Tool-source selection is native configuration, not physical slot transfer."""
from pathlib import Path
import hashlib, json, sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack, item_count


class Tools:
    def __init__(self, profile, mc):
        self.profile=Path(profile);self.mc=mc;self.checks=[];self.n=0
        self.out=self.profile/'wands-release';self.out.mkdir()
    def act(self, kind, **kw): return send(self.profile,kind,**kw)
    def save(self, complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,checks=self.checks,cases=['native-tools-selector'],
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Native Shift+menu key request; actual tool-source selection, deselection, client/server sync, no physical movement, moved-window input and reopening. Not OS shortcut delivery or world mining/building.'),indent=2)+'\n')
    def check(self,name,condition):
        self.checks.append(dict(name=name,passed=bool(condition)));self.save();assert condition,name
        print('PASS',name,flush=True)
    def state(self,label):
        self.act('regression-wands-state',ticks=3)
        value=json.loads((self.profile/'release-wands-state.json').read_text())
        self.n+=1;(self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(value,indent=2)+'\n')
        return value
    def tap(self,index,button=0): self.act('regression-wands-tap',slot=index,button=button,ticks=10)
    def enter(self): self.act('native-wands-tools-request',ticks=20)
    @staticmethod
    def inventory(value): return [stack(v['item'])for v in value['inventory']]
    @staticmethod
    def empty_cursors(value): return all(item_count(v['item'])[1]==0 for v in value['cursors'])
    def seed(self,index,item,count,marker,wear=False):
        data='{release_wand_marker:'+str(marker)+',typed:[B;3b,7b]}'
        suffix=('{release_wand_marker:'+str(marker)+',typed:[B;3b,7b]'+(',Damage:2'if wear else'')+'}')if self.mc=='1.20.1'else('[minecraft:custom_data='+data+(',minecraft:damage=2'if wear else'')+']')
        self.act('server-command'if self.named else'command',command=f'item replace entity @p container.{index} with {item}{suffix} {count}',ticks=5)
    def run(self,loader):
        self.named=loader=='neoforge';command='server-command'if self.named else'command'
        self.act('close',ticks=3)
        for cmd in ['gamemode survival @p','clear @p','item replace entity @p weapon.mainhand with wands:diamond_wand']:
            self.act(command,command=cmd,ticks=6)
        self.seed(1,'minecraft:diamond_pickaxe',1,17,True)
        self.seed(9,'minecraft:diamond_shovel',1,19,True)
        self.seed(10,'minecraft:cobblestone',7,23)
        self.enter();initial=self.state('initial')
        expected_menu='net.nicguzzo.wands.menues.'+('WandMenu'if self.mc in ['1.20.1','1.21.1','1.21.11']else'WandToolsMenu')
        self.check('original-native-selector-screen-and-menu',initial['client']['menu']==expected_menu and initial['client']['screen']=='net.nicguzzo.wands.client.screens.WandToolScreen')
        # Existing publisher preferences are reset through their real UI, never
        # by writing Tools or calling the native click implementation directly.
        for index in initial['selected']:
            assert isinstance(index,int)and 0<=index<36
            self.tap(index)
        baseline=self.state('baseline')
        self.check('native-preferences-can-be-cleared-without-item-movement',baseline['selected']==[] and baseline['client']['selected']==[] and self.inventory(baseline)==self.inventory(initial) and self.empty_cursors(baseline))
        self.tap(1);one=self.state('hotbar-selected')
        self.check('native-hotbar-tool-selection-reaches-server-and-client',one['selected']==[1]and one['client']['selected']==[1])
        self.tap(9);two=self.state('main-selected')
        self.check('native-main-inventory-tool-selection-reaches-server-and-client',set(two['selected'])=={1,9}and set(two['client']['selected'])=={1,9})
        self.tap(9,1);right=self.state('right-click')
        self.check('native-right-click-is-nondestructive-no-toggle',right['selected']==two['selected']and right['client']['selected']==two['client']['selected']and self.inventory(right)==self.inventory(baseline)and self.empty_cursors(right))
        self.act('regression-wands-unlock',ticks=3)
        self.act('regression-move',session=two['client']['session'],dx=16,dy=12,ticks=3)
        moved=self.state('moved');self.check('selector-window-moves-before-deselection',moved['client']['bounds']!=two['client']['bounds'])
        self.tap(1);deselected=self.state('hotbar-deselected')
        self.check('moved-selector-deselects-only-clicked-source',deselected['selected']==[9]and deselected['client']['selected']==[9])
        self.act('close',ticks=5);self.enter();reopened=self.state('reopened')
        self.check('native-selection-persists-through-key-request-reopening',reopened['selected']==[9]and reopened['client']['selected']==[9])
        self.check('all-full-item-data-and-cursors-preserved-through-selection',self.inventory(reopened)==self.inventory(baseline)and self.empty_cursors(reopened))
        self.tap(9);final=self.state('cleared')
        self.check('second-source-deselection-remains-independent-and-conservative',final['selected']==[]and final['client']['selected']==[]and self.inventory(final)==self.inventory(baseline)and self.empty_cursors(final))
        self.act('close',ticks=3);self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),wands_features_checked=True)


def run(profile,mc,loader,*args,**kw): return Tools(profile,mc).run(loader)
