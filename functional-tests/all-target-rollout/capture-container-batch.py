"""Focused natural block-entry capture. Does not run a gameplay suite or force providers."""
from pathlib import Path
import argparse,json,sys
sys.path.insert(0,str(Path(__file__).parent))
from evidence import capture,send

CASES=[
 ('easy-anvils','minecraft:anvil'),('easy-magic','minecraft:enchanting_table'),
 ('visual-workbench','minecraft:crafting_table'),
 ('feeding-trough','animal_feeding_trough:feeding_trough'),
 ('bartering-station','barteringstation:bartering_station'),
 ('archeology-table','betterarcheology:archeology_table'),
 ('betterarcheology-fossil-inventory','betterarcheology:villager_fossil'),
 ('deep-aether-combiner','deep_aether:combiner'),
 ('nether-chest','netherchested:nether_chest'),('graveyard-ossuary','graveyard:ossuary'),
 ('villagersplus-alchemist','villagersplus:alchemist_table'),
 ('villagersplus-ore-grinder','villagersplus:ore_grinder'),
 ('toms-filing-cabinet','toms_storage:filing_cabinet'),
 ('toms-inventory-link','toms_storage:inventory_cable_connector'),
 ('toms-level-emitter','toms_storage:level_emitter'),
 ('toms-connector-filter','toms_storage:inventory_cable_connector_filtered'),
 ('toms-storage-terminal','toms_storage:storage_terminal'),
 ('toms-crafting-terminal','toms_storage:crafting_terminal'),
 ('toms-config','toms_trading_network:vending_machine'),
 ('spell-binding','spell_engine:spell_binding'),
 ('sophisticated-chest','sophisticatedstorage:chest'),
 ('sophisticated-barrel','sophisticatedstorage:barrel'),
 *[(f'sophisticated-limited-barrel-{i}',f'sophisticatedstorage:limited_barrel_{i}') for i in range(1,5)],
 ('sophisticated-decoration','sophisticatedstorage:decoration_table'),
 ('supplementaries-lunch-basket','supplementaries:lunch_basket'),
 ('supplementaries-sack','supplementaries:sack'),
 ('spawn-pigment-shifter','spawn:pigment_shifter'),
 ('travelers-block','travelersbackpack:standard'),
 ('farmers-cooking-pot','farmersdelight:cooking_pot'),
 ('farmers-basket','farmersdelight:basket'),
 ('transmog','transmog:transmogrification_table'),('rune-crafting','runes:crafting_altar'),
 ('universal-sawmill','sawmill:sawmill'),('illager-imbuing','illagerinvasion:imbuing_table'),
 ('alloy-forge','alloy-forgery:bricks_forge_controller'),
 ('alloy-forge','alloy_forgery:bricks_forge_controller'),
 ('alloyed-forge','alloyed:forge'),('alloyed-oven','alloyed:oven'),
 ('waystones-warp-plate','waystones:warp_plate'),
 ('betterend-smelter','betterend:end_stone_smelter'),
 ('betterend-forge','betterend:end_forge'),
]

