"""Real recipe network: numeric request, native plan, output and genuine error/retry."""
from pathlib import Path
import collections,copy,hashlib,json
import nbtlib
from ae2_settings_features import Settings,ENTRIES,isolate,translate,item_cell,stack,MARKER,marker_item,ledger
from ae2_quantum_features import expected_item
from mowzie_features import typed

CELL='{release_crafting_cell:101,typed:[B;5b,9b]}'


def ordinary(mc,item,count=1):return '{id:"'+item+'",'+('Count:'+str(count)+'b'if mc=='1.20.1'else'count:'+str(count))+'}'


def key_item(mc,item):return dict(count=1,signature=ordinary(mc,item))


def cellshell(item,mc):
    value=nbtlib.parse_nbt(item['signature']);assert type(item['count'])is int and item['count']==1
    if mc=='1.20.1':
        for name in ['keys','amts','ic']:value.get('tag',{}).pop(name,None)
    else:value.get('components',{}).pop('ae2:storage_cell_inv',None)
    return typed(value)


def counted(values):
    result=collections.Counter()
    for v in values:
        amount=v['amount'];assert v['amountJavaType']=='java.lang.Long'and type(amount)is int and amount>0 and type(v['item']['count'])is int and v['item']['count']==1
        result[item_cell(v['item'])[1]]+=amount
    return result


def stored_matches(values,mc,logs,planks):
    wanted=collections.Counter()
    for item,amount in [('minecraft:oak_log',logs),('minecraft:oak_planks',planks)]:
        if amount:wanted[item_cell(key_item(mc,item))[1]]=amount
    return counted(values)==wanted


