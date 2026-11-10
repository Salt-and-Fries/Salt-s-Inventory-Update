"""A real AE2 15 network/pattern fixture and ordinary terminal crafting requests."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--loader',required=True);a=p.parse_args()
state={};cmd='server-command' if a.side=='siu' else 'command'
def act(kind,**kw):
 global state
 state=send(a.run,kind,**kw)['client'];return state
def command(text,ticks=6):return act(cmd,command=text,ticks=ticks)
def picture(ui):return capture(a.run,a.side,'1.20.1',a.loader,'ae2-'+ui,'A real powered storage network with a native saved oak-planks crafting pattern, assembler and CPU. Ordinary terminal clicks choose native request/calculation screens. Pictured states only; this is not a full crafting suite.')
act('close',ticks=3);command('fill 6 65 5 11 68 10 minecraft:air',ticks=3)
act('fixture',block='ae2:cable_bus',ticks=10);command('gamemode creative @a');command('kill @e[type=!minecraft:player]')
command('setblock 8 65 8 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"},south:{id:"ae2:terminal"}}')
command('setblock 7 65 8 ae2:creative_energy_cell')
command('setblock 8 65 7 ae2:drive{inv:{item0:{id:"ae2:item_storage_cell_64k",Count:1b,tag:{keys:[{"#c":"ae2:i",id:"minecraft:oak_log"},{"#c":"ae2:i",id:"minecraft:iron_ingot"}],amts:[L;128L,128L],ic:256L}}}}')
command('setblock 9 65 8 ae2:pattern_provider{patterns:[{Slot:0,id:"ae2:crafting_pattern",Count:1b,tag:{in:[{id:"minecraft:oak_log",Count:1b},{},{},{},{},{},{},{},{}],out:{id:"minecraft:oak_planks",Count:4b},recipe:"minecraft:oak_planks",substitute:0b,substituteFluids:0b}}]}')
command('setblock 9 66 8 ae2:molecular_assembler')
command('setblock 8 64 8 ae2:64k_crafting_storage',ticks=50)
command('tp @a 8.5 65 11.5 180 24',ticks=12)
act('use-block-ray',ticks=20)
picture('terminal-populated')
b=(state['windows'][0] if a.side=='siu' else state)['bounds']
act('click',x=b['x']+52,y=b['y']+(38 if a.side=='siu' else 26),button=0,ticks=1)
act('release',x=b['x']+52,y=b['y']+(38 if a.side=='siu' else 26),button=0,ticks=20)
kind=(state['windows'][0]['menu'] if a.side=='siu' else state.get('menuClass',''))
assert kind.endswith('.CraftAmountMenu'),'Native crafting amount navigation did not open: '+kind
picture('craft-amount')
buttons=state['windows'][0]['nativeWidgets'] if a.side=='siu' else state['widgets']
button=next(w for w in buttons if w.get('visible') and w.get('message')=='Next')
x=button['x']+button['width']/2;y=button['y']+button['height']/2
act('click',x=x,y=y,ticks=1);act('release',x=x,y=y,ticks=40)
kind=(state['windows'][0]['menu'] if a.side=='siu' else state.get('menuClass',''))
assert kind.endswith('.CraftConfirmMenu'),'Native calculation navigation did not open: '+kind
picture('craft-confirm')
print('PASS native craft-amount and calculated craft-confirm navigation',flush=True)
