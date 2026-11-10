"""Native quantum-ring slot input/refusal, full data and source lifetime."""
from pathlib import Path
import collections,copy,hashlib,json
import nbtlib
from ae2_settings_features import Settings,ENTRIES,item_cell,pair,isolate,translate,stack,typed

FREQUENCY=123456
DATA='{release_quantum_marker:53,typed:[B;7b,1b]}'
REFUSAL='{release_quantum_refusal_marker:21,typed:[B;4b,9b]}'
SENTINEL='{release_quantum_foreign_marker:7,typed:[B;3b,6b]}'


def has_frequency(native,wanted):
    return native['frequencyJavaType']=='java.lang.Long'and type(native['frequency'])is int and native['frequency']==wanted


def expected_item(mc,item,count,data,quantum=False):
    if mc=='1.20.1':
        tag=nbtlib.parse_nbt(data)
        if quantum:tag['freq']=nbtlib.Long(FREQUENCY)
        return '{id:"'+item+'",Count:'+str(count)+'b,tag:'+tag.snbt()+'}'
    components={'minecraft:custom_data':nbtlib.parse_nbt(data)}
    if quantum:components['ae2:entangled_singularity_id']=nbtlib.Long(FREQUENCY)
    return '{id:"'+item+'",count:'+str(count)+',components:'+nbtlib.Compound(components).snbt()+'}'


def conserved(data,inventory,cursor=None,stored=None):
    if len(data['inventory'])!=len(inventory):return False
    if any(v.get('index',i)!=inventory[i].get('index',i)or item_cell(v)!=item_cell(inventory[i])for i,v in enumerate(data['inventory'])):return False
    if item_cell(data['cursor'])!=item_cell(cursor or dict(count=0,signature='{}')):return False
    actual=collections.Counter()
    for v in data['physical'].values():
        if v['count']:actual[typed(nbtlib.parse_nbt(v['signature']))]+=v['count']
    wanted=collections.Counter()
    if stored and stored['count']:wanted[typed(nbtlib.parse_nbt(stored['signature']))]=stored['count']
    return actual==wanted


