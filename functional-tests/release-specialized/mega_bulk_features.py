"""Real conditional compression-card UI, both mouse directions and exact resources."""
from pathlib import Path
import collections,copy,hashlib,json
import nbtlib
from mega_workbench_features import Workbench,configuration,source_components,shell,counter,encoded_components,SOURCE_FIELDS
from ae2_settings_features import isolate,item_cell,stack,MARKER,marker_item
from ae2_quantum_features import expected_item

SOURCE='{release_mega_bulk_source:149,typed:[B;1b,5b]}'
CELL='{release_mega_bulk_cell:151,typed:[B;7b,3b]}'
UPGRADE='{release_mega_bulk_upgrade:157,typed:[B;8b,2b]}'
CELL_FIELDS=['ae2:storage_cell_config_inv','ae2:storage_cell_fuzzy_mode','ae2:upgrades','megacells:compression_cutoff']
BUTTON='gripe._90.megacells.client.screen.CompressionCutoffButton'


def plain(name,count=1):return '{id:"minecraft:'+name+'",count:'+str(count)+'}'


def cell_configuration(values,key):
    if len(values)!=1 or type(values[0]['empty'])is not bool:return False
    value=values[0]
    if key is None:return value['empty']is True
    return value['empty']is False and value['amountJavaType']=='java.lang.Long'and type(value['amount'])is int and value['amount']==0 and type(value['item']['count'])is int and value['item']['count']==1 and item_cell(value['item'])[1]==item_cell(key)[1]


def cutoff(native,wanted,key):
    """Native raw Integer sentinel, exact recipe chain and no stored physical items."""
    value=native['bulk']
    if not native['cell']['count']:return value=={}and wanted is None
    if any(name in value for name in ['BULK_CELL_ITEM','BULK_CELL_UNIT_COUNT','BULK_CELL_UNIT_FACTOR']):return False
    raw=value.get('BULK_CELL_COMPRESSION_CUTOFF')
    if wanted is None:
        if raw is not None or'megacells:compression_cutoff'in native['cellMutable']:return False
    else:
        if type(wanted)is not int or raw!={'javaType':'java.lang.Integer','value':str(wanted)}:return False
        if'megacells:compression_cutoff'not in native['cellMutable']:return False
    variants=value['variants']
    if key is None:return variants==[]
    if len(variants)!=3:return False
    for actual,(name,count,units)in zip(variants,[('iron_nugget',1,'1'),('iron_ingot',9,'9'),('iron_block',9,'81')]):
        if type(actual['item']['count'])is not int or actual['item']['count']!=count or actual['unitFactor']!=units:return False
        raw_item=nbtlib.parse_nbt(actual['item']['signature']);raw_item['count']=nbtlib.Int(1)
        if stack(raw_item.snbt())!=stack(plain(name)):return False
    return True


def owned(data,native,inventory,source,cell,upgrades,key=None,wanted=None,cursor=None):
    if len(data['inventory'])!=len(inventory)or type(native['sourceSlot'])is not int or native['sourceSlot']!=0:return False
    if item_cell(data['inventory'][0])!=item_cell(native['source'])or shell(native['source'],SOURCE_FIELDS)!=shell(source,SOURCE_FIELDS):return False
    if not source_components(native,key)or not encoded_components(native['source'],SOURCE_FIELDS,native['sourceMutable'])or not encoded_components(native['cell'],CELL_FIELDS,native['cellMutable']):return False
    expected=copy.deepcopy(inventory);expected[0]=dict(expected[0],**native['source'])
    if any(v.get('index',i)!=expected[i].get('index',i)or item_cell(v)!=item_cell(expected[i])for i,v in enumerate(data['inventory']))or item_cell(data['cursor'])!=item_cell(cursor or dict(count=0,signature='{}')):return False
    actual=native['cell']
    if cell is None:
        if actual['count']or native['cellConfig']or native['cellUpgrades']or native['cellFuzzy']!='none':return False
    else:
        if shell(actual,CELL_FIELDS)!=shell(cell,CELL_FIELDS)or not cell_configuration(native['cellConfig'],key)or native['cellFuzzy']!=native['fuzzyMode']or native['fuzzyMode']!='IGNORE_ALL':return False
        if key is not None and'ae2:storage_cell_config_inv'not in native['cellMutable']:return False
        if upgrades and'ae2:upgrades'not in native['cellMutable']:return False
    if not configuration(native['config'],key)or counter(native['upgrades'])!=counter(upgrades)or counter(native['cellUpgrades'])!=counter(upgrades)or not cutoff(native,wanted,key):return False
    return counter(data['physical'].values())==counter(([actual]if actual['count']else[])+upgrades)


