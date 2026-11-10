"""Exact native empty/charged status on all ten MI energy hatch variants."""
from pathlib import Path
import hashlib,importlib.util,json,sys
import nbtlib
from mowzie_features import stack,item_count,typed

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('mi_energy_original_ui',ROOT/'tools/exercise-modern-industrialization.py')
original=importlib.util.module_from_spec(spec);spec.loader.exec_module(original)
HATCHES=[tier+'_energy_'+direction+'_hatch' for tier in ['lv','mv','hv','ev','superconductor'] for direction in ['input','output']]
INPUTS=[ROOT/'tools/exercise-modern-industrialization.py',ROOT/'tools/check-modern-industrialization-windows.py',ROOT/'functional-tests/all-target-rollout/evidence.py']


def inventory(data):return tuple((v['index'],stack(v['item']))for v in data['inventory'])
def no_cursor(data):return bool(data['menus']) and all(item_count(v['carried'])[1]==0 for v in data['menus'])


def seeded(data,mc):
    item=data['inventory'][12]['item'];value=nbtlib.parse_nbt(item)
    metadata=value.get('tag') if mc=='1.20.1'else value.get('components',{}).get('minecraft:custom_data')
    return item_count(item)==('minecraft:diamond',7) and typed(metadata)==typed(nbtlib.parse_nbt('{release_mi_status_marker:41,typed:[B;3b,6b]}'))


def synchronized(data,observed,expected):
    tag=nbtlib.parse_nbt(data['fixture']);energy=observed['energy']
    return (('storedEu' not in tag or isinstance(tag['storedEu'],nbtlib.Long))and int(tag.get('storedEu',0))==expected and len(energy)==1
        and type(energy[0]['eu'])is int and energy[0]['eu']==expected
        and type(energy[0]['maxEu'])is int and energy[0]['maxEu']>=expected)


def run(profile,mc,loader,*args,**kwargs):
    profile=Path(profile);ui=original.Ui(profile.name);out=profile/'mi-energy-release';out.mkdir();checks=[];cases=[]
    def save(complete=False):
        (out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=cases,checks=checks,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            scope='Ten exact native energy hatch panels: empty/charged client-server synchronization, no physical item cells, geometry, typed foreign inventory/cursor conservation and ordinary reopening. Charge is a declared pre-entry world prerequisite, not generated power or multiblock production.'),indent=2)+'\n')
    def check(case,label,value):
        checks.append(dict(case=case,name=label,passed=bool(value)));save();assert value,(case,label);print('PASS',case,label,flush=True)
    def state(tag):
        client,data=ui.save(tag);ui.send('regression-mi-energy-state',ticks=5);observed=json.loads((profile/'release-mi-energy-state.json').read_text())
        (out/(tag+'.json')).write_text(json.dumps(dict(client=client,server=data,observed=observed),indent=2)+'\n')
        return client,data,observed
    ui.send('close',ticks=3);ui.send('command',command='clear @a',ticks=5);ui.send('command',command='gamemode survival @a',ticks=3)
    tag='{release_mi_status_marker:41,typed:[B;3b,6b]}'
    suffix=tag if mc=='1.20.1'else'[minecraft:custom_data='+tag+']'
    ui.send('command',command='item replace entity @a container.12 with minecraft:diamond'+suffix+' 7',ticks=4)
    baseline=None
    for hatch in HATCHES:
        case='machine-modern_industrialization-'+hatch;ui.send('close',ticks=3);ui.block(hatch)
        client,data,observed=state(hatch+'-empty');w,count=original.geometry.check(client)
        if baseline is None:
            check(case,'declared typed foreign inventory fixture exists',seeded(data,mc));baseline=inventory(data)
        check(case,'original zero-item panel and empty synchronized status',count==0 and synchronized(data,observed,0)and observed['menu'].startswith('aztech.modern_industrialization.')and inventory(data)==baseline and no_cursor(data))
        ui.send('close',ticks=3);ui.send('command',command='data merge block 8 65 8 {storedEu:1000L}',ticks=4)
        ui.send('open-ray',ticks=18);client,data,observed=state(hatch+'-charged');original.geometry.check(client)
        check(case,'charged native component matches authoritative prerequisite',synchronized(data,observed,1000)and inventory(data)==baseline and no_cursor(data))
        ui.send('close',ticks=3);ui.send('open-ray',ticks=14);client,data,observed=state(hatch+'-reopened');original.geometry.check(client)
        check(case,'native status and full physical ledger persist on reopening',synchronized(data,observed,1000)and inventory(data)==baseline and no_cursor(data))
        cases.append(case);save()
    ui.send('close',ticks=4);check('all-energy-hatches','closing leaves no owned desktop session',ui.state()['sessions']==0);save(True)
    return dict(cases=len(cases),failed=0,checks=len(checks),mi_energy_features_checked=True)
