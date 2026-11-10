"""Native processing amount child, cancel/back, persistence and real encoding."""
from pathlib import Path
import copy,hashlib,json
from ae2_settings_features import Settings,ENTRIES,isolate,translate,pair,item_cell,stack,MARKER,marker_item
from ae2_quantum_features import expected_item,conserved

INPUT='{release_processing_input:89,typed:[B;1b,7b]}'
OUTPUT='{release_processing_output:97,typed:[B;3b,9b]}'


def amount_cell(value,key,amount):
    if type(value.get('empty'))is not bool:return False
    if amount is None:return value['empty']is True
    return value['empty']is False and value['amountJavaType']=='java.lang.Long'and type(value['amount'])is int and value['amount']==amount and type(value['item']['count'])is int and value['item']['count']==1 and item_cell(value['item'])[1]==item_cell(key)[1]


def array_matches(values,key,amount):
    return bool(values)and amount_cell(values[0],key,amount)and all(v.get('empty')is True for v in values[1:])


def decoded_matches(value,input_key,output_key):
    if value.get('type')!='appeng.crafting.pattern.AEProcessingPattern':return False
    inputs=value['inputs'];outputs=value['outputs']
    return len(inputs)==1 and type(inputs[0]['multiplier'])is int and inputs[0]['multiplier']==37 and len(inputs[0]['alternatives'])==1 and amount_cell(inputs[0]['alternatives'][0],input_key,1)and len(outputs)==1 and amount_cell(outputs[0],output_key,5)


