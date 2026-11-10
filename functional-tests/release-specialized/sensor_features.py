"""Exact native sensor configuration changes, not arbitrary snapshot inequality."""
from pathlib import Path
import hashlib,json,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack


class Sensor:
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0
        self.command='server-command'if loader=='neoforge'else'command'
        self.out=self.profile/'sensor-release';self.out.mkdir()
    def act(self,kind,**kw):return send(self.profile,kind,**kw)
    def cmd(self,text):self.act(self.command,command=text,ticks=5)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=['rocket-sensor'],checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Native zero-slot sensor inversion and explicit sensing-mode changes, authoritative/client synchronization, moved input, no item movement, ordinary reopening and source invalidation. Not rocket flight or redstone automation.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(name=name,passed=bool(value)));self.save();assert value,name;print('PASS',name,flush=True)
    def state(self,label):
        self.act('regression-sensor-state',ticks=4);d=json.loads((self.profile/'release-sensor-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(d,indent=2)+'\n');return d
    def invert(self,d):
        b=d['client']['checkbox'];assert b['active']and b['visible']
        self.act('regression-template-widget-tap',session=d['client']['session'],widget_type=b['type'],message=b['message'],ticks=16)
    def choose(self,d,choice):
        self.act('regression-template-region-tap',session=d['client']['session'],region='native-working-apparatus',
            source_x=choice['source_x'],source_y=choice['source_y'],ticks=16)
    @staticmethod
    def inventory(d):return [stack(v['item'])for v in d['inventory']]
    def run(self):
        self.act('close',ticks=3);self.cmd('gamemode survival @p');self.cmd('clear @p')
        data='{release_sensor_marker:17,typed:[B;2b,5b]}'
        suffix=data if self.mc=='1.20.1'else'[minecraft:custom_data='+data+']'
        self.cmd('item replace entity @p container.9 with minecraft:diamond'+suffix+' 3')
        self.act('fixture',block='ad_astra_giselle_addon:rocket_sensor',ticks=5)
        self.act('client-use-block',ticks=18);initial=self.state('initial');sid=initial['client']['session']
        self.check('original-native-zero-slot-sensor-opens',initial['client']['menu']=='ad_astra_giselle_addon.common.menu.RocketSensorMenu'and initial['client']['screen']=='ad_astra_giselle_addon.client.screen.RocketSensorScreen')
        self.check('initial-native-settings-match-client-and-server',initial['inverted']==initial['client']['inverted']and initial['sensingType']==initial['client']['sensingType'])
        self.invert(initial);changed=self.state('inverted')
        self.check('inversion-click-changes-exact-native-boolean',changed['inverted']is not initial['inverted']and changed['client']['inverted']==changed['inverted'])
        choices=[v for v in changed['client']['choices']if v['type'].endswith(':found')]
        assert len(choices)==1,'Exact native FOUND sensing option required';wanted=choices[0]
        self.choose(changed,wanted);selected=self.state('found-selected')
        self.check('native-list-click-selects-the-requested-mode',selected['sensingType']==wanted['type']and selected['client']['sensingType']==wanted['type']and selected['sensingType']!=initial['sensingType'])
        if selected['client']['locked']:self.act('regression-control',session=sid,control='LOCK',ticks=3)
        self.act('regression-move',session=sid,dx=16,dy=12,ticks=3);moved=self.state('moved')
        self.check('sensor-moves-before-next-native-option-input',moved['client']['bounds']!=selected['client']['bounds'])
        self.invert(moved);restored=self.state('inversion-restored')
        self.check('moved-checkbox-restores-only-inversion',restored['inverted']==initial['inverted']and restored['sensingType']==wanted['type']and restored['client']['inverted']==restored['inverted'])
        self.act('close',ticks=4);self.act('client-use-block',ticks=18);reopened=self.state('reopened')
        self.check('native-sensor-configuration-persists-through-ordinary-reopening',reopened['inverted']==restored['inverted']and reopened['sensingType']==wanted['type']and reopened['client']['sensingType']==wanted['type'])
        self.check('settings-preserve-complete-inventory-and-empty-cursor',self.inventory(reopened)==self.inventory(initial)and all(not nbtlib.parse_nbt(v['item'])for v in reopened['cursors']))
        sid=reopened['client']['session'];self.act('remove-source',ticks=16);self.act('regression-state',ticks=3)
        d=json.loads((self.profile/'regression-state.json').read_text());self.check('sensor-source-removal-closes-its-session',not any(v['session']==sid for v in d['menus']))
        self.act('close',ticks=3);self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),sensor_features_checked=True)


def run(profile,mc,loader,*args,**kw):return Sensor(profile,mc,loader).run()
