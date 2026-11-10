"""Original AE2 priority/emitter inputs and local anchor overlay behavior."""
from pathlib import Path
import collections,copy,hashlib,json,sys
import nbtlib
from mowzie_features import stack,typed

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-regression'))
from send import send
from native_entries import translate
from fixture_isolation import commands as isolate

ENTRIES=ROOT/'output/project-everything-regression/native-entry-cases.json'
CASES=['priority','spatial-anchor','energy-level-emitter']
STATS=['powerConsumption','loadedChunks','allLoadedWorlds','allLoadedChunks','allWorlds','allChunks']
MARKER='{release_ae2_settings_marker:37,typed:[B;2b,8b]}'


def marker_item(mc):
    return '{id:"minecraft:diamond",'+('Count:7b,tag:'+MARKER if mc=='1.20.1'else'count:7,components:{"minecraft:custom_data":'+MARKER+'}')+'}'


def item_cell(row):
    count=row['count'];assert type(count)is int and count>=0
    return (count,typed(nbtlib.parse_nbt(row['signature'])))if count else(0,None)


def ledger(data):
    inventory=tuple((v.get('index',i),item_cell(v))for i,v in enumerate(data['inventory']))
    owned=collections.Counter()
    for v in data['physical'].values():
        if v['count']:owned[typed(nbtlib.parse_nbt(v['signature']))]+=v['count']
    return inventory,tuple(sorted(owned.items(),key=repr)),item_cell(data['cursor'])


def pair(probe,name):
    clients=[v for v in probe['client']if v['menu'].rsplit('.',1)[-1]==name]
    assert len(clients)==1,('Expected one original child screen',name)
    client=clients[0];servers=[v for v in probe['server']if v['session']==client['session']and v['menu']==client['menu']]
    assert len(servers)==1,('Missing actual server menu',name)
    return client,servers[0]


def number(row,key,wanted,java_type):
    v=row['values'][key]
    return v['javaType']==java_type and type(v['value'])is int and v['value']==wanted


