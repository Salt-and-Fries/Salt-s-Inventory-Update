"""Picture the native wand tool picker through its original key request."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)['client']
act('close',ticks=3);act('command',command='gamemode survival @a',ticks=3)
act('command',command='clear @a',ticks=3)
act('held',item='wands:diamond_wand',ticks=6)
for slot,item in [('hotbar.1','minecraft:diamond_pickaxe'),('inventory.0','minecraft:diamond_shovel')]:
 act('command',command=f'item replace entity @a {slot} with {item}',ticks=3)
s=act('native-wands-tools-request',ticks=20)
capture(a.run,a.side,a.minecraft,a.loader,'wands-tools','Original publisher Shift+menu key-request packet while holding its actual wand. The tool-selection inventory is retained because these cells choose tool sources. Raw OS shortcut input and tool-selection behavior are not certified by this picture.')
