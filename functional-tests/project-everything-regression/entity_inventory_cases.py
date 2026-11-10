"""Ordinary, publisher-pinned entity inventory entry fixtures."""
from pathlib import Path
import hashlib

SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()


def cases(mc,loader,installed,items,menus,origin,gaps,hashes,entities=None):
    from nifty_inventory_cases import cases as nifty_cases
    result=nifty_cases(mc,loader,installed,items,menus,origin,gaps,hashes,entities)
    from gear_inventory_cases import cases as gear_cases
    result+=gear_cases(mc,loader,installed,items,menus,origin,gaps,hashes,entities)
    if ('guardvillagers' not in installed or 'guardvillagers:guard_spawn_egg' not in items
            or not any(v.startswith('guardvillagers:') for v in menus)
            or entities is not None and 'guardvillagers:guard' not in entities):
        return result
    gap=next((v for v in gaps if (v['minecraft'],v['loader'])==(mc,loader)
              and v['ui']=='guard-villager' and hashes.intersection(v['publisher_sha256'])),None)
    if gap is None:return result
    x,y,z=origin
    movement='minecraft:generic.movement_speed' if mc in {'1.20.1','1.21.1'} else 'minecraft:movement_speed'
    commands=[
        'gamemode survival @a','difficulty normal','time set day',
        'effect give @a minecraft:hero_of_the_village 600 0 true',
        f'summon guardvillagers:guard {x+.5} {y} {z+.5} '+
        '{NoAI:0b,NoGravity:1b,Invulnerable:1b,PersistenceRequired:1b}',
        f'attribute @e[type=guardvillagers:guard,limit=1] {movement} base set 0',
        f'tp @a {x+.5} {y} {z+3.5} 180 5',
        'item replace entity @a weapon.mainhand with minecraft:air']
    return result+[{'id':'guard-villager','mod_id':'guardvillagers',
             'source':'reviewed-native-entity-entry',
             'publisher_sha256':gap['publisher_sha256'],
             'expected_native_screen':gap['screen'],
             'entry_fixture_helper_sha256':SOURCE_SHA256,
             'actions':[{'type':'fixture','block':'minecraft:stone','ticks':8},
                        *[{'type':'command','command':v,'ticks':12 if v.startswith('summon ') else 4} for v in commands],
                        {'type':'sneak','down':False,'ticks':4},
                        {'type':'client-use-entity','entity':'guardvillagers:guard','shift':False,'ticks':20}]}]
