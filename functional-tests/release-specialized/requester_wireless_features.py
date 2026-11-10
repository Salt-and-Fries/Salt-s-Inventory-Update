"""Real Survival wireless Requester widgets, power, persistence and ownership."""
from pathlib import Path
import copy,json,math,hashlib
import nbtlib
from requester_features import Requester,no_buffered_items
from requester_wireless_observer import SCREEN,MENU
from ae2_settings_features import item_cell,stack
from mowzie_features import typed
from ae2_crafting_features import cellshell,stored_matches

ENERGY=100000.0
SOURCE_TAG='{release_requester_wireless:139,typed:[B;1b,7b]}'


def source_parts(item):
    assert type(item['count'])is int and item['count']==1
    value=nbtlib.parse_nbt(item['signature']);energy=value['components'].pop('ae2:stored_energy')
    assert type(energy)is nbtlib.Double and math.isfinite(float(energy))and float(energy)>0
    return typed(value),float(energy)


def native_energy(item,base,previous,unit):
    shell,value=source_parts(item);wanted,_=source_parts(base)
    if shell!=wanted or type(previous)is not float or not math.isfinite(previous)or not(0<value<=previous<=ENERGY):return None
    if type(unit)is not float or not math.isfinite(unit)or unit<=0:return None
    # Exact reviewed ItemMenuHost debits ten native drain-ticks at a time.
    # The player/access point/config remain fixed throughout this workflow.
    batches=(ENERGY-value)/unit
    if not math.isclose(batches,round(batches),rel_tol=0,abs_tol=1e-6):return None
    return value


def power_matches(power,base,previous,unit,point,position):
    if power['creative']is not False or power['quantumConnected']is not False or power['sameSource']is not True or type(power['sourceSlot'])is not int or power['sourceSlot']!=0:return False
    if power['storedJavaType']!='java.lang.Double'or power['drainJavaType']!='java.lang.Double'or type(power['stored'])is not float or type(power['drain'])is not float:return False
    if not math.isfinite(power['drain'])or power['drain']<=0 or 10*power['drain']!=unit:return False
    if power['accessPoint']!=point or power['position']!=position or type(power['remainingEnergyTicks'])is not int or not(0<=power['remainingEnergyTicks']<=math.ceil(unit/power['drain'])):return False
    energy=native_energy(power['source'],base,previous,unit)
    return energy is not None and energy==power['stored']


