"""Original Arcs launcher and all four actual restricted capability cells."""
from pathlib import Path
import collections,copy,hashlib,json
from ae2_settings_features import Settings,isolate,item_cell,stack,MARKER,marker_item,send
from ae2_quantum_features import expected_item

ARCS=['aquatic_arc','dusk_arc','poison_arc','runic_arc']


def counter(items):
    result=collections.Counter()
    for value in items:
        if value['count']:result[item_cell(value)]+=1
    return result


def owned(data,native,inventory,equipped,cursor=None):
    if len(inventory)!=len(data['inventory'])or any(v.get('index',i)!=inventory[i].get('index',i)or item_cell(v)!=item_cell(inventory[i])for i,v in enumerate(data['inventory']))or item_cell(data['cursor'])!=item_cell(cursor or dict(count=0,signature='{}')):return False
    if len(equipped)!=len(native['capability'])or[item_cell(v)for v in native['capability']]!=[item_cell(v)for v in equipped]:return False
    if len(native['server'])!=1 or len(native['client'])!=1:return False
    c=native['client'][0];s=native['server'][0]
    if type(c['session'])is not int or type(s['session'])is not int or c['session']<=0 or c['session']!=s['session']or c['menu']!=s['menu']or len(s['slots'])!=4:return False
    for i,value in enumerate(s['slots']):
        if value['sameCapability']is not True or type(value['index'])is not int or type(value['containerIndex'])is not int or value['containerIndex']!=i or item_cell(value['item'])!=item_cell(equipped[i]):return False
    menus=[m for m in data['menus']if m['session']==s['session']and m['type']==s['menu']]
    if len(menus)!=1:return False
    for value in s['slots']:
        slots=[v for v in menus[0]['slots']if v['index']==value['index']and v['type']=='com.legacy.blue_skies.client.gui.menu.ArcSlot'and v['player']is False]
        if len(slots)!=1 or item_cell(slots[0])!=item_cell(value['item']):return False
    return counter(data['physical'].values())==counter(equipped)


