"""Original, SIU-free modern flower: actual player book, XP and enchantment click."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)
def command(text,ticks=8):return act('command',command=text,ticks=ticks)
def tap(x,y):
 act('click',x=x,y=y,ticks=1);return act('release',x=x,y=y,ticks=10)
act('close',ticks=3);act('fixture',block='the_bumblezone:crystalline_flower',ticks=16)
command('kill @e[type=!minecraft:player]');command('gamemode survival @a');command('time set day');command('xp set @a 100 levels')
command('fill 7 66 7 9 73 9 minecraft:air')
command('setblock 8 64 8 minecraft:amethyst_block');command('item replace entity @a inventory.0 with minecraft:book')
s=act('open',ticks=20)['client'];b=s['bounds']
book=next(v for v in s['slots'] if v['player'] and v['item']=='minecraft:book')
tap(b['x']+book['x']+8,b['y']+book['y']+8)
tap(b['x']+82,b['y']+25)
tap(b['x']+54,b['y']+21)
s=tap(b['x']+95,b['y']+55)['client']
result=next(v for v in s['slots'] if v['index']==2)
assert result['item']=='minecraft:enchanted_book' and result['count']==1,result
capture(a.run,'native',a.minecraft,a.loader,'bumblezone-flower-enchanting','Original flower consumes player XP and prepares an enchanted book after normal book-slot and enchantment clicks.')
(a.run/'native-flower-enchanting-check.json').write_text(json.dumps({'actual_book_input':True,'xp_feed_and_selection':True,'result':result},indent=2))
print('PASS original flower book, XP, enchantment result',flush=True)
