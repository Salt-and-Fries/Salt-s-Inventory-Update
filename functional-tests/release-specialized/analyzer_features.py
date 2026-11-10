"""Actual analyzer decree insertion, computed pools, modes and close-return."""
from pathlib import Path
import collections,hashlib,json,sys
import nbtlib

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
from mowzie_features import stack


def expected_pools(client):
    expected=client['expectedPools'][client['mode']]
    actual=[v['id']for v in client['pools']]
    return bool(expected) and len(actual)==len(set(actual)) and sorted(actual)==sorted(expected)


class Analyzer:
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0
        self.command='server-command'if loader=='neoforge'else'command'
        self.out=self.profile/'analyzer-release';self.out.mkdir()
    def act(self,kind,**kw):return send(self.profile,kind,**kw)
    def cmd(self,text):self.act(self.command,command=text,ticks=5)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=['bountiful-decree-analyzer'],checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Publisher operator-command analyzer entry; real decree input, native pool refresh, objective/reward mode and resolution, moved input, refusal, exact item recovery and native close-return. No bounty generation or all decree distributions.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(name=name,passed=bool(value)));self.save();assert value,name;print('PASS',name,flush=True)
    def state(self,label):
        self.act('regression-analyzer-state',ticks=4);d=json.loads((self.profile/'release-analyzer-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(d,indent=2)+'\n');return d
    @staticmethod
    def empty(text):return not nbtlib.parse_nbt(text)
    @staticmethod
    def inventory(d):return [stack(v['item'])for v in d['inventory']]
    def inv(self,index):self.act('regression-player-click',slot=index,ticks=10)
    def slot(self,sid):self.act('regression-slot-click',session=sid,slot=0,ticks=10)
    def point(self,x,y):
        self.act('regression-analyzer-point',source_x=x,source_y=y,ticks=3)
        return json.loads((self.profile/'release-analyzer-point.json').read_text())
    def tap(self,x,y):
        p=self.point(x,y);self.act('regression-analyzer-mouse',phase='click',**p,ticks=2)
        self.act('regression-analyzer-mouse',phase='release',**p,ticks=16)
    def run(self):
        self.act('close',ticks=3);self.cmd('gamemode survival @p');self.cmd('clear @p')
        self.act('regression-seed-player',items=[dict(slot=9,item='bountiful:decree',count=1,native_decree=True,
            snbt='{release_analyzer_marker:47,typed:[B;2b,9b]}'),dict(slot=10,item='minecraft:diamond',count=3,snbt='{release_analyzer_marker:48}')],ticks=8)
        self.cmd('bo util analyzer');initial=self.state('initial');sid=initial['client']['session']
        self.check('operator-native-entry-is-an-original-analyzer-inventory',initial['client']['menu']=='io.ejekta.bountiful.content.gui.AnalyzerScreenHandler'and initial['client']['screen']=='io.ejekta.bountiful.client.AnalyzerScreen'and self.empty(initial['decree']))
        self.inv(9);self.slot(sid);inserted=self.state('decree-inserted')
        self.check('real-decree-transfers-once-with-full-typed-data',stack(inserted['decree'])==stack(initial['inventory'][9]['item'])and stack(inserted['client']['decree'])==stack(inserted['decree'])and self.empty(inserted['inventory'][9]['item'])and all(self.empty(v['item'])for v in inserted['cursors']))
        self.check('decree-slot-refreshes-the-native-pool-analysis-without-a-forced-refresh',expected_pools(inserted['client'])and bool(inserted['client']['decreeIds']))
        self.tap(161,10);reward=self.state('reward-mode')
        self.check('mode-click-selects-reward-analysis-with-exact-native-pools',reward['client']['mode']=='REW'and expected_pools(reward['client']))
        if reward['client']['locked']:self.act('regression-control',session=sid,control='LOCK',ticks=3)
        self.act('regression-move',session=sid,dx=16,dy=12,ticks=3);moved=self.state('moved')
        self.check('analyzer-window-moves-before-its-next-native-control',moved['client']['bounds']!=reward['client']['bounds'])
        self.tap(161,10);objective=self.state('objective-restored')
        self.check('moved-mode-control-restores-objective-analysis',objective['client']['mode']=='OBJ'and expected_pools(objective['client']))
        # The native vertical slider is at 152,38 and is 18 by 43 pixels.
        start=self.point(160,43);end=self.point(160,77)
        self.act('regression-analyzer-mouse',phase='click',**start,ticks=2)
        self.act('regression-analyzer-mouse',phase='drag',**end,dx=end['x']-start['x'],dy=end['y']-start['y'],ticks=8)
        self.act('regression-analyzer-mouse',phase='release',**end,ticks=8);resolution=self.state('resolution')
        self.check('native-resolution-slider-recomputes-its-pool-widgets',resolution['client']['resolution']>objective['client']['resolution']and expected_pools(resolution['client'])and all(v['pixSize']==resolution['client']['resolution']for v in resolution['client']['pools']))
        self.slot(sid);self.inv(9);recovered=self.state('decree-recovered')
        self.check('decree-can-be-recovered-with-full-original-data',self.empty(recovered['decree'])and self.inventory(recovered)==self.inventory(initial))
        self.inv(10);self.slot(sid);refused=self.state('diamond-refused')
        self.check('decree-slot-refuses-foreign-input-and-keeps-exact-cursor',self.empty(refused['decree'])and any(stack(v['item'])==stack(initial['inventory'][10]['item'])for v in refused['cursors']))
        self.inv(10);self.inv(9);self.slot(sid);before_close=self.state('before-close')
        self.act('close',ticks=8);self.cmd('bo util analyzer');reopened=self.state('reopened')
        # The publisher uses placeItemBackInInventory, which may choose a new
        # empty player cell. Preserve full stacks and quantities without imposing
        # a slot preference that the original analyzer does not promise.
        self.check('native-close-returns-decree-exactly-once-and-new-menu-is-empty',self.empty(reopened['decree'])and collections.Counter(self.inventory(reopened))==collections.Counter(self.inventory(initial))and all(self.empty(v['item'])for v in reopened['cursors'])and stack(before_close['decree'])==stack(initial['inventory'][9]['item']))
        self.act('close',ticks=3);self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),analyzer_features_checked=True)


def run(profile,mc,loader,*args,**kw):return Analyzer(profile,mc,loader).run()
