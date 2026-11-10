"""Native interface ghost selection, stock amount child and persisted config."""
from pathlib import Path
import collections,copy,hashlib,json
from ae2_settings_features import Settings,ENTRIES,isolate,translate,pair,item_cell,stack,MARKER,marker_item,number
from ae2_quantum_features import expected_item

TEMPLATE='{release_stock_template:83,typed:[B;4b,6b]}'


def inventory_cells(items):return tuple((v.get('index',i),item_cell(v))for i,v in enumerate(items))


def key_item(item):
    count,tag=item_cell(item);assert count>0
    return tag


def counter(items):
    result=collections.Counter()
    for item in items:
        if item['count']:result[item_cell(item)]+=1
    return result


def valid_config(items):
    return bool(items)and all(type(v['empty'])is bool and(v['empty']or(
        v['amountJavaType']=='java.lang.Long'and type(v['amount'])is int and v['amount']>0 and type(v['item']['count'])is int and v['item']['count']==1))for v in items)


def config_matches(native,key,amount):
    if not valid_config(native['config'])or not valid_config(native['storage'])or any(not v['empty']for v in native['storage']):return False
    expected=[]if amount is None else[0]
    if [i for i,v in enumerate(native['config'])if v['empty']is False]!=expected:return False
    if amount is None:return True
    value=native['config'][0]
    return value['amountJavaType']=='java.lang.Long'and type(value['amount'])is int and value['amount']==amount and key_item(value['item'])==key_item(key)


def owned(data,native,inventory,key,initial=None,cursor=None):
    if inventory_cells(data['inventory'])!=inventory_cells(inventory)or item_cell(data['cursor'])!=item_cell(cursor or dict(count=0,signature='{}')):return False
    mirrors=native['mirrors']
    if initial is None:
        if mirrors:return False
    else:
        if len(mirrors)!=1:return False
        mirror=mirrors[0];value=mirror['decoded']
        if mirror['mayPlace']is not False or mirror['mayPickup']is not False or not valid_config([value])or value['empty']:return False
        if value['amountJavaType']!='java.lang.Long'or type(value['amount'])is not int or value['amount']!=initial or key_item(value['item'])!=key_item(key):return False
        menus=[v for v in data['menus']if v['session']==mirror['session']and v['type']=='appeng.menu.implementations.SetStockAmountMenu']
        if len(menus)!=1:return False
        slots=[v for v in menus[0]['slots']if v['index']==mirror['slot']and v['type']=='appeng.menu.slot.InaccessibleSlot'and v['player']is False]
        if len(slots)!=1 or item_cell(slots[0])!=item_cell(mirror['raw']):return False
    # Native inaccessible dialog copies are observed/decoded above. Require the
    # generic physical report to contain exactly those declared copies, with no
    # hidden owned slots; an empty actual storage inventory is checked separately.
    return counter(data['physical'].values())==counter(m['raw']for m in mirrors)


