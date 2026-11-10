"""Real repeated menus, native death/respawn and dimension/source invalidation."""
from pathlib import Path
import collections,copy,hashlib,json
import nbtlib
from ae2_settings_features import Settings
from ae2_quantum_features import expected_item
from mowzie_features import typed

DIAMOND='{release_lifecycle_diamond:163,typed:[B;1b,4b],nested:{check:0s}}'
BANNER='{release_lifecycle_banner:167,typed:[B;9b,5b],nested:{check:4L}}'
GOLD='{release_lifecycle_gold:173,typed:[B;2b,8b],nested:{check:7b}}'
REPEATS=36
EXPECTED_CHECKS=160
RESOURCES=[(9,'minecraft:diamond',17,DIAMOND),(10,'minecraft:white_banner',13,BANNER),(11,'minecraft:gold_ingot',7,GOLD)]
RESOURCE_IDS={v[1]for v in RESOURCES}


def item(text,mc):
    value=nbtlib.parse_nbt(text)
    assert isinstance(value,nbtlib.Compound)
    if not value:return 0,None,None
    key='Count'if mc=='1.20.1'else'count';kind=nbtlib.Byte if mc=='1.20.1'else nbtlib.Int
    assert isinstance(value.get('id'),nbtlib.String)and isinstance(value.get(key),kind)
    count=int(value[key]);assert count>0
    name=str(value['id']);value[key]=kind(1)
    return count,typed(value),name


def items(values,mc):
    result=collections.Counter()
    for value in values:
        count,key,_=item(value,mc)
        if count:result[key]+=count
    return result


def resources(mc):return items([expected_item(mc,name,count,tag)for _,name,count,tag in RESOURCES],mc)


def bounds(server):
    limits={'snapshotLimit':32,'sessionLimit':16,'sourceLimit':128,'graphNodeLimit':128,'graphEdgeLimit':512}
    if any(type(server[k])is not int or server[k]!=v for k,v in limits.items()):return False
    checks={'snapshots':32,'sources':128,'sourceKeys':128,'graphNodes':128,'graphEdges':512,'companions':16,'sourceLocks':16,'automaticSources':16}
    if any(type(server[k])is not int or not 0<=server[k]<=limit for k,limit in checks.items()):return False
    if server['sources']!=server['sourceKeys']or type(server['playerStates'])is not int or server['playerStates']!=1 or type(server['pendingUseTargets'])is not int or server['pendingUseTargets']!=0:return False
    if any(type(server[k])is not int or server[k]==0 for k in ['nonce','playerToken']):return False
    if type(server['worldMatches'])is not bool or server['worldMatches']is not True:return False
    if type(server['recipeHover'])is not bool or server['recipeHover']is not False:return False
    return len(server['menus'])<=16


def conserved(server,mc,expected):
    if not isinstance(server['inventory'],list)or not 36<=len(server['inventory'])<=4137:return False
    if any(type(v['index'])is not int or v['index']!=i for i,v in enumerate(server['inventory'])):return False
    values=[v['item']for v in server['inventory']]+[server['cursor']]
    for menu in server['menus']:
        # The older protocol keeps canonical mirrors; newer families explicitly
        # clear detached mirrors after committing to the player-owned cursor.
        if mc=='1.20.1':
            if items([menu['cursor']],mc)!=items([server['cursor']],mc):return False
        elif item(menu['cursor'],mc)[0]:return False
        # Loom input0/1/2 and crafting input1..9 are physical; a generated
        # result is an observed projection and must be EMPTY in this fixture.
        for slot in menu['slots']:
            if type(slot['projection'])is not bool:return False
            if slot['projection']:
                if item(slot['item'],mc)[0]:return False
            else:values.append(slot['item'])
    for drop in server['drops']:
        if item(drop['item'],mc)[2]in RESOURCE_IDS:values.append(drop['item'])
    return items(values,mc)==expected


def closed(state,mc,expected,*,sources_empty=False,native_death=False):
    server=state['server'];client=state['client']
    if type(client['sessions'])is not int or not 0<=client['sessions']<=16:return False
    if not bounds(server)or not conserved(server,mc,expected)or server['menus']or server['companions']or server['sourceLocks']or server['automaticSources']:return False
    if sources_empty and(server['sources']or server['sourceKeys']):return False
    if native_death:
        return client['deathScreen']is True and server['alive']is False
    return type(client['sessions'])is int and client['sessions']==0 and client['deathScreen']is False and server['alive']is True