class Bulk(Workbench):
    def __init__(self,profile,mc,loader):
        assert(mc,loader)==('1.21.1','neoforge')
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.command='server-command';self.case='bulk-cell-compression-cutoff';self.cases=[];self.checks=[];self.n=0
        self.out=self.profile/'mega-bulk-release';self.out.mkdir();self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='Actual MEGA4.11 handheld workbench with marked empty bulk cell/compression card and ordinary iron-nugget ghost. Original conditional item-icon toolbar, left/right decrement/increment including both wraps, moved input, source protection, reopening and exact complete resource recovery. Native recipe definitions and persisted raw Integer sentinel observed without constructing a mutating BulkCellInventory. No bulk storage throughput, compressed production or every upgrade claim.'),indent=2)+'\n')
    def valid(self,data,key=None,wanted=None,cursor=None):
        a=self.c['native'];b=self.s['native']
        if a['copyMode']!=b['copyMode']or a['fuzzyMode']!=b['fuzzyMode']or a['bulk']!=b['bulk']or not configuration(a['config'],key):return False
        return owned(data,b,self.inventory,self.source,self.inserted,self.upgrades,key,wanted,cursor)
    def button(self):
        values=[v for v in self.c['widgets']if v['type']==BUTTON];assert len(values)==1;return values[0]
    def visible(self,key,wanted):
        button=self.button()
        if button['visible']is not True or button['active']is not True:return False
        variants=self.s['native']['bulk']['variants'];logical=2 if wanted is None or wanted<0 else min(wanted,2)
        return item_cell(button['itemIcon'])==item_cell(variants[logical]['item'])
    def cycle(self,mouse,wanted,label):
        button=self.button();assert button['visible']is True and button['active']is True
        self.act('regression-tap',session=self.c['session'],x=button['x']+button['width']/2,y=button['y']+button['height']/2,button=mouse,ticks=16)
        data=self.frame(label);self.check('original '+label+' persists exact native cutoff and item icon with all resources',self.valid(data,self.template,wanted)and self.visible(self.template,wanted))
    def run(self):
        self.act('close',ticks=3)
        for cmd in isolate(self.origin):self.cmd(cmd)
        self.cmd('clear @a');self.cmd('gamemode survival @a');self.act('held',item='megacells:portable_cell_workbench',ticks=3)
        for index,item,count,tag in [(0,'megacells:portable_cell_workbench',1,SOURCE),(9,'megacells:bulk_item_cell',1,CELL),(10,'minecraft:iron_nugget',5,'{}'),(11,'megacells:compression_card',1,UPGRADE),(12,'minecraft:diamond',7,MARKER)]:self.cmd(f'item replace entity @a container.{index} with {item}[minecraft:custom_data={tag}] {count}'if tag!='{}'else f'item replace entity @a container.{index} with {item} {count}')
        self.open();data=self.frame('ordinary-native-handheld-entry');self.inventory=copy.deepcopy(data['inventory']);self.source=copy.deepcopy(self.inventory[0]);self.cell=copy.deepcopy(self.inventory[9]);self.template=copy.deepcopy(self.inventory[10]);self.upgrade=copy.deepcopy(self.inventory[11]);self.inserted=None;self.upgrades=[]
        self.check('real original handheld entry preserves exact declared full typed prerequisites',self.c['screen']=='gripe._90.megacells.client.screen.PortableCellWorkbenchScreen'and all(stack(self.inventory[i]['signature'],self.inventory[i]['count'])==stack(expected_item(self.mc,item,count,tag))for i,item,count,tag in [(0,'megacells:portable_cell_workbench',1,SOURCE),(9,'megacells:bulk_item_cell',1,CELL),(11,'megacells:compression_card',1,UPGRADE)])and stack(self.inventory[10]['signature'],self.inventory[10]['count'])==stack(plain('iron_nugget',5))and stack(self.inventory[12]['signature'],self.inventory[12]['count'])==stack(marker_item(self.mc))and self.valid(data)and self.button()['visible']is False)
        self.act('regression-player-click',slot=9,ticks=6);self.slot('RestrictedInputSlot');self.inventory[9]['count']=0;self.inserted=self.cell;data=self.frame('actual-empty-bulk-cell')
        self.check('actual empty bulk cell has one native partition and no invented stored contents',self.valid(data)and self.button()['visible']is False and len(self.s['native']['cellConfig'])==1)
        self.act('regression-player-click',slot=10,ticks=6);self.slot('CellPartitionSlot');original=self.inventory;self.inventory=copy.deepcopy(original);self.inventory[10]['count']=0;data=self.frame('actual-nugget-ghost')
        self.check('native single ghost copies ordinary nugget key while all five real nuggets stay held',self.valid(data,self.template,cursor=self.template)and self.button()['visible']is False);self.inventory=original;self.act('regression-player-click',slot=10,ticks=6)
        self.act('regression-player-click',slot=11,ticks=6);self.slot('OptionalRestrictedInputSlot');self.inventory[11]['count']=0;self.upgrades=[self.upgrade];data=self.frame('real-compression-card-reveals-native-control')
        self.check('actual compression card reveals original item-backed cutoff with default native block variant',self.valid(data,self.template)and self.visible(self.template,None))
        self.cycle(0,1,'left-decrement-to-ingot');self.cycle(0,0,'left-decrement-to-nugget')
        before=self.c['bounds'];self.move(self.c);data=self.frame('actual-cutoff-moved');self.check('native cutoff and ghost follow actual window movement',self.c['bounds']['x']==before['x']+16 and self.c['bounds']['y']==before['y']+12 and self.valid(data,self.template,0)and self.visible(self.template,0))
        self.cycle(1,1,'right-increment-to-ingot');self.cycle(1,2,'right-increment-to-block');self.cycle(1,0,'right-wrap-to-nugget');self.cycle(0,-1,'left-wrap-with-native-negative-sentinel')
        self.act('regression-player-click',slot=0,ticks=6);data=self.frame('source-protection-after-native-cutoff-change');self.check('complete marked source stays protected after legitimate nested native changes',self.valid(data,self.template,-1))
        self.act('close',ticks=5);self.open();data=self.frame('ordinary-source-reopened');self.check('ordinary reopening retains original cutoff sentinel filter and physical compression card',self.valid(data,self.template,-1)and self.visible(self.template,-1))
        self.slot('OptionalRestrictedInputSlot');self.act('regression-player-click',slot=11,ticks=6);self.inventory[11]=copy.deepcopy(self.upgrade);self.upgrades=[];data=self.frame('real-card-recovered');self.check('native card removal hides conditional control and recovers exact marked upgrade',self.valid(data,self.template,-1)and self.button()['visible']is False)
        recovered=copy.deepcopy(self.s['native']['cell']);self.slot('RestrictedInputSlot');self.act('regression-player-click',slot=9,ticks=6);self.inventory[9]=recovered;self.inserted=None;data=self.frame('actual-filtered-cell-recovered');self.check('exact fully configured bulk cell recovers and native clear mode releases source ghost ownership',self.valid(data)and shell(recovered,CELL_FIELDS)==shell(self.cell,CELL_FIELDS)and self.button()['visible']is False)
        final=copy.deepcopy(data['inventory']);self.act('close',ticks=5);self.act('regression-state',ticks=3);closed=json.loads((self.profile/'regression-state.json').read_text());(self.out/'closed.json').write_text(json.dumps(closed,indent=2)+'\n')
        self.check('last close leaves complete exact final resources no physical duplicates and no shared cursor',not closed['menus']and counter(closed['physical'].values())==collections.Counter()and [item_cell(v)for v in closed['inventory']]==[item_cell(v)for v in final]and closed['cursor']['count']==0)
        self.cases.append(self.case);self.save(True);return dict(cases=1,failed=0,checks=len(self.checks),mega_bulk_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Bulk(profile,mc,loader).run()
