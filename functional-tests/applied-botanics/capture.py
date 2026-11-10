"""Capture the actual Applied Botanics window and native settings subview."""
from focused import AppBotCheck
import sys

c=AppBotCheck(sys.argv[1])
c.call('close');c.call('fixture',block='minecraft:air');c.cmd('gamemode survival @a')
c.call('held',item='appbot:portable_mana_storage_cell_1k',snbt='{internalCurrentPower:20000.0d,amount:250000L}')
c.call('use-item',shift=False,ticks=15);c.call('inventory')
c.cmd('item replace entity @a inventory.0 with botania:mana_tablet{mana:100000}')
c.cmd('item replace entity @a inventory.1 with ae2:energy_card')
c.inventory_tap(10);c.snap('final-upgrade-picked-up');c.native(213,16)
c.call('move',x=639,y=449,ticks=160)
c.capture('applied-botanics-siu-fullscreen')
c.snap('final-workspace')
c.native(-11,55);c.capture('applied-botanics-settings-fullscreen')
back=next(w for w in c.window()['nativeWidgets'] if w['type'].endswith('.TabButton') and w['visible'])
c.tap(back['x']+back['width']//2,back['y']+back['height']//2)
print('Captured live mana cell, upgrades, separate Inventory and settings.',flush=True)
