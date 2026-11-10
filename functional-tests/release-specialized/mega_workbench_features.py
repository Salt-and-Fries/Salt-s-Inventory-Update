"""Actual handheld workbench entry, native partition controls and full items."""
from pathlib import Path
import collections,copy,hashlib,json
import nbtlib
from ae2_settings_features import Settings,isolate,item_cell,stack,MARKER,marker_item
from ae2_quantum_features import expected_item
from mowzie_features import typed

SOURCE='{release_mega_source:107,typed:[B;1b,9b]}'
CELL='{release_mega_cell:109,typed:[B;5b,3b]}'
TEMPLATE='{release_mega_template:113,typed:[B;2b,7b]}'
UPGRADE='{release_mega_upgrade:127,typed:[B;8b,4b]}'
SOURCE_FIELDS=['minecraft:container','ae2:exported_config_inv','ae2:exported_settings']
CELL_FIELDS=['ae2:storage_cell_config_inv','ae2:storage_cell_fuzzy_mode','ae2:upgrades']


def shell(item,allowed):
    assert type(item['count'])is int and item['count']==1
    value=nbtlib.parse_nbt(item['signature'])
    for name in allowed:value.get('components',{}).pop(name,None)
    return typed(value)


def counter(items):
    result=collections.Counter()
    for value in items:
        if value['count']:result[item_cell(value)]+=1
    return result


def encoded_components(item,allowed,observed):
    raw=nbtlib.parse_nbt(item['signature']).get('components',{})if item['count']else{}
    present={name for name in allowed if name in raw}
    return present==set(observed)and all(typed(raw[name])==typed(nbtlib.parse_nbt(observed[name]))for name in present)


def configuration(values,key=None):
    if len(values)!=63:return False
    nonempty=[]
    for i,value in enumerate(values):
        if type(value['empty'])is not bool:return False
        if value['empty']:continue
        if value['amountJavaType']!='java.lang.Long'or type(value['amount'])is not int or value['amount']!=0 or type(value['item']['count'])is not int or value['item']['count']!=1:return False
        nonempty.append(i)
    if key is None:return not nonempty
    return nonempty==[0]and item_cell(values[0]['item'])[1]==item_cell(key)[1]


def source_components(native,key=None):
    cfg=native['sourceConfig'];items=[v for v in native['sourceContained']if v['count']]
    if counter(items)!=counter([native['cell']]):return False
    if items and'minecraft:container'not in native['sourceMutable']:return False
    if cfg:
        if not configuration(cfg,key)or'ae2:exported_config_inv'not in native['sourceMutable']:return False
    elif key is not None:return False
    settings=native['sourceSettings']
    if settings:
        if settings!={'copy_mode':native['copyMode']}or'ae2:exported_settings'not in native['sourceMutable']:return False
    elif native['copyMode']!='CLEAR_ON_REMOVE'or native['cell']['count']or key is not None:return False
    return True


def owned(data,native,inventory,source,cell,upgrades,key=None,cursor=None):
    if len(data['inventory'])!=len(inventory)or type(native['sourceSlot'])is not int or native['sourceSlot']!=0:return False
    if item_cell(data['inventory'][0])!=item_cell(native['source'])or shell(native['source'],SOURCE_FIELDS)!=shell(source,SOURCE_FIELDS):return False
    if not source_components(native,key)or not encoded_components(native['source'],SOURCE_FIELDS,native['sourceMutable'])or not encoded_components(native['cell'],CELL_FIELDS,native['cellMutable']):return False
    expected=copy.deepcopy(inventory);expected[0]=dict(expected[0],**native['source'])
    if any(v.get('index',i)!=expected[i].get('index',i)or item_cell(v)!=item_cell(expected[i])for i,v in enumerate(data['inventory']))or item_cell(data['cursor'])!=item_cell(cursor or dict(count=0,signature='{}')):return False
    actual_cell=native['cell']
    if cell is None:
        if actual_cell['count']or native['cellConfig']or native['cellUpgrades']or native['cellFuzzy']!='none':return False
    else:
        if shell(actual_cell,CELL_FIELDS)!=shell(cell,CELL_FIELDS)or not configuration(native['cellConfig'],key)or native['cellFuzzy']!=native['fuzzyMode']:return False
        if key is not None and'ae2:storage_cell_config_inv'not in native['cellMutable']:return False
        if upgrades and'ae2:upgrades'not in native['cellMutable']:return False
        if native['cellFuzzy']!='IGNORE_ALL'and'ae2:storage_cell_fuzzy_mode'not in native['cellMutable']:return False
    if not configuration(native['config'],key)or counter(native['upgrades'])!=counter(upgrades)or counter(native['cellUpgrades'])!=counter(upgrades):return False
    # Exact ordinary native cell/upgrade slots only. Ghost partitions are not
    # physical resources; any unaccounted owned slot or duplicate is rejected.
    return counter(data['physical'].values())==counter(([actual_cell]if actual_cell['count']else[])+upgrades)


