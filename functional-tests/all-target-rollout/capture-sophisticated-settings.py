"""Reach each native storage settings container through its visible gear tab."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);a=p.parse_args();rows=[]
def act(kind,**kw):return send(a.run,kind,**kw)['client']
act('catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text())
for name,item in [('backpack','sophisticatedbackpacks:backpack'),('storage','sophisticatedstorage:chest'),('limited-barrel','sophisticatedstorage:limited_barrel_1')]:
 if item not in catalog['items']:continue
 row={'ui':'sophisticated-'+name+'-settings'};rows.append(row)
 try:
  act('close',ticks=2);act('command',command='difficulty peaceful',ticks=2)
  act('command',command='gamemode survival @a',ticks=2)
  act('held',item='minecraft:air',ticks=2)
  act('command',command='setblock 8 66 8 minecraft:air',ticks=2) # Clear the prior tall fossil fixture so the real chest lid can open.
  act('fixture',block='minecraft:stone' if name=='backpack' else item,ticks=10)
  if name=='backpack':act('held',item=item,ticks=5);s=act('use-item',ticks=15)
  else:s=act('client-use-block' if a.side=='siu' else 'open',ticks=15)
  b=(s['windows'][0] if a.side=='siu' else s)['bounds']
  x=b['x']+b['width']+10;y=b['y']+(28 if a.side=='siu' else 15)
  act('click',x=x,y=y,ticks=1);s=act('release',x=x,y=y,ticks=20)
  screens=[w.get('nativeScreen','') for w in s.get('windows',[])] if a.side=='siu' else [s.get('screen','')]
  assert any('SettingsScreen' in screen for screen in screens),screens
  capture(a.run,a.side,a.minecraft,a.loader,row['ui'],'Original gear tab opens the original settings container. Pictured settings layout and normal navigation only; upgrade behavior has separate coverage.')
  row['status']='original-gear-navigation-and-picture-pass'
 except Exception as error:
  row.update(status='pending',reason=str(error)[:280]);print('PENDING',name,str(error)[:120],flush=True)
 (a.run/'sophisticated-settings-entry-checks.json').write_text(json.dumps(rows,indent=2))
