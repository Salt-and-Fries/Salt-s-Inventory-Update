"""Original optional WTlib requester item, charged and linked normally.

Only its real item entry and displayed interface are checked here.
"""
from pathlib import Path
import argparse,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def command(text,ticks=6):return act('command',command=text,ticks=ticks)
act('close',ticks=3);command('gamemode survival @a');command('clear @a');command('kill @e[type=!minecraft:player]')
command('fill 6 65 6 10 68 10 minecraft:air');act('fixture',block='ae2:wireless_access_point',ticks=8)
command('setblock 8 65 9 ae2:creative_energy_cell',ticks=20)
command('setblock 9 65 9 merequester:requester',ticks=30)
command('item replace entity @a weapon.mainhand with merequester:wireless_requester_terminal[ae2:stored_energy=1000000.0d,ae2:wireless_link_target={dimension:"minecraft:overworld",pos:[I;8,65,8]}]',ticks=8)
s=act('use-item',ticks=30)
if a.side=='siu':
 # Some excluded observers cannot introspect AE2's private nested screen host.
 # Require the exact real menu and the dedicated AE2 adapter in that case.
 windows=s.get('windows',[])
 assert any(w.get('menuId')=='merequester:wireless_requester_terminal' and w.get('definition','').endswith('.AEWindow') for w in windows),[(w.get('menuId'),w.get('definition')) for w in windows]
else:
 assert s.get('screen','').endswith('.ReqWirelessTerminalScreen'),s.get('screen')
act('move',x=5,y=5,ticks=3)
capture(a.run,a.side,a.minecraft,a.loader,'merequester-wireless-terminal','Original optional AE2WTlib requester terminal, with native charge/link components and a real powered requester network. Ordinary item use; displayed state only, not exhaustive request processing.')
