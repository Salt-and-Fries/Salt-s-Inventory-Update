"""AE2 19 actual saved components, powered network and native crafting navigation."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--loader',required=True);p.add_argument('--minecraft',default='1.21.1');a=p.parse_args();state={}
def act(kind,**kw):
 global state
 state=send(a.run,kind,**kw)['client'];return state
def command(text,ticks=6):return act('command',command=text,ticks=ticks)
def menu():return state['windows'][0]['menu'] if a.side=='siu' else state.get('menuClass','')
def picture(ui):
 act('move',x=5,y=5,ticks=3)
 return capture(a.run,a.side,a.minecraft,a.loader,'ae2-'+ui,'Installed AE2 native component format, real powered storage network, oak-log pattern, assembler and CPU. Original terminal controls choose the crafting screens. Focused navigation only, not a complete autocrafting suite.')
def tap(x,y):
 act('click',x=x,y=y,ticks=1);return act('release',x=x,y=y,ticks=24)
act('close',ticks=3);command('fill 6 65 5 11 68 10 minecraft:air',ticks=3)
act('fixture',block='ae2:cable_bus',ticks=10);command('gamemode creative @a');command('kill @e[type=!minecraft:player]');command('clear @a')
command('setblock 8 65 8 ae2:cable_bus{cable:{id:"ae2:fluix_glass_cable"},south:{id:"ae2:terminal",enabledKeyTypes:["ae2:i","ae2:f"]}}')
command('setblock 7 65 8 ae2:creative_energy_cell')
cell='{id:"ae2:item_storage_cell_64k",count:1,components:{"ae2:storage_cell_inv":[{"#t":"ae2:i",id:"minecraft:oak_log","#":128L},{"#t":"ae2:i",id:"minecraft:iron_ingot","#":128L}]}}'
# Drive's AEBaseInvBlockEntity retains its keyed item0 compound. Pattern
# Provider uses AppEngInternalInventory's list; these are distinct native codecs.
command('setblock 8 65 7 ae2:drive{inv:{item0:'+cell+'}}')
pattern='{Slot:0,id:"ae2:crafting_pattern",count:1,components:{"ae2:encoded_crafting_pattern":{inputs:[{id:"minecraft:oak_log",count:1},{},{},{},{},{},{},{},{}],result:{id:"minecraft:oak_planks",count:4},recipeId:"minecraft:oak_planks",canSubstitute:0b,canSubstituteFluids:0b}}}'
command('setblock 9 65 8 ae2:pattern_provider{patterns:['+pattern+']}')
command('setblock 9 66 8 ae2:molecular_assembler')
command('setblock 8 64 8 ae2:64k_crafting_storage',ticks=60)
command('tp @a 8.5 65 11.5 180 24',ticks=12);act('use-block-ray',ticks=24);picture('terminal-populated')
b=(state['windows'][0] if a.side=='siu' else state)['bounds']
tap(b['x']+52,b['y']+(38 if a.side=='siu' else 26))
assert menu().endswith('.CraftAmountMenu'),'Native crafting amount did not open: '+menu()
picture('craft-amount')
buttons=state['windows'][0]['nativeWidgets'] if a.side=='siu' else state['widgets']
button=next(w for w in buttons if w.get('visible') and w.get('message')=='Next');tap(button['x']+button['width']/2,button['y']+button['height']/2)
act('wait',ticks=60)
assert menu().endswith('.CraftConfirmMenu'),'Native calculated confirmation did not open: '+menu()
picture('craft-confirm');print('PASS native component-backed craft amount and confirmation navigation',flush=True)
