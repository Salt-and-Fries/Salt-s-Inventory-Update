"""Actual native selector buttons and changing solar/energy status, not item transfers."""
from pathlib import Path
import hashlib,json,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack


class TechRebornStatus:
    def __init__(self,profile,mc,loader):
        assert loader=='fabric'
        self.profile=Path(profile);self.mc=mc;self.checks=[];self.n=0;self.cases=[]
        self.out=self.profile/'techreborn-status-release';self.out.mkdir()
    def act(self,kind,**kw):return send(self.profile,kind,**kw)
    def cmd(self,text,ticks=5):self.act('command',command=text,ticks=ticks)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Four native zero-item machine panels: all radius/launch selector arrows and exact clamping, moved input, persistence, native solar generation/obstruction and elevator energy synchronization, complete physical-item preservation and source cleanup. Not teleportation, launching players, detector automation or a complete Tech Reborn gameplay suite.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(case=self.cases[-1],name=name,passed=bool(value)));self.save();assert value,name;print('PASS',name,flush=True)
    def state(self,label):
        self.act('regression-techreborn-status',ticks=4);d=json.loads((self.profile/'release-techreborn-status.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+self.cases[-1]+'-'+label+'.json')).write_text(json.dumps(d,indent=2)+'\n');return d
    @staticmethod
    def inventory(d):return [stack(v['item'])for v in d['inventory']]
    def arrow(self,d,index):
        b=d['client']['buttons'][index];assert b['active']and b['visible']
        self.act('regression-template-region-tap',session=d['client']['session'],region='native-machine',source_x=b['source_x'],source_y=b['source_y'],ticks=16)
    def selection(self,d,value,label):
        self.check(label,d['native']['selection']==value and d['client']['native']['selection']==value)
    def above(self,d,block):
        p=d['position'];self.cmd(f'setblock {p["x"]} {p["y"]+1} {p["z"]} {block}',ticks=45)
    def run(self):
        self.act('close',ticks=3);self.cmd('gamemode survival @p');self.cmd('clear @p')
        self.cmd('gamerule doDaylightCycle false');self.cmd('time set noon');self.cmd('weather clear')
        tag='{release_tr_status_marker:29,typed:[B;3b,8b]}'
        suffix=tag if self.mc=='1.20.1'else'[minecraft:custom_data='+tag+']'
        self.cmd('item replace entity @p container.9 with minecraft:diamond'+suffix+' 3')
        for key,screen in [('elevator','GuiElevator'),('launchpad','GuiLaunchpad'),('player_detector','GuiPlayerDetector'),('basic_solar_panel','GuiSolar')]:
            self.cases.append(key);self.act('close',ticks=3);self.act('fixture',block='techreborn:'+key,ticks=8)
            self.act('client-use-block',ticks=18);initial=self.state('initial');sid=initial['client']['session']
            self.check('original-native-zero-item-panel-opens',initial['physical_slots']==0 and initial['client']['menu']=='reborncore.common.screen.BuiltScreenHandler'and initial['client']['screen']=='techreborn.client.gui.'+screen)
            if initial['client']['locked']:self.act('regression-control',session=sid,control='LOCK',ticks=3)
            self.act('regression-move',session=sid,dx=18,dy=14,ticks=3);moved=self.state('moved')
            self.check('native-panel-moves-before-input',moved['client']['bounds']!=initial['client']['bounds'])
            if key in ['launchpad','player_detector']:
                self.check('all-four-native-selector-arrows-remain-present',len(moved['client']['buttons'])==4 and all(b['visible']and b['active']for b in moved['client']['buttons']))
                minimum=moved['native']['minimum'];maximum=moved['native']['maximum'];bulk=maximum if key=='launchpad'else 16
                previous=moved
                for ordinal,delta in [(3,-bulk),(1,1),(2,-1),(0,bulk),(3,-bulk)]:
                    wanted=max(minimum,min(maximum,previous['native']['selection']+delta))
                    self.arrow(previous,ordinal);current=self.state('arrow-'+str(ordinal))
                    self.selection(current,wanted,'native-arrow-'+str(ordinal)+'-changes-exact-selection')
                    previous=current
                self.act('close',ticks=4);self.act('client-use-block',ticks=18);reopened=self.state('reopened')
                self.selection(reopened,previous['native']['selection'],'native-selector-persists-on-ordinary-reopening')
            elif key=='basic_solar_panel':
                self.act('wait',ticks=40);generating=self.state('sunlight')
                self.check('native-solar-panel-really-generates-energy',generating['native']['generating']and generating['client']['native']['generating']and generating['native']['rate']>0 and generating['native']['energy']>initial['native']['energy'])
                self.above(generating,'minecraft:stone');blocked=self.state('blocked')
                self.check('native-roof-obstruction-stops-generation-in-both-views',blocked['native']['generating']is False and blocked['client']['native']['generating']is False and blocked['native']['rate']==0)
                self.act('wait',ticks=25);stopped=self.state('stopped')
                self.check('blocked-panel-energy-stays-stable-and-synchronizes',stopped['native']['energy']==blocked['native']['energy']==stopped['client']['native']['energy'])
                self.above(stopped,'minecraft:air');restored=self.state('sunlight-restored')
                self.check('removing-native-obstruction-restores-real-generation',restored['native']['generating']and restored['client']['native']['generating']and restored['native']['energy']>stopped['native']['energy'])
                self.act('close',ticks=4);self.act('client-use-block',ticks=18);reopened=self.state('reopened')
                self.check('solar-status-persists-on-ordinary-reopening',reopened['native']['generating']and reopened['client']['native']['generating'])
            else:
                self.above(moved,'techreborn:basic_solar_panel');charged=self.state('native-solar-charge')
                self.check('elevator-status-observes-native-adjacent-energy-generation',charged['native']['energy']>initial['native']['energy']and 0<charged['client']['native']['energy']<=charged['native']['energy'])
                self.above(charged,'minecraft:air');stable=self.state('charge-stopped')
                self.check('elevator-energy-bar-synchronizes-after-production-stops',stable['native']['energy']>0 and stable['native']['energy']==stable['client']['native']['energy'])
                self.act('close',ticks=4);self.act('client-use-block',ticks=18);reopened=self.state('reopened')
                self.check('native-elevator-energy-persists-on-ordinary-reopening',reopened['native']['energy']==stable['native']['energy']==reopened['client']['native']['energy'])
            self.check('native-controls-preserve-full-items-and-empty-cursors',self.inventory(reopened)==self.inventory(initial)and all(not nbtlib.parse_nbt(v['item'])for v in reopened['cursors']))
            sid=reopened['client']['session'];self.act('remove-source',ticks=16);self.act('regression-state',ticks=3)
            end=json.loads((self.profile/'regression-state.json').read_text());self.check('source-removal-closes-the-original-native-session',not any(v['session']==sid for v in end['menus']))
        self.act('close',ticks=3);self.save(True)
        return dict(cases=len(self.cases),failed=0,checks=len(self.checks),techreborn_status_features_checked=True)


def run(profile,mc,loader,*args,**kw):return TechRebornStatus(profile,mc,loader).run()
