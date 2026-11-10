"""Original MI fluid cells/output hatches: real bucket and item inputs.

Operator commands create pre-entry storage prerequisites only. Native flags are
copied unchanged; no output is repaired or recipe result fabricated during a
measured action. This is storage qualification, not machine production.
"""
from pathlib import Path
import copy,hashlib,json
import nbtlib
from mowzie_features import stack,item_count,typed
from mi_energy_features import original,seeded,INPUTS

FLUIDS=[tier+'_fluid_'+direction+'_hatch' for tier in ['bronze','steel','advanced','highly_advanced','turbo'] for direction in ['input','output']]+['nuclear_fluid_hatch','configurable_tank']
OUTPUTS=[tier+'_item_output_hatch'for tier in ['bronze','steel','advanced','highly_advanced','turbo']]


def physical(data,exclude=()):
    return tuple((v['index'],stack(v['item'])if item_count(v['item'])[1]else stack('{}'))for v in data['inventory']if v['index']not in exclude)


def cursor_is(data,expected=None):
    values=[v['carried']for v in data['menus']if item_count(v['carried'])[1]]
    return bool(data['menus'])and (bool(values)and all(stack(v)==stack(expected)for v in values)if expected else not values)


def amount(cell):
    assert isinstance(cell['amount'],nbtlib.Long),'Native resource amount lost its long type'
    return int(cell['amount'])


def resources(cells):return tuple((i,amount(v),typed(v['key']))for i,v in enumerate(cells)if amount(v))
def policies(cells):return tuple(typed(nbtlib.Compound({k:v for k,v in cell.items()if k not in {'amount','key'}}))for cell in cells)


def cell_state(server,client,kind):
    stored=nbtlib.parse_nbt(server['fixture'])[kind];visible=client['cells'][kind]
    assert len(stored)==len(visible)>0,'Native storage cell count differs'
    assert all(v['index']==i and typed(nbtlib.parse_nbt(v['nbt']))==typed(stored[i])for i,v in enumerate(visible)),'Native client resource/flags differ from server'
    return stored


def item_variant(mc,item,metadata):
    return nbtlib.parse_nbt('{item:"'+item+'",nbt:'+metadata+'}'if mc=='1.20.1'else'{id:"'+item+'",components:{"minecraft:custom_data":'+metadata+'}}')


def physical_item(mc,item,number,metadata=None):
    extra=(',tag:'+metadata if mc=='1.20.1'else',components:{"minecraft:custom_data":'+metadata+'}')if metadata else''
    return '{id:"'+item+'",'+('Count:'+str(number)+'b'if mc=='1.20.1'else'count:'+str(number))+extra+'}'


