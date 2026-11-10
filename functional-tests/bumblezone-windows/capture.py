"""Arrange real live windows for the Bumblezone implementation screenshots."""
from focused import Check
from pathlib import Path
import sys

c=Check(Path(sys.argv[1]))

def place(menu,x,y):
    b=c.window(menu)['bounds']
    assert c.drag(menu,x-b['x'],y-b['y']),menu

def place_inventory(x,y):
    w=next(w for w in c.state['workspaceWindows'] if w['kind']=='INVENTORY')
    if w['locked']=='true':
        c.tap(w['controls']['LOCK'])
    b=w['bounds'];px=b['x']+25;py=b['y']+7;dx=x-b['x'];dy=y-b['y']
    c.call('click',x=px,y=py,button=0,ticks=1)
    c.call('drag',x=px+dx,y=py+dy,dx=dx,dy=dy,button=0,ticks=1)
    c.call('release',x=px+dx,y=py+dy,button=0,ticks=2)

c.cmd('gamemode survival @a')
c.cmd('xp add @a 100 levels')
c.open_block('the_bumblezone:crystalline_flower')
c.inventory()
c.cmd('item replace entity @a inventory.0 with minecraft:book 1')
c.tap(c.item_cell());c.native('crystalline_flower',100,36)
c.native('crystalline_flower',54,21)
c.native('crystalline_flower',95,55)
c.call('move',x=639,y=449,ticks=120)
c.capture('crystalline-flower-final-fullscreen')
place('crystalline_flower',12,108)
place_inventory(12,266)

c.cmd('setblock 10 65 8 the_bumblezone:honey_cocoon')
c.cmd('data merge block 10 65 8 {Items:[{Slot:0b,id:"minecraft:honey_bottle",Count:8b},{Slot:1b,id:"the_bumblezone:pollen_puff",Count:12b}]}')
c.cmd('tp @a 10.5 65 12.5 180 18')
c.call('use-block-ray',ticks=12)
place('strict_9x2',12,25)
c.capture('honey-cocoon-final-fullscreen')

bees=[]
for i in range(14):
    bees.append('{id:"minecraft:bee",UUID:[I;4,5,6,'+str(140+i)+'],Health:10.0f,Age:0,HasStung:0b,HasNectar:1b}')
c.call('held',item='the_bumblezone:buzzing_briefcase',snbt='{BeesStored:['+','.join(bees)+']}')
c.call('use-item',shift=True,ticks=12)
place('buzzing_briefcase',244,135)
c.call('move',x=639,y=449,ticks=120)
c.capture('bumblezone-all-windows-fullscreen')
c.snap('final-workspace')
print('Captured three real Bumblezone windows with separate Inventory.',flush=True)
