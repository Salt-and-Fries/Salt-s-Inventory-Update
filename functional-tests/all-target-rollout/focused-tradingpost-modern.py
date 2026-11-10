"""Natural eight-offer Trading Post fixture; optional search and real purchase check."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--interact',action='store_true');a=p.parse_args()
run=a.run;cmd='server-command' if a.side=='siu' else 'command';checks=[]
def act(kind,**kw):return send(run,kind,**kw)
def command(text):return act(cmd,command=text,ticks=6)
def check(label,ok):
 checks.append({'check':label,'passed':bool(ok)});(run/'tradingpost-focused-checks.json').write_text(json.dumps({'scope':'Eight native offers, search, selection and one purchase only','checks':checks},indent=2))
 assert ok,label
 print('PASS',label,flush=True)
act('close',ticks=3);act('fixture',block='tradingpost:trading_post',ticks=16)
if a.side=='siu':
 act('catalog',ticks=1);x,y,z=map(int,json.loads((run/'catalog.json').read_text()).get('origin','8,65,8').split(','))
else:x,y,z=8,65,8
command('kill @e[type=minecraft:villager]');command('gamemode survival @a');command('effect clear @a');command('time set day')
trades=[('diamond',3,1),('bread',2,3),('apple',2,1),('paper',1,4),('carrot',1,4),('potato',1,4),('pumpkin',2,1),('cookie',1,4)]
recipes=','.join('{buy:{id:"minecraft:emerald",count:'+str(price)+'},sell:{id:"minecraft:'+item+'",count:'+str(amount)+'},maxUses:100,uses:0,xp:2,priceMultiplier:0.0f}' for item,price,amount in trades)
if a.minecraft=='1.20.1':recipes=recipes.replace('count:','Count:')
command(f'summon minecraft:villager {x+2} {y} {z} '+'{NoAI:1b,Invulnerable:1b,Silent:1b,PersistenceRequired:1b,VillagerData:{type:"minecraft:plains",profession:"minecraft:librarian",level:4},Xp:200,Offers:{Recipes:['+recipes+']}}')
command('item replace entity @a inventory.0 with minecraft:emerald 12')
s=act('client-use-block' if a.side=='siu' else 'open',ticks=30)['client']
if a.side=='siu' and not s.get('windows',s.get('allWindows',[])):
 s=act('client-use-block',ticks=20)['client']
if a.side=='siu':
 check('native-TradingPost-screen-hosted',any(w.get('nativeScreen','').endswith('.TradingPostScreen') for w in s.get('windows',[])))
 b=s['windows'][0]['bounds']
else:
 if not s.get('bounds'):s=act('open',ticks=20)['client']
 check('native-TradingPost-screen',s.get('screen','').endswith('.TradingPostScreen'));b=s['bounds']
capture(run,a.side,a.minecraft,a.loader,'trading-post')
if a.interact and a.side=='siu':
 act('xy-click',x=b['x']+51,y=b['y']+32,ticks=3);s=act('text',text='bread',ticks=16)
 capture(run,'siu',a.minecraft,a.loader,'trading-post-search')
 act('xy-click',x=b['x']+36,y=b['y']+56,ticks=12)
 s=act('inspect',ticks=2);slots=s['server']['slots']
 # Bread starts second. Selecting row zero can yield bread only after filtering.
 check('native-search-filters-and-selects-three-bread',slots[0]['item']=='minecraft:emerald' and slots[0]['count']>=2 and slots[2]['item']=='minecraft:bread' and slots[2]['count']==3)
 before=slots[0]['count'];s=act('xy-click',x=b['x']+226,y=b['y']+89,ticks=10)
 check('purchase-deducts-exact-two-emeralds',s['server']['slots'][0]['count']==before-2)
 check('purchase-delivers-exact-three-bread',s['server']['cursor']['item']=='minecraft:bread' and s['server']['cursor']['count']==3)
 act('store',ticks=5);capture(run,'siu',a.minecraft,a.loader,'trading-post-filtered-offer')