class Stock(Settings):
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0;self.case='stock-amount';self.cases=[];self.entry_records=[]
        self.out=self.profile/'ae2-stock-release';self.out.mkdir();self.command='server-command'if loader=='neoforge'else'command'
        self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,original_entries=self.entry_records,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='Native interface ghost template and stock amount child, numeric input/Set on original server config, movement and parent/child reopening, exact typed selected key, full physical player/cursor conservation and empty real interface storage. Native inaccessible display copies are decoded/accounted separately. No positive network stocking/crafting or arbitrary interface network topology claim.'),indent=2)+'\n')
    def frame(self,label,kind='InterfaceMenu'):
        self.act('regression-ae2-child-state',ticks=3);p=json.loads((self.profile/'release-ae2-child-state.json').read_text());self.client,self.server=pair(p,kind)
        self.act('regression-ae2-stock-state',ticks=3);native=json.loads((self.profile/'release-ae2-stock-state.json').read_text())
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(dict(native=native,widgets=p,ledger=data),indent=2)+'\n')
        self.native=native;return data
    def key(self,amount):return config_matches(self.native,self.template,amount)
    def synced(self):
        clients=[v for v in self.native['client']if v['session']==self.client['session']]
        if len(clients)!=1:return False
        cells=clients[0]['cells']
        return len(cells)==len(self.native['config'])and valid_config([v['decoded']for v in cells])and len({v['index']for v in cells})==len(cells)and all(
            v['index']==i and v['decoded']==self.native['config'][i]for i,v in enumerate(sorted(cells,key=lambda x:x['index'])))
    def tap_config(self):
        rows=[v for v in self.native['client']if v['session']==self.client['session']];assert len(rows)==1
        row=rows[0];cells=[v for v in row['cells']if v['index']==0 and v['visible']is True];assert len(cells)==1;cell=cells[0]
        # The lower-right native button is reserved for amount editing. A real
        # upper-left point addresses the ghost cell itself for selection/clear.
        self.act('regression-tap',session=self.client['session'],x=row['left']+cell['x']+3,y=row['top']+cell['y']+3,ticks=16)
    def owned(self,data,inventory=None,initial=None,cursor=None):return owned(data,self.native,inventory or self.inventory,self.template,initial,cursor)
    def amount_button(self):
        found=[w for w in self.client['widgets']if w['type'].endswith('.InterfaceScreen$SetAmountButton')and w['visible']and w['active']];assert len(found)==1,'Expected one original Set Amount button for the only configured ghost cell'
        self.tap(self.client,found[0])
    def set_value(self,value):
        self.edit(self.client,value);buttons=[w for w in self.client['widgets']if w['message']=='Set'and w['visible']and w['active']];assert len(buttons)==1;self.tap(self.client,buttons[0])
    def setup(self):
        self.act('close',ticks=3)
        for cmd in isolate(self.origin):self.cmd(cmd)
        self.cmd('clear @a');rows=[v for v in json.loads(ENTRIES.read_text())['cases']if v['id']=='atlas-ae2-interface'and(v['minecraft'],v['loader'])==(self.mc,self.loader)];assert len(rows)==1
        entry=rows[0];old=[int(v)for v in entry['captured_origin'].split(',')];actions=[]
        for original in entry['actions']:
            action=copy.deepcopy(original);kind=action.pop('type');assert kind in {'fixture','command','open'},('Unreviewed interface world action',kind)
            if kind=='open':continue
            if kind=='command':action['command']=translate(action['command'],old,self.origin);kind=self.command
            self.act(kind,**action);actions.append(dict(type=kind,**action))
        self.cmd('gamemode survival @a')
        for index,item,count,tag in [(9,'minecraft:stone',3,TEMPLATE),(12,'minecraft:diamond',7,MARKER)]:
            suffix=tag if self.mc=='1.20.1'else'[minecraft:custom_data='+tag+']';self.cmd(f'item replace entity @a container.{index} with {item}{suffix} {count}')
        x,y,z=self.origin;self.cmd(f'tp @a {x+.5} {y+2} {z+.5} 0 90');self.act('use-block-ray',ticks=25)
        data=self.frame('original-empty-interface');self.inventory=copy.deepcopy(data['inventory']);self.template=copy.deepcopy(data['inventory'][9]);self.empty9=copy.deepcopy(self.inventory);self.empty9[9]['count']=0
        # The selected AEItemKey is a one-item key even though the held template
        # has three physical items. Count and full key/component data stay separate.
        self.check('actual native interface opens with exact typed prerequisites and empty storage',self.client['screen']==entry['expected_native_screen']and self.client['menu']==entry['expected_native_menu']and stack(self.template['signature'],self.template['count'])==stack(expected_item(self.mc,'minecraft:stone',3,TEMPLATE))and stack(data['inventory'][12]['signature'],data['inventory'][12]['count'])==stack(marker_item(self.mc))and self.key(None)and self.synced()and self.owned(data))
        self.entry_records.append(dict(original=entry,executed_world_actions=actions,entry='Ordinary top-face ray, no forced menu/provider',actual_runtime_staging='staged-artifacts.json'))
    def run(self):
        self.setup();self.act('regression-player-click',slot=9,ticks=8);self.tap_config();data=self.frame('native-ghost-selection')
        self.check('native ghost selection preserves held physical items and their complete key',self.key(3)and self.synced()and self.owned(data,self.empty9,cursor=self.template))
        self.act('regression-player-click',slot=9,ticks=8);data=self.frame('template-restored')
        self.check('ghost selection leaves the whole original player inventory intact',self.key(3)and self.synced()and self.owned(data))
        self.amount_button();data=self.frame('original-stock-amount-child','SetStockAmountMenu')
        self.check('original stock child reflects the actual selected key and initial amount3',self.key(3)and self.owned(data,initial=3)and all(number(v,'getInitialAmount',3,'java.lang.Integer')for v in [self.client,self.server]))
        before=self.client['bounds'];self.move(self.client);data=self.frame('stock-child-moved','SetStockAmountMenu')
        self.check('stock child moves through original unlocked drag while preserving all items',self.client['bounds']['x']==before['x']+16 and self.client['bounds']['y']==before['y']+12 and self.key(3)and self.owned(data,initial=3))
        self.set_value(37);data=self.frame('native-stock37-return')
        self.check('moved numeric input and original Set update the real stock config to37',self.key(37)and self.synced()and self.owned(data))
        self.amount_button();data=self.frame('stock-child-reopened','SetStockAmountMenu')
        self.check('ordinary child reopening receives persisted native amount37',self.key(37)and self.owned(data,initial=37)and all(number(v,'getInitialAmount',37,'java.lang.Integer')for v in [self.client,self.server]))
        self.set_value(41);data=self.frame('native-stock41-return')
        self.check('second original numeric Set retains exact key and server amount41',self.key(41)and self.synced()and self.owned(data))
        self.reopen();data=self.frame('interface-ordinary-reopened')
        self.check('ordinary parent source reopening preserves native stock41 and full key data',self.key(41)and self.synced()and self.owned(data))
        self.tap_config();data=self.frame('native-config-cleared')
        self.check('native empty-cursor ghost clear removes config without physical changes',self.key(None)and self.synced()and self.owned(data))
        self.act('close',ticks=4);self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text())
        (self.out/'closed.json').write_text(json.dumps(data,indent=2)+'\n')
        self.check('stock sessions close with original inventory and no hidden physical items',not data['menus']and inventory_cells(data['inventory'])==inventory_cells(self.inventory)and item_cell(data['cursor'])==(0,None)and not counter(data['physical'].values()))
        self.cases=[self.case];self.save(True);return dict(cases=1,failed=0,checks=len(self.checks),ae2_stock_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Stock(profile,mc,loader).run()
