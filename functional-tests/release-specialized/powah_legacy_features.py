"""Powah5 face/redstone controls, native solar status and bound-card ownership."""
from pathlib import Path
import collections,copy,hashlib,json,uuid
import nbtlib
from ae2_settings_features import Settings,isolate
from ae2_quantum_features import expected_item
from mowzie_features import typed

TRANSFERS=['ALL','EXTRACT','RECEIVE','NONE'];REDSTONE=['IGNORE','ON','OFF']
DIRECTIONS=['down','up','north','south','west','east']
DIAMOND='{release_powah_marker:181,typed:[B;2b,7b],nested:{check:0s}}'
CARD='{release_powah_card:191,typed:[B;8b,5b],nested:{check:3L}}'
BAD='{release_powah_unbound:193,typed:[B;9b,4b],nested:{check:7b}}'
CASES=[('energy_cable','cable','CableContainer',0),('energy_hopper','energy_hopper','EnergyHopperContainer',0),
    ('player_transmitter','player_transmitter','PlayerTransmitterContainer',1),('solar_panel','solar','SolarContainer',0)]
EXPECTED_CHECKS=74


def item(text):
    value=nbtlib.parse_nbt(text);assert isinstance(value,nbtlib.Compound)
    if not value:return 0,None
    assert isinstance(value.get('Count'),nbtlib.Byte)and isinstance(value.get('id'),nbtlib.String)
    count=int(value['Count']);assert count>0;value['Count']=nbtlib.Byte(1);return count,typed(value)


def items(values):
    result=collections.Counter()
    for text in values:
        count,key=item(text)
        if count:result[key]+=count
    return result


def bound_card(player_id,name):
    value=nbtlib.parse_nbt(expected_item('1.20.1','powah:binding_card',1,CARD));number=uuid.UUID(player_id).int
    words=[(number>>shift)&0xffffffff for shift in [96,64,32,0]]
    value['tag']['bound_player_id']=nbtlib.IntArray([v if v<0x80000000 else v-0x100000000 for v in words])
    value['tag']['bound_player_name']=nbtlib.String(name);return value.snbt()


def conserved(server,expected):
    inventory=server['inventory'];world=server['world'];menus=server['menus']
    if not isinstance(inventory,list)or not 36<=len(inventory)<=4137:return False
    if any(type(v['index'])is not int or v['index']!=i for i,v in enumerate(inventory)):return False
    if type(world['inventoryHolder'])is not bool or not isinstance(world['items'],list):return False
    if any(type(v['index'])is not int or v['index']!=i for i,v in enumerate(world['items'])):return False
    if not world['inventoryHolder']and world['items']:return False
    if type(server['ownedSessions'])is not int or server['ownedSessions']!=len(menus)or len(menus)>1:return False
    physical=items([v['item']for v in world['items']])
    for menu in menus:
        if menu['sameWorldTile']is not True:return False
        if items([menu['cursor']])!=items([server['cursor']]):return False
        if any(type(v['virtual'])is not bool for v in menu['slots']):return False
        views=[v for v in menu['slots']if not v['virtual']]
        if items([v['item']for v in views])!=physical:return False
        for view in views:
            index=view['containerIndex']
            if type(index)is not int or not 0<=index<len(world['items'])or item(view['item'])!=item(world['items'][index]['item']):return False
    return items([v['item']for v in inventory]+[server['cursor']])+physical==expected


def next_transfer(current,base):
    assert current in TRANSFERS and base in TRANSFERS
    if base=='ALL':return TRANSFERS[(TRANSFERS.index(current)+1)%4]
    if base in ['EXTRACT','RECEIVE']:return base if current=='NONE'else'NONE'
    return 'NONE'


def next_faces(before,index=None):
    faces=before['faces'];available=before['available'];base=before['transfer']
    assert len(faces)==len(available)==6 and all(type(v)is bool for v in available)and base in TRANSFERS and all(v in TRANSFERS for v in faces)
    if index is not None:
        assert type(index)is int and 0<=index<6
        result=list(faces)
        if available[index]and base!='NONE':result[index]=next_transfer(faces[index],base)
        return result
    # The original publisher's equality loop starts at face1. Preserve that
    # native contract rather than asserting an invented all-button behavior.
    same=len(set(faces[1:]))==1
    return [(next_transfer(v,base)if same else'ALL')if present and base!='NONE'else v for v,present in zip(faces,available)]


