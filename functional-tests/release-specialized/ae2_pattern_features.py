"""Real provider insertion and native remote pattern-slot packets/search."""
from pathlib import Path
import copy,hashlib,json
from ae2_settings_features import Settings,ENTRIES,isolate,translate,pair,item_cell,stack,MARKER,marker_item
from ae2_quantum_features import conserved,expected_item

REFUSAL='{release_pattern_refusal:71,typed:[B;3b,8b]}'


def contents(items):return tuple(item_cell(item)for item in items)


def decoded_fixture(seed):
    return seed['item']['item']=='ae2:processing_pattern'and type(seed['item']['count'])is int and seed['item']['count']==1 and type(seed['inputMultiplier'])is int and seed['inputMultiplier']==1 and all(
        seed[key]['id']==name and seed[key]['javaType']=='java.lang.Long'and type(seed[key]['amount'])is int and seed[key]['amount']==1
        for key,name in [('decodedInput','minecraft:stone'),('decodedOutput','minecraft:diamond')])


def synchronized(native,meta):
    clients=[v for v in native['client']if v['menu'].endswith('.PatternAccessTermMenu')]
    if len(clients)!=1 or clients[0]['session']!=meta['session']:return False
    servers=[v for v in native['server']if(v['session'],v['menu'])==(meta['session'],meta['menu'])]
    if len(servers)!=1:return False
    c,s=clients[0],servers[0]
    if c['shownProviders']!=s['shownProviders']or len(c['records'])!=1 or len(s['records'])!=1:return False
    cr,sr=c['records'][0],s['records'][0]
    if type(cr['serverId'])is not int or type(sr['serverId'])is not int or cr['serverId']!=sr['serverId']:return False
    return contents(cr['items'])==contents(sr['items'])==contents(native['provider'])