class Quantum(Settings):
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0;self.case='quantum-link';self.cases=[];self.entry_records=[]
        self.out=self.profile/'ae2-quantum-release';self.out.mkdir();self.command='server-command'if loader=='neoforge'else'command'
        self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,original_entries=self.entry_records,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='One real quantum ring/chamber per target, normal actual-ray entry, native nonzero frequency item and original restricted slot. Actual pickup/insertion/refused incompatible input/recovery, moved input, ordinary reopening, full typed inventory/physical/cursor data and empty-source invalidation. No two-endpoint quantum-network transport claim.'),indent=2)+'\n')
    def frame(self,label):
        self.act('regression-ae2-child-state',ticks=3);p=json.loads((self.profile/'release-ae2-child-state.json').read_text())
        self.act('regression-ae2-quantum-state',ticks=3);native=json.loads((self.profile/'release-ae2-quantum-state.json').read_text())
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(dict(native=native,client=p,ledger=data),indent=2)+'\n')
        c,s=pair(p,'QNBMenu');menus=[v for v in data['menus']if v['session']==c['session']and v['type']==c['menu']];assert len(menus)==1
        slots=[v for v in menus[0]['slots']if not v['player']and not v['virtual']and v['active']];assert len(slots)==1
        self.client=c;self.slot=slots[0]['index'];self.sid=c['session']
        return native,data
    def setup(self):
        self.act('close',ticks=3)
        for cmd in isolate(self.origin):self.cmd(cmd)
        self.cmd('clear @a');rows=[v for v in json.loads(ENTRIES.read_text())['cases']if v['id']=='atlas-ae2-quantum-network-bridge'and(v['minecraft'],v['loader'])==(self.mc,self.loader)]
        assert len(rows)==1;entry=rows[0];old=[int(v)for v in entry['captured_origin'].split(',')];actions=[]
        for original in entry['actions']:
            action=copy.deepcopy(original);kind=action.pop('type')
            assert kind in {'fixture','command','open'},('Unreviewed quantum fixture action',kind)
            if kind=='open':continue # Replace the old manufactured hit with an actual top-face ray below.
            if kind=='command':action['command']=translate(action['command'],old,self.origin);kind=self.command
            self.act(kind,**action);actions.append(dict(type=kind,**action))
        self.cmd('gamemode survival @a')
        specs=[(9,'ae2:quantum_entangled_singularity',1,DATA,True),(10,'minecraft:stone',4,REFUSAL,False),(12,'minecraft:diamond',7,SENTINEL,False)]
        for index,item,count,metadata,quantum in specs:
            expected=expected_item(self.mc,item,count,metadata,quantum)
            if self.mc=='1.20.1':suffix=nbtlib.parse_nbt(expected)['tag'].snbt()
            else:suffix='['+('ae2:entangled_singularity_id='+str(FREQUENCY)+'L,'if quantum else'')+'minecraft:custom_data='+metadata+']'
            self.cmd(f'item replace entity @a container.{index} with {item}{suffix} {count}')
        x,y,z=self.origin;self.cmd(f'tp @a {x+.5} {y+2} {z+.5} 0 90');self.act('wait',ticks=40);self.act('use-block-ray',ticks=18)
        native,data=self.frame('original-empty-chamber');self.inventory=copy.deepcopy(data['inventory']);self.qes=copy.deepcopy(data['inventory'][9]);self.stone=copy.deepcopy(data['inventory'][10])
        self.check('original formed quantum chamber opens through its actual top ray',self.client['screen']==entry['expected_native_screen']and self.client['menu']==entry['expected_native_menu']and native['formed']is True and has_frequency(native,0) and native['item']['count']==0)
        self.check('native frequency item and complete typed prerequisites are exact',all(stack(data['inventory'][index]['signature'],data['inventory'][index]['count'])==stack(expected_item(self.mc,item,count,metadata,quantum))for index,item,count,metadata,quantum in specs)and conserved(data,self.inventory))
        self.entry_records.append(dict(original=entry,executed_world_actions=actions,entry='Ordinary teleport fixture above chamber and actual use-block-ray; no manufactured hit, provider, menu or screen',prerequisites=specs,actual_runtime_staging='staged-artifacts.json'))
    def click_slot(self):self.act('regression-slot-click',session=self.sid,slot=self.slot,ticks=14)
    def click_player(self,index):self.act('regression-player-click',slot=index,ticks=8)
    def run(self):
        self.setup();empty=copy.deepcopy(self.inventory);empty[9]['count']=0
        self.click_player(9);native,data=self.frame('frequency-item-picked-up')
        self.check('pickup preserves full native item data and foreign inventory',has_frequency(native,0) and conserved(data,empty,cursor=self.qes))
        self.click_slot();native,data=self.frame('frequency-item-inserted')
        self.check('native slot accepts the actual nonzero-frequency item exactly once',native['formed']is True and has_frequency(native,FREQUENCY) and item_cell(native['item'])==item_cell(self.qes)and conserved(data,empty,stored=self.qes))
        refused=copy.deepcopy(empty);refused[10]['count']=0;self.click_player(10);self.click_slot();native,data=self.frame('incompatible-item-refused')
        self.check('native quantum filter refuses the complete incompatible stack',has_frequency(native,FREQUENCY) and item_cell(native['item'])==item_cell(self.qes)and conserved(data,refused,cursor=self.stone,stored=self.qes))
        self.click_player(10);self.click_slot();native,data=self.frame('native-item-recovered')
        self.check('native pickup recovers the original frequency and all metadata',has_frequency(native,0) and native['item']['count']==0 and conserved(data,empty,cursor=self.qes))
        self.click_player(9);native,data=self.frame('physical-item-restored')
        self.check('physical recovery returns all exact inventory slots and clears cursor',conserved(data,self.inventory)and has_frequency(native,0))
        before=self.client['bounds'];self.move(self.client);native,data=self.frame('quantum-window-moved')
        self.check('quantum window moves through actual unlocked drag input',self.client['bounds']['x']==before['x']+16 and self.client['bounds']['y']==before['y']+12 and conserved(data,self.inventory))
        self.click_player(9);self.click_slot();native,data=self.frame('moved-slot-input')
        self.check('moved native slot keeps its original frequency input',has_frequency(native,FREQUENCY) and item_cell(native['item'])==item_cell(self.qes)and conserved(data,empty,stored=self.qes))
        self.reopen();native,data=self.frame('quantum-reopened')
        self.check('ordinary native reopening preserves exact stored frequency and items',native['formed']is True and has_frequency(native,FREQUENCY) and item_cell(native['item'])==item_cell(self.qes)and conserved(data,empty,stored=self.qes))
        self.click_slot();self.click_player(9);native,data=self.frame('final-recovery')
        self.check('final native recovery settles once before source invalidation',has_frequency(native,0) and conserved(data,self.inventory))
        x,y,z=self.origin;self.cmd(f'setblock {x} {y} {z} minecraft:air');self.act('wait',ticks=16);self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text())
        (self.out/'source-invalidated.json').write_text(json.dumps(data,indent=2)+'\n')
        self.check('real empty-source invalidation closes native editing without item changes',not data['menus']and conserved(data,self.inventory))
        self.cases=['quantum-link'];self.save(True);return dict(cases=1,failed=0,checks=len(self.checks),ae2_quantum_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Quantum(profile,mc,loader).run()
