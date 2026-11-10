"""Native terminal-to-status entry, actual nine-CPU selection and moved input."""
from pathlib import Path
import copy,hashlib,json
from ae2_settings_features import Settings,ENTRIES,isolate,translate,pair,ledger,MARKER,marker_item,stack

MENU='appeng.menu.me.crafting.CraftingStatusMenu'
SCREEN='appeng.client.gui.me.crafting.CraftingStatusScreen'
CASE='native-cpu-status'
NAMES=['native terminal entry retains typed player marker and empty cursor',
    'original Crafting Status button opens the real server-backed child',
    'native list contains nine distinct real idle CPUs and requires scrolling',
    'original wheel input reaches the final native CPU rows',
    'original final-row click selects the same actual CPU on the server',
    'CPU status drag preserves authoritative selected CPU and list',
    'moved original wheel input returns to the first native rows',
    'moved first-row click selects the first actual server CPU',
    'ordinary terminal reopening and native child entry retain all real CPUs',
    'all CPU status sessions close without changing owned items']
SCOPE='Original block-use terminal entry and native Crafting Status button. Nine separate real CPUs on a powered controller/dense-cable fixture; native wheel navigation and pointer selection, server/client agreement, moved input, ordinary reopening and complete typed foreign-item/cursor conservation. No client list/status assignment or direct selection callback. Genuine active-job controls are covered by the separate CPU lifecycle/control workflows, not inferred from this idle selector.'


def synchronized(native,client):
    cs=[v for v in native['client']if v['session']==client['session']and v['menu']==MENU]
    ss=[v for v in native['server']if v['session']==client['session']and v['menu']==MENU]
    if len(cs)!=1 or len(ss)!=1:return False
    c,s=cs[0],ss[0]
    if c.get('screen')!=SCREEN:return False
    for value in [c,s]:
        if value['ready']is not True or value['available']is not True or value['active']is not False:return False
        if type(value['selected'])is not int or len(value['cpus'])!=9 or len(set(value['cpus']))!=9:return False
        if not all(type(v)is int and v>=0 for v in value['cpus']):return False
        if value['selected']not in value['cpus']:return False
    return c['cpus']==s['cpus']and c['selected']==s['selected']


