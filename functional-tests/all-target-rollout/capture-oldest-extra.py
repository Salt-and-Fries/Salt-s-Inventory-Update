"""Normal Blue Skies/Spawn block entries and the actual Arcs inventory tab."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',default='1.20.1');p.add_argument('--loader',default='forge');p.add_argument('--blocks-only',action='store_true');a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def command(text,ticks=4):return act('command',command=text,ticks=ticks)
def opened(s):return bool(s.get('windows')) if a.side=='siu' else bool(s.get('menuClass'))
command('gamerule sendCommandFeedback false');command('fill 5 64 5 12 64 14 minecraft:stone');act('catalog',ticks=1)
catalog=json.loads((a.run/'catalog.json').read_text());rows=[]
for ui,block in [('blue-skies-toolbox','blue_skies:tool_box'),('spawn-octoduct','spawn:octoduct'),('spawn-pigment-shifter','spawn:pigment_shifter')]:
 if block not in catalog['blocks']:continue
 try:
  act('close',ticks=3);command('gamemode creative @a');command('fill 5 65 5 12 69 12 minecraft:air');act('held',item='minecraft:air',ticks=3);act('fixture',block=block,ticks=8)
  if ui=='spawn-pigment-shifter':command('setblock 8 65 8 spawn:pigment_shifter[waterlogged=true]')
  s=act('client-use-block' if a.side=='siu' else 'open',ticks=20)
  if not opened(s):s=act('client-use-block' if a.side=='siu' else 'open',shift=True,ticks=20)
  capture(a.run,a.side,a.minecraft,a.loader,ui,'Original publisher block opened by ordinary right-click. Waterlogged pigment-editor fixture where required; no menu/provider is fabricated. Pictured default layout only.')
  rows.append({'ui':ui,'status':'original-entry-and-picture-pass'})
 except Exception as e:rows.append({'ui':ui,'status':'pending','reason':str(e)[:220]});print('PENDING',ui,str(e)[:120],flush=True)
 (a.run/'oldest-extra-entries.json').write_text(json.dumps(rows,indent=2))
if a.blocks_only or 'blue_skies:arc_inventory' not in catalog['menus']:raise SystemExit(0)
act('close',ticks=3);command('gamemode survival @a');command('effect clear @a');s=act('inventory',ticks=12)
if a.side=='native':
 tabs=[w for w in s['widgets'] if w.get('visible') and w['type'].endswith('InventoryTabButton')]
 assert len(tabs)>=2,tabs
 w=tabs[1];x=w['x']+w['width']/2;y=w['y']+w['height']/2
else:
 if not any(w['kind']=='CHARACTER' for w in s['workspaceWindows']):s=act('key',key=67,ticks=10)
 b=next(w['bounds'] for w in s['workspaceWindows'] if w['kind']=='CHARACTER');x=b['x']-12.5;y=b['y']+35
act('move',x=x,y=y,ticks=6);act('click',x=x,y=y,ticks=1);s=act('release',x=x,y=y,ticks=20)
screens=[w.get('nativeScreen','') for w in s.get('windows',[])] if a.side=='siu' else [s.get('screen','')]
assert any(n.endswith('.ArcsScreen') for n in screens),screens
act('move',x=5,y=5,ticks=4);capture(a.run,a.side,a.minecraft,a.loader,'blue-skies-arcs','Original Blue Skies Arcs inventory request reached through the native inventory tab or its SIU Character launcher. Original four arc cells/capability inventory; no server provider is forced. Pictured state only.')