class Settings:
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.cases=[];self.case='';self.n=0;self.baseline=None;self.entry_records=[]
        self.out=self.profile/'ae2-settings-release';self.out.mkdir();self.command='server-command'if loader=='neoforge'else'command'
        self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def act(self,kind,**kw):return send(self.profile,kind,**kw)
    def cmd(self,text):return self.act(self.command,command=text,ticks=5)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,original_entries=self.entry_records,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='Native Storage Bus priority child, Spatial Anchor populated statistics/local overlay, and Energy Level Emitter numeric threshold/redstone mode. Actual pointer/key/text input, exact native server/host values where applicable, moved input and ordinary reopening. Complete typed foreign inventory/physical/cursor preservation. Anchor overlay is intentionally local and resets with its original menu. Not crafting, chunk-loading endurance, arbitrary redstone automation or full AE2 qualification.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(case=self.case,name=name,passed=bool(value)));self.save();assert value,(self.case,name);print('PASS',self.case,name,flush=True)
    def frame(self,label):
        self.act('regression-ae2-child-state',ticks=4);p=json.loads((self.profile/'release-ae2-child-state.json').read_text())
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+self.case+'-'+label+'.json')).write_text(json.dumps(dict(native=p,ledger=data),indent=2)+'\n')
        if self.baseline is not None:assert ledger(data)==self.baseline,'Original input changed owned items/cursor'
        return p,data
    def begin(self,parent):
        self.baseline=None;self.act('close',ticks=3)
        for text in isolate(self.origin):self.cmd(text)
        self.cmd('clear @a')
        entries=[v for v in json.loads(ENTRIES.read_text())['cases']if v['id']==parent and(v['minecraft'],v['loader'])==(self.mc,self.loader)]
        assert len(entries)==1,(parent,self.mc,self.loader);entry=entries[0];actions=[];old=[int(v)for v in entry['captured_origin'].split(',')]
        powered=False
        for original in entry['actions']:
            action=copy.deepcopy(original);kind=action.pop('type')
            assert kind not in {'provider','profile','native-template-probe'},'Forced entry prohibited'
            if kind=='command':action['command']=translate(action['command'],old,self.origin);kind=self.command
            kind={'use-item':'client-use-item','use-entity':'client-use-entity','client-use-block-ray':'open-ray'}.get(kind,kind)
            if self.case=='spatial-anchor'and not powered and kind in {'use-block-ray','open-ray','client-use-block'}:
                x,y,z=self.origin;power=f'setblock {x+1} {y} {z} ae2:creative_energy_cell'
                self.cmd(power);actions.append(dict(type=self.command,command=power,ticks=5,prerequisite='Native pre-entry power, not a chunk-statistic assignment'));powered=True
            self.act(kind,**action);actions.append(dict(type=kind,**action))
        if self.case=='spatial-anchor':assert powered;self.act('wait',ticks=40)
        self.cmd('gamemode survival @a')
        suffix=MARKER if self.mc=='1.20.1'else'[minecraft:custom_data='+MARKER+']'
        self.cmd('item replace entity @a container.12 with minecraft:diamond'+suffix+' 7')
        p,data=self.frame('native-parent');marker=next(v for v in data['inventory']if v['index']==12)
        self.check('exact native parent and typed pre-entry inventory',any(v['menu']==entry['expected_native_menu']for v in p['client'])and stack(marker['signature'],marker['count'])==stack(marker_item(self.mc))and data['cursor']['count']==0)
        self.baseline=ledger(data);self.entry_records.append(dict(case=self.case,original_capture=entry,executed_actions=actions,
            actual_runtime_staging='staged-artifacts.json',scope='Original entry recipe reused on this target; actual native class and current staged publisher are checked separately, no patch-byte equivalence inferred.'))
        return p
    def tap(self,client,widget):
        assert widget['visible']is True and widget['active']is True
        self.act('regression-tap',session=client['session'],x=widget['x']+widget['width']/2,y=widget['y']+widget['height']/2,ticks=14)
    def priority_button(self,p):
        found=[(c,w)for c in p['client']for w in c['widgets']if w['type'].endswith('.TabButton')and(w['message']=='Priority'or'Priority'in w.get('tooltips',[]))and w['visible']and w['active']]
        assert len(found)==1,('Expected unique native Priority tab',[(c['screen'],w.get('icon'))for c,w in found])
        self.tap(*found[0])
    def edit(self,client,value):
        boxes=[v for v in client['widgets']if 'value'in v and v['visible']and v['active']]
        assert len(boxes)==1,'Expected one native numeric EditBox';self.tap(client,boxes[0])
        self.act('key',key=65,modifiers=2,ticks=2);self.act('text',text=str(value),ticks=14)
    def move(self,client):
        if client['locked']:self.act('regression-control',session=client['session'],control='LOCK',ticks=3)
        self.act('regression-move',session=client['session'],dx=16,dy=12,ticks=5)
    def reopen(self):self.act('close',ticks=3);self.act('use-block-ray',ticks=18)
    def priority(self):
        self.case='priority';p=self.begin('atlas-ae2-storage-bus');self.priority_button(p);p,_=self.frame('priority-child');c,s=pair(p,'PriorityMenu')
        self.check('native priority child observes its actual host',c['values']['getPriorityValue']==s['values']['getPriorityValue']==s['values']['nativeHostPriority'])
        self.edit(c,41);p,_=self.frame('priority41');c,s=pair(p,'PriorityMenu')
        self.check('ordinary numeric input sets exact native priority41',all(number(v,k,41,'java.lang.Integer')for v,k in [(c,'getPriorityValue'),(s,'getPriorityValue'),(s,'nativeHostPriority')]))
        before=c['bounds'];self.move(c);p,_=self.frame('priority-moved');c,s=pair(p,'PriorityMenu')
        self.check('priority moves through original drag input',c['bounds']['x']==before['x']+16 and c['bounds']['y']==before['y']+12)
        self.edit(c,37);p,_=self.frame('priority37');c,s=pair(p,'PriorityMenu')
        self.check('moved numeric input sets exact backing host priority37',all(number(v,k,37,'java.lang.Integer')for v,k in [(c,'getPriorityValue'),(s,'getPriorityValue'),(s,'nativeHostPriority')]))
        self.reopen();p,_=self.frame('storage-bus-reopened');self.priority_button(p);p,_=self.frame('priority-reopened');c,s=pair(p,'PriorityMenu')
        self.check('native priority persists after ordinary parent reopening',all(number(v,k,37,'java.lang.Integer')for v,k in [(c,'getPriorityValue'),(s,'getPriorityValue'),(s,'nativeHostPriority')]))
        self.cases.append(self.case);self.save()
    def anchor(self):
        self.case='spatial-anchor';p=self.begin('atlas-ae2-spatial-anchor');c,s=pair(p,'SpatialAnchorMenu');original=c['values']['getOverlayMode'];server_original=s['values']['getOverlayMode']
        self.check('actual populated native anchor statistics synchronize',all(c['values'][k]==s['values'][k]and type(s['values'][k]['value'])is int and s['values'][k]['value']>=0 for k in STATS)and s['values']['allChunks']['value']>0)
        toggles=[w for w in c['widgets']if w.get('currentValue')==original['value']and w['active']and w['visible']];assert len(toggles)==1
        self.tap(c,toggles[0]);p,_=self.frame('anchor-overlay');c,s=pair(p,'SpatialAnchorMenu')
        self.check('original overlay toggle changes local view without inventing a server setting',c['values']['getOverlayMode']!=original and s['values']['getOverlayMode']==server_original)
        before=c['bounds'];self.move(c);p,_=self.frame('anchor-moved');c,s=pair(p,'SpatialAnchorMenu')
        self.check('anchor moves before native overlay input',c['bounds']['x']==before['x']+16 and c['bounds']['y']==before['y']+12)
        toggles=[w for w in c['widgets']if w.get('currentValue')==c['values']['getOverlayMode']['value']and w['active']and w['visible']];assert len(toggles)==1
        self.tap(c,toggles[0]);p,_=self.frame('anchor-overlay-return');c,s=pair(p,'SpatialAnchorMenu')
        self.check('moved overlay input restores the original local value',c['values']['getOverlayMode']==original and s['values']['getOverlayMode']==server_original)
        self.reopen();p,_=self.frame('anchor-reopened');c,s=pair(p,'SpatialAnchorMenu')
        self.check('ordinary anchor reopening preserves native default and real statistics',c['values']['getOverlayMode']==original and all(c['values'][k]==s['values'][k]for k in STATS))
        self.cases.append(self.case);self.save()
    def emitter(self):
        self.case='energy-level-emitter';p=self.begin('atlas-ae2-energy-level-emitter');c,s=pair(p,'EnergyLevelEmitterMenu')
        self.check('native emitter initial threshold and mode synchronize',all(c['values'][k]==s['values'][k]for k in ['getReportingValue','getRedStoneMode']))
        self.edit(c,41);p,_=self.frame('emitter41');c,s=pair(p,'EnergyLevelEmitterMenu')
        self.check('ordinary numeric input sets exact native energy threshold41',all(number(v,'getReportingValue',41,'java.lang.Long')for v in [c,s]))
        mode=s['values']['getRedStoneMode'];toggles=[w for w in c['widgets']if w.get('currentValue')==mode['value']and w['visible']and w['active']];assert len(toggles)==1
        self.tap(c,toggles[0]);p,_=self.frame('emitter-mode');c,s=pair(p,'EnergyLevelEmitterMenu');changed=s['values']['getRedStoneMode']
        self.check('native redstone-mode button changes the original server setting',changed!=mode and c['values']['getRedStoneMode']==changed)
        before=c['bounds'];self.move(c);p,_=self.frame('emitter-moved');c,s=pair(p,'EnergyLevelEmitterMenu')
        self.check('emitter moves through original drag input',c['bounds']['x']==before['x']+16 and c['bounds']['y']==before['y']+12)
        self.edit(c,42);p,_=self.frame('emitter42');c,s=pair(p,'EnergyLevelEmitterMenu')
        self.check('moved emitter input retains native mode and exact threshold42',all(number(v,'getReportingValue',42,'java.lang.Long')and v['values']['getRedStoneMode']==changed for v in [c,s]))
        self.reopen();p,_=self.frame('emitter-reopened');c,s=pair(p,'EnergyLevelEmitterMenu')
        self.check('native emitter threshold and mode persist after ordinary reopening',all(number(v,'getReportingValue',42,'java.lang.Long')and v['values']['getRedStoneMode']==changed for v in [c,s]))
        self.cases.append(self.case);self.save()
    def run(self):
        self.priority();self.anchor();self.emitter();self.act('close',ticks=4);self.act('regression-state',ticks=3)
        data=json.loads((self.profile/'regression-state.json').read_text());self.check('all native setting sessions close without changing owned items',not data['menus']and ledger(data)==self.baseline)
        self.save(True);return dict(cases=3,failed=0,checks=len(self.checks),ae2_settings_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Settings(profile,mc,loader).run()