class Storage:
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.ui=original.Ui(self.profile.name);self.checks=[];self.cases=[];self.probes={};self.case='';self.n=0
        self.out=self.profile/'mi-storage-release';self.out.mkdir();self.unit=81000 if mc=='1.20.1'else 1000
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,probed_cells=self.probes,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Twelve native fluid-cell panels and five output-item hatch panels: declared pre-entry stored resources, original insertion/extraction flags, actual native pointer transfers, exact resource amounts/full item data, foreign inventory/cursors, first/last cells, refusal and reopening. No fluid creation, formed multiblock, pump production or machine recipe claim.'),indent=2)+'\n')
    def check(self,name,value):
        self.checks.append(dict(case=self.case,name=name,passed=bool(value)));self.save();assert value,(self.case,name);print('PASS',self.case,name,flush=True)
    def state(self,label):
        self.n+=1;tag=str(self.n).zfill(3)+'-'+self.case+'-'+label
        client,data=self.ui.save(tag);original.geometry.check(client);self.ui.send('regression-mi-storage-state',ticks=5)
        observed=json.loads((self.profile/'release-mi-storage-state.json').read_text())
        (self.out/(tag+'.json')).write_text(json.dumps(dict(client=client,server=data,observed=observed),indent=2)+'\n')
        return data,observed
    def cmd(self,text):return self.ui.send('command',command=text,ticks=5)
    def place(self,name):
        self.case='machine-modern_industrialization-'+name;self.ui.send('close',ticks=3);self.cmd('clear @a');self.cmd('gamemode survival @a')
        metadata='{release_mi_status_marker:41,typed:[B;3b,6b]}'
        suffix=metadata if self.mc=='1.20.1'else'[minecraft:custom_data='+metadata+']'
        self.cmd('item replace entity @a container.12 with minecraft:diamond'+suffix+' 7');self.ui.block(name)
        data,observed=self.state('empty');self.check('exact foreign typed fixture and empty cursor',seeded(data,self.mc)and cursor_is(data));return data,observed
    def click(self,observed,kind,index):
        cell=observed['cells'][kind][index];self.ui.native(cell['x']+8,cell['y']+8)
    def reopen(self):self.ui.send('close',ticks=4);self.ui.send('open-ray',ticks=16)
    def prerequisite(self,kind,cells):
        # Entire original cell list is retained, including all native policy
        # flags. Only declared pre-entry resource keys/amounts are seeded.
        self.ui.send('close',ticks=3);self.cmd('data merge block 8 65 8 '+nbtlib.Compound({kind:cells}).snbt());self.ui.send('open-ray',ticks=16)
    def fluid_cell(self,name,index):
        initial,client=self.place(name);cells=cell_state(initial,client,'fluids');index=len(cells)-1 if index<0 else index;first=cells[index];assert resources(cells)==()
        original_policies=policies(cells);water=nbtlib.parse_nbt('{fluid:"minecraft:water"}'if self.mc=='1.20.1'else'{id:"minecraft:water"}')
        filled=((index,self.unit,typed(water)),)
        assert bool(first.get('playerExtract',False)),'Native fixture has no player extraction contract'
        inserts=bool(first.get('playerInsert',False));baseline=physical(initial,exclude=(9,));resource='minecraft:water_bucket'if inserts else'minecraft:bucket'
        self.cmd('item replace entity @a container.9 with '+resource+' 1')
        if not inserts:
            modified=copy.deepcopy(cells);modified[index]['amount']=nbtlib.Long(self.unit)
            modified[index]['key']=water
            self.prerequisite('fluids',modified)
        data,client=self.state('prerequisites');cells=cell_state(data,client,'fluids')
        self.check('native flags and declared water prerequisite',policies(cells)==original_policies and resources(cells)==(()if inserts else filled)and physical(data,exclude=(9,))==baseline)
        self.ui.inv(9);self.click(client,'fluids',index);data,client=self.state('native-first-transfer');cells=cell_state(data,client,'fluids')
        expected='minecraft:bucket'if inserts else'minecraft:water_bucket'
        self.check('actual native bucket transfer accounts for water and container',resources(cells)==(filled if inserts else())and policies(cells)==original_policies and cursor_is(data,physical_item(self.mc,expected,1))and physical(data,exclude=(9,))==baseline)
        if inserts:
            self.ui.inv(9);self.ui.inv(9);self.click(client,'fluids',index);data,client=self.state('water-recovered');cells=cell_state(data,client,'fluids')
            self.check('native inverse transfer recovers exactly one water bucket',resources(cells)==()and policies(cells)==original_policies and cursor_is(data,physical_item(self.mc,'minecraft:water_bucket',1))and physical(data,exclude=(9,))==baseline)
        else:
            self.click(client,'fluids',index);data,client=self.state('reinsertion-refused');cells=cell_state(data,client,'fluids')
            self.check('output policy refuses water reinsertion without loss',resources(cells)==()and policies(cells)==original_policies and cursor_is(data,physical_item(self.mc,'minecraft:water_bucket',1))and physical(data,exclude=(9,))==baseline)
        self.ui.inv(9);self.reopen();data,client=self.state('reopened');cells=cell_state(data,client,'fluids')
        self.check('fluid result and full physical ledger persist',resources(cells)==()and policies(cells)==original_policies and cursor_is(data)and stack(data['inventory'][9]['item'])==stack(physical_item(self.mc,'minecraft:water_bucket',1))and physical(data,exclude=(9,))==baseline)
        probe=self.probes.setdefault(self.case,dict(kind='fluids',native_count=len(cells),indices=[]));assert probe['native_count']==len(cells)
        probe['indices'].append(index);self.save();return len(cells)

    def fluid(self,name):
        count=self.fluid_cell(name,0)
        if count>1:self.fluid_cell(name,-1)
        self.check('native first and last fluid cells both completed',self.probes[self.case]['indices']==([0,count-1]if count>1 else[0]))
        self.cases.append(self.case);self.save()
    def output(self,name):
        initial,client=self.place(name);cells=cell_state(initial,client,'items');assert len(cells)>=2
        assert all(not bool(v.get('playerInsert',False))and bool(v.get('playerExtract',False))and amount(v)==0 for v in cells)
        baseline=physical(initial,exclude=(9,10));modified=copy.deepcopy(cells);original_policies=policies(cells)
        examples=[(0,'minecraft:stone',7,9),(len(cells)-1,'minecraft:iron_ingot',9,10)]
        metadata='{release_mi_output_marker:19,typed:[B;4b,8b]}'
        for index,item,number,destination in examples:
            modified[index]['amount']=nbtlib.Long(number);modified[index]['key']=item_variant(self.mc,item,metadata)
        self.prerequisite('items',modified);data,client=self.state('declared-stored-output');actual=cell_state(data,client,'items')
        self.check('stored output metadata and all native policy flags are exact',typed(actual)==typed(modified)and physical(data,exclude=(9,10))==baseline and cursor_is(data))
        recovered={};remaining=dict((v[0],v)for v in resources(modified))
        for index,item,number,destination in examples:
            expected=physical_item(self.mc,item,number,metadata)
            self.click(client,'items',index);data,client=self.state('picked-'+str(index));actual=cell_state(data,client,'items')
            del remaining[index]
            self.check('native pickup preserves exact output '+str(index),resources(actual)==tuple(remaining.values())and policies(actual)==original_policies and cursor_is(data,expected)and physical(data,exclude=(9,10))==baseline)
            self.click(client,'items',index);data,client=self.state('refused-'+str(index));actual=cell_state(data,client,'items')
            self.check('native output-only reinsertion is refused '+str(index),resources(actual)==tuple(remaining.values())and policies(actual)==original_policies and cursor_is(data,expected)and physical(data,exclude=(9,10))==baseline)
            self.ui.inv(destination);self.reopen();data,client=self.state('reopened-'+str(index));actual=cell_state(data,client,'items')
            recovered[destination]=expected
            self.check('recovered full output persists '+str(index),resources(actual)==tuple(remaining.values())and policies(actual)==original_policies and cursor_is(data)and all(stack(data['inventory'][slot]['item'])==stack(value)for slot,value in recovered.items())and physical(data,exclude=(9,10))==baseline)
        self.probes[self.case]=dict(kind='items',native_count=len(cells),indices=[v[0]for v in examples])
        self.check('all declared output recovered and first and last cells completed',resources(actual)==()and self.probes[self.case]['indices']==[0,len(cells)-1])
        self.cases.append(self.case);self.save()
    def run(self):
        for name in FLUIDS:self.fluid(name)
        for name in OUTPUTS:self.output(name)
        self.ui.send('close',ticks=4);self.check('all native MI storage sessions close',self.ui.state()['sessions']==0);self.save(True)
        return dict(cases=len(self.cases),failed=0,checks=len(self.checks),mi_storage_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Storage(profile,mc,loader).run()