class Lifecycle(Settings):
    def __init__(self,profile,mc,loader):
        self.profile=Path(profile);self.mc=mc;self.loader=loader;self.command='server-command'if loader=='neoforge'else'command';self.case='window-lifecycle';self.cases=[];self.checks=[];self.n=0
        self.out=self.profile/'lifecycle-release';self.out.mkdir();self.expected=resources(mc);self.tokens=set()
        self.act('regression-catalog',ticks=2);self.origin=json.loads((self.profile/'regression-catalog.json').read_text())['origin']
    def save(self,complete=False):
        (self.out/'summary.json').write_text(json.dumps(dict(complete=complete,cases=self.cases,checks=self.checks,driver_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope='Thirty-six actual crafting open/insert/recover/close cycles, moved input and bounded native session/snapshot/source history. Real native Loom transient input through death/Respawn button, dimension travel with retained shared cursor, block-source invalidation and ordinary reuse. Full typed inventory/input/cursor conservation and test-resource world drops across loaded dimensions. Original mixed publisher cohort retained; Visual Workbench persistence is respected by recovering the crafting grid before changing sources. No direct cleanup/session callbacks, pending-mutation disconnect, arbitrary death-with-cursor or long-duration performance claim.'),indent=2)+'\n')
    def frame(self,label):
        self.act('regression-lifecycle-state',ticks=4);value=json.loads((self.profile/'release-lifecycle-state.json').read_text());self.n+=1
        (self.out/(str(self.n).zfill(3)+'-'+label+'.json')).write_text(json.dumps(value,indent=2)+'\n');self.state=value;return value
    def approach(self):
        x,y,z=self.origin;self.cmd(f'tp @a {x+.5} {y} {z+3.5} 180 20.5')
    def open(self,kind):
        self.act('use-block-ray',ticks=20);state=self.frame('actual-'+kind+'-entry');s=state['server'];c=state['client']
        assert len(s['menus'])==1 and s['menus'][0][kind]is True and s['menus'][0]['valid']is True and c['sessions']==1
        self.menu=s['menus'][0];windows=[v for v in c['windows']if v['session']==self.menu['session']];assert len(windows)==1
        self.window=windows[0];return state
    def slot(self,index):self.act('regression-slot-click',session=self.menu['session'],slot=index,ticks=6)
    def player_slot(self,name):
        wanted=next((n,count,tag)for _,n,count,tag in RESOURCES if n==name);expected=item(expected_item(self.mc,*wanted),self.mc)
        found=[v['index']for v in self.state['server']['inventory']if item(v['item'],self.mc)==expected];assert len(found)==1,(name,found);return found[0]
    def valid(self,state):return bounds(state['server'])and conserved(state['server'],self.mc,self.expected)
    def full_input(self,state,index,name):
        if len(state['server']['menus'])!=1:return False
        menu=state['server']['menus'][0];matches=[v for v in menu['slots']if v['index']==index];wanted=next((n,count,tag)for _,n,count,tag in RESOURCES if n==name)
        return len(matches)==1 and matches[0]['projection']is False and item(matches[0]['item'],self.mc)==item(expected_item(self.mc,*wanted),self.mc)and self.valid(state)
    def close_source(self):self.act('regression-close',session=self.menu['session'],ticks=8)
    def run(self):
        self.act('close',ticks=3);self.act('fixture',block='minecraft:crafting_table',ticks=8)
        old=self.mc in ['1.20.1','1.21.1'];self.cmd('gamerule '+('keepInventory'if old else'minecraft:keep_inventory')+' true');self.cmd('gamerule '+('doImmediateRespawn'if old else'minecraft:immediate_respawn')+' false')
        self.cmd('gamemode survival @a');self.cmd('time set 6000');self.cmd('weather clear');x,y,z=self.origin;self.cmd(f'spawnpoint @a {x} {y} {z+3}');self.approach()
        for index,name,count,tag in RESOURCES:self.cmd(f'item replace entity @a container.{index} with {name}'+('{'+tag[1:-1]+'}'if old else'[minecraft:custom_data='+tag+']')+f' {count}')
        state=self.frame('declared-native-prerequisites');self.original=copy.deepcopy(state['server']);self.inventory=copy.deepcopy(state['server']['inventory'])
        self.check('original native actor and independently specified full typed prerequisites',closed(state,self.mc,self.expected,sources_empty=True)and state['server']['keepInventory']is True and state['server']['sameOriginalPlayer']is True and item(state['server']['cursor'],self.mc)[0]==0 and all(item(state['server']['inventory'][index]['item'],self.mc)==item(expected_item(self.mc,name,count,tag),self.mc)for index,name,count,tag in RESOURCES))
        for i in range(REPEATS):
            state=self.open('crafting');token=(self.menu['session'],self.menu['token']);self.check('cycle'+str(i)+' genuine fresh native crafting owner',type(token[0])is int and type(token[1])is int and token[1]!=0 and token not in self.tokens and self.valid(state));self.tokens.add(token)
            self.act('regression-player-click',slot=9,ticks=6);self.slot(1);state=self.frame('cycle'+str(i)+'-actual-full-input');self.check('cycle'+str(i)+' exact typed real crafting input',self.full_input(state,1,'minecraft:diamond')and item(state['server']['cursor'],self.mc)[0]==0)
            if i==REPEATS//2:
                before=self.window['bounds'];self.move(dict(session=self.menu['session'],locked=self.window['locked']));state=self.frame('actual-source-moved');after=next(v['bounds']for v in state['client']['windows']if v['session']==self.menu['session']);self.check('actual live source drags with its full typed input intact',after['x']==before['x']+16 and after['y']==before['y']+12 and self.full_input(state,1,'minecraft:diamond'))
            self.slot(1);self.act('regression-player-click',slot=9,ticks=6);state=self.frame('cycle'+str(i)+'-actual-recovery');self.check('cycle'+str(i)+' original complete resource recovery',self.valid(state)and state['server']['inventory']==self.inventory and item(state['server']['cursor'],self.mc)[0]==0)
            self.close_source();state=self.frame('cycle'+str(i)+'-actual-close');self.check('cycle'+str(i)+' closed source releases native ownership and keeps bounded histories',closed(state,self.mc,self.expected)and state['server']['inventory']==self.inventory)
        # Visual Workbench is installed in the real mixed cohorts and keeps its
        # grid natively. Recover that grid BEFORE switching world fixtures. Loom
        # supplies the genuine transient menu-removal contract for death/travel.
        self.act('close',ticks=5);self.cmd(f'setblock {x} {y} {z} minecraft:loom');self.approach();state=self.open('loom');self.check('native transient Loom source opens after repeated crafting cleanup',self.valid(state))
        banner_slot=self.player_slot('minecraft:white_banner');self.act('regression-player-click',slot=banner_slot,ticks=6);self.slot(0);state=self.frame('actual-transient-banner-before-death');self.check('death prerequisite is a real full typed transient native input',self.full_input(state,0,'minecraft:white_banner')and state['server']['keepInventory']is True and item(state['server']['cursor'],self.mc)[0]==0)
        self.cmd('kill @a');self.act('wait',ticks=60);state=self.frame('actual-native-death');self.check('actual native death closes server owners and preserves all typed resources',closed(state,self.mc,self.expected,sources_empty=True,native_death=True))
        buttons=[v for v in state['client']['widgets']if v['message']=='Respawn'and v['visible']is True and v['active']is True];assert len(buttons)==1;button=buttons[0]
        self.act('regression-lifecycle-native-tap',x=button['x']+button['width']/2,y=button['y']+button['height']/2,ticks=60);state=self.frame('actual-respawn-button-result');server=state['server']
        self.check('original Respawn button creates actual replacement player with clean owners and rotated authorization',closed(state,self.mc,self.expected,sources_empty=True)and server['sameOriginalPlayer']is False and server['uuid']==self.original['uuid']and server['nonce']==self.original['nonce']and server['playerToken']!=self.original['playerToken']and item(server['cursor'],self.mc)[0]==0)
        self.approach();self.act('wait',ticks=40);state=self.frame('native-recovery-at-actual-death-location')
        self.check('native pickup recovers any actual transient death drop with its complete typed contents',closed(state,self.mc,self.expected,sources_empty=True)and items([v['item']for v in state['server']['inventory']],self.mc)==self.expected and item(state['server']['cursor'],self.mc)[0]==0)
        state=self.open('loom');self.check('ordinary native source can reopen under actual replacement player credentials',self.valid(state))
        banner_slot=self.player_slot('minecraft:white_banner');gold_slot=self.player_slot('minecraft:gold_ingot');self.act('regression-player-click',slot=banner_slot,ticks=6);self.slot(0);self.act('regression-player-click',slot=gold_slot,ticks=6);state=self.frame('actual-input-and-shared-cursor-before-travel');gold=expected_item(self.mc,'minecraft:gold_ingot',7,GOLD)
        self.check('travel prerequisite owns real input and one full typed shared cursor',self.full_input(state,0,'minecraft:white_banner')and item(state['server']['cursor'],self.mc)==item(gold,self.mc))
        self.cmd(f'execute in minecraft:the_nether run fill {x-3} {y-1} {z-3} {x+3} {y-1} {z+5} minecraft:stone');self.cmd(f'execute in minecraft:the_nether run fill {x-3} {y} {z-3} {x+3} {y+4} {z+5} minecraft:air');pretravel=copy.deepcopy(state['server'])
        self.cmd(f'execute in minecraft:the_nether run tp @a {x+.5} {y} {z+3.5} 180 20.5');self.act('wait',ticks=80);state=self.frame('actual-native-dimension-change');server=state['server']
        self.check('actual dimension travel ends source lifetimes while conserving the shared native cursor',closed(state,self.mc,self.expected,sources_empty=True)and'minecraft:the_nether'in server['dimension']and server['dimension']!=pretravel['dimension']and server['uuid']==pretravel['uuid']and server['nonce']==pretravel['nonce']and item(server['cursor'],self.mc)==item(gold,self.mc))
        self.act('regression-player-click',slot=gold_slot,ticks=6);state=self.frame('actual-shared-cursor-restored');self.check('ordinary player input restores the actual carried resource after world change',closed(state,self.mc,self.expected)and item(state['server']['cursor'],self.mc)[0]==0)
        self.cmd(f'execute in minecraft:overworld run tp @a {x+.5} {y} {z+3.5} 180 20.5');self.act('wait',ticks=60);state=self.frame('actual-overworld-return');self.check('actual return preserves complete resource ownership and connection',closed(state,self.mc,self.expected)and'minecraft:overworld'in state['server']['dimension']and state['server']['nonce']==self.original['nonce'])
        state=self.open('loom');banner_slot=self.player_slot('minecraft:white_banner');self.act('regression-player-click',slot=banner_slot,ticks=6);self.slot(0);state=self.frame('real-transient-input-before-source-destruction');self.check('native input works again after dimension return',self.full_input(state,0,'minecraft:white_banner'))
        self.cmd(f'setblock {x} {y} {z} minecraft:air');self.act('wait',ticks=20);state=self.frame('actual-invalidated-world-source');self.check('native source destruction releases the real menu and returns all transient items',closed(state,self.mc,self.expected)and item(state['server']['cursor'],self.mc)[0]==0)
        self.cmd(f'setblock {x} {y} {z} minecraft:loom');state=self.open('loom');banner_slot=self.player_slot('minecraft:white_banner');self.act('regression-player-click',slot=banner_slot,ticks=6);self.slot(0);state=self.frame('ordinary-new-source-after-invalidation');self.check('fresh source credentials and inputs remain usable after invalidation',self.full_input(state,0,'minecraft:white_banner'))
        self.slot(0);self.act('regression-player-click',slot=banner_slot,ticks=6);self.close_source();self.act('close',ticks=8);state=self.frame('all-original-workspaces-closed');self.check('final actual workspace closure leaves full resources and no owned sessions locks or cursor',closed(state,self.mc,self.expected,sources_empty=True)and item(state['server']['cursor'],self.mc)[0]==0)
        self.cases.append(self.case);self.save(True);assert len(self.checks)==EXPECTED_CHECKS,(len(self.checks),EXPECTED_CHECKS)
        return dict(cases=1,failed=0,checks=len(self.checks),lifecycle_features_checked=True)


def run(profile,mc,loader,*args,**kwargs):return Lifecycle(profile,mc,loader).run()
