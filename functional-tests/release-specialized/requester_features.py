"""Original Requester ghost/numeric controls, remote editing and real production."""
from pathlib import Path
import copy,hashlib,json
from ae2_crafting_features import CELL,Crafting,ordinary,key_item,fixture_pattern,cellshell,stored_matches
from ae2_settings_features import ENTRIES,isolate,translate,item_cell,stack,MARKER,marker_item
from ae2_quantum_features import expected_item

TEMPLATE='{release_requester_template:103,typed:[B;3b,8b]}'


def config(row):
    assert row.get('empty')is False and type(row['index'])is int and type(row['enabled'])is bool
    assert row['amountJavaType']==row['batchJavaType']=='java.lang.Long'and type(row['amount'])is int and row['amount']>=0 and type(row['batch'])is int and row['batch']>=1
    key=row['key'];assert type(key['empty'])is bool
    if key['empty']:identity=None
    else:
        assert type(key['item']['count'])is int and key['item']['count']==1;identity=item_cell(key['item'])[1]
    return row['index'],row['enabled'],identity,row['amount'],row['batch']


def synchronized(native,client,server):
    references=client['references'];trackers=server['trackers'];world=native['requests']
    if len(references)!=1 or len(trackers)!=1 or trackers[0]['sameBackingManager']is not True:return False
    if type(references[0]['id'])is not int or type(trackers[0]['id'])is not int or references[0]['id']!=trackers[0]['id']:return False
    wanted=[config(v)for v in world]
    if not wanted or [v[0]for v in wanted]!=list(range(len(wanted))):return False
    return all([config(v)for v in values]==wanted for values in [references[0]['requests'],trackers[0]['requests'],trackers[0]['cache']])


def matches(native,key,amount,batch,enabled):
    values=[config(v)for v in native['requests']]
    wanted=(0,enabled,None if key is None else item_cell(key)[1],amount,batch)
    return bool(values)and values[0]==wanted and all(v==(i,True,None,0,1)for i,v in enumerate(values[1:],1))


def no_buffered_items(native):
    for value in native['buffers']:
        if value.get('empty')is True:continue
        if value.get('empty')is not False:return False
        for name in ['bufferAmount','pendingAmount']:
            amount=value[name]
            if amount['javaType']!='java.lang.Long'or type(amount['value'])is not int or amount['value']!=0:return False
    return True


