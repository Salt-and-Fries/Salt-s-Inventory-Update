"""Native references only: unchanged publisher mods, SIU absent, disposable world."""
from pathlib import Path
import sys,json,hashlib
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send
RUN=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-native-zenith-design-03'
OUT=ROOT/'output/zenith-window-design';OUT.mkdir(parents=True,exist_ok=True)
rows=[];state={}


def call(kind='wait',**args):
    global state
    r=send(RUN,kind,**args);state=r['client'];return state
def cmd(s):return call('command',command=s,ticks=4)
def slot(i):return call('slot',slot=i,ticks=5)
def put(item,destination):
    source=next(s['index']for s in state['slots']if s['player']and s['item']==item)
    slot(source);slot(destination)
def click(x,y):
    call('click',x=x,y=y,button=0,ticks=1);call('release',x=x,y=y,button=0,ticks=3)
def capture(id,title,block):
    id=id.replace('_','-')
    call('move',x=2,y=2,ticks=3);call('capture',capture=id,ticks=3)
    path=RUN/'screenshots'/(id+'.png');row=dict(id=id,title=title,block=block,capture=str(path.relative_to(ROOT)),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),state=state)
    rows.append(row);(OUT/'native-references.json').write_text(json.dumps(dict(minecraft='1.20.1',loader='fabric',release='2.0.2',siu_loaded=False,run=str(RUN.relative_to(ROOT)),cases=rows),indent=2));print('CAPTURE',id,flush=True)
def opened(block):
    call('close');call('fixture',block=block,ticks=6);cmd('gamemode survival @a');cmd('experience set @a 50 levels');call('open',ticks=12)


for name,title in [('gem_cutting_table','Gem Cutting Table'),('salvaging_table','Salvaging Table'),('simple_reforging_table','Simple Reforging Table')]:
    opened('zenith:'+name);capture('zenith-'+name,title,'zenith:'+name)

# Native populated reforge choices, generated through the original menu.
call('close');call('fixture',block='zenith:reforging_table');cmd('gamemode survival @a');cmd('experience set @a 50 levels')
call('held',item='minecraft:diamond_sword');cmd('item replace entity @a inventory.0 with zenith:common_material 64');cmd('item replace entity @a inventory.1 with zenith:sigil_of_rebirth 64');call('open',ticks=12)
put('minecraft:diamond_sword',0);put('zenith:common_material',1);put('zenith:sigil_of_rebirth',2);call('wait',ticks=18);capture('zenith-reforging_table','Reforging Table','zenith:reforging_table')

# Native generated affixes and an actual opened dropdown, never fabricated text.
call('close');call('fixture',block='zenith:augmenting_table');call('held',item='minecraft:diamond_sword');cmd('execute as @a run zenith lootify zenith:rare');cmd('item replace entity @a inventory.0 with zenith:sigil_of_enhancement 64');call('open',ticks=12)
put('minecraft:diamond_sword',0);put('zenith:sigil_of_enhancement',1);capture('zenith-augmenting_table','Augmenting Table','zenith:augmenting_table')
b=state['bounds'];click(b['x']+80,b['y']+23);capture('zenith-augmenting-dropdown','Augmenting / affix selection','zenith:augmenting_table')
click(b['x']+80,b['y']+43);capture('zenith-augmenting-selected','Augmenting / selected affix','zenith:augmenting_table')

for block,title in [('zenith:library','Enchantment Library'),('zenith:ender_library','Library of Alexandria')]:
    opened(block)
    for ench in ['sharpness','protection','unbreaking','efficiency','mending','fortune']:
        cmd('item replace entity @a hotbar.0 with minecraft:enchanted_book{StoredEnchantments:[{id:"minecraft:'+ench+'",lvl:3s}]}')
        call('wait',ticks=5);put('minecraft:enchanted_book',0)
    capture('zenith-'+block.split(':')[1],title,block)

call('close');call('fixture',block='minecraft:fletching_table');cmd('item replace entity @a hotbar.0 with minecraft:flint 16');cmd('item replace entity @a inventory.0 with minecraft:stick 16');cmd('item replace entity @a inventory.1 with minecraft:feather 16');call('open',ticks=12)
put('minecraft:flint',1);put('minecraft:stick',2);put('minecraft:feather',3);capture('zenith-fletching','Fletching Table','minecraft:fletching_table')

call('close');cmd('fill 6 65 6 10 66 10 minecraft:bookshelf outline');call('fixture',block='minecraft:enchanting_table');cmd('experience set @a 50 levels');cmd('item replace entity @a hotbar.0 with minecraft:diamond_sword');cmd('item replace entity @a inventory.0 with minecraft:lapis_lazuli 64');call('open',ticks=12)
put('minecraft:diamond_sword',0);put('minecraft:lapis_lazuli',1);capture('zenith-enchanting','Enchanting Table','minecraft:enchanting_table')
b=state['bounds'];click(b['x']+155,b['y']-8);capture('zenith-enchanting-info','Available Enchantments (native screen)','minecraft:enchanting_table')
call('close');call('held',item='patchouli:guide_book',snbt='{ "patchouli:book":"zenith:chronicle" }');call('use-item',ticks=15);capture('zenith-chronicle','Chronicle of Shadows (native guidebook)','patchouli:guide_book')