class Processing(Settings):
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.checks=[];self.n=0;self.case='processing-amount';self.cases=[];self.entry_records=[]
        self.out=self.profile/'ae2-processing-release';self.out.mkdir();self.command='server-command'if loader=='neoforge'else'command'
        self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,original_entries=self.entry_records,driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='Original processing-mode ghost slots and local amount child on the same native parent menu; real Set/back/cancel, movement, reopening and zero clear. A real blank pattern is consumed through the original Encode button, its actual native decoded full keys/amounts verified and the encoded item recovered. Complete typed player/cursor/owned-slot conservation. No downstream processing-machine production or fluid encoding claim.'),indent=2)+'\n')
    def frame(self,label,child=False):
        self.act('regression-ae2-child-state',ticks=3);p=json.loads((self.profile/'release-ae2-child-state.json').read_text());self.client,self.server=pair(p,'PatternEncodingTermMenu')
        wanted='SetProcessingPatternAmountScreen'if child else'PatternEncodingTermScreen';assert self.client['screen'].rsplit('.',1)[-1]==wanted
        self.act('regression-ae2-processing-state',ticks=3);self.native=json.loads((self.profile/'release-ae2-processing-state.json').read_text())
        clients=[v for v in self.native['client']if v['session']==self.client['session']];servers=[v for v in self.native['server']if v['session']==self.client['session']]
        assert len(clients)==len(servers)==1;self.c,self.s=clients[0],servers[0];assert self.c['screen']==self.client['screen']and self.c['sameParentMenu']is True
        self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(dict(native=self.native,widgets=p,ledger=data),indent=2)+'\n');return data
    def config(self,a,b):
        return self.c['mode']==self.s['mode']=='PROCESSING'and array_matches(self.s['inputs'],self.input,a)and array_matches(self.s['outputs'],self.output,b)and all([v['decoded']for v in self.c[k]]==self.s[k]for k in ['inputs','outputs'])
    def slot(self,kind,button=0):
        slot=self.c[kind][0]if kind in ['inputs','outputs']else self.c[kind];assert slot['visible']is True
        self.act('regression-tap',session=self.client['session'],x=self.c['left']+slot['x']+7,y=self.c['top']+slot['y']+7,button=button,ticks=15)
    def child_key(self,key,amount):return amount_cell(self.c['current'],key,amount)
    def set_value(self,value):
        self.edit(self.client,value);buttons=[v for v in self.client['widgets']if v['message']=='Set'and v['visible']and v['active']];assert len(buttons)==1;self.tap(self.client,buttons[0])
    def setup(self):
        self.act('close',ticks=3)
        for cmd in isolate(self.origin):self.cmd(cmd)
        self.cmd('clear @a');entries=[v for v in json.loads(ENTRIES.read_text())['cases']if v['id']=='atlas-ae2-pattern-encoding-terminal'and(v['minecraft'],v['loader'])==(self.mc,self.loader)];assert len(entries)==1
        entry=entries[0];old=[int(v)for v in entry['captured_origin'].split(',')];actions=[];entry_action=None
        for original in entry['actions']:
            action=copy.deepcopy(original);kind=action.pop('type');assert kind in {'fixture','command','use-block-ray'},('Unreviewed processing fixture action',kind)
            if kind=='use-block-ray':assert entry_action is None;entry_action=action;continue
            if kind=='command':action['command']=translate(action['command'],old,self.origin);kind=self.command
            self.act(kind,**action);actions.append(dict(type=kind,**action))
        assert entry_action is not None;self.cmd('gamemode survival @a')
        for index,item,count,tag in [(9,'minecraft:stone',3,INPUT),(10,'minecraft:diamond',2,OUTPUT),(11,'ae2:blank_pattern',1,None),(12,'minecraft:diamond',7,MARKER)]:
            suffix=''if tag is None else tag if self.mc=='1.20.1'else'[minecraft:custom_data='+tag+']';self.cmd(f'item replace entity @a container.{index} with {item}{suffix} {count}')
        self.act('use-block-ray',**entry_action);data=self.frame('native-parent');self.inventory=copy.deepcopy(data['inventory']);self.input=copy.deepcopy(self.inventory[9]);self.output=copy.deepcopy(self.inventory[10]);self.blank=copy.deepcopy(self.inventory[11])
        self.check('original parent opens with empty config and exact typed prerequisites',self.client['screen']==entry['expected_native_screen']and self.client['menu']==entry['expected_native_menu']and self.s['mode']==self.c['mode']=='CRAFTING'and all(v['empty']is True for k in ['inputs','outputs']for v in self.s[k])and all(stack(self.inventory[i]['signature'],self.inventory[i]['count'])==stack(expected_item(self.mc,item,count,tag))for i,item,count,tag in [(9,'minecraft:stone',3,INPUT),(10,'minecraft:diamond',2,OUTPUT)])and stack(self.inventory[12]['signature'],self.inventory[12]['count'])==stack(marker_item(self.mc))and self.blank['item']=='ae2:blank_pattern'and type(self.blank['count'])is int and self.blank['count']==1 and conserved(data,self.inventory))
        self.entry_records.append(dict(original=entry,executed_world_actions=actions,entry='Original ordinary front-face ray after declared player-only ingredients',actual_runtime_staging='staged-artifacts.json'))
    def run(self):
        self.setup();modes=[v for v in self.c['modes']if v['mode']=='PROCESSING'and v['visible']and v['active']];assert len(modes)==1;self.tap(self.client,modes[0]);data=self.frame('processing-mode')
        self.check('original mode tab changes actual native processing logic',self.config(None,None)and conserved(data,self.inventory))
        for index,kind,key,a,b in [(9,'inputs',self.input,3,None),(10,'outputs',self.output,3,2)]:
            inv=copy.deepcopy(self.inventory);inv[index]['count']=0;self.act('regression-player-click',slot=index,ticks=6);self.slot(kind);data=self.frame('ghost-'+kind)
            self.check('original '+kind+' ghost retains complete physical template and key',self.config(a,b)and conserved(data,inv,cursor=key))
            self.act('regression-player-click',slot=index,ticks=6)
        data=self.frame('both-templates-restored');self.check('both ghost selections leave the complete inventory intact',self.config(3,2)and conserved(data,self.inventory))
        parent_session=self.client['session'];self.slot('inputs',2);data=self.frame('original-input-amount-child',True)
        self.check('original local child retains parent menu and exact input amount3',self.client['session']==parent_session and self.child_key(self.input,3)and self.config(3,2)and conserved(data,self.inventory))
        before=self.client['bounds'];self.move(self.client);data=self.frame('input-child-moved',True)
        self.check('local amount child moves without duplicating menu or owned items',self.client['session']==parent_session and self.client['bounds']['x']==before['x']+16 and self.client['bounds']['y']==before['y']+12 and self.child_key(self.input,3)and conserved(data,self.inventory))
        self.set_value(37);data=self.frame('input37-set');self.check('moved original Set sends actual input amount37 to native host',self.config(37,2)and conserved(data,self.inventory))
        self.slot('outputs',2);data=self.frame('original-output-child',True);self.check('output child selects original full output key and amount2',self.child_key(self.output,2)and self.config(37,2)and conserved(data,self.inventory))
        self.edit(self.client,41);backs=[v for v in self.client['widgets']if v['type'].endswith('.TabButton')and v['visible']and v['active']];assert len(backs)==1;self.tap(self.client,backs[0]);data=self.frame('output-cancel-back')
        self.check('original back discards unapplied amount41 and preserves output2',self.config(37,2)and conserved(data,self.inventory))
        self.slot('outputs',2);data=self.frame('output-child-reopened',True);self.check('ordinary child reopening still reads unchanged output2',self.child_key(self.output,2)and conserved(data,self.inventory))
        self.set_value(5);data=self.frame('output5-set');self.check('second original Set updates only output amount5',self.config(37,5)and conserved(data,self.inventory))
        self.reopen();data=self.frame('native-parent-reopened');self.check('original source reopening persists both full keys and amounts',self.config(37,5)and conserved(data,self.inventory))
        self.act('regression-player-click',slot=11,ticks=6);self.slot('blank');data=self.frame('real-blank-inserted');inv=copy.deepcopy(self.inventory);inv[11]['count']=0
        self.check('real blank pattern enters original physical input exactly once',self.config(37,5)and item_cell(self.s['blank'])==item_cell(self.blank)and self.s['encoded']['count']==0 and conserved(data,inv,stored=self.blank))
        encode=[v for v in self.client['widgets']if v['type'].endswith('.ActionButton')and v['visible']and v['active']and any('Encode'in x for x in v.get('tooltips',[]))];assert len(encode)==1;self.tap(self.client,encode[0]);data=self.frame('native-real-pattern-encoded');self.encoded=copy.deepcopy(self.s['encoded'])
        self.check('original Encode consumes blank and produces correctly decoded native pattern',self.config(37,5)and self.s['blank']['count']==0 and self.encoded['item']=='ae2:processing_pattern'and type(self.encoded['count'])is int and self.encoded['count']==1 and decoded_matches(self.s.get('decoded',{}),self.input,self.output)and conserved(data,inv,stored=self.encoded))
        self.slot('encoded');self.act('regression-player-click',slot=11,ticks=6);data=self.frame('encoded-pattern-recovered');self.inventory[11]=copy.deepcopy(self.encoded)
        self.check('real encoded pattern returns to player with complete item data',self.config(37,5)and self.s['blank']['count']==self.s['encoded']['count']==0 and conserved(data,self.inventory))
        self.slot('inputs',2);data=self.frame('input-zero-child',True);self.check('reopened input child reflects persisted amount37',self.child_key(self.input,37)and conserved(data,self.inventory))
        self.set_value(0);data=self.frame('input-zero-cleared');self.check('original zero Set clears only input config without consuming items',self.config(None,5)and conserved(data,self.inventory))
        self.slot('outputs');data=self.frame('output-ghost-cleared');self.check('empty-cursor native ghost clear removes output without owned changes',self.config(None,None)and conserved(data,self.inventory))
        self.act('close',ticks=4);self.act('regression-state',ticks=3);data=json.loads((self.profile/'regression-state.json').read_text());(self.out/'closed.json').write_text(json.dumps(data,indent=2)+'\n')
        self.check('all processing sessions close with recovered output and original templates',not data['menus']and conserved(data,self.inventory));self.cases=[self.case];self.save(True)
        return dict(cases=1,failed=0,checks=len(self.checks),ae2_processing_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Processing(profile,mc,loader).run()
