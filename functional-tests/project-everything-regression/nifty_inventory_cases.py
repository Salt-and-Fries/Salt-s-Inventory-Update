"""Publisher-pinned cart inventories entered by the native entity-use packet."""
from pathlib import Path
import hashlib

SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()

def cases(mc,loader,installed,items,menus,origin,gaps,hashes,entities=None):
    if 'niftycarts' not in installed:return []
    x,y,z=origin;result=[]
    for kind in ['plow','seed_drill']:
        entity='niftycarts:'+kind;ui='niftycarts-'+kind.replace('_','-')
        if entities is not None and entity not in entities:continue
        gap=next((row for row in gaps if (row['minecraft'],row['loader'],row['ui'])==(mc,loader,ui)
                  and hashes.intersection(row['publisher_sha256'])),None)
        if gap is None:continue
        commands=['gamemode survival @a',
                  f'summon {entity} {x+.5} {y} {z+.5} '+'{NoGravity:1b,Invulnerable:1b}',
                  f'tp @a {x+.5} {y} {z+2.5} 180 8',
                  'item replace entity @a weapon.mainhand with minecraft:air']
        result.append(dict(id=ui,mod_id='niftycarts',source='reviewed-native-entity-entry',
            publisher_sha256=gap['publisher_sha256'],expected_native_screen=gap['screen'],
            expected_native_menu='net.jmb19905.niftycarts.container.'+('PlowMenu' if kind=='plow' else 'SeedDrillMenu'),
            entry_fixture_helper_sha256=SOURCE_SHA256,
            actions=[dict(type='fixture',block='minecraft:air',ticks=8),
                     *[dict(type='command',command=command,ticks=12 if command.startswith('summon ') else 4) for command in commands],
                     dict(type='sneak',down=True,ticks=4),
                     dict(type='client-use-entity',entity=entity,shift=True,ticks=20),dict(type='sneak',down=False,ticks=3)]))
    return result