class Arcs(Settings):
    def __init__(self,profile,mc,loader):
        assert(mc,loader)==('1.20.1','forge');self.profile=Path(profile);self.mc=mc;self.loader=loader;self.command='command';self.case='blue-skies-arcs';self.cases=[];self.checks=[];self.n=0
        self.out=self.profile/'blue-arcs-release';self.out.mkdir();self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def act(self,command_type,**kw):return send(self.profile,command_type,**kw)
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='Actual SIU Character Arcs attachment sends original publisher request and hosts original native Arcs screen/menu. All four actual player-capability cells, full typed physical items, wrong item and same item/different-NBT duplicate refusal, moved input, ordinary reopening/recovery and parent attachment movement. No forced menu/provider, capability mutation, dungeon unlock progression or arc gameplay effect claim.'),indent=2)+'\n')
    def frame(self,label):
        self.act('regression-blue-arcs-state',ticks=3);self.native=json.loads((self.profile/'release-blue-arcs-state.json').read_text());assert len(self.native['client'])==1;self.client=self.native['client'][0]
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());self.n+=1;(self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(dict(native=self.native,ledger=data),indent=2)+'\n');return data
    def valid(self,data,cursor=None):return owned(data,self.native,self.inventory,self.equipped,cursor)
    def open(self):self.act('regression-attachment-click',attachment='.blueskies.BlueSkiesCharacterButton',ticks=24)
    def slot(self,index):
        row=self.native['server'][0]['slots'][index];assert row['containerIndex']==index;self.act('regression-click',session=self.client['session'],slot=row['index'],button=0,ticks=16)
    def run(self):
        self.act('close',ticks=3)
        for cmd in isolate(self.origin):self.cmd(cmd)
        self.cmd('gamemode survival @a');self.cmd('clear @a')
        for i,name in enumerate(ARCS):self.cmd(f'item replace entity @a container.{9+i} with blue_skies:{name}{{ArcLevel:1,release_blue_arc:{i+131},typed:[B;2b,9b]}} 1')
        self.cmd('item replace entity @a container.14 with blue_skies:aquatic_arc{ArcLevel:1,release_blue_duplicate:137L,typed:[B;5b,4b]} 1');self.cmd('item replace entity @a container.16 with minecraft:diamond'+MARKER+' 7')
        self.act('regression-character',ticks=12);self.open();data=self.frame('original-launcher-and-screen');self.inventory=copy.deepcopy(data['inventory']);self.items=[copy.deepcopy(self.inventory[9+i])for i in range(4)];self.equipped=[dict(count=0,signature='{}')for _ in range(4)]
        self.check('actual Character attachment opens original native Arcs screen and empty capability cells',self.client['screen']=='com.legacy.blue_skies.client.gui.screen.ArcsScreen'and all(stack(self.items[i]['signature'],self.items[i]['count'])==stack(expected_item(self.mc,'blue_skies:'+name,1,'{ArcLevel:1,release_blue_arc:'+str(i+131)+',typed:[B;2b,9b]}'))for i,name in enumerate(ARCS))and stack(self.inventory[14]['signature'],self.inventory[14]['count'])==stack(expected_item(self.mc,'blue_skies:aquatic_arc',1,'{ArcLevel:1,release_blue_duplicate:137L,typed:[B;5b,4b]}'))and stack(self.inventory[16]['signature'],self.inventory[16]['count'])==stack(marker_item(self.mc))and self.valid(data))
        self.act('regression-player-click',slot=16,ticks=6);self.slot(0);data=self.frame('native-wrong-item-refused');sentinel=copy.deepcopy(self.inventory[16]);self.inventory[16]['count']=0
        self.check('original restricted arc slot refuses full typed unrelated diamonds',self.valid(data,cursor=sentinel));self.act('regression-player-click',slot=16,ticks=6);self.inventory[16]=sentinel
        for i in range(4):
            self.act('regression-player-click',slot=9+i,ticks=6);self.slot(i);self.inventory[9+i]['count']=0;self.equipped[i]=copy.deepcopy(self.items[i]);data=self.frame('native-arc-equipped-'+str(i))
            self.check('actual arc cell'+str(i)+' owns exact marked native capability item',self.valid(data))
            if i==0:
                self.act('regression-player-click',slot=14,ticks=6);self.slot(1);data=self.frame('same-item-different-data-refused');duplicate=copy.deepcopy(self.inventory[14]);self.inventory[14]['count']=0
                self.check('native duplicate rule rejects same arc item even with different complete metadata',self.valid(data,cursor=duplicate));self.act('regression-player-click',slot=14,ticks=6);self.inventory[14]=duplicate
        before=self.client['bounds'];self.move(self.client);data=self.frame('native-arcs-window-moved');self.check('original four-cell window moves while retaining exact capability ownership',self.client['bounds']['x']==before['x']+16 and self.client['bounds']['y']==before['y']+12 and self.valid(data))
        self.slot(3);self.act('regression-player-click',slot=12,ticks=6);self.inventory[12]=copy.deepcopy(self.items[3]);self.equipped[3]=dict(count=0,signature='{}');data=self.frame('moved-final-cell-recovered');self.check('moved last native slot returns original full typed arc',self.valid(data))
        self.act('regression-player-click',slot=12,ticks=6);self.slot(3);self.inventory[12]['count']=0;self.equipped[3]=copy.deepcopy(self.items[3]);data=self.frame('moved-final-cell-restored');self.check('moved last native slot accepts its real item again',self.valid(data))
        self.act('close',ticks=4);self.act('regression-character',ticks=12);self.open();data=self.frame('ordinary-arcs-reopened');self.check('ordinary Character launcher reopening retains all four exact actual capability cells',self.valid(data))
        for i in range(4):
            self.slot(i);self.act('regression-player-click',slot=9+i,ticks=6);self.inventory[9+i]=copy.deepcopy(self.items[i]);self.equipped[i]=dict(count=0,signature='{}');data=self.frame('native-arc-recovered-'+str(i));self.check('actual capability cell'+str(i)+' empties with full metadata and exact inventory recovery',self.valid(data))
        self.act('close',ticks=4);self.act('regression-character',ticks=12);self.act('regression-builtin-info',kind='CHARACTER',ticks=2);info=json.loads((self.profile/'regression-builtin.json').read_text())
        if info['locked']:self.act('regression-builtin-control',kind='CHARACTER',control='LOCK',ticks=3)
        self.act('regression-builtin-move',kind='CHARACTER',dx=16,dy=12,ticks=5);self.open();data=self.frame('moved-character-launcher');self.check('Arcs attachment remains reachable after actual Character movement with exact restored inventory',self.valid(data))
        self.act('close',ticks=4);self.act('regression-state',ticks=3);closed=json.loads((self.profile/'regression-state.json').read_text());(self.out/'closed.json').write_text(json.dumps(closed,indent=2)+'\n');self.check('all native Arcs sessions close with exact full inventory and clear cursor',not closed['menus']and counter(closed['physical'].values())==collections.Counter()and[item_cell(v)for v in closed['inventory']]==[item_cell(v)for v in self.inventory]and closed['cursor']['count']==0)
        self.cases.append(self.case);self.save(True);return dict(cases=1,failed=0,checks=len(self.checks),blue_arcs_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Arcs(profile,mc,loader).run()
