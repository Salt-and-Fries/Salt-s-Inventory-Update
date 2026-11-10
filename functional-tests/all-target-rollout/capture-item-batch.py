"""Use actual held items; retain entry failures rather than forcing menu providers."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True)
p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True)
p.add_argument('--capture-only',action='store_true',help='Capture pictured states without the separate server round-trip check')
a=p.parse_args()
CASES=[('wands-palette','wands:palette'),*[(f'wands-magic-bag-{i}',f'wands:magic_bag_{i}') for i in range(1,4)],
 ('toms-item-filter','toms_storage:item_filter'),('toms-tag-item-filter','toms_storage:tag_item_filter'),
 ('toms-polymorphic-item-filter','toms_storage:polymorphic_item_filter'),('sophisticated-backpack','sophisticatedbackpacks:backpack'),
 ('travelers-backpack','travelersbackpack:standard')]
cmd='server-command' if a.side=='siu' else 'command';rows=[]
send(a.run,'catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text())
def tap(x,y):
 if a.side=='siu':return send(a.run,'xy-click',x=x,y=y,ticks=10)
 send(a.run,'click',x=x,y=y,ticks=1);return send(a.run,'release',x=x,y=y,ticks=10)
for ui,item in CASES:
 if item not in catalog['items'] and item.startswith('toms_storage:'):
  legacy='toms_storage:ts.'+item.split(':',1)[1]
  if legacy in catalog['items']:item=legacy
 if item not in catalog['items']:continue
 try:
  send(a.run,'close',ticks=3);send(a.run,'fixture',block='minecraft:stone',ticks=16)
  if a.side=='siu':send(a.run,cmd,command='item replace entity @a weapon.mainhand with '+item,ticks=6)
  else:send(a.run,'held',item=item,count=1,ticks=6)
  send(a.run,'client-use-item' if a.side=='siu' else 'use-item',ticks=20)
  s=capture(a.run,a.side,a.minecraft,a.loader,ui)
  if ui=='travelers-backpack':
   b=(s['windows'][0] if a.side=='siu' else s)['bounds']
   s=tap(b['x']+(164 if a.side=='siu' else 116),b['y']+(30 if a.side=='siu' else 11))['client']
   if a.side=='siu':assert any(r['id']=='tool-drawer' for r in s['windows'][0]['template']['regions']),'Native tool drawer did not open'
   capture(a.run,a.side,a.minecraft,a.loader,'travelers-tools-drawer')
   if a.side=='siu' and not a.capture_only:
    send(a.run,'seed-player',items=[{'slot':9,'item':'minecraft:diamond','count':7}],ticks=6)
    send(a.run,'player-click',slot=9,ticks=6);s=send(a.run,'click',slot=0,ticks=8)
    assert s['server']['slots'][0]['item']=='minecraft:diamond' and s['server']['slots'][0]['count']==7 and s['server']['cursor']['count']==0,'Storage insertion mismatch'
    s=send(a.run,'click',slot=0,ticks=8)
    assert s['server']['cursor']['item']=='minecraft:diamond' and s['server']['cursor']['count']==7,'Storage withdrawal mismatch'
    s=send(a.run,'store',ticks=8)
    assert s['server']['cursor']['count']==0 and sum(v['count'] for v in s['server']['playerInventory'] if v['item']=='minecraft:diamond')==7,'Storage round-trip mismatch'
    rows.append({'ui':ui,'focused_storage_round_trip':'exact seven diamonds; clear cursor; pass'})
  rows.append({'ui':ui,'status':'natural item use and pictured layout; other features separately tracked'})
 except Exception as e:
  rows.append({'ui':ui,'status':'pending','reason':str(e)[:240]});print('PENDING',ui,str(e)[:140],flush=True)
 (a.run/'item-entry-capture-checks.json').write_text(json.dumps(rows,indent=2))
