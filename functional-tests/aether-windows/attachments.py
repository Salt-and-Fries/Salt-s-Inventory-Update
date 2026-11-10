"""Focused coexistence and independent parent movement on the live workspace."""
from pathlib import Path
import argparse
from focused import Check


def move(c,kind,x,y):
    window=c.workspace(kind)
    # Raise the parent from an exposed border before clicking its controls;
    # another independently attached panel can cover its title after a drag.
    b=window['bounds'];others=[w['bounds']for w in c.state['workspaceWindows']if w['instance']!=window['instance']]
    points=[(b['x']+dx,b['y']+dy)for dx,dy in [(2,2),(b['width']-2,2),(2,b['height']-2),(b['width']-2,b['height']-2)]]
    px,py=next((x,y)for x,y in points if not any(o['x']<=x<o['x']+o['width']and o['y']<=y<o['y']+o['height']for o in others))
    c.tap(dict(x=px,y=py))
    window=c.workspace(kind)
    was_locked=window.get('locked')=='true'
    if was_locked:c.tap(window['controls']['LOCK'])
    b=c.workspace(kind)['bounds'];px=b['x']+12;py=b['y']+9
    c.call('click',x=px,y=py,button=0,ticks=1)
    c.call('drag',x=px+x-b['x'],y=py+y-b['y'],dx=x-b['x'],dy=y-b['y'],button=0,ticks=1)
    c.call('release',x=px+x-b['x'],y=py+y-b['y'],button=0,ticks=3)
    if was_locked:c.tap(c.workspace(kind)['controls']['LOCK'])


def main():
    p=argparse.ArgumentParser();p.add_argument('output',type=Path);p.add_argument('--loader',choices=['fabric','forge'],required=True);a=p.parse_args();c=Check(a.output)
    c.call('wait',ticks=1)
    if not any(w['kind']=='INVENTORY'for w in c.state.get('workspaceWindows',[])):c.call('inventory')
    c.call('aether-character');c.call('wait',ticks=8)
    if not any(w.get('menuId')=='salts_inventory_compat:aether_equipment'for w in c.state['allWindows']):c.cloud()
    native='salts_inventory_compat:'+('curios_accessories' if a.loader=='forge' else 'accessories_equipment')
    # Existing Curios/Accessories launcher, in the top of the player preview.
    char=c.workspace('CHARACTER')['content'];c.tap(dict(x=char['x']+32,y=char['y']+14))
    c.passed('native-wearable-launcher-replaces-aether',any(w.get('menuId')==native for w in c.state['allWindows']) and not any(w.get('menuId')=='salts_inventory_compat:aether_equipment'for w in c.state['allWindows']))
    c.cloud();c.passed('cloud-replaces-other-wearable-panel',any(w.get('menuId')=='salts_inventory_compat:aether_equipment'for w in c.state['allWindows']) and not any(w.get('menuId')==native for w in c.state['allWindows']))
    eq=c.window('salts_inventory_compat:aether_equipment')['bounds'].copy();char=c.workspace('CHARACTER')['bounds'].copy();inv=c.workspace('INVENTORY')['bounds'].copy()
    # Move well beyond SIU's edge snap zone, including on repeat runs.
    move(c,'CHARACTER',char['x']+(100 if char['x']<300 else -100),char['y']-30 if char['y']>240 else char['y']+30);eq2=c.window('salts_inventory_compat:aether_equipment')['bounds'];char2=c.workspace('CHARACTER')['bounds']
    c.passed('equipment-moves-with-character',eq2['x']-eq['x']==char2['x']-char['x'] and eq2['y']-eq['y']==char2['y']-char['y'] and char2!=char)
    c.passed('character-move-keeps-inventory-independent',c.workspace('INVENTORY')['bounds']==inv)
    eq=eq2.copy();bag=next((w['bounds'].copy() for w in c.state['allWindows']if w.get('menuId')=='salts_inventory_compat:quark_backpack'),None)
    move(c,'INVENTORY',440 if abs(inv['x']-440)>30 else 360,330 if abs(inv['y']-330)>25 else 290);inv2=c.workspace('INVENTORY')['bounds']
    c.passed('inventory-move-keeps-equipment-independent',c.window('salts_inventory_compat:aether_equipment')['bounds']==eq)
    if bag:
        bag2=c.window('salts_inventory_compat:quark_backpack')['bounds'];c.passed('backpack-moves-with-inventory',bag2['x']-bag['x']==inv2['x']-inv['x'] and bag2['y']-bag['y']==inv2['y']-inv['y'] and bag2!=bag)
    c.capture('aether-attachments-arranged')


if __name__=='__main__':main()
