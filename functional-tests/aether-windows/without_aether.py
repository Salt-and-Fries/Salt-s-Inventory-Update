"""Affected generic launcher regression with the optional Aether mod absent."""
from pathlib import Path
import argparse
from character import CharacterCheck


def main():
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('--loader',required=True,choices=['fabric','forge']);a=p.parse_args()
    c=CharacterCheck(a.output);c.cmd('gamemode survival @a');c.call('wait',ticks=1)
    if not any(w['kind']=='INVENTORY'and w.get('persistentHidden')!='true'for w in c.state.get('workspaceWindows',[])):c.call('inventory',ticks=8)
    c.call('aether-character',ticks=8)
    w=c.char();before=w['bounds'].copy();c.passed('without-aether-normal-character',not w.get('aetherMode',False))
    c.capture('without-aether-normal')
    c.general();path='salts_inventory_compat:'+('curios_accessories'if a.loader=='forge'else'accessories_equipment')
    c.passed('without-aether-generic-panel-opens',c.window(path)is not None)
    c.passed('without-aether-character-unchanged',c.char()['bounds']==before and not c.char().get('aetherMode',False))
    c.capture('without-aether-general')
    # These attached panels intentionally close through their Character toggle.
    c.general();c.call('wait',ticks=8)
    c.passed('without-aether-generic-panel-closes',not any(w.get('menuId')==path for w in c.state['allWindows']))
    c.passed('without-aether-inventory-survives',c.workspace('INVENTORY')is not None and c.char()is not None)


if __name__=='__main__':main()