class Patterns(Settings):
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0;self.case='pattern-access';self.cases=[];self.entry_records=[]
        self.out=self.profile/'ae2-pattern-release';self.out.mkdir();self.command='server-command'if loader=='neoforge'else'command'
        self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,original_entries=self.entry_records,
            driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='One genuine encoded processing pattern created only as a pre-entry player ingredient, native pointer insertion into actual provider, populated native pattern-access records/remote slots, decoded-output search and impossible filter, incompatible refusal, full typed pickup/return/moved input, native shown-provider filter and ordinary reopening. No assigned client records/provider contents, direct handler calls, real processing output or exhaustive provider topology/large-list scroll claim.'),indent=2)+'\n')
    def frame(self,label,kind='PatternAccessTermMenu'):
        self.act('regression-ae2-child-state',ticks=3);p=json.loads((self.profile/'release-ae2-child-state.json').read_text());self.client,_=pair(p,kind)
        self.act('regression-ae2-pattern-state',ticks=3);native=json.loads((self.profile/'release-ae2-pattern-state.json').read_text())
        meta=[v for v in native['client']if v['session']==self.client['session']and v['menu']==self.client['menu']];assert len(meta)==1
        self.meta=meta[0];self.native=native;self.kind=kind
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(dict(native=native,widgets=p,ledger=data),indent=2)+'\n')
        return data
    def owned(self,data,inventory,provider,cursor=None):
        stored=next((v for v in provider if v['count']),None)if self.kind=='PatternProviderMenu'else None
        return conserved(data,inventory,cursor,stored)and contents(self.native['provider'])==contents(provider)and self.native['providerVisible']is True
    def sync(self):return synchronized(self.native,self.meta)
    def remote(self):
        slots=[v for v in self.meta['slots']if v['type'].endswith('.PatternSlot')and v['containerIndex']==0 and v['visible']is True]
        assert len(slots)==1,'Expected one original visible remote pattern cell0'
        return slots[0]
    def tap_cell(self,cell):
        assert cell['visible']is True
        self.act('regression-tap',session=self.client['session'],x=self.meta['left']+cell['x']+8,y=self.meta['top']+cell['y']+8,ticks=18)
    def search(self,value):
        boxes=[w for w in self.client['widgets']if 'value'in w and w['visible']and w['active']];assert len(boxes)==1,'Expected original native search field'
        self.tap(self.client,boxes[0]);self.act('key',key=65,modifiers=2,ticks=2)
        if value:self.act('text',text=value,ticks=14)
        else:self.act('key',key=259,ticks=14)
    def click_player(self,index):self.act('regression-player-click',slot=index,ticks=9)
    def access(self):
        self.act('close',ticks=3);x,y,z=self.origin;self.cmd(f'tp @a {x+.5} {y} {z+3.5} 180 24');self.act('use-block-ray',ticks=30)
    def setup(self):
        self.act('close',ticks=3)
        for cmd in isolate(self.origin):self.cmd(cmd)
        self.cmd('clear @a');entries=[v for v in json.loads(ENTRIES.read_text())['cases']if v['id']=='atlas-ae2-pattern-access-terminal'and(v['minecraft'],v['loader'])==(self.mc,self.loader)];assert len(entries)==1
        entry=entries[0];old=[int(v)for v in entry['captured_origin'].split(',')];actions=[]
        for original in entry['actions']:
            action=copy.deepcopy(original);kind=action.pop('type');assert kind in {'fixture','command','use-block-ray'},('Unreviewed pattern fixture action',kind)
            if kind=='use-block-ray':continue # Defer genuine terminal entry until the real provider has been filled through its UI.
            if kind=='command':action['command']=translate(action['command'],old,self.origin);kind=self.command
            self.act(kind,**action);actions.append(dict(type=kind,**action))
        x,y,z=self.origin;cmd=f'setblock {x-1} {y} {z} ae2:pattern_provider';self.cmd(cmd);actions.append(dict(type=self.command,command=cmd))
        self.act('regression-ae2-pattern-fixture',ticks=5);seed=json.loads((self.profile/'release-ae2-pattern-fixture.json').read_text());self.pattern=seed['item']
        self.cmd('gamemode survival @a')
        for index,item,count,tag in [(10,'minecraft:stone',3,REFUSAL),(12,'minecraft:diamond',7,MARKER)]:
            suffix=tag if self.mc=='1.20.1'else'[minecraft:custom_data='+tag+']'
            self.cmd(f'item replace entity @a container.{index} with {item}{suffix} {count}')
        self.cmd(f'tp @a {x-.5} {y+2} {z+.5} 0 90');self.act('wait',ticks=35);self.act('use-block-ray',ticks=25)
        data=self.frame('original-empty-provider','PatternProviderMenu');self.inventory=copy.deepcopy(data['inventory']);self.empty_provider=copy.deepcopy(self.native['provider']);self.full_provider=copy.deepcopy(self.empty_provider)
        assert self.full_provider;self.full_provider[0]=copy.deepcopy(self.pattern);self.empty9=copy.deepcopy(self.inventory);self.empty9[9]['count']=0
        self.stone=copy.deepcopy(self.inventory[10]);self.empty9_10=copy.deepcopy(self.empty9);self.empty9_10[10]['count']=0
        self.check('genuine native encoder ingredient decodes with exact native input and output',decoded_fixture(seed)and item_cell(data['inventory'][9])==item_cell(self.pattern)and stack(data['inventory'][10]['signature'],data['inventory'][10]['count'])==stack(expected_item(self.mc,'minecraft:stone',3,REFUSAL))and stack(data['inventory'][12]['signature'],data['inventory'][12]['count'])==stack(marker_item(self.mc)))
        self.check('actual provider starts empty with original restricted pattern cells',self.client['screen'].endswith('.PatternProviderScreen')and self.client['menu']=='appeng.menu.implementations.PatternProviderMenu'and all(v['count']==0 for v in self.empty_provider)and self.owned(data,self.inventory,self.empty_provider))
        cells=[v for v in self.meta['slots']if v['type'].endswith('.RestrictedInputSlot')and v['containerIndex']==0 and v['visible']is True];assert len(cells)==1,'Expected native provider pattern cell0, not player menu slot0'
        self.provider_cell=cells[0];self.entry_records.append(dict(original=entry,executed_world_actions=actions,prerequisite='One valid encoded processing pattern only in player slot9 via original native encoder; provider untouched until actual pointer input',actual_runtime_staging='staged-artifacts.json'))
    def run(self):
        self.setup();self.click_player(9);data=self.frame('native-pattern-picked-up','PatternProviderMenu')
        self.check('original pickup preserves the complete encoded pattern and typed foreign items',self.owned(data,self.empty9,self.empty_provider,self.pattern))
        self.tap_cell(self.provider_cell);data=self.frame('native-provider-inserted','PatternProviderMenu')
        self.check('native restricted provider slot accepts the complete encoded item exactly once',self.owned(data,self.empty9,self.full_provider))
        self.access();data=self.frame('actual-populated-pattern-access')
        self.check('ordinary original terminal receives actual populated provider records',self.client['screen']=='appeng.client.gui.me.patternaccess.PatternAccessTermScreen'and self.owned(data,self.empty9,self.full_provider)and self.sync()and self.meta['rows']>0 and item_cell(self.remote()['item'])==item_cell(self.pattern))
        self.search('diamond');data=self.frame('decoded-output-search')
        self.check('original search matches the native decoded output name',self.meta['search']=='diamond'and self.meta['rows']>0 and self.sync()and self.owned(data,self.empty9,self.full_provider)and item_cell(self.remote()['item'])==item_cell(self.pattern))
        self.search('release_no_matching_pattern_985');data=self.frame('impossible-pattern-search')
        self.check('impossible search hides rows without changing native records or owned items',self.meta['search']=='release_no_matching_pattern_985'and self.meta['rows']==0 and not self.meta['slots']and self.sync()and self.owned(data,self.empty9,self.full_provider))
        self.search('');data=self.frame('native-search-cleared')
        self.check('clearing original search restores the same complete remote pattern',self.meta['search']==''and self.meta['rows']>0 and self.sync()and self.owned(data,self.empty9,self.full_provider)and item_cell(self.remote()['item'])==item_cell(self.pattern))
        self.click_player(10);self.tap_cell(self.remote());data=self.frame('native-incompatible-remote-input')
        self.check('native remote-slot filter refuses incompatible items without losing the pattern',self.sync()and self.owned(data,self.empty9_10,self.full_provider,self.stone))
        self.click_player(10);self.tap_cell(self.remote());data=self.frame('native-remote-pattern-recovered')
        self.check('original remote pickup removes exactly the native encoded provider item',self.sync()and self.owned(data,self.empty9,self.empty_provider,self.pattern))
        self.tap_cell(self.remote());data=self.frame('native-remote-pattern-returned')
        self.check('original remote placement returns the complete pattern to its actual provider',self.sync()and self.owned(data,self.empty9,self.full_provider))
        before=self.client['bounds'];self.move(self.client);data=self.frame('pattern-access-moved')
        self.check('pattern terminal moves through actual unlocked drag with unchanged records',self.client['bounds']['x']==before['x']+16 and self.client['bounds']['y']==before['y']+12 and self.sync()and self.owned(data,self.empty9,self.full_provider))
        self.tap_cell(self.remote());data=self.frame('moved-remote-pattern-pickup')
        self.check('moved original remote slot still addresses the correct native provider',self.sync()and self.owned(data,self.empty9,self.empty_provider,self.pattern))
        self.click_player(9);data=self.frame('encoded-item-restored-to-player')
        self.check('real remote recovery restores every original player slot and clears cursor',self.sync()and self.owned(data,self.inventory,self.empty_provider))
        self.click_player(9);self.tap_cell(self.remote());data=self.frame('moved-remote-pattern-insertion')
        self.check('moved original remote input can insert the full pattern again',self.sync()and self.owned(data,self.empty9,self.full_provider))
        original=self.meta['shownProviders'];buttons=[w for w in self.meta['settings']if w['currentValue']==original and w['visible']and w['active']];assert len(buttons)==1
        self.tap(self.client,buttons[0]);data=self.frame('native-shown-provider-filter');changed=self.meta['shownProviders']
        self.check('original shown-provider control changes its real synchronized native setting',changed!=original and self.sync()and self.owned(data,self.empty9,self.full_provider))
        self.access();data=self.frame('pattern-access-ordinary-reopening')
        self.check('ordinary terminal reopening retains native filter and complete physical pattern',self.meta['shownProviders']==changed and self.sync()and self.owned(data,self.empty9,self.full_provider)and item_cell(self.remote()['item'])==item_cell(self.pattern))
        self.tap_cell(self.remote());self.click_player(9);data=self.frame('final-complete-native-recovery')
        self.check('final original remote withdrawal settles all items once',self.sync()and self.owned(data,self.inventory,self.empty_provider))
        self.act('close',ticks=4);self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text())
        (self.out/'closed.json').write_text(json.dumps(data,indent=2)+'\n')
        self.check('native pattern sessions close with original inventory and empty cursor',not data['menus']and conserved(data,self.inventory))
        self.cases=[self.case];self.save(True);return dict(cases=1,failed=0,checks=len(self.checks),ae2_pattern_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Patterns(profile,mc,loader).run()