class Wireless(Requester):
    def __init__(self,profile,mc,loader):
        assert(mc,loader)in[('1.21.1','neoforge'),('26.1.2','neoforge')]
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.command='server-command';self.checks=[];self.n=0;self.case='wireless-requester';self.cases=[];self.entry_records=[]
        self.source_base=None;self.energy_previous=ENERGY;self.unit=None;self.live_wireless=False;self.power_history=[]
        self.out=self.profile/'requester-wireless-release';self.out.mkdir();self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,original_entries=self.entry_records,power_history=self.power_history,driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='Dependency-complete original wireless Requester in Survival. Actual shared ghost/number/Submit/state/search controls, same world manager, Missing status, moved input, source lock and ordinary reopening/block persistence. Passive energy drain stays enabled: full nonenergy source data, native Double getter, fixed access point/player, no quantum recharge, monotonic ten-tick native debit batches. Whole typed physical inventory/cursor/buffers and final real recipe-network resource recovery. No assigned views/config/status/future, creative power exemption, every WTLib upgrade or quantum transport claim.'),indent=2)+'\n')
    def frame(self,label,kind=None,exclude=()):
        self.energy_frame_previous=self.energy_previous
        self.live_wireless=kind=='ReqWirelessTerminalMenu'
        if not self.live_wireless:return super().frame(label,kind,exclude)
        self.act('regression-ae2-crafting-state',ticks=3);self.native=json.loads((self.profile/'release-ae2-crafting-state.json').read_text())
        self.act('regression-requester-state',ticks=3);self.requester=json.loads((self.profile/'release-requester-state.json').read_text())
        rows=[v for v in self.requester['client']if v['menu']==MENU];assert len(rows)==1;self.client=self.c=rows[0];servers=[v for v in self.requester['server']if v['session']==self.c['session']and v['menu']==MENU];assert len(servers)==1;self.s=servers[0]
        self.act('regression-requester-wireless-energy',ticks=3);power=json.loads((self.profile/'release-requester-wireless-energy.json').read_text());assert len(power['server'])==1;self.power=power['server'][0];assert self.power['session']==self.c['session']
        if self.unit is None:self.unit=10*self.power['drain'];self.position=self.power['position']
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(dict(requester=self.requester,network=self.native,power=power,ledger=data),indent=2)+'\n');return data
    def owned(self,data,inventory,**kwargs):
        if self.source_base is None:return super().owned(data,inventory,**kwargs)
        source=data['inventory'][0];energy=native_energy(source,self.source_base,self.energy_frame_previous,self.unit)
        if energy is None:return False
        if self.live_wireless:
            if not power_matches(self.power,self.source_base,self.energy_frame_previous,self.unit,self.point,self.position)or energy>self.power['stored']:return False
        elif any(v['type']==MENU for v in data['menus']):return False
        expected=copy.deepcopy(inventory);expected[0]=dict(expected[0],**source)
        if not super().owned(data,expected,**kwargs):return False
        self.energy_previous=energy;self.power_history.append(dict(frame=self.n,gameTime=data['gameTime'],energy=energy,unit=self.unit,wireless=self.live_wireless));return True
    def open_wireless(self):self.act('client-use-item',ticks=24)
    def run(self):
        self.setup();self.open_block();data=self.frame('original-block-precondition','RequesterMenu');self.check('original actual requester has default empty enabled request before wireless setup',self.req(None,0,1,True)and self.owned(data,self.inventory))
        self.toggle();data=self.frame('original-disabled-request','RequesterMenu');self.check('original block StateBox disables empty request before wireless selection',self.req(None,0,1,False)and self.owned(data,self.inventory));self.act('regression-close',session=self.c['session'],ticks=5)
        x,y,z=self.origin;self.point=[x+3,y,z]
        for cmd in[f'setblock {x+2} {y} {z} ae2:cable_bus{{cable:{{id:"ae2:fluix_glass_cable"}}}}',f'setblock {x+3} {y} {z} ae2:wireless_access_point',f'setblock {x+3} {y+1} {z} ae2:creative_energy_cell',f'setblock {x+4} {y} {z+3} minecraft:stone',f'tp @a {x+4.5} {y+1} {z+3.5} 0 0']:self.cmd(cmd)
        self.act('wait',ticks=25);self.act('held',item='merequester:wireless_requester_terminal',ticks=3)
        components='"minecraft:custom_data":'+SOURCE_TAG+',"ae2:stored_energy":100000.0d,"ae2:wireless_link_target":{dimension:"minecraft:overworld",pos:[I;'+','.join(map(str,self.point))+']}'
        command='item replace entity @a container.0 with merequester:wireless_requester_terminal[minecraft:custom_data='+SOURCE_TAG+',ae2:stored_energy=100000.0d,ae2:wireless_link_target={dimension:"minecraft:overworld",pos:[I;'+','.join(map(str,self.point))+']}] 1';self.cmd(command)
        data=self.frame('declared-real-wireless-prerequisite','DriveMenu');base=dict(item='merequester:wireless_requester_terminal',count=1,signature='{id:"merequester:wireless_requester_terminal",count:1,components:{'+components+'}}');self.inventory[0]=copy.deepcopy(base)
        self.check('exact finite-energy typed linked wireless item precedes original menu entry',item_cell(data['inventory'][0])==item_cell(base)and self.owned(data,self.inventory)and self.stock(16,0));self.source_base=copy.deepcopy(base)
        self.open_wireless();data=self.frame('original-survival-wireless','ReqWirelessTerminalMenu')
        self.check('original Survival wireless screen discovers actual disabled world requester',self.c['screen']==SCREEN and self.position==[x+4.5,y+1,z+3.5]and self.req(None,0,1,False)and self.owned(data,self.inventory)and no_buffered_items(self.requester))
        self.act('regression-player-click',slot=14,ticks=6);self.slot(self.ghost());data=self.frame('wireless-typed-ghost','ReqWirelessTerminalMenu');inv=copy.deepcopy(self.inventory);inv[14]['count']=0
        self.check('wireless ghost selection preserves every held typed template item under passive power changes',self.req(self.template,3,1,False)and self.owned(data,inv,cursor=self.template));self.act('regression-player-click',slot=14,ticks=6);data=self.frame('wireless-template-restored','ReqWirelessTerminalMenu');self.check('wireless template returns intact to physical inventory',self.req(self.template,3,1,False)and self.owned(data,self.inventory))
        self.edit_number('amountField',37);data=self.frame('wireless-amount37','ReqWirelessTerminalMenu');self.check('wireless original amount Enter changes exact same server manager',self.req(self.template,37,1,False)and self.owned(data,self.inventory))
        self.edit_number('batchField',8);data=self.frame('wireless-batch8','ReqWirelessTerminalMenu');self.check('wireless original batch Enter remains independent of desired amount',self.req(self.template,37,8,False)and self.owned(data,self.inventory))
        before=self.c['bounds'];self.move(self.c);data=self.frame('wireless-window-moved','ReqWirelessTerminalMenu');self.check('wireless window and original row controls move without owned-item changes',self.c['bounds']['x']==before['x']+16 and self.c['bounds']['y']==before['y']+12 and self.req(self.template,37,8,False)and self.owned(data,self.inventory))
        self.edit_number('amountField',39,False);self.edit_number('batchField',9,False);self.submit();data=self.frame('wireless-moved-submit','ReqWirelessTerminalMenu');self.check('wireless moved native Submit commits both actual row numbers',self.req(self.template,39,9,False)and self.owned(data,self.inventory))
        self.toggle();data=self.wait_status('ReqWirelessTerminalMenu','MISSING','wireless-genuine-missing');self.check('wireless StateBox reaches genuine native Missing with exact config and physical resources',self.req(self.template,39,9,True)and no_buffered_items(self.requester)and self.stock(16,0)and self.owned(data,self.inventory))
        self.toggle();self.act('wait',ticks=30);data=self.frame('wireless-native-disabled','ReqWirelessTerminalMenu');self.check('wireless StateBox stops actual request without changing resource ownership',self.req(self.template,39,9,False)and no_buffered_items(self.requester)and self.owned(data,self.inventory))
        self.search('stone');data=self.frame('wireless-key-search','ReqWirelessTerminalMenu');self.check('wireless native key search retains actual typed request',self.c['search']=='stone'and self.req(self.template,39,9,False)and self.row()['request']['key']['empty']is False and self.owned(data,self.inventory))
        self.search('release_wireless_no_match_419');data=self.frame('wireless-impossible-search','ReqWirelessTerminalMenu');self.check('wireless impossible search retains original summary but hides row controls only',self.c['matches']==0 and self.c['lines']==1 and not any(v['controls']['stateBox']['visible']for v in self.c['rows'])and self.req(self.template,39,9,False)and self.owned(data,self.inventory))
        self.search('');data=self.frame('wireless-search-cleared','ReqWirelessTerminalMenu');self.check('wireless clear-search restores original request and ghost row',self.c['search']==''and self.req(self.template,39,9,False)and self.ghost()['visible']is True and self.owned(data,self.inventory))
        old=self.c['session'];self.act('regression-close',session=old,ticks=5);self.open_wireless();data=self.frame('wireless-ordinary-reopened','ReqWirelessTerminalMenu');self.check('ordinary native wireless reopening preserves full configuration and opens one actual session',self.c['session']!=old and self.req(self.template,39,9,False)and self.owned(data,self.inventory))
        self.act('regression-player-click',slot=0,ticks=6);data=self.frame('wireless-source-lock','ReqWirelessTerminalMenu');self.check('actual changing-energy selected wireless source remains locked against pickup',self.req(self.template,39,9,False)and self.owned(data,self.inventory))
        self.check('Survival passive energy actually declines in reviewed native batches without recharge or metadata changes',0<self.energy_previous<ENERGY and self.owned(data,self.inventory)and power_matches(self.power,self.source_base,self.energy_frame_previous,self.unit,self.point,self.position))
        self.slot(self.ghost());data=self.frame('wireless-original-clear','ReqWirelessTerminalMenu');self.check('native wireless ghost clear changes only actual request configuration',self.req(None,0,1,False)and no_buffered_items(self.requester)and self.owned(data,self.inventory))
        self.act('regression-close',session=self.c['session'],ticks=5);self.open_block();data=self.frame('block-after-real-wireless-edit','RequesterMenu');self.check('ordinary block reopening receives exact final configuration from actual wireless manager',self.req(None,0,1,False)and self.owned(data,self.inventory)and self.stock(16,0));self.act('regression-close',session=self.c['session'],ticks=5)
        self.look(.5,0,3.5,180,20);self.frame('actual-resource-recovery-terminal','MEStorageMenu');self.repo('minecraft:oak_log');self.act('regression-player-click',slot=11,ticks=6);self.inventory[11]=copy.deepcopy(self.logs);data=self.frame('actual-sixteen-logs-recovered','MEStorageMenu');self.check('all sixteen original recipe resources recover with exact full data after wireless actions',self.stock(0,0)and self.owned(data,self.inventory))
        self.act('regression-close',session=self.client['session'],ticks=5);self.frame('actual-wireless-cell-recovery','DriveMenu');cell=copy.deepcopy(self.native['drive'][0]);self.slot(self.physical_slot());self.act('regression-player-click',slot=10,ticks=6);self.inventory[10]=cell;data=self.frame('actual-empty-cell-recovered','DriveMenu');self.check('real empty storage cell recovers with complete marked shell unchanged',self.owned(data,self.inventory,cell=False)and cellshell(cell,self.mc)==cellshell(self.cell,self.mc)and stored_matches(self.native['stored'],self.mc,0,0))
        self.act('close',ticks=4);self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());(self.out/'closed.json').write_text(json.dumps(data,indent=2)+'\n');self.check('all actual wireless and world menu sessions close with exact physical resources and legitimate depleted source',not data['menus']and self.owned(data,self.inventory,cell=False));self.cases.append(self.case);self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),requester_wireless_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Wireless(profile,mc,loader).run()
