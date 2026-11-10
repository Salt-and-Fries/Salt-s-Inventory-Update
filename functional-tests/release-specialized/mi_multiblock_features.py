"""Genuinely formed MI controllers and native recipe-status transitions."""
from pathlib import Path
import copy,hashlib,json,math
import nbtlib
from mi_storage_features import Storage,physical,cursor_is,resources,typed
from mi_infrastructure_features import tank_approach
from mi_energy_features import INPUTS

MACHINES=['coke_oven','steam_blast_furnace','steam_quarry']
HATCHES=['ITEM_INPUT','ITEM_OUTPUT','FLUID_INPUT','FLUID_OUTPUT']


def select_hatches(blueprint):
    selected=[];used=set()
    for kind in HATCHES:
        choices=[v for v in blueprint if (v['x'],v['y'],v['z'])not in used and kind in {s.split(':')[-1].upper()for s in v['allows']}]
        assert choices,('Native blueprint does not allow required hatch',kind)
        v=copy.deepcopy(min(choices,key=lambda v:(v['y'],v['x'],v['z'])))
        used.add((v['x'],v['y'],v['z']));v['kind']=kind;v['block']='modern_industrialization:bronze_'+kind.lower()+'_hatch';selected.append(v)
    return selected


def active_status(probe):
    client=probe['client']
    return (probe['shapeValid']is True and client['shapeValid']is True and probe['active']is True and client['active']is True
        and all(type(v)in {int,float}and math.isfinite(v)and 0<v<1 for v in [probe['progress'],client.get('progress')]))


