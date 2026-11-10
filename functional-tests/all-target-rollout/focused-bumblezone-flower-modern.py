"""Focused original flower book, XP, native selection packet and result withdrawal."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
checks=[];s={}
def act(kind,**kw):
 global s
 r=send(a.run,kind,**kw);s=r['client'];return r
def command(text,ticks=6):return act('server-command',command=text,ticks=ticks)
def win():return next(w for w in s['windows'] if w.get('nativeScreen','').endswith('.CrystallineFlowerScreen'))
def check(label,ok):
 checks.append({'check':label,'passed':bool(ok)});(a.run/'bumblezone-flower-focused-checks.json').write_text(json.dumps({'scope':'Book input, XP feed, native enchantment selection and result withdrawal only','checks':checks},indent=2))
 assert ok,label
 print('PASS',label,flush=True)
def native(x,y):
 w=win();b=w['bounds'];layout=w['template']
 r=next(r for r in layout['regions'] if r['source']['x']<=x<r['source']['x']+r['source']['width'] and r['source']['y']<=y<r['source']['y']+r['source']['height'])
 return act('xy-click',x=b['x']+7+layout['padding']+r['x']+x-r['source']['x'],y=b['y']+22+layout['padding']+r['y']+y-r['source']['y'],ticks=12)
def owned_slot(index):
 cell=next(cell for cell in win()['slots'] if cell['index']==index)
 return native(cell['x']+8,cell['y']+8)
act('close',ticks=3);act('fixture',block='the_bumblezone:crystalline_flower',ticks=16);act('catalog',ticks=1)
x,y,z=map(int,json.loads((a.run/'catalog.json').read_text())['origin'].split(','))
command('kill @e[type=!minecraft:player]');command(f'tp @a {x+.5} {y} {z+3.5} 180 0')
command(f'setblock {x} {y-1} {z} minecraft:amethyst_block');command('gamemode survival @a');command('xp set @a 100 levels');command('time set day')
act('client-use-block',ticks=20)
check('integrated-flower-template',any(r['id']=='consume' for r in win()['template']['regions']))
act('seed-player',items=[{'slot':9,'item':'minecraft:book','count':1}],ticks=6);act('player-click',slot=9,ticks=6)
r=owned_slot(1);check('original-book-input',r['server']['slots'][1]['item']=='minecraft:book')
native(54,21);r=native(95,55)
check('native-enchantment-selection-produces-result',r['server']['slots'][2]['item']=='minecraft:enchanted_book')
capture(a.run,'siu',a.minecraft,a.loader,'bumblezone-flower-enchanting','Normal block entry; original book input, XP feeding and native enchantment-selection result verified.')
r=owned_slot(2);check('native-result-withdrawal',r['server']['cursor']['item']=='minecraft:enchanted_book' and r['server']['cursor']['count']==1)
r=act('store',ticks=8);check('separate-inventory-receives-enchanted-book',r['server']['cursor']['count']==0 and sum(i['count'] for i in r['server']['playerInventory'] if i['item']=='minecraft:enchanted_book')==1)