class Workbench(Settings):
    def __init__(self,profile,mc,loader):
        assert(mc,loader)==('1.21.1','neoforge')
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.command='server-command';self.case='portable-cell-workbench';self.cases=[];self.checks=[];self.n=0
        self.out=self.profile/'mega-workbench-release';self.out.mkdir();self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='Original handheld portable workbench use, not nonexistent block. Physical marked native cell and fuzzy upgrade, full typed ghost partition without consuming templates, actual copy/clear/partition/fuzzy toolbar, source protection, moved input, ordinary reopening and exact recovery. Only declared owner/cell components may mutate, checked against native decoded full-key contents/settings and independent physical ledgers. No bulk-cell compression, every upgrade or arbitrary automation claim.'),indent=2)+'\n')
    def frame(self,label):
        self.act('regression-mega-workbench-state',ticks=3);native=json.loads((self.profile/'release-mega-workbench-state.json').read_text());assert len(native['client'])==len(native['server'])==1
        self.c=native['client'][0];self.s=native['server'][0];assert self.c['session']==self.s['session']and self.c['menu']==self.s['menu'];self.client=self.c
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(dict(native=native,ledger=data),indent=2)+'\n');return data
    def valid(self,data,key=None,cursor=None):
        a=self.c['native'];b=self.s['native']
        if a['copyMode']!=b['copyMode']or a['fuzzyMode']!=b['fuzzyMode']or not configuration(a['config'],key):return False
        return owned(data,b,self.inventory,self.source,self.inserted,self.upgrades,key,cursor)
    def slot(self,kind,index=0):
        values=[v for v in self.c['slots']if v['type']=='appeng.menu.slot.'+kind and v['containerIndex']==index and v['visible']is True and v['active']is True];assert len(values)==1,(kind,index,values)
        value=values[0];self.act('regression-tap',session=self.c['session'],x=self.c['left']+value['x']+7,y=self.c['top']+value['y']+7,ticks=16)
    def toolbar(self,kind=None,icon=None):
        values=[v for v in self.c['widgets']if v['visible']is True and v['active']is True and(kind is None or v['type']=='appeng.client.gui.widgets.'+kind)and(icon is None or v.get('icon')==icon)];assert len(values)==1,(kind,icon,values);return values[0]
    def open(self):self.act('client-use-item',ticks=24)
    def run(self):
        self.act('close',ticks=3)
        for cmd in isolate(self.origin):self.cmd(cmd)
        self.cmd('clear @a');self.cmd('gamemode survival @a');self.act('held',item='megacells:portable_cell_workbench',ticks=3)
        for index,item,count,tag in [(0,'megacells:portable_cell_workbench',1,SOURCE),(9,'ae2:item_storage_cell_1k',1,CELL),(10,'minecraft:stone',3,TEMPLATE),(11,'ae2:fuzzy_card',1,UPGRADE),(12,'minecraft:diamond',7,MARKER)]:self.cmd(f'item replace entity @a container.{index} with {item}[minecraft:custom_data={tag}] {count}')
        self.open();data=self.frame('original-handheld-entry');self.inventory=copy.deepcopy(data['inventory']);self.source=copy.deepcopy(self.inventory[0]);self.cell=copy.deepcopy(self.inventory[9]);self.template=copy.deepcopy(self.inventory[10]);self.upgrade=copy.deepcopy(self.inventory[11]);self.inserted=None;self.upgrades=[]
        self.check('ordinary handheld item opens actual complete original native workbench with exact prerequisites',self.c['screen']=='gripe._90.megacells.client.screen.PortableCellWorkbenchScreen'and len(self.c['slots'])==72 and all(stack(self.inventory[i]['signature'],self.inventory[i]['count'])==stack(expected_item(self.mc,item,count,tag))for i,item,count,tag in [(0,'megacells:portable_cell_workbench',1,SOURCE),(9,'ae2:item_storage_cell_1k',1,CELL),(10,'minecraft:stone',3,TEMPLATE),(11,'ae2:fuzzy_card',1,UPGRADE)])and stack(self.inventory[12]['signature'],self.inventory[12]['count'])==stack(marker_item(self.mc))and self.valid(data))
        self.act('regression-player-click',slot=12,ticks=6);self.slot('RestrictedInputSlot');data=self.frame('refused-real-diamond');held=copy.deepcopy(self.inventory);held[12]['count']=0;original=self.inventory;self.inventory=held
        self.check('native workbench cell slot refuses unrelated full typed diamond stack',self.valid(data,cursor=original[12]));self.inventory=original;self.act('regression-player-click',slot=12,ticks=6)
        self.act('regression-player-click',slot=9,ticks=6);self.slot('RestrictedInputSlot');self.inventory[9]['count']=0;self.inserted=self.cell;data=self.frame('real-marked-cell-inserted')
        self.check('real cell enters actual native slot and persists inside complete marked source',self.valid(data)and self.s['native']['copyMode']=='CLEAR_ON_REMOVE')
        self.act('regression-player-click',slot=10,ticks=6);self.slot('CellPartitionSlot');data=self.frame('typed-ghost-copied');original=self.inventory;self.inventory=copy.deepcopy(original);self.inventory[10]['count']=0
        self.check('original partition slot copies exact typed key and leaves all three real template items held',self.valid(data,self.template,cursor=self.template));self.inventory=original;self.act('regression-player-click',slot=10,ticks=6);data=self.frame('template-restored')
        self.check('real template returns to exact inventory slot without partition consumption',self.valid(data,self.template))
        self.act('regression-player-click',slot=0,ticks=6);data=self.frame('source-pickup-refused');self.check('source item remains protected after its native contents legitimately change',self.valid(data,self.template))
        before=self.c['bounds'];self.move(self.c);data=self.frame('workbench-moved');self.check('complete native workbench and controls follow actual window movement',self.c['bounds']['x']==before['x']+16 and self.c['bounds']['y']==before['y']+12 and self.valid(data,self.template))
        self.tap(self.c,self.toolbar('ToggleButton'));data=self.frame('native-copy-keep');self.check('original moved copy control updates real host and owner component to keep mode',self.s['native']['copyMode']=='KEEP_ON_REMOVE'and self.valid(data,self.template))
        self.tap(self.c,self.toolbar('ToggleButton'));data=self.frame('native-copy-clear');self.check('original copy control restores native clear mode',self.s['native']['copyMode']=='CLEAR_ON_REMOVE'and self.valid(data,self.template))
        self.act('regression-player-click',slot=11,ticks=6);self.slot('OptionalRestrictedInputSlot');self.inventory[11]['count']=0;self.upgrades=[self.upgrade];data=self.frame('physical-fuzzy-upgrade')
        self.check('real marked fuzzy upgrade enters native cell and reveals its original setting control',self.valid(data,self.template)and self.toolbar('SettingToggleButton')['currentValue']==self.s['native']['fuzzyMode'])
        initial=self.s['native']['fuzzyMode'];self.tap(self.c,self.toolbar('SettingToggleButton'));data=self.frame('native-fuzzy-changed');self.check('original fuzzy toolbar changes actual cell setting without losing any full item data',self.s['native']['fuzzyMode']!=initial and self.toolbar('SettingToggleButton')['currentValue']==self.s['native']['fuzzyMode']and self.valid(data,self.template))
        for i in range(4):
            self.tap(self.c,self.toolbar('SettingToggleButton'));data=self.frame('native-fuzzy-cycle-'+str(i));assert self.valid(data,self.template)
        self.check('five real native fuzzy states cycle back to original setting',self.s['native']['fuzzyMode']==initial and self.valid(data,self.template))
        self.act('close',ticks=5);self.open();data=self.frame('original-source-reopened');self.check('ordinary item reopening retains actual cell full typed partition and upgrade',self.valid(data,self.template)and self.s['native']['copyMode']=='CLEAR_ON_REMOVE'and self.s['native']['fuzzyMode']==initial)
        self.tap(self.c,self.toolbar(icon='CLEAR'));data=self.frame('native-filter-clear');self.check('original Clear control removes ghost partitions from source and cell only',self.valid(data))
        self.act('regression-player-click',slot=10,ticks=6);self.slot('CellPartitionSlot');self.act('regression-player-click',slot=10,ticks=6);data=self.frame('partition-template-restored');self.check('reopened real ghost control restores typed template without physical consumption',self.valid(data,self.template))
        self.tap(self.c,self.toolbar(icon='COG'));data=self.frame('native-empty-cell-partition');self.check('original Partition uses actual empty storage contents and clears manual ghost filter',self.valid(data))
        self.slot('OptionalRestrictedInputSlot');self.act('regression-player-click',slot=11,ticks=6);self.inventory[11]=copy.deepcopy(self.upgrade);self.upgrades=[];data=self.frame('actual-upgrade-recovered');self.check('real upgrade recovers with complete original typed data',self.valid(data))
        recovered=copy.deepcopy(self.s['native']['cell']);self.slot('RestrictedInputSlot');self.act('regression-player-click',slot=9,ticks=6);self.inventory[9]=recovered;self.inserted=None;data=self.frame('actual-cell-recovered');self.check('real cell recovers with native cleared config and original nonconfiguration metadata',self.valid(data)and shell(recovered,CELL_FIELDS)==shell(self.cell,CELL_FIELDS))
        final=copy.deepcopy(data['inventory']);self.act('close',ticks=5);self.act('regression-state',ticks=3);closed=json.loads((self.profile/'regression-state.json').read_text());(self.out/'closed.json').write_text(json.dumps(closed,indent=2)+'\n')
        self.check('all native sessions close with exact complete final inventory and no extra owned resources',not closed['menus']and counter(closed['physical'].values())==collections.Counter()and [item_cell(v)for v in closed['inventory']]==[item_cell(v)for v in final]and closed['cursor']['count']==0)
        self.cases.append(self.case);self.save(True);return dict(cases=1,failed=0,checks=len(self.checks),mega_workbench_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Workbench(profile,mc,loader).run()
