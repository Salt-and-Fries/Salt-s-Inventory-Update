"""Natural entry pictures for specialized original publisher inventories."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import send,capture
p=argparse.ArgumentParser();p.add_argument('run',type=Path);p.add_argument('--side',choices=['native','siu'],required=True);p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True);p.add_argument('--only',action='append');a=p.parse_args()
def act(kind,**kw):return send(a.run,kind,**kw)['client']
def tap(x,y):
 act('click',x=x,y=y,ticks=1);return act('release',x=x,y=y,ticks=12)
def snap(ui,note='Ordinary original block/item interaction; pictured state only. Feature checks are tracked separately.'):
 return capture(a.run,a.side,a.minecraft,a.loader,ui,note)
act('catalog',ticks=1);catalog=json.loads((a.run/'catalog.json').read_text());rows=[]
blocks=[('bountiful','bountiful:bountyboard'),('minecells-crafter','minecells:cell_crafter'),
 ('minecells-unbreakable-crafter','minecells:unbreakable_cell_crafter'),
 ('brick-and-mortar-kiln','brick_and_mortar:kiln'),('expanded-delight-juicer','expandeddelight:juicer'),
 ('crabbers-delight-crab-trap','crabbersdelight:crab_trap'),('crabbers-delight-palm-cabinet','crabbersdelight:palm_cabinet'),
 ('oxidized-copper-kiln','oxidized:copper_kiln'),('seasons-chiller','seasonsextras:chiller'),('seasons-heater','seasonsextras:heater'),
 ('smitherz-smither','smitherz:smither'),('smitherz-grinder','smitherz:grinder'),('simply-swords-runic-forge','simplyswords:runic_forge')]
blocks += [('ironchests-'+name.split(':')[1].replace('_','-'),name) for name in sorted(catalog['blocks']) if name.startswith('ironchests:') and name.endswith(('_chest','_barrel'))]
materials=dict(mason_table='stone',carpenters_table='oak_planks',loom_table='white_wool',glassblower='glass',botanist_workbench='oak_leaves',alchemy_bench='amethyst_block',tinkering_table='iron_bars')
blocks += [('chipped-'+name.replace('_','-'),'chipped:'+name) for name in materials]
items=[('inmis-'+name.split(':')[1].replace('_','-'),name) for name in sorted(catalog['items']) if name.startswith('inmis:') and name.endswith('_backpack')]
for ui,entry in blocks+items:
 if a.only and not any(ui.startswith(name) for name in a.only):continue
 if entry not in catalog['blocks'] and entry not in catalog['items']:continue
 row={'ui':ui,'entry':entry};rows.append(row)
 try:
  act('close',ticks=3);act('fixture',block=entry if (ui,entry) in blocks else 'minecraft:stone',ticks=8)
  act('command',command='gamemode survival @a',ticks=3)
  if ui=='bountiful':
   fixture=json.loads((Path(__file__).parents[1]/'bountiful-window/fixture.json').read_text());act('command',command='data merge block 8 65 8 '+fixture['block_nbt'],ticks=6)
  if ui=='crabbers-delight-crab-trap':
   # The original trap refuses to open on dry land. Keep its actual water
   # requirements rather than forcing a container provider or changing config.
   act('command',command='fill 7 65 7 9 65 9 minecraft:water',ticks=4)
   act('command',command='setblock 8 65 8 crabbersdelight:crab_trap[waterlogged=true]',ticks=12)
  if entry.startswith('chipped:'):
   act('command',command='item replace entity @a inventory.0 with minecraft:'+materials[entry.split(':')[1]]+' 16',ticks=5)
  if (ui,entry) in blocks:s=act('open',ticks=20)
  else:
   act('held',item=entry,ticks=6);s=act('use-item',ticks=20)
  if entry.startswith('chipped:'):
   if a.side=='siu':
    w=s['windows'][0];b=w['bounds'];tap(b['x']+236,b['y']+32);s=tap(b['x']+131,b['y']+74)
    assert s['windows'][0]['definition'].endswith('.ChippedWindow'),'Expected dedicated Chipped workshop'
   else:
    slot=next(v for v in s['slots'] if v['item']=='minecraft:'+materials[entry.split(':')[1]]);b=s['bounds'];s=tap(b['x']+slot['x']+8,b['y']+slot['y']+8)
  s=snap(ui)
  if ui in ['minecells-crafter','minecells-unbreakable-crafter']:
   b=(s['windows'][0] if a.side=='siu' else s)['bounds'];tap(b['x']+41,b['y']+(58 if a.side=='siu' else 36));snap(ui+'-recipe-picker' if ui!='minecells-crafter' else 'minecells-recipe-picker')
  row['status']='ordinary-entry-and-picture-pass'
 except Exception as error:
  row.update(status='pending',reason=str(error)[:350]);print('PENDING',ui,str(error)[:150],flush=True)
  if isinstance(error,TimeoutError):raise
 (a.run/'specialized-batch-captures.json').write_text(json.dumps(rows,indent=2));print(ui,row['status'],flush=True)
