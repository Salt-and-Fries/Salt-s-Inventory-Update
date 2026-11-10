"""Capture matched vanilla/SIU merchant states for a visual review only."""
from pathlib import Path
import sys,json
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'project-everything-ten'))
from send import send

out=Path(sys.argv[1]);siu=sys.argv[2]=='siu';side='siu' if siu else 'vanilla'
state={}
def call(kind,**args):
    global state
    r=send(out,kind,**args);state=r['client'];return r
def command(s):call('command',command=s,ticks=5)
def tap(x,y):
    call('click',x=x,y=y,button=0,ticks=1);call('release',x=x,y=y,button=0,ticks=6)
def capture(label):
    call('capture',capture=side+'-'+label,ticks=10)
    folder=out/'merchant-review';folder.mkdir(exist_ok=True)
    (folder/(label+'.json')).write_text(json.dumps(state,indent=2))
def select(row):
    if siu:
        b=next(w['bounds'] for w in state['workspaceWindows'] if w['kind']=='CONTAINER')
        # Native SIU merchant list starts 13px into the frame, below its title.
        tap(b['x']+30,b['y']+38+row*20)
    else:
        b=state['bounds'];tap(b['x']+30,b['y']+28+row*20)
def recipe(buy,count,sell,sell_count=1,buy_b=None,uses=0,max_uses=16,tag=''):
    result='{id:"'+sell+'",Count:'+str(sell_count)+'b'+(',tag:'+tag if tag else '')+'}'
    return '{buy:{id:"'+buy+'",Count:'+str(count)+'b},sell:'+result+(',buyB:{id:"minecraft:book",Count:1b}' if buy_b else '')+',uses:'+str(uses)+',maxUses:'+str(max_uses)+',xp:5,rewardExp:0b,priceMultiplier:0.0f}'

recipes=[recipe('minecraft:paper',24,'minecraft:emerald'),
 recipe('minecraft:emerald',9,'minecraft:enchanted_book',buy_b=True,tag='{StoredEnchantments:[{id:"minecraft:mending",lvl:1s}]}'),
 recipe('minecraft:emerald',9,'minecraft:bookshelf'),recipe('minecraft:book',4,'minecraft:emerald'),
 recipe('minecraft:emerald',1,'minecraft:lantern'),recipe('minecraft:emerald',1,'minecraft:glass',4),
 recipe('minecraft:ink_sac',5,'minecraft:emerald',uses=12,max_uses=12),
 recipe('minecraft:emerald',18,'minecraft:enchanted_book',buy_b=True,tag='{StoredEnchantments:[{id:"minecraft:efficiency",lvl:4s}]}')]

call('close');call('fixture',block='minecraft:air')
command('kill @e[type=minecraft:villager]')
command('gamemode survival @a')
command('summon minecraft:villager 8.5 65 8.5 {Tags:["siu_review_merchant"],NoAI:1b,Invulnerable:1b,Silent:1b,PersistenceRequired:1b,VillagerData:{type:"minecraft:plains",profession:"minecraft:librarian",level:4},Xp:200,Offers:{Recipes:['+','.join(recipes)+']}}')
command('tp @a 8.5 65 11.5 180 8')
call('aim-entity',entity='minecraft:villager',radius=8)
call('use-entity-ray',entity='minecraft:villager',shift=False,ticks=15)
if siu:call('inventory',ticks=8)
call('move',x=639,y=449,ticks=100)
capture('overview')
select(1)
menu=state['menu'] if siu else state['menuClass']
for slot,item,count in [(0,'minecraft:emerald',9),(1,'minecraft:book',1)]:
    if siu:call('reviewed-session-seed',menu=menu,slot=slot,item=item,count=count,ticks=8)
    else:call('seed',slot=slot,item=item,count=count,ticks=8)
call('move',x=639,y=449,ticks=10);capture('two-input')
call('snapshot-server',ticks=1)
(out/'merchant-review'/'two-input-server.json').write_text((out/'server-state.json').read_text())
# Remove fixture payments before changing the selected offer.
for slot in [0,1]:
    if siu:call('reviewed-session-seed',menu=menu,slot=slot,item='minecraft:air',count=0,ticks=5)
    else:call('seed',slot=slot,item='minecraft:air',count=0,ticks=5)
if siu:
    b=next(w['bounds'] for w in state['workspaceWindows'] if w['kind']=='CONTAINER')
    for _ in range(4):call('scroll',x=b['x']+35,y=b['y']+55,amount=-1,ticks=5)
    select(2)
else:select(6)
call('move',x=639,y=449,ticks=8);capture('sold-out')
call('snapshot-server',ticks=1)
(out/'merchant-review'/'sold-out-server.json').write_text((out/'server-state.json').read_text())
(out/'merchant-review'/'fixture.json').write_text(json.dumps({'minecraft':'1.20.1','profession':'librarian','level':4,'xp':200,'recipes':recipes,'scope':'Matched visual states; fixture payments seeded, no gameplay suite'},indent=2))
print(side,'captured',flush=True)