def main():
 p=argparse.ArgumentParser();p.add_argument('--minecraft',required=True);p.add_argument('--loader',required=True)
 p.add_argument('--siu',type=Path,required=True);p.add_argument('--native',type=Path,required=True)
 p.add_argument('--only',action='append');p.add_argument('--side',choices=['native','siu']);a=p.parse_args();results=[]
 # Independent original/SIU drivers can run together. Keep their journals
 # separate rather than letting the last writer erase the other side.
 out=(a.siu if a.side!='native' else a.native)/('block-entry-capture-checks'+('-'+a.side if a.side else '')+'.json')
 for run,side in [(a.native,'native'),(a.siu,'siu')]:
  if a.side and a.side!=side:continue
  send(run,'catalog',ticks=1);catalog=json.loads((run/'catalog.json').read_text());blocks=set(catalog['blocks'])
  cmd='server-command' if side=='siu' else 'command'
  send(run,cmd,command='time set day',ticks=2)
  for ui,block in CASES:
   if a.only and ui not in a.only:continue
   replacements={'easy-anvils':'easyanvils:minecraft/anvil','easy-magic':'easymagic:minecraft/enchanting_table'}
   if replacements.get(ui) in blocks:block=replacements[ui]
   if block not in blocks and block.startswith('toms_storage:'):
    legacy='toms_storage:ts.'+block.split(':',1)[1]
    if legacy in blocks:block=legacy
   if block not in blocks:continue
   try:
    send(run,'close',ticks=3)
    if ui=='travelers-block':
     # Native placement initializes the backpack wrapper. A bare /setblock
     # creates an uninitialized native block, which is not a valid UI fixture.
     send(run,'fixture',block='minecraft:stone',ticks=16)
     x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
     if side=='siu':send(run,cmd,command='item replace entity @a weapon.mainhand with travelersbackpack:standard',ticks=5)
     else:send(run,'held',item='travelersbackpack:standard',ticks=5)
     send(run,'sneak',down=True,ticks=16)
     for _ in range(2):
      send(run,'client-use-block' if side=='siu' else 'open',shift=True,ticks=16)
     send(run,'sneak',down=False,ticks=8)
     send(run,cmd,command=f'clone {x} {y+1} {z} {x} {y+1} {z} {x} {y} {z} replace move',ticks=8)
     send(run,cmd,command='item replace entity @a weapon.mainhand with minecraft:air',ticks=5)
    else:
     send(run,'fixture',block=block,ticks=16)
     if ui=='spawn-pigment-shifter':
      x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
      send(run,cmd,command=f'setblock {x} {y} {z} spawn:pigment_shifter[waterlogged=true]',ticks=5)
     if ui=='alloy-forge':
      x,y,z=map(int,catalog.get('origin','8,65,8').split(','))
      send(run,cmd,command=f'fill {x-1} {y-1} {z} {x+1} {y+1} {z+2} minecraft:bricks',ticks=5)
      send(run,cmd,command=f'fill {x} {y} {z+1} {x} {y+1} {z+1} minecraft:air',ticks=5)
      send(run,cmd,command=f'setblock {x} {y} {z} {block}[facing=north]',ticks=16)
    entry='client-use-block' if side=='siu' else 'open'
    state=send(run,entry,ticks=16)['client']
    # Some block entities finish native client initialization after placement.
    # Retry the same ordinary interaction; never substitute a menu provider.
    if (side=='native' and not state.get('menuClass')) or (side=='siu' and not state.get('windows')):
        state=send(run,entry,ticks=20)['client']
    if ui=='waystones-warp-plate' and ((side=='native' and not state.get('menuClass')) or (side=='siu' and not state.get('windows'))):
        send(run,'sneak',down=True,ticks=4)
        state=send(run,entry,shift=True,ticks=20)['client']
        send(run,'sneak',down=False,ticks=3)
    if side=='siu':
     windows=state.get('windows',state.get('allWindows',[]))
     assert windows,'No SIU container opened'
     assert all(not w.get('layoutError') for w in windows),'Native layout callback failed'
    else:
     assert state.get('menuClass') and len(state.get('slots',[]))>0,'No native inventory container opened'
    note='Normal client block interaction; pictured layout only. Feature checks are recorded separately.'
    if ui=='easy-anvils' and side=='native' and (run/'native-easyanvils-reference-setting.json').exists():
        note="Original reference uses Easy Anvils' formatting option disabled: its default 21.11.1 guide crashes without SIU. SIU retains formatting through a narrow compatibility fix; see the appendix."
    state=capture(run,side,a.minecraft,a.loader,ui,note)
    results.append({'ui':ui,'block':block,'side':side,'entry_and_capture':'pass',
                    'scope':'Natural block opening and pictured layout only; feature interactions tracked separately'})
   except Exception as e:
    results.append({'ui':ui,'block':block,'side':side,'entry_and_capture':'pending','reason':str(e)[:350],
                    'observed_client':{k:v for k,v in locals().get('state',{}).items() if k in ['screen','menuId','menuClass','bounds','worldLoaded']}})
    print('PENDING',side,ui,str(e)[:90],flush=True)
    connection=locals().get('state',{})
    if not isinstance(e,TimeoutError):
     try:connection=send(run,'wait',ticks=1)['client']
     except Exception:pass
    if isinstance(e,TimeoutError) or connection.get('worldLoaded') is False or connection.get('screen','').endswith('DisconnectedScreen'):
        out.write_text(json.dumps(results,indent=2))
        raise SystemExit('Capture stopped: the client timed out or left its disposable world. Remaining entries are not attempted.')
   out.write_text(json.dumps(results,indent=2))

if __name__=='__main__':main()