def plan_matches(plan,mc,planks):
    if plan.get('ready')is not True or plan['simulation']is not False or type(plan['bytes'])is not int or plan['bytes']<=0:return False
    output=plan['output']
    if output.get('empty')is not False or not stored_matches([output],mc,0,planks):return False
    if not stored_matches(plan['used'],mc,planks//4,0)or plan['missing']or plan['emitted']:return False
    patterns=plan['patterns']
    return len(patterns)==1 and type(patterns[0]['times'])is int and patterns[0]['times']==planks//4 and fixture_pattern(patterns[0]['pattern'],mc)


def fixture_pattern(decoded,mc):
    if decoded.get('type')!='appeng.crafting.pattern.AECraftingPattern':return False
    inputs=decoded['inputs'];outputs=decoded['outputs']
    return len(inputs)==1 and type(inputs[0]['multiplier'])is int and inputs[0]['multiplier']==1 and len(inputs[0]['alternatives'])==1 and stored_matches(inputs[0]['alternatives'],mc,1,0)and stored_matches(outputs,mc,0,4)


def physical_counter(items):
    result=collections.Counter()
    for item in items:
        if item['count']:result[item_cell(item)]+=1
    return result


class Crafting(Settings):
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0;self.case='native-crafting';self.cases=[];self.entry_records=[]
        self.out=self.profile/'ae2-crafting-release';self.out.mkdir();self.command='server-command'if loader=='neoforge'else'command'
        self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,original_entries=self.entry_records,driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='Original native recipe pattern only in pre-entry player fixture; real UI insertion into provider/drive and repository deposit. Actual amount, calculated plan, moved input/CPU selection, original confirmation Cancel returns without consumption, Start consumes logs and produces planks. A second real terminal withdraws planned ingredients, original Start yields native missing-ingredient/local error; restoring through original repository, Replan recalculates without consumption, a second actual withdrawal independently produces another error and Retry completes the real job. Full typed player/cursor/physical resource checks and real cell contents. No assigned plan/status/future, processing-machine output, exhaustive CPU topology or all crafting recipes claim.'),indent=2)+'\n')
    def read_child_widgets(self):
        self.act('regression-ae2-child-state', ticks=3)
        return json.loads((self.profile/'release-ae2-child-state.json').read_text())
    def ensure_item_repository(self,exclude=()):
        before_widgets = self.read_child_widgets()
        rows = [v for v in before_widgets['client'] if v['menu'] == 'appeng.menu.me.common.MEStorageMenu' and v['session'] not in exclude]
        assert len(rows) == 1
        root = rows[0]
        buttons = [v for v in root['widgets'] if v['type'].endswith('.KeyTypeSelectionButton') and v['visible'] and v['active']]
        assert len(buttons) == 1
        if not any('Items' in text for text in buttons[0].get('tooltips', [])):
            self.act('regression-state', ticks=3)
            before_ledger = json.loads((self.profile/'regression-state.json').read_text())
            self.tap(root, buttons[0])
            child_widgets = self.read_child_widgets()
            children = [v for v in child_widgets['client'] if v['session'] == root['session'] and v['menu'] == root['menu']]
            assert len(children) == 1 and children[0]['screen'] == 'appeng.client.gui.implementations.KeyTypeSelectionScreen'
            child = children[0]
            checks = [v for v in child['widgets'] if v['type'].endswith('.AECheckbox') and v['message'] == 'Items' and v['visible'] and v['active']]
            assert len(checks) == 1
            self.tap(child, checks[0])
            selected_widgets = self.read_child_widgets()
            children = [v for v in selected_widgets['client'] if v['session'] == root['session']]
            assert len(children) == 1
            backs = [v for v in children[0]['widgets'] if v['type'].endswith('.TabButton') and v.get('icon') == 'BACK' and v['visible'] and v['active']]
            assert len(backs) == 1
            self.tap(children[0], backs[0])
            after_widgets = self.read_child_widgets()
            roots = [v for v in after_widgets['client'] if v['session'] == root['session'] and v['menu'] == root['menu']]
            assert len(roots) == 1 and roots[0]['screen'] == root['screen']
            after_buttons = [v for v in roots[0]['widgets'] if v['type'].endswith('.KeyTypeSelectionButton') and v['visible'] and v['active']]
            assert len(after_buttons) == 1 and any('Items' in text for text in after_buttons[0].get('tooltips', []))
            self.act('regression-state', ticks=3)
            after_ledger = json.loads((self.profile/'regression-state.json').read_text())
            assert ledger(before_ledger) == ledger(after_ledger), 'Native filter selection changed owned items or cursor'
            path=self.out/('item-key-type-selection-'+str(root['session'])+'.json')
            assert not path.exists()
            path.write_text(json.dumps(dict(before_widgets=before_widgets, child_widgets=child_widgets,
                selected_widgets=selected_widgets, after_widgets=after_widgets,
                before_ledger=before_ledger, after_ledger=after_ledger, session=root['session'],
                button=buttons[0], checkbox=checks[0], back=backs[0],
                scope='Ordinary native visible-type button, Items checkbox and Back tab. Same original menu/session and exact full ownership ledger. No preference/menu assignment.'
            ), indent=2)+'\n')
    def frame(self,label,kind=None,exclude=()):
        if kind == 'MEStorageMenu' and self.mc != '1.20.1':self.ensure_item_repository(exclude)
        self.act('regression-ae2-child-state',ticks=3);p=json.loads((self.profile/'release-ae2-child-state.json').read_text())
        self.act('regression-ae2-crafting-state',ticks=3);self.native=json.loads((self.profile/'release-ae2-crafting-state.json').read_text())
        if kind:
            clients=[v for v in p['client']if v['menu'].rsplit('.',1)[-1]==kind and v['session']not in exclude];assert len(clients)==1,(kind,clients)
            self.client=clients[0];rows=[v for v in self.native['client']if v['session']==self.client['session']];servers=[v for v in self.native['server']if v['session']==self.client['session']]
            assert len(rows)==len(servers)==1 and rows[0]['screen']==self.client['screen'];self.c,self.s=rows[0],servers[0]
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(dict(native=self.native,widgets=p,ledger=data),indent=2)+'\n');return data
    def owned(self, data, inventory, cursor=None, provider=True, cell=True):
        if len(data['inventory']) != len(inventory) or any(
            v.get('index', i) != inventory[i].get('index', i) or item_cell(v) != item_cell(inventory[i])
            for i, v in enumerate(data['inventory'])
        ) or item_cell(data['cursor']) != item_cell(cursor or dict(count=0, signature='{}')):
            return False
        expected = [self.pattern] if provider else []
        if physical_counter(self.native['provider']) != physical_counter(expected):
            return False
        nonempty = [v for v in self.native['drive'] if v['count']]
        if cell:
            if len(nonempty) != 1 or cellshell(nonempty[0], self.mc) != cellshell(self.cell, self.mc):
                return False
        elif nonempty:
            return False
        mirrors = self.native['mirrors']
        for mirror in mirrors:
            if mirror['type'] != 'appeng.menu.slot.InaccessibleSlot' or mirror['mayPlace'] is not False or mirror['mayPickup'] is not False:
                return False
            menus = [v for v in data['menus'] if v['session'] == mirror['session'] and v['type'] == 'appeng.menu.me.crafting.CraftAmountMenu']
            if len(menus) != 1:
                return False
            slots = [v for v in menus[0]['slots'] if v['index'] == mirror['slot'] and v['type'] == mirror['type'] and v['player'] is False]
            if len(slots) != 1 or item_cell(slots[0]) != item_cell(mirror['raw']):
                return False
        opened_provider = any(v['type'] == 'appeng.menu.implementations.PatternProviderMenu' for v in data['menus'])
        opened_drive = any(v['type'] == 'appeng.menu.implementations.DriveMenu' for v in data['menus'])
        return physical_counter(data['physical'].values()) == physical_counter(
            (nonempty if opened_drive else []) + (expected if opened_provider else []) + [v['raw'] for v in mirrors]
        )
    def stock(self,logs,planks):return self.native['powered']is True and stored_matches(self.native['stored'],self.mc,logs,planks)
    def idle(self):return len(self.native['cpus'])==1 and self.native['cpus'][0]['busy']is False and self.native['cpus'][0]['active']is False and self.native['cpus'][0]['bytes']>=65536
    def slot(self,value,button=0):
        assert value['visible']is True;self.act('regression-tap',session=self.client['session'],x=self.c['left']+value['x']+7,y=self.c['top']+value['y']+7,button=button,ticks=16)
    def physical_slot(self,index=0):
        values=[v for v in self.c['slots']if v['containerIndex']==index and v['visible']is True and v['type']=='appeng.menu.slot.RestrictedInputSlot'];assert len(values)==1;return values[0]
    def button(self,message):
        values=[v for v in self.client['widgets']if v['message']==message and v['visible']and v['active']];assert len(values)==1;self.tap(self.client,values[0])
    def repo(self,item='',button=0):self.act('regression-ae2-repository-tap',session=self.client['session'],item=item,button=button,ticks=22)
    def look(self,dx,dy,dz,yaw,pitch):
        x,y,z=self.origin;self.cmd(f'tp @a {x+dx} {y+dy} {z+dz} {yaw} {pitch}');self.act('use-block-ray',ticks=24)
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
        for dx,dy,dz,block in [(1,0,0,'ae2:pattern_provider'),(-1,0,0,'ae2:drive'),(0,1,0,'ae2:creative_energy_cell'),(1,1,0,'ae2:molecular_assembler'),(0,-1,0,'ae2:64k_crafting_storage')]:self.cmd(f'setblock {x+dx} {y+dy} {z+dz} {block}')
        extra=''if self.mc=='1.20.1'else',enabledKeyTypes:["ae2:i","ae2:f"]'
        self.cmd(f'setblock {x} {y} {z-1} ae2:cable_bus{{cable:{{id:"ae2:fluix_glass_cable"}},north:{{id:"ae2:terminal"{extra}}}}}')
        self.cmd('gamemode survival @a');self.act('wait',ticks=60);self.act('regression-ae2-crafting-fixture',ticks=3)
        seed=json.loads((self.profile/'release-ae2-crafting-fixture.json').read_text());self.pattern=seed['pattern']
        for index,item,count,tag in [(10,'ae2:item_storage_cell_64k',1,CELL),(11,'minecraft:oak_log',16,None),(12,'minecraft:diamond',7,MARKER)]:
            suffix=''if tag is None else tag if self.mc=='1.20.1'else'[minecraft:custom_data='+tag+']';self.cmd(f'item replace entity @a container.{index} with {item}{suffix} {count}')
        self.look(1.5,2,.5,0,90);data=self.frame('original-provider','PatternProviderMenu');self.inventory=copy.deepcopy(data['inventory']);self.cell=copy.deepcopy(self.inventory[10]);self.logs=copy.deepcopy(self.inventory[11])
        self.check('genuine world recipe ingredient and powered empty native network prerequisites',self.pattern['item']=='ae2:crafting_pattern'and self.pattern['count']==1 and fixture_pattern(seed['decoded'],self.mc)and item_cell(self.inventory[9])==item_cell(self.pattern)and stack(self.cell['signature'],self.cell['count'])==stack(expected_item(self.mc,'ae2:item_storage_cell_64k',1,CELL))and stack(self.logs['signature'],self.logs['count'])==stack(ordinary(self.mc,'minecraft:oak_log',16))and stack(self.inventory[12]['signature'],self.inventory[12]['count'])==stack(marker_item(self.mc))and self.stock(0,0)and self.idle())
        target=self.physical_slot();self.act('regression-player-click',slot=9,ticks=6);self.slot(target);data=self.frame('pattern-inserted','PatternProviderMenu');self.inventory[9]['count']=0
        self.check('original provider accepts the real pattern with complete item data',self.owned(data,self.inventory,cell=False)and self.stock(0,0)and self.idle())
        self.act('close',ticks=4);self.look(-.5,2,.5,0,90);data=self.frame('original-drive','DriveMenu');self.drive_session=self.client['session'];self.drive_slot=self.physical_slot();self.act('regression-player-click',slot=10,ticks=6);self.slot(self.drive_slot);data=self.frame('cell-inserted','DriveMenu');self.inventory[10]['count']=0
        self.check('real drive cell enters through original slot and preserves full typed shell',self.owned(data,self.inventory)and self.stock(0,0)and self.idle())
        self.look(.5,0,3.5,180,20);data=self.frame('original-terminal','MEStorageMenu');self.act('regression-player-click',slot=11,ticks=6);self.repo();data=self.frame('logs-deposited','MEStorageMenu');self.inventory[11]['count']=0
        self.check('real repository deposit stores exactly sixteen ordinary logs',self.owned(data,self.inventory)and self.stock(16,0)and self.idle())
        self.entry_records.append(dict(original=entry,executed_world_actions=actions,additional_world='Ordinary empty provider/drive/assembler/CPU/power and second cable terminal; no machine inventory or client models assigned',ingredient='Original native world-recipe encoder in player9 before entry',actual_runtime_staging='staged-artifacts.json'))
    def request(self,amount,label):
        if label == 'first':
            before = copy.deepcopy(self.client)
            assert before['menu'] == 'appeng.menu.me.common.MEStorageMenu'
            if before['locked']:
                self.act('regression-control', session=before['session'], control='LOCK', ticks=3)
            self.act('regression-move', session=before['session'], dx=0, dy=40-before['bounds']['y'], ticks=5)
            self.act('regression-ae2-child-state', ticks=3)
            observed = json.loads((self.profile/'release-ae2-child-state.json').read_text())
            rows = [v for v in observed['client'] if v['session'] == before['session'] and v['menu'] == before['menu']]
            assert len(rows) == 1
            after = rows[0]
            assert after['bounds'] == dict(before['bounds'], y=40)
            self.client = after
            (self.out/'ordinary-parent-placement.json').write_text(json.dumps(dict(
                before=before, after=after, observed=observed,
                action=dict(type='regression-move', session=before['session'], dx=0, dy=40-before['bounds']['y']),
                scope='Ordinary header drag before native child creation; no window coordinates or native fields assigned.'
            ), indent=2)+'\n')
        self.repo('minecraft:oak_planks',2);data=self.frame(label+'-amount','CraftAmountMenu');self.edit(self.client,amount);data=self.frame(label+'-amount-edited','CraftAmountMenu')
        self.check(label+' real amount child edits original numeric input without owned changes',self.c['what']==self.s['what']and self.c['what']['empty']is False and item_cell(self.c['what']['item'])[1]==item_cell(key_item(self.mc,'minecraft:oak_planks'))[1]and self.owned(data,self.inventory)and any(v.get('value')==str(amount)for v in self.client['widgets']))
        self.button('Next');self.act('wait',ticks=60);data=self.frame(label+'-native-plan','CraftConfirmMenu')
        self.check(label+' real calculation delivers matching complete original plan',self.c['summary']==self.s['summary']and self.c['summary'].get('ready')is True and self.c['summary']['simulation']is False and plan_matches(self.s['plan'],self.mc,amount)and self.c['error']==self.s['error']=='none'and self.owned(data,self.inventory))
    def return_cancelled_plan(self):
        self.act('regression-ae2-child-state', ticks=3)
        widgets = json.loads((self.profile/'release-ae2-child-state.json').read_text())
        self.act('regression-ae2-crafting-state', ticks=3)
        native = json.loads((self.profile/'release-ae2-crafting-state.json').read_text())
        self.act('regression-state', ticks=3)
        ledger = json.loads((self.profile/'regression-state.json').read_text())
        clients = [v for v in widgets['client'] if v['menu'] == 'appeng.menu.me.crafting.CraftAmountMenu']
        assert len(clients) == 1, 'Native confirmation Cancel must return to its amount screen'
        client = clients[0]
        c = [v for v in native['client'] if v['session'] == client['session']]
        s = [v for v in native['server'] if v['session'] == client['session']]
        assert len(c) == len(s) == 1 and c[0]['what'] == s[0]['what']
        what = c[0]['what']
        assert what['empty'] is False and what['amount'] == 12 and what['amountJavaType'] == 'java.lang.Long'
        assert item_cell(what['item'])[1] == item_cell(key_item(self.mc, 'minecraft:oak_planks'))[1]
        assert any(v.get('value') == '12' and v['visible'] and v['active'] for v in client['widgets'])
        self.native = native
        assert self.owned(ledger, self.inventory) and self.stock(16, 0) and self.idle()
        tabs = [v for v in client['widgets'] if v['type'].endswith('.TabButton') and v['visible'] and v['active']]
        assert len(tabs) == 1
        (self.out/'native-cancel-return.json').write_text(json.dumps(dict(
            widgets=widgets, native=native, ledger=ledger, return_tab=tabs[0],
            scope='Actual native Cancel returns to amount12 without starting work; ordinary native return tab then restores the terminal.'
        ), indent=2)+'\n')
        self.tap(client, tabs[0])
    def reopen_drive(self):
        self.act('regression-state', ticks=3)
        before = json.loads((self.profile/'regression-state.json').read_text())
        drives=[v for v in before['menus'] if v['type']=='appeng.menu.implementations.DriveMenu']
        assert len(drives)<=1
        if not drives:self.look(-.5, 2, .5, 0, 90)
        self.act('regression-ae2-child-state', ticks=3)
        after = json.loads((self.profile/'release-ae2-child-state.json').read_text())
        rows = [v for v in after['client'] if v['menu'] == 'appeng.menu.implementations.DriveMenu']
        assert len(rows) == 1
        (self.out/'ordinary-drive-reentry.json').write_text(json.dumps(dict(
            before=before, after=after, reopened=not bool(drives), ray=dict(dx=-.5, dy=2, dz=.5, yaw=0, pitch=90) if not drives else None,
            scope='Use the retained native Drive session when present; otherwise ordinary world block ray reopens the same untouched drive for physical recovery.'
        ), indent=2)+'\n')
    def run(self):
        self.setup();self.request(12,'first');before=self.client['bounds'];self.move(self.client);data=self.frame('confirmation-moved','CraftConfirmMenu')
        self.check('native computed plan and controls remain intact after window drag',self.client['bounds']['x']==before['x']+16 and self.client['bounds']['y']==before['y']+12 and self.c['summary']==self.s['summary']and plan_matches(self.s['plan'],self.mc,12)and self.owned(data,self.inventory)and self.stock(16,0))
        cpus=[v for v in self.client['widgets']if v['visible']and v['active']and v['message'].startswith('Crafting CPU:')];assert len(cpus)==1;self.tap(self.client,cpus[0]);data=self.frame('native-cpu-selected','CraftConfirmMenu')
        self.check('original CPU cycle selects actual available capacity without item mutation',all(v['values']['getCpuAvailableBytes']['value']>=65536 for v in [self.client,next(s for s in json.loads((self.profile/'release-ae2-child-state.json').read_text())['server']if s['session']==self.client['session'])])and self.owned(data,self.inventory)and plan_matches(self.s['plan'],self.mc,12))
        self.button('Cancel');self.return_cancelled_plan();data=self.frame('native-plan-cancelled','MEStorageMenu')
        self.check('original confirmation Cancel returns without consuming ingredients or starting job',self.stock(16,0)and self.idle()and self.owned(data,self.inventory))
        self.request(12,'accepted');self.button('Start');self.act('wait',ticks=160);data=self.frame('first-job-complete','MEStorageMenu')
        self.check('original Start really consumes three logs and creates twelve planks',self.stock(13,12)and self.idle()and self.owned(data,self.inventory))
        self.request(8,'second');confirm=self.client['session'];self.look(.5,0,-3.5,0,20);data=self.frame('second-real-terminal','MEStorageMenu');other=self.client['session']
        self.check('independent ordinary terminal opens while original confirmation stays alive',other!=confirm and any(v['session']==confirm and v['type'].endswith('.CraftConfirmMenu')for v in data['menus'])and self.stock(13,12)and self.owned(data,self.inventory))
        self.repo('minecraft:oak_log');data=self.frame('planned-materials-withdrawn','MEStorageMenu');held=copy.deepcopy(self.logs);held['count']=13
        self.check('real second terminal withdraws required ingredients without native plan assignment',self.stock(0,12)and self.owned(data,self.inventory,cursor=held))
        self.act('regression-player-click',slot=11,ticks=6);self.inventory[11]=copy.deepcopy(held);data=self.frame('withdrawal-stored','CraftConfirmMenu')
        self.check('planned ingredients move to actual player inventory while calculated plan stays native',plan_matches(self.s['plan'],self.mc,8)and self.c['summary']==self.s['summary']and self.stock(0,12)and self.owned(data,self.inventory))
        self.button('Start');data=self.frame('genuine-native-missing-error','CraftConfirmMenu')
        self.check('original Start receives genuine missing-ingredient local error on same server menu',self.client['screen']=='appeng.client.gui.me.crafting.CraftErrorScreen'and self.c['sameParentMenu']is True and self.c['error']==self.s['error']=='MISSING_INGREDIENT'and self.idle()and self.stock(0,12)and self.owned(data,self.inventory))
        before=self.client['bounds'];self.move(self.client);data=self.frame('native-error-moved','CraftConfirmMenu')
        self.check('real error window moves with intact original Retry Replan Cancel controls',self.client['bounds']['x']==before['x']+16 and self.client['bounds']['y']==before['y']+12 and {'Retry','Replan','Cancel'}<={v['message']for v in self.client['widgets']if v['active']and v['visible']}and self.owned(data,self.inventory))
        data=self.frame('second-terminal-restoration','MEStorageMenu');assert self.client['session']==other;self.act('regression-player-click',slot=11,ticks=6);self.repo();self.inventory[11]['count']=0;data=self.frame('ingredients-restored','MEStorageMenu')
        self.check('original repository restores exact withdrawn ingredients before retry',self.stock(13,12)and self.owned(data,self.inventory))
        data=self.frame('native-error-replan-ready','CraftConfirmMenu');self.button('Replan');self.act('wait',ticks=60);data=self.frame('native-error-replanned','CraftConfirmMenu')
        self.check('original Replan calculates a fresh complete plan after actual ingredient restoration',self.client['screen']=='appeng.client.gui.me.crafting.CraftConfirmScreen'and self.c['error']==self.s['error']=='none'and self.c['summary']==self.s['summary']and plan_matches(self.s['plan'],self.mc,8)and self.stock(13,12)and self.idle()and self.owned(data,self.inventory))
        data=self.frame('second-terminal-rewithdrawal','MEStorageMenu');assert self.client['session']==other;self.repo('minecraft:oak_log');self.act('regression-player-click',slot=11,ticks=6);self.inventory[11]=copy.deepcopy(held);data=self.frame('native-replanned-materials-withdrawn','CraftConfirmMenu')
        self.check('actual second withdrawal removes the newly replanned ingredients without changing plan',self.stock(0,12)and plan_matches(self.s['plan'],self.mc,8)and self.owned(data,self.inventory))
        self.button('Start');data=self.frame('second-genuine-native-error','CraftConfirmMenu')
        self.check('second original Start independently reaches genuine missing-ingredient error',self.client['screen']=='appeng.client.gui.me.crafting.CraftErrorScreen'and self.c['sameParentMenu']is True and self.c['error']==self.s['error']=='MISSING_INGREDIENT'and self.stock(0,12)and self.idle()and self.owned(data,self.inventory))
        data=self.frame('second-terminal-retry-restoration','MEStorageMenu');assert self.client['session']==other;self.act('regression-player-click',slot=11,ticks=6);self.repo();self.inventory[11]['count']=0;data=self.frame('native-retry-ingredients-restored','MEStorageMenu')
        self.check('original second repository restores every ingredient before genuine Retry',self.stock(13,12)and self.owned(data,self.inventory))
        self.act('regression-close',session=other,ticks=6);data=self.frame('error-retry-ready','CraftConfirmMenu');self.button('Retry');self.act('wait',ticks=160);data=self.frame('native-retry-complete','MEStorageMenu')
        self.check('original Retry consumes two more logs and creates eight more planks',self.stock(11,20)and self.idle()and self.owned(data,self.inventory))
        self.repo('minecraft:oak_planks');self.act('regression-player-click',slot=13,ticks=6);self.inventory[13]=dict(self.inventory[13],item='minecraft:oak_planks',count=20,signature=key_item(self.mc,'minecraft:oak_planks')['signature']);data=self.frame('crafted-output-recovered','MEStorageMenu')
        self.check('all twenty real crafted planks recover to player with exact item data',self.stock(11,0)and self.owned(data,self.inventory))
        self.repo('minecraft:oak_log');self.act('regression-player-click',slot=11,ticks=6);self.inventory[11]=dict(self.logs,count=11);data=self.frame('remaining-inputs-recovered','MEStorageMenu')
        self.check('eleven unconsumed original logs recover with no extra stored keys',self.stock(0,0)and self.idle()and self.owned(data,self.inventory))
        self.act('regression-close',session=self.client['session'],ticks=6);self.reopen_drive();data=self.frame('empty-cell-recovery','DriveMenu');recovered=copy.deepcopy(self.native['drive'][0]);self.slot(self.physical_slot());self.act('regression-player-click',slot=10,ticks=6);self.inventory[10]=recovered;data=self.frame('native-empty-cell-recovered','DriveMenu')
        self.check('actual empty storage cell recovers preserving all non-storage metadata',stored_matches(self.native['stored'],self.mc,0,0)and self.owned(data,self.inventory,cell=False)and cellshell(recovered,self.mc)==cellshell(self.cell,self.mc))
        self.act('close',ticks=4);self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());(self.out/'closed.json').write_text(json.dumps(data,indent=2)+'\n')
        self.check('all real crafting and child sessions close with exact final resources',not data['menus']and self.owned(data,self.inventory,cell=False));self.cases=[self.case];self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),ae2_crafting_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Crafting(profile,mc,loader).run()