class Multiblocks(Storage):
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader
        from mi_energy_features import original
        self.ui=original.Ui(self.profile.name);self.checks=[];self.cases=[];self.case='';self.n=0;self.positions=[]
        self.out=self.profile/'mi-multiblock-release';self.out.mkdir();self.unit=81000 if mc=='1.20.1'else 1000
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Three actual native structures per target built from their read-only world blueprint and allowed hatch types. All controllers: invalid/formed/active/missing-structure status, native shape-tray inputs, complete geometry, typed foreign inventory/cursors and reopening. Coke/blast: real native single-input recipe completion with exact guaranteed item output and bounded original probabilistic creosote; no injected production/progress/formation. Quarry: genuine active recipe witness then native structural invalidation, no probabilistic output-completion claim. Input/steam resources are declared pre-entry prerequisites; hatch transfer policies have separate qualification.'),indent=2)+'\n')
    def probe(self,label):
        self.n+=1;self.ui.send('regression-mi-multiblock-state',positions=self.positions,ticks=3)
        p=json.loads((self.profile/'release-mi-multiblock-state.json').read_text())
        (self.out/(str(self.n).zfill(3)+'-'+self.case+'-'+label+'.json')).write_text(json.dumps(p,indent=2)+'\n');return p
    def piece_tags(self,probe):
        values={(v['x'],v['y'],v['z']):nbtlib.parse_nbt(v['nbt'])for v in probe['pieces']}
        assert len(values)==len(self.positions)==4
        return {p['kind']:values[p['x'],p['y'],p['z']]for p in self.positions}
    def one(self,name):
        self.ui.send('close',ticks=3);self.cmd('fill 0 65 0 16 74 16 minecraft:air');self.positions=[]
        initial,_=self.place(name);baseline=physical(initial);p=self.probe('native-unformed');blueprint=p['blueprint']
        assert len(blueprint)>=25 and len({(v['x'],v['y'],v['z'])for v in blueprint})==len(blueprint)
        self.check('original controller reports actual missing structure',p['shapeValid']is False and p['client']['shapeValid']is False and p['active']is False and cursor_is(initial))
        self.ui.native(-14,27);self.ui.send('wait',ticks=4);data,_=self.state('native-shape-tray-opened');p=self.probe('native-shape-tray-opened')
        tray=next(v for v in self.ui.window()['template']['regions']if v['id']=='native-shape-tray')
        self.check('native shape-preview button expands its actual tray and retains resources',tray['source']['width']>32 and p['shapeValid']is False and physical(data)==baseline and cursor_is(data))
        self.ui.native(-14,27);self.ui.send('close',ticks=3)
        self.positions=select_hatches(blueprint);replacements={(v['x'],v['y'],v['z']):v['block']for v in self.positions}
        for v in blueprint:self.cmd(f"setblock {v['x']} {v['y']} {v['z']} {replacements.get((v['x'],v['y'],v['z']),v['block'])}")
        self.cmd(tank_approach(blueprint));self.ui.send('wait',ticks=20);self.ui.send('open-ray',ticks=12)
        data,_=self.state('native-formed-idle');p=self.probe('native-formed-idle');tags=self.piece_tags(p)
        self.check('ordinary native casing and allowed hatches form an idle machine',p['shapeValid']is True and p['client']['shapeValid']is True and p['active']is False and physical(data)==baseline and cursor_is(data))
        self.ui.send('close',ticks=3)
        inputs=copy.deepcopy(tags['ITEM_INPUT']['items']);assert resources(inputs)==()
        input_id={'coke_oven':'minecraft:coal','steam_blast_furnace':'modern_industrialization:uncooked_steel_dust','steam_quarry':'modern_industrialization:bronze_drill'}[name]
        inputs[0]['amount']=nbtlib.Long(1);inputs[0]['key']=nbtlib.parse_nbt('{'+('item'if self.mc=='1.20.1'else'id')+':"'+input_id+'"}')
        steam=copy.deepcopy(tags['FLUID_INPUT']['fluids']);assert resources(steam)==()
        steam[0]['amount']=nbtlib.Long(4*self.unit);steam[0]['key']=nbtlib.parse_nbt('{'+('fluid'if self.mc=='1.20.1'else'id')+':"modern_industrialization:steam"}')
        for kind,key,cells in [('FLUID_INPUT','fluids',steam),('ITEM_INPUT','items',inputs)]:
            at=next(v for v in self.positions if v['kind']==kind);self.cmd(f"data merge block {at['x']} {at['y']} {at['z']} "+nbtlib.Compound({key:cells}).snbt())
        self.ui.send('open-ray',ticks=20);data,_=self.state('native-active-recipe');p=self.probe('native-active-recipe');tags=self.piece_tags(p)
        self.check('real native recipe consumes steam and synchronizes active progress',active_status(p)and sum(int(c['amount'])for c in tags['FLUID_INPUT']['fluids'])<4*self.unit and physical(data)==baseline and cursor_is(data))
        if name!='steam_quarry':
            self.ui.send('wait',ticks=700);data,_=self.state('native-recipe-completed');p=self.probe('native-recipe-completed');tags=self.piece_tags(p)
            expected='modern_industrialization:'+('coke'if name=='coke_oven'else'steel_ingot')
            variant=nbtlib.parse_nbt('{'+('item'if self.mc=='1.20.1'else'id')+':"'+expected+'"}')
            output=resources(tags['ITEM_OUTPUT']['items']);fluid=resources(tags['FLUID_OUTPUT']['fluids'])
            creosote=nbtlib.parse_nbt('{'+('fluid'if self.mc=='1.20.1'else'id')+':"modern_industrialization:creosote"}')
            valid_fluid=fluid==()or(name=='coke_oven'and fluid==((0,self.unit//2,typed(creosote)),))
            self.check('native single recipe yields exact guaranteed output without item creation elsewhere',output==((0,1,typed(variant)),)and resources(tags['ITEM_INPUT']['items'])==()and valid_fluid and p['active']is False and p['client']['active']is False and physical(data)==baseline and cursor_is(data))
        # Removing an ordinary required casing must invalidate the actual
        # matcher. It never edits the controller's formation/recipe state.
        victim=next(v for v in blueprint if v['block']!='minecraft:air'and (v['x'],v['y'],v['z'])not in replacements)
        self.cmd(f"setblock {victim['x']} {victim['y']} {victim['z']} minecraft:air");self.ui.send('wait',ticks=12)
        data,_=self.state('native-structure-invalidated');p=self.probe('native-structure-invalidated');invalid_tags=self.piece_tags(p)
        self.check('native structure removal reaches the original client status',p['shapeValid']is False and p['client']['shapeValid']is False and physical(data)==baseline and cursor_is(data))
        self.reopen();data,_=self.state('native-invalid-controller-reopened');p=self.probe('native-invalid-controller-reopened')
        self.check('invalid status and every native hatch resource persist on ordinary reopening',p['shapeValid']is False and p['client']['shapeValid']is False and typed(self.piece_tags(p))==typed(invalid_tags)and physical(data)==baseline and cursor_is(data))
        self.cases.append(self.case);self.save()
    def run(self):
        for name in MACHINES:self.one(name)
        self.ui.send('close',ticks=4);self.check('native multiblock windows close all owned sessions',self.ui.state()['sessions']==0);self.save(True)
        return dict(cases=3,failed=0,checks=len(self.checks),mi_multiblock_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Multiblocks(profile,mc,loader).run()