def settings_equal(client,server):
    return all(values_valid(v)for v in [client,server])and all(client[k]==server[k]for k in ['redstone','transfer','faces','available'])


def values_valid(value):
    return (value['redstone']in REDSTONE and value['transfer']in TRANSFERS and len(value['faces'])==len(value['available'])==6
        and all(v in TRANSFERS for v in value['faces'])and all(type(v)is bool for v in value['available'])
        and type(value['stored'])is int and type(value['capacity'])is int and 0<=value['stored']<=value['capacity'])


def ray_side(ray):
    x,y,z=[a-b for a,b in zip(ray['hit'],ray['position'])]
    for name,value,low,high in [('west',x,0,.4),('east',x,.6,1),('north',z,0,.4),
                              ('south',z,.6,1),('up',y,.6,1),('down',y,0,.4)]:
        if low<value<high:return name
    return None


class LegacyPowah(Settings):
    def __init__(self,profile,mc,loader):
        assert mc=='1.20.1'and loader in ['fabric','forge']
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.command='command';self.checks=[];self.cases=[];self.case='';self.n=0;self.expected=None
        self.out=self.profile/'powah-legacy-release';self.out.mkdir();self.act('regression-catalog',ticks=2)
        self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Four original Powah5 cable/hopper/transmitter/solar windows. Actual ray/button/cell input, exact original transfer/redstone cycles, moved exterior controls, server persistence, native card binding/refusal/recovery and real solar generation/obstruction. Complete typed actor/canonical cursor/native tile inventory ledger, including while source closed. No assigned power/menu/model/handlers, every energy network, transmitter charging or arbitrary automation claim.'),indent=2)+'\n')
    def frame(self,label):
        self.act('regression-powah-legacy-state',ticks=4);state=json.loads((self.profile/'release-powah-legacy-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+self.case+'-'+label+'.json')).write_text(json.dumps(state,indent=2)+'\n')
        if self.expected is not None:assert conserved(state['server'],self.expected),'Complete native item ownership changed'
        self.state=state;return state
    def source(self,state):
        assert len(state['client'])==len(state['server']['menus'])==1
        client=state['client'][0];server=state['server']['menus'][0]
        assert client['session']==server['session']and client['menu']==server['menu']=='owmii.powah.inventory.'+self.menu_name and server['valid']is True
        return client,server
    def approach(self):
        x,y,z=self.origin;dx=.85 if self.path=='cable'else .5
        self.cmd(f'tp @a {x+dx} {y} {z+3.5} 180 20.5')
    def enter(self):
        self.approach();self.act('use-block-ray',ticks=20);state=self.frame('native-entry');self.source(state);return state
    def point(self,region,x,y):self.act('regression-template-region-tap',session=self.sid,region=region,source_x=x,source_y=y,ticks=12)
    def face(self,index,phase='initial'):
        points=[(186,22),(186,10),(186,16),(192,22),(180,16),(192,16)]
        before=self.source(self.state)[1]['values'];region='native-machine'if self.path=='cable'else'native-energy-faces';x,y=(14,14)if self.path=='cable'else points[index]
        self.point(region,x,y);state=self.frame('face-'+str(index));client,server=self.source(state)
        self.check(phase+' native face'+str(index)+' control changes only the applicable published direction',server['values']['faces']==next_faces(before,index)and settings_equal(client['values'],server['values'])and server['values']['redstone']==before['redstone'])
    def begin(self,block,path,menu_name,count):
        self.case=block;self.path=path;self.menu_name=menu_name;self.expected=None
        self.act('close',ticks=4)
        for text in isolate(self.origin):self.cmd(text)
        self.cmd('time set 18000');self.cmd('weather clear');self.act('fixture',block='powah:'+block+'_starter',ticks=8)
        self.cmd('gamemode survival @a');self.cmd('item replace entity @a container.12 with minecraft:diamond'+DIAMOND+' 7')
        self.expected=items([expected_item('1.20.1','minecraft:diamond',7,DIAMOND)])
        if path=='player_transmitter':
            self.cmd('item replace entity @a hotbar.0 with powah:binding_card'+CARD+' 1');self.cmd('item replace entity @a container.10 with powah:binding_card'+BAD+' 1')
            self.expected+=items([expected_item('1.20.1','powah:binding_card',1,CARD),expected_item('1.20.1','powah:binding_card',1,BAD)])
        if path=='cable':
            x,y,z=self.origin;self.cmd(f'setblock {x+1} {y} {z} powah:energy_cell_starter[waterlogged=false]')
        state=self.frame('independent-prerequisites')
        self.check('exact typed declared resources and empty actual source prerequisites',not state['client']and state['server']['ownedSessions']==0 and not items([v['item']for v in state['server']['world']['items']])and item(state['server']['cursor'])[0]==0)
        if path=='player_transmitter':
            self.bound=bound_card(state['server']['uuid'],state['server']['name']);self.act('regression-use-held-item',shift=False,ticks=10)
            self.expected=items([expected_item('1.20.1','minecraft:diamond',7,DIAMOND),expected_item('1.20.1','powah:binding_card',1,BAD),self.bound]);state=self.frame('original-native-item-binding')
            self.check('original native item use binds exactly the player UUID and name preserving all other typed data',item(state['server']['inventory'][0]['item'])==item(self.bound))
            self.act('inventory',ticks=3);self.act('regression-player-click',slot=36,ticks=6);self.act('regression-player-click',slot=9,ticks=6);self.act('close',ticks=4);state=self.frame('native-bound-card-moved')
            self.check('ordinary player input moves the full bound card away from the opening hand',item(state['server']['inventory'][9]['item'])==item(self.bound)and item(state['server']['inventory'][0]['item'])[0]==0 and item(state['server']['cursor'])[0]==0)
        state=self.enter();client,server=self.source(state);self.sid=server['session']
        self.check('original publisher menu owns the complete expected cells and synchronized native settings',len([v for v in server['slots']if not v['virtual']])==count and settings_equal(client['values'],server['values']))
        if path=='cable':
            self.check('actual native ray connection direction is preserved by both menus',ray_side(state['ray'])=='east'and client['values']['face']==server['values']['face']=='east'and server['values']['available'][5]is True)
    def settings(self):
        if self.path=='cable':self.face(5)
        else:
            for index in range(6):self.face(index)
            before=self.source(self.state)[1]['values'];self.point('native-energy-faces',192,10);state=self.frame('all-faces');client,server=self.source(state)
            self.check('original all-face action retains its exact native transition and applicability',server['values']['faces']==next_faces(before)and settings_equal(client['values'],server['values']))
        modes=[]
        for index in range(3):
            before=self.source(self.state)[1]['values'];modes.append(before['redstone'])
            self.point('native-machine'if self.path=='cable'else'native-redstone',139 if self.path=='cable'else 182,14 if self.path=='cable'else 38)
            state=self.frame('redstone-'+str(index));client,server=self.source(state);after=server['values']
            self.check('redstone'+str(index)+' follows the original exact native enum cycle',after['redstone']==REDSTONE[(REDSTONE.index(before['redstone'])+1)%3]and after['faces']==before['faces']and settings_equal(client['values'],after))
        self.check('all three original redstone modes are reachable',set(modes)==set(REDSTONE))
        client,_=self.source(self.state);old=client['bounds'];self.move(client);state=self.frame('actually-moved');client,_=self.source(state)
        self.check('window drags before its exterior controls are exercised',client['bounds']['x']==old['x']+16 and client['bounds']['y']==old['y']+12)
        self.face(5 if self.path=='cable'else 0,'moved')
    def cards(self):
        for index,name in [(10,'unbound native card'),(12,'foreign typed item')]:
            prior=self.state['server']['inventory'][index]['item'];self.act('regression-player-click',slot=index,ticks=6);self.act('regression-slot-click',session=self.sid,slot=0,ticks=6);state=self.frame('refused-'+str(index));_,server=self.source(state)
            self.check(name+' is refused without changing its complete cursor or world slot',item(state['server']['cursor'])==item(prior)and item(server['slots'][0]['item'])[0]==0 and not items([v['item']for v in state['server']['world']['items']]))
            self.act('regression-player-click',slot=index,ticks=6);self.frame('refused-input-restored')
        self.act('regression-player-click',slot=9,ticks=6);self.act('regression-slot-click',session=self.sid,slot=0,ticks=6);state=self.frame('native-bound-card-inserted');_,server=self.source(state)
        self.check('native bound-card slot and actual world inventory own the same full typed item',item(server['slots'][0]['item'])==item(self.bound)and item(state['server']['world']['items'][0]['item'])==item(self.bound)and item(state['server']['cursor'])[0]==0)
    def solar(self):
        client,server=self.source(self.state)
        self.check('native night fixture contains no assigned solar energy',server['values']['stored']==client['values']['stored']==0 and type(server['values']['capacity'])is int and server['values']['capacity']>0)
        self.cmd('time set 6000');self.act('wait',ticks=60);state=self.frame('native-daylight-generation');client,server=self.source(state)
        self.check('actual unobstructed daylight ticks produce native positive energy',server['values']['stored']>0 and server['values']['stored']<=server['values']['capacity']and server['values']['canSeeSky']is True and client['values']['stored']>0)
        x,y,z=self.origin;self.cmd(f'setblock {x} {y+2} {z} minecraft:stone');self.act('wait',ticks=60);state=self.frame('actual-sky-obstruction');client,server=self.source(state);stopped=server['values']['stored']
        self.check('original client and server detect actual sky obstruction',server['values']['canSeeSky']is False and client['values']['canSeeSky']is False)
        self.act('wait',ticks=45);state=self.frame('native-obstructed-stable');client,server=self.source(state)
        self.check('obstructed native energy remains stable and synchronizes exactly',server['values']['stored']==client['values']['stored']==stopped)
        self.cmd(f'setblock {x} {y+2} {z} minecraft:air');self.act('wait',ticks=60);state=self.frame('original-generation-resumes');client,server=self.source(state)
        self.check('removing the actual obstruction resumes native solar production',server['values']['canSeeSky']is True and server['values']['stored']>stopped and client['values']['stored']>stopped)
    def finish(self):
        before=copy.deepcopy(self.source(self.state)[1]['values']);world=copy.deepcopy(self.state['server']['world']['items'])
        self.act('close',ticks=6);self.frame('closed-world-owns-real-items');state=self.enter();client,server=self.source(state);self.sid=server['session']
        self.check('original native settings and complete world items persist after ordinary reopening',all(server['values'][k]==before[k]for k in ['redstone','faces','transfer','available'])and settings_equal(client['values'],server['values'])and state['server']['world']['items']==world)
        if self.path=='player_transmitter':
            self.act('regression-slot-click',session=self.sid,slot=0,ticks=6);self.act('regression-player-click',slot=9,ticks=6);state=self.frame('full-bound-card-recovered')
            self.check('the full typed bound card recovers exactly once through ordinary input',item(state['server']['inventory'][9]['item'])==item(self.bound)and not items([v['item']for v in state['server']['world']['items']])and item(state['server']['cursor'])[0]==0)
        self.act('close',ticks=8);state=self.frame('all-owned-sources-closed')
        self.check('final closure releases every source with complete items and empty canonical cursor',not state['client']and state['server']['ownedSessions']==0 and not state['server']['menus']and not items([v['item']for v in state['server']['world']['items']])and item(state['server']['cursor'])[0]==0)
        self.cases.append(self.case);self.save()
    def run(self):
        for args in CASES:
            self.begin(*args);self.settings()
            if self.path=='player_transmitter':self.cards()
            if self.path=='solar':self.solar()
            self.finish()
        self.save(True);assert len(self.checks)==EXPECTED_CHECKS,(len(self.checks),EXPECTED_CHECKS)
        return dict(cases=4,failed=0,checks=len(self.checks),powah_legacy_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return LegacyPowah(profile,mc,loader).run()