class CpuStatus(Settings):
    def __init__(self,profile,mc,loader):
        assert(mc,loader)==('1.21.1','neoforge')
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.case=CASE;self.cases=[];self.checks=[];self.n=0;self.baseline=None;self.entry_records=[]
        self.out=self.profile/'ae2-cpu-status-release';self.out.mkdir();self.command='server-command'
        self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,original_entries=self.entry_records,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope=SCOPE),indent=2)+'\n')
    def frame(self,label,status=True):
        self.act('regression-ae2-child-state',ticks=4);widgets=json.loads((self.profile/'release-ae2-child-state.json').read_text())
        native=None
        if status:
            self.act('regression-ae2-cpu-status-state',ticks=4);native=json.loads((self.profile/'release-ae2-cpu-status-state.json').read_text())
            self.client,_=pair(widgets,'CraftingStatusMenu')
            assert synchronized(native,self.client),'Native CPU status did not synchronize'
            self.c=next(v for v in native['client']if v['session']==self.client['session'])
            self.s=next(v for v in native['server']if v['session']==self.client['session'])
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text())
        if self.baseline is not None:assert ledger(data)==self.baseline,'CPU status input changed owned items/cursor'
        self.n+=1;(self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(dict(widgets=widgets,native=native,ledger=data),indent=2)+'\n')
        return widgets,data
    def open_status(self,widgets):
        found=[(c,w)for c in widgets['client']for w in c['widgets']if w['visible']is True and w['active']is True and
            ('Crafting Status'in [w['message'],*w.get('tooltips',[])]or w.get('icon')=='CRAFTING_STATUS')]
        assert len(found)==1,'Expected the original terminal Crafting Status button'
        self.tap(*found[0])
    def wheel(self,direction):
        # One planned traversal of the known nine-row list, rather than retries
        # of a failed action. Observe only after all ordinary wheel steps.
        for _ in range(9):self.act('regression-ae2-cpu-wheel',session=self.client['session'],direction=direction,ticks=2)
    def click_row(self,last):
        c=self.c;rect=c['selection'];row=c['visibleRows']-1 if last else 0
        self.act('regression-tap',session=self.client['session'],x=c['left']+rect['x']+15,
            y=c['top']+rect['y']+(0 if c['narrow']else 19)+row*23+5,ticks=18)
    def setup(self):
        self.act('close',ticks=3)
        for text in isolate(self.origin):self.cmd(text)
        self.cmd('clear @a')
        entry=next(v for v in json.loads(ENTRIES.read_text())['cases']if v['id']=='atlas-ae2-terminal'and(v['minecraft'],v['loader'])==(self.mc,self.loader))
        old=[int(v)for v in entry['captured_origin'].split(',')];actions=[]
        for original in entry['actions']:
            action=copy.deepcopy(original);kind=action.pop('type');assert kind in {'fixture','command','use-block-ray'}
            if kind=='use-block-ray':continue
            if kind=='command':action['command']=translate(action['command'],old,self.origin);kind=self.command
            self.act(kind,**action);actions.append(dict(type=kind,**action))
        x,y,z=self.origin;world=[]
        for dx in range(1,18):world.append(f'setblock {x+dx} {y} {z} ae2:cable_bus{{cable:{{id:"ae2:fluix_smart_dense_cable"}}}}')
        world += [f'setblock {x-1} {y} {z} ae2:creative_energy_cell',f'setblock {x+1} {y+1} {z} ae2:controller']
        world += [f'setblock {x+dx} {y} {z+dz} ae2:64k_crafting_storage'for dx,dz in [*[(i,1)for i in range(2,18,2)],(2,-1)]]
        for command in world:self.cmd(command)
        self.cmd('item replace entity @a container.12 with minecraft:diamond[minecraft:custom_data='+MARKER+'] 7')
        self.cmd('gamemode survival @a');self.act('wait',ticks=70);self.act('use-block-ray',ticks=24);actions.append(dict(type='use-block-ray',ticks=24))
        widgets,data=self.frame('original-terminal',False);client,_=pair(widgets,'MEStorageMenu')
        marker=next(v for v in data['inventory']if v['index']==12)
        self.check(NAMES[0],client['screen']=='appeng.client.gui.me.common.MEStorageScreen'and data['cursor']['count']==0 and stack(marker['signature'],marker['count'])==stack(marker_item(self.mc)))
        self.baseline=ledger(data);self.entry_records.append(dict(original_capture=entry,executed_actions=actions,additional_world_commands=world,
            actual_entry='Original terminal block-use, followed by original Crafting Status widget. No forced provider or menu constructor.'))
        self.open_status(widgets)
    def run(self):
        self.setup();self.frame('native-status-child');ids=copy.deepcopy(self.c['cpus']);first=ids[0];last=ids[-1]
        self.check(NAMES[1],self.client['screen']==SCREEN and synchronized(dict(client=[self.c],server=[self.s]),self.client))
        rows=self.c['visibleRows'];rect=self.c['selection']
        self.check(NAMES[2],type(rows)is int and 0<rows<9 and rect['width']>0 and rect['height']>0 and self.c['scroll']==0)
        self.wheel(-1);self.frame('scrolled-last');self.check(NAMES[3],self.c['cpus']==ids and self.c['scroll']==9-rows)
        self.click_row(True);self.frame('selected-last');self.check(NAMES[4],self.c['selected']==self.s['selected']==last)
        bounds=copy.deepcopy(self.client['bounds']);self.move(self.client);self.frame('status-moved')
        self.check(NAMES[5],self.client['bounds']['x']==bounds['x']+16 and self.client['bounds']['y']==bounds['y']+12 and self.c['cpus']==ids and self.c['selected']==last)
        self.wheel(1);self.frame('moved-scrolled-first');self.check(NAMES[6],self.c['scroll']==0 and self.c['selected']==last)
        self.click_row(False);self.frame('moved-selected-first');self.check(NAMES[7],self.c['selected']==self.s['selected']==first)
        self.reopen();widgets,_=self.frame('ordinary-terminal-reopened',False);pair(widgets,'MEStorageMenu');self.open_status(widgets);self.frame('ordinary-status-reopened')
        self.check(NAMES[8],self.c['cpus']==ids and self.c['selected']==self.s['selected']and self.c['scroll']==0)
        self.act('close',ticks=4);self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());(self.out/'closed.json').write_text(json.dumps(data,indent=2)+'\n')
        self.check(NAMES[9],not data['menus']and ledger(data)==self.baseline)
        self.cases=[CASE];self.save(True);return dict(cases=1,failed=0,checks=len(self.checks),ae2_cpu_status_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return CpuStatus(profile,mc,loader).run()
