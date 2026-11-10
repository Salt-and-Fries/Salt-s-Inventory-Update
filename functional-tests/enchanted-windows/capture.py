"""Arrange five real Enchanted windows with the separate SIU inventory."""
from pathlib import Path
import sys
import math
from focused import Check

modern='1.21.1' in str(sys.argv[1])
if modern:
    from modern import ModernCheck
c=(ModernCheck if modern else Check)(Path(sys.argv[1]))


def place_window(info, x, y):
    b=info['bounds'];controls=info['controls']
    if info['locked']=='true':
        if 'LOCK' in controls:c.tap(controls['LOCK'])
        else:
            at=controls['ELLIPSIS'];c.tap(at);c.tap(dict(x=at['x']-14,y=at['y']+17))
    x0,y0=b['x']+20,b['y']+7;dx,dy=x-b['x'],y-b['y']
    c.call('click',x=x0,y=y0,button=0,ticks=1)
    c.call('drag',x=x0+dx,y=y0+dy,dx=dx,dy=dy,button=0,ticks=1)
    c.call('release',x=x0+dx,y=y0+dy,button=0,ticks=2)


def place(menu,x,y):
    w=c.window(menu)
    place_window(next(v for v in c.state['workspaceWindows'] if v['kind']=='CONTAINER' and v['bounds']==w['bounds']),x,y)


def open_at(block_x,block_z):
    dx,dz=block_x+.5-7.5,block_z+.5-11.5
    yaw=math.degrees(math.atan2(-dx,dz));pitch=math.degrees(math.atan2(1.12,math.hypot(dx,dz)))
    c.cmd(f'tp @a 7.5 65 11.5 {yaw} {pitch}')
    c.call('use-block-ray',ticks=12)


c.call('close');c.cmd('kill @e[type=minecraft:item]')
c.cmd('gamemode survival @a');c.cmd('clear @a')
for x in [6,8,10]:c.cmd(f'setblock {x} 65 9 minecraft:air')
# The formed altar from the focused test is at x=3..5,z=7..8.
open_at(4,8);place('altar',24,22)

# Open the existing distillery before placing the machine immediately in front.
open_at(8,8);place('distillery',190,135)
if not any(w['kind']=='INVENTORY' for w in c.state['workspaceWindows']):c.call('inventory',ticks=4)
place_window(next(w for w in c.state['workspaceWindows'] if w['kind']=='INVENTORY'),360,140)

c.cmd('setblock 6 65 9 enchanted:witch_oven')
if not modern:c.cmd('data merge block 6 65 9 {inventory:{Items:[{Slot:0b,id:"minecraft:oak_log",Count:4b},{Slot:1b,id:"enchanted:clay_jar",Count:8b},{Slot:2b,id:"minecraft:coal",Count:2b}]}}')
open_at(6,9);place('witch_oven',204,22)
if modern:
    for slot,item,count in [(0,'minecraft:oak_log',4),(1,'enchanted:clay_jar',8),(2,'minecraft:coal',2)]:c.insert('witch_oven',slot,item,count)

c.cmd('setblock 8 65 9 enchanted:spinning_wheel')
if not modern:c.cmd('data merge block 8 65 9 {inventory:{Items:[{Slot:0b,id:"enchanted:poppet",Count:2b},{Slot:1b,id:"enchanted:hint_of_rebirth",Count:2b},{Slot:2b,id:"minecraft:shield",Count:1b}]}}')
open_at(8,9);place('spinning_wheel',380,22)
if modern:
    for slot,item,count in [(0,'enchanted:poppet',2),(1,'enchanted:hint_of_rebirth',2),(2,'minecraft:shield',1)]:c.insert('spinning_wheel',slot,item,count)

c.cmd('setblock 10 65 9 enchanted:poppet_shelf')
open_at(10,9);place('poppet_shelf',24,130)

if not any(w['kind']=='INVENTORY' for w in c.state['workspaceWindows']):c.call('inventory',ticks=4)
place_window(next(w for w in c.state['workspaceWindows'] if w['kind']=='INVENTORY'),360,140)
c.cmd('tp @a 7.5 65 11.5 180 18')
c.call('wait',ticks=210)
assert len([w for w in c.state['allWindows'] if w.get('menuId','').startswith('enchanted:')])==5
c.capture('enchanted-five-windows-fullscreen')
c.snap('five-windows-workspace')
print('Captured five live Enchanted windows and separate Inventory.',flush=True)
