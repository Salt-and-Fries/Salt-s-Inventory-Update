"""Focused SSN release-client checks. Does not run the full gameplay suite."""
from pathlib import Path
import json,sys,argparse
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send

class Check:
    def __init__(self,run):
        self.run=Path(run);self.out=self.run/'ssn-checks';self.out.mkdir(exist_ok=True);self.state={}
        self.path=self.out/'journal.json';self.results=json.loads(self.path.read_text())if self.path.exists()else {'scope':'SSN affected-menu checks only','checks':[]}
    def call(self,kind,**args):
        self.state=send(self.run,kind,**args)['client'];return self.state
    def passed(self,name,value):
        self.results['checks'].append(dict(id=name,passed=bool(value)));self.path.write_text(json.dumps(self.results,indent=2));assert value,name;print('PASS',name,flush=True)
    def w(self,path):return next(w for w in self.state.get('allWindows',[])if w.get('menuId')=='storagenetwork:'+path)
    def point(self,path,x,y):
        w=self.w(path);b=w['bounds'];p=w['composition']
        for r in reversed(p['regions']):
            s=r['source']
            if s['x']<=x<s['x']+s['width']and s['y']<=y<s['y']+s['height']and not any(c['x']<=x<c['x']+c['width']and c['y']<=y<c['y']+c['height']for c in r['cutouts']):
                return dict(x=b['x']+7+p['padding']+r['x']+x-s['x']-w['hostScrollX'],y=b['y']+26+p['padding']+r['y']+y-s['y']-w['hostScrollY'])
        raise AssertionError((path,x,y))
    def tap(self,p,button=0):
        self.call('move',**p,ticks=2);self.call('click',**p,button=button,ticks=1);self.call('release',**p,button=button,ticks=5)
    def search(self,path,text):
        w=self.w(path);s=w['nativeValues']['searchBounds'];b=w['nativeBounds'];p=self.point(path,s['x']-b['x']+10,s['y']-b['y']+4)
        self.tap(p,1)
        if text:self.call('text',text=text,ticks=7)
        self.passed(path+'-search-'+(text or 'clear'),self.w(path)['nativeValues']['search']==text)
    def cmd(self,text):self.call('command',command=text,ticks=4)
    def snap(self,name):
        self.call('snapshot-server',ticks=3);d=json.loads((self.run/'server-state.json').read_text());(self.out/(name+'.json')).write_text(json.dumps(d,indent=2));return d
    def native_settings(self,d):return next(m.get('settings',{})for m in d['menus']if '.storagenetwork.'in m.get('class',''))
    def slot(self,path,index,button=0):
        s=next(s for s in self.w(path)['slots']if s['index']==index);self.tap(self.point(path,s['x']+8,s['y']+8),button)
    def inventory(self,index=9):
        if not any(w['kind']=='INVENTORY'for w in self.state.get('workspaceWindows',[])):self.call('inventory',ticks=5)
        w=next(w for w in self.state['workspaceWindows']if w['kind']=='INVENTORY')
        c=next(c for c in w['inventoryCells']if c['inventorySlot']==index);self.tap(dict(x=c['x'],y=c['y']))
    def insert(self,path,index,item,count=1):
        self.cmd(f'item replace entity @a inventory.0 with {item} {count}');self.inventory();self.slot(path,index)
    def open(self,path,network=False,remote=False):
        self.call('close');self.call('gui-scale',scale=1 if 'expanded'in path else 2,ticks=4)
        self.cmd('gamemode survival @a')
        if network:
            self.call('ssn-setup',block='storagenetwork:'+('master'if remote else path),ticks=30)
            if remote:
                self.call('held',item='storagenetwork:'+path,ticks=3);self.call('open',ticks=6);self.call('use-item',ticks=16)
            else:self.call('open',ticks=16)
        else:self.call('fixture',block='storagenetwork:'+path,ticks=8);self.call('open',ticks=12)
        self.w(path)
    def capture(self,name):
        name=name.replace('_','-')
        self.call('move',x=2,y=2,ticks=2);self.call('capture',capture='ssn-'+name,ticks=3)
        (self.out/(name+'-client.json')).write_text(json.dumps(self.state,indent=2))
    def geometry(self,modern=False):
        cases=[('inventory',0,True,False),('request',10,True,False),('request_expanded',10,True,False),('inventory_remote',0,True,True),('crafting_remote',10,True,True),('expanded_remote',10,True,True),('import_filter_kabel',4,False,False),('export_kabel',4,False,False),('filter_kabel',0,False,False),('collector',0,False,False)]
        if modern:cases += [('process_kabel',0,False,False),('cradle',9,False,False),('receiver',1,False,False)]
        for path,slots,network,remote in cases:
            self.open(path,network,remote);w=self.w(path)
            self.passed(path+'-native-template',w['templateClass']=='com.salts_inventory_update.compat.ssn.SsnClient$Template'and not w['layoutError'])
            self.passed(path+'-native-slot-count',len([s for s in w['slots']if not s['player']])==slots)
            if network:self.passed(path+'-native-list-updates',len(w['nativeValues']['stacks'])==27)
            self.call('inventory',ticks=4);self.capture(path)
    def terminal(self,path='inventory',remote=False):
        self.open(path,True,remote);self.call('inventory',ticks=4)
        self.search(path,'diamond');self.capture(path+'-search')
        self.tap(self.point(path,16,18));self.inventory(10);d=self.snap(path+'-withdraw')
        self.passed(path+'-native-network-withdraw-to-inventory','minecraft:diamond'in d['inventory'][10]['item'])
        self.inventory(10);self.tap(self.point(path,16,18));d=self.snap(path+'-deposit')
        self.passed(path+'-native-network-deposit',d['inventory'][10]['item']=='{}'or'minecraft:air'in d['inventory'][10]['item'])
        self.search(path,'')
        # Publisher order controls, not redundant chest sorting.
        y=100 if path in ['request','crafting_remote']else 164
        self.tap(self.point(path,14,y));self.passed(path+'-native-direction-toggle',self.w(path)['nativeValues']['getDownwards']=='true')
        self.tap(self.point(path,30,y));self.passed(path+'-native-sort-toggle',self.w(path)['nativeValues']['getSort']!='NAME')
        focus=self.w(path)['nativeValues']['getAutoFocus'];self.tap(self.point(path,170,y));self.passed(path+'-native-autofocus-toggle',self.w(path)['nativeValues']['getAutoFocus']!=focus)
        self.capture(path+'-functional')
    def crafting(self,path='request',remote=False):
        self.open(path,True,remote);self.call('inventory',ticks=3);self.insert(path,1,'minecraft:oak_log',8)
        d=self.snap(path+'-craft-ready');m=next(m for m in d['menus']if '.storagenetwork.'in m['class'])
        self.passed(path+'-native-crafting-result','minecraft:oak_planks'in m['slots'][0]['item'])
        self.slot(path,0);self.inventory(10);d=self.snap(path+'-craft-pickup')
        self.passed(path+'-native-crafting-output-to-inventory','minecraft:oak_planks'in d['inventory'][10]['item'])
        # The publisher clear-grid button is just below the native order buttons.
        self.tap(self.point(path,65,112));d=self.snap(path+'-clear')
        m=next(m for m in d['menus']if '.storagenetwork.'in m['class']);self.passed(path+'-native-grid-clearing',all(s['item']=='{}'or'minecraft:air'in s['item']for s in m['slots'][1:10]))
        self.capture(path+'-crafted')
    def filters(self,only=None):
        for path in ([only]if only else ['filter_kabel','collector','import_filter_kabel','export_kabel']):
            self.open(path);self.call('inventory',ticks=3)
            self.cmd('item replace entity @a inventory.0 with minecraft:diamond 16');self.inventory(9)
            ghost=self.point(path,16,53 if path in ['import_filter_kabel','export_kabel']else 43)
            self.tap(ghost);d=self.snap(path+'-ghost')
            self.passed(path+'-server-ghost-filter','minecraft:diamond'in json.dumps(self.native_settings(d)))
            if path in ['import_filter_kabel','export_kabel']:
                before=self.native_settings(d)
                self.call('move',**ghost,ticks=2);self.call('scroll',**ghost,amount=1,ticks=5);d=self.snap(path+'-ghost-scroll')
                self.passed(path+'-native-ghost-scroll',before!=self.native_settings(d))
            self.inventory(9);d=self.snap(path+'-ghost-nonconsuming')
            self.passed(path+'-ghost-does-not-consume-item','minecraft:diamond'in d['inventory'][9]['item'])
            self.capture(path+'-filter')
            self.tap(ghost);d=self.snap(path+'-ghost-clear')
            self.passed(path+'-ghost-clear','minecraft:diamond'not in json.dumps(self.native_settings(d)))
            if path!='collector':
                before=d['fixture'];self.tap(self.point(path,68,12));d=self.snap(path+'-priority')
                self.passed(path+'-native-priority-update',before!=d['fixture'])
                if path!='export_kabel':
                    self.tap(self.point(path,160,32 if path=='import_filter_kabel'else 12));e=self.snap(path+'-allowlist')
                    self.passed(path+'-native-allowlist-update',d['fixture']!=e['fixture'])
            if path in ['import_filter_kabel','export_kabel']:
                self.insert(path,0,'storagenetwork:operation_upgrade');self.capture(path+'-operation')
                before=self.snap(path+'-operation-before')['fixture'];self.tap(self.point(path,37,34));after=self.snap(path+'-operation-after')['fixture']
                self.passed(path+'-native-operation-selector',before!=after)
                self.tap(self.point(path,63,34));self.call('text',text='12',ticks=5);d=self.snap(path+'-operation-text')
                self.passed(path+'-native-operation-limit-input',self.native_settings(d).get('operationLimit',0)>0)
    def extras(self):
        for path,item in [('cradle','minecraft:shulker_box'),('receiver','storagenetwork:chunkload_upgrade')]:
            self.open(path);self.call('inventory',ticks=3);self.insert(path,0,item)
            d=self.snap(path+'-insert');m=next(m for m in d['menus']if '.storagenetwork.'in m['class'])
            self.passed(path+'-native-accepted-item',item in m['slots'][0]['item']);self.capture(path+'-active')
            self.slot(path,0);self.inventory(10);d=self.snap(path+'-retrieve');self.passed(path+'-item-retrieval',item in d['inventory'][10]['item'])
        self.open('process_kabel');self.call('inventory',ticks=3);self.cmd('item replace entity @a inventory.0 with minecraft:iron_ingot 16');self.inventory(9)
        self.tap(self.point('process_kabel',16,44));self.inventory(9);d=self.snap('process-input')
        self.passed('process-native-input-ghost','minecraft:iron_ingot'in d['fixture']and 'minecraft:iron_ingot'in d['inventory'][9]['item'])
        self.cmd('item replace entity @a inventory.0 with minecraft:gold_ingot 8');self.inventory(9);self.tap(self.point('process_kabel',122,44));self.inventory(9);d=self.snap('process-output')
        self.passed('process-native-output-ghost','minecraft:gold_ingot'in d['fixture']and 'minecraft:gold_ingot'in d['inventory'][9]['item'])
        for name,x,y in [('redstone',12,12),('reset',48,12),('count',124,12),('always',146,12),('input-face',34,102),('output-face',140,102)]:
            before=self.snap('process-'+name+'-before')['fixture'];self.tap(self.point('process_kabel',x,y));after=self.snap('process-'+name+'-after')['fixture']
            if name=='redstone':
                prior=self.native_settings(json.loads((self.out/('process-'+name+'-before.json')).read_text()))
                current=self.native_settings(json.loads((self.out/('process-'+name+'-after.json')).read_text()))
                self.passed('process-native-'+name,prior.get('needsRedstone')!=current.get('needsRedstone'))
            else:self.passed('process-native-'+name,after!=before if name!='reset'else bool(after))
        self.capture('processing-active')
    def controls(self):
        path='inventory';self.open(path,True);self.call('inventory',ticks=3)
        def workspace():return next(w for w in self.state['workspaceWindows']if w['kind']=='CONTAINER')
        old=workspace();self.tap(old['controls']['PIN']);self.passed('siu-native-window-pins',workspace()['pinMode']!=old['pinMode'])
        self.tap(workspace()['controls']['LOCK']);self.passed('siu-native-window-unlocks',workspace()['locked']=='false')
        b=self.w(path)['bounds'];p=dict(x=b['x']+25,y=b['y']+10);q=dict(x=p['x']-70,y=p['y']+25)
        self.call('click',**p,button=0,ticks=1);self.call('drag',**q,dx=-70,dy=25,button=0,ticks=2);self.call('release',**q,button=0,ticks=4)
        self.passed('siu-native-window-moves',self.w(path)['bounds']!=b);self.search(path,'iron');self.capture('moved-terminal')
        self.tap(workspace()['controls']['CLOSE']);self.passed('siu-native-window-closes-independently',not self.state.get('windows')and any(w['kind']=='INVENTORY'for w in self.state['workspaceWindows']))
        self.call('close');self.call('open',ticks=15);self.passed('siu-native-window-reopens',bool(self.w(path)))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('run');p.add_argument('phase',choices=['geometry','terminal','crafting','filters','extras','controls','final']);p.add_argument('--modern',action='store_true');p.add_argument('--path');p.add_argument('--remote',action='store_true');a=p.parse_args();c=Check(a.run)
    if a.phase=='final':
        c.geometry(a.modern);c.terminal();c.crafting();c.filters();
        if a.modern:c.extras()
        c.controls()
    elif a.phase=='geometry':c.geometry(a.modern)
    elif a.phase=='terminal':c.terminal(a.path or 'inventory',a.remote)
    elif a.phase=='crafting':c.crafting(a.path or 'request',a.remote)
    elif a.phase=='filters':c.filters(a.path)
    else:getattr(c,a.phase)()