class Requester(Crafting):
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0;self.case='requester-block';self.cases=[];self.entry_records=[]
        self.out=self.profile/'requester-release';self.out.mkdir();self.command='server-command'if loader=='neoforge'else'command'
        self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,original_entries=self.entry_records,driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='Original Requester block and network terminal. Typed ghost key, independent actual row/amount/batch/Submit/StateBox controls, real noncraftable Missing status, moved input, native terminal discovery/search/editing of same backing manager, ordinary source reopening/persistence/clear. Genuine provider/assembler/CPU/cell network populated only by normal pointer input; enabling native request produces12planks, consumes3logs, settles empty buffers/pending, recovers exact resources. No assigned reference/tracker/status/plan/future, wireless-terminal or arbitrary automation/topology claim.'),indent=2)+'\n')
    def frame(self,label,kind=None,exclude=()):
        if kind not in ['RequesterMenu','RequesterTerminalMenu']:return super().frame(label,kind,exclude)
        self.act('regression-ae2-crafting-state',ticks=3);self.native=json.loads((self.profile/'release-ae2-crafting-state.json').read_text())
        self.act('regression-requester-state',ticks=3);self.requester=json.loads((self.profile/'release-requester-state.json').read_text())
        rows=[v for v in self.requester['client']if v['menu'].rsplit('.',1)[-1]==kind and v['session']not in exclude];assert len(rows)==1,(kind,rows)
        self.client=self.c=rows[0];servers=[v for v in self.requester['server']if v['session']==self.client['session']and v['menu']==self.client['menu']];assert len(servers)==1;self.s=servers[0]
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(dict(requester=self.requester,network=self.native,ledger=data),indent=2)+'\n');return data
    def req(self,key,amount,batch,enabled):return synchronized(self.requester,self.c,self.s)and matches(self.requester,key,amount,batch,enabled)
    def row(self):
        rows=[v for v in self.c['rows']if v['request'].get('empty')is False and v['request']['index']==0 and v['controls']['stateBox']['visible']is True];assert len(rows)==1;return rows[0]
    def ghost(self):
        reference=self.c['references'][0]['id'];values=[v for v in self.c['slots']if v['index']==0 and v['requesterId']==reference and v['visible']is True];assert len(values)==1;return values[0]
    def toggle(self):self.tap(self.client,self.row()['controls']['stateBox'])
    def edit_number(self,name,value,submit=True):
        box=self.row()['controls'][name];assert 'value'in box;self.tap(self.client,box)
        self.act('key',key=65,modifiers=2,ticks=2);self.act('text',text=str(value),ticks=3)
        if submit:self.act('key',key=257,ticks=14)
    def submit(self):self.tap(self.client,self.row()['controls']['submitButton'])
    def search(self,text):
        values=[v for v in self.client['widgets']if v['type']=='appeng.client.gui.widgets.AETextField'and v['visible']and v['active']];assert len(values)==1
        self.tap(self.client,values[0]);self.act('key',key=65,modifiers=2,ticks=2)
        if text:self.act('text',text=text,ticks=14)
        else:self.act('key',key=259,ticks=14)
    def open_block(self):self.look(.5,3,-.5,0,90)
    def open_terminal(self):self.look(.5,0,-3.5,0,20)
    def setup(self):
        self.act('close',ticks=3)
        for cmd in isolate(self.origin):self.cmd(cmd)
        self.cmd('clear @a');entries=[v for v in json.loads(ENTRIES.read_text())['cases']if v['id']=='atlas-ae2-terminal'and(v['minecraft'],v['loader'])==(self.mc,self.loader)];assert len(entries)==1
        entry=entries[0];old=[int(v)for v in entry['captured_origin'].split(',')];actions=[]
        for original in entry['actions']:
            action=copy.deepcopy(original);kind=action.pop('type');assert kind in {'fixture','command','use-block-ray'},('Unreviewed native terminal world action',kind)
            if kind=='use-block-ray':continue
            if kind=='command':action['command']=translate(action['command'],old,self.origin);kind=self.command
            self.act(kind,**action);actions.append(dict(type=kind,**action))
        x,y,z=self.origin
        for dx,dy,dz,block in [(1,0,0,'ae2:pattern_provider'),(-1,0,0,'ae2:drive'),(0,1,0,'ae2:creative_energy_cell'),(1,1,0,'ae2:molecular_assembler'),(0,-1,0,'ae2:64k_crafting_storage'),(0,1,-1,'merequester:requester')]:self.cmd(f'setblock {x+dx} {y+dy} {z+dz} {block}')
        self.cmd(f'setblock {x} {y} {z-1} ae2:cable_bus{{cable:{{id:"ae2:fluix_glass_cable"}},north:{{id:"merequester:requester_terminal"}}}}')
        self.cmd('gamemode survival @a');self.act('wait',ticks=60);self.act('regression-ae2-crafting-fixture',ticks=3)
        seed=json.loads((self.profile/'release-ae2-crafting-fixture.json').read_text());self.pattern=seed['pattern']
        for index,item,count,tag in [(10,'ae2:item_storage_cell_64k',1,CELL),(11,'minecraft:oak_log',16,None),(12,'minecraft:diamond',7,MARKER),(14,'minecraft:stone',3,TEMPLATE),(15,'minecraft:oak_planks',4,None)]:
            suffix=''if tag is None else tag if self.mc=='1.20.1'else'[minecraft:custom_data='+tag+']';self.cmd(f'item replace entity @a container.{index} with {item}{suffix} {count}')
        self.look(1.5,2,.5,0,90);data=self.frame('original-provider','PatternProviderMenu');self.inventory=copy.deepcopy(data['inventory']);self.cell=copy.deepcopy(self.inventory[10]);self.logs=copy.deepcopy(self.inventory[11]);self.template=copy.deepcopy(self.inventory[14]);self.planks=copy.deepcopy(self.inventory[15])
        self.check('genuine recipe and exact declared typed requester prerequisites precede measured input',self.pattern['item']=='ae2:crafting_pattern'and self.pattern['count']==1 and fixture_pattern(seed['decoded'],self.mc)and item_cell(self.inventory[9])==item_cell(self.pattern)and stack(self.cell['signature'],self.cell['count'])==stack(expected_item(self.mc,'ae2:item_storage_cell_64k',1,CELL))and stack(self.logs['signature'],self.logs['count'])==stack(ordinary(self.mc,'minecraft:oak_log',16))and stack(self.template['signature'],self.template['count'])==stack(expected_item(self.mc,'minecraft:stone',3,TEMPLATE))and stack(self.planks['signature'],self.planks['count'])==stack(ordinary(self.mc,'minecraft:oak_planks',4))and stack(self.inventory[12]['signature'],self.inventory[12]['count'])==stack(marker_item(self.mc))and self.stock(0,0)and self.idle())
        target=self.physical_slot();self.act('regression-player-click',slot=9,ticks=6);self.slot(target);data=self.frame('original-pattern-inserted','PatternProviderMenu');self.inventory[9]['count']=0
        self.check('native provider receives real pattern while all requester template items stay intact',self.owned(data,self.inventory,cell=False)and self.stock(0,0))
        self.act('close',ticks=4);self.look(-.5,2,.5,0,90);data=self.frame('original-drive','DriveMenu');self.drive_session=self.client['session'];target=self.physical_slot();self.act('regression-player-click',slot=10,ticks=6);self.slot(target);self.inventory[10]['count']=0;data=self.frame('real-cell-inserted','DriveMenu')
        self.check('real native cell is inserted and the independent marked shell is preserved',self.owned(data,self.inventory)and self.stock(0,0)and self.idle())
        self.look(.5,0,3.5,180,20);data=self.frame('original-repository','MEStorageMenu');self.act('regression-player-click',slot=11,ticks=6);self.repo();self.inventory[11]['count']=0;data=self.frame('real-logs-deposited','MEStorageMenu')
        self.check('normal repository input stores sixteen real logs for later native requests',self.stock(16,0)and self.idle()and self.owned(data,self.inventory))
        self.act('regression-close',session=self.client['session'],ticks=5);self.entry_records.append(dict(original_terminal=entry,world_actions=actions,additional_world='Empty native Requester at offset0,1,-1 and native terminal part north of second cable, plus genuine empty recipe network',ingredients='Only pre-entry player recipe pattern/cell/logs/full typed marker and ghost templates',entry='Ordinary actual ray, never constructed menu/provider',actual_runtime_staging='staged-artifacts.json'))
    def wait_status(self,kind,name,label,limit=20):
        for index in range(limit):
            self.act('wait',ticks=10);data=self.frame(label+'-'+str(index),kind)
            if self.requester['status'][0]['type']==name and self.row()['controls']['statusDisplay'].get('status')==self.requester['status'][0]['clientType']:return data
        raise AssertionError('Native Requester status did not settle to '+name)
    def run(self):
        self.setup();self.open_block();data=self.frame('original-requester','RequesterMenu')
        self.check('original block exposes synchronized default native request and visible ghost row',self.client['screen']=='com.almostreliable.merequester.client.RequesterScreen'and self.req(None,0,1,True)and self.ghost()['item']['count']==0 and self.owned(data,self.inventory)and no_buffered_items(self.requester))
        self.toggle();data=self.frame('empty-row-disabled','RequesterMenu');self.check('original empty-row StateBox disables request before selection',self.req(None,0,1,False)and self.row()['controls']['stateBox']['selected']is False and self.owned(data,self.inventory))
        self.act('regression-player-click',slot=14,ticks=6);self.slot(self.ghost());data=self.frame('typed-ghost-selected','RequesterMenu');empty14=copy.deepcopy(self.inventory);empty14[14]['count']=0
        self.check('native ghost selection copies full typed key without consuming held three items',self.req(self.template,3,1,False)and self.owned(data,empty14,cursor=self.template))
        self.act('regression-player-click',slot=14,ticks=6);data=self.frame('template-restored','RequesterMenu');self.check('whole physical player inventory returns intact after ghost selection',self.req(self.template,3,1,False)and self.owned(data,self.inventory))
        self.edit_number('amountField',37);data=self.frame('native-amount37','RequesterMenu');self.check('original numeric Enter commits actual server amount37',self.req(self.template,37,1,False)and self.owned(data,self.inventory))
        self.edit_number('batchField',8);data=self.frame('native-batch8','RequesterMenu');self.check('original numeric Enter commits independent server batch8',self.req(self.template,37,8,False)and self.owned(data,self.inventory))
        self.toggle();data=self.wait_status('RequesterMenu','MISSING','real-noncraftable-status')
        self.check('enabling unavailable typed ingredient reaches real native Missing without owned changes',self.req(self.template,37,8,True)and self.stock(16,0)and self.idle()and self.owned(data,self.inventory)and no_buffered_items(self.requester))
        self.toggle();self.act('wait',ticks=30);data=self.frame('native-request-disabled','RequesterMenu');before=self.client['bounds'];self.move(self.client);data=self.frame('native-block-moved','RequesterMenu')
        self.check('requester block moves with its actual row controls and unchanged disabled config',self.client['bounds']['x']==before['x']+16 and self.client['bounds']['y']==before['y']+12 and self.req(self.template,37,8,False)and self.owned(data,self.inventory))
        self.edit_number('amountField',39,False);self.edit_number('batchField',9,False);self.submit();data=self.frame('moved-native-submit','RequesterMenu')
        self.check('moved original Submit commits both actual row numbers together',self.req(self.template,39,9,False)and self.stock(16,0)and self.owned(data,self.inventory))
        self.act('regression-close',session=self.client['session'],ticks=5);self.open_block();data=self.frame('requester-native-reopened','RequesterMenu')
        self.check('ordinary block reopening persists exact full key disabled flag amount and batch',self.req(self.template,39,9,False)and self.owned(data,self.inventory))
        self.cases.append('requester-block');self.case='requester-terminal';self.act('regression-close',session=self.client['session'],ticks=5);self.open_terminal();data=self.frame('original-requester-terminal','RequesterTerminalMenu')
        self.check('original terminal discovers same native manager and all exact request fields',self.client['screen']=='com.almostreliable.merequester.client.RequesterTerminalScreen'and self.req(self.template,39,9,False)and self.owned(data,self.inventory))
        self.search('stone');data=self.frame('native-terminal-key-search','RequesterTerminalMenu');self.check('native terminal search retains actual typed stone request and backing manager',self.c['search']=='stone'and self.req(self.template,39,9,False)and self.row()['request']['key']['empty']is False and self.owned(data,self.inventory))
        self.edit_number('amountField',41);data=self.frame('terminal-amount41','RequesterTerminalMenu');self.check('terminal numeric input edits actual same server requester amount41',self.req(self.template,41,9,False)and self.owned(data,self.inventory))
        before=self.client['bounds'];self.move(self.client);data=self.frame('native-terminal-moved','RequesterTerminalMenu');self.check('terminal and actual request controls move together without item changes',self.client['bounds']['x']==before['x']+16 and self.client['bounds']['y']==before['y']+12 and self.req(self.template,41,9,False)and self.owned(data,self.inventory))
        self.edit_number('batchField',7);data=self.frame('moved-terminal-batch7','RequesterTerminalMenu');self.check('moved terminal input updates actual backing batch7',self.req(self.template,41,7,False)and self.owned(data,self.inventory))
        self.search('release_no_requester_match_347');data=self.frame('native-impossible-search','RequesterTerminalMenu')
        self.check('impossible native search hides request controls while retaining original summary header and models',self.c['search']=='release_no_requester_match_347'and self.c['matches']==0 and self.c['lines']==1 and not any(v['controls']['stateBox']['visible']for v in self.c['rows'])and self.req(self.template,41,7,False)and self.owned(data,self.inventory))
        self.search('');data=self.frame('native-search-cleared','RequesterTerminalMenu');self.check('clearing native search restores same exact request and ghost input',self.c['search']==''and self.req(self.template,41,7,False)and self.ghost()['visible']is True and self.owned(data,self.inventory))
        self.slot(self.ghost());data=self.frame('native-terminal-ghost-cleared','RequesterTerminalMenu');self.check('empty cursor clears ghost without changing physical template resources',self.req(None,0,1,False)and self.owned(data,self.inventory)and no_buffered_items(self.requester))
        self.act('regression-player-click',slot=15,ticks=6);self.slot(self.ghost());self.act('regression-player-click',slot=15,ticks=6);data=self.frame('native-output-ghost-selected','RequesterTerminalMenu')
        self.check('normal output ghost selects real recipe key while all four template planks remain physical',self.req(self.planks,4,1,False)and self.owned(data,self.inventory)and self.stock(16,0))
        self.edit_number('amountField',12,False);self.edit_number('batchField',4,False);self.submit();data=self.frame('native-positive-request-configured','RequesterTerminalMenu');self.check('original terminal Submit configures desired12 with batch4 while disabled',self.req(self.planks,12,4,False)and self.owned(data,self.inventory)and self.stock(16,0))
        self.toggle();complete=False
        for index in range(30):
            self.act('wait',ticks=10);data=self.frame('genuine-request-production-'+str(index),'RequesterTerminalMenu')
            if self.stock(13,12)and self.idle()and no_buffered_items(self.requester)and self.requester['status'][0]['type']=='IDLE':complete=True;break
        self.check('real native request produces twelve planks consumes three logs and settles all physical buffers',complete and self.req(self.planks,12,4,True)and self.owned(data,self.inventory))
        self.toggle();self.act('wait',ticks=30);data=self.frame('completed-request-disabled','RequesterTerminalMenu');self.check('original StateBox stops replenishment and preserves all created resources',self.req(self.planks,12,4,False)and self.stock(13,12)and no_buffered_items(self.requester)and self.owned(data,self.inventory))
        self.act('regression-close',session=self.client['session'],ticks=5);self.open_block();data=self.frame('block-after-terminal-edit','RequesterMenu');self.check('ordinary block reopening receives exact configuration edited through terminal',self.req(self.planks,12,4,False)and self.stock(13,12)and self.owned(data,self.inventory))
        self.slot(self.ghost());data=self.frame('native-final-config-cleared','RequesterMenu');self.check('native final clear removes only ghost configuration and pending resources stay empty',self.req(None,0,1,False)and no_buffered_items(self.requester)and self.stock(13,12)and self.owned(data,self.inventory))
        self.act('regression-close',session=self.client['session'],ticks=5);self.look(.5,0,3.5,180,20);data=self.frame('native-produced-resource-recovery','MEStorageMenu');self.repo('minecraft:oak_planks');self.act('regression-player-click',slot=13,ticks=6);self.inventory[13]=dict(self.inventory[13],item='minecraft:oak_planks',count=12,signature=key_item(self.mc,'minecraft:oak_planks')['signature']);data=self.frame('native-request-output-recovered','MEStorageMenu')
        self.check('all twelve actually produced planks recover independently of the four ghost template planks',self.stock(13,0)and self.owned(data,self.inventory))
        self.repo('minecraft:oak_log');self.act('regression-player-click',slot=11,ticks=6);self.inventory[11]=dict(self.logs,count=13);data=self.frame('native-unused-logs-recovered','MEStorageMenu');self.check('all thirteen unused logs recover with exact original item data and no extra network keys',self.stock(0,0)and self.idle()and self.owned(data,self.inventory))
        self.act('regression-close',session=self.client['session'],ticks=5);data=self.frame('native-empty-cell-recovery','DriveMenu');cell=copy.deepcopy(self.native['drive'][0]);self.slot(self.physical_slot());self.act('regression-player-click',slot=10,ticks=6);self.inventory[10]=cell;data=self.frame('native-cell-recovered','DriveMenu');self.check('actual empty cell recovers with complete marked non-storage shell unchanged',self.owned(data,self.inventory,cell=False)and cellshell(cell,self.mc)==cellshell(self.cell,self.mc)and stored_matches(self.native['stored'],self.mc,0,0))
        self.act('close',ticks=4);self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());(self.out/'closed.json').write_text(json.dumps(data,indent=2)+'\n')
        self.check('all requester and native network sessions close with exact produced and original resources',not data['menus']and self.owned(data,self.inventory,cell=False));self.cases.append('requester-terminal');self.save(True)
        return dict(cases=2,failed=0,checks=len(self.checks),requester_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Requester(profile,mc,loader).run()
